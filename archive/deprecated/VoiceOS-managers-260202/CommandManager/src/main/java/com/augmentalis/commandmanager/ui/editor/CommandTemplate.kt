/**
 * CommandTemplate.kt - Template data models for pre-built commands
 *
 * Provides reusable command templates for common voice command patterns
 */

package com.augmentalis.commandmanager.ui.editor

import android.os.Parcelable
import com.augmentalis.commandmanager.registry.ActionType
import kotlinx.parcelize.Parcelize

/**
 * Command template for reusable command patterns
 */
@Parcelize
data class CommandTemplate(
    val id: String,
    val name: String,
    val category: TemplateCategory,
    val phrases: List<String>,
    val actionType: ActionType,
    val description: String,
    val defaultParams: Map<String, String> = emptyMap(),
    val priority: Int = 50,
    val namespace: String = "default",
    val icon: String? = null,
    val exampleUsage: String? = null,
    val tags: List<String> = emptyList()
) : Parcelable

/**
 * Template categories
 */
@Parcelize
enum class TemplateCategory(val displayName: String) : Parcelable {
    NAVIGATION("Navigation"),
    TEXT_EDITING("Text Editing"),
    SYSTEM("System Controls"),
    APP_SPECIFIC("App Specific"),
    ACCESSIBILITY("Accessibility"),
    MEDIA("Media Control"),
    PRODUCTIVITY("Productivity"),
    COMMUNICATION("Communication"),
    CUSTOM("Custom")
}

/**
 * Template builder for creating custom templates
 */
class TemplateBuilder {
    private var id: String = ""
    private var name: String = ""
    private var category: TemplateCategory = TemplateCategory.CUSTOM
    private val phrases = mutableListOf<String>()
    private var actionType: ActionType = ActionType.CUSTOM_ACTION
    private var description: String = ""
    private val defaultParams = mutableMapOf<String, String>()
    private var priority: Int = 50
    private var namespace: String = "default"
    private var icon: String? = null
    private var exampleUsage: String? = null
    private val tags = mutableListOf<String>()

    fun id(id: String) = apply { this.id = id }
    fun name(name: String) = apply { this.name = name }
    fun category(category: TemplateCategory) = apply { this.category = category }
    fun addPhrase(phrase: String) = apply { this.phrases.add(phrase) }
    fun phrases(vararg phrases: String) = apply { this.phrases.addAll(phrases) }
    fun actionType(type: ActionType) = apply { this.actionType = type }
    fun description(description: String) = apply { this.description = description }
    fun defaultParam(key: String, value: String) = apply { this.defaultParams[key] = value }
    fun priority(priority: Int) = apply { this.priority = priority }
    fun namespace(namespace: String) = apply { this.namespace = namespace }
    fun icon(icon: String) = apply { this.icon = icon }
    fun exampleUsage(usage: String) = apply { this.exampleUsage = usage }
    fun addTag(tag: String) = apply { this.tags.add(tag) }

    fun build(): CommandTemplate {
        require(id.isNotBlank()) { "Template ID cannot be blank" }
        require(name.isNotBlank()) { "Template name cannot be blank" }
        require(phrases.isNotEmpty()) { "Template must have at least one phrase" }
        require(description.isNotBlank()) { "Template description cannot be blank" }

        return CommandTemplate(
            id = id,
            name = name,
            category = category,
            phrases = phrases.toList(),
            actionType = actionType,
            description = description,
            defaultParams = defaultParams.toMap(),
            priority = priority,
            namespace = namespace,
            icon = icon,
            exampleUsage = exampleUsage,
            tags = tags.toList()
        )
    }
}

/**
 * Extension function to create template from builder
 */
fun commandTemplate(init: TemplateBuilder.() -> Unit): CommandTemplate {
    return TemplateBuilder().apply(init).build()
}

/**
 * Template collection for organizing templates
 */
data class TemplateCollection(
    val name: String,
    val description: String,
    val templates: List<CommandTemplate>,
    val category: TemplateCategory? = null
)

/**
 * Template search filter
 */
data class TemplateFilter(
    val category: TemplateCategory? = null,
    val searchQuery: String? = null,
    val tags: List<String> = emptyList(),
    val actionType: ActionType? = null
)

/**
 * Template customization data
 */
data class TemplateCustomization(
    val templateId: String,
    val customPhrases: List<String> = emptyList(),
    val customParams: Map<String, String> = emptyMap(),
    val customPriority: Int? = null,
    val customNamespace: String? = null,
    val customName: String? = null
)
