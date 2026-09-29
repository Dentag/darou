package dev.dentag.darou.call.rendering

import WebRTC.RTCMTLVideoView
import dev.dentag.darou.call.data.media.WebRtcVideoTrack
import dev.dentag.darou.call.domain.model.CallVideoTrack
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView
import platform.UIKit.UIViewContentMode

@OptIn(ExperimentalForeignApi::class)
class CallVideoRenderer {

    private val renderer by lazy {
        RTCMTLVideoView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
            videoContentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
        }
    }
    val view: UIView get() = renderer

    private var boundTrack: WebRtcVideoTrack? = null
    private var detach: (() -> Unit)? = null
    private var closed = false

    fun bind(track: CallVideoTrack, mirror: Boolean) {
        if (closed) return
        renderer.transform = CGAffineTransformMakeScale(if (mirror) -1.0 else 1.0, 1.0)
        val resource = track as WebRtcVideoTrack
        if (boundTrack === resource) return
        unbind()
        val onDetach = { resource.native.removeRenderer(renderer) }
        if (!resource.attach(onDetach)) return
        boundTrack = resource
        detach = onDetach
        resource.native.addRenderer(renderer)
    }

    fun close() {
        if (closed) return
        closed = true
        unbind()
    }

    private fun unbind() {
        detach?.let { boundTrack?.detach(it) }
        boundTrack = null
        detach = null
    }
}
