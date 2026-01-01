/**
 * CollisionMonitor.kt - Runtime UUID collision detection
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/monitoring/CollisionMonitor.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Monitors and detects UUID collisions for system integrity
 */

package com.augmentalis.uuidcreator.monitoring

import com.augmentalis.uuidcreator.database.repository.UUIDRepository
import com.augmentalis.uuidcreator.models.UUIDElement
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Collision Monitor
 *
 * Runtime monitoring for UUID collisions and data integrity issues.
 *
 * ## What It Monitors
 *
 * 1. **Duplicate UUIDs**: Same UUID registered multiple times
 * 2. **Corrupted Storage**: Database integrity issues
 * 3. **Orphaned References**: Children without parents
 * 4. **Invalid Formats**: Malformed UUIDs
 *
 * ## Collision Probability
 *
 * UUID v4 collision probability: ~1 in 2^122 (effectively zero)
 *
 * However, we monitor for:
 * - Manual UUID assignment errors
 * - Database corruption
 * - Programming bugs
 * - Cross-app conflicts
 *
 * ## Usage Examples
 *
 * ```kotlin
 * val monitor = CollisionMonitor(repository, scope)
 *
 * // Check before registration
 * val result = monitor.checkCollision(uuid, element)
 * when (result) {
 *     is CollisionResult.NoCollision -> {
 *         // Safe to register
 *         repository.insert(element)
 *     }
 *     is CollisionResult.Collision -> {
 *         // Handle collision
 *         when (result.suggestedResolution) {
 *             ResolutionStrategy.GenerateNewUuid -> {
 *                 val newUuid = UUIDGenerator.generate()
 *                 element.copy(uuid = newUuid)
 *             }
 *             ResolutionStrategy.SkipRegistration -> {
 *                 // Duplicate element
 *             }
 *         }
 *     }
 * }
 *
 * // Start continuous monitoring
 * monitor.startMonitoring(intervalMinutes = 60)
 *
 * // Listen for collision events
 * monitor.collisions.collect { event ->
 *     when (event) {
 *         is CollisionEvent.DuplicateUuid -> {
 *             log("Collision detected: ${event.uuid}")
 *         }
 *     }
 * }
 * ```
 *
 * @property repository UUID repository
 * @property scope Coroutine scope for background monitoring
 *
 * @since 1.0.0
 */
