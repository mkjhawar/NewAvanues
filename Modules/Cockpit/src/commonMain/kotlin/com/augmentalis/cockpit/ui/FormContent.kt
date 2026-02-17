package com.augmentalis.cockpit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.components.AvanueButton
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.FrameContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Dynamic form field definition for JSON persistence.
 */
@Serializable
data class FormField(
    val type: String = "checkbox",
    val label: String = "",
    val value: String = "",
    val options: List<String> = emptyList()
)

private val formJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

/**
 * Cross-platform dynamic form composable.
 *
 * Renders form fields parsed from [FrameContent.Form.fieldsJson] as a scrollable list
 * of typed input controls. Supports checkbox, text, and number field types.
 *
 * Features:
 * - Dynamic field list from JSON schema
 * - Checkbox toggles for task/checklist items
 * - Text input fields with AvanueTheme styling
 * - Progress bar showing completion ratio
 * - "Add Item" button to append new checklist items
 * - Auto-persists via [onContentStateChanged] callback
 *
 * @param content The Form content model
 * @param onContentStateChanged Callback with updated JSON when fields change
 * @param modifier Compose modifier
 */
@Composable
fun FormContent(
    content: FrameContent.Form,
    onContentStateChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    val fields = remember(content.fieldsJson) {
        mutableStateListOf<FormField>().apply {
            try {
                addAll(formJson.decodeFromString<List<FormField>>(content.fieldsJson))
            } catch (_: Exception) {
                // Empty or invalid JSON — start with empty list
            }
        }
    }

    val completedCount = fields.count { it.type == "checkbox" && it.value == "true" }
    val totalCheckboxes = fields.count { it.type == "checkbox" }
    val progress = if (totalCheckboxes > 0) completedCount.toFloat() / totalCheckboxes else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = content.title,
            color = colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        // Progress bar
        if (totalCheckboxes > 0) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = colors.primary,
                    trackColor = colors.surface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$completedCount/$totalCheckboxes",
                    color = colors.textPrimary.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Fields list
        if (fields.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No items yet",
                    color = colors.textPrimary.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap \"Add Item\" to create your first checklist item",
                    color = colors.textPrimary.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(fields) { index, field ->
                    when (field.type) {
                        "checkbox" -> CheckboxField(
                            field = field,
                            onToggle = { checked ->
                                fields[index] = field.copy(value = checked.toString())
                                emitFieldsUpdate(fields, onContentStateChanged)
                            }
                        )
                        "text" -> TextInputField(
                            field = field,
                            onValueChange = { newValue ->
                                fields[index] = field.copy(value = newValue)
                                emitFieldsUpdate(fields, onContentStateChanged)
                            }
                        )
                        "number" -> TextInputField(
                            field = field,
                            onValueChange = { newValue ->
                                fields[index] = field.copy(value = newValue)
                                emitFieldsUpdate(fields, onContentStateChanged)
                            }
                        )
                    }
                }
            }
        }

        // Add item button
        Spacer(Modifier.height(8.dp))
        AvanueButton(
            onClick = {
                fields.add(FormField(type = "checkbox", label = "New item", value = "false"))
                emitFieldsUpdate(fields, onContentStateChanged)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Item")
        }
    }
}

@Composable
private fun CheckboxField(
    field: FormField,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val isChecked = field.value == "true"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = colors.primary,
                uncheckedColor = colors.textPrimary.copy(alpha = 0.4f),
                checkmarkColor = colors.onPrimary
            )
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = field.label,
            color = if (isChecked) colors.textPrimary.copy(alpha = 0.5f) else colors.textPrimary,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun TextInputField(
    field: FormField,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    var textValue by remember(field) { mutableStateOf(field.value) }

    OutlinedTextField(
        value = textValue,
        onValueChange = {
            textValue = it
            onValueChange(it)
        },
        label = { Text(field.label) },
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.2f),
            focusedLabelColor = colors.primary,
            cursorColor = colors.primary
        ),
        singleLine = true
    )
}

private fun emitFieldsUpdate(
    fields: List<FormField>,
    onContentStateChanged: (String) -> Unit
) {
    val listSerializer = kotlinx.serialization.builtins.ListSerializer(FormField.serializer())
    val fieldsJson = formJson.encodeToString(listSerializer, fields)
    val completed = fields.count { it.type == "checkbox" && it.value == "true" }
    val total = fields.count { it.type == "checkbox" }
    // Escape the JSON string for embedding as a nested JSON value
    val escaped = fieldsJson.replace("\\", "\\\\").replace("\"", "\\\"")
    onContentStateChanged("""{"fieldsJson":"$escaped","completedCount":$completed,"totalCount":$total}""")
}
