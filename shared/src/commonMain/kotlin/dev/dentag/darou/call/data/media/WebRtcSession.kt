package dev.dentag.darou.call.data.media

import com.shepeliev.webrtckmp.OfferAnswerOptions
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.SessionDescription
import com.shepeliev.webrtckmp.SessionDescriptionType
import com.shepeliev.webrtckmp.VideoTrack
import com.shepeliev.webrtckmp.onConnectionStateChange
import com.shepeliev.webrtckmp.onIceCandidate
import com.shepeliev.webrtckmp.onTrack
import dev.dentag.darou.call.data.mapper.toDomain
import dev.dentag.darou.call.data.mapper.toWebRtc
import dev.dentag.darou.call.data.repository.CallRepository
import dev.dentag.darou.call.domain.error.WebRtcException
import dev.dentag.darou.call.domain.model.CallEvent
import dev.dentag.darou.call.domain.model.IceCandidate
import dev.dentag.darou.call.domain.model.MediaConnectionEvent
import dev.dentag.darou.call.domain.model.MediaConnectionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class WebRtcSession(
    private val peer: PeerConnection,
    private val repository: CallRepository,
    private val callId: String,
    private val revision: Int,
    private val isCaller: Boolean,
    private val localVideo: WebRtcVideoTrack?,
) {

    private val negotiationMutex = Mutex()
    private val localDescriptionSent = CompletableDeferred<Unit>()
    private val pendingCandidates = ArrayDeque<IceCandidate>()
    private var remoteVideo: WebRtcVideoTrack? = null

    val videoTracks: Flow<MediaConnectionEvent.VideoChanged> = flow {
        emit(MediaConnectionEvent.VideoChanged(localVideo, remoteVideo))
        peer.onTrack.collect { event ->
            val track = event.track as? VideoTrack ?: return@collect
            remoteVideo?.release()
            remoteVideo = WebRtcVideoTrack(track)
            emit(MediaConnectionEvent.VideoChanged(localVideo, remoteVideo))
        }
    }

    fun detachVideoRenderers() {
        localVideo?.release()
        remoteVideo?.release()
        remoteVideo = null
    }

    val connectionStates: Flow<MediaConnectionState> = peer.onConnectionStateChange
        .onStart { emit(peer.connectionState) }
        .map { it.toDomain() }

    suspend fun createOffer(): Unit = negotiationMutex.withLock {
        check(isCaller && !localDescriptionSent.isCompleted)
        val description = webRtcOperation("Could not create SDP offer") {
            peer.createOffer(
                OfferAnswerOptions(
                    offerToReceiveAudio = true,
                    offerToReceiveVideo = true
                )
            )
                .also { peer.setLocalDescription(it) }
        }
        repository.sendOffer(callId, revision, description.sdp)
        localDescriptionSent.complete(Unit)
    }

    suspend fun handle(event: CallEvent): Unit = negotiationMutex.withLock {
        when (event) {
            is CallEvent.Offer -> if (matches(
                    event.callId,
                    event.revision
                ) && !isCaller
            ) acceptOffer(event.sdp)

            is CallEvent.Answer -> if (matches(
                    event.callId,
                    event.revision
                ) && isCaller
            ) acceptAnswer(event.sdp)

            is CallEvent.Ice -> if (matches(
                    event.callId,
                    event.revision
                )
            ) acceptCandidate(event.candidate)

            else -> Unit
        }
    }

    suspend fun sendLocalCandidates() {
        peer.onIceCandidate.collect { candidate ->
            localDescriptionSent.await()
            repository.sendIceCandidate(callId, revision, candidate.toDomain())
        }
    }

    private suspend fun acceptOffer(sdp: String) {
        if (peer.remoteDescription != null) return
        setRemoteDescription(SessionDescription(SessionDescriptionType.Offer, sdp))
        val description = webRtcOperation("Could not create SDP answer") {
            peer.createAnswer(OfferAnswerOptions()).also { peer.setLocalDescription(it) }
        }
        repository.sendAnswer(callId, revision, description.sdp)
        localDescriptionSent.complete(Unit)
    }

    private suspend fun acceptAnswer(sdp: String) {
        if (peer.remoteDescription != null) return
        setRemoteDescription(SessionDescription(SessionDescriptionType.Answer, sdp))
    }

    private suspend fun setRemoteDescription(description: SessionDescription) {
        webRtcOperation("Could not apply remote SDP") { peer.setRemoteDescription(description) }
        while (pendingCandidates.isNotEmpty()) {
            addCandidate(pendingCandidates.removeFirst())
        }
    }

    private suspend fun acceptCandidate(candidate: IceCandidate) {
        if (peer.remoteDescription == null) pendingCandidates.addLast(candidate)
        else addCandidate(candidate)
    }

    private suspend fun addCandidate(candidate: IceCandidate) {
        webRtcOperation("Could not apply remote ICE candidate") {
            if (!peer.addIceCandidate(candidate.toWebRtc())) {
                throw WebRtcException("WebRTC rejected the remote ICE candidate")
            }
        }
    }

    private fun matches(eventCallId: String, eventRevision: Int): Boolean =
        callId == eventCallId && revision == eventRevision
}
