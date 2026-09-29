package dev.dentag.darou.call.data.media

import com.shepeliev.webrtckmp.VideoTrack
import dev.dentag.darou.call.domain.model.CallVideoTrack

internal class WebRtcVideoTrack(val native: VideoTrack) : CallVideoTrack {

    private val renderers = mutableSetOf<() -> Unit>()
    private var released = false

    fun attach(onDetach: () -> Unit): Boolean {
        if (released) return false
        renderers.add(onDetach)
        return true
    }

    fun detach(onDetach: () -> Unit) {
        if (renderers.remove(onDetach)) onDetach()
    }

    fun release() {
        if (released) return
        released = true
        val attached = renderers.toList()
        renderers.clear()
        attached.forEach { it() }
    }
}
