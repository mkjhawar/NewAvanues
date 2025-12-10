package com.augmentalis.ava.features.teach

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Phase 1.1: ViewModel Extensions for Advanced Training Features
 *
 * Adds bulk import/export, analytics, and similarity analysis to TeachAvaViewModel.
 * These extensions keep the main ViewModel clean while adding advanced functionality.
 */

/**
 * Extension properties and methods for analytics
 */
class TeachAvaAnalytics(
    private val viewModel: TeachAvaViewModel,
    private val context: Context
) {
    private val analyticsCalculator = TrainingAnalyticsCalculator()
    private val similarityAnalyzer = IntentSimilarityAnalyzer()
    private val importExportManager = BulkImportExportManager(context)

    private val _analytics = MutableStateFlow<TrainingAnalytics?>(null)
    val analytics: StateFlow<TrainingAnalytics?> = _analytics.asStateFlow()

    private val _intentDistribution = MutableStateFlow<List<IntentDistribution>>(emptyList())
    val intentDistribution: StateFlow<List<IntentDistribution>> = _intentDistribution.asStateFlow()

    private val _similarityReport = MutableStateFlow<IntentSimilarityReport?>(null)
    val similarityReport: StateFlow<IntentSimilarityReport?> = _similarityReport.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    private val _exportResult = MutableStateFlow<Result<Int>?>(null)
    val exportResult: StateFlow<Result<Int>?> = _exportResult.asStateFlow()

    /**
     * Calculate analytics from current examples
     */
    fun calculateAnalytics(examples: List<com.augmentalis.ava.core.domain.model.TrainExample>) {
        viewModel.viewModelScope.launch {
            val analytics = analyticsCalculator.calculateAnalytics(examples)
            val distribution = analyticsCalculator.calculateIntentDistribution(examples)

            _analytics.value = analytics
            _intentDistribution.value = distribution
        }
    }

    /**
     * Analyze intent similarities
     */
    fun analyzeSimilarities(
        examples: List<com.augmentalis.ava.core.domain.model.TrainExample>,
        threshold: Double = 0.6
    ) {
        viewModel.viewModelScope.launch {
            val report = similarityAnalyzer.analyzeSimilarities(examples, threshold)
            _similarityReport.value = report
        }
    }

    /**
     * Find similar intents to a specific intent
     */
    fun findSimilarIntents(
        targetIntent: String,
        examples: List<com.augmentalis.ava.core.domain.model.TrainExample>,
        limit: Int = 5
    ): List<IntentSimilarity> {
        return similarityAnalyzer.findSimilarIntents(targetIntent, examples, limit)
    }

    /**
     * Export training examples to JSON
     */
    fun exportToJson(
        examples: List<com.augmentalis.ava.core.domain.model.TrainExample>,
        uri: Uri
    ) {
        viewModel.viewModelScope.launch {
            val result = importExportManager.exportToJson(examples, uri)
            _exportResult.value = result
        }
    }

    /**
     * Export training examples to CSV
     */
    fun exportToCsv(
        examples: List<com.augmentalis.ava.core.domain.model.TrainExample>,
        uri: Uri
    ) {
        viewModel.viewModelScope.launch {
            val result = importExportManager.exportToCsv(examples, uri)
            _exportResult.value = result
        }
    }

    /**
     * Import training examples from JSON
     */
    suspend fun importFromJson(
        uri: Uri,
        existingExamples: List<com.augmentalis.ava.core.domain.model.TrainExample>
    ): Result<ImportResult> {
        val existingHashes = existingExamples.map { it.exampleHash }.toSet()
        val result = importExportManager.importFromJson(uri, existingHashes)

        if (result is Result.Success) {
            _importResult.value = result.data
        }

        return result
    }

    /**
     * Import training examples from CSV
     */
    suspend fun importFromCsv(
        uri: Uri,
        existingExamples: List<com.augmentalis.ava.core.domain.model.TrainExample>
    ): Result<ImportResult> {
        val existingHashes = existingExamples.map { it.exampleHash }.toSet()
        val result = importExportManager.importFromCsv(uri, existingHashes)

        if (result is Result.Success) {
            _importResult.value = result.data
        }

        return result
    }

    /**
     * Clear import result
     */
    fun clearImportResult() {
        _importResult.value = null
    }

    /**
     * Clear export result
     */
    fun clearExportResult() {
        _exportResult.value = null
    }
}

/**
 * Helper to get examples from current UI state
 */
fun TeachAvaViewModel.getCurrentExamples(): List<com.augmentalis.ava.core.domain.model.TrainExample> {
    return when (val state = uiState.value) {
        is TeachAvaUiState.Success -> state.examples
        else -> emptyList()
    }
}
