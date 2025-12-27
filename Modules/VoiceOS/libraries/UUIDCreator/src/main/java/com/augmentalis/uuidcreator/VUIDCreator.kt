package com.augmentalis.uuidcreator

import android.content.Context
import android.util.Log
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.uuidcreator.api.IVUIDManager
import com.augmentalis.uuidcreator.core.*
import com.augmentalis.uuidcreator.database.repository.SQLDelightVUIDRepositoryAdapter
import com.augmentalis.uuidcreator.models.*
import com.augmentalis.uuidcreator.targeting.TargetResolver
import com.augmentalis.uuidcreator.spatial.SpatialNavigator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Main VUIDCreator library class
 * Voice Unique Identifier System for Voice & Spatial UI Control
 *
 * Framework-agnostic VUID management with SQLDelight persistence
 *
 * @property context Application context for database
 */
open class VUIDCreator(
    private val context: Context
) : IVUIDManager {

    companion object {
        private const val TAG = "VUIDCreator"
        private const val COMMAND_TIMEOUT = 5000L
        private const val MAX_COMMAND_HISTORY = 100

        /**
         * Global singleton instance (requires initialization)
         */
        @Volatile
        private var INSTANCE: VUIDCreator? = null

        /**
         * Initialize singleton instance
         *
         * Must be called before accessing instance property.
         *
         * @param context Application context
         */
        @JvmStatic
        fun initialize(context: Context): VUIDCreator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VUIDCreator(context.applicationContext).also {
                    INSTANCE = it
                    // Trigger lazy loading in background
                    CoroutineScope(Dispatchers.IO).launch {
                        it.ensureLoaded()
                    }
                }
            }
        }

        /**
         * Get singleton instance
         *
         * @throws IllegalStateException if not initialized
         */
        @JvmStatic
        fun getInstance(): VUIDCreator {
            return INSTANCE ?: throw IllegalStateException(
                "VUIDCreator not initialized. Call VUIDCreator.initialize(context) first."
            )
        }

        /**
         * Quick static access methods
         */
        @JvmStatic
        fun create(context: Context): VUIDCreator = VUIDCreator(context)

        @JvmStatic
        fun generate(): String = VUIDGenerator.generate()
    }

    // Database and repository (SQLDelight-based)
    private val databaseManager = VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(context))
    private val repository = SQLDelightVUIDRepositoryAdapter(databaseManager.uuids)

    // Registry with hybrid storage
    private val registry = VUIDRegistry(repository)
    private val targetResolver = TargetResolver(registry)
    private val spatialNavigator = SpatialNavigator(registry)

    /**
     * Flag indicating if database has been loaded
     */
    @Volatile
    private var isLoaded = false

    /**
     * Mutex for thread-safe database loading
     */
    private val loadMutex = Mutex()

    /**
     * Ensure database cache is loaded
     *
     * Called automatically on first access. Subsequent calls are no-ops.
     */
    suspend fun ensureLoaded() {
        if (!isLoaded) {
            loadMutex.withLock {
                if (!isLoaded) {
                    repository.loadCache()
                    isLoaded = true
                    Log.d(TAG, "VUIDCreator database loaded: ${repository.getCount()} elements")
                }
            }
        }
    }
    
    // Legacy state management from UIKitVoiceCommandSystem (lines 105-113)
    private val registeredTargets = ConcurrentHashMap<String, VoiceTarget>()
    private val commandHistory = mutableListOf<VoiceCommand>()
    private val activeContext = MutableStateFlow<String?>(null)
    
    private val _commandEvents = MutableSharedFlow<VoiceCommand>()
    val commandEvents: SharedFlow<VoiceCommand> = _commandEvents.asSharedFlow()
    
    private val _commandResults = MutableSharedFlow<CommandResult>()
    val commandResults: SharedFlow<CommandResult> = _commandResults.asSharedFlow()
    
    /**
     * Generate a new VUID
     */
    override fun generateVUID(): String = VUIDGenerator.generate()
    
    /**
     * Register an element with VUID
     *
     * Note: Uses Dispatchers.Unconfined in runBlocking to prevent thread starvation
     * when called from coroutine contexts.
     */
    override fun registerElement(element: VUIDElement): String {
        return runBlocking(Dispatchers.Unconfined) {
            registry.register(element)
        }
    }

    /**
     * Unregister an element
     *
     * Note: Uses Dispatchers.Unconfined in runBlocking to prevent thread starvation
     * when called from coroutine contexts.
     */
    override fun unregisterElement(vuid: String): Boolean {
        return runBlocking(Dispatchers.Unconfined) {
            registry.unregister(vuid)
        }
    }
    
    /**
     * Find element by VUID
     */
    override fun findByVUID(vuid: String): VUIDElement? = registry.findByVUID(vuid)
    
    /**
     * Find elements by name
     */
    override fun findByName(name: String): List<VUIDElement> = registry.findByName(name)

    /**
     * Find elements by type
     */
    override fun findByType(type: String): List<VUIDElement> = registry.findByType(type)
    
    /**
     * Find element by position
     */
    override fun findByPosition(position: Int): VUIDElement? {
        val result = spatialNavigator.navigateToPosition(position)
        return result.target
    }
    
    /**
     * Find elements in direction
     */
    override fun findInDirection(fromVUID: String, direction: String): VUIDElement? {
        val dir = when (direction.lowercase()) {
            "left" -> SpatialNavigator.Direction.LEFT
            "right" -> SpatialNavigator.Direction.RIGHT
            "up" -> SpatialNavigator.Direction.UP
            "down" -> SpatialNavigator.Direction.DOWN
            "forward" -> SpatialNavigator.Direction.FORWARD
            "backward" -> SpatialNavigator.Direction.BACKWARD
            "next" -> SpatialNavigator.Direction.NEXT
            "previous" -> SpatialNavigator.Direction.PREVIOUS
            "first" -> SpatialNavigator.Direction.FIRST
            "last" -> SpatialNavigator.Direction.LAST
            else -> return null
        }

        val result = spatialNavigator.navigate(fromVUID, dir)
        return result.target
    }
    
    /**
     * Execute action on element
     */
    override suspend fun executeAction(vuid: String, action: String, parameters: Map<String, Any>): Boolean {
        val element = registry.findByVUID(vuid) ?: return false
        
        if (!element.isEnabled) return false
        
        val actionHandler = element.actions[action] ?: element.actions["default"] ?: return false
        
        return try {
            withTimeout(5000L) {
                actionHandler(parameters)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Process voice command
     */
    override suspend fun processVoiceCommand(command: String): VUIDCommandResult {
        val startTime = System.currentTimeMillis()

        try {
            val parsedCommand = parseVoiceCommand(command)
            val targetResult = targetResolver.resolve(parsedCommand.targetRequest)

            if (targetResult.elements.isEmpty()) {
                return VUIDCommandResult(
                    success = false,
                    message = "No matching elements found",
                    executionTime = System.currentTimeMillis() - startTime
                )
            }

            // Execute on first matching element (highest priority)
            val target = targetResult.elements.first()
            val success = executeAction(target.vuid, parsedCommand.action, parsedCommand.parameters)

            return VUIDCommandResult(
                success = success,
                targetVUID = target.vuid,
                action = parsedCommand.action,
                message = if (success) "Command executed successfully" else "Command execution failed",
                executionTime = System.currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            return VUIDCommandResult(
                success = false,
                error = e.message,
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * Get all registered elements
     */
    override fun getAllElements(): List<VUIDElement> = registry.getAllElements()
    
    /**
     * Clear all registrations
     *
     * Note: Uses Dispatchers.Unconfined in runBlocking to prevent thread starvation
     * when called from coroutine contexts.
     */
    override fun clearAll() {
        runBlocking(Dispatchers.Unconfined) {
            registry.clear()
        }
    }
    
    /**
     * Create element with VUID
     */
    fun createElement(
        name: String? = null,
        type: String = "unknown",
        position: VUIDPosition? = null,
        actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap()
    ): VUIDElement {
        return VUIDElement(
            vuid = generateVUID(),
            name = name,
            type = type,
            position = position,
            actions = actions
        )
    }

    /**
     * Register element with automatic VUID generation
     */
    fun registerWithAutoVUID(
        name: String? = null,
        type: String = "unknown",
        position: VUIDPosition? = null,
        actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap()
    ): String {
        val element = createElement(name, type, position, actions)
        return registerElement(element)
    }

    // ===== BACKWARD COMPATIBILITY METHODS =====

    /**
     * Generate UUID (deprecated, use generateVUID)
     * @deprecated Use generateVUID instead
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use generateVUID instead", ReplaceWith("generateVUID()"))
    fun generateUUID(): String = generateVUID()

    /**
     * Register with auto UUID (deprecated, use registerWithAutoVUID)
     * @deprecated Use registerWithAutoVUID instead
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use registerWithAutoVUID instead", ReplaceWith("registerWithAutoVUID(name, type, position, actions)"))
    fun registerWithAutoUUID(
        name: String? = null,
        type: String = "unknown",
        position: VUIDPosition? = null,
        actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap()
    ): String = registerWithAutoVUID(name, type, position, actions)

    /**
     * Find by UUID (deprecated, use findByVUID)
     * @deprecated Use findByVUID instead
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use findByVUID instead", ReplaceWith("findByVUID(uuid)"))
    fun findByUUID(uuid: String): VUIDElement? = findByVUID(uuid)
    
    /**
     * Get registry statistics
     */
    fun getStats(): RegistryStats = registry.getStats()
    
    /**
     * Navigate spatially
     */
    fun navigate(fromVUID: String, direction: String): VUIDElement? = findInDirection(fromVUID, direction)

    /**
     * Find nearest element
     */
    fun findNearest(fromVUID: String): VUIDElement? {
        return spatialNavigator.findNearest(fromVUID)?.target
    }
    
    /**
     * Parse voice command into structured request
     */
    private fun parseVoiceCommand(command: String): ParsedCommand {
        val normalizedCommand = command.lowercase().trim()
        
        // Direct VUID patterns
        val vuidPattern = Regex("(click|select|focus)\\s+(?:element\\s+)?(?:with\\s+)?(?:vuid|uuid)[\\s-]+([a-zA-Z0-9-]+)")
        vuidPattern.find(normalizedCommand)?.let { match ->
            return ParsedCommand(
                action = match.groupValues[1],
                targetRequest = TargetResolver.TargetRequest(TargetResolver.TargetType.UUID, match.groupValues[2])
            )
        }
        
        // Position patterns
        val positionPattern = Regex("(select|click)\\s+(first|second|third|fourth|fifth|last|\\d+)")
        positionPattern.find(normalizedCommand)?.let { match ->
            val positionText = match.groupValues[2]
            val position = when (positionText) {
                "first" -> 1
                "second" -> 2
                "third" -> 3
                "fourth" -> 4
                "fifth" -> 5
                "last" -> -1
                else -> positionText.toIntOrNull() ?: 1
            }
            return ParsedCommand(
                action = match.groupValues[1],
                targetRequest = TargetResolver.TargetRequest(TargetResolver.TargetType.POSITION, position = position)
            )
        }
        
        // Direction patterns
        val directionPattern = Regex("(move|go)\\s+(left|right|up|down|forward|backward|next|previous)")
        directionPattern.find(normalizedCommand)?.let { match ->
            return ParsedCommand(
                action = "focus",
                targetRequest = TargetResolver.TargetRequest(TargetResolver.TargetType.POSITION, direction = match.groupValues[2])
            )
        }
        
        // Name patterns
        val namePattern = Regex("(click|select|open|focus)\\s+(?:on\\s+)?(.+)")
        namePattern.find(normalizedCommand)?.let { match ->
            return ParsedCommand(
                action = match.groupValues[1],
                targetRequest = TargetResolver.TargetRequest(TargetResolver.TargetType.NAME, match.groupValues[2])
            )
        }
        
        // Default fallback
        return ParsedCommand(
            action = "click",
            targetRequest = TargetResolver.TargetRequest(TargetResolver.TargetType.CONTEXT, normalizedCommand)
        )
    }
    
    /**
     * Register a voice target - EXACT copy from legacy UIKitVoiceCommandSystem (lines 118-128)
     */
    fun registerTarget(target: VoiceTarget): String {
        registeredTargets[target.uuid] = target
        
        // Update parent's children list
        target.parent?.let { parentId ->
            registeredTargets[parentId]?.children?.add(target.uuid)
        }
        
        Log.d(TAG, "Registered voice target: ${target.uuid} (${target.name})")
        return target.uuid
    }
    
    /**
     * Unregister a voice target - EXACT copy from legacy UIKitVoiceCommandSystem (lines 133-147)
     */
    fun unregisterTarget(uuid: String) {
        val target = registeredTargets.remove(uuid)
        
        // Remove from parent's children
        target?.parent?.let { parentId ->
            registeredTargets[parentId]?.children?.remove(uuid)
        }
        
        // Unregister all children recursively
        target?.children?.forEach { childId ->
            unregisterTarget(childId)
        }
        
        Log.d(TAG, "Unregistered voice target: $uuid")
    }
    
    /**
     * Set active context - EXACT copy from legacy UIKitVoiceCommandSystem (lines 469-472)
     */
    fun setContext(context: String?) {
        activeContext.value = context
        Log.d(TAG, "Context changed to: $context")
    }
    
    /**
     * Clear all registered targets - EXACT copy from legacy UIKitVoiceCommandSystem (lines 477-480)
     */
    fun clearTargets() {
        registeredTargets.clear()
        commandHistory.clear()
    }

    /**
     * Parsed command structure
     */
    private data class ParsedCommand(
        val action: String,
        val targetRequest: TargetResolver.TargetRequest,
        val parameters: Map<String, Any> = emptyMap()
    )
}