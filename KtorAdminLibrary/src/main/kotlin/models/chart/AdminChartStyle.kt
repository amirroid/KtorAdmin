package models.chart

/**
 * Enum class defining the available chart styles for the admin dashboard.
 *
 * This enum represents various chart types that can be used for visualizing data in the admin dashboard.
 * Each type corresponds to a specific way of presenting data.
 *
 * ### **Chart Types:**
 * - **`LINE`** → Displays data as a connected line, ideal for showing trends over time.
 * - **`BAR`** → Uses rectangular bars to compare values across different categories.
 * - **`PIE`** → Divides data into proportional slices to show parts of a whole.
 * - **`DOUGHNUT`** → A variation of the pie chart with a hollow center, offering a different visual representation.
 * - **`RADAR`** → Displays multivariate data in a circular layout, useful for comparing multiple variables.
 * - **`POLAR_AREA`** → Similar to a radar chart but with fixed angular widths, effective for cyclical data.
 * - **`BUBBLE`** → Represents data with circles where position and size encode values, suitable for multi-dimensional analysis.
 * - **`SCATTER`** → Plots data points on a Cartesian plane to show relationships between variables.
 */
enum class AdminChartStyle(val chartType: String) {
    LINE("line"),
    BAR("bar"),
    PIE("pie"),
    DOUGHNUT("doughnut"),
    RADAR("radar"),
    POLAR_AREA("polarArea"),
    BUBBLE("bubble"),
    SCATTER("scatter")
}