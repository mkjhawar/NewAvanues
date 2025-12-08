# Room Database Schema Design for AvaUI Component System

**Research Date:** 2025-10-26 22:10:00 PDT
**Purpose:** Database persistence design for Kotlin Multiplatform UI component system
**Status:** Research Complete

---

## Executive Summary

**Recommendation:** Use **Room KMP (2.7.0+)** over SQLDelight for AvaUI component persistence.

**Rationale:**
- Official Google support with stable KMP release (2025)
- First-class support for Android, JVM, and iOS (Tier 1)
- Familiar Android ecosystem integration
- Strong migration tooling and automated migrations
- Production-ready (used in Google Docs iOS app)
- Better for teams with Android/Kotlin background vs SQL-first approach

**Key Challenge:** Map<String, Any> type conversion requires custom TypeConverters with JSON serialization.

---

## 1. Entity Schema Design

### 1.1 ComponentModel Entity

```kotlin
package com.augmentalis.avaui.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import com.augmentalis.avaui.database.converters.PropertiesConverter
import com.augmentalis.avaui.database.converters.PositionConverter

/**
 * Component entity for Room database.
 *
 * Represents a single UI component with its properties and position.
 * Supports 1000+ components efficiently through indexed foreign keys.
 *
 * @property id Unique component identifier (UUID string)
 * @property scenarioId Foreign key to parent LayoutScenario
 * @property type Component type (e.g., "Button", "TextField", "Container")
 * @property position Component position (serialized as JSON)
 * @property properties Component properties map (serialized as JSON)
 * @property createdAt Creation timestamp (milliseconds since epoch)
 * @property updatedAt Last update timestamp (milliseconds since epoch)
 */
@Entity(
    tableName = "components",
    foreignKeys = [
        ForeignKey(
            entity = LayoutScenarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["scenario_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["scenario_id"]),
        Index(value = ["type"]),
        Index(value = ["created_at"])
    ]
)
@TypeConverters(PropertiesConverter::class, PositionConverter::class)
data class ComponentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,  // UUID string

    @ColumnInfo(name = "scenario_id")
    val scenarioId: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "position")
    val position: ComponentPosition,

    @ColumnInfo(name = "properties")
    val properties: Map<String, Any>,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Component position data class.
 * Serialized to JSON for database storage.
 */
@kotlinx.serialization.Serializable
data class ComponentPosition(
    val x: Float,
    val y: Float,
    val z: Float = 0f  // Depth/layer
)
```

### 1.2 LayoutScenario Entity

```kotlin
package com.augmentalis.avaui.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.ColumnInfo
import androidx.room.Index
import com.augmentalis.avaui.database.converters.StringListConverter

/**
 * Layout scenario entity for Room database.
 *
 * Represents a named layout configuration with DSL command and metadata.
 * One scenario can have many components (1:N relationship).
 *
 * @property id Unique scenario identifier (UUID string)
 * @property name Scenario display name
 * @property dslCommand DSL command to recreate this layout
 * @property tags Categorization tags (serialized as JSON array)
 * @property description Optional description
 * @property timestamp Creation/modification timestamp (milliseconds since epoch)
 * @property isActive Whether this scenario is currently active
 * @property componentCount Cached count of associated components
 */
@Entity(
    tableName = "layout_scenarios",
    indices = [
        Index(value = ["name"]),
        Index(value = ["timestamp"]),
        Index(value = ["is_active"])
    ]
)
@TypeConverters(StringListConverter::class)
data class LayoutScenarioEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,  // UUID string

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "dsl_command")
    val dslCommand: String,

    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    @ColumnInfo(name = "component_count")
    val componentCount: Int = 0
)
```

### 1.3 ThemeConfig Entity

