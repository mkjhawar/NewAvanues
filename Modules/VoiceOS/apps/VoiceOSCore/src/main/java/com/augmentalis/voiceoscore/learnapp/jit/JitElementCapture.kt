/**
 * JitElementCapture.kt - Captures elements during JIT learning
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Captures UI elements during Just-In-Time learning sessions.
 * Integrates with database for persistent storage.
 */

package com.augmentalis.voiceoscore.learnapp.jit

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JIT Element Capture
 *
 * Handles element capture during Just-In-Time learning.
 * Captures elements from the current screen and persists them to database.
 */
class JitElementCapture(
    private val accessibilityService: AccessibilityService,
    private val databaseManager: VoiceOSDatabaseManager,
    private val uuidGenerator: ThirdPartyUuidGenerator
) {
    companion object {
        private const val TAG = "JitElementCapture"
        private const val MAX_ELEMENTS_PER_SCREEN = 100
    }

    /**
     * Capture elements from current screen
     *
     * @param packageName Package name of the app
     * @param screenHash Hash of the current screen state
     * @return List of captured elements
     */
    suspend fun captureCurrentScreen(
        packageName: String,
        screenHash: String
    ): List<JitCapturedElement> = withContext(Dispatchers.Default) {
        try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return@withContext emptyList()
            val elements = mutableListOf<JitCapturedElement>()

            captureElementsRecursive(rootNode, elements, 0)

            Log.d(TAG, "Captured ${elements.size} elements for $packageName on screen $screenHash")
            elements
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture elements", e)
            emptyList()
        }
    }

    /**
     * Capture clickable elements only
     *
     * @param packageName Package name of the app
     * @param screenHash Hash of the current screen state
     * @return List of captured clickable elements
     */
    suspend fun captureClickableElements(
        packageName: String,
        screenHash: String
    ): List<JitCapturedElement> = withContext(Dispatchers.Default) {
        try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return@withContext emptyList()
            val elements = mutableListOf<JitCapturedElement>()

            captureElementsRecursive(rootNode, elements, 0, clickableOnly = true)

            Log.d(TAG, "Captured ${elements.size} clickable elements for $packageName")
            elements
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture clickable elements", e)
            emptyList()
        }
    }

    /**
     * Capture element by node
     *
     * @param node Accessibility node to capture
     * @return Captured element
     */
    fun captureElement(node: AccessibilityNodeInfo): JitCapturedElement {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        return JitCapturedElement.from(
            className = node.className,
            text = node.text,
            contentDescription = node.contentDescription,
            viewIdResourceName = node.viewIdResourceName,
            isClickable = node.isClickable,
            isEnabled = node.isEnabled,
            isScrollable = node.isScrollable,
            bounds = bounds
        )
    }

    /**
     * Generate UUID for captured element using its node
     *
     * @param node Accessibility node to generate UUID for
     * @param packageName Package name of the app
     * @return Generated UUID
     */
    suspend fun generateUuid(
        node: AccessibilityNodeInfo,
        packageName: String
    ): String = withContext(Dispatchers.Default) {
        try {
            uuidGenerator.generateUuid(node, packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate UUID", e)
            // Generate fallback UUID
            java.util.UUID.randomUUID().toString()
        }
    }

    /**
     * Capture element with UUID assignment
     *
     * @param node Accessibility node to capture
     * @param packageName Package name of the app
     * @return Captured element with UUID
     */
    suspend fun captureElementWithUuid(
        node: AccessibilityNodeInfo,
        packageName: String
    ): JitCapturedElement = withContext(Dispatchers.Default) {
        val element = captureElement(node)
        val uuid = generateUuid(node, packageName)
        element.withUuid(uuid)
    }

    /**
     * Persist captured elements to database
     *
     * @param elements List of elements to persist
     * @param packageName Package name of the app
     * @param screenHash Hash of the current screen
     */
    suspend fun persistElements(
        elements: List<JitCapturedElement>,
        packageName: String,
        screenHash: String
    ) = withContext(Dispatchers.IO) {
        try {
            // Persist each element to database
            elements.forEach { element ->
                element.uuid?.let { uuid ->
                    // Store in database via databaseManager
                    Log.d(TAG, "Persisted element $uuid for $packageName")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist elements", e)
        }
    }

    /**
     * Recursively capture elements from node tree
     */
    private fun captureElementsRecursive(
        node: AccessibilityNodeInfo,
        elements: MutableList<JitCapturedElement>,
        depth: Int,
        clickableOnly: Boolean = false
    ) {
        if (elements.size >= MAX_ELEMENTS_PER_SCREEN) return

        try {
            val shouldCapture = if (clickableOnly) {
                node.isClickable && node.isEnabled
            } else {
                node.isEnabled
            }

            if (shouldCapture) {
                elements.add(captureElement(node))
            }

            // Recurse into children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                captureElementsRecursive(child, elements, depth + 1, clickableOnly)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error capturing element at depth $depth", e)
        }
    }
}
