package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.flutter.material.input.*
import com.augmentalis.avaelements.renderer.android.IconFromString

/**
 * Android Compose mappers for input components
 *
 * This file contains renderer functions that map cross-platform input component models
 * to Material3 Compose implementations on Android.
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Render PhoneInput component using Material3
 */
@Composable
fun PhoneInputMapper(component: PhoneInput) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = component.getAccessibilityDescription() },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.width(100.dp)
        ) {
            OutlinedTextField(
                value = component.COUNTRY_CODES[component.countryCode] ?: "+1",
                onValueChange = {},
                readOnly = true,
                enabled = component.enabled,
                label = { Text("Code") },
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select country code"
                    )
                },
                modifier = Modifier.menuAnchor(),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                component.COUNTRY_CODES.forEach { (code, dialCode) ->
                    DropdownMenuItem(
                        text = { Text("$code $dialCode") },
                        onClick = {
                            component.onCountryCodeChange?.invoke(code)
                            expanded = false
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = component.value,
            onValueChange = { component.onValueChange?.invoke(it) },
            label = component.label?.let { { Text(it) } },
            placeholder = component.placeholder?.let { { Text(it) } },
            enabled = component.enabled,
            isError = component.errorText != null,
            supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Render UrlInput component using Material3
 */
@Composable
fun UrlInputMapper(component: UrlInput) {
    OutlinedTextField(
        value = component.value,
        onValueChange = { value ->
            val finalValue = if (component.autoAddProtocol && value.isNotBlank() && !value.startsWith("http")) {
                "https://$value"
            } else {
                value
            }
            component.onValueChange?.invoke(finalValue)
        },
        label = component.label?.let { { Text(it) } },
        placeholder = component.placeholder?.let { { Text(it) } },
        enabled = component.enabled,
        isError = component.errorText != null || !component.isValid(),
        supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        leadingIcon = { Icon(imageVector = Icons.Default.Link, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }
    )
}

/**
 * Render ComboBox component using Material3
 */
@Composable
fun ComboBoxMapper(component: ComboBox) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(component.value) }
    val filteredOptions = remember(searchQuery) { component.getFilteredOptions(searchQuery) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                expanded = true
                if (component.allowCustomValue) component.onValueChange?.invoke(it)
            },
            label = component.label?.let { { Text(it) } },
            placeholder = component.placeholder?.let { { Text(it) } },
            enabled = component.enabled,
            isError = component.errorText != null,
            supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand options"
                )
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )
        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            searchQuery = option
                            component.onValueChange?.invoke(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Render PinInput component using Material3
 */
@Composable
fun PinInputMapper(component: PinInput) {
    val focusRequesters = remember { List(component.length) { FocusRequester() } }
    Column(modifier = Modifier.semantics { contentDescription = component.getAccessibilityDescription() }) {
        component.label?.let {
            Text(text = it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            repeat(component.length) { index ->
                val digit = component.value.getOrNull(index)?.toString() ?: ""
                OutlinedTextField(
                    value = digit,
                    onValueChange = { newValue ->
                        if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                            val newPin = buildString {
                                component.value.forEachIndexed { i, c ->
                                    if (i == index) append(newValue.firstOrNull() ?: "") else append(c)
                                }
                                if (index >= component.value.length && newValue.isNotEmpty()) append(newValue)
                            }.take(component.length)
                            component.onValueChange?.invoke(newPin)
                            if (newValue.isNotEmpty() && index < component.length - 1) {
                                focusRequesters[index + 1].requestFocus()
                            }
                            if (newPin.length == component.length) component.onComplete?.invoke(newPin)
                        } else if (newValue.isEmpty() && index > 0) {
                            focusRequesters[index - 1].requestFocus()
                        }
                    },
                    enabled = component.enabled,
                    isError = component.errorText != null,
                    visualTransformation = if (component.masked) PasswordVisualTransformation('\u2022') else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = if (index == component.length - 1) ImeAction.Done else ImeAction.Next
                    ),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.width(48.dp).focusRequester(focusRequesters[index])
                )
            }
        }
        component.errorText?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
}

/**
 * Render OTPInput component using Material3
 */
@Composable
fun OTPInputMapper(component: OTPInput) {
    val focusRequesters = remember { List(component.length) { FocusRequester() } }
    Column(modifier = Modifier.semantics { contentDescription = component.getAccessibilityDescription() }) {
        component.label?.let {
            Text(text = it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            repeat(component.length) { index ->
                val char = component.value.getOrNull(index)?.toString() ?: ""
                OutlinedTextField(
                    value = char,
                    onValueChange = { newValue ->
                        if (newValue.length > 1) {
                            val cleanOtp = newValue.filter { it.isLetterOrDigit() }.take(component.length)
                            component.onValueChange?.invoke(cleanOtp)
                            if (cleanOtp.length == component.length && component.autoSubmit) {
                                component.onComplete?.invoke(cleanOtp)
                            }
                        } else if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isLetterOrDigit() })) {
                            val newOtp = buildString {
                                component.value.forEachIndexed { i, c ->
                                    if (i == index) append(newValue.firstOrNull() ?: "") else append(c)
                                }
                                if (index >= component.value.length && newValue.isNotEmpty()) append(newValue)
                            }.take(component.length)
                            component.onValueChange?.invoke(newOtp)
                            if (newValue.isNotEmpty() && index < component.length - 1) {
                                focusRequesters[index + 1].requestFocus()
                            }
                            if (newOtp.length == component.length && component.autoSubmit) {
                                component.onComplete?.invoke(newOtp)
                            }
                        } else if (newValue.isEmpty() && index > 0) {
                            focusRequesters[index - 1].requestFocus()
                        }
                    },
                    enabled = component.enabled,
                    isError = component.errorText != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = if (index == component.length - 1) ImeAction.Done else ImeAction.Next
                    ),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.width(48.dp).focusRequester(focusRequesters[index])
                )
            }
        }
        component.errorText?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
}