```kotlin
package com.augmentalis.avaui.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.ColumnInfo
import androidx.room.Index
import com.augmentalis.avaui.database.converters.ThemeDataConverter

/**
 * Theme configuration entity for Room database.
 *
 * Stores theme definitions including palette, typography, spacing, and effects.
 * Uses JSON serialization for complex nested structures.
 *
 * @property id Unique theme identifier (UUID or theme name)
 * @property name Theme display name
 * @property version Theme version (semver format)
 * @property author Theme author/creator
 * @property themeData Complete theme definition (serialized as JSON)
 * @property isActive Whether this theme is currently applied
 * @property createdAt Creation timestamp
 * @property updatedAt Last update timestamp
 */
@Entity(
    tableName = "theme_configs",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["is_active"]),
        Index(value = ["created_at"])
    ]
)
@TypeConverters(ThemeDataConverter::class)
data class ThemeConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "version")
    val version: String,

    @ColumnInfo(name = "author")
    val author: String? = null,

    @ColumnInfo(name = "theme_data")
    val themeData: ThemeData,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Theme data wrapper for all theme components.
 * Serialized to JSON for database storage.
 */
@kotlinx.serialization.Serializable
data class ThemeData(
    val colors: Map<String, String>,
    val typography: Map<String, Any>,
    val spacing: Map<String, Int>,
    val effects: Map<String, Any>? = null
)
```

### 1.4 IMUConfig Entity

```kotlin
package com.augmentalis.avaui.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.ColumnInfo
import androidx.room.Index
import com.augmentalis.avaui.database.converters.IMUSettingsConverter

/**
 * IMU (Inertial Measurement Unit) configuration entity.
 *
 * Stores sensor configuration for motion-based UI interactions.
 *
 * @property id Unique config identifier
 * @property name Configuration name/profile
 * @property axisLocks Which axes are locked (serialized as JSON)
 * @property rateLimits Rate limits per axis (serialized as JSON)
 * @property smoothingSettings Smoothing algorithm parameters (serialized as JSON)
 * @property isActive Whether this config is currently active
 * @property createdAt Creation timestamp
 * @property updatedAt Last update timestamp
 */
@Entity(
    tableName = "imu_configs",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["is_active"])
    ]
)
@TypeConverters(IMUSettingsConverter::class)
data class IMUConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "axis_locks")
    val axisLocks: AxisLocks,

    @ColumnInfo(name = "rate_limits")
    val rateLimits: RateLimits,

    @ColumnInfo(name = "smoothing_settings")
    val smoothingSettings: SmoothingSettings,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * IMU axis lock configuration.
 */
@kotlinx.serialization.Serializable
data class AxisLocks(
    val x: Boolean = false,
    val y: Boolean = false,
    val z: Boolean = false
)

/**
 * IMU rate limit configuration (updates per second).
 */
@kotlinx.serialization.Serializable
data class RateLimits(
    val x: Float = 60f,
    val y: Float = 60f,
    val z: Float = 60f
)

/**
 * IMU smoothing algorithm settings.
 */
@kotlinx.serialization.Serializable
data class SmoothingSettings(
    val algorithm: String = "exponential",  // "exponential", "moving_average", "kalman"
    val windowSize: Int = 5,
    val alpha: Float = 0.3f  // For exponential smoothing
)
```

---

## 2. Type Converters

### 2.1 PropertiesConverter (Map<String, Any>)

