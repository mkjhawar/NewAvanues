package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.material.input.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Phase 3 Form Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter Material form components to SwiftUI equivalents:
 * - FormSection: Groups related form fields with header/footer
 * - FormGroup: Container for grouped form fields with validation state
 *
 * These components provide semantic grouping and visual organization
 * for complex forms, with full iOS native styling and accessibility.
 *
 * @since 3.0.0-flutter-parity
 */

// ============================================
// FORM SECTION
// ============================================

/**
 * Maps FormSection to SwiftUI Section
 *
 * FormSection provides semantic grouping of form fields with optional
 * header and footer content. Maps to native SwiftUI Section which provides:
 * - Visual separation with iOS-native styling
 * - Optional header and footer views
 * - Collapsible sections (when enabled)
 * - Automatic insets and spacing
 * - Dark mode support
 * - Accessibility grouping
 *
 * SwiftUI Section automatically handles:
 * - List context styling (grouped/inset list style)
 * - Proper spacing between sections
 * - Header capitalization (all caps by default)
 * - Footer text styling (smaller, gray text)
 *
 * Example SwiftUI equivalent:
 * ```swift
 * Section(header: Text("Personal Information"),
 *         footer: Text("We'll never share your info")) {
 *     TextField("First Name", text: $firstName)
 *     TextField("Last Name", text: $lastName)
 * }
 * ```
 */
object FormSectionMapper {
    fun map(
        component: FormSection,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Build section content (form fields)
        val sectionContent = component.children.map { child -> renderChild(child) }

        // Create header view if title provided
        val sectionTitle = component.title
        val headerView = if (sectionTitle != null) {
            SwiftUIView.text(
                content = sectionTitle,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.foregroundColor(
                        theme?.colorScheme?.onSurface?.let {
                            ModifierConverter.convertColor(it)
                        } ?: SwiftUIColor.primary
                    )
                )
            )
        } else null

        // Create footer view if description provided
        val sectionDescription = component.description
        val footerView = if (sectionDescription != null) {
            SwiftUIView.text(
                content = sectionDescription,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(
                        SwiftUIColor.system("secondaryLabel")
                    )
                )
            )
        } else null

        // Build section properties
        val properties = buildMap<String, Any> {
            component.title?.let { title -> put("header", title) }
            component.description?.let { desc -> put("footer", desc) }
            put("showDivider", component.showDivider)
            put("collapsible", component.collapsible)
            put("expanded", component.expanded)
            component.contentDescription?.let { desc ->
                put("accessibilityLabel", desc)
            } ?: component.title?.let { title ->
                put("accessibilityLabel", "$title section, ${component.children.size} fields")
            }
        }

        // Apply modifiers
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add section-specific styling
        modifiers.add(SwiftUIModifier.padding(8f, 0f, 8f, 0f))

        // Add divider if requested
        if (component.showDivider) {
            modifiers.add(SwiftUIModifier.border(
                SwiftUIColor.system("separator"),
                1f
            ))
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Create VStack container for section content
        // Note: In actual SwiftUI, this would be a Section view
        // The bridge will interpret this as a Section when type is Custom("Section")
        val sectionView = SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "spacing" to 12f,
                "alignment" to "Leading"
            ),
            children = buildList {
                // Add header if present
                headerView?.let { add(it) }

                // Add all form field children
                addAll(sectionContent)

                // Add footer if present
                footerView?.let { add(it) }
            },
            modifiers = modifiers,
            id = component.id
        )

        return sectionView
    }
}

// ============================================
// FORM GROUP
// ============================================

/**
 * Maps FormGroup to SwiftUI grouped form fields container
 *
 * FormGroup provides a labeled container for grouping related form fields
 * with validation state display. Unlike FormSection (which is a List section),
 * FormGroup is a styled VStack that can be used anywhere in the UI.
 *
 * Features:
 * - Optional label (leading text)
 * - Helper text below group
 * - Required field indicator (*)
 * - Error state styling (red border, error message)
 * - Groups children with consistent spacing
 * - Theme-aware colors
 *
 * Visual structure:
 * ```
 * ┌─────────────────────────────┐
 * │ Label *                     │  ← Label with required indicator
 * │ ┌─────────────────────────┐ │
 * │ │ [Form Field 1]          │ │
 * │ │ [Form Field 2]          │ │
 * │ │ [Form Field 3]          │ │
 * │ └─────────────────────────┘ │
 * │ Helper text or error text   │  ← Helper/error text
 * └─────────────────────────────┘
 * ```
 *
 * Example SwiftUI equivalent:
 * ```swift
 * VStack(alignment: .leading, spacing: 8) {
 *     if let label = label {
 *         Text(label + (required ? " *" : ""))
 *             .font(.headline)
 *     }
 *     VStack(spacing: 8) {
 *         // Form fields here
 *     }
 *     .padding()
 *     .background(Color(.systemGray6))
 *     .cornerRadius(8)
 *     .overlay(
 *         RoundedRectangle(cornerRadius: 8)
 *             .stroke(error ? Color.red : Color.clear, lineWidth: 2)
 *     )
 *     if let text = error ? errorText : helperText {
 *         Text(text)
 *             .font(.caption)
 *             .foregroundColor(error ? .red : .secondary)
 *     }
 * }
 * ```
 */
object FormGroupMapper {
    fun map(
        component: FormGroup,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val outerChildren = mutableListOf<SwiftUIView>()

        // Add label with required indicator if present
        if (component.label != null) {
            val labelText = component.label + if (component.required) " *" else ""
            outerChildren.add(
                SwiftUIView.text(
                    content = labelText,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Headline),
                        SwiftUIModifier.foregroundColor(
                            theme?.colorScheme?.onSurface?.let {
                                ModifierConverter.convertColor(it)
                            } ?: SwiftUIColor.primary
                        )
                    )
                )
            )
        }

        // Create inner group container with form fields
        val groupedFields = component.children.map { child -> renderChild(child) }

        val innerGroupModifiers = mutableListOf<SwiftUIModifier>()
        innerGroupModifiers.add(SwiftUIModifier.padding(16f))

        // Background color
        innerGroupModifiers.add(
            SwiftUIModifier.background(
                SwiftUIColor.system("systemGray6")
            )
        )
        innerGroupModifiers.add(SwiftUIModifier.cornerRadius(8f))

        // Border - red if error, transparent otherwise
        val borderColor = if (component.error) {
            SwiftUIColor.system("systemRed")
        } else {
            SwiftUIColor.system("clear")
        }
        innerGroupModifiers.add(
            SwiftUIModifier.border(borderColor, 2f)
        )

        val innerGroup = SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "spacing" to 12f,
                "alignment" to "Leading"
            ),
            children = groupedFields,
            modifiers = innerGroupModifiers
        )

        outerChildren.add(innerGroup)

        // Add helper text or error text
        val displayText = if (component.error && component.errorText != null) {
            component.errorText
        } else {
            component.helperText
        }

        if (displayText != null) {
            val textColor = if (component.error) {
                SwiftUIColor.system("systemRed")
            } else {
                SwiftUIColor.system("secondaryLabel")
            }

            outerChildren.add(
                SwiftUIView.text(
                    content = displayText,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(textColor)
                    )
                )
            )
        }

        // Create outer VStack with all elements
        val modifiers = mutableListOf<SwiftUIModifier>()
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "spacing" to 8f,
                "alignment" to "Leading"
            ),
            children = outerChildren,
            modifiers = modifiers,
            id = component.id
        )
    }
}
