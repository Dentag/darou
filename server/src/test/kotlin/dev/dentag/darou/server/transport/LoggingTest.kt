package dev.dentag.darou.server.transport

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.read.ListAppender
import dev.dentag.darou.server.auth.AuthService
import dev.dentag.darou.server.auth.LoginResult
import dev.dentag.darou.server.call.Admission
import dev.dentag.darou.server.call.CallCoordinator
import dev.dentag.darou.server.call.ClientIdentity
import dev.dentag.darou.server.call.PeerConnection
import dev.dentag.darou.server.signaling.CallAction
import dev.dentag.darou.server.signaling.ClientMessage
import dev.dentag.darou.server.signaling.MediaSignal
import dev.dentag.darou.server.signaling.ServerMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoggingTest {
    @Test
    fun `call history correlates reconnections without credentials or signaling payloads`() = runTest {
        captureLogs { appender ->
            val code = "private-test-code"
            val hash = MessageDigest.getInstance("SHA-256").digest(code.toByteArray())
                .joinToString("") { "%02x".format(it) }
            val auth = AuthService(mapOf("a" to hash, "b" to hash)) { testScheduler.currentTime }
            val calls = CallCoordinator(auth, backgroundScope) { testScheduler.currentTime }
            val privateAddress = "private-test-address"
            val untrustedUser = "private-user\nevent=forged"
            repeat(10) { auth.login(untrustedUser, code, privateAddress) }
            assertIs<LoginResult.RateLimited>(auth.login("a", code, privateAddress))
            val tokenA = assertIs<LoginResult.Granted>(auth.login("a", code, "local-a")).token
            val tokenB = assertIs<LoginResult.Granted>(auth.login("b", code, "local-b")).token
            val instance = "private-client-instance"
            val identityA = ClientIdentity(tokenA, instance)
            val peerA = RecordingPeer()
            val peerB = RecordingPeer()
            val a = assertIs<Admission.Accepted>(calls.connect(identityA, peerA)).client
            val b = assertIs<Admission.Accepted>(calls.connect(ClientIdentity(tokenB, instance), peerB)).client
            calls.receive(a, ClientMessage.Invite)
            val callId = peerB.messages.filterIsInstance<ServerMessage.Incoming>().single().callId
            calls.receive(b, ClientMessage.Action(CallAction.ACCEPT, callId))
            val payload = "private-sdp-and-ice-data"
            for (signal in listOf(MediaSignal.OFFER, MediaSignal.ICE)) {
                calls.receive(a, ClientMessage.Media(signal, callId, 1, buildJsonObject { put("sdp", payload) }))
            }
            // Replacing a still-open socket must not log a false disconnection.
            val replacement = assertIs<Admission.Accepted>(calls.connect(identityA, RecordingPeer())).client
            calls.disconnect(a)
            calls.disconnect(replacement)
            runCurrent()
            advanceTimeBy(30_000)
            val reconnected = assertIs<Admission.Accepted>(calls.connect(identityA, RecordingPeer())).client
            calls.disconnect(reconnected)
            runCurrent()
            advanceTimeBy(60_000)
            runCurrent()
            auth.revoke(tokenA)

            val messages = appender.list.map { it.formattedMessage }
            assertTrue(messages.any { it.startsWith("event=login_succeeded") })
            assertTrue(messages.any { it.startsWith("event=login_rejected") })
            assertTrue(messages.any { it.startsWith("event=login_rate_limited") })
            assertTrue(messages.any { it.startsWith("event=signaling_replaced") && it.contains("previousConnectionId=${a.connectionId}") })
            assertFalse(messages.any { it.startsWith("event=signaling_disconnected connectionId=${a.connectionId} ") })
            assertTrue(messages.any { it.startsWith("event=signaling_reconnected connectionId=${reconnected.connectionId} callId=$callId") })
            assertTrue(messages.any { it.startsWith("event=call_accepted callId=$callId") })
            assertTrue(messages.any { it == "event=call_negotiation_started callId=$callId revision=3" })
            assertEquals(listOf("event=call_ended callId=$callId reason=reconnect_timeout"),
                messages.filter { it.startsWith("event=call_ended") })
            val output = messages.joinToString("\n")
            for (secret in listOf(code, hash, tokenA, tokenB, instance, payload, privateAddress, "private-user", "event=forged")) {
                assertFalse(output.contains(secret), "Sensitive input must not appear in logs")
            }
        }
    }

    @Test
    fun `unexpected failure keeps stack location without exception payloads`() {
        captureLogs { appender ->
            val failure = IllegalStateException("private-exception-message", Exception("private-cause"))
            failure.addSuppressed(Exception("private-suppressed"))
            failure.stackTrace = arrayOf(StackTraceElement("ExampleHandler", "handle", "ExampleHandler.kt", 42))
            LoggerFactory.getLogger("dev.dentag.darou.server.transport.SignalingRoutes")
                .logSignalingFailure("test-connection", failure)
            val event = appender.list.single()
            val output = event.formattedMessage + ThrowableProxyUtil.asString(event.throwableProxy)
            assertTrue(output.contains("connectionId=test-connection"))
            assertTrue(output.contains("java.lang.IllegalStateException"))
            assertTrue(output.contains("ExampleHandler.kt:42"))
            assertFalse(output.contains("private-"))
        }
    }

    private inline fun captureLogs(block: (ListAppender<ILoggingEvent>) -> Unit) {
        val logger = LoggerFactory.getLogger("dev.dentag.darou.server") as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            block(appender)
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    private class RecordingPeer : PeerConnection {
        val messages = mutableListOf<ServerMessage>()
        override fun enqueue(message: ServerMessage) { messages.add(message) }
        override suspend fun close(reason: String) = Unit
    }
}
