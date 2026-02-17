/**
 * HeadTrackingBridge.kt - Maps IMU orientation data to cursor input
 *
 * Extension function that converts DeviceManager IMU data flow into
 * the KMP CursorInput.HeadMovement flow consumed by CursorController.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voicecursor.input

import com.augmentalis.devicemanager.imu.IMUManager
import com.augmentalis.voicecursor.core.CursorInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Convert IMU orientation flow to cursor head movement input.
 *
 * IMUData axes mapping (from IMUManager.processRotationVector):
 * - alpha = orientationBuffer[2] = roll  (rotation around X-axis)
 * - beta  = -orientationBuffer[1] = pitch (rotation around Y-axis)
 * - gamma = orientationBuffer[0] = yaw   (rotation around Z-axis / azimuth)
 *
 * CursorInput.HeadMovement axes:
 * - pitch = up/down head tilt     -> mapped from beta
 * - yaw   = left/right head turn  -> mapped from gamma
 * - roll  = head tilt sideways    -> mapped from alpha
 */
fun IMUManager.toCursorInputFlow(): Flow<CursorInput.HeadMovement> =
    orientationFlow.map { data ->
        CursorInput.HeadMovement(
            pitch = data.beta,
            yaw = data.gamma,
            roll = data.alpha
        )
    }
