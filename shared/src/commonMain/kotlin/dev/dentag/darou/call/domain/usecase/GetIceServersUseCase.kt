package dev.dentag.darou.call.domain.usecase

import dev.dentag.darou.call.domain.model.IceServer

interface GetIceServersUseCase {
    suspend operator fun invoke(): List<IceServer>
}
