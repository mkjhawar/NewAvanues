/**
 * VoiceOSRpcServer.kt - Android gRPC server implementation
 *
 * Implements the VoiceOS RPC server using gRPC-Kotlin.
 * Handles incoming requests and delegates to IVoiceOSServiceDelegate.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore.rpc

import android.util.Log
import com.augmentalis.voiceoscore.rpc.messages.*
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import java.util.concurrent.TimeUnit

private const val TAG = "VoiceOSRpcServer"

/**
 * Android gRPC server for VoiceOS service
 */
actual class VoiceOSRpcServer actual constructor(
    private val delegate: IVoiceOSServiceDelegate,
    private val config: VoiceOSServerConfig
) {
    private var server: Server? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Start the gRPC server
     */
    actual fun start() {
        if (server != null) {
            Log.w(TAG, "Server already running")
            return
        }

        try {
            val serviceImpl = VoiceOSServiceImpl(delegate, scope)

            server = ServerBuilder
                .forPort(config.port)
                .addService(serviceImpl)
                .maxInboundMessageSize(16 * 1024 * 1024) // 16MB
                .build()
                .start()

            Log.i(TAG, "VoiceOS gRPC server started on port ${config.port}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start gRPC server", e)
            throw e
        }
    }

    /**
     * Stop the gRPC server
     */
    actual fun stop() {
        scope.cancel()
        server?.let { s ->
            try {
                s.shutdown()
                if (!s.awaitTermination(5, TimeUnit.SECONDS)) {
                    s.shutdownNow()
                }
                Log.i(TAG, "VoiceOS gRPC server stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping gRPC server", e)
            }
        }
        server = null
    }

    /**
     * Check if server is running
     */
    actual fun isRunning(): Boolean = server?.isShutdown == false
}

/**
 * gRPC service implementation that delegates to IVoiceOSServiceDelegate
 *
 * Uses Kotlin-native protocol types (defined below) with builder pattern,
 * providing full gRPC service without protoc code generation.
 */
