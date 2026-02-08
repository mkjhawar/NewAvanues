package com.augmentalis.avamagic.components.adapters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.glass.*
import com.augmentalis.avamagic.coretypes.*
import com.augmentalis.avamagic.state.rememberMagicState
import com.augmentalis.avamagic.core.*
import com.augmentalis.avamagic.components.*
import com.augmentalis.avamagic.ui.core.data.*
import com.augmentalis.avamagic.ui.core.feedback.*
import com.augmentalis.avamagic.components.forms.*
import com.augmentalis.avamagic.ui.core.layout.*
import com.augmentalis.avamagic.dsl.*

/**
 * ComposeRenderer - Renders Core components using Foundation @Composables
 *
 * This is the Android/Desktop renderer that bridges the gap between
 * platform-agnostic Core definitions and native Compose implementations.
 *
 * Usage:
 * ```kotlin
 * val ui = AvaUI {
 *     Button("Click Me") { onClick = { } }
 * }
 *
 * val renderer = ComposeRenderer()
 * ui.render(renderer)  // → MagicButton @Composable
 * ```
 *
 * Architecture:
 * - Core components are platform-agnostic data models
 * - Foundation components are production-ready @Composables
 * - ComposeRenderer bridges Core → Foundation
 *
 * World-Class Pattern:
 * This follows the React Native / Flutter / .NET MAUI pattern:
 * - Single source of truth (Core components)
 * - Native renderers for each platform (ComposeRenderer for Android/Desktop)
 * - 100% native UI with no cross-platform tax
 */
class ComposeRenderer : Renderer {
    override val platform: Platform = Platform.Android

    private var currentTheme: Theme? = null

    override fun applyTheme(theme: Theme) {
        currentTheme = theme
    }

    @Composable
    override fun render(component: Component): Any {
        return when (component) {
            // Basic components
            is ButtonComponent -> renderButton(component)
            is TextComponent -> renderText(component)
            is TextFieldComponent -> renderTextField(component)
            is IconComponent -> renderIcon(component)
            is ImageComponent -> renderImage(component)

            // Containers
            is CardComponent -> renderCard(component)
            is ChipComponent -> renderChip(component)
            is DividerComponent -> renderDivider(component)
            is BadgeComponent -> renderBadge(component)

            // Layouts
            is ColumnComponent -> renderColumn(component)
            is RowComponent -> renderRow(component)
            is ContainerComponent -> renderContainer(component)
            is ScrollViewComponent -> renderScrollView(component)

            // Lists
            is ListComponent -> renderList(component)

            // Forms
            is CheckboxComponent -> renderCheckbox(component)
            is SwitchComponent -> renderSwitch(component)
            is SliderComponent -> renderSlider(component)
            is RadioComponent -> renderRadio(component)
            is DropdownComponent -> renderDropdown(component)

            // Feedback
            is DialogComponent -> renderDialog(component)
            is ToastComponent -> renderToast(component)
            is AlertComponent -> renderAlert(component)
            is ProgressBarComponent -> renderProgressBar(component)
            is SpinnerComponent -> renderSpinner(component)

            else -> error("Unsupported component type: ${component::class.simpleName}")
        }
    }

    // ==================== Basic Components ====================

    @Composable
    private fun renderButton(button: ButtonComponent) {
        MagicButton(
            text = button.text,
            onClick = button.onClick ?: {},
            variant = when (button.buttonStyle) {
                ButtonScope.ButtonStyle.Primary -> ButtonVariant.Filled
                ButtonScope.ButtonStyle.Secondary -> ButtonVariant.Tonal
                ButtonScope.ButtonStyle.Outlined -> ButtonVariant.Outlined
                ButtonScope.ButtonStyle.Text -> ButtonVariant.Text
                ButtonScope.ButtonStyle.Tertiary -> ButtonVariant.Text
            },
            enabled = button.enabled,
            icon = button.leadingIcon?.let {
                { MagicIcon(it) }
            },
            modifier = applyModifiers(button)
        )
    }

    @Composable
    private fun renderText(text: TextComponent) {
        MagicText(
            text = text.text,
            style = mapFont(text.font),
            color = text.color?.let { MagicColor(it.toComposeColor()) },
            textAlign = mapTextAlign(text.textAlign),
            maxLines = text.maxLines ?: Int.MAX_VALUE,
            overflow = mapTextOverflow(text.overflow),
            modifier = applyModifiers(text)
        )
    }

