/**
 * CockpitServiceImpl.kt - gRPC service implementation for Cockpit
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Implements the CockpitService from cockpit.proto for cross-device management.
 */
package com.augmentalis.universalrpc.desktop.cockpit

import com.augmentalis.universalrpc.cockpit.*
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.augmentalis.vuid.core.VUIDGenerator
import java.util.concurrent.ConcurrentHashMap

/**
 * Window state tracking
 */
data class WindowState(
    val windowId: String,
    val deviceId: String,
    val title: String,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var visible: Boolean = true,
    var focused: Boolean = false
)

/**
 * Layout preset storage
 */
data class StoredLayout(
    val presetId: String,
    val name: String,
    val windows: List<WindowLayoutData>,
    val createdAt: Long = System.currentTimeMillis()
)

data class WindowLayoutData(
    val windowId: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val displayId: String
)

/**
 * Sync session tracking
 */
data class SyncSession(
    val requestId: String,
    val sourceDevice: String,
    val targetDevice: String,
    val dataTypes: List<String>,
    var state: String = "pending",
    var progress: Float = 0f,
    var message: String = "",
    val startedAt: Long = System.currentTimeMillis()
)

/**
 * Implementation of CockpitService gRPC service.
 * Handles device registration, window management, layout presets, and cross-device sync.
 */
