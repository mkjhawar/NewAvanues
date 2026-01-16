package com.augmentalis.voiceoscoreng.jit

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.ProcessingMode
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JitProcessorTest {

    private lateinit var processor: JitProcessor

    @Before
    fun setup() {
        LearnAppDevToggle.reset()
        processor = JitProcessor()
    }

    @After
    fun teardown() {
        LearnAppDevToggle.reset()
    }

    // ==================== Processing Mode Tests ====================

    @Test
    fun `getProcessingMode returns IMMEDIATE by default`() {
        assertEquals(ProcessingMode.IMMEDIATE, processor.getProcessingMode())
    }

    @Test
    fun `setProcessingMode changes mode`() {
        processor.setProcessingMode(ProcessingMode.BATCH)
        assertEquals(ProcessingMode.BATCH, processor.getProcessingMode())
    }

    // ==================== Element Processing Tests ====================

    @Test
    fun `processElement returns success for valid element`() {
        val element = createTestElement()
        val result = processor.processElement(element)

        assertTrue(result.isSuccess)
        assertNotNull(result.vuid)
    }

    @Test
    fun `processElement generates valid VUID`() {
        val element = createTestElement()
        val result = processor.processElement(element)

        assertNotNull(result.vuid)
        assertTrue(result.vuid!!.isNotEmpty())
    }

    @Test
    fun `processElement returns failure for invalid element`() {
        val element = createInvalidElement()
        val result = processor.processElement(element)

        assertFalse(result.isSuccess)
    }

    // ==================== Batch Processing Tests ====================

    @Test
    fun `processElements returns results for all elements`() {
        val elements = listOf(
            createTestElement("button1"),
            createTestElement("button2"),
            createTestElement("input1")
        )

        val results = processor.processElements(elements)

        assertEquals(3, results.size)
    }

    @Test
    fun `processElements in BATCH mode queues elements`() {
        processor.setProcessingMode(ProcessingMode.BATCH)
        val elements = listOf(
            createTestElement("button1"),
            createTestElement("button2")
        )

        val results = processor.processElements(elements)

        assertEquals(2, results.size)
        results.forEach { result ->
            assertTrue(result.processingMode == ProcessingMode.BATCH)
        }
    }

    // ==================== Queue Tests ====================

    @Test
    fun `getQueueSize returns 0 initially`() {
        assertEquals(0, processor.getQueueSize())
    }

    @Test
    fun `queueElement increases queue size`() {
        val element = createTestElement()
        processor.queueElement(element)
        assertEquals(1, processor.getQueueSize())
    }

    @Test
    fun `clearQueue resets queue size to 0`() {
        processor.queueElement(createTestElement("e1"))
        processor.queueElement(createTestElement("e2"))
        processor.clearQueue()
        assertEquals(0, processor.getQueueSize())
    }

    @Test
    fun `processQueue processes all queued elements`() {
        processor.queueElement(createTestElement("e1"))
        processor.queueElement(createTestElement("e2"))
        processor.queueElement(createTestElement("e3"))

        val results = processor.processQueue()

        assertEquals(3, results.size)
        assertEquals(0, processor.getQueueSize())
    }

    // ==================== Feature Gate Tests ====================

    @Test
    fun `processElement respects feature gate in LITE mode`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        val element = createTestElement()
        val result = processor.processElement(element)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `batch mode available in DEV tier`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        processor.setProcessingMode(ProcessingMode.BATCH)
        assertEquals(ProcessingMode.BATCH, processor.getProcessingMode())
    }

    // ==================== Stats Tests ====================

    @Test
    fun `getProcessedCount returns correct count`() {
        processor.processElement(createTestElement("e1"))
        processor.processElement(createTestElement("e2"))

        assertEquals(2, processor.getProcessedCount())
    }

    @Test
    fun `reset clears processed count`() {
        processor.processElement(createTestElement())
        processor.reset()
        assertEquals(0, processor.getProcessedCount())
    }

    // ==================== Helper Methods ====================

    private fun createTestElement(id: String = "test_button"): ElementInfo {
        return ElementInfo(
            className = "android.widget.Button",
            resourceId = id,
            text = "Click Me",
            contentDescription = "Test button",
            isClickable = true,
            bounds = Bounds(0, 0, 100, 50)
        )
    }

    private fun createInvalidElement(): ElementInfo {
        return ElementInfo(
            className = "",
            resourceId = "",
            text = "",
            contentDescription = "",
            isClickable = false,
            bounds = Bounds.EMPTY
        )
    }
}
