package dev.dentag.darou.server.transport

import dev.dentag.darou.server.auth.AuthService
import dev.dentag.darou.server.auth.LoginResult
import dev.dentag.darou.server.call.CallCoordinator
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory

fun Route.authRoutes(auth: AuthService, calls: CallCoordinator, origin: String) {
    val logger = LoggerFactory.getLogger("dev.dentag.darou.server.transport.AuthRoutes")
    post("/api/login") {
        if (!call.hasOrigin(origin)) {
            logger.info("event=login_rejected reason=origin")
            call.respond(HttpStatusCode.Forbidden)
            return@post
        }
        val input = runCatching { Json.parseToJsonElement(call.receiveText()) as? JsonObject }.getOrNull()
        val user = (input?.get("user") as? JsonPrimitive)?.contentOrNull.orEmpty()
        val code = (input?.get("code") as? JsonPrimitive)?.contentOrNull.orEmpty()
        // The loopback listener must sit behind a proxy that overwrites this header.
        val address = call.request.headers["X-Real-IP"] ?: call.request.local.remoteHost
        when (val result = auth.login(user, code, address)) {
            LoginResult.InvalidCredentials -> call.respond(HttpStatusCode.Unauthorized)
            LoginResult.RateLimited -> call.respond(HttpStatusCode.TooManyRequests)
            is LoginResult.Granted -> {
                call.setSessionCookie(result.token, origin)
                call.respondJson(buildJsonObject {})
            }
        }
    }

    get("/api/me") {
        val session = auth.authenticate(call.sessionToken())
        if (session == null) {
            call.respond(HttpStatusCode.Unauthorized)
        } else {
            call.respondJson(buildJsonObject { put("user", session.user) })
        }
    }

    post("/api/logout") {
        if (!call.hasOrigin(origin)) {
            logger.info("event=logout_rejected reason=origin")
            call.respond(HttpStatusCode.Forbidden)
            return@post
        }
        calls.logout(call.sessionToken())?.close("Signed out")
        call.setSessionCookie("", origin, clear = true)
        call.respondJson(buildJsonObject {})
    }
}
