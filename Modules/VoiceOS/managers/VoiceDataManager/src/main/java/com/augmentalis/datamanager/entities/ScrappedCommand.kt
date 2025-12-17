// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scrapped_command")
data class ScrappedCommand(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val text: String,
    val contentDescription: String = "",
    val className: String = "",
    val isClickable: Boolean = false,
    val bounds: String = "",
    val depth: Int = 0,
    val parentClass: String? = "",
    val siblingIndex: Int = 0,
    val hash: String = "",
    val normalizedText: String = "",
    val isInheritedClickable: Boolean = false,
    val confidence: Float = 0.5f,
    val createdDate: Long = System.currentTimeMillis(),
)
