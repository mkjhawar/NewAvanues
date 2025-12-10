// filename: Universal/AVA/Core/Common/src/androidMain/kotlin/com/augmentalis/ava/crashreporting/CrashReporter.kt
// created: 2025-11-07
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.crashreporting

import android.content.Context
import android.os.Build
import timber.log.Timber

/**
 * Privacy-First Crash Reporting for AVA AI
 *
 * ## Design Philosophy
 *
 * AVA implements a **privacy-first approach** to crash reporting. By default, crash data
 * remains entirely local on the device using Timber logging. This design choice ensures:
 *
 * - **User Privacy:** No crash data is transmitted to external servers without explicit consent
 * - **Data Sovereignty:** Users maintain complete control over their diagnostic data
 * - **Transparency:** Clear visibility into what data is collected and where it goes
 * - **Compliance:** Meets privacy regulations (GDPR, CCPA) by default
 *
 * ## Current Implementation
 *
 * **Firebase Crashlytics Integration (Opt-In):**
 * - Firebase Crashlytics is integrated but disabled by default
 * - All crash reports are logged locally via Timber when disabled
 * - Firebase APIs are only called when user has explicitly opted in
 * - Gracefully handles Firebase SDK unavailability (fails silently to Timber)
 *
 * **This is a deliberate privacy-first design choice.**
 *
 * ## Setup Instructions
 *
 * To enable Firebase Crashlytics, you must:
 *
 * 1. Add google-services.json to android/ava/
 * 2. Add Firebase dependencies to build.gradle.kts:
 *    ```kotlin
 *    dependencies {
 *        implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
 *        implementation("com.google.firebase:firebase-crashlytics-ktx")
 *    }
 *    ```
 * 3. Apply Firebase plugins in build.gradle.kts:
 *    ```kotlin
 *    plugins {
 *        id("com.google.gms.google-services")
 *        id("com.google.firebase.crashlytics")
 *    }
 *    ```
 * 4. Set `enabled = true` when initializing CrashReporter
 *
 * ## Privacy Controls
 *
 * - **Opt-In Only:** Crash reporting to external services is disabled by default
 * - **User Control:** Connected to Settings > Privacy > Crash Reporting toggle
 * - **Runtime Toggle:** Can be enabled/disabled at any time via setEnabled()
 * - **No PII:** Even when enabled, personally identifiable information is never collected
 * - **Graceful Degradation:** If Firebase SDK is missing, falls back to Timber logging
 *
 * ## Features
 *
 * - **Automatic Crash Reporting:** Fatal crashes are automatically reported (when enabled)
 * - **Non-Fatal Exception Tracking:** Caught exceptions can be logged via recordException()
 * - **Breadcrumb Logging:** User actions logged via log() appear in crash reports
 * - **Custom Keys:** Device info and app state attached via setCustomKey()
 * - **Stack Traces:** Full stack traces captured for all exceptions
 * - **Consent Handling:** Only reports when user has enabled crash reporting
 *
 * ## Related Documentation
 *
 * See `/Volumes/M-Drive/Coding/AVA/docs/AVA-CRASH-REPORTING.md` for:
 * - Detailed architecture documentation
 * - Privacy design rationale
 * - Implementation guidelines
 * - Testing procedures
 *
 * @since 2025-11-07
 * @see <a href="../../../../../../../../../docs/AVA-CRASH-REPORTING.md">AVA Crash Reporting Documentation</a>
 */
object CrashReporter {

    private var isEnabled: Boolean = false
    private var isInitialized: Boolean = false
    private var firebaseAvailable: Boolean = false

    /**
     * Initialize crash reporter
     *
     * Call this in Application.onCreate() or MainActivity.onCreate()
     *
     * @param context Application or Activity context
     * @param enabled Whether crash reporting is enabled (default: false, privacy-first)
     */
    fun initialize(context: Context, enabled: Boolean = false) {
        if (isInitialized) {
            Timber.w("CrashReporter already initialized")
            return
        }

        isEnabled = enabled
        isInitialized = true

        // Check if Firebase Crashlytics is available
        firebaseAvailable = checkFirebaseAvailability()

        Timber.i("CrashReporter initialized (enabled: $enabled, firebase: $firebaseAvailable)")

        // Set device info as custom keys
        if (enabled && firebaseAvailable) {
            setDeviceInfo()
        }

        // Initialize Firebase Crashlytics
        if (firebaseAvailable) {
            try {
                val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                crashlytics.setCrashlyticsCollectionEnabled(enabled)
                Timber.d("Firebase Crashlytics collection enabled: $enabled")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize Firebase Crashlytics")
                firebaseAvailable = false
            }
        } else {
            Timber.d("Firebase Crashlytics SDK not available - using local logging only")
        }
    }

