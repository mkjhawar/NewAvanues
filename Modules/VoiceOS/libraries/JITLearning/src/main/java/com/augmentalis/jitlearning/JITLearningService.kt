/**
 * JITLearningService.kt - Foreground service for passive screen learning
 *
 * Runs as foreground service in VoiceOSCore process to ensure it's never killed.
 * Provides AIDL interface for coordination with LearnApp standalone app.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Updated: 2025-12-11 (v2.0 - Event streaming + exploration commands)
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
 * │  JITLearningService │◄───AIDL IPC───│  AIDL Client     │
 * │  (Foreground)       │                │  Binding         │
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
 * 2. **Bind**: VoiceOSService binds to forward accessibility events
 * 3. **Running**: Service runs as foreground (notification shown)
 * 4. **Pause**: LearnApp can pause capture via IPC
 * 5. **Resume**: LearnApp resumes capture after exploration
 * 6. **Query**: LearnApp queries state for UI display
 * 7. **Stream**: LearnApp receives events via IAccessibilityEventListener (v2.0)
 * 8. **Execute**: LearnApp sends commands via performAction (v2.0)
 *
 * @since 2.0.0 (JIT-LearnApp Separation)
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

/**
 * JIT Learning Service
 *
 * Foreground service implementing IElementCaptureService for AIDL IPC.
 * Runs passive screen learning in background without user interaction.
 */
class JITLearningService : Service() {

    companion object {
        private const val TAG = "JITLearningService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "jit_learning_service"
        private const val CHANNEL_NAME = "JIT Learning"
    }

    // Service state
    private var isPaused = false
    private var screensLearned = 0
    private var elementsDiscovered = 0
    private var currentPackageName: String? = null
    private var lastCaptureTime = 0L
    private var currentActivityName: String = ""
    private var currentScreenHash: String = ""

    // Event listeners (v2.0)
    private val eventListeners = CopyOnWriteArrayList<IAccessibilityEventListener>()

    // Registered elements for click actions (uuid -> node)
    private val registeredElements = mutableMapOf<String, AccessibilityNodeInfo>()

    // Reference to accessibility service (set via setAccessibilityService)
    private var accessibilityServiceRef: AccessibilityServiceInterface? = null

