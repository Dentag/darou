package dev.dentag.darou.server.transport

import dev.dentag.darou.server.call.PeerConnection
import dev.dentag.darou.server.signaling.ServerMessage
import dev.dentag.darou.server.signaling.SignalingCodec
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close

internal class WebSocketPeer(private val socket: DefaultWebSocketServerSession) : PeerConnection {
    override fun enqueue(message: ServerMessage) {
        socket.outgoing.trySend(Frame.Text(SignalingCodec.encode(message)))
    }

    override suspend fun close(reason: String) {
        socket.close(CloseReason(CloseReason.Codes.NORMAL, reason))
    }
}
