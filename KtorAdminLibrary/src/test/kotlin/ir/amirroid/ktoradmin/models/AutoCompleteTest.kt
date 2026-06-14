package ir.amirroid.ktoradmin.models

import ir.amirroid.ktoradmin.models.common.Reference
import ir.amirroid.ktoradmin.models.common.tableName
import ir.amirroid.ktoradmin.models.types.ColumnType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoCompleteTest {
    @Test
    fun `ColumnSet hasAutoComplete defaults to false`() {
        val column =
            ColumnSet(
                columnName = "user_id",
                verboseName = "User ID",
                type = ColumnType.INTEGER,
            )
        assertFalse(column.hasAutoComplete)
        assertTrue(column.autoCompleteSearchFields.isEmpty())
    }

    @Test
    fun `ColumnSet can have hasAutoComplete set to true`() {
        val column =
            ColumnSet(
                columnName = "user_id",
                verboseName = "User ID",
                type = ColumnType.INTEGER,
                hasAutoComplete = true,
                reference =
                    Reference.ManyToOne(
                        relatedTable = "users",
                        foreignKey = "id",
                    ),
            )
        assertTrue(column.hasAutoComplete)
    }

    @Test
    fun `ColumnSet with reference and hasAutoComplete`() {
        val column =
            ColumnSet(
                columnName = "user_id",
                verboseName = "Users",
                type = ColumnType.INTEGER,
                hasAutoComplete = true,
                reference =
                    Reference.ManyToOne(
                        relatedTable = "users",
                        foreignKey = "id",
                    ),
            )
        assertTrue(column.hasAutoComplete)
        assertEquals("users", column.reference?.tableName)
    }

    @Test
    fun `ColumnSet copy preserves hasAutoComplete`() {
        val original =
            ColumnSet(
                columnName = "user_id",
                verboseName = "User ID",
                type = ColumnType.INTEGER,
                hasAutoComplete = true,
                reference =
                    Reference.ManyToOne(
                        relatedTable = "users",
                        foreignKey = "id",
                    ),
            )
        val copy = original.copy(columnName = "user_id_v2")
        assertTrue(copy.hasAutoComplete)
        assertEquals("user_id_v2", copy.columnName)
    }

    @Test
    fun `ColumnSet with autoCompleteSearchFields`() {
        val searchFields = listOf("username", "email", "id")
        val column =
            ColumnSet(
                columnName = "user_id",
                verboseName = "Users",
                type = ColumnType.INTEGER,
                hasAutoComplete = true,
                autoCompleteSearchFields = searchFields,
                reference =
                    Reference.ManyToOne(
                        relatedTable = "users",
                        foreignKey = "id",
                    ),
            )
        assertTrue(column.hasAutoComplete)
        assertEquals(3, column.autoCompleteSearchFields.size)
        assertEquals("username", column.autoCompleteSearchFields[0])
        assertEquals("email", column.autoCompleteSearchFields[1])
        assertEquals("id", column.autoCompleteSearchFields[2])
    }

    @Test
    fun `ColumnSet with autoCompleteSearchFields preserves on copy`() {
        val searchFields = listOf("username", "email")
        val original =
            ColumnSet(
                columnName = "user_id",
                verboseName = "Users",
                type = ColumnType.INTEGER,
                hasAutoComplete = true,
                autoCompleteSearchFields = searchFields,
                reference =
                    Reference.ManyToOne(
                        relatedTable = "users",
                        foreignKey = "id",
                    ),
            )
        val copy = original.copy(verboseName = "User")
        assertEquals(2, copy.autoCompleteSearchFields.size)
        assertEquals("username", copy.autoCompleteSearchFields[0])
    }
}
