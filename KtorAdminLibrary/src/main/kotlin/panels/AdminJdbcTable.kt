package panels

import models.ColumnSet

interface AdminJdbcTable : AdminPanel {
    fun getAllColumns(): Collection<ColumnSet>
    fun getTableName(): String
}


fun AdminJdbcTable.getAllAllowToShowColumns() = getAllColumns().filter { it.showInPanel }


fun AdminJdbcTable.getAllAllowToShowFieldsInUpsert() =
    getAllAllowToShowColumns().filter { it.showInPanel && it.columnName != getPrimaryKey() && !it.autoNowDate }