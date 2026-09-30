package dev.dentag.darou.call.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class IceConfigurationApi(
    val iceServers: List<IceServerApi>,
)
