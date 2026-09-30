package dev.dentag.darou.ui.di.modules

import dev.dentag.darou.ui.feature.call.CallViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal fun callModule(): Module = module {
    viewModel {
        CallViewModel(
            acceptCallUseCase = get(),
            endCallUseCase = get(),
            inviteCallUseCase = get(),
            observeCallEventsUseCase = get(),
            observeMediaConnectionUseCase = get(),
            rejectCallUseCase = get(),
        )
    }
}
