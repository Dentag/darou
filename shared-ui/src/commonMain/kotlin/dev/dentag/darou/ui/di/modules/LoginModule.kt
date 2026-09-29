package dev.dentag.darou.ui.di.modules

import dev.dentag.darou.ui.feature.login.LoginViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal fun loginModule(): Module = module {
    viewModel { LoginViewModel(loginUseCase = get()) }
}
