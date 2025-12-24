package com.augmentalis.uuidcreator.models

/**
 * Additional metadata for UI elements (VUID migration)
 *
 * Migration: UUID → VUID (VoiceUniqueID)
 * Created: 2025-12-23
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
) {
    /**
     * Convert to deprecated UUIDMetadata for backwards compatibility
     */
    fun toUUIDMetadata(): UUIDMetadata = UUIDMetadata(
        label = label,
        hint = hint,
        role = role,
        state = state,
        attributes = attributes,
        accessibility = accessibility?.toUUIDAccessibility(),
        gestures = gestures,
        voiceCommands = voiceCommands
    )

    companion object {
        /**
         * Convert from deprecated UUIDMetadata
         */
        fun fromUUIDMetadata(metadata: UUIDMetadata): VUIDMetadata = VUIDMetadata(
            label = metadata.label,
            hint = metadata.hint,
            role = metadata.role,
            state = metadata.state,
            attributes = metadata.attributes,
            accessibility = metadata.accessibility?.let { VUIDAccessibility.fromUUIDAccessibility(it) },
            gestures = metadata.gestures,
            voiceCommands = metadata.voiceCommands
        )
    }
}

/**
 * Accessibility information (VUID migration)
 */
data class VUIDAccessibility(
    val contentDescription: String? = null,
    val isImportantForAccessibility: Boolean = true,
    val isClickable: Boolean = false,
    val isFocusable: Boolean = false,
    val isScrollable: Boolean = false,
    val liveRegion: String? = null
) {
    /**
     * Convert to deprecated UUIDAccessibility for backwards compatibility
     */
    fun toUUIDAccessibility(): UUIDAccessibility = UUIDAccessibility(
        contentDescription = contentDescription,
        isImportantForAccessibility = isImportantForAccessibility,
        isClickable = isClickable,
        isFocusable = isFocusable,
        isScrollable = isScrollable,
        liveRegion = liveRegion
    )

    companion object {
        /**
         * Convert from deprecated UUIDAccessibility
         */
        fun fromUUIDAccessibility(accessibility: UUIDAccessibility): VUIDAccessibility = VUIDAccessibility(
            contentDescription = accessibility.contentDescription,
            isImportantForAccessibility = accessibility.isImportantForAccessibility,
            isClickable = accessibility.isClickable,
            isFocusable = accessibility.isFocusable,
            isScrollable = accessibility.isScrollable,
            liveRegion = accessibility.liveRegion
        )
    }
}
