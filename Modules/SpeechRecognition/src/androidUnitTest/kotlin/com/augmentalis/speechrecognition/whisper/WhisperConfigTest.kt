/**
 * WhisperConfigTest.kt - Tests for Whisper engine configuration
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 *
 * Tests validation, thread count calculation, model path resolution,
 * directory creation, and auto-tuning via mocked ActivityManager.
 *
 * Non-Context tests run as plain JUnit4.
 * Context-dependent tests run under Robolectric (RobolectricTestRunner).
 */
package com.augmentalis.speechrecognition.whisper

import android.app.ActivityManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WhisperConfigTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    // ── Default Values ───────────────────────────────────────────

    @Test
    fun `defaults have sensible values`() {
        val config = WhisperConfig()

        assertEquals(WhisperModelSize.BASE, config.modelSize)
        assertEquals("en", config.language)
        assertFalse(config.translateToEnglish)
        assertEquals(0, config.numThreads)
        assertNull(config.customModelPath)
        assertEquals(0.6f, config.vadSensitivity)
        assertEquals(700L, config.silenceThresholdMs)
        assertEquals(300L, config.minSpeechDurationMs)
        assertEquals(30_000L, config.maxChunkDurationMs)
        assertFalse(config.useQuantized)
    }

    // ── effectiveThreadCount ─────────────────────────────────────

    @Test
    fun `effectiveThreadCount returns explicit value when set to 1`() {
        val config = WhisperConfig(numThreads = 1)
        assertEquals(1, config.effectiveThreadCount())
    }

    @Test
    fun `effectiveThreadCount returns explicit value when set to 3`() {
        val config = WhisperConfig(numThreads = 3)
        assertEquals(3, config.effectiveThreadCount())
    }

    @Test
    fun `effectiveThreadCount returns explicit value when set to 8`() {
        val config = WhisperConfig(numThreads = 8)
        assertEquals(8, config.effectiveThreadCount())
    }

    @Test
    fun `effectiveThreadCount auto-detects and clamps to 2 at minimum when zero`() {
        val config = WhisperConfig(numThreads = 0)
        val threads = config.effectiveThreadCount()
        // Implementation: availableProcessors().coerceIn(2, 4)
        assertTrue(threads >= 2, "Auto-detected thread count must be >= 2, was $threads")
        assertTrue(threads <= 4, "Auto-detected thread count must be <= 4, was $threads")
    }

    @Test
    fun `effectiveThreadCount auto-detected value matches coerced processor count`() {
        val config = WhisperConfig(numThreads = 0)
        val expected = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        assertEquals(expected, config.effectiveThreadCount())
    }

    @Test
    fun `effectiveThreadCount is positive for any auto or explicit setting`() {
        listOf(0, 1, 2, 4, 6).forEach { n ->
            val threads = WhisperConfig(numThreads = n).effectiveThreadCount()
            assertTrue(threads > 0, "Thread count should always be positive, got $threads for numThreads=$n")
        }
    }

    // ── validate() — Success Cases ───────────────────────────────

    @Test
    fun `validate passes for default config`() {
        assertTrue(WhisperConfig().validate().isSuccess)
    }

    @Test
    fun `validate passes for vadSensitivity at lower boundary 0 0`() {
        assertTrue(WhisperConfig(vadSensitivity = 0.0f).validate().isSuccess)
    }

    @Test
    fun `validate passes for vadSensitivity at upper boundary 1 0`() {
        assertTrue(WhisperConfig(vadSensitivity = 1.0f).validate().isSuccess)
    }

    @Test
    fun `validate passes for silenceThresholdMs at lower boundary 100`() {
        assertTrue(WhisperConfig(silenceThresholdMs = 100).validate().isSuccess)
    }

    @Test
    fun `validate passes for silenceThresholdMs at upper boundary 5000`() {
        assertTrue(WhisperConfig(silenceThresholdMs = 5000).validate().isSuccess)
    }

    @Test
    fun `validate passes for minSpeechDurationMs at lower boundary 50`() {
        assertTrue(WhisperConfig(minSpeechDurationMs = 50).validate().isSuccess)
    }

    @Test
    fun `validate passes for minSpeechDurationMs at upper boundary 5000`() {
        assertTrue(WhisperConfig(minSpeechDurationMs = 5000).validate().isSuccess)
    }

    @Test
    fun `validate passes for English-only model with language en`() {
        WhisperModelSize.entries.filter { it.isEnglishOnly }.forEach { size ->
            val config = WhisperConfig(modelSize = size, language = "en")
            assertTrue(config.validate().isSuccess, "${size.name} with 'en' should pass")
        }
    }

    @Test
    fun `validate passes for English-only model with language auto`() {
        WhisperModelSize.entries.filter { it.isEnglishOnly }.forEach { size ->
            val config = WhisperConfig(modelSize = size, language = "auto")
            assertTrue(config.validate().isSuccess, "${size.name} with 'auto' should pass")
        }
    }

    @Test
    fun `validate passes for multilingual model with any language`() {
        val languages = listOf("en", "es", "fr", "de", "hi", "zh", "auto")
        WhisperModelSize.entries.filter { !it.isEnglishOnly }.forEach { size ->
            languages.forEach { lang ->
                val config = WhisperConfig(modelSize = size, language = lang)
                assertTrue(
                    config.validate().isSuccess,
                    "${size.name} with '$lang' should pass validation"
                )
            }
        }
    }

    // ── validate() — Failure Cases ───────────────────────────────

    @Test
    fun `validate fails for vadSensitivity just below 0`() {
        val result = WhisperConfig(vadSensitivity = -0.001f).validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `validate fails for vadSensitivity just above 1`() {
        val result = WhisperConfig(vadSensitivity = 1.001f).validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `validate fails for vadSensitivity at minus 0 1`() {
        assertTrue(WhisperConfig(vadSensitivity = -0.1f).validate().isFailure)
    }

    @Test
    fun `validate fails for vadSensitivity at 1 1`() {
        assertTrue(WhisperConfig(vadSensitivity = 1.1f).validate().isFailure)
    }

    @Test
    fun `validate fails for silenceThresholdMs at 99`() {
        assertTrue(WhisperConfig(silenceThresholdMs = 99).validate().isFailure)
    }

    @Test
    fun `validate fails for silenceThresholdMs at 50`() {
        assertTrue(WhisperConfig(silenceThresholdMs = 50).validate().isFailure)
    }

    @Test
    fun `validate fails for silenceThresholdMs at 5001`() {
        assertTrue(WhisperConfig(silenceThresholdMs = 5001).validate().isFailure)
    }

    @Test
    fun `validate fails for silenceThresholdMs at 6000`() {
        assertTrue(WhisperConfig(silenceThresholdMs = 6000).validate().isFailure)
    }

    @Test
    fun `validate fails for minSpeechDurationMs at 49`() {
        assertTrue(WhisperConfig(minSpeechDurationMs = 49).validate().isFailure)
    }

    @Test
    fun `validate fails for minSpeechDurationMs at 30`() {
        assertTrue(WhisperConfig(minSpeechDurationMs = 30).validate().isFailure)
    }

    @Test
    fun `validate fails for minSpeechDurationMs at 5001`() {
        assertTrue(WhisperConfig(minSpeechDurationMs = 5001).validate().isFailure)
    }

    @Test
    fun `validate fails for minSpeechDurationMs at 6000`() {
        assertTrue(WhisperConfig(minSpeechDurationMs = 6000).validate().isFailure)
    }

    @Test
    fun `validate fails for TINY_EN with non-English language`() {
        val result = WhisperConfig(modelSize = WhisperModelSize.TINY_EN, language = "es").validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("English-only") == true)
    }

    @Test
    fun `validate fails for BASE_EN with French language`() {
        val result = WhisperConfig(modelSize = WhisperModelSize.BASE_EN, language = "fr").validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("English-only") == true)
    }

    @Test
    fun `validate fails for SMALL_EN with German language`() {
        val result = WhisperConfig(modelSize = WhisperModelSize.SMALL_EN, language = "de").validate()
        assertTrue(result.isFailure)
    }

    @Test
    fun `validate fails for MEDIUM_EN with Hindi language`() {
        val result = WhisperConfig(modelSize = WhisperModelSize.MEDIUM_EN, language = "hi").validate()
        assertTrue(result.isFailure)
    }

    @Test
    fun `validate failure message names the language when English-only model misused`() {
        val result = WhisperConfig(modelSize = WhisperModelSize.BASE_EN, language = "zh").validate()
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("zh"), "Error message should name the language, was: $message")
    }

    // ── resolveModelPath() ───────────────────────────────────────

    @Test
    fun `resolveModelPath returns customModelPath when that file exists`() {
        val customFile = tmpFolder.newFile("custom-model.bin")
        val config = WhisperConfig(
            modelSize = WhisperModelSize.BASE,
            customModelPath = customFile.absolutePath
        )
        val result = config.resolveModelPath(context)
        assertEquals(customFile.absolutePath, result)
    }

    @Test
    fun `resolveModelPath skips customModelPath when file does not exist`() {
        val nonExistentPath = tmpFolder.root.absolutePath + "/does-not-exist.bin"
        // No other model files exist in Robolectric context — expect null
        val config = WhisperConfig(
            modelSize = WhisperModelSize.BASE,
            customModelPath = nonExistentPath
        )
        // Will not find any path since internal/external dirs also won't have it
        val result = config.resolveModelPath(context)
        assertNull(result, "Non-existent customModelPath should not be returned")
    }

    @Test
    fun `resolveModelPath returns null when no model files exist anywhere`() {
        val config = WhisperConfig(modelSize = WhisperModelSize.BASE)
        val result = config.resolveModelPath(context)
        assertNull(result, "Should return null when no model file exists")
    }

    @Test
    fun `resolveModelPath returns internal filesDir path when file placed there`() {
        val modelSize = WhisperModelSize.TINY
        val internalDir = File(context.filesDir, "whisper/models")
        internalDir.mkdirs()
        val modelFile = File(internalDir, modelSize.ggmlFileName)
        modelFile.createNewFile()

        val config = WhisperConfig(modelSize = modelSize)
        val result = config.resolveModelPath(context)

        assertNotNull(result)
        assertEquals(modelFile.absolutePath, result)

        modelFile.delete()
    }

    @Test
    fun `resolveModelPath returns external filesDir path when file placed there and internal absent`() {
        val modelSize = WhisperModelSize.TINY_EN
        val externalBase = context.getExternalFilesDir(null)
        assertNotNull(externalBase, "Robolectric must provide external files dir")
        val externalDir = File(externalBase, "whisper/models")
        externalDir.mkdirs()
        val modelFile = File(externalDir, modelSize.ggmlFileName)
        modelFile.createNewFile()

        val config = WhisperConfig(modelSize = modelSize)
        val result = config.resolveModelPath(context)

        assertNotNull(result)
        assertEquals(modelFile.absolutePath, result)

        modelFile.delete()
    }

    @Test
    fun `resolveModelPath prefers customModelPath over internal storage`() {
        val customFile = tmpFolder.newFile("priority-model.bin")

        // Also create an internal model to ensure custom wins
        val modelSize = WhisperModelSize.BASE
        val internalDir = File(context.filesDir, "whisper/models")
        internalDir.mkdirs()
        val internalFile = File(internalDir, modelSize.ggmlFileName)
        internalFile.createNewFile()

        val config = WhisperConfig(
            modelSize = modelSize,
            customModelPath = customFile.absolutePath
        )
        val result = config.resolveModelPath(context)

        assertEquals(customFile.absolutePath, result, "customModelPath should take priority over internal")

        internalFile.delete()
    }

    @Test
    fun `resolveModelPath prefers internal over external storage`() {
        val modelSize = WhisperModelSize.SMALL

        // Create internal model
        val internalDir = File(context.filesDir, "whisper/models")
        internalDir.mkdirs()
        val internalFile = File(internalDir, modelSize.ggmlFileName)
        internalFile.createNewFile()

        // Create external model
        val externalBase = context.getExternalFilesDir(null)
        val externalDir = File(externalBase, "whisper/models")
        externalDir.mkdirs()
        val externalFile = File(externalDir, modelSize.ggmlFileName)
        externalFile.createNewFile()

        val config = WhisperConfig(modelSize = modelSize)
        val result = config.resolveModelPath(context)

        assertEquals(internalFile.absolutePath, result, "Internal storage should take priority over external")

        internalFile.delete()
        externalFile.delete()
    }

    @Test
    fun `resolveModelPath respects modelSize when no customModelPath`() {
        // Verify that the path returned for TINY uses TINY's ggmlFileName
        val modelSize = WhisperModelSize.TINY
        val internalDir = File(context.filesDir, "whisper/models")
        internalDir.mkdirs()
        val modelFile = File(internalDir, modelSize.ggmlFileName)
        modelFile.createNewFile()

        val config = WhisperConfig(modelSize = modelSize)
        val result = config.resolveModelPath(context)

        assertNotNull(result)
        assertTrue(result.endsWith(modelSize.ggmlFileName), "Path should end with the model's ggmlFileName")

        modelFile.delete()
    }

    // ── getModelDirectory() ──────────────────────────────────────

    @Test
    fun `getModelDirectory returns correct path under filesDir`() {
        val config = WhisperConfig()
        val dir = config.getModelDirectory(context)

        assertEquals(
            File(context.filesDir, "whisper/models").absolutePath,
            dir.absolutePath
        )
    }

    @Test
    fun `getModelDirectory creates directory when it does not exist`() {
        // Ensure the directory does not exist going in (Robolectric fresh app)
        val expectedDir = File(context.filesDir, "whisper/models")
        if (expectedDir.exists()) expectedDir.deleteRecursively()

        val config = WhisperConfig()
        val result = config.getModelDirectory(context)

        assertTrue(result.exists(), "getModelDirectory must create the directory")
        assertTrue(result.isDirectory, "getModelDirectory must return a directory, not a file")
    }

    @Test
    fun `getModelDirectory is idempotent when called twice`() {
        val config = WhisperConfig()
        val dir1 = config.getModelDirectory(context)
        val dir2 = config.getModelDirectory(context)

        assertEquals(dir1.absolutePath, dir2.absolutePath)
        assertTrue(dir2.exists())
    }

    @Test
    fun `getModelDirectory returns writable directory`() {
        val config = WhisperConfig()
        val dir = config.getModelDirectory(context)
        assertTrue(dir.canWrite(), "Model directory must be writable")
    }

    // ── autoTuned() ──────────────────────────────────────────────

    /**
     * Build a mocked Context backed by a mocked ActivityManager.
     * The MemoryInfo is populated via the callback-style getMemoryInfo().
     */
    private fun contextWithRamMB(totalRamMB: Long): Context {
        val memInfo = ActivityManager.MemoryInfo()
        // totalMem is a public field — set it directly
        memInfo.totalMem = totalRamMB * 1024L * 1024L

        val activityManager = mockk<ActivityManager>()
        val slot = slot<ActivityManager.MemoryInfo>()
        every { activityManager.getMemoryInfo(capture(slot)) } answers {
            slot.captured.totalMem = memInfo.totalMem
        }

        val ctx = mockk<Context>()
        every { ctx.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager

        return ctx
    }

    @Test
    fun `autoTuned with 512MB RAM selects TINY for multilingual`() {
        // forAvailableRAM(512, false): TINY needs minRAMMB 256 <= 512/2=256 — qualifies
        val ctx = contextWithRamMB(512)
        val config = WhisperConfig.autoTuned(ctx, language = "fr")
        assertEquals(WhisperModelSize.TINY, config.modelSize)
        assertEquals("fr", config.language)
    }

    @Test
    fun `autoTuned with 512MB RAM selects TINY_EN for English`() {
        // forAvailableRAM(512, true): TINY_EN needs minRAMMB 256 <= 256 — qualifies
        val ctx = contextWithRamMB(512)
        val config = WhisperConfig.autoTuned(ctx, language = "en")
        assertEquals(WhisperModelSize.TINY_EN, config.modelSize)
    }

    @Test
    fun `autoTuned with 1024MB RAM selects BASE for multilingual`() {
        // forAvailableRAM(1024, false): BASE needs 512 <= 512 — qualifies; SMALL needs 1024 <= 512 — no
        val ctx = contextWithRamMB(1024)
        val config = WhisperConfig.autoTuned(ctx, language = "es")
        assertEquals(WhisperModelSize.BASE, config.modelSize)
    }

    @Test
    fun `autoTuned with 1024MB RAM selects BASE_EN for English`() {
        val ctx = contextWithRamMB(1024)
        val config = WhisperConfig.autoTuned(ctx, language = "en")
        assertEquals(WhisperModelSize.BASE_EN, config.modelSize)
    }

    @Test
    fun `autoTuned with 2048MB RAM selects SMALL for multilingual`() {
        // forAvailableRAM(2048, false): SMALL needs 1024 <= 1024 — qualifies
        val ctx = contextWithRamMB(2048)
        val config = WhisperConfig.autoTuned(ctx, language = "de")
        assertEquals(WhisperModelSize.SMALL, config.modelSize)
    }

    @Test
    fun `autoTuned with 2048MB RAM selects SMALL_EN for English`() {
        val ctx = contextWithRamMB(2048)
        val config = WhisperConfig.autoTuned(ctx, language = "en")
        assertEquals(WhisperModelSize.SMALL_EN, config.modelSize)
    }

    @Test
    fun `autoTuned with 4096MB RAM selects MEDIUM for multilingual`() {
        // forAvailableRAM(4096, false): MEDIUM needs 2048 <= 2048 — qualifies
        val ctx = contextWithRamMB(4096)
        val config = WhisperConfig.autoTuned(ctx, language = "zh")
        assertEquals(WhisperModelSize.MEDIUM, config.modelSize)
    }

    @Test
    fun `autoTuned with 8192MB RAM selects MEDIUM for multilingual`() {
        val ctx = contextWithRamMB(8192)
        val config = WhisperConfig.autoTuned(ctx, language = "ja")
        assertEquals(WhisperModelSize.MEDIUM, config.modelSize)
    }

    @Test
    fun `autoTuned with 8192MB RAM selects MEDIUM_EN for English`() {
        val ctx = contextWithRamMB(8192)
        val config = WhisperConfig.autoTuned(ctx, language = "en")
        assertEquals(WhisperModelSize.MEDIUM_EN, config.modelSize)
    }

    @Test
    fun `autoTuned with very low RAM 100MB falls back to TINY`() {
        // forAvailableRAM(100, false): TINY needs 256 <= 50 — no. Falls back to TINY.
        val ctx = contextWithRamMB(100)
        val config = WhisperConfig.autoTuned(ctx, language = "fr")
        assertEquals(WhisperModelSize.TINY, config.modelSize)
    }

    @Test
    fun `autoTuned with very low RAM 100MB falls back to TINY_EN for English`() {
        val ctx = contextWithRamMB(100)
        val config = WhisperConfig.autoTuned(ctx, language = "en")
        assertEquals(WhisperModelSize.TINY_EN, config.modelSize)
    }

    @Test
    fun `autoTuned default language is English`() {
        val ctx = contextWithRamMB(2048)
        val config = WhisperConfig.autoTuned(ctx)
        assertEquals("en", config.language)
    }

    @Test
    fun `autoTuned result has valid configuration`() {
        val ctx = contextWithRamMB(2048)
        val config = WhisperConfig.autoTuned(ctx, language = "en")
        assertTrue(config.validate().isSuccess, "autoTuned result must pass validation")
    }

    @Test
    fun `autoTuned English detection uses startsWith not equals`() {
        // "en-US" starts with "en" -> isEnglish = true -> english-only variant
        val ctx = contextWithRamMB(2048)
        val config = WhisperConfig.autoTuned(ctx, language = "en-US")
        assertTrue(config.modelSize.isEnglishOnly, "en-US should trigger English-only model selection")
    }

    @Test
    fun `autoTuned non-English language picks multilingual model`() {
        val ctx = contextWithRamMB(2048)
        val config = WhisperConfig.autoTuned(ctx, language = "es")
        assertFalse(config.modelSize.isEnglishOnly, "Non-English language should pick multilingual model")
    }

    // ── WhisperModelSize.forAvailableRAM ─────────────────────────
    // Core logic is also tested in WhisperModelsTest (commonTest).
    // These tests verify the RAM boundary math that autoTuned depends on.

    @Test
    fun `forAvailableRAM 512MB multilingual selects TINY`() {
        // TINY: minRAMMB=256, 256 <= 512/2=256 true
        assertEquals(WhisperModelSize.TINY, WhisperModelSize.forAvailableRAM(512, false))
    }

    @Test
    fun `forAvailableRAM 512MB English selects TINY_EN`() {
        assertEquals(WhisperModelSize.TINY_EN, WhisperModelSize.forAvailableRAM(512, true))
    }

    @Test
    fun `forAvailableRAM 1024MB multilingual selects BASE`() {
        // BASE: minRAMMB=512, 512 <= 1024/2=512 true; SMALL: 1024 <= 512 false
        assertEquals(WhisperModelSize.BASE, WhisperModelSize.forAvailableRAM(1024, false))
    }

    @Test
    fun `forAvailableRAM 1024MB English selects BASE_EN`() {
        assertEquals(WhisperModelSize.BASE_EN, WhisperModelSize.forAvailableRAM(1024, true))
    }

    @Test
    fun `forAvailableRAM 2048MB multilingual selects SMALL`() {
        // SMALL: minRAMMB=1024, 1024 <= 2048/2=1024 true
        assertEquals(WhisperModelSize.SMALL, WhisperModelSize.forAvailableRAM(2048, false))
    }

    @Test
    fun `forAvailableRAM 2048MB English selects SMALL_EN`() {
        assertEquals(WhisperModelSize.SMALL_EN, WhisperModelSize.forAvailableRAM(2048, true))
    }

    @Test
    fun `forAvailableRAM 4096MB multilingual selects MEDIUM`() {
        // MEDIUM: minRAMMB=2048, 2048 <= 4096/2=2048 true
        assertEquals(WhisperModelSize.MEDIUM, WhisperModelSize.forAvailableRAM(4096, false))
    }

    @Test
    fun `forAvailableRAM 4096MB English selects MEDIUM_EN`() {
        assertEquals(WhisperModelSize.MEDIUM_EN, WhisperModelSize.forAvailableRAM(4096, true))
    }

    @Test
    fun `forAvailableRAM 100MB falls back to TINY`() {
        // No model fits: 256 <= 50 is false for all. Fallback = TINY.
        assertEquals(WhisperModelSize.TINY, WhisperModelSize.forAvailableRAM(100, false))
    }

    @Test
    fun `forAvailableRAM 100MB English falls back to TINY_EN`() {
        assertEquals(WhisperModelSize.TINY_EN, WhisperModelSize.forAvailableRAM(100, true))
    }

    @Test
    fun `forAvailableRAM default englishOnly is false`() {
        val result = WhisperModelSize.forAvailableRAM(2048)
        assertFalse(result.isEnglishOnly)
    }

    @Test
    fun `forAvailableRAM never selects english-only for multilingual request`() {
        listOf(100, 512, 1024, 2048, 4096, 8192).forEach { ram ->
            val model = WhisperModelSize.forAvailableRAM(ram, englishOnly = false)
            assertFalse(model.isEnglishOnly, "forAvailableRAM($ram, false) returned English-only ${model.name}")
        }
    }

    @Test
    fun `forAvailableRAM always selects english-only for English request`() {
        listOf(100, 512, 1024, 2048, 4096, 8192).forEach { ram ->
            val model = WhisperModelSize.forAvailableRAM(ram, englishOnly = true)
            assertTrue(model.isEnglishOnly, "forAvailableRAM($ram, true) returned non-English ${model.name}")
        }
    }

    @Test
    fun `forAvailableRAM result always passes validate for English config`() {
        listOf(100, 512, 1024, 2048, 4096).forEach { ram ->
            val modelSize = WhisperModelSize.forAvailableRAM(ram, englishOnly = true)
            val config = WhisperConfig(modelSize = modelSize, language = "en")
            assertTrue(config.validate().isSuccess, "Config for $ram MB RAM should be valid")
        }
    }

    // ── Model Properties ─────────────────────────────────────────

    @Test
    fun `modelSize ggmlFileName for TINY is correct`() {
        assertEquals("ggml-tiny.bin", WhisperModelSize.TINY.ggmlFileName)
    }

    @Test
    fun `modelSize ggmlFileName for TINY_EN is correct`() {
        assertEquals("ggml-tiny.en.bin", WhisperModelSize.TINY_EN.ggmlFileName)
    }

    @Test
    fun `modelSize ggmlFileName for BASE is correct`() {
        assertEquals("ggml-base.bin", WhisperModelSize.BASE.ggmlFileName)
    }

    @Test
    fun `modelSize ggmlFileName for BASE_EN is correct`() {
        assertEquals("ggml-base.en.bin", WhisperModelSize.BASE_EN.ggmlFileName)
    }

    @Test
    fun `modelSize ggmlFileName for SMALL is correct`() {
        assertEquals("ggml-small.bin", WhisperModelSize.SMALL.ggmlFileName)
    }

    @Test
    fun `modelSize ggmlFileName for SMALL_EN is correct`() {
        assertEquals("ggml-small.en.bin", WhisperModelSize.SMALL_EN.ggmlFileName)
    }

    @Test
    fun `modelSize ggmlFileName for MEDIUM is correct`() {
        assertEquals("ggml-medium.bin", WhisperModelSize.MEDIUM.ggmlFileName)
    }

    @Test
    fun `modelSize ggmlFileName for MEDIUM_EN is correct`() {
        assertEquals("ggml-medium.en.bin", WhisperModelSize.MEDIUM_EN.ggmlFileName)
    }

    @Test
    fun `modelSize displayName for SMALL_EN is human readable`() {
        assertEquals("Small (English)", WhisperModelSize.SMALL_EN.displayName)
    }

    @Test
    fun `modelSize vsmName for TINY has correct format`() {
        assertEquals("VoiceOS-Tin-MUL.vlm", WhisperModelSize.TINY.vsmName)
    }

    @Test
    fun `modelSize vsmName for MEDIUM_EN has correct format`() {
        assertEquals("VoiceOS-Med-EN.vlm", WhisperModelSize.MEDIUM_EN.vsmName)
    }

    @Test
    fun `minRAMMB values are ascending with model size`() {
        assertTrue(WhisperModelSize.TINY.minRAMMB < WhisperModelSize.BASE.minRAMMB)
        assertTrue(WhisperModelSize.BASE.minRAMMB < WhisperModelSize.SMALL.minRAMMB)
        assertTrue(WhisperModelSize.SMALL.minRAMMB < WhisperModelSize.MEDIUM.minRAMMB)
    }

    @Test
    fun `approxSizeMB values are positive for all models`() {
        WhisperModelSize.entries.forEach { model ->
            assertTrue(model.approxSizeMB > 0, "${model.name}.approxSizeMB should be positive")
        }
    }

    // ── Data Class Equality ──────────────────────────────────────

    @Test
    fun `configs with same values are equal`() {
        val a = WhisperConfig(modelSize = WhisperModelSize.SMALL, language = "fr")
        val b = WhisperConfig(modelSize = WhisperModelSize.SMALL, language = "fr")
        assertEquals(a, b)
    }

    @Test
    fun `copy preserves all fields`() {
        val config = WhisperConfig(
            modelSize = WhisperModelSize.MEDIUM,
            language = "de",
            translateToEnglish = true,
            numThreads = 4,
            customModelPath = "/sdcard/my-model.bin",
            vadSensitivity = 0.8f,
            silenceThresholdMs = 800,
            minSpeechDurationMs = 200,
            maxChunkDurationMs = 15_000,
            useQuantized = true
        )
        assertEquals(config, config.copy())
    }

    @Test
    fun `configs with different modelSize are not equal`() {
        val a = WhisperConfig(modelSize = WhisperModelSize.TINY)
        val b = WhisperConfig(modelSize = WhisperModelSize.BASE)
        assertTrue(a != b)
    }

    @Test
    fun `configs with different language are not equal`() {
        val a = WhisperConfig(language = "en")
        val b = WhisperConfig(language = "es")
        assertTrue(a != b)
    }

    // ── Custom Model Path Field ──────────────────────────────────

    @Test
    fun `customModelPath is null by default`() {
        assertNull(WhisperConfig().customModelPath)
    }

    @Test
    fun `customModelPath is stored as-is`() {
        val path = "/sdcard/my-model.bin"
        val config = WhisperConfig(customModelPath = path)
        assertEquals(path, config.customModelPath)
    }

    // ── Configuration Combinations ───────────────────────────────

    @Test
    fun `translateToEnglish can be true with any multilingual model`() {
        val config = WhisperConfig(
            modelSize = WhisperModelSize.SMALL,
            language = "es",
            translateToEnglish = true
        )
        assertTrue(config.validate().isSuccess)
        assertTrue(config.translateToEnglish)
    }

    @Test
    fun `useQuantized flag is independent of validation`() {
        val config = WhisperConfig(useQuantized = true)
        assertTrue(config.validate().isSuccess)
        assertTrue(config.useQuantized)
    }

    @Test
    fun `maxChunkDurationMs is stored correctly`() {
        val config = WhisperConfig(maxChunkDurationMs = 15_000)
        assertEquals(15_000L, config.maxChunkDurationMs)
    }
}
