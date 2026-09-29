package dev.dentag.darou.call.data.mapper

import dev.dentag.darou.auth.domain.model.User
import dev.dentag.darou.call.data.model.IceCandidateApi
import dev.dentag.darou.call.data.model.ServerMessageApi
import dev.dentag.darou.call.domain.error.SignalingServerException
import dev.dentag.darou.call.domain.model.CallEvent
import dev.dentag.darou.call.domain.model.IceCandidate

internal fun ServerMessageApi.toDomain(): CallEvent? = when (this) {
    is ServerMessageApi.PongApi -> null
    is ServerMessageApi.ReadyApi -> CallEvent.Ready(User(user), callId, accepted)
    is ServerMessageApi.PresenceApi -> CallEvent.Presence(online)
    is ServerMessageApi.RingingApi -> CallEvent.Ringing(callId)
    is ServerMessageApi.IncomingApi -> CallEvent.Incoming(callId)
    is ServerMessageApi.NegotiateApi -> CallEvent.Negotiate(callId, User(caller), revision)
    is ServerMessageApi.ReconnectingApi -> CallEvent.Reconnecting(callId)
    is ServerMessageApi.EndedApi -> CallEvent.Ended(callId, reason)
    is ServerMessageApi.ErrorApi -> CallEvent.Error(SignalingServerException(message))
    is ServerMessageApi.OfferApi -> CallEvent.Offer(callId, revision, data.sdp)
    is ServerMessageApi.AnswerApi -> CallEvent.Answer(callId, revision, data.sdp)
    is ServerMessageApi.IceApi -> CallEvent.Ice(callId, revision, data.toDomain())
}

private fun IceCandidateApi.toDomain(): IceCandidate = IceCandidate(
    candidate = candidate,
    sdpMid = sdpMid,
    sdpMLineIndex = sdpMLineIndex,
    usernameFragment = usernameFragment,
)
