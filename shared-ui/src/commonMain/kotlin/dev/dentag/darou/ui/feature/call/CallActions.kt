package dev.dentag.darou.ui.feature.call

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.dentag.darou.ui.feature.call.model.CallState
import dev.dentag.darou.ui.feature.call.model.CallUiState
import dev.dentag.darou.ui.resources.Res
import dev.dentag.darou.ui.resources.call_accept
import dev.dentag.darou.ui.resources.call_end
import dev.dentag.darou.ui.resources.call_invite
import dev.dentag.darou.ui.resources.call_reject
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CallActions(
    state: CallUiState,
    onInvite: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onEnd: () -> Unit,
) {
    when (state.call) {
        CallState.Idle -> Button(
            onClick = onInvite,
            enabled = state.canInvite,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.call_invite))
        }
        is CallState.Incoming -> {
            Button(
                onClick = onAccept,
                enabled = state.canAccept,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.call_accept))
            }
            OutlinedButton(
                onClick = onReject,
                enabled = state.canReject,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.call_reject))
            }
        }
        else -> OutlinedButton(
            onClick = onEnd,
            enabled = state.canEnd,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.call_end))
        }
    }
}
