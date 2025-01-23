package ir.amirreza

import io.ktor.server.application.*
import ir.amirreza.listeners.AdminListener
import models.JDBCDrivers
import models.forms.LoginFiled
import mongo.MongoServerAddress
import org.jetbrains.exposed.sql.Database
import plugins.KtorAdmin
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
        mongo(
            null,
            MongoServerAddress("localhost", 27017)
        )
        mediaPath = MEDIA_PATH
        mediaRoot = MEDIA_ROOT
        defaultAwsS3Bucket = "school-data"
        awsS3SignatureDuration = 1.minutes.toJavaDuration()
        authenticateName = "admin"
        loginFields = adminLoginFields
        registerEventListener(AdminListener(database))
    }
}

private val adminLoginFields = listOf(
    LoginFiled(
        name = "Username",
        key = "username"
    ),
    LoginFiled(
        name = "Password",
        key = "password"
    )
)