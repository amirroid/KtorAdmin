package ir.amirroid.ktoradmin.provider.defaultvalue

object DefaultValueProviderRegistry {
    private val cache =
        mutableMapOf<String, ClientDefaultValueProvider>()

    fun register(
        key: String,
        provider: ClientDefaultValueProvider,
    ) {
        cache[key] = provider
    }

    fun get(key: String): ClientDefaultValueProvider =
        cache[key]
            ?: error("No provider registered for key: $key")
}
