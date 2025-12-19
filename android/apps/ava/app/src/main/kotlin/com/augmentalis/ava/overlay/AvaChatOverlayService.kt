// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/overlay/AvaChatOverlayService.kt
// created: 2025-11-08
// author: AVA Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.augmentalis.ava.R
import androidx.compose.ui.res.stringResource
import com.augmentalis.chat.ChatViewModel
import com.augmentalis.overlay.controller.VoiceRecognizer
import com.augmentalis.ava.ui.theme.AvaTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for accessing ChatViewModel dependencies from Service context
 *
 * Services cannot use @HiltViewModel directly, so we use @EntryPoint
 * to get access to the dependencies and construct ChatViewModel manually.
 *
 * Phase 6: Hilt DI Migration - OverlayService @EntryPoint Pattern
 *
 * Note: We inject the individual dependencies rather than ChatViewModel directly
 * because Hilt ViewModels must be created via ViewModelProvider, not direct injection.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatViewModelDependenciesEntryPoint {
    fun conversationRepository(): com.augmentalis.ava.core.domain.repository.ConversationRepository
    fun messageRepository(): com.augmentalis.ava.core.domain.repository.MessageRepository
    fun trainExampleRepository(): com.augmentalis.ava.core.domain.repository.TrainExampleRepository
    fun chatPreferences(): com.augmentalis.ava.core.data.prefs.ChatPreferences
    fun intentClassifier(): com.augmentalis.nlu.IntentClassifier
    fun modelManager(): com.augmentalis.nlu.ModelManager
    fun actionsManager(): com.augmentalis.actions.ActionsManager
    fun responseGenerator(): com.augmentalis.llm.response.ResponseGenerator
    // Issue 5.3: IntentLearningManager removed - use nluSelfLearner below
    fun exportConversationUseCase(): com.augmentalis.ava.core.domain.usecase.ExportConversationUseCase
    fun ragRepository(): com.augmentalis.rag.domain.RAGRepository?
    // ADR-013: Self-Learning NLU dependencies
    fun nluSelfLearner(): com.augmentalis.nlu.NLUSelfLearner
    fun inferenceManager(): com.augmentalis.llm.inference.InferenceManager
    // P0: SOLID Coordinators for single-responsibility decomposition
    fun nluCoordinator(): com.augmentalis.chat.coordinator.NLUCoordinator
    fun responseCoordinator(): com.augmentalis.chat.coordinator.ResponseCoordinator
    fun ragCoordinator(): com.augmentalis.chat.coordinator.RAGCoordinator
    fun actionCoordinator(): com.augmentalis.chat.coordinator.ActionCoordinator
    fun ttsCoordinator(): com.augmentalis.chat.coordinator.TTSCoordinator
    // P0: SOLID State Managers for single-responsibility decomposition
    fun uiStateManager(): com.augmentalis.chat.state.ChatUIStateManager
    fun statusIndicatorState(): com.augmentalis.chat.state.StatusIndicatorState
    // P1: WakeWordEventBus to remove reflection
    fun wakeWordEventBus(): com.augmentalis.chat.event.WakeWordEventBus
}

/**
 * Foreground service that provides an always-on AVA chat overlay
 *
 * UX Design (2024-2025 Best Practices):
 * - Progressive disclosure: Settings hidden in dropdown menu
 * - Minimal controls: Only 1 button visible (⋮ menu)
 * - Tap header to minimize (no separate close button)
 * - 92% opacity default for better see-through
 * - Follows Messenger chat heads pattern
 *
 * Features:
 * - Floating bubble trigger (bottom-right standard position)
 * - See-through overlay - user can view background app
 * - Settings menu: Voice input, Clear chat, Opacity slider
 * - Works system-wide with SYSTEM_ALERT_WINDOW permission
 * - Cognitive load reduction: Settings appear only when needed
 */
