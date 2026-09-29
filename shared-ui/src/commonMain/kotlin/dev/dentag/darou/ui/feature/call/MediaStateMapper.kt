package dev.dentag.darou.ui.feature.call

import dev.dentag.darou.call.domain.model.MediaConnectionState
import dev.dentag.darou.ui.feature.call.model.CallState

internal fun MediaConnectionState.toCallState(callId: String): CallState = when (this) {
    MediaConnectionState.NEW, MediaConnectionState.CONNECTING -> CallState.Connecting(callId)
    MediaConnectionState.CONNECTED -> CallState.Connected(callId)
    MediaConnectionState.DISCONNECTED, MediaConnectionState.CLOSED -> CallState.Disconnected(callId)
    MediaConnectionState.FAILED -> CallState.Failed(callId)
}
