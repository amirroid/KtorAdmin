package ir.amirroid.ktoradmin.utils

import com.squareup.kotlinpoet.ClassName
import ir.amirroid.ktoradmin.models.ColumnSet
import ir.amirroid.ktoradmin.models.field.FieldSet
import ir.amirroid.ktoradmin.models.types.ColumnType
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.panels.AdminMongoCollection

internal object PackagesUtils {
    fun getColumnSetClass() = ClassName(ColumnSet::class.java.packageName, ColumnSet::class.java.simpleName)
    fun getFieldSetClass() = ClassName(FieldSet::class.java.packageName, FieldSet::class.java.simpleName)
    fun getColumnTypeClass() = ClassName(ColumnType::class.java.packageName, ColumnType::class.java.simpleName)
    fun getAdminTableClass() = ClassName(AdminJdbcTable::class.java.packageName, AdminJdbcTable::class.java.simpleName)
    fun getAdminMongoCollectionClass() = ClassName(AdminMongoCollection::class.java.packageName, AdminMongoCollection::class.java.simpleName)

}