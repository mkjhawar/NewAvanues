/**
 * ModifierKeyState.kt - Modifier key state management
 * 
 * Author: Manoj Jhawar
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.utils

/**
 * Manages the state of modifier keys (Shift, Control, Alt)
 * Supports normal, pressed, and locked states
 */
class ModifierKeyState(private val supportsLocked: Boolean = false) {
    
    enum class State {
        NORMAL,
        PRESSED,
        LOCKED
    }
    
    private var state = State.NORMAL
    
    fun isActive(): Boolean = state != State.NORMAL
    
    fun isLocked(): Boolean = state == State.LOCKED
    
    fun isPressed(): Boolean = state == State.PRESSED
    
    fun onPress() {
        state = when (state) {
            State.NORMAL -> State.PRESSED
            State.PRESSED -> if (supportsLocked) State.LOCKED else State.NORMAL
            State.LOCKED -> State.NORMAL
        }
    }
    
    fun onRelease() {
        if (state == State.PRESSED) {
            state = State.NORMAL
        }
    }
    
    fun setOn() {
        state = State.PRESSED
    }
    
    fun setLocked() {
        if (supportsLocked) {
            state = State.LOCKED
        }
    }
    
    fun reset() {
        state = State.NORMAL
    }
    
    fun getState(): State = state
}