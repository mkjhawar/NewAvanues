// Author: Manoj Jhawar
// Purpose: Unit tests for KMP capability models

package com.augmentalis.devicemanager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CapabilityModelsTest {

    @Test
    fun testKmpDeviceInfoCreation() {
        val deviceInfo = KmpDeviceInfo(
            manufacturer = "TestMfg",
            model = "TestModel",
            osVersion = "14.0",
            deviceType = KmpDeviceType.PHONE
        )

        assertEquals("TestMfg", deviceInfo.manufacturer)
        assertEquals("TestModel", deviceInfo.model)
        assertEquals("14.0", deviceInfo.osVersion)
        assertEquals(KmpDeviceType.PHONE, deviceInfo.deviceType)
    }

    @Test
    fun testKmpDeviceInfoDefaults() {
        val deviceInfo = KmpDeviceInfo(
            manufacturer = "Mfg",
            model = "Model",
            osVersion = "1.0"
        )

        assertEquals("", deviceInfo.brand)
        assertEquals("", deviceInfo.device)
        assertEquals(0, deviceInfo.osVersionCode)
        assertEquals(KmpDeviceType.UNKNOWN, deviceInfo.deviceType)
    }

    @Test
    fun testHardwareProfileCreation() {
        val profile = HardwareProfile(
            cpuCores = 8,
            cpuArchitecture = "arm64",
            cpuMaxFrequencyMhz = 2800,
            totalRamMb = 8192,
            availableRamMb = 4096,
            gpuVendor = "Qualcomm",
            gpuRenderer = "Adreno 740",
            internalStorageGb = 256
        )

        assertEquals(8, profile.cpuCores)
        assertEquals("arm64", profile.cpuArchitecture)
        assertEquals(2800, profile.cpuMaxFrequencyMhz)
        assertEquals(8192, profile.totalRamMb)
    }

    @Test
    fun testDeviceFingerprintCreation() {
        val fingerprint = DeviceFingerprint(
            value = "abc123def456",
            type = "hardware",
            components = listOf("cpu", "ram", "storage"),
            timestamp = 1705555555000L
        )

        assertEquals("abc123def456", fingerprint.value)
        assertEquals("hardware", fingerprint.type)
        assertEquals(3, fingerprint.components.size)
        assertNotNull(fingerprint.timestamp)
    }

    @Test
    fun testNetworkCapabilitiesCreation() {
        val caps = NetworkCapabilities(
            hasBluetooth = true,
            hasBluetoothLE = true,
            hasWiFi = true,
            hasWiFiDirect = false,
            hasWiFiAware = false,
            hasNfc = true,
            hasUwb = false,
            hasCellular = true,
            has5G = true
        )

        assertEquals(true, caps.hasBluetooth)
        assertEquals(true, caps.hasWiFi)
        assertEquals(true, caps.has5G)
        assertEquals(false, caps.hasUwb)
    }

    @Test
    fun testPerformanceClassEnum() {
        assertEquals(3, PerformanceClass.entries.size)
        assertNotNull(PerformanceClass.HIGH_END)
        assertNotNull(PerformanceClass.MID_RANGE)
        assertNotNull(PerformanceClass.LOW_END)
    }

    @Test
    fun testKmpDeviceTypeEnum() {
        assertEquals(9, KmpDeviceType.entries.size)
        assertNotNull(KmpDeviceType.PHONE)
        assertNotNull(KmpDeviceType.TABLET)
        assertNotNull(KmpDeviceType.WATCH)
        assertNotNull(KmpDeviceType.TV)
        assertNotNull(KmpDeviceType.DESKTOP)
        assertNotNull(KmpDeviceType.SMART_GLASS)
        assertNotNull(KmpDeviceType.XR_HEADSET)
        assertNotNull(KmpDeviceType.AUTOMOTIVE)
        assertNotNull(KmpDeviceType.UNKNOWN)
    }
}
