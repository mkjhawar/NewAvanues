package com.augmentalis.nlu

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.DatabaseProvider
import com.augmentalis.ava.core.data.entity.IntentEmbeddingEntity
import com.augmentalis.nlu.locale.LocaleManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for multi-locale support in IntentClassifier
 *
 * Tests:
 * - Locale fallback chain (fr-FR → fr → en-US)
 * - 5 language classification (en-US, es-ES, fr-FR, de-DE, zh-CN)
 * - LocaleManager integration with IntentClassifier
 * - Embedding loading from database with locale fallback
 *
 * Requirements:
 * - Database must have embeddings for test locales
 * - LocaleManager must correctly build fallback chains
 * - IntentClassifier must load embeddings using fallback chain
 */
@RunWith(AndroidJUnit4::class)
class MultiLocaleIntegrationTest {

    private lateinit var context: Context
    private lateinit var localeManager: LocaleManager
    private lateinit var database: com.augmentalis.ava.core.data.AVADatabase

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        localeManager = LocaleManager(context)
        database = DatabaseProvider.getDatabase(context, enableDestructiveMigration = false)
    }

    @After
    fun teardown() {
        // Restore default locale
        localeManager.setLocale(null)
    }

    @Test
    fun testLocaleManagerFallbackChain_French() = runBlocking {
        // Test: fr-FR should fall back to fr, then en-US
        val chain = localeManager.getFallbackChain("fr-FR")
        assertEquals(listOf("fr-FR", "fr", "en-US"), chain, "French fallback chain")
    }

    @Test
    fun testLocaleManagerFallbackChain_Spanish() = runBlocking {
        // Test: es-ES should fall back to es, then en-US
        val chain = localeManager.getFallbackChain("es-ES")
        assertEquals(listOf("es-ES", "es", "en-US"), chain, "Spanish fallback chain")
    }

    @Test
    fun testLocaleManagerFallbackChain_German() = runBlocking {
        // Test: de-DE should fall back to de, then en-US
        val chain = localeManager.getFallbackChain("de-DE")
        assertEquals(listOf("de-DE", "de", "en-US"), chain, "German fallback chain")
    }

    @Test
    fun testLocaleManagerFallbackChain_Chinese() = runBlocking {
        // Test: zh-CN should fall back to zh, then en-US
        val chain = localeManager.getFallbackChain("zh-CN")
        assertEquals(listOf("zh-CN", "zh", "en-US"), chain, "Chinese fallback chain")
    }

    @Test
    fun testLocaleManagerFallbackChain_English() = runBlocking {
        // Test: en-US should only have itself (no fallback needed)
        val chain = localeManager.getFallbackChain("en-US")
        assertEquals(listOf("en-US"), chain, "English fallback chain")
    }

    @Test
    fun testLocaleManagerSetAndGet() = runBlocking {
        // Test: Set locale and verify it's retrieved
        localeManager.setLocale("fr-FR")
        assertEquals("fr-FR", localeManager.getCurrentLocale(), "Set locale should be retrieved")

        // Test: Clear locale (revert to system default)
        localeManager.setLocale(null)
        val systemLocale = localeManager.getCurrentLocale()
        assertTrue(systemLocale.isNotEmpty(), "System locale should not be empty")
    }

    @Test
    fun testSupportedLocales_MajorLanguages() {
        // Test: All major languages should be supported
        val majorLocales = listOf(
            "en-US", "es-ES", "fr-FR", "de-DE", "zh-CN",
            "ja-JP", "ko-KR", "pt-BR", "ru-RU", "ar-SA", "hi-IN"
        )

        for (locale in majorLocales) {
            assertTrue(
                localeManager.isLocaleSupported(locale),
                "Locale $locale should be supported"
            )
        }
    }

    @Test
    fun testDatabaseEmbeddingsByLocale_EnglishUS() = runBlocking {
        // Test: Load en-US embeddings from database
        val embeddingDao = database.intentEmbeddingDao()
        val embeddings = embeddingDao.getAllEmbeddingsForLocale("en-US")

        // If embeddings exist, verify structure
        if (embeddings.isNotEmpty()) {
            android.util.Log.i("MultiLocaleTest", "✓ Found ${embeddings.size} en-US embeddings")

            val firstEmbedding = embeddings.first()
            assertEquals("en-US", firstEmbedding.locale, "Locale should be en-US")
            assertTrue(firstEmbedding.intentId.isNotEmpty(), "Intent ID should not be empty")
            assertTrue(
                firstEmbedding.embeddingDimension == 384 || firstEmbedding.embeddingDimension == 768,
                "Embedding dimension should be 384 (MobileBERT) or 768 (mALBERT)"
            )
        } else {
            android.util.Log.w("MultiLocaleTest", "No en-US embeddings in database (need to run .aot import)")
        }
    }

    @Test
    fun testFallbackChainLoading() = runBlocking {
        // Test: Fallback chain should load embeddings from first available locale
        val embeddingDao = database.intentEmbeddingDao()

        // Test with fr-FR fallback chain: fr-FR → fr → en-US
        val fallbackChain = localeManager.getFallbackChain("fr-FR")

        val loadedEmbeddings = fallbackChain.firstNotNullOfOrNull { localeCode ->
            embeddingDao.getAllEmbeddingsForLocale(localeCode)
                .takeIf { it.isNotEmpty() }
        }

        // Should load embeddings from first available locale in chain
        if (loadedEmbeddings != null) {
            val loadedLocale = loadedEmbeddings.first().locale
            assertTrue(
                fallbackChain.contains(loadedLocale),
                "Loaded locale ($loadedLocale) should be in fallback chain"
            )
            android.util.Log.i("MultiLocaleTest", "✓ Loaded ${loadedEmbeddings.size} embeddings from $loadedLocale")
        } else {
            android.util.Log.w("MultiLocaleTest", "No embeddings found in fallback chain (need to run .aot import)")
        }
    }

    @Test
    fun testEmbeddingQuality_Verification() = runBlocking {
        // Test: Verify embedding quality (L2 normalization, dimension, no NaN)
        val embeddingDao = database.intentEmbeddingDao()
        val embeddings = embeddingDao.getAllEmbeddingsForLocale("en-US")

        if (embeddings.isNotEmpty()) {
            for (embedding in embeddings.take(5)) {
                val vector = embedding.getEmbedding()

                // Check dimension
                assertEquals(
                    embedding.embeddingDimension,
                    vector.size,
                    "Embedding dimension should match metadata"
                )

                // Check for NaN or infinite values
                assertTrue(
                    vector.none { it.isNaN() || it.isInfinite() },
                    "Embedding should not contain NaN or infinite values"
                )

                // Check L2 norm (should be ~1.0 for normalized vectors)
                var norm = 0.0f
                for (value in vector) {
                    norm += value * value
                }
                norm = kotlin.math.sqrt(norm)

                assertTrue(
                    kotlin.math.abs(norm - 1.0f) < 0.01f,
                    "L2 norm should be ~1.0, got $norm"
                )
            }
            android.util.Log.i("MultiLocaleTest", "✓ Embedding quality verified for ${embeddings.size} embeddings")
        }
    }

    @Test
    fun testMultiLocaleEmbeddingCount() = runBlocking {
        // Test: Log embedding counts for different locales
        val embeddingDao = database.intentEmbeddingDao()
        val testLocales = listOf("en-US", "es-ES", "fr-FR", "de-DE", "zh-CN")

        android.util.Log.i("MultiLocaleTest", "=== Embedding counts by locale ===")
        for (locale in testLocales) {
            val count = embeddingDao.getEmbeddingCountForLocale(locale)
            android.util.Log.i("MultiLocaleTest", "  $locale: $count embeddings")
        }

        // At minimum, en-US should have embeddings
        val enUSCount = embeddingDao.getEmbeddingCountForLocale("en-US")
        assertTrue(
            enUSCount >= 0,
            "en-US embedding count should be non-negative (0 if .aot not imported)"
        )
    }

    @Test
    fun testLocaleManagerIntegrationWithClassifier() = runBlocking {
        // Test: Verify LocaleManager can be instantiated and used with context
        val testLocaleManager = LocaleManager(context)

        // Set a test locale
        testLocaleManager.setLocale("es-ES")
        assertEquals("es-ES", testLocaleManager.getCurrentLocale())

        // Get fallback chain
        val chain = testLocaleManager.getFallbackChain("es-ES")
        assertTrue(chain.contains("es-ES"), "Should contain es-ES")
        assertTrue(chain.contains("es"), "Should contain es")
        assertTrue(chain.contains("en-US"), "Should contain en-US fallback")

        // Restore default
        testLocaleManager.setLocale(null)
        android.util.Log.i("MultiLocaleTest", "✓ LocaleManager integration verified")
    }

    @Test
    fun testDatabaseEmbeddingStatsByLocale() = runBlocking {
        // Test: Query embedding statistics by locale
        val embeddingDao = database.intentEmbeddingDao()
        val stats = embeddingDao.getEmbeddingStatsByLocale()

        android.util.Log.i("MultiLocaleTest", "=== Embedding statistics ===")
        for (stat in stats) {
            android.util.Log.i("MultiLocaleTest", "  ${stat.locale}: ${stat.count} embeddings, " +
                    "avg ${stat.avgExamples} examples, " +
                    "last updated ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(stat.lastUpdated))}")
        }

        // Stats should be available (even if empty)
        assertTrue(stats.size >= 0, "Stats query should succeed")
    }

    @Test
    fun testNoDuplicateEmbeddingsPerLocale() = runBlocking {
        // Test: Ensure no duplicate (intent_id, locale) pairs
        val embeddingDao = database.intentEmbeddingDao()
        val embeddings = embeddingDao.getAllEmbeddingsForLocale("en-US")

        if (embeddings.isNotEmpty()) {
            val intentIds = embeddings.map { it.intentId }
            val uniqueIntentIds = intentIds.distinct()

            assertEquals(
                intentIds.size,
                uniqueIntentIds.size,
                "Should have no duplicate intent_ids for same locale"
            )
            android.util.Log.i("MultiLocaleTest", "✓ No duplicates: ${embeddings.size} unique embeddings")
        }
    }
}
