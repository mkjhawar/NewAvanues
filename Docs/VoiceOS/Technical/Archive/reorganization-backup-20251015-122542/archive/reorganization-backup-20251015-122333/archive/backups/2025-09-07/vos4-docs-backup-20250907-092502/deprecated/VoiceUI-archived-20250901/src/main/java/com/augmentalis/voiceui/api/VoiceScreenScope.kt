/**
 * VoiceScreenScope.kt - DSL scope for VoiceScreen
 * 
 * Single Responsibility: Provides DSL context for VoiceScreen content blocks
 * Follows SRP by only handling DSL scope functionality
 */

package com.augmentalis.voiceui.api

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.augmentalis.localizationmanager.LocalizationModule

/**
 * DSL scope for VoiceScreen content blocks
 * Provides simplified API for common UI components within a screen
 */
class VoiceScreenScope {
    
    @Composable
    fun text(
        content: String,
        locale: String? = null,
        aiContext: AIContext? = null
    ) {
        // Apply locale and AI context
        val localizedContent = locale?.let { 
            LocalizationModule.getInstance(LocalContext.current)
                .translate(content, it)
        } ?: content
        aiContext?.let { AIContextManager.setContext(content.hashCode().toString(), it) }
        
        Text(text = localizedContent)
    }
    
