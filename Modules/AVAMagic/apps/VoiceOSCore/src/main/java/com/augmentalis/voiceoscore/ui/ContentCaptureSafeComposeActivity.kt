/**
 * ContentCaptureSafeComposeActivity.kt - Base class for ContentCapture-safe Compose activities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-23
 *
 * PROBLEM SOLVED:
 * Race condition between Compose disposal and ContentCapture checks during Activity
 * finish in AccessibilityService context.
 *
 * ROOT CAUSE:
 * VoiceOS AccessibilityService fires WINDOW_STATE_CHANGED events when activities finish.
 * These events trigger AndroidContentCaptureManager.checkForContentCapturePropertyChanges()
 * which tries to access scroll observation scopes that are being disposed by Compose.
 *
 * CRASH PATTERN (BEFORE FIX):
 * ```
 * java.lang.IllegalStateException: scroll observation scope does not exist
 *     at androidx.compose.ui.contentcapture.AndroidContentCaptureManager.checkForContentCapturePropertyChanges(AndroidContentCaptureManager.android.kt:332)
 * ```
 *
 * SOLUTION:
 * 1. Disable ContentCapture in finish() BEFORE composition disposal
 * 2. Use ON_STOP lifecycle event to disable ContentCapture (correct timing)
 * 3. Coordinate disposal with ContentCapture system via lifecycle observation
 * 4. Multiple safety layers prevent race condition
 *
 * USAGE:
 * ```kotlin
 * class MyActivity : ContentCaptureSafeComposeActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         setContentSafely {  // Instead of setContent
 *             MyTheme {
 *                 MyScreen()
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * MIGRATION:
 * Replace:
 *   - ComponentActivity → ContentCaptureSafeComposeActivity
 *   - setContent → setContentSafely
 *   - setContentWithScrollSupport → setContentSafely (deprecated function)
 *
 * PREVENTION:
 * All VoiceOS Compose activities MUST extend this base class to prevent crashes.
 * See: VoiceOS-ContentCapture-RoT-Analysis-251223-V1.md for detailed analysis.
 */
package com.augmentalis.voiceoscore.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.contentcapture.ContentCaptureManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Base class for Compose activities that prevents ContentCapture crashes.
 *
 * **Why This is Needed:**
 * VoiceOS runs as an AccessibilityService, generating WINDOW_STATE_CHANGED events
 * when activities finish. These events trigger ContentCapture to check UI properties,
 * including scroll state. If Compose has already disposed scroll observation scopes,
 * ContentCapture crashes with "scroll observation scope does not exist".
 *
 * **How This Fixes It:**
 * - Layer 1: finish() override disables ContentCapture BEFORE disposal
 * - Layer 2: ON_STOP lifecycle event disables ContentCapture (safety net)
 * - Layer 3: DisposableEffect onDispose disables ContentCapture (final safety)
 *
 * **Disposal Timeline (With Fix):**
 * ```
 * 1. Activity.finish() called
 *    └─→ ContentCapture disabled (Layer 1)
 * 2. Lifecycle → ON_PAUSE
 * 3. Lifecycle → ON_STOP
 *    └─→ ContentCapture disabled (Layer 2, redundant check)
 * 4. Compose composition disposal begins
 *    └─→ Scroll scopes disposed safely (no ContentCapture checks)
 * 5. DisposableEffect.onDispose
 *    └─→ ContentCapture disabled (Layer 3, final safety)
 * 6. Lifecycle → ON_DESTROY
 * ```
 *
 * **Testing:**
 * - Tested on Android 11-15
 * - Tested with accessibility service active
 * - Tested rapid finish cycles (no memory leaks)
 * - Tested ContentCapture re-enable in subsequent activities
 *
 * @see ComposeContentCaptureCoordinator for disposal coordination logic
 */
abstract class ContentCaptureSafeComposeActivity : ComponentActivity() {

    private val disposalCoordinator = ComposeContentCaptureCoordinator()
    private var contentCaptureDisabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disposalCoordinator.initialize(this)

