# DeviceManager - Complete API Reference

**Module Type:** libraries
**Generated:** 2025-10-19 22:03:50 PDT
**Timestamp:** 251019-2203
**Location:** `modules/libraries/DeviceManager`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the DeviceManager module.

## Files


### File: `build/generated/ap_generated_sources/debug/out/androidx/databinding/DataBindingComponent.java`

**Package:** `androidx.databinding`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/generated/ap_generated_sources/debug/out/androidx/databinding/library/baseAdapters/BR.java`

**Package:** `androidx.databinding.library.baseAdapters`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/generated/ap_generated_sources/debug/out/com/augmentalis/devicemanager/BR.java`

**Package:** `com.augmentalis.devicemanager`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/generated/ap_generated_sources/debug/out/com/augmentalis/devicemanager/DataBinderMapperImpl.java`

**Package:** `com.augmentalis.devicemanager`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.util.SparseArray;
  - import android.util.SparseIntArray;
  - import android.view.View;
  - import androidx.databinding.DataBinderMapper;
  - import androidx.databinding.DataBindingComponent;
  - import androidx.databinding.ViewDataBinding;
  - import java.lang.Integer;
  - import java.lang.Object;
  - import java.lang.Override;
  - import java.lang.RuntimeException;
  - ... and 4 more

---

### File: `build/generated/data_binding_trigger/debug/com/augmentalis/devicemanager/DataBindingTriggerClass.java`

**Package:** `com.augmentalis.devicemanager`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/androidTest/java/com/augmentalis/devicemanager/ui/DeviceManagerUITest.kt`

**Package:** `com.augmentalis.devicemanager.ui`

**Classes/Interfaces/Objects:**
  - class DeviceManagerUITest 

**Public Functions:**

**Imports:**
  - import androidx.compose.ui.test.*
  - import androidx.compose.ui.test.junit4.createComposeRule
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import androidx.test.platform.app.InstrumentationRegistry
  - import org.junit.Rule
  - import org.junit.Test
  - import org.junit.runner.RunWith
  - import org.junit.Assert.assertTrue
  - import androidx.compose.foundation.layout.Column
  - import androidx.compose.runtime.Composable
  - ... and 19 more

---

### File: `src/main/java/com/augmentalis/devicemanager/accessibility/FeedbackManager.kt`

**Package:** `com.augmentalis.devicemanager.accessibility`

