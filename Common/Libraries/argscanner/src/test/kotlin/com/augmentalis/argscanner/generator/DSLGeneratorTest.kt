package com.augmentalis.argscanner.generator

import com.augmentalis.argscanner.models.ARScanSession
import com.augmentalis.argscanner.models.DSLGenerationConfig
import com.augmentalis.argscanner.models.ScannedObject
import com.augmentalis.argscanner.models.SpatialRelationship
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DSLGenerator
 *
 * Tests:
 * - DSL generation for different layouts
 * - Component mapping
 * - Voice command integration
 * - Output formatting
 */
class DSLGeneratorTest {

    private lateinit var generator: DSLGenerator

    @Before
    fun setup() {
        generator = DSLGenerator()
    }

    @Test
    fun `test basic DSL generation - list layout`() {
        val session = createSession("TestScan")
        val objects = listOf(
            createObject("desk"),
            createObject("chair")
        )
        val config = DSLGenerationConfig(
            layoutStrategy = DSLGenerationConfig.LayoutStrategy.LIST,
            includeComments = false
        )

        val dsl = generator.generate(session, objects, emptyList(), config)

        assertTrue(dsl.contains("screen TestScanScreen"))
        assertTrue(dsl.contains("column(spacing: 16.0dp)"))
        assertTrue(dsl.contains("MagicRow"))  // desk → MagicRow
    }

    @Test
    fun `test grid layout generation`() {
        val session = createSession("GridTest")
        val objects = listOf(
            createObject("monitor"),
            createObject("keyboard"),
            createObject("mouse")
        )
        val config = DSLGenerationConfig(
            layoutStrategy = DSLGenerationConfig.LayoutStrategy.GRID,
            gridColumns = 2,
            includeComments = false
        )

        val dsl = generator.generate(session, objects, emptyList(), config)

        assertTrue(dsl.contains("grid(columns: 2, spacing: 16.0dp)"))
    }

    @Test
    fun `test component mapping - automatic`() {
        val config = DSLGenerationConfig(
            componentMappingStrategy = DSLGenerationConfig.ComponentMappingStrategy.AUTOMATIC
        )

        assertEquals("MagicRow", config.getComponentType("desk"))
        assertEquals("MagicCard", config.getComponentType("monitor"))
        assertEquals("MagicTextField", config.getComponentType("keyboard"))
        assertEquals("MagicColumn", config.getComponentType("shelf"))
    }

    @Test
    fun `test component mapping - custom`() {
        val config = DSLGenerationConfig(
            componentMappingStrategy = DSLGenerationConfig.ComponentMappingStrategy.MANUAL,
            customMappings = mapOf(
                "desk" -> "CustomDesk",
                "chair" -> "CustomChair"
            )
        )

        assertEquals("CustomDesk", config.getComponentType("desk"))
        assertEquals("CustomChair", config.getComponentType("chair"))
        assertNull(config.getComponentType("unknown"))
    }

    @Test
    fun `test voice commands inclusion`() {
        val session = createSession("VoiceTest")
        val obj = createObject(
            label = "monitor",
            voiceCommands = listOf("select monitor", "show monitor")
        )
        val config = DSLGenerationConfig(
            enableVoiceControl = true,
            includeComments = false
        )

        val dsl = generator.generate(session, listOf(obj), emptyList(), config)

        assertTrue(dsl.contains("voiceCommands:"))
        assertTrue(dsl.contains("select monitor"))
        assertTrue(dsl.contains("show monitor"))
    }

    @Test
    fun `test confidence score inclusion`() {
        val session = createSession("ConfidenceTest")
        val obj = createObject("desk", confidence = 0.92f)
        val config = DSLGenerationConfig(
            includeConfidenceScores = true,
            includeComments = false
        )

        val dsl = generator.generate(session, listOf(obj), emptyList(), config)

        assertTrue(dsl.contains("confidence: 0.92"))
    }

    @Test
    fun `test spatial data inclusion`() {
        val session = createSession("SpatialTest")
        val obj = ScannedObject(
            uuid = "test-1",
            sessionId = "test",
            label = "chair",
            confidence = 0.8f,
            position = ScannedObject.Position3D(1.5f, 0.5f, 2.0f),
            rotation = ScannedObject.Rotation3D(0f, 0f, 0f),
            boundingBox = ScannedObject.BoundingBox3D(0.5f, 1f, 0.5f)
        )
        val config = DSLGenerationConfig(
            includeSpatialData = true,
            includeComments = false
        )

        val dsl = generator.generate(session, listOf(obj), emptyList(), config)

        assertTrue(dsl.contains("position: [1.5, 0.5, 2.0]"))
    }

    @Test
    fun `test comment inclusion`() {
        val session = createSession("CommentTest")
        val obj = createObject("monitor", confidence = 0.85f)
        val config = DSLGenerationConfig(includeComments = true)

        val dsl = generator.generate(session, listOf(obj), emptyList(), config)

        assertTrue(dsl.contains("// AVAMagic UI DSL"))
        assertTrue(dsl.contains("// Generated from AR scan: CommentTest"))
        assertTrue(dsl.contains("// monitor (confidence: 85%)"))
    }

