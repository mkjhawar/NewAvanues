/**
 * IElementInteraction.kt - Interface for element interaction operations
 *
 * Handles click, long click, input text, and navigation actions.
 * Extracted from JITLearningService as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 2.2.0 (SOLID Refactoring)
 */

package com.augmentalis.jitlearning.handlers

import com.augmentalis.jitlearning.CommandType
import com.augmentalis.jitlearning.ExplorationCommand
import com.augmentalis.jitlearning.ScrollDirection

/**
 * Element Interaction Handler Interface
 *
 * Responsibilities:
 * - Perform click actions on elements
 * - Perform long click actions
 * - Input text into fields
 * - Handle navigation actions (back, home)
 * - Perform scroll and swipe gestures
 *
 * Single Responsibility: Element interaction execution
 */
interface IElementInteraction {
    /**
     * Click element by UUID.
     *
     * @param elementUuid Element UUID
     * @return True if click successful
     */
    suspend fun clickElement(elementUuid: String): Boolean

    /**
     * Long click element by UUID.
     *
     * @param elementUuid Element UUID
     * @return True if long click successful
     */
    suspend fun longClickElement(elementUuid: String): Boolean

    /**
     * Input text into element.
     *
     * @param text Text to input
     * @param elementUuid Element UUID
     * @return True if input successful
     */
    suspend fun inputText(text: String, elementUuid: String): Boolean

    /**
     * Perform back navigation.
     *
     * @return True if back successful
     */
    suspend fun pressBack(): Boolean

    /**
     * Perform home navigation.
     *
     * @return True if home successful
     */
    suspend fun pressHome(): Boolean

    /**
     * Perform scroll action.
     *
     * @param direction Scroll direction
     * @param distance Scroll distance
     * @return True if scroll successful
     */
    suspend fun scroll(direction: ScrollDirection, distance: Int): Boolean

    /**
     * Perform swipe gesture.
     *
     * @param direction Swipe direction
     * @return True if swipe successful
     */
    suspend fun swipe(direction: ScrollDirection): Boolean

    /**
     * Perform generic exploration command.
     *
     * @param command Exploration command
     * @return True if command successful
     */
    suspend fun performAction(command: ExplorationCommand): Boolean

    /**
     * Focus on element.
     *
     * @param elementUuid Element UUID
     * @return True if focus successful
     */
    suspend fun focus(elementUuid: String): Boolean

    /**
     * Clear text from element.
     *
     * @param elementUuid Element UUID
     * @return True if clear successful
     */
    suspend fun clearText(elementUuid: String): Boolean

    /**
     * Expand collapsible element.
     *
     * @param elementUuid Element UUID
     * @return True if expand successful
     */
    suspend fun expand(elementUuid: String): Boolean

    /**
     * Select element.
     *
     * @param elementUuid Element UUID
     * @return True if select successful
     */
    suspend fun select(elementUuid: String): Boolean
}
