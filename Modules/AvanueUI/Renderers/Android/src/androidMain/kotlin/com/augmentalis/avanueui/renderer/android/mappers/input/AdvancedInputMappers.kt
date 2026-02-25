package com.augmentalis.avanueui.renderer.android.mappers.input

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.ui.core.form.*
import com.augmentalis.avanueui.core.Orientation
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.IconResolver
import com.augmentalis.avanueui.renderer.android.ModifierConverter
import com.augmentalis.avanueui.renderer.android.toComposeColor
import com.augmentalis.avanueui.theme.AvanueTheme

class SegmentedButtonMapper : ComponentMapper<SegmentedButtonComponent> {
    private val modifierConverter = ModifierConverter()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun map(component: SegmentedButtonComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            if (component.multiSelect) {
                MultiChoiceSegmentedButtonRow(
                    modifier = modifierConverter.convert(component.modifiers)
                ) {
                    component.segments.forEachIndexed { index, segment ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = component.segments.size
                            ),
                            onCheckedChange = { checked ->
                                val newSelection = if (checked) {
                                    component.selectedIndices + index
                                } else {
                                    component.selectedIndices - index
                                }
                                component.onSelectionChanged?.invoke(newSelection)
                            },
                            checked = index in component.selectedIndices,
                            enabled = segment.enabled && component.enabled
                        ) {
                            Text(segment.label)
                        }
                    }
                }
            } else {
                SingleChoiceSegmentedButtonRow(
                    modifier = modifierConverter.convert(component.modifiers)
                ) {
                    component.segments.forEachIndexed { index, segment ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = component.segments.size
                            ),
                            onClick = { component.onSelectionChanged?.invoke(listOf(index)) },
                            selected = index in component.selectedIndices,
                            enabled = segment.enabled && component.enabled
                        ) {
                            Text(segment.label)
                        }
                    }
                }
            }
        }
    }
}

class TextButtonMapper : ComponentMapper<TextButtonComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: TextButtonComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            TextButton(
                onClick = { component.onClick?.invoke() },
                modifier = modifierConverter.convert(component.modifiers),
                enabled = component.enabled
            ) {
                if (component.icon != null && component.iconPosition == IconPosition.Start) {
                    Icon(IconResolver.resolve(component.icon), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(component.label)
                if (component.icon != null && component.iconPosition == IconPosition.End) {
                    Spacer(Modifier.width(8.dp))
                    Icon(IconResolver.resolve(component.icon), contentDescription = null)
                }
            }
        }
    }
}

class OutlinedButtonMapper : ComponentMapper<OutlinedButtonComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: OutlinedButtonComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            OutlinedButton(
                onClick = { component.onClick?.invoke() },
                modifier = modifierConverter.convert(component.modifiers),
                enabled = component.enabled
            ) {
                if (component.icon != null && component.iconPosition == IconPosition.Start) {
                    Icon(IconResolver.resolve(component.icon), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(component.label)
                if (component.icon != null && component.iconPosition == IconPosition.End) {
                    Spacer(Modifier.width(8.dp))
                    Icon(IconResolver.resolve(component.icon), contentDescription = null)
                }
            }
        }
    }
}

class FilledButtonMapper : ComponentMapper<FilledButtonComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: FilledButtonComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Button(
                onClick = { component.onClick?.invoke() },
                modifier = modifierConverter.convert(component.modifiers),
                enabled = component.enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = component.containerColor?.toComposeColor()
                        ?: AvanueTheme.colors.primary,
                    contentColor = component.contentColor?.toComposeColor()
                        ?: AvanueTheme.colors.onPrimary
                )
            ) {
                if (component.icon != null && component.iconPosition == IconPosition.Start) {
                    Icon(IconResolver.resolve(component.icon), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(component.label)
                if (component.icon != null && component.iconPosition == IconPosition.End) {
                    Spacer(Modifier.width(8.dp))
                    Icon(IconResolver.resolve(component.icon), contentDescription = null)
                }
            }
        }
    }
}