```kotlin
package com.augmentalis.avaui.database.converters

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

/**
 * Type converter for Map<String, Any> properties.
 *
 * Uses kotlinx.serialization to serialize/deserialize maps to JSON.
 * Handles primitive types, strings, lists, and nested maps.
 *
 * Performance: ~1ms per conversion for typical 10-field maps.
 *
 * Supported value types:
 * - Primitives: Int, Long, Float, Double, Boolean
 * - Strings
 * - Lists and Maps (nested)
 */
class PropertiesConverter {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Convert Map to JSON string for database storage.
     */
    @TypeConverter
    fun fromPropertiesMap(value: Map<String, Any>?): String? {
        if (value == null) return null
        return json.encodeToString(value.toJsonElement())
    }

    /**
     * Convert JSON string to Map for application use.
     */
    @TypeConverter
    fun toPropertiesMap(value: String?): Map<String, Any>? {
        if (value == null) return null
        val jsonElement = json.parseToJsonElement(value)
        return jsonElement.toMap()
    }

    // Helper: Convert Map to JsonElement
    private fun Map<String, Any>.toJsonElement(): JsonElement {
        val map = mutableMapOf<String, JsonElement>()
        this.forEach { (key, value) ->
            map[key] = when (value) {
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is List<*> -> JsonArray(value.map { it.toJsonPrimitive() })
                is Map<*, *> -> (value as Map<String, Any>).toJsonElement()
                else -> JsonPrimitive(value.toString())
            }
        }
        return JsonObject(map)
    }

    // Helper: Convert Any to JsonPrimitive
    private fun Any?.toJsonPrimitive(): JsonElement {
        return when (this) {
            null -> JsonNull
            is Number -> JsonPrimitive(this)
            is Boolean -> JsonPrimitive(this)
            else -> JsonPrimitive(this.toString())
        }
    }

    // Helper: Convert JsonElement to Map
    private fun JsonElement.toMap(): Map<String, Any> {
        if (this !is JsonObject) return emptyMap()
        return this.mapValues { (_, value) ->
            when (value) {
                is JsonPrimitive -> {
                    when {
                        value.isString -> value.content
                        value.booleanOrNull != null -> value.boolean
                        value.intOrNull != null -> value.int
                        value.longOrNull != null -> value.long
                        value.floatOrNull != null -> value.float
                        value.doubleOrNull != null -> value.double
                        else -> value.content
                    }
                }
                is JsonArray -> value.map { it.toPrimitive() }
                is JsonObject -> value.toMap()
                else -> value.toString()
            }
        }
    }

    // Helper: Convert JsonElement to primitive
    private fun JsonElement.toPrimitive(): Any {
        return when (this) {
            is JsonPrimitive -> {
                when {
                    isString -> content
                    booleanOrNull != null -> boolean
                    intOrNull != null -> int
                    longOrNull != null -> long
                    floatOrNull != null -> float
                    doubleOrNull != null -> double
                    else -> content
                }
            }
            else -> this.toString()
        }
    }
}
```

### 2.2 Additional Converters

```kotlin
package com.augmentalis.avaui.database.converters

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import com.augmentalis.avaui.database.entities.*

/**
 * Type converter for ComponentPosition.
 */
class PositionConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromPosition(value: ComponentPosition?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toPosition(value: String?): ComponentPosition? {
        return value?.let { json.decodeFromString(it) }
    }
}

/**
 * Type converter for List<String> (tags).
 */
class StringListConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let { json.decodeFromString(it) }
    }
}

/**
 * Type converter for ThemeData.
 */
class ThemeDataConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromThemeData(value: ThemeData?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toThemeData(value: String?): ThemeData? {
        return value?.let { json.decodeFromString(it) }
    }
}

/**
 * Type converter for IMU settings.
 */
class IMUSettingsConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromAxisLocks(value: AxisLocks?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toAxisLocks(value: String?): AxisLocks? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromRateLimits(value: RateLimits?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toRateLimits(value: String?): RateLimits? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromSmoothingSettings(value: SmoothingSettings?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toSmoothingSettings(value: String?): SmoothingSettings? {
        return value?.let { json.decodeFromString(it) }
    }
}
```

---

## 3. DAO Interfaces

### 3.1 ComponentDao

