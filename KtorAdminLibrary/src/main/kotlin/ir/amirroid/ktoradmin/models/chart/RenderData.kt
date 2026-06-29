package ir.amirroid.ktoradmin.models.chart

import ir.amirroid.ktoradmin.dashboard.base.RenderDashboardSection

internal data class RenderData(
    val section: RenderDashboardSection,
    val html: String,
)
