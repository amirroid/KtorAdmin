---
layout:
  width: default
  title:
    visible: true
  description:
    visible: false
  tableOfContents:
    visible: true
  outline:
    visible: true
  pagination:
    visible: true
  metadata:
    visible: true
  tags:
    visible: true
  actions:
    visible: true
---

# Chart Section

**Overview**

The `ChartDashboardSection` is an abstract base class for configuring charts in the dashboard section. To add a chart widget to the admin dashboard, you need to extend this class and define its required properties.

#### **Required Properties**

* **`aggregationFunction`** (`ChartDashboardAggregationFunction`): Determines how data is aggregated (e.g., SUM, AVERAGE).
* **`tableName`** (`String`): Specifies the database table from which data is fetched.
* **`labelField`** (`String`): The field used for labeling data points on the X-axis.
* **`valuesFields`** (`List<ChartField>`): A list of fields representing values plotted on the Y-axis.
* **`chartStyle`** (`AdminChartStyle`): Defines the visual representation style of the chart (e.g., LINE, BAR, PIE).

#### **Optional Properties (with Defaults)**

* **`limitCount`** (`Int?`): Maximum number of data points to display (default: `null`).
* **`orderQuery`** (`String?`): SQL-style sorting condition (default: `null`).
* **`tension`** (`Float`): Defines line smoothness (range: `0.0f` to `1.0f`, default: `0f`).
* **`borderWidth`** (`Float`): Thickness of chart borders (default: `1f`).
* **`borderRadius`** (`Float`): Corner radius for border styling (default: `0f`).
* **`tooltipFormat`** (`String`): Defines how tooltip information is displayed in charts. `{field}` represents the name of the field, and `{value}` represents its corresponding data value.

#### **Customization Methods**

* **`provideBorderColor(label: String, valueField: String): String?`**: Returns a custom border color based on the label and value field.
* **`provideFillColor(label: String, valueField: String): String?`**: Returns a custom fill color based on the label and value field.

#### **Example Usage**

```kotlin
class SalesChart : ChartDashboardSection() {
    override val aggregationFunction = ChartDashboardAggregationFunction.SUM
    override val tableName = "sales_data"
    override val labelField = "category"
    override val valuesFields = listOf(ChartField("sales"), ChartField("profit"))
    override val chartStyle = AdminChartStyle.PIE
    override val limitCount = 10
    override val orderQuery = "date DESC"
    override val tension = 0.6f
    override val borderWidth = 2f
}
```

For displaying the configured chart in the dashboard, refer to the section on [**how to configure the dashboard**.](managing-section-layouts.md)

***

### **Related Classes & Enums**

#### **ChartDashboardAggregationFunction**

Defines aggregation functions available for data processing:

* **ALL**: No aggregation applied.
* **SUM**: Sums up values.
* **COUNT**: Counts occurrences.
* **AVERAGE**: Computes the average of values.

#### **ChartField**

Represents a field in a chart with a display-friendly name.

**Properties:**

* **`fieldName`** (`String`): The original field name.
* **`displayName`** (`String`): Readable display name (default: capitalized `fieldName`).

**Example:**

```kotlin
ChartField("price")  // fieldName = "price", displayName = "Price"
```

#### **AdminChartStyle**

Defines available chart styles:

* **LINE**: Connected line (for trends over time).
* **BAR**: Rectangular bars (for category comparison).
* **PIE**: Proportional slices (for whole-part visualization).
* **DOUGHNUT**: Similar to PIE with a hollow center.
* **RADAR**: Circular layout for multivariate comparisons.
* **POLAR\_AREA**: Fixed angular width visualization.
