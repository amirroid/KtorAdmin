package utils

import models.ColumnSet

interface AdminTable {
    fun getAllColumns(): Collection<ColumnSet>
    fun getTableName(): String
    fun getSingularName(): String
    fun getPluralName(): String
    fun getGroupName(): String?
    fun getDatabaseKey(): String?
    fun getPrimaryKey(): String
    fun getDisplayFormat(): String?
}


fun AdminTable.getAllAllowToShowColumns() = getAllColumns().filter { it.showInPanel }
fun List<AdminTable>.findWithPluralName(name: String?) = find { it.getPluralName() == name }