/**
 * InstalledAppsManager.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-11
 */
package com.augmentalis.voiceoscore.accessibility.managers

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class InstalledAppsManager(private val context: Context) {

    private val _appList = MutableStateFlow<Map<String, String>>(mutableMapOf())
    val appList: StateFlow<Map<String, String>> = _appList.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            loadInstalledApps()
        }
    }

    fun reloadInstalledApps() {
        CoroutineScope(Dispatchers.IO).launch {
            loadInstalledApps()
        }
    }

    private fun loadInstalledApps() {
        CoroutineScope(Dispatchers.IO).launch {
            val packageManager = context.applicationContext.packageManager ?: return@launch
            val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = packageManager.queryIntentActivities(mainIntent, 0)
                .mapNotNull { resolveInfo ->
                    try {
                        val appName = resolveInfo.loadLabel(packageManager).toString().lowercase().trim()
                        val packageName = resolveInfo.activityInfo.packageName
                        val cleanedAppName = appName.replace(ALPHABETS_PATTERN, "")
                        AppsName(cleanedAppName, packageName, commands = getInstalledAppCommands(cleanedAppName))
                    } catch (e: Exception) {
                        null // Skip apps that cause processing errors
                    }
                }
                .sortedBy { it.name }
                .filter { it.packageName != context.packageName } // Use context.packageName for current app


            val mutableAppList = apps.toMutableList()
            _appList.value = groupCommandsByPackageName(mutableAppList)
        }
    }

    private fun groupCommandsByPackageName(appList: List<AppsName>): Map<String, String> =
        appList
            .flatMap { app -> app.commands.map { command -> command to app.packageName } }
            .toMap()


    private fun getInstalledAppCommands(name: String): List<String> {
        return listOf("open $name", "start $name", "go to $name")
    }

    companion object {
        private val DIGIT_PATTERN = Regex("\\d+")
        private val ALPHABETS_PATTERN = Regex("[^A-Za-z0-9 ]")
        private const val PACKAGE_NAME_MICROSOFT_OFFICE = "com.microsoft.office.officehubrow"
    }
}


data class AppsName(
    var name: String,
    val packageName: String,
    val commands: List<String>,
)
