package com.augmentalis.magiccode.plugins.universal

import kotlinx.serialization.Serializable

/**
 * Plugin capability definition for discovery and matching.
 *
 * Capabilities describe what a plugin can do, enabling the plugin system
 * to match plugins to requested functionality. This supports the
 * accessibility-first architecture by allowing discovery of plugins
 * that provide specific accessibility features.
 *
 * ## Capability Naming Convention
 * Capabilities use a hierarchical dot-notation:
 * - `llm.text-generation` - LLM text generation
 * - `speech.recognition` - Speech recognition
 * - `accessibility.gaze-control` - Gaze-based control
 *
 * ## Usage
 * ```kotlin
 * val capability = PluginCapability(
 *     id = PluginCapability.SPEECH_RECOGNITION,
 *     name = "Speech Recognition",
 *     version = "1.0.0",
 *     interfaces = setOf("SpeechRecognitionPlugin"),
 *     metadata = mapOf("engine" to "vosk", "languages" to "en,de,fr")
 * )
 * ```
 *
 * @property id Unique capability identifier using dot-notation (e.g., "llm.text-generation")
 * @property name Human-readable capability name
 * @property version Semantic version of this capability implementation
 * @property interfaces Set of interface names this capability implements
 * @property metadata Additional metadata for capability matching and configuration
 * @since 1.0.0
 * @see UniversalPlugin.capabilities
 */
@Serializable
data class PluginCapability(
    val id: String,
    val name: String,
    val version: String,
    val interfaces: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Check if this capability implements a specific interface.
     *
     * @param interfaceName The interface name to check
     * @return true if this capability implements the interface
     */
    fun implementsInterface(interfaceName: String): Boolean {
        return interfaceName in interfaces
    }

    /**
     * Check if this capability matches a capability filter.
     *
     * @param filter Capability ID prefix or exact match
     * @return true if this capability matches the filter
     */
    fun matches(filter: String): Boolean {
        return id == filter || id.startsWith("$filter.")
    }

    /**
     * Get metadata value by key.
     *
     * @param key Metadata key
     * @return Metadata value or null if not found
     */
    fun getMetadata(key: String): String? = metadata[key]

    companion object {
        // ============================================
        // LLM (Large Language Model) Capabilities
        // ============================================

        /** Text generation using large language models */
        const val LLM_TEXT_GENERATION = "llm.text-generation"

        /** Text embedding/vectorization for semantic search */
        const val LLM_EMBEDDING = "llm.embedding"

        /** Chat/conversation handling with context */
        const val LLM_CHAT = "llm.chat"

        /** Code generation and completion */
        const val LLM_CODE = "llm.code"

        // ============================================
        // NLU (Natural Language Understanding) Capabilities
        // ============================================

        /** Intent classification for voice commands */
        const val NLU_INTENT = "nlu.intent-classification"

        /** Named entity extraction from text */
        const val NLU_ENTITY = "nlu.entity-extraction"

        /** Sentiment analysis */
        const val NLU_SENTIMENT = "nlu.sentiment"

        /** Slot filling for structured command parsing */
        const val NLU_SLOT_FILLING = "nlu.slot-filling"

        // ============================================
        // Speech Capabilities
        // ============================================

        /** Speech-to-text recognition */
        const val SPEECH_RECOGNITION = "speech.recognition"

        /** Text-to-speech synthesis */
        const val SPEECH_TTS = "speech.text-to-speech"

        /** Wake word detection */
        const val SPEECH_WAKE_WORD = "speech.wake-word"

        /** Voice activity detection */
        const val SPEECH_VAD = "speech.vad"

        // ============================================
        // Accessibility Capabilities
        // ============================================

        /** UI element handling via accessibility services */
        const val ACCESSIBILITY_HANDLER = "accessibility.handler"

        /** Gaze-based control for eye tracking */
        const val ACCESSIBILITY_GAZE = "accessibility.gaze-control"

        /** Screen reading and audio feedback */
        const val ACCESSIBILITY_SCREEN_READER = "accessibility.screen-reader"

        /** Switch control for single-button input */
        const val ACCESSIBILITY_SWITCH = "accessibility.switch-control"

        /** Voice-based UI navigation */
        const val ACCESSIBILITY_VOICE_NAV = "accessibility.voice-navigation"

        // ============================================
        // RAG (Retrieval Augmented Generation) Capabilities
        // ============================================

        /** Document processing for RAG pipelines */
        const val RAG_DOCUMENT = "rag.document-processing"

        /** Vector embedding generation for RAG */
        const val RAG_EMBEDDING = "rag.embedding"

        /** Vector store/retrieval operations */
        const val RAG_RETRIEVAL = "rag.retrieval"

        /** Context injection for LLM prompts */
        const val RAG_CONTEXT = "rag.context-injection"

        // ============================================
        // UI/Theme Capabilities
        // ============================================

        /** Theme provision and management */
        const val UI_THEME = "ui.theme"

        /** Custom font provision */
        const val UI_FONTS = "ui.fonts"

        /** Icon pack provision */
        const val UI_ICONS = "ui.icons"

        // ============================================
        // Data/Storage Capabilities
        // ============================================

        /** Database operations */
        const val DATA_DATABASE = "data.database"

        /** File system operations */
        const val DATA_FILESYSTEM = "data.filesystem"

        /** Cloud sync capabilities */
        const val DATA_CLOUD_SYNC = "data.cloud-sync"

        /**
         * Create a capability for a custom interface.
         *
         * @param id Capability ID
         * @param name Display name
         * @param version Semantic version
         * @param interfaceName Primary interface name
         * @param additionalMetadata Optional additional metadata
         * @return New PluginCapability instance
         */
        fun custom(
            id: String,
            name: String,
            version: String,
            interfaceName: String,
            additionalMetadata: Map<String, String> = emptyMap()
        ): PluginCapability {
            return PluginCapability(
                id = id,
                name = name,
                version = version,
                interfaces = setOf(interfaceName),
                metadata = additionalMetadata
            )
        }
    }
}
