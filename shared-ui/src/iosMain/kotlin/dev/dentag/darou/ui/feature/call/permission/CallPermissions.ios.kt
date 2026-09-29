package dev.dentag.darou.ui.feature.call.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberCallPermissionsLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)

    return remember(scope) {
        {
            scope.launch {
                val cameraGranted = requestAccess(requireNotNull(AVMediaTypeVideo))
                val microphoneGranted = cameraGranted && requestAccess(requireNotNull(AVMediaTypeAudio))
                currentOnResult(cameraGranted && microphoneGranted)
            }
            Unit
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun requestAccess(mediaType: String): Boolean =
    when (AVCaptureDevice.authorizationStatusForMediaType(mediaType)) {
        AVAuthorizationStatusAuthorized -> true
        AVAuthorizationStatusNotDetermined -> suspendCancellableCoroutine { continuation ->
            AVCaptureDevice.requestAccessForMediaType(mediaType) { granted ->
                if (continuation.isActive) continuation.resume(granted)
            }
        }
        else -> false
    }
