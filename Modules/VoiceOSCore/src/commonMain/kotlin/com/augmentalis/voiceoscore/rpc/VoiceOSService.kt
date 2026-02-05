/**
 * VoiceOSService.kt - VoiceOS RPC service interface (KMP)
 *
 * Defines the service contract for voice and accessibility operations.
 * Uses AVU 2.1 protocol for efficient communication.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore.rpc

import com.augmentalis.voiceoscore.rpc.messages.*
import kotlinx.coroutines.flow.Flow

/**
 * VoiceOS service interface
 */
interface IVoiceOSService {

    // ========================================================================
    // Service Status
    // ========================================================================

    suspend fun getStatus(request: ServiceStatusRequest): ServiceStatus

    // ========================================================================
    // Voice Commands
    // ========================================================================

    suspend fun executeCommand(request: VoiceCommandRequest): VoiceCommandResponse

    // ========================================================================
    // Accessibility Actions
    // ========================================================================

    suspend fun executeAction(request: AccessibilityActionRequest): AccessibilityActionResponse

    // ========================================================================
    // Screen Scraping
    // ========================================================================

    suspend fun scrapeScreen(request: ScrapeScreenRequest): ScrapeScreenResponse

    // ========================================================================
    // Voice Recognition
    // ========================================================================

    suspend fun startRecognition(request: StartRecognitionRequest): VoiceOSResponse

    suspend fun stopRecognition(request: StopRecognitionRequest): VoiceOSResponse

    // ========================================================================
    // App Learning
    // ========================================================================

    suspend fun learnApp(request: LearnAppRequest): LearnedAppInfo

    suspend fun getLearnedApps(request: GetLearnedAppsRequest): LearnedAppsResponse

    suspend fun getCommands(request: GetCommandsRequest): AppCommandsResponse

    // ========================================================================
    // Dynamic Commands
    // ========================================================================

    suspend fun registerCommand(request: RegisterCommandRequest): VoiceOSResponse

    suspend fun unregisterCommand(request: UnregisterCommandRequest): VoiceOSResponse

    // ========================================================================
    // Events
    // ========================================================================

    fun streamEvents(): Flow<VoiceOSEvent>
}

/**
 * Service delegate for platform-specific implementations
 */
interface IVoiceOSServiceDelegate {

    // Status
    suspend fun getServiceStatus(): ServiceStatus

    // Commands
    suspend fun executeVoiceCommand(commandText: String, context: Map<String, String>): VoiceCommandResponse

    // Accessibility
    suspend fun performAction(actionType: AccessibilityActionType, targetAvid: String?, params: Map<String, String>): Boolean
    suspend fun getActionResult(): String?

    // Screen
    suspend fun scrapeCurrentScreen(includeInvisible: Boolean, maxDepth: Int): ScrapeScreenResponse

    // Recognition
    suspend fun startVoiceRecognition(language: String, continuous: Boolean): Boolean
    suspend fun stopVoiceRecognition(): Boolean
    fun getRecognitionResults(): Flow<RecognitionResult>

    // Learning
    suspend fun learnCurrentApp(packageName: String?): LearnedAppInfo?
    suspend fun getLearnedApps(): List<LearnedAppInfo>
    suspend fun getCommandsForApp(packageName: String): List<LearnedCommand>

    // Dynamic commands
    suspend fun registerDynamicCommand(phrase: String, actionType: String, params: Map<String, String>, appPackage: String?): Boolean
    suspend fun unregisterDynamicCommand(phrase: String, appPackage: String?): Boolean

    // Events
    fun getEventFlow(): Flow<VoiceOSEvent>
}

/**
 * RPC Server configuration
 */
data class VoiceOSServerConfig(
    val port: Int = 50051,
    val useUnixSocket: Boolean = false,
    val unixSocketPath: String? = null
)

/**
 * RPC Server interface - platform-specific
 */
expect class VoiceOSRpcServer(
    delegate: IVoiceOSServiceDelegate,
    config: VoiceOSServerConfig
) {
    fun start()
    fun stop()
    fun isRunning(): Boolean
}