class CollisionMonitor(
    private val repository: UUIDRepository,
    private val scope: CoroutineScope
) {

    /**
     * Collision events stream
     */
    private val _collisions = MutableSharedFlow<CollisionEvent>()
    val collisions: SharedFlow<CollisionEvent> = _collisions.asSharedFlow()

    /**
     * Background monitoring job
     */
    private var monitoringJob: Job? = null

    /**
     * Collision log (in-memory)
     */
    private val collisionLog = mutableListOf<CollisionLogEntry>()

    /**
     * Check for collision before registration
     *
     * Verifies UUID doesn't already exist.
     *
     * @param uuid UUID to check
     * @param proposedElement Element attempting registration
     * @return Collision result
     */
    suspend fun checkCollision(
        uuid: String,
        proposedElement: UUIDElement
    ): CollisionResult = withContext(Dispatchers.IO) {
        val existing = repository.getByUuid(uuid)

        if (existing != null) {
            // Collision detected
            val collision = CollisionEvent.DuplicateUuid(
                uuid = uuid,
                existingElement = existing,
                proposedElement = proposedElement,
                timestamp = System.currentTimeMillis()
            )

            // Log collision
            logCollision(collision)

            // Emit event
            _collisions.emit(collision)

            // Determine resolution strategy
            val strategy = suggestResolution(existing, proposedElement)

            return@withContext CollisionResult.Collision(
                uuid = uuid,
                existing = existing,
                proposed = proposedElement,
                suggestedResolution = strategy
            )
        }

        CollisionResult.NoCollision
    }

    /**
     * Start continuous monitoring
     *
     * Periodically scans for:
     * - Duplicate UUIDs
     * - Orphaned references
     * - Corrupted data
     * - Invalid formats
     *
     * @param intervalMinutes Scan interval in minutes
     */
    fun startMonitoring(intervalMinutes: Int = 60) {
        stopMonitoring() // Stop existing job if running

        monitoringJob = scope.launch {
            while (isActive) {
                delay(intervalMinutes.minutes)

                try {
                    performScan()
                } catch (e: Exception) {
                    _collisions.emit(
                        CollisionEvent.MonitoringError("Scan failed: ${e.message}")
                    )
                }
            }
        }
    }

    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * Perform integrity scan
     */
    private suspend fun performScan() = withContext(Dispatchers.IO) {
        val allElements = repository.getAll()
        val uuidCounts = allElements.groupingBy { it.uuid }.eachCount()

        // Check for duplicates
        uuidCounts.filter { it.value > 1 }.forEach { (uuid, count) ->
            _collisions.emit(
                CollisionEvent.DatabaseCorruption(
                    uuid = uuid,
                    details = "UUID appears $count times in database",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // Check for orphaned children
        allElements.forEach { element ->
            element.parent?.let { parentUuid ->
                if (repository.getByUuid(parentUuid) == null) {
                    _collisions.emit(
                        CollisionEvent.OrphanedReference(
                            uuid = element.uuid,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        // Check for invalid formats
        allElements.forEach { element ->
            if (!isValidUuidFormat(element.uuid)) {
                _collisions.emit(
                    CollisionEvent.InvalidFormat(
                        uuid = element.uuid,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Validate UUID format
     *
     * Checks if UUID matches expected format patterns.
     *
     * @param uuid UUID to validate
     * @return true if valid format
     */
    private fun isValidUuidFormat(uuid: String): Boolean {
        // Standard UUID: 8-4-4-4-12
        val standardPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        if (standardPattern.matches(uuid)) return true

        // Custom prefix: prefix-8-4-4-4-12
        val customPattern = Regex("^[a-zA-Z0-9-]+[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        if (customPattern.matches(uuid)) return true

        // Third-party: package.v{version}.type-hash
        val thirdPartyPattern = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+\\.v[0-9]+\\.[0-9]+\\.[0-9]+\\.[a-z]+-[a-f0-9]{12}$")
        if (thirdPartyPattern.matches(uuid)) return true

        return false
    }

    /**
     * Suggest resolution strategy
     *
     * Determines best way to resolve collision.
     *
     * @param existing Existing element
     * @param proposed Proposed element
     * @return Suggested resolution
     */
    private fun suggestResolution(
        existing: UUIDElement,
        proposed: UUIDElement
    ): ResolutionStrategy {
        // If identical name and type → likely duplicate
        if (existing.name == proposed.name && existing.type == proposed.type) {
            return ResolutionStrategy.SkipRegistration
        }

        // If different types → manual UUID conflict
        if (existing.type != proposed.type) {
            return ResolutionStrategy.GenerateNewUuid
        }

        // Default: generate new UUID
        return ResolutionStrategy.GenerateNewUuid
    }

    /**
     * Log collision to memory
     */
    private fun logCollision(event: CollisionEvent.DuplicateUuid) {
        collisionLog.add(
            CollisionLogEntry(
                uuid = event.uuid,
                existingElement = event.existingElement.uuid,
                proposedElement = event.proposedElement.uuid,
                timestamp = event.timestamp,
                resolved = false
            )
        )
    }

    /**
     * Get collision statistics
     *
     * @return Collision stats
     */
    fun getStats(): CollisionStats {
        val totalCollisions = collisionLog.size
        val resolvedCollisions = collisionLog.count { it.resolved }

        return CollisionStats(
            totalCollisions = totalCollisions,
            resolvedCollisions = resolvedCollisions,
            unresolvedCollisions = totalCollisions - resolvedCollisions
        )
    }

    /**
     * Get collision log
     *
     * @return List of all logged collisions
     */
    fun getCollisionLog(): List<CollisionLogEntry> {
        return collisionLog.toList()
    }

    /**
     * Clear collision log
     */
    fun clearLog() {
        collisionLog.clear()
    }
}

/**
 * Collision Result
 *
 * Result of collision check.
 */
sealed class CollisionResult {
    /**
     * No collision detected
     */
    object NoCollision : CollisionResult()

    /**
     * Collision detected
     *
     * @property uuid Colliding UUID
     * @property existing Existing element
     * @property proposed Proposed element
     * @property suggestedResolution Suggested resolution strategy
     */
    data class Collision(
        val uuid: String,
        val existing: UUIDElement,
        val proposed: UUIDElement,
        val suggestedResolution: ResolutionStrategy
    ) : CollisionResult()
}

/**
 * Collision Event
 *
 * Events emitted by collision monitor.
 */
sealed class CollisionEvent {
    /**
     * Duplicate UUID detected
     */
    data class DuplicateUuid(
        val uuid: String,
        val existingElement: UUIDElement,
        val proposedElement: UUIDElement,
        val timestamp: Long
    ) : CollisionEvent()

    /**
     * Database corruption detected
     */
    data class DatabaseCorruption(
        val uuid: String,
        val details: String,
        val timestamp: Long
    ) : CollisionEvent()

    /**
     * Orphaned reference detected
     */
    data class OrphanedReference(
        val uuid: String,
        val timestamp: Long
    ) : CollisionEvent()

    /**
     * Invalid UUID format detected
     */
    data class InvalidFormat(
        val uuid: String,
        val timestamp: Long
    ) : CollisionEvent()

    /**
     * Monitoring error
     */
    data class MonitoringError(
        val message: String
    ) : CollisionEvent()
}

/**
 * Resolution Strategy
 *
 * Suggested ways to resolve collisions.
 */
enum class ResolutionStrategy {
    /** Skip registration (element is duplicate) */
    SkipRegistration,

    /** Generate new UUID for proposed element */
    GenerateNewUuid,

    /** Replace existing with proposed */
    ReplaceExisting,

    /** Merge both elements */
    MergeBoth
}

/**
 * Collision Log Entry
 *
 * Single collision record.
 *
 * @property uuid Colliding UUID
 * @property existingElement Existing element UUID
 * @property proposedElement Proposed element UUID
 * @property timestamp Collision timestamp
 * @property resolved Whether collision was resolved
 */
data class CollisionLogEntry(
    val uuid: String,
    val existingElement: String,
    val proposedElement: String,
    val timestamp: Long,
    var resolved: Boolean = false
)

/**
 * Collision Statistics
 *
 * @property totalCollisions Total collision count
 * @property resolvedCollisions Resolved collision count
 * @property unresolvedCollisions Unresolved collision count
 */
data class CollisionStats(
    val totalCollisions: Int,
    val resolvedCollisions: Int,
    val unresolvedCollisions: Int
) {
    /**
     * Resolution rate (0.0 - 1.0)
     */
    val resolutionRate: Float
        get() = if (totalCollisions == 0) 0f else resolvedCollisions.toFloat() / totalCollisions

    override fun toString(): String {
        return """
            Collision Statistics:
            - Total: $totalCollisions
            - Resolved: $resolvedCollisions
            - Unresolved: $unresolvedCollisions
            - Resolution Rate: ${"%.1f".format(resolutionRate * 100)}%
        """.trimIndent()
    }
}
