package dev.dentag.darou.auth.data.usecase

import dev.dentag.darou.auth.data.repository.AuthRepository
import dev.dentag.darou.auth.domain.model.User
import dev.dentag.darou.auth.domain.usecase.GetSessionUseCase

internal class GetSessionUseCaseImpl(
    private val repository: AuthRepository,
) : GetSessionUseCase {
    override suspend fun invoke(): User? = repository.getSession()
}
