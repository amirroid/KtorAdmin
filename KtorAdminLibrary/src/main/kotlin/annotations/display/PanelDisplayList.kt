package annotations.display

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class PanelDisplayList(
    vararg val field: String
)