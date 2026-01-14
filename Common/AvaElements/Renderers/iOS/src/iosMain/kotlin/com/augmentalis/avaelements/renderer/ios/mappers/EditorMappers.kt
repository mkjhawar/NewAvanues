package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.flutter.material.input.*

/**
 * iOS SwiftUI Mappers for Flutter Editor Components
 *
 * This file maps cross-platform Flutter editor components to iOS SwiftUI
 * bridge representations. The SwiftUI bridge models are consumed by Swift
 * code to render native iOS UI.
 *
 * Architecture:
 * Flutter Component → iOS Mapper → SwiftUIView Bridge → Swift → Native SwiftUI
 *
 * Components Implemented:
 * - RichTextEditor: WYSIWYG editor with formatting toolbar
 * - MarkdownEditor: Markdown editor with live preview
 * - CodeEditor: Code editor with syntax highlighting
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Maps RichTextEditor to SwiftUI TextEditor with formatting toolbar
 *
 * Creates a rich text editing experience with:
 * - WYSIWYG editing interface
 * - Formatting toolbar (bold, italic, underline, lists, etc.)
 * - Support for links and images
 * - HTML/Delta output format
 * - Material Design 3 styling
 *
 * Visual parity with Flutter Quill editor maintained
 */
object RichTextEditorMapper {
    fun map(
        component: RichTextEditor,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Add label if present
        val labelText = component.label
        if (labelText != null) {
            children.add(
                SwiftUIView.text(
                    content = labelText,
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(14f),
                        SwiftUIModifier.fontWeight(FontWeight.Medium),
                        SwiftUIModifier.foregroundColor(
                            theme?.let {
                                SwiftUIColor.rgb(
                                    it.colorScheme.onSurface.red / 255f,
                                    it.colorScheme.onSurface.green / 255f,
                                    it.colorScheme.onSurface.blue / 255f,
                                    it.colorScheme.onSurface.alpha
                                )
                            } ?: SwiftUIColor.primary
                        )
                    )
                )
            )
        }

        // Build toolbar options if showToolbar is true
        val toolbarOptions = if (component.showToolbar) {
            listOf(
                "bold", "italic", "underline", "strikethrough",
                "heading1", "heading2", "heading3",
                "bulletList", "orderedList",
                "link", "image", "codeBlock", "quote",
                "alignLeft", "alignCenter", "alignRight"
            )
        } else {
            emptyList()
        }

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add background color from theme
        theme?.let {
            modifiers.add(
                SwiftUIModifier.background(
                    SwiftUIColor.rgb(
                        it.colorScheme.surface.red / 255f,
                        it.colorScheme.surface.green / 255f,
                        it.colorScheme.surface.blue / 255f,
                        it.colorScheme.surface.alpha
                    )
                )
            )
        } ?: modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("systemBackground")))

        // Add minimum height
        modifiers.add(
            SwiftUIModifier.frame(
                width = null,
                height = SizeValue.Fixed(component.minHeight),
                alignment = ZStackAlignment.TopLeading
            )
        )

        // Add corner radius
        modifiers.add(SwiftUIModifier.cornerRadius(8f))

        // Add border
        theme?.let {
            modifiers.add(
                SwiftUIModifier.border(
                    SwiftUIColor.rgb(
                        it.colorScheme.outline.red / 255f,
                        it.colorScheme.outline.green / 255f,
                        it.colorScheme.outline.blue / 255f,
                        it.colorScheme.outline.alpha
                    ),
                    1f
                )
            )
        } ?: modifiers.add(SwiftUIModifier.border(SwiftUIColor.system("separator"), 1f))

        // Add padding
        modifiers.add(SwiftUIModifier.padding(12f))

        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        return SwiftUIView(
            type = ViewType.RichTextEditor,
            id = component.id,
            properties = mapOf(
                "value" to component.value,
                "placeholder" to (component.placeholder ?: ""),
                "enabled" to component.enabled,
                "required" to component.required,
                "showToolbar" to component.showToolbar,
                "toolbarOptions" to toolbarOptions,
                "errorText" to (component.errorText ?: ""),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = modifiers
        )
    }
}

/**
 * Maps MarkdownEditor to SwiftUI split-pane editor with preview
 *
 * Creates a markdown editing experience with:
 * - Live markdown preview
 * - Syntax highlighting for markdown
 * - Split view (editor + preview) or tab mode
 * - Toolbar with markdown shortcuts
 * - GitHub Flavored Markdown support
 *
 * Visual parity with Flutter markdown_editable_textinput maintained
 */
