// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "language_model")
data class LanguageModel(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val languageCode: String = "",
    val engine: String = "", // "vosk", "vivoka"
    val modelPath: String = "",
    val downloadStatus: String = "", // "not_downloaded", "downloading", "ready"
    val fileSize: Long = 0L,
    val downloadDate: Long? = null,
    val version: String = ""
)