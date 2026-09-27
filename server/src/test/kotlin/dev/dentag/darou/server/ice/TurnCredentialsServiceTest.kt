package dev.dentag.darou.server.ice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TurnCredentialsServiceTest {
    @Test
    fun `TURN REST credentials match a known HMAC vector and expire in one hour`() {
        var time = 0L
        val service = TurnCredentialsService("turn.example.invalid", "unit-test-secret") { time }
        val first = service.issue("a")
        assertEquals("3600:a", first.username)
        assertEquals("RSyWo8lAUnS95B/XLcPRS7vseX4=", first.credential)
        assertEquals(
            listOf(
                "turn:turn.example.invalid:3478?transport=udp",
                "turn:turn.example.invalid:3478?transport=tcp",
                "turns:turn.example.invalid:443?transport=tcp",
            ),
            first.turnUrls,
        )
        time = 60_000L
        val second = service.issue("a")
        assertEquals("3660:a", second.username)
        assertNotEquals(first.credential, second.credential)
        assertNotEquals(second.credential, service.issue("b").credential)
    }
}
