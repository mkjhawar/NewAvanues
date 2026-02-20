/**
 * FileZipManager.kt - Zip file extraction utility for Vivoka VSDK
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 */
package com.augmentalis.speechrecognition.vivoka.model


import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class FileZipManager {

    suspend fun unzip(zipFile: File?, toDir: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "unzip to dir = $toDir")
                if (!toDir.isDirectory) {
                    toDir.mkdirs()
                }

                val canonicalDestDir = toDir.canonicalPath

                ZipFile(zipFile).use { zf ->
                    val entries = zf.entries()
                    var extractedCount = 0

                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement() as ZipEntry
                        val unzipFile = File(toDir, entry.name)

                        // Security: prevent zip path traversal (Zip Slip)
                        val canonicalEntryPath = unzipFile.canonicalPath
                        if (!canonicalEntryPath.startsWith(canonicalDestDir + File.separator) &&
                            canonicalEntryPath != canonicalDestDir) {
                            Log.e(TAG, "Zip entry outside target dir: ${entry.name}")
                            continue
                        }

                        Log.d(TAG, "Unzipping: ${entry.name}")

                        if (entry.isDirectory) {
                            if (!unzipFile.isDirectory) {
                                unzipFile.mkdirs()
                            }
                        } else {
                            // Ensure parent directories exist
                            unzipFile.parentFile?.let { parent ->
                                if (!parent.isDirectory) parent.mkdirs()
                            }

                            zf.getInputStream(entry).use { inputStream ->
                                FileOutputStream(unzipFile, false).use { outputStream ->
                                    val buffer = ByteArray(BUFFER_SIZE)
                                    var bytesRead: Int
                                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                        outputStream.write(buffer, 0, bytesRead)
                                        outputStream.flush()
                                    }
                                }
                            }
                            extractedCount++
                        }
                    }
                    Log.d(TAG, "Unzipped files count: $extractedCount")
                    return@withContext extractedCount != 0
                }
            } catch (e: IOException) {
                Log.e(TAG, "Unzip exception", e)
            }
            return@withContext false
        }
    }

    companion object {
        private const val BUFFER_SIZE = 8 * 1024
        private const val TAG = "ZipManager"
    }

}
