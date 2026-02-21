/**
 * FirebaseRemoteConfigRepository.kt - Firebase Remote Config for Vivoka language models
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 */
package com.augmentalis.speechrecognition.vivoka.model

import android.annotation.SuppressLint
import android.content.Context
import com.augmentalis.speechrecognition.BuildConfig
import com.augmentalis.speechrecognition.vivoka.VivokaPathResolver
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FirebaseRemoteConfigRepository(private val context: Context) {

    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    private val isDebug = BuildConfig.DEBUG
    private val client = OkHttpClient()

    // Credentials injected via BuildConfig — NEVER hardcode in source
    private val downloadUsername: String get() = BuildConfig.VIVOKA_DOWNLOAD_USERNAME
    private val downloadPassword: String get() = BuildConfig.VIVOKA_DOWNLOAD_PASSWORD
    suspend fun init() {
        withContext(Dispatchers.IO) {
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.fetchAndActivate()
        }
    }

    suspend fun getLanguageResource(
        languageId: String,
        callback: (status: FileStatus) -> Unit
    ): String? {
        try {
            callback(FileStatus.Initialization)
            remoteConfig.reset().await()
            remoteConfig.fetch().await()
            remoteConfig.activate().await()

            // Normalize language ID: replace hyphens with underscores for Firebase key format
            val normalizedLanguageId = if (languageId.contains("-")) {
                languageId.replace("-", "_")
            } else {
                languageId
            }

            val resultResource = if (!isDebug) {
                "$normalizedLanguageId$VOICE_RESOURCE_SUFFIX" to remoteConfig.getString("$normalizedLanguageId$VOICE_RESOURCE_SUFFIX")
            } else {
                "$normalizedLanguageId$VOICE_RESOURCE_SUFFIX_DEBUG" to remoteConfig.getString("$normalizedLanguageId$VOICE_RESOURCE_SUFFIX_DEBUG")
            }

            //Download file
            callback(FileStatus.Downloading(0))
            var downloadedProgress = 0
            val isDownloaded = downloadResource(resultResource) {
                // Only update 0, 1 , 2 etc
                if (it != downloadedProgress) {
                    downloadedProgress = it
                    callback(FileStatus.Downloading(downloadedProgress))
                }

            }

            if (!isDownloaded) {
                callback(FileStatus.Error(FileError.REMOTE))
            } else {
                val resultJson = if (!isDebug) {
                    "$normalizedLanguageId$VOICE_CONFIG_FILE_SUFFIX" to remoteConfig.getString("$normalizedLanguageId$VOICE_CONFIG_FILE_SUFFIX")
                } else {
                    "$normalizedLanguageId$VOICE_CONFIG_FILE_SUFFIX_DEBUG" to remoteConfig.getString("$normalizedLanguageId$VOICE_CONFIG_FILE_SUFFIX_DEBUG")
                }

                val configFile = downloadConfigFile(resultJson)

                callback(FileStatus.Extracting)

                if (!configFile.isNullOrBlank()) {
                    val isExtracted = extractFile(languageId)
                    if (isExtracted.first) {
                        copyFiles(languageId)
                        callback(FileStatus.Completed)
                        return configFile
                    } else {
                        callback(FileStatus.Error(FileError.LOCAL))
                    }

                } else {
                    callback(FileStatus.Error(FileError.REMOTE))
                }
            }


        } catch (e: Exception) {
            callback(FileStatus.Error(FileError.REMOTE))
        }
        return null
    }

    @Throws
    private suspend fun downloadResource(
        pair: Pair<String, String>,
        downloadProgress: (progress: Int) -> Unit
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.i(TAG, "downloadResource: ${pair.second}")
                // Encode credentials in Base64
                val credentials: String = Credentials.basic(downloadUsername, downloadPassword)
                val request = Request.Builder()
                    .addHeader(HEADER_AUTHORIZATION, credentials)
                    .url(pair.second)
                    .build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    android.util.Log.i(TAG, "downloadResource: Failed to download file")
                    return@withContext false
                }

                // Create a file in the external storage directory
                val internalDir = context.filesDir
                val tempFile = File(internalDir, VOICE_TEMP)
                if (!tempFile.exists()) {
                    tempFile.mkdir()
                }
                val file = File(tempFile, "${pair.first}.zip")
                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null

                try {
                    inputStream = response.body?.byteStream()
                    outputStream = FileOutputStream(file)

                    // Buffer size for reading data
                    val buffer = ByteArray(2048)
                    var bytesRead: Int
                    val totalBytes = response.body?.contentLength() ?: 0
                    var downloadedBytes: Long = 0

                    // Read from the input stream and write to the output stream
                    while (inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        // Update download progress
                        val progress = (downloadedBytes * 100 / totalBytes)
                        downloadProgress(progress.toInt())
                    }
                    downloadProgress(100)
                    outputStream.flush()
                    return@withContext true
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.util.Log.i(TAG, "Error writing file: ${e.message}")
                    return@withContext false
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.i(TAG, "Error during download: ${e.message}")
                return@withContext false
            }
        }
    }

    private suspend fun downloadConfigFile(pair: Pair<String, String>): String? {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.i(TAG, "downloadConfigFile: ${pair.second}")
                val credentials: String = Credentials.basic(downloadUsername, downloadPassword)
                val request = Request.Builder()
                    .addHeader(HEADER_AUTHORIZATION, credentials)
                    .url(pair.second)
                    .build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    android.util.Log.i(TAG, "downloadConfigFile: Failed to download file")
                    return@withContext null
                }

                return@withContext response.body?.string().toString()
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.i(TAG, " downloadConfigFile Error during download: ${e.message}")
                return@withContext null
            }
        }
    }

    @Throws(Exception::class)
    private suspend fun extractFile(languageId: String): Pair<Boolean, File> {
        return withContext(Dispatchers.IO) {
            val internalDir = context.filesDir
            val sourceFile = if (!isDebug) {
                File(internalDir, "$VOICE_TEMP/$languageId$VOICE_RESOURCE_SUFFIX.zip")
            } else {
                File(internalDir, "$VOICE_TEMP/$languageId$VOICE_RESOURCE_SUFFIX_DEBUG.zip")
            }
            val destinationFile = File(internalDir, VOICE_TEMP)
            val isExtracted = FileZipManager().unzip(sourceFile, destinationFile)
            return@withContext Pair(isExtracted, destinationFile)
        }
    }

    @Throws(Exception::class)
    private suspend fun copyFiles(languageId: String) {
        withContext(Dispatchers.IO) {
            val rootSourceDir = File(context.filesDir, "voice_temp/${languageId}_voice_resource/data/csdk/asr")
            val pathResolver = VivokaPathResolver(context)
            val path = pathResolver.resolveVsdkPath()
            val rootTargetDir = File(path, "data/csdk/asr")

            // List of folders that need to be processed
            val folders = listOf("clc", "acmod", "lm", "ctx")

            // Iterate over each subfolder and move the files
            folders.forEach { subfolder ->

                val sourceDir = File(rootSourceDir, subfolder)
                val targetDir = File(rootTargetDir, subfolder)
                // If the target directory doesn't exist, create it
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                // Move files from sourceDir to targetDir
                sourceDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val targetFile = File(targetDir, file.name)
                        file.copyTo(targetFile, overwrite = true)
                        file.delete()  // Delete the original file after copying
                    }
                }
            }
        }


    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: FirebaseRemoteConfigRepository? = null

        @JvmStatic
        fun getInstance(context: Context): FirebaseRemoteConfigRepository {
            return instance ?: synchronized(this) {
                instance ?: FirebaseRemoteConfigRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }

        private const val VOICE_RESOURCE_SUFFIX = "_voice_resource"
        private const val VOICE_CONFIG_FILE_SUFFIX = "_json"

        private const val VOICE_RESOURCE_SUFFIX_DEBUG = "_voice_resource_debug"
        private const val VOICE_CONFIG_FILE_SUFFIX_DEBUG = "_json_debug"
        const val VOICE_TEMP = "voice_temp"
        private const val HEADER_AUTHORIZATION = "Authorization"

        private const val TAG = "FirebaseRemoteConfigRep"
    }

}

sealed interface FileStatus {
    data object Initialization : FileStatus
    data class Downloading(val progress: Int) : FileStatus
    data object Extracting : FileStatus
    data object Completed : FileStatus
    data class Error(val error: FileError) : FileStatus
}

enum class FileError {
    LOCAL, REMOTE
}
