/**
 * JitElementCapture.kt - Element capture from accessibility tree during JIT learning
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI-Assisted Implementation
 * Created: 2025-12-01
 *
 * Captures UI elements from the live accessibility tree during JIT (Just-In-Time) learning.
 * Only captures actionable elements (clickable, editable, scrollable) for voice command generation.
 *
 * Performance Target: <50ms per screen capture
 *
 * Part of Voice Command Element Persistence feature (Phase 1)
 */
package com.augmentalis.voiceoscore.learnapp.jit

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.ScrapedElementDTO
import com.augmentalis.avidcreator.thirdparty.AvidFingerprint
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import com.augmentalis.voiceoscore.utils.forEachChild
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

// JitCapturedElement is defined in JitCapturedElement.kt

/**
 * JIT Element Capture Engine
 *
 * Captures actionable UI elements from the accessibility tree during passive JIT learning.
 * Designed for speed (<50ms) and minimal memory footprint.
 *
 * ## Features:
 * - Captures only actionable elements (clickable, editable, scrollable)
 * - Uses stable element hashing via AvidFingerprint
 * - Generates stable UUIDs via ThirdPartyAvidGenerator (Phase 4)
 * - Respects depth limits to prevent stack overflow
 * - Timeout guard for performance safety
 *
 * ## Usage:
 * ```kotlin
 * val capturer = JitElementCapture(accessibilityService, databaseManager, thirdPartyGenerator)
 * val elements = capturer.captureScreenElements(packageName)
 * ```
 *
 * @param accessibilityService The accessibility service for tree access
 * @param databaseManager Database manager for persistence
 * @param thirdPartyUuidGenerator UUID generator for element identification (Phase 4)
 */
