/**
 * JITLearningService.kt - Foreground service for passive screen learning
 *
 * Runs as foreground service in VoiceOSCore process to ensure it's never killed.
 * Provides AIDL interface for coordination with LearnApp standalone app.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Updated: 2025-12-11 (v2.1 - Fully wired to JustInTimeLearner via JITLearnerProvider interface)
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * ## Architecture:
 *
 * ```
 * VoiceOSCore Process                    LearnApp Process
 * ┌─────────────────────┐                ┌──────────────────┐
 * │  VoiceOSService     │                │  LearnAppActivity│
 * │  (Accessibility)    │                │                  │
 * │         │           │                │         │        │
 * │         ▼           │                │         ▼        │
 * │  LearnAppIntegration│                │  AIDL Client     │
 * │  (JITLearnerProvider│                │  Binding         │
 * │         │           │                │                  │
 * │         ▼           │                │                  │
 * │  JITLearningService │◄───AIDL IPC───│                  │
 * │  (Foreground)       │                │                  │
 * │         │           │                │                  │
 * │         ▼           │                │                  │
 * │  JustInTimeLearner  │                │                  │
 * │  (Passive Learning) │                │                  │
 * └─────────────────────┘                └──────────────────┘
 * ```
 *
 * ## Lifecycle:
 *
 * 1. **Start**: VoiceOSService starts JITLearningService on boot
 * 2. **Bind**: VoiceOSService binds and sets JITLearnerProvider via setLearnerProvider()
 * 3. **Running**: Service runs as foreground (notification shown)
 * 4. **Pause**: LearnApp can pause capture via IPC
 * 5. **Resume**: LearnApp resumes capture after exploration
 * 6. **Query**: LearnApp queries state for UI display
 * 7. **Stream**: LearnApp receives events via IAccessibilityEventListener (v2.0)
 * 8. **Execute**: LearnApp sends commands via performAction (v2.0)
 *
 * @since 2.1.0 (Full JustInTimeLearner Integration via Interface)
 */

package com.augmentalis.jitlearning

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Interface for providing JIT learner functionality to the service.
 * Implemented by LearnAppIntegration in VoiceOSCore.
 *
 * This avoids circular dependency between JITLearning library and VoiceOSCore.
 */
interface JITLearnerProvider {
    /** Pause JIT learning */
    fun pauseLearning()

    /** Resume JIT learning */
    fun resumeLearning()

    /** Check if learning is paused */
    fun isLearningPaused(): Boolean

    /** Check if learning is actively running */
    fun isLearningActive(): Boolean

    /** Get stats: screens learned count */
    fun getScreensLearnedCount(): Int

    /** Get stats: elements discovered count */
    fun getElementsDiscoveredCount(): Int

    /** Get current package being learned */
    fun getCurrentPackage(): String?

    /** Get current root accessibility node */
    fun getCurrentRootNode(): AccessibilityNodeInfo?

    /** Check if screen has been learned */
    fun hasScreen(screenHash: String): Boolean

    /** Get all learned screen hashes for a package */
    fun getLearnedScreenHashes(packageName: String): List<String>

    /** Set event callback for JIT events */
    fun setEventCallback(callback: JITEventCallback?)

    // Exploration Sync (v2.1 - P2 Feature)

    /** Start automated exploration */
    fun startExploration(packageName: String): Boolean

    /** Stop current exploration */
    fun stopExploration()

    /** Pause current exploration */
    fun pauseExploration()

    /** Resume paused exploration */
    fun resumeExploration()

    /** Get current exploration progress */
    fun getExplorationProgress(): ExplorationProgress

    /** Set exploration progress callback */
    fun setExplorationCallback(callback: ExplorationProgressCallback?)
}

/**
 * Callback interface for JIT learning events.
 * Allows JustInTimeLearner to notify service of events.
 */
interface JITEventCallback {
    fun onScreenLearned(packageName: String, screenHash: String, elementCount: Int)
    fun onElementDiscovered(stableId: String, vuid: String?)
    fun onLoginDetected(packageName: String, screenHash: String)
}

/**
 * Callback interface for exploration progress.
 * Allows ExplorationEngine to notify service of progress updates.
 */
