package dev.dentag.darou.server.transport

import dev.dentag.darou.server.call.Admission
import dev.dentag.darou.server.call.CallCoordinator
import dev.dentag.darou.server.call.ClientIdentity
import dev.dentag.darou.server.signaling.SignalingCodec
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.UUID

fun Route.signalingRoutes(calls: CallCoordinator, origin: String, scope: CoroutineScope) {
    val logger = LoggerFactory.getLogger("dev.dentag.darou.server.transport.SignalingRoutes")
    webSocket("/ws") {
        val connectionId = UUID.randomUUID().toString()
        if (!call.hasOrigin(origin)) {
            logger.info("event=signaling_rejected connectionId={} reason=origin", connectionId)
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Origin"))
            return@webSocket
        }
        val instance = call.request.queryParameters["instance"]
            ?.takeIf { INSTANCE_PATTERN.matches(it) }
            ?: UUID.randomUUID().toString()
        val identity = ClientIdentity(call.sessionToken(), instance)
        val admission = calls.connect(identity, WebSocketPeer(this), connectionId)
        val accepted = when (admission) {
            Admission.Unauthorized -> {
                logger.info("event=signaling_rejected connectionId={} reason=unauthorized", connectionId)
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Login required"))
                return@webSocket
            }
            Admission.AlreadyConnected -> {
                logger.info("event=signaling_rejected connectionId={} reason=already_connected", connectionId)
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Already connected"))
                return@webSocket
            }
            is Admission.Accepted -> admission
        }

        val expiry = launch {
            delay((accepted.expiresAt - System.currentTimeMillis()).coerceAtLeast(0))
            logger.info("event=signaling_session_expired connectionId={}", connectionId)
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Session expired"))
        }
        try {
            accepted.replacedConnection?.let { previous ->
                scope.launch { previous.close("Reconnected") }
            }
            val limiter = MessageRateLimiter()
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                if (!limiter.allow()) {
                    logger.warn("event=signaling_rate_limited connectionId={}", connectionId)
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Rate limit"))
                    break
                }
                val message = SignalingCodec.decode(frame.readText()) ?: continue
                calls.receive(accepted.client, message)
            }
        } catch (cancelled: CancellationException) {
            logger.info("event=signaling_cancelled connectionId={}", connectionId)
            throw cancelled
        } catch (_: IOException) {
            logger.info("event=signaling_transport_failed connectionId={}", connectionId)
        } catch (failure: Exception) {
            logger.logSignalingFailure(connectionId, failure)
            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Internal error"))
        } finally {
            expiry.cancel()
            withContext(NonCancellable) {
                // Never log the peer-controlled close reason text.
                val code = if (closeReason.isCompleted) runCatching { closeReason.await()?.code }.getOrNull() else null
                logger.info("event=signaling_closed connectionId={} closeCode={}", connectionId, code ?: "unknown")
                calls.disconnect(accepted.client)
            }
        }
    }
}

private val INSTANCE_PATTERN = Regex("[a-zA-Z0-9-]{16,64}")

private class MessageRateLimiter {
    private var windowStartedAt = System.currentTimeMillis()
    private var count = 0

    fun allow(): Boolean {
        val now = System.currentTimeMillis()
        if (now - windowStartedAt > WINDOW_MS) {
            windowStartedAt = now
            count = 0
        }
        return ++count <= MAX_MESSAGES
    }

    private companion object {
        const val WINDOW_MS = 10_000L
        const val MAX_MESSAGES = 300
    }
}
