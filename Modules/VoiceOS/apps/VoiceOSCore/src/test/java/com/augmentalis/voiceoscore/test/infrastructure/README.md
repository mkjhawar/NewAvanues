# SQLDelight Test Infrastructure

Created: 2025-11-26 23:25 PST
Agent: Agent 2 - Test Infrastructure Builder

---

## Quick Start

### Basic Repository Test

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyRepositoryTest : BaseRepositoryTest() {

    @Test
    fun testMyFeature() = runTest(coroutineRule.testDispatcher) {
        // Your test code here
        // database and databaseManager are available
    }
}
```

---

## Available Components

### 1. BaseRepositoryTest
Abstract base class for all repository tests.

**Provides:**
- `database: VoiceOSDatabase` - Direct database access
- `databaseManager: VoiceOSDatabaseManager` - All repositories
- `coroutineRule: CoroutineTestRule` - For async testing
- `clearDatabase()` - Reset database to empty state
- `now()`, `past(ms)`, `future(ms)` - Timestamp helpers

**Setup/Teardown:**
- Automatic before each test
- In-memory database created
- Driver closed after each test

### 2. TestDatabaseFactory
Factory for creating test databases.

**Methods:**
- `createInMemoryDatabase()` - New in-memory database
- `createCleanDatabase()` - New empty database (alias)

### 3. TestDatabaseDriverFactory
Factory for creating SQLite drivers.

**Methods:**
- `createDriver()` - In-memory driver
- `createDriver(url)` - Custom URL driver

### 4. CoroutineTestRule
JUnit rule for coroutine testing.

**Usage:**
- Automatically included in BaseRepositoryTest
- Provides `testDispatcher` for runTest()
- Sets up/tears down Dispatchers.Main

### 5. MockAccessibilityService
Mock AccessibilityService for testing.

**Features:**
- Tracks received events
- Service connection state
- Event counting helpers

---

## Examples

### Test with Transaction

```kotlin
@Test
fun testTransaction() = runTest(coroutineRule.testDispatcher) {
    databaseManager.transaction {
        // All operations here are atomic
        databaseManager.commands.insert(...)
        databaseManager.commandHistory.insert(...)
    }
}
```

### Test with Clear

```kotlin
@Test
fun testMultipleStates() = runTest(coroutineRule.testDispatcher) {
    // Insert data
    databaseManager.commands.insert(command1)

    // Test something
    assertEquals(1, databaseManager.commands.getAll().size)

    // Reset
    clearDatabase()

    // Test again
    assertEquals(0, databaseManager.commands.getAll().size)
}
```

### Direct Database Access

```kotlin
@Test
fun testDirectAccess() = runTest(coroutineRule.testDispatcher) {
    // Use query objects directly
    database.commandHistoryQueries.insert(...)
    val count = database.commandHistoryQueries.count().executeAsOne()
    assertEquals(1L, count)
}
```

---

## Dependencies

All required dependencies are configured in `build.gradle.kts`:
- `app.cash.sqldelight:sqlite-driver:2.0.1` - JVM SQLite driver
- `app.cash.turbine:turbine:1.0.0` - Flow testing
- `junit:junit:4.13.2` - Test framework
- `kotlinx-coroutines-test:1.8.1` - Coroutine testing

---

## Key Points

1. **In-Memory:** All tests use in-memory databases (fast, isolated)
2. **No Device:** JVM-based tests, no Android device needed
3. **Auto Cleanup:** Driver closed automatically after each test
4. **Coroutines:** Use `runTest(coroutineRule.testDispatcher)` for async code
5. **Inheritance:** Extend `BaseRepositoryTest` for repository tests

---

## Files

- `BaseRepositoryTest.kt` - Base class for all tests
- `TestDatabaseFactory.kt` - Database creation
- `TestDatabaseDriverFactory.kt` - Driver creation
- `CoroutineTestRule.kt` - Coroutine test rule
- `InfrastructureTest.kt` - Infrastructure verification
- `MockAccessibilityService.kt` - Mock service (in ../mocks/)

---

## Status

✅ Infrastructure Complete
⚠️ Awaiting main code compilation fix

---

For detailed status: See `/docs/TEST-INFRASTRUCTURE-STATUS-20251126-2325.md`