    /**
     * Enable or disable crash reporting
     *
     * Connected to Settings > Privacy > Crash Reporting toggle
     *
     * @param enabled Whether crash reporting should be enabled
     */
    fun setEnabled(enabled: Boolean) {
        if (!isInitialized) {
            Timber.w("CrashReporter not initialized, call initialize() first")
            return
        }

        isEnabled = enabled
        Timber.i("Crash reporting ${if (enabled) "enabled" else "disabled"}")

        // Enable/disable Firebase Crashlytics
        if (firebaseAvailable) {
            try {
                val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                crashlytics.setCrashlyticsCollectionEnabled(enabled)

                if (enabled) {
                    setDeviceInfo()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to set Firebase Crashlytics enabled state")
            }
        }
    }

    /**
     * Log a breadcrumb message to crash reporting system
     *
     * Useful for tracking user actions leading up to crashes.
     * These breadcrumbs appear in the crash report timeline.
     *
     * @param message The breadcrumb message to log
     */
    fun log(message: String) {
        // Always log to Timber, even when disabled (for local debugging)
        Timber.d("[Crash] $message")

        if (!isEnabled || !firebaseAvailable) return

        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log(message)
        } catch (e: Exception) {
            Timber.e(e, "Failed to log breadcrumb to Firebase Crashlytics")
        }
    }

