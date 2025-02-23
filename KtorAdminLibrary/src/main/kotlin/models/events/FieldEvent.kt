package models.events

import models.field.FieldSet

/**
 * Represents an event related to a field's state, such as a change in its value.
 *
 * @property changed Indicates whether the field's value has been changed.
 * @property fieldSet Metadata about the field, including its name, type, and additional attributes.
 * @property value The current value of the column. Its type can vary (e.g., numeric, string, byte array, etc.)
 * based on the field's defined type.
 */
data class FieldEvent(
    val changed: Boolean,
    val fieldSet: FieldSet,
    val value: Any?
)
