package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.components.phase3.input.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Phase 3 Input Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Phase 3 input components to SwiftUI equivalents:
 * - Slider, RangeSlider
 * - DatePicker, TimePicker
 * - RadioButton, RadioGroup
 * - Dropdown, Autocomplete
 * - FileUpload, ImagePicker
 * - Rating, SearchBar
 * - PhoneInput, UrlInput, ComboBox
 */

// ============================================
// SLIDER COMPONENTS
// ============================================

/**
 * Maps Slider component to SwiftUI Slider
 */
object SliderMapper {
    fun map(component: Slider, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add accentColor for the slider
        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Slider,
            properties = mapOf(
                "value" to component.value,
                "range" to mapOf("min" to component.min, "max" to component.max),
                "step" to component.step,
                "onValueChange" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

/**
 * Maps RangeSlider component to custom SwiftUI range slider
 */
object RangeSliderMapper {
    fun map(component: RangeSlider, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Custom("RangeSlider"),
            properties = mapOf(
                "startValue" to component.startValue,
                "endValue" to component.endValue,
                "range" to mapOf("min" to component.min, "max" to component.max),
                "onRangeChange" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// DATE/TIME PICKERS
// ============================================

/**
 * Maps DatePicker component to SwiftUI DatePicker
 */
object DatePickerMapper {
    fun map(component: DatePicker, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.DatePicker,
            properties = mapOf(
                "selectedDate" to (component.selectedDate ?: ""),
                "displayedComponents" to "date",
                "onDateChange" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

/**
 * Maps TimePicker component to SwiftUI DatePicker with time components
 */
object TimePickerMapper {
    fun map(component: TimePicker, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.DatePicker,
            properties = mapOf(
                "selectedTime" to (component.selectedTime ?: ""),
                "displayedComponents" to "hourAndMinute",
                "onTimeChange" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// RADIO COMPONENTS
// ============================================

/**
 * Maps RadioButton component to SwiftUI Button with selection indicator
 */
object RadioButtonMapper {
    fun map(component: RadioButton, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.foregroundColor(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Custom("RadioButton"),
            properties = mapOf(
                "selected" to component.selected,
                "onSelect" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

/**
 * Maps RadioGroup component to SwiftUI Picker or custom radio group
 */
object RadioGroupMapper {
    fun map(component: RadioGroup, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "options" to component.options,
                "selectedValue" to (component.selectedValue ?: ""),
                "onSelectionChange" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// DROPDOWN/AUTOCOMPLETE
// ============================================

/**
 * Maps Dropdown component to SwiftUI Picker
 */
object DropdownMapper {
    fun map(component: Dropdown, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Picker,
            properties = mapOf(
                "options" to component.options,
                "selectedValue" to (component.selectedValue ?: ""),
                "placeholder" to (component.placeholder ?: "Select..."),
                "onSelectionChange" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

/**
 * Maps Autocomplete component to SwiftUI searchable list
 */
object AutocompleteMapper {
    fun map(component: Autocomplete, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Custom("Autocomplete"),
            properties = mapOf(
                "value" to component.value,
                "suggestions" to component.suggestions,
                "onValueChange" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// FILE/IMAGE PICKERS
// ============================================

/**
 * Maps FileUpload component to SwiftUI document picker
 */
object FileUploadMapper {
    fun map(component: FileUpload, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Custom("FileUpload"),
            properties = mapOf(
                "selectedFiles" to component.selectedFiles,
                "multiple" to component.multiple,
                "onFilesSelected" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

/**
 * Maps ImagePicker component to SwiftUI PhotosPicker
 */
object ImagePickerMapper {
    fun map(component: ImagePicker, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Custom("ImagePicker"),
            properties = mapOf(
                "selectedImage" to (component.selectedImage ?: ""),
                "allowCamera" to component.allowCamera,
                "onImageSelected" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// RATING & SEARCH
// ============================================

/**
 * Maps Rating component to custom SwiftUI star rating
 */
object RatingMapper {
    fun map(component: Rating, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.foregroundColor(ModifierConverter.convertColor(it)))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf(
                "rating" to component.rating,
                "maxRating" to component.maxRating,
                "onRatingChange" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

/**
 * Maps SearchBar component to SwiftUI TextField with search styling
 */
object SearchBarMapper {
    fun map(component: SearchBar, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.textFieldStyle("roundedBorder"))

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.TextField,
            properties = mapOf(
                "query" to component.query,
                "placeholder" to component.placeholder,
                "onQueryChange" to "callback",
                "onSearch" to "callback"
            ),
            modifiers = listOf(SwiftUIModifier.custom("searchable")) + modifiers,
            id = component.id
        )
    }
}

// ============================================
// SPECIALIZED INPUT COMPONENTS
// ============================================

/**
 * Maps PhoneInput component to SwiftUI TextField with phone number keyboard
 */
object PhoneInputMapper {
    fun map(component: com.augmentalis.avaelements.flutter.material.input.PhoneInput, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add text field style
        modifiers.add(SwiftUIModifier.textFieldStyle("roundedBorder"))

        // Add keyboard type for phone numbers
        modifiers.add(SwiftUIModifier.keyboardType("phonePad"))

        // Add theme colors if available
        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Custom("PhoneInput"),
            properties = mapOf(
                "value" to component.value,
                "countryCode" to component.countryCode,
                "label" to (component.label ?: ""),
                "placeholder" to (component.placeholder ?: "Phone number"),
                "enabled" to component.enabled,
                "required" to component.required,
                "errorText" to (component.errorText ?: ""),
                "onValueChange" to "callback",
                "onCountryCodeChange" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

/**
 * Maps UrlInput component to SwiftUI TextField with URL keyboard
 */
object UrlInputMapper {
    fun map(component: com.augmentalis.avaelements.flutter.material.input.UrlInput, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add text field style
        modifiers.add(SwiftUIModifier.textFieldStyle("roundedBorder"))

        // Add keyboard type for URLs
        modifiers.add(SwiftUIModifier.keyboardType("URL"))

        // Add autocapitalization disabled for URLs
        modifiers.add(SwiftUIModifier.custom("autocapitalization(.none)"))

        // Add autocorrection disabled for URLs
        modifiers.add(SwiftUIModifier.custom("autocorrectionDisabled()"))

        // Add theme colors if available
        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        // Add validation indicator overlay if there's an error
        if (component.errorText != null) {
            theme?.colorScheme?.error?.let {
                modifiers.add(SwiftUIModifier.foregroundColor(ModifierConverter.convertColor(it)))
            }
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.TextField,
            properties = mapOf(
                "value" to component.value,
                "label" to (component.label ?: ""),
                "placeholder" to (component.placeholder ?: "Enter URL"),
                "enabled" to component.enabled,
                "required" to component.required,
                "errorText" to (component.errorText ?: ""),
                "autoAddProtocol" to component.autoAddProtocol,
                "onValueChange" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

/**
 * Maps ComboBox component to searchable SwiftUI Picker with text input
 */
object ComboBoxMapper {
    fun map(component: com.augmentalis.avaelements.flutter.material.input.ComboBox, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add theme colors if available
        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Custom("ComboBox"),
            properties = mapOf(
                "value" to component.value,
                "options" to component.options,
                "label" to (component.label ?: ""),
                "placeholder" to (component.placeholder ?: "Select or type..."),
                "enabled" to component.enabled,
                "required" to component.required,
                "errorText" to (component.errorText ?: ""),
                "allowCustomValue" to component.allowCustomValue,
                "onValueChange" to "callback"
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}
