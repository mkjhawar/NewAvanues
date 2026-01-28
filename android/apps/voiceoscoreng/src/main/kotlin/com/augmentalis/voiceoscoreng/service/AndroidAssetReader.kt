/**
 * AndroidAssetReader.kt - Android implementation of IAssetReader
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-26
 *
 * Part of the Hybrid Persistence system for VoiceOSCore.
 * Reads ACD files from the Android assets folder.
 */

package com.augmentalis.voiceoscoreng.service

import android.content.Context
import com.augmentalis.commandmanager.IAssetReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Android implementation of [IAssetReader].
 *
 * Reads files from the Android assets folder using the application context.
 * Used to load the known-apps.acd file at startup.
 *
 * @property context Android application context for asset access
 */
class AndroidAssetReader(
    private val context: Context
) : IAssetReader {

    /**
     * Read an asset file as a string.
     *
     * @param filename The asset filename (e.g., "known-apps.acd")
     * @return The file contents as a string, or null if not found or on error
     */
    override suspend fun readAsset(filename: String): String? = withContext(Dispatchers.IO) {
        try {
            context.assets.open(filename).bufferedReader().use { reader ->
                reader.readText()
            }
        } catch (e: IOException) {
            android.util.Log.w(TAG, "Failed to read asset: $filename", e)
            null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Unexpected error reading asset: $filename", e)
            null
        }
    }

    /**
     * Check if an asset file exists.
     *
     * @param filename The asset filename
     * @return True if the asset exists, false otherwise
     */
    override fun assetExists(filename: String): Boolean {
        return try {
            context.assets.open(filename).use { true }
        } catch (e: IOException) {
            false
        }
    }

    companion object {
        private const val TAG = "AndroidAssetReader"
    }
}
