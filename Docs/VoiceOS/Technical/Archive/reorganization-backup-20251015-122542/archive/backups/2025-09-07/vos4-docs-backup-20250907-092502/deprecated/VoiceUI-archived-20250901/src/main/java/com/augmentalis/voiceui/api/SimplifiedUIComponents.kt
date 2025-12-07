/**
 * SimplifiedUIComponents.kt - Missing UI components for simplified VoiceUI
 * 
 * Provides the missing components referenced in SimplifiedExamples.kt
 */

package com.augmentalis.voiceui.api

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.uuidmanager.UUIDManager

/**
 * Toggle switch component with voice support
 */
@Composable
fun toggle(
    label: String,
    checked: Boolean = false,
    locale: String? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    val uuid = remember { UUIDManager.generate() }
    var isChecked by remember { mutableStateOf(checked) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        Switch(
            checked = isChecked,
            onCheckedChange = { newValue ->
                isChecked = newValue
                onCheckedChange?.invoke(newValue)
            }
        )
    }
}

/**
 * Dropdown menu component with voice support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun dropdown(
    label: String,
    options: List<String>,
    selected: String? = null,
    locale: String? = null,
    onSelectionChange: ((String) -> Unit)? = null
) {
    val uuid = remember { UUIDManager.generate() }
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(selected ?: options.firstOrNull() ?: "") }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        selectedOption = option
                        expanded = false
                        onSelectionChange?.invoke(option)
                    }
                )
            }
        }
    }
}

/**
 * Slider component with voice support
 */
@Composable
fun slider(
    label: String,
    value: Float = 0.5f,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    locale: String? = null,
    onValueChange: ((Float) -> Unit)? = null
) {
    val uuid = remember { UUIDManager.generate() }
    var sliderValue by remember { mutableStateOf(value) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text = "$label: ${String.format("%.1f", sliderValue)}")
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                sliderValue = newValue
                onValueChange?.invoke(newValue)
            },
            valueRange = range
        )
    }
}

/**
 * Stepper (number increment/decrement) component with voice support
 */
@Composable
fun stepper(
    label: String,
    value: Int = 0,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    locale: String? = null,
    onValueChange: ((Int) -> Unit)? = null
) {
    val uuid = remember { UUIDManager.generate() }
    var stepperValue by remember { mutableStateOf(value) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (stepperValue > min) {
                        stepperValue--
                        onValueChange?.invoke(stepperValue)
                    }
                },
                enabled = stepperValue > min
            ) {
                Text("-")
            }
            Text(
                text = stepperValue.toString(),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            IconButton(
                onClick = {
                    if (stepperValue < max) {
                        stepperValue++
                        onValueChange?.invoke(stepperValue)
                    }
                },
                enabled = stepperValue < max
            ) {
                Text("+")
            }
        }
    }
}

/**
 * Radio group component with voice support
 */
@Composable
fun radioGroup(
    label: String,
    options: List<String>,
    selected: String? = null,
    locale: String? = null,
    onSelectionChange: ((String) -> Unit)? = null
) {
    val uuid = remember { UUIDManager.generate() }
    var selectedOption by remember { mutableStateOf(selected ?: options.firstOrNull() ?: "") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedOption == option),
                        onClick = {
                            selectedOption = option
                            onSelectionChange?.invoke(option)
                        }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedOption == option),
                    onClick = {
                        selectedOption = option
                        onSelectionChange?.invoke(option)
                    }
                )
                Text(
                    text = option,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * Chip group component with voice support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun chipGroup(
    label: String,
    chips: List<String>,
    selected: Set<String> = emptySet(),
    multiSelect: Boolean = false,
    locale: String? = null,
    onSelectionChange: ((Set<String>) -> Unit)? = null
) {
    val uuid = remember { UUIDManager.generate() }
    var selectedChips by remember { mutableStateOf(selected) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        if (label.isNotEmpty()) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chips.forEach { chip ->
                FilterChip(
                    selected = chip in selectedChips,
                    onClick = {
                        selectedChips = if (multiSelect) {
                            if (chip in selectedChips) {
                                selectedChips - chip
                            } else {
                                selectedChips + chip
                            }
                        } else {
                            if (chip in selectedChips) {
                                emptySet()
                            } else {
                                setOf(chip)
                            }
                        }
                        onSelectionChange?.invoke(selectedChips)
                    },
                    label = { Text(chip) }
                )
            }
        }
    }
}

/**
 * List component with voice support
 */
@Composable
fun list(
    items: List<Any>,
    locale: String? = null,
    itemContent: @Composable (Any) -> Unit
) {
    val uuid = remember { UUIDManager.generate() }
    
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items) { item ->
            itemContent(item)
        }
    }
}

/**
 * Task item component for lists
 */
@Composable
fun taskItem(
    task: Any,
    onComplete: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val uuid = remember { UUIDManager.generate() }
    var isCompleted by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { checked ->
                        isCompleted = checked
                        if (checked) onComplete?.invoke()
                    }
                )
                Text(
                    text = task.toString(),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            onDelete?.let {
                IconButton(onClick = it) {
                    Text("×", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

/**
 * Helper function for language cycling (referenced in examples)
 */
fun nextLanguage(current: String): String {
    val languages = listOf("en", "es", "fr", "de", "ja", "zh")
    val currentIndex = languages.indexOf(current)
    return if (currentIndex >= 0 && currentIndex < languages.size - 1) {
        languages[currentIndex + 1]
    } else {
        languages[0]
    }
}

/**
 * Flow Row helper (for chip layouts)
 */
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Simplified implementation - in production would use proper flow layout
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
}
