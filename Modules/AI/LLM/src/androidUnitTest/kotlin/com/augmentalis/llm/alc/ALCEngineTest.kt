package com.augmentalis.llm.alc

import android.content.Context
import com.augmentalis.llm.LLMResult
import com.augmentalis.llm.alc.interfaces.*
import com.augmentalis.llm.alc.language.LanguagePackManager
import com.augmentalis.llm.domain.ChatMessage
import com.augmentalis.llm.domain.MessageRole
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Unit tests for ALCEngine
 *
 * Tests core functionality:
 * - Initialization and language management
 * - Error handling
 * - Resource querying
 *
 * Note: Full integration tests for chat and language switching
 * require instrumented tests with actual models.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ALCEngineTest {

    private lateinit var context: Context
    private lateinit var languagePackManager: LanguagePackManager
    private lateinit var inferenceStrategy: IInferenceStrategy
    private lateinit var streamingManager: IStreamingManager
    private lateinit var memoryManager: IMemoryManager
    private lateinit var samplerStrategy: ISamplerStrategy
    private lateinit var engine: ALCEngine

    @Before
    fun setup() {
        // Mock dependencies
        context = mockk(relaxed = true)
        languagePackManager = mockk()
        inferenceStrategy = mockk()
        streamingManager = mockk()
        memoryManager = mockk(relaxed = true)
        samplerStrategy = mockk()

        // Mock context.filesDir
        val mockFilesDir = mockk<File>()
        every { context.filesDir } returns mockFilesDir
        every { mockFilesDir.toString() } returns "/mock/files"

        // Create engine
        engine = ALCEngine(
            context = context,
            languagePackManager = languagePackManager,
            inferenceStrategy = inferenceStrategy,
            streamingManager = streamingManager,
            memoryManager = memoryManager,
            samplerStrategy = samplerStrategy,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test getCurrentLanguage returns initial language`() {
        // When
        val language = engine.getCurrentLanguage()

        // Then
        assertNotNull("Should return a language", language)
        assertTrue("Should be a valid language code", language.length == 2)
    }

    @Test
    fun `test initialize fails when language pack not installed`() = runTest {
        // Given
        every { languagePackManager.isLanguageInstalled(any()) } returns false

        // When
        val result = engine.initialize()

        // Then
        assertTrue("Should fail when language pack missing", result is LLMResult.Error)
    }

    @Test
    fun `test switchLanguage fails when language not installed`() = runTest {
        // Given
        every { languagePackManager.isLanguageInstalled("es") } returns false

        // When
        val result = engine.switchLanguage("es")

        // Then
        assertTrue("Should fail when language not installed",
            result is LLMResult.Error)
    }

    @Test
    fun `test getInstalledLanguages returns list from manager`() {
        // Given
        val languages = listOf("en", "es", "fr", "de")
        every { languagePackManager.getInstalledLanguages() } returns languages

        // When
        val result = engine.getInstalledLanguages()

        // Then
        assertEquals("Should return same list", languages, result)
    }

    @Test
    fun `test isLanguageInstalled delegates to manager`() {
        // Given
        every { languagePackManager.isLanguageInstalled("ja") } returns true
        every { languagePackManager.isLanguageInstalled("ru") } returns false

        // When & Then
        assertTrue("Should return true for installed", engine.isLanguageInstalled("ja"))
        assertFalse("Should return false for not installed", engine.isLanguageInstalled("ru"))
    }

    @Test
    fun `test chat returns error when engine not initialized`() = runTest {
        // Given - Engine not initialized
        val messages = listOf(ChatMessage(role = MessageRole.USER, content = "Hello"))

        // When
        val responses = engine.chat(messages).toList()

        // Then
        assertTrue("Should emit error responses", responses.isNotEmpty())
    }

    @Test
    fun `test getStats returns null when engine not initialized`() {
        // When
        val stats = engine.getStats()

        // Then
        assertNull("Should return null when not initialized", stats)
    }

    @Test
    fun `test getMemoryInfo returns null when engine not initialized`() {
        // When
        val memInfo = engine.getMemoryInfo()

        // Then
        assertNull("Should return null when not initialized", memInfo)
    }

    @Test
    fun `test isGenerating returns false when engine not initialized`() {
        // When
        val isGenerating = engine.isGenerating()

        // Then
        assertFalse("Should return false when not initialized", isGenerating)
    }

    @Test
    fun `test engine basic functionality`() {
        // Test basic methods don't throw exceptions
        assertFalse("isGenerating should be false initially", engine.isGenerating())
        assertNull("getStats should be null initially", engine.getStats())
        assertNull("getMemoryInfo should be null initially", engine.getMemoryInfo())
        assertNotNull("getCurrentLanguage should not be null", engine.getCurrentLanguage())
    }

}
