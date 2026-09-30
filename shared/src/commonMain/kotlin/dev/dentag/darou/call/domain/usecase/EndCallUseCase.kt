package dev.dentag.darou.call.domain.usecase

interface EndCallUseCase {
    suspend operator fun invoke(callId: String)
}
