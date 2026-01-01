package com.augmentalis.argscanner.generator

import com.augmentalis.argscanner.models.ARScanSession
import com.augmentalis.argscanner.models.DSLGenerationConfig
import com.augmentalis.argscanner.models.ScannedObject
import com.augmentalis.argscanner.models.SpatialRelationship

/**
 * DSLGenerator - Converts scanned AR objects to AVAMagic UI DSL
 *
 * Generates voice-first declarative UI code from spatial AR scan data:
 * - Maps detected objects to AVAMagic components
 * - Preserves spatial relationships in layout
 * - Integrates voice control commands
 * - Generates .vos format output
 *
 * Created by: Manoj Jhawar, manoj@ideahq.net
 */
class DSLGenerator {

    /**
     * Generate AVAMagic UI DSL from scan session
     *
     * @param session Scan session metadata
     * @param objects Detected and filtered objects
     * @param relationships Spatial relationships between objects
     * @param config Generation configuration
     * @return Generated DSL code
     */
    fun generate(
        session: ARScanSession,
        objects: List<ScannedObject>,
        relationships: List<SpatialRelationship>,
        config: DSLGenerationConfig
    ): String {
        val dsl = StringBuilder()

        // Header comment
        if (config.includeComments) {
            dsl.appendLine("// AVAMagic UI DSL")
            dsl.appendLine("// Generated from AR scan: ${session.name}")
            dsl.appendLine("// Session: ${session.sessionId}")
            dsl.appendLine("// Objects: ${objects.size}")
            if (config.includeTimestamps) {
                dsl.appendLine("// Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
            }
            dsl.appendLine()
        }

        // Screen declaration
        dsl.appendLine("screen ${session.name.replace(" ", "")}Screen {")
        dsl.appendLine()

        // Generate layout based on strategy
        when (config.layoutStrategy) {
            DSLGenerationConfig.LayoutStrategy.SPATIAL -> generateSpatialLayout(dsl, objects, relationships, config)
            DSLGenerationConfig.LayoutStrategy.GRID -> generateGridLayout(dsl, objects, config)
            DSLGenerationConfig.LayoutStrategy.LIST -> generateListLayout(dsl, objects, config)
            DSLGenerationConfig.LayoutStrategy.GROUPED -> generateGroupedLayout(dsl, objects, relationships, config)
            DSLGenerationConfig.LayoutStrategy.HIERARCHICAL -> generateHierarchicalLayout(dsl, objects, relationships, config)
        }

        dsl.appendLine("}")

        return if (config.prettifyOutput) {
            prettifyDSL(dsl.toString())
        } else {
            dsl.toString()
        }
    }

    /**
     * Generate spatial layout (preserves physical positioning)
     */
    private fun generateSpatialLayout(
        dsl: StringBuilder,
        objects: List<ScannedObject>,
        relationships: List<SpatialRelationship>,
        config: DSLGenerationConfig
    ) {
        if (config.includeComments) {
            dsl.appendLine("    // Spatial layout (preserves physical positions)")
        }

        dsl.appendLine("    layout(type: \"spatial\") {")
        dsl.appendLine()

        objects.forEach { obj ->
            generateComponent(dsl, obj, config, indent = 8)
        }

        dsl.appendLine("    }")
    }

    /**
     * Generate grid layout
     */
    private fun generateGridLayout(
        dsl: StringBuilder,
        objects: List<ScannedObject>,
        config: DSLGenerationConfig
    ) {
        if (config.includeComments) {
            dsl.appendLine("    // Grid layout (${config.gridColumns} columns)")
        }

        dsl.appendLine("    grid(columns: ${config.gridColumns}, spacing: ${config.spacing}dp) {")
        dsl.appendLine()

        objects.forEach { obj ->
            generateComponent(dsl, obj, config, indent = 8)
        }

        dsl.appendLine("    }")
    }

    /**
     * Generate list layout
     */
    private fun generateListLayout(
        dsl: StringBuilder,
        objects: List<ScannedObject>,
        config: DSLGenerationConfig
    ) {
        if (config.includeComments) {
            dsl.appendLine("    // Vertical list layout")
        }

        dsl.appendLine("    column(spacing: ${config.spacing}dp) {")
        dsl.appendLine()

        objects.forEach { obj ->
            generateComponent(dsl, obj, config, indent = 8)
        }

        dsl.appendLine("    }")
    }

    /**
     * Generate grouped layout (groups by spatial relationships)
     */
    private fun generateGroupedLayout(
        dsl: StringBuilder,
        objects: List<ScannedObject>,
        relationships: List<SpatialRelationship>,
        config: DSLGenerationConfig
    ) {
        if (config.includeComments) {
            dsl.appendLine("    // Grouped layout (by spatial relationships)")
        }

        // Group objects by relationships
        val groups = groupObjects(objects, relationships)

        groups.forEach { group ->
            if (group.size == 1) {
                // Single object, no group
                generateComponent(dsl, group.first(), config, indent = 4)
            } else {
                // Multiple objects, create group
                if (config.includeComments) {
                    dsl.appendLine("    // Group: ${group.size} objects")
                }
                dsl.appendLine("    group {")
                group.forEach { obj ->
                    generateComponent(dsl, obj, config, indent = 8)
                }
                dsl.appendLine("    }")
                dsl.appendLine()
            }
        }
    }

    /**
     * Generate hierarchical layout (parent-child relationships)
     */
    private fun generateHierarchicalLayout(
        dsl: StringBuilder,
        objects: List<ScannedObject>,
        relationships: List<SpatialRelationship>,
        config: DSLGenerationConfig
    ) {
        if (config.includeComments) {
            dsl.appendLine("    // Hierarchical layout (parent-child)")
        }

        // Build hierarchy (e.g., desk contains monitor + keyboard)
        val hierarchy = buildHierarchy(objects, relationships)

        hierarchy.forEach { node ->
            generateHierarchyNode(dsl, node, config, indent = 4)
        }
    }

    /**
     * Generate component DSL for a scanned object
     */
    private fun generateComponent(
        dsl: StringBuilder,
        obj: ScannedObject,
        config: DSLGenerationConfig,
        indent: Int
    ) {
        val indentStr = " ".repeat(indent)
        val componentType = config.getComponentType(obj.label) ?: "MagicCard"

        if (config.includeComments) {
            dsl.appendLine("$indentStr// ${obj.label} (confidence: ${(obj.confidence * 100).toInt()}%)")
        }

        dsl.appendLine("$indentStr$componentType(")
        dsl.appendLine("$indentStr    id: \"${obj.uuid}\",")
        dsl.appendLine("$indentStr    label: \"${obj.getVoiceFriendlyName()}\",")

        if (config.includeSpatialData) {
            dsl.appendLine("$indentStr    position: [${obj.position.x}, ${obj.position.y}, ${obj.position.z}],")
        }

        if (config.enableVoiceControl) {
            val voiceCommands = obj.voiceCommands.joinToString(", ") { "\"$it\"" }
            dsl.appendLine("$indentStr    voiceCommands: [$voiceCommands],")
        }

        if (config.includeConfidenceScores) {
            dsl.appendLine("$indentStr    confidence: ${obj.confidence},")
        }

        dsl.appendLine("$indentStr    onClick: { navigateTo(\"${obj.label}\") }")
        dsl.appendLine("$indentStr)")
        dsl.appendLine()
    }

    /**
     * Generate hierarchy node (parent with children)
     */
    private fun generateHierarchyNode(
        dsl: StringBuilder,
        node: HierarchyNode,
        config: DSLGenerationConfig,
        indent: Int
    ) {
        val indentStr = " ".repeat(indent)

        if (node.children.isEmpty()) {
            // Leaf node
            generateComponent(dsl, node.obj, config, indent)
        } else {
            // Parent node with children
            val componentType = config.getComponentType(node.obj.label) ?: "MagicColumn"

            if (config.includeComments) {
                dsl.appendLine("$indentStr// ${node.obj.label} with ${node.children.size} children")
            }

            dsl.appendLine("$indentStr$componentType(id: \"${node.obj.uuid}\") {")

            node.children.forEach { child ->
                generateHierarchyNode(dsl, child, config, indent + 4)
            }

            dsl.appendLine("$indentStr}")
            dsl.appendLine()
        }
    }

    /**
     * Group objects by spatial relationships
     */
    private fun groupObjects(
        objects: List<ScannedObject>,
        relationships: List<SpatialRelationship>
    ): List<List<ScannedObject>> {
        val groups = mutableListOf<MutableList<ScannedObject>>()
        val assigned = mutableSetOf<String>()

        // Find grouped relationships
        relationships.filter { it.isGrouped }.forEach { rel ->
            val sourceUuid = rel.sourceObjectUuid
            val targetUuid = rel.targetObjectUuid

            // Find existing group
            val group = groups.find { it.any { obj -> obj.uuid == sourceUuid || obj.uuid == targetUuid } }

            if (group != null) {
                // Add to existing group
                objects.find { it.uuid == sourceUuid }?.let {
                    if (!assigned.contains(it.uuid)) {
                        group.add(it)
                        assigned.add(it.uuid)
                    }
                }
                objects.find { it.uuid == targetUuid }?.let {
                    if (!assigned.contains(it.uuid)) {
                        group.add(it)
                        assigned.add(it.uuid)
                    }
                }
            } else {
                // Create new group
                val newGroup = mutableListOf<ScannedObject>()
                objects.find { it.uuid == sourceUuid }?.let {
                    newGroup.add(it)
                    assigned.add(it.uuid)
                }
                objects.find { it.uuid == targetUuid }?.let {
                    newGroup.add(it)
                    assigned.add(it.uuid)
                }
                if (newGroup.isNotEmpty()) {
                    groups.add(newGroup)
                }
            }
        }

        // Add ungrouped objects
        objects.filter { !assigned.contains(it.uuid) }.forEach { obj ->
            groups.add(mutableListOf(obj))
        }

        return groups
    }

    /**
     * Build hierarchy from relationships
     */
    private fun buildHierarchy(
        objects: List<ScannedObject>,
        relationships: List<SpatialRelationship>
    ): List<HierarchyNode> {
        val nodes = objects.map { HierarchyNode(it, mutableListOf()) }.toMutableList()
        val nodeMap = nodes.associateBy { it.obj.uuid }

        // Build parent-child relationships
        relationships.filter { it.proximityLevel == SpatialRelationship.ProximityLevel.VERY_CLOSE }.forEach { rel ->
            val parent = nodeMap[rel.sourceObjectUuid]
            val child = nodeMap[rel.targetObjectUuid]

            if (parent != null && child != null) {
                parent.children.add(child)
                nodes.remove(child)  // Remove from root level
            }
        }

        return nodes
    }

    /**
     * Prettify DSL output (proper indentation)
     */
    private fun prettifyDSL(dsl: String): String {
        // Already properly indented in generation
        return dsl
    }

    /**
     * Hierarchy node for parent-child relationships
     */
    private data class HierarchyNode(
        val obj: ScannedObject,
        val children: MutableList<HierarchyNode>
    )
}
