package dev.dentag.darou.call.data.media

import com.shepeliev.webrtckmp.IceTransportPolicy
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.RtcConfiguration
import dev.dentag.darou.call.data.mapper.toWebRtc
import dev.dentag.darou.call.data.repository.CallRepository
import dev.dentag.darou.call.data.repository.IceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class WebRtcSessionFactory(
    private val callRepository: CallRepository,
    private val iceRepository: IceRepository,
) {

    suspend fun withSession(
        callId: String,
        revision: Int,
        isCaller: Boolean,
        relayOnly: Boolean = false,
        block: suspend CoroutineScope.(WebRtcSession) -> Unit,
    ): Unit = withContext(Dispatchers.Main) {
        val configuration = RtcConfiguration(
            iceServers = iceRepository.getServers().map { it.toWebRtc() },
            iceTransportPolicy = if (relayOnly) IceTransportPolicy.Relay else IceTransportPolicy.All,
        )
        val peer = webRtcOperation("Could not create WebRTC connection") { PeerConnection(configuration) }
        val localMedia = LocalCallMedia()
        try {
            localMedia.start(peer)
            coroutineScope {
                val session = WebRtcSession(peer, callRepository, callId, revision, isCaller, localMedia.videoTrack)
                val candidatesJob = launch(start = CoroutineStart.UNDISPATCHED) { session.sendLocalCandidates() }
                try {
                    block(session)
                } finally {
                    candidatesJob.cancel()
                    session.detachVideoRenderers()
                }
            }
        } finally {
            try {
                peer.close()
            } finally {
                localMedia.close()
            }
        }
    }
}
