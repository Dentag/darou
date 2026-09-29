package dev.dentag.darou.call.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface ServerMessageApi {

    @Serializable
    @SerialName("pong")
    data object PongApi : ServerMessageApi

    @Serializable
    @SerialName("ready")
    data class ReadyApi(
        val user: String,
        val callId: String? = null,
        val accepted: Boolean = false,
    ) : ServerMessageApi

    @Serializable
    @SerialName("presence")
    data class PresenceApi(
        val online: Boolean,
    ) : ServerMessageApi

    @Serializable
    @SerialName("ringing")
    data class RingingApi(
        val callId: String,
    ) : ServerMessageApi

    @Serializable
    @SerialName("incoming")
    data class IncomingApi(
        val callId: String,
    ) : ServerMessageApi

    @Serializable
    @SerialName("negotiate")
    data class NegotiateApi(
        val callId: String,
        val caller: String,
        val revision: Int,
    ) : ServerMessageApi

    @Serializable
    @SerialName("reconnecting")
    data class ReconnectingApi(
        val callId: String,
    ) : ServerMessageApi

    @Serializable
    @SerialName("ended")
    data class EndedApi(
        val callId: String,
        val reason: String,
    ) : ServerMessageApi

    @Serializable
    @SerialName("error")
    data class ErrorApi(
        val message: String,
    ) : ServerMessageApi

    @Serializable
    @SerialName("offer")
    data class OfferApi(
        val callId: String,
        val revision: Int,
        val data: SessionDescriptionApi,
    ) : ServerMessageApi

    @Serializable
    @SerialName("answer")
    data class AnswerApi(
        val callId: String,
        val revision: Int,
        val data: SessionDescriptionApi,
    ) : ServerMessageApi

    @Serializable
    @SerialName("ice")
    data class IceApi(
        val callId: String,
        val revision: Int,
        val data: IceCandidateApi,
    ) : ServerMessageApi
}
