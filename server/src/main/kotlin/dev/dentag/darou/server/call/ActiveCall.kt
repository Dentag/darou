package dev.dentag.darou.server.call

import dev.dentag.darou.server.signaling.MediaSignal

/** One call's transitions. Accessed exclusively under CallCoordinator's lock. */
internal class ActiveCall(
    val id: String,
    val caller: String,
    val owners: Map<String, ClientIdentity>,
) {
    var accepted: Boolean = false
        private set
    var revision: Int = 0
        private set
    private var negotiatedAt = 0L
    private var negotiation = NegotiationState.AWAITING_OFFER

    fun accept(user: String): Boolean {
        if (user == caller || accepted) return false
        accepted = true
        return true
    }

    fun canReject(user: String): Boolean = user != caller && !accepted

    fun beginNegotiation(now: Long, force: Boolean): Boolean {
        if (!accepted || (!force && now - negotiatedAt < RESTART_INTERVAL_MS)) return false
        revision++
        negotiatedAt = now
        negotiation = NegotiationState.AWAITING_OFFER
        return true
    }

    fun allowMedia(user: String, signal: MediaSignal, messageRevision: Int): Boolean {
        if (!accepted || messageRevision != revision) return false
        when (signal) {
            MediaSignal.OFFER -> {
                if (user != caller || negotiation != NegotiationState.AWAITING_OFFER) return false
                negotiation = NegotiationState.AWAITING_ANSWER
            }
            MediaSignal.ANSWER -> {
                if (user == caller || negotiation != NegotiationState.AWAITING_ANSWER) return false
                negotiation = NegotiationState.COMPLETE
            }
            MediaSignal.ICE -> Unit
        }
        return true
    }

    private companion object {
        const val RESTART_INTERVAL_MS = 5_000L
    }
}
