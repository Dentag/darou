package dev.dentag.darou.call.domain.error

class SignalingClosedException(
    val code: Int?,
    val reason: String?,
    cause: Throwable? = null,
) : Exception("Signaling connection closed: code=$code, reason=$reason", cause)
