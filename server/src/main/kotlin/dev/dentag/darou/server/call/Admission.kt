package dev.dentag.darou.server.call

sealed interface Admission {
    data object Unauthorized : Admission
    data object AlreadyConnected : Admission
    class Accepted(
        val client: ConnectedClient,
        val expiresAt: Long,
        val replacedConnection: PeerConnection?,
    ) : Admission
}
