package ir.amirroid.ktoradmin.modules

import io.ktor.server.application.*
import io.ktor.server.auth.principal
import io.ktor.server.routing.*
import ir.amirroid.ktoradmin.authentication.KtorAdminPrincipal
import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.dashboard.base.RenderDashboardSection
import ir.amirroid.ktoradmin.dashboard.chart.ChartDashboardSection
import ir.amirroid.ktoradmin.dashboard.grid.SectionInfo
import ir.amirroid.ktoradmin.dashboard.list.ListDashboardSection
import ir.amirroid.ktoradmin.dashboard.simple.TextDashboardSection
import ir.amirroid.ktoradmin.models.PanelGroup
import ir.amirroid.ktoradmin.models.chart.ChartData
import ir.amirroid.ktoradmin.models.chart.ListData
import ir.amirroid.ktoradmin.models.chart.RenderData
import ir.amirroid.ktoradmin.models.chart.TextData
import ir.amirroid.ktoradmin.models.toTableGroups
import ir.amirroid.ktoradmin.modules.add.handleAddNewItem
import ir.amirroid.ktoradmin.modules.confirmation.handleGetConfirmation
import ir.amirroid.ktoradmin.modules.custompages.handleCustomPage
import ir.amirroid.ktoradmin.modules.list.handlePanelList
import ir.amirroid.ktoradmin.modules.update.handleEditItem
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.panels.AdminMongoCollection
import ir.amirroid.ktoradmin.panels.AdminPanel
import ir.amirroid.ktoradmin.repository.JdbcQueriesRepository
import ir.amirroid.ktoradmin.repository.MongoClientRepository
import ir.amirroid.ktoradmin.template.TemplateModel
import ir.amirroid.ktoradmin.utils.Constants
import ir.amirroid.ktoradmin.utils.addCommonModels
import ir.amirroid.ktoradmin.utils.forbidden
import ir.amirroid.ktoradmin.utils.serverError
import ir.amirroid.ktoradmin.utils.withAuthenticate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal fun Routing.configureGetRouting(
    panels: List<AdminPanel>,
    authenticateName: String? = null,
) {
    val panelGroups = panels.toTableGroups()
    withAuthenticate(authenticateName) {
        route("/${DynamicConfiguration.adminPath}") {
            get {
                call.renderAdminPanel(panelGroups, panels)
            }
            route("/${Constants.RESOURCES_PATH}") {
                // Database resource routes (specific, matched first)
                route("/{pluralName}") {
                    get {
                        val pagePath = call.parameters["pluralName"]!!
                        val customPage = DynamicConfiguration.getCustomPage(pagePath)
                        if (customPage != null) {
                            call.handleCustomPage(panelGroups, pagePath)
                        } else {
                            call.handlePanelList(panels, panelGroups)
                        }
                    }
                    get("/add") {
                        call.handleAddNewItem(panels, panelGroups)
                    }
                    get("/{primaryKey}") {
                        val pluralName = call.parameters["pluralName"]!!
                        val primaryKey = call.parameters["primaryKey"]!!
                        val fullPath = "$pluralName/$primaryKey"
                        val customPage = DynamicConfiguration.getCustomPage(fullPath)
                        if (customPage != null) {
                            call.handleCustomPage(panelGroups, fullPath)
                        } else {
                            call.handleEditItem(panels, panelGroups)
                        }
                    }
                    get("/{primaryKey}/{field}") {
                        val pluralName = call.parameters["pluralName"]!!
                        val primaryKey = call.parameters["primaryKey"]!!
                        val field = call.parameters["field"]!!
                        val fullPath = "$pluralName/$primaryKey/$field"
                        val customPage = DynamicConfiguration.getCustomPage(fullPath)
                        if (customPage != null) {
                            call.handleCustomPage(panelGroups, fullPath)
                        } else {
                            call.handleGetConfirmation(panels, panelGroups)
                        }
                    }
                }
                // Custom page routes (catch-all for 4+ segment nested paths)
                get("/{pagePath...}") {
                    call.handleCustomPage(panelGroups)
                }
            }
        }
    }
}

private suspend fun ApplicationCall.renderAdminPanel(
    panelGroups: List<PanelGroup>,
    panels: List<AdminPanel>,
) {
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
        val model =
            TemplateModel(
                mutableMapOf(
                    "panelGroups" to panelGroups,
                    "sectionsData" to
                        sectionsData.associateBy {
                            when (it) {
                                is TextData -> it.section.index
                                is ChartData -> it.section.index
                                is ListData -> it.section.index
                                is RenderData -> it.section.index
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
                }.addCommonModels(null, panelGroups, applicationCall = this),
            )
        DynamicConfiguration.template.renderDashboard(this, model)
    }.onFailure {
        serverError(it.message.orEmpty(), it)
    }
}

internal suspend fun getSectionsData(panels: List<AdminPanel>): List<Any> =
    coroutineScope {
        val tablePanels = panels.filterIsInstance<AdminJdbcTable>()
        val collectionPanels = panels.filterIsInstance<AdminMongoCollection>()

        // Using async to launch each section's data loading concurrently
        DynamicConfiguration.dashboard?.grid?.let { grid ->
            val sectionJobs =
                grid.sections.map { section ->
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

                            is RenderDashboardSection -> {
                                val html = section.render()
                                RenderData(section = section, html = html)
                            }

                            else -> null
                        }
                    }
                }

            // Waiting for all jobs to complete and returning the results
            return@coroutineScope sectionJobs.awaitAll().filterNotNull()
        } ?: emptyList()
    }

internal fun getSectionsInfo(): List<SectionInfo> = DynamicConfiguration.dashboard?.grid?.toSectionInfo() ?: emptyList()
