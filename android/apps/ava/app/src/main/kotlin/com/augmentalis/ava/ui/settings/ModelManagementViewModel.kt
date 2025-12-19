/**
 * Model Management ViewModel
 *
 * ViewModel for LLM model download and management UI.
 * Handles:
 * - Fetching available models from HuggingFace
 * - Managing downloads with progress tracking
 * - Listing installed models
 * - Storage management
 * - Model deletion
 *
 * Created: 2025-12-06
 * Author: AVA AI Team
 */

package com.augmentalis.ava.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.augmentalis.llm.download.LLMModelDownloader
import com.augmentalis.llm.download.ModelStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for model management screen
 */
@HiltViewModel
class ModelManagementViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)
    private val downloader = LLMModelDownloader(application, workManager)
    private val storageManager = ModelStorageManager(application)

    private val _uiState = MutableStateFlow<ModelManagementUiState>(ModelManagementUiState.Loading)
    val uiState: StateFlow<ModelManagementUiState> = _uiState.asStateFlow()

    private val _storageInfo = MutableStateFlow<ModelStorageManager.StorageInfo?>(null)
    val storageInfo: StateFlow<ModelStorageManager.StorageInfo?> = _storageInfo.asStateFlow()

    val downloadStates = downloader.downloadStates

    init {
        loadModels()
        loadStorageInfo()
    }

    /**
     * Load available and installed models
     */
    fun loadModels() {
        viewModelScope.launch {
            try {
                _uiState.value = ModelManagementUiState.Loading

                val available = downloader.getAvailableModels()
                val installed = downloader.getInstalledModels()

                _uiState.value = ModelManagementUiState.Success(
                    availableModels = available,
                    installedModels = installed
                )

                Timber.i("Loaded ${available.size} available models, ${installed.size} installed")

            } catch (e: Exception) {
                Timber.e(e, "Failed to load models")
                _uiState.value = ModelManagementUiState.Error(
                    message = "Failed to load models: ${e.message}"
                )
            }
        }
    }

    /**
     * Load storage information
     */
    fun loadStorageInfo() {
        viewModelScope.launch {
            try {
                val info = storageManager.getModelStorageUsage()
                _storageInfo.value = info
            } catch (e: Exception) {
                Timber.e(e, "Failed to load storage info")
            }
        }
    }

    /**
     * Start downloading a model
     */
    fun downloadModel(modelInfo: LLMModelDownloader.ModelInfo) {
        viewModelScope.launch {
            try {
                val result = downloader.downloadModel(modelInfo)
                if (result is com.augmentalis.ava.core.common.Result.Error) {
                    Timber.e("Download failed: ${result.message}")
                    // Show error to user
                } else {
                    Timber.i("Started download: ${modelInfo.modelId}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start download")
            }
        }
    }

    /**
     * Cancel a download
     */
    fun cancelDownload(modelId: String) {
        viewModelScope.launch {
            try {
                downloader.cancelDownload(modelId)
                Timber.i("Cancelled download: $modelId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to cancel download")
            }
        }
    }

    /**
     * Delete an installed model
     */
    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            try {
                val result = downloader.deleteModel(modelId)
                if (result is com.augmentalis.ava.core.common.Result.Success) {
                    Timber.i("Deleted model: $modelId")
                    // Refresh model list
                    loadModels()
                    loadStorageInfo()
                } else {
                    Timber.e("Failed to delete model: $modelId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete model")
            }
        }
    }

    /**
     * Check if model is installed
     */
    fun isModelInstalled(modelId: String): Boolean {
        val state = _uiState.value
        return if (state is ModelManagementUiState.Success) {
            state.installedModels.any { it.first == modelId }
        } else {
            false
        }
    }

    /**
     * Get available storage bytes
     */
    val availableStorageBytes: Long
        get() = _storageInfo.value?.availableBytes ?: 0L
}

/**
 * UI state for model management screen
 */
sealed class ModelManagementUiState {
    object Loading : ModelManagementUiState()

    data class Success(
        val availableModels: List<LLMModelDownloader.ModelInfo>,
        val installedModels: List<Pair<String, Long>>
    ) : ModelManagementUiState()

    data class Error(
        val message: String
    ) : ModelManagementUiState()
}
