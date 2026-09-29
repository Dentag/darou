package dev.dentag.darou.call.data.usecase

import dev.dentag.darou.call.data.repository.MediaRepository
import dev.dentag.darou.call.domain.model.CallEvent
import dev.dentag.darou.call.domain.model.MediaConnectionEvent
import dev.dentag.darou.call.domain.model.MediaSessionConfig
import dev.dentag.darou.call.domain.usecase.ObserveMediaConnectionUseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveMediaConnectionUseCaseImpl(
    private val repository: MediaRepository,
) : ObserveMediaConnectionUseCase {
    override fun invoke(config: MediaSessionConfig, signals: Flow<CallEvent>): Flow<MediaConnectionEvent> =
        repository.observeConnection(config, signals)
}
