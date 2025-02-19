package panels

import models.ColumnSet
import models.isNotListReference

interface AdminJdbcTable : AdminPanel {
    fun getAllColumns(): Collection<ColumnSet>
    fun getTableName(): String
    fun getPanelListColumns(): List<String>
}


fun AdminJdbcTable.getAllAllowToShowColumns() =
    getAllColumns().filter { it.showInPanel && it.isNotListReference && it.columnName in getPanelListColumns() }

fun AdminJdbcTable.getAllAllowToShowColumnsInUpsert() =
    getAllColumns().filter { it.showInPanel && it.autoNowDate == null && it.isNotListReference }

fun AdminJdbcTable.getAllAllowToShowColumnsInUpsertView() =
    getAllColumns().filter { it.showInPanel && it.autoNowDate == null }


fun AdminJdbcTable.getAllAutoNowDateInsertColumns() =
    getAllColumns().filter { it.autoNowDate != null }

fun AdminJdbcTable.getAllAutoNowDateUpdateColumns() =
    getAllColumns().filter { it.autoNowDate != null && it.autoNowDate.updateOnChange }