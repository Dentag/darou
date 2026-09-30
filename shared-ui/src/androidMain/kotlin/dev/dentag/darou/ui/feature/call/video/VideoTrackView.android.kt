package dev.dentag.darou.ui.feature.call.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import dev.dentag.darou.call.domain.model.CallVideoTrack
import dev.dentag.darou.call.rendering.CallVideoRenderer

@Composable
internal actual fun VideoTrackView(track: CallVideoTrack, mirror: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val renderer = remember(context) { CallVideoRenderer(context) }

    AndroidView(
        factory = { renderer.view },
        modifier = modifier,
        onReset = null,
        update = { renderer.bind(track, mirror) },
        onRelease = { renderer.close() },
    )
}
