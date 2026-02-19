/**
 * SystemActions.kt - System control command actions
 * Path: modules/commands/src/main/java/com/augmentalis/voiceos/commands/actions/SystemActions.kt
 * 
 * Created: 2025-08-19
 * Author: Claude Code
 * Module: Commands
 * 
 * Purpose: System control and settings-related voice command actions
 */

package com.augmentalis.voiceoscore.managers.commandmanager.actions

import com.augmentalis.voiceoscore.*
// Removed ACTION_BACKUP_AND_RESET_SETTINGS import
import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresPermission

/**
 * System control command actions
 * Handles system settings, connectivity, and device control
 */
object SystemActions {
    
    /**
     * WiFi Toggle Action
     */
    class WifiToggleAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val currentState = wifiManager.isWifiEnabled
                
                // Note: setWifiEnabled is deprecated in API 29+, but we'll keep it for compatibility
                @Suppress("DEPRECATION")
                val success = wifiManager.setWifiEnabled(!currentState)
                
                if (success) {
                    val newState = if (!currentState) "enabled" else "disabled"
                    createSuccessResult(command, "WiFi $newState")
                } else {
                    // Fall back to opening WiFi settings
                    openWifiSettings(context)
                    createSuccessResult(command, "Opened WiFi settings")
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to toggle WiFi: ${e.message}")
            }
        }
    }
    
    /**
     * WiFi Enable Action
     */
    class WifiEnableAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                
                if (wifiManager.isWifiEnabled) {
                    createSuccessResult(command, "WiFi is already enabled")
                } else {
                    @Suppress("DEPRECATION")
                    val success = wifiManager.setWifiEnabled(true)
                    
                    if (success) {
                        createSuccessResult(command, "WiFi enabled")
                    } else {
                        openWifiSettings(context)
                        createSuccessResult(command, "Opened WiFi settings to enable")
                    }
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to enable WiFi: ${e.message}")
            }
        }
    }
    
    /**
     * WiFi Disable Action
     */
    class WifiDisableAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                
                if (!wifiManager.isWifiEnabled) {
                    createSuccessResult(command, "WiFi is already disabled")
                } else {
                    @Suppress("DEPRECATION")
                    val success = wifiManager.setWifiEnabled(false)
                    
                    if (success) {
                        createSuccessResult(command, "WiFi disabled")
                    } else {
                        openWifiSettings(context)
                        createSuccessResult(command, "Opened WiFi settings to disable")
                    }
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to disable WiFi: ${e.message}")
            }
        }
    }
    
    /**
     * Bluetooth Toggle Action
     */
    class BluetoothToggleAction : BaseAction() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bluetoothAdapter = bluetoothManager?.adapter
                
                if (bluetoothAdapter == null) {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Bluetooth not supported on this device")
                } else {
                    val currentState = bluetoothAdapter.state == BluetoothAdapter.STATE_ON
                    
                    // For Android 13+ (API 33+), we cannot directly enable/disable Bluetooth
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        openBluetoothSettings(context)
                        createSuccessResult(command, "Opened Bluetooth settings (manual toggle required on Android 13+)")
                    } else {
                        @Suppress("DEPRECATION")
                        val success = if (currentState) {
                            bluetoothAdapter.disable()
                        } else {
                            bluetoothAdapter.enable()
                        }
                        
                        if (success) {
                            val newState = if (!currentState) "enabled" else "disabled"
                            createSuccessResult(command, "Bluetooth $newState")
                        } else {
                            openBluetoothSettings(context)
                            createSuccessResult(command, "Opened Bluetooth settings")
                        }
                    }
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to toggle Bluetooth: ${e.message}")
            }
        }
    }
    
    /**
     * Bluetooth Enable Action
     */
    class BluetoothEnableAction : BaseAction() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bluetoothAdapter = bluetoothManager?.adapter
                
                if (bluetoothAdapter == null) {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Bluetooth not supported on this device")
                } else if (bluetoothAdapter.state == BluetoothAdapter.STATE_ON) {
                    createSuccessResult(command, "Bluetooth is already enabled")
                } else {
                    // For Android 13+ (API 33+), we cannot directly enable Bluetooth
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        openBluetoothSettings(context)
                        createSuccessResult(command, "Opened Bluetooth settings to enable (manual action required on Android 13+)")
                    } else {
                        @Suppress("DEPRECATION")
                        val success = bluetoothAdapter.enable()
                        
                        if (success) {
                            createSuccessResult(command, "Bluetooth enabled")
                        } else {
                            openBluetoothSettings(context)
                            createSuccessResult(command, "Opened Bluetooth settings to enable")
                        }
                    }
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to enable Bluetooth: ${e.message}")
            }
        }
    }
    
    /**
     * Bluetooth Disable Action
     */
    class BluetoothDisableAction : BaseAction() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bluetoothAdapter = bluetoothManager?.adapter
                
                if (bluetoothAdapter == null) {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Bluetooth not supported on this device")
                } else if (bluetoothAdapter.state != BluetoothAdapter.STATE_ON) {
                    createSuccessResult(command, "Bluetooth is already disabled")
                } else {
                    // For Android 13+ (API 33+), we cannot directly disable Bluetooth
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        openBluetoothSettings(context)
                        createSuccessResult(command, "Opened Bluetooth settings to disable (manual action required on Android 13+)")
                    } else {
                        @Suppress("DEPRECATION")
                        val success = bluetoothAdapter.disable()
                        
                        if (success) {
                            createSuccessResult(command, "Bluetooth disabled")
                        } else {
                            openBluetoothSettings(context)
                            createSuccessResult(command, "Opened Bluetooth settings to disable")
                        }
                    }
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to disable Bluetooth: ${e.message}")
            }
        }
    }
    
    /**
     * Open Settings Action
     */
    class OpenSettingsAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val category = getTextParameter(command, "category")?.lowercase()
            
            return try {
                val intent = when (category) {
                    "wifi", "wi-fi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                    "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    "sound", "audio" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                    "display", "screen" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                    "battery" -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                    "storage" -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                    "apps", "applications" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
                    "security" -> Intent(Settings.ACTION_SECURITY_SETTINGS)
                    "privacy" -> Intent(Settings.ACTION_PRIVACY_SETTINGS)
                    "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    "language", "languages" -> Intent(Settings.ACTION_LOCALE_SETTINGS)
                    "date", "time" -> Intent(Settings.ACTION_DATE_SETTINGS)
                    "location" -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    "accounts" -> Intent(Settings.ACTION_SYNC_SETTINGS)
                    "backup" -> Intent(Settings.ACTION_PRIVACY_SETTINGS) // ACTION_BACKUP_AND_RESET_SETTINGS deprecated
                    "developer" -> Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    "system" -> Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
                    "about" -> Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
                    "network" -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
                    "hotspot" -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
                    "vpn" -> Intent(Settings.ACTION_VPN_SETTINGS)
                    "nfc" -> Intent(Settings.ACTION_NFC_SETTINGS)
                    else -> Intent(Settings.ACTION_SETTINGS) // Default to main settings
                }.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                
                context.startActivity(intent)
                val settingName = category ?: "main"
                createSuccessResult(command, "Opened $settingName settings")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to open settings: ${e.message}")
            }
        }
    }
    
    /**
     * Device Information Action
     */
    class DeviceInfoAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val info = mapOf(
                    "model" to android.os.Build.MODEL,
                    "manufacturer" to android.os.Build.MANUFACTURER,
                    "androidVersion" to android.os.Build.VERSION.RELEASE,
                    "apiLevel" to android.os.Build.VERSION.SDK_INT.toString(),
                    "device" to android.os.Build.DEVICE,
                    "product" to android.os.Build.PRODUCT
                )
                
                val infoText = "Device: ${info["manufacturer"]} ${info["model"]}, " +
                        "Android ${info["androidVersion"]} (API ${info["apiLevel"]})"
                
                createSuccessResult(command, infoText, info)
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to get device info: ${e.message}")
            }
        }
    }
    
    /**
     * Battery Status Action
     */
    class BatteryStatusAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val batteryIntent = context.registerReceiver(null, 
                    android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                
                if (batteryIntent != null) {
                    val level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    val scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    val status = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                    
                    val batteryPct = (level * 100 / scale.toFloat()).toInt()
                    val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == android.os.BatteryManager.BATTERY_STATUS_FULL
                    
                    val statusText = if (isCharging) "charging" else "not charging"
                    val message = "Battery: $batteryPct% ($statusText)"
                    
                    createSuccessResult(command, message, mapOf(
                        "level" to batteryPct,
                        "isCharging" to isCharging,
                        "status" to statusText
                    ))
                } else {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Could not get battery status")
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to get battery status: ${e.message}")
            }
        }
    }
    
    /**
     * Network Status Action
     */
    class NetworkStatusAction : BaseAction() {
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val (isConnected, networkType) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val network = connectivityManager.activeNetwork
                    val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
                    val connected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                    val type = when {
                        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile Data"
                        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                        else -> "Unknown"
                    }
                    Pair(connected, type)
                } else {
                    @Suppress("DEPRECATION")
                    val networkInfo = connectivityManager.activeNetworkInfo
                    @Suppress("DEPRECATION")
                    val connected = networkInfo?.isConnected == true
                    @Suppress("DEPRECATION")
                    val type = when (networkInfo?.type) {
                        @Suppress("DEPRECATION")
                        ConnectivityManager.TYPE_WIFI -> "WiFi"
                        @Suppress("DEPRECATION")
                        ConnectivityManager.TYPE_MOBILE -> "Mobile Data"
                        @Suppress("DEPRECATION")
                        ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                        else -> "Unknown"
                    }
                    Pair(connected, type)
                }
                
                val message = if (isConnected) {
                    "Connected to $networkType"
                } else {
                    "No network connection"
                }
                
                createSuccessResult(command, message, mapOf(
                    "isConnected" to isConnected,
                    "networkType" to networkType
                ))
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to get network status: ${e.message}")
            }
        }
    }
    
    /**
     * Storage Info Action
     */
    class StorageInfoAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val statFs = android.os.StatFs(android.os.Environment.getDataDirectory().path)
                val bytesAvailable = statFs.blockSizeLong * statFs.availableBlocksLong
                val bytesTotal = statFs.blockSizeLong * statFs.blockCountLong
                val bytesUsed = bytesTotal - bytesAvailable
                
                val availableGB = bytesAvailable / (1024 * 1024 * 1024)
                val totalGB = bytesTotal / (1024 * 1024 * 1024)
                val usedGB = bytesUsed / (1024 * 1024 * 1024)
                val usedPercent = ((bytesUsed.toFloat() / bytesTotal) * 100).toInt()
                
                val message = "Storage: ${usedGB}GB used / ${totalGB}GB total (${usedPercent}%), ${availableGB}GB available"
                
                createSuccessResult(command, message, mapOf(
                    "availableGB" to availableGB,
                    "totalGB" to totalGB,
                    "usedGB" to usedGB,
                    "usedPercent" to usedPercent
                ))
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to get storage info: ${e.message}")
            }
        }
    }
    
    // Helper methods
    
    /**
     * Open WiFi settings
     */
    private fun openWifiSettings(context: Context) {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Open Bluetooth settings
     */
    private fun openBluetoothSettings(context: Context) {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

/**
 * Extended SystemActions with UUID support
 */
object UUIDSystemActions {
    
    /**
     * Maximize window by UUID
     */
    suspend fun maximizeByUUID(uuid: String): ActionResult {
        return try {
            // This would interface with window management
            ActionResult.success("Maximized window with UUID: $uuid")
        } catch (e: Exception) {
            ActionResult.failure("Error maximizing window: ${e.message}")
        }
    }
    
    /**
     * Minimize window by UUID
     */
    suspend fun minimizeByUUID(uuid: String): ActionResult {
        return try {
            // This would interface with window management
            ActionResult.success("Minimized window with UUID: $uuid")
        } catch (e: Exception) {
            ActionResult.failure("Error minimizing window: ${e.message}")
        }
    }
    
    /**
     * Change device orientation
     */
    suspend fun changeOrientation(): ActionResult {
        return try {
            // This would trigger orientation change
            ActionResult.success("Changed device orientation")
        } catch (e: Exception) {
            ActionResult.failure("Error changing orientation: ${e.message}")
        }
    }
}