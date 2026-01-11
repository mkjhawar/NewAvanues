package com.augmentalis.llm.provider

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.ava.core.common.Result
import com.augmentalis.llm.Language
import com.augmentalis.llm.ScreenContext
import com.augmentalis.llm.UserContext
import com.augmentalis.llm.ExpertiseLevel
import com.augmentalis.llm.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Basic integration tests for LocalLLMProvider
 *
 * Tests provider initialization, configuration, and basic functionality
 * without requiring fully initialized LLM models.
 *
 * Created: 2025-11-15
 * Part of: P7 LLM Integration Phase 2 - Milestone 2
 */
@RunWith(AndroidJUnit4::class)
class LocalLLMProviderBasicTest {

    private lateinit var context: Context
    private lateinit var provider: LocalLLMProvider

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        provider = LocalLLMProvider(context = context, autoModelSelection = true)
    }

    @After
    fun tearDown() {
        runBlocking {
            provider.cleanup()
        }
    }

    @Test
    fun testProviderCreation() {
        assertNotNull("Provider should be created", provider)
    }

    @Test
    fun testGetProviderInfo() {
        val info = provider.getInfo()

        assertEquals("ALC Engine", info.name)
        assertEquals("1.0", info.version)
        assertTrue(info.isLocal)
        assertTrue(info.capabilities.supportsStreaming)
        assertTrue(info.capabilities.supportsChat)
        assertFalse(info.capabilities.supportsFunctionCalling)
        assertEquals(2048, info.capabilities.maxContextLength)
    }

    @Test
    fun testEstimateCostIsZero() {
        val cost = provider.estimateCost(inputTokens = 100, outputTokens = 50)
        assertEquals(0.0, cost, 0.001)
    }

    @Test
    fun testIsGeneratingBeforeInit() {
        assertFalse(provider.isGenerating())
    }

    @Test
    fun testHealthCheckBeforeInit() = runBlocking {
        val result = provider.checkHealth()

        assertTrue(result is Result.Success)
        val health = (result as Result.Success).data
        assertEquals(HealthStatus.UNHEALTHY, health.status)
    }

    @Test
    fun testDetectLanguageEnglish() {
        val (lang, conf) = provider.detectLanguage("Hello world")

        assertEquals(Language.ENGLISH, lang)
        assertTrue(conf > 0.7f)
    }

    @Test
    fun testDetectLanguageSpanish() {
        val (lang, conf) = provider.detectLanguage("Hola mundo")

        assertEquals(Language.SPANISH, lang)
        assertTrue(conf > 0.5f)
    }

    @Test
    fun testGetRecommendedModel() {
        val modelId = provider.getRecommendedModel("Hello world")

        assertNotNull(modelId)
        assertTrue(modelId.isNotEmpty())
    }

    @Test
    fun testGetAvailableModels() {
        val models = provider.getAvailableModels()

        assertNotNull(models)
        assertTrue(models.isNotEmpty())
    }

    @Test
    fun testBuildSystemPrompt() {
        val prompt = provider.buildSystemPrompt()

        assertNotNull(prompt)
        assertTrue(prompt.isNotEmpty())
        assertTrue(prompt.contains("AVA", ignoreCase = true))
    }

    @Test
    fun testSetScreenContext() {
        provider.setScreenContext(ScreenContext.ChatScreen)

        val prompt = provider.buildSystemPrompt()
        assertNotNull(prompt)
    }

    @Test
    fun testSetUserContext() {
        val userContext = UserContext(
            name = "Test User",
            language = Language.ENGLISH,
            expertiseLevel = ExpertiseLevel.BEGINNER
        )

        provider.setUserContext(userContext)

        val prompt = provider.buildSystemPrompt()
        assertTrue(prompt.contains("Test User", ignoreCase = true))
    }

    @Test
    fun testFormatWithSystemPrompt() {
        val formatted = provider.formatWithSystemPrompt("Hello")

        assertNotNull(formatted)
        assertTrue(formatted.contains("Hello"))
        assertTrue(formatted.length > "Hello".length)
    }

    @Test
    fun testInitWithInvalidPath() = runBlocking {
        val config = LLMConfig(
            modelPath = "/invalid/path",
            modelLib = "test",
            device = "opencl"
        )

        val result = provider.initialize(config)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message?.contains("not found", ignoreCase = true) == true)
    }

    @Test
    fun testCleanup() = runBlocking {
        provider.cleanup()

        val health = provider.checkHealth()
        assertEquals(HealthStatus.UNHEALTHY, (health as Result.Success).data.status)
    }
}
