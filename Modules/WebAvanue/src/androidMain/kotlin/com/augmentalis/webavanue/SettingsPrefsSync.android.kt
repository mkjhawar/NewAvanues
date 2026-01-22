package com.augmentalis.webavanue

import android.content.Context

/**
 * Android-specific: Sync download path to SharedPreferences
 *
 * This allows synchronous access in AndroidDownloadQueue callback
 * without blocking with runBlocking.
 *
 * Called from SettingsViewModel when download path changes.
 */
fun syncDownloadPathToPrefs(context: Context, path: String?) {
    val prefs = context.getSharedPreferences("webavanue_download", Context.MODE_PRIVATE)
    if (path != null) {
        prefs.edit().putString("download_path", path).apply()
    } else {
        prefs.edit().remove("download_path").apply()
    }
}
