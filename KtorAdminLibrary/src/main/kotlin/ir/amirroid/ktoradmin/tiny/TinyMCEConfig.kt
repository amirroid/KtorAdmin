package ir.amirroid.ktoradmin.tiny

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ir.amirroid.ktoradmin.models.UploadTarget

/**
 * Configuration for TinyMCE editor with basic and professional presets.
 * Developers can modify these presets as needed.
 *
 * @property height Editor height in pixels.
 * @property language UI language of the editor.
 * @property directionality Text direction ("ltr" or "rtl").
 * @property plugins Comma-separated list of enabled plugins.
 * @property toolbar Space-separated list of toolbar buttons.
 * @property branding Shows or hides TinyMCE branding.
 * @property menubar Enables or disables the menu bar.
 * @property statusbar Enables or disables the status bar.
 * @property fontFormats Defines available font families.
 * @property uploadTarget Defines the file upload target.
 */
@Serializable
data class TinyMCEConfig(
    val height: Int,
    val language: String,
    val directionality: String,
    val plugins: String,
    val toolbar: String,
    val branding: Boolean,
    val menubar: Boolean,
    val statusbar: Boolean,
    @SerialName("font_formats")
    val fontFormats: String?,
    @Transient
    val uploadTarget: UploadTarget? = null
) {
    companion object {
        /**
         * Basic configuration with minimal features.
         */
        val Basic = TinyMCEConfig(
            height = 400,
            language = "en",
            directionality = "ltr",
            plugins = "lists link image code table hr charmap preview",
            toolbar = "undo redo | styleselect | bold italic underline | alignleft aligncenter alignright | " +
                    "bullist numlist | table | link image | code preview",
            branding = false,
            menubar = true,
            statusbar = true,
            fontFormats = null,
            uploadTarget = null
        )

        /**
         * Professional configuration with all advanced features enabled.
         */
        val Professional = TinyMCEConfig(
            height = 500,
            language = "en",
            directionality = "ltr",
            plugins = "advlist autolink lists link image charmap print preview hr anchor " +
                    "pagebreak searchreplace wordcount visualblocks visualchars code fullscreen " +
                    "insertdatetime media nonbreaking save table directionality emoticons template " +
                    "paste textpattern help autosave",
            toolbar = "undo redo | styleselect | bold italic underline strikethrough | " +
                    "alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | " +
                    "link image media table | code preview fullscreen | charmap emoticons | " +
                    "forecolor backcolor | removeformat",
            branding = false,
            menubar = true,
            statusbar = true,
            fontFormats = "Arial=arial,helvetica,sans-serif; Times New Roman=times new roman,times,serif; Courier New=courier new,courier,monospace;",
            uploadTarget = null
        )
    }
}