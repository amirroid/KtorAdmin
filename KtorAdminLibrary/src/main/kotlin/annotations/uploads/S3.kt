package annotations.uploads

import models.FileDeleteStrategy

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class S3Upload(
    val bucket: String = "",
    val deleteStrategy: FileDeleteStrategy = FileDeleteStrategy.INHERIT
)