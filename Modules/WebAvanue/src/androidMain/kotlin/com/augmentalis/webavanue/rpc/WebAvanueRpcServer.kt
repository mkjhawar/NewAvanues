/**
 * WebAvanueRpcServer.kt - Android gRPC server implementation
 *
 * Implements the WebAvanue RPC server using gRPC-Kotlin.
 * Handles incoming requests and delegates to IWebAvanueServiceDelegate.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.webavanue.rpc

import android.util.Log
import com.augmentalis.webavanue.rpc.messages.*
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import java.util.concurrent.TimeUnit

private const val TAG = "WebAvanueRpcServer"

/**
 * Android gRPC server for WebAvanue service
 */
actual class WebAvanueRpcServer actual constructor(
    private val delegate: IWebAvanueServiceDelegate,
    private val config: WebAvanueServerConfig
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
            val serviceImpl = WebAvanueServiceImpl(delegate, scope)

            server = ServerBuilder
                .forPort(config.port)
                .addService(serviceImpl)
                .maxInboundMessageSize(16 * 1024 * 1024) // 16MB
                .build()
                .start()

            Log.i(TAG, "WebAvanue gRPC server started on port ${config.port}")
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
                Log.i(TAG, "WebAvanue gRPC server stopped")
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

    /**
     * Get the port the server is listening on
     */
    actual fun getPort(): Int = server?.port ?: -1
}

/**
 * gRPC service implementation that delegates to IWebAvanueServiceDelegate
 *
 * Note: This is a simplified implementation. In production, you would use
 * protobuf definitions and generated code from webavanue.proto.
 * This implementation uses JSON serialization over gRPC for simplicity.
 */
internal class WebAvanueServiceImpl(
    private val delegate: IWebAvanueServiceDelegate,
    private val scope: CoroutineScope
) : WebAvanueServiceGrpcKt.WebAvanueServiceCoroutineImplBase() {

    override suspend fun getTabs(request: GetTabsRequestProto): GetTabsResponseProto {
        val tabs = delegate.getTabs()
        val activeTabId = delegate.getActiveTabId()

        return GetTabsResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .addAllTabs(tabs.map { it.toProto() })
            .setActiveTabId(activeTabId ?: "")
            .build()
    }

    override suspend fun createTab(request: CreateTabRequestProto): CreateTabResponseProto {
        val tab = delegate.createTab(request.url, request.makeActive)

        return CreateTabResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(tab != null)
            .apply { tab?.let { setTab(it.toProto()) } }
            .build()
    }

    override suspend fun closeTab(request: CloseTabRequestProto): WebAvanueResponseProto {
        val success = delegate.closeTab(request.tabId)

        return WebAvanueResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(success)
            .build()
    }

    override suspend fun switchTab(request: SwitchTabRequestProto): WebAvanueResponseProto {
        val success = delegate.switchTab(request.tabId)

        return WebAvanueResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(success)
            .build()
    }

    override suspend fun navigate(request: NavigateRequestProto): NavigationResponseProto {
        val success = delegate.navigate(request.tabId, request.url)

        return NavigationResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(success)
            .setUrl(request.url)
            .build()
    }

    override suspend fun goBack(request: GoBackRequestProto): NavigationResponseProto {
        val success = delegate.goBack(request.tabId)

        return NavigationResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(success)
            .build()
    }

    override suspend fun goForward(request: GoForwardRequestProto): NavigationResponseProto {
        val success = delegate.goForward(request.tabId)

        return NavigationResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(success)
            .build()
    }

    override suspend fun reload(request: ReloadRequestProto): NavigationResponseProto {
        val success = delegate.reload(request.tabId, request.hardReload)

        return NavigationResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(success)
            .build()
    }

    override suspend fun scroll(request: ScrollRequestProto): WebAvanueResponseProto {
        val direction = ScrollDirection.valueOf(request.direction.name)
        val success = delegate.scroll(request.tabId, direction, request.amount)

        return WebAvanueResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(success)
            .build()
    }

    override suspend fun executeVoiceCommand(request: VoiceCommandRequestProto): VoiceCommandResponseProto {
        val response = delegate.executeVoiceCommand(
            command = request.command,
            tabId = request.tabId.takeIf { it.isNotEmpty() },
            params = request.paramsMap
        )

        return VoiceCommandResponseProto.newBuilder()
            .setRequestId(request.requestId)
            .setSuccess(response.success)
            .setAction(response.action ?: "")
            .setResult(response.result ?: "")
            .setError(response.error ?: "")
            .build()
    }

    override fun streamEvents(request: StreamEventsRequestProto): kotlinx.coroutines.flow.Flow<WebAvanueEventProto> {
        return kotlinx.coroutines.flow.flow {
            delegate.getEventFlow()
                .catch { e -> Log.e(TAG, "Event stream error", e) }
                .collect { event ->
                    emit(event.toProto())
                }
        }
    }

    // Extension functions to convert between KMP types and Proto types
    private fun TabInfo.toProto(): TabInfoProto = TabInfoProto.newBuilder()
        .setTabId(tabId)
        .setUrl(url)
        .setTitle(title)
        .setIsActive(isActive)
        .setIsPinned(isPinned)
        .setIndex(index)
        .build()

    private fun WebAvanueEvent.toProto(): WebAvanueEventProto = when (this) {
        is WebAvanueEvent.PageLoaded -> WebAvanueEventProto.newBuilder()
            .setTabId(tabId)
            .setTimestamp(timestamp)
            .setEventType("PAGE_LOADED")
            .setUrl(url)
            .setTitle(title)
            .build()
        is WebAvanueEvent.NavigationStarted -> WebAvanueEventProto.newBuilder()
            .setTabId(tabId)
            .setTimestamp(timestamp)
            .setEventType("NAVIGATION_STARTED")
            .setUrl(url)
            .build()
        is WebAvanueEvent.TabCreated -> WebAvanueEventProto.newBuilder()
            .setTabId(tabId)
            .setTimestamp(timestamp)
            .setEventType("TAB_CREATED")
            .build()
        is WebAvanueEvent.TabClosed -> WebAvanueEventProto.newBuilder()
            .setTabId(tabId)
            .setTimestamp(timestamp)
            .setEventType("TAB_CLOSED")
            .build()
        is WebAvanueEvent.TabActivated -> WebAvanueEventProto.newBuilder()
            .setTabId(tabId)
            .setTimestamp(timestamp)
            .setEventType("TAB_ACTIVATED")
            .build()
        is WebAvanueEvent.DownloadProgress -> WebAvanueEventProto.newBuilder()
            .setTabId(tabId)
            .setTimestamp(timestamp)
            .setEventType("DOWNLOAD_PROGRESS")
            .build()
    }
}