```kotlin
package com.augmentalis.avaui.database.daos

import androidx.room.*
import com.augmentalis.avaui.database.entities.ComponentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ComponentEntity.
 *
 * Provides CRUD operations with performance optimizations for 1000+ components.
 * All queries are suspending functions for coroutine support.
 */
@Dao
interface ComponentDao {

    /**
     * Insert a single component.
     *
     * @param component Component to insert
     * @return Row ID of inserted component
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(component: ComponentEntity): Long

    /**
     * Insert multiple components (batch operation).
     * Optimized for bulk inserts when loading scenarios.
     *
     * @param components List of components to insert
     * @return List of row IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(components: List<ComponentEntity>): List<Long>

    /**
     * Update an existing component.
     *
     * @param component Component with updated values
     */
    @Update
    suspend fun update(component: ComponentEntity)

    /**
     * Delete a component by ID.
     *
     * @param componentId Component UUID to delete
     */
    @Query("DELETE FROM components WHERE id = :componentId")
    suspend fun deleteById(componentId: String)

    /**
     * Delete all components for a scenario.
     * Used when deleting or clearing a layout scenario.
     *
     * @param scenarioId Scenario UUID
     */
    @Query("DELETE FROM components WHERE scenario_id = :scenarioId")
    suspend fun deleteByScenarioId(scenarioId: String)

    /**
     * Get a component by ID.
     *
     * @param componentId Component UUID
     * @return Component entity or null if not found
     */
    @Query("SELECT * FROM components WHERE id = :componentId")
    suspend fun getById(componentId: String): ComponentEntity?

    /**
     * Get all components for a scenario.
     * Returns Flow for reactive updates.
     *
     * @param scenarioId Scenario UUID
     * @return Flow of component list
     */
    @Query("SELECT * FROM components WHERE scenario_id = :scenarioId ORDER BY created_at ASC")
    fun getByScenarioId(scenarioId: String): Flow<List<ComponentEntity>>

    /**
     * Get components by type (e.g., all buttons).
     *
     * @param type Component type string
     * @return List of components of specified type
     */
    @Query("SELECT * FROM components WHERE type = :type")
    suspend fun getByType(type: String): List<ComponentEntity>

    /**
     * Get component count for a scenario.
     * Useful for updating cached count in LayoutScenarioEntity.
     *
     * @param scenarioId Scenario UUID
     * @return Number of components
     */
    @Query("SELECT COUNT(*) FROM components WHERE scenario_id = :scenarioId")
    suspend fun getCountByScenarioId(scenarioId: String): Int

    /**
     * Search components by property value.
     * Note: JSON field search is slower, use sparingly.
     *
     * @param searchTerm Property value to search for
     * @return List of matching components
     */
    @Query("SELECT * FROM components WHERE properties LIKE '%' || :searchTerm || '%'")
    suspend fun searchByProperty(searchTerm: String): List<ComponentEntity>
}
```

### 3.2 LayoutScenarioDao

```kotlin
package com.augmentalis.avaui.database.daos

import androidx.room.*
import com.augmentalis.avaui.database.entities.LayoutScenarioEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for LayoutScenarioEntity.
 */
@Dao
interface LayoutScenarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scenario: LayoutScenarioEntity): Long

    @Update
    suspend fun update(scenario: LayoutScenarioEntity)

    @Delete
    suspend fun delete(scenario: LayoutScenarioEntity)

    @Query("DELETE FROM layout_scenarios WHERE id = :scenarioId")
    suspend fun deleteById(scenarioId: String)

    @Query("SELECT * FROM layout_scenarios WHERE id = :scenarioId")
    suspend fun getById(scenarioId: String): LayoutScenarioEntity?

    /**
     * Get all scenarios, ordered by timestamp (newest first).
     */
    @Query("SELECT * FROM layout_scenarios ORDER BY timestamp DESC")
    fun getAll(): Flow<List<LayoutScenarioEntity>>

    /**
     * Get active scenario (only one should be active at a time).
     */
    @Query("SELECT * FROM layout_scenarios WHERE is_active = 1 LIMIT 1")
    suspend fun getActive(): LayoutScenarioEntity?

    /**
     * Deactivate all scenarios.
     * Call before activating a new scenario.
     */
    @Query("UPDATE layout_scenarios SET is_active = 0")
    suspend fun deactivateAll()

    /**
     * Activate a specific scenario.
     */
    @Query("UPDATE layout_scenarios SET is_active = 1 WHERE id = :scenarioId")
    suspend fun activate(scenarioId: String)

    /**
     * Update component count for a scenario.
     */
    @Query("UPDATE layout_scenarios SET component_count = :count WHERE id = :scenarioId")
    suspend fun updateComponentCount(scenarioId: String, count: Int)

    /**
     * Search scenarios by name or tags.
     */
    @Query("SELECT * FROM layout_scenarios WHERE name LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<LayoutScenarioEntity>

    /**
     * Get scenarios by tag.
     */
    @Query("SELECT * FROM layout_scenarios WHERE tags LIKE '%' || :tag || '%'")
    suspend fun getByTag(tag: String): List<LayoutScenarioEntity>
}
```

### 3.3 ThemeConfigDao

