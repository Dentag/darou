package dev.dentag.darou.call.data.mapper

import dev.dentag.darou.call.data.model.IceCandidateApi
import dev.dentag.darou.call.domain.model.IceCandidate

internal fun IceCandidate.toApi(): IceCandidateApi = IceCandidateApi(
    candidate = candidate,
    sdpMid = sdpMid,
    sdpMLineIndex = sdpMLineIndex,
    usernameFragment = usernameFragment,
)
