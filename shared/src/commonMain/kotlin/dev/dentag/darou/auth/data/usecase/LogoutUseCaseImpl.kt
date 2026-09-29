package dev.dentag.darou.auth.data.usecase

import dev.dentag.darou.auth.data.repository.AuthRepository
import dev.dentag.darou.auth.domain.usecase.LogoutUseCase

internal class LogoutUseCaseImpl(
    private val repository: AuthRepository,
) : LogoutUseCase {
    override suspend fun invoke() = repository.logout()
}