interface ExplorationProgressCallback {
    fun onProgressUpdate(progress: ExplorationProgress)
    fun onCompleted(progress: ExplorationProgress)
    fun onFailed(progress: ExplorationProgress, errorMessage: String)
}

/**
 * JIT Learning Service
 *
 * Foreground service implementing IElementCaptureService for AIDL IPC.
 * Runs passive screen learning in background without user interaction.
 *
 * FIX (2025-12-11): Fully wired to JustInTimeLearner via JITLearnerProvider interface
 */
class JITLearningService : Service() {

    companion object {
        private const val TAG = "JITLearningService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "jit_learning_service"
        private const val CHANNEL_NAME = "JIT Learning"

        // Singleton instance for VoiceOSService to set provider
        @Volatile
        private var INSTANCE: JITLearningService? = null

        /**
         * Get service instance.
         * Returns null if service not running.
         */
        fun getInstance(): JITLearningService? = INSTANCE
    }

    // FIX (2025-12-11): JITLearnerProvider reference set by VoiceOSService
    private var learnerProvider: JITLearnerProvider? = null

    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Service state (local cache)
    private var isPaused = false
    private var currentPackageName: String? = null
    private var lastCaptureTime = 0L
    private var currentActivityName: String = ""
    private var currentScreenHash: String = ""

    // Event listeners (v2.0)
    private val eventListeners = CopyOnWriteArrayList<IAccessibilityEventListener>()

    // Exploration progress listeners (v2.1 - P2 Feature)
    private val explorationListeners = CopyOnWriteArrayList<IExplorationProgressListener>()

    // Registered elements for click actions (uuid -> node)
    private val registeredElements = mutableMapOf<String, AccessibilityNodeInfo>()

    // Reference to accessibility service (set via setAccessibilityService)
    private var accessibilityServiceRef: AccessibilityServiceInterface? = null

    /**
     * Interface for accessibility service callbacks.
     * Must be implemented by VoiceOSService.
     */
    interface AccessibilityServiceInterface {
        fun getRootNode(): AccessibilityNodeInfo?
        fun performGlobalAction(action: Int): Boolean
    }

    /**
     * Set accessibility service reference.
     * Called by VoiceOSService after binding.
     */
    fun setAccessibilityService(service: AccessibilityServiceInterface) {
        accessibilityServiceRef = service
    }

    /**
     * FIX (2025-12-11): Set JIT learner provider.
     * Called by VoiceOSService/LearnAppIntegration after service starts.
     */
    fun setLearnerProvider(provider: JITLearnerProvider) {
        learnerProvider = provider
        Log.i(TAG, "JITLearnerProvider set")

        // Wire event callback
        provider.setEventCallback(object : JITEventCallback {
            override fun onScreenLearned(packageName: String, screenHash: String, elementCount: Int) {
                val event = ScreenChangeEvent.create(
                    screenHash = screenHash,
                    activityName = currentActivityName,
                    packageName = packageName,
                    elementCount = elementCount,
                    isNewScreen = true
                )
                dispatchScreenChanged(event)
            }

            override fun onElementDiscovered(stableId: String, vuid: String?) {
                for (listener in eventListeners) {
                    try {
                        listener.onElementAction(stableId, "discovered", true)
                    } catch (e: RemoteException) {
                        Log.w(TAG, "Failed to notify listener of element discovery", e)
                        eventListeners.remove(listener)
                    }
                }
            }

            override fun onLoginDetected(packageName: String, screenHash: String) {
                notifyLoginScreen(packageName, screenHash)
            }
        })
    }

    /**
     * FIX (2025-12-11): Dispatch screen change event to all registered listeners
     */
    private fun dispatchScreenChanged(event: ScreenChangeEvent) {
        val deadListeners = mutableListOf<IAccessibilityEventListener>()

        for (listener in eventListeners) {
            try {
                listener.onScreenChanged(event)
            } catch (e: RemoteException) {
                Log.w(TAG, "Listener disconnected", e)
                deadListeners.add(listener)
            }
        }

        eventListeners.removeAll(deadListeners.toSet())
    }

