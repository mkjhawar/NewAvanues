/**
 * UserConsentManager.kt - User consent management for privacy compliance
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-09
 * Phase: 3 (Medium Priority)
 * Issue: User consent management for data collection
 */
package com.augmentalis.voiceoscore.privacy

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe user consent manager for privacy compliance
 *
 * Features:
 * - Granular consent management (analytics, crash reports, usage metrics, etc.)
 * - Persistent consent storage using SharedPreferences
 * - Reactive consent state via Kotlin Flow
 * - GDPR/CCPA compliance support
 * - Consent versioning for policy updates
 * - Consent withdrawal support
 *
 * Consent types:
 * - Analytics: App usage analytics (events, screens, actions)
 * - Crash Reports: Automated crash and error reporting
 * - Usage Metrics: Command usage statistics, performance metrics
 * - Voice Data: Voice command recordings and transcripts
 * - Diagnostic Data: System diagnostics, logs, debug info
 *
 * Usage:
 * ```kotlin
 * val consentManager = UserConsentManager(context)
 *
 * // Check if user has consented to analytics
 * if (consentManager.hasConsent(ConsentType.ANALYTICS)) {
 *     analyticsCollector.trackEvent("user_action")
 * }
 *
 * // Grant consent
 * consentManager.grantConsent(ConsentType.ANALYTICS)
 * consentManager.grantConsent(ConsentType.USAGE_METRICS)
 *
 * // Revoke consent
 * consentManager.revokeConsent(ConsentType.CRASH_REPORTS)
 *
 * // Observe consent changes (for UI)
 * consentManager.consentState.collect { state ->
 *     updateUI(state)
 * }
 * ```
 *
 * Thread Safety: All operations are thread-safe
 */