    @Composable
    private fun renderTextField(textField: TextFieldComponent) {
        val state = rememberMagicState(textField.value)

        // Sync state changes back to onValueChange
        LaunchedEffect(state.value) {
            if (state.value != textField.value) {
                textField.onValueChange?.invoke(state.value)
            }
        }

        MagicTextField(
            state = state,
            label = textField.label,
            placeholder = textField.placeholder,
            helperText = if (textField.isError) null else null,
            errorText = if (textField.isError) textField.errorMessage else null,
            leadingIcon = textField.leadingIcon?.let {
                { MagicIcon(it) }
            },
            trailingIcon = textField.trailingIcon?.let {
                { MagicIcon(it) }
            },
            enabled = textField.enabled,
            readOnly = textField.readOnly,
            modifier = applyModifiers(textField)
        )
    }

    @Composable
    private fun renderIcon(icon: IconComponent) {
        MagicIcon(
            name = icon.name,
            tint = icon.tint?.let { MagicColor(it.toComposeColor()) },
            contentDescription = icon.contentDescription,
            modifier = applyModifiers(icon)
        )
    }

    @Composable
    private fun renderImage(image: ImageComponent) {
        MagicImage(
            source = image.source,
            contentDescription = image.contentDescription,
            contentScale = mapContentScale(image.contentScale),
            modifier = applyModifiers(image)
        )
    }

    // ==================== Containers ====================

    @Composable
    private fun renderCard(card: CardComponent) {
        MagicCard(
            variant = when (card.elevation) {
                0 -> CardVariant.Outlined
                1 -> CardVariant.Filled
                else -> CardVariant.Elevated
            },
            modifier = applyModifiers(card)
        ) {
            card.children.forEach { child ->
                render(child)
            }
        }
    }

    @Composable
    private fun renderChip(chip: ChipComponent) {
        MagicChip(
            text = chip.label,
            onClick = chip.onClick,
            leadingIcon = chip.icon?.let {
                { MagicIcon(it) }
            },
            trailingIcon = if (chip.deletable && chip.onDelete != null) {
                { MagicIcon("close", onClick = chip.onDelete) }
            } else null,
            variant = if (chip.selected) ChipVariant.Filled else ChipVariant.Outlined,
            modifier = applyModifiers(chip)
        )
    }

    @Composable
    private fun renderDivider(divider: DividerComponent) {
        MagicDivider(
            orientation = when (divider.orientation) {
                Orientation.Horizontal -> DividerOrientation.Horizontal
                Orientation.Vertical -> DividerOrientation.Vertical
            },
            thickness = divider.thickness?.dp ?: 1.dp,
            color = divider.color?.let { MagicColor(it.toComposeColor()) },
            modifier = applyModifiers(divider)
        )
    }

    @Composable
    private fun renderBadge(badge: BadgeComponent) {
        MagicBadge(
            content = badge.content,
            modifier = applyModifiers(badge)
        )
    }

    // ==================== Layouts ====================

    @Composable
    private fun renderColumn(column: ColumnComponent) {
        V(
            spacing = mapSpacing(column.arrangement),
            horizontalAlignment = mapHorizontalAlignment(column.horizontalAlignment),
            modifier = applyModifiers(column)
        ) {
            column.children.forEach { child ->
                render(child)
            }
        }
    }

    @Composable
    private fun renderRow(row: RowComponent) {
        H(
            spacing = mapSpacing(row.arrangement),
            verticalAlignment = mapVerticalAlignment(row.verticalAlignment),
            modifier = applyModifiers(row)
        ) {
            row.children.forEach { child ->
                render(child)
            }
        }
    }

    @Composable
    private fun renderContainer(container: ContainerComponent) {
        MagicBox(
            contentAlignment = mapAlignment(container.alignment),
            modifier = applyModifiers(container)
        ) {
            container.child?.let { render(it) }
        }
    }

    @Composable
    private fun renderScrollView(scroll: ScrollViewComponent) {
        when (scroll.orientation) {
            Orientation.Vertical -> MagicScroll(modifier = applyModifiers(scroll)) {
                scroll.child?.let { render(it) }
            }
            Orientation.Horizontal -> MagicScrollH(modifier = applyModifiers(scroll)) {
                scroll.child?.let { render(it) }
            }
        }
    }

