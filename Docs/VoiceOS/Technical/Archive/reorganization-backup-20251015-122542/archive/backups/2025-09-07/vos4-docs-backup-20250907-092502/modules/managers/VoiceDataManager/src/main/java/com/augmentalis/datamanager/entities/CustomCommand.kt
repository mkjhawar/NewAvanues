// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "custom_command")
@TypeConverters(StringListConverter::class)
data class CustomCommand(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val name: String = "",
    val phrases: List<String> = emptyList(),
    val action: String = "",
    val parameters: String = "", // JSON
    val language: String = "",
    val isActive: Boolean = true,
    val createdDate: Long = 0L,
    val usageCount: Int = 0,
    val lastUsed: Long = 0L
)

class StringListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString("|")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split("|")
    }
}