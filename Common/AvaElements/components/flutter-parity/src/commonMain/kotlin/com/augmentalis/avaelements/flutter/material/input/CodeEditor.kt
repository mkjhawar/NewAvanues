package com.augmentalis.avaelements.flutter.material.input

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * CodeEditor component - Flutter Material parity
 *
 * A code editor with syntax highlighting, line numbers, and code-specific features.
 * Supports multiple programming languages with customizable themes.
 *
 * **Flutter Equivalent:** `CodeField` (from code_text_field)
 * **Material Design 3:** Custom implementation with monospace font
 *
 * ## Features
 * - Syntax highlighting for multiple languages
 * - Line numbers
 * - Code folding
 * - Auto-indentation
 * - Bracket matching
 * - Tab/space handling
 * - Multiple color themes
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * CodeEditor(
 *     value = "fun main() {\n    println(\"Hello\")\n}",
 *     language = "kotlin",
 *     label = "Code",
 *     placeholder = "Enter code...",
 *     onValueChange = { code ->
 *         // Handle code change
 *     },
 *     showLineNumbers = true,
 *     theme = "monokai"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property value Current code content
 * @property language Programming language for syntax highlighting
 * @property label Label text displayed above the editor
 * @property placeholder Placeholder text when empty
 * @property enabled Whether the editor is enabled
 * @property required Whether the field is required
 * @property showLineNumbers Whether to show line numbers
 * @property theme Color theme name (e.g., "monokai", "github", "dracula")
 * @property minHeight Minimum height in dp
 * @property errorText Error message to display (null if valid)
 * @property contentDescription Accessibility description
 * @property onValueChange Callback invoked when code changes
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class CodeEditor(
    override val type: String = "CodeEditor",
    override val id: String? = null,
    val value: String = "",
    val language: String = "kotlin",
    val label: String? = null,
    val placeholder: String? = null,
    val enabled: Boolean = true,
    val required: Boolean = false,
    val showLineNumbers: Boolean = true,
    val theme: String = "default",
    val minHeight: Float = 300f,
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
        val base = contentDescription ?: label ?: "Code editor"
        val languageInfo = ", $language"
        val requiredState = if (required) ", required" else ""
        val errorState = if (errorText != null) ", error: $errorText" else ""
        return "$base$languageInfo$requiredState$errorState"
    }

    companion object {
        /**
         * Supported programming languages
         */
        val SUPPORTED_LANGUAGES = listOf(
            "kotlin", "java", "javascript", "typescript", "python",
            "go", "rust", "c", "cpp", "csharp", "swift",
            "html", "css", "json", "xml", "sql", "yaml", "markdown"
        )

        /**
         * Available color themes
         */
        val THEMES = listOf(
            "default", "monokai", "github", "dracula", "tomorrow-night",
            "solarized-light", "solarized-dark", "vs-code"
        )
    }
}
