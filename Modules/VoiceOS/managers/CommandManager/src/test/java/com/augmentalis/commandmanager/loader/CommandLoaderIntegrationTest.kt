/**
 * CommandLoaderIntegrationTest.kt
 *
 * Created: 2025-10-23 02:43 PDT
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Integration tests for CommandLoader with CommandManager
 * Coverage: Initialization, command loading, locale handling, English fallback
 * Location: CommandManager test suite
 *
 * Changelog:
 * - v1.0.0 (2025-10-23): Initial integration test suite for CommandLoader
 */

package com.augmentalis.voiceoscore.loader

import android.content.Context
import com.augmentalis.voiceoscore.database.CommandDatabase
import com.augmentalis.voiceoscore.database.DatabaseVersionDao
import com.augmentalis.voiceoscore.database.sqldelight.DatabaseVersionEntity
import com.augmentalis.voiceoscore.database.VoiceCommandDao
import com.augmentalis.voiceoscore.database.sqldelight.VoiceCommandEntity
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

/**
 * Integration tests for CommandLoader with CommandManager.
 *
 * Test Coverage:
 * - initializeCommands() calls CommandLoader.initializeCommands()
 * - Commands are loaded for current locale
 * - English fallback when locale unavailable
 * - Command count matches for each locale
 * - Database persistence of localized commands
 * - Version tracking prevents duplicate loads
 * - Error handling for missing JSON files
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class CommandLoaderIntegrationTest {

    private lateinit var mockContext: Context
    private lateinit var mockCommandDao: VoiceCommandDao
    private lateinit var mockVersionDao: DatabaseVersionDao
    private lateinit var commandLoader: CommandLoader

    // Sample JSON data for testing
    private val englishCommandsJson = """
        {
            "version": "1.0",
            "locale": "en-US",
            "commands": [
                {
                    "action_id": "nav_back",
                    "primary_text": "go back",
                    "synonyms": ["back", "return"],
                    "category": "navigation",
                    "description": "Navigate back"
                },
                {
                    "action_id": "nav_home",
                    "primary_text": "go home",
                    "synonyms": ["home"],
                    "category": "navigation",
                    "description": "Go to home screen"
                }
            ]
        }
    """.trimIndent()

    private val spanishCommandsJson = """
        {
            "version": "1.0",
            "locale": "es-ES",
            "commands": [
                {
                    "action_id": "nav_back",
                    "primary_text": "volver",
                    "synonyms": ["regresar", "atrás"],
                    "category": "navigation",
                    "description": "Navegar hacia atrás"
                }
            ]
        }
    """.trimIndent()

    @Before
    fun setUp() {
        // Mock Context and AssetManager
        mockContext = mockk(relaxed = true)
        val mockAssets = mockk<android.content.res.AssetManager>(relaxed = true)
        every { mockContext.assets } returns mockAssets

        // Mock VoiceCommandDao
        mockCommandDao = mockk(relaxed = true)
        coEvery { mockCommandDao.hasCommandsForLocale(any()) } returns false
        coEvery { mockCommandDao.getCommandCount(any()) } returns 0
        coEvery { mockCommandDao.insertBatch(any()) } returns listOf(1L, 2L)
        coEvery { mockCommandDao.deleteAllCommands() } returns 0
        coEvery { mockCommandDao.getDatabaseStats() } returns emptyList()

        // Mock DatabaseVersionDao
        mockVersionDao = mockk(relaxed = true)
        coEvery { mockVersionDao.getVersion() } returns null
        coEvery { mockVersionDao.setVersion(any()) } just Runs
        coEvery { mockVersionDao.clearVersion() } just Runs

        // Create CommandLoader with mocked dependencies
        commandLoader = CommandLoader(mockContext, mockCommandDao, mockVersionDao)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ===== Initialization Tests =====

    @Test
    fun `initializeCommands calls CommandLoader initializeCommands`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(englishCommandsJson.toByteArray())

        coEvery { mockCommandDao.getDatabaseStats() } returns listOf(
            mockk {
                every { locale } returns "en-US"
                every { count } returns 2
            }
        )

        // Act
        val result = commandLoader.initializeCommands()

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        coVerify { mockVersionDao.setVersion(any()) }
    }

    @Test
    fun `initializeCommands loads English first as fallback`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(englishCommandsJson.toByteArray())

        coEvery { mockCommandDao.getDatabaseStats() } returns listOf(
            mockk {
                every { locale } returns "en-US"
                every { count } returns 2
            }
        )

        // Act
        val result = commandLoader.initializeCommands()

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        // Verify English was loaded with fallback flag
        coVerify { mockCommandDao.insertBatch(any()) }
    }

    @Test
    fun `initializeCommands skips reload when version matches`() = runTest {
        // Arrange
        val existingVersion = DatabaseVersionEntity(
            id = 1,
            jsonVersion = "1.0",
            commandCount = 50,
            locales = "en-US,es-ES",
            loadedAt = System.currentTimeMillis()
        )
        coEvery { mockVersionDao.getVersion() } returns existingVersion
        coEvery { mockCommandDao.getCommandCount("en-US") } returns 50

        // Act
        val result = commandLoader.initializeCommands()

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        val success = result as CommandLoader.LoadResult.Success
        assertEquals(50, success.commandCount)
        // Should NOT reload
        coVerify(exactly = 0) { mockCommandDao.insertBatch(any()) }
    }

    @Test
    fun `initializeCommands reloads when version mismatch`() = runTest {
        // Arrange
        val oldVersion = DatabaseVersionEntity(
            id = 1,
            jsonVersion = "0.9", // Old version
            commandCount = 40,
            locales = "en-US",
            loadedAt = System.currentTimeMillis()
        )
        coEvery { mockVersionDao.getVersion() } returns oldVersion

        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(englishCommandsJson.toByteArray())

        coEvery { mockCommandDao.getDatabaseStats() } returns listOf(
            mockk {
                every { locale } returns "en-US"
                every { count } returns 2
            }
        )

        // Act
        val result = commandLoader.initializeCommands()

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        // Should reload because version mismatch
        coVerify { mockCommandDao.insertBatch(any()) }
    }

    // ===== Locale Loading Tests =====

    @Test
    fun `loadLocale successfully loads English commands`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(englishCommandsJson.toByteArray())

        // Act
        val result = commandLoader.loadLocale("en-US", isFallback = true)

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        val success = result as CommandLoader.LoadResult.Success
        assertEquals(2, success.commandCount)
        assertEquals(listOf("en-US"), success.locales)
        coVerify { mockCommandDao.insertBatch(any()) }
    }

    @Test
    fun `loadLocale successfully loads Spanish commands`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/es-ES.json") } returns ByteArrayInputStream(spanishCommandsJson.toByteArray())

        // Act
        val result = commandLoader.loadLocale("es-ES", isFallback = false)

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        val success = result as CommandLoader.LoadResult.Success
        assertEquals(1, success.commandCount)
        coVerify { mockCommandDao.insertBatch(any()) }
    }

    @Test
    fun `loadLocale skips if already loaded`() = runTest {
        // Arrange
        coEvery { mockCommandDao.hasCommandsForLocale("en-US") } returns true
        coEvery { mockCommandDao.getCommandCount("en-US") } returns 50

        // Act
        val result = commandLoader.loadLocale("en-US", isFallback = true)

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        val success = result as CommandLoader.LoadResult.Success
        assertEquals(50, success.commandCount)
        // Should NOT insert again
        coVerify(exactly = 0) { mockCommandDao.insertBatch(any()) }
    }

    @Test
    fun `loadLocale returns LocaleNotFound for missing JSON`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/xx-XX.json") } throws FileNotFoundException()

        // Act
        val result = commandLoader.loadLocale("xx-XX", isFallback = false)

        // Assert
        assertTrue(result is CommandLoader.LoadResult.LocaleNotFound)
        val notFound = result as CommandLoader.LoadResult.LocaleNotFound
        assertEquals("xx-XX", notFound.locale)
    }

    @Test
    fun `loadLocale returns Error for invalid JSON`() = runTest {
        // Arrange
        val invalidJson = "{ invalid json }"
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(invalidJson.toByteArray())

        // Act
        val result = commandLoader.loadLocale("en-US", isFallback = true)

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Error)
    }

    // ===== English Fallback Tests =====

    @Test
    fun `English is always loaded as fallback`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(englishCommandsJson.toByteArray())

        coEvery { mockCommandDao.getDatabaseStats() } returns listOf(
            mockk {
                every { locale } returns "en-US"
                every { count } returns 2
            }
        )

        // Act
        val result = commandLoader.initializeCommands()

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        // Verify English was loaded with isFallback = true
        coVerify { mockCommandDao.insertBatch(any()) }
    }

    @Test
    fun `User locale loads after English fallback`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(englishCommandsJson.toByteArray())
        every { mockAssets.open("localization/commands/es-ES.json") } returns ByteArrayInputStream(spanishCommandsJson.toByteArray())

        coEvery { mockCommandDao.getDatabaseStats() } returns listOf(
            mockk {
                every { locale } returns "en-US"
                every { count } returns 2
            },
            mockk {
                every { locale } returns "es-ES"
                every { count } returns 1
            }
        )

        // Set system locale to Spanish
        java.util.Locale.setDefault(java.util.Locale("es", "ES"))

        // Act
        val result = commandLoader.initializeCommands()

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        val success = result as CommandLoader.LoadResult.Success
        assertEquals(3, success.commandCount) // 2 English + 1 Spanish
        assertTrue(success.locales.contains("en-US"))
        assertTrue(success.locales.contains("es-ES"))
    }

    @Test
    fun `Falls back to English when user locale unavailable`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(englishCommandsJson.toByteArray())
        every { mockAssets.open("localization/commands/xx-XX.json") } throws FileNotFoundException()

        coEvery { mockCommandDao.getDatabaseStats() } returns listOf(
            mockk {
                every { locale } returns "en-US"
                every { count } returns 2
            }
        )

        // Set system locale to unsupported locale
        java.util.Locale.setDefault(java.util.Locale("xx", "XX"))

        // Act
        val result = commandLoader.initializeCommands()

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        val success = result as CommandLoader.LoadResult.Success
        assertEquals(2, success.commandCount) // Only English
        assertEquals(listOf("en-US"), success.locales)
    }

    // ===== Command Count Tests =====

    @Test
    fun `command count matches loaded JSON`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(englishCommandsJson.toByteArray())

        // Act
        val result = commandLoader.loadLocale("en-US", isFallback = true)

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        val success = result as CommandLoader.LoadResult.Success
        assertEquals(2, success.commandCount) // 2 commands in englishCommandsJson
    }

    @Test
    fun `Spanish locale has correct command count`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/es-ES.json") } returns ByteArrayInputStream(spanishCommandsJson.toByteArray())

        // Act
        val result = commandLoader.loadLocale("es-ES", isFallback = false)

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        val success = result as CommandLoader.LoadResult.Success
        assertEquals(1, success.commandCount) // 1 command in spanishCommandsJson
    }

    // ===== Database Persistence Tests =====

    @Test
    fun `commands are persisted to database`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(englishCommandsJson.toByteArray())

        val capturedCommands = slot<List<VoiceCommandEntity>>()
        coEvery { mockCommandDao.insertBatch(capture(capturedCommands)) } returns listOf(1L, 2L)

        // Act
        val result = commandLoader.loadLocale("en-US", isFallback = true)

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        coVerify { mockCommandDao.insertBatch(any()) }

        // Verify captured commands
        assertTrue(capturedCommands.isCaptured)
        val commands = capturedCommands.captured
        assertEquals(2, commands.size)
        assertEquals("nav_back", commands[0].id)
        assertEquals("go back", commands[0].primaryText)
    }

    @Test
    fun `database version is updated after successful load`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(englishCommandsJson.toByteArray())

        coEvery { mockCommandDao.getDatabaseStats() } returns listOf(
            mockk {
                every { locale } returns "en-US"
                every { count } returns 2
            }
        )

        val capturedVersion = slot<DatabaseVersionEntity>()
        coEvery { mockVersionDao.setVersion(capture(capturedVersion)) } just Runs

        // Act
        val result = commandLoader.initializeCommands()

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        coVerify { mockVersionDao.setVersion(any()) }

        // Verify version entity
        assertTrue(capturedVersion.isCaptured)
        val version = capturedVersion.captured
        assertEquals("1.0", version.jsonVersion)
        assertEquals(2, version.commandCount)
    }

    // ===== Available Locales Tests =====

    @Test
    fun `getAvailableLocales returns locales from assets`() {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.list("localization/commands") } returns arrayOf(
            "en-US.json",
            "es-ES.json",
            "fr-FR.json",
            "de-DE.json"
        )

        // Act
        val result = commandLoader.getAvailableLocales()

        // Assert
        assertEquals(4, result.size)
        assertTrue(result.contains("en-US"))
        assertTrue(result.contains("es-ES"))
        assertTrue(result.contains("fr-FR"))
        assertTrue(result.contains("de-DE"))
    }

    @Test
    fun `getAvailableLocales filters out non-JSON files`() {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.list("localization/commands") } returns arrayOf(
            "en-US.json",
            "README.md",
            "es-ES.json",
            "config.txt"
        )

        // Act
        val result = commandLoader.getAvailableLocales()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.contains("en-US"))
        assertTrue(result.contains("es-ES"))
        assertFalse(result.contains("README"))
        assertFalse(result.contains("config"))
    }

    @Test
    fun `getAvailableLocales returns empty list when no JSON files`() {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.list("localization/commands") } returns arrayOf()

        // Act
        val result = commandLoader.getAvailableLocales()

        // Assert
        assertTrue(result.isEmpty())
    }

    // ===== Reload Tests =====

    @Test
    fun `reloadAll clears and reloads commands`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(englishCommandsJson.toByteArray())

        coEvery { mockCommandDao.getDatabaseStats() } returns listOf(
            mockk {
                every { locale } returns "en-US"
                every { count } returns 2
            }
        )

        // Act
        val result = commandLoader.reloadAll()

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        coVerify { mockCommandDao.deleteAllCommands() }
        coVerify { mockCommandDao.insertBatch(any()) }
    }

    @Test
    fun `forceReload clears version tracking`() = runTest {
        // Arrange
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(englishCommandsJson.toByteArray())

        coEvery { mockCommandDao.getDatabaseStats() } returns listOf(
            mockk {
                every { locale } returns "en-US"
                every { count } returns 2
            }
        )

        // Act
        val result = commandLoader.forceReload()

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Success)
        coVerify { mockVersionDao.clearVersion() }
        coVerify { mockCommandDao.deleteAllCommands() }
        coVerify { mockCommandDao.insertBatch(any()) }
    }

    // ===== Error Handling Tests =====

    @Test
    fun `initialization fails gracefully on database error`() = runTest {
        // Arrange
        coEvery { mockCommandDao.getDatabaseStats() } throws RuntimeException("Database error")

        // Act
        val result = commandLoader.initializeCommands()

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Error)
        val error = result as CommandLoader.LoadResult.Error
        assertTrue(error.message.contains("Initialization failed"))
    }

    @Test
    fun `loadLocale fails gracefully on parse error`() = runTest {
        // Arrange
        val malformedJson = "{ malformed }"
        val mockAssets = mockContext.assets
        every { mockAssets.open("localization/commands/en-US.json") } returns ByteArrayInputStream(malformedJson.toByteArray())

        // Act
        val result = commandLoader.loadLocale("en-US", isFallback = true)

        // Assert
        assertTrue(result is CommandLoader.LoadResult.Error)
    }
}
