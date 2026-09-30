package dev.dentag.darou.call.data.api

import dev.dentag.darou.call.data.model.ClientMessageApi
import dev.dentag.darou.core.domain.error.ApiException
import dev.dentag.darou.core.network.config.ServerConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

internal class SignalingApi(
    private val client: HttpClient,
    private val config: ServerConfig,
) {

    suspend fun withSession(
        instanceId: String,
        block: suspend (SignalingSession) -> Unit,
    ) {
        try {
            client.webSocket(
                request = {
                    url(config.origin)
                    url.protocol = URLProtocol.WSS
                    url.encodedPath = "/ws"
                    url.parameters.append("instance", instanceId)
                    header(HttpHeaders.Origin, config.origin)
                    expectSuccess = true
                    timeout {
                        requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                        socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                    }
                },
            ) {
                runSession(SignalingSession(this), block)
            }
        } catch (cause: Exception) {
            when (cause) {
                is CancellationException -> throw cause
                is ResponseException -> throw ApiException.Http(cause.response.status.value)
                is IOException, is HttpRequestTimeoutException -> throw ApiException.Network(cause)
                else -> throw cause
            }
        }
    }

    private suspend fun runSession(
        session: SignalingSession,
        block: suspend (SignalingSession) -> Unit,
    ) = coroutineScope {
        val heartbeat = launch {
            while (isActive) {
                delay(20.seconds)
                session.send(ClientMessageApi.PingApi)
            }
        }
        try {
            block(session)
        } finally {
            heartbeat.cancel()
        }
    }
}
