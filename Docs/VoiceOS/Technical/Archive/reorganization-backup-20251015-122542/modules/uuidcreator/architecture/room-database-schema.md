/**
 * Room Database Schema Design
 * Path: /docs/modules/UUIDCreator/architecture/roomDatabaseSchema.md
 *
 * Created: 2025-10-08 00:35 PST
 * Last Modified: 2025-10-08 00:35 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * Module: UUIDCreator
 *
 * Purpose: Define Room database schema for UUIDCreator hybrid storage
 *
 * Changelog:
 * - v1.0.0 (2025-10-08 00:35 PST): Initial schema design for Phase 2
 */

# UUIDCreator Room Database Schema

**Version**: 1
**Database Name**: `uuid_creator_database`
**Technology**: AndroidX Room + KSP

---

## 🎯 Design Goals

1. **Hybrid Storage**: Room (on-disk) + ConcurrentHashMap (in-memory cache)
2. **Performance**: O(1) lookups from in-memory cache
3. **Persistence**: All data survives app restarts
4. **Analytics**: Track usage patterns and performance
5. **Hierarchy**: Support parent-child relationships
6. **Lazy Loading**: Load database on first access
7. **Thread Safety**: Maintain existing thread-safe operations

---

## 📊 Schema Overview

### Entity Relationship Diagram

```
┌─────────────────────┐
│  UUIDElementEntity  │ (Main element storage)
│  - uuid (PK)        │
│  - name             │
│  - type             │
│  - description      │
│  - parent_uuid      │
│  - is_enabled       │
│  - priority         │
│  - timestamp        │
│  - metadata_json    │
│  - position_json    │
└─────────────────────┘
           │
           │ 1:N
           ▼
┌─────────────────────┐
│ UUIDHierarchyEntity │ (Parent-child relationships - normalized)
│  - id (PK)          │
│  - parent_uuid (FK) │
│  - child_uuid (FK)  │
│  - depth            │
│  - path             │
│  - order_index      │
└─────────────────────┘
           │
           │ 1:N
           ▼
┌─────────────────────┐
│ UUIDAnalyticsEntity │ (Usage tracking)
│  - uuid (PK/FK)     │
│  - access_count     │
│  - first_accessed   │
│  - last_accessed    │
│  - execution_time   │
│  - success_count    │
│  - failure_count    │
│  - lifecycle_state  │
└─────────────────────┘
```

---

## 📋 Entity Definitions

### 1. UUIDElementEntity

**Purpose**: Store core UUID element data

**Table Name**: `uuid_elements`

```kotlin
@Entity(
    tableName = "uuid_elements",
    indices = [
        Index(value = ["name"], name = "idx_uuid_element_name"),
        Index(value = ["type"], name = "idx_uuid_element_type"),
        Index(value = ["parent_uuid"], name = "idx_uuid_element_parent"),
        Index(value = ["timestamp"], name = "idx_uuid_element_timestamp")
    ]
)
data class UUIDElementEntity(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    val uuid: String,

    @ColumnInfo(name = "name")
    val name: String?,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "parent_uuid")
    val parentUuid: String?,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "priority")
    val priority: Int = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    // JSON serialized fields
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String?,  // Serialized UUIDMetadata

    @ColumnInfo(name = "position_json")
    val positionJson: String?   // Serialized UUIDPosition
)
```

**Indexes**:
- `name` - Fast lookup by element name
- `type` - Fast lookup by element type
- `parent_uuid` - Fast hierarchy queries
- `timestamp` - Chronological ordering

**JSON Fields**:
- `metadata_json` - Stores UUIDMetadata as JSON string
- `position_json` - Stores UUIDPosition as JSON string

**Excluded from Entity** (kept in-memory only):
- `actions: Map<String, (Map<String, Any>) -> Unit>` - Function references cannot be serialized
- `children: MutableList<String>` - Stored in UUIDHierarchyEntity (normalized)

---

### 2. UUIDHierarchyEntity

**Purpose**: Store parent-child relationships in normalized form

**Table Name**: `uuid_hierarchy`

