package ir.amirreza

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.Thymeleaf
import io.ktor.server.thymeleaf.ThymeleafContent
import org.jetbrains.exposed.sql.*
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.io.File

fun Application.configureRouting() {
    routing {
        staticFiles("/$MEDIA_ROOT", File(MEDIA_PATH))
        staticFiles("/$MEDIA_ROOT", File("tasks"))
    }
}
