package utils

import com.squareup.kotlinpoet.ClassName
import models.ColumnSet
import models.ColumnType
import tables.AdminJdbcTable

object PackagesUtils {
    fun getColumnSetClass() = ClassName(ColumnSet::class.java.packageName, ColumnSet::class.java.simpleName)
    fun getColumnTypeClass() = ClassName(ColumnType::class.java.packageName, ColumnType::class.java.simpleName)
    fun getAdminTableClass() = ClassName(AdminJdbcTable::class.java.packageName, AdminJdbcTable::class.java.simpleName)

}