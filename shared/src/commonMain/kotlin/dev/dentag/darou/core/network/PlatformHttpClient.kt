package dev.dentag.darou.core.network

import io.ktor.client.HttpClient

internal expect fun createPlatformHttpClient(): HttpClient
