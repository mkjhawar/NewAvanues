package com.augmentalis.avanueui.dsl

import com.augmentalis.avanueui.core.*

/**
 * AvaUI DSL entry point
 *
 * Usage:
 * ```kotlin
 * val ui = AvaUI {
 *     theme = Themes.iOS26LiquidGlass
 *
 *     Column {
 *         padding(16f)
 *
 *         Text("Hello World") {
 *             font = Font.Title
 *             color = theme.colorScheme.primary
 *         }
 *
 *         Button("Click Me") {
 *             style = ButtonStyle.Primary
 *             onClick = { println("Clicked!") }
 *         }
 *     }
 * }
 * ```
 */
class AvaUI(builder: AvaUIScope.() -> Unit) {
    private val scope = AvaUIScope()

    val theme: Theme
    val root: Component?

    init {
        scope.builder()
        theme = scope.theme ?: Themes.Material3Light
        root = scope.rootComponent
    }

    /**
     * Render to platform-specific UI
     */
    fun render(renderer: Renderer): Any {
        renderer.applyTheme(theme)
        return root?.render(renderer) ?: throw IllegalStateException("No root component defined")
    }
}

/**
 * Top-level scope for AvaUI DSL
 */
class AvaUIScope internal constructor() {
    var theme: Theme? = null
    internal var rootComponent: Component? = null

    // Layout components
    fun Column(
        id: String? = null,
        style: ComponentStyle? = null,
        builder: ColumnScope.() -> Unit
    ): ColumnComponent {
        val scope = ColumnScope(id, style)
        scope.builder()
        return scope.build().also { rootComponent = it }
    }

    fun Row(
        id: String? = null,
        style: ComponentStyle? = null,
        builder: RowScope.() -> Unit
    ): RowComponent {
        val scope = RowScope(id, style)
        scope.builder()
        return scope.build().also { rootComponent = it }
    }

    fun Container(
        id: String? = null,
        style: ComponentStyle? = null,
        builder: ContainerScope.() -> Unit
    ): ContainerComponent {
        val scope = ContainerScope(id, style)
        scope.builder()
        return scope.build().also { rootComponent = it }
    }

    fun ScrollView(
        id: String? = null,
        orientation: Orientation = Orientation.Vertical,
        style: ComponentStyle? = null,
        builder: ScrollViewScope.() -> Unit
    ): ScrollViewComponent {
        val scope = ScrollViewScope(id, orientation, style)
        scope.builder()
        return scope.build().also { rootComponent = it }
    }

