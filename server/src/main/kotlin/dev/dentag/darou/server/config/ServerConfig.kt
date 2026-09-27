package dev.dentag.darou.server.config

// Not a data class: generated toString() would expose credentials.
class ServerConfig(
    val publicOrigin: String,
    val port: Int,
    val loginHashes: Map<String, String>,
    val turnHost: String,
    val turnSecret: String,
)
