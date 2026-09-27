package dev.dentag.darou.server.call

data class ClientIdentity(val token: String, val instance: String) {
    override fun toString(): String = "ClientIdentity(<redacted>)"
}
