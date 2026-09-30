package dev.dentag.darou.call.data.repository

import dev.dentag.darou.call.data.api.SignalingApi
import dev.dentag.darou.call.data.api.SignalingSession
import dev.dentag.darou.call.data.mapper.toDomain
import dev.dentag.darou.call.data.mapper.toApi
import dev.dentag.darou.call.data.model.ClientMessageApi
import dev.dentag.darou.call.data.model.ServerMessageApi
import dev.dentag.darou.call.data.model.SessionDescriptionApi
import dev.dentag.darou.call.domain.error.SignalingNotConnectedException
import dev.dentag.darou.call.domain.model.CallEvent
import dev.dentag.darou.call.domain.model.IceCandidate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlin.uuid.Uuid

internal class CallRepositoryImpl(
    private val api: SignalingApi,
) : CallRepository {

    private val instanceId = Uuid.random().toString()
    private val connectionMutex = Mutex()
    private val activeSession = MutableStateFlow<SignalingSession?>(null)

    override fun observeEvents(): Flow<CallEvent> = channelFlow {
        check(connectionMutex.tryLock()) { "Call events already have an active collector" }
        try {
            api.withSession(instanceId) { session ->
                while (true) {
                    val message = session.receive()
                    if (message is ServerMessageApi.ReadyApi) {
                        activeSession.value = session
                    }
                    message.toDomain()?.let { send(it) }
                }
            }
        } finally {
            activeSession.value = null
            connectionMutex.unlock()
        }
    }

    override suspend fun invite() = send(ClientMessageApi.InviteApi)

    override suspend fun accept(callId: String) = send(ClientMessageApi.AcceptApi(callId))

    override suspend fun reject(callId: String) = send(ClientMessageApi.RejectApi(callId))

    override suspend fun end(callId: String) = send(ClientMessageApi.EndApi(callId))

    override suspend fun sendOffer(callId: String, revision: Int, sdp: String) = send(
        ClientMessageApi.OfferApi(callId, revision, SessionDescriptionApi(type = "offer", sdp = sdp)),
    )

    override suspend fun sendAnswer(callId: String, revision: Int, sdp: String) = send(
        ClientMessageApi.AnswerApi(callId, revision, SessionDescriptionApi(type = "answer", sdp = sdp)),
    )

    override suspend fun sendIceCandidate(callId: String, revision: Int, candidate: IceCandidate) = send(
        ClientMessageApi.IceApi(callId, revision, candidate.toApi()),
    )

    private suspend fun send(message: ClientMessageApi) {
        val session = activeSession.value ?: throw SignalingNotConnectedException()
        session.send(message)
    }
}
