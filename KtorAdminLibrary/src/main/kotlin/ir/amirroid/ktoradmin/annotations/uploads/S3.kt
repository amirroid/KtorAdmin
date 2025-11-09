package ir.amirroid.ktoradmin.annotations.uploads

import ir.amirroid.ktoradmin.models.FileDeleteStrategy

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class S3Upload(
    val bucket: String = "",
    val deleteStrategy: FileDeleteStrategy = FileDeleteStrategy.INHERIT
)