package dev.dentag.darou.ui.feature.call.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.dentag.darou.call.domain.model.CallVideoTrack
import dev.dentag.darou.ui.resources.Res
import dev.dentag.darou.ui.resources.call_video_local
import dev.dentag.darou.ui.resources.call_video_remote
import dev.dentag.darou.ui.resources.call_video_waiting
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CallVideoPanel(local: CallVideoTrack?, remote: CallVideoTrack?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.call_video_remote), style = MaterialTheme.typography.labelLarge)
        Surface(
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            if (remote != null) {
                VideoTrackView(remote, mirror = false, modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.call_video_waiting))
                }
            }
        }
        if (local != null) {
            Text(stringResource(Res.string.call_video_local), style = MaterialTheme.typography.labelLarge)
            Surface(
                modifier = Modifier.width(160.dp).aspectRatio(3f / 4f),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                VideoTrackView(local, mirror = true, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
