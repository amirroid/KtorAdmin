package ir.amirroid.ktoradmin.provider.defaultvalue

/**
 * Provides a client-side default value for a column or field.
 *
 * This is used during insert/update operations when a value is missing (null),
 * and the system supplies a computed default value at runtime.
 *
 * Each provider must have a unique key used for registry lookup.
 */
interface ClientDefaultValueProvider {
    /**
     * Unique key used to register and resolve this provider from registry.
     */
    val key: String

    /**
     * Returns a default value for the target field.
     * If null is returned, no default will be applied.
     */
    fun provide(): Any?
}
