/**
 * InputProcessor.kt - Interface for input processing
 * Direct implementation - no unnecessary abstractions
 * 
 * Author: Manoj Jhawar
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.interfaces

import android.view.inputmethod.InputConnection

/**
 * Interface for processing keyboard input
 */
interface InputProcessor {
    fun processKeyPress(keyCode: Int, inputConnection: InputConnection)
    fun processText(text: String, inputConnection: InputConnection)
    fun processBackspace(inputConnection: InputConnection)
    fun processEnter(inputConnection: InputConnection)
    fun processSpace(inputConnection: InputConnection)
}