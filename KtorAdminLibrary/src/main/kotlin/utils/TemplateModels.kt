package utils

import configuration.DynamicConfiguration
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import panels.AdminJdbcTable
import panels.getAllAllowToShowColumnsInUpsert

fun Map<String, Any>.addCommonUpsertModels(table: AdminJdbcTable): Map<String, Any> {
    return toMutableMap().apply {
        if (table.getAllAllowToShowColumnsInUpsert().any { it.hasRichEditor }) {
            val config = Json.encodeToString(DynamicConfiguration.tinyMCEConfig)
            set("tinyMCEConfig", config)
        }
    }.toMap()
}