```kotlin
package com.augmentalis.avaui.database.daos

import androidx.room.*
import com.augmentalis.avaui.database.entities.ThemeConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ThemeConfigEntity.
 */
@Dao
interface ThemeConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(theme: ThemeConfigEntity): Long

    @Update
    suspend fun update(theme: ThemeConfigEntity)

    @Delete
    suspend fun delete(theme: ThemeConfigEntity)

    @Query("DELETE FROM theme_configs WHERE id = :themeId")
    suspend fun deleteById(themeId: String)

    @Query("SELECT * FROM theme_configs WHERE id = :themeId")
    suspend fun getById(themeId: String): ThemeConfigEntity?

    @Query("SELECT * FROM theme_configs WHERE name = :name")
    suspend fun getByName(name: String): ThemeConfigEntity?

    @Query("SELECT * FROM theme_configs ORDER BY created_at DESC")
    fun getAll(): Flow<List<ThemeConfigEntity>>

    @Query("SELECT * FROM theme_configs WHERE is_active = 1 LIMIT 1")
    suspend fun getActive(): ThemeConfigEntity?

    @Query("UPDATE theme_configs SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE theme_configs SET is_active = 1 WHERE id = :themeId")
    suspend fun activate(themeId: String)
}
```

### 3.4 IMUConfigDao

```kotlin
package com.augmentalis.avaui.database.daos

import androidx.room.*
import com.augmentalis.avaui.database.entities.IMUConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for IMUConfigEntity.
 */
@Dao
interface IMUConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: IMUConfigEntity): Long

    @Update
    suspend fun update(config: IMUConfigEntity)

    @Delete
    suspend fun delete(config: IMUConfigEntity)

    @Query("DELETE FROM imu_configs WHERE id = :configId")
    suspend fun deleteById(configId: String)

    @Query("SELECT * FROM imu_configs WHERE id = :configId")
    suspend fun getById(configId: String): IMUConfigEntity?

    @Query("SELECT * FROM imu_configs WHERE name = :name")
    suspend fun getByName(name: String): IMUConfigEntity?

    @Query("SELECT * FROM imu_configs ORDER BY created_at DESC")
    fun getAll(): Flow<List<IMUConfigEntity>>

    @Query("SELECT * FROM imu_configs WHERE is_active = 1 LIMIT 1")
    suspend fun getActive(): IMUConfigEntity?

    @Query("UPDATE imu_configs SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE imu_configs SET is_active = 1 WHERE id = :configId")
    suspend fun activate(configId: String)
}
```

---

## 4. Database Class

```kotlin
package com.augmentalis.avaui.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.augmentalis.avaui.database.entities.*
import com.augmentalis.avaui.database.daos.*
import com.augmentalis.avaui.database.converters.*

/**
 * AvaUI Room Database.
 *
 * Version history:
 * - v1: Initial schema with all four entities
 *
 * To add migrations, see database/migrations/ package.
 */
@Database(
    entities = [
        ComponentEntity::class,
        LayoutScenarioEntity::class,
        ThemeConfigEntity::class,
        IMUConfigEntity::class
    ],
    version = 1,
    exportSchema = true  // Generate schema JSON for migration testing
)
@TypeConverters(
    PropertiesConverter::class,
    PositionConverter::class,
    StringListConverter::class,
    ThemeDataConverter::class,
    IMUSettingsConverter::class
)
abstract class AvaUIDatabase : RoomDatabase() {

    abstract fun componentDao(): ComponentDao
    abstract fun layoutScenarioDao(): LayoutScenarioDao
    abstract fun themeConfigDao(): ThemeConfigDao
    abstract fun imuConfigDao(): IMUConfigDao

    companion object {
        const val DATABASE_NAME = "avaui.db"
        const val DATABASE_VERSION = 1
    }
}
```

---

## 5. Database Migration Strategy

### 5.1 Migration Framework

