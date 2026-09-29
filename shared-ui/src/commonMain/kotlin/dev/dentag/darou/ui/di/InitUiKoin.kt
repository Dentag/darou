package dev.dentag.darou.ui.di

import dev.dentag.darou.ui.di.modules.loginModule
import org.koin.core.context.loadKoinModules

fun initUiKoin() {
    val login = loginModule()
    loadKoinModules(login)
}
