package com.augmentalis.avanueui.renderer.android.mappers.input

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.augmentalis.avanueui.ui.core.form.DatePickerComponent
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.ModifierConverter
import java.text.SimpleDateFormat
import java.util.*

/**
 * DatePickerMapper - Maps DatePickerComponent to Material3 DatePicker
 */
class DatePickerMapper : ComponentMapper<DatePickerComponent> {
    private val modifierConverter = ModifierConverter()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun map(component: DatePickerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var showDialog by remember { mutableStateOf(false) }
            val dateFormatter = remember { SimpleDateFormat(component.dateFormat, Locale.getDefault()) }

            val displayText = component.selectedDate?.let {
                dateFormatter.format(Date(it))
            } ?: "Select date"

            OutlinedTextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                enabled = component.enabled,
                label = component.label?.let { { Text(it) } },
                trailingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = "Select date")
                },
                modifier = modifierConverter.convert(component.modifiers)
                    .clickable(enabled = component.enabled) { showDialog = true }
            )

            if (showDialog) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = component.selectedDate
                )

                DatePickerDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    component.onDateChange?.invoke(it)
                                }
                                showDialog = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}
