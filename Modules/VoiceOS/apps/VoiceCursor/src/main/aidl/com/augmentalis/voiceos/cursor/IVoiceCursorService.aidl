package com.augmentalis.voiceos.cursor;

import com.augmentalis.voiceos.cursor.CursorPosition;
import com.augmentalis.voiceos.cursor.CursorConfiguration;

/**
 * Voice Cursor Service Interface
 *
 * Provides cursor overlay and gesture control functionality
 * for cross-app usage via IPC.
 */
interface IVoiceCursorService {

    /**
     * Check if cursor API is initialized and ready
     *
     * @return true if initialized, false otherwise
     */
    boolean isInitialized();

    /**
     * Show cursor overlay with optional configuration
     *
     * @param config Cursor configuration (null = use defaults)
     * @return true if cursor shown successfully, false otherwise
     */
    boolean showCursor(in CursorConfiguration config);

    /**
     * Hide cursor overlay
     *
     * @return true if cursor hidden successfully, false otherwise
     */
    boolean hideCursor();

    /**
     * Toggle cursor visibility (show if hidden, hide if shown)
     *
     * @return true if toggle successful, false otherwise
     */
    boolean toggleCursor();

    /**
     * Check if cursor is currently visible
     *
     * @return true if cursor is visible, false otherwise
     */
    boolean isVisible();

    /**
     * Center cursor on screen
     *
     * @return true if cursor centered successfully, false otherwise
     */
    boolean centerCursor();

    /**
     * Move cursor to specified position
     *
     * @param position Target position (x, y coordinates)
     * @param animate Whether to animate movement (default: true)
     * @return true if cursor moved successfully, false otherwise
     */
    boolean moveTo(in CursorPosition position, boolean animate);

    /**
     * Get current cursor position
     *
     * @return Current cursor position, or null if not visible/initialized
     */
    CursorPosition getCurrentPosition();

    /**
     * Execute cursor action at current or specified position
     *
     * Actions:
     * - 0 = SINGLE_CLICK
     * - 1 = DOUBLE_CLICK
     * - 2 = LONG_PRESS
     * - 3 = SCROLL_UP
     * - 4 = SCROLL_DOWN
     * - 5 = DRAG_START
     * - 6 = DRAG_END
     *
     * @param action Action code (see above)
     * @param position Optional target position (null = current cursor position)
     * @return true if action executed successfully, false otherwise
     */
    boolean executeAction(int action, in CursorPosition position);

    /**
     * Perform click at current cursor position
     *
     * @return true if click performed successfully, false otherwise
     */
    boolean click();

    /**
     * Perform double-click at current cursor position
     *
     * @return true if double-click performed successfully, false otherwise
     */
    boolean doubleClick();

    /**
     * Perform long press at current cursor position
     *
     * @return true if long press performed successfully, false otherwise
     */
    boolean longPress();

    /**
     * Perform scroll up at current cursor position
     *
     * @return true if scroll performed successfully, false otherwise
     */
    boolean scrollUp();

    /**
     * Perform scroll down at current cursor position
     *
     * @return true if scroll performed successfully, false otherwise
     */
    boolean scrollDown();

    /**
     * Update cursor configuration
     *
     * @param config New cursor configuration
     * @return true if configuration updated successfully, false otherwise
     */
    boolean updateConfiguration(in CursorConfiguration config);
}
