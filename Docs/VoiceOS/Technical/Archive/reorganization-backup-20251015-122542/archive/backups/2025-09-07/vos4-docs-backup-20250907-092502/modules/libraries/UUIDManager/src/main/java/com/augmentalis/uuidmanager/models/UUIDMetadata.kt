package com.augmentalis.uuidmanager.models

/**
 * Additional metadata for UI elements
 */
data class UUIDMetadata(
    val label: String? = null,
    val hint: String? = null,
    val role: String? = null,
    val state: Map<String, Any> = emptyMap(),
    val attributes: Map<String, String> = emptyMap(),
    val accessibility: UUIDAccessibility? = null,
    val gestures: List<String> = emptyList(),
    val voiceCommands: List<String> = emptyList()
)

/**
 * Accessibility information
 */
data class UUIDAccessibility(
    val contentDescription: String? = null,
    val isImportantForAccessibility: Boolean = true,
    val isClickable: Boolean = false,
    val isFocusable: Boolean = false,
    val isScrollable: Boolean = false,
    val liveRegion: String? = null
)