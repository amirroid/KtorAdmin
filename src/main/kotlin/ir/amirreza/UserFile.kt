package ir.amirreza

import ir.amirroid.ktoradmin.annotations.computed.Computed
import ir.amirroid.ktoradmin.annotations.date.AutoNowDate
import ir.amirroid.ktoradmin.annotations.display.PanelDisplayList
import ir.amirroid.ktoradmin.annotations.enumeration.Enumeration
import ir.amirroid.ktoradmin.annotations.field.FieldInfo
import ir.amirroid.ktoradmin.annotations.limit.Limits
import ir.amirroid.ktoradmin.annotations.mongo.MongoCollection
import ir.amirroid.ktoradmin.annotations.order.DefaultOrder
import ir.amirroid.ktoradmin.annotations.preview.Preview
import ir.amirroid.ktoradmin.annotations.query.AdminQueries
import ir.amirroid.ktoradmin.annotations.text_area.TextAreaField
import ir.amirroid.ktoradmin.annotations.uploads.LocalUpload
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