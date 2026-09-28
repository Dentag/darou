package dev.dentag.darou.server.auth

import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** In-memory demo authentication. All access to sessions and attempts is synchronized. */
class AuthService(
    private val loginHashes: Map<String, String>,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    private val sessions = mutableMapOf<String, Session>()
    private val failures = mutableMapOf<String, MutableList<Long>>()
    private val random = SecureRandom()

    @Synchronized
    fun login(user: String, code: String, address: String): LoginResult {
        val time = now()
        failures.entries.removeIf { (_, attempts) -> attempts.all { time - it > ATTEMPT_WINDOW_MS } }
        val attempts = failures.getOrPut(address) { mutableListOf() }
        attempts.removeIf { time - it > ATTEMPT_WINDOW_MS }
        if (attempts.size >= MAX_ATTEMPTS || failures.size > MAX_ADDRESSES) {
            logger.warn("event=login_rate_limited reason=attempt_limit")
            return LoginResult.RateLimited
        }

        val expectedHash = loginHashes[user]
        val valid = expectedHash != null && MessageDigest.isEqual(
            expectedHash.toByteArray(),
            sha256(code).toByteArray(),
        )
        if (!valid) {
            attempts.add(time)
            logger.info("event=login_rejected reason=invalid_credentials")
            return LoginResult.InvalidCredentials
        }

        sessions.entries.removeIf { (_, session) -> session.expiresAt <= time }
        if (sessions.size >= MAX_SESSIONS) {
            logger.warn("event=login_rate_limited reason=session_limit")
            return LoginResult.RateLimited
        }

        val bytes = ByteArray(32).also(random::nextBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        sessions[token] = Session(user, time + SESSION_SECONDS * 1000L)
        logger.info("event=login_succeeded")
        return LoginResult.Granted(token = token, user = user)
    }

    @Synchronized
    fun authenticate(token: String): Session? = sessions[token]?.takeIf { it.expiresAt > now() }

    @Synchronized
    fun revoke(token: String): Session? = sessions.remove(token).also {
        if (it != null) logger.info("event=logout_succeeded")
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    companion object {
        const val SESSION_SECONDS = 8 * 60 * 60
        private const val ATTEMPT_WINDOW_MS = 10 * 60 * 1000L
        private const val MAX_ATTEMPTS = 10
        private const val MAX_ADDRESSES = 4096
        private const val MAX_SESSIONS = 128
    }
}
