/**
 * VoiceScreenDSL.kt - DSL for building voice-enabled screens
 * 
 * Simplified API for creating voice-controlled UI
 * Author: VOS4 Development Team
 * Created: 2025-08-30
 */

package com.augmentalis.voiceui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * DSL for building voice-enabled screens
 */
@Composable
fun VoiceScreenDSL(
    screenId: String = "screen",
    content: @Composable VoiceScreenScope.() -> Unit
) {
    val scope = remember { VoiceScreenScope(screenId) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        scope.content()
    }
}

/**
 * Scope for VoiceScreen DSL
 */
@Stable
class VoiceScreenScope(private val screenId: String) {
    
    @Composable
    fun text(content: String, modifier: Modifier = Modifier) {
        Text(
            text = content,
            modifier = modifier.padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
    
    @Composable
    fun input(
        label: String,
        modifier: Modifier = Modifier,
        onValueChange: (String) -> Unit = {}
    ) {
        var value by remember { mutableStateOf("") }
        VoiceUITextField(
            label = label,
            value = value,
            onValueChange = { newValue ->
                value = newValue
                onValueChange(newValue)
            },
            modifier = modifier.padding(vertical = 4.dp)
        )
    }
    
    @Composable
    fun password(
        label: String = "Password",
        modifier: Modifier = Modifier,
        onValueChange: (String) -> Unit = {}
    ) {
        var value by remember { mutableStateOf("") }
        VoiceUITextField(
            label = label,
            value = value,
            onValueChange = { newValue ->
                value = newValue
                onValueChange(newValue)
            },
            isPassword = true,
            modifier = modifier.padding(vertical = 4.dp)
        )
    }
    
    @Composable
    fun button(
        text: String,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {}
    ) {
        VoiceUIButton(
            text = text,
            onClick = onClick,
            modifier = modifier.padding(vertical = 4.dp)
        )
    }
    
    @Composable
    fun toggle(
        label: String,
        checked: Boolean = false,
        modifier: Modifier = Modifier,
        onCheckedChange: (Boolean) -> Unit = {}
    ) {
        var isChecked by remember { mutableStateOf(checked) }
        
        Row(
            modifier = modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label)
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isChecked,
                onCheckedChange = { newValue ->
                    isChecked = newValue
                    onCheckedChange(newValue)
                }
            )
        }
    }
    
    @Composable
    fun dropdown(
        label: String,
        options: List<String>,
        modifier: Modifier = Modifier,
        onSelectionChange: (String) -> Unit = {}
    ) {
        var expanded by remember { mutableStateOf(false) }
        var selectedOption by remember { mutableStateOf(options.firstOrNull() ?: "") }
        
        Box(modifier = modifier.padding(vertical = 4.dp)) {
            OutlinedButton(
                onClick = { expanded = true }
            ) {
                Text("$label: $selectedOption")
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            selectedOption = option
                            onSelectionChange(option)
                            expanded = false
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
        modifier: Modifier = Modifier,
        onValueChange: (Float) -> Unit = {}
    ) {
        var sliderValue by remember { mutableStateOf(value) }
        
        Column(modifier = modifier.padding(vertical = 4.dp)) {
            Text("$label: ${String.format("%.1f", sliderValue)}")
            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    sliderValue = newValue
                    onValueChange(newValue)
                },
                valueRange = range
            )
        }
    }
}