package dev.dentag.darou.server.call

import dev.dentag.darou.server.signaling.ServerMessage

/** Transport boundary. Enqueue must be non-blocking; network I/O happens outside the call lock. */
interface PeerConnection {
    fun enqueue(message: ServerMessage)
    suspend fun close(reason: String)
}

