package com.augmentalis.avaelements.flutter.material.input

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * RichTextEditor component - Flutter Material parity
 *
 * A WYSIWYG rich text editor with formatting toolbar for creating styled content.
 * Supports bold, italic, underline, lists, links, and more.
 *
 * **Flutter Equivalent:** `QuillEditor` (from flutter_quill)
 * **Material Design 3:** Custom implementation with formatting toolbar
 *
 * ## Features
 * - WYSIWYG editing
 * - Formatting toolbar (bold, italic, underline, etc.)
 * - Lists (ordered/unordered)
 * - Links and images
 * - Undo/redo support
 * - HTML/Delta output
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * RichTextEditor(
 *     value = "<p>Hello <strong>world</strong>!</p>",
 *     label = "Content",
 *     placeholder = "Enter your text...",
 *     onValueChange = { html ->
 *         // Handle content change
 *     },
 *     showToolbar = true
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property value Current content (HTML or Delta JSON)
 * @property label Label text displayed above the editor
 * @property placeholder Placeholder text when empty
 * @property enabled Whether the editor is enabled
 * @property required Whether the field is required
 * @property showToolbar Whether to show the formatting toolbar
 * @property minHeight Minimum height in dp
 * @property errorText Error message to display (null if valid)
 * @property contentDescription Accessibility description
 * @property onValueChange Callback invoked when content changes
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class RichTextEditor(
    override val type: String = "RichTextEditor",
    override val id: String? = null,
    val value: String = "",
    val label: String? = null,
    val placeholder: String? = null,
    val enabled: Boolean = true,
    val required: Boolean = false,
    val showToolbar: Boolean = true,
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
        val base = contentDescription ?: label ?: "Rich text editor"
        val requiredState = if (required) ", required" else ""
        val errorState = if (errorText != null) ", error: $errorText" else ""
        return "$base$requiredState$errorState"
    }

    /**
     * Available formatting options
     */
    enum class Format {
        BOLD,
        ITALIC,
        UNDERLINE,
        STRIKETHROUGH,
        HEADING_1,
        HEADING_2,
        HEADING_3,
        BULLET_LIST,
        ORDERED_LIST,
        LINK,
        IMAGE,
        CODE_BLOCK,
        QUOTE,
        ALIGN_LEFT,
        ALIGN_CENTER,
        ALIGN_RIGHT
    }
}
