package dev.dentag.darou.server

import dev.dentag.darou.server.auth.AuthService
import dev.dentag.darou.server.call.CallCoordinator
import dev.dentag.darou.server.config.ServerConfig
import dev.dentag.darou.server.ice.TurnCredentialsService
import dev.dentag.darou.server.transport.authRoutes
import dev.dentag.darou.server.transport.iceRoutes
import dev.dentag.darou.server.transport.signalingRoutes
import dev.dentag.darou.server.transport.webRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

/** Composition root: constructs services and owns their lifetime. */
fun Application.darouModule(config: ServerConfig) {
    val logger = LoggerFactory.getLogger("dev.dentag.darou.server.ApplicationModule")
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val auth = AuthService(config.loginHashes)
    val calls = CallCoordinator(auth, scope)
    val turn = TurnCredentialsService(config.turnHost, config.turnSecret)

    monitor.subscribe(ApplicationStarted) { logger.info("event=server_started") }
    monitor.subscribe(ApplicationStopped) {
        scope.cancel()
        logger.info("event=server_stopped")
    }
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 20.seconds
        maxFrameSize = 65536
    }
    routing {
        webRoutes()
        authRoutes(auth, calls, config.publicOrigin)
        iceRoutes(auth, turn)
        signalingRoutes(calls, config.publicOrigin, scope)
    }
}
