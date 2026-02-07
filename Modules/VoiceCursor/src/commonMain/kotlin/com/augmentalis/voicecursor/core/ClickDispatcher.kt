package com.augmentalis.voicecursor.core

/**
 * Abstraction for click dispatch from cursor overlay.
 * App layer implements this using AccessibilityService.dispatchGesture().
 */
interface ClickDispatcher {
    fun dispatchClick(x: Int, y: Int)
    fun dispatchLongPress(x: Int, y: Int) {}
    fun dispatchDrag(startX: Int, startY: Int, endX: Int, endY: Int) {}
}
