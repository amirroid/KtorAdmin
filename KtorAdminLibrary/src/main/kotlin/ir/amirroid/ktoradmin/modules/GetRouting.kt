package ir.amirroid.ktoradmin.modules

import ir.amirroid.ktoradmin.authentication.KtorAdminPrincipal
import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.dashboard.chart.ChartDashboardSection
import ir.amirroid.ktoradmin.dashboard.grid.SectionInfo
import ir.amirroid.ktoradmin.dashboard.list.ListDashboardSection
import ir.amirroid.ktoradmin.dashboard.simple.TextDashboardSection
import io.ktor.server.application.*
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.velocity.*
import ir.amirroid.ktoradmin.models.PanelGroup
import ir.amirroid.ktoradmin.models.chart.ChartData
import ir.amirroid.ktoradmin.models.chart.ListData
import ir.amirroid.ktoradmin.models.chart.TextData
import ir.amirroid.ktoradmin.models.toTableGroups
import ir.amirroid.ktoradmin.modules.add.handleAddNewItem
import ir.amirroid.ktoradmin.modules.confirmation.handleGetConfirmation
import ir.amirroid.ktoradmin.modules.list.handlePanelList
import ir.amirroid.ktoradmin.modules.update.handleEditItem
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.panels.AdminMongoCollection
import ir.amirroid.ktoradmin.panels.AdminPanel
import ir.amirroid.ktoradmin.repository.JdbcQueriesRepository
import ir.amirroid.ktoradmin.repository.MongoClientRepository
import ir.amirroid.ktoradmin.utils.Constants
import ir.amirroid.ktoradmin.utils.addCommonModels
import ir.amirroid.ktoradmin.utils.forbidden
import ir.amirroid.ktoradmin.utils.serverError
import ir.amirroid.ktoradmin.utils.withAuthenticate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


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
                }.addCommonModels(null, panelGroups, applicationCall = this)
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