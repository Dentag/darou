package dev.dentag.darou.call.data.model

import dev.dentag.darou.call.data.serialization.IceServerUrlsSerializer
import kotlinx.serialization.Serializable

@Serializable
internal class IceServerApi(
    @Serializable(with = IceServerUrlsSerializer::class)
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null,
)
