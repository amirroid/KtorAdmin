package panels

import models.ColumnSet

interface AdminJdbcTable : AdminPanel {
    fun getAllColumns(): Collection<ColumnSet>
    fun getTableName(): String
    fun getSearchColumns(): List<String>
    fun getFilterColumns(): List<String>
}


fun AdminJdbcTable.getAllAllowToShowColumns() = getAllColumns().filter { it.showInPanel }