/**
 * TextInputService.kt - Text input processing service
 * Direct implementation - no unnecessary abstractions
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.services

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.augmentalis.voicekeyboard.interfaces.InputProcessor
import com.augmentalis.voicekeyboard.utils.KeyboardConstants

/**
 * Service implementation for text input processing
 * Handles all keyboard input processing logic
 */
class TextInputService : InputProcessor {
    
    override fun processKeyPress(keyCode: Int, inputConnection: InputConnection) {
        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> processBackspace(inputConnection)
            KeyEvent.KEYCODE_ENTER -> processEnter(inputConnection)
            KeyEvent.KEYCODE_SPACE -> processSpace(inputConnection)
            KeyboardConstants.KEYCODE_SHIFT -> handleSpecialKey(keyCode)
            KeyboardConstants.KEYCODE_MODE_CHANGE -> handleSpecialKey(keyCode)
            KeyboardConstants.KEYCODE_VOICE -> handleSpecialKey(keyCode)
            KeyboardConstants.KEYCODE_SETTINGS -> handleSpecialKey(keyCode)
            KeyboardConstants.KEYCODE_EMOJI -> handleSpecialKey(keyCode)
            KeyboardConstants.KEYCODE_DICTATION -> handleSpecialKey(keyCode)
            else -> processCharacter(keyCode, inputConnection)
        }
    }
    
    override fun processText(text: String, inputConnection: InputConnection) {
        inputConnection.commitText(text, 1)
    }
    
    override fun processBackspace(inputConnection: InputConnection) {
        val selectedText = inputConnection.getSelectedText(0)
        if (selectedText.isNullOrEmpty()) {
            inputConnection.deleteSurroundingText(1, 0)
        } else {
            inputConnection.commitText("", 1)
        }
    }
    
    override fun processEnter(inputConnection: InputConnection) {
        // This would need editor info to determine proper action
        inputConnection.commitText("\n", 1)
    }
    
    fun processEnterWithEditorInfo(inputConnection: InputConnection, editorInfo: EditorInfo?) {
        val imeOptions = editorInfo?.imeOptions ?: 0
        val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
        
        when (actionId) {
            EditorInfo.IME_ACTION_SEARCH -> inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
            EditorInfo.IME_ACTION_GO -> inputConnection.performEditorAction(EditorInfo.IME_ACTION_GO)
            EditorInfo.IME_ACTION_SEND -> inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEND)
            EditorInfo.IME_ACTION_NEXT -> inputConnection.performEditorAction(EditorInfo.IME_ACTION_NEXT)
            EditorInfo.IME_ACTION_DONE -> inputConnection.performEditorAction(EditorInfo.IME_ACTION_DONE)
            else -> inputConnection.commitText("\n", 1)
        }
    }
    
    override fun processSpace(inputConnection: InputConnection) {
        inputConnection.commitText(" ", 1)
    }
    
    private fun processCharacter(keyCode: Int, inputConnection: InputConnection) {
        inputConnection.commitText(keyCode.toChar().toString(), 1)
    }
    
    private fun handleSpecialKey(keyCode: Int) {
        // Special keys need to be handled by the coordinator
        // This method serves as a placeholder for the interface
        // TODO: Implement special key handling for keyCode: $keyCode
        when (keyCode) {
            KeyboardConstants.KEYCODE_SHIFT -> { /* TODO: Handle shift */ }
            KeyboardConstants.KEYCODE_MODE_CHANGE -> { /* TODO: Handle mode change */ }
            KeyboardConstants.KEYCODE_VOICE -> { /* TODO: Handle voice input */ }
            KeyboardConstants.KEYCODE_SETTINGS -> { /* TODO: Handle settings */ }
            KeyboardConstants.KEYCODE_EMOJI -> { /* TODO: Handle emoji picker */ }
            KeyboardConstants.KEYCODE_DICTATION -> { /* TODO: Handle dictation */ }
            else -> { /* TODO: Handle unknown special key */ }
        }
    }
}