/**
 * UUIDElementEntity.kt - Room entity for UUID element storage
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/entities/UUIDElementEntity.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Room entity for persistent storage of UUID elements
 */

package com.augmentalis.uuidcreator.database.entities

import androidx.room.*

/**
 * Room entity for UUID element storage
 *
 * This entity stores the persistent data for UUID elements.
 * Action handlers and mutable children lists are kept in-memory only.
 */
@Entity(
    tableName = "uuid_elements",
    indices = [
        Index(value = ["name"], name = "idx_uuid_element_name"),
        Index(value = ["type"], name = "idx_uuid_element_type"),
        Index(value = ["parent_uuid"], name = "idx_uuid_element_parent"),
        Index(value = ["timestamp"], name = "idx_uuid_element_timestamp")
    ]
)
data class UUIDElementEntity(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    val uuid: String,

    @ColumnInfo(name = "name")
    val name: String?,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "parent_uuid")
    val parentUuid: String?,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "priority")
    val priority: Int = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    // JSON serialized fields (converted by TypeConverters)
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String?,  // Serialized UUIDMetadata

    @ColumnInfo(name = "position_json")
    val positionJson: String?   // Serialized UUIDPosition
)
