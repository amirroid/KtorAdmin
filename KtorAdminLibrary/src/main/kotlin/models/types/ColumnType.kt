package models.types

enum class ColumnType {
    STRING,
    INTEGER,
    UINTEGER,
    FILE,
    BYTES,
    UBYTES,
    SHORT,
    USHORT,
    LONG,
    ULONG,
    DOUBLE,
    FLOAT,
    BIG_DECIMAL,
    CHAR,
    BINARY,
    BOOLEAN,
    ENUMERATION,
    DATE,
    DURATION,
    DATETIME,
    TIMESTAMP_WITH_TIMEZONE,
    NOT_AVAILABLE
}

internal val ColumnType.isNumeric: Boolean
    get() = when (this) {
        ColumnType.INTEGER, ColumnType.UINTEGER, ColumnType.SHORT, ColumnType.USHORT,
        ColumnType.LONG, ColumnType.ULONG, ColumnType.DOUBLE, ColumnType.FLOAT,
        ColumnType.BIG_DECIMAL -> true

        else -> false
    }