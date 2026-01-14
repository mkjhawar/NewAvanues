/**
 * QualityMetricDTOTest.kt - Unit tests for QualityMetricDTO business logic
 *
 * Part of VOS-META-001 Phase 1 testing
 * Created: 2025-12-03
 *
 * Tests quality scoring, level categorization, and suggestion generation.
 */
package com.augmentalis.voiceoscore.commands

import com.augmentalis.database.dto.MetadataQualityLevel
import com.augmentalis.database.dto.QualityMetricDTO
import org.junit.Assert.*
import org.junit.Test

class QualityMetricDTOTest {

    @Test
    fun `quality score 100 maps to EXCELLENT level`() {
        val metric = createMetric(qualityScore = 100)
        assertEquals(MetadataQualityLevel.EXCELLENT, metric.toQualityLevel())
    }

    @Test
    fun `quality score 80 maps to EXCELLENT level`() {
        val metric = createMetric(qualityScore = 80)
        assertEquals(MetadataQualityLevel.EXCELLENT, metric.toQualityLevel())
    }

    @Test
    fun `quality score 79 maps to GOOD level`() {
        val metric = createMetric(qualityScore = 79)
        assertEquals(MetadataQualityLevel.GOOD, metric.toQualityLevel())
    }

    @Test
    fun `quality score 60 maps to GOOD level`() {
        val metric = createMetric(qualityScore = 60)
        assertEquals(MetadataQualityLevel.GOOD, metric.toQualityLevel())
    }

    @Test
    fun `quality score 59 maps to ACCEPTABLE level`() {
        val metric = createMetric(qualityScore = 59)
        assertEquals(MetadataQualityLevel.ACCEPTABLE, metric.toQualityLevel())
    }

    @Test
    fun `quality score 40 maps to ACCEPTABLE level`() {
        val metric = createMetric(qualityScore = 40)
        assertEquals(MetadataQualityLevel.ACCEPTABLE, metric.toQualityLevel())
    }

    @Test
    fun `quality score 39 maps to POOR level`() {
        val metric = createMetric(qualityScore = 39)
        assertEquals(MetadataQualityLevel.POOR, metric.toQualityLevel())
    }

    @Test
    fun `quality score 0 maps to POOR level`() {
        val metric = createMetric(qualityScore = 0)
        assertEquals(MetadataQualityLevel.POOR, metric.toQualityLevel())
    }

    @Test
    fun `element needs manual command when poor quality and no commands`() {
        val metric = createMetric(
            qualityScore = 30,
            commandCount = 0
        )

        assertTrue("Should need manual command", metric.needsManualCommand())
    }

    @Test
    fun `element does not need manual command when has commands`() {
        val metric = createMetric(
            qualityScore = 30,
            commandCount = 1
        )

        assertFalse("Should not need manual command", metric.needsManualCommand())
    }

    @Test
    fun `element does not need manual command when good quality`() {
        val metric = createMetric(
            qualityScore = 60,
            commandCount = 0
        )

        assertFalse("Should not need manual command", metric.needsManualCommand())
    }

    @Test
    fun `getMetadataCount returns 0 when no metadata`() {
        val metric = createMetric(
            hasText = false,
            hasContentDesc = false,
            hasResourceId = false
        )

        assertEquals(0, metric.getMetadataCount())
    }

    @Test
    fun `getMetadataCount returns 1 when only text`() {
        val metric = createMetric(
            hasText = true,
            hasContentDesc = false,
            hasResourceId = false
        )

        assertEquals(1, metric.getMetadataCount())
    }

    @Test
    fun `getMetadataCount returns 2 when text and contentDesc`() {
        val metric = createMetric(
            hasText = true,
            hasContentDesc = true,
            hasResourceId = false
        )

        assertEquals(2, metric.getMetadataCount())
    }

    @Test
    fun `getMetadataCount returns 3 when all metadata present`() {
        val metric = createMetric(
            hasText = true,
            hasContentDesc = true,
            hasResourceId = true
        )

        assertEquals(3, metric.getMetadataCount())
    }

    @Test
    fun `getSuggestions includes text when no text or contentDesc`() {
        val metric = createMetric(
            hasText = false,
            hasContentDesc = false
        )

        val suggestions = metric.getSuggestions()
        assertTrue(
            "Should suggest adding text/contentDescription",
            suggestions.any { it.contains("text") || it.contains("contentDescription") }
        )
    }

    @Test
    fun `getSuggestions does not include text when contentDesc present`() {
        val metric = createMetric(
            hasText = false,
            hasContentDesc = true
        )

        val suggestions = metric.getSuggestions()
        assertFalse(
            "Should not suggest text when contentDesc present",
            suggestions.any { it.contains("Add text or contentDescription") }
        )
    }

    @Test
    fun `getSuggestions includes resource id when missing`() {
        val metric = createMetric(hasResourceId = false)

        val suggestions = metric.getSuggestions()
        assertTrue(
            "Should suggest adding resource ID",
            suggestions.any { it.contains("android:id") }
        )
    }

    @Test
    fun `getSuggestions does not include resource id when present`() {
        val metric = createMetric(hasResourceId = true)

        val suggestions = metric.getSuggestions()
        assertFalse(
            "Should not suggest resource ID when present",
            suggestions.any { it.contains("android:id") }
        )
    }

    @Test
    fun `getSuggestions includes voice command when no commands`() {
        val metric = createMetric(commandCount = 0)

        val suggestions = metric.getSuggestions()
        assertTrue(
            "Should suggest voice command assignment",
            suggestions.any { it.contains("voice command") }
        )
    }

    @Test
    fun `getSuggestions does not include voice command when commands exist`() {
        val metric = createMetric(commandCount = 2)

        val suggestions = metric.getSuggestions()
        assertFalse(
            "Should not suggest voice command when commands exist",
            suggestions.any { it.contains("Assign voice command") }
        )
    }

    @Test
    fun `getSuggestions returns all three when element has nothing`() {
        val metric = createMetric(
            hasText = false,
            hasContentDesc = false,
            hasResourceId = false,
            commandCount = 0
        )

        val suggestions = metric.getSuggestions()
        assertEquals("Should return 3 suggestions", 3, suggestions.size)
    }

    @Test
    fun `getSuggestions returns empty when element is perfect`() {
        val metric = createMetric(
            hasText = true,
            hasContentDesc = true,
            hasResourceId = true,
            commandCount = 1
        )

        val suggestions = metric.getSuggestions()
        assertEquals("Should return 0 suggestions", 0, suggestions.size)
    }

    @Test
    fun `manual command count is tracked separately`() {
        val metric = createMetric(
            commandCount = 5,
            manualCommandCount = 2
        )

        assertEquals(5, metric.commandCount)
        assertEquals(2, metric.manualCommandCount)
    }

    // Helper function
    private fun createMetric(
        qualityScore: Int = 50,
        hasText: Boolean = true,
        hasContentDesc: Boolean = true,
        hasResourceId: Boolean = true,
        commandCount: Int = 1,
        manualCommandCount: Int = 0
    ): QualityMetricDTO {
        return QualityMetricDTO(
            elementUuid = "test-uuid",
            appId = "com.example.app",
            qualityScore = qualityScore,
            hasText = hasText,
            hasContentDesc = hasContentDesc,
            hasResourceId = hasResourceId,
            commandCount = commandCount,
            manualCommandCount = manualCommandCount,
            lastAssessed = System.currentTimeMillis()
        )
    }
}
