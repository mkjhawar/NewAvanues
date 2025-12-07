/**
 * RepositoryTests.kt - Unit tests for VoiceDataManager SQLDelight repositories
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 *
 * Tests repository wrappers and their integration with SQLDelight
 */
package com.augmentalis.datamanager.data

import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.database.dto.CommandHistoryDTO
import com.augmentalis.database.dto.CustomCommandDTO
import com.augmentalis.database.dto.ErrorReportDTO
import com.augmentalis.database.dto.UserPreferenceDTO
import com.augmentalis.database.repositories.ICommandHistoryRepository
import com.augmentalis.database.repositories.ICommandRepository
import com.augmentalis.database.repositories.IErrorReportRepository
import com.augmentalis.database.repositories.IUserPreferenceRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryTests {

    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    private lateinit var mockCommandRepo: ICommandRepository
    private lateinit var mockHistoryRepo: ICommandHistoryRepository
    private lateinit var mockPreferenceRepo: IUserPreferenceRepository
    private lateinit var mockErrorRepo: IErrorReportRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock all repositories
        mockCommandRepo = mockk(relaxed = true)
        mockHistoryRepo = mockk(relaxed = true)
        mockPreferenceRepo = mockk(relaxed = true)
        mockErrorRepo = mockk(relaxed = true)

        // Mock DatabaseManager object
        mockkObject(DatabaseManager)
        every { DatabaseManager.commands } returns mockCommandRepo
        every { DatabaseManager.commandHistory } returns mockHistoryRepo
        every { DatabaseManager.userPreferences } returns mockPreferenceRepo
        every { DatabaseManager.errorReports } returns mockErrorRepo
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== CustomCommandRepo Tests ====================

    @Test
    fun `CustomCommandRepo insert delegates to repository`() = runTest {
        val repo = CustomCommandRepo()
        val command = CustomCommandDTO(
            id = 0,
            name = "Test Command",
            description = "Test description",
            phrases = listOf("test", "testing"),
            action = "ACTION",
            parameters = null,
            language = "en",
            isActive = true,
            usageCount = 0
        )

        coEvery { mockCommandRepo.insert(any()) } returns 1L

        val result = repo.insert(command)

        assertEquals(1L, result)
        coVerify { mockCommandRepo.insert(command) }
    }

    @Test
    fun `CustomCommandRepo getAll returns all commands`() = runTest {
        val repo = CustomCommandRepo()
        val commands = listOf(
            CustomCommandDTO(id = 1, name = "Cmd1", description = null, phrases = listOf("cmd1"),
                action = "ACT", parameters = null, language = "en", isActive = true, usageCount = 0),
            CustomCommandDTO(id = 2, name = "Cmd2", description = null, phrases = listOf("cmd2"),
                action = "ACT", parameters = null, language = "en", isActive = true, usageCount = 0)
        )

        coEvery { mockCommandRepo.getAll() } returns commands

        val result = repo.getAll()

        assertEquals(2, result.size)
        assertEquals("Cmd1", result[0].name)
        assertEquals("Cmd2", result[1].name)
    }

    @Test
    fun `CustomCommandRepo getActiveCommands filters active commands`() = runTest {
        val repo = CustomCommandRepo()
        val activeCommands = listOf(
            CustomCommandDTO(id = 1, name = "Active", description = null, phrases = listOf("active"),
                action = "ACT", parameters = null, language = "en", isActive = true, usageCount = 0)
        )

        coEvery { mockCommandRepo.getActive() } returns activeCommands

        val result = repo.getActiveCommands()

        assertEquals(1, result.size)
        assertTrue(result[0].isActive)
    }

    @Test
    fun `CustomCommandRepo toggleCommandActive toggles status`() = runTest {
        val repo = CustomCommandRepo()
        val command = CustomCommandDTO(
            id = 1, name = "Test", description = null, phrases = listOf("test"),
            action = "ACT", parameters = null, language = "en", isActive = true, usageCount = 0
        )

        coEvery { mockCommandRepo.getById(1L) } returns command
        coEvery { mockCommandRepo.setActiveStatus(1L, false) } just Runs

        repo.toggleCommandActive(1L)

        coVerify { mockCommandRepo.setActiveStatus(1L, false) }
    }

    @Test
    fun `CustomCommandRepo deleteById removes command`() = runTest {
        val repo = CustomCommandRepo()

        coEvery { mockCommandRepo.delete(1L) } just Runs

        repo.deleteById(1L)

        coVerify { mockCommandRepo.delete(1L) }
    }

    @Test
    fun `CustomCommandRepo count returns total commands`() = runTest {
        val repo = CustomCommandRepo()

        coEvery { mockCommandRepo.count() } returns 10L

        val result = repo.count()

        assertEquals(10L, result)
    }

    // ==================== CommandHistoryRepo Tests ====================

    @Test
    fun `CommandHistoryRepo insert creates history entry`() = runTest {
        val repo = CommandHistoryRepo()
        val entry = CommandHistoryDTO(
            id = 0,
            originalText = "go home",
            processedCommand = "HOME",
            confidence = 0.95,
            timestamp = System.currentTimeMillis(),
            language = "en",
            engineUsed = "Vosk",
            success = true,
            executionTimeMs = 150
        )

        coEvery { mockHistoryRepo.insert(any()) } returns 1L

        val result = repo.insert(entry)

        assertEquals(1L, result)
        coVerify { mockHistoryRepo.insert(entry) }
    }

    @Test
    fun `CommandHistoryRepo getRecentCommands returns limited results`() = runTest {
        val repo = CommandHistoryRepo()
        val entries = listOf(
            CommandHistoryDTO(id = 1, originalText = "cmd1", processedCommand = "CMD1",
                confidence = 0.9, timestamp = 1000, language = "en", engineUsed = "Vosk",
                success = true, executionTimeMs = 100)
        )

        coEvery { mockHistoryRepo.getRecent(50) } returns entries

        val result = repo.getRecentCommands(50)

        assertEquals(1, result.size)
    }

    @Test
    fun `CommandHistoryRepo getSuccessRate calculates rate`() = runTest {
        val repo = CommandHistoryRepo()

        coEvery { mockHistoryRepo.getSuccessRate() } returns 0.85

        val result = repo.getSuccessRate()

        assertEquals(0.85f, result)
    }

    @Test
    fun `CommandHistoryRepo cleanupOldEntries removes old entries`() = runTest {
        val repo = CommandHistoryRepo()

        coEvery { mockHistoryRepo.cleanupOldEntries(any(), any()) } just Runs

        repo.cleanupOldEntries(100, 30)

        coVerify { mockHistoryRepo.cleanupOldEntries(any(), 100L) }
    }

    @Test
    fun `CommandHistoryRepo deleteAll clears all entries`() = runTest {
        val repo = CommandHistoryRepo()

        coEvery { mockHistoryRepo.deleteAll() } just Runs

        repo.deleteAll()

        coVerify { mockHistoryRepo.deleteAll() }
    }

    @Test
    fun `CommandHistoryRepo count returns total entries`() = runTest {
        val repo = CommandHistoryRepo()

        coEvery { mockHistoryRepo.count() } returns 42L

        val result = repo.count()

        assertEquals(42L, result)
    }

    // ==================== UserPreferenceRepo Tests ====================

    @Test
    fun `UserPreferenceRepo setPreference stores key-value`() = runTest {
        val repo = UserPreferenceRepo()

        coEvery { mockPreferenceRepo.setValue(any(), any(), any()) } just Runs

        repo.setPreference("theme", "dark", "string")

        coVerify { mockPreferenceRepo.setValue("theme", "dark", "string") }
    }

    @Test
    fun `UserPreferenceRepo getString returns value or default`() = runTest {
        val repo = UserPreferenceRepo()

        coEvery { mockPreferenceRepo.getValue("theme") } returns "dark"
        coEvery { mockPreferenceRepo.getValue("missing") } returns null

        val result1 = repo.getString("theme", "light")
        val result2 = repo.getString("missing", "default")

        assertEquals("dark", result1)
        assertEquals("default", result2)
    }

    @Test
    fun `UserPreferenceRepo getInt parses integer value`() = runTest {
        val repo = UserPreferenceRepo()

        coEvery { mockPreferenceRepo.getValue("volume") } returns "80"

        val result = repo.getInt("volume", 50)

        assertEquals(80, result)
    }

    @Test
    fun `UserPreferenceRepo getBoolean parses boolean value`() = runTest {
        val repo = UserPreferenceRepo()

        coEvery { mockPreferenceRepo.getValue("enabled") } returns "true"

        val result = repo.getBoolean("enabled", false)

        assertEquals(true, result)
    }

    @Test
    fun `UserPreferenceRepo getAll returns all entries`() = runTest {
        val repo = UserPreferenceRepo()
        val prefs = listOf(
            UserPreferenceDTO(key = "theme", value = "dark", type = "string",
                updatedAt = System.currentTimeMillis())
        )

        coEvery { mockPreferenceRepo.getAll() } returns prefs

        val result = repo.getAll()

        assertEquals(1, result.size)
        assertEquals("theme", result[0].key)
    }

    @Test
    fun `UserPreferenceRepo delete removes key`() = runTest {
        val repo = UserPreferenceRepo()

        coEvery { mockPreferenceRepo.delete("theme") } just Runs

        repo.delete("theme")

        coVerify { mockPreferenceRepo.delete("theme") }
    }

    @Test
    fun `UserPreferenceRepo deleteAll removes all`() = runTest {
        val repo = UserPreferenceRepo()

        coEvery { mockPreferenceRepo.deleteAll() } just Runs

        repo.deleteAll()

        coVerify { mockPreferenceRepo.deleteAll() }
    }

    @Test
    fun `UserPreferenceRepo exists checks key presence`() = runTest {
        val repo = UserPreferenceRepo()

        coEvery { mockPreferenceRepo.exists("theme") } returns true
        coEvery { mockPreferenceRepo.exists("missing") } returns false

        assertTrue(repo.exists("theme"))
        assertEquals(false, repo.exists("missing"))
    }

    // ==================== ErrorReportRepo Tests ====================

    @Test
    fun `ErrorReportRepo insert creates error report`() = runTest {
        val repo = ErrorReportRepo()
        val report = ErrorReportDTO(
            id = 0,
            errorType = "NullPointer",
            message = "Object is null",
            stackTrace = "stack...",
            context = null,
            timestamp = System.currentTimeMillis(),
            isSent = false
        )

        coEvery { mockErrorRepo.insert(any()) } returns 1L

        val result = repo.insert(report)

        assertEquals(1L, result)
    }

    @Test
    fun `ErrorReportRepo getUnsentReports returns pending reports`() = runTest {
        val repo = ErrorReportRepo()
        val reports = listOf(
            ErrorReportDTO(id = 1, errorType = "Error", message = "msg", stackTrace = "stack",
                context = null, timestamp = 1000, isSent = false)
        )

        coEvery { mockErrorRepo.getUnsent() } returns reports

        val result = repo.getUnsentReports()

        assertEquals(1, result.size)
        assertEquals(false, result[0].isSent)
    }

    @Test
    fun `ErrorReportRepo markAsSent updates report status`() = runTest {
        val repo = ErrorReportRepo()

        coEvery { mockErrorRepo.markSent(1L) } just Runs

        repo.markAsSent(1L)

        coVerify { mockErrorRepo.markSent(1L) }
    }

    @Test
    fun `ErrorReportRepo cleanupSentReports cleans up old entries`() = runTest {
        val repo = ErrorReportRepo()

        coEvery { mockErrorRepo.deleteOlderThan(any()) } just Runs

        repo.cleanupSentReports(30)

        coVerify { mockErrorRepo.deleteOlderThan(any()) }
    }

    @Test
    fun `ErrorReportRepo count returns total reports`() = runTest {
        val repo = ErrorReportRepo()

        coEvery { mockErrorRepo.count() } returns 5L

        val result = repo.count()

        assertEquals(5L, result)
    }

    @Test
    fun `ErrorReportRepo getAll returns all reports`() = runTest {
        val repo = ErrorReportRepo()
        val reports = listOf(
            ErrorReportDTO(id = 1, errorType = "Error", message = "msg", stackTrace = "stack",
                context = null, timestamp = 1000, isSent = false)
        )

        coEvery { mockErrorRepo.getAll() } returns reports

        val result = repo.getAll()

        assertEquals(1, result.size)
    }

    @Test
    fun `ErrorReportRepo deleteAll clears all reports`() = runTest {
        val repo = ErrorReportRepo()

        coEvery { mockErrorRepo.deleteAll() } just Runs

        repo.deleteAll()

        coVerify { mockErrorRepo.deleteAll() }
    }

    @Test
    fun `ErrorReportRepo getById returns specific report`() = runTest {
        val repo = ErrorReportRepo()
        val report = ErrorReportDTO(id = 1, errorType = "Error", message = "msg",
            stackTrace = "stack", context = null, timestamp = 1000, isSent = false)

        coEvery { mockErrorRepo.getById(1L) } returns report

        val result = repo.getById(1L)

        assertEquals("Error", result?.errorType)
    }
}
