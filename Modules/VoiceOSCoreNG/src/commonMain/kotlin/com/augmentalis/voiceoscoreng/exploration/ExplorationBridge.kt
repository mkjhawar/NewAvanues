/**
 * ExplorationBridge.kt - Bridge to UniversalRPC ExplorationService
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Connects VoiceOSCoreNG to UniversalRPC's ExplorationService for
 * JIT learning and element discovery.
 */
package com.augmentalis.voiceoscoreng.exploration

import com.augmentalis.universalrpc.exploration.*
import com.augmentalis.voiceoscoreng.avu.CommandActionType
import com.augmentalis.voiceoscoreng.avu.QuantizedCommand
import com.augmentalis.voiceoscoreng.repository.RepositoryProvider
import com.augmentalis.voiceoscoreng.repository.VuidEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Bridge between VoiceOSCoreNG and UniversalRPC ExplorationService.
 *
 * Provides high-level exploration operations that:
 * 1. Call UniversalRPC gRPC methods
 * 2. Convert results to VoiceOSCoreNG types
 * 3. Save to database via RepositoryProvider
 *
 * @param client ExplorationServiceClient from UniversalRPC
 */
class ExplorationBridge(
    private val client: ExplorationServiceClient
) {

    /**
     * Start full exploration for an app.
     *
     * @param packageName App package to explore
     * @param options Exploration options
     * @return ExplorationSession for tracking progress
     */
    suspend fun startExploration(
        packageName: String,
        options: ExplorationOptions = ExplorationOptions()
    ): ExplorationSession {
        val request = StartExplorationRequest(
            package_name = packageName,
            config = ExplorationConfig(
                max_depth = options.maxDepth.toLong(),
                include_hidden = options.includeHidden,
                timeout_ms = options.timeoutMs.toLong()
            )
        )

        val call = client.StartExploration()
        return ExplorationSession(
            packageName = packageName,
            call = call,
            request = request,
            bridge = this
        )
    }

    /**
     * JIT learn current screen.
     *
     * Captures current screen elements and generates commands.
     * Results are automatically saved to the database.
     *
     * @param packageName App package name
     * @param activityName Current activity/screen name
     * @return List of learned elements with generated commands
     */
    suspend fun learnCurrentScreen(
        packageName: String,
        activityName: String
    ): List<LearnedElementResult> {
        val request = JITLearnRequest(
            package_name = packageName,
            activity_name = activityName
        )

        val call = client.LearnJIT()
        val result = call.execute(request)

        // Convert and save results
        val learnedElements = result.elements.map { element ->
            element.toLearnedElementResult(packageName, activityName)
        }

        // Save to database
        saveLearnedElements(learnedElements)

        return learnedElements
    }

    /**
     * Capture a specific element by coordinates.
     *
     * @param packageName App package name
     * @param x X coordinate
     * @param y Y coordinate
     * @return Captured element details
     */
    suspend fun captureElement(
        packageName: String,
        x: Int,
        y: Int
    ): LearnedElementResult? {
        val request = CaptureElementRequest(
            package_name = packageName,
            x = x.toLong(),
            y = y.toLong()
        )

        val call = client.CaptureElement()
        val result = call.execute(request)

        return if (result.vuid.isNotEmpty()) {
            result.toLearnedElementResult(packageName, "")
        } else null
    }

    /**
     * Get exploration progress.
     */
    suspend fun getProgress(packageName: String): ExplorationProgressResult {
        val request = GetProgressRequest(package_name = packageName)
        val call = client.GetProgress()
        val result = call.execute(request)

        return ExplorationProgressResult(
            state = result.state.name,
            screensExplored = result.screens_explored.toInt(),
            elementsFound = result.elements_found.toInt(),
            commandsGenerated = result.commands_generated.toInt(),
            errorMessage = result.error_message
        )
    }

    /**
     * Stop ongoing exploration.
     */
    suspend fun stopExploration(packageName: String): Boolean {
        val request = StopExplorationRequest(package_name = packageName)
        val call = client.StopExploration()
        val result = call.execute(request)
        return result.success
    }

    /**
     * Get all learned data for an app.
     */
    suspend fun getLearnedData(packageName: String): List<LearnedElementResult> {
        val request = GetLearnedDataRequest(package_name = packageName)
        val call = client.GetLearnedData()
        val result = call.execute(request)

        return result.elements.map { it.toLearnedElementResult(packageName, "") }
    }

    // ========== Private Helpers ==========

    private suspend fun saveLearnedElements(elements: List<LearnedElementResult>) {
        // Save VUIDs
        val vuidEntries = elements.map { it.toVuidEntry() }
        RepositoryProvider.vuids.saveAll(vuidEntries)

        // Save commands
        val commands = elements.flatMap { it.toQuantizedCommands() }
        RepositoryProvider.commands.saveAll(commands)
    }
}

