package dev.dentag.darou.ui.feature.call.model

internal sealed interface CallState {

    val callId: String?

    data object Idle : CallState {
        override val callId: String? = null
    }

    data class Restoring(override val callId: String) : CallState

    data class Outgoing(override val callId: String) : CallState

    data class Incoming(override val callId: String) : CallState

    data class Connecting(override val callId: String) : CallState

    data class Connected(override val callId: String) : CallState

    data class Disconnected(override val callId: String) : CallState

    data class Failed(override val callId: String) : CallState

    data class Reconnecting(override val callId: String) : CallState
}
