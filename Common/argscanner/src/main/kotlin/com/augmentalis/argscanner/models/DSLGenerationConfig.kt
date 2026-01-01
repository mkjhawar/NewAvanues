package com.augmentalis.argscanner.models

import kotlinx.serialization.Serializable

/**
 * DSLGenerationConfig - Configuration for AVAMagic UI DSL generation
 *
 * Controls how scanned objects are converted to AVAMagic UI DSL:
 * - Component mapping rules
 * - Layout strategies
 * - Voice control integration
 * - Export formatting
 *
 * Created by: Manoj Jhawar, manoj@ideahq.net
 */
@Serializable
data class DSLGenerationConfig(
    // Component Mapping
    val componentMappingStrategy: ComponentMappingStrategy = ComponentMappingStrategy.AUTOMATIC,
    val customMappings: Map<String, String> = emptyMap(),  // label -> component type

    // Layout Strategy
    val layoutStrategy: LayoutStrategy = LayoutStrategy.SPATIAL,
    val gridColumns: Int = 2,  // For grid layout
    val spacing: Float = 16f,  // Spacing between components (dp)

    // Voice Control
    val enableVoiceControl: Boolean = true,
    val generateVoiceCommands: Boolean = true,
    val voiceCommandPrefix: String? = null,  // Optional prefix (e.g., "navigate to")

    // UUID Integration
    val useUUIDCreator: Boolean = true,
    val registerWithVoiceOS: Boolean = true,

    // Grouping
    val enableGrouping: Boolean = true,
    val groupingThreshold: Float = 1.5f,  // Max distance (meters) for grouping

    // Filtering
    val minConfidence: Float = 0.7f,  // Minimum detection confidence
    val excludeLabels: List<String> = emptyList(),  // Labels to exclude
    val includeOnlyLabels: List<String> = emptyList(),  // If non-empty, only include these

    // Export Format
    val outputFormat: ARScanSession.DSLFormat = ARScanSession.DSLFormat.AVAMAGIC,
    val prettifyOutput: Boolean = true,
    val includeComments: Boolean = true,

    // Metadata
    val includeTimestamps: Boolean = false,
    val includeConfidenceScores: Boolean = false,
    val includeSpatialData: Boolean = true,

    // Advanced
    val customProperties: Map<String, Any> = emptyMap()
) {
    /**
     * Component mapping strategy
     */
    enum class ComponentMappingStrategy {
        AUTOMATIC,      // Automatic label -> component mapping
        MANUAL,         // Use customMappings only
        HYBRID          // Automatic with custom overrides
    }

    /**
     * Layout generation strategy
     */
    enum class LayoutStrategy {
        SPATIAL,        // Preserve spatial relationships
        GRID,           // Fixed grid layout
        LIST,           // Vertical list
        GROUPED,        // Group by spatial relationships
        HIERARCHICAL    // Tree-based hierarchy (e.g., desk contains monitor + keyboard)
    }

    /**
     * Get component type for a detected label
     */
    fun getComponentType(label: String): String? {
        return when (componentMappingStrategy) {
            ComponentMappingStrategy.AUTOMATIC -> {
                getAutomaticMapping(label)
            }
            ComponentMappingStrategy.MANUAL -> {
                customMappings[label]
            }
            ComponentMappingStrategy.HYBRID -> {
                customMappings[label] ?: getAutomaticMapping(label)
            }
        }
    }

    /**
     * Automatic label to component mapping
     */
    private fun getAutomaticMapping(label: String): String? {
        return when {
            // UI Controls
            label.contains("button", ignoreCase = true) -> "MagicButton"
            label.contains("switch", ignoreCase = true) -> "MagicSwitch"
            label.contains("slider", ignoreCase = true) -> "MagicSlider"

            // Display Components
            label.contains("text", ignoreCase = true) -> "MagicText"
            label.contains("image", ignoreCase = true) -> "MagicImage"
            label.contains("icon", ignoreCase = true) -> "MagicIcon"
            label.contains("label", ignoreCase = true) -> "MagicText"

            // Containers
            label.contains("card", ignoreCase = true) -> "MagicCard"
            label.contains("panel", ignoreCase = true) -> "MagicPanel"
            label.contains("container", ignoreCase = true) -> "MagicContainer"

            // Furniture -> Containers (abstract representation)
            label.contains("desk", ignoreCase = true) -> "MagicRow"
            label.contains("shelf", ignoreCase = true) -> "MagicColumn"
            label.contains("table", ignoreCase = true) -> "MagicGrid"
            label.contains("chair", ignoreCase = true) -> null  // Decorative, skip

            // Electronics -> Interactive Components
            label.contains("monitor", ignoreCase = true) -> "MagicCard"
            label.contains("screen", ignoreCase = true) -> "MagicCard"
            label.contains("keyboard", ignoreCase = true) -> "MagicTextField"
            label.contains("mouse", ignoreCase = true) -> null  // Skip (pointing device)

            // Default: Generic card
            else -> "MagicCard"
        }
    }

    /**
     * Check if label should be excluded
     */
    fun shouldExcludeLabel(label: String): Boolean {
        // Check explicit exclusions
        if (excludeLabels.any { it.equals(label, ignoreCase = true) }) {
            return true
        }

        // Check include-only list
        if (includeOnlyLabels.isNotEmpty()) {
            return !includeOnlyLabels.any { it.equals(label, ignoreCase = true) }
        }

        return false
    }

    /**
     * Check if object meets confidence threshold
     */
    fun meetsConfidenceThreshold(confidence: Float): Boolean {
        return confidence >= minConfidence
    }

    /**
     * Get voice command for object
     */
    fun getVoiceCommand(objectName: String, actionName: String): String {
        return buildString {
            if (voiceCommandPrefix != null) {
                append(voiceCommandPrefix)
                append(" ")
            }
            append(actionName)
            append(" ")
            append(objectName)
        }.trim()
    }

    companion object {
        /**
         * Default configuration for indoor room scanning
         */
        fun forIndoorRoom(): DSLGenerationConfig {
            return DSLGenerationConfig(
                componentMappingStrategy = ComponentMappingStrategy.HYBRID,
                layoutStrategy = LayoutStrategy.SPATIAL,
                enableGrouping = true,
                groupingThreshold = 1.5f,
                minConfidence = 0.7f,
                enableVoiceControl = true
            )
        }

        /**
         * Default configuration for workspace scanning
         */
        fun forWorkspace(): DSLGenerationConfig {
            return DSLGenerationConfig(
                componentMappingStrategy = ComponentMappingStrategy.HYBRID,
                layoutStrategy = LayoutStrategy.GROUPED,
                enableGrouping = true,
                groupingThreshold = 1.0f,  // Tighter grouping for workspace
                minConfidence = 0.75f,  // Higher confidence for office items
                enableVoiceControl = true,
                includeOnlyLabels = listOf(
                    "desk", "chair", "monitor", "keyboard", "mouse",
                    "laptop", "phone", "lamp", "plant"
                )
            )
        }

        /**
         * Default configuration for retail scanning
         */
        fun forRetail(): DSLGenerationConfig {
            return DSLGenerationConfig(
                componentMappingStrategy = ComponentMappingStrategy.AUTOMATIC,
                layoutStrategy = LayoutStrategy.GRID,
                gridColumns = 3,
                spacing = 12f,
                enableGrouping = false,  // Products usually independent
                minConfidence = 0.8f,  // High confidence for products
                enableVoiceControl = true,
                generateVoiceCommands = true
            )
        }

        /**
         * Minimal configuration for testing
         */
        fun minimal(): DSLGenerationConfig {
            return DSLGenerationConfig(
                componentMappingStrategy = ComponentMappingStrategy.AUTOMATIC,
                layoutStrategy = LayoutStrategy.LIST,
                enableGrouping = false,
                minConfidence = 0.5f,  // Lower threshold for testing
                includeComments = true,
                includeConfidenceScores = true,
                includeSpatialData = true
            )
        }
    }
}
