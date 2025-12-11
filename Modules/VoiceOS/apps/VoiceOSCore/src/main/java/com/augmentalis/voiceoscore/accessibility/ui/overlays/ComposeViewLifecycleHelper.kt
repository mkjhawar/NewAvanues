/**
 * ComposeViewLifecycleHelper.kt - Helper for ComposeView lifecycle management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-10
 *
 * Purpose: Provides lifecycle management for ComposeView used in system overlays.
 * Fixes: ViewTreeLifecycleOwner not found crash when ComposeView is added to WindowManager.
 */
package com.augmentalis.voiceoscore.accessibility.ui.overlays

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Custom LifecycleOwner for ComposeView overlays.
 *
 * This class provides the required lifecycle management for ComposeView instances
 * that are added directly to WindowManager (not part of an Activity/Fragment).
 */
class ComposeViewLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    /**
     * Initialize lifecycle and move to RESUMED state.
     * Call this when creating the ComposeView.
     */
    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    /**
     * Destroy lifecycle and move to DESTROYED state.
     * Call this when disposing the ComposeView.
     */
    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

/**
 * Helper to create a properly configured ComposeView for system overlays.
 *
 * Usage:
 * ```kotlin
 * val (composeView, lifecycleOwner) = createOverlayComposeView(context) {
 *     MyComposableContent()
 * }
 *
 * // Add to WindowManager
 * windowManager.addView(composeView, params)
 *
 * // Later, when hiding/disposing:
 * windowManager.removeView(composeView)
 * lifecycleOwner.onDestroy()
 * ```
 *
 * @param context Android context
 * @param content Composable content to render
 * @return Pair of ComposeView and its LifecycleOwner
 */
fun createOverlayComposeView(
    context: Context,
    content: @Composable () -> Unit
): Pair<ComposeView, ComposeViewLifecycleOwner> {
    val lifecycleOwner = ComposeViewLifecycleOwner().apply {
        onCreate()
    }

    val composeView = ComposeView(context).apply {
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        setContent(content)
    }

    return Pair(composeView, lifecycleOwner)
}

/**
 * Extension function to set up lifecycle for an existing ComposeView.
 *
 * Usage:
 * ```kotlin
 * val lifecycleOwner = composeView.setupLifecycle {
 *     MyComposableContent()
 * }
 * ```
 *
 * @param content Composable content to render
 * @return ComposeViewLifecycleOwner that was created
 */
fun ComposeView.setupLifecycle(
    content: @Composable () -> Unit
): ComposeViewLifecycleOwner {
    val lifecycleOwner = ComposeViewLifecycleOwner().apply {
        onCreate()
    }

    setViewTreeLifecycleOwner(lifecycleOwner)
    setViewTreeSavedStateRegistryOwner(lifecycleOwner)
    setContent(content)

    return lifecycleOwner
}
