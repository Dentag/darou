package dev.dentag.darou.ui.feature.login.model

import dev.dentag.darou.auth.domain.model.User
import dev.dentag.darou.core.domain.error.ApiException

internal data class LoginUiState(
    val userId: String = "",
    val code: String = "",
    val isLoading: Boolean = false,
    val error: ApiException? = null,
    val authenticatedUser: User? = null,
) {
    val canSubmit: Boolean
        get() = !isLoading && authenticatedUser == null && userId.isNotBlank() && code.isNotBlank()
}
