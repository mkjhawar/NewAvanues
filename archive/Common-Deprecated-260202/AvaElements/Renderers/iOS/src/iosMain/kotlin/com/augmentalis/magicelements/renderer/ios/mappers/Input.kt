package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.material.input.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Flutter Material Input Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity advanced input components to SwiftUI equivalents.
 *
 * Components:
 * - PhoneInput → TextField with country code picker
 * - UrlInput → TextField with URL validation
 * - ComboBox → TextField with suggestions dropdown
 * - PinInput → HStack of individual digit fields
 * - OTPInput → HStack of OTP digit fields
 * - MaskInput → TextField with input mask
 * - RichTextEditor → TextEditor with formatting toolbar
 * - MarkdownEditor → TextEditor with markdown preview
 * - CodeEditor → TextEditor with syntax highlighting
 * - FormSection → VStack with section header
 * - MultiSelect → List with checkboxes
 *
 * @since 3.0.0-flutter-parity-ios
 */

// Note: PhoneInput, UrlInput, and ComboBox mappers exist in Phase3InputMappers.kt

// ============================================
// PIN INPUT
// ============================================

object PinInputMapper {
    fun map(component: PinInput, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Label
        component.label?.let { label ->
            children.add(SwiftUIView.text(
                content = label,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                )
            ))
        }

        // PIN digit boxes
        val digitBoxes = mutableListOf<SwiftUIView>()
        val digits = component.value.padEnd(component.length, ' ')

        for (i in 0 until component.length) {
            val digit = if (i < component.value.length) {
                if (component.masked) "●" else digits[i].toString()
            } else {
                ""
            }

            digitBoxes.add(SwiftUIView(
                type = ViewType.ZStack,
                properties = emptyMap(),
                children = listOf(
                    SwiftUIView.text(
                        content = digit,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Title),
                            SwiftUIModifier.fontWeight(FontWeight.Bold)
                        )
                    )
                ),
                modifiers = listOf(
                    SwiftUIModifier(ModifierType.Custom, mapOf("frame" to mapOf("width" to 48f, "height" to 56f))),
                    SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                    SwiftUIModifier.cornerRadius(8f),
                    SwiftUIModifier(ModifierType.Custom, mapOf("overlay" to mapOf(
                        "shape" to "RoundedRectangle",
                        "cornerRadius" to 8f,
                        "stroke" to if (component.errorText != null) SwiftUIColor.system("systemRed") else SwiftUIColor.system("separator")
                    )))
                )
            ))
        }

        children.add(SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 8f),
            children = digitBoxes
        ))

        // Error text
        component.errorText?.let { error ->
            children.add(SwiftUIView.text(
                content = error,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemRed"))
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = children,
        )
    }
}

// ============================================
// OTP INPUT
// ============================================

object OTPInputMapper {
    fun map(component: OTPInput, theme: Theme?): SwiftUIView {
        // OTP is similar to PIN, reuse the same pattern
        val children = mutableListOf<SwiftUIView>()

        component.label?.let { label ->
            children.add(SwiftUIView.text(
                content = label,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                )
            ))
        }

        // OTP digit boxes
        val digitBoxes = mutableListOf<SwiftUIView>()
        val digits = component.value.padEnd(component.length, ' ')

        for (i in 0 until component.length) {
            val digit = if (i < component.value.length) digits[i].toString() else ""

            digitBoxes.add(SwiftUIView(
                type = ViewType.ZStack,
                properties = emptyMap(),
                children = listOf(
                    SwiftUIView.text(
                        content = digit,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Title),
                            SwiftUIModifier.fontWeight(FontWeight.Bold)
                        )
                    )
                ),
                modifiers = listOf(
                    SwiftUIModifier(ModifierType.Custom, mapOf("frame" to mapOf("width" to 48f, "height" to 56f))),
                    SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                    SwiftUIModifier.cornerRadius(8f),
                    SwiftUIModifier(ModifierType.Custom, mapOf("overlay" to mapOf(
                        "shape" to "RoundedRectangle",
                        "cornerRadius" to 8f,
                        "stroke" to if (component.errorText != null) SwiftUIColor.system("systemRed") else SwiftUIColor.system("separator")
                    )))
                )
            ))
        }

        children.add(SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 8f),
            children = digitBoxes
        ))

        component.errorText?.let { error ->
            children.add(SwiftUIView.text(
                content = error,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemRed"))
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = children,
        )
    }
}

// ============================================
// MASK INPUT
// ============================================

object MaskInputMapper {
    fun map(component: MaskInput, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        component.label?.let { label ->
            children.add(SwiftUIView.text(
                content = label,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                )
            ))
        }

        // TextField with masked input
        children.add(SwiftUIView(
            type = ViewType.Custom("TextField"),
            properties = mapOf(
                "text" to component.value,
                "prompt" to (component.placeholder ?: component.mask)
            ),
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf("disabled" to !component.enabled))
            )
        ))

        component.errorText?.let { error ->
            children.add(SwiftUIView.text(
                content = error,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemRed"))
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 4f),
            children = children,
        )
    }
}

// ============================================
// RICH TEXT EDITOR
// ============================================

object RichTextEditorMapper {
    fun map(component: RichTextEditor, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        component.label?.let { label ->
            children.add(SwiftUIView.text(
                content = label,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                )
            ))
        }

