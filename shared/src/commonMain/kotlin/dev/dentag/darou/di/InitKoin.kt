package dev.dentag.darou.di

import dev.dentag.darou.di.modules.authModule
import dev.dentag.darou.di.modules.networkModule
import org.koin.core.context.startKoin

fun initKoin(serverOrigin: String) {
    val network = networkModule(serverOrigin)
    val auth = authModule()

    startKoin {
        modules(network, auth)
    }
}
