package ir.amirreza

import io.ktor.server.application.*
import ir.amirreza.action.MyCustomAction
import ir.amirreza.dashboard.CustomDashboard
import ir.amirreza.listeners.AdminListener
import mapper.KtorAdminValueMapper
import models.JDBCDrivers
import models.UploadTarget
import models.forms.LoginFiled
import mongo.MongoCredential
import mongo.MongoServerAddress
import org.jetbrains.exposed.sql.Database
import plugins.KtorAdmin
import tiny.TinyMCEConfig
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.times
import kotlin.time.toJavaDuration

const val MEDIA_ROOT = "files"
const val MEDIA_PATH = "uploads"

fun Application.configureAdmin(database: Database) {
    install(KtorAdmin) {
        jdbc(
            key = null,
            url = "jdbc:postgresql://localhost:5432/postgres",
            username = "amirreza",
            password = "your_password",
            driver = JDBCDrivers.POSTGRES
        )
        mongo(
            "0@localhost",
            MongoServerAddress("localhost", 27017),
            MongoCredential(
                "amirreza", "admin", "your_password"
            ),
        )
        mediaPath = MEDIA_PATH
        mediaRoot = MEDIA_ROOT
        adminDashboard = CustomDashboard()
        defaultAwsS3Bucket = "school-data"
        awsS3SignatureDuration = 1.minutes.toJavaDuration()
        authenticateName = "admin"
        loginFields = adminLoginFields
        csrfTokenExpirationTime = 1000 * 60
        registerCustomAdminActionForAll(MyCustomAction())
        registerEventListener(AdminListener(database))
        canDownloadDataAsCsv = true
        canDownloadDataAsPdf = true
        tinyMCEConfig = TinyMCEConfig.Professional.copy(uploadTarget = UploadTarget.LocalFile(null))
        registerValueMapper(
            CustomValueMapper
        )
    }
}

object CustomValueMapper : KtorAdminValueMapper {
    override fun map(value: Any?): Any? {
        return when (value) {
            is Int -> value.times(2)
            is Double -> value.times(2)
            else -> null
        }
    }

    override fun restore(value: Any?): Any? {
        return when (value) {
            is Int -> value.div(2)
            is Double -> value.div(2)
            else -> null
        }
    }

    override val key: String
        get() = "timesTo2"
}

private val adminLoginFields = listOf(
    LoginFiled(
        name = "Username",
        key = "username",
        autoComplete = "username"
    ),
    LoginFiled(
        name = "Password",
        key = "password",
        autoComplete = "current-password",
        type = "password"
    )
)