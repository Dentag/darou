package dev.dentag.darou.server.auth

data class Session(val user: String, val expiresAt: Long)
