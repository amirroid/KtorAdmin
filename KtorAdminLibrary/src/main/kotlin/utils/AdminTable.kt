package utils

import models.ColumnSet

interface AdminTable {
    fun getAllColumns(): Collection<ColumnSet>
    fun getTableName(): String
    fun getPluralName(): String
    fun getGroupName(): String
}