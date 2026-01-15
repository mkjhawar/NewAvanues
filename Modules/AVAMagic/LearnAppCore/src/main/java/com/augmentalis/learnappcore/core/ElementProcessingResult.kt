/**
 * ElementProcessingResult.kt - Result of element processing by LearnAppCore
 *
 * Data class representing the outcome of processing an element.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-04
 * Moved to LearnAppCore: 2025-12-11
 * Related: JIT-LearnApp Merge (ADR-001)
 *
 * @since 1.1.0 (JIT-LearnApp Merge)
 */

package com.augmentalis.learnappcore.core

import com.augmentalis.database.dto.GeneratedCommandDTO

/**
 * Element Processing Result
 *
 * Result of processing an element through LearnAppCore.
 *
 * ## Success Case:
 * ```kotlin
 * ElementProcessingResult(
 *     uuid = "com.example.app.button-a7f3e2c1d4b5",
 *     command = GeneratedCommandDTO(...),
 *     success = true,
 *     error = null
 * )
 * ```
 *
 * ## Failure Case - No Label:
 * ```kotlin
 * ElementProcessingResult(
 *     uuid = "com.example.app.element-hash",
 *     command = null,
 *     success = false,
 *     error = "No label found for command"
 * )
 * ```
 *
 * ## Failure Case - Exception:
 * ```kotlin
 * ElementProcessingResult(
 *     uuid = "",
 *     command = null,
 *     success = false,
 *     error = "Database insert failed: ..."
 * )
 * ```
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
