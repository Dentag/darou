package dev.dentag.darou.ui.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.dentag.darou.auth.domain.usecase.LoginUseCase
import dev.dentag.darou.core.domain.error.ApiException
import dev.dentag.darou.ui.feature.login.model.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class LoginViewModel(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUserIdChanged(userId: String) {
        _uiState.update { state ->
            if (state.isLoading || state.authenticatedUser != null) state
            else state.copy(userId = userId, error = null)
        }
    }

    fun onCodeChanged(code: String) {
        _uiState.update { state ->
            if (state.isLoading || state.authenticatedUser != null) state
            else state.copy(code = code, error = null)
        }
    }

    fun login() {
        val state = _uiState.value
        if (!state.canSubmit) return
        if (!_uiState.compareAndSet(state, state.copy(isLoading = true, error = null))) return

        viewModelScope.launch {
            try {
                val user = loginUseCase(state.userId, state.code)
                _uiState.update { it.copy(authenticatedUser = user, code = "") }
            } catch (error: ApiException) {
                _uiState.update { it.copy(error = error) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
