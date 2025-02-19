package models.common

sealed class Reference(val type: String) {
    /**
     * Represents a one-to-one relationship between two tables.
     */
    data class OneToOne(
        val relatedTable: String,
        val foreignKey: String,
    ) : Reference("select")

    /**
     * Represents a many-to-one relationship between two tables.
     */
    data class ManyToOne(
        val relatedTable: String,
        val foreignKey: String,
    ) : Reference("select")


    /**
     * Represents a many-to-many relationship between two tables using a join table.
     */
    data class ManyToMany(
        val relatedTable: String,
        val joinTable: String,
        val leftPrimaryKey: String,
        val rightPrimaryKey: String,
    ) : Reference("list")
}

/**
 * Converts a Reference instance into a formatted string representation.
 */
fun Reference.toFormattedString(): String {
    return when (this) {
        is Reference.OneToOne -> """
            |Reference.OneToOne(
            |   relatedTable = "$relatedTable",
            |   foreignKey = "$foreignKey"
            |)
        """.trimMargin()

        is Reference.ManyToOne -> """
            |Reference.ManyToOne(
            |   relatedTable = "$relatedTable",
            |   foreignKey = "$foreignKey"
            |)
        """.trimMargin()

        is Reference.ManyToMany -> """
            |Reference.ManyToMany(
            |   relatedTable = "$relatedTable",
            |   joinTable = "$joinTable",
            |   leftPrimaryKey = "$leftPrimaryKey",
            |   rightPrimaryKey = "$rightPrimaryKey"
            |)
        """.trimMargin()
    }
}

val Reference.tableName: String
    get() = when (this) {
        is Reference.OneToOne -> this.relatedTable
        is Reference.ManyToOne -> this.relatedTable
        is Reference.ManyToMany -> this.relatedTable
    }


val Reference.foreignKey: String?
    get() = when (this) {
        is Reference.OneToOne -> this.foreignKey
        is Reference.ManyToOne -> this.foreignKey
        else -> null
    }
