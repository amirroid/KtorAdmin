package ir.amirreza.previews

import ir.amirroid.ktoradmin.preview.KtorAdminPreview

class ImagePreview : KtorAdminPreview() {
    override val key: String
        get() = "image"

    override fun createPreview(tableName: String, name: String, value: Any?): String? {
        return value?.toString()?.let {
            expandable(name) {
                image(it)
            }
        }
    }
}