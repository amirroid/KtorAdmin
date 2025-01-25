package ir.amirreza

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//object CustomObjectIdSerializer: KSerializer<String> {
//    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ObjectIdSerializer", PrimitiveKind.STRING)
//    override fun deserialize(decoder: Decoder): String {
//        return when (decoder) {
//            is BsonDecoder -> decoder.decodeBsonValue().asObjectId().value.toHexString()
//            else -> throw SerializationException("ObjectId is not supported by ${decoder::class}")
//        }
//    }
//
//    override fun serialize(encoder: Encoder, value: String) {
//        return when (encoder) {
//            is JsonEncoder -> encoder.encodeString(value)
//            else -> throw SerializationException("ObjectId is not supported by ${encoder::class}")
//        }
//    }
//}

@Serializable
data class UserFile(
    @SerialName("_id")
//    @Serializable(CustomObjectIdSerializer::class)
    val id: String? = null,
    val title: String,
    val file: String,
    val createdAt: Instant = Clock.System.now()
)