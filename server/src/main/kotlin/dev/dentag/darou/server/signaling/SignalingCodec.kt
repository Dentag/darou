package dev.dentag.darou.server.signaling

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/** Maps the browser wire format to typed commands; malformed messages are ignored. */
object SignalingCodec {
    fun decode(text: String): ClientMessage? {
        val message = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null
        val type = message.string("type")
        if (type == "ping") return ClientMessage.Ping
        if (type == "invite") return ClientMessage.Invite

        val callId = message.string("callId").takeIf(String::isNotEmpty) ?: return null
        CallAction.entries.find { it.wireName == type }?.let {
            return ClientMessage.Action(it, callId)
        }
        val signal = MediaSignal.entries.find { it.wireName == type } ?: return null
        val revision = message.string("revision").toIntOrNull() ?: return null
        val data = message["data"] as? JsonObject ?: return null
        if (signal != MediaSignal.ICE && (data.string("type") != type || data.string("sdp").isEmpty())) {
            return null
        }
        return ClientMessage.Media(signal, callId, revision, data)
    }

    fun encode(message: ServerMessage): String = buildJsonObject {
        when (message) {
            ServerMessage.Pong -> put("type", "pong")
            is ServerMessage.Ready -> {
                put("type", "ready")
                put("user", message.user)
                if (message.callId != null) {
                    put("callId", message.callId)
                    put("accepted", message.accepted)
                }
            }
            is ServerMessage.Presence -> {
                put("type", "presence")
                put("online", message.online)
            }
            is ServerMessage.Ringing -> {
                put("type", "ringing")
                put("callId", message.callId)
            }
            is ServerMessage.Incoming -> {
                put("type", "incoming")
                put("callId", message.callId)
            }
            is ServerMessage.Negotiate -> {
                put("type", "negotiate")
                put("callId", message.callId)
                put("caller", message.caller)
                put("revision", message.revision)
            }
            is ServerMessage.Reconnecting -> {
                put("type", "reconnecting")
                put("callId", message.callId)
            }
            is ServerMessage.Ended -> {
                put("type", "ended")
                put("callId", message.callId)
                put("reason", message.reason)
            }
            is ServerMessage.Error -> {
                put("type", "error")
                put("message", message.message)
            }
            is ServerMessage.Media -> {
                put("type", message.signal.wireName)
                put("callId", message.callId)
                put("revision", message.revision)
                put("data", message.data)
            }
        }
    }.toString()

    private fun JsonObject.string(name: String): String = (get(name) as? JsonPrimitive)?.contentOrNull.orEmpty()
}