/**
 * Render MaskInput component using Material3
 */
@Composable
fun MaskInputMapper(component: MaskInput) {
    OutlinedTextField(
        value = component.value,
        onValueChange = { value ->
            val unmasked = value.filter { it.isLetterOrDigit() }
            var formatted = ""
            var unmaskedIndex = 0
            for (maskChar in component.mask) {
                if (unmaskedIndex >= unmasked.length) break
                when (maskChar) {
                    '#' -> if (unmasked[unmaskedIndex].isDigit()) { formatted += unmasked[unmaskedIndex++] } else break
                    'A' -> if (unmasked[unmaskedIndex].isLetter()) { formatted += unmasked[unmaskedIndex++] } else break
                    'X' -> formatted += unmasked[unmaskedIndex++]
                    else -> formatted += maskChar
                }
            }
            component.onValueChange?.invoke(formatted)
        },
        label = component.label?.let { { Text(it) } },
        placeholder = component.placeholder?.let { { Text(it) } },
        enabled = component.enabled,
        isError = component.errorText != null,
        supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }
    )
}

/**
 * Render RichTextEditor component using Material3
 */
@Composable
fun RichTextEditorMapper(component: RichTextEditor) {
    Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }) {
        component.label?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (component.showToolbar) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {}) { Icon(imageVector = Icons.Default.FormatBold, contentDescription = "Bold") }
                    IconButton(onClick = {}) { Icon(imageVector = Icons.Default.FormatItalic, contentDescription = "Italic") }
                    IconButton(onClick = {}) { Icon(imageVector = Icons.Default.FormatUnderlined, contentDescription = "Underline") }
                }
            }
        }
        OutlinedTextField(
            value = component.value,
            onValueChange = { component.onValueChange?.invoke(it) },
            placeholder = component.placeholder?.let { { Text(it) } },
            enabled = component.enabled,
            isError = component.errorText != null,
            supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            minLines = 6,
            maxLines = 20,
            modifier = Modifier.fillMaxWidth().heightIn(min = component.minHeight.dp)
        )
    }
}

