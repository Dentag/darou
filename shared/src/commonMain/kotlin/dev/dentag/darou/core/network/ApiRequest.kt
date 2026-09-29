package dev.dentag.darou.core.network

import dev.dentag.darou.core.domain.error.ApiException
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException

internal suspend fun <T> executeApiRequest(
    request: suspend () -> HttpResponse,
    decode: suspend (HttpResponse) -> T,
): T = try {
    decodeResponse(request(), decode)
} catch (cause: Exception) {
    when (cause) {
        is CancellationException -> throw cause
        is IOException, is HttpRequestTimeoutException -> throw ApiException.Network(cause)
        else -> throw cause
    }
}

private suspend fun <T> decodeResponse(
    response: HttpResponse,
    decode: suspend (HttpResponse) -> T,
): T {
    val code = response.status.value
    if (code !in 200..299) {
        throw ApiException.Http(code)
    }

    return try {
        decode(response)
    } catch (cause: Exception) {
        when (cause) {
            is SerializationException,
            is ContentConvertException,
            is NoTransformationFoundException -> throw ApiException.InvalidResponse(cause)
            else -> throw cause
        }
    }
}
