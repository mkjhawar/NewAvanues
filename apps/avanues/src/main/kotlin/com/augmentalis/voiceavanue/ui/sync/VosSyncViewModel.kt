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
import com.augmentalis.database.dto.PhraseSuggestionDTO
import com.augmentalis.database.dto.VosFileRegistryDTO
import com.augmentalis.database.repositories.IPhraseSuggestionRepository
import com.augmentalis.database.repositories.IVosFileRegistryRepository
import com.augmentalis.voiceavanue.data.AvanuesSettings
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.data.SftpCredentialStore
import com.augmentalis.voiceoscore.vos.sync.SftpAuthMode
import com.augmentalis.voiceoscore.vos.sync.SftpResult
import com.augmentalis.voiceoscore.vos.sync.SyncStatus
import com.augmentalis.voiceoscore.vos.sync.VosSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
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
    private val settingsRepository: AvanuesSettingsRepository,
    private val credentialStore: SftpCredentialStore,
    private val phraseSuggestionRepo: IPhraseSuggestionRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    val settings: StateFlow<AvanuesSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AvanuesSettings())

    private val _registryFiles = MutableStateFlow<List<VosFileRegistryDTO>>(emptyList())
    val registryFiles: StateFlow<List<VosFileRegistryDTO>> = _registryFiles.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _pendingSuggestionCount = MutableStateFlow(0L)
    val pendingSuggestionCount: StateFlow<Long> = _pendingSuggestionCount.asStateFlow()

    init {
        loadRegistryFiles()
        loadSuggestionCount()
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
            SftpAuthMode.SshKey(settings.vosSftpKeyPath, credentialStore.getPassphrase())
        } else {
            SftpAuthMode.Password(credentialStore.getPassword())
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
                authMode = buildAuthMode(s),
                hostKeyChecking = s.vosSftpHostKeyMode
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
                remotePath = s.vosSftpRemotePath,
                hostKeyChecking = s.vosSftpHostKeyMode
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
                downloadDir = downloadDir,
                hostKeyChecking = s.vosSftpHostKeyMode
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
                downloadDir = downloadDir,
                hostKeyChecking = s.vosSftpHostKeyMode
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

    // ==================== Periodic Sync ====================

    fun updateAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateVosAutoSyncEnabled(enabled)
            if (enabled) {
                val hours = settings.value.vosSyncIntervalHours.toLong().coerceAtLeast(1)
                VosSyncWorker.schedulePeriodic(appContext, hours)
                _actionMessage.value = "Auto-sync enabled (every ${hours}h)"
            } else {
                VosSyncWorker.cancel(appContext)
                _actionMessage.value = "Auto-sync disabled"
            }
        }
    }

    fun updateSyncInterval(hours: Int) {
        viewModelScope.launch {
            settingsRepository.updateVosSyncIntervalHours(hours)
            if (settings.value.vosAutoSyncEnabled) {
                VosSyncWorker.schedulePeriodic(appContext, hours.toLong().coerceAtLeast(1))
                _actionMessage.value = "Sync interval updated to ${hours}h"
            }
        }
    }

    fun storePassword(password: String) {
        credentialStore.storePassword(password)
    }

    fun storePassphrase(passphrase: String) {
        credentialStore.storePassphrase(passphrase)
    }

    fun getPassword(): String = credentialStore.getPassword()

    fun getPassphrase(): String = credentialStore.getPassphrase()

    // ==================== Phrase Suggestions ====================

    private fun loadSuggestionCount() {
        viewModelScope.launch {
            _pendingSuggestionCount.value = phraseSuggestionRepo.getPendingCount()
        }
    }

    fun submitSuggestion(commandId: String, originalPhrase: String, suggestedPhrase: String, locale: String) {
        viewModelScope.launch {
            phraseSuggestionRepo.insert(
                PhraseSuggestionDTO(
                    id = 0,
                    commandId = commandId,
                    originalPhrase = originalPhrase,
                    suggestedPhrase = suggestedPhrase,
                    locale = locale,
                    createdAt = System.currentTimeMillis(),
                    status = "pending",
                    source = "user"
                )
            )
            loadSuggestionCount()
            _actionMessage.value = "Suggestion submitted"
        }
    }

    fun exportSuggestions() {
        viewModelScope.launch {
            val suggestions = phraseSuggestionRepo.getPendingByLocale(settings.value.voiceLocale)
            if (suggestions.isEmpty()) {
                _actionMessage.value = "No pending suggestions to export"
                return@launch
            }

            val json = org.json.JSONArray()
            for (s in suggestions) {
                val obj = org.json.JSONObject()
                obj.put("commandId", s.commandId)
                obj.put("originalPhrase", s.originalPhrase)
                obj.put("suggestedPhrase", s.suggestedPhrase)
                obj.put("locale", s.locale)
                obj.put("createdAt", s.createdAt)
                json.put(obj)
            }

            val dir = File(
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                ),
                "vos-suggestions"
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "suggestions-${settings.value.voiceLocale}-${System.currentTimeMillis()}.json")
            file.writeText(json.toString(2))
            _actionMessage.value = "Exported ${suggestions.size} suggestion(s) to Downloads/vos-suggestions/"
        }
    }
}