**Classes/Interfaces/Objects:**
  - class FeedbackManager(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import android.media.AudioManager
  - import android.media.ToneGenerator
  - import android.os.Build
  - import android.os.VibrationEffect
  - import android.os.Vibrator
  - import android.os.VibratorManager
  - import android.util.Log
  - import androidx.annotation.RequiresApi
  - ... and 7 more

---

### File: `src/main/java/com/augmentalis/devicemanager/accessibility/TTSManager.kt`

**Package:** `com.augmentalis.devicemanager.accessibility`

**Classes/Interfaces/Objects:**
  - class TTSManager(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import android.media.AudioAttributes
  - import android.media.AudioManager
  - import android.os.Build
  - import android.speech.tts.TextToSpeech
  - import android.speech.tts.UtteranceProgressListener
  - import android.speech.tts.Voice
  - import android.util.Log
  - import androidx.annotation.RequiresApi
  - ... and 7 more

---

### File: `src/main/java/com/augmentalis/devicemanager/audio/AudioCapture.kt`

**Package:** `com.augmentalis.devicemanager.audio`

**Classes/Interfaces/Objects:**
  - class AudioCapture(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.media.AudioRecord
  - import android.media.MediaRecorder
  - import android.util.Log
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.launch
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/devicemanager/audio/AudioConfig.kt`

**Package:** `com.augmentalis.devicemanager.audio`

**Classes/Interfaces/Objects:**
  - data class AudioConfig(

**Public Functions:**

**Imports:**
  - import android.media.AudioFormat

---

### File: `src/main/java/com/augmentalis/devicemanager/audio/AudioEffects.kt`

**Package:** `com.augmentalis.devicemanager.audio`

**Classes/Interfaces/Objects:**
  - class AudioEffects 

**Public Functions:**

**Imports:**
  - import android.media.audiofx.*
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/devicemanager/audio/AudioEnhancement.kt`

**Package:** `com.augmentalis.devicemanager.audio`

**Classes/Interfaces/Objects:**
  - class AudioEnhancement 

**Public Functions:**

**Imports:**
  - import android.media.audiofx.AcousticEchoCanceler
  - import android.media.audiofx.AutomaticGainControl
  - import android.media.audiofx.NoiseSuppressor
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/devicemanager/audio/AudioModels.kt`

**Package:** `com.augmentalis.devicemanager.audio`

**Classes/Interfaces/Objects:**
  - data class AudioDevice(
  - enum class AudioProfile 
  - enum class EqualizerPreset(val value
  - enum class ReverbPreset 
  - data class AudioLatency(
  - data class EffectConfig(
  - data class EnhancementConfig(
  - data class SpatialConfig(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/devicemanager/audio/AudioRouting.kt`

**Package:** `com.augmentalis.devicemanager.audio`

**Classes/Interfaces/Objects:**
  - class AudioRouting(

**Public Functions:**

**Imports:**
  - import android.bluetooth.BluetoothProfile
  - import android.content.Context
  - import android.media.AudioDeviceCallback
  - import android.media.AudioDeviceInfo
  - import android.media.AudioManager
  - import android.os.Build
  - import android.util.Log
  - import com.augmentalis.devicemanager.network.BluetoothManager
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - ... and 4 more

---

### File: `src/main/java/com/augmentalis/devicemanager/audio/AudioService.kt`

**Package:** `com.augmentalis.devicemanager.audio`

**Classes/Interfaces/Objects:**
  - class AudioService(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.media.AudioAttributes
  - import android.media.AudioFocusRequest
  - import android.media.AudioManager
  - import android.os.Build
  - import android.util.Log
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow

---

### File: `src/main/java/com/augmentalis/devicemanager/audio/SpatialAudio.kt`

**Package:** `com.augmentalis.devicemanager.audio`

**Classes/Interfaces/Objects:**
  - class SpatialAudio(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.media.AudioManager
  - import android.media.audiofx.Virtualizer
  - import android.os.Build
  - import android.util.Log
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow

---

### File: `src/main/java/com/augmentalis/devicemanager/bluetooth/BluetoothPublicAPI.kt`

**Package:** `com.augmentalis.devicemanager.bluetooth`

**Classes/Interfaces/Objects:**
  - class BluetoothPublicAPI(

**Public Functions:**

**Imports:**
  - import android.Manifest
  - import android.annotation.SuppressLint
  - import android.bluetooth.BluetoothDevice
  - import android.content.Context
  - import android.os.Build
  - import android.util.Log
  - import androidx.annotation.RequiresPermission
  - import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector
  - import com.augmentalis.devicemanager.network.BluetoothManager
  - import kotlinx.coroutines.CoroutineScope
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/devicemanager/capabilities/CapabilityQuery.kt`

**Package:** `com.augmentalis.devicemanager.capabilities`

**Classes/Interfaces/Objects:**
  - class CapabilityQuery(private val context
  - data class CapabilitySnapshot(
  - data class DeviceInfoSnapshot(
  - data class NetworkFeatures(
  - data class SensorFeatures(
  - data class HardwareFeatures(
  - data class MediaFeatures(
  - data class BiometricFeatures(
  - data class DisplayFeatures(
  - data class BehavioralFeatures(
  - data class CapabilityComparison(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.os.Build
  - import android.util.Log
  - import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext
  - import org.json.JSONArray
  - import org.json.JSONObject

---

### File: `src/main/java/com/augmentalis/devicemanager/compatibility/ApiCompatibility.kt`

**Package:** `com.augmentalis.devicemanager.compatibility`

**Classes/Interfaces/Objects:**
  - object ApiCompatibility 

**Public Functions:**
  - fun Context.vibrateCompat(pattern: LongArray, repeat: Int = -1) 
  - fun Context.isNetworkAvailableCompat(): Boolean 
  - fun Context.checkPermissionCompat(permission: String): Boolean 
  - fun Context.getBiometricStatusCompat(): ApiCompatibility.BiometricStatus 

**Imports:**
  - import android.os.Build
  - import android.content.Context
  - import android.hardware.camera2.CameraManager
  - import android.hardware.biometrics.BiometricManager
  - import android.hardware.biometrics.BiometricPrompt
  - import androidx.annotation.RequiresApi
  - import androidx.annotation.RequiresPermission
  - import androidx.biometric.BiometricManager as AndroidXBiometricManager
  - import androidx.core.content.ContextCompat
  - import android.Manifest
  - ... and 11 more

---

### File: `src/main/java/com/augmentalis/devicemanager/compatibility/XRCompatibility.kt`

**Package:** `com.augmentalis.devicemanager.compatibility`

**Classes/Interfaces/Objects:**
  - object XRCompatibility 

**Public Functions:**
  - fun Context.isXRSupported(): Boolean 
  - fun Context.getXRCapabilities(): XRCompatibility.XRCapabilities 
  - fun Context.getXRMode(): XRCompatibility.XRMode 
  - fun Context.initializeXRIfAvailable(): Boolean 

**Imports:**
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.util.Log
  - import kotlin.reflect.KClass

---

### File: `src/main/java/com/augmentalis/devicemanager/dashboardui/DeviceInfoUI.kt`

**Package:** `com.augmentalis.devicemanager.dashboardui`

**Classes/Interfaces/Objects:**
  - data class CapabilityItem(

**Public Functions:**
  - fun DeviceInfoScreen(
  - fun OverviewTab(deviceManager: DeviceManager) 
  - fun NetworkTab(deviceManager: DeviceManager) 
  - fun SensorsTab(deviceManager: DeviceManager) 
  - fun SecurityTab(deviceManager: DeviceManager) 
  - fun AudioTab(@Suppress("UNUSED_PARAMETER") deviceManager: DeviceManager) 
  - fun CapabilitiesGrid(deviceManager: DeviceManager) 
  - fun CapabilityChip(capability: CapabilityItem) 
  - fun StatusOverview(deviceManager: DeviceManager) 
  - fun DeviceCard(
  - fun InfoRow(label: String, value: String) 
  - fun ConnectionRow(icon: ImageVector, label: String, count: Int) 
  - fun BiometricTypeCard(type: BiometricManager.BiometricType) 

**Imports:**
  - import androidx.compose.animation.*
  - import androidx.compose.foundation.*
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.*
  - import androidx.compose.foundation.shape.CircleShape
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material.icons.automirrored.filled.*
  - import androidx.compose.material3.*
  - ... and 14 more

---

### File: `src/main/java/com/augmentalis/devicemanager/dashboardui/DeviceManagerActivity.kt`

**Package:** `com.augmentalis.devicemanager.dashboardui`

**Classes/Interfaces/Objects:**
  - class DeviceManagerActivity 

**Public Functions:**
  - fun DeviceManagerContent(viewModel: DeviceViewModel) 
  - fun OverviewTab(
  - fun HardwareTab(
  - fun SensorsTab(
  - fun NetworkTab(
  - fun AudioTab(
  - fun DisplayTab(
  - fun DeviceStatusCard(
  - fun QuickStatsGrid(
  - fun DiagnosticsCard(
  - fun SystemCapabilitiesCard(
  - fun HardwareDetailsCard(hardwareInfo: HardwareInfo?) 
  - fun BatteryCard(batteryInfo: BatteryInfo?) 
  - fun IMUDataCard(imuData: Triple<Float, Float, Float>) 
  - fun NetworkStatusCard(networkInfo: NetworkConnectionInfo?) 
  - fun StatusItem(
  - fun QuickStatCard(
  - fun LoadingOverlay() 
  - fun ErrorSnackbar(
  - fun SuccessSnackbar(
  - fun CapabilityRow(
  - fun HardwareRow(label: String, value: String) 
  - fun BatteryStatItem(label: String, value: String) 
  - fun IMUAxisDisplay(axis: String, value: Float, color: Color) 
  - fun NetworkInfoRow(label: String, value: String, icon: ImageVector) 
  - fun FoldableStatusCard(state: String) 
  - fun TestSensorsCard(onTest: () -> Unit) 
  - fun SensorItemCard(sensor: SensorInfo) 
  - fun WiFiSection(
  - fun BluetoothSection(
  - fun UWBStatusCard() 
  - fun AudioDeviceCard(device: AudioDeviceInfo) 
  - fun DisplaySpecsCard(displayInfo: DisplayInfo?) 
  - fun XRCapabilitiesCard() 
  - fun SensorSpec(label: String, value: String) 
  - fun NetworkItem(name: String, icon: ImageVector) 
  - fun DisplaySpecRow(label: String, value: String) 
  - fun XRFeatureRow(feature: String, supported: Boolean) 
  - fun getBatteryColor(level: Int): Color 
  - fun getSensorColor(type: Int): Color 
  - fun getSensorIcon(type: Int): ImageVector 
  - fun getNetworkIcon(type: String): ImageVector 

**Imports:**
  - import android.os.Bundle
  - import androidx.activity.ComponentActivity
  - import androidx.activity.compose.setContent
  - import androidx.activity.viewModels
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.*
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.*
  - import androidx.compose.foundation.lazy.grid.*
  - ... and 25 more

---

### File: `src/main/java/com/augmentalis/devicemanager/dashboardui/DeviceManagerSimple.kt`

**Package:** `com.augmentalis.devicemanager.dashboardui`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun SimpleDeviceManagerUI(

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.*
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.text.font.FontWeight
  - import androidx.compose.ui.unit.dp
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/devicemanager/dashboardui/DeviceViewModel.kt`

**Package:** `com.augmentalis.devicemanager.dashboardui`

**Classes/Interfaces/Objects:**
  - data class HardwareInfo(
  - data class BatteryInfo(
  - data class SensorInfo(
  - data class NetworkConnectionInfo(
  - data class AudioDeviceInfo(
  - data class DisplayInfo(
  - class DeviceViewModel(private val context
  - class DeviceViewModelFactory(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.hardware.Sensor
  - import android.hardware.SensorManager
  - import android.net.ConnectivityManager
  - import android.net.NetworkCapabilities
  - import android.os.BatteryManager
  - import android.os.Build
  - import android.util.Log
  - import androidx.lifecycle.LiveData
  - import androidx.lifecycle.MutableLiveData
  - ... and 17 more

---

### File: `src/main/java/com/augmentalis/devicemanager/dashboardui/FeedbackUI.kt`

**Package:** `com.augmentalis.devicemanager.dashboardui`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun FeedbackSettingsScreen(
  - fun FeedbackOverviewTab(
  - fun HapticSettingsTab(
  - fun AudioSettingsTab(
  - fun VisualSettingsTab(
  - fun FeedbackPresetsTab(
  - fun FeedbackStatusGrid(feedbackState: FeedbackState) 
  - fun QuickActionsCard(feedbackManager: FeedbackManager) 
  - fun FeedbackTestCard(feedbackManager: FeedbackManager) 
  - fun FeedbackTestTypeCard(
  - fun FeedbackPresetButtons(feedbackManager: FeedbackManager) 
  - fun FeedbackCard(
  - fun StatusChip(name: String, enabled: Boolean, icon: ImageVector) 

**Imports:**
  - import androidx.compose.animation.*
  - import androidx.compose.foundation.*
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.*
  - import androidx.compose.foundation.shape.CircleShape
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - ... and 13 more

---

### File: `src/main/java/com/augmentalis/devicemanager/dashboardui/GlassmorphismUtils.kt`

**Package:** `com.augmentalis.devicemanager.dashboardui`

**Classes/Interfaces/Objects:**
  - data class GlassMorphismConfig(
  - object DeviceColors 
  - object DeviceGlassConfigs 

**Public Functions:**
  - fun Modifier.glassMorphism(

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.border
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.draw.blur
  - import androidx.compose.ui.graphics.Brush
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.Dp
  - import androidx.compose.ui.unit.dp

---

### File: `src/main/java/com/augmentalis/devicemanager/DeviceInfo.kt`

**Package:** `com.augmentalis.devicemanager`

**Classes/Interfaces/Objects:**
  - class DeviceInfo(private val context
  - data class DeviceProfile(
  - data class DisplayProfile(
  - data class HardwareInfo(
  - data class ScalingProfile(
  - data class CameraInfo(
  - enum class CameraFacing 
  - data class USBDeviceInfo(
  - data class ExternalDisplay(
  - enum class DisplayConnectionType 
  - data class InputDeviceInfo(
  - data class MotionRangeInfo(
  - data class ConnectedDevice(
  - enum class DeviceType 
  - enum class ConnectionType 
  - object UsbConstants 

**Public Functions:**

**Imports:**
  - import android.content.BroadcastReceiver
  - import android.content.Context
  - import android.content.Context.RECEIVER_EXPORTED
  - import android.content.Intent
  - import android.content.IntentFilter
  - import android.content.res.Configuration
  - import android.hardware.Camera
  - import android.hardware.Sensor
  - import android.hardware.SensorManager
  - import android.hardware.camera2.CameraCharacteristics
  - ... and 20 more

---

### File: `src/main/java/com/augmentalis/devicemanager/deviceinfo/cache/DeviceInfoCache.kt`

**Package:** `com.augmentalis.devicemanager.deviceinfo.cache`

**Classes/Interfaces/Objects:**
  - class DeviceInfoCache(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import com.augmentalis.devicemanager.*
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext
  - import kotlinx.serialization.Serializable
  - import kotlinx.serialization.json.Json
  - import kotlinx.serialization.encodeToString
  - import kotlinx.serialization.decodeFromString
  - import java.io.File
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/devicemanager/deviceinfo/certification/CertificationDetector.kt`

**Package:** `com.augmentalis.devicemanager.deviceinfo.certification`

**Classes/Interfaces/Objects:**
  - class CertificationDetector(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.os.Build
  - import android.util.Log
  - import org.json.JSONObject
  - import java.io.File
  - import java.util.Properties

---

### File: `src/main/java/com/augmentalis/devicemanager/deviceinfo/detection/DeviceDetection.kt`

**Package:** `com.augmentalis.devicemanager.deviceinfo.detection`

**Classes/Interfaces/Objects:**
  - class DeviceDetection(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.content.res.Configuration
  - import android.hardware.Sensor
  - import android.hardware.SensorManager
  - import android.hardware.camera2.CameraCharacteristics
  - import android.hardware.camera2.CameraManager
  - import android.hardware.display.DisplayManager
  - import android.hardware.usb.UsbDevice
  - import android.hardware.usb.UsbManager
  - ... and 8 more

---

### File: `src/main/java/com/augmentalis/devicemanager/deviceinfo/detection/DeviceDetector.kt`

**Package:** `com.augmentalis.devicemanager.deviceinfo.detection`

**Classes/Interfaces/Objects:**
  - object DeviceDetector 

**Public Functions:**

**Imports:**
  - import android.Manifest
  - import android.annotation.SuppressLint
  - import android.bluetooth.BluetoothAdapter
  - import android.bluetooth.BluetoothManager
  - import android.bluetooth.BluetoothProfile
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.hardware.Camera
  - import android.hardware.Sensor
  - import android.hardware.SensorManager
  - ... and 18 more

---

### File: `src/main/java/com/augmentalis/devicemanager/deviceinfo/detection/manufacturers/SamsungDetector.kt`

**Package:** `com.augmentalis.devicemanager.deviceinfo.detection.manufacturers`

**Classes/Interfaces/Objects:**
  - object SamsungDetector 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.os.Build
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/devicemanager/deviceinfo/detection/smartglass/RealWearDetector.kt`

**Package:** `com.augmentalis.devicemanager.deviceinfo.detection.smartglass`

**Classes/Interfaces/Objects:**
  - object RealWearDetector 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.os.Build
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/devicemanager/deviceinfo/detection/smartglass/VuzixDetector.kt`

**Package:** `com.augmentalis.devicemanager.deviceinfo.detection.smartglass`

**Classes/Interfaces/Objects:**
  - object VuzixDetector 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.os.Build
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/devicemanager/deviceinfo/detection/SmartGlassDetection.kt`

**Package:** `com.augmentalis.devicemanager.deviceinfo.detection`

**Classes/Interfaces/Objects:**
  - class SmartGlassDetection(private val context
  - enum class SmartGlassType 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.os.Build
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/devicemanager/deviceinfo/manufacturer/ManufacturerDetection.kt`

**Package:** `com.augmentalis.devicemanager.deviceinfo.manufacturer`

**Classes/Interfaces/Objects:**
  - class ManufacturerDetection(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.os.Build
  - import android.util.Log
  - import java.lang.reflect.Method

---

### File: `src/main/java/com/augmentalis/devicemanager/DeviceManager.kt`

**Package:** `com.augmentalis.devicemanager`

**Classes/Interfaces/Objects:**
  - class DeviceManager(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import androidx.lifecycle.DefaultLifecycleObserver
  - import androidx.lifecycle.LifecycleOwner
  - import com.augmentalis.devicemanager.audio.AudioService
  - import com.augmentalis.devicemanager.display.DisplayOverlayManager
  - import com.augmentalis.devicemanager.network.BluetoothManager
  - import com.augmentalis.devicemanager.network.WiFiManager
  - import com.augmentalis.devicemanager.network.UwbManager
  - import com.augmentalis.devicemanager.network.NfcManager
  - ... and 15 more

---

### File: `src/main/java/com/augmentalis/devicemanager/display/DisplayOverlayManager.kt`

**Package:** `com.augmentalis.devicemanager.display`

**Classes/Interfaces/Objects:**
  - class DisplayOverlayManager(private val context
  - enum class DisplayMode 
  - enum class SmartGlassesType 
  - enum class OverlayPosition 
  - data class DisplayConfig(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.PixelFormat
  - import android.hardware.display.DisplayManager
  - import android.os.Build
  - import android.provider.Settings
  - import android.view.Display
  - import android.view.Gravity
  - import android.view.View
  - import android.view.WindowManager
  - import android.widget.FrameLayout
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/devicemanager/imu/IMUPublicAPI.kt`

**Package:** `com.augmentalis.devicemanager.imu`

**Classes/Interfaces/Objects:**
  - class IMUPublicAPI(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.devicemanager.sensors.imu.*
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.cancel
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.map
  - import kotlinx.coroutines.launch

---

### File: `src/main/java/com/augmentalis/devicemanager/network/BluetoothManager.kt`

**Package:** `com.augmentalis.devicemanager.network`

**Classes/Interfaces/Objects:**
  - class BluetoothManager(

**Public Functions:**

**Imports:**
  - import android.Manifest
  - import android.annotation.SuppressLint
  - import android.bluetooth.*
  - import android.bluetooth.le.*
  - import android.content.BroadcastReceiver
  - import android.content.Context
  - import android.content.Context.RECEIVER_EXPORTED
  - import android.content.Intent
  - import android.content.IntentFilter
  - import android.media.AudioManager
  - ... and 9 more

---

### File: `src/main/java/com/augmentalis/devicemanager/network/CellularManager.kt`

**Package:** `com.augmentalis.devicemanager.network`

**Classes/Interfaces/Objects:**
  - class CellularManager(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.os.Build
  - import android.telephony.TelephonyManager
  - import android.util.Log
  - import androidx.annotation.RequiresPermission
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow

---

### File: `src/main/java/com/augmentalis/devicemanager/network/NfcManager.kt`

**Package:** `com.augmentalis.devicemanager.network`

**Classes/Interfaces/Objects:**
  - class NfcManager(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.nfc.NfcAdapter
  - import android.nfc.NfcManager
  - import android.os.Build
  - import android.util.Log
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow

---

### File: `src/main/java/com/augmentalis/devicemanager/network/UsbNetworkManager.kt`

**Package:** `com.augmentalis.devicemanager.network`

**Classes/Interfaces/Objects:**
  - class UsbNetworkManager(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.net.ConnectivityManager
  - import android.net.LinkProperties
  - import android.net.Network
  - import android.net.NetworkCapabilities
  - import android.os.Build
  - import android.util.Log
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow

---

### File: `src/main/java/com/augmentalis/devicemanager/network/UwbManager.kt`

**Package:** `com.augmentalis.devicemanager.network`

**Classes/Interfaces/Objects:**
  - class UwbManager(

**Public Functions:**

**Imports:**
  - import android.Manifest
  - import android.annotation.SuppressLint
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.hardware.Sensor
  - import android.hardware.SensorManager
  - import android.os.Build
  - import android.util.Log
  - import androidx.annotation.RequiresApi
  - import androidx.annotation.RequiresPermission
  - ... and 9 more

---

### File: `src/main/java/com/augmentalis/devicemanager/network/WiFiManager.kt`

**Package:** `com.augmentalis.devicemanager.network`

**Classes/Interfaces/Objects:**
  - class WiFiManager(

**Public Functions:**

**Imports:**
  - import android.Manifest
  - import android.annotation.SuppressLint  
  - import android.content.BroadcastReceiver
  - import android.content.Context
  - import android.content.Context.RECEIVER_EXPORTED
  - import android.content.Intent
  - import android.content.IntentFilter
  - import android.content.pm.PackageManager
  - import android.hardware.display.DisplayManager
  - import android.view.Display
  - ... and 15 more

---

### File: `src/main/java/com/augmentalis/devicemanager/profile/HardwareProfiler.kt`

**Package:** `com.augmentalis.devicemanager.profile`

**Classes/Interfaces/Objects:**
  - class HardwareProfiler(private val context
  - data class HardwareProfile(
  - data class DeviceInfoProfile(
  - data class CpuInfo(
  - data class MemoryInfo(
  - data class GpuInfo(
  - data class StorageInfo(
  - data class CapabilityMatrix(
  - enum class PerformanceClass 

**Public Functions:**

**Imports:**
  - import android.app.ActivityManager
  - import android.content.Context
  - import android.os.Build
  - import android.os.Environment
  - import android.os.StatFs
  - import android.util.Log
  - import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext
  - import org.json.JSONArray
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/devicemanager/security/BiometricManager.kt`

**Package:** `com.augmentalis.devicemanager.security`

**Classes/Interfaces/Objects:**
  - class BiometricManager(

**Public Functions:**

**Imports:**
  - import android.Manifest
  - import android.annotation.SuppressLint
  - import android.app.KeyguardManager
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.hardware.biometrics.BiometricManager as AndroidBiometricManager
  - import android.hardware.biometrics.BiometricPrompt
  - import android.hardware.fingerprint.FingerprintManager
  - import android.os.Build
  - import android.os.CancellationSignal
  - ... and 20 more

---

### File: `src/main/java/com/augmentalis/devicemanager/sensors/imu/AdaptiveFilter.kt`

**Package:** `com.augmentalis.devicemanager.sensors.imu`

**Classes/Interfaces/Objects:**
  - class AdaptiveFilter 
  - data class FilterState(
  - data class FilterConfig(

**Public Functions:**

**Imports:**
  - import kotlin.math.*

---

### File: `src/main/java/com/augmentalis/devicemanager/sensors/imu/CalibrationManager.kt`

**Package:** `com.augmentalis.devicemanager.sensors.imu`

**Classes/Interfaces/Objects:**
  - class CalibrationManager 
  - data class MovementRange(

**Public Functions:**

**Imports:**
  - import kotlinx.coroutines.CancellationException
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.cancel
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.launch
  - import kotlinx.coroutines.withContext

---

### File: `src/main/java/com/augmentalis/devicemanager/sensors/imu/CursorAdapter.kt`

**Package:** `com.augmentalis.devicemanager.sensors.imu`

**Classes/Interfaces/Objects:**
  - class CursorAdapter(
  - data class CursorPosition(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.cancel
  - import kotlinx.coroutines.cancelChildren
  - import kotlinx.coroutines.channels.BufferOverflow
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.SharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/devicemanager/sensors/imu/EnhancedSensorFusion.kt`

**Package:** `com.augmentalis.devicemanager.sensors.imu`

**Classes/Interfaces/Objects:**
  - class EnhancedSensorFusion 
  - class SimpleKalmanFilter 

**Public Functions:**

**Imports:**
  - import kotlin.math.*

---

### File: `src/main/java/com/augmentalis/devicemanager/sensors/imu/IMUDataPool.kt`

**Package:** `com.augmentalis.devicemanager.sensors.imu`

**Classes/Interfaces/Objects:**
  - class IMUDataPool(capacity
  - data class IMUData(var alpha

**Public Functions:**

**Imports:**
  - import java.util.LinkedList

---

### File: `src/main/java/com/augmentalis/devicemanager/sensors/imu/IMUManager.kt`

**Package:** `com.augmentalis.devicemanager.sensors.imu`

**Classes/Interfaces/Objects:**
  - class IMUManager private constructor(
  - data class OrientationData(
  - data class MotionData(
  - data class SensorCapabilities(
  - data class CalibrationResult(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.Context.WINDOW_SERVICE
  - import android.hardware.Sensor
  - import android.hardware.SensorEvent
  - import android.hardware.SensorEventListener
  - import android.hardware.SensorManager
  - import android.util.Log
  - import android.view.Display
  - import android.view.Surface
  - import android.view.WindowManager
  - ... and 11 more

---

### File: `src/main/java/com/augmentalis/devicemanager/sensors/imu/IMUMathUtils.kt`

**Package:** `com.augmentalis.devicemanager.sensors.imu`

**Classes/Interfaces/Objects:**
  - data class Vector3(
  - data class Quaternion(
  - data class EulerAngles(

**Public Functions:**
  - fun slerp(a: Quaternion, b: Quaternion, t: Float): Quaternion 
  - fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 
  - fun angularDistance(a: Quaternion, b: Quaternion): Float 

**Imports:**
  - import kotlin.math.*

---

### File: `src/main/java/com/augmentalis/devicemanager/sensors/imu/MotionPredictor.kt`

**Package:** `com.augmentalis.devicemanager.sensors.imu`

**Classes/Interfaces/Objects:**
  - class MotionPredictor 
  - class MotionFilter 
  - class AngularVelocityTracker 

**Public Functions:**

**Imports:**
  - import kotlin.math.*

---

### File: `src/main/java/com/augmentalis/devicemanager/sensors/imu/MovingAverage.kt`

**Package:** `com.augmentalis.devicemanager.sensors.imu`

**Classes/Interfaces/Objects:**
  - class MovingAverage(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/devicemanager/sensors/LidarManager.kt`

**Package:** `com.augmentalis.devicemanager.sensors`

**Classes/Interfaces/Objects:**
  - class LidarManager(

**Public Functions:**

**Imports:**
  - import android.Manifest
  - import android.annotation.SuppressLint
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.graphics.ImageFormat
  - import android.graphics.PointF
  - import android.hardware.Sensor
  - import android.hardware.SensorEvent
  - import android.hardware.SensorEventListener
  - import android.hardware.SensorManager
  - ... and 21 more

---

### File: `src/main/java/com/augmentalis/devicemanager/sensors/SensorFusionManager.kt`

**Package:** `com.augmentalis.devicemanager.sensors`

**Classes/Interfaces/Objects:**
  - class SensorFusionManager(private val context
  - enum class FusionMode 
  - data class FusedOrientation(
  - data class FusedMotion(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.hardware.Sensor
  - import android.hardware.SensorEvent
  - import android.hardware.SensorEventListener
  - import android.hardware.SensorManager
  - import android.util.Log
  - import com.augmentalis.devicemanager.sensors.imu.EulerAngles
  - import com.augmentalis.devicemanager.sensors.imu.Quaternion
  - import com.augmentalis.devicemanager.sensors.imu.Vector3
  - import com.augmentalis.devicemanager.sensors.imu.slerp
  - ... and 9 more

---

### File: `src/main/java/com/augmentalis/devicemanager/smartdevices/FoldableDeviceManager.kt`

**Package:** `com.augmentalis.devicemanager.smartdevices`

**Classes/Interfaces/Objects:**
  - class FoldableDeviceManager(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.res.Configuration
  - import android.graphics.Rect
  - import android.hardware.Sensor
  - import android.hardware.SensorEvent
  - import android.hardware.SensorEventListener
  - import android.hardware.SensorManager
  - import android.os.Build
  - import android.util.Log
  - import android.view.Surface
  - ... and 6 more

---

### File: `src/main/java/com/augmentalis/devicemanager/smartglasses/GlassesManager.kt`

**Package:** `com.augmentalis.devicemanager.smartglasses`

**Classes/Interfaces/Objects:**
  - class GlassesManager(
  - enum class GlassDisplayMode 
  - data class GlassesCapabilities(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector
  - import com.augmentalis.devicemanager.deviceinfo.detection.SmartGlassType
  - import com.augmentalis.devicemanager.deviceinfo.detection.SmartGlassDetection
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow

---

### File: `src/main/java/com/augmentalis/devicemanager/smartglasses/XRManager.kt`

**Package:** `com.augmentalis.devicemanager.smartglasses`

**Classes/Interfaces/Objects:**
  - class XRManager(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.os.Build
  - import android.util.Log
  - import kotlinx.coroutines.flow.*

---

### File: `src/main/java/com/augmentalis/devicemanager/usb/USBDeviceMonitor.kt`

**Package:** `com.augmentalis.devicemanager.usb`

**Classes/Interfaces/Objects:**
  - class USBDeviceMonitor(private val context

**Public Functions:**

**Imports:**
  - import android.content.BroadcastReceiver
  - import android.content.Context
  - import android.content.Context.RECEIVER_EXPORTED
  - import android.content.Intent
  - import android.content.IntentFilter
  - import android.hardware.usb.UsbDevice
  - import android.hardware.usb.UsbManager
  - import android.os.Build
  - import android.util.Log
  - import kotlinx.coroutines.*
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/devicemanager/uwb/UWBDetector.kt`

**Package:** `com.augmentalis.devicemanager.uwb`

**Classes/Interfaces/Objects:**
  - class UWBDetector(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.os.Build
  - import android.util.Log
  - import androidx.annotation.RequiresApi
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow

---

### File: `src/main/java/com/augmentalis/devicemanager/video/VideoManager.kt`

**Package:** `com.augmentalis.devicemanager.video`

**Classes/Interfaces/Objects:**
  - class VideoManager(private val context
  - enum class CameraFacing 
  - data class CameraDetails(
  - data class CameraCapabilities(
  - enum class CameraState 
  - enum class RecordingState 
  - enum class HardwareLevel 
  - enum class FlashMode 
  - enum class VideoProfile(val width
  - enum class VideoFilter 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.ImageFormat
  - import android.hardware.camera2.*
  - import android.media.*
  - import android.os.Build
  - import android.os.Handler
  - import android.os.HandlerThread
  - import android.util.Log
  - import android.util.Size
  - import android.view.Surface
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/devicemanager/wifi/WiFiPublicAPI.kt`

**Package:** `com.augmentalis.devicemanager.wifi`

**Classes/Interfaces/Objects:**
  - class WiFiPublicAPI(

**Public Functions:**

**Imports:**
  - import android.Manifest
  - import android.annotation.SuppressLint
  - import android.content.Context
  - import android.net.wifi.ScanResult
  - import android.net.wifi.WifiInfo
  - import android.os.Build
  - import android.util.Log
  - import androidx.annotation.RequiresPermission
  - import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector
  - import com.augmentalis.devicemanager.network.WiFiManager
  - ... and 6 more

---

### File: `src/test/java/com/augmentalis/devicemanager/sensors/imu/CursorAdapterMathTest.kt`

**Package:** `com.augmentalis.devicemanager.sensors.imu`

**Classes/Interfaces/Objects:**
  - class CursorAdapterMathTest 

**Public Functions:**

**Imports:**
  - import org.junit.*
  - import org.junit.Assert.*
  - import kotlin.math.*

---

### File: `src/test/java/com/augmentalis/devicemanager/sensors/imu/CursorAdapterTest.kt`

**Package:** `com.augmentalis.devicemanager.sensors.imu`

**Classes/Interfaces/Objects:**
  - class CursorAdapterTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import io.mockk.*
  - import io.mockk.impl.annotations.MockK
  - import io.mockk.impl.annotations.SpyK
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.*
  - import kotlinx.coroutines.test.*
  - import org.junit.*
  - import org.junit.Assert.*
  - ... and 4 more

---

### File: `src/test/java/com/augmentalis/devicemanager/ui/DeviceViewModelTest.kt`

**Package:** `com.augmentalis.devicemanager.ui`

**Classes/Interfaces/Objects:**
  - class DeviceViewModelTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.hardware.SensorManager
  - import android.net.ConnectivityManager
  - import android.os.BatteryManager
  - import androidx.arch.core.executor.testing.InstantTaskExecutorRule
  - import androidx.lifecycle.Observer
  - import kotlinx.coroutines.ExperimentalCoroutinesApi
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.test.StandardTestDispatcher
  - import kotlinx.coroutines.test.TestCoroutineScheduler
  - ... and 25 more

---

## Summary

**Total Files:** 68

**Module Structure:**
```
                  build
                    generated
                      ap_generated_sources
                        debug
                          out
                            androidx
                              databinding
                                library
                                  baseAdapters
                            com
                              augmentalis
                                devicemanager
                      data_binding_base_class_source_out
                        debug
                          out
                      data_binding_trigger
                        debug
                          com
                            augmentalis
                              devicemanager
                      res
                        pngs
                          debug
                        resValues
                          debug
                    intermediates
                      aapt_friendly_merged_manifests
                        debug
                          processDebugManifest
                            aapt
                      aar_metadata
                        debug
                          writeDebugAarMetadata
                      annotation_processor_list
                        debug
                          javaPreCompileDebug
                      compile_library_classes_jar
                        debug
                          bundleLibCompileToJarDebug
                      compile_r_class_jar
                        debug
                          generateDebugRFile
                      compile_symbol_list
                        debug
                          generateDebugRFile
                      compiled_local_resources
                        debug
                          compileDebugLibraryResources
                            out
                      data_binding_artifact
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
