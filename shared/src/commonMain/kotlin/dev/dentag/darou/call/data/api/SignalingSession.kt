package dev.dentag.darou.call.data.api

import dev.dentag.darou.call.data.model.ClientMessageApi
import dev.dentag.darou.call.data.model.ServerMessageApi
import dev.dentag.darou.call.domain.error.SignalingClosedException
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class SignalingSession(
    private val socket: DefaultClientWebSocketSession,
) {
    suspend fun send(message: ClientMessageApi) {
        val text = SignalingCodec.encode(message)
        try {
            socket.send(Frame.Text(text))
        } catch (cause: ClosedSendChannelException) {
            currentCoroutineContext().ensureActive()
            throw closedException(cause)
        }
    }

    suspend fun receive(): ServerMessageApi {
        while (true) {
            val result = socket.incoming.receiveCatching()
            result.exceptionOrNull()?.let { throw it }
            val frame = result.getOrNull() ?: throw closedException()
            if (frame is Frame.Text) {
                return SignalingCodec.decode(frame.readText())
            }
        }
    }

    private suspend fun closedException(cause: Throwable? = null): SignalingClosedException {
        val reason = socket.closeReason.await()
        return SignalingClosedException(
            code = reason?.code?.toInt(),
            reason = reason?.message,
            cause = cause,
        )
    }
}
