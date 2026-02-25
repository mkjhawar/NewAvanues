package com.augmentalis.netavanue.signaling

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SignalingMessageTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    @Test
    fun `CreateSessionMessage serializes correctly`() {
        val msg = CreateSessionMessage(
            fingerprint = "abc123",
            licenseToken = "license_xyz",
            sessionType = SessionType.CAST,
            capabilities = DeviceCapabilityDto(cpuCores = 8, ramMb = 8192),
        )
        val encoded = json.encodeToString(CreateSessionMessage.serializer(), msg)
        val decoded = json.decodeFromString(CreateSessionMessage.serializer(), encoded)
        assertEquals("abc123", decoded.fingerprint)
        assertEquals(SessionType.CAST, decoded.sessionType)
        assertEquals(8, decoded.capabilities.cpuCores)
    }

    @Test
    fun `SessionCreatedEvent deserializes from server JSON`() {
        val serverJson = """{"type":"SESSION_CREATED","sessionId":"sess_123","inviteCode":"AVNE-K7M3-P9X2","hubFingerprint":"abc123","role":"HOST","turnCredentials":null}"""
        val event = json.decodeFromString(SessionCreatedEvent.serializer(), serverJson)
        assertEquals("sess_123", event.sessionId)
        assertEquals("AVNE-K7M3-P9X2", event.inviteCode)
        assertEquals("HOST", event.role)
    }

    @Test
    fun `ParticipantInfo deserializes correctly`() {
        val serverJson = """{"fingerprint":"fp_abc","deviceName":"Pixel 9","platform":"ANDROID","deviceType":"PHONE","role":"GUEST","capabilityScore":450,"isLicensed":false}"""
        val info = json.decodeFromString(ParticipantInfo.serializer(), serverJson)
        assertEquals("fp_abc", info.fingerprint)
        assertEquals("Pixel 9", info.deviceName)
        assertEquals(450, info.capabilityScore)
        assertEquals(false, info.isLicensed)
    }

    @Test
    fun `TurnCredential deserializes correctly`() {
        val serverJson = """{"username":"1234:fp_abc","password":"secret","urls":["stun:stun.avanues.com:3478","turn:turn.avanues.com:3478?transport=udp"],"ttlSeconds":3600}"""
        val creds = json.decodeFromString(TurnCredential.serializer(), serverJson)
        assertEquals("1234:fp_abc", creds.username)
        assertEquals(2, creds.urls.size)
        assertEquals(3600, creds.ttlSeconds)
    }

    @Test
    fun `HubElectedEvent deserializes with enum reason`() {
        val serverJson = """{"type":"HUB_ELECTED","sessionId":"sess_1","hubFingerprint":"fp_xyz","reason":"CAPABILITY_CHANGE","score":550}"""
        val event = json.decodeFromString(HubElectedEvent.serializer(), serverJson)
        assertEquals(HubElectionReason.CAPABILITY_CHANGE, event.reason)
        assertEquals(550, event.score)
    }

    @Test
    fun `SignalingErrorEvent deserializes correctly`() {
        val serverJson = """{"type":"ERROR","code":"SESSION_FULL","message":"Session has reached max participants"}"""
        val event = json.decodeFromString(SignalingErrorEvent.serializer(), serverJson)
        assertEquals("SESSION_FULL", event.code)
    }

    @Test
    fun `DeviceCapabilityDto handles optional fields`() {
        val minimalJson = """{"cpuCores":4,"ramMb":4096}"""
        val dto = json.decodeFromString(DeviceCapabilityDto.serializer(), minimalJson)
        assertEquals(4, dto.cpuCores)
        assertEquals(4096, dto.ramMb)
        assertEquals(null, dto.batteryPercent)
        assertEquals(null, dto.supportedCodecs)
    }

    @Test
    fun `RegisterDeviceMessage serializes all fields`() {
        val msg = RegisterDeviceMessage(
            fingerprint = "fp_abc",
            publicKey = "base64key==",
            deviceName = "Test Device",
            platform = DevicePlatform.ANDROID,
            deviceType = DeviceType.PHONE,
        )
        val encoded = json.encodeToString(RegisterDeviceMessage.serializer(), msg)
        assertNotNull(encoded)
        val decoded = json.decodeFromString(RegisterDeviceMessage.serializer(), encoded)
        assertEquals(DevicePlatform.ANDROID, decoded.platform)
        assertEquals(DeviceType.PHONE, decoded.deviceType)
    }

    @Test
    fun `enums serialize to expected string values`() {
        assertEquals("\"CAST\"", json.encodeToString(SessionType.serializer(), SessionType.CAST))
        assertEquals("\"ANDROID\"", json.encodeToString(DevicePlatform.serializer(), DevicePlatform.ANDROID))
        assertEquals("\"WIFI\"", json.encodeToString(NetworkType.serializer(), NetworkType.WIFI))
        assertEquals("\"HOST\"", json.encodeToString(ParticipantRole.serializer(), ParticipantRole.HOST))
    }
}
