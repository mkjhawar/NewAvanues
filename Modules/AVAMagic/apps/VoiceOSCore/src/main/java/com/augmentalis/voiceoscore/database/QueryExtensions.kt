/**
 * QueryExtensions.kt - Extension functions for SQLDelight query objects
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-20
 *
 * Provides batch operation extensions for SQLDelight Queries interfaces.
 */
package com.augmentalis.voiceoscore.database

import android.util.Log
import com.augmentalis.database.element.ScrapedElementQueries
import com.augmentalis.database.element.ScrapedHierarchyQueries
import com.augmentalis.database.element.ElementRelationshipQueries
import com.augmentalis.database.GeneratedCommandQueries
import com.augmentalis.database.navigation.ScreenTransitionQueries
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
import com.augmentalis.voiceoscore.scraping.entities.ScrapedHierarchyEntity
import com.augmentalis.voiceoscore.scraping.entities.ElementRelationshipEntity
import com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity
import com.augmentalis.voiceoscore.scraping.entities.toDTO
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransacterImpl

private const val TAG = "QueryExtensions"

/**
 * Insert elements in batch and return database-assigned IDs
 *
 * Inserts each element individually within a transaction and collects
 * the database-assigned IDs using last_insert_rowid().
 *
 * @param elements List of ScrapedElementEntity to insert
 * @return List of database-assigned IDs in the same order as input elements
 */
fun ScrapedElementQueries.insertBatchWithIds(elements: List<ScrapedElementEntity>): List<Long> {
    val assignedIds = mutableListOf<Long>()

    (this as Transacter).transaction {
        elements.forEach { element ->
            // Convert entity to DTO and insert
            val dto = element.toDTO()
            insert(
                elementHash = dto.elementHash,
                appId = dto.appId,
                uuid = dto.uuid,
                className = dto.className,
                viewIdResourceName = dto.viewIdResourceName,
                text = dto.text,
                contentDescription = dto.contentDescription,
                bounds = dto.bounds,
                isClickable = dto.isClickable,
                isLongClickable = dto.isLongClickable,
                isEditable = dto.isEditable,
                isScrollable = dto.isScrollable,
                isCheckable = dto.isCheckable,
                isFocusable = dto.isFocusable,
                isEnabled = dto.isEnabled,
                depth = dto.depth,
                indexInParent = dto.indexInParent,
                scrapedAt = dto.scrapedAt,
                semanticRole = dto.semanticRole,
                inputType = dto.inputType,
                visualWeight = dto.visualWeight,
                isRequired = dto.isRequired,
                formGroupId = dto.formGroupId,
                placeholderText = dto.placeholderText,
                validationPattern = dto.validationPattern,
                backgroundColor = dto.backgroundColor,
                screen_hash = dto.screen_hash
            )

            // Capture the database-assigned ID
            val id = lastInsertRowId().executeAsOne()
            assignedIds.add(id)
        }
    }

    Log.d(TAG, "Batch inserted ${assignedIds.size} elements with IDs")
    return assignedIds
}

/**
 * Insert hierarchy relationships in batch
 *
 * Note: ScrapedHierarchyEntity uses element IDs, but the schema uses element hashes.
 * This function converts IDs to hashes using a lookup approach.
 *
 * @param hierarchyList List of ScrapedHierarchyEntity to insert
 * @param idToHashMap Optional mapping from element IDs to hashes for efficient conversion
 */
