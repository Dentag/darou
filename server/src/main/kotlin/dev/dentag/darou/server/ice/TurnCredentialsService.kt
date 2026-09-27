package dev.dentag.darou.server.ice

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TurnCredentialsService(
    private val host: String,
    private val sharedSecret: String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun issue(user: String): IceConfiguration {
        val username = "${now() / 1000 + CREDENTIAL_LIFETIME_SECONDS}:$user"
        val mac = Mac.getInstance("HmacSHA1").apply {
            init(SecretKeySpec(sharedSecret.toByteArray(), "HmacSHA1"))
        }
        return IceConfiguration(
            stunUrl = "stun:$host:3478",
            turnUrls = listOf(
                "turn:$host:3478?transport=udp",
                "turn:$host:3478?transport=tcp",
                "turns:$host:443?transport=tcp",
            ),
            username = username,
            credential = Base64.getEncoder().encodeToString(mac.doFinal(username.toByteArray())),
        )
    }

    companion object {
        private const val CREDENTIAL_LIFETIME_SECONDS = 3600
    }
}
