package com.augmentalis.netavanue.peer

import com.augmentalis.netavanue.ice.candidate.IceCandidate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleSdpTest {

    @Test
    fun `SDP encode-decode roundtrip preserves credentials`() {
        val original = SimpleSdp(
            sessionId = "1234567890",
            iceUfrag = "abcd",
            icePwd = "secretpassword1234567890",
            candidates = listOf(
                IceCandidate.host("192.168.1.5", 54321),
            ),
            isOffer = true,
        )
        val encoded = original.encode()
        val decoded = SimpleSdp.decode(encoded)

        assertEquals("1234567890", decoded.sessionId)
        assertEquals("abcd", decoded.iceUfrag)
        assertEquals("secretpassword1234567890", decoded.icePwd)
        assertTrue(decoded.isOffer)
        assertEquals(1, decoded.candidates.size)
    }

    @Test
    fun `SDP contains required lines`() {
        val sdp = SimpleSdp(
            sessionId = "99",
            iceUfrag = "test",
            icePwd = "pwd",
            candidates = emptyList(),
            isOffer = true,
        )
        val encoded = sdp.encode()
        assertTrue(encoded.contains("v=0"))
        assertTrue(encoded.contains("a=ice-ufrag:test"))
        assertTrue(encoded.contains("a=ice-pwd:pwd"))
        assertTrue(encoded.contains("m=application"))
        assertTrue(encoded.contains("webrtc-datachannel"))
    }

    @Test
    fun `SDP candidate parsing handles host type`() {
        val candidate = IceCandidate.host("10.0.0.5", 12345)
        val sdp = SimpleSdp(
            sessionId = "1",
            iceUfrag = "u",
            icePwd = "p",
            candidates = listOf(candidate),
            isOffer = false,
        )
        val decoded = SimpleSdp.decode(sdp.encode())
        assertEquals(1, decoded.candidates.size)
        assertEquals("10.0.0.5", decoded.candidates[0].address)
        assertEquals(12345, decoded.candidates[0].port)
    }

    @Test
    fun `SDP answer has setup active`() {
        val sdp = SimpleSdp("1", "u", "p", emptyList(), isOffer = false)
        assertTrue(sdp.encode().contains("a=setup:active"))
    }

    @Test
    fun `SDP offer has setup actpass`() {
        val sdp = SimpleSdp("1", "u", "p", emptyList(), isOffer = true)
        assertTrue(sdp.encode().contains("a=setup:actpass"))
    }
}
