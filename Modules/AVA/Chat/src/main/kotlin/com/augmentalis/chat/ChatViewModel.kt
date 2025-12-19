package com.augmentalis.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.prefs.ChatPreferences
import com.augmentalis.ava.core.data.prefs.ConversationMode
import com.augmentalis.ava.core.domain.model.Conversation
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.core.domain.model.MessageRole
import com.augmentalis.ava.core.domain.repository.ConversationRepository
import com.augmentalis.ava.core.domain.repository.MessageRepository
import com.augmentalis.ava.core.domain.repository.TrainExampleRepository
import com.augmentalis.chat.coordinator.ActionCoordinator
import com.augmentalis.chat.coordinator.IActionCoordinator
import com.augmentalis.chat.coordinator.NLUCoordinator
import com.augmentalis.chat.coordinator.RAGCoordinator
import com.augmentalis.chat.coordinator.ResponseCoordinator
import com.augmentalis.chat.coordinator.TTSCoordinator
import com.augmentalis.chat.data.BuiltInIntents
import com.augmentalis.chat.event.WakeWordEventBus
import com.augmentalis.llm.response.IntentTemplates
import com.augmentalis.nlu.IntentClassification
import com.augmentalis.nlu.IntentClassifier
import com.augmentalis.nlu.ModelManager
import com.augmentalis.actions.ActionsManager
import com.augmentalis.actions.ActionResult
import com.augmentalis.llm.response.ResponseGenerator
import com.augmentalis.llm.response.ResponseChunk
import com.augmentalis.llm.response.ResponseContext
// Issue 5.3: IntentLearningManager removed - all learning via NLUSelfLearner
import com.augmentalis.nlu.NLUSelfLearner
import com.augmentalis.llm.inference.InferenceManager
import com.augmentalis.llm.teacher.LLMResponseParser
import com.augmentalis.llm.teacher.LLMTeacherPrompt
import com.augmentalis.ava.core.domain.usecase.ExportConversationUseCase
import com.augmentalis.ava.core.domain.usecase.ExportFormat
import com.augmentalis.ava.core.domain.usecase.PrivacyOptions
import com.augmentalis.ava.core.domain.usecase.ExportResult
import com.augmentalis.rag.domain.RAGRepository
import com.augmentalis.rag.domain.SearchResponse
import com.augmentalis.rag.domain.SearchResult
import com.augmentalis.rag.domain.SearchQuery
import com.augmentalis.rag.domain.SearchFilters
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for ChatScreen, managing conversation state and message flow.
 *
 * Responsibilities:
 * - Maintain current conversation ID and message list
 * - Handle user input and message sending
 * - Orchestrate NLU classification (Phase 2)
 * - Auto-prompt on low confidence (Phase 2, Task P2T06)
 * - Manage Teach-AVA bottom sheet state (Phase 3)
 * - Control history overlay visibility (Phase 4)
 * - RAG-enhanced responses (RAG Phase 2, Task 2)
 *
 * Follows MVVM pattern with reactive StateFlow for UI updates.
 *
 * Key Features (Phase 2, Task P2T06):
 * - When NLU confidence <= 0.5, shows "Teach AVA" button to prompt user training
 * - Uses "unknown" template for low confidence responses
 * - Threshold is configurable (default 0.5, future: SharedPreferences)
 *
 * RAG Features (RAG Phase 2, Task 2):
 * - Optional RAG context retrieval before LLM generation
 * - Document source citations in message bubbles
 * - RAG-enabled state management
 * - Performance target: <200ms retrieval time
 *
 * Dependencies are injected via Hilt:
 * @param conversationRepository Repository for conversation operations
 * @param messageRepository Repository for message operations
 * @param trainExampleRepository Repository for training examples (user-taught intents)
 * @param chatPreferences User preferences manager (Phase 5 - P5T03)
 * @param intentClassifier NLU classification engine
 * @param modelManager NLU model management
 * @param actionsManager Actions system wrapper (eliminates Context dependency)
 * @param responseGenerator LLM-based response generator with template fallback
 * @param ragRepository Optional RAG repository for document retrieval (RAG Phase 2)
 * @param ttsCoordinator TTS coordinator for voice output (SOLID refactoring)
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val trainExampleRepository: TrainExampleRepository,
    private val chatPreferences: ChatPreferences,
    private val intentClassifier: IntentClassifier,
    private val modelManager: ModelManager,
    private val actionsManager: ActionsManager,
    private val responseGenerator: ResponseGenerator,
    // Issue 5.3: IntentLearningManager removed - use nluSelfLearner below
    private val exportConversationUseCase: ExportConversationUseCase,
    private val ragRepository: RAGRepository?,
    // ADR-013: Self-Learning NLU with LLM-as-Teacher
    private val nluSelfLearner: NLUSelfLearner,
    private val inferenceManager: InferenceManager,
    // P0: SOLID Coordinators for single-responsibility decomposition
    private val nluCoordinator: NLUCoordinator,
    private val responseCoordinator: ResponseCoordinator,
    private val ragCoordinator: RAGCoordinator,
    private val actionCoordinator: ActionCoordinator,
    private val ttsCoordinator: TTSCoordinator,
    // P1: WakeWordEventBus to remove reflection
    private val wakeWordEventBus: WakeWordEventBus,
    // P0: SOLID State Managers for single-responsibility decomposition
    private val uiStateManager: com.augmentalis.chat.state.ChatUIStateManager,
    private val statusIndicatorState: com.augmentalis.chat.state.StatusIndicatorState,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"

        /**
         * Default confidence threshold for auto-prompt on low confidence (Task P2T06).
         * When confidence <= this value, the "unknown" template is used and
         * the Teach-AVA button is shown to allow user training.
         *
         * Edge case handling: confidence = exactly 0.5 triggers teach mode
         * (uses <= comparison, so 0.5 and below go to teach mode).
         *
         * Typical behavior:
         * - confidence 0.49 → teach mode (unknown template, show button)
         * - confidence 0.50 → teach mode (unknown template, show button)
         * - confidence 0.51 → normal mode (template based on classified intent)
         *
         * Phase 5 (P5T03): Now configurable via ChatPreferences
         */
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.5f

    }

    /**
     * Confidence threshold from preferences (Phase 5 - P5T03).
     * Reactive StateFlow that updates when user changes threshold in settings.
     */
    private val confidenceThreshold: StateFlow<Float> = chatPreferences.confidenceThreshold

    // ADR-014: Unified Confidence Threshold System
    /**
     * Teach threshold - below this, show "Teach AVA" button (default 0.5f).
     * User-configurable in Settings (Developer Mode).
     */
    private val teachThreshold: StateFlow<Float> = chatPreferences.teachThreshold

    /**
     * LLM fallback threshold - below this, route to LLM for response (default 0.65f).
     * Controls when NLU hands off to LLM due to low confidence.
     */
    private val llmFallbackThreshold: StateFlow<Float> = chatPreferences.llmFallbackThreshold

    /**
     * Self-learning threshold - above this, LLM responses are learned (default 0.65f).
     * Controls when to auto-train NLU from successful LLM responses.
     */
    private val selfLearningThreshold: StateFlow<Float> = chatPreferences.selfLearningThreshold

    /**
     * Conversation mode from preferences (Phase 5 - P5T03).
     * Determines whether to append to last conversation or create new on app open.
     */
    private val conversationMode: StateFlow<ConversationMode> = chatPreferences.conversationMode

    // ==================== RAG Settings (Phase 2 - RAG Integration) ====================

    /**
     * RAG enabled state from preferences (Phase 2 - RAG Integration).
     * Determines whether to use document retrieval for response generation.
     */
    val ragEnabled: StateFlow<Boolean> = chatPreferences.ragEnabled

    /**
     * Selected document IDs for RAG from preferences (Phase 2 - RAG Integration).
     * List of document IDs to use for context retrieval.
     */
    val selectedDocumentIds: StateFlow<List<String>> = chatPreferences.selectedDocumentIds

    /**
     * RAG similarity threshold from preferences (Phase 2 - RAG Integration).
     * Minimum similarity score for document chunks to be included in context.
     */
    val ragThreshold: StateFlow<Float> = chatPreferences.ragThreshold

    /**
     * Recent source citations from RAG retrieval (Phase 2 - RAG Integration).
     * Updated after each message with RAG context, shown in message bubbles.
     */
    private val _recentSourceCitations = MutableStateFlow<List<com.augmentalis.chat.domain.SourceCitation>>(emptyList())
    val recentSourceCitations: StateFlow<List<com.augmentalis.chat.domain.SourceCitation>> = _recentSourceCitations.asStateFlow()

    /**
     * RAG context builder for assembling document context (Phase 2 - RAG Integration).
     * Formats search results into LLM-ready context strings.
     */
    private val ragContextBuilder = com.augmentalis.chat.domain.RAGContextBuilder()

    // ==================== Query Optimization - Caching (Phase 5 - P5T04) ====================

    /**
     * Message pagination page size from preferences (Phase 5 - P5T04).
     * Configurable by user, defaults to 50 messages per page.
     *
     * Performance impact:
     * - Before: Load all messages (~50MB for 100 messages, >500ms load time)
     * - After: Load N at a time (~25MB initial for default 50, <250ms load time)
     */
    private val messagePageSize: Int
        get() = chatPreferences.getMessagePageSize()

    /**
     * Timestamp of last conversation list fetch (Phase 5 - P5T04).
     * Used to avoid refetching conversation list on every history show.
     * Cache is invalidated when new conversations are created or deleted.
     */
    private var conversationsCacheTimestamp = 0L

    /**
     * Conversations cache TTL from preferences (Phase 5 - P5T04).
     * Configurable by user, defaults to 5000ms (5 seconds).
     */
    private val conversationsCacheTTL: Long
        get() = chatPreferences.getConversationsCacheTTL()

    /**
     * Timestamp of last candidate intents fetch (Phase 5 - P5T04).
     * Used to avoid refetching intents on every message send.
     * Cache is invalidated when new training examples are added.
     */
    private var candidateIntentsCacheTimestamp = 0L

    /**
     * Intents cache TTL from preferences (Phase 5 - P5T04).
     * Configurable by user, defaults to 10000ms (10 seconds).
     */
    private val intentsCacheTTL: Long
        get() = chatPreferences.getIntentsCacheTTL()

    // ==================== NLU Components ====================
    // Classification cache is now managed by NLUCoordinator (SOLID refactoring C-01)

    /**
     * NLU initialization state - delegated to StatusIndicatorState (SOLID refactoring)
     */
    val isNLUReady: StateFlow<Boolean> = statusIndicatorState.isNLUReady

    // ==================== Status Indicator State (REQ-001, REQ-002, REQ-003) ====================
    // Delegated to StatusIndicatorState (SOLID refactoring)

    /**
     * NLU model loaded state (REQ-002) - delegated to StatusIndicatorState.
     */
    val isNLULoaded: StateFlow<Boolean> = statusIndicatorState.isNLULoaded

    /**
     * LLM model loaded state (REQ-002) - delegated to StatusIndicatorState.
     */
    val isLLMLoaded: StateFlow<Boolean> = statusIndicatorState.isLLMLoaded

    /**
     * Which system last responded (REQ-001) - delegated to StatusIndicatorState.
     */
    val lastResponder: StateFlow<String?> = statusIndicatorState.lastResponder

    /**
     * Timestamp when last responder was set (REQ-001) - delegated to StatusIndicatorState.
     */
    val lastResponderTimestamp: StateFlow<Long> = statusIndicatorState.lastResponderTimestamp

    /**
     * LLM fallback invoked flag (REQ-003 CRITICAL) - delegated to StatusIndicatorState.
     */
    val llmFallbackInvoked: StateFlow<Boolean> = statusIndicatorState.llmFallbackInvoked

    // ==================== State ====================
    // Most UI state now delegated to ChatUIStateManager (SOLID refactoring)

    /**
     * Current active conversation ID - delegated to ChatUIStateManager.
     */
    val activeConversationId: StateFlow<String?> = uiStateManager.activeConversationId

    /**
     * List of messages for the active conversation - delegated to ChatUIStateManager.
     */
    val messages: StateFlow<List<Message>> = uiStateManager.messages

    /**
     * Loading state - delegated to ChatUIStateManager.
     */
    val isLoading: StateFlow<Boolean> = uiStateManager.isLoading

    /**
     * Error message state - delegated to ChatUIStateManager.
     */
    val errorMessage: StateFlow<String?> = uiStateManager.errorMessage

    /**
     * Candidate intents for NLU classification - delegated to NLUCoordinator.
     */
    val candidateIntents: StateFlow<List<String>> = nluCoordinator.candidateIntents

    /**
     * Teach-AVA mode state - delegated to ChatUIStateManager.
     */
    val teachAvaModeMessageId: StateFlow<String?> = uiStateManager.teachAvaModeMessageId

    /**
     * Bottom sheet visibility state - delegated to ChatUIStateManager.
     */
    val showTeachBottomSheet: StateFlow<Boolean> = uiStateManager.showTeachBottomSheet

    /**
     * App preference bottom sheet state - delegated to ChatUIStateManager.
     */
    val showAppPreferenceSheet: StateFlow<Boolean> = uiStateManager.showAppPreferenceSheet

    /**
     * Capability requiring app resolution - delegated to ChatUIStateManager.
     */
    val resolutionCapability: StateFlow<String?> = uiStateManager.resolutionCapability

    // ==================== Confidence Learning Dialog State (REQ-004) ====================
    // Delegated to ChatUIStateManager (SOLID refactoring)

    /**
     * Confidence learning dialog state - delegated to ChatUIStateManager.
     */
    val confidenceLearningDialogState: StateFlow<com.augmentalis.chat.components.ConfidenceLearningState?> = uiStateManager.confidenceLearningDialogState

    // ==================== Developer Settings (REQ-007) ====================

    /**
     * Flash mode enabled state (REQ-007).
     * When true, StatusIndicator pulses/flashes during active NLU/LLM processing.
     * Useful for developers and QA to see which system is working in real-time.
     */
    val isFlashModeEnabled: StateFlow<Boolean> = com.augmentalis.ava.core.data.prefs.DeveloperPreferences(context)
        .isFlashModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Current message being taught - delegated to ChatUIStateManager.
     */
    val currentTeachMessageId: StateFlow<String?> = uiStateManager.currentTeachMessageId

    /**
     * History overlay visibility state - delegated to ChatUIStateManager.
     */
    val showHistoryOverlay: StateFlow<Boolean> = uiStateManager.showHistoryOverlay

    /**
     * Show accessibility permission prompt - delegated to ChatUIStateManager.
     */
    val showAccessibilityPrompt: StateFlow<Boolean> = uiStateManager.showAccessibilityPrompt

    /**
     * List of all conversations - delegated to ChatUIStateManager.
     */
    val conversations: StateFlow<List<Conversation>> = uiStateManager.conversations

    // ==================== Message Pagination State (Phase 5 - P5T04) ====================
    // Delegated to ChatUIStateManager (SOLID refactoring)

    /**
     * Current offset for message pagination - delegated to ChatUIStateManager.
     */
    val messageOffset: StateFlow<Int> = uiStateManager.messageOffset

    /**
     * Has more messages flag - delegated to ChatUIStateManager.
     */
    val hasMoreMessages: StateFlow<Boolean> = uiStateManager.hasMoreMessages

    /**
     * Total message count - delegated to ChatUIStateManager.
     */
    val totalMessageCount: StateFlow<Int> = uiStateManager.totalMessageCount

    /**
     * Current generation job (for cancellation support).
     * Set when LLM response generation starts, cleared when complete.
     * Used by stopGeneration() to cancel ongoing generation.
     *
     * Issue 3.3 Fix: @Volatile for thread-safe visibility across coroutines.
     */
    @Volatile
    private var generationJob: kotlinx.coroutines.Job? = null

    // ==================== TTS State (Phase 1.2) ====================

    /**
     * TTS initialization state (delegated to TTSCoordinator)
     */
    val isTTSReady: StateFlow<Boolean> = ttsCoordinator.isTTSReady

    /**
     * Currently speaking state (delegated to TTSCoordinator)
     */
    val isTTSSpeaking: StateFlow<Boolean> = ttsCoordinator.isTTSSpeaking

    /**
     * ID of message currently being spoken (delegated to TTSCoordinator)
     */
    val speakingMessageId: StateFlow<String?> = ttsCoordinator.speakingMessageId

    /**
     * TTS settings (delegated to TTSCoordinator)
     */
    val ttsSettings: StateFlow<com.augmentalis.chat.tts.TTSSettings> = ttsCoordinator.ttsSettings

    // ==================== RAG State (RAG Phase 2, Task 2) ====================

    // ==================== Initialization ====================

    init {
        initializeNLU()
        initializeConversation()
        initializeActions()
        initializeLLM()
        initializeRAG()
        observeWakeWordEvents()
    }

    /**
     * Observe wake word detection events via WakeWordEventBus.
     * When a wake word is detected, triggers voice input mode.
     *
     * P1 SOLID Fix: Uses WakeWordEventBus instead of reflection.
     * This removes the dependency on app module and enables proper testing.
     *
     * Integration flow:
     * 1. WakeWordService detects wake word → broadcasts intent
     * 2. MainActivity.wakeWordReceiver receives → emits to WakeWordEventBus
     * 3. ChatViewModel observes via event bus → triggers onWakeWordDetected()
     */
    private fun observeWakeWordEvents() {
        viewModelScope.launch {
            wakeWordEventBus.events.collect { event ->
                Log.d(TAG, "Wake word event received: ${event.keyword} at ${event.timestamp}")
                onWakeWordDetected(event.keyword)
            }
        }
    }

    /**
     * Called when wake word is detected.
     * Triggers voice input mode for hands-free interaction.
     *
     * @param keyword The detected wake word (e.g., "AVA", "Hey AVA")
     */
    private fun onWakeWordDetected(keyword: String) {
        Log.i(TAG, "Wake word detected: $keyword - ready for voice input")
        uiStateManager.setWakeWordDetected(keyword)

        // TODO: Trigger speech recognition here
        // For now, show visual feedback that wake word was detected
        // Speech recognition will be implemented in Phase 2
    }

    /** Wake word detected state - delegated to ChatUIStateManager */
    val wakeWordDetected: StateFlow<String?> = uiStateManager.wakeWordDetected

    /** Clear wake word detected state after handling */
    fun clearWakeWordDetected() {
        uiStateManager.clearWakeWordDetected()
    }

    /**
     * Initialize NLU classifier via NLUCoordinator.
     *
     * SOLID Refactoring (C-01): Delegates to NLUCoordinator for single responsibility.
     * The coordinator handles:
     * - Model availability check
     * - IntentClassifier initialization
     * - Candidate intents loading
     * - Classification caching
     *
     * Error handling:
     * - Model not found: Sets error state, UI should prompt download
     * - Initialization failed: Logs error, falls back to non-NLU mode
     */
    private fun initializeNLU() {
        viewModelScope.launch {
            Log.d(TAG, "Initializing NLU via NLUCoordinator...")

            when (val result = nluCoordinator.initialize()) {
                is Result.Success -> {
                    // Sync coordinator state to StatusIndicatorState
                    statusIndicatorState.setNLUReady(nluCoordinator.isNLUReady.value)
                    statusIndicatorState.setNLULoaded(nluCoordinator.isNLULoaded.value)
                    Log.i(TAG, "NLU initialized via coordinator - ready: ${isNLUReady.value}")
                }
                is Result.Error -> {
                    uiStateManager.setError(result.message)
                    statusIndicatorState.setNLULoaded(false)
                    Log.e(TAG, "NLU initialization failed: ${result.message}", result.exception)
                }
            }

            // Observe coordinator's NLU ready state for ongoing updates
            launch {
                nluCoordinator.isNLUReady.collect { ready ->
                    statusIndicatorState.setNLUReady(ready)
                }
            }

            // Initialize StatusIndicatorState flash mode with viewModelScope
            statusIndicatorState.initFlashMode(viewModelScope)
        }
    }

    /**
     * Initializes the active conversation (Phase 4, Task P4T03; Phase 5, Task P5T03).
     * Respects conversation mode preference to either append to last or create new.
     *
     * Flow (Phase 5 - P5T03):
     * 1. Check conversation mode from ChatPreferences
     * 2. If APPEND mode:
     *    a. Load last active conversation ID from preferences
     *    b. If valid, restore that conversation
     *    c. Otherwise, load most recent conversation
     * 3. If NEW mode:
     *    a. Always create new conversation
     * 4. Load messages for active conversation
     * 5. Load conversation list for history overlay
     *
     * Conversation modes:
     * - APPEND (default): Continue most recent conversation across app restarts
     * - NEW: Create fresh conversation each time app opens
     */
    private fun initializeConversation() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Initializing conversation...")

                val mode = conversationMode.value
                Log.d(TAG, "Conversation mode: $mode")

                when (mode) {
                    ConversationMode.APPEND -> {
                        // Try to restore last active conversation
                        val lastActiveId = chatPreferences.getLastActiveConversationId()
                        Log.d(TAG, "Last active conversation ID: $lastActiveId")

                        if (lastActiveId != null) {
                            // Validate conversation still exists
                            when (val result = conversationRepository.getConversationById(lastActiveId)) {
                                is Result.Success -> {
                                    uiStateManager.setActiveConversationId(lastActiveId)
                                    observeMessages(lastActiveId)
                                    Log.d(TAG, "Restored last active conversation: ${result.data.title} (ID: $lastActiveId)")
                                    loadConversations()
                                    return@launch
                                }
                                is Result.Error -> {
                                    Log.w(TAG, "Last active conversation not found, falling back to most recent")
                                    // Fall through to load most recent
                                }
                            }
                        }

                        // No valid last active ID, load most recent or create new
                        // Use firstOrNull with timeout to avoid indefinite blocking
                        val conversations = withTimeoutOrNull(5000L) {
                            conversationRepository.getAllConversations().first()
                        } ?: emptyList()
                        if (conversations.isNotEmpty()) {
                            // Edge case: Null-safe access to mostRecent conversation
                            val mostRecent = conversations.maxByOrNull { it.updatedAt }
                            if (mostRecent != null) {
                                uiStateManager.setActiveConversationId(mostRecent.id)
                                observeMessages(mostRecent.id)
                                chatPreferences.setLastActiveConversationId(mostRecent.id)
                                Log.d(TAG, "Loaded most recent conversation: ${mostRecent.title} (ID: ${mostRecent.id})")
                            } else {
                                // Edge case: List is not empty but maxByOrNull returned null (shouldn't happen, but defensive)
                                Log.w(TAG, "Conversation list not empty but maxByOrNull returned null, creating new")
                                createNewConversationInternal(conversationRepository, "New Conversation")
                            }
                        } else {
                            // No conversations exist, create a new one
                            Log.d(TAG, "No conversations found, creating new conversation")
                            createNewConversationInternal(conversationRepository, "New Conversation")
                        }
                    }

                    ConversationMode.NEW -> {
                        // Always create new conversation
                        Log.d(TAG, "NEW mode: Creating new conversation")
                        createNewConversationInternal(conversationRepository, "New Conversation")
                    }
                }

                // Load conversations for history overlay
                loadConversations()

                // Show welcome message if conversation is empty (Phase 6)
                showWelcomeMessageIfNeeded()
            } catch (e: Exception) {
                uiStateManager.setError("Initialization failed: ${e.message}")
                Log.e(TAG, "Exception in initializeConversation", e)
            }
        }
    }

    /**
     * Helper method to create a new conversation (Phase 5 - P5T03).
     * Extracted for reuse in initializeConversation().
     *
     * @param repo ConversationRepository instance
     * @param title Initial conversation title
     */
    private suspend fun createNewConversationInternal(
        repo: ConversationRepository,
        title: String
    ) {
        when (val result = repo.createConversation(title)) {
            is Result.Success -> {
                uiStateManager.setActiveConversationId(result.data.id)
                observeMessages(result.data.id)
                chatPreferences.setLastActiveConversationId(result.data.id)
                Log.d(TAG, "Created new conversation: ${result.data.title} (ID: ${result.data.id})")
            }
            is Result.Error -> {
                uiStateManager.setError("Failed to create conversation: ${result.message}")
                Log.e(TAG, "Failed to create initial conversation", result.exception)
            }
        }
    }

    /**
     * Show welcome message if conversation is empty.
     * Phase 6 (Fix): Shows context-aware welcome message based on NLU readiness.
     *
     * When NLU is not ready, explains to user that voice commands are still loading
     * but LLM is available for conversation.
     */
    private suspend fun showWelcomeMessageIfNeeded() {
        val conversationId = activeConversationId.value ?: return

        // Check if conversation is empty (no messages yet)
        if (messages.value.isEmpty()) {
            val welcomeContent = if (!isNLUReady.value) {
                "Hello! I'm AVA, your AI assistant. I'm still waking up my voice command system, " +
                "but I can chat with you using my language model. Voice commands will be available in a moment..."
            } else {
                "Hello! I'm AVA, your AI assistant. How can I help you today?"
            }

            val welcomeMessage = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = welcomeContent,
                timestamp = System.currentTimeMillis(),
                intent = null,
                confidence = null
            )

            when (messageRepository.addMessage(welcomeMessage)) {
                is Result.Success -> {
                    Log.d(TAG, "Welcome message shown (NLU ready: ${isNLUReady.value})")
                }
                is Result.Error -> {
                    Log.w(TAG, "Failed to show welcome message")
                }
            }
        }
    }

    /**
     * Initialize action handlers (Agent 1: Intent Action Handlers).
     * Registers all built-in action handlers for executing intents.
     *
     * This enables AVA to actually perform actions (open apps, set alarms, etc.)
     * instead of just returning template responses.
     *
     * Error handling:
     * - Initialization is idempotent (safe to call multiple times)
     * - Failures are logged but don't block app startup
     * - Template responses still work if actions fail to initialize
     */
    /**
     * ADR-014 Phase B (C1): Initialize action handlers and wait for ready state.
     * Ensures ActionsManager is fully initialized before processing messages.
     */
    private fun initializeActions() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Initializing action handlers...")
                actionsManager.initialize()

                // Wait for ready state with timeout
                val ready = withTimeoutOrNull(5000L) {
                    actionsManager.isReady.first { it }
                }

                if (ready == true) {
                    Log.i(TAG, "Action handlers initialized and ready")
                } else {
                    Log.w(TAG, "Action handlers initialization timed out")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize action handlers: ${e.message}", e)
                // Non-fatal: App continues with template responses only
            }
        }
    }

    /**
     * Initialize LLM for response generation.
     *
     * Attempts to initialize the HybridResponseGenerator's LLM component.
     * If initialization fails, the system will fall back to improved templates.
     *
     * Benefits of LLM initialization:
     * - Natural language responses for any query
     * - Context-aware generation
     * - Handles queries that don't match known intents
     */
    private fun initializeLLM() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Initializing LLM for response generation...")

                // Check if responseGenerator is HybridResponseGenerator
                if (responseGenerator is com.augmentalis.llm.response.HybridResponseGenerator) {
                    // DYNAMIC MODEL DISCOVERY: No hardcoded model names
                    // Uses ModelDiscovery to find installed models at runtime
                    val modelDiscovery = com.augmentalis.llm.alc.loader.ModelDiscovery(context)

                    // Get selected model from preferences, or discover first available
                    val selectedModelId = chatPreferences.getSelectedLLMModel()
                    val discoveredModel = if (selectedModelId != null) {
                        modelDiscovery.getModelById(selectedModelId)
                    } else {
                        // No model selected - find first available
                        modelDiscovery.getFirstAvailableModel()
                    }

                    if (discoveredModel == null) {
                        Log.w(TAG, "No LLM models found on device - LLM responses disabled")
                        Log.i(TAG, "Install models to /sdcard/ava-ai-models/llm/ to enable LLM")
                        // TODO: Show UI prompt to download model (see #backlog)
                        // TODO: Provide download URL or guide user to model installation
                        return@launch
                    }

                    Log.i(TAG, "Discovered LLM model: ${discoveredModel.id} (${discoveredModel.getDisplaySize()})")

                    // Build config from discovered model
                    // Use the model's format extension (v2.0: .adm for MLC, .adg for GGUF, .adr for LiteRT)
                    val modelExtension = when (discoveredModel.format) {
                        com.augmentalis.llm.alc.loader.ModelDiscovery.ModelFormat.MLC -> ".adm"
                        com.augmentalis.llm.alc.loader.ModelDiscovery.ModelFormat.GGUF -> ".adg"
                        com.augmentalis.llm.alc.loader.ModelDiscovery.ModelFormat.LITERT -> ".adr"
                    }
                    val llmConfig = com.augmentalis.llm.domain.LLMConfig(
                        modelPath = discoveredModel.path,
                        modelLib = "${discoveredModel.id}$modelExtension",  // e.g., AVA-GE3-4B16.adm
                        device = "cpu",
                        maxMemoryMB = if (discoveredModel.sizeGB > 2.0f) 3072 else 1536
                    )

                    val result = responseGenerator.initialize(llmConfig)

                    when (result) {
                        is com.augmentalis.ava.core.common.Result.Success -> {
                            Log.i(TAG, "LLM initialized successfully (${discoveredModel.id}) - natural responses enabled")
                            // Save selection for next time
                            chatPreferences.setSelectedLLMModel(discoveredModel.id)
                        }
                        is com.augmentalis.ava.core.common.Result.Error -> {
                            Log.w(TAG, "LLM initialization failed: ${result.message}")
                            Log.w(TAG, "Falling back to improved template responses")
                        }
                    }
                } else {
                    Log.d(TAG, "ResponseGenerator is not HybridResponseGenerator, using templates only")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception initializing LLM: ${e.message}", e)
                // Non-critical: Improved templates will still provide good responses
            }
        }
    }

    /**
     * Initialize RAG settings (RAG Phase 2, Task 1 - Settings Foundation).
     *
     * Logs current RAG configuration from preferences.
     * Full RAG integration (document retrieval, context injection) is pending Task 2.
     */
    private fun initializeRAG() {
        Log.d(TAG, "RAG settings initialized - enabled: ${ragEnabled.value}, threshold: ${ragThreshold.value}")
        Log.d(TAG, "RAG Phase 2 status: Task 1 complete (settings), Task 2 pending (integration)")
    }

    /**
     * Observes messages for the given conversation ID (Phase 5, Task P5T04).
     * Loads messages using pagination for better performance.
     *
     * Flow:
     * 1. Reset pagination state (offset, hasMore)
     * 2. Get total message count for conversation
     * 3. Load initial page of messages (most recent MESSAGE_PAGE_SIZE messages)
     * 4. Update UI state with loaded messages
     * 5. Observe Flow for real-time updates (new messages sent)
     *
     * Performance:
     * - Before: Load all messages (~500ms for 100 messages)
     * - After: Load first 50 messages (~250ms for 50 messages)
     *
     * @param conversationId ID of conversation to observe
     */
    private fun observeMessages(conversationId: String) {
        viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "Loading messages for conversation: $conversationId")

                // 1. Reset pagination state
                uiStateManager.resetPagination()

                // 2. Get total message count
                var currentTotalCount = 0
                when (val countResult = messageRepository.getMessageCount(conversationId)) {
                    is Result.Success -> {
                        currentTotalCount = countResult.data
                        uiStateManager.updatePagination(0, true, currentTotalCount)
                        Log.d(TAG, "Total messages in conversation: ${countResult.data}")
                    }
                    is Result.Error -> {
                        Log.w(TAG, "Failed to get message count: ${countResult.message}")
                    }
                }

                // 3. Load initial page using pagination
                when (val result = messageRepository.getMessagesPaginated(
                    conversationId = conversationId,
                    limit = messagePageSize,
                    offset = 0
                )) {
                        is Result.Success -> {
                            val loadedMessages = result.data
                            uiStateManager.setMessages(loadedMessages)
                            val newOffset = loadedMessages.size

                            // Update hasMore flag (Edge case: Handle exact page size match)
                            // hasMore is true if: we got a full page AND there are more messages beyond current offset
                            val hasMore = loadedMessages.size >= messagePageSize && newOffset < currentTotalCount
                            uiStateManager.updatePagination(newOffset, hasMore, currentTotalCount)

                            // Edge case logging: If we loaded exactly all messages
                            if (newOffset >= currentTotalCount) {
                                Log.d(TAG, "All messages loaded (offset=$newOffset, total=$currentTotalCount)")
                            }

                            val loadTime = System.currentTimeMillis() - startTime
                            Log.i(TAG, "=== Message Load Performance (Phase 5 - P5T04) ===")
                            Log.i(TAG, "  Loaded: ${loadedMessages.size} messages")
                            Log.i(TAG, "  Total: $currentTotalCount messages")
                            Log.i(TAG, "  Load time: ${loadTime}ms")
                            Log.i(TAG, "  Has more: $hasMore")
                            Log.i(TAG, "  Memory saved: ~${(currentTotalCount - loadedMessages.size) * 0.5}MB (estimated)")
                        }
                        is Result.Error -> {
                            uiStateManager.setError("Failed to load messages: ${result.message}")
                            Log.e(TAG, "Failed to load paginated messages", result.exception)
                        }
                    }

                // 4. Observe Flow for real-time updates (new messages)
                // This ensures newly sent messages appear without manual refresh
                messageRepository.getMessagesForConversation(conversationId)
                    .catch { e ->
                        Log.e(TAG, "Message Flow error: ${e.message}", e)
                    }
                    .collect { allMessages ->
                        // Only update if we have new messages beyond our current offset
                        // This prevents overwriting paginated data with full dataset
                        // Edge case: Protect against race condition where allMessages could be empty during deletion
                        if (allMessages.isEmpty()) {
                            Log.d(TAG, "Flow returned empty message list - possible deletion or race condition")
                            return@collect
                        }

                        val currentMessages = messages.value
                        if (allMessages.size > currentMessages.size) {
                            Log.d(TAG, "New messages detected: ${allMessages.size - currentMessages.size} new")
                            // Edge case: Ensure we don't exceed total message count
                            val currentOffset = messageOffset.value
                            val safeOffset = minOf(currentOffset, allMessages.size)
                            uiStateManager.setMessages(allMessages.takeLast(maxOf(safeOffset, allMessages.size)))
                            uiStateManager.updatePagination(currentOffset, hasMoreMessages.value, allMessages.size)
                        }
                    }

            } catch (e: Exception) {
                uiStateManager.setError("Failed to load messages: ${e.message}")
                Log.e(TAG, "Exception in observeMessages", e)
            }
        }
    }

    // ==================== Public Methods ====================

    /**
     * Activates teach mode for a specific message (shows "Teach AVA" button and bottom sheet).
     * Called when user long-presses a message or when auto-prompt triggers on low confidence.
     *
     * Phase 3, Task P3T04: Updated to trigger bottom sheet display.
     *
     * @param messageId ID of message to teach
     */
    fun activateTeachMode(messageId: String) {
        uiStateManager.setTeachAvaModeMessageId(messageId)
        uiStateManager.openTeachBottomSheet(messageId)
        Log.d(TAG, "Teach mode activated for message: $messageId (bottom sheet shown)")
    }

    /**
     * Deactivates teach mode (clears the teach button).
     */
    fun deactivateTeachMode() {
        uiStateManager.setTeachAvaModeMessageId(null)
        Log.d(TAG, "Teach mode deactivated")
    }

    /**
     * Dismisses the Teach-AVA bottom sheet (Phase 3, Task P3T04).
     * Called when user closes the bottom sheet without selecting an intent,
     * or after successfully teaching an intent.
     *
     * Clears both the bottom sheet visibility and current teach message ID.
     */
    fun dismissTeachBottomSheet() {
        uiStateManager.closeTeachBottomSheet()
        Log.d(TAG, "Teach bottom sheet dismissed")
    }

    /**
     * Shows the conversation history overlay (Phase 4, Task P4T02).
     * Called when user triggers "show_history" intent or taps history button.
     *
     * Flow:
     * 1. Set overlay visibility to true
     * 2. Load/refresh conversation list from repository
     * 3. UI displays overlay with conversation list
     *
     * @see dismissHistory
     * @see loadConversations
     */
    fun showHistory() {
        uiStateManager.setShowHistoryOverlay(true)
        loadConversations()
        Log.d(TAG, "History overlay shown")
    }

    /**
     * Dismisses the conversation history overlay (Phase 4, Task P4T02).
     * Called when user closes overlay or selects a conversation.
     */
    fun dismissHistory() {
        uiStateManager.setShowHistoryOverlay(false)
        Log.d(TAG, "History overlay dismissed")
    }

    /**
     * Dismiss accessibility permission prompt (ADR-014 Phase 4).
     * Called when user closes the dialog or navigates to settings.
     */
    fun dismissAccessibilityPrompt() {
        uiStateManager.hideAccessibilityPrompt()
        Log.d(TAG, "Accessibility prompt dismissed")
    }

    /**
     * ADR-014 Phase B (C4): Dismiss app preference sheet.
     */
    fun dismissAppPreferenceSheet() {
        uiStateManager.hideAppPreferenceSheet()
        Log.d(TAG, "App preference sheet dismissed")
    }

    /**
     * ADR-014 Phase B (C4): Handle user app selection.
     * Saves the user's preference and re-executes the action.
     */
    fun onAppSelected(capability: String, packageName: String, appName: String, remember: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "User selected app: $appName ($packageName) for capability: $capability, remember: $remember")

                // Save preference via ActionsManager
                actionsManager.saveAppPreference(capability, packageName, appName, remember)

                // Dismiss sheet
                dismissAppPreferenceSheet()

                // TODO: Re-execute the original action that triggered the resolution
                // This would require storing the original intent and utterance
                Log.d(TAG, "App preference saved. User can retry the command.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save app preference: ${e.message}", e)
                uiStateManager.setError("Failed to save preference: ${e.message}")
            }
        }
    }

    /**
     * ADR-014 Phase D3: Open device accessibility settings.
     * Called when user wants to enable VoiceOS accessibility service.
     */
    fun openAccessibilitySettings() {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            // Dismiss prompt after opening settings
            uiStateManager.hideAccessibilityPrompt()
            Log.d(TAG, "Opened accessibility settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings", e)
        }
    }

    /**
     * Gets the current message being taught (Phase 3, Task P3T04).
     * Used by the UI to display the utterance in the bottom sheet.
     *
     * @return Message with ID matching _currentTeachMessageId, or null if not found
     */
    fun getCurrentTeachMessage(): Message? {
        val messageId = currentTeachMessageId.value ?: return null
        return messages.value.find { it.id == messageId }
    }

    /**
     * Handles the Teach-AVA action when user selects an intent (Phase 3, Task P3T03).
     * Creates a TrainExample entity and saves it to the database.
     *
     * Flow:
     * 1. Retrieve the message from _messages state
     * 2. Generate hash for deduplication (MD5 of utterance + intent)
     * 3. Create TrainExample entity with user-selected intent
     * 4. Save to TrainExampleRepository
     * 5. Reload candidate intents to include new intent
     * 6. Show success message
     * 7. Dismiss bottom sheet and deactivate teach mode
     *
     * Error handling:
     * - Message not found: Sets error state, doesn't crash
     * - Repository save failure: Sets error state, logs exception
     * - Duplicate example: Shows user-friendly error message
     *
     * @param messageId ID of message to teach (should match _currentTeachMessageId)
     * @param intent User-selected intent for this utterance
     */
    fun handleTeachAva(messageId: String, intent: String) {
        viewModelScope.launch {
            try {
                uiStateManager.setLoading(true)
                uiStateManager.clearError()
                Log.d(TAG, "handleTeachAva called: messageId=$messageId, intent=$intent")

                // 1. Get the message from _messages state
                val message = messages.value.find { it.id == messageId }
                if (message == null) {
                    uiStateManager.setError("Message not found")
                    Log.e(TAG, "Cannot teach AVA: message with ID $messageId not found")
                    return@launch
                }

                val utterance = message.content
                Log.d(TAG, "Teaching utterance: '$utterance' -> intent: '$intent'")

                // 2. Generate hash for deduplication
                val exampleHash = com.augmentalis.ava.core.data.repository.TrainExampleRepositoryImpl
                    .generateHash(utterance, intent)

                // 3. Create TrainExample entity
                val trainExample = com.augmentalis.ava.core.domain.model.TrainExample(
                    id = 0, // Auto-generated by database
                    exampleHash = exampleHash,
                    utterance = utterance,
                    intent = intent,
                    locale = Locale.getDefault().toLanguageTag(), // Use device locale
                    source = com.augmentalis.ava.core.domain.model.TrainExampleSource.MANUAL,
                    createdAt = System.currentTimeMillis(),
                    usageCount = 0,
                    lastUsed = null
                )

                // 4. Save to TrainExampleRepository
                when (val result = trainExampleRepository.addTrainExample(trainExample)) {
                    is Result.Success -> {
                        Log.d(TAG, "Successfully saved training example: $trainExample")

                        // Invalidate intents cache (Phase 5 - P5T04)
                        candidateIntentsCacheTimestamp = 0L
                        Log.d(TAG, "Invalidated candidate intents cache after training example saved")

                        // Clear NLU classification cache to force re-classification (Phase 5 - P5T04)
                        clearNLUCache()

                        // 5. Reload candidate intents to pick up new intent
                        loadCandidateIntents()

                        // 6. Show success message (will be displayed by UI as toast)
                        uiStateManager.setError("Successfully taught AVA: \"$utterance\" → $intent")

                        // 7. Dismiss bottom sheet and deactivate teach mode
                        dismissTeachBottomSheet()
                        deactivateTeachMode()

                        Log.i(TAG, "Teach AVA completed successfully")
                    }
                    is Result.Error -> {
                        // Handle specific errors
                        val errorMsg = if (result.message?.contains("Duplicate") == true) {
                            "This example already exists in your training data"
                        } else {
                            "Failed to save training example: ${result.message}"
                        }
                        uiStateManager.setError(errorMsg)
                        Log.e(TAG, "Failed to save training example", result.exception)
                    }
                }

            } catch (e: Exception) {
                uiStateManager.setError("Failed to teach AVA: ${e.message}")
                Log.e(TAG, "Exception in handleTeachAva", e)
            } finally {
                uiStateManager.setLoading(false)
            }
        }
    }

    // ==================== Confidence Learning Methods (REQ-004) ====================

    /**
     * Confirm NLU's interpretation and learn from it (REQ-004).
     * Called when user clicks YES in confidence learning dialog.
     *
     * Issue 5.3: Now uses unified NLUSelfLearner for learning.
     */
    fun confirmInterpretation() {
        viewModelScope.launch {
            try {
                val dialogState = confidenceLearningDialogState.value ?: return@launch

                Log.i(TAG, "User confirmed interpretation: \"${dialogState.userInput}\" → ${dialogState.interpretedIntent}")

                // Issue 5.3: Use NLUSelfLearner for unified learning
                val success = nluSelfLearner.learnFromLLM(
                    utterance = dialogState.userInput,
                    intent = dialogState.interpretedIntent,
                    confidence = 1.0f, // User confirmed = high confidence
                    variations = emptyList()
                )

                if (success) {
                    Log.i(TAG, "Successfully learned confirmed interpretation via NLUSelfLearner")
                } else {
                    Log.w(TAG, "Failed to learn confirmed interpretation (may already exist)")
                }

                // Dismiss dialog
                uiStateManager.clearConfidenceLearningDialog()

            } catch (e: Exception) {
                Log.e(TAG, "Error confirming interpretation", e)
                uiStateManager.clearConfidenceLearningDialog()
            }
        }
    }

    /**
     * User selected an alternate intent (REQ-004).
     * Called when user selects alternate from dialog.
     *
     * Issue 5.3: Now uses unified NLUSelfLearner for learning.
     */
    fun selectAlternateIntent(alternate: com.augmentalis.chat.components.AlternateIntent) {
        viewModelScope.launch {
            try {
                val dialogState = confidenceLearningDialogState.value ?: return@launch

                Log.i(TAG, "User selected alternate: \"${dialogState.userInput}\" → ${alternate.intentId}")

                // Issue 5.3: Use NLUSelfLearner for unified learning
                val success = nluSelfLearner.learnFromLLM(
                    utterance = dialogState.userInput,
                    intent = alternate.intentId,
                    confidence = 1.0f, // User corrected = high confidence
                    variations = emptyList()
                )

                if (success) {
                    Log.i(TAG, "Successfully learned corrected interpretation via NLUSelfLearner")
                } else {
                    Log.w(TAG, "Failed to learn corrected interpretation (may already exist)")
                }

                // Dismiss dialog
                uiStateManager.clearConfidenceLearningDialog()

            } catch (e: Exception) {
                Log.e(TAG, "Error selecting alternate intent", e)
                uiStateManager.clearConfidenceLearningDialog()
            }
        }
    }

    /**
     * Dismiss confidence learning dialog without learning (REQ-004).
     * Called when user clicks SKIP button.
     */
    fun dismissConfidenceLearningDialog() {
        Log.d(TAG, "User dismissed confidence learning dialog")
        uiStateManager.clearConfidenceLearningDialog()
    }

    /**
     * Sends a user message and triggers AVA response.
     *
     * Flow (Task P2T03):
     * 1. Create user message entity
     * 2. Save to database
     * 3. Classify intent with NLU (tokenize → classify → get confidence)
     * 4. Generate AVA response template based on intent + confidence
     * 5. Save AVA message to database with intent/confidence metadata
     * 6. Update UI state
     *
     * Performance Target: <500ms end-to-end (user send → AVA response displayed)
     * - Tokenization: <5ms
     * - Classification: <100ms (target <50ms)
     * - Database ops: <40ms
     * - Total: ~145-205ms (well under 500ms target)
     *
     * @param text User message text
     */
    fun sendMessage(text: String) {
        // Edge case: Reject blank/empty messages early
        if (text.isBlank()) {
            Log.d(TAG, "Rejecting blank message")
            return
        }

        // Edge case: Ensure active conversation exists
        val conversationId = activeConversationId.value
        if (conversationId == null) {
            uiStateManager.setError("No active conversation")
            Log.e(TAG, "Cannot send message: no active conversation")
            return
        }

        // Cancel any ongoing generation before starting new one
        generationJob?.cancel()

        // Launch new generation job and store reference for cancellation
        generationJob = viewModelScope.launch {
            try {
                uiStateManager.setLoading(true)
                uiStateManager.clearError()
                statusIndicatorState.resetForNewMessage()  // Reset fallback flag (REQ-003)

                val totalStartTime = System.currentTimeMillis()

                // 1. Create user message
                val userMessage = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = MessageRole.USER,
                    content = text.trim(),
                    timestamp = System.currentTimeMillis()
                )

                // 2. Save user message to database
                val dbStartTime = System.currentTimeMillis()
                val userMessageResult = messageRepository.addMessage(userMessage)
                if (userMessageResult is Result.Error) {
                    uiStateManager.setError("Failed to save message: ${userMessageResult.message}")
                    return@launch
                }
                val dbTime = System.currentTimeMillis() - dbStartTime
                Log.d(TAG, "User message saved to DB in ${dbTime}ms")

                // 3. Classify intent with NLU via NLUCoordinator (SOLID refactoring C-01)
                val nluStartTime = System.currentTimeMillis()
                val classification = nluCoordinator.classify(text)

                val classifiedIntent: String?
                val confidenceScore: Float?
                val inferenceTimeMs: Long

                if (classification != null) {
                    classifiedIntent = classification.intent
                    confidenceScore = classification.confidence
                    inferenceTimeMs = classification.inferenceTimeMs
                    Log.d(TAG, "NLU Classification via coordinator:")
                    Log.d(TAG, "  Intent: $classifiedIntent")
                    Log.d(TAG, "  Confidence: $confidenceScore")
                    Log.d(TAG, "  Inference time: ${inferenceTimeMs}ms")
                } else {
                    // NLU not ready or classification failed
                    Log.d(TAG, "NLU not ready, skipping classification - using LLM-only mode")
                    classifiedIntent = null
                    confidenceScore = null
                    inferenceTimeMs = 0
                    // REQ-003: Mark that LLM will handle (NLU not available)
                    statusIndicatorState.markLLMResponded()
                    Log.i(TAG, "*** LLM HANDLING (NLU Not Ready) ***")
                }

                val nluTime = System.currentTimeMillis() - nluStartTime
                Log.d(TAG, "NLU classification completed in ${nluTime}ms")

                // 3.5. Handle built-in intents (Phase 4, Task P4T02)
                // Edge case: Null-safe confidence check before comparison
                val currentThreshold = confidenceThreshold.value
                if (classifiedIntent == BuiltInIntents.SHOW_HISTORY &&
                    confidenceScore != null &&
                    confidenceScore > currentThreshold) {
                    Log.d(TAG, "Handling show_history intent (confidence=$confidenceScore, threshold=$currentThreshold)")
                    showHistory()
                    // Return early - don't create AVA response message for system intents
                    return@launch
                }

                // 3.6. Execute action handlers via ActionCoordinator (SOLID refactoring C-01)
                // Try to execute action if handler exists and confidence is above threshold
                if (classifiedIntent != null &&
                    confidenceScore != null &&
                    confidenceScore > currentThreshold &&
                    actionCoordinator.hasHandler(classifiedIntent)) {

                    Log.d(TAG, "Executing action via ActionCoordinator: $classifiedIntent (confidence=$confidenceScore)")
                    val actionStartTime = System.currentTimeMillis()

                    val category = actionCoordinator.getCategoryForIntent(classifiedIntent)
                    val actionExecutionResult = actionCoordinator.executeActionWithRouting(
                        intent = classifiedIntent,
                        category = category,
                        utterance = text.trim()
                    )

                    val actionTime = System.currentTimeMillis() - actionStartTime
                    Log.d(TAG, "Action executed via coordinator in ${actionTime}ms")

                    // Update response content based on action result
                    val actionFeedback = when (actionExecutionResult) {
                        is IActionCoordinator.ActionExecutionResult.Success -> {
                            if (actionExecutionResult.needsAccessibility) {
                                Log.d(TAG, "Accessibility service needed, showing prompt")
                                uiStateManager.showAccessibilityPrompt()
                            }
                            actionExecutionResult.message
                        }
                        is IActionCoordinator.ActionExecutionResult.Failure -> {
                            Log.w(TAG, "Action failed: ${actionExecutionResult.message}")
                            actionExecutionResult.message
                        }
                        is IActionCoordinator.ActionExecutionResult.NeedsResolution -> {
                            Log.d(TAG, "Action needs app resolution for capability: ${actionExecutionResult.capability}")
                            // ADR-014 Phase B (C4): Trigger app preference sheet
                            uiStateManager.showAppPreferenceSheet(actionExecutionResult.capability)
                            actionExecutionResult.message
                        }
                        is IActionCoordinator.ActionExecutionResult.NoHandler -> {
                            Log.w(TAG, "No handler found for intent: ${actionExecutionResult.intent}")
                            "I don't know how to handle that action yet."
                        }
                    }

                    // Create AVA message with action feedback
                    val avaMessage = Message(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        role = MessageRole.ASSISTANT,
                        content = actionFeedback,
                        timestamp = System.currentTimeMillis(),
                        intent = classifiedIntent,
                        confidence = confidenceScore
                    )

                    // Save AVA message to database
                    val avaDbStartTime = System.currentTimeMillis()
                    val avaMessageResult = messageRepository.addMessage(avaMessage)
                    if (avaMessageResult is Result.Error) {
                        uiStateManager.setError("Failed to save AVA response: ${avaMessageResult.message}")
                    }
                    val avaDbTime = System.currentTimeMillis() - avaDbStartTime
                    Log.d(TAG, "AVA message saved to DB in ${avaDbTime}ms")

                    // REQ-001: Mark NLU as last responder (high confidence action)
                    statusIndicatorState.markNLUResponded()
                    Log.i(TAG, "*** NLU HANDLED ACTION via Coordinator (Confidence: $confidenceScore) ***")

                    // Log total end-to-end time
                    val totalTime = System.currentTimeMillis() - totalStartTime
                    Log.i(TAG, "=== Message Send Performance Metrics (with Action) ===")
                    Log.i(TAG, "  User message DB: ${dbTime}ms")
                    Log.i(TAG, "  NLU classification: ${nluTime}ms (inference: ${inferenceTimeMs}ms)")
                    Log.i(TAG, "  Action execution: ${actionTime}ms")
                    Log.i(TAG, "  AVA message DB: ${avaDbTime}ms")
                    Log.i(TAG, "  Total end-to-end: ${totalTime}ms")

                    // Return early - action was executed, don't use template response
                    return@launch
                }

                // 4. Generate AVA response using LLM with template fallback
                val responseStartTime = System.currentTimeMillis()

                // Build IntentClassification for response generator (non-null version)
                val responseClassification = IntentClassification(
                    intent = classifiedIntent ?: BuiltInIntents.UNKNOWN,
                    confidence = confidenceScore ?: 0.0f,
                    inferenceTimeMs = inferenceTimeMs
                )

                // Build conversation history for context (Phase 1.1 - Multi-turn Context)
                // Get last 10 messages for context window
                val conversationHistory = when (val historyResult =
                    messageRepository.getRecentMessagesForContext(conversationId, limit = 10)) {
                    is Result.Success -> historyResult.data
                    is Result.Error -> {
                        Log.w(TAG, "Failed to load conversation history: ${historyResult.message}")
                        emptyList()
                    }
                }

                // Build response context with conversation history
                // Convert Message list to Pair<role, content> format expected by ResponseContext
                val formattedHistory = conversationHistory.map {
                    it.role.name to it.content
                }
                val responseContext = ResponseContext(
                    actionResult = null,
                    conversationHistory = formattedHistory
                )

                // 3.7. RAG Context Retrieval via RAGCoordinator (SOLID refactoring C-01)
                val ragStartTime = System.currentTimeMillis()
                val ragResult = ragCoordinator.retrieveContext(text.trim())
                val ragContext = ragResult.context
                val ragRetrievalTimeMs = ragResult.retrievalTimeMs

                // Update source citations from coordinator result
                _recentSourceCitations.value = ragResult.citations

                val ragTime = System.currentTimeMillis() - ragStartTime
                Log.d(TAG, "RAG retrieval via coordinator completed in ${ragTime}ms")
                if (ragContext != null) {
                    Log.d(TAG, "  RAG context: ${ragContext.length} chars from ${ragResult.citations.size} sources")
                }

                // 4. Generate AVA response via ResponseCoordinator (SOLID refactoring C-01)
                Log.d(TAG, "Generating response via ResponseCoordinator")
                Log.d(TAG, "  Intent: $classifiedIntent, Confidence: $confidenceScore")
                if (ragContext != null) {
                    Log.d(TAG, "  RAG context: ${ragContext.length} chars from ${ragResult.citations.size} sources")
                }

                val responseResult = responseCoordinator.generateResponse(
                    userMessage = text.trim(),
                    classification = responseClassification,
                    context = responseContext,
                    ragContext = ragContext,
                    scope = viewModelScope
                )

                val responseTime = System.currentTimeMillis() - responseStartTime
                Log.d(TAG, "Response generation via coordinator completed in ${responseTime}ms")

                // Update state from coordinator result
                statusIndicatorState.setLastResponder(responseResult.respondedBy)
                if (responseResult.wasLLMFallback) {
                    statusIndicatorState.setLLMFallbackInvoked(true)
                }

                // REQ-004: Trigger confidence learning dialog for low confidence responses
                // (Learning and response cleanup is now handled by ResponseCoordinator)
                if (responseResult.wasLLMFallback && classifiedIntent != null && confidenceScore != null) {
                    // Get alternate intents directly from responseClassification (no duplicate cache needed)
                    val allScores = responseClassification.allScores

                    // Convert to AlternateIntent list, excluding the top intent and low scores
                    val alternates = allScores
                        .filter { it.key != classifiedIntent && it.value >= 0.3f }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(3)
                        .map { (intentId, score) ->
                            com.augmentalis.chat.components.AlternateIntent(
                                intentId = intentId,
                                displayName = intentId,
                                confidence = score
                            )
                        }

                    Log.i(TAG, "REQ-004: Triggering confidence learning dialog with ${alternates.size} alternates")
                    uiStateManager.setConfidenceLearningDialogState(
                        com.augmentalis.chat.components.ConfidenceLearningState(
                            userInput = text.trim(),
                            interpretedIntent = classifiedIntent,
                            confidence = confidenceScore,
                            alternateIntents = alternates
                        )
                    )
                }

                // Get response content from coordinator result
                val responseContent = responseResult.content

                // 5. Create AVA message with intent and confidence
                val avaMessage = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = responseContent,
                    timestamp = System.currentTimeMillis(),
                    intent = classifiedIntent,
                    confidence = confidenceScore
                )

                // 6. Save AVA message to database
                val avaDbStartTime = System.currentTimeMillis()
                val avaMessageResult = messageRepository.addMessage(avaMessage)
                if (avaMessageResult is Result.Error) {
                    uiStateManager.setError("Failed to save AVA response: ${avaMessageResult.message}")
                }
                val avaDbTime = System.currentTimeMillis() - avaDbStartTime
                Log.d(TAG, "AVA message saved to DB in ${avaDbTime}ms")

                // 6.5. Auto-speak AVA response if enabled (Phase 1.2)
                val currentTTSSettings = ttsSettings.value
                if (currentTTSSettings.enabled && currentTTSSettings.autoSpeak) {
                    Log.d(TAG, "Auto-speaking AVA response (TTS enabled)")
                    speakMessage(responseContent)
                }

                // 7. Activate teach mode if low confidence (Task P2T06)
                if (shouldShowTeachButton(confidenceScore)) {
                    activateTeachMode(avaMessage.id)
                }

                // Log total end-to-end time (Task P2T03: Timing metrics)
                val totalTime = System.currentTimeMillis() - totalStartTime
                Log.i(TAG, "=== Message Send Performance Metrics (with LLM) ===")
                Log.i(TAG, "  User message DB: ${dbTime}ms")
                Log.i(TAG, "  NLU classification: ${nluTime}ms (inference: ${inferenceTimeMs}ms)")
                if (ragTime > 0) {
                    Log.i(TAG, "  RAG retrieval: ${ragTime}ms (search: ${ragRetrievalTimeMs}ms)")
                }
                Log.i(TAG, "  Response generation: ${responseTime}ms")
                Log.i(TAG, "  AVA message DB: ${avaDbTime}ms")
                Log.i(TAG, "  Total end-to-end: ${totalTime}ms")
                Log.i(TAG, "  Target: <500ms | Actual: ${totalTime}ms | " +
                        if (totalTime < 500) "✓ PASS" else "✗ FAIL (expected with LLM/RAG)")

                // UI updates automatically via observeMessages flow

            } catch (e: Exception) {
                uiStateManager.setError("Failed to send message: ${e.message}")
                Log.e(TAG, "Exception in sendMessage", e)
            } finally {
                uiStateManager.setLoading(false)
                generationJob = null
            }
        }
    }

    /**
     * Switches to a different conversation (Phase 4, Task P4T03; Phase 5, Task P5T03).
     * Called when user selects a conversation from history overlay.
     *
     * Flow:
     * 1. Validate conversation exists
     * 2. Clear current message list (prevent flash of old messages)
     * 3. Update active conversation ID
     * 4. Save conversation ID to preferences (for restoration)
     * 5. Load messages for new conversation
     * 6. Dismiss history overlay
     * 7. Log conversation switch
     *
     * Phase 5 (P5T03): Now saves active conversation ID to preferences
     *
     * Error handling:
     * - Conversation not found: Sets error state, stays on current conversation
     * - Repository failure: Sets error state, logs exception
     *
     * @param conversationId ID of conversation to switch to
     */
    fun switchConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                uiStateManager.setLoading(true)
                uiStateManager.clearError()

                Log.d(TAG, "Switching to conversation: $conversationId")

                // 1. Validate conversation exists
                when (val result = conversationRepository.getConversationById(conversationId)) {
                    is Result.Success -> {
                        val conversation = result.data
                        Log.d(TAG, "Found conversation: ${conversation.title}")

                        // 2. Clear current messages (prevent flash of old messages)
                        uiStateManager.clearMessages()

                        // 3. Update active conversation ID
                        uiStateManager.setActiveConversationId(conversationId)

                        // 4. Save to preferences for restoration (Phase 5 - P5T03)
                        chatPreferences.setLastActiveConversationId(conversationId)

                        // 5. Load messages for new conversation
                        observeMessages(conversationId)

                        // 6. Dismiss history overlay
                        dismissHistory()

                        // 7. Log conversation switch
                        Log.i(TAG, "Successfully switched to conversation: ${conversation.title} (ID: $conversationId)")
                    }
                    is Result.Error -> {
                        uiStateManager.setError("Conversation not found: ${result.message}")
                        Log.e(TAG, "Failed to get conversation: ${result.message}", result.exception)
                    }
                }

            } catch (e: Exception) {
                uiStateManager.setError("Failed to switch conversation: ${e.message}")
                Log.e(TAG, "Exception in switchConversation", e)
            } finally {
                uiStateManager.setLoading(false)
            }
        }
    }

    /**
     * Creates a new conversation and switches to it (Phase 4, Task P4T03).
     * Called when user taps "New Conversation" button in history overlay.
     *
     * Phase 5 (P5T04): Invalidates conversations cache after successful creation.
     *
     * Flow:
     * 1. Generate new conversation ID
     * 2. Create conversation in repository
     * 3. Switch to new conversation
     * 4. Dismiss history overlay
     * 5. Invalidate conversations cache to ensure list is refreshed
     * 6. Clear input field (handled by UI)
     *
     * Error handling:
     * - Repository failure: Sets error state, stays on current conversation
     *
     * @param title Initial conversation title (default: "New Conversation")
     */
    fun createNewConversation(title: String = "New Conversation") {
        viewModelScope.launch {
            try {
                uiStateManager.setLoading(true)
                uiStateManager.clearError()

                Log.d(TAG, "Creating new conversation: $title")

                when (val result = conversationRepository.createConversation(title)) {
                    is Result.Success -> {
                        val conversation = result.data
                        Log.d(TAG, "Created conversation: ${conversation.title} (ID: ${conversation.id})")

                        // Invalidate conversations cache (Phase 5 - P5T04)
                        conversationsCacheTimestamp = 0L
                        Log.d(TAG, "Invalidated conversations cache after creating new conversation")

                        // Switch to new conversation (this also dismisses history overlay)
                        switchConversation(conversation.id)

                        Log.i(TAG, "Successfully created and switched to new conversation")
                    }
                    is Result.Error -> {
                        uiStateManager.setError("Failed to create conversation: ${result.message}")
                        Log.e(TAG, "Failed to create conversation", result.exception)
                    }
                }

            } catch (e: Exception) {
                uiStateManager.setError("Failed to create conversation: ${e.message}")
                Log.e(TAG, "Exception in createNewConversation", e)
            } finally {
                uiStateManager.setLoading(false)
            }
        }
    }

    /**
     * Loads the next page of messages for the active conversation (Phase 5, Task P5T04).
     * Called when user taps "Load More" button at top of message list.
     *
     * Flow:
     * 1. Check if more messages are available and not already loading
     * 2. Fetch next page using current offset
     * 3. Prepend new messages to existing list (older messages go at top)
     * 4. Update offset and hasMore flag
     * 5. Log performance metrics
     *
     * Performance:
     * - Typical load time: <200ms for 50 messages
     * - Memory impact: +25MB per 50 messages loaded
     *
     * Error handling:
     * - No more messages: Method returns early, no error shown
     * - Already loading: Method returns early to prevent duplicate requests
     * - Repository failure: Sets error state, logs exception
     */
    fun loadMoreMessages() {
        // Early return if no more messages or already loading
        if (!hasMoreMessages.value || isLoading.value) {
            Log.d(TAG, "loadMoreMessages: skipped (hasMore=${hasMoreMessages.value}, isLoading=${isLoading.value})")
            return
        }

        val conversationId = activeConversationId.value
        if (conversationId == null) {
            Log.w(TAG, "loadMoreMessages: No active conversation")
            return
        }

        viewModelScope.launch {
            try {
                uiStateManager.setLoading(true)
                uiStateManager.clearError()

                val startTime = System.currentTimeMillis()
                val currentOffset = messageOffset.value
                val currentTotalCount = totalMessageCount.value
                Log.d(TAG, "Loading more messages: offset=$currentOffset, limit=$messagePageSize")

                when (val result = messageRepository.getMessagesPaginated(
                    conversationId = conversationId,
                    limit = messagePageSize,
                    offset = currentOffset
                )) {
                        is Result.Success -> {
                            val newMessages = result.data
                            if (newMessages.isNotEmpty()) {
                                // Prepend older messages to the beginning of the list
                                val currentMessages = messages.value
                                uiStateManager.setMessages(newMessages + currentMessages)

                                // Edge case: Prevent offset from exceeding total count
                                val newOffset = currentOffset + newMessages.size
                                val clampedOffset = minOf(newOffset, currentTotalCount)

                                // Update hasMore flag
                                val hasMore = newMessages.size >= messagePageSize && clampedOffset < currentTotalCount
                                uiStateManager.updatePagination(clampedOffset, hasMore, currentTotalCount)

                                // Edge case logging: Detect offset overflow
                                if (newOffset > currentTotalCount) {
                                    Log.w(TAG, "Offset overflow detected: calculated=$newOffset, total=$currentTotalCount, clamped to $clampedOffset")
                                }

                                val loadTime = System.currentTimeMillis() - startTime
                                Log.i(TAG, "=== Load More Messages Performance (Phase 5 - P5T04) ===")
                                Log.i(TAG, "  Loaded: ${newMessages.size} messages")
                                Log.i(TAG, "  Total loaded: $clampedOffset/$currentTotalCount messages")
                                Log.i(TAG, "  Load time: ${loadTime}ms")
                                Log.i(TAG, "  Has more: $hasMore")
                            } else {
                                // No more messages available
                                uiStateManager.updatePagination(currentOffset, false, currentTotalCount)
                                Log.d(TAG, "No more messages to load")
                            }
                    }
                    is Result.Error -> {
                        uiStateManager.setError("Failed to load more messages: ${result.message}")
                        Log.e(TAG, "Failed to load more paginated messages", result.exception)
                    }
                }

            } catch (e: Exception) {
                uiStateManager.setError("Failed to load more messages: ${e.message}")
                Log.e(TAG, "Exception in loadMoreMessages", e)
            } finally {
                uiStateManager.setLoading(false)
            }
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        uiStateManager.clearError()
    }

    /**
     * Clears NLU classification cache (Phase 5 - P5T04).
     * Called when user teaches new intents to force re-classification with updated intent list.
     *
     * Use cases:
     * - After user teaches AVA a new intent (handleTeachAva)
     * - When intent model is updated/retrained
     * - When user wants to reset cached classifications
     *
     * Performance: Cache rebuild happens organically as user sends messages.
     */
    fun clearNLUCache() {
        // Delegate to NLUCoordinator (SOLID refactoring C-01)
        nluCoordinator.clearClassificationCache()
        Log.d(TAG, "NLU classification cache cleared via coordinator")
    }

    /**
     * Stops ongoing LLM generation (if any).
     * Cancels the current generation job and clears loading state.
     *
     * Use cases:
     * - User triggers "Stop Generation" voice command
     * - User manually cancels long-running response
     * - Timeout or error requires cancellation
     *
     * Thread-safe: Can be called from any thread.
     * Idempotent: Safe to call multiple times, no-op if no generation active.
     */
    fun stopGeneration() {
        val job = generationJob
        if (job != null && job.isActive) {
            Log.d(TAG, "Stopping ongoing generation...")
            job.cancel()
            generationJob = null
            uiStateManager.setLoading(false)
            Log.i(TAG, "Generation stopped successfully")
        } else {
            Log.d(TAG, "No active generation to stop")
        }
    }

    // ==================== Private Methods ====================

    /**
     * Evaluates if teach mode should be activated based on confidence score.
     * Task P2T06: Auto-prompt on low confidence.
     *
     * Logic:
     * - If NLU is not ready: don't show teach button (user messages go to LLM directly)
     * - If confidence is null (no classification): don't show teach button
     * - If confidence <= threshold (default 0.5): show teach button with unknown template
     * - If confidence > threshold: show normal template based on classified intent
     *
     * Edge cases handled:
     * - NLU not ready: returns false (prevents teach mode during initialization)
     * - confidence = 0.5 (exactly threshold): triggers teach mode (uses <= comparison)
     * - confidence = 0.0: triggers teach mode only if NLU is ready (very low)
     * - confidence = 1.0: normal mode (high confidence)
     *
     * Phase 5 (P5T03): Now uses reactive threshold from ChatPreferences
     * Phase 6 (Fix): Added NLU readiness check to prevent teach mode during initialization
     *
     * @param confidence Confidence score from NLU classifier (0.0 to 1.0), or null if no classification
     * @return true if teach button should be shown, false otherwise
     */
    internal fun shouldShowTeachButton(confidence: Float?): Boolean {
        // Only show teach button if NLU is ready AND confidence is low
        // This prevents teach mode from appearing during NLU initialization (0-25s)
        return isNLUReady.value &&
               confidence != null &&
               confidence <= confidenceThreshold.value
    }

    /**
     * Gets the appropriate response template based on confidence score.
     * Task P2T06: Use "unknown" template for low confidence to trigger user teaching.
     *
     * @param intent Classified intent (may be "unknown" if no match)
     * @param confidence Confidence score from classifier
     * @return Response template string, or null if should use default behavior
     */
    private fun getResponseTemplate(intent: String?, confidence: Float?): String? {
        // If confidence is low, use unknown template to prompt user for teaching
        if (shouldShowTeachButton(confidence)) {
            Log.d(TAG, "Low confidence detected (${confidence}), using unknown template to prompt teaching")
            return "I'm not sure what you meant. Could you help me understand?\n\nTap 'Teach AVA' to show me the correct intent."
        }

        // For higher confidence, return null to use intent-based templates
        // (This will be handled by IntentTemplates in Phase 2)
        return null
    }

    /**
     * Load candidate intents for NLU classification.
     * Delegated to NLUCoordinator (SOLID refactoring).
     *
     * @see NLUCoordinator.loadCandidateIntents
     */
    private fun loadCandidateIntents() {
        viewModelScope.launch {
            nluCoordinator.loadCandidateIntents()
        }
    }

    /**
     * Loads all conversations from repository (Phase 4, Task P4T02).
     * Sorts by updatedAt descending (most recent first).
     * Called when history overlay is shown or conversations are modified.
     *
     * Phase 5 (P5T04): Implements in-memory caching with 5-second TTL.
     * Cache is checked before database fetch to reduce query load on repeated history opens.
     *
     * Flow:
     * 1. Check cache freshness before fetching
     * 2. Fetch all conversations from ConversationRepository (if cache miss)
     * 3. Sort by updatedAt timestamp (most recent first)
     * 4. Update _conversations state and cache timestamp
     * 5. Error handling (set errorMessage if fails)
     *
     * Error handling:
     * - Repository failure: Sets error state, keeps existing conversation list
     * - Empty list: Handles gracefully (shows empty state in UI)
     *
     * Performance: ~10-30ms for typical datasets (10-100 conversations)
     * Cache impact: Reduces database queries by ~80% during typical usage (history shown multiple times)
     */
    private fun loadConversations() {
        viewModelScope.launch {
            try {
                // Check cache freshness (Phase 5 - P5T04)
                val now = System.currentTimeMillis()
                if (conversations.value.isNotEmpty() &&
                    (now - conversationsCacheTimestamp) < conversationsCacheTTL) {
                    val cacheAge = now - conversationsCacheTimestamp
                    Log.d(TAG, "Using cached conversations (age: ${cacheAge}ms, TTL: ${conversationsCacheTTL}ms)")
                    return@launch
                }

                Log.d(TAG, "Loading conversations from repository...")
                val startTime = System.currentTimeMillis()

                // Fetch all conversations with timeout to avoid blocking
                val conversations = withTimeoutOrNull(5000L) {
                    conversationRepository.getAllConversations().first()
                } ?: emptyList()

                // Sort by updatedAt descending (most recent first)
                // Edge case: Handle empty list gracefully
                val sortedConversations = if (conversations.isEmpty()) {
                    Log.d(TAG, "Conversations list is empty - no sorting needed")
                    emptyList()
                } else {
                    conversations.sortedByDescending { it.updatedAt }
                }

                // Update state
                uiStateManager.setConversations(sortedConversations)

                // Update cache timestamp (Phase 5 - P5T04)
                conversationsCacheTimestamp = now

                val loadTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "Loaded ${sortedConversations.size} conversations in ${loadTime}ms")

                if (sortedConversations.isEmpty()) {
                    Log.d(TAG, "No conversations found (empty list)")
                } else {
                    Log.d(TAG, "Most recent conversation: ${sortedConversations.first().title}")
                }

            } catch (e: Exception) {
                uiStateManager.setError("Failed to load conversations: ${e.message}")
                Log.e(TAG, "Exception in loadConversations", e)
            }
        }
    }

    // ==================== Export Functionality (Phase 1.1) ====================

    /**
     * Export a single conversation to file and share
     * Phase 1.1 - Export Conversations
     *
     * @param conversationId Conversation to export
     * @param format Export format (JSON or CSV)
     * @param privacyOptions Privacy controls for export
     */
    fun exportConversation(
        conversationId: String,
        format: ExportFormat = ExportFormat.JSON,
        privacyOptions: PrivacyOptions = PrivacyOptions()
    ) {
        viewModelScope.launch {
            try {
                uiStateManager.setLoading(true)
                uiStateManager.clearError()

                Log.d(TAG, "Exporting conversation: $conversationId (format: $format)")

                when (val result = exportConversationUseCase.exportConversation(
                    conversationId = conversationId,
                    format = format,
                    privacyOptions = privacyOptions
                )) {
                    is Result.Success -> {
                        val exportResult = result.data
                        Log.d(TAG, "Export successful: ${exportResult.filename}")

                        // Write to file
                        val file = File(context.cacheDir, exportResult.filename)
                        file.writeText(exportResult.content)

                        // Share file
                        shareFile(file, exportResult.mimeType)

                        Log.i(TAG, "Successfully exported and shared conversation")
                    }
                    is Result.Error -> {
                        uiStateManager.setError("Export failed: ${result.message}")
                        Log.e(TAG, "Failed to export conversation", result.exception)
                    }
                }

            } catch (e: Exception) {
                uiStateManager.setError("Export failed: ${e.message}")
                Log.e(TAG, "Exception in exportConversation", e)
            } finally {
                uiStateManager.setLoading(false)
            }
        }
    }

    /**
     * Export all conversations to file and share
     * Phase 1.1 - Export Conversations
     *
     * @param format Export format (JSON or CSV)
     * @param privacyOptions Privacy controls for export
     */
    fun exportAllConversations(
        format: ExportFormat = ExportFormat.JSON,
        privacyOptions: PrivacyOptions = PrivacyOptions()
    ) {
        viewModelScope.launch {
            try {
                uiStateManager.setLoading(true)
                uiStateManager.clearError()

                Log.d(TAG, "Exporting all conversations (format: $format)")

                when (val result = exportConversationUseCase.exportAllConversations(
                    format = format,
                    privacyOptions = privacyOptions
                )) {
                    is Result.Success -> {
                        val exportResult = result.data
                        Log.d(TAG, "Export successful: ${exportResult.filename}")

                        // Write to file
                        val file = File(context.cacheDir, exportResult.filename)
                        file.writeText(exportResult.content)

                        // Share file
                        shareFile(file, exportResult.mimeType)

                        Log.i(TAG, "Successfully exported and shared all conversations")
                    }
                    is Result.Error -> {
                        uiStateManager.setError("Export failed: ${result.message}")
                        Log.e(TAG, "Failed to export all conversations", result.exception)
                    }
                }

            } catch (e: Exception) {
                uiStateManager.setError("Export failed: ${e.message}")
                Log.e(TAG, "Exception in exportAllConversations", e)
            } finally {
                uiStateManager.setLoading(false)
            }
        }
    }

    /**
     * Share file using Android share intent
     * Privacy-first: Uses FileProvider for secure file sharing
     *
     * @param file File to share
     * @param mimeType MIME type of file
     */
    private fun shareFile(file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AVA Conversation Export")
                putExtra(Intent.EXTRA_TEXT, "Exported conversation data from AVA AI")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share conversation export")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Log.d(TAG, "Share intent launched successfully")
        } catch (e: Exception) {
            uiStateManager.setError("Failed to share file: ${e.message}")
            Log.e(TAG, "Failed to share file", e)
        }
    }

    /**
     * Delete a conversation and all its messages
     * Phase 1.1 - Conversation Management
     *
     * @param conversationId Conversation to delete
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                uiStateManager.setLoading(true)
                uiStateManager.clearError()

                Log.d(TAG, "Deleting conversation: $conversationId")

                when (val result = conversationRepository.deleteConversation(conversationId)) {
                    is Result.Success -> {
                        Log.i(TAG, "Conversation deleted successfully")

                        // Invalidate conversations cache
                        conversationsCacheTimestamp = 0L
                        loadConversations()

                        // If deleted conversation was active, switch to most recent
                        if (activeConversationId.value == conversationId) {
                            val conversations = conversations.value
                            if (conversations.isNotEmpty()) {
                                val mostRecent = conversations.first()
                                switchConversation(mostRecent.id)
                            } else {
                                // No conversations left, create new one
                                createNewConversation()
                            }
                        }
                    }
                    is Result.Error -> {
                        uiStateManager.setError("Failed to delete conversation: ${result.message}")
                        Log.e(TAG, "Failed to delete conversation", result.exception)
                    }
                }

            } catch (e: Exception) {
                uiStateManager.setError("Failed to delete conversation: ${e.message}")
                Log.e(TAG, "Exception in deleteConversation", e)
            } finally {
                uiStateManager.setLoading(false)
            }
        }
    }

    // ==================== RAG Control Methods (RAG Phase 2, Task 2) ====================

    /**
     * Enables or disables RAG-enhanced responses.
     *
     * When enabled, AVA will retrieve relevant context from selected documents
     * before generating responses.
     *
     * @param enabled True to enable RAG, false to disable
     */
    fun setRAGEnabled(enabled: Boolean) {
        chatPreferences.setRagEnabled(enabled)
        Log.d(TAG, "RAG ${if (enabled) "enabled" else "disabled"}")

        if (!enabled) {
            // Clear source citations when disabling RAG
            _recentSourceCitations.value = emptyList()
        }
    }

    /**
     * Sets the list of document IDs to use for RAG retrieval.
     *
     * Only chunks from these documents will be considered when retrieving context.
     * Empty list disables RAG retrieval.
     *
     * @param documentIds List of document IDs to search
     */
    fun setSelectedDocuments(documentIds: List<String>) {
        chatPreferences.setSelectedDocumentIds(documentIds)
        chatPreferences.setRagEnabled(documentIds.isNotEmpty())
        Log.d(TAG, "Selected ${documentIds.size} documents for RAG")

        if (documentIds.isEmpty()) {
            _recentSourceCitations.value = emptyList()
        }
    }

    /**
     * Sets the similarity threshold for RAG retrieval.
     *
     * Only chunks with similarity >= threshold will be retrieved.
     * Higher threshold = more strict (only very relevant chunks)
     * Lower threshold = more lenient (more chunks, possibly less relevant)
     *
     * @param threshold Similarity threshold (0.0 to 1.0)
     */
    fun setRAGThreshold(threshold: Float) {
        require(threshold in 0f..1f) { "Threshold must be between 0.0 and 1.0" }
        chatPreferences.setRagThreshold(threshold)
        Log.d(TAG, "RAG similarity threshold set to: $threshold")
    }

    /**
     * Gets the current RAG-enabled state (Phase 2 Task 1 - Settings Foundation).
     * Note: Full RAG integration (Task 2) is pending.
     *
     * @return True if RAG is enabled and documents are selected
     */
    fun isRAGActive(): Boolean {
        return ragEnabled.value && selectedDocumentIds.value.isNotEmpty()
    }

    // ==================== TTS Functionality (Phase 1.2) ====================

    /**
     * Speak a message using TTS.
     * Uses TTSCoordinator for SOLID single-responsibility.
     *
     * @param text Text to speak
     * @param messageId Optional message ID for UI tracking
     */
    fun speakMessage(text: String, messageId: String? = null) {
        when (val result = ttsCoordinator.speak(text = text, messageId = messageId)) {
            is Result.Success -> {
                Log.d(TAG, "TTS speak started successfully for message: $messageId")
            }
            is Result.Error -> {
                Log.e(TAG, "TTS speak failed: ${result.message}")
                uiStateManager.setError("Speech failed: ${result.message}")
            }
        }
    }

    /**
     * Stop current TTS speech.
     */
    fun stopSpeaking() {
        ttsCoordinator.stop()
        Log.d(TAG, "TTS speech stopped")
    }

    /**
     * Toggle TTS enabled state.
     */
    fun toggleTTS() {
        ttsCoordinator.toggleEnabled()
    }

    /**
     * Toggle TTS auto-speak state.
     */
    fun toggleAutoSpeak() {
        ttsCoordinator.toggleAutoSpeak()
    }
}
