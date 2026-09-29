package dev.dentag.darou.ui.feature.call.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import dev.dentag.darou.call.domain.model.CallVideoTrack
import dev.dentag.darou.call.rendering.CallVideoRenderer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun VideoTrackView(track: CallVideoTrack, mirror: Boolean, modifier: Modifier) {
    val renderer = remember { CallVideoRenderer() }

    UIKitView(
        factory = { renderer.view },
        modifier = modifier,
        onReset = null,
        update = { renderer.bind(track, mirror) },
        onRelease = { renderer.close() },
    )
}
