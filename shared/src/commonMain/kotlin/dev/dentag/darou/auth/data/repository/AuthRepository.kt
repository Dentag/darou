package dev.dentag.darou.auth.data.repository

import dev.dentag.darou.auth.domain.model.User

internal interface AuthRepository {
    suspend fun login(userId: String, code: String): User
    suspend fun getSession(): User?
    suspend fun logout()
}