    @Composable
    fun input(
        label: String,
        value: String = "",
        locale: String? = null,
        onValueChange: ((String) -> Unit)? = null
    ) {
        // Apply locale for label
        val localizedLabel = locale?.let {
            LocalizationModule.getInstance(LocalContext.current)
                .translate(label, it)
        } ?: label
        var textValue by remember { mutableStateOf(value) }
        
        OutlinedTextField(
            value = textValue,
            onValueChange = { 
                textValue = it
                onValueChange?.invoke(it)
            },
            label = { Text(localizedLabel) },
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    @Composable
    fun password(
        label: String = "Password",
        value: String = "",
        locale: String? = null,
        onValueChange: ((String) -> Unit)? = null
    ) {
        // Apply locale for label
        val localizedLabel = locale?.let {
            LocalizationModule.getInstance(LocalContext.current)
                .translate(label, it)
        } ?: label
        var textValue by remember { mutableStateOf(value) }
        
        OutlinedTextField(
            value = textValue,
            onValueChange = { 
                textValue = it
                onValueChange?.invoke(it)
            },
            label = { Text(localizedLabel) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    @Composable
    fun button(
        text: String,
        locale: String? = null,
        onClick: (() -> Unit)? = null
    ) {
        // Apply locale for button text
        val localizedText = locale?.let {
            LocalizationModule.getInstance(LocalContext.current)
                .translate(text, it)
        } ?: text
        
        Button(
            onClick = onClick ?: { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = localizedText)
        }
    }
    
    @Composable
    fun toggle(
        label: String,
        checked: Boolean = false,
        locale: String? = null,
        onCheckedChange: ((Boolean) -> Unit)? = null
    ) {
        // Apply locale for label
        val localizedLabel = locale?.let {
            LocalizationModule.getInstance(LocalContext.current)
                .translate(label, it)
        } ?: label
        var checkedState by remember { mutableStateOf(checked) }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = localizedLabel, modifier = Modifier.weight(1f))
            Switch(
                checked = checkedState,
                onCheckedChange = {
                    checkedState = it
                    onCheckedChange?.invoke(it)
                }
            )
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun dropdown(
        label: String,
        options: List<String>,
        selected: String? = null,
        locale: String? = null,
        onSelectionChange: ((String) -> Unit)? = null
    ) {
        var expanded by remember { mutableStateOf(false) }
        var selectedOption by remember { mutableStateOf(selected ?: options.firstOrNull() ?: "") }
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
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
    
    @Composable
    fun slider(
        label: String,
        value: Float = 0.5f,
        range: ClosedFloatingPointRange<Float> = 0f..1f,
        locale: String? = null,
        onValueChange: ((Float) -> Unit)? = null
    ) {
        var sliderValue by remember { mutableStateOf(value) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = "$label: ${"%.2f".format(sliderValue)}")
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    onValueChange?.invoke(it)
                },
                valueRange = range,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    
    @Composable
    fun stepper(
        label: String,
        value: Int = 0,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        locale: String? = null,
        onValueChange: ((Int) -> Unit)? = null
    ) {
        var stepperValue by remember { mutableStateOf(value) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    if (stepperValue > min) {
                        stepperValue--
                        onValueChange?.invoke(stepperValue)
                    }
                }
            ) {
                Text("-")
            }
            Text(text = stepperValue.toString(), modifier = Modifier.padding(horizontal = 16.dp))
            IconButton(
                onClick = {
                    if (stepperValue < max) {
                        stepperValue++
                        onValueChange?.invoke(stepperValue)
                    }
                }
            ) {
                Text("+")
            }
        }
    }
    
    @Composable
    fun radioGroup(
        label: String,
        options: List<String>,
        selected: String? = null,
        locale: String? = null,
        onSelectionChange: ((String) -> Unit)? = null
    ) {
        var selectedOption by remember { mutableStateOf(selected ?: options.firstOrNull() ?: "") }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (option == selectedOption),
                            onClick = {
                                selectedOption = option
                                onSelectionChange?.invoke(option)
                            },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (option == selectedOption),
                        onClick = null
                    )
                    Text(
                        text = option,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
    
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
        var selectedChips by remember { mutableStateOf(selected) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                setOf(chip)
                            }
                            onSelectionChange?.invoke(selectedChips)
                        },
                        label = { Text(chip) }
                    )
                }
            }
        }
    }
    
    @Composable
    fun list(
        items: List<Any>,
        locale: String? = null,
        itemContent: @Composable (Any) -> Unit
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items.size) { index ->
                itemContent(items[index])
            }
        }
    }
    
    @Composable
    fun card(
        title: String? = null,
        locale: String? = null,
        aiContext: AIContext? = null,
        content: @Composable () -> Unit
    ) {
        // Apply locale and AI context
        val localizedTitle = title?.let { t ->
            locale?.let { l ->
                LocalizationModule.getInstance(LocalContext.current)
                    .translate(t, l)
            } ?: t
        }
        aiContext?.let { AIContextManager.setContext("card_${title.hashCode()}", it) }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                localizedTitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                content()
            }
        }
    }
    
    @Composable
    fun section(
        title: String,
        locale: String? = null,
        aiContext: AIContext? = null,
        content: @Composable () -> Unit
    ) {
        // Apply locale and AI context
        val localizedTitle = locale?.let {
            LocalizationModule.getInstance(LocalContext.current)
                .translate(title, it)
        } ?: title
        aiContext?.let { AIContextManager.setContext("section_${title.hashCode()}", it) }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = localizedTitle,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
    
    @Composable
    fun spacer(height: Int = 16) {
        Spacer(modifier = Modifier.height(height.dp))
    }
    
    @Composable
    fun row(
        locale: String? = null,
        aiContext: AIContext? = null,
        content: @Composable RowScope.() -> Unit
    ) {
        // Apply AI context for row container
        aiContext?.let { AIContextManager.setContext("row_${System.currentTimeMillis()}", it) }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Pass locale context to children via composition local if needed
            // Locale context passed to children via LocalizationModule
            content()
        }
    }
    
    @Composable
    fun column(
        locale: String? = null,
        aiContext: AIContext? = null,
        content: @Composable ColumnScope.() -> Unit
    ) {
        // Apply AI context for column container
        aiContext?.let { AIContextManager.setContext("column_${System.currentTimeMillis()}", it) }
        
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pass locale context to children via composition local if needed
            // Locale context passed to children via LocalizationModule
            content()
        }
    }
}

// Using system-wide LocalizationModule from com.augmentalis.localizationmanager
