package com.augmentalis.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.prefs.ChatPreferences
import com.augmentalis.ava.core.domain.model.Conversation
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.core.domain.model.MessageRole
import com.augmentalis.ava.core.domain.repository.MessageRepository
import com.augmentalis.chat.coordinator.ActionCoordinator
import com.augmentalis.chat.coordinator.ConversationManager
import com.augmentalis.chat.coordinator.ExportCoordinator
import com.augmentalis.chat.coordinator.ExportFormat
import com.augmentalis.chat.coordinator.IActionCoordinator
import com.augmentalis.chat.coordinator.NLUCoordinator
import com.augmentalis.chat.coordinator.PrivacyOptions
import com.augmentalis.chat.coordinator.RAGCoordinator
import com.augmentalis.chat.coordinator.ResponseCoordinator
import com.augmentalis.chat.coordinator.TTSCoordinator
import com.augmentalis.chat.coordinator.TeachingFlowManager
import com.augmentalis.chat.data.BuiltInIntents
import com.augmentalis.chat.event.WakeWordEventBus
import com.augmentalis.nlu.IntentClassification
import com.augmentalis.actions.ActionsManager
import com.augmentalis.llm.response.ResponseGenerator
import com.augmentalis.llm.response.ResponseContext
import com.augmentalis.rag.domain.RAGRepository
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.augmentalis.ava.core.data.util.AvidHelper
import javax.inject.Inject