class CockpitServiceImpl(
    private val deviceRegistry: DeviceRegistry
) : CockpitServiceGrpc.CockpitServiceImplBase() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    // Window tracking
    private val windows = ConcurrentHashMap<String, WindowState>()
    private val windowMutex = Mutex()

    // Layout presets
    private val layouts = ConcurrentHashMap<String, StoredLayout>()
    private val layoutMutex = Mutex()

    // Sync sessions
    private val syncSessions = ConcurrentHashMap<String, SyncSession>()
    private val syncMutex = Mutex()

    // Active event streams
    private val eventObservers = ConcurrentHashMap<String, StreamObserver<DeviceEvent>>()

    // ========== Device Management ==========

    override fun registerDevice(
        request: DeviceInfo,
        responseObserver: StreamObserver<CockpitResponse>
    ) {
        scope.launch {
            try {
                val device = deviceRegistry.registerDevice(
                    deviceId = request.deviceId,
                    name = request.name,
                    platform = request.platform,
                    version = request.version,
                    capabilities = request.capabilitiesMap
                )

                val response = CockpitResponse.newBuilder()
                    .setRequestId(VUIDGenerator.generateRequestVuid())
                    .setSuccess(true)
                    .setMessage("Device registered successfully")
                    .setResultJson(json.encodeToString(
                        mapOf(
                            "device_id" to device.deviceId,
                            "registered_at" to device.lastSeen
                        )
                    ))
                    .build()

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onNext(
                    createErrorResponse("Failed to register device: ${e.message}")
                )
                responseObserver.onCompleted()
            }
        }
    }

    override fun getDevices(
        request: GetDevicesRequest,
        responseObserver: StreamObserver<DevicesResponse>
    ) {
        val devices = deviceRegistry.getAllDevices(onlineOnly = request.onlineOnly)

        val response = DevicesResponse.newBuilder()
            .addAllDevices(devices.map { it.toProto() })
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun sendDeviceCommand(
        request: DeviceCommand,
        responseObserver: StreamObserver<CockpitResponse>
    ) {
        scope.launch {
            try {
                val device = deviceRegistry.getDevice(request.deviceId)
                if (device == null) {
                    responseObserver.onNext(
                        createErrorResponse("Device not found: ${request.deviceId}")
                    )
                    responseObserver.onCompleted()
                    return@launch
                }

                val result = when (request.command.lowercase()) {
                    "sync" -> handleSyncCommand(device, request.paramsMap)
                    "screenshot" -> handleScreenshotCommand(device, request.paramsMap)
                    "status" -> handleStatusCommand(device)
                    "heartbeat" -> handleHeartbeatCommand(device)
                    else -> CommandResult(false, "Unknown command: ${request.command}")
                }

                val response = CockpitResponse.newBuilder()
                    .setRequestId(request.requestId)
                    .setSuccess(result.success)
                    .setMessage(result.message)
                    .setResultJson(result.resultJson ?: "")
                    .build()

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onNext(
                    createErrorResponse("Command failed: ${e.message}", request.requestId)
                )
                responseObserver.onCompleted()
            }
        }
    }

    // ========== Window Management ==========

    override fun getWindows(
        request: GetWindowsRequest,
        responseObserver: StreamObserver<WindowsResponse>
    ) {
        val filteredWindows = if (request.deviceId.isNotEmpty()) {
            windows.values.filter { it.deviceId == request.deviceId }
        } else {
            windows.values.toList()
        }

        val response = WindowsResponse.newBuilder()
            .addAllWindows(filteredWindows.map { it.toProto() })
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun sendWindowCommand(
        request: WindowCommand,
        responseObserver: StreamObserver<CockpitResponse>
    ) {
        scope.launch {
            try {
                val window = windows[request.windowId]
                if (window == null) {
                    responseObserver.onNext(
                        createErrorResponse("Window not found: ${request.windowId}")
                    )
                    responseObserver.onCompleted()
                    return@launch
                }

                val result = windowMutex.withLock {
                    when (request.command.lowercase()) {
                        "move" -> {
                            val x = request.paramsMap["x"]?.toIntOrNull() ?: window.x
                            val y = request.paramsMap["y"]?.toIntOrNull() ?: window.y
                            window.x = x
                            window.y = y
                            CommandResult(true, "Window moved to ($x, $y)")
                        }
                        "resize" -> {
                            val width = request.paramsMap["width"]?.toIntOrNull() ?: window.width
                            val height = request.paramsMap["height"]?.toIntOrNull() ?: window.height
                            window.width = width
                            window.height = height
                            CommandResult(true, "Window resized to ${width}x${height}")
                        }
                        "show" -> {
                            window.visible = true
                            CommandResult(true, "Window shown")
                        }
                        "hide" -> {
                            window.visible = false
                            CommandResult(true, "Window hidden")
                        }
                        "focus" -> {
                            // Unfocus all other windows on same device
                            windows.values
                                .filter { it.deviceId == window.deviceId }
                                .forEach { it.focused = false }
                            window.focused = true
                            CommandResult(true, "Window focused")
                        }
                        "close" -> {
                            windows.remove(request.windowId)
                            CommandResult(true, "Window closed")
                        }
                        else -> CommandResult(false, "Unknown command: ${request.command}")
                    }
                }

                val response = CockpitResponse.newBuilder()
                    .setRequestId(request.requestId)
                    .setSuccess(result.success)
                    .setMessage(result.message)
                    .build()

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onNext(
                    createErrorResponse("Window command failed: ${e.message}", request.requestId)
                )
                responseObserver.onCompleted()
            }
        }
    }

    // ========== Layout Management ==========

    override fun saveLayout(
        request: LayoutPreset,
        responseObserver: StreamObserver<CockpitResponse>
    ) {
        scope.launch {
            try {
                val presetId = request.presetId.ifEmpty { VUIDGenerator.generatePresetVuid() }

                val layout = layoutMutex.withLock {
                    StoredLayout(
                        presetId = presetId,
                        name = request.name,
                        windows = request.windowsList.map {
                            WindowLayoutData(
                                windowId = it.windowId,
                                x = it.x,
                                y = it.y,
                                width = it.width,
                                height = it.height,
                                displayId = it.displayId
                            )
                        }
                    ).also { layouts[presetId] = it }
                }

                val response = CockpitResponse.newBuilder()
                    .setRequestId(VUIDGenerator.generateRequestVuid())
                    .setSuccess(true)
                    .setMessage("Layout saved: ${layout.name}")
                    .setResultJson(json.encodeToString(mapOf("preset_id" to presetId)))
                    .build()

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onNext(
                    createErrorResponse("Failed to save layout: ${e.message}")
                )
                responseObserver.onCompleted()
            }
        }
    }

    override fun applyLayout(
        request: ApplyLayoutRequest,
        responseObserver: StreamObserver<CockpitResponse>
    ) {
        scope.launch {
            try {
                val layout = layouts[request.presetId]
                if (layout == null) {
                    responseObserver.onNext(
                        createErrorResponse("Layout not found: ${request.presetId}")
                    )
                    responseObserver.onCompleted()
                    return@launch
                }

                windowMutex.withLock {
                    layout.windows.forEach { layoutWindow ->
                        windows[layoutWindow.windowId]?.apply {
                            x = layoutWindow.x
                            y = layoutWindow.y
                            width = layoutWindow.width
                            height = layoutWindow.height
                        }
                    }
                }

                val response = CockpitResponse.newBuilder()
                    .setRequestId(request.requestId)
                    .setSuccess(true)
                    .setMessage("Layout applied: ${layout.name}")
                    .build()

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onNext(
                    createErrorResponse("Failed to apply layout: ${e.message}", request.requestId)
                )
                responseObserver.onCompleted()
            }
        }
    }

    override fun getLayouts(
        request: GetLayoutsRequest,
        responseObserver: StreamObserver<LayoutsResponse>
    ) {
        val presets = layouts.values.map { layout ->
            LayoutPreset.newBuilder()
                .setPresetId(layout.presetId)
                .setName(layout.name)
                .addAllWindows(layout.windows.map {
                    WindowLayout.newBuilder()
                        .setWindowId(it.windowId)
                        .setX(it.x)
                        .setY(it.y)
                        .setWidth(it.width)
                        .setHeight(it.height)
                        .setDisplayId(it.displayId)
                        .build()
                })
                .build()
        }

        val response = LayoutsResponse.newBuilder()
            .addAllPresets(presets)
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    // ========== Cross-Device Sync ==========

    override fun startSync(
        request: SyncRequest,
        responseObserver: StreamObserver<SyncStatus>
    ) {
        scope.launch {
            try {
                val sourceDevice = deviceRegistry.getDevice(request.sourceDevice)
                val targetDevice = deviceRegistry.getDevice(request.targetDevice)

                if (sourceDevice == null) {
                    responseObserver.onNext(
                        createSyncStatus(request.requestId, "failed", 0f, "Source device not found")
                    )
                    responseObserver.onCompleted()
                    return@launch
                }

                if (targetDevice == null) {
                    responseObserver.onNext(
                        createSyncStatus(request.requestId, "failed", 0f, "Target device not found")
                    )
                    responseObserver.onCompleted()
                    return@launch
                }

                val session = syncMutex.withLock {
                    SyncSession(
                        requestId = request.requestId,
                        sourceDevice = request.sourceDevice,
                        targetDevice = request.targetDevice,
                        dataTypes = request.dataTypesList,
                        state = "syncing",
                        progress = 0f,
                        message = "Sync started"
                    ).also { syncSessions[request.requestId] = it }
                }

                // Start async sync process
                scope.launch {
                    performSync(session)
                }

                val response = createSyncStatus(
                    request.requestId,
                    session.state,
                    session.progress,
                    session.message
                )

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onNext(
                    createSyncStatus(request.requestId, "failed", 0f, "Sync failed: ${e.message}")
                )
                responseObserver.onCompleted()
            }
        }
    }

    override fun getSyncStatus(
        request: GetSyncStatusRequest,
        responseObserver: StreamObserver<SyncStatus>
    ) {
        val session = syncSessions[request.requestId]
        if (session == null) {
            responseObserver.onNext(
                createSyncStatus(request.requestId, "failed", 0f, "Sync session not found")
            )
        } else {
            responseObserver.onNext(
                createSyncStatus(request.requestId, session.state, session.progress, session.message)
            )
        }
        responseObserver.onCompleted()
    }

    // ========== Event Streaming ==========

    override fun streamDeviceEvents(
        request: StreamDeviceEventsRequest,
        responseObserver: StreamObserver<DeviceEvent>
    ) {
        val streamId = VUIDGenerator.generateStreamVuid()
        eventObservers[streamId] = responseObserver

        val deviceFilter = request.deviceIdsList.toSet()

        scope.launch {
            try {
                deviceRegistry.deviceEvents
                    .filter { event ->
                        deviceFilter.isEmpty() || event.deviceId in deviceFilter
                    }
                    .onEach { event ->
                        val protoEvent = DeviceEvent.newBuilder()
                            .setDeviceId(event.deviceId)
                            .setEventType(event.eventType.name.lowercase())
                            .setTimestamp(event.timestamp)
                            .apply {
                                event.device?.let { device = it.toProto() }
                            }
                            .build()

                        responseObserver.onNext(protoEvent)
                    }
                    .launchIn(this)
            } catch (e: Exception) {
                eventObservers.remove(streamId)
                responseObserver.onError(e)
            }
        }
    }

    // ========== Window Registration (internal use) ==========

    suspend fun registerWindow(state: WindowState) = windowMutex.withLock {
        windows[state.windowId] = state
    }

    suspend fun unregisterWindow(windowId: String) = windowMutex.withLock {
        windows.remove(windowId)
    }

    // ========== Helper Methods ==========

    private suspend fun handleSyncCommand(
        device: RegisteredDevice,
        params: Map<String, String>
    ): CommandResult {
        val dataTypes = params["data_types"]?.split(",") ?: listOf("settings")
        // Implementation would trigger sync to specified device
        return CommandResult(
            success = true,
            message = "Sync initiated for ${dataTypes.joinToString(", ")}",
            resultJson = json.encodeToString(mapOf("device_id" to device.deviceId, "data_types" to dataTypes))
        )
    }

    private suspend fun handleScreenshotCommand(
        device: RegisteredDevice,
        params: Map<String, String>
    ): CommandResult {
        // Screenshot would be requested from the device client
        return CommandResult(
            success = true,
            message = "Screenshot requested from ${device.name}",
            resultJson = json.encodeToString(mapOf("device_id" to device.deviceId, "pending" to true))
        )
    }

    private fun handleStatusCommand(device: RegisteredDevice): CommandResult {
        val stats = deviceRegistry.getDeviceStats()
        return CommandResult(
            success = true,
            message = "Device status retrieved",
            resultJson = json.encodeToString(
                mapOf(
                    "device_id" to device.deviceId,
                    "online" to device.online,
                    "last_seen" to device.lastSeen,
                    "platform" to device.platform,
                    "total_devices" to stats.total,
                    "online_devices" to stats.online
                )
            )
        )
    }

    private suspend fun handleHeartbeatCommand(device: RegisteredDevice): CommandResult {
        deviceRegistry.heartbeat(device.deviceId)
        return CommandResult(
            success = true,
            message = "Heartbeat acknowledged",
            resultJson = json.encodeToString(mapOf("device_id" to device.deviceId, "timestamp" to System.currentTimeMillis()))
        )
    }

    private suspend fun performSync(session: SyncSession) {
        try {
            val totalSteps = session.dataTypes.size
            var completedSteps = 0

            session.dataTypes.forEach { dataType ->
                syncMutex.withLock {
                    session.state = "syncing"
                    session.message = "Syncing $dataType..."
                    session.progress = completedSteps.toFloat() / totalSteps
                }

                // Simulate sync work (real implementation would transfer data)
                kotlinx.coroutines.delay(500)

                completedSteps++
            }

            syncMutex.withLock {
                session.state = "completed"
                session.progress = 1f
                session.message = "Sync completed successfully"
            }
        } catch (e: Exception) {
            syncMutex.withLock {
                session.state = "failed"
                session.message = "Sync failed: ${e.message}"
            }
        }
    }

    private fun createErrorResponse(message: String, requestId: String = ""): CockpitResponse {
        return CockpitResponse.newBuilder()
            .setRequestId(requestId.ifEmpty { VUIDGenerator.generateRequestVuid() })
            .setSuccess(false)
            .setMessage(message)
            .build()
    }

    private fun createSyncStatus(
        requestId: String,
        state: String,
        progress: Float,
        message: String
    ): SyncStatus {
        return SyncStatus.newBuilder()
            .setRequestId(requestId)
            .setState(state)
            .setProgress(progress)
            .setMessage(message)
            .build()
    }

    private fun RegisteredDevice.toProto(): DeviceInfo {
        return DeviceInfo.newBuilder()
            .setDeviceId(deviceId)
            .setName(name)
            .setPlatform(platform)
            .setVersion(version)
            .setOnline(online)
            .setLastSeen(lastSeen)
            .putAllCapabilities(capabilities)
            .build()
    }

    private fun WindowState.toProto(): WindowInfo {
        return WindowInfo.newBuilder()
            .setWindowId(windowId)
            .setDeviceId(deviceId)
            .setTitle(title)
            .setX(x)
            .setY(y)
            .setWidth(width)
            .setHeight(height)
            .setVisible(visible)
            .setFocused(focused)
            .build()
    }

    fun shutdown() {
        eventObservers.clear()
        scope.cancel()
    }
}

private data class CommandResult(
    val success: Boolean,
    val message: String,
    val resultJson: String? = null
)
