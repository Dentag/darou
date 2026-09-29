package dev.dentag.darou.call.data.repository

import dev.dentag.darou.call.data.media.WebRtcSessionFactory
import dev.dentag.darou.call.domain.error.WebRtcException
import dev.dentag.darou.call.domain.model.CallEvent
import dev.dentag.darou.call.domain.model.MediaConnectionEvent
import dev.dentag.darou.call.domain.model.MediaConnectionState
import dev.dentag.darou.call.domain.model.MediaSessionConfig
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

internal class MediaRepositoryImpl(
    private val sessionFactory: WebRtcSessionFactory,
) : MediaRepository {

    override fun observeConnection(
        config: MediaSessionConfig,
        signals: Flow<CallEvent>,
    ): Flow<MediaConnectionEvent> = channelFlow {
        sessionFactory.withSession(
            callId = config.callId,
            revision = config.revision,
            isCaller = config.isCaller,
            relayOnly = config.relayOnly,
        ) { session ->
            val statesJob = launch(start = CoroutineStart.UNDISPATCHED) {
                session.connectionStates.collect { state ->
                    if (state == MediaConnectionState.FAILED) {
                        throw WebRtcException("WebRTC connection failed")
                    }
                    send(MediaConnectionEvent.StateChanged(state))
                }
            }
            val videoJob = launch(start = CoroutineStart.UNDISPATCHED) {
                session.videoTracks.collect { send(it) }
            }
            try {
                if (config.isCaller) session.createOffer()
                signals.collect(session::handle)
            } finally {
                statesJob.cancel()
                videoJob.cancel()
            }
        }
    }
}
