package ir.amirreza

import annotations.actions.AdminActions
import annotations.computed.Computed
import annotations.confirmation.Confirmation
import annotations.date.AutoNowDate
import annotations.display.PanelDisplayList
import annotations.enumeration.Enumeration
import annotations.field.FieldInfo
import annotations.info.ColumnInfo
import annotations.limit.Limits
import annotations.mongo.MongoCollection
import annotations.order.DefaultOrder
import annotations.preview.Preview
import annotations.query.AdminQueries
import annotations.rich_editor.RichEditor
import annotations.text_area.TextAreaField
import annotations.type.OverrideFieldType
import annotations.uploads.LocalUpload
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import models.actions.Action
import models.types.FieldType
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
@PanelDisplayList("_id", "title")
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

    @LocalUpload
    @Limits(
        allowedMimeTypes = ["image/png", "image/jpeg", "image/jpg"]
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