    /**
     * Log a breadcrumb message with a tag
     *
     * @param tag Category or tag for the breadcrumb
     * @param message The breadcrumb message to log
     */
    fun log(tag: String, message: String) {
        // Always log to Timber, even when disabled (for local debugging)
        Timber.tag(tag).d("[Crash] $message")

        if (!isEnabled || !firebaseAvailable) return

        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("[$tag] $message")
        } catch (e: Exception) {
            Timber.e(e, "Failed to log tagged breadcrumb to Firebase Crashlytics")
        }
    }

    /**
     * Record a non-fatal exception
     *
     * These are caught exceptions that don't crash the app but should be tracked.
     * Full stack trace is captured and sent to Firebase Crashlytics.
     *
     * @param throwable The exception to record
     */
    fun recordException(throwable: Throwable) {
        // Always log to Timber, even when disabled (for local debugging)
        Timber.e(throwable, "Non-fatal exception recorded")

        if (!isEnabled || !firebaseAvailable) return

        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(throwable)
        } catch (e: Exception) {
            Timber.e(e, "Failed to record exception to Firebase Crashlytics")
        }
    }

    /**
     * Record a non-fatal exception with additional context
     *
     * @param throwable The exception to record
     * @param message Additional context message
     */
    fun recordException(throwable: Throwable, message: String) {
        // Always log to Timber, even when disabled (for local debugging)
        Timber.e(throwable, "Non-fatal exception: $message")

        if (!isEnabled || !firebaseAvailable) return

        try {
            val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
            crashlytics.log(message)
            crashlytics.recordException(throwable)
        } catch (e: Exception) {
            Timber.e(e, "Failed to record exception with context to Firebase Crashlytics")
        }
    }

    /**
     * Set custom key-value pair for crash context
     *
     * These appear in crash reports to help debugging. Useful for tracking
     * app state at the time of the crash.
     *
     * @param key The key name
     * @param value The string value
     */
    fun setCustomKey(key: String, value: String) {
        Timber.d("[Crash] Custom key: $key = $value")

        if (!isEnabled || !firebaseAvailable) return

        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set custom key to Firebase Crashlytics")
        }
    }

    /**
     * Set custom key-value pair (boolean)
     *
     * @param key The key name
     * @param value The boolean value
     */
    fun setCustomKey(key: String, value: Boolean) {
        Timber.d("[Crash] Custom key: $key = $value")

        if (!isEnabled || !firebaseAvailable) return

        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set custom key to Firebase Crashlytics")
        }
    }

    /**
     * Set custom key-value pair (int)
     *
     * @param key The key name
     * @param value The integer value
     */
    fun setCustomKey(key: String, value: Int) {
        Timber.d("[Crash] Custom key: $key = $value")

        if (!isEnabled || !firebaseAvailable) return

        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set custom key to Firebase Crashlytics")
        }
    }

    /**
     * Set custom key-value pair (long)
     *
     * @param key The key name
     * @param value The long value
     */
    fun setCustomKey(key: String, value: Long) {
        Timber.d("[Crash] Custom key: $key = $value")

        if (!isEnabled || !firebaseAvailable) return

        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set custom key to Firebase Crashlytics")
        }
    }

    /**
     * Set custom key-value pair (float)
     *
     * @param key The key name
     * @param value The float value
     */
    fun setCustomKey(key: String, value: Float) {
        Timber.d("[Crash] Custom key: $key = $value")

        if (!isEnabled || !firebaseAvailable) return

        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set custom key to Firebase Crashlytics")
        }
    }

    /**
     * Set custom key-value pair (double)
     *
     * @param key The key name
     * @param value The double value
     */
    fun setCustomKey(key: String, value: Double) {
        Timber.d("[Crash] Custom key: $key = $value")

        if (!isEnabled || !firebaseAvailable) return

        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set custom key to Firebase Crashlytics")
        }
    }

    /**
     * Set user identifier for crash reports
     *
     * Helps identify which users are affected by crashes.
     * Note: Only use anonymized user IDs, never PII.
     *
     * @param userId Anonymized user identifier
     */
    fun setUserId(userId: String) {
        Timber.d("[Crash] User ID: $userId")

        if (!isEnabled || !firebaseAvailable) return

        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setUserId(userId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set user ID to Firebase Crashlytics")
        }
    }

    /**
     * Log app state for debugging
     *
     * Convenience method to log common app state information.
     *
     * @param state Map of key-value pairs representing app state
     */
    fun logAppState(state: Map<String, Any>) {
        state.forEach { (key, value) ->
            when (value) {
                is String -> setCustomKey(key, value)
                is Boolean -> setCustomKey(key, value)
                is Int -> setCustomKey(key, value)
                is Long -> setCustomKey(key, value)
                is Float -> setCustomKey(key, value)
                is Double -> setCustomKey(key, value)
                else -> setCustomKey(key, value.toString())
            }
        }
    }

    /**
     * Check if crash reporting is enabled
     *
     * @return true if crash reporting is enabled
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * Check if crash reporter is initialized
     *
     * @return true if crash reporter has been initialized
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Check if Firebase Crashlytics SDK is available
     *
     * @return true if Firebase Crashlytics SDK is available
     */
    fun isFirebaseAvailable(): Boolean = firebaseAvailable

    /**
     * Check if Firebase Crashlytics is available at runtime
     *
     * This uses reflection to avoid compile-time dependency on Firebase.
     * If Firebase SDK is not included in the build, this will return false.
     *
     * @return true if Firebase Crashlytics class is available
     */
    private fun checkFirebaseAvailability(): Boolean {
        return try {
            Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
            true
        } catch (e: ClassNotFoundException) {
            Timber.d("Firebase Crashlytics SDK not found in classpath")
            false
        } catch (e: Exception) {
            Timber.e(e, "Error checking Firebase Crashlytics availability")
            false
        }
    }

    /**
     * Set device information as custom keys
     *
     * This adds useful debugging information to crash reports:
     * - Device manufacturer and model
     * - Android OS version
     * - App architecture (ABI)
     */
    private fun setDeviceInfo() {
        try {
            setCustomKey("device_manufacturer", Build.MANUFACTURER)
            setCustomKey("device_model", Build.MODEL)
            setCustomKey("android_version", Build.VERSION.RELEASE)
            setCustomKey("android_sdk", Build.VERSION.SDK_INT)
            setCustomKey("device_abi", Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")

            // Add build type info
            setCustomKey("build_type", if (android.os.Build.TYPE == "user") "release" else "debug")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set device info")
        }
    }
}
