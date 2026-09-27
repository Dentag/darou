package dev.dentag.darou.server.transport

import dev.dentag.darou.server.auth.AuthService
import dev.dentag.darou.server.ice.TurnCredentialsService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

fun Route.iceRoutes(auth: AuthService, turn: TurnCredentialsService) {
    get("/api/ice") {
        val session = auth.authenticate(call.sessionToken())
        if (session == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return@get
        }
        val config = turn.issue(session.user)
        call.respondJson(buildJsonObject {
            putJsonArray("iceServers") {
                add(buildJsonObject { put("urls", config.stunUrl) })
                add(buildJsonObject {
                    putJsonArray("urls") { config.turnUrls.forEach { add(it) } }
                    put("username", config.username)
                    put("credential", config.credential)
                })
            }
        })
    }
}
