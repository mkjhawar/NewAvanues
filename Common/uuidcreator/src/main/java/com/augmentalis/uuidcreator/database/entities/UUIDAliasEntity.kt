/**
 * UUIDAliasEntity.kt - Room entity for UUID aliases
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/entities/UUIDAliasEntity.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Persistent storage for human-readable UUID aliases
 */

package com.augmentalis.uuidcreator.database.entities

import androidx.room.*

/**
 * UUID Alias Entity
 *
 * Stores human-readable aliases for UUIDs with bidirectional lookup support.
 *
 * ## Database Schema
 *
 * ```sql
 * CREATE TABLE uuid_aliases (
 *     id INTEGER PRIMARY KEY AUTOINCREMENT,
 *     alias TEXT NOT NULL UNIQUE,
 *     uuid TEXT NOT NULL,
 *     is_primary BOOLEAN NOT NULL DEFAULT 0,
 *     created_at INTEGER NOT NULL,
 *     FOREIGN KEY (uuid) REFERENCES uuid_elements(uuid) ON DELETE CASCADE
 * )
 *
 * CREATE INDEX idx_alias ON uuid_aliases(alias)
 * CREATE INDEX idx_uuid ON uuid_aliases(uuid)
 * ```
 *
 * ## Usage Example
 *
 * ```kotlin
 * val alias = UUIDAliasEntity(
 *     alias = "instagram_like_btn",
 *     uuid = "com.instagram.android.v12.0.0.button-abc123",
 *     isPrimary = true,
 *     createdAt = System.currentTimeMillis()
 * )
 *
 * aliasDao.insert(alias)
 * ```
 *
 * @property id Auto-generated primary key
 * @property alias Human-readable alias (unique)
 * @property uuid Target UUID
 * @property isPrimary Whether this is the primary alias for the UUID
 * @property createdAt Creation timestamp (milliseconds)
 *
 * @since 1.0.0
 */
@Entity(
    tableName = "uuid_aliases",
    indices = [
        Index(value = ["alias"], unique = true),
        Index(value = ["uuid"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = UUIDElementEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["uuid"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UUIDAliasEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /**
     * Human-readable alias
     *
     * Format: lowercase alphanumeric + underscores
     * Length: 3-50 characters
     * Examples: "instagram_like_btn", "submit_btn", "main_menu"
     */
    @ColumnInfo(name = "alias")
    val alias: String,

    /**
     * Target UUID
     *
     * Supports all UUID formats:
     * - Standard: "550e8400-e29b-41d4-a716-446655440000"
     * - Custom: "btn-550e8400-e29b-41d4-a716-446655440000"
     * - Third-party: "com.instagram.android.v12.0.0.button-abc123"
     */
    @ColumnInfo(name = "uuid")
    val uuid: String,

    /**
     * Primary alias flag
     *
     * One UUID can have multiple aliases. The primary alias is
     * the preferred/auto-generated one.
     */
    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false,

    /**
     * Creation timestamp
     *
     * Milliseconds since epoch.
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
