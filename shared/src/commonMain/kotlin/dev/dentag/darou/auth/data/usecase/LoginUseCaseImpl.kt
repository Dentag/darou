package dev.dentag.darou.auth.data.usecase

import dev.dentag.darou.auth.data.repository.AuthRepository
import dev.dentag.darou.auth.domain.model.User
import dev.dentag.darou.auth.domain.usecase.LoginUseCase

internal class LoginUseCaseImpl(
    private val repository: AuthRepository,
) : LoginUseCase {
    override suspend fun invoke(userId: String, code: String): User = repository.login(userId, code)
}
