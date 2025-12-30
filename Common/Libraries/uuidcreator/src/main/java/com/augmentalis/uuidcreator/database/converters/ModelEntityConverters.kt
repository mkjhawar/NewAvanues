/**
 * ModelEntityConverters.kt - Converters between model and entity classes
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/converters/ModelEntityConverters.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Extension functions to convert between domain models and Room entities
 */

package com.augmentalis.uuidcreator.database.converters

import com.augmentalis.uuidcreator.database.entities.UUIDAnalyticsEntity
import com.augmentalis.uuidcreator.database.entities.UUIDElementEntity
import com.augmentalis.uuidcreator.database.entities.UUIDHierarchyEntity
import com.augmentalis.uuidcreator.models.UUIDElement
import com.augmentalis.uuidcreator.models.UUIDMetadata
import com.augmentalis.uuidcreator.models.UUIDPosition
import com.google.gson.Gson

/**
 * Gson instance for JSON serialization
 */
private val gson = Gson()

// ==================== UUIDElement ↔ UUIDElementEntity ====================

/**
 * Convert UUIDElement model to UUIDElementEntity for database storage
 *
 * Note: Children list and action handlers are not stored in entity.
 * Children are stored separately in UUIDHierarchyEntity.
 * Actions are kept in-memory only.
 */
fun UUIDElement.toEntity(): UUIDElementEntity {
    return UUIDElementEntity(
        uuid = this.uuid,
        name = this.name,
        type = this.type,
        description = this.description,
        parentUuid = this.parent,
        isEnabled = this.isEnabled,
        priority = this.priority,
        timestamp = this.timestamp,
        metadataJson = this.metadata?.let { gson.toJson(it) },
        positionJson = this.position?.let { gson.toJson(it) }
    )
}

/**
 * Convert UUIDElementEntity from database to UUIDElement model
 *
 * @param children List of child UUIDs (loaded from UUIDHierarchyEntity)
 * @param actions Map of action handlers (kept in-memory only, not persisted)
 * @return UUIDElement model with all data
 */
fun UUIDElementEntity.toModel(
    children: MutableList<String> = mutableListOf(),
    actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap()
): UUIDElement {
    return UUIDElement(
        uuid = this.uuid,
        name = this.name,
        type = this.type,
        description = this.description,
        parent = this.parentUuid,
        children = children,
        position = this.positionJson?.let { gson.fromJson(it, UUIDPosition::class.java) },
        actions = actions,
        isEnabled = this.isEnabled,
        priority = this.priority,
        metadata = this.metadataJson?.let { gson.fromJson(it, UUIDMetadata::class.java) },
        timestamp = this.timestamp
    )
}

// ==================== Batch Converters ====================

/**
 * Convert list of UUIDElement models to entities
 */
fun List<UUIDElement>.toEntities(): List<UUIDElementEntity> {
    return this.map { it.toEntity() }
}

/**
 * Convert list of UUIDElementEntity to models
 *
 * Note: Children lists must be populated separately from UUIDHierarchyEntity
 */
fun List<UUIDElementEntity>.toModels(
    childrenMap: Map<String, MutableList<String>> = emptyMap(),
    actionsMap: Map<String, Map<String, (Map<String, Any>) -> Unit>> = emptyMap()
): List<UUIDElement> {
    return this.map { entity ->
        entity.toModel(
            children = childrenMap[entity.uuid] ?: mutableListOf(),
            actions = actionsMap[entity.uuid] ?: emptyMap()
        )
    }
}

// ==================== UUIDHierarchyEntity Helpers ====================

/**
 * Create hierarchy entity for parent-child relationship
 *
 * @param parentUuid Parent UUID
 * @param childUuid Child UUID
 * @param depth Depth in hierarchy (0 = direct child, 1+ = nested)
 * @param orderIndex Position in children list
 * @return UUIDHierarchyEntity
 */
fun createHierarchyEntity(
    parentUuid: String,
    childUuid: String,
    depth: Int = 0,
    orderIndex: Int = 0
): UUIDHierarchyEntity {
    val path = "/$parentUuid/$childUuid"
    return UUIDHierarchyEntity(
        parentUuid = parentUuid,
        childUuid = childUuid,
        depth = depth,
        path = path,
        orderIndex = orderIndex
    )
}

/**
 * Build children map from hierarchy entities
 *
 * @param hierarchies List of hierarchy relationships
 * @return Map of parent UUID to sorted list of child UUIDs
 */
fun List<UUIDHierarchyEntity>.toChildrenMap(): Map<String, MutableList<String>> {
    return this.groupBy { it.parentUuid }
        .mapValues { (_, hierarchies) ->
            hierarchies.sortedBy { it.orderIndex }
                .map { it.childUuid }
                .toMutableList()
        }
}

// ==================== UUIDAnalyticsEntity Helpers ====================

/**
 * Create initial analytics entity for a new UUID element
 *
 * @param uuid UUID of the element
 * @return UUIDAnalyticsEntity with default values
 */
fun createAnalyticsEntity(uuid: String): UUIDAnalyticsEntity {
    return UUIDAnalyticsEntity(
        uuid = uuid,
        accessCount = 0,
        firstAccessed = System.currentTimeMillis(),
        lastAccessed = System.currentTimeMillis(),
        executionTimeMs = 0,
        successCount = 0,
        failureCount = 0,
        lifecycleState = UUIDAnalyticsEntity.LifecycleState.CREATED.name
    )
}

/**
 * Update analytics entity with access information
 *
 * @param analytics Current analytics entity
 * @param executionTimeMs Execution time in milliseconds
 * @param success Whether the execution was successful
 * @return Updated analytics entity
 */
fun UUIDAnalyticsEntity.recordAccess(
    executionTimeMs: Long = 0,
    success: Boolean = true
): UUIDAnalyticsEntity {
    return this.copy(
        accessCount = this.accessCount + 1,
        lastAccessed = System.currentTimeMillis(),
        executionTimeMs = this.executionTimeMs + executionTimeMs,
        successCount = if (success) this.successCount + 1 else this.successCount,
        failureCount = if (!success) this.failureCount + 1 else this.failureCount,
        lifecycleState = if (this.lifecycleState == UUIDAnalyticsEntity.LifecycleState.CREATED.name)
            UUIDAnalyticsEntity.LifecycleState.ACTIVE.name
        else
            this.lifecycleState
    )
}
