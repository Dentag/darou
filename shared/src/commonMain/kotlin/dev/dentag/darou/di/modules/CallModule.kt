package dev.dentag.darou.di.modules

import dev.dentag.darou.call.data.api.IceApi
import dev.dentag.darou.call.data.api.SignalingApi
import dev.dentag.darou.call.data.media.WebRtcSessionFactory
import dev.dentag.darou.call.data.repository.CallRepository
import dev.dentag.darou.call.data.repository.CallRepositoryImpl
import dev.dentag.darou.call.data.repository.IceRepository
import dev.dentag.darou.call.data.repository.IceRepositoryImpl
import dev.dentag.darou.call.data.repository.MediaRepository
import dev.dentag.darou.call.data.repository.MediaRepositoryImpl
import dev.dentag.darou.call.data.usecase.AcceptCallUseCaseImpl
import dev.dentag.darou.call.data.usecase.EndCallUseCaseImpl
import dev.dentag.darou.call.data.usecase.GetIceServersUseCaseImpl
import dev.dentag.darou.call.data.usecase.InviteCallUseCaseImpl
import dev.dentag.darou.call.data.usecase.ObserveCallEventsUseCaseImpl
import dev.dentag.darou.call.data.usecase.ObserveMediaConnectionUseCaseImpl
import dev.dentag.darou.call.data.usecase.RejectCallUseCaseImpl
import dev.dentag.darou.call.domain.usecase.AcceptCallUseCase
import dev.dentag.darou.call.domain.usecase.EndCallUseCase
import dev.dentag.darou.call.domain.usecase.GetIceServersUseCase
import dev.dentag.darou.call.domain.usecase.InviteCallUseCase
import dev.dentag.darou.call.domain.usecase.ObserveCallEventsUseCase
import dev.dentag.darou.call.domain.usecase.ObserveMediaConnectionUseCase
import dev.dentag.darou.call.domain.usecase.RejectCallUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

internal fun callModule(): Module = module {
    single<IceApi> { IceApi(client = get(), config = get()) }
    single<SignalingApi> { SignalingApi(client = get(), config = get()) }
    single<CallRepository> { CallRepositoryImpl(api = get()) }
    single<IceRepository> { IceRepositoryImpl(api = get()) }
    single<MediaRepository> { MediaRepositoryImpl(sessionFactory = get()) }
    single<WebRtcSessionFactory> { WebRtcSessionFactory(callRepository = get(), iceRepository = get()) }

    factory<AcceptCallUseCase> { AcceptCallUseCaseImpl(get()) }
    factory<EndCallUseCase> { EndCallUseCaseImpl(get()) }
    factory<GetIceServersUseCase> { GetIceServersUseCaseImpl(get()) }
    factory<InviteCallUseCase> { InviteCallUseCaseImpl(get()) }
    factory<ObserveCallEventsUseCase> { ObserveCallEventsUseCaseImpl(get()) }
    factory<ObserveMediaConnectionUseCase> { ObserveMediaConnectionUseCaseImpl(get()) }
    factory<RejectCallUseCase> { RejectCallUseCaseImpl(get()) }
}
