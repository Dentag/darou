package dev.dentag.darou.ui.feature.call

import dev.dentag.darou.call.domain.model.CallEvent
import dev.dentag.darou.call.domain.model.MediaConnectionEvent
import dev.dentag.darou.call.domain.model.MediaSessionConfig
import dev.dentag.darou.call.domain.usecase.ObserveMediaConnectionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal class CallMediaController(
    private val observeMediaConnectionUseCase: ObserveMediaConnectionUseCase,
    private val scope: CoroutineScope,
    private val onEvent: (MediaSessionConfig, MediaConnectionEvent) -> Unit,
    private val onStopped: () -> Unit,
    private val onFailure: (MediaSessionConfig, Throwable) -> Unit,
) {

    private var activeConfig: MediaSessionConfig? = null
    private var signals: Channel<CallEvent>? = null
    private var job: Job? = null

    suspend fun start(config: MediaSessionConfig) {
        if (activeConfig == config && job?.isActive == true) return
        stop()
        val input = Channel<CallEvent>(Channel.UNLIMITED)
        activeConfig = config
        signals = input
        job = scope.launch {
            try {
                observeMediaConnectionUseCase(config, input.receiveAsFlow()).collect { event ->
                    if (activeConfig == config) onEvent(config, event)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (activeConfig == config) onFailure(config, error)
            } finally {
                input.cancel()
                if (activeConfig == config) onStopped()
            }
        }
    }

    fun handle(event: CallEvent) {
        val config = activeConfig ?: return
        if (event.matches(config)) signals?.trySend(event)
    }

    suspend fun stop() {
        val previousJob = job
        job = null
        activeConfig = null
        onStopped()
        signals?.cancel()
        signals = null
        previousJob?.cancelAndJoin()
    }

    private fun CallEvent.matches(config: MediaSessionConfig): Boolean = when (this) {
        is CallEvent.Offer -> callId == config.callId && revision == config.revision
        is CallEvent.Answer -> callId == config.callId && revision == config.revision
        is CallEvent.Ice -> callId == config.callId && revision == config.revision
        else -> false
    }
}
