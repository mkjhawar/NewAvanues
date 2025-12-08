package com.augmentalis.argscanner.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SpatialRelationship model
 *
 * Tests:
 * - Relationship calculations
 * - Proximity detection
 * - Grouping logic
 * - Relative positioning
 */
class SpatialRelationshipTest {

    @Test
    fun `test touching proximity level`() {
        val source = createObject(position = pos(0f, 0f, 0f))
        val target = createObject(position = pos(0.05f, 0f, 0f))

        val relationship = SpatialRelationship.from("session", source, target)

        assertEquals(SpatialRelationship.ProximityLevel.TOUCHING, relationship.proximityLevel)
        assertTrue(relationship.areTouching())
    }

    @Test
    fun `test very close proximity`() {
        val source = createObject(position = pos(0f, 0f, 0f))
        val target = createObject(position = pos(0.3f, 0f, 0f))

        val relationship = SpatialRelationship.from("session", source, target)

        assertEquals(SpatialRelationship.ProximityLevel.VERY_CLOSE, relationship.proximityLevel)
    }

    @Test
    fun `test vertical alignment - above`() {
        val source = createObject(position = pos(0f, 0f, 0f))
        val target = createObject(position = pos(0f, 1f, 0f))

        val relationship = SpatialRelationship.from("session", source, target)

        assertEquals(SpatialRelationship.RelativePosition.ABOVE, relationship.relativePosition)
        assertTrue(relationship.areVerticallyAligned())
    }

    @Test
    fun `test vertical alignment - below`() {
        val source = createObject(position = pos(0f, 1f, 0f))
        val target = createObject(position = pos(0f, 0f, 0f))

        val relationship = SpatialRelationship.from("session", source, target)

        assertEquals(SpatialRelationship.RelativePosition.BELOW, relationship.relativePosition)
        assertTrue(relationship.areVerticallyAligned())
    }

    @Test
    fun `test horizontal alignment - left`() {
        val source = createObject(position = pos(1f, 0f, 0f))
        val target = createObject(position = pos(0f, 0f, 0f))

        val relationship = SpatialRelationship.from("session", source, target)

        assertEquals(SpatialRelationship.RelativePosition.LEFT, relationship.relativePosition)
        assertTrue(relationship.areHorizontallyAligned())
    }

    @Test
    fun `test horizontal alignment - right`() {
        val source = createObject(position = pos(0f, 0f, 0f))
        val target = createObject(position = pos(1f, 0f, 0f))

        val relationship = SpatialRelationship.from("session", source, target)

        assertEquals(SpatialRelationship.RelativePosition.RIGHT, relationship.relativePosition)
        assertTrue(relationship.areHorizontallyAligned())
    }

    @Test
    fun `test workspace grouping - desk and chair`() {
        val desk = createObject(label = "desk", position = pos(0f, 0f, 0f))
        val chair = createObject(label = "chair", position = pos(0.5f, 0f, 0f))

        val relationship = SpatialRelationship.from("session", desk, chair)

        assertTrue(relationship.isGrouped)
        assertEquals(SpatialRelationship.GroupType.WORKSPACE, relationship.groupType)
    }

    @Test
    fun `test furniture grouping - sofa and table`() {
        val sofa = createObject(label = "sofa", position = pos(0f, 0f, 0f))
        val table = createObject(label = "table", position = pos(0.8f, 0f, 0f))

        val relationship = SpatialRelationship.from("session", sofa, table)

        assertTrue(relationship.isGrouped)
        assertEquals(SpatialRelationship.GroupType.FURNITURE, relationship.groupType)
    }

    @Test
    fun `test stack grouping - vertical alignment`() {
        val bottom = createObject(label = "box", position = pos(0f, 0f, 0f))
        val top = createObject(label = "book", position = pos(0f, 0.3f, 0f))

        val relationship = SpatialRelationship.from("session", bottom, top)

        assertTrue(relationship.isGrouped)
        assertEquals(SpatialRelationship.GroupType.STACK, relationship.groupType)
    }

    @Test
    fun `test no grouping - far apart`() {
        val obj1 = createObject(position = pos(0f, 0f, 0f))
        val obj2 = createObject(position = pos(5f, 0f, 0f))

        val relationship = SpatialRelationship.from("session", obj1, obj2)

        assertFalse(relationship.isGrouped)
        assertNull(relationship.groupType)
    }

    @Test
    fun `test distance calculation accuracy`() {
        val source = createObject(position = pos(0f, 0f, 0f))
        val target = createObject(position = pos(3f, 4f, 0f))

        val relationship = SpatialRelationship.from("session", source, target)

        // 3-4-5 triangle
        assertEquals(5f, relationship.distance, 0.01f)
        assertEquals(5f, relationship.horizontalDistance, 0.01f)
        assertEquals(4f, relationship.verticalDistance, 0.01f)
    }

    // Helper methods
    private fun createObject(
        label: String = "object",
        position: ScannedObject.Position3D
    ) = ScannedObject(
        uuid = java.util.UUID.randomUUID().toString(),
        sessionId = "test",
        label = label,
        confidence = 0.8f,
        position = position,
        rotation = ScannedObject.Rotation3D(0f, 0f, 0f),
        boundingBox = ScannedObject.BoundingBox3D(0.5f, 0.5f, 0.5f)
    )

    private fun pos(x: Float, y: Float, z: Float) = ScannedObject.Position3D(x, y, z)
}
