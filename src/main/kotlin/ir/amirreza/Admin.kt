package ir.amirreza

import io.ktor.server.application.*
import models.JDBCDrivers
import plugins.KtorAdmin
import javax.swing.plaf.synth.Region
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

const val MEDIA_ROOT = "files"
const val MEDIA_PATH = "uploads"

fun Application.configureAdmin() {
    install(KtorAdmin) {
        jdbc(
            key = null,
            url = "jdbc:postgresql://localhost:5432/postgres",
            username = "amirreza",
            password = "your_password",
            driver = JDBCDrivers.POSTGRES
        )
        mediaPath = MEDIA_PATH
        mediaRoot = MEDIA_ROOT
        defaultAwsS3Bucket = "school-data"
        awsS3SignatureDuration = 1.minutes.toJavaDuration()
    }
}