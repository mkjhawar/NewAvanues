package com.augmentalis.browseravanue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for Favorite/Bookmark
 * Maps to 'favorites' table in browser database
 *
 * Architecture:
 * - Data layer entity (not domain model)
 * - Pure database representation
 * - Mapped to/from domain Favorite via FavoriteMapper
 * - Index on URL for fast lookups
 * - Index on folder for folder-based queries
 */
@Entity(
    tableName = "favorites",
    indices = [
        Index(value = ["url"], unique = true),
        Index(value = ["folder"])
    ]
)
data class FavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "favicon")
    val favicon: String?,

    @ColumnInfo(name = "folder")
    val folder: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "visit_count")
    val visitCount: Int,

    @ColumnInfo(name = "last_visited")
    val lastVisited: Long?,

    @ColumnInfo(name = "tags")
    val tags: String, // Comma-separated tags

    @ColumnInfo(name = "notes")
    val notes: String?
)
