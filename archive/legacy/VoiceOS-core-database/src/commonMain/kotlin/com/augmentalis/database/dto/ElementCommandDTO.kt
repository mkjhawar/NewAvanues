/**
 * ElementCommandDTO.kt - Data Transfer Object for user-assigned element commands
 *
 * Part of Metadata Quality Overlay & Manual Command Assignment feature (VOS-META-001)
 * Created: 2025-12-03
 *
 * Represents a voice command assigned by the user to a specific UI element.
 * Different from CustomCommandDTO which represents general command macros.
 */
package com.augmentalis.database.dto

/**
 * Element-specific voice command
 *
 * @property id Database ID (0 for new records)
 * @property elementUuid UUID of the target element (from ThirdPartyUuidGenerator)
 * @property commandPhrase User-spoken command phrase (e.g., "submit button", "send")
 * @property confidence Recognition confidence (1.0 for user-assigned)
 * @property createdAt Timestamp when command was created
 * @property createdBy Creator identifier ("user" for manual, "ai" for auto-generated)
 * @property isSynonym true if this is a synonym of another command for the same element
 * @property appId Package name of the app (e.g., "com.realwear.testcomp")
 */
data class ElementCommandDTO(
    val id: Long = 0L,
    val elementUuid: String,
    val commandPhrase: String,
    val confidence: Double = 1.0,
    val createdAt: Long,
    val createdBy: String = "user",
    val isSynonym: Boolean = false,
    val appId: String
) {
    /**
     * Validate command phrase
     */
    fun isValid(): Boolean {
        return commandPhrase.length in 3..50 &&
               commandPhrase.matches(Regex("[a-zA-Z0-9 ]+")) &&
               elementUuid.isNotBlank() &&
               appId.isNotBlank()
    }

    /**
     * Get sanitized command phrase (lowercase, single spaces)
     */
    fun getSanitizedPhrase(): String {
        return commandPhrase.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}

/**
 * Extension function to convert database entity to DTO
 */
fun com.augmentalis.database.Element_command.toDTO(): ElementCommandDTO {
    return ElementCommandDTO(
        id = id,
        elementUuid = element_uuid,
        commandPhrase = command_phrase,
        confidence = confidence ?: 1.0,
        createdAt = created_at,
        createdBy = created_by ?: "user",
        isSynonym = is_synonym == 1L,
        appId = app_id
    )
}

/**
 * Metadata quality metric for UI element
 *
 * @property elementUuid UUID of the element
 * @property appId Package name
 * @property qualityScore Quality score 0-100 (0=poorest, 100=best)
 * @property hasText Element has text property
 * @property hasContentDesc Element has contentDescription
 * @property hasResourceId Element has resource ID
 * @property commandCount Total number of voice commands (auto + manual)
 * @property manualCommandCount Number of user-assigned commands
 * @property lastAssessed Timestamp of last quality assessment
 */
data class QualityMetricDTO(
    val elementUuid: String,
    val appId: String,
    val qualityScore: Int,
    val hasText: Boolean,
    val hasContentDesc: Boolean,
    val hasResourceId: Boolean,
    val commandCount: Int = 0,
    val manualCommandCount: Int = 0,
    val lastAssessed: Long
) {
    /**
     * Get quality level enum from score
     */
    fun toQualityLevel(): MetadataQualityLevel {
        return when {
            qualityScore >= 80 -> MetadataQualityLevel.EXCELLENT
            qualityScore >= 60 -> MetadataQualityLevel.GOOD
            qualityScore >= 40 -> MetadataQualityLevel.ACCEPTABLE
            else -> MetadataQualityLevel.POOR
        }
    }

    /**
     * Check if element needs manual command assignment
     */
    fun needsManualCommand(): Boolean {
        return qualityScore < 40 && commandCount == 0
    }

    /**
     * Get metadata present count (0-3)
     */
    fun getMetadataCount(): Int {
        var count = 0
        if (hasText) count++
        if (hasContentDesc) count++
        if (hasResourceId) count++
        return count
    }

    /**
     * Get suggestions for improvement
     */
    fun getSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()

        if (!hasText && !hasContentDesc) {
            suggestions.add("Add text or contentDescription to element")
        }
        if (!hasResourceId) {
            suggestions.add("Add android:id to element in XML")
        }
        if (commandCount == 0) {
            suggestions.add("Assign voice command via manual command assignment")
        }

        return suggestions
    }
}

/**
 * Metadata quality level categories
 */
enum class MetadataQualityLevel {
    EXCELLENT,  // 80-100: Has all metadata, voice-controllable
    GOOD,       // 60-79: Has most metadata, good voice control
    ACCEPTABLE, // 40-59: Has some metadata, limited voice control
    POOR        // 0-39: Missing metadata, needs manual commands
}

/**
 * Extension function to convert database entity to DTO
 */
fun com.augmentalis.database.Element_quality_metric.toDTO(): QualityMetricDTO {
    return QualityMetricDTO(
        elementUuid = element_uuid,
        appId = app_id,
        qualityScore = quality_score?.toInt() ?: 0,
        hasText = has_text == 1L,
        hasContentDesc = has_content_desc == 1L,
        hasResourceId = has_resource_id == 1L,
        commandCount = command_count?.toInt() ?: 0,
        manualCommandCount = manual_command_count?.toInt() ?: 0,
        lastAssessed = last_assessed
    )
}

/**
 * Quality statistics summary for an app
 */
data class QualityStatsDTO(
    val appId: String,
    val totalElements: Int,
    val excellentCount: Int,
    val goodCount: Int,
    val acceptableCount: Int,
    val poorCount: Int,
    val totalManualCommands: Int,
    val avgQualityScore: Double
) {
    /**
     * Get percentage of elements with poor quality
     */
    fun getPoorPercentage(): Double {
        return if (totalElements > 0) {
            (poorCount.toDouble() / totalElements) * 100
        } else 0.0
    }

    /**
     * Check if app has significant quality issues
     */
    fun hasQualityIssues(): Boolean {
        return getPoorPercentage() > 20.0 // >20% poor quality
    }

    /**
     * Get voice control coverage percentage (elements with commands)
     */
    fun getVoiceControlCoverage(): Double {
        val elementsWithCommands = totalElements - poorCount // Assume poor = no commands
        return if (totalElements > 0) {
            (elementsWithCommands.toDouble() / totalElements) * 100
        } else 0.0
    }
}
