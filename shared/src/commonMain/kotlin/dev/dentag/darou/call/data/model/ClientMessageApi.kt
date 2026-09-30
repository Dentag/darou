package dev.dentag.darou.call.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface ClientMessageApi {

    @Serializable
    @SerialName("ping")
    data object PingApi : ClientMessageApi

    @Serializable
    @SerialName("invite")
    data object InviteApi : ClientMessageApi

    @Serializable
    @SerialName("accept")
    data class AcceptApi(
        val callId: String,
    ) : ClientMessageApi

    @Serializable
    @SerialName("reject")
    data class RejectApi(
        val callId: String,
    ) : ClientMessageApi

    @Serializable
    @SerialName("end")
    data class EndApi(
        val callId: String,
    ) : ClientMessageApi

    @Serializable
    @SerialName("restart")
    data class RestartApi(
        val callId: String,
    ) : ClientMessageApi

    @Serializable
    @SerialName("offer")
    data class OfferApi(
        val callId: String,
        val revision: Int,
        val data: SessionDescriptionApi,
    ) : ClientMessageApi

    @Serializable
    @SerialName("answer")
    data class AnswerApi(
        val callId: String,
        val revision: Int,
        val data: SessionDescriptionApi,
    ) : ClientMessageApi

    @Serializable
    @SerialName("ice")
    data class IceApi(
        val callId: String,
        val revision: Int,
        val data: IceCandidateApi,
    ) : ClientMessageApi
}
