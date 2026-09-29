package dev.dentag.darou.call.data.usecase

import dev.dentag.darou.call.data.repository.CallRepository
import dev.dentag.darou.call.domain.model.CallEvent
import dev.dentag.darou.call.domain.usecase.ObserveCallEventsUseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveCallEventsUseCaseImpl(
    private val repository: CallRepository,
) : ObserveCallEventsUseCase {
    override fun invoke(): Flow<CallEvent> = repository.observeEvents()
}
