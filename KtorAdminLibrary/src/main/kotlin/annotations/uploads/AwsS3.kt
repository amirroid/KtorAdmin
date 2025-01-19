package annotations.uploads

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class AwsS3Upload(val bucket: String = "")