class UserConsentManager(
    context: Context
) {
    companion object {
        private const val TAG = "UserConsentManager"
        private const val PREFS_NAME = "voiceos_user_consent"
        private const val KEY_CONSENT_VERSION = "consent_version"
        private const val KEY_CONSENT_TIMESTAMP = "consent_timestamp"
        private const val CURRENT_CONSENT_VERSION = 1
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Reactive consent state for UI binding
    private val _consentState = MutableStateFlow(loadConsentState())
    val consentState: StateFlow<ConsentState> = _consentState.asStateFlow()

    /**
     * Check if user has granted consent for specific type
     *
     * @param type ConsentType to check
     * @return true if consent is granted, false otherwise
     */
    fun hasConsent(type: ConsentType): Boolean {
        val key = type.toPreferenceKey()
        val granted = prefs.getBoolean(key, false)
        Log.d(TAG, "Consent check: $type = $granted")
        return granted
    }

    /**
     * Check if user has granted all specified consents
     *
     * @param types List of ConsentType to check
     * @return true if ALL consents are granted, false otherwise
     */
    fun hasAllConsents(vararg types: ConsentType): Boolean {
        return types.all { hasConsent(it) }
    }

    /**
     * Check if user has granted any of the specified consents
     *
     * @param types List of ConsentType to check
     * @return true if ANY consent is granted, false otherwise
     */
    fun hasAnyConsent(vararg types: ConsentType): Boolean {
        return types.any { hasConsent(it) }
    }

    /**
     * Grant consent for specific type
     *
     * Updates SharedPreferences and emits new state to Flow.
     *
     * @param type ConsentType to grant
     */
    fun grantConsent(type: ConsentType) {
        val key = type.toPreferenceKey()
        prefs.edit()
            .putBoolean(key, true)
            .putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            .putInt(KEY_CONSENT_VERSION, CURRENT_CONSENT_VERSION)
            .apply()

        Log.i(TAG, "Consent granted: $type")
        _consentState.value = loadConsentState()
    }

    /**
     * Grant multiple consents at once
     *
     * @param types ConsentTypes to grant
     */
    fun grantConsents(vararg types: ConsentType) {
        val editor = prefs.edit()
        for (type in types) {
            editor.putBoolean(type.toPreferenceKey(), true)
        }
        editor.putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
        editor.putInt(KEY_CONSENT_VERSION, CURRENT_CONSENT_VERSION)
        editor.apply()

        Log.i(TAG, "Consents granted: ${types.joinToString(", ")}")
        _consentState.value = loadConsentState()
    }

    /**
     * Revoke consent for specific type
     *
     * Updates SharedPreferences and emits new state to Flow.
     *
     * @param type ConsentType to revoke
     */
    fun revokeConsent(type: ConsentType) {
        val key = type.toPreferenceKey()
        prefs.edit()
            .putBoolean(key, false)
            .putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            .apply()

        Log.i(TAG, "Consent revoked: $type")
        _consentState.value = loadConsentState()
    }

    /**
     * Revoke multiple consents at once
     *
     * @param types ConsentTypes to revoke
     */
    fun revokeConsents(vararg types: ConsentType) {
        val editor = prefs.edit()
        for (type in types) {
            editor.putBoolean(type.toPreferenceKey(), false)
        }
        editor.putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
        editor.apply()

        Log.i(TAG, "Consents revoked: ${types.joinToString(", ")}")
        _consentState.value = loadConsentState()
    }

    /**
     * Revoke ALL consents (user withdrawal)
     *
     * Clears all consent preferences. Useful for "Delete my data" flows.
     */
    fun revokeAllConsents() {
        prefs.edit().clear().apply()
        Log.w(TAG, "All consents revoked (user withdrawal)")
        _consentState.value = loadConsentState()
    }

    /**
     * Get consent timestamp (when user last updated consent)
     *
     * @return Timestamp in milliseconds, or 0 if never set
     */
    fun getConsentTimestamp(): Long {
        return prefs.getLong(KEY_CONSENT_TIMESTAMP, 0)
    }

    /**
     * Get consent version (for policy updates)
     *
     * @return Version number, or 0 if never set
     */
    fun getConsentVersion(): Int {
        return prefs.getInt(KEY_CONSENT_VERSION, 0)
    }

    /**
     * Check if consent policy needs update
     *
     * Returns true if user consented to an older version of the policy.
     * Use this to trigger consent re-confirmation after policy updates.
     *
     * @return true if consent needs update, false otherwise
     */
    fun needsConsentUpdate(): Boolean {
        val userVersion = getConsentVersion()
        val needsUpdate = userVersion < CURRENT_CONSENT_VERSION
        if (needsUpdate) {
            Log.w(TAG, "Consent policy outdated: user=$userVersion, current=$CURRENT_CONSENT_VERSION")
        }
        return needsUpdate
    }

    /**
     * Check if user has EVER granted any consent
     *
     * Useful for determining if consent dialog should be shown.
     *
     * @return true if user has granted at least one consent, false otherwise
     */
    fun hasGrantedAnyConsent(): Boolean {
        return ConsentType.entries.any { hasConsent(it) }
    }

    /**
     * Get current consent state for all types
     *
     * @return ConsentState with all consent statuses
     */
    fun getCurrentConsentState(): ConsentState {
        return loadConsentState()
    }

    /**
     * Load consent state from SharedPreferences
     */
    private fun loadConsentState(): ConsentState {
        return ConsentState(
            analytics = hasConsent(ConsentType.ANALYTICS),
            crashReports = hasConsent(ConsentType.CRASH_REPORTS),
            usageMetrics = hasConsent(ConsentType.USAGE_METRICS),
            voiceData = hasConsent(ConsentType.VOICE_DATA),
            diagnosticData = hasConsent(ConsentType.DIAGNOSTIC_DATA),
            timestamp = getConsentTimestamp(),
            version = getConsentVersion()
        )
    }

    /**
     * Export consent state for logging/debugging
     *
     * @return Human-readable consent state summary
     */
    fun exportConsentState(): String {
        val state = getCurrentConsentState()
        return buildString {
            appendLine("User Consent State:")
            appendLine("  Analytics: ${state.analytics}")
            appendLine("  Crash Reports: ${state.crashReports}")
            appendLine("  Usage Metrics: ${state.usageMetrics}")
            appendLine("  Voice Data: ${state.voiceData}")
            appendLine("  Diagnostic Data: ${state.diagnosticData}")
            appendLine("  Version: ${state.version}")
            appendLine("  Last Updated: ${state.timestamp}")
        }
    }
}

/**
 * Types of consent for different data collection purposes
 */
enum class ConsentType(val description: String) {
    /** App usage analytics (events, screens, actions) */
    ANALYTICS("App usage analytics"),

    /** Automated crash and error reporting */
    CRASH_REPORTS("Crash and error reporting"),

    /** Command usage statistics, performance metrics */
    USAGE_METRICS("Usage statistics and metrics"),

    /** Voice command recordings and transcripts */
    VOICE_DATA("Voice recordings and transcripts"),

    /** System diagnostics, logs, debug info */
    DIAGNOSTIC_DATA("System diagnostics and logs");

    /**
     * Convert ConsentType to SharedPreferences key
     */
    fun toPreferenceKey(): String {
        return "consent_${name.lowercase()}"
    }
}

/**
 * Current consent state for all types
 *
 * @property analytics Analytics consent status
 * @property crashReports Crash reporting consent status
 * @property usageMetrics Usage metrics consent status
 * @property voiceData Voice data consent status
 * @property diagnosticData Diagnostic data consent status
 * @property timestamp When consent was last updated (milliseconds)
 * @property version Consent policy version user agreed to
 */
data class ConsentState(
    val analytics: Boolean,
    val crashReports: Boolean,
    val usageMetrics: Boolean,
    val voiceData: Boolean,
    val diagnosticData: Boolean,
    val timestamp: Long,
    val version: Int
) {
    /**
     * Check if ALL consents are granted
     */
    fun hasAllConsents(): Boolean {
        return analytics && crashReports && usageMetrics && voiceData && diagnosticData
    }

    /**
     * Check if NO consents are granted
     */
    fun hasNoConsents(): Boolean {
        return !analytics && !crashReports && !usageMetrics && !voiceData && !diagnosticData
    }

    /**
     * Count granted consents
     */
    fun countGranted(): Int {
        var count = 0
        if (analytics) count++
        if (crashReports) count++
        if (usageMetrics) count++
        if (voiceData) count++
        if (diagnosticData) count++
        return count
    }
}

/**
 * Consent manager builder for easier configuration
 *
 * Example:
 * ```kotlin
 * val manager = UserConsentManagerBuilder(context)
 *     .grantAnalytics()
 *     .grantUsageMetrics()
 *     .build()
 * ```
 */
class UserConsentManagerBuilder(private val context: Context) {
    private val manager = UserConsentManager(context)
    private val toGrant = mutableListOf<ConsentType>()

    fun grantAnalytics(): UserConsentManagerBuilder {
        toGrant.add(ConsentType.ANALYTICS)
        return this
    }

    fun grantCrashReports(): UserConsentManagerBuilder {
        toGrant.add(ConsentType.CRASH_REPORTS)
        return this
    }

    fun grantUsageMetrics(): UserConsentManagerBuilder {
        toGrant.add(ConsentType.USAGE_METRICS)
        return this
    }

    fun grantVoiceData(): UserConsentManagerBuilder {
        toGrant.add(ConsentType.VOICE_DATA)
        return this
    }

    fun grantDiagnosticData(): UserConsentManagerBuilder {
        toGrant.add(ConsentType.DIAGNOSTIC_DATA)
        return this
    }

    fun grantAll(): UserConsentManagerBuilder {
        toGrant.addAll(ConsentType.entries)
        return this
    }

    fun build(): UserConsentManager {
        if (toGrant.isNotEmpty()) {
            manager.grantConsents(*toGrant.toTypedArray())
        }
        return manager
    }
}
