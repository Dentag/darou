package dev.dentag.darou.ui.feature.call.model

import dev.dentag.darou.auth.domain.model.User
import dev.dentag.darou.call.domain.model.CallVideoTrack

internal data class CallUiState(
    val user: User? = null,
    val signaling: SignalingState = SignalingState.CONNECTING,
    val isPeerOnline: Boolean = false,
    val call: CallState = CallState.Idle,
    val localVideo: CallVideoTrack? = null,
    val remoteVideo: CallVideoTrack? = null,
    val isActionPending: Boolean = false,
    val endedReason: String? = null,
    val error: Throwable? = null,
) {

    private val canSendAction: Boolean
        get() = signaling == SignalingState.CONNECTED && !isActionPending

    val canInvite: Boolean
        get() = canSendAction && isPeerOnline && call == CallState.Idle

    val canAccept: Boolean
        get() = canSendAction && call is CallState.Incoming

    val canReject: Boolean
        get() = canSendAction && call is CallState.Incoming

    val canEnd: Boolean
        get() = canSendAction && call.callId != null && call !is CallState.Incoming
}
