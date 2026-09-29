package dev.dentag.darou.call.data.usecase

import dev.dentag.darou.call.data.repository.CallRepository
import dev.dentag.darou.call.domain.usecase.EndCallUseCase

internal class EndCallUseCaseImpl(
    private val repository: CallRepository,
) : EndCallUseCase {
    override suspend fun invoke(callId: String): Unit = repository.end(callId)
}
