/**
 * VoiceKeyboardService.kt - Main keyboard input method service
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import com.augmentalis.voicekeyboard.ui.KeyboardView
import com.augmentalis.voicekeyboard.di.KeyboardServiceContainer
import com.augmentalis.voicekeyboard.interfaces.*
import com.augmentalis.voicekeyboard.preferences.KeyboardSettingsActivity
import com.augmentalis.voicekeyboard.utils.IMEUtil
import com.augmentalis.voicekeyboard.utils.KeyboardActions
import com.augmentalis.voicekeyboard.utils.KeyboardConstants
import com.augmentalis.voicekeyboard.utils.ModifierKeyState
import kotlinx.coroutines.*

/**
 * Main keyboard service implementing InputMethodService
 * SOLID Principles:
 * - Single Responsibility: Only handles IME lifecycle and coordination
 * - Open/Closed: Extensible through handlers and processors
 * - Dependency Inversion: Depends on abstractions (handlers, preferences)
 */
class VoiceKeyboardService : InputMethodService(), KeyboardActionListener {
    
    companion object {
        private const val TAG = "VoiceKeyboard"
        private const val ONE_FRAME_DELAY = 1000L / 60L
        const val EXTRA_TEXT = "text"
    }
    
    // Service container and interfaces
    private lateinit var serviceContainer: KeyboardServiceContainer
    private lateinit var preferencesManager: KeyboardPreferencesManager
    private lateinit var inputProcessor: InputProcessor
    private lateinit var voiceInputListener: VoiceInputListener
    private lateinit var gestureProcessor: GestureProcessor
    private lateinit var dictationManager: DictationManager
    
    // UI components
    private lateinit var keyboardView: KeyboardView
    private lateinit var inputMethodManager: InputMethodManager
    
    // State management
    private var isAlphabetMode = true
    private var isKeyboardVisible = false
    private var currentKeyboardHeight = 0
    private var currentInputConnection: InputConnection? = null
    private var currentEditorInfo: EditorInfo? = null
    
    // Cursor position tracking
    private var cursorPosition = 0
    private var selectionStart = 0
    private var selectionEnd = 0
    
    // Modifier keys
    private val shiftKeyState = ModifierKeyState(supportsLocked = true)
    private val controlKeyState = ModifierKeyState(supportsLocked = false)
    
    // Coroutine scope and jobs
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var autoCapitalizationJob: Job? = null
    private var suggestionUpdateJob: Job? = null
    
    // Voice command receiver
    private val voiceCommandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handleVoiceCommand(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VoiceKeyboardService created")
        
        // Register with broadcast receiver
        KeyboardBroadcastReceiver.setActiveService(this)
        
        // Initialize service container and get dependencies
        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        serviceContainer = KeyboardServiceContainer(this)
        
        // Get services from container
        preferencesManager = serviceContainer.getPreferencesManager()
        inputProcessor = serviceContainer.getInputProcessor()
        voiceInputListener = serviceContainer.getVoiceInputListener()
        gestureProcessor = serviceContainer.getGestureProcessor()
        dictationManager = serviceContainer.getDictationManager(
            onDictationResult = { text -> handleDictationResult(text) },
            onDictationStateChanged = { isActive -> handleDictationStateChange(isActive) }
        )
        
        // Register voice command receiver
        registerVoiceCommandReceiver()
    }
    
    override fun onCreateInputView(): View {
        Log.d(TAG, "Creating input view")
        
        keyboardView = KeyboardView(this).apply {
            setActionListener(this@VoiceKeyboardService)
            setVoiceInputEnabled(preferencesManager.isVoiceInputEnabled())
            setGestureTypingEnabled(preferencesManager.isGestureTypingEnabled())
            setTheme(preferencesManager.getKeyboardTheme())
            
            // Track keyboard height
            post {
                currentKeyboardHeight = height
                if (currentKeyboardHeight > 0 && isKeyboardVisible) {
                    IMEUtil.sendKeyboardHeightToApp(context, currentKeyboardHeight, false)
                }
            }
        }
        
        return keyboardView
    }
    
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.d(TAG, "Starting input, restarting: $restarting")
        
