/**
 * CoreCommandResult.kt - Result of core command generation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-20
 *
 * Contains all generated commands without UI/overlay dependencies.
 * App layer can extend this with overlay items as needed.
 */
package com.augmentalis.voiceoscore

/**
 * Result of core command generation.
 *
 * Contains all generated commands and speech phrases.
 * Overlay handling is delegated to app layer.
 */
data class CoreCommandResult(
    /** All generated quantized commands */
    val allCommands: List<QuantizedCommand>,
    /** Commands that should be persisted (stable elements) */
    val staticCommands: List<QuantizedCommand>,
    /** Commands that are dynamic (position-based, transient) */
    val dynamicCommands: List<QuantizedCommand>,
    /** Commands for list index access ("first", "second", etc.) */
    val indexCommands: List<QuantizedCommand>,
    /** Commands for list item labels */
    val labelCommands: List<QuantizedCommand>,
    /** Commands for numeric selection ("1", "2", "3", etc.) */
    val numericCommands: List<QuantizedCommand>,
    /** All phrases for speech engine grammar */
    val speechPhrases: List<String>
)

/**
 * Result of incremental command update (for scroll/content changes).
 */
data class IncrementalCommandResult(
    /** Total commands after merge */
    val totalCommands: Int,
    /** Number of newly generated commands */
    val newCommands: Int,
    /** Number of commands preserved from previous state */
    val preservedCommands: Int,
    /** Number of commands removed */
    val removedCommands: Int,
    /** Merged command list */
    val mergedCommands: List<QuantizedCommand>,
    /** All phrases for speech engine grammar */
    val speechPhrases: List<String>
)

/**
 * App version information.
 *
 * Used for tracking which app version commands were generated for.
 */
data class AppVersionInfo(
    val versionCode: Long,
    val versionName: String
)
