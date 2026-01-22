package com.augmentalis.nlu.ava.model

/**
 * Core data structures for .ava file format
 *
 * .ava Format v2.0: Universal Format with 3-letter IPC codes
 * Used for: Core intents, VoiceOS integration, user-taught intents
 *
 * Supports both v1.0 (JSON) and v2.0 (Universal) formats
 * No logic, no I/O - pure data classes only
 */

/**
 * Represents a single intent with examples
 *
 * v2.0 additions:
 * - ipcCode: 3-letter Universal IPC code (VCM, AIQ, etc.)
 * - ipcTemplate: Pre-formatted IPC message template
 */
data class AvaIntent(
    val id: String,
    val canonical: String,
    val synonyms: List<String>,
    val category: String,
    val priority: Int,
    val tags: List<String>,
    val locale: String,
    val source: String,
    // v2.0 Universal Format fields
    val ipcCode: String? = null,           // VCM, AIQ, URL, etc.
    val ipcTemplate: String? = null        // CODE:id:data format
) {
    /**
     * Get IPC code - either explicit or derived from category
     */
    fun getIPCCode(): String {
        return ipcCode ?: mapCategoryToIPCCode(category)
    }

    /**
     * Get IPC template for this intent
     */
    fun getIPCTemplate(): String {
        return ipcTemplate ?: "${getIPCCode()}:$id:$canonical"
    }

    private fun mapCategoryToIPCCode(category: String): String {
        return when (category) {
            "voice_command", "navigation", "device_control",
            "media_control", "system_control" -> "VCM"
            "ai_query" -> "AIQ"
            "speech_text" -> "STT"
            "context_share" -> "CTX"
            "suggestion" -> "SUG"
            else -> "VCM" // Default to voice command
        }
    }
}

/**
 * Metadata about an .ava file
 */
data class AvaFileMetadata(
    val filename: String,
    val category: String,
    val name: String,
    val description: String,
    val intentCount: Int
)

/**
 * Complete .ava file structure
 */
data class AvaFile(
    val schema: String,
    val version: String,
    val locale: String,
    val metadata: AvaFileMetadata,
    val intents: List<AvaIntent>,
    val globalSynonyms: Map<String, List<String>>
)
