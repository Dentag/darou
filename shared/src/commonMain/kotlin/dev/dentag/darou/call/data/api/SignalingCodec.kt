package dev.dentag.darou.call.data.api

import dev.dentag.darou.call.data.model.ClientMessageApi
import dev.dentag.darou.call.data.model.ServerMessageApi
import dev.dentag.darou.core.domain.error.ApiException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal object SignalingCodec {

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(message: ClientMessageApi): String = json.encodeToString<ClientMessageApi>(message)

    fun decode(text: String): ServerMessageApi = try {
        json.decodeFromString<ServerMessageApi>(text)
    } catch (cause: SerializationException) {
        throw ApiException.InvalidResponse(cause)
    }
}
