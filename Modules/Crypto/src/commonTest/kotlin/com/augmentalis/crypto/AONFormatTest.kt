/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto

import com.augmentalis.crypto.aon.AONCodec
import com.augmentalis.crypto.aon.AONFormat
import com.augmentalis.crypto.aon.AONFooter
import com.augmentalis.crypto.aon.AONHeader
import com.augmentalis.crypto.digest.CryptoDigest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Cross-platform AON format tests.
 *
 * These tests verify:
 * - Magic byte detection
 * - Header/footer parsing
 * - CRC32 computation consistency
 * - SHA-256 / MD5 / HMAC-SHA256 basic correctness
 * - AONCodec.isAON() detection
 * - Non-AON passthrough (backward compatibility)
 */
class AONFormatTest {

    // ─── Magic Byte Detection ─────────────────────────────────

    @Test
    fun isAON_withValidMagic_returnsTrue() {
        val data = ByteArray(AONFormat.HEADER_SIZE + AONFormat.FOOTER_SIZE + 10)
        AONFormat.MAGIC.copyInto(data)
        assertTrue(AONCodec.isAON(data))
    }

    @Test
    fun isAON_withInvalidMagic_returnsFalse() {
        val data = ByteArray(AONFormat.HEADER_SIZE + AONFormat.FOOTER_SIZE + 10)
        // Write wrong magic
        data[0] = 0x00
        assertFalse(AONCodec.isAON(data))
    }

    @Test
    fun isAON_withTooSmallData_returnsFalse() {
        val data = ByteArray(4)
        assertFalse(AONCodec.isAON(data))
    }

    @Test
    fun isAON_withEmptyData_returnsFalse() {
        assertFalse(AONCodec.isAON(ByteArray(0)))
    }

    @Test
    fun isAON_withRawOnnx_returnsFalse() {
        // ONNX files start with 0x08 (protobuf field tag), not AVA-AON magic
        val onnxData = byteArrayOf(0x08, 0x06, 0x12, 0x04)
        assertFalse(AONCodec.isAON(onnxData))
    }

    // ─── Header Parsing ───────────────────────────────────────

    @Test
    fun headerParse_extractsModelId() {
        val headerBytes = ByteArray(AONFormat.HEADER_SIZE)
        AONFormat.MAGIC.copyInto(headerBytes)
        AONFormat.putIntLE(headerBytes, AONFormat.OFF_FORMAT_VERSION, AONFormat.FORMAT_VERSION)
        AONFormat.putString(headerBytes, AONFormat.OFF_MODEL_ID, "AVA-384-NLU-INT8", AONFormat.MODEL_ID_SIZE)

        val header = AONHeader.parse(headerBytes)
        assertEquals("AVA-384-NLU-INT8", header.modelId)
        assertTrue(header.hasValidMagic())
        assertEquals(AONFormat.FORMAT_VERSION, header.formatVersion)
    }

    @Test
    fun headerParse_extractsTimestamps() {
        val headerBytes = ByteArray(AONFormat.HEADER_SIZE)
        AONFormat.MAGIC.copyInto(headerBytes)
        AONFormat.putIntLE(headerBytes, AONFormat.OFF_FORMAT_VERSION, AONFormat.FORMAT_VERSION)
        AONFormat.putLongLE(headerBytes, AONFormat.OFF_CREATED_TIMESTAMP, 1700000000L)
        AONFormat.putLongLE(headerBytes, AONFormat.OFF_EXPIRY_TIMESTAMP, 1800000000L)

        val header = AONHeader.parse(headerBytes)
        assertEquals(1700000000L, header.createdTimestamp)
        assertEquals(1800000000L, header.expiryTimestamp)
    }

    @Test
    fun headerParse_extractsLicenseTier() {
        val headerBytes = ByteArray(AONFormat.HEADER_SIZE)
        AONFormat.MAGIC.copyInto(headerBytes)
        headerBytes[AONFormat.OFF_LICENSE_TIER] = 2 // enterprise

        val header = AONHeader.parse(headerBytes)
        assertEquals(2, header.licenseTier.toInt())
    }

    @Test
    fun headerParse_noPackageRestrictions_whenAllZero() {
        val headerBytes = ByteArray(AONFormat.HEADER_SIZE)
        AONFormat.MAGIC.copyInto(headerBytes)

        val header = AONHeader.parse(headerBytes)
        assertFalse(header.hasPackageRestrictions())
    }

    // ─── Footer Parsing ───────────────────────────────────────

