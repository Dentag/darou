package dev.dentag.darou.call.data.mapper

import dev.dentag.darou.call.data.model.IceServerApi
import dev.dentag.darou.call.domain.model.IceServer
import dev.dentag.darou.core.domain.error.ApiException

internal fun IceServerApi.toDomain(): IceServer {
    if (urls.isEmpty() || urls.any(String::isBlank)) {
        throw ApiException.InvalidResponse(IllegalStateException("ICE server must contain non-blank URLs"))
    }
    return IceServer(urls = urls, username = username, credential = credential)
}
