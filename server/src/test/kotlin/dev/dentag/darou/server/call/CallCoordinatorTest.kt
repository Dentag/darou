package dev.dentag.darou.server.call

import dev.dentag.darou.server.auth.AuthService
import dev.dentag.darou.server.auth.LoginResult
import dev.dentag.darou.server.signaling.CallAction
import dev.dentag.darou.server.signaling.ClientMessage
import dev.dentag.darou.server.signaling.MediaSignal
import dev.dentag.darou.server.signaling.ServerMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CallCoordinatorTest {
    @Test
    fun `only one simultaneous invitation creates a call`() = runTest {
        val fixture = fixture()
        listOf(fixture.a, fixture.b).map { client ->
            async { fixture.calls.receive(client, ClientMessage.Invite) }
        }.awaitAll()
        val messages = fixture.pa.messages + fixture.pb.messages
        assertEquals(1, messages.filterIsInstance<ServerMessage.Incoming>().size)
        assertEquals(1, messages.filterIsInstance<ServerMessage.Ringing>().size)
        assertEquals(1, messages.filterIsInstance<ServerMessage.Error>().size)
    }

    @Test
    fun `caller cannot accept and ringing times out after 45 seconds`() = runTest {
        val fixture = fixture()
        val id = fixture.invite()
        fixture.calls.receive(fixture.a, ClientMessage.Action(CallAction.ACCEPT, id))
        assertTrue(fixture.pa.messages.none { it is ServerMessage.Negotiate })
        runCurrent()
        advanceTimeBy(44_999)
        assertTrue(fixture.pa.messages.none { it is ServerMessage.Ended })
        advanceTimeBy(1)
        runCurrent()
        assertEquals("Нет ответа", fixture.pa.messages.filterIsInstance<ServerMessage.Ended>().single().reason)
    }

    @Test
    fun `media enforces caller role ordering and current revision`() = runTest {
        val fixture = fixture()
        val id = fixture.accept()
        fixture.calls.receive(fixture.b, media(MediaSignal.OFFER, id, 1))
        fixture.calls.receive(fixture.b, media(MediaSignal.ANSWER, id, 1))
        fixture.calls.receive(fixture.a, media(MediaSignal.OFFER, "wrong-call", 1))
        fixture.calls.receive(fixture.a, media(MediaSignal.OFFER, id, 0))
        assertTrue((fixture.pa.messages + fixture.pb.messages).none { it is ServerMessage.Media })

        fixture.calls.receive(fixture.a, media(MediaSignal.OFFER, id, 1))
        fixture.calls.receive(fixture.a, media(MediaSignal.OFFER, id, 1))
        fixture.calls.receive(fixture.a, media(MediaSignal.ANSWER, id, 1))
        fixture.calls.receive(fixture.b, media(MediaSignal.ANSWER, id, 1))
        fixture.calls.receive(fixture.b, media(MediaSignal.ANSWER, id, 1))
        assertEquals(1, fixture.pa.messages.filterIsInstance<ServerMessage.Media>().size)
        assertEquals(1, fixture.pb.messages.filterIsInstance<ServerMessage.Media>().size)

        fixture.calls.receive(fixture.a, ClientMessage.Action(CallAction.RESTART, id))
        assertEquals(1, fixture.pa.messages.filterIsInstance<ServerMessage.Negotiate>().last().revision)
        advanceTimeBy(5_000)
        fixture.calls.receive(fixture.a, ClientMessage.Action(CallAction.RESTART, id))
        assertEquals(2, fixture.pa.messages.filterIsInstance<ServerMessage.Negotiate>().last().revision)
        fixture.calls.receive(fixture.a, media(MediaSignal.ICE, id, 1))
        assertEquals(1, fixture.pb.messages.filterIsInstance<ServerMessage.Media>().size)
    }

    @Test
    fun `replacement ignores messages and cleanup from old socket`() = runTest {
        val fixture = fixture()
        val id = fixture.accept()
        assertIs<Admission.AlreadyConnected>(fixture.calls.connect(ClientIdentity(fixture.a.identity.token, "other-tab"), RecordingPeer()))
        val replacement = RecordingPeer()
        val admission = assertIs<Admission.Accepted>(fixture.calls.connect(fixture.a.identity, replacement))
        assertEquals(fixture.pa, admission.replacedConnection)
        fixture.calls.receive(fixture.a, ClientMessage.Action(CallAction.END, id))
        fixture.calls.disconnect(fixture.a)
        fixture.calls.receive(admission.client, ClientMessage.Ping)
        assertTrue(replacement.messages.contains(ServerMessage.Pong))
        assertTrue(fixture.pb.messages.none { it is ServerMessage.Ended })
        assertEquals(2, replacement.messages.filterIsInstance<ServerMessage.Negotiate>().last().revision)
    }

    @Test
    fun `reconnection invalidates old timeout but later outage still expires`() = runTest {
        val fixture = fixture()
        val id = fixture.accept()
        fixture.calls.disconnect(fixture.a)
        assertIs<Admission.AlreadyConnected>(fixture.calls.connect(ClientIdentity(fixture.a.identity.token, "other-tab"), RecordingPeer()))
        runCurrent()
        advanceTimeBy(30_000)
        val replacement = RecordingPeer()
        val admission = assertIs<Admission.Accepted>(fixture.calls.connect(fixture.a.identity, replacement))
        assertEquals(id, replacement.messages.filterIsInstance<ServerMessage.Ready>().single().callId)
        assertEquals(2, replacement.messages.filterIsInstance<ServerMessage.Negotiate>().single().revision)
        fixture.calls.disconnect(admission.client)
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()
        assertTrue(fixture.pb.messages.none { it is ServerMessage.Ended })
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(id, fixture.pb.messages.filterIsInstance<ServerMessage.Ended>().single().callId)
    }

    @Test
    fun `old invitation timeout cannot end a later call`() = runTest {
        val fixture = fixture()
        val first = fixture.invite()
        runCurrent()
        advanceTimeBy(10_000)
        fixture.calls.receive(fixture.b, ClientMessage.Action(CallAction.REJECT, first))
        val second = fixture.accept()
        advanceTimeBy(40_000)
        runCurrent()
        assertTrue(fixture.pb.messages.filterIsInstance<ServerMessage.Ended>().none { it.callId == second })
    }

    @Test
    fun `logout of another session cannot end the owned call`() = runTest {
        val fixture = fixture()
        val id = fixture.accept()
        val other = assertIs<LoginResult.Granted>(fixture.auth.login("a", "test-a", "local")).token
        assertIs<Admission.AlreadyConnected>(fixture.calls.connect(ClientIdentity(other, "other-tab"), RecordingPeer()))
        assertNull(fixture.calls.logout(other))
        assertTrue(fixture.pb.messages.none { it is ServerMessage.Ended })
        assertEquals(fixture.pa, fixture.calls.logout(fixture.a.identity.token))
        assertEquals(id, fixture.pb.messages.filterIsInstance<ServerMessage.Ended>().single().callId)
        assertIs<Admission.Unauthorized>(fixture.calls.connect(fixture.a.identity, RecordingPeer()))
    }

    @Test
    fun `expired session cannot send commands or reconnect`() = runTest {
        val fixture = fixture()
        advanceTimeBy(AuthService.SESSION_SECONDS * 1000L)
        fixture.calls.receive(fixture.a, ClientMessage.Invite)
        assertTrue(fixture.pb.messages.none { it is ServerMessage.Incoming })
        assertIs<Admission.Unauthorized>(fixture.calls.connect(fixture.a.identity, RecordingPeer()))
    }

    private suspend fun TestScope.fixture(): Fixture {
        val hashes = listOf("a", "b").associateWith { user ->
            MessageDigest.getInstance("SHA-256").digest("test-$user".toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
        val auth = AuthService(hashes) { testScheduler.currentTime }
        val calls = CallCoordinator(auth, backgroundScope) { testScheduler.currentTime }
        suspend fun connect(user: String, peer: RecordingPeer): ConnectedClient {
            val token = assertIs<LoginResult.Granted>(auth.login(user, "test-$user", "local")).token
            return assertIs<Admission.Accepted>(calls.connect(ClientIdentity(token, "tab-$user"), peer)).client
        }
        val pa = RecordingPeer()
        val pb = RecordingPeer()
        return Fixture(auth, calls, connect("a", pa), connect("b", pb), pa, pb)
    }

    private fun media(signal: MediaSignal, id: String, revision: Int) = ClientMessage.Media(
        signal, id, revision, buildJsonObject {
            put("type", signal.wireName)
            put("sdp", "synthetic test description")
        },
    )

    private class RecordingPeer : PeerConnection {
        val messages = mutableListOf<ServerMessage>()
        override fun enqueue(message: ServerMessage) { messages.add(message) }
        override suspend fun close(reason: String) = Unit
    }

    private class Fixture(
        val auth: AuthService,
        val calls: CallCoordinator,
        val a: ConnectedClient,
        val b: ConnectedClient,
        val pa: RecordingPeer,
        val pb: RecordingPeer,
    ) {
        suspend fun invite(): String {
            calls.receive(a, ClientMessage.Invite)
            return pa.messages.filterIsInstance<ServerMessage.Ringing>().last().callId
        }

        suspend fun accept(): String {
            val id = invite()
            calls.receive(b, ClientMessage.Action(CallAction.ACCEPT, id))
            return id
        }
    }
}