    @Test
    fun footerParse_extractsMagic() {
        val footerBytes = ByteArray(AONFormat.FOOTER_SIZE)
        AONFormat.FOOTER_MAGIC.copyInto(footerBytes, AONFormat.FOFF_FOOTER_MAGIC)

        val footer = AONFooter.parse(footerBytes)
        assertTrue(footer.hasValidMagic())
    }

    @Test
    fun footerParse_invalidMagic() {
        val footerBytes = ByteArray(AONFormat.FOOTER_SIZE)
        // Don't write footer magic
        val footer = AONFooter.parse(footerBytes)
        assertFalse(footer.hasValidMagic())
    }

    // ─── Byte Helpers ─────────────────────────────────────────

    @Test
    fun intLE_roundTrip() {
        val buf = ByteArray(4)
        AONFormat.putIntLE(buf, 0, 0x12345678)
        assertEquals(0x12345678, AONFormat.getIntLE(buf, 0))
    }

    @Test
    fun longLE_roundTrip() {
        val buf = ByteArray(8)
        AONFormat.putLongLE(buf, 0, 0x123456789ABCDEF0L)
        assertEquals(0x123456789ABCDEF0L, AONFormat.getLongLE(buf, 0))
    }

    @Test
    fun stringExtract_nullTerminated() {
        val buf = ByteArray(32)
        val text = "AVA-384"
        text.encodeToByteArray().copyInto(buf)
        assertEquals("AVA-384", AONFormat.extractString(buf, 0, 32))
    }

    // ─── CryptoDigest ─────────────────────────────────────────

    @Test
    fun crc32_emptyData() {
        val crc = CryptoDigest.crc32(ByteArray(0))
        assertEquals(0, crc)
    }

    @Test
    fun crc32_knownValue() {
        // CRC32 of "123456789" = 0xCBF43926
        val data = "123456789".encodeToByteArray()
        val crc = CryptoDigest.crc32(data)
        assertEquals(0xCBF43926.toInt(), crc)
    }

    @Test
    fun crc32_multiChunk_matchesSingleChunk() {
        val data = "Hello, World!".encodeToByteArray()
        val chunk1 = data.copyOfRange(0, 5)
        val chunk2 = data.copyOfRange(5, data.size)

        val singleCrc = CryptoDigest.crc32(data)
        val multiCrc = CryptoDigest.crc32(chunk1, chunk2)
        assertEquals(singleCrc, multiCrc)
    }

    @Test
    fun sha256_knownValue() = runTest {
        // SHA-256 of empty string
        val hash = CryptoDigest.sha256(ByteArray(0))
        assertEquals(32, hash.size)
        // First byte of SHA-256("") is 0xe3
        assertEquals(0xe3.toByte(), hash[0])
    }

    @Test
    fun sha256Truncated16_returns16Bytes() = runTest {
        val hash = CryptoDigest.sha256Truncated16("test".encodeToByteArray())
        assertEquals(16, hash.size)

        // Verify it matches first 16 bytes of full SHA-256
        val fullHash = CryptoDigest.sha256("test".encodeToByteArray())
        assertTrue(hash.contentEquals(fullHash.copyOf(16)))
    }

    @Test
    fun md5_knownValue() = runTest {
        // MD5 of empty string = d41d8cd98f00b204e9800998ecf8427e
        val hash = CryptoDigest.md5(ByteArray(0))
        assertEquals(16, hash.size)
        assertEquals(0xd4.toByte(), hash[0])
    }

    @Test
    fun hmacSha256_producesConsistentResult() = runTest {
        val key = "test-key".encodeToByteArray()
        val data = "test-data".encodeToByteArray()

        val result1 = CryptoDigest.hmacSha256(key, data)
        val result2 = CryptoDigest.hmacSha256(key, data)

        assertEquals(32, result1.size)
        assertTrue(result1.contentEquals(result2))
    }

    // ─── AONCodec Non-AON Passthrough ─────────────────────────

    @Test
    fun unwrap_nonAON_returnsDataAsIs() = runTest {
        // ONNX protobuf data — no AON magic
        val onnxData = byteArrayOf(0x08, 0x06, 0x12, 0x04, 0x6F, 0x6E, 0x6E, 0x78)
        val result = AONCodec.unwrap(onnxData)
        assertTrue(result.contentEquals(onnxData))
    }

    // ─── HMAC Key Obfuscation ─────────────────────────────────

    @Test
    fun hmacKey_deobfuscates_correctly() {
        val key = AONFormat.getDefaultHmacKey()
        val expected = "AVA-AON-HMAC-SECRET-KEY-V1-CHANGE-IN-PRODUCTION"
        assertEquals(expected, key.decodeToString())
    }
}
