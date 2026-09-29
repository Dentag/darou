package dev.dentag.darou.call.data.api

import dev.dentag.darou.call.data.model.IceConfigurationApi
import dev.dentag.darou.core.network.config.ServerConfig
import dev.dentag.darou.core.network.executeApiRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class IceApi(
    private val client: HttpClient,
    private val config: ServerConfig,
) {

    suspend fun getConfiguration(): IceConfigurationApi = executeApiRequest(
        request = { client.get("${config.origin}/api/ice") },
        decode = { it.body<IceConfigurationApi>() },
    )
}
