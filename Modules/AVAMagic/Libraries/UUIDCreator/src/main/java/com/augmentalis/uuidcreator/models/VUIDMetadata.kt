package com.augmentalis.uuidcreator.models

/**
 * Additional metadata for UI elements
 */
data class VUIDMetadata(
    val label: String? = null,
    val hint: String? = null,
    val role: String? = null,
    val state: Map<String, Any> = emptyMap(),
    val attributes: Map<String, String> = emptyMap(),
    val accessibility: VUIDAccessibility? = null,
    val gestures: List<String> = emptyList(),
    val voiceCommands: List<String> = emptyList()
)

/**
 * Accessibility information
 */
data class VUIDAccessibility(
    val contentDescription: String? = null,
    val isImportantForAccessibility: Boolean = true,
    val isClickable: Boolean = false,
    val isFocusable: Boolean = false,
    val isScrollable: Boolean = false,
    val liveRegion: String? = null
)