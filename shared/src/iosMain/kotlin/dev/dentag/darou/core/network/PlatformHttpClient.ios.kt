package dev.dentag.darou.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal actual fun createPlatformHttpClient(): HttpClient = createHttpClient(Darwin) {
    configureSession {
        HTTPCookieStorage = null
        HTTPShouldSetCookies = false
        URLCache = null
    }
}
