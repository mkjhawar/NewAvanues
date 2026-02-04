package com.augmentalis.avaelements.flutter.material.input

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * MarkdownEditor component - Flutter Material parity
 *
 * A markdown editor with live preview, syntax highlighting, and toolbar shortcuts.
 * Ideal for documentation, comments, and content creation.
 *
 * **Flutter Equivalent:** `MarkdownEditableTextInput` (from markdown_editable_textinput)
 * **Material Design 3:** Custom implementation with split view
 *
 * ## Features
 * - Markdown syntax support
 * - Live preview pane
 * - Syntax highlighting
 * - Toolbar with markdown shortcuts
 * - Split/tab view modes
 * - GitHub Flavored Markdown support
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * MarkdownEditor(
 *     value = "# Hello World\n\nThis is **bold** text.",
 *     label = "Documentation",
 *     placeholder = "Enter markdown...",
 *     onValueChange = { markdown ->
 *         // Handle markdown change
 *     },
 *     showPreview = true,
 *     splitView = true
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property value Current markdown content
 * @property label Label text displayed above the editor
 * @property placeholder Placeholder text when empty
 * @property enabled Whether the editor is enabled
 * @property required Whether the field is required
 * @property showPreview Whether to show live preview
 * @property splitView Show editor and preview side-by-side (vs tabs)
 * @property minHeight Minimum height in dp
 * @property errorText Error message to display (null if valid)
 * @property contentDescription Accessibility description
 * @property onValueChange Callback invoked when content changes
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class MarkdownEditor(
    override val type: String = "MarkdownEditor",
    override val id: String? = null,
    val value: String = "",
    val label: String? = null,
    val placeholder: String? = null,
    val enabled: Boolean = true,
    val required: Boolean = false,
    val showPreview: Boolean = true,
    val splitView: Boolean = false,
    val minHeight: Float = 200f,
    val errorText: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onValueChange: ((String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: label ?: "Markdown editor"
        val requiredState = if (required) ", required" else ""
        val errorState = if (errorText != null) ", error: $errorText" else ""
        val previewState = if (showPreview) ", with preview" else ""
        return "$base$requiredState$errorState$previewState"
    }

    /**
     * Markdown toolbar actions
     */
    enum class Action {
        BOLD,
        ITALIC,
        HEADING,
        LINK,
        IMAGE,
        CODE,
        CODE_BLOCK,
        QUOTE,
        BULLET_LIST,
        ORDERED_LIST,
        CHECKBOX,
        TABLE,
        HORIZONTAL_RULE
    }
}
