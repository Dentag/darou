package dev.dentag.darou.server.signaling

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignalingCodecTest {
    @Test
    fun `malformed and incomplete media messages are ignored`() {
        listOf(
            "not json", "[]", "null", "{}",
            """{"type":"offer","callId":"call","revision":1,"data":{"type":"answer","sdp":"test"}}""",
            """{"type":"offer","callId":"call","revision":1,"data":{"type":"offer"}}""",
            """{"type":"ice","callId":"call","revision":{},"data":{}}""",
        ).forEach { assertNull(SignalingCodec.decode(it)) }
    }

    @Test
    fun `browser numeric revision and opaque ICE data are preserved`() {
        val message = assertIs<ClientMessage.Media>(SignalingCodec.decode(
            """{"type":"ice","callId":"call","revision":2,"data":{"candidate":"synthetic"}}""",
        ))
        assertEquals(2, message.revision)
        assertEquals(MediaSignal.ICE, message.signal)
        assertEquals("synthetic", message.data["candidate"]?.jsonPrimitive?.content)
    }

    @Test
    fun `ready omits absent call and negotiation keeps a numeric revision`() {
        val ready = Json.parseToJsonElement(SignalingCodec.encode(ServerMessage.Ready("a", null, false))).jsonObject
        assertTrue("callId" !in ready && "accepted" !in ready)
        val negotiation = Json.parseToJsonElement(SignalingCodec.encode(ServerMessage.Negotiate("call", "a", 2))).jsonObject
        assertEquals("negotiate", negotiation["type"]?.jsonPrimitive?.content)
        assertEquals(false, negotiation["revision"]?.jsonPrimitive?.isString)
    }
}
