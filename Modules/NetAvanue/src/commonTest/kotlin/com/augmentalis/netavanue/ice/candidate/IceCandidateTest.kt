package com.augmentalis.netavanue.ice.candidate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IceCandidateTest {

    @Test
    fun `host candidate has highest type preference`() {
        val host = IceCandidate.host("192.168.1.5", 54321)
        val srflx = IceCandidate.serverReflexive("203.0.113.50", 41234, "192.168.1.5", 54321, "stun.example.com:3478")

        assertTrue(host.priority > srflx.priority, "Host priority (${host.priority}) should be > srflx (${srflx.priority})")
    }

    @Test
    fun `candidate priority follows RFC formula`() {
        // priority = (2^24 * typePreference) + (2^8 * localPreference) + (2^0 * (256 - componentId))
        val priority = IceCandidate.calculatePriority(
            typePreference = 126,
            localPreference = 65535,
            componentId = 1,
        )
        val expected = (126L shl 24) + (65535L shl 8) + (256L - 1)
        assertEquals(expected, priority)
    }

    @Test
    fun `SDP encoding format is correct`() {
        val candidate = IceCandidate.host("192.168.1.5", 54321)
        val sdp = candidate.toSdpAttribute()
        assertTrue(sdp.startsWith("candidate:"))
        assertTrue(sdp.contains("udp"))
        assertTrue(sdp.contains("192.168.1.5"))
        assertTrue(sdp.contains("54321"))
        assertTrue(sdp.contains("typ host"), "SDP should contain 'typ host': $sdp")
    }

    @Test
    fun `server-reflexive SDP includes related address`() {
        val candidate = IceCandidate.serverReflexive(
            "203.0.113.50", 41234,
            "192.168.1.5", 54321,
            "stun.example.com:3478",
        )
        val sdp = candidate.toSdpAttribute()
        assertTrue(sdp.contains("raddr 192.168.1.5"))
        assertTrue(sdp.contains("rport 54321"))
    }

    @Test
    fun `candidate pair formation pairs same component`() {
        val local = listOf(
            IceCandidate.host("192.168.1.5", 54321),
            IceCandidate.host("10.0.0.5", 54322),
        )
        val remote = listOf(
            IceCandidate.host("192.168.1.10", 12345),
        )
        val pairs = IceCandidatePair.formPairs(local, remote, isControlling = true)
        assertEquals(2, pairs.size)
        // Sorted by priority descending
        assertTrue(pairs[0].priority >= pairs[1].priority)
    }

    @Test
    fun `candidate pair priority is deterministic`() {
        val local = IceCandidate.host("192.168.1.5", 54321)
        val remote = IceCandidate.host("192.168.1.10", 12345)

        val pair1 = IceCandidatePair(local, remote, isControlling = true)
        val pair2 = IceCandidatePair(local, remote, isControlling = true)

        assertEquals(pair1.priority, pair2.priority)
    }
}