    // TODO: Integrate JustInTimeLearner in Phase 4
    // private lateinit var jitLearner: JustInTimeLearner

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
     * AIDL Binder Implementation
     *
     * Implements IElementCaptureService interface for IPC.
     */
    private val binder = object : IElementCaptureService.Stub() {

        // ================================================================
        // EXISTING METHODS (v1.0)
        // ================================================================

        override fun pauseCapture() {
            Log.i(TAG, "Pause capture request via AIDL")
            isPaused = true
            // TODO: Call jitLearner.pause() in Phase 4
        }

        override fun resumeCapture() {
            Log.i(TAG, "Resume capture request via AIDL")
            isPaused = false
            // TODO: Call jitLearner.resume() in Phase 4
        }

        override fun queryState(): JITState {
            Log.d(TAG, "Query state request via AIDL")
            return JITState(
                isActive = !isPaused,
                currentPackage = currentPackageName,
                screensLearned = screensLearned,
                elementsDiscovered = elementsDiscovered,
                lastCaptureTime = lastCaptureTime
            )
        }

        override fun getLearnedScreenHashes(packageName: String): List<String> {
            Log.d(TAG, "Get learned screen hashes for: $packageName")
            // TODO: Query database for screen hashes in Phase 4
            return emptyList()
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
        // SCREEN/ELEMENT QUERIES (v2.0)
        // ================================================================

        override fun getCurrentScreenInfo(): ParcelableNodeInfo? {
            Log.d(TAG, "Get current screen info request")
            val rootNode = accessibilityServiceRef?.getRootNode() ?: return null

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
            // TODO: Find menu node and get all children
            // This requires traversing the tree to find the menu by ID
            return null
        }

        override fun queryElements(selector: String): List<ParcelableNodeInfo> {
            Log.d(TAG, "Query elements with selector: $selector")
            // TODO: Implement selector-based element search
            // Selectors: "class:Button", "id:*submit*", "text:Login"
            return emptyList()
        }

        // ================================================================
        // EXPLORATION COMMANDS (v2.0)
        // ================================================================

        override fun performClick(elementUuid: String): Boolean {
            Log.d(TAG, "Perform click on: $elementUuid")
            val node = registeredElements[elementUuid]
            if (node == null) {
                Log.w(TAG, "Element not registered: $elementUuid")
                return false
            }

            val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            notifyElementAction(elementUuid, "click", success)
            return success
        }

        override fun performScroll(direction: String, distance: Int): Boolean {
            Log.d(TAG, "Perform scroll: $direction, $distance")
            val rootNode = accessibilityServiceRef?.getRootNode() ?: return false

            // Find scrollable node
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
            notifyScroll(direction, distance, 0) // newElementsCount calculated later

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
                    val node = registeredElements[command.elementUuid] ?: return false
                    val success = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                    notifyElementAction(command.elementUuid, "longClick", success)
                    success
                }

                CommandType.SCROLL -> {
                    performScroll(command.direction.name, command.distance)
                }

                CommandType.SWIPE -> {
                    // TODO: Implement gesture dispatch
                    Log.w(TAG, "Swipe not yet implemented")
                    false
                }

                CommandType.SET_TEXT -> {
                    val node = registeredElements[command.elementUuid] ?: return false
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
                    val node = registeredElements[command.elementUuid] ?: return false
                    node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                }

                CommandType.CLEAR_TEXT -> {
                    val node = registeredElements[command.elementUuid] ?: return false
                    val args = android.os.Bundle()
                    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                }

                CommandType.EXPAND -> {
                    val node = registeredElements[command.elementUuid] ?: return false
                    node.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
                }

                CommandType.SELECT -> {
                    val node = registeredElements[command.elementUuid] ?: return false
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
            // Note: We store ParcelableNodeInfo but need actual AccessibilityNodeInfo for actions
            // This requires the element to be found again when action is performed
            // For now, store a placeholder that signals the element should be found
            // TODO: Implement proper element lookup in Phase 4
        }

        override fun clearRegisteredElements() {
            Log.d(TAG, "Clear registered elements")
            // Recycle nodes before clearing
            for ((_, node) in registeredElements) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    node.recycle()
                }
            }
            registeredElements.clear()
        }
    }

    // ================================================================
    // EVENT NOTIFICATION HELPERS (v2.0)
    // ================================================================

    /**
     * Notify all listeners of screen change.
     */
    fun notifyScreenChanged(event: ScreenChangeEvent) {
        for (listener in eventListeners) {
            try {
                listener.onScreenChanged(event)
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to notify listener of screen change", e)
                eventListeners.remove(listener)
            }
        }
    }

    /**
     * Notify all listeners of element action.
     */
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

    /**
     * Notify all listeners of scroll.
     */
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

    /**
     * Notify all listeners of dynamic content.
     */
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

    /**
     * Notify all listeners of menu discovery.
     */
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

    /**
     * Notify all listeners of login screen.
     */
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

    // ================================================================
    // HELPER METHODS
    // ================================================================

    /**
     * Find first scrollable node in tree.
     */
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

    /**
     * Register accessibility node for later action.
     * Called internally when capturing elements.
     */
    fun registerAccessibilityNode(uuid: String, node: AccessibilityNodeInfo) {
        registeredElements[uuid] = node
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "JIT Learning Service created")

        // Create notification channel
        createNotificationChannel()

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())

        // TODO: Initialize JustInTimeLearner in Phase 4
        // jitLearner = JustInTimeLearner(...)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "JIT Learning Service started")
        return START_STICKY  // Auto-restart if killed
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "JIT Learning Service bound")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "JIT Learning Service destroyed")
        // TODO: Cleanup JustInTimeLearner in Phase 4
    }

    /**
     * Create notification channel for foreground service
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW  // Low importance to avoid interruptions
            ).apply {
                description = "Passive voice command learning service"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Create foreground service notification
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JIT Learning Active")
            .setContentText("Learning voice commands passively...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)  // TODO: Use proper icon
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Process accessibility event (called from VoiceOSService)
     *
     * TODO: Implement in Phase 4
     */
    fun onAccessibilityEvent(packageName: String, event: android.view.accessibility.AccessibilityEvent) {
        if (isPaused) return

        // TODO: Forward to JustInTimeLearner in Phase 4
        // jitLearner.onAccessibilityEvent(packageName, event)

        // Update state
        currentPackageName = packageName
        lastCaptureTime = System.currentTimeMillis()
    }
}
