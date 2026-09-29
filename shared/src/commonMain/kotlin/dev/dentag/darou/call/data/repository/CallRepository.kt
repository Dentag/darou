package dev.dentag.darou.call.data.repository

import dev.dentag.darou.call.domain.model.CallEvent
import dev.dentag.darou.call.domain.model.IceCandidate
import kotlinx.coroutines.flow.Flow

internal interface CallRepository {
    fun observeEvents(): Flow<CallEvent>
    suspend fun invite()
    suspend fun accept(callId: String)
    suspend fun reject(callId: String)
    suspend fun end(callId: String)
    suspend fun sendOffer(callId: String, revision: Int, sdp: String)
    suspend fun sendAnswer(callId: String, revision: Int, sdp: String)
    suspend fun sendIceCandidate(callId: String, revision: Int, candidate: IceCandidate)
}
