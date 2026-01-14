/**
 * ScreenHierarchySerialization.kt - AVU Format Serialization Support
 *
 * Provides AVU (Avanues Universal) format serialization for screen hierarchy
 * enabling export to external systems, storage, and AI/NLU integration.
 *
 * Created: 2025-12-06
 * Part of: Universal Screen Hierarchy System
 *
 * @author IDEACODE v10.3
 */

package com.augmentalis.avaelements.context

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * AVU format serialization utilities for screen hierarchy
 *
 * Provides conversion to AVU (Avanues Universal) format for:
 * - Storage and caching (.avu files)
 * - Network transmission
 * - AI/NLU system integration
 * - Debugging and logging
 *
 * Example usage:
 * ```kotlin
 * // Serialize to AVU format
 * val avu = ScreenHierarchySerializer.toAvu(hierarchy)
 *
 * // Serialize quantized (AI-optimized)
 * val quantizedAvu = ScreenHierarchySerializer.toQuantizedAvu(hierarchy)
 * ```
 */
object ScreenHierarchySerializer {

    /**
     * AVU format configuration (JSON-based with avu-1.0 schema)
     */
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val compactJson = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    /**
     * Serialize screen hierarchy to AVU format
     *
     * @param hierarchy Screen hierarchy to serialize
     * @param pretty Use pretty printing (default: true)
     * @return AVU format string
     */
    fun toAvu(hierarchy: ScreenHierarchy, pretty: Boolean = true): String {
        val serializable = hierarchy.toSerializable()
        return if (pretty) {
            json.encodeToString(serializable)
        } else {
            compactJson.encodeToString(serializable)
        }
    }

    /**
     * Serialize quantized screen to AVU format (AI-optimized)
     *
     * @param hierarchy Screen hierarchy
     * @param pretty Use pretty printing (default: true)
     * @return AVU format string
     */
    fun toQuantizedAvu(hierarchy: ScreenHierarchy, pretty: Boolean = true): String {
        val quantized = ScreenQuantizer.quantize(hierarchy)
        return if (pretty) {
            json.encodeToString(quantized)
        } else {
            compactJson.encodeToString(quantized)
        }
    }

    /**
     * Serialize NLU context to AVU format
     *
     * @param context NLU context
     * @param pretty Use pretty printing (default: false for efficiency)
     * @return AVU format string
     */
    fun toNLUAvu(context: NLUContext, pretty: Boolean = false): String {
        return if (pretty) {
            json.encodeToString(context)
        } else {
            compactJson.encodeToString(context)
        }
    }

    /**
     * Serialize component node tree to AVU format
     *
     * @param node Root component node
     * @param pretty Use pretty printing (default: true)
     * @return AVU format string
     */
    fun componentTreeToAvu(node: ComponentNode, pretty: Boolean = true): String {
        val serializable = node.toSerializable()
        return if (pretty) {
            json.encodeToString(serializable)
        } else {
            compactJson.encodeToString(serializable)
        }
    }

    // Legacy JSON method names (deprecated, redirect to AVU methods)
    @Deprecated("Use toAvu() instead", ReplaceWith("toAvu(hierarchy, pretty)"))
    fun toJson(hierarchy: ScreenHierarchy, pretty: Boolean = true): String = toAvu(hierarchy, pretty)

    @Deprecated("Use toQuantizedAvu() instead", ReplaceWith("toQuantizedAvu(hierarchy, pretty)"))
    fun toQuantizedJson(hierarchy: ScreenHierarchy, pretty: Boolean = true): String = toQuantizedAvu(hierarchy, pretty)

    @Deprecated("Use toNLUAvu() instead", ReplaceWith("toNLUAvu(context, pretty)"))
    fun toNLUJson(context: NLUContext, pretty: Boolean = false): String = toNLUAvu(context, pretty)

    @Deprecated("Use componentTreeToAvu() instead", ReplaceWith("componentTreeToAvu(node, pretty)"))
    fun componentTreeToJson(node: ComponentNode, pretty: Boolean = true): String = componentTreeToAvu(node, pretty)
}

/**
 * Convert ScreenHierarchy to serializable representation
 */
private fun ScreenHierarchy.toSerializable(): SerializableScreenHierarchy {
    return SerializableScreenHierarchy(
        screenId = screenId,
        screenHash = screenHash,
        screenType = screenType.name,
        screenPurpose = screenPurpose,
        primaryAction = primaryAction?.name,
        appContext = appContext.toSerializable(),
        navigationContext = navigationContext.toSerializable(),
        root = root.toSerializable(),
        commandableElements = commandableElements.map { it.toSerializable() },
        formFields = formFields.map { it.toSerializable() },
        actions = actions.map { it.toSerializable() },
        dataDisplay = dataDisplay.map { it.toSerializable() },
        timestamp = timestamp,
        complexity = complexity.toSerializable()
    )
}

