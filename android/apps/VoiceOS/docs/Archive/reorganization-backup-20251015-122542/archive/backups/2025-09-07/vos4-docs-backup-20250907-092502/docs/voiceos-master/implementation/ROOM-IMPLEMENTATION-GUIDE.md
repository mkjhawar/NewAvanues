# Room Database Implementation Guide for VOS4
**Author:** Manoj Jhawar  
**Code-Reviewed-By:** CCA  
**Date:** 2025-09-07  
**Status:** ✅ ACTIVE  
**Version:** 1.0.0

## 📋 Overview

VOS4 uses Room database exclusively for all local data persistence. This guide provides comprehensive implementation patterns and best practices for Room database usage across all VOS4 modules.

## 🏗️ Architecture

### Database Hierarchy
```
VOS4 Room Database Architecture
├── VoiceOSDatabase (Main Database)
│   ├── 13 Entities
│   ├── 13 DAOs
│   └── Type Converters
├── LocalizationDatabase (Module-specific)
│   ├── UserPreference Entity
│   └── PreferencesDao
└── Future Module Databases
```

## 🔧 Implementation Patterns

### 1. Entity Definition
```kotlin
@Entity(tableName = "your_table_name")
data class YourEntity(
    @PrimaryKey val id: Long = 0,
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)
```

### 2. DAO Interface
```kotlin
@Dao
interface YourEntityDao {
    @Query("SELECT * FROM your_table_name")
    suspend fun getAll(): List<YourEntity>
    
    @Query("SELECT * FROM your_table_name WHERE id = :id")
    suspend fun getById(id: Long): YourEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: YourEntity): Long
    
    @Update
    suspend fun update(entity: YourEntity)
    
    @Delete
    suspend fun delete(entity: YourEntity)
    
    @Query("DELETE FROM your_table_name WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

### 3. Database Class
```kotlin
@Database(
    entities = [YourEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(YourTypeConverters::class)
abstract class YourDatabase : RoomDatabase() {
    abstract fun yourEntityDao(): YourEntityDao
    
    companion object {
        @Volatile
        private var INSTANCE: YourDatabase? = null
        
        fun getInstance(context: Context): YourDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    YourDatabase::class.java,
                    "your_database_name"
                )
                .fallbackToDestructiveMigration() // Development only
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### 4. Repository Pattern
```kotlin
class YourRepository(private val dao: YourEntityDao) {
    suspend fun insert(entity: YourEntity) = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }
    
    suspend fun getAll(): List<YourEntity> = withContext(Dispatchers.IO) {
        dao.getAll()
    }
    
    suspend fun update(entity: YourEntity) = withContext(Dispatchers.IO) {
        dao.update(entity)
    }
    
    suspend fun delete(entity: YourEntity) = withContext(Dispatchers.IO) {
        dao.delete(entity)
    }
}
```

## 📦 Module Integration

### Build Configuration (build.gradle.kts)
```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
}

android {
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.generateKotlin", "true")
    }
}

dependencies {
    val roomVersion = "2.6.1"
    
    // KSP for Room
    ksp("androidx.room:room-compiler:$roomVersion")
    
    // Room runtime
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
}
```

### Application Initialization
```kotlin
class VoiceOS : Application() {
    lateinit var database: VoiceOSDatabase
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Room database
        database = VoiceOSDatabase.getInstance(this)
        
        // Initialize DatabaseManager for modules
        DatabaseManager.init(this)
    }
}
```

## 🔄 Migration from ObjectBox

### Key Differences
| Aspect | ObjectBox | Room |
|--------|-----------|------|
| Query Language | Proprietary API | Standard SQL |
| Annotations | `@Id` | `@PrimaryKey` |
| Relationships | Limited | Full SQL support |
| Async | Built-in | Coroutines/Flow |
| Type Safety | Runtime | Compile-time |

### Migration Steps
1. Replace ObjectBox dependencies with Room
2. Convert `@Id` to `@PrimaryKey`
3. Create DAO interfaces for each entity
4. Implement Room database class
5. Update repositories to use DAOs
6. Convert queries to SQL

## 🎯 Best Practices

### 1. Always Use Suspend Functions
```kotlin
// ✅ GOOD
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    suspend fun getAll(): List<User>
}

// ❌ BAD - Blocks main thread
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAll(): List<User>
}
```

### 2. Use Flow for Reactive Data
```kotlin
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllFlow(): Flow<List<User>>
}
```

### 3. Type Converters for Complex Types
```kotlin
class Converters {
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        return value.split(",").map { it.trim() }
    }
    
    @TypeConverter
    fun fromListString(list: List<String>): String {
        return list.joinToString(",")
    }
}
```

### 4. Proper Error Handling
```kotlin
suspend fun safeInsert(entity: YourEntity): Result<Long> {
    return try {
        val id = dao.insert(entity)
        Result.success(id)
    } catch (e: SQLiteException) {
        Log.e(TAG, "Database insert failed", e)
        Result.failure(e)
    }
}
```

## 🚨 Common Pitfalls

### 1. Missing KSP Plugin
**Problem:** Room annotations not processed  
**Solution:** Add KSP plugin to build.gradle.kts

### 2. Main Thread Database Access
**Problem:** ANR from database operations on main thread  
**Solution:** Use suspend functions or withContext(Dispatchers.IO)

### 3. Missing Type Converters
**Problem:** Room can't store complex types  
**Solution:** Create and register TypeConverters

### 4. Forgetting Schema Export
**Problem:** No migration history for production  
**Solution:** Set exportSchema = true for production builds

## 📊 Performance Optimization

### 1. Indexing
```kotlin
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey val id: Long,
    val email: String
)
```

### 2. Transaction Management
```kotlin
@Transaction
suspend fun updateUserWithPosts(user: User, posts: List<Post>) {
    userDao.update(user)
    postDao.insertAll(posts)
}
```

### 3. Query Optimization
```kotlin
// Use LIMIT for large datasets
@Query("SELECT * FROM users ORDER BY created_at DESC LIMIT :limit")
suspend fun getRecentUsers(limit: Int): List<User>
```

## 🔍 Testing

### Unit Testing DAOs
```kotlin
@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    private lateinit var database: TestDatabase
    private lateinit var userDao: UserDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, TestDatabase::class.java
        ).build()
        userDao = database.userDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieve() = runTest {
        val user = User(id = 1, name = "Test")
        userDao.insert(user)
        
        val retrieved = userDao.getById(1)
        assertEquals(user, retrieved)
    }
}
```

## 📱 Current Implementation Status

### ✅ Implemented Modules
- **VoiceDataManager**: 13 entities, full Room implementation
- **LocalizationManager**: User preferences with Room
- **DatabaseManager**: Centralized database management

### 🚧 Pending Modules
- CommandManager: Direct database access planned
- LicenseManager: License storage migration needed
- HUDManager: Settings persistence planned

## 🔗 Related Documentation
- [DATABASE-MIGRATION-OBJECTBOX-TO-ROOM-GUIDE.md](DATABASE-MIGRATION-OBJECTBOX-TO-ROOM-GUIDE.md)
- [CODING-STANDARDS.md](../Agent-Instructions/CODING-STANDARDS.md)
- [VoiceDataManager Documentation](modules/managers/VoiceDataManager/)

## 📝 Summary

Room database is the **ONLY** approved database solution for VOS4. All modules must follow the patterns and practices outlined in this guide. ObjectBox has been completely replaced and should not be used in any new development.

---
**Status:** ✅ ACTIVE  
**Last Updated:** 2025-09-07  
**Next Review:** When Room 3.0 is released