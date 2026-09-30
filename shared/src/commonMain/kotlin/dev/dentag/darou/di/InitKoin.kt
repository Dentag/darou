package dev.dentag.darou.di

import dev.dentag.darou.di.modules.authModule
import dev.dentag.darou.di.modules.callModule
import dev.dentag.darou.di.modules.networkModule
import org.koin.core.context.startKoin

fun initKoin(serverOrigin: String) {
    val network = networkModule(serverOrigin)
    val auth = authModule()
    val call = callModule()

    startKoin {
        modules(network, auth, call)
    }
}
