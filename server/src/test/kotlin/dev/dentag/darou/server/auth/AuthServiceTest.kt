package dev.dentag.darou.server.auth

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class AuthServiceTest {
    @Test
    fun `sessions have independent tokens and expire at the boundary`() {
        var time = 0L
        val auth = AuthService(mapOf("a" to hash("test-code"))) { time }
        val first = assertIs<LoginResult.Granted>(auth.login("a", "test-code", "local"))
        val second = assertIs<LoginResult.Granted>(auth.login("a", "test-code", "local"))
        assertNotEquals(first.token, second.token)
        auth.revoke(first.token)
        assertNull(auth.authenticate(first.token))
        assertEquals("a", auth.authenticate(second.token)?.user)
        time = AuthService.SESSION_SECONDS * 1000L
        assertNull(auth.authenticate(second.token))
    }

    @Test
    fun `failed attempts block an address until the attempt window passes`() {
        var time = 0L
        val auth = AuthService(mapOf("a" to hash("test-code"))) { time }
        repeat(10) {
            assertIs<LoginResult.InvalidCredentials>(auth.login("a", "wrong", "blocked"))
        }
        assertIs<LoginResult.RateLimited>(auth.login("a", "test-code", "blocked"))
        assertIs<LoginResult.Granted>(auth.login("a", "test-code", "other"))
        time = 600_001L
        assertIs<LoginResult.Granted>(auth.login("a", "test-code", "blocked"))
        assertIs<LoginResult.InvalidCredentials>(auth.login("unknown", "test-code", "other"))
    }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