```kotlin
package com.augmentalis.avaui.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 1 to version 2.
 *
 * Example: Adding a new column to ComponentEntity.
 *
 * Changes:
 * - Add "order_index" column to components table
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE components
            ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )

        // Create index for new column
        db.execSQL(
            """
            CREATE INDEX index_components_order_index
            ON components(order_index)
            """.trimIndent()
        )
    }
}

/**
 * Migration from version 2 to version 3.
 *
 * Example: Adding a new entity for component templates.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create new table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS component_templates (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                properties TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Create indices
        db.execSQL("CREATE INDEX index_component_templates_name ON component_templates(name)")
        db.execSQL("CREATE INDEX index_component_templates_type ON component_templates(type)")
    }
}

/**
 * Destructive migration fallback.
 *
 * WARNING: This will delete all data!
 * Use only for development or when data loss is acceptable.
 */
val DESTRUCTIVE_MIGRATION_FALLBACK = object : Migration(0, Int.MAX_VALUE) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Drop all tables
        db.execSQL("DROP TABLE IF EXISTS components")
        db.execSQL("DROP TABLE IF EXISTS layout_scenarios")
        db.execSQL("DROP TABLE IF EXISTS theme_configs")
        db.execSQL("DROP TABLE IF EXISTS imu_configs")

        // Tables will be recreated by Room
    }
}
```

### 5.2 Migration Best Practices

**1. Always Test Migrations**

```kotlin
package com.augmentalis.avaui.database.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AvaUIDatabase::class.java
    )

    @Test
    fun migrate1To2() {
        // Create database at version 1
        helper.createDatabase(TEST_DB_NAME, 1).apply {
            // Insert test data
            execSQL(
                """
                INSERT INTO components (id, scenario_id, type, position, properties, created_at, updated_at)
                VALUES ('test-1', 'scenario-1', 'Button', '{"x":0,"y":0}', '{}', 0, 0)
                """.trimIndent()
            )
            close()
        }

        // Run migration
        helper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, MIGRATION_1_2)

        // Verify migration
        helper.getMigrationDatabase(TEST_DB_NAME, 2).apply {
            // Query to verify new column exists
            query("SELECT order_index FROM components WHERE id = 'test-1'").use { cursor ->
                assert(cursor.moveToFirst())
                assert(cursor.getInt(0) == 0)  // Default value
            }
        }
    }

    companion object {
        private const val TEST_DB_NAME = "migration-test"
    }
}
```

**2. Automated Migrations (Room 2.4.0+)**

```kotlin
package com.augmentalis.avaui.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.AutoMigration
import androidx.room.RenameColumn

/**
 * Example using AutoMigration for simple schema changes.
 */
@Database(
    entities = [ComponentEntity::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(
            from = 2,
            to = 3,
            spec = ComponentEntity.RenamePropertyColumnSpec::class
        )
    ]
)
abstract class AvaUIDatabaseWithAutoMigration : RoomDatabase() {
    // DAOs...
}

// AutoMigrationSpec for column rename
@RenameColumn(
    tableName = "components",
    fromColumnName = "properties",
    toColumnName = "component_properties"
)
class RenamePropertyColumnSpec : AutoMigrationSpec
```

**3. Version Management**

```kotlin
// In app build.gradle.kts
android {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }
}

// This generates JSON schema files for each version:
// schemas/1.json
// schemas/2.json
// schemas/3.json
//
// These files are used for migration testing and documentation.
```

**4. Production Migration Strategy**

```kotlin
package com.augmentalis.avaui.database

import androidx.room.Room
import android.content.Context

object DatabaseFactory {

    fun createDatabase(context: Context, enableDestructiveMigration: Boolean = false): AvaUIDatabase {
        val builder = Room.databaseBuilder(
            context.applicationContext,
            AvaUIDatabase::class.java,
            AvaUIDatabase.DATABASE_NAME
        )

        return if (enableDestructiveMigration) {
            // Development mode: destroy and recreate on schema changes
            builder
                .fallbackToDestructiveMigration()
                .build()
        } else {
            // Production mode: require explicit migrations
            builder
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
```

---

## 6. KMP Platform Setup

### 6.1 Common Main (Expect Declarations)

```kotlin
// In commonMain/database/DatabaseBuilder.kt
package com.augmentalis.avaui.database

import androidx.room.RoomDatabase

/**
 * Platform-specific database builder.
 * Each platform provides its own implementation.
 */
expect object DatabaseBuilder {
    fun buildDatabase(): AvaUIDatabase
}
```

### 6.2 Android Platform

