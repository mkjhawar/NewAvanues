package com.augmentalis.argscanner.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ScannedObject model
 *
 * Tests:
 * - Data model integrity
 * - Helper method calculations
 * - Edge case handling
 * - Confidence thresholds
 */
class ScannedObjectTest {

    @Test
    fun `test distance calculation from origin`() {
        val obj = createTestObject(
            position = ScannedObject.Position3D(x = 3f, y = 4f, z = 0f)
        )

        // 3-4-5 triangle: sqrt(9 + 16 + 0) = 5
        assertEquals(5f, obj.distanceFromOrigin(), 0.01f)
    }

    @Test
    fun `test volume calculation`() {
        val obj = createTestObject(
            boundingBox = ScannedObject.BoundingBox3D(
                width = 2f,
                height = 3f,
                depth = 4f
            )
        )

        assertEquals(24f, obj.volume(), 0.01f)
    }

    @Test
    fun `test confidence threshold - confident object`() {
        val obj = createTestObject(confidence = 0.85f)
        assertTrue(obj.isConfident(threshold = 0.7f))
    }

    @Test
    fun `test confidence threshold - not confident`() {
        val obj = createTestObject(confidence = 0.6f)
        assertFalse(obj.isConfident(threshold = 0.7f))
    }

    @Test
    fun `test voice friendly name - with custom name`() {
        val obj = createTestObject(
            label = "office_chair",
            voiceName = "My Chair"
        )

        assertEquals("My Chair", obj.getVoiceFriendlyName())
    }

    @Test
    fun `test voice friendly name - without custom name`() {
        val obj = createTestObject(label = "office_chair")

        assertEquals("office chair", obj.getVoiceFriendlyName())
    }

    @Test
    fun `test tracking quality enum ordering`() {
        assertTrue(ScannedObject.TrackingQuality.EXCELLENT.ordinal >
                  ScannedObject.TrackingQuality.GOOD.ordinal)
        assertTrue(ScannedObject.TrackingQuality.GOOD.ordinal >
                  ScannedObject.TrackingQuality.NORMAL.ordinal)
        assertTrue(ScannedObject.TrackingQuality.NORMAL.ordinal >
                  ScannedObject.TrackingQuality.POOR.ordinal)
    }

    @Test
    fun `test zero volume object`() {
        val obj = createTestObject(
            boundingBox = ScannedObject.BoundingBox3D(
                width = 0f,
                height = 0f,
                depth = 0f
            )
        )

        assertEquals(0f, obj.volume(), 0.01f)
    }

    @Test
    fun `test object at origin has zero distance`() {
        val obj = createTestObject(
            position = ScannedObject.Position3D(x = 0f, y = 0f, z = 0f)
        )

        assertEquals(0f, obj.distanceFromOrigin(), 0.01f)
    }

    // Helper method
    private fun createTestObject(
        uuid: String = "test-uuid",
        sessionId: String = "test-session",
        label: String = "test_object",
        confidence: Float = 0.8f,
        position: ScannedObject.Position3D = ScannedObject.Position3D(0f, 0f, 0f),
        rotation: ScannedObject.Rotation3D = ScannedObject.Rotation3D(0f, 0f, 0f),
        boundingBox: ScannedObject.BoundingBox3D = ScannedObject.BoundingBox3D(1f, 1f, 1f),
        voiceName: String? = null
    ) = ScannedObject(
        uuid = uuid,
        sessionId = sessionId,
        label = label,
        confidence = confidence,
        position = position,
        rotation = rotation,
        boundingBox = boundingBox,
        voiceName = voiceName
    )
}
