/**
 * CleanupPreviewUiState.kt - UI state models for cleanup preview screen
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-15
 *
 * P2 Task 2.2: Cleanup preview screen with statistics and safety indicators
 */

package com.augmentalis.voiceoscore.cleanup.ui

import android.graphics.drawable.Drawable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * UI state for cleanup preview screen with sealed class hierarchy
 */
sealed class CleanupPreviewUiState {
    /**
     * Loading state while fetching cleanup preview
     */
    data object Loading : CleanupPreviewUiState()

    /**
     * Preview state showing statistics and affected apps
     */
    data class Preview(
        val statistics: CleanupStatistics,
        val affectedApps: List<AffectedAppInfo>,
        val safetyLevel: SafetyLevel,
        val gracePeriodDays: Int,
        val keepUserApproved: Boolean
    ) : CleanupPreviewUiState()

    /**
     * Executing state during cleanup operation
     */
    data class Executing(
        val progress: Int,  // 0-100
        val currentApp: String
    ) : CleanupPreviewUiState()

    /**
     * Success state after cleanup completes
     */
    data class Success(
        val deletedCount: Int,
        val preservedCount: Int,
        val durationMs: Long
    ) : CleanupPreviewUiState() {
        fun getDurationSeconds(): String {
            return String.format("%.1fs", durationMs / 1000.0)
        }
    }

    /**
     * Error state if cleanup fails
     */
    data class Error(
        val message: String,
        val canRetry: Boolean
    ) : CleanupPreviewUiState()
}

/**
 * Cleanup statistics for preview display
 */
data class CleanupStatistics(
    val commandsToDelete: Int,
    val commandsToPreserve: Int,
    val appsAffected: Int,
    val deletionPercentage: Int,  // 0-100
    val estimatedSizeMB: Double
) {
    /**
     * Get deletion rate text
     */
    fun getDeletionRateText(): String = "$deletionPercentage% deletion rate"

    /**
     * Get total commands count
     */
    fun getTotalCommands(): Int = commandsToDelete + commandsToPreserve

    /**
     * Get formatted size estimate
     */
    fun getFormattedSize(): String {
        return when {
            estimatedSizeMB < 0.1 -> "${(estimatedSizeMB * 1024).toInt()} KB"
            estimatedSizeMB < 1.0 -> String.format("%.1f MB", estimatedSizeMB)
            else -> String.format("%.2f MB", estimatedSizeMB)
        }
    }
}

/**
 * Information about app affected by cleanup
 */
data class AffectedAppInfo(
    val packageName: String,
    val appName: String,
    val commandsToDelete: Int,
    val icon: Drawable? = null
)

/**
 * Safety level based on deletion percentage
 */
enum class SafetyLevel(
    val displayName: String,
    val color: Color,
    val icon: ImageVector,
    val description: String
) {
    SAFE(
        displayName = "Safe",
        color = Color(0xFF4CAF50),  // Green
        icon = Icons.Default.CheckCircle,
        description = "Low impact cleanup (<10% deletion)"
    ),
    MODERATE(
        displayName = "Moderate",
        color = Color(0xFFFFC107),  // Yellow/Amber
        icon = Icons.Default.Warning,
        description = "Medium impact cleanup (10-50% deletion)"
    ),
    HIGH_RISK(
        displayName = "High Risk",
        color = Color(0xFFFF5722),  // Red/Deep Orange
        icon = Icons.Default.Error,
        description = "High impact cleanup (>50% deletion)"
    );

    /**
     * Get color with opacity for background
     */
    fun getBackgroundColor(): Color {
        return color.copy(alpha = 0.1f)
    }
}

/**
 * Calculate safety level from deletion percentage
 */
fun calculateSafetyLevel(deletionPercentage: Int): SafetyLevel {
    return when {
        deletionPercentage < 10 -> SafetyLevel.SAFE
        deletionPercentage < 50 -> SafetyLevel.MODERATE
        else -> SafetyLevel.HIGH_RISK
    }
}
