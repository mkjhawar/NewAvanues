package com.augmentalis.voiceoscoreng.handlers

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DragHandlerTest {

    private lateinit var handler: DragHandler

    @BeforeTest
    fun setup() {
        handler = DragHandler()
    }

    @AfterTest
    fun teardown() {
        // No cleanup needed for DragHandler
    }

    // ==================== handleCommand "drag up" Tests ====================

    @Test
    fun `handleCommand drag up creates upward drag`() {
        val result = handler.handleCommand("drag up")

        assertTrue(result.success)
        assertNotNull(result.operation)
        assertNull(result.error)

        // Should start at center and move up
        val op = result.operation!!
        assertEquals(540, op.startX)
        assertEquals(960, op.startY)
        assertEquals(540, op.endX)
        assertTrue(op.endY < op.startY, "End Y should be less than start Y for upward drag")
    }

    @Test
    fun `handleCommand DRAG UP is case insensitive`() {
        val result = handler.handleCommand("DRAG UP")

        assertTrue(result.success)
        assertNotNull(result.operation)
    }

    @Test
    fun `handleCommand drag up with extra whitespace`() {
        val result = handler.handleCommand("  drag up  ")

        assertTrue(result.success)
        assertNotNull(result.operation)
    }

    // ==================== handleCommand "drag down" Tests ====================

    @Test
    fun `handleCommand drag down creates downward drag`() {
        val result = handler.handleCommand("drag down")

        assertTrue(result.success)
        assertNotNull(result.operation)
        assertNull(result.error)

        // Should start at center and move down
        val op = result.operation!!
        assertEquals(540, op.startX)
        assertEquals(960, op.startY)
        assertEquals(540, op.endX)
        assertTrue(op.endY > op.startY, "End Y should be greater than start Y for downward drag")
    }

    @Test
    fun `handleCommand drag down with mixed case`() {
        val result = handler.handleCommand("Drag Down")

        assertTrue(result.success)
        assertNotNull(result.operation)
    }

    // ==================== handleCommand "drag left" Tests ====================

    @Test
    fun `handleCommand drag left creates leftward drag`() {
        val result = handler.handleCommand("drag left")

        assertTrue(result.success)
        assertNotNull(result.operation)
        assertNull(result.error)

        // Should start at center and move left
        val op = result.operation!!
        assertEquals(540, op.startX)
        assertEquals(960, op.startY)
        assertEquals(960, op.endY)
        assertTrue(op.endX < op.startX, "End X should be less than start X for leftward drag")
    }

    // ==================== handleCommand "drag right" Tests ====================

    @Test
    fun `handleCommand drag right creates rightward drag`() {
        val result = handler.handleCommand("drag right")

        assertTrue(result.success)
        assertNotNull(result.operation)
        assertNull(result.error)

        // Should start at center and move right
        val op = result.operation!!
        assertEquals(540, op.startX)
        assertEquals(960, op.startY)
        assertEquals(960, op.endY)
        assertTrue(op.endX > op.startX, "End X should be greater than start X for rightward drag")
    }

    // ==================== handleCommand "drag from X,Y to X,Y" Tests ====================

    @Test
    fun `handleCommand drag from coordinates to coordinates parses correctly`() {
        val result = handler.handleCommand("drag from 100,200 to 300,400")

        assertTrue(result.success)
        assertNotNull(result.operation)
        assertNull(result.error)

        val op = result.operation!!
        assertEquals(100, op.startX)
        assertEquals(200, op.startY)
        assertEquals(300, op.endX)
        assertEquals(400, op.endY)
    }

    @Test
    fun `handleCommand drag from coordinates case insensitive`() {
        val result = handler.handleCommand("DRAG FROM 50,100 TO 150,200")

        assertTrue(result.success)
        assertNotNull(result.operation)
        assertEquals(50, result.operation!!.startX)
        assertEquals(100, result.operation!!.startY)
        assertEquals(150, result.operation!!.endX)
        assertEquals(200, result.operation!!.endY)
    }

    @Test
    fun `handleCommand drag from large coordinates`() {
        val result = handler.handleCommand("drag from 1000,2000 to 500,1500")

        assertTrue(result.success)
        assertNotNull(result.operation)
        assertEquals(1000, result.operation!!.startX)
        assertEquals(2000, result.operation!!.startY)
        assertEquals(500, result.operation!!.endX)
        assertEquals(1500, result.operation!!.endY)
    }

    @Test
    fun `handleCommand drag from zero coordinates`() {
        val result = handler.handleCommand("drag from 0,0 to 100,100")

        assertTrue(result.success)
        assertNotNull(result.operation)
        assertEquals(0, result.operation!!.startX)
        assertEquals(0, result.operation!!.startY)
    }

    // ==================== handleCommand with invalid format Tests ====================

    @Test
    fun `handleCommand with invalid command returns error`() {
        val result = handler.handleCommand("swipe up")

        assertFalse(result.success)
        assertNull(result.operation)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Unknown drag command"))
    }

    @Test
    fun `handleCommand with incomplete drag from returns error`() {
        val result = handler.handleCommand("drag from 100,200")

        assertFalse(result.success)
        assertNull(result.operation)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Could not parse coordinates"))
    }

    @Test
    fun `handleCommand with invalid coordinates format returns error`() {
        val result = handler.handleCommand("drag from abc,def to ghi,jkl")

        assertFalse(result.success)
        assertNull(result.operation)
        assertNotNull(result.error)
    }

    @Test
    fun `handleCommand with empty string returns error`() {
        val result = handler.handleCommand("")

        assertFalse(result.success)
        assertNull(result.operation)
        assertNotNull(result.error)
    }

    @Test
    fun `handleCommand with just drag returns error`() {
        val result = handler.handleCommand("drag")

        assertFalse(result.success)
        assertNull(result.operation)
        assertNotNull(result.error)
    }

    // ==================== createDrag Tests ====================

    @Test
    fun `createDrag with valid coordinates succeeds`() {
        val result = handler.createDrag(100, 200, 300, 400)

        assertTrue(result.success)
        assertNotNull(result.operation)
        assertNull(result.error)

        val op = result.operation!!
        assertEquals(100, op.startX)
        assertEquals(200, op.startY)
        assertEquals(300, op.endX)
        assertEquals(400, op.endY)
        assertEquals(300L, op.duration) // Default duration
    }

    @Test
    fun `createDrag with custom duration`() {
        val result = handler.createDrag(0, 0, 100, 100, 500L)

        assertTrue(result.success)
        assertNotNull(result.operation)
        assertEquals(500L, result.operation!!.duration)
    }

    @Test
    fun `createDrag with zero coordinates succeeds`() {
        val result = handler.createDrag(0, 0, 0, 0)

        assertTrue(result.success)
        assertNotNull(result.operation)
    }

    @Test
    fun `createDrag with negative startX fails`() {
        val result = handler.createDrag(-1, 200, 300, 400)

        assertFalse(result.success)
        assertNull(result.operation)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Invalid coordinates"))
    }

    @Test
    fun `createDrag with negative startY fails`() {
        val result = handler.createDrag(100, -1, 300, 400)

        assertFalse(result.success)
        assertNull(result.operation)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Invalid coordinates"))
    }

    @Test
    fun `createDrag with negative endX fails`() {
        val result = handler.createDrag(100, 200, -1, 400)

        assertFalse(result.success)
        assertNull(result.operation)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Invalid coordinates"))
    }

    @Test
    fun `createDrag with negative endY fails`() {
        val result = handler.createDrag(100, 200, 300, -1)

        assertFalse(result.success)
        assertNull(result.operation)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Invalid coordinates"))
    }

    @Test
    fun `createDrag with all negative coordinates fails`() {
        val result = handler.createDrag(-10, -20, -30, -40)

        assertFalse(result.success)
        assertNull(result.operation)
        assertNotNull(result.error)
    }

    // ==================== Direction.fromString Tests ====================

    @Test
    fun `Direction fromString UP returns UP`() {
        val direction = DragHandler.Direction.fromString("UP")

        assertNotNull(direction)
        assertEquals(DragHandler.Direction.UP, direction)
    }

    @Test
    fun `Direction fromString up lowercase returns UP`() {
        val direction = DragHandler.Direction.fromString("up")

        assertNotNull(direction)
        assertEquals(DragHandler.Direction.UP, direction)
    }

    @Test
    fun `Direction fromString DOWN returns DOWN`() {
        val direction = DragHandler.Direction.fromString("down")

        assertNotNull(direction)
        assertEquals(DragHandler.Direction.DOWN, direction)
    }

    @Test
    fun `Direction fromString LEFT returns LEFT`() {
        val direction = DragHandler.Direction.fromString("left")

        assertNotNull(direction)
        assertEquals(DragHandler.Direction.LEFT, direction)
    }

    @Test
    fun `Direction fromString RIGHT returns RIGHT`() {
        val direction = DragHandler.Direction.fromString("right")

        assertNotNull(direction)
        assertEquals(DragHandler.Direction.RIGHT, direction)
    }

    @Test
    fun `Direction fromString mixed case returns correct direction`() {
        assertEquals(DragHandler.Direction.UP, DragHandler.Direction.fromString("Up"))
        assertEquals(DragHandler.Direction.DOWN, DragHandler.Direction.fromString("DoWn"))
        assertEquals(DragHandler.Direction.LEFT, DragHandler.Direction.fromString("LEFT"))
        assertEquals(DragHandler.Direction.RIGHT, DragHandler.Direction.fromString("RiGhT"))
    }

    @Test
    fun `Direction fromString invalid returns null`() {
        val direction = DragHandler.Direction.fromString("diagonal")

        assertNull(direction)
    }

    @Test
    fun `Direction fromString empty string returns null`() {
        val direction = DragHandler.Direction.fromString("")

        assertNull(direction)
    }

    @Test
    fun `Direction fromString whitespace returns null`() {
        val direction = DragHandler.Direction.fromString("  ")

        assertNull(direction)
    }

    @Test
    fun `Direction fromString with typo returns null`() {
        val direction = DragHandler.Direction.fromString("upp")

        assertNull(direction)
    }

    // ==================== DragOperation Tests ====================

    @Test
    fun `DragOperation default duration is 300ms`() {
        val operation = DragOperation(0, 0, 100, 100)

        assertEquals(300L, operation.duration)
    }

    @Test
    fun `DragOperation with custom duration`() {
        val operation = DragOperation(0, 0, 100, 100, 1000L)

        assertEquals(1000L, operation.duration)
    }

    @Test
    fun `DragOperation data class equality`() {
        val op1 = DragOperation(10, 20, 30, 40, 500L)
        val op2 = DragOperation(10, 20, 30, 40, 500L)

        assertEquals(op1, op2)
    }

    // ==================== DragResult Tests ====================

    @Test
    fun `DragResult success case`() {
        val operation = DragOperation(100, 200, 300, 400)
        val result = DragResult(success = true, operation = operation)

        assertTrue(result.success)
        assertNotNull(result.operation)
        assertNull(result.error)
    }

    @Test
    fun `DragResult failure case`() {
        val result = DragResult(success = false, error = "Some error")

        assertFalse(result.success)
        assertNull(result.operation)
        assertEquals("Some error", result.error)
    }

    @Test
    fun `DragResult default values`() {
        val result = DragResult(success = false)

        assertFalse(result.success)
        assertNull(result.operation)
        assertNull(result.error)
    }

    // ==================== Directional drag with custom distance Tests ====================

    @Test
    fun `handleCommand drag up with pixel distance`() {
        val result = handler.handleCommand("drag up 300")

        assertTrue(result.success)
        assertNotNull(result.operation)

        val op = result.operation!!
        assertEquals(540, op.startX)
        assertEquals(960, op.startY)
        assertEquals(540, op.endX)
        assertEquals(660, op.endY) // 960 - 300 = 660
    }

    @Test
    fun `handleCommand drag down with pixel distance`() {
        val result = handler.handleCommand("drag down 150")

        assertTrue(result.success)
        assertNotNull(result.operation)

        val op = result.operation!!
        assertEquals(960, op.startY)
        assertEquals(1110, op.endY) // 960 + 150 = 1110
    }

    @Test
    fun `handleCommand drag left with pixel distance`() {
        val result = handler.handleCommand("drag left 100")

        assertTrue(result.success)
        assertNotNull(result.operation)

        val op = result.operation!!
        assertEquals(540, op.startX)
        assertEquals(440, op.endX) // 540 - 100 = 440
    }

    @Test
    fun `handleCommand drag right with pixel distance`() {
        val result = handler.handleCommand("drag right 250")

        assertTrue(result.success)
        assertNotNull(result.operation)

        val op = result.operation!!
        assertEquals(540, op.startX)
        assertEquals(790, op.endX) // 540 + 250 = 790
    }

    // ==================== DEFAULT_DRAG_DISTANCE Tests ====================

    @Test
    fun `default drag distance is used when no distance specified`() {
        val resultUp = handler.handleCommand("drag up")
        val resultDown = handler.handleCommand("drag down")

        assertTrue(resultUp.success)
        assertTrue(resultDown.success)

        // Default distance is 200
        val expectedUpEndY = 960 - DragHandler.DEFAULT_DRAG_DISTANCE
        val expectedDownEndY = 960 + DragHandler.DEFAULT_DRAG_DISTANCE

        assertEquals(expectedUpEndY, resultUp.operation!!.endY)
        assertEquals(expectedDownEndY, resultDown.operation!!.endY)
    }

    @Test
    fun `DEFAULT_DRAG_DISTANCE constant equals 200`() {
        assertEquals(200, DragHandler.DEFAULT_DRAG_DISTANCE)
    }
}