```kotlin
@Entity(
    tableName = "uuid_hierarchy",
    indices = [
        Index(value = ["parent_uuid"], name = "idx_hierarchy_parent"),
        Index(value = ["child_uuid"], name = "idx_hierarchy_child"),
        Index(value = ["depth"], name = "idx_hierarchy_depth"),
        Index(value = ["path"], name = "idx_hierarchy_path")
    ],
    foreignKeys = [
        ForeignKey(
            entity = UUIDElementEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["parent_uuid"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UUIDElementEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["child_uuid"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UUIDHierarchyEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "parent_uuid")
    val parentUuid: String,

    @ColumnInfo(name = "child_uuid")
    val childUuid: String,

    @ColumnInfo(name = "depth")
    val depth: Int = 0,  // 0 = direct child, 1+ = nested depth

    @ColumnInfo(name = "path")
    val path: String,  // e.g., "/root/parent/child" for hierarchy traversal

    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0  // Preserve child order
)
```

**Indexes**:
- `parent_uuid` - Fast lookup of all children
- `child_uuid` - Fast lookup of parent
- `depth` - Query by hierarchy depth
- `path` - Hierarchical path queries

**Foreign Keys**:
- CASCADE delete - removing parent removes all hierarchy entries

**Why Normalized**:
- Avoids JSON array in UUIDElementEntity
- Enables efficient hierarchy queries
- Supports deep traversal queries
- Maintains referential integrity

---

### 3. UUIDAnalyticsEntity

**Purpose**: Track usage statistics and performance metrics

**Table Name**: `uuid_analytics`

```kotlin
@Entity(
    tableName = "uuid_analytics",
    indices = [
        Index(value = ["access_count"], name = "idx_analytics_access_count"),
        Index(value = ["last_accessed"], name = "idx_analytics_last_accessed"),
        Index(value = ["lifecycle_state"], name = "idx_analytics_lifecycle")
    ],
    foreignKeys = [
        ForeignKey(
            entity = UUIDElementEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["uuid"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UUIDAnalyticsEntity(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    val uuid: String,

    @ColumnInfo(name = "access_count")
    val accessCount: Long = 0,

    @ColumnInfo(name = "first_accessed")
    val firstAccessed: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "execution_time_ms")
    val executionTimeMs: Long = 0,  // Cumulative execution time

    @ColumnInfo(name = "success_count")
    val successCount: Long = 0,

    @ColumnInfo(name = "failure_count")
    val failureCount: Long = 0,

    @ColumnInfo(name = "lifecycle_state")
    val lifecycleState: String = "created"  // created, active, deprecated, deleted
)
```

**Indexes**:
- `access_count` - Find most/least used elements
- `last_accessed` - Find recently used elements
- `lifecycle_state` - Filter by lifecycle

**Lifecycle States**:
- `created` - Newly registered
- `active` - Being used regularly
- `deprecated` - Marked for removal
- `deleted` - Soft delete before cascade

---

## 🔧 Type Converters

**Purpose**: Convert complex types to/from database-compatible types

```kotlin
class UUIDCreatorTypeConverters {

    private val gson = Gson()

    // UUIDMetadata converters
    @TypeConverter
    fun fromUUIDMetadata(metadata: UUIDMetadata?): String? {
        return metadata?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toUUIDMetadata(json: String?): UUIDMetadata? {
        return json?.let { gson.fromJson(it, UUIDMetadata::class.java) }
    }

    // UUIDPosition converters
    @TypeConverter
    fun fromUUIDPosition(position: UUIDPosition?): String? {
        return position?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toUUIDPosition(json: String?): UUIDPosition? {
        return json?.let { gson.fromJson(it, UUIDPosition::class.java) }
    }
}
```

---

## 📝 DAO Interfaces

### UUIDElementDao

```kotlin
@Dao
interface UUIDElementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(element: UUIDElementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(elements: List<UUIDElementEntity>)

    @Update
    suspend fun update(element: UUIDElementEntity)

    @Delete
    suspend fun delete(element: UUIDElementEntity)

    @Query("DELETE FROM uuid_elements WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)

    @Query("SELECT * FROM uuid_elements")
    suspend fun getAll(): List<UUIDElementEntity>

    @Query("SELECT * FROM uuid_elements WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): UUIDElementEntity?

    @Query("SELECT * FROM uuid_elements WHERE name = :name")
    suspend fun getByName(name: String): List<UUIDElementEntity>

    @Query("SELECT * FROM uuid_elements WHERE type = :type")
    suspend fun getByType(type: String): List<UUIDElementEntity>

    @Query("SELECT * FROM uuid_elements WHERE parent_uuid = :parentUuid")
    suspend fun getChildren(parentUuid: String): List<UUIDElementEntity>

    @Query("SELECT * FROM uuid_elements WHERE is_enabled = :enabled")
    suspend fun getByEnabled(enabled: Boolean): List<UUIDElementEntity>

    @Query("SELECT COUNT(*) FROM uuid_elements")
    suspend fun getCount(): Int

    @Query("DELETE FROM uuid_elements")
    suspend fun deleteAll()
}
```

