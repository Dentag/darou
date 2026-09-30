package dev.dentag.darou.ui.feature.call.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.dentag.darou.call.domain.model.CallVideoTrack

@Composable
internal expect fun VideoTrackView(track: CallVideoTrack, mirror: Boolean, modifier: Modifier)