    // Basic components
    fun Text(
        text: String,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (TextScope.() -> Unit)? = null
    ): TextComponent {
        val scope = TextScope(text, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun Button(
        text: String,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (ButtonScope.() -> Unit)? = null
    ): ButtonComponent {
        val scope = ButtonScope(text, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun Image(
        source: String,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (ImageScope.() -> Unit)? = null
    ): ImageComponent {
        val scope = ImageScope(source, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun Checkbox(
        label: String,
        checked: Boolean = false,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (CheckboxScope.() -> Unit)? = null
    ): CheckboxComponent {
        val scope = CheckboxScope(label, checked, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun TextField(
        value: String = "",
        placeholder: String = "",
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (TextFieldScope.() -> Unit)? = null
    ): TextFieldComponent {
        val scope = TextFieldScope(value, placeholder, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun Switch(
        checked: Boolean = false,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (SwitchScope.() -> Unit)? = null
    ): SwitchComponent {
        val scope = SwitchScope(checked, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun Card(
        id: String? = null,
        style: ComponentStyle? = null,
        builder: CardScope.() -> Unit
    ): CardComponent {
        val scope = CardScope(id, style)
        scope.builder()
        return scope.build().also { rootComponent = it }
    }

    fun Icon(
        name: String,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (IconScope.() -> Unit)? = null
    ): IconComponent {
        val scope = IconScope(name, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    // Form components
    fun Radio(
        options: List<RadioOption>,
        selectedValue: String? = null,
        groupName: String,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (RadioScope.() -> Unit)? = null
    ): RadioComponent {
        val scope = RadioScope(options, selectedValue, groupName, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun Slider(
        value: Float,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (SliderScope.() -> Unit)? = null
    ): SliderComponent {
        val scope = SliderScope(value, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun Dropdown(
        options: List<DropdownOption>,
        selectedValue: String? = null,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (DropdownScope.() -> Unit)? = null
    ): DropdownComponent {
        val scope = DropdownScope(options, selectedValue, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun DatePicker(
        selectedDate: Long? = null,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (DatePickerScope.() -> Unit)? = null
    ): DatePickerComponent {
        val scope = DatePickerScope(selectedDate, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun TimePicker(
        hour: Int = 0,
        minute: Int = 0,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (TimePickerScope.() -> Unit)? = null
    ): TimePickerComponent {
        val scope = TimePickerScope(hour, minute, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun FileUpload(
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (FileUploadScope.() -> Unit)? = null
    ): FileUploadComponent {
        val scope = FileUploadScope(id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun SearchBar(
        value: String = "",
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (SearchBarScope.() -> Unit)? = null
    ): SearchBarComponent {
        val scope = SearchBarScope(value, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun Rating(
        value: Float = 0f,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (RatingScope.() -> Unit)? = null
    ): RatingComponent {
        val scope = RatingScope(value, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    // Feedback components
    fun Dialog(
        isOpen: Boolean = false,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (DialogScope.() -> Unit)? = null
    ): DialogComponent {
        val scope = DialogScope(isOpen, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun Toast(
        message: String,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (ToastScope.() -> Unit)? = null
    ): ToastComponent {
        val scope = ToastScope(message, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun Alert(
        title: String,
        message: String,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (AlertScope.() -> Unit)? = null
    ): AlertComponent {
        val scope = AlertScope(title, message, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun ProgressBar(
        value: Float,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (ProgressBarScope.() -> Unit)? = null
    ): ProgressBarComponent {
        val scope = ProgressBarScope(value, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun Spinner(
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (SpinnerScope.() -> Unit)? = null
    ): SpinnerComponent {
        val scope = SpinnerScope(id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun Badge(
        content: String,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: (BadgeScope.() -> Unit)? = null
    ): BadgeComponent {
        val scope = BadgeScope(content, id, style)
        builder?.invoke(scope)
        return scope.build().also { rootComponent = it }
    }

    fun Tooltip(
        content: String,
        id: String? = null,
        style: ComponentStyle? = null,
        builder: TooltipScope.() -> Unit
    ): TooltipComponent {
        val scope = TooltipScope(content, id, style)
        scope.builder()
        return scope.build().also { rootComponent = it }
    }
}

// ==================== Layout Component Scopes ====================

class ColumnScope(
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var arrangement: Arrangement = Arrangement.Start
    var horizontalAlignment: Alignment = Alignment.Start
    private val children = mutableListOf<Component>()

    fun Text(text: String, builder: (TextScope.() -> Unit)? = null) {
        val scope = TextScope(text)
        builder?.invoke(scope)
        children.add(scope.build())
    }

    fun Button(text: String, builder: (ButtonScope.() -> Unit)? = null) {
        val scope = ButtonScope(text)
        builder?.invoke(scope)
        children.add(scope.build())
    }

    fun Image(source: String, builder: (ImageScope.() -> Unit)? = null) {
        val scope = ImageScope(source)
        builder?.invoke(scope)
        children.add(scope.build())
    }

    fun Checkbox(label: String, checked: Boolean = false, builder: (CheckboxScope.() -> Unit)? = null) {
        val scope = CheckboxScope(label, checked)
        builder?.invoke(scope)
        children.add(scope.build())
    }

    fun TextField(value: String = "", placeholder: String = "", builder: (TextFieldScope.() -> Unit)? = null) {
        val scope = TextFieldScope(value, placeholder)
        builder?.invoke(scope)
        children.add(scope.build())
    }

    fun Row(builder: RowScope.() -> Unit) {
        val scope = RowScope(null, null)
        scope.builder()
        children.add(scope.build())
    }

    fun Column(builder: ColumnScope.() -> Unit) {
        val scope = ColumnScope(null, null)
        scope.builder()
        children.add(scope.build())
    }

    fun Card(builder: CardScope.() -> Unit) {
        val scope = CardScope(null, null)
        scope.builder()
        children.add(scope.build())
    }

    internal fun build() = ColumnComponent(
        id = id,
        style = style,
        modifiers = modifiers,
        arrangement = arrangement,
        horizontalAlignment = horizontalAlignment,
        children = children
    )
}

class RowScope(
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var arrangement: Arrangement = Arrangement.Start
    var verticalAlignment: Alignment = Alignment.CenterStart
    private val children = mutableListOf<Component>()

    fun Text(text: String, builder: (TextScope.() -> Unit)? = null) {
        val scope = TextScope(text)
        builder?.invoke(scope)
        children.add(scope.build())
    }

    fun Button(text: String, builder: (ButtonScope.() -> Unit)? = null) {
        val scope = ButtonScope(text)
        builder?.invoke(scope)
        children.add(scope.build())
    }

    fun Image(source: String, builder: (ImageScope.() -> Unit)? = null) {
        val scope = ImageScope(source)
        builder?.invoke(scope)
        children.add(scope.build())
    }

    fun Icon(name: String, builder: (IconScope.() -> Unit)? = null) {
        val scope = IconScope(name)
        builder?.invoke(scope)
        children.add(scope.build())
    }

    fun Column(builder: ColumnScope.() -> Unit) {
        val scope = ColumnScope(null, null)
        scope.builder()
        children.add(scope.build())
    }

    fun Row(builder: RowScope.() -> Unit) {
        val scope = RowScope(null, null)
        scope.builder()
        children.add(scope.build())
    }

    internal fun build() = RowComponent(
        id = id,
        style = style,
        modifiers = modifiers,
        arrangement = arrangement,
        verticalAlignment = verticalAlignment,
        children = children
    )
}

class ContainerScope(
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var alignment: Alignment = Alignment.TopStart
    private var child: Component? = null

    fun Text(text: String, builder: (TextScope.() -> Unit)? = null) {
        val scope = TextScope(text)
        builder?.invoke(scope)
        child = scope.build()
    }

    fun Button(text: String, builder: (ButtonScope.() -> Unit)? = null) {
        val scope = ButtonScope(text)
        builder?.invoke(scope)
        child = scope.build()
    }

    fun Column(builder: ColumnScope.() -> Unit) {
        val scope = ColumnScope(null, null)
        scope.builder()
        child = scope.build()
    }

    fun Row(builder: RowScope.() -> Unit) {
        val scope = RowScope(null, null)
        scope.builder()
        child = scope.build()
    }

    internal fun build() = ContainerComponent(
        id = id,
        style = style,
        modifiers = modifiers,
        alignment = alignment,
        child = child
    )
}

class ScrollViewScope(
    private val id: String?,
    private val orientation: Orientation,
    private val style: ComponentStyle?
) : ComponentScope() {
    private var child: Component? = null

    fun Column(builder: ColumnScope.() -> Unit) {
        val scope = ColumnScope(null, null)
        scope.builder()
        child = scope.build()
    }

    fun Row(builder: RowScope.() -> Unit) {
        val scope = RowScope(null, null)
        scope.builder()
        child = scope.build()
    }

    internal fun build() = ScrollViewComponent(
        id = id,
        style = style,
        modifiers = modifiers,
        orientation = orientation,
        child = child
    )
}

class CardScope(
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var elevation: Int = 1
    private val children = mutableListOf<Component>()

    fun Text(text: String, builder: (TextScope.() -> Unit)? = null) {
        val scope = TextScope(text)
        builder?.invoke(scope)
        children.add(scope.build())
    }

    fun Column(builder: ColumnScope.() -> Unit) {
        val scope = ColumnScope(null, null)
        scope.builder()
        children.add(scope.build())
    }

    fun Row(builder: RowScope.() -> Unit) {
        val scope = RowScope(null, null)
        scope.builder()
        children.add(scope.build())
    }

    internal fun build() = CardComponent(
        id = id,
        style = style,
        modifiers = modifiers,
        elevation = elevation,
        children = children
    )
}

// ==================== Basic Component Scopes ====================

class TextScope(
    private val text: String,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var font: Font = Font.Body
    var color: Color = Color.Black
    var textAlign: TextAlign = TextAlign.Start
    var maxLines: Int? = null
    var overflow: TextOverflow = TextOverflow.Clip

    enum class TextAlign {
        Start, Center, End, Justify
    }

    enum class TextOverflow {
        Clip, Ellipsis, Visible
    }

    internal fun build() = TextComponent(
        text = text,
        id = id,
        style = style,
        modifiers = modifiers,
        font = font,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow
    )
}

class ButtonScope(
    private val text: String,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var buttonStyle: ButtonStyle = ButtonStyle.Primary
    var enabled: Boolean = true
    var onClick: (() -> Unit)? = null
    var leadingIcon: String? = null
    var trailingIcon: String? = null

    enum class ButtonStyle {
        Primary,
        Secondary,
        Tertiary,
        Text,
        Outlined
    }

    internal fun build() = ButtonComponent(
        text = text,
        id = id,
        style = style,
        modifiers = modifiers,
        buttonStyle = buttonStyle,
        enabled = enabled,
        onClick = onClick,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}

class ImageScope(
    private val source: String,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var contentDescription: String? = null
    var contentScale: ContentScale = ContentScale.Fit

    enum class ContentScale {
        Fit, Fill, Crop, None
    }

    internal fun build() = ImageComponent(
        source = source,
        id = id,
        style = style,
        modifiers = modifiers,
        contentDescription = contentDescription,
        contentScale = contentScale
    )
}

class CheckboxScope(
    private val label: String,
    private val checked: Boolean,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var enabled: Boolean = true
    var onCheckedChange: ((Boolean) -> Unit)? = null

    internal fun build() = CheckboxComponent(
        label = label,
        checked = checked,
        id = id,
        style = style,
        modifiers = modifiers,
        enabled = enabled,
        onCheckedChange = onCheckedChange
    )
}

class TextFieldScope(
    private val value: String,
    private val placeholder: String,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var label: String? = null
    var enabled: Boolean = true
    var readOnly: Boolean = false
    var isError: Boolean = false
    var errorMessage: String? = null
    var leadingIcon: String? = null
    var trailingIcon: String? = null
    var maxLength: Int? = null
    var onValueChange: ((String) -> Unit)? = null

    internal fun build() = TextFieldComponent(
        value = value,
        placeholder = placeholder,
        id = id,
        style = style,
        modifiers = modifiers,
        label = label,
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        errorMessage = errorMessage,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        maxLength = maxLength,
        onValueChange = onValueChange
    )
}

class SwitchScope(
    private val checked: Boolean,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var enabled: Boolean = true
    var onCheckedChange: ((Boolean) -> Unit)? = null

    internal fun build() = SwitchComponent(
        checked = checked,
        id = id,
        style = style,
        modifiers = modifiers,
        enabled = enabled,
        onCheckedChange = onCheckedChange
    )
}

class IconScope(
    private val name: String,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var tint: Color? = null
    var contentDescription: String? = null

    internal fun build() = IconComponent(
        name = name,
        id = id,
        style = style,
        modifiers = modifiers,
        tint = tint,
        contentDescription = contentDescription
    )
}

// ==================== Form Component Scopes ====================

class RadioScope(
    private val options: List<RadioOption>,
    private val selectedValue: String?,
    private val groupName: String,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var orientation: Orientation = Orientation.Vertical
    var onValueChange: ((String) -> Unit)? = null

    internal fun build() = RadioComponent(
        options = options,
        selectedValue = selectedValue,
        groupName = groupName,
        id = id,
        style = style,
        modifiers = modifiers,
        orientation = orientation,
        onValueChange = onValueChange
    )
}

class SliderScope(
    private val value: Float,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
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

class DropdownScope(
    private val options: List<DropdownOption>,
    private val selectedValue: String?,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var placeholder: String = "Select..."
    var searchable: Boolean = false
    var onValueChange: ((String) -> Unit)? = null

    internal fun build() = DropdownComponent(
        options = options,
        selectedValue = selectedValue,
        placeholder = placeholder,
        searchable = searchable,
        id = id,
        style = style,
        modifiers = modifiers,
        onValueChange = onValueChange
    )
}

class DatePickerScope(
    private val selectedDate: Long?,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
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

class TimePickerScope(
    private val hour: Int,
    private val minute: Int,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
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

class FileUploadScope(
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var accept: List<String> = emptyList()
    var multiple: Boolean = false
    var maxSize: Long? = null
    var placeholder: String = "Choose files..."
    var onFilesSelected: ((List<FileData>) -> Unit)? = null

    internal fun build() = FileUploadComponent(
        accept = accept,
        multiple = multiple,
        maxSize = maxSize,
        placeholder = placeholder,
        id = id,
        style = style,
        modifiers = modifiers,
        onFilesSelected = onFilesSelected
    )
}

class SearchBarScope(
    private val value: String,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var placeholder: String = "Search..."
    var showClearButton: Boolean = true
    var suggestions: List<String> = emptyList()
    var onValueChange: ((String) -> Unit)? = null
    var onSearch: ((String) -> Unit)? = null

    internal fun build() = SearchBarComponent(
        value = value,
        placeholder = placeholder,
        showClearButton = showClearButton,
        suggestions = suggestions,
        id = id,
        style = style,
        modifiers = modifiers,
        onValueChange = onValueChange,
        onSearch = onSearch
    )
}

class RatingScope(
    private val value: Float,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
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

// ==================== Feedback Component Scopes ====================

class DialogScope(
    private val isOpen: Boolean,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var title: String? = null
    var content: Component? = null
    var actions: List<DialogAction> = emptyList()
    var dismissible: Boolean = true
    var onDismiss: (() -> Unit)? = null

    fun Text(text: String, builder: (TextScope.() -> Unit)? = null) {
        val scope = TextScope(text)
        builder?.invoke(scope)
        content = scope.build()
    }

    fun Column(builder: ColumnScope.() -> Unit) {
        val scope = ColumnScope(null, null)
        scope.builder()
        content = scope.build()
    }

    internal fun build() = DialogComponent(
        isOpen = isOpen,
        title = title,
        content = content,
        actions = actions,
        dismissible = dismissible,
        id = id,
        style = style,
        modifiers = modifiers,
        onDismiss = onDismiss
    )
}

class ToastScope(
    private val message: String,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var duration: Long = 3000
    var severity: ToastSeverity = ToastSeverity.Info
    var position: ToastPosition = ToastPosition.BottomCenter
    var action: ToastAction? = null

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

class AlertScope(
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

class ProgressBarScope(
    private val value: Float,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
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

class SpinnerScope(
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

class BadgeScope(
    private val content: String,
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

class TooltipScope(
    private val content: String,
    private val id: String? = null,
    private val style: ComponentStyle? = null
) : ComponentScope() {
    var position: TooltipPosition = TooltipPosition.Top
    private var child: Component? = null

    fun Text(text: String, builder: (TextScope.() -> Unit)? = null) {
        val scope = TextScope(text)
        builder?.invoke(scope)
        child = scope.build()
    }

    fun Button(text: String, builder: (ButtonScope.() -> Unit)? = null) {
        val scope = ButtonScope(text)
        builder?.invoke(scope)
        child = scope.build()
    }

    fun Icon(name: String, builder: (IconScope.() -> Unit)? = null) {
        val scope = IconScope(name)
        builder?.invoke(scope)
        child = scope.build()
    }

    internal fun build() = TooltipComponent(
        content = content,
        position = position,
        child = child ?: TextComponent(
            text = "",
            id = null,
            style = null,
            modifiers = emptyList(),
            font = Font.Body,
            color = Color.Black,
            textAlign = TextScope.TextAlign.Start,
            maxLines = null,
            overflow = TextScope.TextOverflow.Clip
        ),
        id = id,
        style = style,
        modifiers = modifiers
    )
}