### UUIDHierarchyDao

```kotlin
@Dao
interface UUIDHierarchyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hierarchy: UUIDHierarchyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hierarchies: List<UUIDHierarchyEntity>)

    @Delete
    suspend fun delete(hierarchy: UUIDHierarchyEntity)

    @Query("SELECT * FROM uuid_hierarchy WHERE parent_uuid = :parentUuid ORDER BY order_index")
    suspend fun getChildren(parentUuid: String): List<UUIDHierarchyEntity>

    @Query("SELECT * FROM uuid_hierarchy WHERE child_uuid = :childUuid")
    suspend fun getParent(childUuid: String): UUIDHierarchyEntity?

    @Query("SELECT * FROM uuid_hierarchy WHERE path LIKE :pathPrefix || '%'")
    suspend fun getDescendants(pathPrefix: String): List<UUIDHierarchyEntity>

    @Query("SELECT * FROM uuid_hierarchy WHERE depth = :depth")
    suspend fun getByDepth(depth: Int): List<UUIDHierarchyEntity>

    @Query("DELETE FROM uuid_hierarchy WHERE parent_uuid = :parentUuid")
    suspend fun deleteByParent(parentUuid: String)

    @Query("DELETE FROM uuid_hierarchy WHERE child_uuid = :childUuid")
    suspend fun deleteByChild(childUuid: String)
}
```

### UUIDAnalyticsDao

```kotlin
@Dao
interface UUIDAnalyticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analytics: UUIDAnalyticsEntity)

    @Update
    suspend fun update(analytics: UUIDAnalyticsEntity)

    @Query("SELECT * FROM uuid_analytics WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): UUIDAnalyticsEntity?

    @Query("SELECT * FROM uuid_analytics ORDER BY access_count DESC LIMIT :limit")
    suspend fun getMostUsed(limit: Int = 10): List<UUIDAnalyticsEntity>

    @Query("SELECT * FROM uuid_analytics ORDER BY access_count ASC LIMIT :limit")
    suspend fun getLeastUsed(limit: Int = 10): List<UUIDAnalyticsEntity>

    @Query("SELECT * FROM uuid_analytics ORDER BY last_accessed DESC LIMIT :limit")
    suspend fun getRecentlyUsed(limit: Int = 10): List<UUIDAnalyticsEntity>

    @Query("SELECT * FROM uuid_analytics WHERE lifecycle_state = :state")
    suspend fun getByLifecycleState(state: String): List<UUIDAnalyticsEntity>

    @Query("UPDATE uuid_analytics SET access_count = access_count + 1, last_accessed = :timestamp WHERE uuid = :uuid")
    suspend fun incrementAccessCount(uuid: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE uuid_analytics SET success_count = success_count + 1 WHERE uuid = :uuid")
    suspend fun incrementSuccessCount(uuid: String)

    @Query("UPDATE uuid_analytics SET failure_count = failure_count + 1 WHERE uuid = :uuid")
    suspend fun incrementFailureCount(uuid: String)

    @Query("DELETE FROM uuid_analytics")
    suspend fun deleteAll()
}
```

---

## 🗄️ Database Class

```kotlin
@Database(
    entities = [
        UUIDElementEntity::class,
        UUIDHierarchyEntity::class,
        UUIDAnalyticsEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(UUIDCreatorTypeConverters::class)
abstract class UUIDCreatorDatabase : RoomDatabase() {

    abstract fun uuidElementDao(): UUIDElementDao
    abstract fun uuidHierarchyDao(): UUIDHierarchyDao
    abstract fun uuidAnalyticsDao(): UUIDAnalyticsDao

    companion object {
        private const val DATABASE_NAME = "uuid_creator_database"

        @Volatile
        private var INSTANCE: UUIDCreatorDatabase? = null

        fun getInstance(context: Context): UUIDCreatorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): UUIDCreatorDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                UUIDCreatorDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // For version 1
            .build()
        }
    }
}
```

