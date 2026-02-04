/**
 * ResponsiveContext.android.kt
 * Android implementation using DeviceManager
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-23
 * Version: 1.0.0
 */

package com.augmentalis.avamagic.responsive

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.augmentalis.devicemanager.DeviceManager
import com.augmentalis.devicemanager.smartdevices.FoldableDeviceManager

/**
 * Remember responsive context for Android using DeviceManager
 * Recomposes when configuration changes (rotation, fold state, etc.)
 */
@Composable
actual fun rememberResponsiveContext(): ResponsiveContext {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val deviceManager = remember {
        DeviceManager.getInstance(context.applicationContext)
    }

    // Get device metrics from DeviceManager
    val metrics by remember(configuration) {
        derivedStateOf {
            val displayProfile = deviceManager.info.getDisplayProfile()
            DeviceMetrics(
                widthPixels = displayProfile.widthPixels,
                heightPixels = displayProfile.heightPixels,
                widthDp = displayProfile.widthPixels / displayProfile.density,
                heightDp = displayProfile.heightPixels / displayProfile.density,
                density = displayProfile.density,
                densityDpi = displayProfile.densityDpi,
                diagonalInches = displayProfile.diagonalInches
            )
        }
    }

    // Calculate breakpoint from metrics
    val breakpoint by remember(metrics) {
        derivedStateOf {
            when {
                metrics.widthDp < 600 -> Breakpoint.XS
                metrics.widthDp < 840 -> Breakpoint.SM
                metrics.widthDp < 1240 -> Breakpoint.MD
                metrics.widthDp < 1440 -> Breakpoint.LG
                else -> Breakpoint.XL
            }
        }
    }

    // Get device type from DeviceManager
    val deviceType by remember {
        derivedStateOf {
            when {
                deviceManager.info.isXR() -> DeviceType.XR
                deviceManager.info.isTV() -> DeviceType.TV
                deviceManager.info.isFoldable() -> DeviceType.FOLDABLE
                deviceManager.info.isWearable() -> DeviceType.WEARABLE
                deviceManager.info.isTablet() -> DeviceType.TABLET
                deviceManager.info.isAutomotive() -> DeviceType.DESKTOP
                else -> DeviceType.PHONE
            }
        }
    }

    // Get fold state if device is foldable
    val foldState by remember(configuration) {
        derivedStateOf {
            if (!deviceManager.info.isFoldable()) {
                null
            } else {
                try {
                    val foldableManager = FoldableDeviceManager(context.applicationContext)
                    foldableManager.initialize()

                    FoldState(
                        isFoldable = true,
                        hingeAngle = foldableManager.getHingeAngle(),
                        isOpen = foldableManager.getFoldState() == FoldableDeviceManager.FoldState.OPEN,
                        shouldAvoidCrease = foldableManager.shouldAvoidCrease()
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    return remember(metrics, breakpoint, deviceType, foldState) {
        ResponsiveContext(
            breakpoint = breakpoint,
            deviceType = deviceType,
            metrics = metrics,
            foldState = foldState
        )
    }
}