```kotlin
// In androidMain/database/DatabaseBuilder.kt
package com.augmentalis.avaui.database

import android.content.Context
import androidx.room.Room
import com.augmentalis.avaui.database.migrations.*

actual object DatabaseBuilder {

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun buildDatabase(): AvaUIDatabase {
        return Room.databaseBuilder(
            appContext,
            AvaUIDatabase::class.java,
            AvaUIDatabase.DATABASE_NAME
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
    }
}
```

### 6.3 JVM Platform

```kotlin
// In jvmMain/database/DatabaseBuilder.kt
package com.augmentalis.avaui.database

import androidx.room.Room
import java.io.File

actual object DatabaseBuilder {

    actual fun buildDatabase(): AvaUIDatabase {
        val dbFile = File(System.getProperty("user.home"), ".avaui/${AvaUIDatabase.DATABASE_NAME}")
        dbFile.parentFile?.mkdirs()

        return Room.databaseBuilder<AvaUIDatabase>(
            name = dbFile.absolutePath
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
    }
}
```

### 6.4 iOS Platform

```kotlin
// In iosMain/database/DatabaseBuilder.kt
package com.augmentalis.avaui.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual object DatabaseBuilder {

    actual fun buildDatabase(): AvaUIDatabase {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )

        val dbPath = requireNotNull(documentDirectory?.path) + "/${AvaUIDatabase.DATABASE_NAME}"

        return Room.databaseBuilder<AvaUIDatabase>(
            name = dbPath,
            factory = { AvaUIDatabase::class.instantiateImpl() }
        )
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
    }
}
```

### 6.5 Gradle Configuration

```kotlin
// In build.gradle.kts
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("androidx.room:room-runtime:2.7.0")
                implementation("androidx.sqlite:sqlite-bundled:2.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }

        androidMain {
            dependencies {
                implementation("androidx.room:room-ktx:2.7.0")
            }
        }

        iosMain {
            dependencies {
                implementation("androidx.room:room-runtime:2.7.0")
            }
        }
    }
}

dependencies {
    // KSP for Room code generation
    add("kspCommonMainMetadata", "androidx.room:room-compiler:2.7.0")
    add("kspAndroid", "androidx.room:room-compiler:2.7.0")
    add("kspIosX64", "androidx.room:room-compiler:2.7.0")
    add("kspIosArm64", "androidx.room:room-compiler:2.7.0")
    add("kspIosSimulatorArm64", "androidx.room:room-compiler:2.7.0")
}
```

---

## 7. Performance Optimization

### 7.1 Indexing Strategy

**Current Indices (Already in Schema):**
- `components`: `scenario_id`, `type`, `created_at`
- `layout_scenarios`: `name`, `timestamp`, `is_active`
- `theme_configs`: `name` (unique), `is_active`, `created_at`
- `imu_configs`: `name` (unique), `is_active`

**Additional Indices for 1000+ Components:**

```kotlin
// Add to ComponentEntity if query performance degrades
indices = [
    Index(value = ["scenario_id", "type"]),  // Composite for filtered lists
    Index(value = ["scenario_id", "created_at"]),  // For ordered retrieval
    Index(value = ["updated_at"])  // For "recently modified" queries
]
```

### 7.2 Batch Operations

```kotlin
// Example: Efficient bulk insert
suspend fun loadScenarioWithComponents(
    scenario: LayoutScenarioEntity,
    components: List<ComponentEntity>
) {
    db.withTransaction {
        layoutScenarioDao.insert(scenario)
        componentDao.insertAll(components)  // Single transaction, multiple inserts
        layoutScenarioDao.updateComponentCount(scenario.id, components.size)
    }
}
```

### 7.3 Pagination for Large Datasets

```kotlin
// Add to ComponentDao for paginated loading
@Query("""
    SELECT * FROM components
    WHERE scenario_id = :scenarioId
    ORDER BY created_at ASC
    LIMIT :limit OFFSET :offset
""")
suspend fun getPagedByScenarioId(
    scenarioId: String,
    limit: Int,
    offset: Int
): List<ComponentEntity>
```

### 7.4 Performance Benchmarks

**Expected Performance (on mid-range device):**

| Operation | 100 Components | 1000 Components | 5000 Components |
|-----------|---------------|-----------------|-----------------|
| Insert All | ~10ms | ~80ms | ~400ms |
| Query All | ~5ms | ~40ms | ~200ms |
| Update One | ~2ms | ~2ms | ~2ms |
| Delete by Scenario | ~8ms | ~60ms | ~300ms |