@AndroidEntryPoint
class AvaChatOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var bubbleView: ComposeView? = null
    private var chatOverlayView: ComposeView? = null

    // Issue I-07 Fix: Changed from simple var to StateFlow for thread safety
    // The previous `var isChatVisible` could cause race conditions when
    // accessed from multiple coroutines/threads simultaneously.
    private val _isChatVisible = MutableStateFlow(false)
    val isChatVisible = _isChatVisible.asStateFlow()

    // StateFlow-based state for proper Compose reactivity
    private val _opacity = MutableStateFlow(0.92f) // 92% default for better see-through
    private val _textInput = MutableStateFlow("")
    private val _isVoiceInputActive = MutableStateFlow(false)

    // Chat functionality
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var voiceRecognizer: VoiceRecognizer

    override fun onCreate() {
        super.onCreate()
        Timber.d("AvaChatOverlayService created")

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Initialize ChatViewModel with repositories
        initializeChatViewModel()

        // Initialize VoiceRecognizer
        initializeVoiceRecognizer()

        // Start foreground service with notification
        startForegroundService()

        // Show floating bubble
        showFloatingBubble()

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("AvaChatOverlayService onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("AvaChatOverlayService destroyed")

        // Release VoiceRecognizer resources
        if (::voiceRecognizer.isInitialized) {
            voiceRecognizer.release()
        }

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        // Remove all overlays
        bubbleView?.let { windowManager.removeView(it) }
        chatOverlayView?.let { windowManager.removeView(it) }
    }

    /**
     * Start as foreground service (required for Android 8+)
     */
    private fun startForegroundService() {
        val channelId = "ava_overlay_service"
        val channelName = "AVA Overlay Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps AVA accessible from any app"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, com.augmentalis.ava.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setContentTitle("AVA is running")
            .setContentText("Tap the floating bubble to chat")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    /**
     * Show floating bubble that triggers chat overlay
     */
    private fun showFloatingBubble() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 200
        }

        bubbleView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AvaChatOverlayService)
            setViewTreeSavedStateRegistryOwner(this@AvaChatOverlayService)

            setContent {
                AvaTheme {
                    FloatingBubble(
                        onBubbleClick = { toggleChatOverlay() }
                    )
                }
            }
        }

        windowManager.addView(bubbleView, params)
    }

    /**
     * Toggle chat overlay visibility
     * Issue I-07: Uses StateFlow.value for thread-safe access
     */
    private fun toggleChatOverlay() {
        if (_isChatVisible.value) {
            hideChatOverlay()
        } else {
            showChatOverlay()
        }
    }

    /**
     * Show chat overlay
     */
    private fun showChatOverlay() {
        if (chatOverlayView != null) return // Already visible

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        chatOverlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AvaChatOverlayService)
            setViewTreeSavedStateRegistryOwner(this@AvaChatOverlayService)

            setContent {
                AvaTheme {
                    // Collect StateFlow values for proper Compose reactivity
                    val textInput by _textInput.collectAsState()
                    val isVoiceInputActive by _isVoiceInputActive.collectAsState()
                    val opacity by _opacity.collectAsState()

                    ChatOverlayScreen(
                        chatViewModel = chatViewModel,
                        textInput = textInput,
                        onTextInputChange = { _textInput.value = it },
                        isVoiceInputActive = isVoiceInputActive,
                        opacity = opacity,
                        onOpacityChange = { _opacity.value = it },
                        onMinimize = { hideChatOverlay() },
                        onVoiceInput = { handleVoiceInput() },
                        onClearChat = { handleClearChat() },
                        onSendMessage = { handleSendMessage() }
                    )
                }
            }
        }

        windowManager.addView(chatOverlayView, params)
        _isChatVisible.value = true // Issue I-07: Thread-safe StateFlow update
    }

    /**
     * Hide chat overlay
     * Issue I-07: Uses StateFlow.value for thread-safe state update
     */
    private fun hideChatOverlay() {
        chatOverlayView?.let {
            windowManager.removeView(it)
            chatOverlayView = null
            _isChatVisible.value = false // Issue I-07: Thread-safe StateFlow update
        }
    }

    /**
     * Initialize ChatViewModel using Hilt @EntryPoint pattern
     *
     * Phase 6: Hilt DI Migration - OverlayService @EntryPoint Pattern
     *
     * Since Services cannot use @HiltViewModel directly (ViewModels must be created
     * via ViewModelProvider), we use @EntryPoint to get the dependencies and
     * construct ChatViewModel manually.
     *
     * All dependencies are still managed by Hilt - we're just assembling them
     * into the ViewModel instance ourselves since we can't use ViewModelProvider
     * in a Service context.
     */
    private fun initializeChatViewModel() {
        try {
            // Get dependencies from Hilt using EntryPoint pattern
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                ChatViewModelDependenciesEntryPoint::class.java
            )

            // Construct ChatViewModel with Hilt-provided dependencies
            chatViewModel = ChatViewModel(
                conversationRepository = entryPoint.conversationRepository(),
                messageRepository = entryPoint.messageRepository(),
                trainExampleRepository = entryPoint.trainExampleRepository(),
                chatPreferences = entryPoint.chatPreferences(),
                intentClassifier = entryPoint.intentClassifier(),
                modelManager = entryPoint.modelManager(),
                actionsManager = entryPoint.actionsManager(),
                responseGenerator = entryPoint.responseGenerator(),
                // Issue 5.3: IntentLearningManager removed - use nluSelfLearner below
                exportConversationUseCase = entryPoint.exportConversationUseCase(),
                ragRepository = entryPoint.ragRepository(),
                // ADR-013: Self-Learning NLU dependencies
                nluSelfLearner = entryPoint.nluSelfLearner(),
                inferenceManager = entryPoint.inferenceManager(),
                // P0: SOLID Coordinators for single-responsibility decomposition
                nluCoordinator = entryPoint.nluCoordinator(),
                responseCoordinator = entryPoint.responseCoordinator(),
                ragCoordinator = entryPoint.ragCoordinator(),
                actionCoordinator = entryPoint.actionCoordinator(),
                ttsCoordinator = entryPoint.ttsCoordinator(),
                // P0: SOLID State Managers for single-responsibility decomposition
                uiStateManager = entryPoint.uiStateManager(),
                statusIndicatorState = entryPoint.statusIndicatorState(),
                // P1: WakeWordEventBus to remove reflection
                wakeWordEventBus = entryPoint.wakeWordEventBus(),
                context = applicationContext
            )

            Timber.d("ChatViewModel initialized successfully via Hilt @EntryPoint")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize ChatViewModel via Hilt")
        }
    }

    /**
     * Initialize VoiceRecognizer for voice input
     */
    private fun initializeVoiceRecognizer() {
        voiceRecognizer = VoiceRecognizer(
            context = applicationContext,
            onPartialResult = { partial ->
                Timber.d("Voice input partial: $partial")
                _textInput.value = partial
            },
            onFinalResult = { final ->
                Timber.d("Voice input final: $final")
                _textInput.value = final
                _isVoiceInputActive.value = false
                // Auto-send message after voice input
                handleSendMessage()
            },
            onError = { error ->
                Timber.e("Voice input error: $error")
                _isVoiceInputActive.value = false
            }
        )
        Timber.d("VoiceRecognizer initialized successfully")
    }

    /**
     * Handle voice input button click
     */
    private fun handleVoiceInput() {
        if (_isVoiceInputActive.value) {
            // Stop listening
            voiceRecognizer.stopListening()
            _isVoiceInputActive.value = false
            Timber.d("Voice input stopped")
        } else {
            // Start listening
            voiceRecognizer.startListening()
            _isVoiceInputActive.value = true
            Timber.d("Voice input started")
        }
    }

    /**
     * Handle clear chat button click
     */
    private fun handleClearChat() {
        _textInput.value = ""
        Timber.d("Chat input cleared")
    }

    /**
     * Handle send message button click
     */
    private fun handleSendMessage() {
        val message = _textInput.value.trim()
        if (message.isNotEmpty()) {
            Timber.d("Sending message: $message")
            chatViewModel.sendMessage(message)
            _textInput.value = ""
        }
    }

    companion object {
        /**
         * Start the overlay service
         */
        fun start(context: Context) {
            val intent = Intent(context, AvaChatOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop the overlay service
         */
        fun stop(context: Context) {
            val intent = Intent(context, AvaChatOverlayService::class.java)
            context.stopService(intent)
        }
    }
}

/**
 * Floating bubble UI
 */
@Composable
private fun FloatingBubble(
    onBubbleClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onBubbleClick,
        containerColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(56.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Open AVA Chat",
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/**
 * Chat overlay screen - Simplified UX with progressive disclosure
 * Based on 2024-2025 UX research: minimal controls, tap header to minimize
 */
@Composable
private fun ChatOverlayScreen(
    chatViewModel: ChatViewModel,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    isVoiceInputActive: Boolean,
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    onMinimize: () -> Unit,
    onVoiceInput: () -> Unit,
    onClearChat: () -> Unit,
    onSendMessage: () -> Unit
) {
    var showSettingsMenu by remember { mutableStateOf(false) }
    val messages by chatViewModel.messages.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = opacity),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        // Simplified header - tap to minimize, menu for settings
        SimplifiedHeader(
            opacity = opacity,
            showSettingsMenu = showSettingsMenu,
            onHeaderClick = onMinimize,
            onMenuClick = { showSettingsMenu = !showSettingsMenu },
            onDismissMenu = { showSettingsMenu = false },
            onOpacityChange = onOpacityChange,
            onVoiceInput = onVoiceInput,
            onClearChat = onClearChat
        )

        // Chat content (always visible when overlay is shown)
        ChatContent(
            messages = messages,
            textInput = textInput,
            onTextInputChange = onTextInputChange,
            isVoiceInputActive = isVoiceInputActive,
            onSendMessage = onSendMessage
        )
    }
}

/**
 * Simplified header - Progressive disclosure pattern
 * Tap header to minimize, single menu button for settings (reduces cognitive load)
 */
@Composable
private fun SimplifiedHeader(
    opacity: Float,
    showSettingsMenu: Boolean,
    onHeaderClick: () -> Unit,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onVoiceInput: () -> Unit,
    onClearChat: () -> Unit
) {
    Box {
        // Main header row - clickable to minimize
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AVA icon and title (clickable to minimize)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                    onClick = onHeaderClick
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Tap to minimize",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.overlay_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.overlay_minimize_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Single menu button (progressive disclosure)
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.overlay_menu_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Settings dropdown menu (progressive disclosure)
        DropdownMenu(
            expanded = showSettingsMenu,
            onDismissRequest = onDismissMenu,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 8.dp)
        ) {
            // Voice input option
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(stringResource(R.string.overlay_menu_voice_input))
                    }
                },
                onClick = {
                    onVoiceInput()
                    onDismissMenu()
                }
            )

            // Clear chat option
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(stringResource(R.string.overlay_menu_clear_chat))
                    }
                },
                onClick = {
                    onClearChat()
                    onDismissMenu()
                }
            )

            Divider()

            // Opacity slider in menu (progressive disclosure)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.overlay_transparency_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.overlay_transparency_value, ((1 - opacity) * 100).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0.3f..0.95f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Chat content - always visible (simplified UX, no expand/collapse)
 */
@Composable
private fun ChatContent(
    messages: List<com.augmentalis.ava.core.domain.model.Message>,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    isVoiceInputActive: Boolean,
    onSendMessage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp) // Fixed height for chat interface
            .padding(16.dp)
    ) {
        // Chat messages area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = if (messages.isEmpty()) Alignment.Center else Alignment.TopStart
        ) {
            if (messages.isEmpty()) {
                // Empty state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = stringResource(R.string.overlay_notification_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.overlay_welcome_primary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.overlay_welcome_secondary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Messages list (simplified - full implementation would use LazyColumn)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    messages.takeLast(5).forEach { message ->
                        Text(
                            text = "${message.role.name}: ${message.content}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextInputChange,
                placeholder = {
                    Text(if (isVoiceInputActive) "Listening..." else "Ask AVA anything...")
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                enabled = !isVoiceInputActive
            )

            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
