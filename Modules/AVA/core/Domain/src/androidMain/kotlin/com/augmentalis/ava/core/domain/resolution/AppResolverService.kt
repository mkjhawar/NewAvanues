package com.augmentalis.ava.core.domain.resolution

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import com.augmentalis.ava.core.domain.model.AppPlatform
import com.augmentalis.ava.core.domain.model.AppResolution
import com.augmentalis.ava.core.domain.model.InstalledApp
import com.augmentalis.ava.core.domain.model.ResolutionSource
import com.augmentalis.ava.core.domain.repository.AppPreferencesRepository

/**
 * Service for resolving the best app for any capability.
 *
 * Core logic:
 * 1. Check if user has a saved preference → use it
 * 2. Scan for installed apps with this capability
 * 3. If only one app → auto-select and save
 * 4. If multiple apps → return MultipleAvailable for UI to prompt
 * 5. If no apps → return NoneAvailable
 *
 * Part of Intelligent Resolution System (Chapter 71).
 *
 * Author: Manoj Jhawar
 */
class AppResolverService(
    private val context: Context,
    private val preferencesRepository: AppPreferencesRepository,
    private val capabilityRegistry: CapabilityRegistry = CapabilityRegistry
) {
    companion object {
        private const val TAG = "AppResolverService"
    }

    private val packageManager: PackageManager = context.packageManager

    /**
     * Resolve the best app for a capability.
     *
     * This is the main entry point. It will:
     * - Return Resolved if user has a preference or only one app is available
     * - Return MultipleAvailable if user needs to choose
     * - Return NoneAvailable if no apps can handle this capability
     *
     * @param capability The capability ID (e.g., "email", "sms")
     * @return AppResolution indicating the result
     */
    suspend fun resolveApp(capability: String): AppResolution {
        val definition = capabilityRegistry.get(capability)
            ?: return AppResolution.UnknownCapability(capability)

        Log.d(TAG, "Resolving app for capability: $capability")

        // 1. Check saved preference
        val savedPreference = preferencesRepository.getPreferredApp(capability)
        if (savedPreference != null) {
            if (isAppInstalled(savedPreference.packageName)) {
                Log.d(TAG, "Using saved preference: ${savedPreference.packageName}")
                return AppResolution.Resolved(
                    packageName = savedPreference.packageName,
                    appName = savedPreference.appName,
                    source = ResolutionSource.USER_PREFERENCE
                )
            } else {
                // App was uninstalled, clear the preference
                Log.w(TAG, "Preferred app ${savedPreference.packageName} no longer installed, clearing")
                preferencesRepository.clearPreferredApp(capability)
            }
        }

        // 2. Scan for installed apps
        val installedApps = findInstalledApps(definition)
        Log.d(TAG, "Found ${installedApps.size} apps for $capability: ${installedApps.map { it.packageName }}")

        return when {
            installedApps.isEmpty() -> {
                Log.d(TAG, "No apps available for $capability")
                AppResolution.NoneAvailable(
                    capability = capability,
                    suggestedApps = definition.knownApps
                        .filter { it.platform != AppPlatform.IOS }
                        .take(3)
                )
            }

            installedApps.size == 1 -> {
                // Auto-select the only available app
                val app = installedApps.first()
                Log.d(TAG, "Auto-selecting single app: ${app.packageName}")
                preferencesRepository.setPreferredApp(
                    capability = capability,
                    packageName = app.packageName,
                    appName = app.appName,
                    setBy = "auto"
                )
                AppResolution.Resolved(
                    packageName = app.packageName,
                    appName = app.appName,
                    source = ResolutionSource.AUTO_DETECTED
                )
            }

            else -> {
                // Multiple apps - need user to choose
                Log.d(TAG, "Multiple apps available, prompting user")
                AppResolution.MultipleAvailable(
                    capability = capability,
                    capabilityDisplayName = definition.displayName,
                    apps = installedApps,
                    recommendedIndex = findRecommendedIndex(installedApps, definition)
                )
            }
        }
    }

    /**
     * Save user's app choice for a capability.
     *
     * @param capability The capability ID
     * @param packageName The chosen app's package name
     * @param appName The chosen app's display name
     * @param remember If true, save the preference permanently
     */
    suspend fun savePreference(
        capability: String,
        packageName: String,
        appName: String,
        remember: Boolean = true
    ) {
        if (remember) {
            preferencesRepository.setPreferredApp(
                capability = capability,
                packageName = packageName,
                appName = appName,
                setBy = "user"
            )
            Log.i(TAG, "Saved preference: $capability → $packageName")
        }

        // Always record usage for learning
        preferencesRepository.recordUsage(capability, packageName)
    }

    /**
     * Get all capabilities with their current preference status.
     * Used for Settings screen.
     *
     * @return List of all capabilities with their current state
     */
    suspend fun getAllCapabilityPreferences(): List<CapabilityWithPreference> {
        val savedPreferences = preferencesRepository.getAllPreferences()

        return capabilityRegistry.capabilities.map { (id, definition) ->
            val installedApps = findInstalledApps(definition)
            val savedPref = savedPreferences[id]

            CapabilityWithPreference(
                capability = id,
                displayName = definition.displayName,
                category = definition.category,
                selectedApp = savedPref?.let { pref ->
                    installedApps.find { it.packageName == pref.packageName }
                },
                availableApps = installedApps,
                canChange = installedApps.size > 1
            )
        }
    }

    /**
     * Clear a saved preference.
     */
    suspend fun clearPreference(capability: String) {
        preferencesRepository.clearPreferredApp(capability)
        Log.i(TAG, "Cleared preference for: $capability")
    }

    /**
     * Get icon for an installed app.
     */
    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    // ==================== Private Methods ====================

    private fun findInstalledApps(definition: CapabilityDefinition): List<InstalledApp> {
        val found = mutableSetOf<InstalledApp>()

        // Method 1: Query by intent specifications
        for (intentSpec in definition.androidIntents) {
            val intent = buildIntentFromSpec(intentSpec)
            val resolveInfos = packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

            for (resolveInfo in resolveInfos) {
                val app = resolveInfoToInstalledApp(resolveInfo)
                if (app != null) {
                    found.add(app)
                }
            }
        }

        // Method 2: Check known apps directly
        for (knownApp in definition.knownApps) {
            if (knownApp.platform != AppPlatform.IOS && isAppInstalled(knownApp.packageName)) {
                if (found.none { it.packageName == knownApp.packageName }) {
                    found.add(
                        InstalledApp(
                            packageName = knownApp.packageName,
                            appName = knownApp.displayName
                        )
                    )
                }
            }
        }

        return found.toList().sortedBy { it.appName.lowercase() }
    }

    private fun buildIntentFromSpec(spec: IntentSpec): Intent {
        return Intent(spec.action).apply {
            spec.dataScheme?.let { scheme ->
                data = Uri.parse("$scheme:")
            }
            spec.mimeType?.let { type = it }
            spec.packageName?.let { setPackage(it) }
        }
    }

    private fun resolveInfoToInstalledApp(resolveInfo: ResolveInfo): InstalledApp? {
        return try {
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(packageManager).toString()

            // Skip system UI and chooser activities
            if (packageName.contains("android.") && packageName.contains("chooser")) {
                return null
            }

            InstalledApp(
                packageName = packageName,
                appName = appName
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get app info from ResolveInfo", e)
            null
        }
    }

    private fun findRecommendedIndex(apps: List<InstalledApp>, definition: CapabilityDefinition): Int {
        // Recommend based on known app priority (first in list = most popular)
        val knownPackages = definition.knownApps.map { it.packageName }

        for ((index, app) in apps.withIndex()) {
            val knownIndex = knownPackages.indexOf(app.packageName)
            if (knownIndex != -1) {
                return index
            }
        }

        return 0
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

/**
 * A capability with its current preference status.
 * Used for Settings screen.
 */
data class CapabilityWithPreference(
    val capability: String,
    val displayName: String,
    val category: CapabilityCategory,
    val selectedApp: InstalledApp?,
    val availableApps: List<InstalledApp>,
    val canChange: Boolean
)
