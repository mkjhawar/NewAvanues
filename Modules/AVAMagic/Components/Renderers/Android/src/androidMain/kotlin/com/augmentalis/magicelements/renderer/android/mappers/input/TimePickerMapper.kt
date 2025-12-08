package com.augmentalis.avaelements.renderer.android.mappers.input

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.augmentalis.avanues.avamagic.ui.core.form.Time
import com.augmentalis.avanues.avamagic.ui.core.form.TimePickerComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * TimePickerMapper - Maps TimePickerComponent to Material3 TimePicker
 */
class TimePickerMapper : ComponentMapper<TimePickerComponent> {
    private val modifierConverter = ModifierConverter()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun map(component: TimePickerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var showDialog by remember { mutableStateOf(false) }

            val displayText = component.selectedTime?.format(component.is24Hour)
                ?: component.placeholder

            OutlinedTextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                enabled = component.enabled,
                label = component.label?.let { { Text(it) } },
                trailingIcon = {
                    Icon(Icons.Default.Schedule, contentDescription = "Select time")
                },
                modifier = modifierConverter.convert(component.modifiers)
                    .clickable(enabled = component.enabled) { showDialog = true }
            )

            if (showDialog) {
                val timePickerState = rememberTimePickerState(
                    initialHour = component.selectedTime?.hour ?: 0,
                    initialMinute = component.selectedTime?.minute ?: 0,
                    is24Hour = component.is24Hour
                )

                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                component.onTimeSelected?.invoke(
                                    Time(timePickerState.hour, timePickerState.minute)
                                )
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
                    },
                    text = {
                        TimePicker(state = timePickerState)
                    }
                )
            }
        }
    }
}
