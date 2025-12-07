/**
 * NavigationEdgeEntity.kt - Room entity for navigation edges
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/database/entities/NavigationEdgeEntity.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Room database entity for storing navigation graph edges
 */

package com.augmentalis.voiceoscore.learnapp.database.entities


/**
 * Navigation Edge Entity
 *
 * Data class storing navigation graph edges (screen transitions) (Room removed, now using SQLDelight).
 *
 * @property edgeId Edge ID (primary key)
 * @property packageName Package name (foreign key)
 * @property sessionId Session ID (foreign key)
 * @property fromScreenHash Source screen hash
 * @property clickedElementUuid Clicked element UUID
 * @property toScreenHash Destination screen hash
 * @property timestamp When edge was discovered
 *
 * @since 1.0.0
 */
data class NavigationEdgeEntity(
    val edgeId: String,
    val packageName: String,
    val sessionId: String,
    val fromScreenHash: String,
    val clickedElementUuid: String,
    val toScreenHash: String,
    val timestamp: Long
)
