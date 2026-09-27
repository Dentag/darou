package dev.dentag.darou.server.signaling

enum class MediaSignal(val wireName: String) {
    OFFER("offer"),
    ANSWER("answer"),
    ICE("ice"),
}
