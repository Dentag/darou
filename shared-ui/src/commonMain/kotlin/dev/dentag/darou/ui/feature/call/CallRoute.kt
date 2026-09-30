package dev.dentag.darou.ui.feature.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dentag.darou.ui.feature.call.permission.CallPermissionsDialog
import dev.dentag.darou.ui.feature.call.permission.rememberCallPermissionsLauncher
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun CallRoute() {
    val viewModel = koinViewModel<CallViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPermissionsDenied by remember { mutableStateOf(false) }
    val requestPermissions = rememberCallPermissionsLauncher { granted ->
        val action = pendingAction
        pendingAction = null
        if (action != null) {
            if (granted) action() else showPermissionsDenied = true
        }
    }

    CallScreen(
        state = state,
        onInvite = {
            if (pendingAction == null && state.canInvite) {
                pendingAction = viewModel::invite
                requestPermissions()
            }
        },
        onAccept = {
            val callId = state.call.callId
            if (pendingAction == null && state.canAccept && callId != null) {
                pendingAction = {
                    if (viewModel.uiState.value.call.callId == callId) viewModel.accept()
                }
                requestPermissions()
            }
        },
        onReject = viewModel::reject,
        onEnd = viewModel::end,
        onDismissError = viewModel::dismissError,
    )

    if (showPermissionsDenied) {
        CallPermissionsDialog(onDismiss = { showPermissionsDenied = false })
    }
}
