package dev.dentag.darou.call.rendering

import android.content.Context
import android.view.View
import com.shepeliev.webrtckmp.WebRtc
import dev.dentag.darou.call.data.media.WebRtcVideoTrack
import dev.dentag.darou.call.domain.model.CallVideoTrack
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

class CallVideoRenderer(context: Context) {

    private val renderer by lazy {
        SurfaceViewRenderer(context).apply {
            init(WebRtc.rootEglBase.eglBaseContext, null)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            setEnableHardwareScaler(true)
        }
    }
    val view: View get() = renderer

    private var boundTrack: WebRtcVideoTrack? = null
    private var detach: (() -> Unit)? = null
    private var closed = false

    fun bind(track: CallVideoTrack, mirror: Boolean) {
        if (closed) return
        renderer.setMirror(mirror)
        val resource = track as WebRtcVideoTrack
        if (boundTrack === resource) return
        unbind()
        val onDetach = {
            resource.native.removeSink(renderer)
            renderer.clearImage()
        }
        if (!resource.attach(onDetach)) return
        boundTrack = resource
        detach = onDetach
        resource.native.addSink(renderer)
    }

    fun close() {
        if (closed) return
        closed = true
        unbind()
        renderer.release()
    }

    private fun unbind() {
        detach?.let { boundTrack?.detach(it) }
        boundTrack = null
        detach = null
    }
}
