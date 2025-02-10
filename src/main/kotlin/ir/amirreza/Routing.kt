package ir.amirreza

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ir.amirreza.services.TaskService
import ir.amirreza.services.TokenService
import ir.amirreza.services.User
import ir.amirreza.services.UserService
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.*
import plugins.KtorAdmin
import java.io.File
import java.time.ZoneId
import java.util.*

fun Application.configureRouting(database: Database) {
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
        authenticate("admin") {
            get("/tokens") {
                call.respond(HttpStatusCode.OK, tokenService.getAll())
            }
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
        post("/login") {
            val user = call.receive<User>()
            userService.getUser(user.username, user.password)?.id ?: userService.create(user)
            call.respond(user)
        }
    }
}
