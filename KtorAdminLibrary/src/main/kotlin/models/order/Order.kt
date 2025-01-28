package models.order


data class Order(
    val name: String,
    val direction: String = "ASC",
)


fun Order.toFormattedString(): String {
    return """
        |Order(
        |       name = "$name",
        |       direction = "$direction",
        |)
    """.trimMargin("|")
}