    // ==================== Lists ====================

    @Composable
    private fun renderList(list: ListComponent) {
        V(modifier = applyModifiers(list)) {
            list.items.forEach { item ->
                MagicListItem(
                    headline = item.headline,
                    supporting = item.supporting,
                    overline = item.overline,
                    leading = item.leadingIcon?.let {
                        { MagicIcon(it) }
                    },
                    trailing = item.trailingIcon?.let {
                        { MagicIcon(it) }
                    },
                    onClick = item.onClick
                )
                if (list.showDividers) {
                    MagicDivider()
                }
            }
        }
    }

    // ==================== Forms ====================

    @Composable
    private fun renderCheckbox(checkbox: CheckboxComponent) {
        var checked by remember { mutableStateOf(checkbox.checked) }

        Row(
            modifier = applyModifiers(checkbox),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    checkbox.onCheckedChange?.invoke(it)
                },
                enabled = checkbox.enabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            MagicText(checkbox.label, style = TextVariant.BodyMedium)
        }
    }

    @Composable
    private fun renderSwitch(switch: SwitchComponent) {
        var checked by remember { mutableStateOf(switch.checked) }

        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                switch.onCheckedChange?.invoke(it)
            },
            enabled = switch.enabled,
            modifier = applyModifiers(switch)
        )
    }

    @Composable
    private fun renderSlider(slider: SliderComponent) {
        var value by remember { mutableStateOf(slider.value) }

        Column(modifier = applyModifiers(slider)) {
            if (slider.showLabel) {
                MagicText(
                    text = slider.labelFormatter?.invoke(value) ?: value.toString(),
                    style = TextVariant.LabelSmall
                )
            }
            Slider(
                value = value,
                onValueChange = {
                    value = it
                    slider.onValueChange?.invoke(it)
                },
                valueRange = slider.valueRange,
                steps = slider.steps
            )
        }
    }

    @Composable
    private fun renderRadio(radio: RadioComponent) {
        var selected by remember { mutableStateOf(radio.selectedValue) }

        when (radio.orientation) {
            Orientation.Vertical -> V(modifier = applyModifiers(radio)) {
                radio.options.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selected == option.value,
                            onClick = {
                                selected = option.value
                                radio.onValueChange?.invoke(option.value)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MagicText(option.label, style = TextVariant.BodyMedium)
                    }
                }
            }
            Orientation.Horizontal -> H(modifier = applyModifiers(radio)) {
                radio.options.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selected == option.value,
                            onClick = {
                                selected = option.value
                                radio.onValueChange?.invoke(option.value)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MagicText(option.label, style = TextVariant.BodyMedium)
                    }
                }
            }
        }
    }

    @Composable
    private fun renderDropdown(dropdown: DropdownComponent) {
        var expanded by remember { mutableStateOf(false) }
        var selectedValue by remember { mutableStateOf(dropdown.selectedValue) }

        Box(modifier = applyModifiers(dropdown)) {
            OutlinedButton(onClick = { expanded = true }) {
                MagicText(
                    text = selectedValue ?: dropdown.placeholder,
                    style = TextVariant.BodyMedium
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                dropdown.options.forEach { option ->
                    DropdownMenuItem(
                        text = { MagicText(option.label, style = TextVariant.BodyMedium) },
                        onClick = {
                            selectedValue = option.value
                            dropdown.onValueChange?.invoke(option.value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    // ==================== Feedback ====================

    @Composable
    private fun renderDialog(dialog: DialogComponent) {
        if (dialog.isOpen) {
            AlertDialog(
                onDismissRequest = {
                    if (dialog.dismissible) {
                        dialog.onDismiss?.invoke()
                    }
                },
                title = dialog.title?.let {
                    { MagicText(it, style = TextVariant.HeadlineSmall) }
                },
                text = dialog.content?.let {
                    { render(it) }
                },
                confirmButton = {
                    dialog.actions.forEach { action ->
                        TextButton(onClick = action.onClick) {
                            MagicText(action.label, style = TextVariant.LabelLarge)
                        }
                    }
                },
                modifier = applyModifiers(dialog)
            )
        }
    }

    @Composable
    private fun renderToast(toast: ToastComponent) {
        // Note: Material3 doesn't have built-in Toast/Snackbar
        // This would need SnackbarHost integration
        Snackbar(modifier = applyModifiers(toast)) {
            MagicText(toast.message, style = TextVariant.BodyMedium)
        }
    }

    @Composable
    private fun renderAlert(alert: AlertComponent) {
        Card(
            modifier = applyModifiers(alert),
            colors = CardDefaults.cardColors(
                containerColor = when (alert.severity) {
                    AlertSeverity.Info -> MaterialTheme.colorScheme.secondaryContainer
                    AlertSeverity.Success -> MaterialTheme.colorScheme.tertiaryContainer
                    AlertSeverity.Warning -> MaterialTheme.colorScheme.errorContainer
                    AlertSeverity.Error -> MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                alert.icon?.let {
                    MagicIcon(it)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    MagicText(alert.title, style = TextVariant.TitleMedium)
                    MagicText(alert.message, style = TextVariant.BodyMedium)
                }
                if (alert.dismissible) {
                    IconButton(onClick = { alert.onDismiss?.invoke() }) {
                        MagicIcon("close")
                    }
                }
            }
        }
    }

    @Composable
    private fun renderProgressBar(progress: ProgressBarComponent) {
        Column(modifier = applyModifiers(progress)) {
            if (progress.indeterminate) {
                LinearProgressIndicator()
            } else {
                LinearProgressIndicator(progress = progress.value)
            }
            if (progress.showLabel) {
                MagicText(
                    text = progress.labelFormatter?.invoke(progress.value)
                        ?: "${(progress.value * 100).toInt()}%",
                    style = TextVariant.LabelSmall
                )
            }
        }
    }

    @Composable
    private fun renderSpinner(spinner: SpinnerComponent) {
        Column(
            modifier = applyModifiers(spinner),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = when (spinner.size) {
                    SpinnerSize.Small -> Modifier.size(24.dp)
                    SpinnerSize.Medium -> Modifier.size(40.dp)
                    SpinnerSize.Large -> Modifier.size(56.dp)
                }
            )
            spinner.label?.let {
                Spacer(modifier = Modifier.height(8.dp))
                MagicText(it, style = TextVariant.BodySmall)
            }
        }
    }

    // ==================== Helper Methods ====================

    @Composable
    private fun applyModifiers(component: Component): Modifier {
        var modifier = Modifier

        // Apply ComponentStyle
        component.style?.let { style ->
            style.width?.let { modifier = modifier.width(it.dp) }
            style.height?.let { modifier = modifier.height(it.dp) }
            style.padding?.let { modifier = modifier.padding(it.dp) }
            style.margin?.let { modifier = modifier.padding(it.dp) }  // Margin as padding
            style.backgroundColor?.let {
                modifier = modifier.background(MagicColor(it.toComposeColor()).value)
            }
        }

        return modifier
    }

    private fun mapFont(font: Font): TextVariant {
        return when (font) {
            Font.DisplayLarge -> TextVariant.DisplayLarge
            Font.DisplayMedium -> TextVariant.DisplayMedium
            Font.DisplaySmall -> TextVariant.DisplaySmall
            Font.HeadlineLarge -> TextVariant.HeadlineLarge
            Font.HeadlineMedium -> TextVariant.HeadlineMedium
            Font.HeadlineSmall -> TextVariant.HeadlineSmall
            Font.TitleLarge -> TextVariant.TitleLarge
            Font.TitleMedium -> TextVariant.TitleMedium
            Font.TitleSmall -> TextVariant.TitleSmall
            Font.BodyLarge -> TextVariant.BodyLarge
            Font.BodyMedium -> TextVariant.BodyMedium
            Font.BodySmall -> TextVariant.BodySmall
            Font.LabelLarge -> TextVariant.LabelLarge
            Font.LabelMedium -> TextVariant.LabelMedium
            Font.LabelSmall -> TextVariant.LabelSmall
            else -> TextVariant.BodyMedium
        }
    }

    private fun mapTextAlign(textAlign: TextScope.TextAlign): TextAlign {
        return when (textAlign) {
            TextScope.TextAlign.Start -> TextAlign.Start
            TextScope.TextAlign.Center -> TextAlign.Center
            TextScope.TextAlign.End -> TextAlign.End
            TextScope.TextAlign.Justify -> TextAlign.Justify
        }
    }

    private fun mapTextOverflow(overflow: TextScope.TextOverflow): TextOverflow {
        return when (overflow) {
            TextScope.TextOverflow.Clip -> TextOverflow.Clip
            TextScope.TextOverflow.Ellipsis -> TextOverflow.Ellipsis
            TextScope.TextOverflow.Visible -> TextOverflow.Visible
        }
    }

    private fun mapContentScale(scale: ImageScope.ContentScale): androidx.compose.ui.layout.ContentScale {
        return when (scale) {
            ImageScope.ContentScale.Fit -> androidx.compose.ui.layout.ContentScale.Fit
            ImageScope.ContentScale.Fill -> androidx.compose.ui.layout.ContentScale.FillBounds
            ImageScope.ContentScale.Crop -> androidx.compose.ui.layout.ContentScale.Crop
            ImageScope.ContentScale.None -> androidx.compose.ui.layout.ContentScale.None
        }
    }

    private fun mapSpacing(arrangement: Arrangement): androidx.compose.ui.unit.Dp {
        return when (arrangement) {
            Arrangement.Start -> 0.dp
            Arrangement.SpaceBetween -> 0.dp
            Arrangement.SpaceAround -> 0.dp
            Arrangement.SpaceEvenly -> 0.dp
            Arrangement.End -> 0.dp
            Arrangement.Center -> 0.dp
            is Arrangement.SpacedBy -> arrangement.space.dp
        }
    }

    private fun mapHorizontalAlignment(alignment: com.augmentalis.avamagic.core.Alignment): Alignment.Horizontal {
        return when (alignment) {
            com.augmentalis.avamagic.core.Alignment.Start,
            com.augmentalis.avamagic.core.Alignment.TopStart,
            com.augmentalis.avamagic.core.Alignment.CenterStart,
            com.augmentalis.avamagic.core.Alignment.BottomStart -> Alignment.Start
            com.augmentalis.avamagic.core.Alignment.End,
            com.augmentalis.avamagic.core.Alignment.TopEnd,
            com.augmentalis.avamagic.core.Alignment.CenterEnd,
            com.augmentalis.avamagic.core.Alignment.BottomEnd -> Alignment.End
            else -> Alignment.CenterHorizontally
        }
    }

    private fun mapVerticalAlignment(alignment: com.augmentalis.avamagic.core.Alignment): Alignment.Vertical {
        return when (alignment) {
            com.augmentalis.avamagic.core.Alignment.Top,
            com.augmentalis.avamagic.core.Alignment.TopStart,
            com.augmentalis.avamagic.core.Alignment.TopCenter,
            com.augmentalis.avamagic.core.Alignment.TopEnd -> Alignment.Top
            com.augmentalis.avamagic.core.Alignment.Bottom,
            com.augmentalis.avamagic.core.Alignment.BottomStart,
            com.augmentalis.avamagic.core.Alignment.BottomCenter,
            com.augmentalis.avamagic.core.Alignment.BottomEnd -> Alignment.Bottom
            else -> Alignment.CenterVertically
        }
    }

    private fun mapAlignment(alignment: com.augmentalis.avamagic.core.Alignment): Alignment {
        return when (alignment) {
            com.augmentalis.avamagic.core.Alignment.TopStart -> Alignment.TopStart
            com.augmentalis.avamagic.core.Alignment.TopCenter -> Alignment.TopCenter
            com.augmentalis.avamagic.core.Alignment.TopEnd -> Alignment.TopEnd
            com.augmentalis.avamagic.core.Alignment.CenterStart -> Alignment.CenterStart
            com.augmentalis.avamagic.core.Alignment.Center -> Alignment.Center
            com.augmentalis.avamagic.core.Alignment.CenterEnd -> Alignment.CenterEnd
            com.augmentalis.avamagic.core.Alignment.BottomStart -> Alignment.BottomStart
            com.augmentalis.avamagic.core.Alignment.BottomCenter -> Alignment.BottomCenter
            com.augmentalis.avamagic.core.Alignment.BottomEnd -> Alignment.BottomEnd
            else -> Alignment.TopStart
        }
    }

    private fun com.augmentalis.avamagic.core.Color.toComposeColor(): Color {
        return Color(red, green, blue, alpha)
    }
}
