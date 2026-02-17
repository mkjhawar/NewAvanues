package com.augmentalis.noteavanue.model

import kotlinx.serialization.Serializable

/**
 * Folder model for organizing notes.
 *
 * Supports both manual folders (user-created) and smart folders
 * (auto-populated via search filter, e.g. "all dictated notes").
 */
@Serializable
data class NoteFolder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val icon: String = "folder",
    val sortOrder: Int = 0,
    val isSmartFolder: Boolean = false,
    /** SQLite WHERE clause fragment for smart folders, e.g. "source = 'DICTATED'" */
    val smartFilter: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)
