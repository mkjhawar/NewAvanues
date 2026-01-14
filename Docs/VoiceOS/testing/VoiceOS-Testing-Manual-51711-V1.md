# VoiceOS Testing Manual

**Version:** 4.1.1
**Last Updated:** 2025-11-07
**Status:** Living Document
**Framework:** IDEACODE v5.3

---

## Overview

This comprehensive testing manual covers all testing aspects of VoiceOS v4.1+, from unit tests to device testing. It provides testing strategies, procedures, automation scripts, and quality standards for ensuring VoiceOS reliability and performance.

**Target Audience:**
- QA Engineers
- Android Developers
- Test Automation Engineers
- DevOps Engineers
- Contributors

**Prerequisites:**
- Android Studio or IntelliJ IDEA
- Android SDK 24+ (minimum API level)
- Physical Android device or emulator
- Gradle 8.10.2+
- Kotlin 1.9.25+

---

## Table of Contents

1. [Testing Strategy](#testing-strategy)
2. [Test Environment Setup](#test-environment-setup)
3. [Unit Testing](#unit-testing)
4. [Integration Testing](#integration-testing)
5. [UI Testing](#ui-testing)
6. [Accessibility Testing](#accessibility-testing)
7. [Database Testing](#database-testing)
8. [Voice Command Testing](#voice-command-testing)
9. [Performance Testing](#performance-testing)
10. [Security Testing](#security-testing)
11. [Device Testing](#device-testing)
12. [Automated Testing](#automated-testing)
13. [Manual Testing Procedures](#manual-testing-procedures)
14. [CI/CD Integration](#cicd-integration)
15. [Test Reporting](#test-reporting)
16. [Troubleshooting](#troubleshooting)

---

## 1. Testing Strategy

### 1.1 Testing Pyramid

```
                    /\
                   /  \
                  / E2E \         10% - End-to-End Tests
                 /______\
                /        \
               /Integration\      30% - Integration Tests
              /____________\
             /              \
            /   Unit Tests   \    60% - Unit Tests
           /__________________\
```

**Philosophy:**
- **60% Unit Tests**: Fast, isolated, component-level testing
- **30% Integration Tests**: Module interaction testing
- **10% E2E Tests**: Full user flow testing

### 1.2 Testing Goals

**Quality Metrics:**
- ✅ **Code Coverage**: Minimum 70% line coverage
- ✅ **Test Execution Time**: <5 minutes for unit tests
- ✅ **Integration Tests**: <15 minutes
- ✅ **E2E Tests**: <30 minutes
- ✅ **Zero Crashes**: No unhandled exceptions in production

**Test Coverage Requirements:**
- **Critical Path**: 100% coverage (voice commands, accessibility, database)
- **Core Modules**: 80% coverage (VoiceOSCore, SpeechRecognition)
- **UI Modules**: 60% coverage (VoiceUI, LearnApp)
- **Utility Modules**: 70% coverage (Managers, Libraries)

### 1.3 Test Types Overview

| Test Type | Purpose | Tools | Execution Time |
|-----------|---------|-------|----------------|
| Unit Tests | Test individual functions/classes | JUnit 4, Mockito, Robolectric | <5 min |
| Integration Tests | Test module interactions | JUnit 4, Hilt, Room | <15 min |
| UI Tests | Test user interface | Espresso, Compose Testing | <30 min |
| Accessibility Tests | Test accessibility compliance | Espresso, AccessibilityChecks | <10 min |
| Database Tests | Test database operations | Room Testing, SQLite | <5 min |
| Voice Tests | Test voice command execution | Custom framework | <20 min |
| Performance Tests | Test speed and memory | Android Profiler, Benchmark | <15 min |
| Security Tests | Test security vulnerabilities | Static analysis, Penetration | Variable |
| Device Tests | Test on physical devices | Manual, Firebase Test Lab | Variable |

---

## 2. Test Environment Setup

### 2.1 Local Development Setup

**Install Dependencies:**

```bash
# Clone repository
git clone https://gitlab.com/AugmentalisES/voiceos.git
cd voiceos

# Checkout database update branch (v4.1+)
git checkout voiceos-database-update

# Verify Gradle wrapper
./gradlew --version
# Should show: Gradle 8.10.2

# Sync project
./gradlew clean build
```

**Configure Test Environment:**

Create `local.properties`:
```properties
# Android SDK location
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk

# Test configurations
test.environment=local
test.database.inMemory=true
test.mock.speechRecognition=true
```

**Install Testing Tools:**

```bash
# Install Android command-line tools
brew install android-platform-tools

# Verify ADB
adb version
# Should show: Android Debug Bridge version 1.0.41+

# List connected devices
adb devices
```

### 2.2 Test Module Structure

```
tests/
├── voiceoscore-unit-tests/          # Unit tests for VoiceOSCore
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── test/kotlin/             # JUnit tests
│   │   └── androidTest/kotlin/      # Instrumented tests
│   └── README.md
├── integration-tests/               # Integration tests
│   ├── database-integration/
│   ├── speech-integration/
│   └── accessibility-integration/
├── ui-tests/                        # UI tests (Espresso, Compose)
├── performance-tests/               # Performance benchmarks
└── device-tests/                    # Device-specific tests
```

### 2.3 Gradle Test Configuration

**Module: `tests/voiceoscore-unit-tests/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

android {
    namespace = "com.augmentalis.voiceoscore.tests"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Module under test
    implementation(project(":modules:apps:VoiceOSCore"))

    // Testing frameworks
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
    testImplementation("org.robolectric:robolectric:4.10.3")

    // Coroutines testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Room testing
    testImplementation("androidx.room:room-testing:2.6.0")

    // Truth assertions
    testImplementation("com.google.truth:truth:1.1.5")

    // AndroidX test
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:runner:1.5.2")
    testImplementation("androidx.test.ext:junit:1.1.5")
}
```

---

## 3. Unit Testing

### 3.1 Unit Testing Philosophy

**Principles:**
- **Isolation**: Test one unit at a time, mock dependencies
- **Speed**: Tests should run in milliseconds
- **Repeatability**: Same input = same output
- **Independence**: Tests can run in any order

### 3.2 Testing Room DAOs

**Test: `AppDaoTest.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import com.augmentalis.voiceoscore.database.dao.AppDao
import com.augmentalis.voiceoscore.database.entities.AppEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.google.common.truth.Truth.assertThat

/**
 * Unit tests for AppDao
 *
 * Tests all CRUD operations, queries, and complex transactions
 */
@RunWith(RobolectricTestRunner::class)
class AppDaoTest {

    private lateinit var database: VoiceOSAppDatabase
    private lateinit var appDao: AppDao

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceOSAppDatabase::class.java
        )
            .allowMainThreadQueries() // OK for tests
            .build()

        appDao = database.appDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ========== BASIC CRUD TESTS ==========

    @Test
    fun `insert app should return app when queried`() = runBlocking {
        // Given: An app entity
        val app = createTestApp(
            packageName = "com.test.app",
            appName = "Test App"
        )

        // When: Insert app
        appDao.insert(app)

        // Then: Should be retrievable
        val retrieved = appDao.getApp("com.test.app")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.appName).isEqualTo("Test App")
    }

    @Test
    fun `update app should persist changes`() = runBlocking {
        // Given: An inserted app
        val app = createTestApp(packageName = "com.test.app")
        appDao.insert(app)

        // When: Update app
        val updated = app.copy(appName = "Updated App")
        appDao.update(updated)

        // Then: Changes should persist
        val retrieved = appDao.getApp("com.test.app")
        assertThat(retrieved?.appName).isEqualTo("Updated App")
    }

    @Test
    fun `delete app should remove from database`() = runBlocking {
        // Given: An inserted app
        val app = createTestApp(packageName = "com.test.app")
        appDao.insert(app)

        // When: Delete app
        appDao.deleteApp("com.test.app")

        // Then: Should not be retrievable
        val retrieved = appDao.getApp("com.test.app")
        assertThat(retrieved).isNull()
    }

    // ========== QUERY TESTS ==========

    @Test
    fun `getAppCount should return correct count`() = runBlocking {
        // Given: Multiple apps
        appDao.insert(createTestApp("com.app1", "App 1"))
        appDao.insert(createTestApp("com.app2", "App 2"))
        appDao.insert(createTestApp("com.app3", "App 3"))

        // When: Get count
        val count = appDao.getAppCount()

        // Then: Should return 3
        assertThat(count).isEqualTo(3)
    }

    @Test
    fun `getAppByName should find app by partial name match`() = runBlocking {
        // Given: Apps with different names
        appDao.insert(createTestApp("com.instagram", "Instagram"))
        appDao.insert(createTestApp("com.facebook", "Facebook"))
        appDao.insert(createTestApp("com.twitter", "Twitter"))

        // When: Search by partial name (case-insensitive)
        val result = appDao.getAppByName("insta")

        // Then: Should find Instagram
        assertThat(result).isNotNull()
        assertThat(result?.appName).isEqualTo("Instagram")
    }

    @Test
    fun `getFullyLearnedAppCount should return correct count`() = runBlocking {
        // Given: Mix of fully learned and partial apps
        appDao.insert(createTestApp("com.app1", isFullyLearned = true))
        appDao.insert(createTestApp("com.app2", isFullyLearned = true))
        appDao.insert(createTestApp("com.app3", isFullyLearned = false))

        // When: Get fully learned count
        val count = appDao.getFullyLearnedAppCount()

        // Then: Should return 2
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `getAppsByExplorationStatus should filter correctly`() = runBlocking {
        // Given: Apps with different exploration statuses
        appDao.insert(createTestApp("com.app1", explorationStatus = "COMPLETE"))
        appDao.insert(createTestApp("com.app2", explorationStatus = "IN_PROGRESS"))
        appDao.insert(createTestApp("com.app3", explorationStatus = "COMPLETE"))

        // When: Query by status
        val completed = appDao.getAppsByExplorationStatus("COMPLETE")

        // Then: Should return 2 apps
        assertThat(completed).hasSize(2)
        assertThat(completed.map { it.packageName })
            .containsExactly("com.app1", "com.app3")
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `insert duplicate packageName should replace existing`() = runBlocking {
        // Given: An app with REPLACE conflict strategy
        val app1 = createTestApp("com.test", "Original")
        appDao.insert(app1)

        // When: Insert same packageName with different data
        val app2 = createTestApp("com.test", "Replaced")
        appDao.insert(app2)

        // Then: Should replace (not error)
        val result = appDao.getApp("com.test")
        assertThat(result?.appName).isEqualTo("Replaced")
    }

    @Test
    fun `getApp with non-existent packageName should return null`() = runBlocking {
        // When: Query non-existent app
        val result = appDao.getApp("com.nonexistent.app")

        // Then: Should return null
        assertThat(result).isNull()
    }

    @Test
    fun `getAllApps on empty database should return empty list`() = runBlocking {
        // When: Query empty database
        val apps = appDao.getAllApps()

        // Then: Should return empty list
        assertThat(apps).isEmpty()
    }

    // ========== HELPER FUNCTIONS ==========

    private fun createTestApp(
        packageName: String,
        appName: String = "Test App",
        isFullyLearned: Boolean = false,
        explorationStatus: String? = null
    ): AppEntity {
        return AppEntity(
            packageName = packageName,
            appId = java.util.UUID.randomUUID().toString(),
            appName = appName,
            versionCode = 1L,
            versionName = "1.0",
            appHash = "hash_$packageName",
            isFullyLearned = isFullyLearned,
            explorationStatus = explorationStatus,
            firstScraped = System.currentTimeMillis(),
            lastScraped = System.currentTimeMillis()
        )
    }
}
```

### 3.3 Testing Voice Command Handler

**Test: `DatabaseCommandHandlerTest.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.commands

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.voiceoscore.commands.DatabaseCommandHandler
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import com.augmentalis.voiceoscore.database.entities.AppEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.google.common.truth.Truth.assertThat

/**
 * Unit tests for DatabaseCommandHandler
 *
 * Tests all 20 voice commands for database interaction
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseCommandHandlerTest {

    private lateinit var context: Context
    private lateinit var database: VoiceOSAppDatabase
    private lateinit var handler: DatabaseCommandHandler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create in-memory database
        database = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            VoiceOSAppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        handler = DatabaseCommandHandler(context, database)

        // Seed test data
        seedTestData()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun seedTestData() = runBlocking {
        // Add test apps
        database.appDao().insert(
            AppEntity(
                packageName = "com.instagram",
                appId = "app1",
                appName = "Instagram",
                versionCode = 100L,
                versionName = "1.0.0",
                appHash = "hash1",
                exploredElementCount = 100,
                scrapedElementCount = 50,
                isFullyLearned = true,
                totalScreens = 20,
                firstScraped = System.currentTimeMillis(),
                lastScraped = System.currentTimeMillis()
            )
        )

        database.appDao().insert(
            AppEntity(
                packageName = "com.twitter",
                appId = "app2",
                appName = "Twitter",
                versionCode = 50L,
                versionName = "0.5.0",
                appHash = "hash2",
                exploredElementCount = 30,
                scrapedElementCount = 20,
                isFullyLearned = false,
                totalScreens = 10,
                firstScraped = System.currentTimeMillis(),
                lastScraped = System.currentTimeMillis()
            )
        )
    }

    // ========== STATISTICS COMMANDS ==========

    @Test
    fun `show database stats should return formatted statistics`() = runBlocking {
        // When: Execute "show database stats" command
        val result = handler.handleCommand("show database stats")

        // Then: Should return statistics with app count
        assertThat(result).isNotNull()
        assertThat(result).contains("2 apps")
        assertThat(result).contains("fully explored")
        assertThat(result).contains("Database is")
    }

    @Test
    fun `how many apps should return app count breakdown`() = runBlocking {
        // When: Execute "how many apps" command
        val result = handler.handleCommand("how many apps")

        // Then: Should return count breakdown
        assertThat(result).isNotNull()
        assertThat(result).contains("2 apps")
        assertThat(result).contains("fully learned")
    }

    @Test
    fun `database size should return size in readable format`() = runBlocking {
        // When: Execute "database size" command
        val result = handler.handleCommand("database size")

        // Then: Should return size (KB, MB, etc.)
        assertThat(result).isNotNull()
        assertThat(result).matches(".*\\d+\\.\\d+ (KB|MB|GB).*")
    }

    // ========== APP QUERY COMMANDS ==========

    @Test
    fun `list learned apps should return top apps with completion`() = runBlocking {
        // When: Execute "list learned apps" command
        val result = handler.handleCommand("list learned apps")

        // Then: Should list apps with percentages
        assertThat(result).isNotNull()
        assertThat(result).contains("Instagram")
        assertThat(result).contains("Twitter")
        assertThat(result).contains("%")
    }

    @Test
    fun `show app details for Instagram should return detailed info`() = runBlocking {
        // When: Execute "show app details for Instagram" command
        val result = handler.handleCommand("show app details for Instagram")

        // Then: Should return detailed app info
        assertThat(result).isNotNull()
        assertThat(result).contains("Instagram")
        assertThat(result).contains("screens")
        assertThat(result).contains("elements")
        assertThat(result).contains("explored")
    }

    @Test
    fun `show app details for non-existent app should return not found`() = runBlocking {
        // When: Execute command for non-existent app
        val result = handler.handleCommand("show app details for NonExistentApp")

        // Then: Should return not found message
        assertThat(result).isNotNull()
        assertThat(result).contains("not found")
    }

    // ========== MIGRATION COMMANDS ==========

    @Test
    fun `migration status should return completion status`() = runBlocking {
        // When: Execute "migration status" command
        val result = handler.handleCommand("migration status")

        // Then: Should return migration info
        assertThat(result).isNotNull()
        // Migration should be complete after v4.1
        assertThat(result).containsMatch("(complete|pending)")
    }

    // ========== PATTERN MATCHING TESTS ==========

    @Test
    fun `command matching should be case-insensitive`() = runBlocking {
        // When: Execute commands with different cases
        val lower = handler.handleCommand("show database stats")
        val upper = handler.handleCommand("SHOW DATABASE STATS")
        val mixed = handler.handleCommand("Show Database Stats")

        // Then: All should match and return results
        assertThat(lower).isNotNull()
        assertThat(upper).isNotNull()
        assertThat(mixed).isNotNull()
    }

    @Test
    fun `unrecognized command should return null`() = runBlocking {
        // When: Execute unrecognized command
        val result = handler.handleCommand("this is not a valid command")

        // Then: Should return null (fallback to other handlers)
        assertThat(result).isNull()
    }

    @Test
    fun `partial pattern match should work`() = runBlocking {
        // When: Execute with extra words
        val result = handler.handleCommand("please show me database stats now")

        // Then: Should still match pattern
        assertThat(result).isNotNull()
        assertThat(result).contains("apps")
    }

    // ========== PERFORMANCE TESTS ==========

    @Test
    fun `statistics command should execute under 500ms`() = runBlocking {
        // When: Measure execution time
        val startTime = System.currentTimeMillis()
        handler.handleCommand("show database stats")
        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime

        // Then: Should complete quickly
        assertThat(executionTime).isLessThan(500L)
    }
}
```

### 3.4 Testing Coroutines

**Test: `VoiceCommandProcessorTest.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.commands

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Testing suspend functions with coroutines test library
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class VoiceCommandProcessorTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `processCommand should execute asynchronously`() = runTest(testDispatcher) {
        // Given: A voice command processor
        // When: Process command
        // Then: Should complete without blocking

        // Test implementation here
    }
}
```

### 3.5 Running Unit Tests

**Run all unit tests:**
```bash
./gradlew :tests:voiceoscore-unit-tests:test

# With coverage
./gradlew :tests:voiceoscore-unit-tests:jacocoTestReport
```

**Run specific test class:**
```bash
./gradlew :tests:voiceoscore-unit-tests:test --tests AppDaoTest
```

**Run specific test method:**
```bash
./gradlew :tests:voiceoscore-unit-tests:test --tests AppDaoTest."insert app should return app when queried"
```

---

## 4. Integration Testing

### 4.1 Integration Testing Strategy

**Purpose:** Test interactions between multiple modules/components

**Key Areas:**
- Database + DAO + Migration interactions
- VoiceCommandProcessor + DatabaseCommandHandler integration
- AccessibilityService + UI Scraping integration
- Speech Recognition + Command Processing integration

### 4.2 Database Migration Integration Test

**Test: `DatabaseMigrationIntegrationTest.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import com.augmentalis.voiceoscore.database.migration.DatabaseMigrationHelper
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.google.common.truth.Truth.assertThat

/**
 * Integration test for database migration
 *
 * Tests the complete migration flow from LearnApp/AppScraping to VoiceOSAppDatabase
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationIntegrationTest {

    private lateinit var context: Context
    private lateinit var migrationHelper: DatabaseMigrationHelper

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        migrationHelper = DatabaseMigrationHelper(context)
    }

    @Test
    fun `migration should be idempotent`() = runBlocking {
        // When: Run migration twice
        migrationHelper.migrateIfNeeded()
        val firstComplete = migrationHelper.isMigrationComplete()

        migrationHelper.migrateIfNeeded()
        val secondComplete = migrationHelper.isMigrationComplete()

        // Then: Should remain complete, no errors
        assertThat(firstComplete).isTrue()
        assertThat(secondComplete).isTrue()
    }

    @Test
    fun `migration should preserve all data`() = runBlocking {
        // Given: Apps in old databases (simulated)
        // When: Run migration
        migrationHelper.migrateIfNeeded()

        // Then: All data should be in unified database
        val database = VoiceOSAppDatabase.getInstance(context)
        val appCount = database.appDao().getAppCount()

        // Verify migration occurred
        assertThat(migrationHelper.isMigrationComplete()).isTrue()
    }
}
```

### 4.3 Voice Command End-to-End Integration Test

**Test: `VoiceCommandE2ETest.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.voiceoscore.commands.DatabaseCommandHandler
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import com.augmentalis.voiceoscore.scraping.VoiceCommandProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.google.common.truth.Truth.assertThat

/**
 * End-to-end integration test for voice command flow
 *
 * Tests: Voice Input → Command Matching → Execution → Response
 */
@RunWith(RobolectricTestRunner::class)
class VoiceCommandE2ETest {

    private lateinit var context: Context
    private lateinit var database: VoiceOSAppDatabase
    private lateinit var commandHandler: DatabaseCommandHandler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = VoiceOSAppDatabase.getInstance(context)
        commandHandler = DatabaseCommandHandler(context, database)
    }

    @Test
    fun `full voice command flow should execute successfully`() = runBlocking {
        // Given: A voice input
        val voiceInput = "show database stats"

        // When: Process through command handler
        val result = commandHandler.handleCommand(voiceInput)

        // Then: Should return valid response
        assertThat(result).isNotNull()
        assertThat(result).contains("apps")
    }

    @Test
    fun `command priority chain should work correctly`() = runBlocking {
        // Given: Multiple command types
        // When: Process database command
        val dbResult = commandHandler.handleCommand("show database stats")

        // Then: Database command should be handled
        assertThat(dbResult).isNotNull()

        // When: Process unrecognized command
        val unknown = commandHandler.handleCommand("unrecognized command")

        // Then: Should return null (fallback to next handler)
        assertThat(unknown).isNull()
    }
}
```

---

## 5. UI Testing

### 5.1 Espresso Testing (XML Views)

**Test: `MainActivityTest.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.voiceui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for MainActivity using Espresso
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun mainActivity_shouldDisplay() {
        // Verify main activity is displayed
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickStartButton_shouldNavigateToVoiceScreen() {
        // Click start button
        onView(withId(R.id.btn_start))
            .perform(click())

        // Verify navigation
        onView(withId(R.id.voice_screen))
            .check(matches(isDisplayed()))
    }
}
```

### 5.2 Compose Testing

**Test: `VoiceUIComposeTest.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.voiceui.compose.VoiceScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for Compose-based screens
 */
@RunWith(AndroidJUnit4::class)
class VoiceUIComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun voiceScreen_shouldDisplayMicrophoneButton() {
        // Set content
        composeTestRule.setContent {
            VoiceScreen()
        }

        // Verify microphone button is displayed
        composeTestRule
            .onNodeWithContentDescription("Microphone")
            .assertIsDisplayed()
    }

    @Test
    fun clickMicrophoneButton_shouldStartListening() {
        composeTestRule.setContent {
            VoiceScreen()
        }

        // Click microphone
        composeTestRule
            .onNodeWithContentDescription("Microphone")
            .performClick()

        // Verify listening state
        composeTestRule
            .onNodeWithText("Listening...")
            .assertIsDisplayed()
    }
}
```

---

## 6. Accessibility Testing

### 6.1 Accessibility Validation

**Test: `AccessibilityTest.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.accessibility

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.voiceui.MainActivity
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility compliance tests
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var accessibilityValidator: AccessibilityValidator

    @Before
    fun setup() {
        accessibilityValidator = AccessibilityValidator()
            .setRunChecksFromRootView(true)
            .setSuppressingResultMatcher(
                allOf(
                    AccessibilityCheckResultUtils.matchesCheck(
                        SpeakableTextPresentCheck::class.java
                    )
                )
            )
    }

    @Test
    fun mainScreen_shouldPassAccessibilityChecks() {
        // Verify all views pass accessibility checks
        onView(withId(R.id.main_container))
            .check(accessibilityValidator)
    }
}
```

### 6.2 TalkBack Testing (Manual)

**TalkBack Test Scenarios:**

1. **Navigation Testing**
   - Enable TalkBack
   - Navigate through all screens using swipe gestures
   - Verify all elements are announced correctly
   - Check focus order is logical

2. **Voice Command Testing**
   - Use voice commands with TalkBack enabled
   - Verify command feedback is spoken
   - Check no conflicts with TalkBack announcements

3. **Form Testing**
   - Navigate to forms
   - Verify labels are announced
   - Check error messages are accessible

**Checklist:**
```
□ All buttons have contentDescription
□ All images have contentDescription (if meaningful)
□ Decorative elements are hidden from TalkBack
□ Custom views expose accessibility properties
□ Focus order is logical
□ Touch target sizes meet minimum (48dp x 48dp)
```

---

## 7. Database Testing

### 7.1 Database Schema Testing

**Test: `DatabaseSchemaTest.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.google.common.truth.Truth.assertThat

/**
 * Database schema and migration tests
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseSchemaTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VoiceOSAppDatabase::class.java.canonicalName
    )

    @Test
    fun `database should create with correct schema`() {
        // Create database
        val database = Room.inMemoryDatabaseBuilder(
            context,
            VoiceOSAppDatabase::class.java
        ).build()

        // Verify tables exist
        val tableNames = database.openHelper.readableDatabase
            .query("SELECT name FROM sqlite_master WHERE type='table'")
            .use { cursor ->
                mutableListOf<String>().apply {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(0))
                    }
                }
            }

        assertThat(tableNames).contains("apps")
        assertThat(tableNames).contains("scraped_elements")
        assertThat(tableNames).contains("generated_commands")

        database.close()
    }

    @Test
    fun `foreign key constraints should be enabled`() {
        val database = Room.inMemoryDatabaseBuilder(
            context,
            VoiceOSAppDatabase::class.java
        ).build()

        val foreignKeysEnabled = database.openHelper.readableDatabase
            .query("PRAGMA foreign_keys")
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0) == 1
            }

        assertThat(foreignKeysEnabled).isTrue()

        database.close()
    }
}
```

### 7.2 Database Performance Testing

**Test: `DatabasePerformanceTest.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import com.augmentalis.voiceoscore.database.entities.AppEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.google.common.truth.Truth.assertThat

/**
 * Database performance benchmarks
 */
@RunWith(RobolectricTestRunner::class)
class DatabasePerformanceTest {

    private lateinit var database: VoiceOSAppDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceOSAppDatabase::class.java
        ).build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `batch insert 100 apps should complete under 1 second`() = runBlocking {
        val apps = (1..100).map { i ->
            AppEntity(
                packageName = "com.test.app$i",
                appId = "id$i",
                appName = "App $i",
                versionCode = i.toLong(),
                versionName = "1.0.$i",
                appHash = "hash$i",
                firstScraped = System.currentTimeMillis(),
                lastScraped = System.currentTimeMillis()
            )
        }

        val startTime = System.currentTimeMillis()
        database.appDao().insertBatch(apps)
        val endTime = System.currentTimeMillis()

        val duration = endTime - startTime
        assertThat(duration).isLessThan(1000L) // Should complete in <1s
    }

    @Test
    fun `query all apps should complete under 100ms`() = runBlocking {
        // Insert test data
        val apps = (1..50).map { i ->
            AppEntity(
                packageName = "com.test.app$i",
                appId = "id$i",
                appName = "App $i",
                versionCode = i.toLong(),
                versionName = "1.0.$i",
                appHash = "hash$i",
                firstScraped = System.currentTimeMillis(),
                lastScraped = System.currentTimeMillis()
            )
        }
        database.appDao().insertBatch(apps)

        // Measure query time
        val startTime = System.currentTimeMillis()
        val results = database.appDao().getAllApps()
        val endTime = System.currentTimeMillis()

        val duration = endTime - startTime
        assertThat(results).hasSize(50)
        assertThat(duration).isLessThan(100L) // Should complete in <100ms
    }
}
```

---

## 8. Voice Command Testing

### 8.1 Voice Command Test Suite

**Test: `VoiceCommandAcceptanceTest.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.voice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.voiceoscore.commands.DatabaseCommandHandler
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import com.augmentalis.voiceoscore.database.entities.AppEntity
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.google.common.truth.Truth.assertThat

/**
 * Acceptance tests for all 20 voice commands
 *
 * Validates that each voice command works as specified
 */
@RunWith(RobolectricTestRunner::class)
class VoiceCommandAcceptanceTest {

    private lateinit var context: Context
    private lateinit var database: VoiceOSAppDatabase
    private lateinit var handler: DatabaseCommandHandler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            VoiceOSAppDatabase::class.java
        ).build()
        handler = DatabaseCommandHandler(context, database)

        seedTestData()
    }

    private fun seedTestData() = runBlocking {
        // Add realistic test data
        val apps = listOf(
            AppEntity(
                packageName = "com.instagram.android",
                appId = "app1",
                appName = "Instagram",
                versionCode = 12345L,
                versionName = "200.0.0",
                appHash = "hash1",
                exploredElementCount = 312,
                scrapedElementCount = 47,
                totalScreens = 47,
                isFullyLearned = true,
                explorationStatus = "COMPLETE",
                firstExplored = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000), // 3 days ago
                lastExplored = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000),
                firstScraped = System.currentTimeMillis(),
                lastScraped = System.currentTimeMillis()
            ),
            AppEntity(
                packageName = "com.twitter.android",
                appId = "app2",
                appName = "Twitter",
                versionCode = 9999L,
                versionName = "9.50.0",
                appHash = "hash2",
                exploredElementCount = 75,
                scrapedElementCount = 25,
                totalScreens = 20,
                isFullyLearned = false,
                explorationStatus = "IN_PROGRESS",
                firstExplored = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000), // 1 day ago
                lastExplored = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000),
                firstScraped = System.currentTimeMillis(),
                lastScraped = System.currentTimeMillis()
            ),
            AppEntity(
                packageName = "com.facebook.katana",
                appId = "app3",
                appName = "Facebook",
                versionCode = 5000L,
                versionName = "350.0.0",
                appHash = "hash3",
                exploredElementCount = 50,
                scrapedElementCount = 30,
                totalScreens = 15,
                isFullyLearned = false,
                explorationStatus = "IN_PROGRESS",
                firstScraped = System.currentTimeMillis(),
                lastScraped = System.currentTimeMillis()
            )
        )

        database.appDao().insertBatch(apps)
    }

    // ========== CATEGORY 1: STATISTICS COMMANDS ==========

    @Test
    fun `COMMAND show database stats - should return full summary`() = runBlocking {
        val result = handler.handleCommand("show database stats")

        assertThat(result).isNotNull()
        assertThat(result).contains("3 apps")
        assertThat(result).contains("fully explored")
        assertThat(result).contains("Database is")
        assertThat(result).contains("elements")
    }

    @Test
    fun `COMMAND how many apps - should return app count`() = runBlocking {
        val result = handler.handleCommand("how many apps")

        assertThat(result).isNotNull()
        assertThat(result).contains("3 apps")
        assertThat(result).contains("fully learned")
    }

    @Test
    fun `COMMAND database size - should return readable size`() = runBlocking {
        val result = handler.handleCommand("database size")

        assertThat(result).isNotNull()
        // Should contain size with unit (KB, MB, GB)
        assertThat(result).containsMatch("\\d+\\.\\d+ (KB|MB|GB)")
    }

    @Test
    fun `COMMAND element count - should return total elements`() = runBlocking {
        val result = handler.handleCommand("element count")

        assertThat(result).isNotNull()
        assertThat(result).containsMatch("\\d+ elements")
    }

    // ========== CATEGORY 2: MIGRATION COMMANDS ==========

    @Test
    fun `COMMAND migration status - should return migration state`() = runBlocking {
        val result = handler.handleCommand("migration status")

        assertThat(result).isNotNull()
        // Should contain either "complete" or "pending"
        assertThat(result).containsMatch("(complete|pending)")
    }

    // ========== CATEGORY 3: APP QUERY COMMANDS ==========

    @Test
    fun `COMMAND list learned apps - should list top apps with completion`() = runBlocking {
        val result = handler.handleCommand("list learned apps")

        assertThat(result).isNotNull()
        assertThat(result).contains("Instagram")
        assertThat(result).contains("Twitter")
        assertThat(result).contains("%") // Completion percentage
    }

    @Test
    fun `COMMAND show app details for Instagram - should return detailed info`() = runBlocking {
        val result = handler.handleCommand("show app details for Instagram")

        assertThat(result).isNotNull()
        assertThat(result).contains("Instagram")
        assertThat(result).contains("47 screens")
        assertThat(result).contains("elements")
        assertThat(result).contains("explored")
    }

    @Test
    fun `COMMAND show app details for Twitter - should return Twitter info`() = runBlocking {
        val result = handler.handleCommand("show app details for Twitter")

        assertThat(result).isNotNull()
        assertThat(result).contains("Twitter")
        assertThat(result).contains("20 screens")
    }

    @Test
    fun `COMMAND show app details for NonExistent - should return not found`() = runBlocking {
        val result = handler.handleCommand("show app details for NonExistentApp")

        assertThat(result).isNotNull()
        assertThat(result).contains("not found")
    }

    @Test
    fun `COMMAND which apps need learning - should list incomplete apps`() = runBlocking {
        val result = handler.handleCommand("which apps need learning")

        assertThat(result).isNotNull()
        // Should NOT include Instagram (100% complete)
        assertThat(result).doesNotContain("Instagram")
        // Should include Twitter and Facebook (incomplete)
        assertThat(result).containsMatch("(Twitter|Facebook)")
    }

    @Test
    fun `COMMAND most learned app - should return Instagram`() = runBlocking {
        val result = handler.handleCommand("most learned app")

        assertThat(result).isNotNull()
        assertThat(result).contains("Instagram")
        assertThat(result).contains("47 screens")
    }

    @Test
    fun `COMMAND recently learned apps - should list by last explored`() = runBlocking {
        val result = handler.handleCommand("recently learned apps")

        assertThat(result).isNotNull()
        // Should contain time references
        assertThat(result).containsMatch("(today|yesterday|\\d+ days ago)")
    }

    // ========== CATEGORY 4: MANAGEMENT COMMANDS ==========

    @Test
    fun `COMMAND clear app data for Twitter - should delete app`() = runBlocking {
        val result = handler.handleCommand("clear app data for Twitter")

        assertThat(result).isNotNull()
        assertThat(result).contains("Cleared")
        assertThat(result).contains("Twitter")

        // Verify deletion
        val app = database.appDao().getAppByName("Twitter")
        assertThat(app).isNull()
    }

    @Test
    fun `COMMAND optimize database - should run VACUUM`() = runBlocking {
        val result = handler.handleCommand("optimize database")

        assertThat(result).isNotNull()
        assertThat(result).containsMatch("(Optimized|Reduced size)")
    }

    @Test
    fun `COMMAND database integrity check - should verify database`() = runBlocking {
        val result = handler.handleCommand("database integrity check")

        assertThat(result).isNotNull()
        assertThat(result).containsMatch("(integrity OK|No errors)")
    }

    // ========== PATTERN MATCHING TESTS ==========

    @Test
    fun `commands should be case-insensitive`() = runBlocking {
        val lower = handler.handleCommand("show database stats")
        val upper = handler.handleCommand("SHOW DATABASE STATS")
        val mixed = handler.handleCommand("Show Database Stats")

        assertThat(lower).isNotNull()
        assertThat(upper).isNotNull()
        assertThat(mixed).isNotNull()
    }

    @Test
    fun `commands should handle extra words`() = runBlocking {
        val result = handler.handleCommand("please show me the database stats right now")

        assertThat(result).isNotNull()
        assertThat(result).contains("apps")
    }

    @Test
    fun `unrecognized commands should return null`() = runBlocking {
        val result = handler.handleCommand("this is not a valid command at all")

        assertThat(result).isNull()
    }

    // ========== PERFORMANCE TESTS ==========

    @Test
    fun `all commands should execute under 500ms`() = runBlocking {
        val commands = listOf(
            "show database stats",
            "how many apps",
            "database size",
            "list learned apps",
            "migration status"
        )

        commands.forEach { command ->
            val startTime = System.currentTimeMillis()
            handler.handleCommand(command)
            val endTime = System.currentTimeMillis()

            val duration = endTime - startTime
            assertThat(duration).isLessThan(500L)
        }
    }
}
```

### 8.2 Manual Voice Command Testing

**Device Testing Procedure:**

```markdown
# Voice Command Manual Test Checklist

## Prerequisites
- [ ] VoiceOS app installed on device
- [ ] Accessibility service enabled
- [ ] Speech recognition configured
- [ ] Test apps installed (Instagram, Twitter, Facebook)

## Test Execution

### Category 1: Statistics Commands

1. **Test: "show database stats"**
   - [ ] Say: "show database stats"
   - [ ] Expected: "You have X apps. Y fully explored, Z partial. Database is A MB with B elements."
   - [ ] Result: PASS / FAIL
   - [ ] Notes: ___________

2. **Test: "how many apps"**
   - [ ] Say: "how many apps"
   - [ ] Expected: "X apps in database. Y fully learned, Z partially learned."
   - [ ] Result: PASS / FAIL
   - [ ] Notes: ___________

3. **Test: "database size"**
   - [ ] Say: "database size"
   - [ ] Expected: "Database is X MB."
   - [ ] Result: PASS / FAIL
   - [ ] Notes: ___________

4. **Test: "element count"**
   - [ ] Say: "element count"
   - [ ] Expected: "X UI elements in database."
   - [ ] Result: PASS / FAIL
   - [ ] Notes: ___________

### Category 2: App Query Commands

5. **Test: "list learned apps"**
   - [ ] Say: "list learned apps"
   - [ ] Expected: List of apps with completion percentages
   - [ ] Result: PASS / FAIL
   - [ ] Notes: ___________

6. **Test: "show app details for Instagram"**
   - [ ] Say: "show app details for Instagram"
   - [ ] Expected: Detailed info about Instagram
   - [ ] Result: PASS / FAIL
   - [ ] Notes: ___________

7. **Test: "which apps need learning"**
   - [ ] Say: "which apps need learning"
   - [ ] Expected: List of incomplete apps
   - [ ] Result: PASS / FAIL
   - [ ] Notes: ___________

### Category 3: Management Commands

8. **Test: "optimize database"**
   - [ ] Say: "optimize database"
   - [ ] Expected: "Optimized database. Reduced size by X MB."
   - [ ] Result: PASS / FAIL
   - [ ] Notes: ___________

9. **Test: "database integrity check"**
   - [ ] Say: "database integrity check"
   - [ ] Expected: "Database integrity OK. No errors found."
   - [ ] Result: PASS / FAIL
   - [ ] Notes: ___________

### Edge Cases

10. **Test: Case insensitivity**
    - [ ] Say: "SHOW DATABASE STATS" (uppercase)
    - [ ] Expected: Same response as lowercase
    - [ ] Result: PASS / FAIL

11. **Test: Extra words**
    - [ ] Say: "please show me database stats now"
    - [ ] Expected: Should still match pattern
    - [ ] Result: PASS / FAIL

12. **Test: Invalid command**
    - [ ] Say: "this is not a command"
    - [ ] Expected: "Command not recognized" (fallback)
    - [ ] Result: PASS / FAIL

## Performance Check
- [ ] All commands respond within 500ms
- [ ] No stuttering or lag
- [ ] Smooth user experience

## Summary
- Total Tests: 12
- Passed: ___
- Failed: ___
- Pass Rate: ___%

## Issues Found
1. ___________
2. ___________
3. ___________
```

---

## 9. Performance Testing

### 9.1 Performance Benchmarks

**Target Metrics:**
- **App Startup**: <2 seconds (cold start)
- **Voice Command Response**: <500ms
- **Database Queries**: <100ms
- **UI Rendering**: 60 FPS (16ms per frame)
- **Memory Usage**: <150MB baseline

### 9.2 Benchmark Test

**Test: `PerformanceBenchmarkTest.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Performance benchmarks using Jetpack Benchmark library
 */
@RunWith(AndroidJUnit4::class)
class PerformanceBenchmarkTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun benchmarkDatabaseQuery() {
        benchmarkRule.measureRepeated {
            // Measure database query performance
            // Implementation here
        }
    }

    @Test
    fun benchmarkVoiceCommandProcessing() {
        benchmarkRule.measureRepeated {
            // Measure voice command processing time
            // Implementation here
        }
    }
}
```

### 9.3 Memory Profiling

**Manual Memory Testing:**

1. **Install Android Profiler**
   ```bash
   # Launch Android Studio
   # Open Profiler tab
   # Select VoiceOS app
   ```

2. **Test Scenarios**
   - Launch app → Monitor baseline memory
   - Navigate through screens → Check for leaks
   - Execute voice commands → Monitor spikes
   - Background operation → Check retention

3. **Memory Leak Detection**
   ```bash
   # Use LeakCanary
   # Add to build.gradle
   debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'
   ```

---

## 10. Security Testing

### 10.1 Security Test Areas

**Key Security Concerns:**
1. **PII Handling**: User data protection
2. **Accessibility Permissions**: Service security
3. **Database Encryption**: Data at rest
4. **Network Communication**: Data in transit
5. **Voice Data**: Speech recognition privacy

### 10.2 PII Redaction Testing

**Test: `PIIRedactionTest.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.security

import com.augmentalis.voiceoscore.accessibility.utils.PIIRedactionHelper
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * Tests for PII redaction in logs
 */
class PIIRedactionTest {

    @Test
    fun `should redact email addresses`() {
        val input = "User email is john.doe@example.com"
        val result = PIIRedactionHelper.redactPII(input)

        assertThat(result).doesNotContain("john.doe@example.com")
        assertThat(result).contains("[EMAIL]")
    }

    @Test
    fun `should redact phone numbers`() {
        val input = "Call me at 555-123-4567"
        val result = PIIRedactionHelper.redactPII(input)

        assertThat(result).doesNotContain("555-123-4567")
        assertThat(result).contains("[PHONE]")
    }

    @Test
    fun `should redact credit card numbers`() {
        val input = "Card: 4532-1234-5678-9010"
        val result = PIIRedactionHelper.redactPII(input)

        assertThat(result).doesNotContain("4532-1234-5678-9010")
        assertThat(result).contains("[CARD]")
    }
}
```

### 10.3 Security Checklist

```markdown
# Security Testing Checklist

## Data Protection
- [ ] PII is redacted from all logs
- [ ] Database is encrypted at rest (if sensitive data)
- [ ] No credentials in source code
- [ ] No API keys hardcoded

## Permission Management
- [ ] Accessibility permission requested properly
- [ ] Microphone permission requested properly
- [ ] Storage permission requested (if needed)
- [ ] Permissions checked at runtime

## Secure Communication
- [ ] HTTPS used for all network calls
- [ ] Certificate pinning implemented (if applicable)
- [ ] No sensitive data in URLs

## Code Security
- [ ] ProGuard/R8 enabled for release builds
- [ ] No debug code in production
- [ ] Static analysis tools run (Lint, Detekt)
- [ ] Dependency vulnerabilities checked

## Voice Data Security
- [ ] Voice recordings not stored (unless explicit consent)
- [ ] Speech recognition results not logged with PII
- [ ] User can delete voice history
```

---

## 11. Device Testing

### 11.1 Device Matrix

**Test Devices:**

| Device | Android Version | Screen Size | DPI | Purpose |
|--------|----------------|-------------|-----|---------|
| Pixel 6 | Android 14 | 6.4" | 411 dpi | Primary test device |
| Samsung Galaxy S21 | Android 13 | 6.2" | 420 dpi | Samsung-specific testing |
| OnePlus 9 | Android 13 | 6.55" | 402 dpi | OEM customization testing |
| Xiaomi Mi 11 | Android 12 | 6.81" | 515 dpi | MIUI testing |
| Android Emulator | Android 10 (API 29) | Various | Various | Minimum SDK testing |

### 11.2 Device-Specific Tests

**Test Areas:**
1. **Accessibility Service Behavior** (varies by OEM)
2. **Speech Recognition** (Google vs. device OEM)
3. **Overlay Permissions** (different implementations)
4. **Background Service** (battery optimization)
5. **Notification Behavior** (importance channels)

### 11.3 Firebase Test Lab Integration

**Configuration: `.github/workflows/firebase-test.yml`**

```yaml
name: Firebase Test Lab

on:
  push:
    branches: [ main, voiceos-database-update ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Build APK
        run: ./gradlew assembleDebug assembleAndroidTest

      - name: Run tests on Firebase Test Lab
        uses: google-github-actions/auth@v1
        with:
          credentials_json: ${{ secrets.FIREBASE_SERVICE_ACCOUNT }}

      - name: Execute tests
        run: |
          gcloud firebase test android run \
            --type instrumentation \
            --app app/build/outputs/apk/debug/app-debug.apk \
            --test app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
            --device model=Pixel2,version=28,locale=en,orientation=portrait \
            --device model=Pixel3,version=29,locale=en,orientation=portrait \
            --device model=Pixel4,version=30,locale=en,orientation=portrait
```

---

## 12. Automated Testing

### 12.1 Continuous Integration

**GitHub Actions Workflow: `.github/workflows/test.yml`**

```yaml
name: VoiceOS Test Suite

on:
  push:
    branches: [ main, develop, voiceos-database-update ]
  pull_request:
    branches: [ main, develop ]

jobs:
  unit-tests:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run unit tests
        run: ./gradlew test --stacktrace

      - name: Generate test report
        run: ./gradlew jacocoTestReport

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./build/reports/jacoco/test/jacocoTestReport.xml
          flags: unittests
          name: codecov-umbrella

  integration-tests:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run integration tests
        run: ./gradlew :tests:integration-tests:test

  lint:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Run Android Lint
        run: ./gradlew lint

      - name: Upload lint results
        uses: actions/upload-artifact@v3
        with:
          name: lint-results
          path: '**/build/reports/lint-results*.html'
```

### 12.2 Test Automation Scripts

**Script: `scripts/run-all-tests.sh`**

```bash
#!/bin/bash
# VoiceOS Comprehensive Test Runner
# Usage: ./scripts/run-all-tests.sh

set -e  # Exit on error

echo "🧪 VoiceOS Test Suite"
echo "===================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run test and track results
run_test() {
    local test_name=$1
    local test_command=$2

    echo -e "${YELLOW}Running: $test_name${NC}"

    if eval $test_command; then
        echo -e "${GREEN}✓ $test_name PASSED${NC}"
        ((PASSED_TESTS++))
    else
        echo -e "${RED}✗ $test_name FAILED${NC}"
        ((FAILED_TESTS++))
    fi

    ((TOTAL_TESTS++))
    echo ""
}

# 1. Unit Tests
run_test "Unit Tests" "./gradlew :tests:voiceoscore-unit-tests:test"

# 2. Integration Tests
run_test "Integration Tests" "./gradlew :tests:integration-tests:test"

# 3. Database Tests
run_test "Database Tests" "./gradlew :tests:database-tests:test"

# 4. Lint Check
run_test "Lint Check" "./gradlew lint"

# 5. Static Analysis
run_test "Static Analysis" "./gradlew detekt"

# 6. Compile Check
run_test "Compile Check" "./gradlew assembleDebug"

# Summary
echo "===================="
echo "📊 Test Summary"
echo "===================="
echo -e "Total Tests: $TOTAL_TESTS"
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
echo -e "${RED}Failed: $FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}❌ Some tests failed${NC}"
    exit 1
fi
```

**Make executable:**
```bash
chmod +x scripts/run-all-tests.sh
```

---

## 13. Manual Testing Procedures

### 13.1 Smoke Test Checklist

**Quick verification after build:**

```markdown
# Smoke Test Checklist (10 minutes)

## Installation
- [ ] App installs successfully
- [ ] No crash on first launch
- [ ] Permissions requested properly

## Core Functionality
- [ ] Accessibility service can be enabled
- [ ] Voice recognition starts
- [ ] Database initializes
- [ ] No crashes during 2-minute usage

## Critical Paths
- [ ] Can learn an app
- [ ] Can execute voice command
- [ ] Database stores data
- [ ] App survives background/foreground

## Pass Criteria
- All items checked = PASS
- Any failure = FAIL (investigate)
```

### 13.2 Regression Test Suite

**Comprehensive testing before release:**

```markdown
# Regression Test Suite (2 hours)

## Module 1: VoiceOSCore (30 min)
- [ ] Accessibility service lifecycle
- [ ] UI scraping accuracy
- [ ] Database operations
- [ ] Voice command processing
- [ ] Cursor movement
- [ ] Overlay display

## Module 2: LearnApp (30 min)
- [ ] App exploration flow
- [ ] Consent dialogs
- [ ] Screen discovery
- [ ] Progress tracking
- [ ] Completion detection

## Module 3: VoiceUI (20 min)
- [ ] Navigation flows
- [ ] Settings persistence
- [ ] Theme switching
- [ ] Responsive layout

## Module 4: Speech Recognition (20 min)
- [ ] Engine switching
- [ ] Accuracy validation
- [ ] Language support
- [ ] Offline mode

## Module 5: Integration (20 min)
- [ ] End-to-end user flows
- [ ] Cross-module communication
- [ ] Data synchronization
- [ ] Error handling

## Pass Criteria
- 95%+ tests pass
- No critical bugs
- Performance acceptable
```

### 13.3 User Acceptance Testing (UAT)

**Test with real users:**

```markdown
# UAT Test Script

## Participant Profile
- Name: ___________
- Android Version: ___________
- Device: ___________
- Experience Level: Beginner / Intermediate / Advanced

## Task 1: Setup (5 min)
**Instructions:** "Install VoiceOS and enable accessibility service"
- [ ] Completed successfully
- [ ] Time taken: _____ minutes
- [ ] Difficulty: Easy / Medium / Hard
- [ ] Comments: ___________

## Task 2: Learn an App (10 min)
**Instructions:** "Use LearnApp to learn Instagram"
- [ ] Completed successfully
- [ ] Time taken: _____ minutes
- [ ] Difficulty: Easy / Medium / Hard
- [ ] Comments: ___________

## Task 3: Voice Command (5 min)
**Instructions:** "Say 'show database stats' to see your progress"
- [ ] Completed successfully
- [ ] Time taken: _____ minutes
- [ ] Difficulty: Easy / Medium / Hard
- [ ] Comments: ___________

## Overall Feedback
- What did you like? ___________
- What was confusing? ___________
- What would you improve? ___________
- Would you use this app? Yes / No / Maybe
```

---

## 14. CI/CD Integration

### 14.1 GitLab CI Configuration

**File: `.gitlab-ci.yml`**

```yaml
stages:
  - build
  - test
  - deploy

variables:
  ANDROID_COMPILE_SDK: "34"
  ANDROID_BUILD_TOOLS: "34.0.0"

before_script:
  - export GRADLE_USER_HOME=`pwd`/.gradle
  - chmod +x ./gradlew

cache:
  paths:
    - .gradle/wrapper
    - .gradle/caches

build:
  stage: build
  script:
    - ./gradlew assembleDebug
  artifacts:
    paths:
      - app/build/outputs/

unit_tests:
  stage: test
  script:
    - ./gradlew test
  artifacts:
    reports:
      junit: '**/build/test-results/test/TEST-*.xml'

integration_tests:
  stage: test
  script:
    - ./gradlew :tests:integration-tests:test
  artifacts:
    reports:
      junit: '**/build/test-results/test/TEST-*.xml'

lint:
  stage: test
  script:
    - ./gradlew lint
  artifacts:
    reports:
      codequality: '**/build/reports/lint-results*.xml'

deploy_beta:
  stage: deploy
  script:
    - ./gradlew assembleRelease
    # Upload to Play Store beta track
  only:
    - develop
  when: manual
```

### 14.2 Pre-Commit Hooks

**File: `.git/hooks/pre-commit`**

```bash
#!/bin/bash
# VoiceOS Pre-Commit Hook
# Runs tests before allowing commit

echo "Running pre-commit checks..."

# Run unit tests
echo "1. Running unit tests..."
./gradlew :tests:voiceoscore-unit-tests:test --quiet
if [ $? -ne 0 ]; then
    echo "❌ Unit tests failed. Commit aborted."
    exit 1
fi

# Run lint
echo "2. Running lint..."
./gradlew lint --quiet
if [ $? -ne 0 ]; then
    echo "⚠️  Lint issues found. Review before committing."
    # Don't block commit, just warn
fi

# Run static analysis
echo "3. Running static analysis..."
./gradlew detekt --quiet
if [ $? -ne 0 ]; then
    echo "⚠️  Code quality issues found."
fi

echo "✓ Pre-commit checks passed!"
exit 0
```

---

## 15. Test Reporting

### 15.1 JUnit HTML Reports

**Generate HTML reports:**

```bash
# Run tests with HTML report generation
./gradlew test

# Open report
open tests/voiceoscore-unit-tests/build/reports/tests/test/index.html
```

### 15.2 Code Coverage Reports

**Generate JaCoCo coverage:**

```bash
# Run tests with coverage
./gradlew jacocoTestReport

# Open coverage report
open tests/voiceoscore-unit-tests/build/reports/jacoco/test/html/index.html
```

**Configure JaCoCo: `build.gradle.kts`**

```kotlin
plugins {
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.10"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    // Exclude generated files
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/R.class",
                    "**/R$*.class",
                    "**/BuildConfig.*",
                    "**/Manifest*.*",
                    "**/*_Factory.*",
                    "**/*_MembersInjector.*"
                )
            }
        })
    )
}
```

### 15.3 Test Dashboard

**Sample Test Report Structure:**

```
VoiceOS Test Report - v4.1.1
Generated: 2025-11-07 14:30:00

┌─────────────────────────────────────────┐
│           Test Summary                  │
├─────────────────────────────────────────┤
│ Total Tests:        847                 │
│ Passed:             823 (97.2%)         │
│ Failed:             12 (1.4%)           │
│ Skipped:            12 (1.4%)           │
│ Duration:           18m 34s             │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│       Code Coverage                     │
├─────────────────────────────────────────┤
│ Line Coverage:      72.4%               │
│ Branch Coverage:    65.8%               │
│ Method Coverage:    78.3%               │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│     Module Breakdown                    │
├─────────────────────────────────────────┤
│ VoiceOSCore:        85% (423/498)       │
│ LearnApp:           68% (201/295)       │
│ VoiceUI:            62% (145/234)       │
│ SpeechRecognition:  79% (187/237)       │
│ Managers:           71% (234/329)       │
└─────────────────────────────────────────┘

Failed Tests:
  1. AppDaoTest.complexTransactionTest
  2. VoiceCommandProcessorTest.speechEngineFailure
  ...
```

---

## 16. Troubleshooting

### 16.1 Common Test Failures

**Problem: "Database not found"**
```
Solution:
- Ensure using in-memory database for tests
- Check database builder configuration
- Verify Room dependencies are correct
```

**Problem: "Coroutine test hangs"**
```
Solution:
- Use runTest from kotlinx-coroutines-test
- Set test dispatcher properly
- Check for blocking calls in suspend functions
```

**Problem: "AccessibilityService not available"**
```
Solution:
- Use Robolectric for unit tests
- Mock AccessibilityService for isolated tests
- Use instrumented tests for real service testing
```

### 16.2 Test Environment Issues

**Problem: "Gradle build fails"**
```bash
# Clear Gradle cache
rm -rf ~/.gradle/caches/
rm -rf .gradle/

# Re-sync
./gradlew clean build
```

**Problem: "Tests pass locally, fail on CI"**
```
Solution:
- Check CI environment variables
- Verify Android SDK versions match
- Review CI logs for specific errors
- Ensure deterministic test data
```

---

## 17. Best Practices

### 17.1 Test Writing Guidelines

**DO:**
- ✅ Write tests before fixing bugs
- ✅ Use meaningful test names
- ✅ Test one thing per test
- ✅ Use AAA pattern (Arrange, Act, Assert)
- ✅ Mock external dependencies
- ✅ Clean up resources in @After
- ✅ Use test data builders
- ✅ Assert on specific values, not just "not null"

**DON'T:**
- ❌ Test implementation details
- ❌ Write flaky tests (time-dependent, random)
- ❌ Share state between tests
- ❌ Mock what you don't own
- ❌ Test Android framework (trust it works)
- ❌ Ignore test failures

### 17.2 Code Coverage Goals

**Target Coverage:**
- **Critical Paths**: 100% (voice commands, database, accessibility)
- **Core Logic**: 80% (business logic, algorithms)
- **UI Code**: 60% (screens, composables)
- **Utilities**: 70% (helpers, extensions)

**Note:** Coverage is a metric, not a goal. 100% coverage doesn't guarantee bug-free code.

---

## 18. Resources

### 18.1 Testing Frameworks

- **JUnit 4**: https://junit.org/junit4/
- **Mockito**: https://site.mockito.org/
- **Robolectric**: http://robolectric.org/
- **Espresso**: https://developer.android.com/training/testing/espresso
- **Compose Testing**: https://developer.android.com/jetpack/compose/testing
- **Truth**: https://truth.dev/

### 18.2 Documentation

- **Android Testing Guide**: https://developer.android.com/training/testing
- **Room Testing**: https://developer.android.com/training/data-storage/room/testing-db
- **Coroutines Testing**: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/

### 18.3 Tools

- **Android Studio Profiler**: Memory, CPU, Network profiling
- **Firebase Test Lab**: Cloud-based device testing
- **LeakCanary**: Memory leak detection
- **Android Lint**: Static code analysis

---

## Appendix A: Test Data Builders

**File: `tests/common/TestDataBuilders.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.common

import com.augmentalis.voiceoscore.database.entities.AppEntity
import java.util.UUID

/**
 * Test data builders for consistent test data creation
 */
object TestDataBuilders {

    /**
     * Builder for AppEntity
     */
    fun appEntity(
        packageName: String = "com.test.app",
        appName: String = "Test App",
        versionCode: Long = 1L,
        versionName: String = "1.0.0",
        isFullyLearned: Boolean = false,
        exploredElementCount: Int? = null,
        scrapedElementCount: Int? = null
    ): AppEntity {
        return AppEntity(
            packageName = packageName,
            appId = UUID.randomUUID().toString(),
            appName = appName,
            versionCode = versionCode,
            versionName = versionName,
            appHash = "hash_$packageName",
            isFullyLearned = isFullyLearned,
            exploredElementCount = exploredElementCount,
            scrapedElementCount = scrapedElementCount,
            firstScraped = System.currentTimeMillis(),
            lastScraped = System.currentTimeMillis()
        )
    }

    /**
     * Batch create apps
     */
    fun apps(count: Int): List<AppEntity> {
        return (1..count).map { i ->
            appEntity(
                packageName = "com.test.app$i",
                appName = "App $i"
            )
        }
    }
}
```

---

## Appendix B: Test Utilities

**File: `tests/common/TestUtils.kt`**

```kotlin
package com.augmentalis.voiceoscore.tests.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Test utilities for common testing needs
 */

/**
 * JUnit rule for setting up coroutine test dispatcher
 */
@ExperimentalCoroutinesApi
class CoroutineTestRule : TestWatcher() {
    private val testDispatcher = StandardTestDispatcher()

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

/**
 * Extension function for waiting in tests
 */
fun waitFor(timeoutMs: Long = 5000, predicate: () -> Boolean): Boolean {
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeoutMs) {
        if (predicate()) {
            return true
        }
        Thread.sleep(100)
    }
    return false
}
```

---

**Version:** 4.1.1
**Last Updated:** 2025-11-07
**Maintainer:** VOS4 Development Team
**Framework:** IDEACODE v5.3

---

**Document Status:** ✅ Complete
**Next Review:** 2025-12-07 (monthly)
