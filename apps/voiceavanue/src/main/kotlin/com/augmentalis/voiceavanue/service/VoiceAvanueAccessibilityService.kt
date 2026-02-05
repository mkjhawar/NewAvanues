/**
 * VoiceAvanueAccessibilityService.kt - Unified accessibility service
 *
 * Bridges Android Accessibility API to VoiceOSCore.
 * Handles screen scraping, accessibility actions, and event forwarding.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.rpc.IVoiceOSServiceDelegate
import com.augmentalis.voiceoscore.rpc.messages.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

private const val TAG = "AvaAccessibility"

class VoiceAvanueAccessibilityService : AccessibilityService(), IVoiceOSServiceDelegate {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val eventFlow = MutableSharedFlow<VoiceOSEvent>(replay = 0, extraBufferCapacity = 64)

    private var isRecognizing = false
    private var currentPackage: String? = null
    private var commandCount = 0

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }

        instance = this
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        currentPackage = event.packageName?.toString()

        // Forward relevant events
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                serviceScope.launch {
                    eventFlow.emit(
                        VoiceOSEvent.ActionExecuted(
                            timestamp = System.currentTimeMillis(),
                            actionType = "WINDOW_CHANGED",
                            success = true
                        )
                    )
                }
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                serviceScope.launch {
                    eventFlow.emit(
                        VoiceOSEvent.ActionExecuted(
                            timestamp = System.currentTimeMillis(),
                            actionType = "CLICK",
                            success = true
                        )
                    )
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "Accessibility service destroyed")
    }

    // =========================================================================
    // IVoiceOSServiceDelegate Implementation
    // =========================================================================

    override suspend fun getServiceStatus(): ServiceStatus {
        return ServiceStatus(
            requestId = "",
            isActive = true,
            currentApp = currentPackage,
            isRecognizing = isRecognizing,
            recognizedCommands = commandCount
        )
    }

    override suspend fun executeVoiceCommand(
        commandText: String,
        context: Map<String, String>
    ): VoiceCommandResponse {
        Log.i(TAG, "Executing voice command: $commandText")
        commandCount++

        // TODO: Integrate with VoiceOSCore command processor
        return VoiceCommandResponse(
            requestId = "",
            success = true,
            action = "PROCESSED",
            result = "Command received: $commandText"
        )
    }

    override suspend fun performAction(
        actionType: AccessibilityActionType,
        targetAvid: String?,
        params: Map<String, String>
    ): Boolean {
        Log.i(TAG, "Performing action: $actionType on $targetAvid")

        val rootNode = rootInActiveWindow ?: return false

        return try {
            when (actionType) {
                AccessibilityActionType.CLICK -> {
                    findNodeByAvid(rootNode, targetAvid)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        ?: false
                }
                AccessibilityActionType.LONG_CLICK -> {
                    findNodeByAvid(rootNode, targetAvid)?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                        ?: false
                }
                AccessibilityActionType.SCROLL_FORWARD -> {
                    findNodeByAvid(rootNode, targetAvid)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                        ?: false
                }
                AccessibilityActionType.SCROLL_BACKWARD -> {
                    findNodeByAvid(rootNode, targetAvid)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                        ?: false
                }
                AccessibilityActionType.SET_TEXT -> {
                    val text = params["text"] ?: return false
                    val node = findNodeByAvid(rootNode, targetAvid) ?: return false
                    val args = android.os.Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                    }
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                }
                AccessibilityActionType.FOCUS -> {
                    findNodeByAvid(rootNode, targetAvid)?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        ?: false
                }
                AccessibilityActionType.CLEAR_FOCUS -> {
                    findNodeByAvid(rootNode, targetAvid)?.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS)
                        ?: false
                }
                AccessibilityActionType.SELECT -> {
                    findNodeByAvid(rootNode, targetAvid)?.performAction(AccessibilityNodeInfo.ACTION_SELECT)
                        ?: false
                }
                AccessibilityActionType.DISMISS -> {
                    findNodeByAvid(rootNode, targetAvid)?.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
                        ?: false
                }
                AccessibilityActionType.COPY -> {
                    findNodeByAvid(rootNode, targetAvid)?.performAction(AccessibilityNodeInfo.ACTION_COPY)
                        ?: false
                }
                AccessibilityActionType.PASTE -> {
                    findNodeByAvid(rootNode, targetAvid)?.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                        ?: false
                }
                AccessibilityActionType.CUT -> {
                    findNodeByAvid(rootNode, targetAvid)?.performAction(AccessibilityNodeInfo.ACTION_CUT)
                        ?: false
                }
                AccessibilityActionType.EXPAND -> {
                    findNodeByAvid(rootNode, targetAvid)?.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
                        ?: false
                }
                AccessibilityActionType.COLLAPSE -> {
                    findNodeByAvid(rootNode, targetAvid)?.performAction(AccessibilityNodeInfo.ACTION_COLLAPSE)
                        ?: false
                }
                else -> false
            }
        } finally {
            rootNode.recycle()
        }
    }

    override suspend fun getActionResult(): String? {
        return null // Action results are async via events
    }

    override suspend fun scrapeCurrentScreen(
        includeInvisible: Boolean,
        maxDepth: Int
    ): ScrapeScreenResponse {
        val rootNode = rootInActiveWindow
        val elements = mutableListOf<ScrapedElement>()

        if (rootNode != null) {
            scrapeNode(rootNode, elements, includeInvisible, maxDepth, 0)
            rootNode.recycle()
        }

        return ScrapeScreenResponse(
            requestId = "",
            packageName = currentPackage ?: "",
            activityName = "",
            elements = elements,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun scrapeNode(
        node: AccessibilityNodeInfo,
        elements: MutableList<ScrapedElement>,
        includeInvisible: Boolean,
        maxDepth: Int,
        currentDepth: Int
    ) {
        if (currentDepth > maxDepth) return
        if (!includeInvisible && !node.isVisibleToUser) return

        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)

        elements.add(
            ScrapedElement(
                avid = generateAvid(node),
                className = node.className?.toString() ?: "",
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                bounds = ElementBounds(
                    left = rect.left,
                    top = rect.top,
                    right = rect.right,
                    bottom = rect.bottom
                ),
                isClickable = node.isClickable,
                isScrollable = node.isScrollable,
                isEditable = node.isEditable
            )
        )

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            scrapeNode(child, elements, includeInvisible, maxDepth, currentDepth + 1)
            child.recycle()
        }
    }

    private fun generateAvid(node: AccessibilityNodeInfo): String {
        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString()?.substringAfterLast('.') ?: ""
        val text = node.text?.toString()?.take(20) ?: ""
        return "AVID:${viewId.hashCode().toString(16)}:$className:${text.hashCode().toString(16)}"
    }

    private fun findNodeByAvid(root: AccessibilityNodeInfo, avid: String?): AccessibilityNodeInfo? {
        if (avid == null) return null
        return findNodeRecursive(root, avid)
    }

    private fun findNodeRecursive(node: AccessibilityNodeInfo, avid: String): AccessibilityNodeInfo? {
        if (generateAvid(node) == avid) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeRecursive(child, avid)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    override suspend fun startVoiceRecognition(language: String, continuous: Boolean): Boolean {
        isRecognizing = true
        serviceScope.launch {
            eventFlow.emit(
                VoiceOSEvent.RecognitionStateChanged(
                    timestamp = System.currentTimeMillis(),
                    isRecognizing = true
                )
            )
        }
        return true
    }

    override suspend fun stopVoiceRecognition(): Boolean {
        isRecognizing = false
        serviceScope.launch {
            eventFlow.emit(
                VoiceOSEvent.RecognitionStateChanged(
                    timestamp = System.currentTimeMillis(),
                    isRecognizing = false
                )
            )
        }
        return true
    }

    override fun getRecognitionResults(): Flow<RecognitionResult> {
        // TODO: Integrate with SpeechRecognition module
        return MutableSharedFlow()
    }

    override suspend fun learnCurrentApp(packageName: String?): LearnedAppInfo? {
        val pkg = packageName ?: currentPackage ?: return null
        // TODO: Implement app learning
        return LearnedAppInfo(
            packageName = pkg,
            appName = pkg.substringAfterLast('.'),
            commandCount = 0,
            learnedAt = System.currentTimeMillis()
        )
    }

    override suspend fun getLearnedApps(): List<LearnedAppInfo> {
        // TODO: Integrate with database
        return emptyList()
    }

    override suspend fun getCommandsForApp(packageName: String): List<LearnedCommand> {
        // TODO: Integrate with database
        return emptyList()
    }

    override suspend fun registerDynamicCommand(
        phrase: String,
        actionType: String,
        params: Map<String, String>,
        appPackage: String?
    ): Boolean {
        // TODO: Implement dynamic command registration
        return true
    }

    override suspend fun unregisterDynamicCommand(phrase: String, appPackage: String?): Boolean {
        // TODO: Implement dynamic command unregistration
        return true
    }

    override fun getEventFlow(): Flow<VoiceOSEvent> = eventFlow.asSharedFlow()

    companion object {
        @Volatile
        private var instance: VoiceAvanueAccessibilityService? = null

        fun getInstance(): VoiceAvanueAccessibilityService? = instance

        fun isEnabled(context: Context): Boolean {
            val accessibilityServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val serviceName = "${context.packageName}/${VoiceAvanueAccessibilityService::class.java.canonicalName}"
            return accessibilityServices?.contains(serviceName) == true
        }
    }
}