---

## 🔄 Hybrid Storage Strategy

### Repository Pattern with In-Memory Cache

```kotlin
class UUIDRepository(
    private val elementDao: UUIDElementDao,
    private val hierarchyDao: UUIDHierarchyDao,
    private val analyticsDao: UUIDAnalyticsDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    // In-memory cache (existing ConcurrentHashMap)
    private val elementsCache = ConcurrentHashMap<String, UUIDElement>()
    private val nameIndexCache = ConcurrentHashMap<String, MutableSet<String>>()
    private val typeIndexCache = ConcurrentHashMap<String, MutableSet<String>>()
    private val hierarchyIndexCache = ConcurrentHashMap<String, MutableSet<String>>()

    private var isLoaded = false

    // Lazy load from Room on first access
    suspend fun loadCache() = withContext(dispatcher) {
        if (!isLoaded) {
            val elements = elementDao.getAll()
            val hierarchies = hierarchyDao.getAll()

            elements.forEach { entity ->
                val element = entity.toModel(
                    children = hierarchies
                        .filter { it.parentUuid == entity.uuid }
                        .map { it.childUuid }
                        .toMutableList()
                )
                elementsCache[element.uuid] = element

                // Build indexes
                element.name?.let { name ->
                    nameIndexCache.getOrPut(name.lowercase()) { mutableSetOf() }.add(element.uuid)
                }
                typeIndexCache.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.uuid)
                element.parent?.let { parent ->
                    hierarchyIndexCache.getOrPut(parent) { mutableSetOf() }.add(element.uuid)
                }
            }

            isLoaded = true
        }
    }

    // Insert: Update both Room and cache
    suspend fun insert(element: UUIDElement) = withContext(dispatcher) {
        // Save to Room
        elementDao.insert(element.toEntity())

        // Save hierarchy relationships
        element.children.forEachIndexed { index, childUuid ->
            hierarchyDao.insert(
                UUIDHierarchyEntity(
                    parentUuid = element.uuid,
                    childUuid = childUuid,
                    depth = 0,
                    path = "/${element.uuid}/$childUuid",
                    orderIndex = index
                )
            )
        }

        // Save analytics
        analyticsDao.insert(
            UUIDAnalyticsEntity(uuid = element.uuid)
        )

        // Update cache
        elementsCache[element.uuid] = element
        element.name?.let { name ->
            nameIndexCache.getOrPut(name.lowercase()) { mutableSetOf() }.add(element.uuid)
        }
        typeIndexCache.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.uuid)
    }

    // Get: O(1) from cache
    fun getByUuid(uuid: String): UUIDElement? = elementsCache[uuid]

    fun getAll(): List<UUIDElement> = elementsCache.values.toList()
}
```

---

## 📊 Migration Strategy

### Phase 2 Migration Steps:

1. **Create Entity Classes** - All 3 entities with annotations
2. **Create DAO Interfaces** - All CRUD operations
3. **Create Database Class** - Room database with type converters
4. **Create Repository** - Hybrid storage implementation
5. **Update UUIDRegistry** - Use repository instead of direct ConcurrentHashMap
6. **Add Lazy Loading** - Load database on first UUIDCreator instantiation
7. **Update Tests** - Test Room persistence and cache consistency

---

## ✅ Benefits of This Design

1. **Performance**: O(1) lookups from in-memory cache
2. **Persistence**: All data survives app restarts
3. **Analytics**: Built-in usage tracking
4. **Hierarchy**: Normalized relationships for efficient queries
5. **Type Safety**: Room compile-time validation
6. **Thread Safe**: Maintains existing ConcurrentHashMap safety
7. **Lazy Load**: No startup penalty, loads on demand
8. **Backward Compatible**: Existing UUIDElement model unchanged for API

---

**Schema Version**: 1
**Export Schema**: Yes (for migrations)
**Migration Strategy**: Version 1 = fallbackToDestructiveMigration (acceptable for initial version)

---

**End of Schema Design**
