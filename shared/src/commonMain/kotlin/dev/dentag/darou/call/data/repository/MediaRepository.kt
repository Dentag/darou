package dev.dentag.darou.call.data.repository

import dev.dentag.darou.call.domain.model.CallEvent
import dev.dentag.darou.call.domain.model.MediaConnectionEvent
import dev.dentag.darou.call.domain.model.MediaSessionConfig
import kotlinx.coroutines.flow.Flow

internal interface MediaRepository {
    fun observeConnection(config: MediaSessionConfig, signals: Flow<CallEvent>): Flow<MediaConnectionEvent>
}
