package ir.amirreza

import annotations.date.AutoNowDate
import annotations.enumeration.Enumeration
import annotations.field.FieldInfo
import annotations.info.ColumnInfo
import annotations.mongo.MongoCollection
import annotations.order.DefaultOrder
import annotations.query.AdminQueries
import annotations.uploads.LocalUpload
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

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
    filters = ["createdAt", "title", "isUploaded"],
    searches = ["title"]
)
@DefaultOrder(
    "createdAt",
    "DESC"
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
    @FieldInfo(
        verboseName = "Created At"
    )
    @AutoNowDate(true)
    val createdAt: kotlinx.datetime.LocalDateTime = LocalDateTime.now().toKotlinLocalDateTime(),
    val isUploaded: Boolean = false,
    @LocalUpload
    @FieldInfo(
        nullable = true,
        readOnly = true
    )
    val thumbnail:String?
)