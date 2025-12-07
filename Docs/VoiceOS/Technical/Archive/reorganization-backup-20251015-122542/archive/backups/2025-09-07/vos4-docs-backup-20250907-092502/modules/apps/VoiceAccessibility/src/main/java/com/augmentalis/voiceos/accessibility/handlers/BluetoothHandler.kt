/**
 * BluetoothHandler.kt - Bluetooth device control handler
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Migration Team
 * Created: 2025-09-03
 * 
 * Handles Bluetooth device control commands including enable/disable,
 * settings navigation, and device management.
 * 
 * Migrated from Legacy Avenue BluetoothAction.kt with VOS4 patterns
 */
package com.augmentalis.voiceos.accessibility.handlers

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService

/**
 * Handler for Bluetooth-related voice commands
 * Provides device connectivity control and settings access
 */
class BluetoothHandler(
    private val service: VoiceAccessibilityService
) : ActionHandler {

    companion object {
        private const val TAG = "BluetoothHandler"
        
        // Supported actions
        private val SUPPORTED_ACTIONS = listOf(
            "bluetooth_on",
            "bluetooth_off", 
            "bluetooth_enable",
            "bluetooth_disable",
            "bluetooth_toggle",
            "bluetooth_settings",
            "pair_device",
            "bluetooth_status"
        )
    }

    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun initialize() {
        Log.d(TAG, "Initializing BluetoothHandler")
        
        try {
            val bluetoothManager = service.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter
            
            if (bluetoothAdapter == null) {
                Log.w(TAG, "Bluetooth adapter not available on this device")
            } else {
                Log.d(TAG, "Bluetooth adapter initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Bluetooth adapter", e)
        }
    }

    override fun canHandle(action: String): Boolean {
        return SUPPORTED_ACTIONS.any { supportedAction ->
            action.contains(supportedAction, ignoreCase = true) ||
            supportedAction.contains(action.replace(" ", "_"), ignoreCase = true)
        }
    }

    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS
    }

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        Log.d(TAG, "Executing Bluetooth action: $action")

        return when {
            // Enable Bluetooth commands
            action.contains("bluetooth_on", ignoreCase = true) ||
            action.contains("bluetooth_enable", ignoreCase = true) ||
            action.contains("turn on bluetooth", ignoreCase = true) ||
            action.contains("enable bluetooth", ignoreCase = true) -> {
                enableBluetooth(true)
            }

            // Disable Bluetooth commands  
            action.contains("bluetooth_off", ignoreCase = true) ||
            action.contains("bluetooth_disable", ignoreCase = true) ||
            action.contains("turn off bluetooth", ignoreCase = true) ||
            action.contains("disable bluetooth", ignoreCase = true) -> {
                enableBluetooth(false)
            }

            // Toggle Bluetooth
            action.contains("bluetooth_toggle", ignoreCase = true) ||
            action.contains("toggle bluetooth", ignoreCase = true) -> {
                toggleBluetooth()
            }

            // Open Bluetooth settings
            action.contains("bluetooth_settings", ignoreCase = true) ||
            action.contains("bluetooth setup", ignoreCase = true) ||
            action.contains("pair device", ignoreCase = true) -> {
                openBluetoothSettings()
            }

            // Check Bluetooth status
            action.contains("bluetooth_status", ignoreCase = true) ||
            action.contains("bluetooth state", ignoreCase = true) -> {
                checkBluetoothStatus()
            }

            else -> {
                Log.w(TAG, "Unknown Bluetooth action: $action")
                false
            }
        }
    }

    /**
     * Enable or disable Bluetooth
     */
    private fun enableBluetooth(enable: Boolean): Boolean {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter not available")
            openBluetoothSettings() // Fallback to settings
            return true // Consider it handled by opening settings
        }

        return try {
            // Check permissions for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    service, 
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasPermission) {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not granted, opening settings")
                    openBluetoothSettings()
                    return true
                }
            }

            val currentState = bluetoothAdapter!!.state == BluetoothAdapter.STATE_ON
            
            if (enable && !currentState) {
                Log.i(TAG, "Enabling Bluetooth")
                // For API 33+, we can't directly enable/disable Bluetooth
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    openBluetoothSettings()
                } else {
                    @Suppress("DEPRECATION", "MissingPermission")
                    bluetoothAdapter!!.enable()
                }
                true
            } else if (!enable && currentState) {
                Log.i(TAG, "Disabling Bluetooth")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    openBluetoothSettings()
                } else {
                    @Suppress("DEPRECATION", "MissingPermission")
                    bluetoothAdapter!!.disable()
                }
                true
            } else {
                val stateMessage = if (currentState) "already enabled" else "already disabled"
                Log.d(TAG, "Bluetooth is $stateMessage")
                true // Still considered successful
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception accessing Bluetooth", e)
            openBluetoothSettings() // Fallback
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error controlling Bluetooth", e)
            openBluetoothSettings() // Fallback
            true
        }
    }

    /**
     * Toggle Bluetooth state
     */
    private fun toggleBluetooth(): Boolean {
        if (bluetoothAdapter == null) {
            openBluetoothSettings()
            return true
        }

        return try {
            val currentState = bluetoothAdapter!!.state == BluetoothAdapter.STATE_ON
            enableBluetooth(!currentState)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling Bluetooth", e)
            openBluetoothSettings()
            true
        }
    }

    /**
     * Open Bluetooth settings
     */
    private fun openBluetoothSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            service.startActivity(intent)
            Log.d(TAG, "Opened Bluetooth settings")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Bluetooth settings", e)
            
            // Fallback to general settings
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                service.startActivity(fallbackIntent)
                Log.d(TAG, "Opened general settings as fallback")
                true
            } catch (e2: Exception) {
                Log.e(TAG, "Error opening fallback settings", e2)
                false
            }
        }
    }

    /**
     * Check and announce Bluetooth status
     */
    private fun checkBluetoothStatus(): Boolean {
        if (bluetoothAdapter == null) {
            Log.i(TAG, "Bluetooth not available on this device")
            // TODO: Integrate with TTS to announce status
            return true
        }

        return try {
            val isEnabled = bluetoothAdapter!!.state == BluetoothAdapter.STATE_ON
            val statusMessage = if (isEnabled) "Bluetooth is enabled" else "Bluetooth is disabled"
            Log.i(TAG, statusMessage)
            
            // TODO: Integrate with TTS to announce status
            // service.announceFeedback(statusMessage)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Bluetooth status", e)
            false
        }
    }

    override fun dispose() {
        Log.d(TAG, "Disposing BluetoothHandler")
        bluetoothAdapter = null
    }
}