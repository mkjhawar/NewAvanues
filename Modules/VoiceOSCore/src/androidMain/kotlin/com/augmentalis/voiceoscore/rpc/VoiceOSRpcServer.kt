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
 * Note: This is a simplified implementation. In production, you would use
 * protobuf definitions and generated code from voiceos.proto.
 * This implementation uses JSON serialization over gRPC for simplicity.
 */
internal class VoiceOSServiceImpl(
    private val delegate: IVoiceOSServiceDelegate,
    private val scope: CoroutineScope
) : VoiceOSServiceGrpcKt.VoiceOSServiceCoroutineImplBase() {

    override suspend fun getStatus(request: ServiceStatusRequestProto): ServiceStatusProto {
        val status = delegate.getServiceStatus()

        return ServiceStatusProto.newBuilder()
            .setRequestId(request.requestId)
            .setIsActive(status.isActive)
            .setCurrentApp(status.currentApp ?: "")
            .setIsRecognizing(status.isRecognizing)
            .setRecognizedCommands(status.recognizedCommands)
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
            .setError(response.error ?: "")
            .build()
    }

    override suspend fun executeAction(request: AccessibilityActionRequestProto): AccessibilityActionResponseProto {
        val actionType = AccessibilityActionType.valueOf(request.actionType.name)
        val success = delegate.performAction(
            actionType = actionType,
            targetAvid = request.targetAvid.takeIf { it.isNotEmpty() },
            params = request.paramsMap
        )
        val result = delegate.getActionResult()

        return AccessibilityActionResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(success)
            .setResult(result ?: "")
            .build()
    }

    override suspend fun scrapeScreen(request: ScrapeScreenRequestProto): ScrapeScreenResponseProto {
        val response = delegate.scrapeCurrentScreen(
            includeInvisible = request.includeInvisible,
            maxDepth = request.maxDepth
        )

        return ScrapeScreenResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setPackageName(response.packageName)
            .setActivityName(response.activityName)
            .addAllElements(response.elements.map { it.toProto() })
            .setTimestamp(response.timestamp)
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

    override suspend fun learnApp(request: LearnAppRequestProto): LearnedAppInfoProto {
        val appInfo = delegate.learnCurrentApp(
            packageName = request.packageName.takeIf { it.isNotEmpty() }
        )

        return LearnedAppInfoProto.newBuilder()
            .setRequestId(request.requestId)
            .apply {
                appInfo?.let {
                    setPackageName(it.packageName)
                    setAppName(it.appName)
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
            .addAllCommands(commands.map { it.toProto() })
            .build()
    }

    override suspend fun registerCommand(request: RegisterCommandRequestProto): VoiceOSResponseProto {
        val success = delegate.registerDynamicCommand(
            phrase = request.phrase,
            actionType = request.actionType,
            params = request.paramsMap,
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
    private fun ScrapedElement.toProto(): ScrapedElementProto = ScrapedElementProto.newBuilder()
        .setAvid(avid)
        .setClassName(className)
        .setText(text ?: "")
        .setContentDescription(contentDescription ?: "")
        .setBoundsLeft(bounds.left)
        .setBoundsTop(bounds.top)
        .setBoundsRight(bounds.right)
        .setBoundsBottom(bounds.bottom)
        .setIsClickable(isClickable)
        .setIsScrollable(isScrollable)
        .setIsEditable(isEditable)
        .build()

    private fun LearnedAppInfo.toProto(): LearnedAppInfoProto = LearnedAppInfoProto.newBuilder()
        .setPackageName(packageName)
        .setAppName(appName)
        .setCommandCount(commandCount)
        .setLearnedAt(learnedAt)
        .build()

    private fun LearnedCommand.toProto(): LearnedCommandProto = LearnedCommandProto.newBuilder()
        .setPhrase(phrase)
        .setActionType(actionType)
        .setTargetAvid(targetAvid ?: "")
        .putAllParams(params)
        .build()

    private fun VoiceOSEvent.toProto(): VoiceOSEventProto = when (this) {
        is VoiceOSEvent.CommandRecognized -> VoiceOSEventProto.newBuilder()
            .setTimestamp(timestamp)
            .setEventType("COMMAND_RECOGNIZED")
            .setCommandText(commandText)
            .setConfidence(confidence)
            .build()
        is VoiceOSEvent.ActionExecuted -> VoiceOSEventProto.newBuilder()
            .setTimestamp(timestamp)
            .setEventType("ACTION_EXECUTED")
            .setActionType(actionType)
            .setSuccess(success)
            .build()
        is VoiceOSEvent.RecognitionStateChanged -> VoiceOSEventProto.newBuilder()
            .setTimestamp(timestamp)
            .setEventType("RECOGNITION_STATE_CHANGED")
            .setIsRecognizing(isRecognizing)
            .build()
        is VoiceOSEvent.Error -> VoiceOSEventProto.newBuilder()
            .setTimestamp(timestamp)
            .setEventType("ERROR")
            .setErrorMessage(message)
            .setErrorCode(code)
            .build()
    }
}

// Placeholder Proto message types - these would be generated from .proto files
// In a real implementation, use protoc to generate these from voiceos.proto

typealias ServiceStatusRequestProto = com.augmentalis.voiceoscore.proto.ServiceStatusRequest
typealias ServiceStatusProto = com.augmentalis.voiceoscore.proto.ServiceStatus
typealias VoiceCommandRequestProto = com.augmentalis.voiceoscore.proto.VoiceCommandRequest
typealias VoiceCommandResponseProto = com.augmentalis.voiceoscore.proto.VoiceCommandResponse
typealias AccessibilityActionRequestProto = com.augmentalis.voiceoscore.proto.AccessibilityActionRequest
typealias AccessibilityActionResponseProto = com.augmentalis.voiceoscore.proto.AccessibilityActionResponse
typealias ScrapeScreenRequestProto = com.augmentalis.voiceoscore.proto.ScrapeScreenRequest
typealias ScrapeScreenResponseProto = com.augmentalis.voiceoscore.proto.ScrapeScreenResponse
typealias StartRecognitionRequestProto = com.augmentalis.voiceoscore.proto.StartRecognitionRequest
typealias StopRecognitionRequestProto = com.augmentalis.voiceoscore.proto.StopRecognitionRequest
typealias VoiceOSResponseProto = com.augmentalis.voiceoscore.proto.VoiceOSResponse
typealias LearnAppRequestProto = com.augmentalis.voiceoscore.proto.LearnAppRequest
typealias LearnedAppInfoProto = com.augmentalis.voiceoscore.proto.LearnedAppInfo
typealias GetLearnedAppsRequestProto = com.augmentalis.voiceoscore.proto.GetLearnedAppsRequest
typealias LearnedAppsResponseProto = com.augmentalis.voiceoscore.proto.LearnedAppsResponse
typealias GetCommandsRequestProto = com.augmentalis.voiceoscore.proto.GetCommandsRequest
typealias AppCommandsResponseProto = com.augmentalis.voiceoscore.proto.AppCommandsResponse
typealias RegisterCommandRequestProto = com.augmentalis.voiceoscore.proto.RegisterCommandRequest
typealias UnregisterCommandRequestProto = com.augmentalis.voiceoscore.proto.UnregisterCommandRequest
typealias StreamEventsRequestProto = com.augmentalis.voiceoscore.proto.StreamEventsRequest
typealias VoiceOSEventProto = com.augmentalis.voiceoscore.proto.VoiceOSEvent
typealias ScrapedElementProto = com.augmentalis.voiceoscore.proto.ScrapedElement
typealias LearnedCommandProto = com.augmentalis.voiceoscore.proto.LearnedCommand
typealias VoiceOSServiceGrpcKt = com.augmentalis.voiceoscore.proto.VoiceOSServiceGrpcKt
