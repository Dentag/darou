package dev.dentag.darou.auth.domain.usecase

import dev.dentag.darou.auth.domain.model.User

interface GetSessionUseCase {
    suspend operator fun invoke(): User?
}
