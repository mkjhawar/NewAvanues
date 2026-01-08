/**
 * ElementInteractionHandler.kt - Implementation of element interaction operations
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

import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.jitlearning.CommandType
import com.augmentalis.jitlearning.ExplorationCommand
import com.augmentalis.jitlearning.JITLearningService
import com.augmentalis.jitlearning.ScrollDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Element Interaction Handler
 *
 * Executes interaction commands on accessibility elements.
 *
 * Features:
 * - Click/long click actions
 * - Text input
 * - Scroll/swipe gestures
 * - Navigation actions (back, home)
 * - Focus and selection
 *
 * Dependencies:
 * - INodeCache for element lookup
 * - AccessibilityServiceInterface for global actions
 *
 * Thread Safety: Suspend functions are safe for coroutine use
 */
class ElementInteractionHandler(
    private val nodeCache: INodeCache,
    private val accessibilityService: JITLearningService.AccessibilityServiceInterface
) : IElementInteraction {

    companion object {
        private const val TAG = "ElementInteractionHandler"
    }

    override suspend fun clickElement(elementUuid: String): Boolean = withContext(Dispatchers.Main) {
        val node = nodeCache.getNode(elementUuid)
        if (node == null) {
            Log.w(TAG, "Element not found: $elementUuid")
            return@withContext false
        }

        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override suspend fun longClickElement(elementUuid: String): Boolean = withContext(Dispatchers.Main) {
        val node = nodeCache.getNode(elementUuid)
        if (node == null) {
            Log.w(TAG, "Element not found: $elementUuid")
            return@withContext false
        }

        node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    override suspend fun inputText(text: String, elementUuid: String): Boolean = withContext(Dispatchers.Main) {
        val node = nodeCache.getNode(elementUuid)
        if (node == null) {
            Log.w(TAG, "Element not found: $elementUuid")
            return@withContext false
        }

        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    override suspend fun pressBack(): Boolean = withContext(Dispatchers.Main) {
        accessibilityService.performGlobalAction(
            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
        )
    }

    override suspend fun pressHome(): Boolean = withContext(Dispatchers.Main) {
        accessibilityService.performGlobalAction(
            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
        )
    }

    override suspend fun scroll(direction: ScrollDirection, distance: Int): Boolean = withContext(Dispatchers.Main) {
        val rootNode = accessibilityService.getRootNode() ?: return@withContext false

        val scrollableNode = findScrollableNode(rootNode)
        if (scrollableNode == null) {
            Log.w(TAG, "No scrollable node found")
            return@withContext false
        }

        val action = when (direction) {
            ScrollDirection.UP, ScrollDirection.LEFT -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            ScrollDirection.DOWN, ScrollDirection.RIGHT -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }

        scrollableNode.performAction(action)
    }

    override suspend fun swipe(direction: ScrollDirection): Boolean = withContext(Dispatchers.Main) {
        val gestureAction = when (direction) {
            ScrollDirection.UP -> android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_UP
            ScrollDirection.DOWN -> android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_DOWN
            ScrollDirection.LEFT -> android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_LEFT
            ScrollDirection.RIGHT -> android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT
        }

        accessibilityService.performGlobalAction(gestureAction)
    }

    override suspend fun performAction(command: ExplorationCommand): Boolean {
        return when (command.type) {
            CommandType.CLICK -> clickElement(command.elementUuid)
            CommandType.LONG_CLICK -> longClickElement(command.elementUuid)
            CommandType.SCROLL -> scroll(command.direction, command.distance)
            CommandType.SWIPE -> swipe(command.direction)
            CommandType.SET_TEXT -> inputText(command.text, command.elementUuid)
            CommandType.BACK -> pressBack()
            CommandType.HOME -> pressHome()
            CommandType.FOCUS -> focus(command.elementUuid)
            CommandType.CLEAR_TEXT -> clearText(command.elementUuid)
            CommandType.EXPAND -> expand(command.elementUuid)
            CommandType.SELECT -> select(command.elementUuid)
        }
    }

    override suspend fun focus(elementUuid: String): Boolean = withContext(Dispatchers.Main) {
        val node = nodeCache.getNode(elementUuid)
        if (node == null) {
            Log.w(TAG, "Element not found: $elementUuid")
            return@withContext false
        }

        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    }

    override suspend fun clearText(elementUuid: String): Boolean = withContext(Dispatchers.Main) {
        val node = nodeCache.getNode(elementUuid)
        if (node == null) {
            Log.w(TAG, "Element not found: $elementUuid")
            return@withContext false
        }

        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    override suspend fun expand(elementUuid: String): Boolean = withContext(Dispatchers.Main) {
        val node = nodeCache.getNode(elementUuid)
        if (node == null) {
            Log.w(TAG, "Element not found: $elementUuid")
            return@withContext false
        }

        node.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
    }

    override suspend fun select(elementUuid: String): Boolean = withContext(Dispatchers.Main) {
        val node = nodeCache.getNode(elementUuid)
        if (node == null) {
            Log.w(TAG, "Element not found: $elementUuid")
            return@withContext false
        }

        node.performAction(AccessibilityNodeInfo.ACTION_SELECT)
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            if (child.isScrollable) return child

            val scrollable = findScrollableNode(child)
            if (scrollable != null) return scrollable
        }

        return null
    }
}
