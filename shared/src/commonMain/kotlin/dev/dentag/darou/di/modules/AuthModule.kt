package dev.dentag.darou.di.modules

import dev.dentag.darou.auth.data.api.AuthApi
import dev.dentag.darou.auth.data.repository.AuthRepository
import dev.dentag.darou.auth.data.repository.AuthRepositoryImpl
import dev.dentag.darou.auth.data.usecase.GetSessionUseCaseImpl
import dev.dentag.darou.auth.data.usecase.LoginUseCaseImpl
import dev.dentag.darou.auth.data.usecase.LogoutUseCaseImpl
import dev.dentag.darou.auth.domain.usecase.GetSessionUseCase
import dev.dentag.darou.auth.domain.usecase.LoginUseCase
import dev.dentag.darou.auth.domain.usecase.LogoutUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

internal fun authModule(): Module = module {
    single<AuthApi> { AuthApi(client = get(), config = get()) }
    single<AuthRepository> { AuthRepositoryImpl(api = get()) }

    factory<LoginUseCase> { LoginUseCaseImpl(get()) }
    factory<GetSessionUseCase> { GetSessionUseCaseImpl(get()) }
    factory<LogoutUseCase> { LogoutUseCaseImpl(get()) }
}
