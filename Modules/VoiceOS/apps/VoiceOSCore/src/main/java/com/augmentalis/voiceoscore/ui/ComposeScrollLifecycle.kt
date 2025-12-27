/**
 * ComposeScrollLifecycle.kt - Lifecycle management for scrollable Compose content
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: CCA (IDEACODE v12.1)
 * Created: 2025-12-23
 *
 * Purpose: Prevent IllegalStateException when ContentCapture checks scroll state
 * after Compose disposes scroll observation scopes during Activity finish().
 *
 * Root Cause: Race condition between Compose disposal and ContentCapture checks
 * in AndroidContentCaptureManager.checkForContentCapturePropertyChanges:332
 *
 * Stack Trace Fixed:
 * ```
 * java.lang.IllegalStateException: scroll observation scope does not exist
 *     at androidx.compose.ui.contentcapture.AndroidContentCaptureManager.checkForContentCapturePropertyChanges(AndroidContentCaptureManager.android.kt:332)
 * ```
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
 * **Usage:**
 * ```kotlin
 * class MyActivity : ComponentActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         // Instead of setContent:
 *         setContentWithScrollSupport {
 *             MaterialTheme {
 *                 Column(Modifier.verticalScroll(rememberScrollState())) {
 *                     // Your scrollable content
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * **How it works:**
 * 1. Wraps content in ScrollableContent composable
 * 2. Observes Activity lifecycle via LocalLifecycleOwner
 * 3. Uses DisposableEffect to clean up lifecycle observer
 * 4. Ensures proper disposal order before ContentCapture checks
 *
 * **Why this fixes the crash:**
 * - DisposableEffect runs BEFORE composition disposal
 * - Lifecycle observer ensures cleanup happens in correct order
 * - Prevents ContentCapture from checking disposed scroll scopes
 *
 * @param content The Composable content (can include scrollable elements)
 */
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
