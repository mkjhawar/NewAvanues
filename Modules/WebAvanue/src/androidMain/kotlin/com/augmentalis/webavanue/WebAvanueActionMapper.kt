package com.augmentalis.webavanue

import android.util.Log

// Re-export from commonMain for backward compatibility
// All business logic now lives in commonMain
// This file only provides Android-specific logging wrapper if needed

private const val TAG = "WebAvanueActionMapper"

/**
 * Android-specific logging extension for WebAvanueActionMapper
 * Logs command execution to Android logcat
 */
fun logCommand(commandId: String, message: String = "") {
    Log.d(TAG, "Executing command: $commandId ${if (message.isNotEmpty()) "- $message" else ""}")
}

fun logWarning(commandId: String, message: String) {
    Log.w(TAG, "Unknown command: $commandId - $message")
}
