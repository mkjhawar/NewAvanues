/**
 * VOS4UUIDIntegration.kt - Integration adapter for VOS4
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/integration/VOS4UUIDIntegration.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Integration adapter for wiring UUIDCreator into VOS4
 *
 * NOTE: This file is NOT wired into VOS4. It provides integration interfaces only.
 */

package com.augmentalis.uuidcreator.integration

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
import com.augmentalis.uuidcreator.analytics.UuidAnalytics
import com.augmentalis.uuidcreator.database.UUIDCreatorDatabase
import com.augmentalis.uuidcreator.database.repository.UUIDRepository
import com.augmentalis.uuidcreator.hierarchy.HierarchicalUuidManager
import com.augmentalis.uuidcreator.models.UUIDElement
import com.augmentalis.uuidcreator.monitoring.CollisionMonitor
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import com.augmentalis.uuidcreator.thirdparty.UuidStabilityTracker
import kotlinx.coroutines.CoroutineScope

/**
 * VOS4 UUID Integration
 *
 * Central integration point for wiring UUIDCreator into VOS4.
 * Provides unified access to all UUID functionality.
 *
 * ## Integration Pattern
 *
 * ```kotlin
 * // In VOS4 Application.onCreate()
 * class VOS4Application : Application() {
 *     lateinit var uuidIntegration: VOS4UUIDIntegration
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         uuidIntegration = VOS4UUIDIntegration.initialize(this)
 *     }
 * }
 *
 * // In VOS4 AccessibilityService
 * class VOS4AccessibilityService : AccessibilityService() {
 *     private val integration by lazy {
 *         (application as VOS4Application).uuidIntegration
 *     }
 *
 *     override fun onAccessibilityEvent(event: AccessibilityEvent) {
 *         integration.handleAccessibilityEvent(event)
 *     }
 * }
 *
 * // In VOS4 Voice Command Handler
 * class VOS4VoiceHandler {
 *     private val integration = VOS4UUIDIntegration.getInstance()
 *
 *     suspend fun processCommand(command: String): Boolean {
 *         return integration.processVoiceCommand(command)
 *     }
 * }
 * ```
 *
 * ## Features
 *
 * - Unified API for all UUID operations
 * - Third-party app scanning
 * - Voice command processing
 * - Alias management
 * - Analytics tracking
 * - Collision monitoring
 *
 * @property context Application context
 * @property uuidCreator Core UUIDCreator instance
 * @property thirdPartyGenerator Third-party UUID generator
 * @property aliasManager Alias manager
 * @property hierarchyManager Hierarchy manager
 * @property analytics Analytics tracker
 * @property collisionMonitor Collision monitor
 * @property stabilityTracker UUID stability tracker
 * @property accessibilityService Accessibility service wrapper
 * @property voiceCommandProcessor Voice command processor
 *
 * @since 1.0.0
 */
