package utils

import configuration.DynamicConfiguration
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.PanelGroup
import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.AdminPanel
import panels.getAllAllowToShowColumnsInUpsert
import panels.getAllAllowToShowFieldsInUpsert
import repository.JdbcQueriesRepository
import repository.MongoClientRepository

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

internal suspend fun MutableMap<String, Any>.addCommonModels(panelGroups: List<PanelGroup>): Map<String, Any> =
    coroutineScope {
        val panels = panelGroups.map { it.panels }.flatten()
        this@addCommonModels.apply {
            val counts = mutableMapOf<String, Long>()

            val tables = panels.filterIsInstance<AdminJdbcTable>()
            val mongoPanels = panels.filterIsInstance<AdminMongoCollection>()

            val jdbcCountsDeferred = async {
                JdbcQueriesRepository.getCountOfTables(tables).filterValues { it != 0L }
            }
            val mongoCountsDeferred = async {
                MongoClientRepository.getCountOfCollections(mongoPanels).filterValues { it != 0L }
            }

            val jdbcCounts = jdbcCountsDeferred.await()
            val mongoCounts = mongoCountsDeferred.await()

            counts.putAll(jdbcCounts)
            counts.putAll(mongoCounts)

            put("counts", counts)
        }
    }