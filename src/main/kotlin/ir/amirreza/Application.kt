package ir.amirreza

import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureTemplating()
    configureRouting()
    configureAdmin()
}
