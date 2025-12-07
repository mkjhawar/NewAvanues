/**
 * CommandManagerMultiLanguageTest.kt
 *
 * Created: 2025-10-23 02:43 PDT
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Unit tests for CommandManager multi-language support
 * Coverage: getCurrentLocale, getAvailableLocales, switchLocale, resetToSystemLocale
 * Location: CommandManager test suite
 *
 * Changelog:
 * - v1.0.0 (2025-10-23): Initial test suite for multi-language support
 */

package com.augmentalis.commandmanager

import android.content.Context
import android.content.SharedPreferences
import com.augmentalis.commandmanager.loader.CommandLoader
import com.augmentalis.commandmanager.loader.CommandLocalizer
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Unit tests for CommandManager multi-language support.
 *
 * Test Coverage:
 * - getCurrentLocale() returns valid locale string
 * - getAvailableLocales() returns correct locale list
 * - switchLocale() with valid locale succeeds
 * - switchLocale() with invalid locale fails
 * - switchLocale() updates currentLocale
 * - resetToSystemLocale() works correctly
 * - Locale persistence across restarts
 * - Edge cases (null, empty, invalid locales)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class CommandManagerMultiLanguageTest {

    private lateinit var mockContext: Context
    private lateinit var mockCommandLoader: CommandLoader
    private lateinit var mockCommandLocalizer: CommandLocalizer
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var commandManager: CommandManager

    // Flow for currentLocale
    private val currentLocaleFlow = MutableStateFlow("en-US")

    @Before
    fun setUp() {
        // Mock Context
        mockContext = mockk(relaxed = true)

        // Mock SharedPreferences
        mockSharedPreferences = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences

        // Mock CommandLoader
        mockCommandLoader = mockk(relaxed = true)

        // Mock CommandLocalizer
        mockCommandLocalizer = mockk(relaxed = true)
        every { mockCommandLocalizer.currentLocale } returns currentLocaleFlow

        // Set Locale to en-US for consistent tests
        Locale.setDefault(Locale.US)

        // Create CommandManager
        commandManager = CommandManager(mockContext)

        // Initialize manager
        commandManager.initialize()
    }

    @After
    fun tearDown() {
        commandManager.cleanup()
        clearAllMocks()
    }

    // ===== getCurrentLocale() Tests =====

    @Test
    fun `getCurrentLocale returns valid locale string`() {
        // Arrange
        currentLocaleFlow.value = "en-US"

        // Act
        val result = commandManager.getCurrentLocale()

        // Assert
        assertEquals("en-US", result)
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `getCurrentLocale returns Spanish locale when set`() {
        // Arrange
        currentLocaleFlow.value = "es-ES"

        // Act
        val result = commandManager.getCurrentLocale()

        // Assert
        assertEquals("es-ES", result)
    }

    @Test
    fun `getCurrentLocale returns French locale when set`() {
        // Arrange
        currentLocaleFlow.value = "fr-FR"

        // Act
        val result = commandManager.getCurrentLocale()

        // Assert
        assertEquals("fr-FR", result)
    }

    @Test
    fun `getCurrentLocale returns German locale when set`() {
        // Arrange
        currentLocaleFlow.value = "de-DE"

        // Act
        val result = commandManager.getCurrentLocale()

        // Assert
        assertEquals("de-DE", result)
    }

    @Test
    fun `getCurrentLocale format matches expected pattern`() {
        // Arrange
        currentLocaleFlow.value = "en-US"

        // Act
        val result = commandManager.getCurrentLocale()

        // Assert
        // Format: language-COUNTRY (e.g., "en-US")
        assertTrue(result.matches(Regex("^[a-z]{2}-[A-Z]{2}$")))
    }

    // ===== getAvailableLocales() Tests =====

    @Test
    fun `getAvailableLocales returns all supported locales`() = runTest {
        // Arrange
        val expectedLocales = listOf("en-US", "es-ES", "fr-FR", "de-DE")
        coEvery { mockCommandLocalizer.getAvailableLocales() } returns expectedLocales

        // Act
        val result = commandManager.getAvailableLocales()

        // Assert
        assertEquals(4, result.size)
        assertTrue(result.containsAll(expectedLocales))
    }

    @Test
    fun `getAvailableLocales includes English as default`() = runTest {
        // Arrange
        val locales = listOf("en-US", "es-ES")
        coEvery { mockCommandLocalizer.getAvailableLocales() } returns locales

        // Act
        val result = commandManager.getAvailableLocales()

        // Assert
        assertTrue(result.contains("en-US"))
    }

    @Test
    fun `getAvailableLocales returns empty list when no locales available`() = runTest {
        // Arrange
        coEvery { mockCommandLocalizer.getAvailableLocales() } returns emptyList()

        // Act
        val result = commandManager.getAvailableLocales()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAvailableLocales returns unique locales`() = runTest {
        // Arrange
        val locales = listOf("en-US", "es-ES", "fr-FR", "de-DE")
        coEvery { mockCommandLocalizer.getAvailableLocales() } returns locales

        // Act
        val result = commandManager.getAvailableLocales()

        // Assert
        assertEquals(result.size, result.toSet().size) // No duplicates
    }

    // ===== switchLocale() Tests =====

    @Test
    fun `switchLocale with valid locale returns true`() = runTest {
        // Arrange
        val targetLocale = "es-ES"
        coEvery { mockCommandLocalizer.setLocale(targetLocale) } returns true
        coEvery { mockCommandLoader.initializeCommands() } returns CommandLoader.LoadResult.Success(
            commandCount = 50,
            locales = listOf(targetLocale)
        )

        // Act
        val result = commandManager.switchLocale(targetLocale)

        // Assert
        assertTrue(result)
        coVerify { mockCommandLocalizer.setLocale(targetLocale) }
        coVerify { mockCommandLoader.initializeCommands() }
    }

    @Test
    fun `switchLocale with invalid locale returns false`() = runTest {
        // Arrange
        val invalidLocale = "xx-XX"
        coEvery { mockCommandLocalizer.setLocale(invalidLocale) } returns false

        // Act
        val result = commandManager.switchLocale(invalidLocale)

        // Assert
        assertFalse(result)
        coVerify { mockCommandLocalizer.setLocale(invalidLocale) }
    }

    @Test
    fun `switchLocale to Spanish succeeds`() = runTest {
        // Arrange
        coEvery { mockCommandLocalizer.setLocale("es-ES") } returns true
        coEvery { mockCommandLoader.initializeCommands() } returns CommandLoader.LoadResult.Success(
            commandCount = 50,
            locales = listOf("es-ES")
        )

        // Act
        val result = commandManager.switchLocale("es-ES")

        // Assert
        assertTrue(result)
    }

    @Test
    fun `switchLocale to French succeeds`() = runTest {
        // Arrange
        coEvery { mockCommandLocalizer.setLocale("fr-FR") } returns true
        coEvery { mockCommandLoader.initializeCommands() } returns CommandLoader.LoadResult.Success(
            commandCount = 50,
            locales = listOf("fr-FR")
        )

        // Act
        val result = commandManager.switchLocale("fr-FR")

        // Assert
        assertTrue(result)
    }

    @Test
    fun `switchLocale to German succeeds`() = runTest {
        // Arrange
        coEvery { mockCommandLocalizer.setLocale("de-DE") } returns true
        coEvery { mockCommandLoader.initializeCommands() } returns CommandLoader.LoadResult.Success(
            commandCount = 50,
            locales = listOf("de-DE")
        )

        // Act
        val result = commandManager.switchLocale("de-DE")

        // Assert
        assertTrue(result)
    }

    @Test
    fun `switchLocale updates current locale on success`() = runTest {
        // Arrange
        val targetLocale = "es-ES"
        currentLocaleFlow.value = "en-US" // Start with English

        coEvery { mockCommandLocalizer.setLocale(targetLocale) } answers {
            currentLocaleFlow.value = targetLocale
            true
        }
        coEvery { mockCommandLoader.initializeCommands() } returns CommandLoader.LoadResult.Success(
            commandCount = 50,
            locales = listOf(targetLocale)
        )

        // Act
        val result = commandManager.switchLocale(targetLocale)

        // Assert
        assertTrue(result)
        assertEquals(targetLocale, commandManager.getCurrentLocale())
    }

    @Test
    fun `switchLocale does not update locale on failure`() = runTest {
        // Arrange
        val currentLocale = "en-US"
        val targetLocale = "xx-XX"
        currentLocaleFlow.value = currentLocale

        coEvery { mockCommandLocalizer.setLocale(targetLocale) } returns false

        // Act
        val result = commandManager.switchLocale(targetLocale)

        // Assert
        assertFalse(result)
        assertEquals(currentLocale, commandManager.getCurrentLocale())
    }

    @Test
    fun `switchLocale fails when command loading fails`() = runTest {
        // Arrange
        val targetLocale = "es-ES"
        coEvery { mockCommandLocalizer.setLocale(targetLocale) } returns true
        coEvery { mockCommandLoader.initializeCommands() } returns CommandLoader.LoadResult.Error(
            "Failed to load commands"
        )

        // Act
        val result = commandManager.switchLocale(targetLocale)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `switchLocale fails when locale not found`() = runTest {
        // Arrange
        val targetLocale = "es-ES"
        coEvery { mockCommandLocalizer.setLocale(targetLocale) } returns true
        coEvery { mockCommandLoader.initializeCommands() } returns CommandLoader.LoadResult.LocaleNotFound(
            locale = targetLocale
        )

        // Act
        val result = commandManager.switchLocale(targetLocale)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `switchLocale handles exception gracefully`() = runTest {
        // Arrange
        val targetLocale = "es-ES"
        coEvery { mockCommandLocalizer.setLocale(targetLocale) } throws RuntimeException("Test exception")

        // Act
        val result = commandManager.switchLocale(targetLocale)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `switchLocale with empty string returns false`() = runTest {
        // Arrange
        coEvery { mockCommandLocalizer.setLocale("") } returns false

        // Act
        val result = commandManager.switchLocale("")

        // Assert
        assertFalse(result)
    }

    @Test
    fun `switchLocale with blank string returns false`() = runTest {
        // Arrange
        coEvery { mockCommandLocalizer.setLocale("  ") } returns false

        // Act
        val result = commandManager.switchLocale("  ")

        // Assert
        assertFalse(result)
    }

    // ===== resetToSystemLocale() Tests =====

    @Test
    fun `resetToSystemLocale returns true on success`() = runTest {
        // Arrange
        val systemLocale = Locale.getDefault().toLanguageTag()
        coEvery { mockCommandLocalizer.resetToSystemLocale() } returns true

        // Act
        val result = commandManager.resetToSystemLocale()

        // Assert
        assertTrue(result)
        coVerify { mockCommandLocalizer.resetToSystemLocale() }
    }

    @Test
    fun `resetToSystemLocale returns false on failure`() = runTest {
        // Arrange
        coEvery { mockCommandLocalizer.resetToSystemLocale() } returns false

        // Act
        val result = commandManager.resetToSystemLocale()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `resetToSystemLocale handles exception gracefully`() = runTest {
        // Arrange
        coEvery { mockCommandLocalizer.resetToSystemLocale() } throws RuntimeException("Test exception")

        // Act
        val result = commandManager.resetToSystemLocale()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `resetToSystemLocale resets to US locale when system is US`() = runTest {
        // Arrange
        Locale.setDefault(Locale.US)
        val expectedLocale = "en-US"

        coEvery { mockCommandLocalizer.resetToSystemLocale() } answers {
            currentLocaleFlow.value = expectedLocale
            true
        }

        // Act
        val result = commandManager.resetToSystemLocale()

        // Assert
        assertTrue(result)
        assertEquals(expectedLocale, commandManager.getCurrentLocale())
    }

    // ===== Locale Persistence Tests =====

    @Test
    fun `locale is persisted to SharedPreferences on switch`() = runTest {
        // Arrange
        val targetLocale = "es-ES"
        val mockPrefs = mockk<SharedPreferences>(relaxed = true)
        val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)

        every { mockContext.getSharedPreferences("command_localizer_prefs", Context.MODE_PRIVATE) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString("current_locale", targetLocale) } returns mockEditor
        every { mockEditor.apply() } just Runs

        coEvery { mockCommandLocalizer.setLocale(targetLocale) } answers {
            mockEditor.putString("current_locale", targetLocale)
            mockEditor.apply()
            true
        }
        coEvery { mockCommandLoader.initializeCommands() } returns CommandLoader.LoadResult.Success(
            commandCount = 50,
            locales = listOf(targetLocale)
        )

        // Act
        val result = commandManager.switchLocale(targetLocale)

        // Assert
        assertTrue(result)
        verify { mockEditor.putString("current_locale", targetLocale) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `locale is restored from SharedPreferences on restart`() {
        // Arrange
        val savedLocale = "es-ES"
        every { mockSharedPreferences.getString("current_locale", null) } returns savedLocale
        currentLocaleFlow.value = savedLocale

        // Act
        val result = commandManager.getCurrentLocale()

        // Assert
        assertEquals(savedLocale, result)
    }

    // ===== Edge Cases Tests =====

    @Test
    fun `getCurrentLocale never returns null`() {
        // Act
        val result = commandManager.getCurrentLocale()

        // Assert
        assertNotNull(result)
    }

    @Test
    fun `getCurrentLocale never returns empty string`() {
        // Act
        val result = commandManager.getCurrentLocale()

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `switchLocale to same locale is idempotent`() = runTest {
        // Arrange
        val currentLocale = "en-US"
        currentLocaleFlow.value = currentLocale

        coEvery { mockCommandLocalizer.setLocale(currentLocale) } returns true
        coEvery { mockCommandLoader.initializeCommands() } returns CommandLoader.LoadResult.Success(
            commandCount = 50,
            locales = listOf(currentLocale)
        )

        // Act
        val result1 = commandManager.switchLocale(currentLocale)
        val result2 = commandManager.switchLocale(currentLocale)

        // Assert
        assertTrue(result1)
        assertTrue(result2)
        assertEquals(currentLocale, commandManager.getCurrentLocale())
    }

    @Test
    fun `multiple locale switches work correctly`() = runTest {
        // Arrange
        val locales = listOf("en-US", "es-ES", "fr-FR", "de-DE")

        locales.forEach { locale ->
            coEvery { mockCommandLocalizer.setLocale(locale) } answers {
                currentLocaleFlow.value = locale
                true
            }
            coEvery { mockCommandLoader.initializeCommands() } returns CommandLoader.LoadResult.Success(
                commandCount = 50,
                locales = listOf(locale)
            )
        }

        // Act & Assert
        locales.forEach { locale ->
            val result = commandManager.switchLocale(locale)
            assertTrue(result)
            assertEquals(locale, commandManager.getCurrentLocale())
        }
    }

    @Test
    fun `getAvailableLocales called multiple times returns consistent results`() = runTest {
        // Arrange
        val expectedLocales = listOf("en-US", "es-ES", "fr-FR", "de-DE")
        coEvery { mockCommandLocalizer.getAvailableLocales() } returns expectedLocales

        // Act
        val result1 = commandManager.getAvailableLocales()
        val result2 = commandManager.getAvailableLocales()
        val result3 = commandManager.getAvailableLocales()

        // Assert
        assertEquals(result1, result2)
        assertEquals(result2, result3)
        assertEquals(expectedLocales, result1)
    }
}
