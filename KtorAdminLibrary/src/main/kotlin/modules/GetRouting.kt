package modules

import authentication.KtorAdminPrincipal
import configuration.DynamicConfiguration
import dashboard.chart.ChartDashboardSection
import dashboard.row.RowData
import dashboard.simple.TextDashboardSection
import io.ktor.server.application.*
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.velocity.*
import models.PanelGroup
import models.chart.ChartData
import models.chart.TextData
import models.toTableGroups
import modules.add.handleAddNewItem
import modules.list.handlePanelList
import modules.update.handleEditItem
import panels.AdminJdbcTable
import panels.AdminPanel
import repository.JdbcQueriesRepository
import utils.Constants
import utils.serverError
import utils.withAuthenticate


internal fun Routing.configureGetRouting(panels: List<AdminPanel>, authenticateName: String? = null) {
    val panelGroups = panels.toTableGroups()
    withAuthenticate(authenticateName) {
        route("/admin") {
            get {
                call.renderAdminPanel(panelGroups, panels)
            }
            route("/{pluralName}") {
                get {
                    call.handlePanelList(panels, panelGroups)
                }
                get("add") {
                    call.handleAddNewItem(panels, panelGroups)
                }
                get("/{primaryKey}") {
                    call.handleEditItem(panels, panelGroups)
                }
            }
        }
    }
}

private suspend fun ApplicationCall.renderAdminPanel(panelGroups: List<PanelGroup>, panels: List<AdminPanel>) {
    runCatching {
        val sectionsData = getSectionsData(panels)
        val rowData = getRowData()
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_dashboard.vm",
                model = mutableMapOf(
                    "panelGroups" to panelGroups,
                    "username" to principal<KtorAdminPrincipal>()!!.name,
                    "sectionsData" to sectionsData.associateBy {
                        if (it is TextData) {
                            it.section.index
                        } else (it as ChartData).section.index
                    },
                    "rowData" to rowData,
                )
            )
        )
    }.onFailure {
        serverError(it.message.orEmpty(), it)
    }
}


internal fun getSectionsData(panels: List<AdminPanel>): List<Any> {
    return DynamicConfiguration.dashboard?.rows?.map { row ->
        row.sections.mapNotNull { section ->
            when (section) {
                is ChartDashboardSection -> {
                    val table =
                        panels.filterIsInstance<AdminJdbcTable>().first { it.getTableName() == section.tableName }
                    JdbcQueriesRepository.getChartData(table, section)
                }

                is TextDashboardSection -> {
                    val table =
                        panels.filterIsInstance<AdminJdbcTable>().first { it.getTableName() == section.tableName }
                    JdbcQueriesRepository.getTextData(table, section)
                }

                else -> null
            }
        }
    }?.flatten() ?: emptyList()
}


internal fun getRowData(): List<List<RowData>> {
    return DynamicConfiguration.dashboard?.rows?.map { row ->
        row.toRowData()
    } ?: emptyList()
}