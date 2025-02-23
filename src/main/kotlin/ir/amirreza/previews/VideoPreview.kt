package ir.amirreza.previews

import preview.KtorAdminPreview

class VideoPreview : KtorAdminPreview() {
    override val key: String
        get() = "video_preview"

    override fun createPreview(tableName: String, name: String, value: Any?): String? {
        return value?.toString()?.let { video ->
            expandable(title = "Video preview") {
                video(src = video)
            }
        }
    }
}