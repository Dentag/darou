package dev.dentag.darou.ui.feature.login

import dev.dentag.darou.core.domain.error.ApiException
import dev.dentag.darou.ui.resources.Res
import dev.dentag.darou.ui.resources.login_error_credentials
import dev.dentag.darou.ui.resources.login_error_invalid_response
import dev.dentag.darou.ui.resources.login_error_network
import dev.dentag.darou.ui.resources.login_error_rate_limit
import dev.dentag.darou.ui.resources.login_error_request
import dev.dentag.darou.ui.resources.login_error_server
import org.jetbrains.compose.resources.StringResource

internal fun ApiException.messageResource(): StringResource = when (this) {
    is ApiException.Network -> Res.string.login_error_network
    is ApiException.Http -> httpMessageResource()
    is ApiException.InvalidResponse -> Res.string.login_error_invalid_response
}

private fun ApiException.Http.httpMessageResource(): StringResource = when (code) {
    401 -> Res.string.login_error_credentials
    429 -> Res.string.login_error_rate_limit
    in 500..599 -> Res.string.login_error_server
    else -> Res.string.login_error_request
}
