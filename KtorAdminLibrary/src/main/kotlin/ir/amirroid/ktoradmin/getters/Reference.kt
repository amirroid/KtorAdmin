package ir.amirroid.ktoradmin.getters

import ir.amirroid.ktoradmin.models.ColumnSet
import ir.amirroid.ktoradmin.models.common.DisplayItem
import ir.amirroid.ktoradmin.models.common.Reference
import ir.amirroid.ktoradmin.models.common.tableName
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.panels.getAllAllowToShowColumnsInUpsertView
import ir.amirroid.ktoradmin.repository.JdbcQueriesRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Retrieves reference items for columns with foreign key relationships.
 * Implements a temporary caching mechanism to avoid duplicate database queries
 * for the same referenced table within a single function call.
 *
 * @param tables List of all available database tables
 * @param columns List of columns to check for references
 * @return Map of columns to their referenced display items
 * @throws IllegalArgumentException if any referenced table is not found in the schema
 */
internal suspend fun getReferencesItems(
    tables: List<AdminJdbcTable>,
    columns: List<ColumnSet>
): Map<ColumnSet, List<DisplayItem>> = coroutineScope {
    // Extract columns with references early for better readability
    val columnsWithReferences = columns.filter { it.reference != null }

    // Early validation of tables
    validateReferencedTables(columnsWithReferences, tables)

    // Use temporary cache for optimization
    val tempReferenceCache = mutableMapOf<String, List<DisplayItem>>()

    // Launch async tasks for each column with reference
    columnsWithReferences.map { column ->
        async {
            val referencedTableName = column.reference!!.tableName

            // Get or compute references using cache
            val references = tempReferenceCache.getOrPut(referencedTableName) {
                val referencedTable = tables.first { it.getTableName() == referencedTableName }
                JdbcQueriesRepository.getAllReferences(table = referencedTable)
            }

            column to references
        }
    }.awaitAll().toMap() // Wait for all tasks to complete and return the map
}

/**
 * Retrieves selected reference items for many-to-many relationships in a specific table.
 * Used to show currently selected items in many-to-many relationship views.
 *
 * @param table The current table being processed
 * @param tables List of all available database tables
 * @param primaryKey Primary key value of the current record
 * @return Map of columns to their selected reference items
 * @throws IllegalArgumentException if any referenced table is not found in the schema
 */
internal suspend fun getSelectedReferencesItems(
    table: AdminJdbcTable,
    tables: List<AdminJdbcTable>,
    primaryKey: String
): Map<ColumnSet, Map<String, Any>> = coroutineScope {
    // Get relevant columns with many-to-many references
    val columnsWithReferences = table.getAllAllowToShowColumnsInUpsertView()
        .filter { it.reference is Reference.ManyToMany }

    // Validate join tables exist
    validateManyToManyTables(columnsWithReferences, tables)

    // Launch async tasks for each reference column
    columnsWithReferences.map { column ->
        async {
            val reference = column.reference as Reference.ManyToMany
            val joinTable = tables.first { it.getTableName() == reference.joinTable }

            // Get selected references and map to string keys
            val selectedReferences = JdbcQueriesRepository.getAllSelectedReferenceInListReference(
                table = joinTable,
                columnSet = column,
                primaryKey = primaryKey
            ).associateBy { it.toString() }

            column to selectedReferences
        }
    }.awaitAll().toMap()  // Wait for all tasks to complete and return the map
}

/**
 * Validates that all referenced tables exist in the schema.
 *
 * @param columnsWithReferences List of columns with references to validate
 * @param tables List of all available tables
 * @throws IllegalArgumentException if any referenced table is not found
 */
private fun validateReferencedTables(
    columnsWithReferences: List<ColumnSet>,
    tables: List<AdminJdbcTable>
) {
    val missingTables = columnsWithReferences
        .mapNotNull { column -> column.reference?.tableName }
        .filter { tableName ->
            tables.none { it.getTableName() == tableName }
        }

    if (missingTables.isNotEmpty()) {
        throw IllegalArgumentException(
            "Error: The following tables are missing from the schema: ${missingTables.joinToString()}"
        )
    }
}

/**
 * Validates that all join tables for many-to-many relationships exist in the schema.
 *
 * @param columnsWithReferences List of columns with many-to-many references
 * @param tables List of all available tables
 * @throws IllegalArgumentException if any join table is not found
 */
private fun validateManyToManyTables(
    columnsWithReferences: List<ColumnSet>,
    tables: List<AdminJdbcTable>
) {
    val missingJoinTables = columnsWithReferences
        .map { (it.reference as Reference.ManyToMany).joinTable }
        .filter { joinTable ->
            tables.none { it.getTableName() == joinTable }
        }

    if (missingJoinTables.isNotEmpty()) {
        throw IllegalArgumentException(
            "Error: The following join tables are missing from the schema: ${missingJoinTables.joinToString()}"
        )
    }
}