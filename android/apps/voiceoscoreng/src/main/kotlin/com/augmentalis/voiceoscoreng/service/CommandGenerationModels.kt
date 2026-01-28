package com.augmentalis.voiceoscoreng.service

import com.augmentalis.commandmanager.DisplayCommand
import com.augmentalis.commandmanager.QuantizedCommand

/**
 * App-specific data models for command generation results.
 *
 * Core models (AppVersionInfo, IncrementalCommandResult) are in VoiceOSCore.
 * This file contains app-specific result types with overlay support.
 */

/**
 * Result of command generation containing all generated commands
 * and metadata for UI display, persistence, and overlay rendering.
 *
 * App-specific: includes overlayItems for visual badge display.
 */
data class CommandGenerationResult(
    val uiCommands: List<DisplayCommand>,
    val allQuantizedCommands: List<QuantizedCommand>,
    val staticCommands: List<QuantizedCommand>,
    val dynamicCommands: List<QuantizedCommand>,
    val indexCommands: List<QuantizedCommand>,
    val labelCommands: List<QuantizedCommand>,
    val numericCommands: List<QuantizedCommand>,
    val overlayItems: List<OverlayStateManager.NumberOverlayItem>
)

/**
 * Result of incremental command update (app-specific, simplified).
 *
 * For full result with merged commands, use IncrementalCommandResult from VoiceOSCore.
 */
data class IncrementalUpdateResult(
    val totalCommands: Int,
    val newCommands: Int,
    val preservedCommands: Int,
    val removedCommands: Int
)