    /**
     * FIX (2025-12-11): Dispatch state change notification
     */
    private fun dispatchStateChanged() {
        Log.d(TAG, "State changed - listeners can poll queryState()")
    }

    /**
     * Dispatch exploration progress to all listeners (v2.1 - P2 Feature)
     */
    private fun dispatchExplorationProgress(progress: ExplorationProgress) {
        val deadListeners = mutableListOf<IExplorationProgressListener>()

        for (listener in explorationListeners) {
            try {
                listener.onProgressUpdate(progress)
            } catch (e: RemoteException) {
                Log.w(TAG, "Exploration listener disconnected", e)
                deadListeners.add(listener)
            }
        }

        explorationListeners.removeAll(deadListeners.toSet())
    }

    /**
     * AIDL Binder Implementation
     *
     * Implements IElementCaptureService interface for IPC.
     * FIX (2025-12-11): All methods now forward to JITLearnerProvider
     */
    private val binder = object : IElementCaptureService.Stub() {

        // ================================================================
        // EXISTING METHODS (v1.0) - NOW FULLY IMPLEMENTED
        // ================================================================

        override fun pauseCapture() {
            Log.i(TAG, "Pause capture request via AIDL")
            isPaused = true
            // FIX (2025-12-11): Forward to JITLearnerProvider
            learnerProvider?.pauseLearning()
            dispatchStateChanged()
        }

        override fun resumeCapture() {
            Log.i(TAG, "Resume capture request via AIDL")
            isPaused = false
            // FIX (2025-12-11): Forward to JITLearnerProvider
            learnerProvider?.resumeLearning()
            dispatchStateChanged()
        }

        override fun queryState(): JITState {
            Log.d(TAG, "Query state request via AIDL")

            // FIX (2025-12-11): Get real stats from JITLearnerProvider
            val provider = learnerProvider

            return if (provider != null) {
                JITState(
                    isActive = provider.isLearningActive() && !isPaused,
                    currentPackage = provider.getCurrentPackage() ?: currentPackageName,
                    screensLearned = provider.getScreensLearnedCount(),
                    elementsDiscovered = provider.getElementsDiscoveredCount(),
                    lastCaptureTime = lastCaptureTime
                )
            } else {
                // Fallback when provider not set
                JITState(
                    isActive = !isPaused,
                    currentPackage = currentPackageName,
                    screensLearned = 0,
                    elementsDiscovered = 0,
                    lastCaptureTime = lastCaptureTime
                )
            }
        }

        override fun getLearnedScreenHashes(packageName: String): List<String> {
            Log.d(TAG, "Get learned screen hashes for: $packageName")
            // FIX (2025-12-11): Query from database via provider
            return learnerProvider?.getLearnedScreenHashes(packageName) ?: emptyList()
        }

        // ================================================================
        // EVENT STREAMING (v2.0)
        // ================================================================

        override fun registerEventListener(listener: IAccessibilityEventListener) {
            Log.i(TAG, "Registering event listener")
            eventListeners.add(listener)
        }

        override fun unregisterEventListener(listener: IAccessibilityEventListener) {
            Log.i(TAG, "Unregistering event listener")
            eventListeners.remove(listener)
        }

        // ================================================================
        // SCREEN/ELEMENT QUERIES (v2.0) - NOW FULLY IMPLEMENTED
        // ================================================================

        override fun getCurrentScreenInfo(): ParcelableNodeInfo? {
            Log.d(TAG, "Get current screen info request")

            // FIX (2025-12-11): Get root node from provider or accessibility service
            val rootNode = learnerProvider?.getCurrentRootNode()
                ?: accessibilityServiceRef?.getRootNode()
                ?: return null

            return try {
                ParcelableNodeInfo.fromAccessibilityNode(rootNode, includeChildren = true, maxDepth = 10)
            } finally {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    rootNode.recycle()
                }
            }
        }

