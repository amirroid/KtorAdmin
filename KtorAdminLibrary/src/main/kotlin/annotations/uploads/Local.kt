package annotations.uploads

import models.FileDeleteStrategy

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class LocalUpload(
    val path: String = "",
    val deleteStrategy: FileDeleteStrategy = FileDeleteStrategy.INHERIT
)