object MarkdownEditorMapper {
    fun map(
        component: MarkdownEditor,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Add label if present
        val labelText = component.label
        if (labelText != null) {
            children.add(
                SwiftUIView.text(
                    content = labelText,
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(14f),
                        SwiftUIModifier.fontWeight(FontWeight.Medium),
                        SwiftUIModifier.foregroundColor(
                            theme?.let {
                                SwiftUIColor.rgb(
                                    it.colorScheme.onSurface.red / 255f,
                                    it.colorScheme.onSurface.green / 255f,
                                    it.colorScheme.onSurface.blue / 255f,
                                    it.colorScheme.onSurface.alpha
                                )
                            } ?: SwiftUIColor.primary
                        )
                    )
                )
            )
        }

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add background color from theme
        theme?.let {
            modifiers.add(
                SwiftUIModifier.background(
                    SwiftUIColor.rgb(
                        it.colorScheme.surface.red / 255f,
                        it.colorScheme.surface.green / 255f,
                        it.colorScheme.surface.blue / 255f,
                        it.colorScheme.surface.alpha
                    )
                )
            )
        } ?: modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("systemBackground")))

        // Add minimum height
        modifiers.add(
            SwiftUIModifier.frame(
                width = null,
                height = SizeValue.Fixed(component.minHeight),
                alignment = ZStackAlignment.TopLeading
            )
        )

        // Add corner radius
        modifiers.add(SwiftUIModifier.cornerRadius(8f))

        // Add border
        theme?.let {
            modifiers.add(
                SwiftUIModifier.border(
                    SwiftUIColor.rgb(
                        it.colorScheme.outline.red / 255f,
                        it.colorScheme.outline.green / 255f,
                        it.colorScheme.outline.blue / 255f,
                        it.colorScheme.outline.alpha
                    ),
                    1f
                )
            )
        } ?: modifiers.add(SwiftUIModifier.border(SwiftUIColor.system("separator"), 1f))

        // Add padding
        modifiers.add(SwiftUIModifier.padding(12f))

        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        return SwiftUIView(
            type = ViewType.MarkdownEditor,
            id = component.id,
            properties = mapOf(
                "value" to component.value,
                "placeholder" to (component.placeholder ?: ""),
                "enabled" to component.enabled,
                "required" to component.required,
                "showPreview" to component.showPreview,
                "splitView" to component.splitView,
                "syntaxHighlight" to true,
                "errorText" to (component.errorText ?: ""),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = modifiers
        )
    }
}

/**
 * Maps CodeEditor to SwiftUI code editing view with syntax highlighting
 *
 * Creates a code editing experience with:
 * - Syntax highlighting for multiple languages
 * - Line numbers
 * - Auto-indentation
 * - Bracket matching
 * - Multiple color themes
 * - Monospace font
 *
 * Visual parity with Flutter CodeField maintained
 */
object CodeEditorMapper {
    fun map(
        component: CodeEditor,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Add label if present
        val labelText = component.label
        if (labelText != null) {
            children.add(
                SwiftUIView.text(
                    content = labelText,
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(14f),
                        SwiftUIModifier.fontWeight(FontWeight.Medium),
                        SwiftUIModifier.foregroundColor(
                            theme?.let {
                                SwiftUIColor.rgb(
                                    it.colorScheme.onSurface.red / 255f,
                                    it.colorScheme.onSurface.green / 255f,
                                    it.colorScheme.onSurface.blue / 255f,
                                    it.colorScheme.onSurface.alpha
                                )
                            } ?: SwiftUIColor.primary
                        )
                    )
                )
            )
        }

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add background color from theme (darker for code editor)
        theme?.let {
            val isDark = it.colorScheme.mode == ColorScheme.ColorMode.Dark
            val bgColor = if (isDark) {
                SwiftUIColor.rgb(0.1f, 0.1f, 0.12f, 1.0f)
            } else {
                SwiftUIColor.rgb(0.95f, 0.95f, 0.97f, 1.0f)
            }
            modifiers.add(SwiftUIModifier.background(bgColor))
        } ?: modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("systemBackground")))

        // Add minimum height
        modifiers.add(
            SwiftUIModifier.frame(
                width = null,
                height = SizeValue.Fixed(component.minHeight),
                alignment = ZStackAlignment.TopLeading
            )
        )

        // Add corner radius
        modifiers.add(SwiftUIModifier.cornerRadius(8f))

        // Add border
        theme?.let {
            modifiers.add(
                SwiftUIModifier.border(
                    SwiftUIColor.rgb(
                        it.colorScheme.outline.red / 255f,
                        it.colorScheme.outline.green / 255f,
                        it.colorScheme.outline.blue / 255f,
                        it.colorScheme.outline.alpha
                    ),
                    1f
                )
            )
        } ?: modifiers.add(SwiftUIModifier.border(SwiftUIColor.system("separator"), 1f))

        // Add padding
        modifiers.add(SwiftUIModifier.padding(12f))

        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        return SwiftUIView(
            type = ViewType.CodeEditor,
            id = component.id,
            properties = mapOf(
                "code" to component.value,
                "language" to component.language,
                "placeholder" to (component.placeholder ?: ""),
                "enabled" to component.enabled,
                "required" to component.required,
                "showLineNumbers" to component.showLineNumbers,
                "theme" to component.theme,
                "readOnly" to !component.enabled,
                "errorText" to (component.errorText ?: ""),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = modifiers
        )
    }
}