internal class VoiceOSServiceImpl(
    private val delegate: IVoiceOSServiceDelegate,
    private val scope: CoroutineScope
) : VoiceOSServiceGrpcKt.VoiceOSServiceCoroutineImplBase() {

    override suspend fun getStatus(request: ServiceStatusRequestProto): ServiceStatusProto {
        val status = delegate.getServiceStatus()

        return ServiceStatusProto.newBuilder()
            .setRequestId(request.requestId)
            .setIsReady(status.isReady)
            .setIsAccessibilityEnabled(status.isAccessibilityEnabled)
            .setIsVoiceRecognitionActive(status.isVoiceRecognitionActive)
            .setCurrentLanguage(status.currentLanguage)
            .setVersion(status.version)
            .addAllCapabilities(status.capabilities)
            .build()
    }

    override suspend fun executeCommand(request: VoiceCommandRequestProto): VoiceCommandResponseProto {
        val response = delegate.executeVoiceCommand(
            commandText = request.commandText,
            context = request.contextMap
        )

        return VoiceCommandResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(response.success)
            .setAction(response.action ?: "")
            .setResult(response.result ?: "")
            .setConfidence(response.confidence)
            .setError(response.error ?: "")
            .build()
    }

    override suspend fun executeAction(request: AccessibilityActionRequestProto): AccessibilityActionResponseProto {
        val actionType = AccessibilityActionType.valueOf(request.actionTypeName)
        val success = delegate.performAction(
            actionType = actionType,
            targetAvid = request.targetAvid.takeIf { it.isNotEmpty() },
            params = request.paramsMap
        )
        val result = delegate.getActionResult()

        return AccessibilityActionResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(success)
            .setResultText(result ?: "")
            .build()
    }

    override suspend fun scrapeScreen(request: ScrapeScreenRequestProto): ScrapeScreenResponseProto {
        val response = delegate.scrapeCurrentScreen(
            includeInvisible = request.includeInvisible,
            maxDepth = request.maxDepth
        )

        return ScrapeScreenResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(response.success)
            .setPackageName(response.packageName)
            .setActivityName(response.activityName)
            .addAllElements(response.elements.map { it.toProto() })
            .build()
    }

    override suspend fun startRecognition(request: StartRecognitionRequestProto): VoiceOSResponseProto {
        val success = delegate.startVoiceRecognition(
            language = request.language,
            continuous = request.continuous
        )

        return VoiceOSResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(success)
            .build()
    }

    override suspend fun stopRecognition(request: StopRecognitionRequestProto): VoiceOSResponseProto {
        val success = delegate.stopVoiceRecognition()

        return VoiceOSResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(success)
            .build()
    }

    override suspend fun learnApp(request: LearnAppRequestProto): LearnedAppInfoProtoMsg {
        val appInfo = delegate.learnCurrentApp(
            packageName = request.packageName.takeIf { it.isNotEmpty() }
        )

        return LearnedAppInfoProtoMsg.newBuilder()
            .setRequestId(request.requestId)
            .apply {
                appInfo?.let {
                    setPackageName(it.packageName)
                    setAppName(it.appName)
                    setScreenCount(it.screenCount)
                    setElementCount(it.elementCount)
                    setCommandCount(it.commandCount)
                    setLearnedAt(it.learnedAt)
                }
            }
            .build()
    }

    override suspend fun getLearnedApps(request: GetLearnedAppsRequestProto): LearnedAppsResponseProto {
        val apps = delegate.getLearnedApps()

        return LearnedAppsResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .addAllApps(apps.map { it.toProto() })
            .build()
    }

    override suspend fun getCommands(request: GetCommandsRequestProto): AppCommandsResponseProto {
        val commands = delegate.getCommandsForApp(request.packageName)

        return AppCommandsResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setPackageName(request.packageName)
            .addAllCommands(commands.map { it.toProto() })
            .build()
    }

    override suspend fun registerCommand(request: RegisterCommandRequestProto): VoiceOSResponseProto {
        val success = delegate.registerDynamicCommand(
            phrase = request.phrase,
            actionType = request.actionType,
            params = request.actionParams,
            appPackage = request.appPackage.takeIf { it.isNotEmpty() }
        )

        return VoiceOSResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(success)
            .build()
    }

    override suspend fun unregisterCommand(request: UnregisterCommandRequestProto): VoiceOSResponseProto {
        val success = delegate.unregisterDynamicCommand(
            phrase = request.phrase,
            appPackage = request.appPackage.takeIf { it.isNotEmpty() }
        )

        return VoiceOSResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(success)
            .build()
    }

    override fun streamEvents(request: StreamEventsRequestProto): kotlinx.coroutines.flow.Flow<VoiceOSEventProto> {
        return kotlinx.coroutines.flow.flow {
            delegate.getEventFlow()
                .catch { e -> Log.e(TAG, "Event stream error", e) }
                .collect { event ->
                    emit(event.toProto())
                }
        }
    }

    // Extension functions to convert between KMP types and Proto types
    private fun ScreenElement.toProto(): ScreenElementProto = ScreenElementProto.newBuilder()
        .setAvid(avid)
        .setClassName(className)
        .setText(text)
        .setContentDescription(contentDescription)
        .setBoundsLeft(bounds.left)
        .setBoundsTop(bounds.top)
        .setBoundsRight(bounds.right)
        .setBoundsBottom(bounds.bottom)
        .setIsClickable(isClickable)
        .setIsScrollable(isScrollable)
        .setIsEditable(isEditable)
        .build()

    private fun LearnedAppInfo.toProto(): LearnedAppInfoProtoMsg = LearnedAppInfoProtoMsg.newBuilder()
        .setPackageName(packageName)
        .setAppName(appName)
        .setScreenCount(screenCount)
        .setElementCount(elementCount)
        .setCommandCount(commandCount)
        .setLearnedAt(learnedAt)
        .build()

    private fun LearnedCommand.toProto(): LearnedCommandProto = LearnedCommandProto.newBuilder()
        .setPhrase(phrase)
        .setActionType(actionType)
        .setTargetAvid(targetAvid)
        .setConfidence(confidence)
        .build()

    private fun VoiceOSEvent.toProto(): VoiceOSEventProto = when (this) {
        is VoiceOSEvent.RecognitionStarted -> VoiceOSEventProto.newBuilder()
            .setTimestamp(timestamp)
            .setEventType("RECOGNITION_STARTED")
            .setLanguage(language)
            .build()
        is VoiceOSEvent.RecognitionResult -> VoiceOSEventProto.newBuilder()
            .setTimestamp(timestamp)
            .setEventType("RECOGNITION_RESULT")
            .setTranscript(transcript)
            .setConfidence(confidence)
            .setIsFinal(isFinal)
            .build()
        is VoiceOSEvent.RecognitionStopped -> VoiceOSEventProto.newBuilder()
            .setTimestamp(timestamp)
            .setEventType("RECOGNITION_STOPPED")
            .setReason(reason)
            .build()
        is VoiceOSEvent.CommandExecuted -> VoiceOSEventProto.newBuilder()
            .setTimestamp(timestamp)
            .setEventType("COMMAND_EXECUTED")
            .setCommand(command)
            .setSuccess(success)
            .setResult(result ?: "")
            .build()
        is VoiceOSEvent.ScreenChanged -> VoiceOSEventProto.newBuilder()
            .setTimestamp(timestamp)
            .setEventType("SCREEN_CHANGED")
            .setPackageName(packageName)
            .setActivityName(activityName)
            .build()
        is VoiceOSEvent.AccessibilityStateChanged -> VoiceOSEventProto.newBuilder()
            .setTimestamp(timestamp)
            .setEventType("ACCESSIBILITY_STATE_CHANGED")
            .setIsEnabled(isEnabled)
            .build()
    }
}

