package dev.dentag.darou.call.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class IceCandidateApi(
    val candidate: String,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
    val usernameFragment: String? = null,
)
