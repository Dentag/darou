package dev.dentag.darou.ui.feature.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.dentag.darou.ui.feature.call.model.CallUiState
import dev.dentag.darou.ui.feature.call.model.SignalingState
import dev.dentag.darou.ui.feature.call.video.CallVideoPanel
import dev.dentag.darou.ui.resources.Res
import dev.dentag.darou.ui.resources.call_dismiss_error
import dev.dentag.darou.ui.resources.call_ended
import dev.dentag.darou.ui.resources.call_peer_offline
import dev.dentag.darou.ui.resources.call_peer_online
import dev.dentag.darou.ui.resources.call_pending
import dev.dentag.darou.ui.resources.call_title
import dev.dentag.darou.ui.resources.call_user
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CallScreen(
    state: CallUiState,
    onInvite: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onEnd: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.safeDrawingPadding().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.call_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                state.user?.let { user ->
                    Text(stringResource(Res.string.call_user, user.id))
                }
                Text(
                    text = stringResource(state.signaling.messageResource()),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (state.signaling == SignalingState.CONNECTED) {
                    Text(
                        stringResource(
                            if (state.isPeerOnline) Res.string.call_peer_online
                            else Res.string.call_peer_offline,
                        ),
                    )
                    Text(
                        text = stringResource(state.call.messageResource()),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                if (state.localVideo != null || state.remoteVideo != null) {
                    CallVideoPanel(local = state.localVideo, remote = state.remoteVideo)
                }
                if (state.signaling == SignalingState.CONNECTING || state.isActionPending) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (state.isActionPending) {
                    Text(stringResource(Res.string.call_pending))
                }
                state.endedReason?.let { reason ->
                    Text(stringResource(Res.string.call_ended, reason))
                }
                state.error?.let { error ->
                    Text(
                        text = error.callMessage(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    TextButton(onClick = onDismissError) {
                        Text(stringResource(Res.string.call_dismiss_error))
                    }
                }
                CallActions(state, onInvite, onAccept, onReject, onEnd)
            }
        }
    }
}
