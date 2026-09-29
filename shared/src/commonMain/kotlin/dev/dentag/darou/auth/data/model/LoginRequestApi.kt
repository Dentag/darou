package dev.dentag.darou.auth.data.model

import kotlinx.serialization.Serializable

@Serializable
internal class LoginRequestApi(val user: String, val code: String)
