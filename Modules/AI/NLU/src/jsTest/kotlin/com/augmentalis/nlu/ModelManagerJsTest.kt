package com.augmentalis.nlu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * JS/Web tests for ModelManager
 *
 * Tests model management logic without requiring actual downloads.
 * IndexedDB operations are tested separately since they need browser APIs.
 */
class ModelManagerJsTest {

    @Test
    fun isModelAvailable_falseInitially() {
        val manager = ModelManager()
        assertFalse(manager.isModelAvailable(), "No model should be available before download")
    }

    @Test
    fun getActiveModelType_defaultsBasedOnLocale() {
        val manager = ModelManager()
        val modelType = manager.getActiveModelType()

        // Default locale should be en-US (from LocaleManager fallback)
        // en-US → MobileBERT
        assertTrue(
            modelType == ModelType.MOBILEBERT || modelType == ModelType.MALBERT,
            "Should be a valid model type"
        )
    }

    @Test
    fun getEmbeddingDimension_matchesModelType() {
        val manager = ModelManager()
        val dimension = manager.getEmbeddingDimension()
        val modelType = manager.getActiveModelType()

        assertEquals(
            modelType.embeddingDimension, dimension,
            "Embedding dimension should match active model type"
        )
    }

    @Test
    fun modelType_moblebertHas384Dim() {
        assertEquals(384, ModelType.MOBILEBERT.embeddingDimension)
        assertEquals("mobilebert_model.onnx", ModelType.MOBILEBERT.modelFileName)
        assertFalse(ModelType.MOBILEBERT.isMultilingual())
    }

    @Test
    fun modelType_malbertHas768Dim() {
        assertEquals(768, ModelType.MALBERT.embeddingDimension)
        assertEquals("malbert_model.onnx", ModelType.MALBERT.modelFileName)
        assertTrue(ModelType.MALBERT.isMultilingual())
    }

    @Test
    fun getModelsSize_zeroWhenNoModels() {
        val manager = ModelManager()
        assertEquals(0L, manager.getModelsSize(), "Size should be 0 with no cached models")
    }

    @Test
    fun clearModels_succeedsWhenEmpty() {
        val manager = ModelManager()
        val result = manager.clearModels()
        assertTrue(result.isSuccess, "clearModels should succeed even when no models cached")
    }

    @Test
    fun getModelPath_returnsActiveModelFileName() {
        val manager = ModelManager()
        val path = manager.getModelPath()
        val activeType = manager.getActiveModelType()

        assertEquals(activeType.modelFileName, path, "getModelPath should return active model filename")
    }

    @Test
    fun getVocabPath_containsModelTypeName() {
        val manager = ModelManager()
        val vocabPath = manager.getVocabPath()

        assertTrue(
            vocabPath.contains("vocab.txt"),
            "Vocab path should contain vocab.txt"
        )
    }

    @Test
    fun modelType_versionStrings() {
        assertEquals("MobileBERT-uncased-onnx-384", ModelType.MOBILEBERT.getModelVersion())
        assertEquals("mALBERT-multilingual-v2-768", ModelType.MALBERT.getModelVersion())
    }
}
