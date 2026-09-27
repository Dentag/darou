package dev.dentag.darou.server.transport

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.webRoutes() {
    get("/health") { call.respondText("ok") }
    referenceAsset("/", "index.html", ContentType.Text.Html)
    for (resource in listOf(
        "app.js", "api-client.js", "call-controller.js", "call-view.js", "diagnostics.js",
        "local-media.js", "signaling-client.js", "peer-session.js", "media-stats.js",
    )) {
        referenceAsset("/$resource", resource, ContentType.Application.JavaScript)
    }
    referenceAsset("/style.css", "style.css", ContentType.Text.CSS)
}

private fun Route.referenceAsset(path: String, resource: String, type: ContentType) {
    val content = requireNotNull(ReferenceAssets::class.java.getResource("/web/$resource")) {
        "Missing browser resource: $resource"
    }.readText()
    get(path) { call.respondText(content, type) }
}

private object ReferenceAssets
