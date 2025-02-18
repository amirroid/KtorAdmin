package annotations.type

import models.types.ColumnType

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class OverrideColumnType(val type: ColumnType)