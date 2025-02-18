package panels

import models.ColumnSet
import models.isNotListReference

interface AdminJdbcTable : AdminPanel {
    fun getAllColumns(): Collection<ColumnSet>
    fun getTableName(): String
}


fun AdminJdbcTable.getAllAllowToShowColumns() = getAllColumns().filter { it.showInPanel && it.isNotListReference }

fun AdminJdbcTable.getAllAllowToShowColumnsInUpsert() =
    getAllAllowToShowColumns().filter { it.showInPanel && it.autoNowDate == null }

fun AdminJdbcTable.getAllAllowToShowColumnsInUpsertView() =
    getAllColumns().filter { it.showInPanel && it.autoNowDate == null }


fun AdminJdbcTable.getAllAutoNowDateInsertColumns() =
    getAllAllowToShowColumns().filter { it.autoNowDate != null }

fun AdminJdbcTable.getAllAutoNowDateUpdateColumns() =
    getAllAllowToShowColumns().filter { it.autoNowDate != null && it.autoNowDate.updateOnChange }