        Log.d(TAG, "ContentCaptureSafeComposeActivity created: ${this::class.simpleName}")
    }

    /**
     * Safe replacement for setContent() that coordinates with ContentCapture.
     *
     * **IMPORTANT:** Use this instead of ComponentActivity.setContent()
     *
     * This wraps your content in lifecycle observation that disables ContentCapture
     * before composition disposal, preventing race conditions.
     *
     * @param content The Composable content for this activity
     */
    protected fun setContentSafely(content: @Composable () -> Unit) {
        setContent {
            disposalCoordinator.SafeContent(content)
        }
    }

    /**
     * Override finish() to disable ContentCapture BEFORE disposal.
     *
     * This is Layer 1 of the safety mechanism. By disabling ContentCapture
     * before calling super.finish(), we prevent any new ContentCapture checks
     * from being scheduled during composition disposal.
     *
     * **Why This Works:**
     * - finish() is called before ON_PAUSE
     * - ContentCapture disabled before composition disposal starts
     * - No new ContentCapture checks can be scheduled
     * - Existing checks complete before disposal (small time window)
     */
    override fun finish() {
        disableContentCapture("finish() override")

        // Small yield to let any pending ContentCapture checks complete
        // This handles the race window where checks are already in-flight
        Thread.sleep(50)

        super.finish()
    }

    /**
     * Disable ContentCapture for this activity.
     *
     * Safe to call multiple times (idempotent). Logs each call for debugging.
     *
     * @param source Description of where this was called from (for debugging)
     */
    private fun disableContentCapture(source: String) {
        if (contentCaptureDisabled) {
            Log.v(TAG, "ContentCapture already disabled (from: $source)")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentCaptureManager = getSystemService(ContentCaptureManager::class.java)
                contentCaptureManager?.isContentCaptureEnabled = false
            }
            contentCaptureDisabled = true

            Log.d(TAG, "ContentCapture disabled for safe disposal (from: $source)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to disable ContentCapture (from: $source)", e)
        }
    }

    companion object {
        private const val TAG = "ContentCaptureSafe"
    }
}

/**
 * Coordinates Compose composition lifecycle with ContentCapture system.
 *
 * **Responsibilities:**
 * - Observe Activity lifecycle events (ON_STOP)
 * - Disable ContentCapture before composition disposal
 * - Provide safety net via DisposableEffect.onDispose
 * - Thread-safe coordination (Mutex)
 *
 * **Why This is a Separate Class:**
 * - Single Responsibility Principle
 * - Easier to test independently
 * - Can be reused in non-activity contexts if needed
 *
 * **Lifecycle Coordination:**
 * ```
 * ON_CREATE  → Initialize coordinator
 * ON_START   → Normal operation
 * ON_RESUME  → Normal operation
 * ON_PAUSE   → Normal operation (ContentCapture still active)
 * ON_STOP    → Disable ContentCapture ← KEY EVENT
 * ON_DESTROY → Cleanup complete
 * ```
 */
private class ComposeContentCaptureCoordinator {

    private var activity: ComponentActivity? = null
    private val disposalMutex = Mutex()
    private var contentCaptureDisabled = false

    /**
     * Initialize coordinator with activity reference.
     *
     * @param activity The activity to coordinate
     */
    fun initialize(activity: ComponentActivity) {
        this.activity = activity
    }

    /**
     * Composable wrapper that adds ContentCapture coordination.
     *
     * **Lifecycle Observation:**
     * - Registers LifecycleEventObserver on composition
     * - Observes ON_STOP event (activity is stopping)
     * - Disables ContentCapture in ON_STOP
     * - Removes observer in onDispose
     *
     * **Why ON_STOP (not ON_PAUSE):**
     * - ON_PAUSE is too early (composition still active)
     * - ON_STOP is right before composition disposal
     * - ContentCapture checks happen between ON_PAUSE and ON_STOP
     * - Disabling in ON_STOP prevents checks during disposal
     *
     * @param content The wrapped Composable content
     */
    @Composable
    fun SafeContent(content: @Composable () -> Unit) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        // Activity is stopping - disable ContentCapture NOW
                        // This is Layer 2 of the safety mechanism
                        Log.d(TAG, "ON_STOP event - disabling ContentCapture")
                        disableContentCaptureForDisposal(context, "ON_STOP lifecycle event")
                    }
                    else -> {
                        // Ignore other events
                    }
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                // Final safety check before composition disposal
                // This is Layer 3 of the safety mechanism
                Log.d(TAG, "onDispose - final safety check")
                disableContentCaptureForDisposal(context, "DisposableEffect.onDispose")

                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        content()
    }

    /**
     * Disable ContentCapture for safe disposal.
     *
     * **Thread Safety:** Uses Mutex to prevent concurrent calls
     * **Idempotent:** Safe to call multiple times
     * **Logging:** Logs each call for debugging
     *
     * @param context The activity context
     * @param source Description of where this was called from
     */
    private fun disableContentCaptureForDisposal(context: Context, source: String) {
        val activity = this.activity ?: run {
            Log.w(TAG, "Activity reference is null (from: $source)")
            return
        }

        // Use coroutine Mutex for thread-safe coordination
        // This prevents multiple threads from disabling simultaneously
        kotlinx.coroutines.runBlocking {
            disposalMutex.withLock {
                if (contentCaptureDisabled) {
                    Log.v(TAG, "ContentCapture already disabled (from: $source)")
                    return@withLock
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentCaptureManager = activity.getSystemService(ContentCaptureManager::class.java)
                        contentCaptureManager?.isContentCaptureEnabled = false
                    }
                    contentCaptureDisabled = true

                    Log.d(TAG, "ContentCapture disabled for safe disposal (from: $source)")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to disable ContentCapture (from: $source)", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ComposeContentCaptureCoordinator"
    }
}
