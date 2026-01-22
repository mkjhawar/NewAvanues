package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.dsl.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Basic Component Mappers
 *
 * Maps AvaElements basic UI components to SwiftUI views
 */

/**
 * Maps TextComponent to SwiftUI Text
 */
object TextMapper {
    fun map(component: TextComponent, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add font styling
        modifiers.add(SwiftUIModifier.font(ModifierConverter.convertFontStyle(component.font)))
        modifiers.add(SwiftUIModifier.fontWeight(ModifierConverter.convertFontWeight(component.font.weight)))

        // Add color
        modifiers.add(SwiftUIModifier.foregroundColor(ModifierConverter.convertColor(component.color)))

        // Add text alignment if needed
        if (component.textAlign != TextScope.TextAlign.Start) {
            // Would need multilineTextAlignment modifier
        }

        // Add max lines if specified
        if (component.maxLines != null) {
            // Would need lineLimit modifier
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView.text(
            content = component.text,
            modifiers = modifiers
        ).copy(id = component.id)
    }
}

/**
 * Maps ButtonComponent to SwiftUI Button
 */
object ButtonMapper {
    fun map(component: ButtonComponent, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Determine button appearance based on style
        val buttonStyle = when (component.buttonStyle) {
            ButtonScope.ButtonStyle.Primary -> {
                // Primary button: colored background
                val bgColor = theme?.colorScheme?.primary?.let {
                    ModifierConverter.convertColor(it)
                } ?: SwiftUIColor.blue
                modifiers.add(SwiftUIModifier.background(bgColor))

                val fgColor = theme?.colorScheme?.onPrimary?.let {
                    ModifierConverter.convertColor(it)
                } ?: SwiftUIColor.white
                modifiers.add(SwiftUIModifier.foregroundColor(fgColor))

                ButtonStyleType.Bordered
            }
            ButtonScope.ButtonStyle.Secondary -> {
                // Secondary button: tinted
                val fgColor = theme?.colorScheme?.secondary?.let {
                    ModifierConverter.convertColor(it)
                } ?: SwiftUIColor.secondary
                modifiers.add(SwiftUIModifier.foregroundColor(fgColor))

                ButtonStyleType.Bordered
            }
            ButtonScope.ButtonStyle.Outlined -> {
                // Outlined button: border only
                val borderColor = theme?.colorScheme?.outline?.let {
                    ModifierConverter.convertColor(it)
                } ?: SwiftUIColor.system("gray")
                modifiers.add(SwiftUIModifier.border(borderColor, 1f))

                ButtonStyleType.Plain
            }
            ButtonScope.ButtonStyle.Text -> {
                // Text button: no background
                ButtonStyleType.Plain
            }
        }

        // Add disabled state
        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Build button label (with icons if present)
        val label = buildButtonLabel(component, theme)

        return SwiftUIView(
            type = ViewType.Button,
            id = component.id,
            properties = mapOf(
                "label" to component.text,
                "action" to (component.onClick?.toString() ?: ""),
                "style" to buttonStyle.name
            ),
            children = listOfNotNull(label),
            modifiers = modifiers
        )
    }

    private fun buildButtonLabel(component: ButtonComponent, theme: Theme?): SwiftUIView? {
        // If there are icons, create an HStack with icon + text
        if (component.leadingIcon != null || component.trailingIcon != null) {
            val children = mutableListOf<SwiftUIView>()

            component.leadingIcon?.let {
                children.add(IconMapper.mapSystemIcon(it, theme))
            }

            children.add(SwiftUIView.text(component.text))

            component.trailingIcon?.let {
                children.add(IconMapper.mapSystemIcon(it, theme))
            }

            return SwiftUIView.hStack(
                spacing = 8f,
                alignment = VerticalAlignment.Center,
                children = children
            )
        }

        return null
    }
}

/**
 * Maps TextFieldComponent to SwiftUI TextField
 */
object TextFieldMapper {
    fun map(component: TextFieldComponent, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add padding
        modifiers.add(SwiftUIModifier.padding(12f))

        // Add background
        val bgColor = theme?.colorScheme?.surfaceVariant?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.system("systemGray6")
        modifiers.add(SwiftUIModifier.background(bgColor))

        // Add border if error
        if (component.isError) {
            val errorColor = theme?.colorScheme?.error?.let {
                ModifierConverter.convertColor(it)
            } ?: SwiftUIColor.red
            modifiers.add(SwiftUIModifier.border(errorColor, 1f))
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Build field with label and icons
        val children = mutableListOf<SwiftUIView>()

        // If there's a label, wrap in VStack
        val hasLabel = component.label != null || component.errorMessage != null

        if (hasLabel) {
            val vStackChildren = mutableListOf<SwiftUIView>()

            // Add label
            component.label?.let {
                vStackChildren.add(SwiftUIView.text(
                    content = it,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(
                            theme?.colorScheme?.onSurfaceVariant?.let { color ->
                                ModifierConverter.convertColor(color)
                            } ?: SwiftUIColor.system("secondaryLabel")
                        )
                    )
                ))
            }

            // Create text field with icons if present
            val fieldView = if (component.leadingIcon != null || component.trailingIcon != null) {
                createTextFieldWithIcons(component, theme)
            } else {
                createSimpleTextField(component, theme)
            }
            vStackChildren.add(fieldView)

            // Add error message
            if (component.isError && component.errorMessage != null) {
                vStackChildren.add(SwiftUIView.text(
                    content = component.errorMessage!!,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption2),
                        SwiftUIModifier.foregroundColor(
                            theme?.colorScheme?.error?.let { color ->
                                ModifierConverter.convertColor(color)
                            } ?: SwiftUIColor.red
                        )
                    )
                ))
            }