class JitElementCapture(
    private val accessibilityService: AccessibilityService,
    private val databaseManager: VoiceOSDatabaseManager,
    private val thirdPartyUuidGenerator: com.augmentalis.uuidcreator.thirdparty.ThirdPartyAvidGenerator
) {
    /**
     * Developer settings for JIT configuration
     */
    private val developerSettings by lazy {
        LearnAppDeveloperSettings(accessibilityService)
    }

    companion object {
        private const val TAG = "JitElementCapture"

        // Element filtering - only capture actionable elements
        private fun isActionable(node: AccessibilityNodeInfo): Boolean {
            return node.isClickable || node.isEditable || node.isScrollable ||
                   node.isLongClickable || node.isCheckable
        }

        // Check if element has meaningful label for voice commands
        private fun hasLabel(node: AccessibilityNodeInfo): Boolean {
            return !node.text.isNullOrBlank() ||
                   !node.contentDescription.isNullOrBlank() ||
                   !node.viewIdResourceName.isNullOrBlank()
        }
    }

    /**
     * Capture UI elements from current screen
     *
     * Traverses the accessibility tree and captures all actionable elements
     * with labels that can be used for voice commands.
     *
     * @param packageName Package name of the app being captured
     * @return List of captured elements (empty if capture fails or times out)
     */
    suspend fun captureScreenElements(packageName: String): List<JitCapturedElement> {
        val startTime = System.currentTimeMillis()

        return withTimeoutOrNull(developerSettings.getJitCaptureTimeoutMs()) {
            withContext(Dispatchers.Main) {
                val elements = mutableListOf<JitCapturedElement>()

                val rootNode = accessibilityService.rootInActiveWindow
                if (rootNode == null) {
                    Log.w(TAG, "No root node available for capture")
                    return@withContext emptyList()
                }

                try {
                    // Get app version for fingerprinting
                    val appVersion = getAppVersion(packageName)

                    // Traverse and capture
                    traverseAndCapture(
                        node = rootNode,
                        packageName = packageName,
                        appVersion = appVersion,
                        elements = elements,
                        depth = 0,
                        indexInParent = 0
                    )

                    val elapsed = System.currentTimeMillis() - startTime
                    Log.i(TAG, "Captured ${elements.size} elements in ${elapsed}ms for $packageName")

                    elements
                } catch (e: Exception) {
                    Log.e(TAG, "Error during element capture for $packageName", e)
                    emptyList()
                } finally {
                    rootNode.recycle()
                }
            }
        } ?: run {
            val elapsed = System.currentTimeMillis() - startTime
            Log.w(TAG, "Element capture timed out after ${elapsed}ms for $packageName")
            emptyList()
        }
    }

    /**
     * Traverse accessibility tree and capture actionable elements
     *
     * Uses depth-first traversal with early termination on depth/count limits.
     * Only captures elements that are actionable AND have labels.
     *
     * Phase 4 (2025-12-02): Now suspend function to support UUID generation.
     */
    private suspend fun traverseAndCapture(
        node: AccessibilityNodeInfo,
        packageName: String,
        appVersion: String,
        elements: MutableList<JitCapturedElement>,
        depth: Int,
        indexInParent: Int
    ) {
        // Depth limit check
        if (depth > developerSettings.getJitMaxTraversalDepth()) {
            Log.v(TAG, "Max depth (${developerSettings.getJitMaxTraversalDepth()}) reached, stopping branch")
            return
        }

        // Element count limit check
        if (elements.size >= developerSettings.getJitMaxElementsCaptured()) {
            Log.v(TAG, "Max elements (${developerSettings.getJitMaxElementsCaptured()}) reached, stopping capture")
            return
        }

        // Capture this node if actionable and has label
        if (isActionable(node) && hasLabel(node)) {
            try {
                val element = captureNode(node, packageName, appVersion, depth, indexInParent)
                if (element != null) {
                    elements.add(element)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error capturing node at depth $depth", e)
            }
        }

        // Traverse children
        node.forEachChild { child ->
            traverseAndCapture(
                node = child,
                packageName = packageName,
                appVersion = appVersion,
                elements = elements,
                depth = depth + 1,
                indexInParent = elements.size  // Approximate index
            )
        }
    }

    /**
     * Capture a single node's data
     *
     * Extracts all relevant properties from the node and calculates stable hash.
     *
     * Phase 4 (2025-12-02): Now generates stable UUIDs using ThirdPartyAvidGenerator.
     * This ensures JIT-captured elements have UUIDs that match LearnApp-captured elements.
     */
    private suspend fun captureNode(
        node: AccessibilityNodeInfo,
        packageName: String,
        appVersion: String,
        depth: Int,
        indexInParent: Int
    ): JitCapturedElement? {
        try {
            // Get bounds
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            // Calculate stable element hash using AvidFingerprint
            val fingerprint = AvidFingerprint.fromNode(
                node = node,
                packageName = packageName,
                appVersion = appVersion
            )
            val elementHash = fingerprint.generateHash()

            // Phase 4: Generate stable UUID using ThirdPartyAvidGenerator
            // Uses same algorithm as LearnApp for consistency
            val uuid = try {
                thirdPartyUuidGenerator.generateUuid(node, packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to generate UUID for element: ${e.message}")
                null
            }

            return JitCapturedElement(
                elementHash = elementHash,
                className = node.className?.toString() ?: "unknown",
                viewIdResourceName = node.viewIdResourceName,
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                bounds = bounds,
                isClickable = node.isClickable,
                isLongClickable = node.isLongClickable,
                isEditable = node.isEditable,
                isScrollable = node.isScrollable,
                isCheckable = node.isCheckable,
                isFocusable = node.isFocusable,
                isEnabled = node.isEnabled,
                depth = depth,
                indexInParent = indexInParent,
                uuid = uuid  // Phase 4: Store UUID
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture node: ${e.message}")
            return null
        }
    }

    /**
     * Persist captured elements to database
     *
     * Saves elements to scraped_element table and returns the count of new elements.
     *
     * FIX (2025-12-02): Added screenHash parameter for deduplication (Spec 009 - Phase 3)
     *
     * @param packageName Package name (used as appId)
     * @param elements List of captured elements
     * @param screenHash Screen hash for deduplication
     * @return Number of new elements persisted (excluding duplicates)
     */
    suspend fun persistElements(
        packageName: String,
        elements: List<JitCapturedElement>,
        screenHash: String? = null
    ): Int = withContext(Dispatchers.IO) {
        var newCount = 0
        val timestamp = System.currentTimeMillis()

        for (element in elements) {
            try {
                // Check if element already exists (by hash AND app)
                val existing = databaseManager.scrapedElements.getByHashAndApp(
                    element.elementHash,
                    packageName
                )
                if (existing != null) {
                    Log.v(TAG, "Element already exists for $packageName: ${element.elementHash}")
                    continue
                }

                // NEW: Check for cross-app collision
                val globalExisting = databaseManager.scrapedElements.getByHash(element.elementHash)
                if (globalExisting != null && globalExisting.appId != packageName) {
                    Log.e(TAG, "APP ID MISMATCH: Element ${element.elementHash} from " +
                               "${globalExisting.appId} blocking $packageName")
                }

                // Insert new element using DTO
                val elementDTO = ScrapedElementDTO(
                    id = 0L,  // Auto-generated by database
                    elementHash = element.elementHash,
                    appId = packageName,
                    uuid = element.uuid,  // Phase 4 (2025-12-02): Store UUID from ThirdPartyAvidGenerator
                    className = element.className,
                    viewIdResourceName = element.viewIdResourceName,
                    text = element.text,
                    contentDescription = element.contentDescription,
                    bounds = "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                    isClickable = if (element.isClickable) 1L else 0L,
                    isLongClickable = if (element.isLongClickable) 1L else 0L,
                    isEditable = if (element.isEditable) 1L else 0L,
                    isScrollable = if (element.isScrollable) 1L else 0L,
                    isCheckable = if (element.isCheckable) 1L else 0L,
                    isFocusable = if (element.isFocusable) 1L else 0L,
                    isEnabled = if (element.isEnabled) 1L else 0L,
                    depth = element.depth.toLong(),
                    indexInParent = element.indexInParent.toLong(),
                    scrapedAt = timestamp,
                    semanticRole = null,
                    inputType = null,
                    visualWeight = null,
                    isRequired = null,
                    formGroupId = null,
                    placeholderText = null,
                    validationPattern = null,
                    backgroundColor = null,
                    screen_hash = screenHash  // FIX (2025-12-02): Store screen hash for deduplication
                )
                databaseManager.scrapedElements.insert(elementDTO)

                newCount++
                Log.v(TAG, "Persisted element: ${element.elementHash}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist element: ${element.elementHash}", e)
            }
        }

        Log.i(TAG, "Persisted $newCount new elements out of ${elements.size} captured for $packageName")
        newCount
    }

    /**
     * Get app version for fingerprinting
     */
    private fun getAppVersion(packageName: String): String {
        return try {
            val packageInfo = accessibilityService.packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionCode.toString()
        } catch (e: Exception) {
            Log.w(TAG, "Could not get version for $packageName, using 0")
            "0"
        }
    }
}
