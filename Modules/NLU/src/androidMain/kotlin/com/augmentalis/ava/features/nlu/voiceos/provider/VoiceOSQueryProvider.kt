package com.augmentalis.ava.features.nlu.voiceos.provider

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log

/**
 * VoiceOS ContentProvider queries
 *
 * Queries VoiceOS via ContentProvider for:
 * - App context (current foreground app)
 * - Clickable UI elements
 * - Command hierarchy
 */
class VoiceOSQueryProvider(private val context: Context) {

    companion object {
        private const val TAG = "VoiceOSQueryProvider"
        private const val VOICEOS_AUTHORITY = "com.avanues.voiceos.provider"
        private val VOICEOS_APP_CONTEXT_URI = Uri.parse("content://$VOICEOS_AUTHORITY/app_context")
        private val VOICEOS_CLICKABLE_ELEMENTS_URI = Uri.parse("content://$VOICEOS_AUTHORITY/clickable_elements")
    }

    /**
     * Query current app context from VoiceOS
     */
    fun queryAppContext(): String? {
        return try {
            val cursor = context.contentResolver.query(
                VOICEOS_APP_CONTEXT_URI,
                null,
                null,
                null,
                null
            )

            var result: String? = null
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        val packageNameIndex = cursor.getColumnIndex("package_name")
                        if (packageNameIndex >= 0) {
                            result = cursor.getString(packageNameIndex)
                        }
                    }
                } finally {
                    cursor.close()
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error querying VoiceOS app context: ${e.message}", e)
            null
        }
    }

    /**
     * Query clickable elements from VoiceOS
     */
    fun queryClickableElements(): List<Map<String, String>> {
        return try {
            val elements = mutableListOf<Map<String, String>>()
            val cursor = context.contentResolver.query(
                VOICEOS_CLICKABLE_ELEMENTS_URI,
                null,
                null,
                null,
                null
            )

            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        val element = mutableMapOf<String, String>()

                        for (i in 0 until cursor.columnCount) {
                            val columnName = cursor.getColumnName(i)
                            val value = cursor.getString(i) ?: ""
                            element[columnName] = value
                        }

                        elements.add(element)
                    }
                } finally {
                    cursor.close()
                }
            }

            Log.d(TAG, "Found ${elements.size} clickable elements")
            elements
        } catch (e: Exception) {
            Log.e(TAG, "Error querying clickable elements: ${e.message}", e)
            emptyList()
        }
    }
}
