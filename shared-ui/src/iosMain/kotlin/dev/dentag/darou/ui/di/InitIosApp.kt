package dev.dentag.darou.ui.di

import dev.dentag.darou.di.initKoin

fun initIosApp(serverOrigin: String) {
    initKoin(serverOrigin)
    initUiKoin()
}
