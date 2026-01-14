/**
 * IMEUtil.kt - Keyboard utility functions and broadcast communication
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-07
 */
package com.avanues.utils

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.EditorInfo

/**
 * Utility class for Input Method Editor operations and broadcast communication
 * Handles communication between keyboard and VoiceAccessibility service
 */
object IMEUtil {
    
    private const val TAG = "VoiceKeyboard"
    
    // Custom action for keyboards with action label
    const val IME_ACTION_CUSTOM_LABEL = EditorInfo.IME_MASK_ACTION + 1
    
    /**
     * Calculate edit distance between two words using Damerau-Levenshtein algorithm
     * Used for word suggestions and auto-correction
     */
    fun editDistance(
        lowerCaseWord: CharSequence,
        word: CharArray,
        offset: Int,
        length: Int
    ): Int {
        val sl = lowerCaseWord.length
        val tl = length
        val dp = Array(sl + 1) { IntArray(tl + 1) }
        
        for (i in 0..sl) {
            dp[i][0] = i
        }
        for (j in 0..tl) {
            dp[0][j] = j
        }
        
        for (i in 0 until sl) {
            for (j in 0 until tl) {
                val sc = lowerCaseWord[i]
                val tc = word[offset + j].lowercaseChar()
                val cost = if (sc == tc) 0 else 1
                
                dp[i + 1][j + 1] = minOf(
                    dp[i][j + 1] + 1,
                    dp[i + 1][j] + 1,
                    dp[i][j] + cost
                )
                
                // Handle transposition cases
                if (i > 0 && j > 0 &&
                    sc == word[offset + j - 1].lowercaseChar() &&
                    tc == lowerCaseWord[i - 1]
                ) {
                    dp[i + 1][j + 1] = minOf(
                        dp[i + 1][j + 1],
                        dp[i - 1][j - 1] + cost
                    )
                }
            }
        }
        
        return dp[sl][tl]
    }
    
    /**
     * Remove duplicate suggestions from the list
     * Keeps first occurrence and removes subsequent duplicates
     */
    fun removeDupes(
        suggestions: MutableList<CharSequence>,
        stringsPool: MutableList<CharSequence>
    ) {
        if (suggestions.size < 2) return
        
        var i = 1
        while (i < suggestions.size) {
            val current = suggestions[i]
            var isDuplicate = false
            
            for (j in 0 until i) {
                if (TextUtils.equals(current, suggestions[j])) {
                    removeSuggestion(suggestions, i, stringsPool)
                    i--
                    isDuplicate = true
                    break
                }
            }
            
            if (!isDuplicate) i++
        }
    }
    
    /**
     * Trim suggestions list to maximum size
     */
    fun trimSuggestions(
        suggestions: MutableList<CharSequence>,
        maxSuggestions: Int,
        stringsPool: MutableList<CharSequence>
    ) {
        while (suggestions.size > maxSuggestions) {
            removeSuggestion(suggestions, maxSuggestions, stringsPool)
        }
    }
    
    private fun removeSuggestion(
        suggestions: MutableList<CharSequence>,
        indexToRemove: Int,
        stringsPool: MutableList<CharSequence>
    ) {
        val removed = suggestions.removeAt(indexToRemove)
        if (removed is StringBuilder) {
            stringsPool.add(removed)
        }
    }
    
    /**
     * Get IME action ID from editor info
     * Handles custom labels and no-enter-action flag
     */
    fun getImeOptionsActionIdFromEditorInfo(editorInfo: EditorInfo): Int {
        return when {
            (editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0 -> {
                EditorInfo.IME_ACTION_NONE
            }
            editorInfo.actionLabel != null -> {
                IME_ACTION_CUSTOM_LABEL
            }
            else -> {
                editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
            }
        }
    }
    
    /**
     * Send keyboard height to VoiceAccessibility service
     * Used for overlay positioning and UI adjustments
     */
    fun sendKeyboardHeightToApp(
        context: Context,
        height: Int,
        overlapKeyboard: Boolean
    ) {
        val intent = Intent(KeyboardConstants.ACTION_KEYBOARD_HEIGHT).apply {
            setPackage(context.packageName) // Make intent explicit for security
            putExtra(KeyboardConstants.KEY_KEYBOARD_HEIGHT, height)
            putExtra(KeyboardConstants.KEY_OVERLAP_KEYBOARD, overlapKeyboard)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        context.sendBroadcast(intent)
        
        Log.d(TAG, "Sent keyboard height: $height, overlap: $overlapKeyboard")
    }
    
    /**
     * Send keyboard open/close status to VoiceAccessibility
     * Critical for dictation flow and voice command handling
     */
    fun sendKeyboardOpenStatusToApp(
        context: Context,
        isOpened: Boolean
    ) {
        val intent = Intent(KeyboardConstants.ACTION_KEYBOARD_OPEN_STATUS).apply {
            setPackage(context.packageName) // Make intent explicit for security
            putExtra(KeyboardConstants.KEY_OPENED, isOpened)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        context.sendBroadcast(intent)
        
        Log.d(TAG, "Sent keyboard open status: $isOpened")
    }
    
    /**
     * Launch dictation mode
     * Triggers VoiceAccessibility to start listening for dictation
     */
    fun launchDictation(context: Context) {
        val intent = Intent(KeyboardConstants.ACTION_LAUNCH_DICTATION).apply {
            setPackage(context.packageName) // Make intent explicit for security
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        context.sendBroadcast(intent)
        
        Log.d(TAG, "Launched dictation mode")
    }
    
    /**
     * Toggle keyboard command bar visibility
     * Shows/hides additional command options in keyboard UI
     */
    fun showHideKeyboardCommandBar(context: Context) {
        val intent = Intent(KeyboardConstants.ACTION_KEYBOARD_COMMAND_BAR).apply {
            setPackage(context.packageName) // Make intent explicit for security
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        context.sendBroadcast(intent)
        
        Log.d(TAG, "Toggled keyboard command bar")
    }
    
    /**
     * Send dictation status to VoiceAccessibility
     * Indicates whether keyboard is actively in dictation mode
     */
    fun sendDictationStatus(
        context: Context,
        isDictationActive: Boolean
    ) {
        val intent = Intent(KeyboardConstants.ACTION_DICTATION_STATUS).apply {
            setPackage(context.packageName) // Make intent explicit for security
            putExtra(KeyboardConstants.KEY_DICTATION_ACTIVE, isDictationActive)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        context.sendBroadcast(intent)
        
        Log.d(TAG, "Sent dictation status: $isDictationActive")
    }
}