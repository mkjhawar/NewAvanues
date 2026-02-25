package com.augmentalis.netavanue.session

import com.augmentalis.netavanue.signaling.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SessionTest {

    private fun createTestSession() = Session(
        sessionId = "sess_123",
        inviteCode = "AVNE-K7M3-P9X2",
        sessionType = SessionType.CAST,
        initialRole = ParticipantRole.HOST,
        initialHubFingerprint = "fp_host",
    )

    private fun testParticipant(name: String, fp: String) = ParticipantInfo(
        fingerprint = fp,
        deviceName = name,
        platform = "ANDROID",
        deviceType = "PHONE",
        role = "GUEST",
        capabilityScore = 300,
        isLicensed = false,
    )

    @Test
    fun `session starts active with correct initial state`() {
        val session = createTestSession()
        assertTrue(session.isActive.value)
        assertEquals(ParticipantRole.HOST, session.myRole.value)
        assertEquals("fp_host", session.hubFingerprint.value)
        assertEquals(0, session.participantCount)
        assertTrue(session.isHost)
    }

    @Test
    fun `addParticipant increases count`() {
        val session = createTestSession()
        session.addParticipant(testParticipant("Alice", "fp_alice"))
        assertEquals(1, session.participantCount)
        session.addParticipant(testParticipant("Bob", "fp_bob"))
        assertEquals(2, session.participantCount)
    }

    @Test
    fun `removeParticipant decreases count`() {
        val session = createTestSession()
        session.addParticipant(testParticipant("Alice", "fp_alice"))
        session.addParticipant(testParticipant("Bob", "fp_bob"))
        session.removeParticipant("fp_alice")
        assertEquals(1, session.participantCount)
        assertEquals("fp_bob", session.participants.value[0].fingerprint)
    }

    @Test
    fun `updateHub changes hub fingerprint`() {
        val session = createTestSession()
        assertEquals("fp_host", session.hubFingerprint.value)
        session.updateHub("fp_new_hub")
        assertEquals("fp_new_hub", session.hubFingerprint.value)
    }

    @Test
    fun `updateRole changes my role`() {
        val session = createTestSession()
        assertTrue(session.isHost)
        assertFalse(session.isHub)
        session.updateRole(ParticipantRole.HUB)
        assertTrue(session.isHub)
    }

    @Test
    fun `end marks session inactive`() {
        val session = createTestSession()
        assertTrue(session.isActive.value)
        session.end()
        assertFalse(session.isActive.value)
    }

    @Test
    fun `updateTurnCredentials stores credentials`() {
        val session = createTestSession()
        assertEquals(null, session.turnCredentials.value)
        val creds = TurnCredential(
            username = "user",
            password = "pass",
            urls = listOf("turn:turn.avanues.com:3478"),
            ttlSeconds = 3600,
        )
        session.updateTurnCredentials(creds)
        assertEquals("user", session.turnCredentials.value?.username)
    }

    @Test
    fun `updateParticipants replaces entire list`() {
        val session = createTestSession()
        session.addParticipant(testParticipant("Old", "fp_old"))
        assertEquals(1, session.participantCount)
        session.updateParticipants(listOf(
            testParticipant("New1", "fp_new1"),
            testParticipant("New2", "fp_new2"),
        ))
        assertEquals(2, session.participantCount)
        assertEquals("fp_new1", session.participants.value[0].fingerprint)
    }
}
