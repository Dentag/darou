package dev.dentag.darou.server.signaling

enum class CallAction(val wireName: String) {
    ACCEPT("accept"),
    REJECT("reject"),
    END("end"),
    RESTART("restart"),
}
