package panels

import models.ColumnSet
import models.actions.Action
import models.order.Order

interface AdminJdbcTable : AdminPanel {
    fun getAllColumns(): Collection<ColumnSet>
    fun getTableName(): String
}


fun AdminJdbcTable.getAllAllowToShowColumns() = getAllColumns().filter { it.showInPanel }


fun AdminJdbcTable.getAllAllowToShowColumnsInUpsert() =
    getAllAllowToShowColumns().filter { it.showInPanel && it.autoNowDate == null }


fun AdminJdbcTable.getAllAutoNowDateInsertColumns() =
    getAllAllowToShowColumns().filter { it.autoNowDate != null }

fun AdminJdbcTable.getAllAutoNowDateUpdateColumns() =
    getAllAllowToShowColumns().filter { it.autoNowDate != null && it.autoNowDate.updateOnChange }