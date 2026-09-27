package dev.dentag.darou.server.call

class ConnectedClient(
    val user: String,
    val identity: ClientIdentity,
    val connection: PeerConnection,
    val connectionId: String,
)
