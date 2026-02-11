/**
 * VosSyncViewModel.kt - ViewModel for VOS Sync management screen
 *
 * Bridges VosSyncManager, VOS file registry, and settings to the UI.
 * Exposes sync status, file list, and action methods for the sync screen.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-11
 */
package com.augmentalis.voiceavanue.ui.sync

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.database.dto.VosFileRegistryDTO
import com.augmentalis.database.repositories.IVosFileRegistryRepository
import com.augmentalis.voiceavanue.data.AvanuesSettings
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceoscore.vos.sync.SftpAuthMode
import com.augmentalis.voiceoscore.vos.sync.SftpResult
import com.augmentalis.voiceoscore.vos.sync.SyncStatus
import com.augmentalis.voiceoscore.vos.sync.VosSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VosSyncViewModel @Inject constructor(
    private val syncManager: VosSyncManager,
    private val registry: IVosFileRegistryRepository,
    private val settingsRepository: AvanuesSettingsRepository
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    val settings: StateFlow<AvanuesSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AvanuesSettings())

    private val _registryFiles = MutableStateFlow<List<VosFileRegistryDTO>>(emptyList())
    val registryFiles: StateFlow<List<VosFileRegistryDTO>> = _registryFiles.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadRegistryFiles()
    }

    fun loadRegistryFiles() {
        viewModelScope.launch {
            _registryFiles.value = registry.getAll()
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    private fun buildAuthMode(settings: AvanuesSettings): SftpAuthMode {
        return if (settings.vosSftpKeyPath.isNotBlank()) {
            SftpAuthMode.SshKey(settings.vosSftpKeyPath)
        } else {
            // Fallback: no auth configured, try empty password (user must configure)
            SftpAuthMode.Password("")
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            val s = settings.value
            if (s.vosSftpHost.isBlank()) {
                _actionMessage.value = "SFTP host is not configured"
                return@launch
            }

            _actionMessage.value = "Testing connection..."
            val result = syncManager.testConnection(
                host = s.vosSftpHost,
                port = s.vosSftpPort,
                username = s.vosSftpUsername,
                authMode = buildAuthMode(s)
            )

            _actionMessage.value = when (result) {
                is SftpResult.Success -> "Connection successful!"
                is SftpResult.Error -> "Connection failed: ${result.message}"
            }
        }
    }

    fun uploadAll() {
        viewModelScope.launch {
            val s = settings.value
            if (s.vosSftpHost.isBlank()) {
                _actionMessage.value = "SFTP host is not configured"
                return@launch
            }

            _actionMessage.value = "Uploading..."
            val result = syncManager.uploadLocalFiles(
                host = s.vosSftpHost,
                port = s.vosSftpPort,
                username = s.vosSftpUsername,
                authMode = buildAuthMode(s),
                remotePath = s.vosSftpRemotePath
            )

            when (result) {
                is SftpResult.Success -> {
                    _actionMessage.value = "Uploaded ${result.data} file(s)"
                    settingsRepository.updateVosLastSyncTime(System.currentTimeMillis())
                    loadRegistryFiles()
                }
                is SftpResult.Error -> {
                    _actionMessage.value = "Upload failed: ${result.message}"
                }
            }
        }
    }

    fun downloadAll() {
        viewModelScope.launch {
            val s = settings.value
            if (s.vosSftpHost.isBlank()) {
                _actionMessage.value = "SFTP host is not configured"
                return@launch
            }

            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "commands"
            ).absolutePath

            _actionMessage.value = "Downloading..."
            val result = syncManager.downloadNewFiles(
                host = s.vosSftpHost,
                port = s.vosSftpPort,
                username = s.vosSftpUsername,
                authMode = buildAuthMode(s),
                remotePath = s.vosSftpRemotePath,
                downloadDir = downloadDir
            )

            when (result) {
                is SftpResult.Success -> {
                    _actionMessage.value = "Downloaded ${result.data} file(s)"
                    settingsRepository.updateVosLastSyncTime(System.currentTimeMillis())
                    loadRegistryFiles()
                }
                is SftpResult.Error -> {
                    _actionMessage.value = "Download failed: ${result.message}"
                }
            }
        }
    }

    fun fullSync() {
        viewModelScope.launch {
            val s = settings.value
            if (s.vosSftpHost.isBlank()) {
                _actionMessage.value = "SFTP host is not configured"
                return@launch
            }

            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "commands"
            ).absolutePath

            _actionMessage.value = "Syncing..."
            val result = syncManager.syncAll(
                host = s.vosSftpHost,
                port = s.vosSftpPort,
                username = s.vosSftpUsername,
                authMode = buildAuthMode(s),
                remotePath = s.vosSftpRemotePath,
                downloadDir = downloadDir
            )

            when (result) {
                is SftpResult.Success -> {
                    val r = result.data
                    val msg = buildString {
                        append("Sync complete: ")
                        append("${r.uploadedCount} uploaded, ${r.downloadedCount} downloaded")
                        if (r.hasErrors) append(" (${r.errors.size} error(s))")
                    }
                    _actionMessage.value = msg
                    settingsRepository.updateVosLastSyncTime(System.currentTimeMillis())
                    loadRegistryFiles()
                }
                is SftpResult.Error -> {
                    _actionMessage.value = "Sync failed: ${result.message}"
                }
            }
        }
    }
}
