package dev.dentag.darou.call.domain.usecase

interface AcceptCallUseCase {
    suspend operator fun invoke(callId: String)
}
