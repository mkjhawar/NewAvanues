package com.augmentalis.Avanues.web.universal.presentation.controller

/**
 * GestureMapper - Maps gesture type strings to JavaScript function calls
 *
 * This is a pure, platform-agnostic mapping object that converts VoiceOS gesture
 * types to AvanuesGestures JavaScript library calls. Can be used on any platform
 * that has a JavaScript execution capability.
 *
 * Usage:
 * ```
 * val script = GestureMapper.mapToScript("GESTURE_CLICK", 100f, 200f, 0)
 * if (script != null) {
 *     webView.evaluateJavaScript("JSON.stringify($script)")
 * }
 * ```
 */
object GestureMapper {

    /**
     * Map a gesture type to its corresponding JavaScript function call
     *
     * @param gestureType VoiceOS gesture type (e.g., "GESTURE_CLICK")
     * @param x X coordinate for the gesture
     * @param y Y coordinate for the gesture
     * @param modifiers Keyboard modifier bitmask (used by some gestures)
     * @return JavaScript expression string, or null if gesture type is unknown
     */
    fun mapToScript(
        gestureType: String,
        x: Float,
        y: Float,
        modifiers: Int = 0
    ): String? {
        return when (gestureType) {
            // Basic pointer gestures
            "GESTURE_CLICK" -> "window.AvanuesGestures.click($x, $y)"
            "GESTURE_DOUBLE_CLICK" -> "window.AvanuesGestures.doubleClick($x, $y)"
            "GESTURE_LONG_PRESS" -> "window.AvanuesGestures.longPress($x, $y)"
            "GESTURE_TAP" -> "window.AvanuesGestures.tap($x, $y)"

            // Drag gestures
            "GESTURE_DRAG_START" -> "window.AvanuesGestures.dragStart($x, $y)"
            "GESTURE_DRAG_MOVE" -> "window.AvanuesGestures.dragMove($x, $y)"
            "GESTURE_DRAG_END" -> "window.AvanuesGestures.dragEnd($x, $y)"

            // Swipe gestures
            "GESTURE_SWIPE_LEFT" -> "window.AvanuesGestures.swipeLeft($x, $y)"
            "GESTURE_SWIPE_RIGHT" -> "window.AvanuesGestures.swipeRight($x, $y)"
            "GESTURE_SWIPE_UP" -> "window.AvanuesGestures.swipeUp($x, $y)"
            "GESTURE_SWIPE_DOWN" -> "window.AvanuesGestures.swipeDown($x, $y)"

            // Selection gestures
            "GESTURE_SELECT_START" -> "window.AvanuesGestures.selectStart($x, $y)"
            "GESTURE_SELECT_EXTEND" -> "window.AvanuesGestures.selectExtend($x, $y)"
            "GESTURE_SELECT_WORD" -> "window.AvanuesGestures.selectWord($x, $y)"
            "GESTURE_SELECT_ALL" -> "window.AvanuesGestures.selectAll()"
            "GESTURE_CLEAR_SELECTION" -> "window.AvanuesGestures.clearSelection()"

            // Clipboard gestures (async)
            "GESTURE_COPY" -> "await window.AvanuesGestures.copy()"
            "GESTURE_CUT" -> "await window.AvanuesGestures.cut()"
            "GESTURE_PASTE" -> "await window.AvanuesGestures.paste($x, $y)"

            // 3D transform gestures
            "GESTURE_ROTATE_X" -> "window.AvanuesGestures.rotateX($x, $y, $modifiers)"
            "GESTURE_ROTATE_Y" -> "window.AvanuesGestures.rotateY($x, $y, $modifiers)"
            "GESTURE_ROTATE_Z" -> "window.AvanuesGestures.rotateZ($x, $y, $modifiers)"
            "GESTURE_PAN" -> "window.AvanuesGestures.pan($x, $y)"
            "GESTURE_TILT" -> "window.AvanuesGestures.tilt($x, $y, $modifiers)"
            "GESTURE_ORBIT" -> "window.AvanuesGestures.orbit($x, $y, $modifiers, 0)"

            // Zoom/Scale gestures
            "GESTURE_ZOOM_IN" -> "window.AvanuesGestures.zoomIn($x, $y)"
            "GESTURE_ZOOM_OUT" -> "window.AvanuesGestures.zoomOut($x, $y)"
            "GESTURE_RESET_ZOOM" -> "window.AvanuesGestures.resetZoom()"
            "GESTURE_SCALE" -> "window.AvanuesGestures.scale($x, $y, ${modifiers / 100.0})"

            // Scrolling gestures
            "GESTURE_SCROLL_UP" -> "window.AvanuesGestures.scrollBy(0, -100)"
            "GESTURE_SCROLL_DOWN" -> "window.AvanuesGestures.scrollBy(0, 100)"
            "GESTURE_SCROLL_LEFT" -> "window.AvanuesGestures.scrollBy(-100, 0)"
            "GESTURE_SCROLL_RIGHT" -> "window.AvanuesGestures.scrollBy(100, 0)"
            "GESTURE_SCROLL_TO_TOP" -> "window.AvanuesGestures.scrollToTop()"
            "GESTURE_SCROLL_TO_BOTTOM" -> "window.AvanuesGestures.scrollToBottom()"
            "GESTURE_PAGE_UP" -> "window.AvanuesGestures.pageUp()"
            "GESTURE_PAGE_DOWN" -> "window.AvanuesGestures.pageDown()"
            "GESTURE_FLING" -> "window.AvanuesGestures.fling($modifiers, 'down')"

            // Grab gestures
            "GESTURE_GRAB" -> "window.AvanuesGestures.grab($x, $y)"
            "GESTURE_RELEASE" -> "window.AvanuesGestures.release()"
            "GESTURE_THROW" -> "window.AvanuesGestures.throwElement($x, $y)"

            // Drawing gestures
            "GESTURE_STROKE_START" -> "window.AvanuesGestures.strokeStart($x, $y)"
            "GESTURE_STROKE_MOVE" -> "window.AvanuesGestures.strokeMove($x, $y)"
            "GESTURE_STROKE_END" -> "window.AvanuesGestures.strokeEnd()"
            "GESTURE_ERASE" -> "window.AvanuesGestures.erase($x, $y)"

            // Focus & Input gestures
            "GESTURE_FOCUS" -> "window.AvanuesGestures.focus($x, $y)"
            "GESTURE_HOVER" -> "window.AvanuesGestures.hover($x, $y)"
            "GESTURE_HOVER_OUT" -> "window.AvanuesGestures.hoverOut($x, $y)"

            else -> null
        }
    }

