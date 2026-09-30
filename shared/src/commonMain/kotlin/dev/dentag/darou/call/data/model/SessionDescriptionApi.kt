package dev.dentag.darou.call.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class SessionDescriptionApi(
    val type: String,
    val sdp: String,
)