        currentEditorInfo = attribute
        currentInputConnection = currentInputConnection
        
        // Configure keyboard based on input type
        attribute?.let { configureKeyboardForInputType(it) }
        
        // Update shift state based on input
        updateShiftStateFromInput()
    }
    
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.d(TAG, "Starting input view")
        
        // Update keyboard layout
        keyboardView.updateLayout(getKeyboardLayoutForInputType(info))
        
        // Start voice input if enabled
        if (preferencesManager.isAutoVoiceInputEnabled()) {
            startVoiceInput()
        }
    }
    
    override fun onWindowShown() {
        super.onWindowShown()
        Log.d(TAG, "Keyboard window shown")
        
        // Update visibility state
        isKeyboardVisible = true
        dictationManager.updateKeyboardVisibility(true)
        
        // Send keyboard open status
        IMEUtil.sendKeyboardOpenStatusToApp(this, true)
        
        // Send keyboard height if available
        if (currentKeyboardHeight > 0) {
            IMEUtil.sendKeyboardHeightToApp(this, currentKeyboardHeight, false)
        }
    }
    
    override fun onWindowHidden() {
        super.onWindowHidden()
        Log.d(TAG, "Keyboard window hidden")
        
        // Update visibility state
        isKeyboardVisible = false
        dictationManager.updateKeyboardVisibility(false)
        
        // Send keyboard close status
        IMEUtil.sendKeyboardOpenStatusToApp(this, false)
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        Log.d(TAG, "Finishing input")
        
        // Cancel pending jobs
        autoCapitalizationJob?.cancel()
        suggestionUpdateJob?.cancel()
        
        // Stop voice input if active
        voiceInputListener.stopListening()
        
        // Stop any active dictation
        if (dictationManager.isDictationActive.value) {
            dictationManager.stopDictation()
        }
        
        // Clear state
        currentInputConnection = null
        currentEditorInfo = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Destroying service")
        
        // Clear service reference
        KeyboardBroadcastReceiver.setActiveService(null)
        
        // Cleanup
        unregisterReceiver(voiceCommandReceiver)
        serviceContainer.release()
        
        // Cancel coroutine scope
        coroutineScope.cancel()
    }
    
    // KeyboardActionListener implementation
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        
        // Delegate to input processor for key handling
        inputProcessor.processKeyPress(primaryCode, ic)
        
        // Handle special coordinator actions
        when (primaryCode) {
            KeyboardConstants.KEYCODE_SHIFT -> handleShift()
            KeyboardConstants.KEYCODE_MODE_CHANGE -> handleModeChange()
            KeyboardConstants.KEYCODE_VOICE -> handleVoiceKey()
            KeyboardConstants.KEYCODE_SETTINGS -> showSettings()
            KeyboardConstants.KEYCODE_EMOJI -> showEmojiPicker()
            KeyboardConstants.KEYCODE_DICTATION -> toggleDictation()
        }
    }
    
    override fun onText(text: CharSequence) {
        currentInputConnection?.let { ic ->
            inputProcessor.processText(text.toString(), ic)
        }
    }
    
    override fun swipeLeft() {
        if (preferencesManager.isSwipeEnabled()) {
            handleSwipeAction(SwipeDirection.LEFT)
        }
    }
    
    override fun swipeRight() {
        if (preferencesManager.isSwipeEnabled()) {
            handleSwipeAction(SwipeDirection.RIGHT)
        }
    }
    
    override fun swipeDown() {
        if (preferencesManager.isSwipeEnabled()) {
            hideKeyboard()
        }
    }
    
    override fun swipeUp() {
        if (preferencesManager.isSwipeEnabled()) {
            showCandidatesView()
        }
    }
    
    override fun onLongPress(key: Int) {
        when (key) {
            KeyEvent.KEYCODE_0 -> showSymbolsKeyboard()
            KeyboardActions.KEYCODE_VOICE -> startContinuousVoiceInput()
            else -> showAlternativeCharacters(key)
        }
    }
    
    // Voice input handling
    private fun startVoiceInput() {
        voiceInputListener.startListening { text ->
            currentInputConnection?.commitText(text, 1)
        }
    }
    
    private fun startContinuousVoiceInput() {
        voiceInputListener.startContinuousListening { text ->
            currentInputConnection?.commitText(text, 1)
        }
    }
    
    private fun handleVoiceCommand(intent: Intent) {
        when (intent.action) {
            KeyboardConstants.ACTION_VOICE_KEY_CODE -> {
                val keyCode = intent.getIntExtra("keyCode", 0)
                handleVoiceKeyCode(keyCode)
            }
            KeyboardConstants.ACTION_VOICE_KEY_COMMAND -> {
                val command = intent.getStringExtra(KeyboardConstants.KEY_COMMAND)
                command?.let { handleVoiceCommandString(it) }
            }
            KeyboardConstants.ACTION_CLOSE_COMMAND -> hideKeyboard()
            KeyboardConstants.ACTION_VOICE_SWITCH_KEYBOARD -> switchToNextKeyboard()
            KeyboardConstants.ACTION_VOICE_COMMAND_SHOW_INPUT -> showKeyboard()
            KeyboardConstants.ACTION_FREE_SPEECH_COMMAND -> handleFreeSpeechCommand()
            KeyboardConstants.ACTION_LAUNCH_DICTATION -> toggleDictation()
        }
    }
    
    // Public methods for broadcast receiver
    fun handleVoiceKeyCode(keyCode: Int) {
        Log.d(TAG, "Handling voice key code: $keyCode")
        onKey(keyCode, null)
    }
    
    fun handleVoiceCommandString(command: String) {
        Log.d(TAG, "Handling voice command: $command")
        
        // Check if dictation handler can process
        if (dictationManager.processVoiceCommand(command)) {
            return
        }
        
        // Handle other commands
        when (command.lowercase()) {
            "type", "keyboard" -> {
                if (dictationManager.isDictationActive.value) {
                    dictationManager.stopDictation()
                }
            }
            "delete", "backspace" -> currentInputConnection?.deleteSurroundingText(1, 0)
            "enter", "new line" -> currentInputConnection?.let { handleEnter(it) }
            "space" -> currentInputConnection?.let { handleSpace(it) }
            else -> currentInputConnection?.commitText(command, 1)
        }
    }
    
    fun handleCloseCommand() = hideKeyboard()
    fun handleSwitchKeyboard() = switchToNextKeyboard()
    fun handleShowInput() = showKeyboard()
    fun handleFreeSpeechCommand() {
        Log.d(TAG, "Handling free speech command")
        dictationManager.startDictation()
    }
    fun handleLaunchDictation() = toggleDictation()
    
    // Gesture typing handling
    fun onGestureTypingPath(points: List<Pair<Float, Float>>) {
        gestureProcessor.processGesture(points) { word ->
            currentInputConnection?.commitText(word, 1)
        }
    }
    
    // Input handling methods
    private fun handleBackspace(ic: InputConnection) {
        val selectedText = ic.getSelectedText(0)
        if (selectedText.isNullOrEmpty()) {
            ic.deleteSurroundingText(1, 0)
        } else {
            ic.commitText("", 1)
        }
    }
    
    private fun handleEnter(ic: InputConnection) {
        val imeOptions = currentEditorInfo?.imeOptions ?: 0
        val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
        
        when (actionId) {
            EditorInfo.IME_ACTION_SEARCH -> ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
            EditorInfo.IME_ACTION_GO -> ic.performEditorAction(EditorInfo.IME_ACTION_GO)
            EditorInfo.IME_ACTION_SEND -> ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
            EditorInfo.IME_ACTION_NEXT -> ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
            EditorInfo.IME_ACTION_DONE -> ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
            else -> ic.commitText("\n", 1)
        }
    }
    
    private fun handleSpace(ic: InputConnection) {
        ic.commitText(" ", 1)
        
        // Auto-capitalization after period
        if (preferencesManager.isAutoCapitalizationEnabled()) {
            autoCapitalizeIfNeeded(ic)
        }
    }
    
    private fun handleShift() {
        shiftKeyState.onPress()
        keyboardView.setShifted(shiftKeyState.isActive())
    }
    
    private fun handleModeChange() {
        isAlphabetMode = !isAlphabetMode
        keyboardView.setAlphabetMode(isAlphabetMode)
    }
    
    private fun handleVoiceKey() {
        if (preferencesManager.isVoiceInputEnabled()) {
            // Launch dictation through broadcast
            IMEUtil.launchDictation(this)
        }
    }
    
    private fun toggleDictation() {
        Log.d(TAG, "Toggling dictation")
        if (dictationManager.isDictationActive.value) {
            dictationManager.stopDictation()
        } else {
            dictationManager.startDictation()
        }
    }
    
    private fun handleDictationResult(text: String) {
        Log.d(TAG, "Dictation result: $text")
        currentInputConnection?.commitText(text, 1)
    }
    
    private fun handleDictationStateChange(isActive: Boolean) {
        Log.d(TAG, "Dictation state changed: $isActive")
        // TODO: Update UI to show dictation state
    }
    
    private fun handleCharacter(primaryCode: Int, ic: InputConnection) {
        var code = primaryCode
        
        // Apply shift if needed
        if (shiftKeyState.isActive() && Character.isLetter(code)) {
            code = Character.toUpperCase(code)
            
            // Reset shift if not locked
            if (!shiftKeyState.isLocked()) {
                shiftKeyState.reset()
                keyboardView.setShifted(false)
            }
        }
        
        ic.commitText(code.toChar().toString(), 1)
        
        // Update suggestions if enabled
        if (preferencesManager.areSuggestionsEnabled()) {
            updateSuggestions()
        }
    }
    
    // Helper methods
    private fun configureKeyboardForInputType(editorInfo: EditorInfo) {
        val inputType = editorInfo.inputType
        
        when (inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER -> keyboardView.showNumberKeyboard()
            EditorInfo.TYPE_CLASS_PHONE -> keyboardView.showPhoneKeyboard()
            EditorInfo.TYPE_CLASS_DATETIME -> keyboardView.showDateTimeKeyboard()
            EditorInfo.TYPE_CLASS_TEXT -> {
                val variation = inputType and EditorInfo.TYPE_MASK_VARIATION
                when (variation) {
                    EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> keyboardView.showEmailKeyboard()
                    EditorInfo.TYPE_TEXT_VARIATION_URI -> keyboardView.showUrlKeyboard()
                    EditorInfo.TYPE_TEXT_VARIATION_PASSWORD -> keyboardView.showPasswordKeyboard()
                    else -> keyboardView.showQwertyKeyboard()
                }
            }
            else -> keyboardView.showQwertyKeyboard()
        }
    }
    
    private fun getKeyboardLayoutForInputType(editorInfo: EditorInfo?): KeyboardLayout {
        if (editorInfo == null) return KeyboardLayout.QWERTY
        
        return when (editorInfo.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER -> KeyboardLayout.NUMERIC
            EditorInfo.TYPE_CLASS_PHONE -> KeyboardLayout.PHONE
            else -> KeyboardLayout.QWERTY
        }
    }
    
    private fun updateShiftStateFromInput() {
        val ic = currentInputConnection ?: return
        val cursorPos = getCursorPosition(ic)
        
        // Auto-capitalize at beginning or after period
        if (cursorPos == 0 || isAfterPeriod(ic, cursorPos)) {
            shiftKeyState.setOn()
            keyboardView.setShifted(true)
        }
    }
    
    private fun autoCapitalizeIfNeeded(ic: InputConnection) {
        autoCapitalizationJob?.cancel()
        autoCapitalizationJob = coroutineScope.launch {
            delay(100) // Small delay to let text settle
            withContext(Dispatchers.Main) {
                if (isAfterPeriod(ic, getCursorPosition(ic))) {
                    shiftKeyState.setOn()
                    keyboardView.setShifted(true)
                }
            }
        }
    }
    
    private fun isAfterPeriod(ic: InputConnection, position: Int): Boolean {
        if (position < 2) return false
        
        val textBefore = ic.getTextBeforeCursor(2, 0)
        return textBefore?.matches(Regex("\\. ?")) == true
    }
    
    private fun getCursorPosition(ic: InputConnection): Int {
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        return extracted?.selectionStart ?: 0
    }
    
    private fun updateSuggestions() {
        suggestionUpdateJob?.cancel()
        suggestionUpdateJob = coroutineScope.launch {
            delay(200) // Debounce
            
            val word = getCurrentWord()
            if (word.isNotEmpty()) {
                val suggestions = getSuggestionsForWord(word)
                withContext(Dispatchers.Main) {
                    keyboardView.updateSuggestions(suggestions)
                }
            }
        }
    }
    
    private fun getCurrentWord(): String {
        val ic = currentInputConnection ?: return ""
        val textBefore = ic.getTextBeforeCursor(20, 0) ?: return ""
        
        return textBefore.split(Regex("\\s")).lastOrNull() ?: ""
    }
    
    private fun getSuggestionsForWord(word: String): List<String> {
        // This would integrate with dictionary/prediction engine
        // TODO: Implement dictionary lookup for the provided word
        return if (word.isNotEmpty()) {
            emptyList() // Placeholder - will use word for dictionary lookup
        } else {
            emptyList()
        }
    }
    
    private fun handleSwipeAction(direction: SwipeDirection) {
        when (direction) {
            SwipeDirection.LEFT -> switchToPreviousKeyboard()
            SwipeDirection.RIGHT -> switchToNextKeyboard()
            else -> {}
        }
    }
    
    private fun switchToPreviousKeyboard() {
        // Use modern API (minSdk 28 guarantees availability)
        switchToPreviousInputMethod()
    }
    
    private fun switchToNextKeyboard() {
        inputMethodManager.showInputMethodPicker()
    }
    
    private fun showSettings() {
        val intent = Intent(this, KeyboardSettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
    
    private fun showEmojiPicker() {
        keyboardView.showEmojiKeyboard()
    }
    
    private fun showSymbolsKeyboard() {
        keyboardView.showSymbolsKeyboard()
    }
    
    private fun showAlternativeCharacters(key: Int) {
        keyboardView.showAlternativeCharacters(key)
    }
    
    private fun showCandidatesView() {
        setCandidatesViewShown(true)
    }
    
    private fun hideKeyboard() {
        requestHideSelf(0)
    }
    
    private fun showKeyboard() {
        // Use modern API (minSdk 28 guarantees availability)
        requestShowSelf(InputMethodManager.SHOW_IMPLICIT)
    }
    
    private fun registerVoiceCommandReceiver() {
        val filter = IntentFilter().apply {
            addAction(KeyboardConstants.ACTION_VOICE_KEY_CODE)
            addAction(KeyboardConstants.ACTION_VOICE_KEY_COMMAND)
            addAction(KeyboardConstants.ACTION_CLOSE_COMMAND)
            addAction(KeyboardConstants.ACTION_VOICE_SWITCH_KEYBOARD)
            addAction(KeyboardConstants.ACTION_VOICE_COMMAND_SHOW_INPUT)
            addAction(KeyboardConstants.ACTION_FREE_SPEECH_COMMAND)
            addAction(KeyboardConstants.ACTION_LAUNCH_DICTATION)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(voiceCommandReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(voiceCommandReceiver, filter)
        }
    }
}

// Supporting interfaces and enums
interface KeyboardActionListener {
    fun onKey(primaryCode: Int, keyCodes: IntArray?)
    fun onText(text: CharSequence)
    fun swipeLeft()
    fun swipeRight()
    fun swipeDown()
    fun swipeUp()
    fun onLongPress(key: Int)
}

enum class KeyboardLayout {
    QWERTY,
    NUMERIC,
    PHONE,
    SYMBOLS,
    EMOJI
}

enum class SwipeDirection {
    LEFT, RIGHT, UP, DOWN
}