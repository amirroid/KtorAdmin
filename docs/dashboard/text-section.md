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

# Text Section

**Overview**

The `TextDashboardSection` is an abstract base class designed for displaying textual data within a dashboard section. It provides a structured approach to retrieving and presenting text-based information from a database table.

#### **Required Properties**

* **`tableName`** (`String`): Specifies the database table from which data is retrieved.
* **`fieldName`** (`String`): Defines the specific field that contains the text data to be displayed.
* **`hintText`** (`String`): A placeholder or descriptive text explaining the displayed data.
* **`aggregationFunction`** (`TextDashboardAggregationFunction`): Determines how text data is processed (e.g., COUNT, AVERAGE, LAST\_ITEM).

#### **Optional Properties (with Defaults)**

* **`orderQuery`** (`String?`): An optional SQL-style sorting condition for retrieving data (default: `null`).

#### **Aggregation Functions**

The `TextDashboardAggregationFunction` enum defines the available aggregation functions for processing text-based data:

* **`SUM`**: Computes the sum of numerical values in the specified field.
* **`COUNT`**: Counts the number of occurrences in the specified field.
* **`AVERAGE`**: Calculates the average of numerical values in the specified field.
* **`PROFIT_PERCENTAGE`**: Computes the profit percentage based on relevant numeric data fields. For accurate results, it is recommended to set `orderQuery` so that items are sorted from latest to earliest.
* **`LAST_ITEM`**: Retrieves the most recent item in the specified field (supports non-numeric values).

#### **Usage Notes**

* When using **`SUM`**, **`COUNT`**, **`AVERAGE`**, or **`PROFIT_PERCENTAGE`**, ensure that the specified `fieldName` contains numeric data.
* **`LAST_ITEM`** is the only function that supports non-numeric fields.
* For **`PROFIT_PERCENTAGE`**, it is recommended to use an `orderQuery` that sorts items from latest to earliest to ensure correct calculations. Otherwise, the percentage calculation may be reversed, leading to inaccurate results.

#### **Example Usage**

```kotlin
class TotalUsersSection : TextDashboardSection() {
    override val tableName = "users"
    override val fieldName = "id" // Numeric field
    override val hintText = "Total number of users"
    override val aggregationFunction = TextDashboardAggregationFunction.COUNT // Valid
}

class LatestCommentSection : TextDashboardSection() {
    override val tableName = "user_comments"
    override val fieldName = "comment_text" // Non-numeric field
    override val hintText = "Latest user comment"
    override val aggregationFunction = TextDashboardAggregationFunction.LAST_ITEM // Valid
    override val orderQuery = "created_at DESC"
}

class ProfitPercentageSection : TextDashboardSection() {
    override val tableName = "sales"
    override val fieldName = "profit"
    override val hintText = "Profit percentage calculation"
    override val aggregationFunction = TextDashboardAggregationFunction.PROFIT_PERCENTAGE // Valid
    override val orderQuery = "date DESC" // Recommended sorting order
}
```

For displaying the configured chart in the dashboard, refer to the section on [**how to configure the dashboard**.](managing-section-layouts.md)

#### **Conclusion**

To integrate a text-based widget into the dashboard panel, extend `TextDashboardSection` and implement the required properties. For detailed instructions on configuring the dashboard, refer to the dashboard configuration section.
