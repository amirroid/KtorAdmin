package ir.amirreza

import io.ktor.server.application.*
import models.JDBCDrivers
import plugins.KtorAdmin

fun Application.configureAdmin() {
    install(KtorAdmin) {
        jdbc(
            key = null,
            url = "jdbc:postgresql://localhost:5432/postgres",
            username = "amirreza",
            password = "your_password",
            driver = JDBCDrivers.POSTGRES
        )
    }
}