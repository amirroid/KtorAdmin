package ir.amirroid.ktoradmin.provider.defaultvalue.uuid

import ir.amirroid.ktoradmin.provider.defaultvalue.ClientDefaultValueProvider
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class UUIDVersion {
    V7,
    V4,
}

class UUIDDefaultValueProvider(
    private val version: UUIDVersion = UUIDVersion.V4,
) : ClientDefaultValueProvider {
    override val key: String =
        when (version) {
            UUIDVersion.V4 -> "uuid-v4"
            UUIDVersion.V7 -> "uuid-v7"
        }

    @OptIn(ExperimentalUuidApi::class)
    override fun provide(): Uuid =
        when (version) {
            UUIDVersion.V4 -> Uuid.generateV4()
            UUIDVersion.V7 -> Uuid.generateV7()
        }
}
