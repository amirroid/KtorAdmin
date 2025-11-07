package ir.amirreza

import annotations.computed.Computed
import annotations.date.AutoNowDate
import annotations.display.PanelDisplayList
import annotations.enumeration.Enumeration
import annotations.field.FieldInfo
import annotations.limit.Limits
import annotations.mongo.MongoCollection
import annotations.order.DefaultOrder
import annotations.preview.Preview
import annotations.query.AdminQueries
import annotations.text_area.TextAreaField
import annotations.uploads.LocalUpload
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    collectionName = "user_files",
    primaryKey = "_id",
    singularName = "File",
    pluralName = "Files",
)
@AdminQueries(
    filters = ["createdAt", "title", "isUploaded"],
    searches = ["title"]
)
@DefaultOrder(
    "createdAt",
    "DESC"
)
@PanelDisplayList("_id", "title", "number", "createdAt", "file")
@Serializable
data class UserFile(
    @FieldInfo("_id")
    @SerialName("_id")
//    @Serializable(CustomObjectIdSerializer::class)
    val id: String? = null,
    @Enumeration("Test", "Te")
    val title: String,

    @TextAreaField
    val description: String,

    @FieldInfo(nullable = true)
    val number: Int? = 12,

    @LocalUpload
    @Limits(
        allowedMimeTypes = ["image/png", "image/jpeg", "image/jpg"]
    )
    @FieldInfo(
        nullable = true,
    )
    @Preview("image")
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
    val thumbnail: String?,
    @Computed(
        compute = "{title}.toLowerCase().replaceAll(' ', '-')"
    )
    val slug: String
)