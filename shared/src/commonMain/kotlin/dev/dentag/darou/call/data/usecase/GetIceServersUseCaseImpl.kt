package dev.dentag.darou.call.data.usecase

import dev.dentag.darou.call.data.repository.IceRepository
import dev.dentag.darou.call.domain.model.IceServer
import dev.dentag.darou.call.domain.usecase.GetIceServersUseCase

internal class GetIceServersUseCaseImpl(
    private val repository: IceRepository,
) : GetIceServersUseCase {
    override suspend fun invoke(): List<IceServer> = repository.getServers()
}
