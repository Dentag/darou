package dev.dentag.darou.ui.di

import dev.dentag.darou.ui.di.modules.callModule
import dev.dentag.darou.ui.di.modules.loginModule
import org.koin.core.context.loadKoinModules

fun initUiKoin() {
    val call = callModule()
    val login = loginModule()
    loadKoinModules(listOf(call, login))
}
