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
     * Represents a one-to-many relationship between two tables.
     */
    data class OneToMany(
        val relatedTable: String,
        val foreignKey: String,
    ) : Reference("select")

    /**
     * Represents a many-to-one relationship between two tables.
     */
    data class ManyToOne(
        val relatedTable: String,
        val foreignKey: String,
    ) : Reference("list")

    /**
     * Represents a many-to-many relationship between two tables using a join table.
     */
    data class ManyToMany(
        val relatedTable: String,
        val joinTable: String,
        val leftPrimaryKey: String,
        val rightPrimaryKey: String,
    ) : Reference("tow_list")
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

        is Reference.OneToMany -> """
            |Reference.OneToMany(
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
        is Reference.OneToMany -> this.relatedTable
        is Reference.ManyToMany -> this.relatedTable
        is Reference.ManyToOne -> this.relatedTable
    }


val Reference.foreignKey: String
    get() = when (this) {
        is Reference.OneToOne -> this.foreignKey
        is Reference.ManyToOne -> this.foreignKey
        is Reference.ManyToMany -> this.rightPrimaryKey
        is Reference.OneToMany -> this.foreignKey
    }
