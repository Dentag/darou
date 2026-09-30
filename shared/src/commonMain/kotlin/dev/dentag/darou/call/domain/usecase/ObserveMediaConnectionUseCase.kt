package dev.dentag.darou.call.domain.usecase

import dev.dentag.darou.call.domain.model.CallEvent
import dev.dentag.darou.call.domain.model.MediaConnectionEvent
import dev.dentag.darou.call.domain.model.MediaSessionConfig
import kotlinx.coroutines.flow.Flow

interface ObserveMediaConnectionUseCase {
    operator fun invoke(config: MediaSessionConfig, signals: Flow<CallEvent>): Flow<MediaConnectionEvent>
}
