package dev.dentag.darou.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dentag.darou.ui.feature.call.CallRoute
import dev.dentag.darou.ui.feature.login.LoginScreen
import dev.dentag.darou.ui.feature.login.LoginViewModel
import dev.dentag.darou.ui.theme.DarouTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val viewModel = koinViewModel<LoginViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DarouTheme {
        if (state.authenticatedUser == null) {
            LoginScreen(
                state = state,
                onUserIdChanged = viewModel::onUserIdChanged,
                onCodeChanged = viewModel::onCodeChanged,
                onLogin = viewModel::login,
            )
        } else {
            CallRoute()
        }
    }
}
