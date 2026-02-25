/**
 * DeviceViewModelTest.kt - Unit tests for DeviceViewModel
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-02
 * 
 * Tests device management functionality and UI state management
 */
package com.augmentalis.devicemanager.ui

import android.content.Context
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

// Import the data classes from DeviceViewModel
import com.augmentalis.devicemanager.dashboardui.HardwareInfo
import com.augmentalis.devicemanager.dashboardui.BatteryInfo
import com.augmentalis.devicemanager.dashboardui.SensorInfo
import com.augmentalis.devicemanager.dashboardui.NetworkConnectionInfo
import com.augmentalis.devicemanager.dashboardui.AudioDeviceInfo
import com.augmentalis.devicemanager.dashboardui.DisplayInfo
import com.augmentalis.devicemanager.dashboardui.DeviceViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class DeviceViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sensorManager: SensorManager

    @Mock
    private lateinit var connectivityManager: ConnectivityManager

    @Mock
    private lateinit var batteryManager: BatteryManager

    @Mock
    private lateinit var hardwareObserver: Observer<HardwareInfo>

    @Mock
    private lateinit var batteryObserver: Observer<BatteryInfo>

    @Mock
    private lateinit var sensorsObserver: Observer<List<SensorInfo>>

    @Mock
    private lateinit var networkObserver: Observer<NetworkConnectionInfo>

    @Mock
    private lateinit var audioObserver: Observer<List<AudioDeviceInfo>>

    @Mock
    private lateinit var displayObserver: Observer<DisplayInfo>

    @Mock
    private lateinit var xrObserver: Observer<Boolean>

    @Mock
    private lateinit var bluetoothObserver: Observer<List<String>>

    @Mock
    private lateinit var wifiObserver: Observer<List<String>>

    @Mock
    private lateinit var uwbObserver: Observer<Boolean>

    @Mock
    private lateinit var loadingObserver: Observer<Boolean>

    @Mock
    private lateinit var errorObserver: Observer<String?>

    @Mock
    private lateinit var successObserver: Observer<String?>

    private lateinit var viewModel: DeviceViewModel
    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Setup mocks - ensure services are properly mocked before ViewModel creation
        whenever(context.getSystemService(Context.SENSOR_SERVICE)).thenReturn(sensorManager)
        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        whenever(context.getSystemService(Context.BATTERY_SERVICE)).thenReturn(batteryManager)
        
        // Add additional required mocks
        whenever(context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(null)
        whenever(context.getSystemService(Context.DISPLAY_SERVICE)).thenReturn(null)
        whenever(context.getSystemService(Context.WINDOW_SERVICE)).thenReturn(null)
        whenever(context.packageManager).thenReturn(null)
        whenever(context.contentResolver).thenReturn(null)
        whenever(context.resources).thenReturn(null)
        
        // Mock sensor manager methods
        whenever(sensorManager.getSensorList(any())).thenReturn(emptyList())
        
        // Mock battery manager methods
        whenever(batteryManager.getIntProperty(any())).thenReturn(75)
        
        // Mock connectivity manager methods  
        whenever(connectivityManager.activeNetwork).thenReturn(null)
        whenever(connectivityManager.getNetworkCapabilities(any())).thenReturn(null)
        
        // Create ViewModel after all mocks are set up
        viewModel = DeviceViewModel(context)
        
        // Advance time to allow init coroutines to start
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Setup observers
        viewModel.hardwareInfo.observeForever(hardwareObserver)
        viewModel.batteryInfo.observeForever(batteryObserver)
        viewModel.sensorsList.observeForever(sensorsObserver)
        viewModel.networkInfo.observeForever(networkObserver)
        viewModel.audioDevices.observeForever(audioObserver)
        viewModel.displayInfo.observeForever(displayObserver)
        viewModel.xrSupported.observeForever(xrObserver)
        viewModel.bluetoothDevices.observeForever(bluetoothObserver)
        viewModel.wifiNetworks.observeForever(wifiObserver)
        viewModel.uwbSupported.observeForever(uwbObserver)
        viewModel.isLoading.observeForever(loadingObserver)
        viewModel.errorMessage.observeForever(errorObserver)
        viewModel.successMessage.observeForever(successObserver)
    }
    
    @After
    fun tearDown() {
        // Remove observers
        viewModel.hardwareInfo.removeObserver(hardwareObserver)
        viewModel.batteryInfo.removeObserver(batteryObserver)
        viewModel.sensorsList.removeObserver(sensorsObserver)
        viewModel.networkInfo.removeObserver(networkObserver)
        viewModel.audioDevices.removeObserver(audioObserver)
        viewModel.displayInfo.removeObserver(displayObserver)
        viewModel.xrSupported.removeObserver(xrObserver)
        viewModel.bluetoothDevices.removeObserver(bluetoothObserver)
        viewModel.wifiNetworks.removeObserver(wifiObserver)
        viewModel.uwbSupported.removeObserver(uwbObserver)
        viewModel.isLoading.removeObserver(loadingObserver)
        viewModel.errorMessage.removeObserver(errorObserver)
        viewModel.successMessage.removeObserver(successObserver)
        
        // Reset main dispatcher
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        // Test initial state
        assertEquals(false, viewModel.isLoading.value)
        assertEquals(null, viewModel.errorMessage.value)
        assertEquals(null, viewModel.successMessage.value)
    }

    @Test
    fun testHardwareInfoLoading() {
        // Hardware info should be loaded on initialization
        testDispatcher.scheduler.advanceUntilIdle()
        
        val hardwareInfo = viewModel.hardwareInfo.value
        assertNotNull(hardwareInfo)
        assertTrue(hardwareInfo!!.model.isNotEmpty())
        assertTrue(hardwareInfo.manufacturer.isNotEmpty())
        assertTrue(hardwareInfo.androidVersion.isNotEmpty())
        assertTrue(hardwareInfo.apiLevel > 0)
    }

    @Test
    fun testBatteryInfoLoading() {
        // Battery info should be loaded
        testDispatcher.scheduler.advanceUntilIdle()
        
        val batteryInfo = viewModel.batteryInfo.value
        assertNotNull(batteryInfo)
        assertTrue(batteryInfo!!.level >= 0 && batteryInfo.level <= 100)
        assertNotNull(batteryInfo.health)
    }

    @Test
    fun testSensorsListLoading() = runTest {
        // Sensors should be loaded
        testDispatcher.scheduler.advanceUntilIdle()
        
        val sensors = viewModel.sensorsList.value
        assertNotNull(sensors)
        // Even if empty, list should be initialized
        assertTrue(sensors is List<*>)
    }

    @Test
    fun testNetworkInfoLoading() = runTest {
        // Network info should be loaded
        testDispatcher.scheduler.advanceUntilIdle()
        
        val networkInfo = viewModel.networkInfo.value
        assertNotNull(networkInfo)
        assertNotNull(networkInfo!!.type)
        assertNotNull(networkInfo.capabilities)
    }

    @Test
    fun testRefreshAllData() = runTest {
        viewModel.refreshAllData()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should show success message
        assertEquals("Device data refreshed", viewModel.successMessage.value)
        
        // Loading should complete
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun testBluetoothScanning() = runTest {
        viewModel.scanBluetoothDevices()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should have scanned devices
        val devices = viewModel.bluetoothDevices.value
        assertNotNull(devices)
        assertTrue(devices!!.isNotEmpty())
        
        // Should show success message
        assertEquals("Bluetooth scan completed", viewModel.successMessage.value)
    }

    @Test
    fun testWiFiScanning() = runTest {
        viewModel.scanWiFiNetworks()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should have scanned networks
        val networks = viewModel.wifiNetworks.value
        assertNotNull(networks)
        assertTrue(networks!!.isNotEmpty())
        
        // Should show success message
        assertEquals("WiFi scan completed", viewModel.successMessage.value)
    }

    @Test
    fun testSensorTesting() = runTest {
        viewModel.testSensors()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should show success message
        assertEquals("All sensors operational", viewModel.successMessage.value)
        
        // Loading should complete
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun testDiagnostics() = runTest {
        viewModel.runDiagnostics()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should show diagnostic results
        val message = viewModel.successMessage.value
        assertNotNull(message)
        assertTrue(message!!.contains("Hardware: OK"))
        assertTrue(message.contains("Sensors:"))
        assertTrue(message.contains("Network:"))
        assertTrue(message.contains("Battery:"))
    }

    @Test
    fun testErrorMessageClearing() {
        viewModel.clearError()
        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun testSuccessMessageClearing() {
        viewModel.clearSuccess()
        assertEquals(null, viewModel.successMessage.value)
    }

}