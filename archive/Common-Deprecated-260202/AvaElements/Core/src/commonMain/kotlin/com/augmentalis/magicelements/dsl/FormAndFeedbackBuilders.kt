package com.augmentalis.avaelements.dsl

import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.components.form.*
import com.augmentalis.avaelements.components.feedback.*

/**
 * FormAndFeedbackBuilders
 *
 * DSL builders and extension functions for Phase 3.1 and 3.2 components:
 * - Form Components (8): Radio, Slider, Dropdown, DatePicker, TimePicker, FileUpload, SearchBar, Rating
 * - Feedback Components (7): Dialog, Toast, Alert, ProgressBar, Spinner, Badge, Tooltip
 *
 * Usage:
 * ```kotlin
 * val ui = AvaUI {
 *     Column {
 *         Radio {
 *             options = listOf(RadioOption("opt1", "Option 1"))
 *             selectedValue = "opt1"
 *         }
 *
 *         Slider {
 *             value = 50f
 *             valueRange = 0f..100f
 *         }
 *
 *         Dialog {
 *             isOpen = true
 *             title = "Confirm"
 *         }
 *     }
 * }
 * ```
 */

// ==================== Form Component Builders ====================

/**
 * Radio builder for DSL
 */
class RadioBuilder(
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    private val optionsList = mutableListOf<RadioOption>()
    var selectedValue: String? = null
    var groupName: String = "radio-group"
    var orientation: Orientation = Orientation.Vertical
    var onValueChange: ((String) -> Unit)? = null

    fun option(value: String, label: String, enabled: Boolean = true) {
        optionsList.add(RadioOption(value, label, enabled))
    }

    fun options(vararg options: RadioOption) {
        optionsList.addAll(options)
    }

    fun options(options: List<RadioOption>) {
        optionsList.addAll(options)
    }

    internal fun build() = RadioComponent(
        options = optionsList,
        selectedValue = selectedValue,
        groupName = groupName,
        id = id,
        style = style,
        modifiers = modifiers,
        orientation = orientation,
        onValueChange = onValueChange
    )
}

/**
 * Slider builder for DSL
 */
class SliderBuilder(
    private val initialValue: Float = 0f,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var value: Float = initialValue
    var valueRange: ClosedFloatingPointRange<Float> = 0f..1f
    var steps: Int = 0
    var showLabel: Boolean = true
    var labelFormatter: ((Float) -> String)? = null
    var onValueChange: ((Float) -> Unit)? = null

    internal fun build() = SliderComponent(
        value = value,
        valueRange = valueRange,
        steps = steps,
        showLabel = showLabel,
        labelFormatter = labelFormatter,
        id = id,
        style = style,
        modifiers = modifiers,
        onValueChange = onValueChange
    )
}

/**
 * Dropdown builder for DSL
 */
class DropdownBuilder(
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    private val optionsList = mutableListOf<DropdownOption>()
    var selectedValue: String? = null
    var placeholder: String = "Select an option"
    var searchable: Boolean = false
    var onValueChange: ((String) -> Unit)? = null

    fun option(value: String, label: String, icon: String? = null, disabled: Boolean = false) {
        optionsList.add(DropdownOption(value, label, icon, disabled))
    }

    fun options(vararg options: DropdownOption) {
        optionsList.addAll(options)
    }

    fun options(options: List<DropdownOption>) {
        optionsList.addAll(options)
    }

    internal fun build() = DropdownComponent(
        options = optionsList,
        selectedValue = selectedValue,
        placeholder = placeholder,
        searchable = searchable,
        id = id,
        style = style,
        modifiers = modifiers,
        onValueChange = onValueChange
    )
}

/**
 * DatePicker builder for DSL
 */
class DatePickerBuilder(
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var selectedDate: Long? = null
    var minDate: Long? = null
    var maxDate: Long? = null
    var dateFormat: String = "yyyy-MM-dd"
    var onDateChange: ((Long) -> Unit)? = null

    internal fun build() = DatePickerComponent(
        selectedDate = selectedDate,
        minDate = minDate,
        maxDate = maxDate,
        dateFormat = dateFormat,
        id = id,
        style = style,
        modifiers = modifiers,
        onDateChange = onDateChange
    )
}

/**
 * TimePicker builder for DSL
 */
class TimePickerBuilder(
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var hour: Int = 0
    var minute: Int = 0
    var is24Hour: Boolean = true
    var onTimeChange: ((Int, Int) -> Unit)? = null

    internal fun build() = TimePickerComponent(
        hour = hour,
        minute = minute,
        is24Hour = is24Hour,
        id = id,
        style = style,
        modifiers = modifiers,
        onTimeChange = onTimeChange
    )
}

/**
 * FileUpload builder for DSL
 */
