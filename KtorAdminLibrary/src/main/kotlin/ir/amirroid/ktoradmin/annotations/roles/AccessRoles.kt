package ir.amirroid.ktoradmin.annotations.roles

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AccessRoles(vararg val role: String)
