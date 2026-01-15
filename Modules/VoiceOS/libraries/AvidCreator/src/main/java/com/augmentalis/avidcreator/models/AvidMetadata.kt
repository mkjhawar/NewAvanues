/**
 * AvidMetadata.kt - Additional metadata for AVID elements
 *
 * Updated: 2026-01-15 - Migrated to AVID naming
 */
package com.augmentalis.avidcreator.models

/**
 * Additional metadata for UI elements
 */
data class AvidMetadata(
    val label: String? = null,
    val hint: String? = null,
    val role: String? = null,
    val state: Map<String, Any> = emptyMap(),
    val attributes: Map<String, String> = emptyMap(),
    val accessibility: AvidAccessibility? = null,
    val gestures: List<String> = emptyList(),
    val voiceCommands: List<String> = emptyList()
)

/**
 * Accessibility information
 */
data class AvidAccessibility(
    val contentDescription: String? = null,
    val isImportantForAccessibility: Boolean = true,
    val isClickable: Boolean = false,
    val isFocusable: Boolean = false,
    val isScrollable: Boolean = false,
    val liveRegion: String? = null
)

