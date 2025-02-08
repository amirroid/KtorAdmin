package tiny

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import models.UploadTarget

/**
 * Data class representing the configuration options for TinyMCE.
 * This class includes all standard settings except error handlers.
 *
 * @property height The height of the editor in pixels.
 * @property language The language of the editor interface.
 * @property directionality Text direction ("ltr" or "rtl").
 * @property plugins A comma-separated list of plugins to enable.
 * @property toolbar A space-separated list of toolbar buttons.
 * @property branding Whether to display the TinyMCE branding.
 * @property menubar Whether to display the menu bar.
 * @property statusbar Whether to display the status bar at the bottom.
 * @property resize Whether the editor can be resized.
 * @property contentCss Custom CSS file for styling content inside the editor.
 * @property fontFormats Custom font families for the editor.
 * @property uploadTarget The target for file uploads.
 */
@Serializable
data class TinyMCEConfig(
    val height: Int = 400,
    val language: String? = "fa",
    val directionality: String? = "rtl",
    val plugins: String? = "advlist autolink lists link image charmap print preview hr anchor " +
            "pagebreak searchreplace wordcount visualblocks visualchars code fullscreen " +
            "insertdatetime media nonbreaking table emoticons template help",
    val toolbar: String? = "undo redo | styleselect | bold italic underline | alignleft aligncenter alignright alignjustify | " +
            "bullist numlist outdent indent | link image media | preview fullscreen | help",
    val branding: Boolean = false,
    val menubar: Boolean = true,
    val statusbar: Boolean = true,
    val resize: String? = "both",
    @SerialName("content_css")
    val contentCss: String? = null,
    @SerialName("font_formats")
    val fontFormats: String? = null,

    @Transient
    val uploadTarget: UploadTarget? = null
)