    /**
     * Check if a gesture type is valid/supported
     *
     * @param gestureType The gesture type to check
     * @return true if the gesture type is supported
     */
    fun isSupported(gestureType: String): Boolean {
        return mapToScript(gestureType, 0f, 0f, 0) != null
    }

    /**
     * Get all supported gesture types
     *
     * @return Set of all supported gesture type strings
     */
    fun getSupportedGestures(): Set<String> = setOf(
        // Basic pointer
        "GESTURE_CLICK", "GESTURE_DOUBLE_CLICK", "GESTURE_LONG_PRESS", "GESTURE_TAP",
        // Drag
        "GESTURE_DRAG_START", "GESTURE_DRAG_MOVE", "GESTURE_DRAG_END",
        // Swipe
        "GESTURE_SWIPE_LEFT", "GESTURE_SWIPE_RIGHT", "GESTURE_SWIPE_UP", "GESTURE_SWIPE_DOWN",
        // Selection
        "GESTURE_SELECT_START", "GESTURE_SELECT_EXTEND", "GESTURE_SELECT_WORD",
        "GESTURE_SELECT_ALL", "GESTURE_CLEAR_SELECTION",
        // Clipboard
        "GESTURE_COPY", "GESTURE_CUT", "GESTURE_PASTE",
        // 3D Transform
        "GESTURE_ROTATE_X", "GESTURE_ROTATE_Y", "GESTURE_ROTATE_Z",
        "GESTURE_PAN", "GESTURE_TILT", "GESTURE_ORBIT",
        // Zoom/Scale
        "GESTURE_ZOOM_IN", "GESTURE_ZOOM_OUT", "GESTURE_RESET_ZOOM", "GESTURE_SCALE",
        // Scrolling
        "GESTURE_SCROLL_UP", "GESTURE_SCROLL_DOWN", "GESTURE_SCROLL_LEFT", "GESTURE_SCROLL_RIGHT",
        "GESTURE_SCROLL_TO_TOP", "GESTURE_SCROLL_TO_BOTTOM", "GESTURE_PAGE_UP", "GESTURE_PAGE_DOWN",
        "GESTURE_FLING",
        // Grab
        "GESTURE_GRAB", "GESTURE_RELEASE", "GESTURE_THROW",
        // Drawing
        "GESTURE_STROKE_START", "GESTURE_STROKE_MOVE", "GESTURE_STROKE_END", "GESTURE_ERASE",
        // Focus & Input
        "GESTURE_FOCUS", "GESTURE_HOVER", "GESTURE_HOVER_OUT"
    )

    /**
     * Check if a gesture requires coordinates
     *
     * @param gestureType The gesture type to check
     * @return true if the gesture uses x,y coordinates
     */
    fun requiresCoordinates(gestureType: String): Boolean {
        return gestureType !in setOf(
            "GESTURE_SELECT_ALL",
            "GESTURE_CLEAR_SELECTION",
            "GESTURE_COPY",
            "GESTURE_CUT",
            "GESTURE_RESET_ZOOM",
            "GESTURE_RELEASE",
            "GESTURE_STROKE_END",
            "GESTURE_SCROLL_UP",
            "GESTURE_SCROLL_DOWN",
            "GESTURE_SCROLL_LEFT",
            "GESTURE_SCROLL_RIGHT",
            "GESTURE_SCROLL_TO_TOP",
            "GESTURE_SCROLL_TO_BOTTOM",
            "GESTURE_PAGE_UP",
            "GESTURE_PAGE_DOWN"
        )
    }
}
