package com.augmentalis.browseravanue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for Tab
 * Maps to 'tabs' table in browser database
 *
 * Architecture:
 * - Data layer entity (not domain model)
 * - Pure database representation
 * - Mapped to/from domain Tab via TabMapper
 */
@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "favicon")
    val favicon: String?,

    @ColumnInfo(name = "is_desktop_mode")
    val isDesktopMode: Boolean,

    @ColumnInfo(name = "can_go_back")
    val canGoBack: Boolean,

    @ColumnInfo(name = "can_go_forward")
    val canGoForward: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long,

    @ColumnInfo(name = "is_loading")
    val isLoading: Boolean
)