class FileUploadBuilder(
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    private val acceptList = mutableListOf<String>()
    var multiple: Boolean = false
    var maxSize: Long? = null
    var placeholder: String = "Choose file(s)"
    var onFilesSelected: ((List<FileData>) -> Unit)? = null

    fun accept(vararg types: String) {
        acceptList.addAll(types)
    }

    fun accept(types: List<String>) {
        acceptList.addAll(types)
    }

    internal fun build() = FileUploadComponent(
        accept = acceptList,
        multiple = multiple,
        maxSize = maxSize,
        placeholder = placeholder,
        id = id,
        style = style,
        modifiers = modifiers,
        onFilesSelected = onFilesSelected
    )
}

/**
 * SearchBar builder for DSL
 */
class SearchBarBuilder(
    private val initialValue: String = "",
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var value: String = initialValue
    var placeholder: String = "Search..."
    var showClearButton: Boolean = true
    private val suggestionsList = mutableListOf<String>()
    var onValueChange: ((String) -> Unit)? = null
    var onSearch: ((String) -> Unit)? = null

    fun suggestions(vararg items: String) {
        suggestionsList.addAll(items)
    }

    fun suggestions(items: List<String>) {
        suggestionsList.clear()
        suggestionsList.addAll(items)
    }

    internal fun build() = SearchBarComponent(
        value = value,
        placeholder = placeholder,
        showClearButton = showClearButton,
        suggestions = suggestionsList,
        id = id,
        style = style,
        modifiers = modifiers,
        onValueChange = onValueChange,
        onSearch = onSearch
    )
}

/**
 * Rating builder for DSL
 */
class RatingBuilder(
    private val initialValue: Float = 0f,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var value: Float = initialValue
    var maxRating: Int = 5
    var allowHalf: Boolean = false
    var readonly: Boolean = false
    var icon: String = "star"
    var onRatingChange: ((Float) -> Unit)? = null

    internal fun build() = RatingComponent(
        value = value,
        maxRating = maxRating,
        allowHalf = allowHalf,
        readonly = readonly,
        icon = icon,
        id = id,
        style = style,
        modifiers = modifiers,
        onRatingChange = onRatingChange
    )
}

// ==================== Feedback Component Builders ====================

/**
 * Dialog builder for DSL
 */
class DialogBuilder(
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var isOpen: Boolean = false
    var title: String? = null
    var content: Component? = null
    private val actionsList = mutableListOf<DialogAction>()
    var dismissible: Boolean = true
    var onDismiss: (() -> Unit)? = null

    fun action(label: String, style: DialogActionStyle = DialogActionStyle.Text, onClick: () -> Unit) {
        actionsList.add(DialogAction(label, style, onClick))
    }

    fun actions(vararg actions: DialogAction) {
        actionsList.addAll(actions)
    }

    internal fun build() = DialogComponent(
        isOpen = isOpen,
        title = title,
        content = content,
        actions = actionsList,
        dismissible = dismissible,
        id = id,
        style = style,
        modifiers = modifiers,
        onDismiss = onDismiss
    )
}

/**
 * Toast builder for DSL
 */
class ToastBuilder(
    private val message: String,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var duration: Long = 3000
    var severity: ToastSeverity = ToastSeverity.Info
    var position: ToastPosition = ToastPosition.BottomCenter
    var action: ToastAction? = null

    fun action(label: String, onClick: () -> Unit) {
        action = ToastAction(label, onClick)
    }

    internal fun build() = ToastComponent(
        message = message,
        duration = duration,
        severity = severity,
        position = position,
        action = action,
        id = id,
        style = style,
        modifiers = modifiers
    )
}

/**
 * Alert builder for DSL
 */
class AlertBuilder(
    private val title: String,
    private val message: String,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var severity: AlertSeverity = AlertSeverity.Info
    var dismissible: Boolean = true
    var icon: String? = null
    var onDismiss: (() -> Unit)? = null

    internal fun build() = AlertComponent(
        title = title,
        message = message,
        severity = severity,
        dismissible = dismissible,
        icon = icon,
        id = id,
        style = style,
        modifiers = modifiers,
        onDismiss = onDismiss
    )
}

/**
 * ProgressBar builder for DSL
 */
class ProgressBarBuilder(
    private val initialValue: Float = 0f,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var value: Float = initialValue
    var showLabel: Boolean = false
    var labelFormatter: ((Float) -> String)? = null
    var indeterminate: Boolean = false

    internal fun build() = ProgressBarComponent(
        value = value,
        showLabel = showLabel,
        labelFormatter = labelFormatter,
        indeterminate = indeterminate,
        id = id,
        style = style,
        modifiers = modifiers
    )
}

/**
 * Spinner builder for DSL
 */
class SpinnerBuilder(
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var size: SpinnerSize = SpinnerSize.Medium
    var label: String? = null

    internal fun build() = SpinnerComponent(
        size = size,
        label = label,
        id = id,
        style = style,
        modifiers = modifiers
    )
}

/**
 * Badge builder for DSL
 */
class BadgeBuilder(
    private val content: String = "",
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var variant: BadgeVariant = BadgeVariant.Default
    var size: BadgeSize = BadgeSize.Medium

    internal fun build() = BadgeComponent(
        content = content,
        variant = variant,
        size = size,
        id = id,
        style = style,
        modifiers = modifiers
    )
}

