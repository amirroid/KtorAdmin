package ir.amirreza

import authentication.ktorAdmin
import io.ktor.server.application.*
import io.ktor.server.auth.*
import ir.amirreza.listeners.AdminListener
import models.JDBCDrivers
import org.jetbrains.exposed.sql.Database
import plugins.KtorAdmin
import providers.StorageProvider
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

const val MEDIA_ROOT = "files"
const val MEDIA_PATH = "uploads"

fun Application.configureAdmin(database: Database) {
    install(KtorAdmin) {
        jdbc(
            key = null,
            url = "jdbc:postgresql://0.0.0.0:5432/postgres",
            username = "amirreza",
            password = "your_password",
            driver = JDBCDrivers.POSTGRES
        )
        mediaPath = MEDIA_PATH
        mediaRoot = MEDIA_ROOT
        defaultAwsS3Bucket = "school-data"
        awsS3SignatureDuration = 1.minutes.toJavaDuration()
        authenticateName = "admin"
        registerEventListener(AdminListener(database))
    }
}