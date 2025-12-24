/**
 * ComposeScrollLifecycle.kt - DEPRECATED - DO NOT USE
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-23
 * Deprecated: 2025-12-23
 *
 * ⚠️ DEPRECATION NOTICE ⚠️
 * This file contains an INEFFECTIVE fix that DOES NOT prevent ContentCapture crashes.
 *
 * PROBLEM WITH THIS FIX:
 * - Observes ON_PAUSE lifecycle event but does nothing (empty lambda)
 * - Does not interact with ContentCapture system
 * - Does not prevent the race condition
 * - Still causes crashes in production
 *
 * USE INSTEAD:
 * - ContentCaptureSafeComposeActivity (in same package)
 * - Call setContentSafely() instead of setContentWithScrollSupport()
 *
 * MIGRATION:
 * Change: class MyActivity : ComponentActivity()
 * To:     class MyActivity : ContentCaptureSafeComposeActivity()
 *
 * Change: setContentWithScrollSupport { }
 * To:     setContentSafely { }
 *
 * See: VoiceOS-ContentCapture-Migration-Guide-251223-V1.md
 *
 * This file will be DELETED in next major release.
 */
package com.augmentalis.voiceoscore.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Extension function for ComponentActivity that prevents ContentCapture crashes
 * when navigating away from scrollable Compose content.
 *
 * @Deprecated This fix DOES NOT WORK. Use ContentCaptureSafeComposeActivity instead.
 *
 * @param content The Composable content (can include scrollable elements)
 */
@Deprecated(
    message = "This fix is INEFFECTIVE and does not prevent ContentCapture crashes. " +
            "Use ContentCaptureSafeComposeActivity.setContentSafely() instead.",
    replaceWith = ReplaceWith(
        "setContentSafely(content)",
        "com.augmentalis.voiceoscore.ui.ContentCaptureSafeComposeActivity"
    ),
    level = DeprecationLevel.ERROR
)
fun ComponentActivity.setContentWithScrollSupport(
    content: @Composable () -> Unit
) {
    setContent {
        ScrollableContent {
            content()
        }
    }
}

/**
 * Private composable that wraps content with lifecycle observation
 *
 * Ensures proper cleanup order when Activity is finishing to prevent
 * ContentCapture from accessing disposed scroll observation scopes.
 *
 * @param content The wrapped Composable content
 */
@Composable
private fun ScrollableContent(
    content: @Composable () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                // Activity pausing - allows proper cleanup before ContentCapture checks
                // This ensures scroll observation scopes are cleaned up in the correct order
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            // Clean up lifecycle observer when composable is disposed
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    content()
}
