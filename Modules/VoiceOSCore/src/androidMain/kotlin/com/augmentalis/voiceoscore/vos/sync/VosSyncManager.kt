/**
 * VosSyncManager.kt - Orchestrator for VOS file sync over SFTP
 *
 * Coordinates upload/download of VOS files between device and SFTP server.
 * Queries the VOS file registry for pending uploads, compares hashes with
 * the server manifest for downloads, and updates registry timestamps.
 *
 * Observable via syncStatus StateFlow for UI binding.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-11
 */
package com.augmentalis.voiceoscore.vos.sync

import android.util.Log
import com.augmentalis.database.repositories.IVosFileRegistryRepository
import com.augmentalis.voiceoscore.vos.VosFileImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private const val TAG = "VosSyncManager"

class VosSyncManager(
    private val sftpClient: VosSftpClient,
    private val registry: IVosFileRegistryRepository,
    importer: VosFileImporter?
) {
    @Volatile
    private var _importer: VosFileImporter? = importer

    /**
     * Late-bind the VOS file importer.
     * Called from AccessibilityService.onServiceReady() after DB initialization,
     * since VoiceCommandDaoAdapter is runtime-managed and not Hilt-injectable.
     */
    fun setImporter(importer: VosFileImporter) {
        _importer = importer
        Log.i(TAG, "VosFileImporter bound")
    }

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /**
     * Test connection to the SFTP server.
     * Connects, verifies, then disconnects.
     */
    suspend fun testConnection(
        host: String,
        port: Int,
        username: String,
        authMode: SftpAuthMode,
        hostKeyChecking: String = "accept-new"
    ): SftpResult<Unit> {
        _syncStatus.value = _syncStatus.value.copy(error = null)

        val result = sftpClient.connect(host, port, username, authMode, hostKeyChecking)
        return when (result) {
            is SftpResult.Success -> {
                _syncStatus.value = _syncStatus.value.copy(isConnected = true, error = null)
                sftpClient.disconnect()
                _syncStatus.value = _syncStatus.value.copy(isConnected = false)
                SftpResult.Success(Unit)
            }
            is SftpResult.Error -> {
                _syncStatus.value = _syncStatus.value.copy(
                    isConnected = false,
                    error = result.message
                )
                result
            }
        }
    }

    /**
     * Upload all local files that haven't been uploaded yet.
     * Queries registry for `uploaded_at IS NULL AND source = 'local'`,
     * uploads each file, then updates the uploaded_at timestamp.
     *
     * @return Number of files successfully uploaded.
     */
    suspend fun uploadLocalFiles(
        host: String,
        port: Int,
        username: String,
        authMode: SftpAuthMode,
        remotePath: String,
        hostKeyChecking: String = "accept-new"
    ): SftpResult<Int> {
        // Connect
        val connectResult = sftpClient.connect(host, port, username, authMode, hostKeyChecking)
        if (connectResult is SftpResult.Error) {
            _syncStatus.value = _syncStatus.value.copy(error = connectResult.message, isSyncing = false)
            return connectResult
        }
        _syncStatus.value = _syncStatus.value.copy(isConnected = true, isSyncing = true, error = null)

        try {
            val pendingFiles = registry.getNotUploaded()
            if (pendingFiles.isEmpty()) {
                Log.i(TAG, "No files to upload")
                _syncStatus.value = _syncStatus.value.copy(isSyncing = false)
                sftpClient.disconnect()
                _syncStatus.value = _syncStatus.value.copy(isConnected = false)
                return SftpResult.Success(0)
            }

            Log.i(TAG, "Uploading ${pendingFiles.size} files...")
            var uploadedCount = 0
            val errors = mutableListOf<String>()

            // Fetch existing manifest to update
            val manifestResult = sftpClient.fetchManifest(remotePath)
            val existingManifest = (manifestResult as? SftpResult.Success)?.data ?: ServerManifest()
            val manifestEntries = existingManifest.files.toMutableList()

            for ((index, dto) in pendingFiles.withIndex()) {
                val localPath = dto.localPath
                if (localPath == null || !File(localPath).exists()) {
                    Log.w(TAG, "Skipping ${dto.fileName} — local file missing: $localPath")
                    errors.add("${dto.fileName}: local file missing")
                    continue
                }

                _syncStatus.value = _syncStatus.value.copy(
                    progress = SyncProgress(
                        currentFile = dto.fileName,
                        currentIndex = index + 1,
                        totalFiles = pendingFiles.size
                    )
                )

                val remoteFilePath = "$remotePath/${dto.fileName}"
                val uploadResult = sftpClient.uploadFile(
                    localPath = localPath,
                    remotePath = remoteFilePath,
                    onProgress = { transferred, total ->
                        _syncStatus.value = _syncStatus.value.copy(
                            progress = _syncStatus.value.progress?.copy(
                                bytesTransferred = transferred,
                                totalBytes = total
                            )
                        )
                    }
                )

                when (uploadResult) {
                    is SftpResult.Success -> {
                        val now = System.currentTimeMillis()
                        registry.updateUploadedAt(dto.id, now)
                        uploadedCount++

                        // Add to manifest
                        manifestEntries.add(
                            ManifestEntry(
                                hash = dto.contentHash,
                                filename = dto.fileName,
                                size = File(localPath).length(),
                                uploadedAt = now,
                                domain = dto.domain,
                                locale = if (dto.fileType == "app") dto.fileId else null
                            )
                        )
                    }
                    is SftpResult.Error -> {
                        Log.w(TAG, "Failed to upload ${dto.fileName}: ${uploadResult.message}")
                        errors.add("${dto.fileName}: ${uploadResult.message}")
                    }
                }
            }

            // Upload updated manifest
            val updatedManifest = existingManifest.copy(
                files = manifestEntries,
                lastUpdated = System.currentTimeMillis()
            )
            sftpClient.uploadManifest(updatedManifest, remotePath)

            Log.i(TAG, "Upload complete: $uploadedCount/${pendingFiles.size} files")
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                progress = null,
                error = if (errors.isNotEmpty()) "${errors.size} error(s)" else null
            )

            sftpClient.disconnect()
            _syncStatus.value = _syncStatus.value.copy(isConnected = false)
            return SftpResult.Success(uploadedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Upload error: ${e.message}", e)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                error = "Upload error: ${e.message}"
            )
            sftpClient.disconnect()
            _syncStatus.value = _syncStatus.value.copy(isConnected = false)
            return SftpResult.Error("Upload error: ${e.message}", e)
        }
    }

    /**
     * Download new files from the server that don't exist locally.
     * Fetches manifest, compares hashes with local registry, downloads + imports new files.
     *
     * @return Number of files successfully downloaded.
     */
    suspend fun downloadNewFiles(
        host: String,
        port: Int,
        username: String,
        authMode: SftpAuthMode,
        remotePath: String,
        downloadDir: String,
        hostKeyChecking: String = "accept-new"
    ): SftpResult<Int> {
        val connectResult = sftpClient.connect(host, port, username, authMode, hostKeyChecking)
        if (connectResult is SftpResult.Error) {
            _syncStatus.value = _syncStatus.value.copy(error = connectResult.message, isSyncing = false)
            return connectResult
        }
        _syncStatus.value = _syncStatus.value.copy(isConnected = true, isSyncing = true, error = null)

        try {
            // Fetch manifest
            val manifestResult = sftpClient.fetchManifest(remotePath)
            val manifest = when (manifestResult) {
                is SftpResult.Success -> manifestResult.data
                is SftpResult.Error -> {
                    _syncStatus.value = _syncStatus.value.copy(
                        isSyncing = false,
                        error = manifestResult.message
                    )
                    sftpClient.disconnect()
                    _syncStatus.value = _syncStatus.value.copy(isConnected = false)
                    return manifestResult
                }
            }

            if (manifest.files.isEmpty()) {
                Log.i(TAG, "No files on server")
                _syncStatus.value = _syncStatus.value.copy(isSyncing = false)
                sftpClient.disconnect()
                _syncStatus.value = _syncStatus.value.copy(isConnected = false)
                return SftpResult.Success(0)
            }

            // Compare hashes — find files we don't have
            val newFiles = mutableListOf<ManifestEntry>()
            for (entry in manifest.files) {
                val exists = registry.existsByHash(entry.hash)
                if (!exists) {
                    newFiles.add(entry)
                }
            }

            if (newFiles.isEmpty()) {
                Log.i(TAG, "All server files already present locally")
                _syncStatus.value = _syncStatus.value.copy(isSyncing = false)
                sftpClient.disconnect()
                _syncStatus.value = _syncStatus.value.copy(isConnected = false)
                return SftpResult.Success(0)
            }

            Log.i(TAG, "Downloading ${newFiles.size} new files...")
            var downloadedCount = 0
            val errors = mutableListOf<String>()

            // Ensure download directory exists
            val downloadDirFile = File(downloadDir)
            if (!downloadDirFile.exists()) {
                downloadDirFile.mkdirs()
            }

            for ((index, entry) in newFiles.withIndex()) {
                _syncStatus.value = _syncStatus.value.copy(
                    progress = SyncProgress(
                        currentFile = entry.filename,
                        currentIndex = index + 1,
                        totalFiles = newFiles.size
                    )
                )

                val remoteFilePath = "$remotePath/${entry.filename}"
                val localFilePath = "$downloadDir/${entry.filename}"

                val downloadResult = sftpClient.downloadFile(
                    remotePath = remoteFilePath,
                    localPath = localFilePath,
                    onProgress = { transferred, total ->
                        _syncStatus.value = _syncStatus.value.copy(
                            progress = _syncStatus.value.progress?.copy(
                                bytesTransferred = transferred,
                                totalBytes = total
                            )
                        )
                    }
                )

                when (downloadResult) {
                    is SftpResult.Success -> {
                        // Import the downloaded file into the command DB
                        if (_importer != null) {
                            val importResult = _importer!!.importFromFile(localFilePath)
                            if (importResult.success) {
                                downloadedCount++
                                Log.d(TAG, "Downloaded and imported ${entry.filename}")
                            } else {
                                Log.w(TAG, "Downloaded but import failed for ${entry.filename}: ${importResult.errorMessage}")
                                errors.add("${entry.filename}: import failed — ${importResult.errorMessage}")
                            }
                        } else {
                            downloadedCount++
                            Log.d(TAG, "Downloaded ${entry.filename} (no importer available)")
                        }
                    }
                    is SftpResult.Error -> {
                        Log.w(TAG, "Failed to download ${entry.filename}: ${downloadResult.message}")
                        errors.add("${entry.filename}: ${downloadResult.message}")
                    }
                }
            }

            Log.i(TAG, "Download complete: $downloadedCount/${newFiles.size} files")
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                progress = null,
                error = if (errors.isNotEmpty()) "${errors.size} error(s)" else null
            )

            sftpClient.disconnect()
            _syncStatus.value = _syncStatus.value.copy(isConnected = false)
            return SftpResult.Success(downloadedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                error = "Download error: ${e.message}"
            )
            sftpClient.disconnect()
            _syncStatus.value = _syncStatus.value.copy(isConnected = false)
            return SftpResult.Error("Download error: ${e.message}", e)
        }
    }

    /**
     * Full sync: upload local files, then download new server files.
     */
    suspend fun syncAll(
        host: String,
        port: Int,
        username: String,
        authMode: SftpAuthMode,
        remotePath: String,
        downloadDir: String,
        hostKeyChecking: String = "accept-new"
    ): SftpResult<SyncResult> {
        val errors = mutableListOf<String>()

        val uploadResult = uploadLocalFiles(host, port, username, authMode, remotePath, hostKeyChecking)
        val uploadedCount = when (uploadResult) {
            is SftpResult.Success -> uploadResult.data
            is SftpResult.Error -> {
                errors.add("Upload: ${uploadResult.message}")
                0
            }
        }

        val downloadResult = downloadNewFiles(host, port, username, authMode, remotePath, downloadDir, hostKeyChecking)
        val downloadedCount = when (downloadResult) {
            is SftpResult.Success -> downloadResult.data
            is SftpResult.Error -> {
                errors.add("Download: ${downloadResult.message}")
                0
            }
        }

        val result = SyncResult(
            uploadedCount = uploadedCount,
            downloadedCount = downloadedCount,
            errors = errors
        )

        _syncStatus.value = _syncStatus.value.copy(
            lastSyncTime = System.currentTimeMillis()
        )

        return if (errors.isEmpty()) {
            SftpResult.Success(result)
        } else {
            SftpResult.Success(result) // Still success, errors are in the result
        }
    }
}
