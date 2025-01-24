package utils

import com.squareup.kotlinpoet.ClassName
import models.ColumnSet
import models.field.FieldSet
import models.types.ColumnType
import panels.AdminJdbcTable
import panels.AdminMongoCollection

internal object PackagesUtils {
    fun getColumnSetClass() = ClassName(ColumnSet::class.java.packageName, ColumnSet::class.java.simpleName)
    fun getFieldSetClass() = ClassName(FieldSet::class.java.packageName, FieldSet::class.java.simpleName)
    fun getColumnTypeClass() = ClassName(ColumnType::class.java.packageName, ColumnType::class.java.simpleName)
    fun getAdminTableClass() = ClassName(AdminJdbcTable::class.java.packageName, AdminJdbcTable::class.java.simpleName)
    fun getAdminMongoCollectionClass() = ClassName(AdminMongoCollection::class.java.packageName, AdminMongoCollection::class.java.simpleName)

}