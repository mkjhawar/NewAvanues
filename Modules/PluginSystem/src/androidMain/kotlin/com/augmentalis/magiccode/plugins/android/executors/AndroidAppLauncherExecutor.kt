/**
 * AndroidAppLauncherExecutor.kt - Android implementation of IAppLauncher
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 *
 * Bridges the AppLauncherPlugin to Android for app launching and discovery.
 */
package com.augmentalis.magiccode.plugins.android.executors

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.augmentalis.magiccode.plugins.android.ServiceRegistry
import com.augmentalis.magiccode.plugins.builtin.AppInfo
import com.augmentalis.magiccode.plugins.builtin.IAppLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of IAppLauncher.
 *
 * Uses PackageManager for app discovery and Intent for launching.
 *
 * @param serviceRegistry Registry to retrieve AccessibilityService from
 */
class AndroidAppLauncherExecutor(
    private val serviceRegistry: ServiceRegistry
) : IAppLauncher {

    private val accessibilityService: AccessibilityService?
        get() = serviceRegistry.getSync(ServiceRegistry.ACCESSIBILITY_SERVICE)

    private val context: Context?
        get() = accessibilityService

    private val packageManager: PackageManager?
        get() = context?.packageManager

    override suspend fun launchApp(packageName: String): Boolean = withContext(Dispatchers.Main) {
        val ctx = context ?: return@withContext false
        val pm = packageManager ?: return@withContext false

        try {
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun launchActivity(packageName: String, activityName: String): Boolean =
        withContext(Dispatchers.Main) {
            val ctx = context ?: return@withContext false

            try {
                val intent = Intent().apply {
                    setClassName(packageName, activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = packageManager ?: return@withContext emptyList()

        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val activities: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            activities.mapNotNull { resolveInfo ->
                try {
                    val appInfo = resolveInfo.activityInfo.applicationInfo
                    val displayName = pm.getApplicationLabel(appInfo).toString()
                    val packageName = appInfo.packageName

                    AppInfo(
                        packageName = packageName,
                        displayName = displayName,
                        aliases = generateAliases(displayName),
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        launchActivity = resolveInfo.activityInfo.name
                    )
                } catch (e: Exception) {
                    null
                }
            }.distinctBy { it.packageName }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun isAppInstalled(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val pm = packageManager ?: return@withContext false

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override suspend fun getAppInfo(packageName: String): AppInfo? = withContext(Dispatchers.IO) {
        val pm = packageManager ?: return@withContext null

        try {
            val appInfo: ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }

            val displayName = pm.getApplicationLabel(appInfo).toString()

            // Get launch activity
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            val launchActivity = launchIntent?.component?.className

            AppInfo(
                packageName = packageName,
                displayName = displayName,
                aliases = generateAliases(displayName),
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                launchActivity = launchActivity
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getRecentApps(limit: Int): List<AppInfo> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext emptyList()

        try {
            val usageStatsManager = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return@withContext emptyList()

            val endTime = System.currentTimeMillis()
            val startTime = endTime - (24 * 60 * 60 * 1000) // Last 24 hours

            val usageStats: List<UsageStats> = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ) ?: return@withContext emptyList()

            usageStats
                .filter { it.totalTimeInForeground > 0 }
                .sortedByDescending { it.lastTimeUsed }
                .take(limit)
                .mapNotNull { stats ->
                    getAppInfo(stats.packageName)
                }
        } catch (e: Exception) {
            // Usage stats permission not granted or other error
            emptyList()
        }
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private fun generateAliases(displayName: String): List<String> {
        val aliases = mutableListOf<String>()

        // Add lowercase version
        val lowerName = displayName.lowercase()
        if (lowerName != displayName) {
            aliases.add(lowerName)
        }

        // Add single-word version if multi-word
        val words = displayName.split(" ", "-", "_")
        if (words.size > 1) {
            // Add first word if meaningful
            val firstWord = words.first()
            if (firstWord.length >= 3) {
                aliases.add(firstWord.lowercase())
            }
            // Add last word if meaningful and different
            val lastWord = words.last()
            if (lastWord.length >= 3 && lastWord != firstWord) {
                aliases.add(lastWord.lowercase())
            }
        }

        // Handle common app name patterns
        when {
            lowerName.startsWith("google ") -> {
                aliases.add(lowerName.removePrefix("google "))
            }
            lowerName.contains("maps") -> {
                aliases.add("maps")
                aliases.add("navigation")
            }
            lowerName.contains("mail") || lowerName.contains("email") -> {
                aliases.add("mail")
                aliases.add("email")
            }
            lowerName.contains("calendar") -> {
                aliases.add("calendar")
            }
            lowerName.contains("camera") -> {
                aliases.add("camera")
            }
            lowerName.contains("phone") || lowerName.contains("dialer") -> {
                aliases.add("phone")
                aliases.add("dialer")
            }
            lowerName.contains("message") || lowerName.contains("sms") -> {
                aliases.add("messages")
                aliases.add("sms")
                aliases.add("text")
            }
            lowerName.contains("settings") -> {
                aliases.add("settings")
            }
        }

        return aliases.distinct()
    }
}
