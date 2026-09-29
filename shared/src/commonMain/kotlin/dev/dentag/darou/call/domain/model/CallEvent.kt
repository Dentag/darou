package dev.dentag.darou.call.domain.model

import dev.dentag.darou.auth.domain.model.User
import dev.dentag.darou.call.domain.error.SignalingServerException

sealed interface CallEvent {

    data class Ready(val user: User, val callId: String?, val accepted: Boolean) : CallEvent

    data class Presence(val online: Boolean) : CallEvent

    data class Ringing(val callId: String) : CallEvent

    data class Incoming(val callId: String) : CallEvent

    data class Negotiate(val callId: String, val caller: User, val revision: Int) : CallEvent

    data class Reconnecting(val callId: String) : CallEvent

    data class Ended(val callId: String, val reason: String) : CallEvent

    data class Error(val exception: SignalingServerException) : CallEvent

    data class Offer(val callId: String, val revision: Int, val sdp: String) : CallEvent

    data class Answer(val callId: String, val revision: Int, val sdp: String) : CallEvent

    data class Ice(val callId: String, val revision: Int, val candidate: IceCandidate) : CallEvent
}
