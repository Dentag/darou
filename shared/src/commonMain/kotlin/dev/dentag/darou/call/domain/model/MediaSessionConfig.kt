package dev.dentag.darou.call.domain.model

data class MediaSessionConfig(
    val callId: String,
    val revision: Int,
    val isCaller: Boolean,
    val relayOnly: Boolean = false,
)
