package dev.dentag.darou.call.data.media

import com.shepeliev.webrtckmp.FacingMode
import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.videoTracks

internal class LocalCallMedia {

    private var audio: MediaStream? = null
    private var video: MediaStream? = null
    var videoTrack: WebRtcVideoTrack? = null
        private set

    suspend fun start(peer: PeerConnection) {
        check(audio == null && video == null)
        audio = webRtcOperation("Could not start microphone capture") {
            MediaDevices.getUserMedia {
                audio {
                    echoCancellation { ideal(true) }
                    noiseSuppression { ideal(true) }
                }
            }
        }
        video = webRtcOperation("Could not start camera capture") {
            MediaDevices.getUserMedia {
                video {
                    facingMode { ideal(FacingMode.User) }
                    width { ideal(1280) }
                    height { ideal(720) }
                    frameRate { ideal(30.0) }
                }
            }
        }
        videoTrack = video?.videoTracks?.firstOrNull()?.let(::WebRtcVideoTrack)
        webRtcOperation("Could not attach local media tracks") {
            listOfNotNull(audio, video).forEach { stream ->
                stream.tracks.forEach { track -> peer.addTrack(track, stream) }
            }
        }
    }

    fun close() {
        videoTrack?.release()
        videoTrack = null
        val audioStream = audio
        val videoStream = video
        audio = null
        video = null
        try {
            videoStream?.release()
        } finally {
            audioStream?.release()
        }
    }
}