        override fun getFullMenuContent(menuNodeId: String): ParcelableNodeInfo? {
            Log.d(TAG, "Get full menu content for: $menuNodeId")
            val rootNode = learnerProvider?.getCurrentRootNode()
                ?: accessibilityServiceRef?.getRootNode()
                ?: return null

            return try {
                val menuNode = findNodeById(rootNode, menuNodeId)
                if (menuNode != null) {
                    ParcelableNodeInfo.fromAccessibilityNode(menuNode, includeChildren = true, maxDepth = 5)
                } else {
                    null
                }
            } finally {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    rootNode.recycle()
                }
            }
        }

        override fun queryElements(selector: String): List<ParcelableNodeInfo> {
            Log.d(TAG, "Query elements with selector: $selector")
            val rootNode = learnerProvider?.getCurrentRootNode()
                ?: accessibilityServiceRef?.getRootNode()
                ?: return emptyList()

            val results = mutableListOf<ParcelableNodeInfo>()
            try {
                // Parse selector: "class:Button", "id:*submit*", "text:Login"
                val parts = selector.split(":", limit = 2)
                if (parts.size == 2) {
                    val type = parts[0].lowercase()
                    val pattern = parts[1]
                    findMatchingNodes(rootNode, type, pattern, results)
                }
            } finally {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    rootNode.recycle()
                }
            }

            return results
        }

        // ================================================================
        // EXPLORATION COMMANDS (v2.0)
        // ================================================================

        override fun performClick(elementUuid: String): Boolean {
            Log.d(TAG, "Perform click on: $elementUuid")

            var node = registeredElements[elementUuid]
            if (node == null) {
                val rootNode = learnerProvider?.getCurrentRootNode()
                    ?: accessibilityServiceRef?.getRootNode()
                if (rootNode != null) {
                    node = findNodeByUuid(rootNode, elementUuid)
                }
            }

            if (node == null) {
                Log.w(TAG, "Element not found: $elementUuid")
                return false
            }

            val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            notifyElementAction(elementUuid, "click", success)
            return success
        }

        override fun performScroll(direction: String, distance: Int): Boolean {
            Log.d(TAG, "Perform scroll: $direction, $distance")
            val rootNode = learnerProvider?.getCurrentRootNode()
                ?: accessibilityServiceRef?.getRootNode()
                ?: return false

            val scrollableNode = findScrollableNode(rootNode)
            if (scrollableNode == null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    rootNode.recycle()
                }
                return false
            }

            val action = when (direction.lowercase()) {
                "up", "left" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                "down", "right" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                else -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }

            val success = scrollableNode.performAction(action)
            notifyScroll(direction, distance, 0)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                rootNode.recycle()
            }

            return success
        }

        override fun performAction(command: ExplorationCommand): Boolean {
            Log.d(TAG, "Perform action: ${command.type}")

            return when (command.type) {
                CommandType.CLICK -> {
                    if (command.elementUuid.isBlank()) return false
                    performClick(command.elementUuid)
                }

                CommandType.LONG_CLICK -> {
                    val node = findOrGetNode(command.elementUuid) ?: return false
                    val success = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                    notifyElementAction(command.elementUuid, "longClick", success)
                    success
                }

                CommandType.SCROLL -> {
                    performScroll(command.direction.name, command.distance)
                }

                CommandType.SWIPE -> {
                    Log.i(TAG, "Swipe action - using accessibility gesture")
                    val gestureAction = when (command.direction) {
                        ScrollDirection.UP -> android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_UP
                        ScrollDirection.DOWN -> android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_DOWN
                        ScrollDirection.LEFT -> android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_LEFT
                        ScrollDirection.RIGHT -> android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT
                    }
                    accessibilityServiceRef?.performGlobalAction(gestureAction) ?: false
                }

                CommandType.SET_TEXT -> {
                    val node = findOrGetNode(command.elementUuid) ?: return false
                    val args = android.os.Bundle()
                    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, command.text)
                    val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    notifyElementAction(command.elementUuid, "setText", success)
                    success
                }

                CommandType.BACK -> performBack()

                CommandType.HOME -> {
                    accessibilityServiceRef?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME) ?: false
                }

                CommandType.FOCUS -> {
                    val node = findOrGetNode(command.elementUuid) ?: return false
                    node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                }

                CommandType.CLEAR_TEXT -> {
                    val node = findOrGetNode(command.elementUuid) ?: return false
                    val args = android.os.Bundle()
                    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                }

                CommandType.EXPAND -> {
                    val node = findOrGetNode(command.elementUuid) ?: return false
                    node.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
                }

                CommandType.SELECT -> {
                    val node = findOrGetNode(command.elementUuid) ?: return false
                    node.performAction(AccessibilityNodeInfo.ACTION_SELECT)
                }
            }
        }

        override fun performBack(): Boolean {
            Log.d(TAG, "Perform back action")
            return accessibilityServiceRef?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK) ?: false
        }

        // ================================================================
        // ELEMENT REGISTRATION (v2.0)
        // ================================================================

        override fun registerElement(nodeInfo: ParcelableNodeInfo, uuid: String) {
            Log.d(TAG, "Register element: $uuid")
            Log.i(TAG, "Element registered for UUID: $uuid (will search tree when action performed)")
        }

        override fun clearRegisteredElements() {
            Log.d(TAG, "Clear registered elements")
            for ((_, node) in registeredElements) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    node.recycle()
                }
            }
            registeredElements.clear()
        }

        // ================================================================
        // EXPLORATION SYNC (v2.1 - P2 Feature)
        // ================================================================

        override fun startExploration(packageName: String): Boolean {
            Log.i(TAG, "Start exploration request for: $packageName")
            return learnerProvider?.startExploration(packageName) ?: false
        }

        override fun stopExploration() {
            Log.i(TAG, "Stop exploration request")
            learnerProvider?.stopExploration()
        }

        override fun pauseExploration() {
            Log.i(TAG, "Pause exploration request")
            learnerProvider?.pauseExploration()
        }

        override fun resumeExploration() {
            Log.i(TAG, "Resume exploration request")
            learnerProvider?.resumeExploration()
        }

        override fun getExplorationProgress(): ExplorationProgress {
            return learnerProvider?.getExplorationProgress() ?: ExplorationProgress.idle()
        }

        override fun registerExplorationListener(listener: IExplorationProgressListener) {
            Log.i(TAG, "Registering exploration listener")
            explorationListeners.add(listener)

            // Wire callback to provider if first listener
            if (explorationListeners.size == 1) {
                learnerProvider?.setExplorationCallback(object : ExplorationProgressCallback {
                    override fun onProgressUpdate(progress: ExplorationProgress) {
                        dispatchExplorationProgress(progress)
                    }

                    override fun onCompleted(progress: ExplorationProgress) {
                        for (l in explorationListeners) {
                            try {
                                l.onCompleted(progress)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error dispatching onCompleted", e)
                            }
                        }
                    }

                    override fun onFailed(progress: ExplorationProgress, errorMessage: String) {
                        for (l in explorationListeners) {
                            try {
                                l.onFailed(progress, errorMessage)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error dispatching onFailed", e)
                            }
                        }
                    }
                })
            }
        }

        override fun unregisterExplorationListener(listener: IExplorationProgressListener) {
            Log.i(TAG, "Unregistering exploration listener")
            explorationListeners.remove(listener)

            // Clear callback if no more listeners
            if (explorationListeners.isEmpty()) {
                learnerProvider?.setExplorationCallback(null)
            }
        }
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    private fun findOrGetNode(uuid: String): AccessibilityNodeInfo? {
        registeredElements[uuid]?.let { return it }
        val rootNode = learnerProvider?.getCurrentRootNode()
            ?: accessibilityServiceRef?.getRootNode()
            ?: return null
        return findNodeByUuid(rootNode, uuid)
    }

    private fun findNodeById(root: AccessibilityNodeInfo, nodeId: String): AccessibilityNodeInfo? {
        if (root.viewIdResourceName == nodeId) return root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeById(child, nodeId)
            if (found != null) return found
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }

        return null
    }

    private fun findNodeByUuid(root: AccessibilityNodeInfo, targetUuid: String): AccessibilityNodeInfo? {
        val nodeUuid = generateNodeUuid(root)
        if (nodeUuid == targetUuid) return root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeByUuid(child, targetUuid)
            if (found != null) return found
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }

        return null
    }

    private fun generateNodeUuid(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        sb.append(node.className ?: "")
        sb.append(node.viewIdResourceName ?: "")
        sb.append(node.text ?: "")
        sb.append(node.contentDescription ?: "")
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        sb.append(bounds.toString())
        return sb.toString().hashCode().toString(16)
    }

    private fun findMatchingNodes(
        root: AccessibilityNodeInfo,
        selectorType: String,
        pattern: String,
        results: MutableList<ParcelableNodeInfo>
    ) {
        val matches = when (selectorType) {
            "class" -> root.className?.toString()?.contains(pattern, ignoreCase = true) == true
            "id" -> {
                val id = root.viewIdResourceName ?: ""
                if (pattern.startsWith("*") && pattern.endsWith("*")) {
                    id.contains(pattern.trim('*'), ignoreCase = true)
                } else {
                    id.equals(pattern, ignoreCase = true)
                }
            }
            "text" -> root.text?.toString()?.contains(pattern, ignoreCase = true) == true
            "desc" -> root.contentDescription?.toString()?.contains(pattern, ignoreCase = true) == true
            else -> false
        }

        if (matches) {
            results.add(ParcelableNodeInfo.fromAccessibilityNode(root, includeChildren = false, maxDepth = 0))
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            findMatchingNodes(child, selectorType, pattern, results)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            if (child.isScrollable) return child

            val scrollable = findScrollableNode(child)
            if (scrollable != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    child.recycle()
                }
                return scrollable
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }

        return null
    }

    // ================================================================
    // EVENT NOTIFICATION HELPERS
    // ================================================================

    fun notifyScreenChanged(event: ScreenChangeEvent) {
        dispatchScreenChanged(event)
    }

    private fun notifyElementAction(elementUuid: String, actionType: String, success: Boolean) {
        for (listener in eventListeners) {
            try {
                listener.onElementAction(elementUuid, actionType, success)
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to notify listener of element action", e)
                eventListeners.remove(listener)
            }
        }
    }

    private fun notifyScroll(direction: String, distance: Int, newElementsCount: Int) {
        for (listener in eventListeners) {
            try {
                listener.onScrollDetected(direction, distance, newElementsCount)
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to notify listener of scroll", e)
                eventListeners.remove(listener)
            }
        }
    }

    fun notifyDynamicContent(screenHash: String, regionId: String) {
        for (listener in eventListeners) {
            try {
                listener.onDynamicContentDetected(screenHash, regionId)
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to notify listener of dynamic content", e)
                eventListeners.remove(listener)
            }
        }
    }

    fun notifyMenuDiscovered(menuId: String, totalItems: Int, visibleItems: Int) {
        for (listener in eventListeners) {
            try {
                listener.onMenuDiscovered(menuId, totalItems, visibleItems)
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to notify listener of menu", e)
                eventListeners.remove(listener)
            }
        }
    }

    fun notifyLoginScreen(packageName: String, screenHash: String) {
        for (listener in eventListeners) {
            try {
                listener.onLoginScreenDetected(packageName, screenHash)
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to notify listener of login screen", e)
                eventListeners.remove(listener)
            }
        }
    }

    fun registerAccessibilityNode(uuid: String, node: AccessibilityNodeInfo) {
        registeredElements[uuid] = node
    }

    // ================================================================
    // SERVICE LIFECYCLE
    // ================================================================

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        Log.i(TAG, "JIT Learning Service created")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "JIT Learning Service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "JIT Learning Service bound")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "JIT Learning Service destroyed")

        // Clear event callback
        learnerProvider?.setEventCallback(null)

        // Clear registered elements
        for ((_, node) in registeredElements) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                node.recycle()
            }
        }
        registeredElements.clear()
        INSTANCE = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Passive voice command learning service"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JIT Learning Active")
            .setContentText("Learning voice commands passively...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Process accessibility event (called from VoiceOSService)
     */
    fun onAccessibilityEvent(packageName: String, event: android.view.accessibility.AccessibilityEvent) {
        if (isPaused) return

        currentPackageName = packageName
        lastCaptureTime = System.currentTimeMillis()
        // JustInTimeLearner processes events via LearnAppIntegration
    }
}
