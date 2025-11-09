package ir.amirroid.ktoradmin.models.date

class AutoNowDate(
    val updateOnChange: Boolean
)

fun AutoNowDate.toFormattedString() = "AutoNowDate(updateOnChange= $updateOnChange)"