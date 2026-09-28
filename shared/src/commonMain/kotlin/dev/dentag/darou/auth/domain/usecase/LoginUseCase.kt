package dev.dentag.darou.auth.domain.usecase

import dev.dentag.darou.auth.domain.model.User

interface LoginUseCase {
    suspend operator fun invoke(userId: String, code: String): User
}
