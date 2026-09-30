package dev.dentag.darou.call.domain.usecase

interface RejectCallUseCase {
    suspend operator fun invoke(callId: String)
}
