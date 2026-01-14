<!--
filename: TESTING.md
created: 2025-01-23 22:00:00 PST
author: VOS4 Development Team
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: Testing procedures for VosDataManager module
last-modified: 2025-01-23 22:15:00 PST
version: 1.0.1
-->

# VosDataManager Testing Guide

## Testing Database Size Calculation

### Manual Test Procedure

Add this test code to any Activity or the Application class:

```kotlin
// Test in VoiceOS.kt or any Activity
private fun testDatabaseSize() {
    Log.d("DatabaseTest", "=== Testing Database Size ===")
    
    // Get initial size
    val initialSize = ObjectBox.getDatabaseSizeMB()
    Log.d("DatabaseTest", "Initial size: $initialSize MB")
    
    // Add test data
    val testPrefs = UserPreference(
        key = "test_key_${System.currentTimeMillis()}",
        value = "test_value",
        category = "test"
    )
    
    lifecycleScope.launch {
        dataManager.userPreferences.insert(testPrefs)
        
        // Check size after insert
        val sizeAfterInsert = ObjectBox.getDatabaseSizeMB()
        Log.d("DatabaseTest", "Size after insert: $sizeAfterInsert MB")
        
        // Get statistics
        val stats = ObjectBox.getStatistics()
        stats.forEach { (key, value) ->
            Log.d("DatabaseTest", "$key: $value")
        }
        
        // Test database path
        val dbPath = ObjectBox.getDatabasePath()
        Log.d("DatabaseTest", "Database path: $dbPath")
    }
}
```

### Expected Output in Logcat

Filter by "DatabaseTest" or "VosObjectBox" to see:

```
D/DatabaseTest: === Testing Database Size ===
D/DatabaseTest: Initial size: 0.125 MB
V/VosObjectBox: Database size: 0.125 MB (131072 bytes)
D/DatabaseTest: Size after insert: 0.126 MB
V/VosObjectBox: Database size: 0.126 MB (132096 bytes)
D/DatabaseTest: initialized: true
D/DatabaseTest: database_path: /data/data/com.augmentalis.voiceos/files/vos4-database
D/DatabaseTest: size_mb: 0.126
D/DatabaseTest: entity_classes: 12
D/DatabaseTest: Database path: /data/data/com.augmentalis.voiceos/files/vos4-database
```

### Automated Test

Create a unit test in `VosDataManagerTest.kt`:

```kotlin
@RunWith(AndroidJUnit4::class)
class VosDataManagerTest {
    
    private lateinit var context: Context
    private lateinit var databaseModule: DatabaseModule
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        databaseModule = DatabaseModule(context)
        databaseModule.initialize()
    }
    
    @After
    fun tearDown() {
        ObjectBox.clearAllData()
        ObjectBox.close()
    }
    
    @Test
    fun testDatabaseSizeCalculation() {
        // Initial size should be small
        val initialSize = ObjectBox.getDatabaseSizeMB()
        assertTrue("Initial DB size should be > 0", initialSize > 0)
        assertTrue("Initial DB size should be < 1 MB", initialSize < 1.0f)
        
        // Add data
        runBlocking {
            repeat(100) { i ->
                databaseModule.userPreferences.insert(
                    UserPreference(
                        key = "test_$i",
                        value = "value_$i",
                        category = "test"
                    )
                )
            }
        }
        
        // Size should increase
        val newSize = ObjectBox.getDatabaseSizeMB()
        assertTrue("DB size should increase after inserts", newSize > initialSize)
    }
    
    @Test
    fun testGetStatistics() {
        val stats = ObjectBox.getStatistics()
        
        assertNotNull(stats)
        assertEquals(true, stats["initialized"])
        assertNotNull(stats["database_path"])
        assertNotNull(stats["size_mb"])
        assertEquals(12, stats["entity_classes"]) // We have 12 entities
    }
    
    @Test
    fun testClearAllData() {
        // Add some data
        runBlocking {
            databaseModule.userPreferences.insert(
                UserPreference(key = "test", value = "value", category = "test")
            )
        }
        
        // Clear all data
        ObjectBox.clearAllData()
        
        // Verify data is cleared
        runBlocking {
            val allPrefs = databaseModule.userPreferences.getAll()
            assertTrue("Data should be cleared", allPrefs.isEmpty())
        }
    }
}
```

## Testing Memory Leak Fix

### Verify ApplicationContext Usage

1. Use Android Studio's **Memory Profiler**:
   - Run the app
   - Open Memory Profiler
   - Force garbage collection
   - Create and destroy activities
   - Check that activities are properly garbage collected

2. Use **LeakCanary** (optional):
   ```gradle
   debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'
   ```

3. Check lint warnings:
   ```bash
   ./gradlew :managers:VosDataManager:lint
   ```
   
   The StaticFieldLeak warning should be reduced or eliminated after using applicationContext.

## Performance Testing

### Database Size Growth Test

```kotlin
@Test
fun testDatabaseGrowth() {
    val measurements = mutableListOf<Pair<Int, Float>>()
    
    runBlocking {
        for (count in listOf(0, 100, 500, 1000, 5000)) {
            if (count > 0) {
                repeat(count - measurements.lastOrNull()?.first ?: 0) {
                    databaseModule.commandHistory.insert(
                        CommandHistoryEntry(
                            command = "test_command_$it",
                            timestamp = System.currentTimeMillis(),
                            source = "test"
                        )
                    )
                }
            }
            
            val size = ObjectBox.getDatabaseSizeMB()
            measurements.add(count to size)
            Log.d("PerfTest", "Records: $count, Size: $size MB")
        }
    }
    
    // Verify reasonable growth
    measurements.forEach { (count, size) ->
        // Rough estimate: ~1KB per record
        val expectedMaxSize = (count * 1024) / (1024f * 1024f) + 0.5f // +0.5MB base
        assertTrue(
            "Size for $count records should be < $expectedMaxSize MB, but was $size MB",
            size < expectedMaxSize
        )
    }
}
```

## Checklist for Production

- [ ] Database initializes without errors
- [ ] Size calculation returns accurate values
- [ ] Statistics method provides all expected fields
- [ ] Clear all data removes all entities
- [ ] No memory leaks detected
- [ ] Logging appears at correct levels
- [ ] Database path is correct for the app
- [ ] Performance is acceptable (< 50ms for most operations)

## Common Issues

1. **sizeOnDisk() returns 0**
   - Ensure database is initialized
   - Check that data has been written

2. **Memory leak warnings persist**
   - Verify using applicationContext everywhere
   - Check for other static references to activities/fragments

3. **Database not found**
   - Check file permissions
   - Verify initialization succeeded
   - Check logs for initialization errors