/**
 * Convert ComponentNode to serializable representation
 */
private fun ComponentNode.toSerializable(): SerializableComponentNode {
    return SerializableComponentNode(
        id = id,
        type = type,
        role = role.name,
        bounds = bounds?.toSerializable(),
        voiceLabel = voiceLabel,
        voiceCommands = voiceCommands,
        text = text,
        contentDescription = contentDescription,
        value = value?.toString(),
        isEnabled = isEnabled,
        isVisible = isVisible,
        isFocused = isFocused,
        isInteractive = isInteractive,
        children = children.map { it.toSerializable() },
        parent = parent,
        depth = depth
    )
}

private fun Rectangle.toSerializable() = SerializableRectangle(left, top, right, bottom)
private fun AppContext.toSerializable() = SerializableAppContext(appId, appName, packageName, activityName, category)
private fun NavigationContext.toSerializable() = SerializableNavigationContext(currentScreen, previousScreen, navigationStack, canNavigateBack, canNavigateForward)
private fun CommandableElement.toSerializable() = SerializableCommandableElement(id, voiceLabel, componentType, primaryCommand, alternateCommands, parameters, priority)
private fun FormField.toSerializable() = SerializableFormField(id, fieldType.name, label, placeholder, currentValue, isRequired, voiceLabel)
private fun ActionElement.toSerializable() = SerializableActionElement(id, actionType.name, label, destination, confirmationRequired, voiceCommand)
private fun DataElement.toSerializable() = SerializableDataElement(id, dataType.name, content, semanticMeaning, isScrollable)
private fun ComplexityScore.toSerializable() = SerializableComplexityScore(totalComponents, interactiveComponents, maxDepth, formFieldCount, actionCount)

/**
 * Serializable data classes
 */

@Serializable
data class SerializableScreenHierarchy(
    val screenId: String,
    val screenHash: String,
    val screenType: String,
    val screenPurpose: String?,
    val primaryAction: String?,
    val appContext: SerializableAppContext,
    val navigationContext: SerializableNavigationContext,
    val root: SerializableComponentNode,
    val commandableElements: List<SerializableCommandableElement>,
    val formFields: List<SerializableFormField>,
    val actions: List<SerializableActionElement>,
    val dataDisplay: List<SerializableDataElement>,
    val timestamp: Long,
    val complexity: SerializableComplexityScore
)

@Serializable
data class SerializableComponentNode(
    val id: String,
    val type: String,
    val role: String,
    val bounds: SerializableRectangle?,
    val voiceLabel: String?,
    val voiceCommands: List<String>,
    val text: String?,
    val contentDescription: String?,
    val value: String?,
    val isEnabled: Boolean,
    val isVisible: Boolean,
    val isFocused: Boolean,
    val isInteractive: Boolean,
    val children: List<SerializableComponentNode>,
    val parent: String?,
    val depth: Int
)

@Serializable
data class SerializableRectangle(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Serializable
data class SerializableAppContext(
    val appId: String,
    val appName: String,
    val packageName: String,
    val activityName: String?,
    val category: String?
)

@Serializable
data class SerializableNavigationContext(
    val currentScreen: String,
    val previousScreen: String?,
    val navigationStack: List<String>,
    val canNavigateBack: Boolean,
    val canNavigateForward: Boolean
)

@Serializable
data class SerializableCommandableElement(
    val id: String,
    val voiceLabel: String,
    val componentType: String,
    val primaryCommand: String,
    val alternateCommands: List<String>,
    val parameters: Map<String, String>,
    val priority: Int
)

@Serializable
data class SerializableFormField(
    val id: String,
    val fieldType: String,
    val label: String?,
    val placeholder: String?,
    val currentValue: String?,
    val isRequired: Boolean,
    val voiceLabel: String?
)

@Serializable
data class SerializableActionElement(
    val id: String,
    val actionType: String,
    val label: String,
    val destination: String?,
    val confirmationRequired: Boolean,
    val voiceCommand: String
)

@Serializable
data class SerializableDataElement(
    val id: String,
    val dataType: String,
    val content: String?,
    val semanticMeaning: String?,
    val isScrollable: Boolean
)

@Serializable
data class SerializableComplexityScore(
    val totalComponents: Int,
    val interactiveComponents: Int,
    val maxDepth: Int,
    val formFieldCount: Int,
    val actionCount: Int
)
