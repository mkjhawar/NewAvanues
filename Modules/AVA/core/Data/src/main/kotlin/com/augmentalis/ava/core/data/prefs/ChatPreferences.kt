package com.augmentalis.ava.core.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Chat preferences manager using SharedPreferences (Phase 5 - P5T03).
 *
 * Manages user preferences for conversation behavior:
 * - Conversation mode: "append" (continue recent) vs "new" (new conversation each session)
 * - Last active conversation ID (for restoration)
 * - Confidence threshold (for teach mode triggering)
 *
 * Design rationale:
 * - SharedPreferences for lightweight key-value storage
 * - StateFlow for reactive preference updates
 * - Singleton pattern for app-wide access
 *
 * @param context Application context for SharedPreferences
 */
class ChatPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // ==================== Conversation Mode ====================

    private val _conversationMode = MutableStateFlow(getConversationMode())
    val conversationMode: StateFlow<ConversationMode> = _conversationMode.asStateFlow()

    /**
     * Gets the current conversation mode.
     *
     * @return ConversationMode.APPEND or ConversationMode.NEW
     */
    fun getConversationMode(): ConversationMode {
        val modeString = prefs.getString(KEY_CONVERSATION_MODE, ConversationMode.APPEND.name)
        return ConversationMode.valueOf(modeString ?: ConversationMode.APPEND.name)
    }

    /**
     * Sets the conversation mode.
     *
     * @param mode ConversationMode to set (APPEND or NEW)
     */
    fun setConversationMode(mode: ConversationMode) {
        prefs.edit().putString(KEY_CONVERSATION_MODE, mode.name).apply()
        _conversationMode.value = mode
    }

    // ==================== Last Active Conversation ====================

    /**
     * Gets the last active conversation ID.
     *
     * @return Conversation ID or null if none set
     */
    fun getLastActiveConversationId(): String? {
        return prefs.getString(KEY_LAST_ACTIVE_CONVERSATION_ID, null)
    }

    /**
     * Sets the last active conversation ID.
     *
     * @param conversationId ID to save, or null to clear
     */
    fun setLastActiveConversationId(conversationId: String?) {
        if (conversationId != null) {
            prefs.edit().putString(KEY_LAST_ACTIVE_CONVERSATION_ID, conversationId).apply()
        } else {
            prefs.edit().remove(KEY_LAST_ACTIVE_CONVERSATION_ID).apply()
        }
    }

    // ==================== Confidence Thresholds (ADR-014 Unified System) ====================

    private val _confidenceThreshold = MutableStateFlow(getConfidenceThreshold())
    val confidenceThreshold: StateFlow<Float> = _confidenceThreshold.asStateFlow()

    /**
     * Gets the main confidence threshold for action execution.
     * Actions execute only when NLU confidence exceeds this threshold.
     *
     * @return Threshold value (0.0 to 1.0), default 0.65
     */
    fun getConfidenceThreshold(): Float {
        return prefs.getFloat(KEY_CONFIDENCE_THRESHOLD, DEFAULT_CONFIDENCE_THRESHOLD)
    }

    /**
     * Sets the main confidence threshold.
     *
     * @param threshold Value between 0.0 and 1.0
     */
    fun setConfidenceThreshold(threshold: Float) {
        val clampedThreshold = threshold.coerceIn(0.0f, 1.0f)
        prefs.edit().putFloat(KEY_CONFIDENCE_THRESHOLD, clampedThreshold).apply()
        _confidenceThreshold.value = clampedThreshold
    }

    // ==================== Teach Threshold ====================

    private val _teachThreshold = MutableStateFlow(getTeachThreshold())
    val teachThreshold: StateFlow<Float> = _teachThreshold.asStateFlow()

    /**
     * Gets the threshold below which "Teach AVA" button is shown.
     * Low confidence responses show the teach button to allow user correction.
     *
     * @return Threshold value (0.0 to 1.0), default 0.5
     */
    fun getTeachThreshold(): Float {
        return prefs.getFloat(KEY_TEACH_THRESHOLD, DEFAULT_TEACH_THRESHOLD)
    }

    /**
     * Sets the teach threshold.
     *
     * @param threshold Value between 0.0 and 1.0
     */
    fun setTeachThreshold(threshold: Float) {
        val clampedThreshold = threshold.coerceIn(0.0f, 1.0f)
        prefs.edit().putFloat(KEY_TEACH_THRESHOLD, clampedThreshold).apply()
        _teachThreshold.value = clampedThreshold
    }

    // ==================== LLM Fallback Threshold ====================

    private val _llmFallbackThreshold = MutableStateFlow(getLLMFallbackThreshold())
    val llmFallbackThreshold: StateFlow<Float> = _llmFallbackThreshold.asStateFlow()

    /**
     * Gets the threshold below which LLM is used instead of templates.
     * Low confidence responses use LLM for flexible response generation.
     *
     * @return Threshold value (0.0 to 1.0), default 0.65
     */
    fun getLLMFallbackThreshold(): Float {
        return prefs.getFloat(KEY_LLM_FALLBACK_THRESHOLD, DEFAULT_LLM_FALLBACK_THRESHOLD)
    }

    /**
     * Sets the LLM fallback threshold.
     *
     * @param threshold Value between 0.0 and 1.0
     */
    fun setLLMFallbackThreshold(threshold: Float) {
        val clampedThreshold = threshold.coerceIn(0.0f, 1.0f)
        prefs.edit().putFloat(KEY_LLM_FALLBACK_THRESHOLD, clampedThreshold).apply()
        _llmFallbackThreshold.value = clampedThreshold
    }

    // ==================== Self-Learning Threshold ====================

    private val _selfLearningThreshold = MutableStateFlow(getSelfLearningThreshold())
    val selfLearningThreshold: StateFlow<Float> = _selfLearningThreshold.asStateFlow()

    /**
     * Gets the threshold below which NLU learns from LLM responses.
     * When LLM classifies an intent, NLU learns if confidence is below this.
     *
     * @return Threshold value (0.0 to 1.0), default 0.65
     */
    fun getSelfLearningThreshold(): Float {
        return prefs.getFloat(KEY_SELF_LEARNING_THRESHOLD, DEFAULT_SELF_LEARNING_THRESHOLD)
    }

    /**
     * Sets the self-learning threshold.
     *
     * @param threshold Value between 0.0 and 1.0
     */
    fun setSelfLearningThreshold(threshold: Float) {
        val clampedThreshold = threshold.coerceIn(0.0f, 1.0f)
        prefs.edit().putFloat(KEY_SELF_LEARNING_THRESHOLD, clampedThreshold).apply()
        _selfLearningThreshold.value = clampedThreshold
    }

    // ==================== LLM Model Selection (A/B Testing) ====================

    private val _selectedLLMModel = MutableStateFlow(getSelectedLLMModel())
    val selectedLLMModel: StateFlow<String?> = _selectedLLMModel.asStateFlow()

    /**
     * Gets the currently selected LLM model ID.
     *
     * Returns null if no model is selected - caller should use ModelDiscovery
     * to find available models and prompt user to select one.
     *
     * @return Model ID if previously selected, null otherwise
     */
    fun getSelectedLLMModel(): String? {
        val storedModel = prefs.getString(KEY_SELECTED_LLM_MODEL, null)

        // FIX: Auto-migrate old model names to new AVA naming convention v2.0
        // This fixes the issue where cached old model names cause "Model directory not found" errors
        val migratedModel = when (storedModel) {
            "AVA-GEM-2B-Q4" -> "AVA-GE2-2B16"  // Old Gemma 2 name
            "AVA-GEM-4B-Q4" -> "AVA-GE3-4B16"  // Old Gemma 3 name
            else -> storedModel
        }

        // If migration occurred, persist the new name
        if (migratedModel != storedModel && migratedModel != null) {
            prefs.edit().putString(KEY_SELECTED_LLM_MODEL, migratedModel).apply()
            _selectedLLMModel.value = migratedModel
        }

        return migratedModel
    }

    /**
     * Sets the selected LLM model for A/B testing.
     *
     * @param modelId Model ID to use (e.g., "AVA-GE2-2B16", "AVA-GE3-4B16")
     */
    fun setSelectedLLMModel(modelId: String) {
        prefs.edit().putString(KEY_SELECTED_LLM_MODEL, modelId).apply()
        _selectedLLMModel.value = modelId
    }

    /**
     * Check if using experimental Gemma 3 4B model.
     *
     * @return true if AVA-GE3-4B16 is selected
     */
    fun isUsingGemma3(): Boolean {
        return getSelectedLLMModel() == "AVA-GE3-4B16"
    }

    // ==================== RAG Settings (Phase 2 - RAG Integration) ====================

    private val _ragEnabled = MutableStateFlow(getRagEnabled())
    val ragEnabled: StateFlow<Boolean> = _ragEnabled.asStateFlow()

    /**
     * Gets whether RAG (Retrieval-Augmented Generation) is enabled.
     *
     * @return true if RAG is enabled, default false
     */
    fun getRagEnabled(): Boolean {
        return prefs.getBoolean(KEY_RAG_ENABLED, DEFAULT_RAG_ENABLED)
    }

    /**
     * Sets whether RAG is enabled.
     *
     * @param enabled true to enable RAG, false to disable
     */
    fun setRagEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RAG_ENABLED, enabled).apply()
        _ragEnabled.value = enabled
    }

    private val _selectedDocumentIds = MutableStateFlow(getSelectedDocumentIds())
    val selectedDocumentIds: StateFlow<List<String>> = _selectedDocumentIds.asStateFlow()

    /**
     * Gets the list of selected document IDs for RAG.
     *
     * @return List of document IDs, empty if none selected
     */
    fun getSelectedDocumentIds(): List<String> {
        val idsString = prefs.getString(KEY_RAG_DOCUMENT_IDS, null)
        return if (idsString.isNullOrBlank()) {
            emptyList()
        } else {
            idsString.split(",").filter { it.isNotBlank() }
        }
    }

    /**
     * Sets the list of selected document IDs for RAG.
     *
     * @param documentIds List of document IDs to use for RAG
     */
    fun setSelectedDocumentIds(documentIds: List<String>) {
        val idsString = documentIds.joinToString(",")
        prefs.edit().putString(KEY_RAG_DOCUMENT_IDS, idsString).apply()
        _selectedDocumentIds.value = documentIds
    }

    private val _ragThreshold = MutableStateFlow(getRagThreshold())
    val ragThreshold: StateFlow<Float> = _ragThreshold.asStateFlow()

    /**
     * Gets the RAG similarity threshold.
     *
     * @return Threshold value (0.0 to 1.0), default 0.7
     */
    fun getRagThreshold(): Float {
        return prefs.getFloat(KEY_RAG_THRESHOLD, DEFAULT_RAG_THRESHOLD)
    }

    /**
     * Sets the RAG similarity threshold.
     *
     * @param threshold Value between 0.0 and 1.0
     */
    fun setRagThreshold(threshold: Float) {
        val clampedThreshold = threshold.coerceIn(0.0f, 1.0f)
        prefs.edit().putFloat(KEY_RAG_THRESHOLD, clampedThreshold).apply()
        _ragThreshold.value = clampedThreshold
    }

    // ==================== Cache Configuration ====================

    /**
     * Gets the message pagination page size.
     *
     * @return Number of messages to load per page, default 50
     */
    fun getMessagePageSize(): Int {
        return prefs.getInt(KEY_MESSAGE_PAGE_SIZE, DEFAULT_MESSAGE_PAGE_SIZE)
    }

    /**
     * Sets the message pagination page size.
     *
     * @param size Number of messages per page (1-200)
     */
    fun setMessagePageSize(size: Int) {
        val clampedSize = size.coerceIn(1, 200)
        prefs.edit().putInt(KEY_MESSAGE_PAGE_SIZE, clampedSize).apply()
    }

    /**
     * Gets the conversations cache TTL in milliseconds.
     *
     * @return Cache TTL in ms, default 5000ms (5 seconds)
     */
    fun getConversationsCacheTTL(): Long {
        return prefs.getLong(KEY_CONVERSATIONS_CACHE_TTL, DEFAULT_CONVERSATIONS_CACHE_TTL_MS)
    }

    /**
     * Sets the conversations cache TTL.
     *
     * @param ttlMs Cache TTL in milliseconds (1000-60000)
     */
    fun setConversationsCacheTTL(ttlMs: Long) {
        val clampedTTL = ttlMs.coerceIn(1000L, 60000L)
        prefs.edit().putLong(KEY_CONVERSATIONS_CACHE_TTL, clampedTTL).apply()
    }

    /**
     * Gets the intents cache TTL in milliseconds.
     *
     * @return Cache TTL in ms, default 10000ms (10 seconds)
     */
    fun getIntentsCacheTTL(): Long {
        return prefs.getLong(KEY_INTENTS_CACHE_TTL, DEFAULT_INTENTS_CACHE_TTL_MS)
    }

    /**
     * Sets the intents cache TTL.
     *
     * @param ttlMs Cache TTL in milliseconds (1000-60000)
     */
    fun setIntentsCacheTTL(ttlMs: Long) {
        val clampedTTL = ttlMs.coerceIn(1000L, 60000L)
        prefs.edit().putLong(KEY_INTENTS_CACHE_TTL, clampedTTL).apply()
    }

    /**
     * Gets the NLU classification LRU cache max size.
     *
     * @return Max cache entries, default 100
     */
    fun getNLUCacheMaxSize(): Int {
        return prefs.getInt(KEY_NLU_CACHE_MAX_SIZE, DEFAULT_NLU_CACHE_MAX_SIZE)
    }

    /**
     * Sets the NLU classification LRU cache max size.
     *
     * @param maxSize Max cache entries (10-1000)
     */
    fun setNLUCacheMaxSize(maxSize: Int) {
        val clampedSize = maxSize.coerceIn(10, 1000)
        prefs.edit().putInt(KEY_NLU_CACHE_MAX_SIZE, clampedSize).apply()
    }

    // ==================== Clear All ====================

    /**
     * Clears all chat preferences (reset to defaults).
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        _conversationMode.value = ConversationMode.APPEND
        _confidenceThreshold.value = DEFAULT_CONFIDENCE_THRESHOLD
        _teachThreshold.value = DEFAULT_TEACH_THRESHOLD
        _llmFallbackThreshold.value = DEFAULT_LLM_FALLBACK_THRESHOLD
        _selfLearningThreshold.value = DEFAULT_SELF_LEARNING_THRESHOLD
    }

    companion object {
        private const val PREFS_NAME = "chat_preferences"
        private const val KEY_CONVERSATION_MODE = "conversation_mode"
        private const val KEY_LAST_ACTIVE_CONVERSATION_ID = "last_active_conversation_id"

        // ADR-014: Unified confidence threshold system
        private const val KEY_CONFIDENCE_THRESHOLD = "confidence_threshold"
        private const val KEY_TEACH_THRESHOLD = "teach_threshold"
        private const val KEY_LLM_FALLBACK_THRESHOLD = "llm_fallback_threshold"
        private const val KEY_SELF_LEARNING_THRESHOLD = "self_learning_threshold"

        // Unified default: 0.65f for main threshold
        // This balances action execution accuracy with LLM fallback frequency
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.65f
        private const val DEFAULT_TEACH_THRESHOLD = 0.5f
        private const val DEFAULT_LLM_FALLBACK_THRESHOLD = 0.65f
        private const val DEFAULT_SELF_LEARNING_THRESHOLD = 0.65f

        // LLM model selection keys (A/B testing)
        private const val KEY_SELECTED_LLM_MODEL = "selected_llm_model"
        // NO DEFAULT MODEL - will be dynamically discovered from storage
        // See: ModelDiscovery.kt for runtime model detection
        // Migration from old names handled in getSelectedLLMModel()
        private const val DEFAULT_LLM_MODEL: String = ""

        // RAG settings keys (Phase 2 - RAG Integration)
        private const val KEY_RAG_ENABLED = "rag_enabled"
        private const val KEY_RAG_DOCUMENT_IDS = "rag_document_ids"
        private const val KEY_RAG_THRESHOLD = "rag_threshold"
        private const val DEFAULT_RAG_ENABLED = false
        private const val DEFAULT_RAG_THRESHOLD = 0.7f

        // Cache configuration keys
        private const val KEY_MESSAGE_PAGE_SIZE = "message_page_size"
        private const val KEY_CONVERSATIONS_CACHE_TTL = "conversations_cache_ttl"
        private const val KEY_INTENTS_CACHE_TTL = "intents_cache_ttl"
        private const val KEY_NLU_CACHE_MAX_SIZE = "nlu_cache_max_size"

        // Cache configuration defaults
        private const val DEFAULT_MESSAGE_PAGE_SIZE = 50
        private const val DEFAULT_CONVERSATIONS_CACHE_TTL_MS = 5000L
        private const val DEFAULT_INTENTS_CACHE_TTL_MS = 10000L
        private const val DEFAULT_NLU_CACHE_MAX_SIZE = 100

        @Volatile
        private var INSTANCE: ChatPreferences? = null

        /**
         * Gets the singleton instance of ChatPreferences.
         *
         * @param context Application context
         * @return ChatPreferences instance
         */
        fun getInstance(context: Context): ChatPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Resets the singleton instance (for testing only).
         *
         * INTERNAL API - DO NOT USE IN PRODUCTION CODE.
         * This method exists solely for unit testing to allow proper
         * mocking of SharedPreferences.
         */
        @Suppress("unused")
        internal fun resetInstance() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
}

/**
 * Conversation mode enum for user preference.
 *
 * - APPEND: Continue most recent conversation across app restarts
 * - NEW: Create new conversation each time app is opened
 */
enum class ConversationMode {
    /**
     * Append to most recent conversation (default).
     * User's messages continue in the same conversation history.
     */
    APPEND,

    /**
     * Create new conversation each session.
     * Each app open starts a fresh conversation.
     */
    NEW
}
