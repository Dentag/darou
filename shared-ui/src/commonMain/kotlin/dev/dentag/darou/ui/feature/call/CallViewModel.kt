package dev.dentag.darou.ui.feature.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.dentag.darou.call.domain.model.CallEvent
import dev.dentag.darou.call.domain.model.MediaConnectionEvent
import dev.dentag.darou.call.domain.model.MediaConnectionState
import dev.dentag.darou.call.domain.model.MediaSessionConfig
import dev.dentag.darou.call.domain.usecase.AcceptCallUseCase
import dev.dentag.darou.call.domain.usecase.EndCallUseCase
import dev.dentag.darou.call.domain.usecase.InviteCallUseCase
import dev.dentag.darou.call.domain.usecase.ObserveCallEventsUseCase
import dev.dentag.darou.call.domain.usecase.ObserveMediaConnectionUseCase
import dev.dentag.darou.call.domain.usecase.RejectCallUseCase
import dev.dentag.darou.ui.feature.call.model.CallState
import dev.dentag.darou.ui.feature.call.model.CallUiState
import dev.dentag.darou.ui.feature.call.model.SignalingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class CallViewModel(
    private val acceptCallUseCase: AcceptCallUseCase,
    private val endCallUseCase: EndCallUseCase,
    private val inviteCallUseCase: InviteCallUseCase,
    private val observeCallEventsUseCase: ObserveCallEventsUseCase,
    observeMediaConnectionUseCase: ObserveMediaConnectionUseCase,
    private val rejectCallUseCase: RejectCallUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private val media = CallMediaController(
        observeMediaConnectionUseCase = observeMediaConnectionUseCase,
        scope = viewModelScope,
        onEvent = ::onMediaEvent,
        onStopped = ::clearVideo,
        onFailure = ::onMediaFailure,
    )

    init {
        observeEvents()
    }

    fun invite() {
        val state = _uiState.value
        if (!state.canInvite) return
        sendAction(state) { inviteCallUseCase() }
    }

    fun accept() {
        val state = _uiState.value
        if (!state.canAccept) return
        val callId = state.call.callId ?: return
        sendAction(state) { acceptCallUseCase(callId) }
    }

    fun reject() {
        val state = _uiState.value
        if (!state.canReject) return
        val callId = state.call.callId ?: return
        sendAction(state) { rejectCallUseCase(callId) }
    }

    fun end() {
        val state = _uiState.value
        if (!state.canEnd) return
        val callId = state.call.callId ?: return
        sendAction(state) { endCallUseCase(callId) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            try {
                observeCallEventsUseCase().collect(::onEvent)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                error.printStackTrace()
                _uiState.update { it.copy(error = error) }
            } finally {
                withContext(NonCancellable) { media.stop() }
                _uiState.update {
                    it.copy(
                        signaling = SignalingState.DISCONNECTED,
                        isPeerOnline = false,
                        call = CallState.Idle,
                        isActionPending = false,
                    )
                }
            }
        }
    }

    private suspend fun onEvent(event: CallEvent) {
        when (event) {
            is CallEvent.Ready -> onReady(event)
            is CallEvent.Presence -> _uiState.update { it.copy(isPeerOnline = event.online) }
            is CallEvent.Ringing -> updateCall(CallState.Outgoing(event.callId))
            is CallEvent.Incoming -> updateCall(CallState.Incoming(event.callId))
            is CallEvent.Negotiate -> onNegotiate(event)
            is CallEvent.Reconnecting -> onReconnecting(event)
            is CallEvent.Ended -> onEnded(event)
            is CallEvent.Error -> {
                event.exception.printStackTrace()
                _uiState.update {
                    it.copy(isActionPending = false, error = event.exception)
                }
            }

            is CallEvent.Offer, is CallEvent.Answer, is CallEvent.Ice -> media.handle(event)
        }
    }

    private fun onReady(event: CallEvent.Ready) {
        val callId = event.callId
        val call = when {
            callId == null -> CallState.Idle
            event.accepted -> CallState.Connecting(callId)
            else -> CallState.Restoring(callId)
        }
        _uiState.update {
            it.copy(user = event.user, signaling = SignalingState.CONNECTED, call = call)
        }
    }

    private suspend fun onNegotiate(event: CallEvent.Negotiate) {
        val state = _uiState.value
        val user = state.user ?: return
        if (state.call.callId != event.callId) return
        updateCall(CallState.Connecting(event.callId))
        media.start(
            MediaSessionConfig(
                callId = event.callId,
                revision = event.revision,
                isCaller = event.caller.id == user.id,
            ),
        )
    }

    private suspend fun onReconnecting(event: CallEvent.Reconnecting) {
        if (_uiState.value.call.callId != event.callId) return
        media.stop()
        updateCall(CallState.Reconnecting(event.callId))
    }

    private fun onMediaEvent(config: MediaSessionConfig, event: MediaConnectionEvent) {
        when (event) {
            is MediaConnectionEvent.StateChanged -> onMediaState(config, event.state)
            is MediaConnectionEvent.VideoChanged -> _uiState.update {
                if (it.call.callId != config.callId) it
                else it.copy(localVideo = event.local, remoteVideo = event.remote)
            }
        }
    }

    private fun clearVideo() {
        _uiState.update { it.copy(localVideo = null, remoteVideo = null) }
    }

    private fun onMediaState(config: MediaSessionConfig, state: MediaConnectionState) {
        _uiState.update {
            if (it.call.callId != config.callId) it
            else it.copy(call = state.toCallState(config.callId))
        }
    }

    private fun onMediaFailure(config: MediaSessionConfig, error: Throwable) {
        error.printStackTrace()
        _uiState.update {
            if (it.call.callId != config.callId) it
            else it.copy(
                call = CallState.Failed(config.callId),
                isActionPending = false,
                error = error
            )
        }
    }

    private suspend fun onEnded(event: CallEvent.Ended) {
        if (_uiState.value.call.callId != event.callId) return
        media.stop()
        _uiState.update { state ->
            if (state.call.callId != event.callId) state
            else state.copy(
                call = CallState.Idle,
                isActionPending = false,
                endedReason = event.reason,
            )
        }
    }

    private fun updateCall(call: CallState) {
        _uiState.update {
            it.copy(call = call, isActionPending = false, endedReason = null, error = null)
        }
    }

    private fun sendAction(state: CallUiState, action: suspend () -> Unit) {
        val pendingState = state.copy(isActionPending = true, endedReason = null, error = null)
        if (!_uiState.compareAndSet(state, pendingState)) return

        viewModelScope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                error.printStackTrace()
                _uiState.update { it.copy(isActionPending = false, error = error) }
            }
        }
    }
}
