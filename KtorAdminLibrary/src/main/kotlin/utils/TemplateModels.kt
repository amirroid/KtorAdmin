package utils

import configuration.DynamicConfiguration
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.getAllAllowToShowColumnsInUpsert
import panels.getAllAllowToShowFieldsInUpsert

internal fun Map<String, Any>.addCommonUpsertModels(table: AdminJdbcTable, username: String?): Map<String, Any> {
    return toMutableMap().apply {
        if (table.getAllAllowToShowColumnsInUpsert().any { it.hasRichEditor }) {
            val json = Json {
                encodeDefaults = true
            }
            val config = json.encodeToString(DynamicConfiguration.tinyMCEConfig)
            set("tinyMCEConfig", config)
        }
        username?.let { put("username", it) }
    }.toMap()
}

internal fun Map<String, Any>.addCommonUpsertModels(table: AdminMongoCollection, username: String?): Map<String, Any> {
    return toMutableMap().apply {
        if (table.getAllAllowToShowFieldsInUpsert().any { it.hasRichEditor }) {
            val json = Json {
                encodeDefaults = true
            }
            val config = json.encodeToString(DynamicConfiguration.tinyMCEConfig)
            set("tinyMCEConfig", config)
        }
        username?.let { put("username", it) }
    }.toMap()
}