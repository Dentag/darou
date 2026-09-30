package dev.dentag.darou.ui.feature.call

import dev.dentag.darou.ui.feature.call.model.CallState
import dev.dentag.darou.ui.feature.call.model.SignalingState
import dev.dentag.darou.ui.resources.Res
import dev.dentag.darou.ui.resources.call_connected
import dev.dentag.darou.ui.resources.call_connecting
import dev.dentag.darou.ui.resources.call_disconnected
import dev.dentag.darou.ui.resources.call_failed
import dev.dentag.darou.ui.resources.call_idle
import dev.dentag.darou.ui.resources.call_incoming
import dev.dentag.darou.ui.resources.call_outgoing
import dev.dentag.darou.ui.resources.call_reconnecting
import dev.dentag.darou.ui.resources.call_restoring
import dev.dentag.darou.ui.resources.call_signaling_connected
import dev.dentag.darou.ui.resources.call_signaling_connecting
import dev.dentag.darou.ui.resources.call_signaling_disconnected
import org.jetbrains.compose.resources.StringResource

internal fun SignalingState.messageResource(): StringResource = when (this) {
    SignalingState.CONNECTING -> Res.string.call_signaling_connecting
    SignalingState.CONNECTED -> Res.string.call_signaling_connected
    SignalingState.DISCONNECTED -> Res.string.call_signaling_disconnected
}

internal fun CallState.messageResource(): StringResource = when (this) {
    CallState.Idle -> Res.string.call_idle
    is CallState.Restoring -> Res.string.call_restoring
    is CallState.Outgoing -> Res.string.call_outgoing
    is CallState.Incoming -> Res.string.call_incoming
    is CallState.Connecting -> Res.string.call_connecting
    is CallState.Connected -> Res.string.call_connected
    is CallState.Disconnected -> Res.string.call_disconnected
    is CallState.Failed -> Res.string.call_failed
    is CallState.Reconnecting -> Res.string.call_reconnecting
}
