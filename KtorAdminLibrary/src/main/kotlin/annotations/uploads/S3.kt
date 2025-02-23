package annotations.uploads

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class S3Upload(val bucket: String = "")