        // Formatting toolbar
        if (component.showToolbar) {
            val toolbarButtons = listOf("bold", "italic", "underline", "link", "list.bullet", "list.number")
            val toolbar = SwiftUIView(
                type = ViewType.HStack,
                properties = mapOf("spacing" to 12f),
                children = toolbarButtons.map { icon ->
                    SwiftUIView(
                        type = ViewType.Custom("Button"),
                        properties = mapOf("systemImage" to icon),
                        modifiers = listOf(
                            SwiftUIModifier(ModifierType.Custom, mapOf("buttonStyle" to "borderless"))
                        )
                    )
                },
                modifiers = listOf(
                    SwiftUIModifier.padding(8f),
                    SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                    SwiftUIModifier.cornerRadius(8f)
                )
            )
            children.add(toolbar)
        }

        // Text editor
        children.add(SwiftUIView(
            type = ViewType.Custom("TextEditor"),
            properties = mapOf("text" to component.value),
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf("frame" to mapOf("minHeight" to 120f))),
                SwiftUIModifier.padding(8f),
                SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                SwiftUIModifier.cornerRadius(8f)
            )
        ))

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = children,
        )
    }
}

// ============================================
// MARKDOWN EDITOR
// ============================================

object MarkdownEditorMapper {
    fun map(component: MarkdownEditor, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        component.label?.let { label ->
            children.add(SwiftUIView.text(
                content = label,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                )
            ))
        }

        // Editor and preview tabs
        val tabs = SwiftUIView(
            type = ViewType.Custom("TabView"),
            properties = emptyMap(),
            children = listOf(
                // Editor tab
                SwiftUIView(
                    type = ViewType.Custom("TextEditor"),
                    properties = mapOf("text" to component.value),
                    modifiers = listOf(
                        SwiftUIModifier(ModifierType.Custom, mapOf("frame" to mapOf("minHeight" to 200f))),
                        SwiftUIModifier.padding(8f),
                        SwiftUIModifier(ModifierType.Custom, mapOf("tabItem" to mapOf("label" to "Edit", "systemImage" to "pencil")))
                    )
                ),
                // Preview tab
                SwiftUIView(
                    type = ViewType.Custom("ScrollView"),
                    properties = emptyMap(),
                    children = listOf(
                        SwiftUIView.text(
                            content = component.value,
                            modifiers = listOf(SwiftUIModifier.padding(8f))
                        )
                    ),
                    modifiers = listOf(
                        SwiftUIModifier(ModifierType.Custom, mapOf("frame" to mapOf("minHeight" to 200f))),
                        SwiftUIModifier(ModifierType.Custom, mapOf("tabItem" to mapOf("label" to "Preview", "systemImage" to "eye")))
                    )
                )
            )
        )

        children.add(tabs)

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = children,
        )
    }
}

// ============================================
// CODE EDITOR
// ============================================

object CodeEditorMapper {
    fun map(component: CodeEditor, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        component.label?.let { label ->
            children.add(SwiftUIView.text(
                content = label,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                )
            ))
        }

        // Language selector
        val header = SwiftUIView(
            type = ViewType.HStack,
            properties = emptyMap(),
            children = listOf(
                SwiftUIView.text(
                    content = component.language.uppercase(),
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.fontWeight(FontWeight.Semibold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                    )
                ),
                SwiftUIView(
                    type = ViewType.Custom("Spacer"),
                    properties = emptyMap()
                ),
                SwiftUIView(
                    type = ViewType.Custom("Button"),
                    properties = mapOf("label" to "Copy", "systemImage" to "doc.on.doc"),
                    modifiers = listOf(
                        SwiftUIModifier(ModifierType.Custom, mapOf("buttonStyle" to "borderless"))
                    )
                )
            ),
            modifiers = listOf(SwiftUIModifier.padding(8f))
        )
        children.add(header)

        // Code editor
        children.add(SwiftUIView(
            type = ViewType.Custom("TextEditor"),
            properties = mapOf("text" to component.value),
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.System),
                SwiftUIModifier(ModifierType.Custom, mapOf("frame" to mapOf("minHeight" to 200f))),
                SwiftUIModifier.padding(8f),
                SwiftUIModifier.background(SwiftUIColor.system("black")),
                SwiftUIModifier.cornerRadius(8f)
            )
        ))

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 0f),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                SwiftUIModifier.cornerRadius(8f)
            ),
        )
    }
}

// ============================================
// FORM SECTION
// ============================================

object FormSectionMapper {
    fun map(component: FormSection, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Header
        component.title?.let { title ->
            children.add(SwiftUIView.text(
                content = title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                )
            ))
        }

        component.description?.let { desc ->
            children.add(SwiftUIView.text(
                content = desc,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                )
            ))
        }

        // Form fields
        component.children.forEach { child ->
            children.add(renderChild(child))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 16f),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(16f),
                SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                SwiftUIModifier.cornerRadius(12f)
            ),
        )
    }
}

// ============================================
// MULTI SELECT
// ============================================

object MultiSelectMapper {
    fun map(component: MultiSelect, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        component.label?.let { label ->
            children.add(SwiftUIView.text(
                content = label,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                )
            ))
        }

        // Options list with checkboxes
        component.options.forEach { option ->
            val isSelected = component.selectedValues.contains(option)

            val row = SwiftUIView(
                type = ViewType.HStack,
                properties = mapOf("spacing" to 12f),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf(
                            "systemName" to if (isSelected) "checkmark.square.fill" else "square",
                            "size" to 20f
                        ),
                        modifiers = listOf(
                            SwiftUIModifier.foregroundColor(
                                if (isSelected) SwiftUIColor.primary else SwiftUIColor.system("secondaryLabel")
                            )
                        )
                    ),
                    SwiftUIView.text(
                        content = option,
                        modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
                    )
                ),
                modifiers = listOf(
                    SwiftUIModifier.padding(12f),
                    SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                    SwiftUIModifier.cornerRadius(8f)
                )
            )
            children.add(row)
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = children,
        )
    }
}
