package dev.dentag.darou.core.network.config

import io.ktor.http.URLProtocol
import io.ktor.http.Url

internal object ServerConfigValidator {
    fun validate(config: ServerConfig) {
        val url = Url(config.origin)

        requireHttps(url)
        requireHost(url)
        requireNoPath(url)
        requireNoQuery(url)
        requireNoFragment(url)
        requireNoCredentials(url)
    }

    private fun requireHttps(url: Url) {
        require(url.protocol == URLProtocol.HTTPS) { "Server origin must use HTTPS" }
    }

    private fun requireHost(url: Url) {
        require(url.host.isNotBlank()) { "Server origin must contain a host" }
    }

    private fun requireNoPath(url: Url) {
        require(url.encodedPath.isEmpty() || url.encodedPath == "/") {
            "Server origin must not contain a path"
        }
    }

    private fun requireNoQuery(url: Url) {
        require(url.parameters.isEmpty()) { "Server origin must not contain query parameters" }
    }

    private fun requireNoFragment(url: Url) {
        require(url.fragment.isEmpty()) { "Server origin must not contain a fragment" }
    }

    private fun requireNoCredentials(url: Url) {
        require(url.user == null && url.password == null) {
            "Server origin must not contain credentials"
        }
    }
}
