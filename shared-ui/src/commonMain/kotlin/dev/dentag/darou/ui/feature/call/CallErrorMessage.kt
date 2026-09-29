package dev.dentag.darou.ui.feature.call

import androidx.compose.runtime.Composable
import dev.dentag.darou.call.domain.error.SignalingClosedException
import dev.dentag.darou.call.domain.error.SignalingNotConnectedException
import dev.dentag.darou.call.domain.error.SignalingServerException
import dev.dentag.darou.call.domain.error.WebRtcException
import dev.dentag.darou.core.domain.error.ApiException
import dev.dentag.darou.ui.resources.Res
import dev.dentag.darou.ui.resources.call_error_closed
import dev.dentag.darou.ui.resources.call_error_http
import dev.dentag.darou.ui.resources.call_error_invalid_response
import dev.dentag.darou.ui.resources.call_error_network
import dev.dentag.darou.ui.resources.call_error_not_connected
import dev.dentag.darou.ui.resources.call_error_unknown
import dev.dentag.darou.ui.resources.call_error_webrtc
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Throwable.callMessage(): String = when (this) {
    is ApiException.Network -> stringResource(Res.string.call_error_network)
    is ApiException.Http -> stringResource(Res.string.call_error_http, code)
    is ApiException.InvalidResponse -> stringResource(Res.string.call_error_invalid_response)
    is SignalingClosedException -> stringResource(Res.string.call_error_closed)
    is SignalingNotConnectedException -> stringResource(Res.string.call_error_not_connected)
    is SignalingServerException -> message ?: stringResource(Res.string.call_error_unknown)
    is WebRtcException -> stringResource(Res.string.call_error_webrtc)
    else -> stringResource(Res.string.call_error_unknown)
}
