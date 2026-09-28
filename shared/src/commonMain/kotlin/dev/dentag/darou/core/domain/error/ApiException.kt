package dev.dentag.darou.core.domain.error

sealed class ApiException private constructor(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** Сбой соединения или передачи данных: нет сети, таймаут, ошибка TLS. */
    class Network(cause: Throwable) : ApiException("Network request failed", cause)

    /** Сервер ответил HTTP-статусом ошибки, например 401, 429 или 500. */
    class Http(val code: Int) : ApiException("HTTP request failed with status $code")

    /** Содержимое ответа не соответствует ожидаемому формату или структуре. */
    class InvalidResponse(cause: Throwable) : ApiException("Unexpected server response", cause)
}
