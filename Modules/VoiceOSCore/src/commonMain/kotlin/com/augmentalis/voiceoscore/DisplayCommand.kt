/**
 * DisplayCommand.kt - UI display command format
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-20
 *
 * Command format for UI display and exploration results.
 * Contains full element reference for rich UI presentation.
 *
 * Note: This is different from GeneratedCommand (in ICommandGenerator.kt)
 * which is a flat database record for JIT learning persistence.
 *
 * For voice recognition and execution, use QuantizedCommand instead.
 */
package com.augmentalis.voiceoscore

/**
 * Voice command for UI display with full element reference.
 *
 * Used for UI display in exploration results and AVU formatting.
 * Contains full ElementInfo reference for rich presentation.
 *
 * @param phrase Primary voice command phrase (e.g., "tap Settings")
 * @param alternates Alternative phrases that trigger same action
 * @param targetVuid Element fingerprint/identifier
 * @param action Action type (tap, toggle, scroll, focus, etc.)
 * @param element Full element information for rich UI display
 * @param derivedLabel Human-readable label derived from element
 */
data class DisplayCommand(
    val phrase: String,
    val alternates: List<String>,
    val targetVuid: String,
    val action: String,
    val element: ElementInfo,
    val derivedLabel: String = ""
)
