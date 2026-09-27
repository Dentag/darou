package dev.dentag.darou.server.signaling

import kotlinx.serialization.json.JsonObject

sealed interface ServerMessage {
    data object Pong : ServerMessage
    data class Ready(val user: String, val callId: String?, val accepted: Boolean) : ServerMessage
    data class Presence(val online: Boolean) : ServerMessage
    data class Ringing(val callId: String) : ServerMessage
    data class Incoming(val callId: String) : ServerMessage
    data class Negotiate(val callId: String, val caller: String, val revision: Int) : ServerMessage
    data class Reconnecting(val callId: String) : ServerMessage
    data class Ended(val callId: String, val reason: String) : ServerMessage
    data class Error(val message: String) : ServerMessage
    data class Media(
        val signal: MediaSignal,
        val callId: String,
        val revision: Int,
        val data: JsonObject,
    ) : ServerMessage
}
