package dev.dentag.darou.server.transport

import org.slf4j.Logger

/** Exception messages, causes and suppressed exceptions may contain request data. */
internal fun Logger.logSignalingFailure(connectionId: String, failure: Exception) {
    val safeFailure = Throwable(failure.javaClass.name).apply {
        stackTrace = failure.stackTrace
    }
    error("event=signaling_failed connectionId=$connectionId", safeFailure)
}
