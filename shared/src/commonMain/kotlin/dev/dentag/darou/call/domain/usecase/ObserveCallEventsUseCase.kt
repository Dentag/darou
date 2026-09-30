package dev.dentag.darou.call.domain.usecase

import dev.dentag.darou.call.domain.model.CallEvent
import kotlinx.coroutines.flow.Flow

interface ObserveCallEventsUseCase {
    operator fun invoke(): Flow<CallEvent>
}