/**
 * ViewModel for ChatScreen - Orchestrator for chat functionality.
 *
 * SOLID Refactoring: This ViewModel now acts as a thin orchestrator that
 * delegates to specialized coordinators:
 * - NLUCoordinator: NLU classification and intent management
 * - ResponseCoordinator: Response generation (LLM + templates)
 * - RAGCoordinator: Document retrieval for context
 * - ActionCoordinator: Intent action execution
 * - TTSCoordinator: Text-to-speech functionality
 * - ExportCoordinator: Conversation export and sharing
 * - TeachingFlowManager: Teach-AVA flow and training
 * - ConversationManager: Conversation lifecycle management
 *
 * Responsibilities:
 * - Orchestrate message flow between coordinators
 * - Expose UI state from coordinators
 * - Handle user input and dispatch to appropriate coordinator
 *
 * @author Manoj Jhawar / Claude AI
 * @since 2025-01-15 (SOLID refactoring)
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    // Message repository for direct message operations
    private val messageRepository: MessageRepository,
    private val chatPreferences: ChatPreferences,
    private val actionsManager: ActionsManager,
    private val responseGenerator: ResponseGenerator,
    private val ragRepository: RAGRepository?,
    // SOLID Coordinators
    private val nluCoordinator: NLUCoordinator,
    private val responseCoordinator: ResponseCoordinator,
    private val ragCoordinator: RAGCoordinator,
    private val actionCoordinator: ActionCoordinator,
    private val ttsCoordinator: TTSCoordinator,
    private val exportCoordinator: ExportCoordinator,
    private val teachingFlowManager: TeachingFlowManager,
    private val conversationManager: ConversationManager,
    // Event buses and state managers
    private val wakeWordEventBus: WakeWordEventBus,
    private val uiStateManager: com.augmentalis.chat.state.ChatUIStateManager,
    private val statusIndicatorState: com.augmentalis.chat.state.StatusIndicatorState,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    // ==================== Thresholds (from preferences) ====================

    private val confidenceThreshold: StateFlow<Float> = chatPreferences.confidenceThreshold
    private val teachThreshold: StateFlow<Float> = chatPreferences.teachThreshold
    private val llmFallbackThreshold: StateFlow<Float> = chatPreferences.llmFallbackThreshold

    // ==================== RAG Settings ====================

    val ragEnabled: StateFlow<Boolean> = chatPreferences.ragEnabled
    val selectedDocumentIds: StateFlow<List<String>> = chatPreferences.selectedDocumentIds
    val ragThreshold: StateFlow<Float> = chatPreferences.ragThreshold

    private val _recentSourceCitations = MutableStateFlow<List<com.augmentalis.chat.domain.SourceCitation>>(emptyList())
    val recentSourceCitations: StateFlow<List<com.augmentalis.chat.domain.SourceCitation>> = _recentSourceCitations.asStateFlow()

    private val ragContextBuilder = com.augmentalis.chat.domain.RAGContextBuilder()

    // ==================== NLU State (delegated to NLUCoordinator) ====================

    val isNLUReady: StateFlow<Boolean> = statusIndicatorState.isNLUReady
    val isNLULoaded: StateFlow<Boolean> = statusIndicatorState.isNLULoaded
    val candidateIntents: StateFlow<List<String>> = nluCoordinator.candidateIntents

    // ==================== Status Indicator State ====================

    val isLLMLoaded: StateFlow<Boolean> = statusIndicatorState.isLLMLoaded
    val lastResponder: StateFlow<String?> = statusIndicatorState.lastResponder
    val lastResponderTimestamp: StateFlow<Long> = statusIndicatorState.lastResponderTimestamp
    val llmFallbackInvoked: StateFlow<Boolean> = statusIndicatorState.llmFallbackInvoked

    // ==================== Conversation State (delegated to ConversationManager) ====================

    val activeConversationId: StateFlow<String?> = conversationManager.activeConversationId
    val messages: StateFlow<List<Message>> = conversationManager.messages
    val conversations: StateFlow<List<Conversation>> = conversationManager.conversations
    val showHistoryOverlay: StateFlow<Boolean> = conversationManager.showHistoryOverlay
    val messageOffset: StateFlow<Int> = conversationManager.messageOffset
    val hasMoreMessages: StateFlow<Boolean> = conversationManager.hasMoreMessages
    val totalMessageCount: StateFlow<Int> = conversationManager.totalMessageCount

    // ==================== Teaching State (delegated to TeachingFlowManager) ====================

    val teachAvaModeMessageId: StateFlow<String?> = teachingFlowManager.teachAvaModeMessageId
    val showTeachBottomSheet: StateFlow<Boolean> = teachingFlowManager.showTeachBottomSheet
    val currentTeachMessageId: StateFlow<String?> = teachingFlowManager.currentTeachMessageId
    val confidenceLearningDialogState: StateFlow<com.augmentalis.chat.coordinator.ConfidenceLearningState?> =
        teachingFlowManager.confidenceLearningDialogState

    // ==================== UI State (delegated to ChatUIStateManager) ====================

    val isLoading: StateFlow<Boolean> = uiStateManager.isLoading
    val errorMessage: StateFlow<String?> = uiStateManager.errorMessage
    val showAccessibilityPrompt: StateFlow<Boolean> = uiStateManager.showAccessibilityPrompt
    val showAppPreferenceSheet: StateFlow<Boolean> = uiStateManager.showAppPreferenceSheet
    val resolutionCapability: StateFlow<String?> = uiStateManager.resolutionCapability
    val wakeWordDetected: StateFlow<String?> = uiStateManager.wakeWordDetected

    // ==================== Developer Settings ====================

    val isFlashModeEnabled: StateFlow<Boolean> = com.augmentalis.ava.core.data.prefs.DeveloperPreferences(context)
        .isFlashModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // ==================== TTS State (delegated to TTSCoordinator) ====================

    val isTTSReady: StateFlow<Boolean> = ttsCoordinator.isTTSReady
    val isTTSSpeaking: StateFlow<Boolean> = ttsCoordinator.isTTSSpeaking
    val speakingMessageId: StateFlow<String?> = ttsCoordinator.speakingMessageId
    val ttsSettings: StateFlow<com.augmentalis.chat.tts.TTSSettings> = ttsCoordinator.ttsSettings

    // ==================== Export State (delegated to ExportCoordinator) ====================

    val isExporting: StateFlow<Boolean> = exportCoordinator.isExporting

    // ==================== Generation Control ====================

    @Volatile
    private var generationJob: kotlinx.coroutines.Job? = null

    // ==================== Initialization ====================

    init {
        initializeNLU()
        initializeConversation()
        initializeActions()
        initializeLLM()
        initializeRAG()
        observeWakeWordEvents()
    }

    private fun observeWakeWordEvents() {
        viewModelScope.launch {
            wakeWordEventBus.events.collect { event ->
                Log.d(TAG, "Wake word event received: ${event.keyword} at ${event.timestamp}")
                onWakeWordDetected(event.keyword)
            }
        }
    }

    private fun onWakeWordDetected(keyword: String) {
        Log.i(TAG, "Wake word detected: $keyword - ready for voice input")
        uiStateManager.setWakeWordDetected(keyword)
    }

    fun clearWakeWordDetected() {
        uiStateManager.clearWakeWordDetected()
    }

    private fun initializeNLU() {
        viewModelScope.launch {
            Log.d(TAG, "Initializing NLU via NLUCoordinator...")
            when (val result = nluCoordinator.initialize()) {
                is Result.Success -> {
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

            launch {
                nluCoordinator.isNLUReady.collect { ready ->
                    statusIndicatorState.setNLUReady(ready)
                }
            }

            statusIndicatorState.initFlashMode(viewModelScope)
        }
    }

    private fun initializeConversation() {
        viewModelScope.launch {
            Log.d(TAG, "Initializing conversation via ConversationManager...")
            when (val result = conversationManager.initialize()) {
                is Result.Success -> {
                    Log.i(TAG, "Conversation initialized successfully")
                    showWelcomeMessageIfNeeded()
                }
                is Result.Error -> {
                    uiStateManager.setError(result.message)
                    Log.e(TAG, "Conversation initialization failed: ${result.message}", result.exception)
                }
            }
        }
    }

    private suspend fun showWelcomeMessageIfNeeded() {
        val conversationId = activeConversationId.value ?: return
        if (messages.value.isEmpty()) {
            val welcomeContent = if (!isNLUReady.value) {
                "Hello! I'm AVA, your AI assistant. I'm still waking up my voice command system, " +
                "but I can chat with you using my language model. Voice commands will be available in a moment..."
            } else {
                "Hello! I'm AVA, your AI assistant. How can I help you today?"
            }

            val welcomeMessage = Message(
                id = AvidHelper.randomMessageAVID(),
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = welcomeContent,
                timestamp = System.currentTimeMillis(),
                intent = null,
                confidence = null
            )

            when (messageRepository.addMessage(welcomeMessage)) {
                is Result.Success -> Log.d(TAG, "Welcome message shown (NLU ready: ${isNLUReady.value})")
                is Result.Error -> Log.w(TAG, "Failed to show welcome message")
            }
        }
    }

    private fun initializeActions() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Initializing action handlers...")
                actionsManager.initialize()
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
            }
        }
    }

    private fun initializeLLM() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Initializing LLM for response generation...")
                if (responseGenerator is com.augmentalis.llm.response.HybridResponseGenerator) {
                    val modelDiscovery = com.augmentalis.llm.alc.loader.ModelDiscovery(context)
                    val selectedModelId = chatPreferences.getSelectedLLMModel()
                    val discoveredModel = if (selectedModelId != null) {
                        modelDiscovery.getModelById(selectedModelId)
                    } else {
                        modelDiscovery.getFirstAvailableModel()
                    }

                    if (discoveredModel == null) {
                        Log.w(TAG, "No LLM models found on device - LLM responses disabled")
                        return@launch
                    }

                    Log.i(TAG, "Discovered LLM model: ${discoveredModel.id} (${discoveredModel.getDisplaySize()})")

                    val modelExtension = when (discoveredModel.format) {
                        com.augmentalis.llm.alc.loader.ModelDiscovery.ModelFormat.MLC -> ".adm"
                        com.augmentalis.llm.alc.loader.ModelDiscovery.ModelFormat.GGUF -> ".adg"
                        com.augmentalis.llm.alc.loader.ModelDiscovery.ModelFormat.LITERT -> ".adr"
                    }
                    val llmConfig = com.augmentalis.llm.domain.LLMConfig(
                        modelPath = discoveredModel.path,
                        modelLib = "${discoveredModel.id}$modelExtension",
                        device = "cpu",
                        maxMemoryMB = if (discoveredModel.sizeGB > 2.0f) 3072 else 1536
                    )

                    when (val result = responseGenerator.initialize(llmConfig)) {
                        is com.augmentalis.llm.LLMResult.Success -> {
                            Log.i(TAG, "LLM initialized successfully (${discoveredModel.id})")
                            chatPreferences.setSelectedLLMModel(discoveredModel.id)
                        }
                        is com.augmentalis.llm.LLMResult.Error -> {
                            Log.w(TAG, "LLM initialization failed: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception initializing LLM: ${e.message}", e)
            }
        }
    }

    private fun initializeRAG() {
        Log.d(TAG, "RAG settings initialized - enabled: ${ragEnabled.value}, threshold: ${ragThreshold.value}")
    }

    // ==================== Public Methods - Teach AVA (delegated) ====================

    fun activateTeachMode(messageId: String) {
        teachingFlowManager.activateTeachMode(messageId)
    }

    fun deactivateTeachMode() {
        teachingFlowManager.deactivateTeachMode()
    }

    fun dismissTeachBottomSheet() {
        teachingFlowManager.dismissTeachBottomSheet()
    }

    fun getCurrentTeachMessage(): Message? {
        val messageId = currentTeachMessageId.value ?: return null
        return messages.value.find { it.id == messageId }
    }

    fun handleTeachAva(messageId: String, intent: String) {
        viewModelScope.launch {
            uiStateManager.setLoading(true)
            uiStateManager.clearError()

            val message = messages.value.find { it.id == messageId }
            if (message == null) {
                uiStateManager.setError("Message not found")
                uiStateManager.setLoading(false)
                return@launch
            }

            when (val result = teachingFlowManager.handleTeachAva(messageId, intent, message.content)) {
                is Result.Success -> {
                    nluCoordinator.invalidateAndReloadIntents()
                    nluCoordinator.clearClassificationCache()
                    uiStateManager.setError("Successfully taught AVA: \"${message.content}\" -> $intent")
                }
                is Result.Error -> {
                    uiStateManager.setError(result.message ?: "Failed to teach AVA")
                }
            }
            uiStateManager.setLoading(false)
        }
    }

    // ==================== Public Methods - Confidence Learning (delegated) ====================

    fun confirmInterpretation() {
        viewModelScope.launch {
            val dialogState = confidenceLearningDialogState.value ?: return@launch
            teachingFlowManager.confirmInterpretation(dialogState.userInput, dialogState.interpretedIntent)
        }
    }

    fun selectAlternateIntent(alternate: com.augmentalis.chat.coordinator.AlternateIntent) {
        viewModelScope.launch {
            val dialogState = confidenceLearningDialogState.value ?: return@launch
            teachingFlowManager.selectAlternateIntent(dialogState.userInput, alternate.intentId)
        }
    }

    fun dismissConfidenceLearningDialog() {
        teachingFlowManager.dismissConfidenceLearningDialog()
    }

    // ==================== Public Methods - History (delegated) ====================

    fun showHistory() {
        conversationManager.showHistory()
        viewModelScope.launch { conversationManager.loadConversations() }
    }

    fun dismissHistory() {
        conversationManager.dismissHistory()
    }

    // ==================== Public Methods - Accessibility ====================

    fun dismissAccessibilityPrompt() {
        uiStateManager.hideAccessibilityPrompt()
    }

    fun dismissAppPreferenceSheet() {
        uiStateManager.hideAppPreferenceSheet()
    }

    fun onAppSelected(capability: String, packageName: String, appName: String, remember: Boolean) {
        viewModelScope.launch {
            try {
                actionsManager.saveAppPreference(capability, packageName, appName, remember)
                dismissAppPreferenceSheet()
            } catch (e: Exception) {
                uiStateManager.setError("Failed to save preference: ${e.message}")
            }
        }
    }

    fun openAccessibilitySettings() {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            uiStateManager.hideAccessibilityPrompt()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings", e)
        }
    }

    // ==================== Public Methods - Message Sending ====================

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val conversationId = activeConversationId.value
        if (conversationId == null) {
            uiStateManager.setError("No active conversation")
            return
        }

        generationJob?.cancel()

        generationJob = viewModelScope.launch {
            try {
                uiStateManager.setLoading(true)
                uiStateManager.clearError()
                statusIndicatorState.resetForNewMessage()

                val totalStartTime = System.currentTimeMillis()

                // Create and save user message
                val userMessage = Message(
                    id = AvidHelper.randomMessageAVID(),
                    conversationId = conversationId,
                    role = MessageRole.USER,
                    content = text.trim(),
                    timestamp = System.currentTimeMillis()
                )

                val userMessageResult = messageRepository.addMessage(userMessage)
                if (userMessageResult is Result.Error) {
                    uiStateManager.setError("Failed to save message: ${userMessageResult.message}")
                    return@launch
                }

                // NLU Classification
                val classification = nluCoordinator.classify(text)
                val classifiedIntent = classification?.intent
                val confidenceScore = classification?.confidence
                val inferenceTimeMs = classification?.inferenceTimeMs ?: 0

                if (classification == null) {
                    statusIndicatorState.markLLMResponded()
                }

                // Handle built-in intents
                val currentThreshold = confidenceThreshold.value
                if (classifiedIntent == BuiltInIntents.SHOW_HISTORY &&
                    confidenceScore != null && confidenceScore > currentThreshold) {
                    showHistory()
                    return@launch
                }

                // Execute action handlers if available
                if (classifiedIntent != null &&
                    confidenceScore != null &&
                    confidenceScore > currentThreshold &&
                    actionCoordinator.hasHandler(classifiedIntent)) {

                    val category = actionCoordinator.getCategoryForIntent(classifiedIntent)
                    val actionResult = actionCoordinator.executeActionWithRouting(
                        intent = classifiedIntent,
                        category = category,
                        utterance = text.trim()
                    )

                    val actionFeedback = when (actionResult) {
                        is IActionCoordinator.ActionExecutionResult.Success -> {
                            if (actionResult.needsAccessibility) uiStateManager.showAccessibilityPrompt()
                            actionResult.message
                        }
                        is IActionCoordinator.ActionExecutionResult.Failure -> actionResult.message
                        is IActionCoordinator.ActionExecutionResult.NeedsResolution -> {
                            uiStateManager.showAppPreferenceSheet(actionResult.capability)
                            actionResult.message
                        }
                        is IActionCoordinator.ActionExecutionResult.NoHandler -> "I don't know how to handle that action yet."
                    }

                    val avaMessage = Message(
                        id = AvidHelper.randomMessageAVID(),
                        conversationId = conversationId,
                        role = MessageRole.ASSISTANT,
                        content = actionFeedback,
                        timestamp = System.currentTimeMillis(),
                        intent = classifiedIntent,
                        confidence = confidenceScore
                    )
                    messageRepository.addMessage(avaMessage)
                    statusIndicatorState.markNLUResponded()
                    return@launch
                }

                // RAG Context Retrieval
                val ragResult = ragCoordinator.retrieveContext(text.trim())
                _recentSourceCitations.value = ragResult.citations

                // Response Generation
                val responseClassification = IntentClassification(
                    intent = classifiedIntent ?: BuiltInIntents.UNKNOWN,
                    confidence = confidenceScore ?: 0.0f,
                    inferenceTimeMs = inferenceTimeMs
                )

                val conversationHistory = when (val historyResult =
                    messageRepository.getRecentMessagesForContext(conversationId, limit = 10)) {
                    is Result.Success -> historyResult.data.map { it.role.name to it.content }
                    is Result.Error -> emptyList()
                }

                val responseContext = ResponseContext(
                    actionResult = null,
                    conversationHistory = conversationHistory
                )

                val responseResult = responseCoordinator.generateResponse(
                    userMessage = text.trim(),
                    classification = responseClassification,
                    context = responseContext,
                    ragContext = ragResult.context,
                    scope = viewModelScope
                )

                statusIndicatorState.setLastResponder(responseResult.respondedBy)
                if (responseResult.wasLLMFallback) {
                    statusIndicatorState.setLLMFallbackInvoked(true)

                    // Trigger confidence learning dialog
                    if (classifiedIntent != null && confidenceScore != null) {
                        val allScores = responseClassification.allScores
                        val alternates = allScores
                            .filter { it.key != classifiedIntent && it.value >= 0.3f }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(3)
                            .map { (intentId, score) ->
                                com.augmentalis.chat.coordinator.AlternateIntent(intentId, intentId, score)
                            }

                        teachingFlowManager.showConfidenceLearningDialog(
                            com.augmentalis.chat.coordinator.ConfidenceLearningState(
                                userInput = text.trim(),
                                interpretedIntent = classifiedIntent,
                                confidence = confidenceScore,
                                alternateIntents = alternates
                            )
                        )
                    }
                }

                // Save AVA response
                val avaMessage = Message(
                    id = AvidHelper.randomMessageAVID(),
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = responseResult.content,
                    timestamp = System.currentTimeMillis(),
                    intent = classifiedIntent,
                    confidence = confidenceScore
                )
                messageRepository.addMessage(avaMessage)

                // Auto-speak if enabled
                val currentTTSSettings = ttsSettings.value
                if (currentTTSSettings.enabled && currentTTSSettings.autoSpeak) {
                    speakMessage(responseResult.content)
                }

                // Activate teach mode if low confidence
                if (teachingFlowManager.shouldShowTeachButton(confidenceScore, isNLUReady.value)) {
                    teachingFlowManager.activateTeachMode(avaMessage.id)
                }

                Log.i(TAG, "Message send completed in ${System.currentTimeMillis() - totalStartTime}ms")

            } catch (e: Exception) {
                uiStateManager.setError("Failed to send message: ${e.message}")
                Log.e(TAG, "Exception in sendMessage", e)
            } finally {
                uiStateManager.setLoading(false)
                generationJob = null
            }
        }
    }

    // ==================== Public Methods - Conversation (delegated) ====================

    fun switchConversation(conversationId: String) {
        viewModelScope.launch {
            uiStateManager.setLoading(true)
            conversationManager.switchConversation(conversationId)
            uiStateManager.setLoading(false)
        }
    }

    fun createNewConversation(title: String = "New Conversation") {
        viewModelScope.launch {
            uiStateManager.setLoading(true)
            conversationManager.createNewConversation(title)
            uiStateManager.setLoading(false)
        }
    }

    fun loadMoreMessages() {
        viewModelScope.launch {
            conversationManager.loadMoreMessages()
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            uiStateManager.setLoading(true)
            conversationManager.deleteConversation(conversationId)
            uiStateManager.setLoading(false)
        }
    }

    // ==================== Public Methods - Export (delegated) ====================

    fun exportConversation(
        conversationId: String,
        format: ExportFormat = ExportFormat.JSON,
        privacyOptions: PrivacyOptions = PrivacyOptions()
    ) {
        viewModelScope.launch {
            uiStateManager.setLoading(true)
            when (val result = exportCoordinator.exportConversation(conversationId, format, privacyOptions)) {
                is Result.Success -> exportCoordinator.shareExport(result.data)
                is Result.Error -> uiStateManager.setError(result.message ?: "Export failed")
            }
            uiStateManager.setLoading(false)
        }
    }

    fun exportAllConversations(
        format: ExportFormat = ExportFormat.JSON,
        privacyOptions: PrivacyOptions = PrivacyOptions()
    ) {
        viewModelScope.launch {
            uiStateManager.setLoading(true)
            when (val result = exportCoordinator.exportAllConversations(format, privacyOptions)) {
                is Result.Success -> exportCoordinator.shareExport(result.data)
                is Result.Error -> uiStateManager.setError(result.message ?: "Export failed")
            }
            uiStateManager.setLoading(false)
        }
    }

    // ==================== Public Methods - Utility ====================

    fun clearError() {
        uiStateManager.clearError()
    }

    fun clearNLUCache() {
        nluCoordinator.clearClassificationCache()
    }

    fun stopGeneration() {
        val job = generationJob
        if (job != null && job.isActive) {
            job.cancel()
            generationJob = null
            uiStateManager.setLoading(false)
            Log.i(TAG, "Generation stopped successfully")
        }
    }

    // ==================== Public Methods - RAG ====================

    fun setRAGEnabled(enabled: Boolean) {
        chatPreferences.setRagEnabled(enabled)
        if (!enabled) _recentSourceCitations.value = emptyList()
    }

    fun setSelectedDocuments(documentIds: List<String>) {
        chatPreferences.setSelectedDocumentIds(documentIds)
        chatPreferences.setRagEnabled(documentIds.isNotEmpty())
        if (documentIds.isEmpty()) _recentSourceCitations.value = emptyList()
    }

    fun setRAGThreshold(threshold: Float) {
        require(threshold in 0f..1f) { "Threshold must be between 0.0 and 1.0" }
        chatPreferences.setRagThreshold(threshold)
    }

    fun isRAGActive(): Boolean = ragEnabled.value && selectedDocumentIds.value.isNotEmpty()

    // ==================== Public Methods - TTS (delegated) ====================

    fun speakMessage(text: String, messageId: String? = null) {
        when (val result = ttsCoordinator.speak(text = text, messageId = messageId)) {
            is Result.Success -> Log.d(TAG, "TTS speak started for message: $messageId")
            is Result.Error -> uiStateManager.setError("Speech failed: ${result.message}")
        }
    }

    fun stopSpeaking() {
        ttsCoordinator.stop()
    }

    fun toggleTTS() {
        ttsCoordinator.toggleEnabled()
    }

    fun toggleAutoSpeak() {
        ttsCoordinator.toggleAutoSpeak()
    }
}