fun ScrapedHierarchyQueries.insertBatch(
    hierarchyList: List<ScrapedHierarchyEntity>,
    idToHashMap: Map<Long, String>? = null
) {
    if (hierarchyList.isEmpty()) {
        Log.d(TAG, "Empty hierarchy list, skipping batch insert")
        return
    }

    (this as Transacter).transaction {
        hierarchyList.forEach { hierarchy ->
            try {
                // Convert IDs to hashes
                // If map is not provided, use ID.toString() as fallback (will need migration later)
                val parentHash = idToHashMap?.get(hierarchy.parentElementId)
                    ?: hierarchy.parentElementId.toString()
                val childHash = idToHashMap?.get(hierarchy.childElementId)
                    ?: hierarchy.childElementId.toString()

                // Insert hierarchy relationship
                insert(
                    parentElementHash = parentHash,
                    childElementHash = childHash,
                    depth = hierarchy.depth.toLong(),
                    createdAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert hierarchy relationship: parent=${hierarchy.parentElementId}, child=${hierarchy.childElementId}", e)
            }
        }
    }
    Log.d(TAG, "Batch inserted ${hierarchyList.size} hierarchy relationships")
}

/**
 * Insert generated commands in batch
 *
 * @param commands List of GeneratedCommandEntity to insert
 */
fun GeneratedCommandQueries.insertBatch(commands: List<GeneratedCommandEntity>) {
    (this as Transacter).transaction {
        commands.forEach { command ->
            insert(
                elementHash = command.elementHash,
                commandText = command.commandText,
                actionType = command.actionType,
                confidence = command.confidence,
                synonyms = command.synonyms,
                isUserApproved = command.isUserApproved,
                usageCount = command.usageCount,
                lastUsed = command.lastUsed,
                createdAt = command.createdAt,
                appId = command.appId,
                appVersion = command.appVersion,
                versionCode = command.versionCode,
                lastVerified = command.lastVerified,
                isDeprecated = command.isDeprecated
            )
        }
    }
    Log.d(TAG, "Batch inserted ${commands.size} generated commands")
}

/**
 * Insert element relationships in batch
 *
 * @param relationships List of ElementRelationshipEntity to insert
 */
fun ElementRelationshipQueries.insertAll(relationships: List<ElementRelationshipEntity>) {
    (this as Transacter).transaction {
        relationships.forEach { relationship ->
            val dto = relationship.toDTO()
            insert(
                sourceElementHash = dto.sourceElementHash,
                targetElementHash = dto.targetElementHash,
                relationshipType = dto.relationshipType,
                relationshipData = dto.relationshipData,
                confidence = dto.confidence,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt
            )
        }
    }
    Log.d(TAG, "Batch inserted ${relationships.size} element relationships")
}

/**
 * Record a screen transition with automatic UPSERT logic
 *
 * Checks if the transition already exists and either updates statistics
 * or inserts a new record.
 *
 * @param fromHash Source screen hash
 * @param toHash Destination screen hash
 * @param transitionTime Transition duration in milliseconds (nullable)
 */
fun ScreenTransitionQueries.recordTransition(
    fromHash: String,
    toHash: String,
    transitionTime: Long?
) {
    (this as Transacter).transaction {
        // Check if transition exists (with NULL trigger)
        val existing = getExistingTransition(
            fromHash,
            toHash,
            null,  // triggerElementHash
            "auto_transition"  // triggerAction for auto-detected transitions
        ).executeAsOneOrNull()

        val currentTime = transitionTime ?: System.currentTimeMillis()

        if (existing != null) {
            // Update existing transition
            val newCount = existing.transitionCount + 1
            val newAvg = if (transitionTime != null) {
                ((existing.avgDurationMs * existing.transitionCount) + transitionTime) / newCount
            } else {
                existing.avgDurationMs
            }

            // updateTransition parameters: avgDurationMs, lastTransitionAt, fromScreenHash, toScreenHash, triggerElementHash, triggerAction
            updateTransition(
                newAvg,
                currentTime,
                fromHash,
                toHash,
                null,
                "auto_transition"
            )
        } else {
            // insertTransition parameters: fromScreenHash, toScreenHash, triggerElementHash, triggerAction, avgDurationMs, lastTransitionAt
            insertTransition(
                fromHash,
                toHash,
                null,
                "auto_transition",
                transitionTime ?: 0L,
                currentTime
            )
        }
    }

    Log.d(TAG, "Recorded screen transition: ${fromHash.take(8)} → ${toHash.take(8)}" +
            if (transitionTime != null) " (${transitionTime}ms)" else "")
}
