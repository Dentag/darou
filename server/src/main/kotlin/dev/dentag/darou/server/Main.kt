package dev.dentag.darou.server

import dev.dentag.darou.server.config.loadServerConfig
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val config = loadServerConfig()
    embeddedServer(Netty, configure = {
        connector {
            host = "127.0.0.1"
            port = config.port
        }
        connectionGroupSize = 1
        workerGroupSize = 2
        callGroupSize = 2
    }) {
        darouModule(config)
    }.start(wait = true)
}
