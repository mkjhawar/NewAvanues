/**
 * DeviceManagerUITest.kt - UI instrumentation tests for DeviceManager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-02
 * 
 * Tests UI components, interactions, and visual elements
 */
package com.augmentalis.devicemanager.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag

// Import test tag functions
import androidx.compose.ui.test.hasTestTag

// Import the data classes and UI components
import com.augmentalis.devicemanager.dashboardui.HardwareInfo
import com.augmentalis.devicemanager.dashboardui.BatteryInfo
import com.augmentalis.devicemanager.dashboardui.SensorInfo
import com.augmentalis.devicemanager.dashboardui.NetworkConnectionInfo
import com.augmentalis.devicemanager.dashboardui.AudioDeviceInfo
import com.augmentalis.devicemanager.dashboardui.DisplayInfo
import com.augmentalis.devicemanager.dashboardui.DeviceViewModel

// Import UI components
import com.augmentalis.devicemanager.dashboardui.DeviceManagerContent
import com.augmentalis.devicemanager.dashboardui.OverviewTab
import com.augmentalis.devicemanager.dashboardui.HardwareTab
import com.augmentalis.devicemanager.dashboardui.SensorsTab
import com.augmentalis.devicemanager.dashboardui.NetworkTab
import com.augmentalis.devicemanager.dashboardui.AudioTab
import com.augmentalis.devicemanager.dashboardui.DisplayTab
import com.augmentalis.devicemanager.dashboardui.DiagnosticsCard
import com.augmentalis.devicemanager.dashboardui.TestSensorsCard
import com.augmentalis.devicemanager.dashboardui.DeviceStatusCard

@RunWith(AndroidJUnit4::class)
class DeviceManagerUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testDeviceManagerActivityLaunch() {
        composeTestRule.setContent {
            DeviceManagerContent(
                viewModel = DeviceViewModel(context)
            )
        }

        // Verify main components are displayed
        composeTestRule.onNodeWithText("Device Manager").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Refresh").assertIsDisplayed()
        
