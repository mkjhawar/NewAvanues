/**
 * ScreenContextEntity.kt - Screen-level context for accessibility scraping
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-18
 */
package com.augmentalis.voiceoscore.scraping.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing screen-level context information
 *
 * Captures high-level context about the screen/window where elements appear.
 * Enables AI to understand flows, navigation patterns, and screen purpose.
 *
 * @property id Auto-generated primary key
 * @property screenHash MD5 hash of screen signature (packageName + activityName + window title)
 * @property appId Foreign key to ScrapedAppEntity
 * @property packageName Package name of the app
 * @property activityName Activity/window class name (if available)
 * @property windowTitle Window title or header text
 * @property screenType Inferred screen type (e.g., "login", "checkout", "settings", "home", "form")
 * @property formContext Form-specific context (e.g., "registration", "payment", "search")
 * @property navigationLevel Depth in navigation hierarchy (0 = main screen, 1+ = nested)
 * @property primaryAction Primary user action on this screen (e.g., "submit", "search", "browse")
 * @property elementCount Number of interactive elements on screen
 * @property hasBackButton Whether screen has back navigation
 * @property firstScraped Timestamp when screen was first scraped
 * @property lastScraped Timestamp when screen was last scraped
 * @property visitCount Number of times screen has been scraped
 */
@Entity(
    tableName = "screen_contexts",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedAppEntity::class,
            parentColumns = ["app_id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["screen_hash"], unique = true),
        Index("app_id"),
        Index("package_name"),
        Index("screen_type")
    ]
)
data class ScreenContextEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "screen_hash")
    val screenHash: String,

    @ColumnInfo(name = "app_id")
    val appId: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "activity_name")
    val activityName: String?,

    @ColumnInfo(name = "window_title")
    val windowTitle: String?,

    // AI Context (Phase 2)
    @ColumnInfo(name = "screen_type")
    val screenType: String?,

    @ColumnInfo(name = "form_context")
    val formContext: String?,

    @ColumnInfo(name = "navigation_level")
    val navigationLevel: Int = 0,

    @ColumnInfo(name = "primary_action")
    val primaryAction: String?,

    // Screen metrics
    @ColumnInfo(name = "element_count")
    val elementCount: Int = 0,

    @ColumnInfo(name = "has_back_button")
    val hasBackButton: Boolean = false,

    // Timestamps
    @ColumnInfo(name = "first_scraped")
    val firstScraped: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_scraped")
    val lastScraped: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "visit_count")
    val visitCount: Int = 1
)