// Placeholder Proto message types - these would be generated from .proto files
// In a real implementation, use protoc to generate these from webavanue.proto

typealias GetTabsRequestProto = com.augmentalis.webavanue.proto.GetTabsRequest
typealias GetTabsResponseProto = com.augmentalis.webavanue.proto.GetTabsResponse
typealias CreateTabRequestProto = com.augmentalis.webavanue.proto.CreateTabRequest
typealias CreateTabResponseProto = com.augmentalis.webavanue.proto.CreateTabResponse
typealias CloseTabRequestProto = com.augmentalis.webavanue.proto.CloseTabRequest
typealias SwitchTabRequestProto = com.augmentalis.webavanue.proto.SwitchTabRequest
typealias NavigateRequestProto = com.augmentalis.webavanue.proto.NavigateRequest
typealias GoBackRequestProto = com.augmentalis.webavanue.proto.GoBackRequest
typealias GoForwardRequestProto = com.augmentalis.webavanue.proto.GoForwardRequest
typealias ReloadRequestProto = com.augmentalis.webavanue.proto.ReloadRequest
typealias ScrollRequestProto = com.augmentalis.webavanue.proto.ScrollRequest
typealias VoiceCommandRequestProto = com.augmentalis.webavanue.proto.VoiceCommandRequest
typealias VoiceCommandResponseProto = com.augmentalis.webavanue.proto.VoiceCommandResponse
typealias StreamEventsRequestProto = com.augmentalis.webavanue.proto.StreamEventsRequest
typealias WebAvanueResponseProto = com.augmentalis.webavanue.proto.WebAvanueResponse
typealias NavigationResponseProto = com.augmentalis.webavanue.proto.NavigationResponse
typealias TabInfoProto = com.augmentalis.webavanue.proto.TabInfo
typealias WebAvanueEventProto = com.augmentalis.webavanue.proto.WebAvanueEvent
typealias WebAvanueServiceGrpcKt = com.augmentalis.webavanue.proto.WebAvanueServiceGrpcKt
