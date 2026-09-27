package dev.dentag.darou.server.call

internal enum class NegotiationState {
    AWAITING_OFFER,
    AWAITING_ANSWER,
    COMPLETE,
}
