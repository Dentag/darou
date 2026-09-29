package dev.dentag.darou.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createPlatformHttpClient(): HttpClient = createHttpClient(OkHttp)
