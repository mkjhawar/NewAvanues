/**
 * AccessibilityServiceState.kt - Composable state hook for accessibility service
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Remembers the accessibility service enabled state with lifecycle awareness.
 * Automatically updates when app resumes, ensuring the UI reflects the current
 * state after the user returns from accessibility settings.
 *
 * @param context Application or activity context for checking accessibility settings
 * @return MutableState containing current accessibility service enabled status
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val context = LocalContext.current
 *     val isServiceEnabled = rememberAccessibilityServiceState(context)
 *
 *     if (isServiceEnabled.value) {
 *         // Show active UI
 *     } else {
 *         // Show setup prompt
 *     }
 * }
 * ```
 */
@Composable
fun rememberAccessibilityServiceState(context: Context): MutableState<Boolean> {
    val state = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initial check on first composition
    LaunchedEffect(Unit) {
        state.value = AccessibilityServiceHelper.isVoiceOSServiceEnabled(context)
    }

    // Update on lifecycle resume events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.value = AccessibilityServiceHelper.isVoiceOSServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return state
}

/**
 * Remembers the microphone permission state with lifecycle awareness.
 * Automatically updates when app resumes.
 *
 * @param context Application or activity context for checking permission
 * @return MutableState containing current microphone permission status
 */
@Composable
fun rememberMicrophonePermissionState(context: Context): MutableState<Boolean> {
    val state = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initial check on first composition
    LaunchedEffect(Unit) {
        state.value = AccessibilityServiceHelper.isMicrophonePermissionGranted(context)
    }

    // Update on lifecycle resume events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.value = AccessibilityServiceHelper.isMicrophonePermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return state
}
