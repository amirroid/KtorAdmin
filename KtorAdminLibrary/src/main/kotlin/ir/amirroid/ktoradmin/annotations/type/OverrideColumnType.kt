package ir.amirroid.ktoradmin.annotations.type

import ir.amirroid.ktoradmin.models.types.ColumnType

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class OverrideColumnType(val type: ColumnType)