**Optimization Triggers:**
- Query time > 100ms: Add indices
- Insert time > 500ms: Increase batch size, use transactions
- JSON parsing > 50ms per record: Consider denormalizing frequently-queried fields

---

## 8. Alternative: SQLDelight Comparison

### 8.1 When to Consider SQLDelight

**Choose SQLDelight if:**
- Team has strong SQL expertise
- Need maximum control over SQL queries
- Prefer SQL as source of truth
- Already using SQLDelight in codebase

**Stick with Room if:**
- Team has Android/Kotlin background (Room is more familiar)
- Prefer annotation-based approach
- Want Google's long-term support and updates
- Need seamless Android ecosystem integration

### 8.2 Quick SQLDelight Example

```sql
-- In components.sq
CREATE TABLE ComponentEntity (
    id TEXT NOT NULL PRIMARY KEY,
    scenario_id TEXT NOT NULL,
    type TEXT NOT NULL,
    position TEXT NOT NULL,
    properties TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (scenario_id) REFERENCES LayoutScenarioEntity(id) ON DELETE CASCADE
);

CREATE INDEX component_scenario_idx ON ComponentEntity(scenario_id);
CREATE INDEX component_type_idx ON ComponentEntity(type);

-- Query definitions
selectAll:
SELECT * FROM ComponentEntity;

selectByScenarioId:
SELECT * FROM ComponentEntity WHERE scenario_id = ?;

insert:
INSERT INTO ComponentEntity(id, scenario_id, type, position, properties, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?);
```

**Pros of SQLDelight:**
- Compile-time SQL verification
- More explicit query control
- Smaller generated code footprint

**Cons of SQLDelight:**
- More boilerplate (write raw SQL for everything)
- Steeper learning curve for Kotlin-first developers
- Less Android ecosystem integration

---

## 9. Implementation Checklist

### Phase 1: Core Schema
- [ ] Define all four entity classes with annotations
- [ ] Implement type converters (PropertiesConverter is most complex)
- [ ] Create DAO interfaces with basic CRUD operations
- [ ] Define database class with all converters
- [ ] Write unit tests for type converters

### Phase 2: Platform Setup
- [ ] Create expect/actual DatabaseBuilder for all platforms
- [ ] Configure Gradle with Room KSP dependencies
- [ ] Test database creation on Android
- [ ] Test database creation on JVM
- [ ] Test database creation on iOS

### Phase 3: Migrations
- [ ] Enable schema export in build config
- [ ] Create migration test infrastructure
- [ ] Write first migration (example: v1 to v2)
- [ ] Test migration with MigrationTestHelper
- [ ] Document migration strategy for team

### Phase 4: Performance Testing
- [ ] Benchmark insert/query operations with 1000+ components
- [ ] Add indices if query times exceed 100ms
- [ ] Test batch operations
- [ ] Profile JSON serialization overhead
- [ ] Optimize hot paths

### Phase 5: Production Readiness
- [ ] Add proper error handling in DAOs
- [ ] Implement database backup/restore
- [ ] Add database version reporting
- [ ] Write developer documentation
- [ ] Create migration runbook for production

---

## 10. Conclusion

**Summary:**
- **Room KMP is production-ready** for Android, JVM, and iOS as of 2025
- **Type converters solve Map<String, Any>** using kotlinx.serialization
- **Automated migrations** reduce boilerplate for simple schema changes
- **Manual migrations** handle complex transformations
- **Performance is sufficient** for 1000+ components with proper indexing

**Key Decisions:**
1. Use Room over SQLDelight (better for Kotlin-first teams)
2. JSON serialization for complex types (Map, nested objects)
3. Foreign key cascade for scenario-component relationship
4. Indexed queries for performance at scale
5. Platform-specific database builders for KMP

**Next Steps:**
1. Review schema with team
2. Implement Phase 1 (core schema)
3. Set up migration testing infrastructure
4. Benchmark on target devices
5. Iterate on performance optimizations

---

**Created by Manoj Jhawar, manoj@ideahq.net**
