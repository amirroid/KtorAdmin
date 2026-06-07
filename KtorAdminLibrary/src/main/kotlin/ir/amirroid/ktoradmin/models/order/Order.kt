package ir.amirroid.ktoradmin.models.order

data class Order(
    val name: String,
    val direction: String = "ASC",
)

fun Order.toFormattedString(): String =
    """
        |Order(
        |       name = "$name",
        |       direction = "$direction",
        |)
    """.trimMargin("|")
