package ir.amirroid.ktoradmin.annotations.file

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class AllowedMimeTypes(vararg val types: String)