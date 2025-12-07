/**
 * LearnAppTestReceiver.kt - Broadcast receiver for LearnApp test automation
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/receivers/LearnAppTestReceiver.kt
 *
 * Author: VoiceOS Test Automation
 * Created: 2025-11-28
 *
 * Broadcast receiver that allows external test scripts (like the emulator test script)
 * to trigger LearnApp exploration via ADB broadcasts.
 *
 * Usage from ADB:
 * ```bash
 * adb shell am broadcast \
 *   -a com.augmentalis.voiceos.LEARNAPP_START \
 *   -e target_package "com.google.android.gm"
 * ```
 */

package com.augmentalis.voiceoscore.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.augmentalis.voiceoscore.learnapp.integration.LearnAppIntegration

/**
 * Broadcast receiver for triggering LearnApp exploration via ADB
 *
 * This receiver enables test automation by allowing external scripts
 * to trigger LearnApp exploration programmatically.
 *
 * ## Intent Format
 *
 * **Action:** `com.augmentalis.voiceos.LEARNAPP_START`
 *
 * **Extras:**
 * - `target_package` (String, required): Package name of app to learn
 *
 * ## Example Usage
 *
 * ### From ADB:
 * ```bash
 * adb shell am broadcast \
 *   -a com.augmentalis.voiceos.LEARNAPP_START \
 *   -e target_package "com.google.android.gm"
 * ```
 *
 * ### From Kotlin:
 * ```kotlin
 * val intent = Intent("com.augmentalis.voiceos.LEARNAPP_START").apply {
 *     putExtra("target_package", "com.google.android.gm")
 * }
 * context.sendBroadcast(intent)
 * ```
 *
 * @since 1.0.0
 */
class LearnAppTestReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "LearnAppTestReceiver"

        /**
         * Action for starting LearnApp exploration
         */
        const val ACTION_LEARNAPP_START = "com.augmentalis.voiceos.LEARNAPP_START"

        /**
         * Extra key for target package name
         */
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        // Verify action
        if (intent.action != ACTION_LEARNAPP_START) {
            Log.w(TAG, "Ignoring unknown action: ${intent.action}")
            return
        }

        // Get target package
        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        if (targetPackage.isNullOrBlank()) {
            Log.e(TAG, "Missing or empty target_package extra")
            return
        }

        Log.i(TAG, "Triggering LearnApp for package: $targetPackage")

        // Get LearnAppIntegration instance and trigger learning
        try {
            val integration = try {
                LearnAppIntegration.getInstance()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "LearnAppIntegration not initialized. VoiceOS Accessibility Service must be enabled.")
                Log.e(TAG, "To enable: Settings → Accessibility → VoiceOS → Enable")
                return
            }

            // Trigger learning
            integration.triggerLearning(targetPackage)
            Log.i(TAG, "LearnApp triggered successfully for $targetPackage")

            // Log completion marker for test script to detect
            Log.i("LearnApp", "LearnApp complete for $targetPackage")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger LearnApp for $targetPackage", e)
        }
    }
}
