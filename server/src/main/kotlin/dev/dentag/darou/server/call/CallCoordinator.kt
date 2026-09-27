package dev.dentag.darou.server.call

import dev.dentag.darou.server.auth.AuthService
import dev.dentag.darou.server.signaling.CallAction
import dev.dentag.darou.server.signaling.ClientMessage
import dev.dentag.darou.server.signaling.ServerMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Owns online clients and the single demo call, independently of Ktor.
 * Every transition, including timer callbacks, runs under mutex. Private helpers require that lock.
 * AuthService never calls back here; transport enqueue never suspends.
 */
class CallCoordinator(
    private val auth: AuthService,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val logger = LoggerFactory.getLogger(CallCoordinator::class.java)
    private val mutex = Mutex()
    private val clients = mutableMapOf<String, ConnectedClient>()
    private val reconnectTickets = mutableMapOf<String, String>()
    private var active: ActiveCall? = null

    suspend fun connect(
        identity: ClientIdentity,
        connection: PeerConnection,
        connectionId: String = UUID.randomUUID().toString(),
    ): Admission = mutex.withLock {
        val session = auth.authenticate(identity.token) ?: return@withLock Admission.Unauthorized
        val user = session.user
        val previous = clients[user]
        val owner = active?.owners?.get(user)
        if ((previous != null && previous.identity != identity) || (owner != null && owner != identity)) {
            return@withLock Admission.AlreadyConnected
        }

        val client = ConnectedClient(user, identity, connection, connectionId)
        clients[user] = client
        val reconnecting = reconnectTickets.remove(user) != null
        logger.info("event=signaling_connected connectionId={} callId={}", connectionId, active?.id ?: "none")
        if (previous != null) {
            logger.info("event=signaling_replaced connectionId={} previousConnectionId={} callId={}",
                connectionId, previous.connectionId, active?.id ?: "none")
        }
        if (reconnecting) {
            logger.info("event=signaling_reconnected connectionId={} callId={}", connectionId, active?.id ?: "none")
        }
        send(user, ServerMessage.Ready(user, active?.id, active?.accepted == true))
        broadcastPresence()
        active?.let { negotiate(it, force = true) }
        Admission.Accepted(client, session.expiresAt, previous?.connection)
    }

    suspend fun receive(client: ConnectedClient, message: ClientMessage) = mutex.withLock {
        if (clients[client.user] !== client || auth.authenticate(client.identity.token) == null) return@withLock

        when (message) {
            ClientMessage.Ping -> send(client.user, ServerMessage.Pong)
            ClientMessage.Invite -> invite(client.user)
            is ClientMessage.Action -> {
                val call = ownedCall(client, message.callId) ?: return@withLock
                handleAction(client.user, call, message.action)
            }
            is ClientMessage.Media -> {
                val call = ownedCall(client, message.callId) ?: return@withLock
                if (call.allowMedia(client.user, message.signal, message.revision)) {
                    send(peer(client.user), ServerMessage.Media(message.signal, call.id, call.revision, message.data))
                }
            }
        }
    }

    suspend fun disconnect(client: ConnectedClient) = mutex.withLock {
        // The finally block of a replaced socket must not remove its replacement.
        if (clients[client.user] !== client) return@withLock
        clients.remove(client.user)
        val call = active
        logger.info("event=signaling_disconnected connectionId={} callId={}", client.connectionId, call?.id ?: "none")
        if (call?.accepted == true && auth.authenticate(client.identity.token) != null) {
            awaitReconnect(client, call)
        } else if (call != null) {
            end("Собеседник отключился", "participant_disconnected")
        }
        broadcastPresence()
    }

    suspend fun logout(token: String): PeerConnection? = mutex.withLock {
        val session = auth.revoke(token) ?: return@withLock null
        if (active?.owners?.get(session.user)?.token == token) {
            end("Собеседник вышел из аккаунта", "logout")
        }
        clients[session.user]?.takeIf { it.identity.token == token }?.connection
    }

    private fun ownedCall(client: ConnectedClient, callId: String): ActiveCall? = active?.takeIf {
        it.id == callId && it.owners[client.user] == client.identity
    }

    private fun invite(user: String) {
        if (active != null || peer(user) !in clients) {
            logger.info("event=call_invite_rejected connectionId={} reason=busy_or_unavailable", clients[user]?.connectionId)
            send(user, ServerMessage.Error("Собеседник недоступен или уже идёт звонок"))
            return
        }
        val call = ActiveCall(
            id = UUID.randomUUID().toString(),
            caller = user,
            owners = clients.mapValues { (_, client) -> client.identity },
        )
        active = call
        logger.info("event=call_invited callId={} callerConnectionId={} calleeConnectionId={}",
            call.id, clients[user]?.connectionId, clients[peer(user)]?.connectionId)
        send(user, ServerMessage.Ringing(call.id))
        send(peer(user), ServerMessage.Incoming(call.id))
        scope.launch {
            delay(INVITATION_TIMEOUT_MS)
            mutex.withLock {
                if (active?.id == call.id && !call.accepted) end("Нет ответа", "invitation_timeout")
            }
        }
    }

    private fun handleAction(user: String, call: ActiveCall, action: CallAction) {
        when (action) {
            CallAction.ACCEPT -> if (call.accept(user)) {
                logger.info("event=call_accepted callId={} connectionId={}", call.id, clients[user]?.connectionId)
                negotiate(call, force = true)
            }
            CallAction.REJECT -> if (call.canReject(user)) {
                logger.info("event=call_rejected callId={} connectionId={}", call.id, clients[user]?.connectionId)
                end("Звонок отклонён", "rejected")
            }
            CallAction.END -> {
                logger.info("event=call_end_requested callId={} connectionId={}", call.id, clients[user]?.connectionId)
                end("Звонок завершён", "participant_ended")
            }
            CallAction.RESTART -> negotiate(call)
        }
    }

    private fun negotiate(call: ActiveCall, force: Boolean = false) {
        if (!call.owners.keys.all { it in clients }) return
        if (!call.beginNegotiation(now(), force)) return
        logger.info("event=call_negotiation_started callId={} revision={}", call.id, call.revision)
        val message = ServerMessage.Negotiate(call.id, call.caller, call.revision)
        call.owners.keys.forEach { send(it, message) }
    }

    private fun awaitReconnect(client: ConnectedClient, call: ActiveCall) {
        val user = client.user
        val ticket = UUID.randomUUID().toString()
        reconnectTickets[user] = ticket
        logger.info("event=reconnect_wait_started callId={} connectionId={} timeoutSeconds={}",
            call.id, client.connectionId, RECONNECT_TIMEOUT_MS / 1000)
        send(peer(user), ServerMessage.Reconnecting(call.id))
        scope.launch {
            delay(RECONNECT_TIMEOUT_MS)
            mutex.withLock {
                if (active?.id == call.id && reconnectTickets[user] == ticket) {
                    end("Не удалось восстановить связь с собеседником за минуту", "reconnect_timeout")
                }
            }
        }
    }

    private fun end(reason: String, logReason: String) {
        active?.let { call ->
            logger.info("event=call_ended callId={} reason={}", call.id, logReason)
            call.owners.keys.forEach { send(it, ServerMessage.Ended(call.id, reason)) }
        }
        active = null
        reconnectTickets.clear()
    }

    private fun broadcastPresence() {
        clients.keys.forEach { send(it, ServerMessage.Presence(peer(it) in clients)) }
    }

    private fun send(user: String, message: ServerMessage) {
        clients[user]?.connection?.enqueue(message)
    }

    // Fixed demo directory; accounts/contacts are a separate roadmap milestone.
    private fun peer(user: String): String = if (user == "a") "b" else "a"

    private companion object {
        const val INVITATION_TIMEOUT_MS = 45_000L
        const val RECONNECT_TIMEOUT_MS = 60_000L
    }
}
