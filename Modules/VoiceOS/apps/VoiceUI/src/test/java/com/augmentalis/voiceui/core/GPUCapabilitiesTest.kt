/**
 * GPUCapabilitiesTest.kt - Unit tests for GPU capability detection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-27
 */
package com.augmentalis.voiceui.core

import android.os.Build
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for GPUCapabilities detection logic
 */
class GPUCapabilitiesTest {

    @Test
    fun `MIN_GPU_API is API 31`() {
        assertEquals(Build.VERSION_CODES.S, GPUCapabilities.MIN_GPU_API)
    }

    @Test
    fun `accelerationMode is never null`() {
        assertNotNull(GPUCapabilities.accelerationMode)
    }

    @Test
    fun `gpuInfo contains API level`() {
        val info = GPUCapabilities.gpuInfo
        assertTrue("GPU info should contain API level", info.contains("API:"))
    }

    @Test
    fun `getCapabilityReport returns all required fields`() {
        val report = GPUCapabilities.getCapabilityReport()

        assertTrue(report.containsKey("apiLevel"))
        assertTrue(report.containsKey("accelerationMode"))
        assertTrue(report.containsKey("gpuAvailable"))
        assertTrue(report.containsKey("blurSupported"))
        assertTrue(report.containsKey("colorFilterSupported"))
        assertTrue(report.containsKey("device"))
        assertTrue(report.containsKey("manufacturer"))
    }

    @Test
    fun `blur and color filter support match GPU availability`() {
        // These should always be the same on any given device
        assertEquals(
            GPUCapabilities.isGpuAccelerationAvailable,
            GPUCapabilities.isBlurSupported
        )
        assertEquals(
            GPUCapabilities.isGpuAccelerationAvailable,
            GPUCapabilities.isColorFilterSupported
        )
    }

    @Test
    fun `accelerationMode has valid display name`() {
        val mode = GPUCapabilities.accelerationMode
        assertFalse(mode.displayName.isBlank())
    }

    @Test
    fun `all acceleration modes have display names`() {
        GPUCapabilities.AccelerationMode.entries.forEach { mode ->
            assertFalse(
                "Mode ${mode.name} should have display name",
                mode.displayName.isBlank()
            )
        }
    }
}
