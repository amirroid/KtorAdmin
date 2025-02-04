package models.chart

/**
 * Enum class to define the available chart styles for the admin dashboard.
 *
 * This enum defines various types of charts that can be used in the admin dashboard for visualizing data.
 * Each chart type corresponds to a specific style of data presentation.
 *
 * - `LINE`: Represents a line chart, which is typically used to display trends over time. It’s ideal for showing continuous data and patterns.
 * - `BAR`: Represents a bar chart, commonly used to compare different categories or groups. It uses rectangular bars to display data.
 * - `PIE`: Represents a pie chart, which is used to show parts of a whole. Each slice of the pie represents a proportional part of the total data.
 * - `DOUGHNUT`: Represents a doughnut chart, a variation of the pie chart with a hole in the center. It is often used for similar purposes as pie charts, but with a different visual style.
 * - `RADAR`: Represents a radar chart, used to display multivariate data in a circular format. It is useful for showing how different variables compare to each other in a single view.
 * - `POLAR_AREA`: Represents a polar area chart, which is similar to a radar chart but with each section having a fixed angular width. It’s suitable for representing cyclical data.
 */
enum class AdminChartStyle(val chartType: String) {
    LINE("line"),
    BAR("bar"),
    PIE("pie"),
    DOUGHNUT("doughnut"),
    RADAR("radar"),
    POLAR_AREA("polarArea")
}