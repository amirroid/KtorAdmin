package models.chart

/**
 * Represents a field in a chart with a display-friendly name and toolbox text.
 *
 * @property fieldName The original name of the field.
 * @property displayName A more readable name for display, defaults to capitalizing the first letter of `fieldName`.
 *
 * ### Example Output:
 * ```
 * ChartField("price")
 * → fieldName = "price"
 * → displayName = "Price"
 *
 * ChartField("quantity", "Quantity")
 * → fieldName = "quantity"
 * → displayName = "Quantity"
 * ```
 */
class ChartField(
    val fieldName: String,
    val displayName: String = fieldName.replaceFirstChar { it.uppercase() },
)