package dev.dentag.darou.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun <T : HttpClientEngineConfig> createHttpClient(
    engineFactory: HttpClientEngineFactory<T>,
    engineConfig: T.() -> Unit = {},
): HttpClient = HttpClient(engineFactory) {
    engine(engineConfig)
    expectSuccess = false
    followRedirects = false

    install(HttpCookies)
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
    }
}
