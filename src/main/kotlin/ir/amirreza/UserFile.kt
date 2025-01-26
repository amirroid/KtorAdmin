package ir.amirreza

import annotations.enumeration.Enumeration
import annotations.field.FieldInfo
import annotations.info.ColumnInfo
import annotations.mongo.MongoCollection
import annotations.query.AdminQueries
import annotations.uploads.LocalUpload
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

@MongoCollection(
    "user_files",
    "_id",
    singularName = "File",
    pluralName = "Files"
)
@AdminQueries(
    filters = ["createdAt", "title"]
)
@Serializable
data class UserFile(
    @FieldInfo("_id")
    @SerialName("_id")
//    @Serializable(CustomObjectIdSerializer::class)
    val id: String? = null,
    @Enumeration("Test", "Te")
    val title: String,
    @LocalUpload
    val file: String,
    val createdAt: Instant = Clock.System.now(),
    val isUploaded: Boolean = false,
)