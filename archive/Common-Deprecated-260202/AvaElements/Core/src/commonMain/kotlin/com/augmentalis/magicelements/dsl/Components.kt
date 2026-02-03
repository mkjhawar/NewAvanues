package com.augmentalis.avaelements.dsl

import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow
import com.augmentalis.avaelements.core.*

// ==================== Layout Components ====================

data class ColumnComponent(
    override val type: String = "Column",
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val arrangement: Arrangement,
    val horizontalAlignment: Alignment,
    val children: List<Component>
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class RowComponent(
    override val type: String = "Row",
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val arrangement: Arrangement,
    val verticalAlignment: Alignment,
    val children: List<Component>
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class ContainerComponent(
    override val type: String = "Container",
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val alignment: Alignment,
    val child: Component?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class ScrollViewComponent(
    override val type: String = "ScrollView",
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val orientation: Orientation,
    val child: Component?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class CardComponent(
    override val type: String = "Card",
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val elevation: Int,
    val children: List<Component>
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

// ==================== Basic Components ====================

data class TextComponent(
    override val type: String = "Text",
    val text: String,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val font: Font,
    val color: Color,
    val textAlign: TextScope.TextAlign,
    val maxLines: Int?,
    val overflow: TextScope.TextOverflow
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class ButtonComponent(
    override val type: String = "Button",
    val text: String,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val buttonStyle: ButtonScope.ButtonStyle,
    val enabled: Boolean,
    val onClick: (() -> Unit)?,
    val leadingIcon: String?,
    val trailingIcon: String?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class ImageComponent(
    override val type: String = "Image",
    val source: String,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val contentDescription: String?,
    val contentScale: ImageScope.ContentScale
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class CheckboxComponent(
    override val type: String = "Checkbox",
    val label: String,
    val checked: Boolean,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val enabled: Boolean,
    val onCheckedChange: ((Boolean) -> Unit)?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class TextFieldComponent(
    override val type: String = "TextField",
    val value: String,
    val placeholder: String,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val label: String?,
    val enabled: Boolean,
    val readOnly: Boolean,
    val isError: Boolean,
    val errorMessage: String?,
    val leadingIcon: String?,
    val trailingIcon: String?,
    val maxLength: Int?,
    val onValueChange: ((String) -> Unit)?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class SwitchComponent(
    override val type: String = "Switch",
    val checked: Boolean,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val enabled: Boolean,
    val onCheckedChange: ((Boolean) -> Unit)?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class IconComponent(
    override val type: String = "Icon",
    val name: String,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val tint: Color?,
    val contentDescription: String?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

// ==================== Form Components ====================

data class RadioComponent(
    override val type: String = "Radio",
    val options: List<RadioOption>,
    val selectedValue: String?,
    val groupName: String,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val orientation: Orientation,
    val onValueChange: ((String) -> Unit)?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class RadioOption(
    val value: String,
    val label: String,
    val enabled: Boolean = true
)

data class SliderComponent(
    override val type: String = "Slider",
    val value: Float,
    val valueRange: ClosedFloatingPointRange<Float>,
    val steps: Int,
    val showLabel: Boolean,
    val labelFormatter: ((Float) -> String)?,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val onValueChange: ((Float) -> Unit)?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class DropdownComponent(
    override val type: String = "Dropdown",
    val options: List<DropdownOption>,
    val selectedValue: String?,
    val placeholder: String,
    val searchable: Boolean,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val onValueChange: ((String) -> Unit)?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class DropdownOption(
    val value: String,
    val label: String,
    val icon: String?,
    val disabled: Boolean
)

data class DatePickerComponent(
    override val type: String = "DatePicker",
    val selectedDate: Long?,
    val minDate: Long?,
    val maxDate: Long?,
    val dateFormat: String,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val onDateChange: ((Long) -> Unit)?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class TimePickerComponent(
    override val type: String = "TimePicker",
    val hour: Int,
    val minute: Int,
    val is24Hour: Boolean,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val onTimeChange: ((Int, Int) -> Unit)?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class FileUploadComponent(
    override val type: String = "FileUpload",
    val accept: List<String>,
    val multiple: Boolean,
    val maxSize: Long?,
    val placeholder: String,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val onFilesSelected: ((List<FileData>) -> Unit)?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class FileData(
    val name: String,
    val size: Long,
    val type: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FileData

        if (name != other.name) return false
        if (size != other.size) return false
        if (type != other.type) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

data class SearchBarComponent(
    override val type: String = "SearchBar",
    val value: String,
    val placeholder: String,
    val showClearButton: Boolean,
    val suggestions: List<String>,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val onValueChange: ((String) -> Unit)?,
    val onSearch: ((String) -> Unit)?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class RatingComponent(
    override val type: String = "Rating",
    val value: Float,
    val maxRating: Int,
    val allowHalf: Boolean,
    val readonly: Boolean,
    val icon: String,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val onRatingChange: ((Float) -> Unit)?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

// ==================== Feedback Components ====================

data class DialogComponent(
    override val type: String = "Dialog",
    val isOpen: Boolean,
    val title: String?,
    val content: Component?,
    val actions: List<DialogAction>,
    val dismissible: Boolean,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val onDismiss: (() -> Unit)?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class DialogAction(
    val label: String,
    val style: DialogActionStyle,
    val onClick: () -> Unit
)

enum class DialogActionStyle {
    Primary, Secondary, Text, Outlined
}

data class ToastComponent(
    override val type: String = "Toast",
    val message: String,
    val duration: Long,
    val severity: ToastSeverity,
    val position: ToastPosition,
    val action: ToastAction?,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

enum class ToastSeverity {
    Success, Info, Warning, Error
}

enum class ToastPosition {
    TopLeft, TopCenter, TopRight,
    BottomLeft, BottomCenter, BottomRight
}

data class ToastAction(
    val label: String,
    val onClick: () -> Unit
)

data class AlertComponent(
    override val type: String = "Alert",
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val dismissible: Boolean,
    val icon: String?,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>,
    val onDismiss: (() -> Unit)?
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

enum class AlertSeverity {
    Success, Info, Warning, Error
}

data class ProgressBarComponent(
    override val type: String = "ProgressBar",
    val value: Float,
    val showLabel: Boolean,
    val labelFormatter: ((Float) -> String)?,
    val indeterminate: Boolean,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>
) : Component {
    init {
        require(value in 0.0f..1.0f) { "Progress value must be between 0.0 and 1.0" }
    }

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

data class SpinnerComponent(
    override val type: String = "Spinner",
    val size: SpinnerSize,
    val label: String?,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

enum class SpinnerSize {
    Small, Medium, Large
}

data class BadgeComponent(
    override val type: String = "Badge",
    val content: String,
    val variant: BadgeVariant,
    val size: BadgeSize,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

enum class BadgeVariant {
    Default, Primary, Secondary, Success, Warning, Error
}

enum class BadgeSize {
    Small, Medium, Large
}

data class TooltipComponent(
    override val type: String = "Tooltip",
    val content: String,
    val position: TooltipPosition,
    val child: Component,
    override val id: String?,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

enum class TooltipPosition {
    Top, Bottom, Left, Right
}