    @Test
    fun `test grouped layout generation`() {
        val session = createSession("GroupedTest")
        val desk = createObject("desk", position = pos(0f, 0f, 0f))
        val monitor = createObject("monitor", position = pos(0f, 0.5f, 0f))
        val keyboard = createObject("keyboard", position = pos(0f, 0f, 0.3f))

        val objects = listOf(desk, monitor, keyboard)
        val relationships = listOf(
            createRelationship("session", desk, monitor, isGrouped = true, groupType = SpatialRelationship.GroupType.WORKSPACE),
            createRelationship("session", desk, keyboard, isGrouped = true, groupType = SpatialRelationship.GroupType.WORKSPACE)
        )

        val config = DSLGenerationConfig(
            layoutStrategy = DSLGenerationConfig.LayoutStrategy.GROUPED,
            enableGrouping = true,
            includeComments = false
        )

        val dsl = generator.generate(session, objects, relationships, config)

        assertTrue(dsl.contains("group {"))
    }

    @Test
    fun `test filtering by confidence threshold`() {
        val config = DSLGenerationConfig(minConfidence = 0.7f)

        assertTrue(config.meetsConfidenceThreshold(0.8f))
        assertFalse(config.meetsConfidenceThreshold(0.6f))
    }

    @Test
    fun `test label exclusion`() {
        val config = DSLGenerationConfig(
            excludeLabels = listOf("mouse", "pen")
        )

        assertTrue(config.shouldExcludeLabel("mouse"))
        assertTrue(config.shouldExcludeLabel("pen"))
        assertFalse(config.shouldExcludeLabel("keyboard"))
    }

    @Test
    fun `test include-only labels`() {
        val config = DSLGenerationConfig(
            includeOnlyLabels = listOf("desk", "chair", "monitor")
        )

        assertFalse(config.shouldExcludeLabel("desk"))
        assertFalse(config.shouldExcludeLabel("chair"))
        assertTrue(config.shouldExcludeLabel("mouse"))
    }

    @Test
    fun `test preset config - indoor room`() {
        val config = DSLGenerationConfig.forIndoorRoom()

        assertEquals(DSLGenerationConfig.LayoutStrategy.SPATIAL, config.layoutStrategy)
        assertTrue(config.enableGrouping)
        assertEquals(1.5f, config.groupingThreshold, 0.01f)
        assertEquals(0.7f, config.minConfidence, 0.01f)
    }

    @Test
    fun `test preset config - workspace`() {
        val config = DSLGenerationConfig.forWorkspace()

        assertEquals(DSLGenerationConfig.LayoutStrategy.GROUPED, config.layoutStrategy)
        assertEquals(1.0f, config.groupingThreshold, 0.01f)  // Tighter grouping
        assertEquals(0.75f, config.minConfidence, 0.01f)  // Higher confidence
        assertTrue(config.includeOnlyLabels.contains("desk"))
    }

    @Test
    fun `test preset config - retail`() {
        val config = DSLGenerationConfig.forRetail()

        assertEquals(DSLGenerationConfig.LayoutStrategy.GRID, config.layoutStrategy)
        assertEquals(3, config.gridColumns)
        assertFalse(config.enableGrouping)
        assertEquals(0.8f, config.minConfidence, 0.01f)
    }

    // Helper methods
    private fun createSession(name: String) = ARScanSession.create(
        name = name,
        environment = ARScanSession.Environment.INDOOR
    )

    private fun createObject(
        label: String,
        confidence: Float = 0.8f,
        position: ScannedObject.Position3D = pos(0f, 0f, 0f),
        voiceCommands: List<String> = emptyList()
    ) = ScannedObject(
        uuid = java.util.UUID.randomUUID().toString(),
        sessionId = "test",
        label = label,
        confidence = confidence,
        position = position,
        rotation = ScannedObject.Rotation3D(0f, 0f, 0f),
        boundingBox = ScannedObject.BoundingBox3D(0.5f, 0.5f, 0.5f),
        voiceCommands = voiceCommands
    )

    private fun pos(x: Float, y: Float, z: Float) = ScannedObject.Position3D(x, y, z)

    private fun createRelationship(
        sessionId: String,
        source: ScannedObject,
        target: ScannedObject,
        isGrouped: Boolean = false,
        groupType: SpatialRelationship.GroupType? = null
    ) = SpatialRelationship(
        sessionId = sessionId,
        sourceObjectUuid = source.uuid,
        targetObjectUuid = target.uuid,
        distance = 0.5f,
        horizontalDistance = 0.3f,
        verticalDistance = 0.4f,
        relativePosition = SpatialRelationship.RelativePosition.ABOVE,
        proximityLevel = SpatialRelationship.ProximityLevel.CLOSE,
        isGrouped = isGrouped,
        groupType = groupType
    )
}
