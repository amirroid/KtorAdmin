package modules

import authentication.KtorAdminPrincipal
import configuration.DynamicConfiguration
import dashboard.chart.ChartDashboardSection
import dashboard.grid.SectionInfo
import dashboard.list.ListDashboardSection
import dashboard.simple.TextDashboardSection
import io.ktor.server.application.*
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.velocity.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import models.PanelGroup
import models.chart.ChartData
import models.chart.ListData
import models.chart.TextData
import models.toTableGroups
import modules.add.handleAddNewItem
import modules.confirmation.handleGetConfirmation
import modules.list.handlePanelList
import modules.update.handleEditItem
import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.AdminPanel
import repository.JdbcQueriesRepository
import repository.MongoClientRepository
import utils.Constants
import utils.addCommonModels
import utils.forbidden
import utils.serverError
import utils.withAuthenticate


internal fun Routing.configureGetRouting(panels: List<AdminPanel>, authenticateName: String? = null) {
    val panelGroups = panels.toTableGroups()
    withAuthenticate(authenticateName) {
        route("/${DynamicConfiguration.adminPath}") {
            get {
                call.renderAdminPanel(panelGroups, panels)
            }
            route("/${Constants.RESOURCES_PATH}/{pluralName}") {
                get {
                    call.handlePanelList(panels, panelGroups)
                }
                get("/add") {
                    call.handleAddNewItem(panels, panelGroups)
                }
                get("/{primaryKey}") {
                    call.handleEditItem(panels, panelGroups)
                }
                get("/{primaryKey}/{field}") {
                    call.handleGetConfirmation(panels, panelGroups)
                }
            }
        }
    }
}

private suspend fun ApplicationCall.renderAdminPanel(panelGroups: List<PanelGroup>, panels: List<AdminPanel>) {
    runCatching {
        val sectionsData = getSectionsData(panels)
        val sectionsInfo = getSectionsInfo()
        val gridTemplate = DynamicConfiguration.dashboard?.grid?.gridTemplate ?: emptyList()
        val mediaTemplates = DynamicConfiguration.dashboard?.grid?.mediaTemplates ?: emptyList()
        val user = principal<KtorAdminPrincipal>()
        val username = user?.name
        if (user?.dashboardAccess == false) {
            return forbidden("You do not have permission to access the admin dashboard.")
        }
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_dashboard.vm",
                model = mutableMapOf(
                    "panelGroups" to panelGroups,
                    "sectionsData" to sectionsData.associateBy {
                        when (it) {
                            is TextData -> it.section.index
                            is ChartData -> it.section.index
                            is ListData -> it.section.index
                            else -> 0
                        }
                    },
                    "sectionsInfo" to sectionsInfo,
                    "gridTemplate" to gridTemplate,
                    "mediaTemplates" to mediaTemplates,
                ).apply {
                    username?.let {
                        put("username", it)
                    }
                }.addCommonModels(null, panelGroups)
            )
        )
    }.onFailure {
        serverError(it.message.orEmpty(), it)
    }
}


internal suspend fun getSectionsData(panels: List<AdminPanel>): List<Any> = coroutineScope {
    val tablePanels = panels.filterIsInstance<AdminJdbcTable>()
    val collectionPanels = panels.filterIsInstance<AdminMongoCollection>()

    // Using async to launch each section's data loading concurrently
    DynamicConfiguration.dashboard?.grid?.let { grid ->
        val sectionJobs = grid.sections.map { section ->
            async {
                when (section) {
                    is ChartDashboardSection -> {
                        val table = tablePanels.firstOrNull { it.getTableName() == section.tableName }
                        val collection = collectionPanels.firstOrNull { it.getCollectionName() == section.tableName }
                        when {
                            table != null -> JdbcQueriesRepository.getChartData(table, section)
                            collection != null -> MongoClientRepository.getChartData(collection, section)
                            else -> null
                        }
                    }

                    is TextDashboardSection -> {
                        val table = tablePanels.firstOrNull { it.getTableName() == section.tableName }
                        val collection = collectionPanels.firstOrNull { it.getCollectionName() == section.tableName }
                        when {
                            table != null -> JdbcQueriesRepository.getTextData(table, section)
                            collection != null -> MongoClientRepository.getTextData(collection, section)
                            else -> null
                        }
                    }

                    is ListDashboardSection -> {
                        val table = tablePanels.firstOrNull { it.getTableName() == section.tableName }
                        val collection = collectionPanels.firstOrNull { it.getCollectionName() == section.tableName }
                        when {
                            table != null -> JdbcQueriesRepository.getListSectionData(table, section)
                            collection != null -> MongoClientRepository.getListSectionData(collection, section)
                            else -> null
                        }
                    }

                    else -> null
                }
            }
        }

        // Waiting for all jobs to complete and returning the results
        return@coroutineScope sectionJobs.awaitAll().filterNotNull()
    } ?: emptyList()
}

internal fun getSectionsInfo(): List<SectionInfo> {
    return DynamicConfiguration.dashboard?.grid?.toSectionInfo() ?: emptyList()
}