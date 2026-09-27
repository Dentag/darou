package dev.dentag.darou.server.signaling

import kotlinx.serialization.json.JsonObject

sealed interface ClientMessage {
    data object Ping : ClientMessage
    data object Invite : ClientMessage
    data class Action(val action: CallAction, val callId: String) : ClientMessage
    data class Media(
        val signal: MediaSignal,
        val callId: String,
        val revision: Int,
        val data: JsonObject,
    ) : ClientMessage
}
