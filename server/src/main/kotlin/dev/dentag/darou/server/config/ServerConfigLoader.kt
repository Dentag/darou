package dev.dentag.darou.server.config

import java.io.File

fun loadServerConfig(environment: Map<String, String> = System.getenv()): ServerConfig {
    fun required(name: String): String = requireNotNull(environment[name]) { "Missing $name" }

    val port = environment["PORT"]?.toInt() ?: 8080
    require(port in 1..65535) { "PORT must be between 1 and 65535" }
    return ServerConfig(
        publicOrigin = required("PUBLIC_ORIGIN"),
        port = port,
        loginHashes = mapOf(
            "a" to required("LOGIN_A_SHA256"),
            "b" to required("LOGIN_B_SHA256"),
        ),
        turnHost = required("TURN_HOST"),
        turnSecret = File(required("CREDENTIALS_DIRECTORY"), "turn-secret").readText().trim(),
    )
}