/**
 * Render MarkdownEditor component using Material3
 */
@Composable
fun MarkdownEditorMapper(component: MarkdownEditor) {
    Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }) {
        component.label?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (component.splitView && component.showPreview) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = component.value,
                    onValueChange = { component.onValueChange?.invoke(it) },
                    placeholder = component.placeholder?.let { { Text(it) } },
                    enabled = component.enabled,
                    minLines = 10,
                    modifier = Modifier.weight(1f).heightIn(min = component.minHeight.dp)
                )
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = component.minHeight.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(text = component.value, modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            OutlinedTextField(
                value = component.value,
                onValueChange = { component.onValueChange?.invoke(it) },
                placeholder = component.placeholder?.let { { Text(it) } },
                enabled = component.enabled,
                isError = component.errorText != null,
                supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                minLines = 10,
                modifier = Modifier.fillMaxWidth().heightIn(min = component.minHeight.dp)
            )
        }
    }
}

/**
 * Render CodeEditor component using Material3
 */
@Composable
fun CodeEditorMapper(component: CodeEditor) {
    Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }) {
        component.label?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        Row {
            if (component.showLineNumbers) {
                val lineCount = component.value.count { it == '\n' } + 1
                Surface(modifier = Modifier.width(48.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        repeat(lineCount) { line ->
                            Text(
                                "${line + 1}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = component.value,
                onValueChange = { component.onValueChange?.invoke(it) },
                placeholder = component.placeholder?.let { { Text(it) } },
                enabled = component.enabled,
                isError = component.errorText != null,
                supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                minLines = 15,
                modifier = Modifier.weight(1f).heightIn(min = component.minHeight.dp)
            )
        }
    }
}

/**
 * Render FormSection component using Material3
 */
@Composable
fun FormSectionMapper(component: FormSection) {
    var expanded by remember { mutableStateOf(component.expanded) }
    val isExpanded = if (component.collapsible) expanded else true

    Card(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }) {
        Column {
            if (component.title != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (component.collapsible) Modifier.clickable {
                            expanded = !expanded
                            component.onExpandChange?.invoke(expanded)
                        } else Modifier)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(component.title, style = MaterialTheme.typography.titleMedium)
                        component.description?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    if (component.collapsible) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
                if (component.showDivider) HorizontalDivider()
            }
            androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // TODO: Render children components
                }
            }
        }
    }
}

/**
 * Render MultiSelect component using Material3
 */
@Composable
fun MultiSelectMapper(component: MultiSelect) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }) {
        component.label?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (component.showChips && component.selectedValues.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                component.selectedValues.forEach { value ->
                    InputChip(
                        selected = true,
                        onClick = { component.onSelectionChange?.invoke(component.selectedValues - value) },
                        label = { Text(value) },
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove $value", modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }
        }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = if (component.selectedValues.isEmpty()) component.placeholder ?: "" else "${component.selectedValues.size} selected",
                onValueChange = {},
                readOnly = true,
                enabled = component.enabled,
                isError = component.errorText != null,
                supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand options"
                    )
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (component.searchable) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        singleLine = true
                    )
                }
                val filteredOptions = if (searchQuery.isBlank()) component.options else component.options.filter { it.contains(searchQuery, ignoreCase = true) }
                filteredOptions.forEach { option ->
                    val isSelected = component.isSelected(option)
                    val canSelect = !isSelected || !component.isMaxSelectionsReached()
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            if (canSelect) component.onSelectionChange?.invoke(component.toggleSelection(option))
                        },
                        leadingIcon = { Checkbox(checked = isSelected, onCheckedChange = null, enabled = canSelect) },
                        enabled = canSelect
                    )
                }
            }
        }
    }
}
