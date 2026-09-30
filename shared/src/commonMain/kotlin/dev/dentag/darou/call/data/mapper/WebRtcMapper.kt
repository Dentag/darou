package dev.dentag.darou.call.data.mapper

import com.shepeliev.webrtckmp.PeerConnectionState
import dev.dentag.darou.call.domain.model.IceCandidate
import dev.dentag.darou.call.domain.model.IceServer
import dev.dentag.darou.call.domain.model.MediaConnectionState
import com.shepeliev.webrtckmp.IceCandidate as RtcIceCandidate
import com.shepeliev.webrtckmp.IceServer as RtcIceServer

internal fun IceServer.toWebRtc(): RtcIceServer = RtcIceServer(
    urls = urls,
    username = username.orEmpty(),
    password = credential.orEmpty(),
)

internal fun IceCandidate.toWebRtc(): RtcIceCandidate = RtcIceCandidate(
    sdpMid = sdpMid.orEmpty(),
    sdpMLineIndex = sdpMLineIndex ?: -1,
    candidate = candidate,
)

internal fun RtcIceCandidate.toDomain(): IceCandidate = IceCandidate(
    candidate = candidate,
    sdpMid = sdpMid,
    sdpMLineIndex = sdpMLineIndex,
    usernameFragment = null,
)

internal fun PeerConnectionState.toDomain(): MediaConnectionState = when (this) {
    PeerConnectionState.New -> MediaConnectionState.NEW
    PeerConnectionState.Connecting -> MediaConnectionState.CONNECTING
    PeerConnectionState.Connected -> MediaConnectionState.CONNECTED
    PeerConnectionState.Disconnected -> MediaConnectionState.DISCONNECTED
    PeerConnectionState.Failed -> MediaConnectionState.FAILED
    PeerConnectionState.Closed -> MediaConnectionState.CLOSED
}