        // Verify tabs are displayed
        composeTestRule.onNodeWithText("Overview").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hardware").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sensors").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Display").assertIsDisplayed()
    }

    @Test
    fun testOverviewTabDisplay() {
        composeTestRule.setContent {
            OverviewTab(
                hardwareInfo = HardwareInfo(
                    model = "Test Device",
                    manufacturer = "Test Manufacturer",
                    brand = "Test Brand",
                    device = "test_device",
                    board = "test_board",
                    hardware = "test_hardware",
                    product = "test_product",
                    androidVersion = "14",
                    apiLevel = 34,
                    buildId = "TEST123",
                    kernelVersion = "5.10"
                ),
                batteryInfo = BatteryInfo(
                    level = 85,
                    isCharging = true,
                    chargingType = "USB",
                    temperature = 25f,
                    voltage = 4200,
                    health = "Good",
                    technology = "Li-ion",
                    capacity = 5000
                ),
                networkInfo = NetworkConnectionInfo(
                    type = "WiFi",
                    isConnected = true,
                    isMetered = false,
                    bandwidth = 100000,
                    signalStrength = -50,
                    ssid = "TestNetwork",
                    capabilities = listOf("Internet", "Validated")
                ),
                sensorsCount = 15,
                audioDevicesCount = 3,
                xrSupported = true,
                uwbSupported = false,
                onRunDiagnostics = { }
            )
        }

        // Verify device status card
        composeTestRule.onNodeWithText("Device Status").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Device").assertIsDisplayed()
        composeTestRule.onNodeWithText("85%").assertIsDisplayed()
        composeTestRule.onNodeWithText("WiFi").assertIsDisplayed()
        
        // Verify quick stats
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sensors").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audio").assertIsDisplayed()
        
        // Verify diagnostics card
        composeTestRule.onNodeWithText("System Diagnostics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Run Diagnostics").assertIsDisplayed()
    }

    @Test
    fun testHardwareTabDisplay() {
        composeTestRule.setContent {
            HardwareTab(
                hardwareInfo = HardwareInfo(
                    model = "Pixel 8 Pro",
                    manufacturer = "Google",
                    brand = "google",
                    device = "husky",
                    board = "husky",
                    hardware = "husky",
                    product = "husky",
                    androidVersion = "14",
                    apiLevel = 34,
                    buildId = "UP1A.231005.007",
                    kernelVersion = "5.15.94"
                ),
                batteryInfo = BatteryInfo(
                    level = 75,
                    isCharging = false,
                    chargingType = "None",
                    temperature = 28f,
                    voltage = 4100,
                    health = "Good",
                    technology = "Li-ion",
                    capacity = 5050
                ),
                foldableState = "FLAT"
            )
        }

        // Verify hardware details card
        composeTestRule.onNodeWithText("Hardware Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pixel 8 Pro").assertIsDisplayed()
        composeTestRule.onNodeWithText("Google").assertIsDisplayed()
        
        // Verify battery card
        composeTestRule.onNodeWithText("Battery").assertIsDisplayed()
        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Good").assertIsDisplayed()
    }

    @Test
    fun testSensorsTabDisplay() {
        val sensorsList = listOf(
            SensorInfo(
                name = "Accelerometer",
                type = 1,
                vendor = "Test Vendor",
                version = 1,
                power = 0.5f,
                maximumRange = 100f,
                resolution = 0.01f,
                minDelay = 1000,
                isWakeUp = false
            ),
            SensorInfo(
                name = "Gyroscope",
                type = 4,
                vendor = "Test Vendor",
                version = 1,
                power = 0.6f,
                maximumRange = 200f,
                resolution = 0.02f,
                minDelay = 2000,
                isWakeUp = false
            )
        )

        composeTestRule.setContent {
            SensorsTab(
                sensorsList = sensorsList,
                imuData = Triple(1.5f, 2.3f, 3.1f),
                onTestSensors = { }
            )
        }

        // Verify IMU data card
        composeTestRule.onNodeWithText("IMU Data (Live)").assertIsDisplayed()
        composeTestRule.onNodeWithText("X").assertIsDisplayed()
        composeTestRule.onNodeWithText("Y").assertIsDisplayed()
        composeTestRule.onNodeWithText("Z").assertIsDisplayed()
        
        // Verify test sensors button
        composeTestRule.onNodeWithText("Test All Sensors").assertIsDisplayed()
        
        // Verify sensor items
        composeTestRule.onNodeWithText("Accelerometer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gyroscope").assertIsDisplayed()
    }

    @Test
    fun testNetworkTabDisplay() {
        composeTestRule.setContent {
            NetworkTab(
                networkInfo = NetworkConnectionInfo(
                    type = "WiFi",
                    isConnected = true,
                    isMetered = false,
                    bandwidth = 50000,
                    signalStrength = -60,
                    ssid = "HomeNetwork",
                    capabilities = listOf("Internet")
                ),
                bluetoothDevices = listOf(
                    "Headphones - AA:BB:CC:DD:EE:FF",
                    "Speaker - 11:22:33:44:55:66"
                ),
                wifiNetworks = listOf(
                    "HomeNetwork - 5GHz",
                    "GuestNetwork - 2.4GHz"
                ),
                uwbSupported = true,
                onScanBluetooth = { },
                onScanWiFi = { }
            )
        }

        // Verify network status card
        composeTestRule.onNodeWithText("Network Status").assertIsDisplayed()
        composeTestRule.onNodeWithText("WiFi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
        
        // Verify WiFi section
        composeTestRule.onNodeWithText("WiFi Networks").assertIsDisplayed()
        composeTestRule.onNodeWithText("HomeNetwork - 5GHz").assertIsDisplayed()
        
        // Verify Bluetooth section
        composeTestRule.onNodeWithText("Bluetooth Devices").assertIsDisplayed()
        composeTestRule.onNodeWithText("Headphones - AA:BB:CC:DD:EE:FF").assertIsDisplayed()
        
        // Verify UWB card
        composeTestRule.onNodeWithText("Ultra-Wideband").assertIsDisplayed()
    }

    @Test
    fun testAudioTabDisplay() {
        val audioDevices = listOf(
            AudioDeviceInfo(
                id = 1,
                name = "Built-in Speaker",
                type = "Speaker",
                isInput = false,
                sampleRates = listOf(44100, 48000),
                channelMasks = listOf(1, 2),
                formats = listOf(16, 24)
            ),
            AudioDeviceInfo(
                id = 2,
                name = "Built-in Microphone",
                type = "Microphone",
                isInput = true,
                sampleRates = listOf(44100),
                channelMasks = listOf(1),
                formats = listOf(16)
            )
        )

        composeTestRule.setContent {
            AudioTab(audioDevices = audioDevices)
        }

        // Verify audio devices are displayed
        composeTestRule.onNodeWithText("Built-in Speaker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Built-in Microphone").assertIsDisplayed()
        composeTestRule.onNodeWithText("Speaker • 44100 Hz").assertIsDisplayed()
        composeTestRule.onNodeWithText("Microphone • 44100 Hz").assertIsDisplayed()
    }

    @Test
    fun testDisplayTabDisplay() {
        composeTestRule.setContent {
            DisplayTab(
                displayInfo = DisplayInfo(
                    width = 1440,
                    height = 3120,
                    density = 3.5f,
                    scaledDensity = 3.5f,
                    refreshRate = 120f,
                    hdrCapabilities = listOf("HDR10", "HDR10+"),
                    isRound = false,
                    cutoutInfo = "Present"
                ),
                xrSupported = true
            )
        }

        // Verify display specs card
        composeTestRule.onNodeWithText("Display Specifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("1440 × 3120").assertIsDisplayed()
        composeTestRule.onNodeWithText("120 Hz").assertIsDisplayed()
        composeTestRule.onNodeWithText("Supported").assertIsDisplayed() // HDR
        
        // Verify XR capabilities card
        composeTestRule.onNodeWithText("XR Capabilities").assertIsDisplayed()
        composeTestRule.onNodeWithText("Extended Reality Ready").assertIsDisplayed()
    }

    @Test
    fun testTabNavigation() {
        composeTestRule.setContent {
            DeviceManagerContent(
                viewModel = DeviceViewModel(context)
            )
        }

        // Test switching between tabs
        composeTestRule.onNodeWithText("Hardware").performClick()
        composeTestRule.onNodeWithText("Hardware Details").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Sensors").performClick()
        composeTestRule.onNodeWithText("IMU Data (Live)").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Network").performClick()
        composeTestRule.onNodeWithText("Network Status").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Audio").performClick()
        // Audio tab content depends on detected devices
        
        composeTestRule.onNodeWithText("Display").performClick()
        composeTestRule.onNodeWithText("Display Specifications").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Overview").performClick()
        composeTestRule.onNodeWithText("Device Status").assertIsDisplayed()
    }

    @Test
    fun testDiagnosticsButtonInteraction() {
        var diagnosticsClicked = false
        
        composeTestRule.setContent {
            DiagnosticsCard(
                onRunDiagnostics = { diagnosticsClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Run Diagnostics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Run Diagnostics").performClick()
        
        assertTrue(diagnosticsClicked)
    }

    @Test
    fun testSensorTestButtonInteraction() {
        var testClicked = false
        
        composeTestRule.setContent {
            TestSensorsCard(
                onTest = { testClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Test All Sensors").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test All Sensors").performClick()
        
        assertTrue(testClicked)
    }

    @Test
    fun testNetworkScanButtonsInteraction() {
        var wifiScanClicked = false
        var bluetoothScanClicked = false
        
        composeTestRule.setContent {
            NetworkTab(
                networkInfo = NetworkConnectionInfo(
                    type = "WiFi",
                    isConnected = true,
                    isMetered = false,
                    bandwidth = 50000,
                    signalStrength = -60,
                    ssid = "TestNetwork",
                    capabilities = emptyList()
                ),
                bluetoothDevices = emptyList(),
                wifiNetworks = emptyList(),
                uwbSupported = false,
                onScanBluetooth = { bluetoothScanClicked = true },
                onScanWiFi = { wifiScanClicked = true }
            )
        }

        // Test WiFi scan button
        composeTestRule.onNodeWithContentDescription("Scan").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Scan")[0].performClick()
        assertTrue(wifiScanClicked)
        
        // Test Bluetooth scan button
        composeTestRule.onAllNodesWithContentDescription("Scan")[1].performClick()
        assertTrue(bluetoothScanClicked)
    }

    @Test
    fun testGlassmorphismStyling() {
        composeTestRule.setContent {
            DeviceStatusCard(
                model = "Test Device",
                androidVersion = "14",
                batteryLevel = 90,
                isCharging = false,
                networkType = "WiFi",
                isConnected = true
            )
        }

        // Verify glassmorphism card is rendered
        composeTestRule.onNode(hasTestTag("device_status_card"))
            .assertIsDisplayed()
        
        // The glassmorphism styling itself is visual and harder to test programmatically
        // but we can verify the card structure exists
        composeTestRule.onNodeWithText("Device Status").assertIsDisplayed()
    }
}