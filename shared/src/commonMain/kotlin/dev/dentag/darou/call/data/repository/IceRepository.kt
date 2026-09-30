package dev.dentag.darou.call.data.repository

import dev.dentag.darou.call.domain.model.IceServer

internal interface IceRepository {
    suspend fun getServers(): List<IceServer>
}
