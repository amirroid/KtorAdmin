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
import ir.amirreza.services.TaskService
import ir.amirreza.services.TokenService
import ir.amirreza.services.UserService
import org.jetbrains.exposed.sql.*
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.io.File

fun Application.configureRouting() {
    val database =
        Database.connect(
            url = "jdbc:postgresql://localhost:5432/postgres",
            user = "amirreza",
            driver = "org.postgresql.Driver",
            password = "your_password",
        )
    val tasksService = TaskService(database)
    val userService = UserService(database)
    val tokenService = TokenService(database)
    routing {
        staticFiles("/$MEDIA_ROOT", File(MEDIA_PATH))
        staticFiles("/$MEDIA_ROOT", File("tasks"))
        get("/tasks") {
            call.respond(HttpStatusCode.OK, tasksService.readAll())
        }
        get("/tasks/{id}") {
            val id = call.parameters["id"]!!.toInt()
            call.respond(HttpStatusCode.OK, tasksService.read(id)!!)
        }
        get("/users") {
            call.respond(HttpStatusCode.OK, userService.readAll())
        }
        get("/users/{id}") {
            val id = call.parameters["id"]!!.toInt()
            call.respond(HttpStatusCode.OK, userService.read(id)!!)
        }
        get("/tokens") {
            call.respond(HttpStatusCode.OK, tokenService.getAll())
        }
        get("/tokens/{id}") {
            val id = call.parameters["id"]!!.toInt()
            call.respond(HttpStatusCode.OK, tokenService.getToken(id)!!)
        }
        get("/all/") {
            val tasks = tasksService.readAll()
            val users = userService.readAll()
            val tokens = tokenService.getAll()
            call.respond(
                HttpStatusCode.OK, mapOf(
                    "tasks" to tasks,
                    "users" to users,
                    "tokens" to tokens
                )
            )
        }
    }
}
