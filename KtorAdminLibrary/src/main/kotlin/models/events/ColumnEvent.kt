package models.events

import models.ColumnSet

/**
 * Represents an event related to a column's state, such as a change in its value.
 *
 * @property changed Indicates whether the column's value has been changed.
 * @property columnSet Metadata about the column, including its name, type, and additional attributes.
 * @property value The current value of the column. Its type can vary (e.g., numeric, string, byte array, etc.)
 * based on the column's defined type.
 */
data class ColumnEvent(
    val changed: Boolean,
    val columnSet: ColumnSet,
    val value: Any?
)