class IconButtonMapper : ComponentMapper<IconButtonComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: IconButtonComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val icon = IconResolver.resolve(component.icon)
            when (component.variant) {
                IconButtonVariant.STANDARD -> IconButton(
                    onClick = { component.onClick?.invoke() },
                    modifier = modifierConverter.convert(component.modifiers),
                    enabled = component.enabled
                ) {
                    Icon(icon, contentDescription = component.contentDescription)
                }
                IconButtonVariant.FILLED -> FilledIconButton(
                    onClick = { component.onClick?.invoke() },
                    modifier = modifierConverter.convert(component.modifiers),
                    enabled = component.enabled
                ) {
                    Icon(icon, contentDescription = component.contentDescription)
                }
                IconButtonVariant.FILLED_TONAL -> FilledTonalIconButton(
                    onClick = { component.onClick?.invoke() },
                    modifier = modifierConverter.convert(component.modifiers),
                    enabled = component.enabled
                ) {
                    Icon(icon, contentDescription = component.contentDescription)
                }
                IconButtonVariant.OUTLINED -> OutlinedIconButton(
                    onClick = { component.onClick?.invoke() },
                    modifier = modifierConverter.convert(component.modifiers),
                    enabled = component.enabled
                ) {
                    Icon(icon, contentDescription = component.contentDescription)
                }
            }
        }
    }
}

class ImagePickerMapper : ComponentMapper<ImagePickerComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ImagePickerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val launcher = rememberLauncherForActivityResult(
                contract = if (component.allowMultiple) {
                    ActivityResultContracts.GetMultipleContents()
                } else {
                    ActivityResultContracts.GetContent()
                }
            ) { result ->
                when (result) {
                    is android.net.Uri -> component.onImageSelected?.invoke(listOf(result.toString()))
                    is List<*> -> component.onImageSelected?.invoke(result.mapNotNull { (it as? android.net.Uri)?.toString() })
                    else -> {}
                }
            }

            val mimeType = when (component.sourceType) {
                ImageSourceType.GALLERY -> "image/*"
                ImageSourceType.CAMERA -> "image/*"
                ImageSourceType.BOTH -> "image/*"
            }

            OutlinedButton(
                onClick = { launcher.launch(mimeType) },
                modifier = modifierConverter.convert(component.modifiers),
                enabled = component.enabled
            ) {
                Icon(IconResolver.resolve("image"), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(component.label ?: "Select Image")
            }
        }
    }
}

class ColorPickerMapper : ComponentMapper<ColorPickerComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ColorPickerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var selectedColor by remember {
                mutableStateOf(
                    component.selectedColor?.toComposeColor() ?: Color.Blue
                )
            }
            var showDialog by remember { mutableStateOf(false) }

            // Color preview button
            Row(
                modifier = modifierConverter.convert(component.modifiers),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                component.label?.let {
                    Text(it)
                }

                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { showDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    color = selectedColor,
                    shadowElevation = 2.dp
                ) {}
            }

            // Color picker dialog
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text(component.label ?: "Select Color") },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Predefined colors grid
                            val colors = listOf(
                                Color.Red, Color.Green, Color.Blue, Color.Yellow,
                                Color.Cyan, Color.Magenta, Color(0xFFFFA500), Color(0xFF800080),
                                Color.Black, Color.Gray, Color.White, Color(0xFF8B4513)
                            )

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(120.dp)
                            ) {
                                items(colors.size) { index ->
                                    Surface(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable {
                                                selectedColor = colors[index]
                                                component.onColorSelected?.invoke(
                                                    colorToHex(colors[index])
                                                )
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors[index],
                                        border = if (selectedColor == colors[index]) {
                                            BorderStroke(2.dp, Color.Black)
                                        } else null
                                    ) {}
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Done")
                        }
                    }
                )
            }
        }
    }

    private fun colorToHex(color: Color): String {
        val red = (color.red * 255).toInt()
        val green = (color.green * 255).toInt()
        val blue = (color.blue * 255).toInt()
        return String.format("#%02X%02X%02X", red, green, blue)
    }
}

