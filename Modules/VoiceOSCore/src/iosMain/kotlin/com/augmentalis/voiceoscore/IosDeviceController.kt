/**
 * IosDeviceController.kt - iOS implementation of IDeviceController
 *
 * Provides device hardware control for iOS:
 * - Volume: Read-only via AVAudioSession.outputVolume (setting system volume
 *   requires MPVolumeView slider manipulation — not exposed as a clean API)
 * - Brightness: Full read/write via UIScreen.mainScreen.brightness
 * - Flashlight: Full control via AVCaptureDevice torch mode
 * - Screen on/off: Not available from app sandbox
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.hasTorch
import platform.AVFoundation.isTorchAvailable
import platform.AVFoundation.torchMode
import platform.Foundation.NSLog
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
class IosDeviceController : IDeviceController {

    override fun setVolume(level: Int): Boolean {
        // iOS does not provide a public API to set system volume programmatically.
        // MPVolumeView contains a hidden UISlider that can be manipulated, but this
        // requires adding it to the view hierarchy — not suitable for a non-UI controller.
        NSLog("IosDeviceController: setVolume($level) not available — iOS has no public volume-setting API")
        return false
    }

    override fun getVolume(): Int {
        // AVAudioSession.outputVolume is a KVO-only property not directly
        // accessible via K/N ObjC interop. Return -1 to indicate unavailable.
        // Real volume reading requires MPVolumeView or KVO observer from Swift.
        NSLog("IosDeviceController: getVolume() not available via K/N — requires Swift KVO bridge")
        return -1
    }

    override fun setBrightness(level: Int): Boolean {
        return try {
            val brightness = (level.coerceIn(0, 100) / 100.0)
            UIScreen.mainScreen.setBrightness(brightness)
            NSLog("IosDeviceController: setBrightness($level) -> $brightness")
            true
        } catch (e: Exception) {
            NSLog("IosDeviceController: setBrightness() failed: ${e.message}")
            false
        }
    }

    override fun getBrightness(): Int {
        return try {
            (UIScreen.mainScreen.brightness * 100).toInt().coerceIn(0, 100)
        } catch (e: Exception) {
            NSLog("IosDeviceController: getBrightness() failed: ${e.message}")
            50
        }
    }

    override fun setScreenState(on: Boolean): Boolean {
        NSLog("IosDeviceController: setScreenState($on) not available — iOS does not allow screen on/off from app sandbox")
        return false
    }

    override fun setFlashlight(on: Boolean): Boolean {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        if (device == null) {
            NSLog("IosDeviceController: No video capture device available")
            return false
        }
        if (!device.hasTorch || !device.isTorchAvailable()) {
            NSLog("IosDeviceController: Device does not have a torch or torch is unavailable")
            return false
        }

        return try {
            device.lockForConfiguration(null)
            device.torchMode = if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff
            device.unlockForConfiguration()
            NSLog("IosDeviceController: setFlashlight($on) success")
            true
        } catch (e: Exception) {
            NSLog("IosDeviceController: setFlashlight() failed: ${e.message}")
            false
        }
    }
}
