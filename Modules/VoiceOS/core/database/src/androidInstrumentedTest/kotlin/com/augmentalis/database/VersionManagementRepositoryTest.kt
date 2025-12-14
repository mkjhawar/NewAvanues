/**
 * VersionManagementRepositoryTest.kt - Tests for version management repository methods
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-14
 *
 * Tests the 6 new version management methods in Schema v3.
 * Follows TDD principles - tests define expected behavior.
 */

package com.augmentalis.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.repositories.impl.SQLDelightGeneratedCommandRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VersionManagementRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var driver: AndroidSqliteDriver
    private lateinit var database: VoiceOSDatabase
    private lateinit var repository: SQLDelightGeneratedCommandRepository

    @Before
    fun setup() {
        // Create in-memory database for each test
        driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null  // In-memory database
        )
        database = VoiceOSDatabase(driver)
        repository = SQLDelightGeneratedCommandRepository(database)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    /**
     * Helper function to create test command
     */
    private fun createTestCommand(
        elementHash: String,
        commandText: String,
        appId: String = "com.test.app",
        versionCode: Long = 100L,
        appVersion: String = "1.0.0",
        isDeprecated: Long = 0L
    ): GeneratedCommandDTO {
        return GeneratedCommandDTO(
            id = 0,  // Auto-generated
            elementHash = elementHash,
            commandText = commandText,
            actionType = "CLICK",
            confidence = 0.8,
            synonyms = null,
            isUserApproved = 0L,
            usageCount = 0,
            lastUsed = null,
            createdAt = System.currentTimeMillis(),
            appId = appId,
            appVersion = appVersion,
            versionCode = versionCode,
            lastVerified = System.currentTimeMillis(),
            isDeprecated = isDeprecated
        )
    }

    // ========== markVersionDeprecated() Tests ==========

    /**
     * Test 1: markVersionDeprecated marks all commands for old version
     *
     * TDD: When app updates from v100 to v200, ALL v100 commands MUST be marked deprecated
     */
    @Test
    fun testMarkVersionDeprecated_marksAllCommandsForOldVersion() = runBlocking {
        // Insert 50 commands for Gmail v100
        repeat(50) { i ->
            val command = createTestCommand(
                elementHash = "gmail_element_$i",
                commandText = "click button $i",
                appId = "com.google.android.gm",
                versionCode = 100L,
                appVersion = "8.2024.10.100",
                isDeprecated = 0L
            )
            repository.insert(command)
        }

        // Insert 30 commands for Gmail v200 (already current)
        repeat(30) { i ->
            val command = createTestCommand(
                elementHash = "gmail_element_new_$i",
                commandText = "click new button $i",
                appId = "com.google.android.gm",
                versionCode = 200L,
                appVersion = "8.2024.11.200",
                isDeprecated = 0L
            )
            repository.insert(command)
        }

        // Mark v100 as deprecated
        val rowsAffected = repository.markVersionDeprecated("com.google.android.gm", 100L)

        // Verify exactly 50 commands were marked
        assertEquals("Should mark 50 v100 commands", 50, rowsAffected)

        // Verify deprecated commands
        val deprecated = repository.getDeprecatedCommands("com.google.android.gm")
        assertEquals("Should have 50 deprecated commands", 50, deprecated.size)
        assertTrue("All deprecated should be v100", deprecated.all { it.versionCode == 100L })

        // Verify active commands (v200 should remain active)
        val active = repository.getActiveCommands("com.google.android.gm", 200L, 100)
        assertEquals("Should have 30 active v200 commands", 30, active.size)
        assertTrue("All active should be v200", active.all { it.versionCode == 200L && it.isDeprecated == 0L })
    }

    /**
     * Test 2: markVersionDeprecated returns 0 when no commands match
     *
     * TDD: Marking non-existent version MUST return 0 without errors
     */
    @Test
    fun testMarkVersionDeprecated_returnsZeroWhenNoCommands() = runBlocking {
        // Insert commands for v100
        repeat(10) { i ->
            repository.insert(createTestCommand("hash_$i", "cmd $i", versionCode = 100L))
        }

        // Try to mark v200 (doesn't exist)
        val rowsAffected = repository.markVersionDeprecated("com.test.app", 200L)

        assertEquals("Should return 0 when no commands match", 0, rowsAffected)
    }

    /**
     * Test 3: markVersionDeprecated only affects specified package
     *
     * TDD: Deprecating Gmail v100 MUST NOT affect Chrome v100
     */
    @Test
    fun testMarkVersionDeprecated_onlyAffectsSpecifiedPackage() = runBlocking {
        // Insert Gmail v100 commands
        repeat(20) { i ->
            repository.insert(createTestCommand(
                "gmail_$i", "cmd $i",
                appId = "com.google.android.gm",
                versionCode = 100L
            ))
        }

        // Insert Chrome v100 commands
        repeat(15) { i ->
            repository.insert(createTestCommand(
                "chrome_$i", "cmd $i",
                appId = "com.android.chrome",
                versionCode = 100L
            ))
        }

        // Mark only Gmail v100 as deprecated
        val rowsAffected = repository.markVersionDeprecated("com.google.android.gm", 100L)

        assertEquals("Should mark only Gmail commands", 20, rowsAffected)

        // Verify Chrome commands are still active
        val chromeActive = repository.getActiveCommands("com.android.chrome", 100L, 100)
        assertEquals("Chrome v100 should remain active", 15, chromeActive.size)
    }

    // ========== updateCommandVersion() Tests ==========

    /**
     * Test 4: updateCommandVersion updates all version fields
     *
     * TDD: MUST update versionCode, appVersion, lastVerified, and isDeprecated
     */
    @Test
    fun testUpdateCommandVersion_updatesAllFields() = runBlocking {
        // Insert command with old version
        val commandId = repository.insert(createTestCommand(
            "test_hash", "test cmd",
            versionCode = 100L,
            appVersion = "1.0.0",
            isDeprecated = 1L  // Deprecated
        ))

        val newTimestamp = System.currentTimeMillis()

        // Update to new version
        repository.updateCommandVersion(
            id = commandId,
            versionCode = 200L,
            appVersion = "2.0.0",
            lastVerified = newTimestamp,
            isDeprecated = 0L  // No longer deprecated
        )

        // Verify all fields updated
        val updated = repository.getById(commandId)!!
        assertEquals("Version code should be updated", 200L, updated.versionCode)
        assertEquals("App version should be updated", "2.0.0", updated.appVersion)
        assertEquals("Last verified should be updated", newTimestamp, updated.lastVerified)
        assertEquals("Should no longer be deprecated", 0L, updated.isDeprecated)
    }

    /**
     * Test 5: updateCommandVersion preserves other fields
     *
     * TDD: MUST NOT modify elementHash, commandText, usageCount, or other fields
     */
    @Test
    fun testUpdateCommandVersion_preservesOtherFields() = runBlocking {
        // Insert command with specific values
        val command = createTestCommand("test_hash", "click button")
        val commandId = repository.insert(command)

        // Increment usage before update
        repository.incrementUsage(commandId, System.currentTimeMillis())
        repository.markApproved(commandId)

        // Update version
        repository.updateCommandVersion(
            id = commandId,
            versionCode = 200L,
            appVersion = "2.0.0",
            lastVerified = System.currentTimeMillis(),
            isDeprecated = 0L
        )

        // Verify other fields preserved
        val updated = repository.getById(commandId)!!
        assertEquals("Element hash should be preserved", "test_hash", updated.elementHash)
        assertEquals("Command text should be preserved", "click button", updated.commandText)
        assertEquals("Usage count should be preserved", 1L, updated.usageCount)
        assertEquals("Approved status should be preserved", 1L, updated.isUserApproved)
    }

    // ========== updateCommandDeprecated() Tests ==========

    /**
     * Test 6: updateCommandDeprecated marks command as deprecated
     *
     * TDD: MUST change isDeprecated from 0 to 1
     */
    @Test
    fun testUpdateCommandDeprecated_marksAsDeprecated() = runBlocking {
        // Insert active command
        val commandId = repository.insert(createTestCommand(
            "test", "cmd",
            isDeprecated = 0L
        ))

        // Mark as deprecated
        repository.updateCommandDeprecated(commandId, 1L)

        // Verify deprecated
        val updated = repository.getById(commandId)!!
        assertEquals("Should be deprecated", 1L, updated.isDeprecated)
    }

    /**
     * Test 7: updateCommandDeprecated marks command as active
     *
     * TDD: MUST change isDeprecated from 1 to 0 (un-deprecate)
     */
    @Test
    fun testUpdateCommandDeprecated_marksAsActive() = runBlocking {
        // Insert deprecated command
        val commandId = repository.insert(createTestCommand(
            "test", "cmd",
            isDeprecated = 1L
        ))

        // Mark as active (un-deprecate)
        repository.updateCommandDeprecated(commandId, 0L)

        // Verify active
        val updated = repository.getById(commandId)!!
        assertEquals("Should be active", 0L, updated.isDeprecated)
    }

    // ========== deleteDeprecatedCommands() Tests ==========

    /**
     * Test 8: deleteDeprecatedCommands respects grace period
     *
     * TDD: MUST delete only commands with lastVerified < threshold
     */
    @Test
    fun testDeleteDeprecatedCommands_respectsGracePeriod() = runBlocking {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)  // 30 days in ms
        val fifteenDaysAgo = now - (15L * 24 * 60 * 60 * 1000)  // 15 days

        // Insert old deprecated command (45 days old - should be deleted)
        repository.insert(createTestCommand(
            "old", "old cmd",
            isDeprecated = 1L
        ).copy(lastVerified = thirtyDaysAgo - (15L * 24 * 60 * 60 * 1000)))

        // Insert recent deprecated command (15 days old - should be kept)
        repository.insert(createTestCommand(
            "recent", "recent cmd",
            isDeprecated = 1L
        ).copy(lastVerified = fifteenDaysAgo))

        // Delete commands older than 30 days
        val deleted = repository.deleteDeprecatedCommands(
            olderThan = thirtyDaysAgo,
            keepUserApproved = false
        )

        assertEquals("Should delete 1 old command", 1, deleted)

        // Verify recent command still exists
        val remaining = repository.getAll()
        assertEquals("Should have 1 command remaining", 1, remaining.size)
        assertEquals("Remaining should be recent command", "recent cmd", remaining[0].commandText)
    }

    /**
     * Test 9: deleteDeprecatedCommands preserves user-approved commands
     *
     * TDD: When keepUserApproved=true, MUST preserve commands with isUserApproved=1
     */
    @Test
    fun testDeleteDeprecatedCommands_preservesUserApproved() = runBlocking {
        val oldTimestamp = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)  // 60 days ago

        // Insert old deprecated user-approved command
        repository.insert(createTestCommand(
            "approved", "approved cmd",
            isDeprecated = 1L
        ).copy(lastVerified = oldTimestamp, isUserApproved = 1L))

        // Insert old deprecated non-approved command
        repository.insert(createTestCommand(
            "not_approved", "not approved cmd",
            isDeprecated = 1L
        ).copy(lastVerified = oldTimestamp, isUserApproved = 0L))

        // Delete with keepUserApproved=true
        val deleted = repository.deleteDeprecatedCommands(
            olderThan = System.currentTimeMillis(),
            keepUserApproved = true
        )

        assertEquals("Should delete only non-approved command", 1, deleted)

        // Verify user-approved command preserved
        val remaining = repository.getAll()
        assertEquals("Should have 1 command remaining", 1, remaining.size)
        assertEquals("Should preserve approved command", "approved cmd", remaining[0].commandText)
        assertEquals("Should be user-approved", 1L, remaining[0].isUserApproved)
    }

    /**
     * Test 10: deleteDeprecatedCommands deletes user-approved when flag is false
     *
     * TDD: When keepUserApproved=false, MUST delete all old deprecated commands
     */
    @Test
    fun testDeleteDeprecatedCommands_deletesUserApprovedWhenFlagFalse() = runBlocking {
        val oldTimestamp = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)

        // Insert old deprecated user-approved command
        repository.insert(createTestCommand(
            "approved", "approved cmd",
            isDeprecated = 1L
        ).copy(lastVerified = oldTimestamp, isUserApproved = 1L))

        // Delete with keepUserApproved=false
        val deleted = repository.deleteDeprecatedCommands(
            olderThan = System.currentTimeMillis(),
            keepUserApproved = false
        )

        assertEquals("Should delete user-approved command", 1, deleted)
        assertEquals("Database should be empty", 0, repository.count())
    }

    /**
     * Test 11: deleteDeprecatedCommands only deletes deprecated commands
     *
     * TDD: MUST NOT delete active commands (isDeprecated=0)
     */
    @Test
    fun testDeleteDeprecatedCommands_onlyDeletesDeprecated() = runBlocking {
        val oldTimestamp = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)

        // Insert old active command (should NOT be deleted)
        repository.insert(createTestCommand(
            "active", "active cmd",
            isDeprecated = 0L
        ).copy(lastVerified = oldTimestamp))

        // Insert old deprecated command (should be deleted)
        repository.insert(createTestCommand(
            "deprecated", "deprecated cmd",
            isDeprecated = 1L
        ).copy(lastVerified = oldTimestamp))

        // Delete old deprecated commands
        val deleted = repository.deleteDeprecatedCommands(
            olderThan = System.currentTimeMillis(),
            keepUserApproved = false
        )

        assertEquals("Should delete only deprecated command", 1, deleted)

        // Verify active command preserved
        val remaining = repository.getAll()
        assertEquals("Should have 1 active command", 1, remaining.size)
        assertEquals("Should preserve active command", "active cmd", remaining[0].commandText)
        assertEquals("Should be active", 0L, remaining[0].isDeprecated)
    }

    // ========== getDeprecatedCommands() Tests ==========

    /**
     * Test 12: getDeprecatedCommands returns only deprecated commands
     *
     * TDD: MUST return only commands with isDeprecated=1 for specified package
     */
    @Test
    fun testGetDeprecatedCommands_returnsOnlyDeprecated() = runBlocking {
        val packageName = "com.test.app"

        // Insert active commands
        repeat(20) { i ->
            repository.insert(createTestCommand(
                "active_$i", "active $i",
                appId = packageName,
                isDeprecated = 0L
            ))
        }

        // Insert deprecated commands
        repeat(15) { i ->
            repository.insert(createTestCommand(
                "deprecated_$i", "deprecated $i",
                appId = packageName,
                isDeprecated = 1L
            ))
        }

        // Get deprecated commands
        val deprecated = repository.getDeprecatedCommands(packageName)

        assertEquals("Should return 15 deprecated commands", 15, deprecated.size)
        assertTrue("All should be deprecated", deprecated.all { it.isDeprecated == 1L })
        assertTrue("All should be for correct package", deprecated.all { it.appId == packageName })
    }

    /**
     * Test 13: getDeprecatedCommands sorted by lastVerified descending
     *
     * TDD: MUST return newest deprecated commands first
     */
    @Test
    fun testGetDeprecatedCommands_sortedByLastVerifiedDesc() = runBlocking {
        val now = System.currentTimeMillis()
        val packageName = "com.test.app"

        // Insert deprecated commands with different timestamps
        repository.insert(createTestCommand(
            "old", "old",
            appId = packageName,
            isDeprecated = 1L
        ).copy(lastVerified = now - 10000))

        repository.insert(createTestCommand(
            "newest", "newest",
            appId = packageName,
            isDeprecated = 1L
        ).copy(lastVerified = now))

        repository.insert(createTestCommand(
            "middle", "middle",
            appId = packageName,
            isDeprecated = 1L
        ).copy(lastVerified = now - 5000))

        // Get deprecated commands
        val deprecated = repository.getDeprecatedCommands(packageName)

        assertEquals("Should have 3 commands", 3, deprecated.size)
        assertEquals("First should be newest", "newest", deprecated[0].commandText)
        assertEquals("Second should be middle", "middle", deprecated[1].commandText)
        assertEquals("Third should be oldest", "old", deprecated[2].commandText)
    }

    // ========== getActiveCommands() Tests ==========

    /**
     * Test 14: getActiveCommands filters by version and deprecated status
     *
     * TDD: MUST return only commands with matching versionCode AND isDeprecated=0
     */
    @Test
    fun testGetActiveCommands_filtersByVersionAndDeprecated() = runBlocking {
        val packageName = "com.test.app"

        // Insert v100 active commands (should NOT be returned)
        repeat(10) { i ->
            repository.insert(createTestCommand(
                "v100_$i", "v100 $i",
                appId = packageName,
                versionCode = 100L,
                isDeprecated = 0L
            ))
        }

        // Insert v200 deprecated commands (should NOT be returned)
        repeat(5) { i ->
            repository.insert(createTestCommand(
                "v200_dep_$i", "v200 deprecated $i",
                appId = packageName,
                versionCode = 200L,
                isDeprecated = 1L
            ))
        }

        // Insert v200 active commands (SHOULD be returned)
        repeat(25) { i ->
            repository.insert(createTestCommand(
                "v200_active_$i", "v200 active $i",
                appId = packageName,
                versionCode = 200L,
                isDeprecated = 0L
            ))
        }

        // Get active commands for v200
        val active = repository.getActiveCommands(packageName, 200L, 100)

        assertEquals("Should return 25 v200 active commands", 25, active.size)
        assertTrue("All should be v200", active.all { it.versionCode == 200L })
        assertTrue("All should be active", active.all { it.isDeprecated == 0L })
        assertTrue("All should be for correct package", active.all { it.appId == packageName })
    }

    /**
     * Test 15: getActiveCommands respects limit
     *
     * TDD: MUST return at most 'limit' commands, sorted by usage
     */
    @Test
    fun testGetActiveCommands_respectsLimit() = runBlocking {
        val packageName = "com.test.app"

        // Insert 100 active commands with varying usage
        repeat(100) { i ->
            val commandId = repository.insert(createTestCommand(
                "cmd_$i", "command $i",
                appId = packageName,
                versionCode = 100L,
                isDeprecated = 0L
            ))

            // Give varying usage counts (highest usage = cmd_99)
            repeat(i) {
                repository.incrementUsage(commandId, System.currentTimeMillis())
            }
        }

        // Get top 10 active commands
        val active = repository.getActiveCommands(packageName, 100L, limit = 10)

        assertEquals("Should return exactly 10 commands", 10, active.size)

        // Verify sorted by usage descending
        assertTrue("Should be sorted by usage desc",
            active.zipWithNext().all { (a, b) -> a.usageCount >= b.usageCount })

        // Verify highest usage commands returned
        assertTrue("Should include most-used commands",
            active[0].usageCount >= 90L)  // cmd_99, cmd_98, etc.
    }
}
