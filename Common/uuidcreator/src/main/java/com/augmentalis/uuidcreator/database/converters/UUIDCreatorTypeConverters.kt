/**
 * UUIDCreatorTypeConverters.kt - Room type converters for complex types
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/converters/UUIDCreatorTypeConverters.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Type converters for serializing complex types to/from database
 */

package com.augmentalis.uuidcreator.database.converters

import androidx.room.TypeConverter
import com.augmentalis.uuidcreator.models.UUIDMetadata
import com.augmentalis.uuidcreator.models.UUIDPosition
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Type converters for Room database
 *
 * Converts complex types (UUIDMetadata, UUIDPosition) to/from JSON strings
 * for storage in SQLite database.
 */
class UUIDCreatorTypeConverters {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    // ==================== UUIDMetadata Converters ====================

    /**
     * Convert UUIDMetadata to JSON string
     */
    @TypeConverter
    fun fromUUIDMetadata(metadata: UUIDMetadata?): String? {
        return metadata?.let { gson.toJson(it) }
    }

    /**
     * Convert JSON string to UUIDMetadata
     */
    @TypeConverter
    fun toUUIDMetadata(json: String?): UUIDMetadata? {
        return json?.let {
            try {
                gson.fromJson(it, UUIDMetadata::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    // ==================== UUIDPosition Converters ====================

    /**
     * Convert UUIDPosition to JSON string
     */
    @TypeConverter
    fun fromUUIDPosition(position: UUIDPosition?): String? {
        return position?.let { gson.toJson(it) }
    }

    /**
     * Convert JSON string to UUIDPosition
     */
    @TypeConverter
    fun toUUIDPosition(json: String?): UUIDPosition? {
        return json?.let {
            try {
                gson.fromJson(it, UUIDPosition::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
