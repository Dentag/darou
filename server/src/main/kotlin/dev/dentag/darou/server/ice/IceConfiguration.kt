package dev.dentag.darou.server.ice

// Keep credentials out of generated toString() output.
class IceConfiguration(
    val stunUrl: String,
    val turnUrls: List<String>,
    val username: String,
    val credential: String,
)
