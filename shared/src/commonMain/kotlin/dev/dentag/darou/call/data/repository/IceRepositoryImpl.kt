package dev.dentag.darou.call.data.repository

import dev.dentag.darou.call.data.api.IceApi
import dev.dentag.darou.call.data.mapper.toDomain
import dev.dentag.darou.call.domain.model.IceServer
import dev.dentag.darou.core.domain.error.ApiException

internal class IceRepositoryImpl(
    private val api: IceApi,
) : IceRepository {

    override suspend fun getServers(): List<IceServer> {
        val servers = api.getConfiguration().iceServers
        if (servers.isEmpty()) {
            throw ApiException.InvalidResponse(IllegalStateException("ICE configuration must contain servers"))
        }
        return servers.map { it.toDomain() }
    }
}
