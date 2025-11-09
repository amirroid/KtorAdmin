package ir.amirreza

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


fun Application.module() {
    val database =
        Database.connect(
            url = "jdbc:postgresql://localhost:5432/postgres",
            user = "amirreza",
            driver = "org.postgresql.Driver",
            password = "your_password",
        )
    configureSecurity()
    configureSerialization()
    configureTemplating()
    configureRouting(database)
    configureAdmin(database)
    monitor.subscribe(ApplicationStopping) {
        HibernateUtil.closeSession()
    }
}
