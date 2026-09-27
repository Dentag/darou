package dev.dentag.darou.server.transport

import dev.dentag.darou.server.auth.AuthService
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import kotlinx.serialization.json.JsonObject

internal fun ApplicationCall.sessionToken(): String = request.cookies["session"].orEmpty()

internal fun ApplicationCall.hasOrigin(origin: String): Boolean = request.headers["Origin"] == origin

internal fun ApplicationCall.setSessionCookie(token: String, origin: String, clear: Boolean = false) {
    response.cookies.append(
        Cookie(
            name = "session",
            value = token,
            path = "/",
            maxAge = if (clear) 0 else AuthService.SESSION_SECONDS,
            secure = origin.startsWith("https:"),
            httpOnly = true,
            extensions = mapOf("SameSite" to "Strict"),
        ),
    )
}

internal suspend fun ApplicationCall.respondJson(value: JsonObject) {
    respondText(value.toString(), ContentType.Application.Json)
}
