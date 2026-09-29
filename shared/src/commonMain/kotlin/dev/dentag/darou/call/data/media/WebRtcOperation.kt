package dev.dentag.darou.call.data.media

import dev.dentag.darou.call.domain.error.WebRtcException
import kotlin.coroutines.cancellation.CancellationException

internal suspend fun <T> webRtcOperation(message: String, block: suspend () -> T): T = try {
    block()
} catch (cause: CancellationException) {
    throw cause
} catch (cause: WebRtcException) {
    throw cause
} catch (cause: Exception) {
    throw WebRtcException(message, cause)
}
