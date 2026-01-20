package com.augmentalis.voiceoscoreng.service

import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Data models for command generation results.
 * Extracted from DynamicCommandGenerator for SOLID compliance.
 */

/**
 * App version info holder.
 */
data class AppVersionInfo(
    val versionCode: Long,
    val versionName: String
)

/**
 * Result of command generation containing all generated commands
 * and metadata for UI display and persistence.
 */
data class CommandGenerationResult(
    val legacyCommands: List<GeneratedCommand>,
    val allQuantizedCommands: List<QuantizedCommand>,
    val staticCommands: List<QuantizedCommand>,
    val dynamicCommands: List<QuantizedCommand>,
    val indexCommands: List<QuantizedCommand>,
    val labelCommands: List<QuantizedCommand>,
    val numericCommands: List<QuantizedCommand>,
    val overlayItems: List<OverlayStateManager.NumberOverlayItem>
)

/**
 * Result of incremental command update (for scroll/content changes).
 */
data class IncrementalUpdateResult(
    val totalCommands: Int,
    val newCommands: Int,
    val preservedCommands: Int,
    val removedCommands: Int
)
