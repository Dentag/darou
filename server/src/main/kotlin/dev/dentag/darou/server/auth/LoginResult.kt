package dev.dentag.darou.server.auth

sealed interface LoginResult {
    class Granted(val token: String) : LoginResult
    data object InvalidCredentials : LoginResult
    data object RateLimited : LoginResult
}