class MultiSelectMapper : ComponentMapper<MultiSelectComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: MultiSelectComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var expanded by remember { mutableStateOf(false) }
            var searchQuery by remember { mutableStateOf("") }

            Column(modifier = modifierConverter.convert(component.modifiers)) {
                component.label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (component.enabled) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = if (component.selectedValues.isEmpty()) {
                            component.placeholder
                        } else {
                            "${component.selectedValues.size} selected"
                        },
                        onValueChange = {},
                        readOnly = true,
                        enabled = component.enabled,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (component.searchable) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                singleLine = true
                            )
                        }

                        val filteredOptions = component.options.filter { (_, label) ->
                            searchQuery.isEmpty() || label.contains(searchQuery, ignoreCase = true)
                        }

                        filteredOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = value in component.selectedValues,
                                            onCheckedChange = null
                                        )
                                        Text(label)
                                    }
                                },
                                onClick = {
                                    // Toggle selection
                                    val canSelect = component.maxSelections == null ||
                                        component.selectedValues.size < component.maxSelections!! ||
                                        value in component.selectedValues
                                    if (canSelect) {
                                        component.onSelectionChanged?.invoke(
                                            if (value in component.selectedValues) {
                                                component.selectedValues - value
                                            } else {
                                                component.selectedValues + value
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

class DateRangePickerMapper : ComponentMapper<DateRangePickerComponent> {
    private val modifierConverter = ModifierConverter()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun map(component: DateRangePickerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var showDialog by remember { mutableStateOf(false) }
            val dateRangePickerState = rememberDateRangePickerState()

            Column(modifier = modifierConverter.convert(component.modifiers)) {
                component.label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                OutlinedTextField(
                    value = buildString {
                        if (component.startDate != null && component.endDate != null) {
                            append("${component.startDate} - ${component.endDate}")
                        } else if (component.startDate != null) {
                            append("From: ${component.startDate}")
                        } else if (component.endDate != null) {
                            append("To: ${component.endDate}")
                        } else {
                            append("Select date range")
                        }
                    },
                    onValueChange = {},
                    readOnly = true,
                    enabled = component.enabled,
                    trailingIcon = {
                        IconButton(onClick = { if (component.enabled) showDialog = true }) {
                            Icon(
                                imageVector = IconResolver.resolve("calendar_today"),
                                contentDescription = "Select date range"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (component.enabled) showDialog = true }
                )

                if (showDialog) {
                    DatePickerDialog(
                        onDismissRequest = { showDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                showDialog = false
                                // Notify about selection changes
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    ) {
                        DateRangePicker(
                            state = dateRangePickerState,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

class TagInputMapper : ComponentMapper<TagInputComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: TagInputComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var inputValue by remember { mutableStateOf(component.inputValue) }

            Column(modifier = modifierConverter.convert(component.modifiers)) {
                component.label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Display tags
                if (component.tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        component.tags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { },
                                label = { Text(tag) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { component.onTagRemoved?.invoke(tag) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = IconResolver.resolve("close"),
                                            contentDescription = "Remove $tag",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Input field
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    placeholder = { Text(component.placeholder) },
                    enabled = component.enabled && (component.maxTags == null || component.tags.size < component.maxTags!!),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            if (inputValue.isNotBlank()) {
                                val canAdd = (component.allowDuplicates || inputValue !in component.tags) &&
                                    (component.maxTags == null || component.tags.size < component.maxTags!!)
                                if (canAdd) {
                                    component.onTagAdded?.invoke(inputValue)
                                    inputValue = ""
                                }
                            }
                        }
                    )
                )

                // Show count if max is set
                component.maxTags?.let { max ->
                    Text(
                        text = "${component.tags.size}/$max tags",
                        style = MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

class ToggleMapper : ComponentMapper<ToggleComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ToggleComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Row(
                modifier = modifierConverter.convert(component.modifiers)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = component.label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    component.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = AvanueTheme.colors.textSecondary
                        )
                    }
                }

                Switch(
                    checked = component.checked,
                    onCheckedChange = { component.onCheckedChange?.invoke(it) },
                    enabled = component.enabled
                )
            }
        }
    }
}

class ToggleButtonGroupMapper : ComponentMapper<ToggleButtonGroupComponent> {
    private val modifierConverter = ModifierConverter()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun map(component: ToggleButtonGroupComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            if (component.multiSelect) {
                MultiChoiceSegmentedButtonRow(
                    modifier = modifierConverter.convert(component.modifiers)
                ) {
                    component.options.forEachIndexed { index, (icon, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = component.options.size
                            ),
                            onCheckedChange = { checked ->
                                val newSelection = if (checked) {
                                    component.selectedIndices + index
                                } else {
                                    component.selectedIndices - index
                                }
                                component.onSelectionChanged?.invoke(newSelection)
                            },
                            checked = index in component.selectedIndices,
                            enabled = component.enabled,
                            icon = if (icon != label) {
                                { Icon(IconResolver.resolve(icon), contentDescription = null) }
                            } else null
                        ) {
                            Text(label)
                        }
                    }
                }
            } else {
                SingleChoiceSegmentedButtonRow(
                    modifier = modifierConverter.convert(component.modifiers)
                ) {
                    component.options.forEachIndexed { index, (icon, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = component.options.size
                            ),
                            onClick = {
                                val newSelection = if (index in component.selectedIndices) {
                                    emptySet()
                                } else {
                                    setOf(index)
                                }
                                component.onSelectionChanged?.invoke(newSelection)
                            },
                            selected = index in component.selectedIndices,
                            enabled = component.enabled,
                            icon = if (icon != label) {
                                { Icon(IconResolver.resolve(icon), contentDescription = null) }
                            } else null
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}

class StepperMapper : ComponentMapper<StepperComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: StepperComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val arrangement = if (component.orientation == Orientation.Horizontal) {
                Arrangement.spacedBy(8.dp)
            } else {
                Arrangement.spacedBy(4.dp)
            }

            if (component.orientation == Orientation.Horizontal) {
                Row(
                    modifier = modifierConverter.convert(component.modifiers),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = arrangement
                ) {
                    component.label?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium)
                    }

                    FilledIconButton(
                        onClick = { component.onValueChanged?.invoke(component.decrement().value) },
                        enabled = component.enabled && component.canDecrement
                    ) {
                        Icon(IconResolver.resolve("remove"), contentDescription = "Decrease")
                    }

                    Text(
                        text = component.value.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    FilledIconButton(
                        onClick = { component.onValueChanged?.invoke(component.increment().value) },
                        enabled = component.enabled && component.canIncrement
                    ) {
                        Icon(IconResolver.resolve("add"), contentDescription = "Increase")
                    }
                }
            } else {
                Column(
                    modifier = modifierConverter.convert(component.modifiers),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    component.label?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium)
                    }

                    FilledIconButton(
                        onClick = { component.onValueChanged?.invoke(component.increment().value) },
                        enabled = component.enabled && component.canIncrement
                    ) {
                        Icon(IconResolver.resolve("add"), contentDescription = "Increase")
                    }

                    Text(
                        text = component.value.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    FilledIconButton(
                        onClick = { component.onValueChanged?.invoke(component.decrement().value) },
                        enabled = component.enabled && component.canDecrement
                    ) {
                        Icon(IconResolver.resolve("remove"), contentDescription = "Decrease")
                    }
                }
            }
        }
    }
}

class IconPickerMapper : ComponentMapper<IconPickerComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: IconPickerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var showDialog by remember { mutableStateOf(false) }
            var searchQuery by remember { mutableStateOf("") }

            Column(modifier = modifierConverter.convert(component.modifiers)) {
                component.label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Current icon display
                OutlinedButton(
                    onClick = { if (component.enabled) showDialog = true },
                    enabled = component.enabled
                ) {
                    if (component.value.isNotBlank()) {
                        Icon(
                            imageVector = IconResolver.resolve(component.value),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(component.value)
                    } else {
                        Text("Select Icon")
                    }
                }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text(component.label ?: "Select Icon") },
                        text = {
                            Column {
                                if (component.showSearch) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text(component.placeholder) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }

                                val filteredIcons = component.icons.filter { icon ->
                                    searchQuery.isEmpty() ||
                                    icon.name.contains(searchQuery, ignoreCase = true) ||
                                    icon.label.contains(searchQuery, ignoreCase = true)
                                }

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(component.gridColumns),
                                    modifier = Modifier.height(300.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(filteredIcons.size) { index ->
                                        val icon = filteredIcons[index]
                                        Surface(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clickable {
                                                    component.onIconChanged?.invoke(icon.name)
                                                    showDialog = false
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (icon.name == component.value) {
                                                AvanueTheme.colors.primaryContainer
                                            } else {
                                                AvanueTheme.colors.surface
                                            }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = IconResolver.resolve(icon.name),
                                                    contentDescription = icon.label
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
            }
        }
    }
}