// ============================================================================
// gRPC Protocol Types - Kotlin-native implementations with builder pattern.
// These are complete implementations used in lieu of protoc code generation.
// ============================================================================

/**
 * gRPC service definition with abstract coroutine-based service implementation.
 */
object VoiceOSServiceGrpcKt {
    abstract class VoiceOSServiceCoroutineImplBase : io.grpc.BindableService {
        open suspend fun getStatus(request: ServiceStatusRequestProto): ServiceStatusProto =
            throw UnsupportedOperationException("getStatus not implemented")
        open suspend fun executeCommand(request: VoiceCommandRequestProto): VoiceCommandResponseProto =
            throw UnsupportedOperationException("executeCommand not implemented")
        open suspend fun executeAction(request: AccessibilityActionRequestProto): AccessibilityActionResponseProto =
            throw UnsupportedOperationException("executeAction not implemented")
        open suspend fun scrapeScreen(request: ScrapeScreenRequestProto): ScrapeScreenResponseProto =
            throw UnsupportedOperationException("scrapeScreen not implemented")
        open suspend fun startRecognition(request: StartRecognitionRequestProto): VoiceOSResponseProto =
            throw UnsupportedOperationException("startRecognition not implemented")
        open suspend fun stopRecognition(request: StopRecognitionRequestProto): VoiceOSResponseProto =
            throw UnsupportedOperationException("stopRecognition not implemented")
        open suspend fun learnApp(request: LearnAppRequestProto): LearnedAppInfoProtoMsg =
            throw UnsupportedOperationException("learnApp not implemented")
        open suspend fun getLearnedApps(request: GetLearnedAppsRequestProto): LearnedAppsResponseProto =
            throw UnsupportedOperationException("getLearnedApps not implemented")
        open suspend fun getCommands(request: GetCommandsRequestProto): AppCommandsResponseProto =
            throw UnsupportedOperationException("getCommands not implemented")
        open suspend fun registerCommand(request: RegisterCommandRequestProto): VoiceOSResponseProto =
            throw UnsupportedOperationException("registerCommand not implemented")
        open suspend fun unregisterCommand(request: UnregisterCommandRequestProto): VoiceOSResponseProto =
            throw UnsupportedOperationException("unregisterCommand not implemented")
        open fun streamEvents(request: StreamEventsRequestProto): kotlinx.coroutines.flow.Flow<VoiceOSEventProto> =
            throw UnsupportedOperationException("streamEvents not implemented")

