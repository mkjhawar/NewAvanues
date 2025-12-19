package com.augmentalis.actions.handlers

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import com.augmentalis.actions.ActionResult
import com.augmentalis.actions.IntentActionHandler

/**
 * Action handler for Bluetooth control.
 *
 * Handles turning Bluetooth on/off.
 *
 * Note: Due to Android permissions, this typically opens Bluetooth settings
 * rather than directly toggling, as direct toggle requires BLUETOOTH_ADMIN permission.
 */
class BluetoothOnActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "BluetoothOnHandler"
    }

    override val intent = "bluetooth_on"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Turning Bluetooth on for utterance: '$utterance'")

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter == null) {
                return ActionResult.Failure(
                    message = "This device doesn't support Bluetooth"
                )
            }

            if (bluetoothAdapter.isEnabled) {
                return ActionResult.Success(
                    message = "Bluetooth is already on"
                )
            }

            // Open Bluetooth settings (direct toggle requires BLUETOOTH_ADMIN)
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            Log.i(TAG, "Opened Bluetooth settings")
            ActionResult.Success(
                message = "Opening Bluetooth settings - please turn it on"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to control Bluetooth", e)
            ActionResult.Failure(
                message = "Failed to control Bluetooth: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for turning Bluetooth off.
 */
class BluetoothOffActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "BluetoothOffHandler"
    }

    override val intent = "bluetooth_off"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Turning Bluetooth off for utterance: '$utterance'")

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter == null) {
                return ActionResult.Failure(
                    message = "This device doesn't support Bluetooth"
                )
            }

            if (!bluetoothAdapter.isEnabled) {
                return ActionResult.Success(
                    message = "Bluetooth is already off"
                )
            }

            // Open Bluetooth settings
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            Log.i(TAG, "Opened Bluetooth settings")
            ActionResult.Success(
                message = "Opening Bluetooth settings - please turn it off"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to control Bluetooth", e)
            ActionResult.Failure(
                message = "Failed to control Bluetooth: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for WiFi control.
 */
class WifiOnActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "WifiOnHandler"
    }

    override val intent = "wifi_on"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Turning WiFi on for utterance: '$utterance'")

            // Open WiFi settings (direct toggle deprecated in Android Q+)
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            Log.i(TAG, "Opened WiFi settings")
            ActionResult.Success(
                message = "Opening WiFi settings"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to control WiFi", e)
            ActionResult.Failure(
                message = "Failed to control WiFi: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for turning WiFi off.
 */
class WifiOffActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "WifiOffHandler"
    }

    override val intent = "wifi_off"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Turning WiFi off for utterance: '$utterance'")

            // Open WiFi settings
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            Log.i(TAG, "Opened WiFi settings")
            ActionResult.Success(
                message = "Opening WiFi settings"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to control WiFi", e)
            ActionResult.Failure(
                message = "Failed to control WiFi: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for volume up.
 */
class VolumeUpActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "VolumeUpHandler"
    }

    override val intent = "volume_up"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Increasing volume for utterance: '$utterance'")

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.adjustVolume(android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI)

            Log.i(TAG, "Volume increased")
            ActionResult.Success(
                message = "Volume increased"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to increase volume", e)
            ActionResult.Failure(
                message = "Failed to increase volume: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for volume down.
 */
class VolumeDownActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "VolumeDownHandler"
    }

    override val intent = "volume_down"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Decreasing volume for utterance: '$utterance'")

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.adjustVolume(android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI)

            Log.i(TAG, "Volume decreased")
            ActionResult.Success(
                message = "Volume decreased"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrease volume", e)
            ActionResult.Failure(
                message = "Failed to decrease volume: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for muting volume.
 */
class VolumeMuteActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "VolumeMuteHandler"
    }

    override val intent = "volume_mute"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Muting volume for utterance: '$utterance'")

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.adjustVolume(android.media.AudioManager.ADJUST_MUTE, android.media.AudioManager.FLAG_SHOW_UI)

            Log.i(TAG, "Volume muted")
            ActionResult.Success(
                message = "Volume muted"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mute volume", e)
            ActionResult.Failure(
                message = "Failed to mute volume: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for battery status.
 */
class BatteryStatusActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "BatteryStatusHandler"
    }

    override val intent = "battery_status"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Getting battery status for utterance: '$utterance'")

            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = batteryManager.isCharging

            val status = if (isCharging) "charging" else "not charging"

            Log.i(TAG, "Battery: $batteryLevel%, $status")
            ActionResult.Success(
                message = "Battery is at $batteryLevel% and $status"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get battery status", e)
            ActionResult.Failure(
                message = "Failed to get battery status: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for flashlight/torch.
 */
class FlashlightOnActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "FlashlightOnHandler"
    }

    override val intent = "flashlight_on"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Turning flashlight on for utterance: '$utterance'")

            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, true)

            Log.i(TAG, "Flashlight turned on")
            ActionResult.Success(
                message = "Flashlight turned on"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn on flashlight", e)
            ActionResult.Failure(
                message = "Failed to turn on flashlight: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for turning flashlight off.
 */
class FlashlightOffActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "FlashlightOffHandler"
    }

    override val intent = "flashlight_off"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Turning flashlight off for utterance: '$utterance'")

            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, false)

            Log.i(TAG, "Flashlight turned off")
            ActionResult.Success(
                message = "Flashlight turned off"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn off flashlight", e)
            ActionResult.Failure(
                message = "Failed to turn off flashlight: ${e.message}",
                exception = e
            )
        }
    }
}
