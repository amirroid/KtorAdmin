package ir.amirroid.ktoradmin.models.chart

import ir.amirroid.ktoradmin.dashboard.simple.TextDashboardSection

internal data class TextData(
    val section: TextDashboardSection,
    val value: String,
)