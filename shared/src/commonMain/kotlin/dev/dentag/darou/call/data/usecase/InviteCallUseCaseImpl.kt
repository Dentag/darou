package dev.dentag.darou.call.data.usecase

import dev.dentag.darou.call.data.repository.CallRepository
import dev.dentag.darou.call.domain.usecase.InviteCallUseCase

internal class InviteCallUseCaseImpl(
    private val repository: CallRepository,
) : InviteCallUseCase {
    override suspend fun invoke(): Unit = repository.invite()
}
