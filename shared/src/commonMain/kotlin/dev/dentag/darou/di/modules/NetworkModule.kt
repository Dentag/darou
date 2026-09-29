package dev.dentag.darou.di.modules

import dev.dentag.darou.core.network.createPlatformHttpClient
import dev.dentag.darou.core.network.config.ServerConfig
import dev.dentag.darou.core.network.config.ServerConfigValidator
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

internal fun networkModule(serverOrigin: String): Module {
    val config = ServerConfig(origin = serverOrigin.trimEnd('/'))
    ServerConfigValidator.validate(config)

    return module {
        single<ServerConfig> { config }
        single<HttpClient> { createPlatformHttpClient() } onClose { it?.close() }
    }
}
