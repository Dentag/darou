package dev.dentag.darou.call.data.usecase

import dev.dentag.darou.call.data.repository.CallRepository
import dev.dentag.darou.call.domain.usecase.RejectCallUseCase

internal class RejectCallUseCaseImpl(
    private val repository: CallRepository,
) : RejectCallUseCase {
    override suspend fun invoke(callId: String): Unit = repository.reject(callId)
}
