package dev.dentag.darou.auth.data.repository

import dev.dentag.darou.auth.data.api.AuthApi
import dev.dentag.darou.auth.data.model.LoginRequestApi
import dev.dentag.darou.auth.data.model.UserResponseApi
import dev.dentag.darou.auth.domain.model.User
import dev.dentag.darou.core.domain.error.ApiException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AuthRepositoryImpl(
    private val api: AuthApi,
) : AuthRepository {

    private val sessionMutex = Mutex()

    override suspend fun login(userId: String, code: String): User = sessionMutex.withLock {
        val user = api.login(LoginRequestApi(user = userId, code = code)).toUser()
        if (user.id != userId) {
            throw ApiException.InvalidResponse(IllegalStateException("Login response user does not match the requested user"))
        }
        user
    }

    override suspend fun getSession(): User? = sessionMutex.withLock {
        try {
            api.getSession().toUser()
        } catch (cause: ApiException.Http) {
            if (cause.code == 401) null else throw cause
        }
    }

    override suspend fun logout(): Unit = sessionMutex.withLock {
        api.logout()
    }

    private fun UserResponseApi.toUser(): User {
        if (user.isBlank()) {
            throw ApiException.InvalidResponse(IllegalStateException("User response must contain a non-blank user identifier"))
        }
        return User(id = user)
    }
}
