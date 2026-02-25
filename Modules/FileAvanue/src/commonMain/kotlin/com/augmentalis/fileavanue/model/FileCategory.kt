package com.augmentalis.fileavanue.model

import kotlinx.serialization.Serializable

/**
 * Quick-access file categories for the dashboard view.
 *
 * Each category maps to a MediaStore query (Android) or a file type filter
 * (Desktop/Web). The file browser controller uses [mimePrefix] to filter
 * items when browsing by category.
 */
@Serializable
enum class FileCategory(val displayName: String, val mimePrefix: String, val iconName: String) {
    IMAGES("Images", "image/", "image"),
    VIDEOS("Videos", "video/", "videocam"),
    AUDIO("Audio", "audio/", "music_note"),
    DOCUMENTS("Documents", "", "description"),
    DOWNLOADS("Downloads", "", "download"),
    RECENT("Recent", "", "history"),
}