class VOS4UUIDIntegration private constructor(
    private val context: Context
) {

    /**
     * Core components
     */
    val uuidCreator: UUIDCreator
    val thirdPartyGenerator: ThirdPartyUuidGenerator
    val aliasManager: UuidAliasManager
    val hierarchyManager: HierarchicalUuidManager
    val analytics: UuidAnalytics
    val collisionMonitor: CollisionMonitor
    val stabilityTracker: UuidStabilityTracker
    val accessibilityService: UUIDAccessibilityService
    val voiceCommandProcessor: UUIDVoiceCommandProcessor

    /**
     * Database and repository
     */
    private val database: UUIDCreatorDatabase
    private val repository: UUIDRepository

    init {
        // Initialize core
        uuidCreator = UUIDCreator.initialize(context)

        // Initialize database
        database = UUIDCreatorDatabase.getInstance(context)
        repository = UUIDRepository(
            elementDao = database.uuidElementDao(),
            hierarchyDao = database.uuidHierarchyDao(),
            analyticsDao = database.uuidAnalyticsDao(),
            aliasDao = database.uuidAliasDao()
        )

        // Initialize components
        thirdPartyGenerator = ThirdPartyUuidGenerator(context)
        aliasManager = UuidAliasManager(database)
        hierarchyManager = HierarchicalUuidManager(repository)
        analytics = UuidAnalytics(repository)
        collisionMonitor = CollisionMonitor(repository, CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
        stabilityTracker = UuidStabilityTracker(context)
        accessibilityService = UUIDAccessibilityService(context)
        voiceCommandProcessor = UUIDVoiceCommandProcessor(
            uuidCreator = uuidCreator,
            aliasManager = aliasManager,
            analytics = analytics
        )

        // Initialize accessibility service
        accessibilityService.initialize()

        // Start collision monitoring (hourly scans)
        collisionMonitor.startMonitoring(intervalMinutes = 60)
    }

    /**
     * Process voice command
     *
     * Main entry point for voice command processing.
     *
     * @param command Voice command text
     * @return true if command executed successfully
     */
    suspend fun processVoiceCommand(command: String): Boolean {
        return voiceCommandProcessor.processCommand(command)
    }

    /**
     * Scan app via accessibility
     *
     * Scans third-party app and generates UUIDs.
     *
     * @param packageName Package name to scan
     * @param rootNode Root accessibility node
     */
    suspend fun scanApp(packageName: String, rootNode: AccessibilityNodeInfo) {
        accessibilityService.scanPackage(packageName, rootNode)
    }

    /**
     * Register UI element
     *
     * Registers element from VOS4 UI.
     *
     * @param element UUID element to register
     * @return Element UUID
     */
    suspend fun registerElement(element: UUIDElement): String {
        // Check for collisions
        val collisionResult = collisionMonitor.checkCollision(element.uuid, element)
        if (collisionResult is com.augmentalis.uuidcreator.monitoring.CollisionResult.Collision) {
            // Handle collision (could throw or generate new UUID)
            throw IllegalStateException("UUID collision detected: ${element.uuid}")
        }

        // Register element
        return uuidCreator.registerElement(element)
    }

    /**
     * Detect app update and remap UUIDs
     *
     * @param packageName Package that was updated
     * @return Number of UUIDs remapped
     */
    suspend fun handleAppUpdate(packageName: String): Int {
        val updated = stabilityTracker.detectAppUpdate(packageName)
        if (updated) {
            val mappings = stabilityTracker.remapUuidsForUpdatedApp(packageName)
            return mappings.size
        }
        return 0
    }

    /**
     * Get integration statistics
     *
     * @return Integration stats
     */
    suspend fun getStats(): IntegrationStats {
        return IntegrationStats(
            totalElements = uuidCreator.getAllElements().size,
            totalAliases = aliasManager.getStats().totalAliases,
            scannedPackages = accessibilityService.getScanStats().scannedPackages,
            collisions = collisionMonitor.getStats()
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: VOS4UUIDIntegration? = null

        /**
         * Initialize integration
         *
         * @param context Application context
         * @return Integration instance
         */
        fun initialize(context: Context): VOS4UUIDIntegration {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VOS4UUIDIntegration(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        /**
         * Get integration instance
         *
         * @return Integration instance
         * @throws IllegalStateException if not initialized
         */
        fun getInstance(): VOS4UUIDIntegration {
            return INSTANCE ?: throw IllegalStateException(
                "VOS4UUIDIntegration not initialized. Call initialize(context) first."
            )
        }
    }
}

/**
 * Integration Statistics
 *
 * @property totalElements Total registered elements
 * @property totalAliases Total registered aliases
 * @property scannedPackages Number of scanned packages
 * @property collisions Collision statistics
 */
data class IntegrationStats(
    val totalElements: Int,
    val totalAliases: Int,
    val scannedPackages: Int,
    val collisions: com.augmentalis.uuidcreator.monitoring.CollisionStats
) {
    override fun toString(): String {
        return """
            VOS4 UUID Integration Statistics:
            - Total Elements: $totalElements
            - Total Aliases: $totalAliases
            - Scanned Packages: $scannedPackages
            - ${collisions.toString().replace("\n", "\n  ")}
        """.trimIndent()
    }
}
