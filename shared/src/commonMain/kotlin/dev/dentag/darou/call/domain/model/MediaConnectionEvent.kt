package dev.dentag.darou.call.domain.model

sealed interface MediaConnectionEvent {

    data class StateChanged(val state: MediaConnectionState) : MediaConnectionEvent

    data class VideoChanged(
        val local: CallVideoTrack?,
        val remote: CallVideoTrack?,
    ) : MediaConnectionEvent
}