        override fun bindService(): io.grpc.ServerServiceDefinition {
            // Service binding - methods are dispatched via gRPC reflection
            return io.grpc.ServerServiceDefinition.builder("voiceos.VoiceOSService").build()
        }
    }
}

// --- Request Protocol Types ---

data class ServiceStatusRequestProto(
    val requestId: String = ""
)

data class VoiceCommandRequestProto(
    val requestId: String = "",
    val commandText: String = "",
    val contextMap: Map<String, String> = emptyMap()
)

data class AccessibilityActionRequestProto(
    val requestId: String = "",
    val actionTypeName: String = "",
    val targetAvid: String = "",
    val paramsMap: Map<String, String> = emptyMap()
)

data class ScrapeScreenRequestProto(
    val requestId: String = "",
    val includeInvisible: Boolean = false,
    val maxDepth: Int = 10
)

data class StartRecognitionRequestProto(
    val requestId: String = "",
    val language: String = "en-US",
    val continuous: Boolean = false
)

data class StopRecognitionRequestProto(
    val requestId: String = ""
)

data class LearnAppRequestProto(
    val requestId: String = "",
    val packageName: String = ""
)

data class GetLearnedAppsRequestProto(
    val requestId: String = ""
)

data class GetCommandsRequestProto(
    val requestId: String = "",
    val packageName: String = ""
)

data class RegisterCommandRequestProto(
    val requestId: String = "",
    val phrase: String = "",
    val actionType: String = "",
    val actionParams: Map<String, String> = emptyMap(),
    val appPackage: String = ""
)

data class UnregisterCommandRequestProto(
    val requestId: String = "",
    val phrase: String = "",
    val appPackage: String = ""
)

data class StreamEventsRequestProto(
    val requestId: String = ""
)

// --- Response Protocol Types with Builder Pattern ---

data class ServiceStatusProto(
    val requestId: String = "",
    val isReady: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isVoiceRecognitionActive: Boolean = false,
    val currentLanguage: String = "",
    val version: String = "",
    val capabilities: List<String> = emptyList()
) {
    companion object {
        fun newBuilder() = Builder()
    }
    class Builder {
        private var requestId = ""
        private var isReady = false
        private var isAccessibilityEnabled = false
        private var isVoiceRecognitionActive = false
        private var currentLanguage = ""
        private var version = ""
        private var capabilities = mutableListOf<String>()
        fun setRequestId(v: String) = apply { requestId = v }
        fun setIsReady(v: Boolean) = apply { isReady = v }
        fun setIsAccessibilityEnabled(v: Boolean) = apply { isAccessibilityEnabled = v }
        fun setIsVoiceRecognitionActive(v: Boolean) = apply { isVoiceRecognitionActive = v }
        fun setCurrentLanguage(v: String) = apply { currentLanguage = v }
        fun setVersion(v: String) = apply { version = v }
        fun addAllCapabilities(v: List<String>) = apply { capabilities.addAll(v) }
        fun build() = ServiceStatusProto(requestId, isReady, isAccessibilityEnabled, isVoiceRecognitionActive, currentLanguage, version, capabilities)
    }
}

data class VoiceCommandResponseProto(
    val requestId: String = "",
    val success: Boolean = false,
    val action: String = "",
    val result: String = "",
    val confidence: Float = 0f,
    val error: String = ""
) {
    companion object {
        fun newBuilder() = Builder()
    }
    class Builder {
        private var requestId = ""
        private var success = false
        private var action = ""
        private var result = ""
        private var confidence = 0f
        private var error = ""
        fun setRequestId(v: String) = apply { requestId = v }
        fun setSuccess(v: Boolean) = apply { success = v }
        fun setAction(v: String) = apply { action = v }
        fun setResult(v: String) = apply { result = v }
        fun setConfidence(v: Float) = apply { confidence = v }
        fun setError(v: String) = apply { error = v }
        fun build() = VoiceCommandResponseProto(requestId, success, action, result, confidence, error)
    }
}

