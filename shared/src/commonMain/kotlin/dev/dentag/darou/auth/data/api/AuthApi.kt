package dev.dentag.darou.auth.data.api

import dev.dentag.darou.auth.data.model.LoginRequestApi
import dev.dentag.darou.auth.data.model.UserResponseApi
import dev.dentag.darou.core.network.config.ServerConfig
import dev.dentag.darou.core.network.executeApiRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

internal class AuthApi(
    private val client: HttpClient,
    private val config: ServerConfig,
) {
    suspend fun login(request: LoginRequestApi): UserResponseApi = executeApiRequest(
        request = {
            client.post("${config.origin}/api/login") {
                header(HttpHeaders.Origin, config.origin)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        },
        decode = { it.body<UserResponseApi>() },
    )

    suspend fun getSession(): UserResponseApi = executeApiRequest(
        request = { client.get("${config.origin}/api/me") },
        decode = { it.body<UserResponseApi>() },
    )

    suspend fun logout(): Unit = executeApiRequest(
        request = {
            client.post("${config.origin}/api/logout") {
                header(HttpHeaders.Origin, config.origin)
            }
        },
        decode = { Unit },
    )
}