/**
 * Tooltip builder for DSL
 */
class TooltipBuilder(
    private val content: String,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var position: TooltipPosition = TooltipPosition.Top
    var child: Component? = null

    internal fun build(): TooltipComponent {
        requireNotNull(child) { "Tooltip must have a child component" }
        return TooltipComponent(
            content = content,
            position = position,
            child = child!!,
            id = id,
            style = style,
            modifiers = modifiers
        )
    }
}

// ==================== DSL Extension Functions ====================

// These allow using the components directly in layout builders like Column/Row

// Form component extensions
fun ComponentScope.Radio(
    id: String? = null,
    builder: RadioBuilder.() -> Unit
): RadioComponent {
    val radioBuilder = RadioBuilder(id, null)
    radioBuilder.builder()
    return radioBuilder.build()
}

fun ComponentScope.Slider(
    value: Float = 0f,
    id: String? = null,
    builder: SliderBuilder.() -> Unit = {}
): SliderComponent {
    val sliderBuilder = SliderBuilder(value, id, null)
    sliderBuilder.builder()
    return sliderBuilder.build()
}

fun ComponentScope.Dropdown(
    id: String? = null,
    builder: DropdownBuilder.() -> Unit
): DropdownComponent {
    val dropdownBuilder = DropdownBuilder(id, null)
    dropdownBuilder.builder()
    return dropdownBuilder.build()
}

fun ComponentScope.DatePicker(
    id: String? = null,
    builder: DatePickerBuilder.() -> Unit = {}
): DatePickerComponent {
    val datePickerBuilder = DatePickerBuilder(id, null)
    datePickerBuilder.builder()
    return datePickerBuilder.build()
}

fun ComponentScope.TimePicker(
    id: String? = null,
    builder: TimePickerBuilder.() -> Unit = {}
): TimePickerComponent {
    val timePickerBuilder = TimePickerBuilder(id, null)
    timePickerBuilder.builder()
    return timePickerBuilder.build()
}

fun ComponentScope.FileUpload(
    id: String? = null,
    builder: FileUploadBuilder.() -> Unit = {}
): FileUploadComponent {
    val fileUploadBuilder = FileUploadBuilder(id, null)
    fileUploadBuilder.builder()
    return fileUploadBuilder.build()
}

fun ComponentScope.SearchBar(
    value: String = "",
    id: String? = null,
    builder: SearchBarBuilder.() -> Unit = {}
): SearchBarComponent {
    val searchBarBuilder = SearchBarBuilder(value, id, null)
    searchBarBuilder.builder()
    return searchBarBuilder.build()
}

fun ComponentScope.Rating(
    value: Float = 0f,
    id: String? = null,
    builder: RatingBuilder.() -> Unit = {}
): RatingComponent {
    val ratingBuilder = RatingBuilder(value, id, null)
    ratingBuilder.builder()
    return ratingBuilder.build()
}

// Feedback component extensions
fun ComponentScope.Dialog(
    id: String? = null,
    builder: DialogBuilder.() -> Unit = {}
): DialogComponent {
    val dialogBuilder = DialogBuilder(id, null)
    dialogBuilder.builder()
    return dialogBuilder.build()
}

fun ComponentScope.Toast(
    message: String,
    id: String? = null,
    builder: ToastBuilder.() -> Unit = {}
): ToastComponent {
    val toastBuilder = ToastBuilder(message, id, null)
    toastBuilder.builder()
    return toastBuilder.build()
}

fun ComponentScope.Alert(
    title: String,
    message: String,
    id: String? = null,
    builder: AlertBuilder.() -> Unit = {}
): AlertComponent {
    val alertBuilder = AlertBuilder(title, message, id, null)
    alertBuilder.builder()
    return alertBuilder.build()
}

fun ComponentScope.ProgressBar(
    value: Float = 0f,
    id: String? = null,
    builder: ProgressBarBuilder.() -> Unit = {}
): ProgressBarComponent {
    val progressBarBuilder = ProgressBarBuilder(value, id, null)
    progressBarBuilder.builder()
    return progressBarBuilder.build()
}

fun ComponentScope.Spinner(
    id: String? = null,
    builder: SpinnerBuilder.() -> Unit = {}
): SpinnerComponent {
    val spinnerBuilder = SpinnerBuilder(id, null)
    spinnerBuilder.builder()
    return spinnerBuilder.build()
}

fun ComponentScope.Badge(
    content: String = "",
    id: String? = null,
    builder: BadgeBuilder.() -> Unit = {}
): BadgeComponent {
    val badgeBuilder = BadgeBuilder(content, id, null)
    badgeBuilder.builder()
    return badgeBuilder.build()
}

fun ComponentScope.Tooltip(
    content: String,
    id: String? = null,
    builder: TooltipBuilder.() -> Unit
): TooltipComponent {
    val tooltipBuilder = TooltipBuilder(content, id, null)
    tooltipBuilder.builder()
    return tooltipBuilder.build()
}
