/**
 * ElementProcessingResult.kt - Result of element processing by LearnAppCore
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Data class representing the outcome of processing an element.
 */

package com.augmentalis.voiceoscore.learnapp.core

import com.augmentalis.database.dto.GeneratedCommandDTO

/**
 * Element Processing Result
 *
 * Result of processing an element through LearnAppCore.
 *
 * @property uuid Generated UUID for the element (empty string on failure)
 * @property command Generated voice command (null if no label or error)
 * @property success Whether processing succeeded
 * @property error Error message if failed (null on success)
 */
data class ElementProcessingResult(
    val uuid: String,
    val command: GeneratedCommandDTO?,
    val success: Boolean,
    val error: String? = null
)