            return SwiftUIView.vStack(
                spacing = 4f,
                alignment = HorizontalAlignment.Leading,
                children = vStackChildren,
                modifiers = modifiers
            ).copy(id = component.id)
        } else {
            // Simple text field without label
            val fieldView = if (component.leadingIcon != null || component.trailingIcon != null) {
                createTextFieldWithIcons(component, theme)
            } else {
                createSimpleTextField(component, theme)
            }

            return fieldView.copy(
                modifiers = fieldView.modifiers + modifiers,
                id = component.id
            )
        }
    }

    private fun createSimpleTextField(component: TextFieldComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.TextField,
            properties = mapOf(
                "placeholder" to component.placeholder,
                "text" to component.value,
                "isEnabled" to component.enabled,
                "isReadOnly" to component.readOnly
            )
        )
    }

    private fun createTextFieldWithIcons(component: TextFieldComponent, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        component.leadingIcon?.let {
            children.add(IconMapper.mapSystemIcon(it, theme))
        }

        children.add(createSimpleTextField(component, theme))

        component.trailingIcon?.let {
            children.add(IconMapper.mapSystemIcon(it, theme))
        }

        return SwiftUIView.hStack(
            spacing = 8f,
            alignment = VerticalAlignment.Center,
            children = children
        )
    }
}

/**
 * Maps CheckboxComponent to SwiftUI Toggle (with checkmark style)
 */
object CheckboxMapper {
    fun map(component: CheckboxComponent, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add disabled state
        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Toggle,
            id = component.id,
            properties = mapOf(
                "label" to component.label,
                "isOn" to component.checked,
                "style" to "checkbox"  // Custom style hint for checkbox appearance
            ),
            modifiers = modifiers
        )
    }
}

/**
 * Maps SwitchComponent to SwiftUI Toggle
 */
object SwitchMapper {
    fun map(component: SwitchComponent, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add disabled state
        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
        }

        // Add tint color from theme
        theme?.colorScheme?.primary?.let {
            // Would add toggleStyle with custom color
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Toggle,
            id = component.id,
            properties = mapOf(
                "isOn" to component.checked,
                "style" to "switch"
            ),
            modifiers = modifiers
        )
    }
}

/**
 * Maps IconComponent to SwiftUI Image with SF Symbol
 */
object IconMapper {
    fun map(component: IconComponent, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add tint color
        val tintColor = component.tint?.let {
            ModifierConverter.convertColor(it)
        } ?: theme?.colorScheme?.primary?.let {
            ModifierConverter.convertColor(it)
        }

        tintColor?.let {
            modifiers.add(SwiftUIModifier.foregroundColor(it))
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Image,
            id = component.id,
            properties = mapOf(
                "systemName" to component.name,
                "contentDescription" to (component.contentDescription ?: component.name)
            ),
            modifiers = modifiers
        )
    }

    fun mapSystemIcon(name: String, theme: Theme?, tint: SwiftUIColor? = null): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        val iconTint = tint ?: theme?.colorScheme?.primary?.let {
            ModifierConverter.convertColor(it)
        }

        iconTint?.let {
            modifiers.add(SwiftUIModifier.foregroundColor(it))
        }

        return SwiftUIView(
            type = ViewType.Image,
            properties = mapOf("systemName" to name),
            modifiers = modifiers
        )
    }
}

/**
 * Maps ImageComponent to SwiftUI AsyncImage
 */
object ImageMapper {
    fun map(component: ImageComponent, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add content scale mode
        val contentMode = when (component.contentScale) {
            ImageScope.ContentScale.Fit -> "fit"
            ImageScope.ContentScale.Fill -> "fill"
            ImageScope.ContentScale.Crop -> "fill"  // With clipped modifier
            ImageScope.ContentScale.None -> "none"
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Add clip if crop
        if (component.contentScale == ImageScope.ContentScale.Crop) {
            // Would add clipped() modifier
        }

        return SwiftUIView(
            type = ViewType.Image,
            id = component.id,
            properties = mapOf(
                "url" to component.source,
                "contentMode" to contentMode,
                "contentDescription" to (component.contentDescription ?: "")
            ),
            modifiers = modifiers
        )
    }
}
