package ir.amirroid.ktoradmin.preview

open class ExtraPreview {
    fun expandable(
        title: String,
        content: () -> String,
    ): String =
        """
        <div class="expandable" onclick="toggleExpand(this)">
            <div class="expandable-header">
                <span class="expandable-title">$title</span>
                <svg class="expandable-icon" width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M5.25 7.5L10 12.25L14.75 7.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </div>
            <div class="expandable-content">
                ${content()}
            </div>
        </div>
        """.trimIndent()

    fun video(src: String): String =
        """
        <div class="video-container">
            <div class="video-wrapper">
                <video class="video-player" src="$src" controls></video>
            </div>
        </div>
        """.trimIndent()

    fun image(src: String): String =
        """
        <div class="image-container-preview">
            <img class="image-content-preview" src="$src" alt="image">
        </div>
        """.trimIndent()
}
