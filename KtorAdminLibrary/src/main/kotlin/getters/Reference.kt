package getters

import models.ColumnSet
import models.common.DisplayItem
import models.common.Reference
import models.common.tableName
import repository.JdbcQueriesRepository
import panels.AdminJdbcTable
import panels.getAllAllowToShowColumnsInUpsertView

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
internal fun getReferencesItems(
    tables: List<AdminJdbcTable>,
    columns: List<ColumnSet>
): Map<ColumnSet, List<DisplayItem>> {
    // Get columns that have reference relationships defined
    val columnsWithReferences = columns.filter { it.reference != null }

    // Validate that all referenced tables exist in the schema
    validateReferencedTables(columnsWithReferences, tables)

    // Temporary cache to store reference data during function execution
    val tempReferenceCache = mutableMapOf<String, List<DisplayItem>>()

    return columnsWithReferences.associateWith { column ->
        val tableName = column.reference!!.tableName
        // Fetch and cache reference data for each table (only once per table)
        tempReferenceCache.getOrPut(tableName) {
            JdbcQueriesRepository.getAllReferences(
                table = tables.first { it.getTableName() == tableName }
            )
        }
    }
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
internal fun getSelectedReferencesItems(
    table: AdminJdbcTable,
    tables: List<AdminJdbcTable>,
    primaryKey: String
): Map<ColumnSet, Map<String, Any>> {
    // Get columns with many-to-many relationships that are allowed to be shown
    val columnsWithReferences = table.getAllAllowToShowColumnsInUpsertView()
        .filter { it.reference is Reference.ManyToMany }

    // Validate that all referenced tables exist in the schema
    validateReferencedTables(columnsWithReferences, tables)

    return columnsWithReferences.associateWith { column ->
        // Get all selected references for the column and create a map with string keys
        JdbcQueriesRepository.getAllSelectedReferenceInListReference(
            table = table,
            columnSet = column,
            primaryKey = primaryKey
        ).associateBy { it.toString() }
    }
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
    val missingTables = columnsWithReferences.any { column ->
        tables.none { it.getTableName() == column.reference!!.tableName }
    }
    if (missingTables) {
        throw IllegalArgumentException(
            "Error: Some referenced tables do not exist or are not defined in the current schema."
        )
    }
}