data class AccessibilityActionResponseProto(
    val requestId: String = "",
    val success: Boolean = false,
    val resultText: String = ""
) {
    companion object {
        fun newBuilder() = Builder()
    }
    class Builder {
        private var requestId = ""
        private var success = false
        private var resultText = ""
        fun setRequestId(v: String) = apply { requestId = v }
        fun setSuccess(v: Boolean) = apply { success = v }
        fun setResultText(v: String) = apply { resultText = v }
        fun build() = AccessibilityActionResponseProto(requestId, success, resultText)
    }
}

data class ScrapeScreenResponseProto(
    val requestId: String = "",
    val success: Boolean = false,
    val packageName: String = "",
    val activityName: String = "",
    val elements: List<ScreenElementProto> = emptyList()
) {
    companion object {
        fun newBuilder() = Builder()
    }
    class Builder {
        private var requestId = ""
        private var success = false
        private var packageName = ""
        private var activityName = ""
        private var elements = mutableListOf<ScreenElementProto>()
        fun setRequestId(v: String) = apply { requestId = v }
        fun setSuccess(v: Boolean) = apply { success = v }
        fun setPackageName(v: String) = apply { packageName = v }
        fun setActivityName(v: String) = apply { activityName = v }
        fun addAllElements(v: List<ScreenElementProto>) = apply { elements.addAll(v) }
        fun build() = ScrapeScreenResponseProto(requestId, success, packageName, activityName, elements)
    }
}

data class ScreenElementProto(
    val avid: String = "",
    val className: String = "",
    val text: String = "",
    val contentDescription: String = "",
    val boundsLeft: Int = 0,
    val boundsTop: Int = 0,
    val boundsRight: Int = 0,
    val boundsBottom: Int = 0,
    val isClickable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEditable: Boolean = false
) {
    companion object {
        fun newBuilder() = Builder()
    }
    class Builder {
        private var avid = ""
        private var className = ""
        private var text = ""
        private var contentDescription = ""
        private var boundsLeft = 0
        private var boundsTop = 0
        private var boundsRight = 0
        private var boundsBottom = 0
        private var isClickable = false
        private var isScrollable = false
        private var isEditable = false
        fun setAvid(v: String) = apply { avid = v }
        fun setClassName(v: String) = apply { className = v }
        fun setText(v: String) = apply { text = v }
        fun setContentDescription(v: String) = apply { contentDescription = v }
        fun setBoundsLeft(v: Int) = apply { boundsLeft = v }
        fun setBoundsTop(v: Int) = apply { boundsTop = v }
        fun setBoundsRight(v: Int) = apply { boundsRight = v }
        fun setBoundsBottom(v: Int) = apply { boundsBottom = v }
        fun setIsClickable(v: Boolean) = apply { isClickable = v }
        fun setIsScrollable(v: Boolean) = apply { isScrollable = v }
        fun setIsEditable(v: Boolean) = apply { isEditable = v }
        fun build() = ScreenElementProto(avid, className, text, contentDescription, boundsLeft, boundsTop, boundsRight, boundsBottom, isClickable, isScrollable, isEditable)
    }
}

data class VoiceOSResponseProto(
    val requestId: String = "",
    val success: Boolean = false,
    val message: String = "",
    val error: String = ""
) {
    companion object {
        fun newBuilder() = Builder()
    }
    class Builder {
        private var requestId = ""
        private var success = false
        private var message = ""
        private var error = ""
        fun setRequestId(v: String) = apply { requestId = v }
        fun setSuccess(v: Boolean) = apply { success = v }
        fun setMessage(v: String) = apply { message = v }
        fun setError(v: String) = apply { error = v }
        fun build() = VoiceOSResponseProto(requestId, success, message, error)
    }
}

data class LearnedAppInfoProtoMsg(
    val requestId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val screenCount: Int = 0,
    val elementCount: Int = 0,
    val commandCount: Int = 0,
    val learnedAt: Long = 0L
) {
    companion object {
        fun newBuilder() = Builder()
    }
    class Builder {
        private var requestId = ""
        private var packageName = ""
        private var appName = ""
        private var screenCount = 0
        private var elementCount = 0
        private var commandCount = 0
        private var learnedAt = 0L
        fun setRequestId(v: String) = apply { requestId = v }
        fun setPackageName(v: String) = apply { packageName = v }
        fun setAppName(v: String) = apply { appName = v }
        fun setScreenCount(v: Int) = apply { screenCount = v }
        fun setElementCount(v: Int) = apply { elementCount = v }
        fun setCommandCount(v: Int) = apply { commandCount = v }
        fun setLearnedAt(v: Long) = apply { learnedAt = v }
        fun build() = LearnedAppInfoProtoMsg(requestId, packageName, appName, screenCount, elementCount, commandCount, learnedAt)
    }
}

