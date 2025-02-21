package preview

open class ExtraPreview {
    fun expandable(title: String, content: () -> String): String {
        return """
        <div class="expandable" onclick="toggleExpand(this)">
            <div class="expandable-header">
                <span class="expandable-title">${title}</span>
                <svg class="expandable-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M19.9201 8.94995L13.4001 15.47C12.6301 16.24 11.3701 16.24 10.6001 15.47L4.08008 8.94995" stroke="#292D32" stroke-width="1.5" stroke-miterlimit="10" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </div>
            <div class="expandable-content">
                ${content()}
            </div>
        </div>
    """.trimIndent()
    }
}