/**
 * Exploration options.
 */
data class ExplorationOptions(
    val maxDepth: Int = 10,
    val includeHidden: Boolean = false,
    val timeoutMs: Int = 60000,
    val skipDangerous: Boolean = true
)

/**
 * Exploration session for tracking progress.
 */
class ExplorationSession(
    val packageName: String,
    private val call: com.squareup.wire.GrpcCall<StartExplorationRequest, ExplorationResult>,
    private val request: StartExplorationRequest,
    private val bridge: ExplorationBridge
) {
    /**
     * Execute exploration and return results.
     */
    suspend fun execute(): ExplorationResultData {
        val result = call.execute(request)
        val elements = result.elements.map {
            it.toLearnedElementResult(packageName, "")
        }

        return ExplorationResultData(
            success = result.success,
            elementsFound = elements,
            errorMessage = result.error_message
        )
    }

    /**
     * Get current progress.
     */
    suspend fun getProgress(): ExplorationProgressResult {
        return bridge.getProgress(packageName)
    }

    /**
     * Stop exploration.
     */
    suspend fun stop(): Boolean {
        return bridge.stopExploration(packageName)
    }
}

/**
 * Exploration result data.
 */
data class ExplorationResultData(
    val success: Boolean,
    val elementsFound: List<LearnedElementResult>,
    val errorMessage: String
)

/**
 * Exploration progress result.
 */
data class ExplorationProgressResult(
    val state: String,
    val screensExplored: Int,
    val elementsFound: Int,
    val commandsGenerated: Int,
    val errorMessage: String
)

/**
 * Learned element result with generated commands.
 */
data class LearnedElementResult(
    val vuid: String,
    val packageName: String,
    val activityName: String,
    val elementType: String,
    val text: String,
    val contentDescription: String,
    val bounds: IntArray?,
    val suggestedCommands: List<String>,
    val confidence: Float
) {
    /**
     * Convert to VuidEntry for database storage.
     */
    fun toVuidEntry(): VuidEntry {
        return VuidEntry(
            vuid = vuid,
            packageName = packageName,
            activityName = activityName,
            contentHash = vuid, // Use VUID as content hash
            elementType = elementType,
            text = text,
            contentDescription = contentDescription,
            bounds = bounds,
            metadata = mapOf(
                "confidence" to confidence.toString(),
                "suggestedCommands" to suggestedCommands.joinToString("|")
            )
        )
    }

    /**
     * Convert to QuantizedCommands for database storage.
     */
    fun toQuantizedCommands(): List<QuantizedCommand> {
        return suggestedCommands.mapIndexed { index, phrase ->
            val actionType = inferActionType(phrase, elementType)
            QuantizedCommand(
                uuid = "${vuid}_cmd_$index",
                phrase = phrase,
                actionType = actionType,
                targetVuid = vuid,
                confidence = confidence,
                metadata = mapOf(
                    "packageName" to packageName,
                    "screenId" to activityName,
                    "elementType" to elementType
                )
            )
        }
    }

    private fun inferActionType(phrase: String, elementType: String): CommandActionType {
        val lowerPhrase = phrase.lowercase()
        val lowerType = elementType.lowercase()

        return when {
            lowerPhrase.contains("scroll") -> CommandActionType.SCROLL
            lowerPhrase.contains("type") || lowerPhrase.contains("enter") -> CommandActionType.TYPE
            lowerType.contains("edittext") -> CommandActionType.TYPE
            lowerType.contains("scroll") -> CommandActionType.SCROLL
            else -> CommandActionType.TAP
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as LearnedElementResult
        return vuid == other.vuid
    }

    override fun hashCode(): Int = vuid.hashCode()
}

/**
 * Extension to convert UniversalRPC LearnedElement to VoiceOSCoreNG type.
 */
fun LearnedElement.toLearnedElementResult(
    packageName: String,
    activityName: String
): LearnedElementResult {
    val boundsArray = bounds?.let {
        intArrayOf(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt())
    }

    return LearnedElementResult(
        vuid = vuid,
        packageName = packageName,
        activityName = activityName,
        elementType = element_type,
        text = text,
        contentDescription = content_desc,
        bounds = boundsArray,
        suggestedCommands = suggested_commands,
        confidence = confidence
    )
}
