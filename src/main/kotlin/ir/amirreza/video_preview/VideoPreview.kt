package ir.amirreza.video_preview

import preview.KtorAdminPreview

class VideoPreview : KtorAdminPreview() {
    override val key: String
        get() = "video_preview"

    override fun createPreview(tableName: String, name: String, value: Any?): String? {
        return expandable("Video preview") {
            """<video controls width="640"><source src="$value" type="video/mp4">مرورگر شما از ویدیو پشتیبانی نمی‌کند.</video>"""
        }
    }
}