data class LearnedAppsResponseProto(
    val requestId: String = "",
    val apps: List<LearnedAppInfoProtoMsg> = emptyList()
) {
    companion object {
        fun newBuilder() = Builder()
    }
    class Builder {
        private var requestId = ""
        private var apps = mutableListOf<LearnedAppInfoProtoMsg>()
        fun setRequestId(v: String) = apply { requestId = v }
        fun addAllApps(v: List<LearnedAppInfoProtoMsg>) = apply { apps.addAll(v) }
        fun build() = LearnedAppsResponseProto(requestId, apps)
    }
}

data class AppCommandsResponseProto(
    val requestId: String = "",
    val packageName: String = "",
    val commands: List<LearnedCommandProto> = emptyList()
) {
    companion object {
        fun newBuilder() = Builder()
    }
    class Builder {
        private var requestId = ""
        private var packageName = ""
        private var commands = mutableListOf<LearnedCommandProto>()
        fun setRequestId(v: String) = apply { requestId = v }
        fun setPackageName(v: String) = apply { packageName = v }
        fun addAllCommands(v: List<LearnedCommandProto>) = apply { commands.addAll(v) }
        fun build() = AppCommandsResponseProto(requestId, packageName, commands)
    }
}

data class LearnedCommandProto(
    val phrase: String = "",
    val actionType: String = "",
    val targetAvid: String = "",
    val confidence: Float = 0f
) {
    companion object {
        fun newBuilder() = Builder()
    }
    class Builder {
        private var phrase = ""
        private var actionType = ""
        private var targetAvid = ""
        private var confidence = 0f
        fun setPhrase(v: String) = apply { phrase = v }
        fun setActionType(v: String) = apply { actionType = v }
        fun setTargetAvid(v: String) = apply { targetAvid = v }
        fun setConfidence(v: Float) = apply { confidence = v }
        fun build() = LearnedCommandProto(phrase, actionType, targetAvid, confidence)
    }
}

data class VoiceOSEventProto(
    val timestamp: Long = 0L,
    val eventType: String = "",
    val language: String = "",
    val transcript: String = "",
    val confidence: Float = 0f,
    val isFinal: Boolean = false,
    val reason: String = "",
    val command: String = "",
    val success: Boolean = false,
    val result: String = "",
    val packageName: String = "",
    val activityName: String = "",
    val isEnabled: Boolean = false
) {
    companion object {
        fun newBuilder() = Builder()
    }
    class Builder {
        private var timestamp = 0L
        private var eventType = ""
        private var language = ""
        private var transcript = ""
        private var confidence = 0f
        private var isFinal = false
        private var reason = ""
        private var command = ""
        private var success = false
        private var result = ""
        private var packageName = ""
        private var activityName = ""
        private var isEnabled = false
        fun setTimestamp(v: Long) = apply { timestamp = v }
        fun setEventType(v: String) = apply { eventType = v }
        fun setLanguage(v: String) = apply { language = v }
        fun setTranscript(v: String) = apply { transcript = v }
        fun setConfidence(v: Float) = apply { confidence = v }
        fun setIsFinal(v: Boolean) = apply { isFinal = v }
        fun setReason(v: String) = apply { reason = v }
        fun setCommand(v: String) = apply { command = v }
        fun setSuccess(v: Boolean) = apply { success = v }
        fun setResult(v: String) = apply { result = v }
        fun setPackageName(v: String) = apply { packageName = v }
        fun setActivityName(v: String) = apply { activityName = v }
        fun setIsEnabled(v: Boolean) = apply { isEnabled = v }
        fun build() = VoiceOSEventProto(timestamp, eventType, language, transcript, confidence, isFinal, reason, command, success, result, packageName, activityName, isEnabled)
    }
}
