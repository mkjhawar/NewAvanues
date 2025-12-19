package com.augmentalis.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.augmentalis.chat.tts.TTSSettings
import com.augmentalis.chat.tts.VoiceInfo
import com.augmentalis.chat.tts.VoiceQuality

/**
 * TTS control button for message bubbles.
 *
 * Displays a speaker icon button that allows users to speak individual messages.
 * Shows different states: idle, speaking, error.
 *
 * @param isSpeaking True if this message is currently being spoken
 * @param enabled True if TTS is available and ready
 * @param onSpeak Callback when user taps to speak this message
 * @param onStop Callback when user taps to stop speaking
 * @param modifier Optional modifier
 */
@Composable
fun TTSButton(
    isSpeaking: Boolean,
    enabled: Boolean = true,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = if (isSpeaking) onStop else onSpeak,
        enabled = enabled,
        modifier = modifier.semantics {
            contentDescription = if (isSpeaking) {
                "Stop speaking this message"
            } else {
                "Speak this message"
            }
        }
    ) {
        Icon(
            imageVector = if (isSpeaking) {
                Icons.Filled.VolumeOff
            } else {
                Icons.Filled.VolumeUp
            },
            contentDescription = null,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                isSpeaking -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * TTS settings panel for chat screen.
 *
 * Displays TTS controls in an expandable panel:
 * - Enable/disable TTS
 * - Auto-speak toggle
 * - Voice selection
 * - Speech rate slider
 * - Pitch slider
 *
 * @param settings Current TTS settings
 * @param availableVoices List of available voices
 * @param isSpeaking True if currently speaking
 * @param onToggleEnabled Callback to toggle TTS enabled
 * @param onToggleAutoSpeak Callback to toggle auto-speak
 * @param onVoiceSelected Callback when voice is selected
 * @param onSpeechRateChanged Callback when speech rate changes
 * @param onPitchChanged Callback when pitch changes
 * @param onTestSpeak Callback to test current settings
 * @param onStopSpeaking Callback to stop current speech
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TTSSettingsPanel(
    settings: TTSSettings,
    availableVoices: List<VoiceInfo>,
    isSpeaking: Boolean,
    onToggleEnabled: () -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onVoiceSelected: (String?) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onTestSpeak: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with expand/collapse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Text-to-Speech",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Enable/Disable switch
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = { onToggleEnabled() },
                        modifier = Modifier.semantics {
                            contentDescription = if (settings.enabled) {
                                "TTS enabled. Tap to disable."
                            } else {
                                "TTS disabled. Tap to enable."
                            }
                        }
                    )

                    // Expand/collapse button
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) {
                                Icons.Filled.ExpandLess
                            } else {
                                Icons.Filled.ExpandMore
                            },
                            contentDescription = if (expanded) "Collapse settings" else "Expand settings"
                        )
                    }
                }
            }

            // Expanded settings
            AnimatedVisibility(visible = expanded && settings.enabled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Auto-speak toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-speak responses",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = settings.autoSpeak,
                            onCheckedChange = { onToggleAutoSpeak() },
                            modifier = Modifier.semantics {
                                contentDescription = if (settings.autoSpeak) {
                                    "Auto-speak enabled. AVA will speak all responses automatically."
                                } else {
                                    "Auto-speak disabled. Tap speaker button to hear responses."
                                }
                            }
                        )
                    }

                    Divider()

                    // Voice selection
                    VoiceSelector(
                        voices = availableVoices,
                        selectedVoiceId = settings.selectedVoice,
                        onVoiceSelected = onVoiceSelected
                    )

                    Divider()

                    // Speech rate slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Speech Rate",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${(settings.speechRate * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.speechRate,
                            onValueChange = onSpeechRateChanged,
                            valueRange = 0.5f..2.0f,
                            steps = 14, // 0.1 increments
                            modifier = Modifier.semantics {
                                contentDescription = "Speech rate: ${(settings.speechRate * 100).toInt()} percent"
                            }
                        )
                    }

                    // Pitch slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pitch",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${(settings.pitch * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.pitch,
                            onValueChange = onPitchChanged,
                            valueRange = 0.5f..2.0f,
                            steps = 14, // 0.1 increments
                            modifier = Modifier.semantics {
                                contentDescription = "Pitch: ${(settings.pitch * 100).toInt()} percent"
                            }
                        )
                    }

                    Divider()

                    // Test button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTestSpeak,
                            enabled = !isSpeaking,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Voice")
                        }

                        if (isSpeaking) {
                            FilledTonalButton(
                                onClick = onStopSpeaking,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Voice selector dropdown.
 *
 * @param voices Available voices
 * @param selectedVoiceId Currently selected voice ID
 * @param onVoiceSelected Callback when voice is selected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceSelector(
    voices: List<VoiceInfo>,
    selectedVoiceId: String?,
    onVoiceSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Voice",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = voices.find { it.id == selectedVoiceId }?.name ?: "System Default",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // System default option
                DropdownMenuItem(
                    text = { Text("System Default") },
                    onClick = {
                        onVoiceSelected(null)
                        expanded = false
                    },
                    leadingIcon = {
                        if (selectedVoiceId == null) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    }
                )

                // Available voices
                voices.forEach { voice ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(voice.name)
                                Text(
                                    text = voice.locale,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onVoiceSelected(voice.id)
                            expanded = false
                        },
                        leadingIcon = {
                            if (voice.id == selectedVoiceId) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        trailingIcon = {
                            if (voice.requiresNetwork) {
                                Icon(
                                    imageVector = Icons.Filled.Cloud,
                                    contentDescription = "Requires network",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

// ==================== Previews ====================

@Preview(showBackground = true)
@Composable
private fun TTSButtonPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Idle state
            TTSButton(
                isSpeaking = false,
                enabled = true,
                onSpeak = {},
                onStop = {}
            )

            // Speaking state
            TTSButton(
                isSpeaking = true,
                enabled = true,
                onSpeak = {},
                onStop = {}
            )

            // Disabled state
            TTSButton(
                isSpeaking = false,
                enabled = false,
                onSpeak = {},
                onStop = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TTSSettingsPanelPreview() {
    MaterialTheme {
        val sampleVoices = listOf(
            VoiceInfo(
                id = "en-us-x-tpd-network",
                name = "US English (Female)",
                locale = "English (United States)",
                quality = VoiceQuality.HIGH,
                requiresNetwork = false
            ),
            VoiceInfo(
                id = "en-us-x-tpf-network",
                name = "US English (Male)",
                locale = "English (United States)",
                quality = VoiceQuality.HIGH,
                requiresNetwork = true
            )
        )

        TTSSettingsPanel(
            settings = TTSSettings(
                enabled = true,
                autoSpeak = false,
                speechRate = 1.0f,
                pitch = 1.0f
            ),
            availableVoices = sampleVoices,
            isSpeaking = false,
            onToggleEnabled = {},
            onToggleAutoSpeak = {},
            onVoiceSelected = {},
            onSpeechRateChanged = {},
            onPitchChanged = {},
            onTestSpeak = {},
            onStopSpeaking = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TTSSettingsPanelDarkPreview() {
    MaterialTheme {
        val sampleVoices = listOf(
            VoiceInfo(
                id = "en-us-x-tpd-network",
                name = "US English (Female)",
                locale = "English (United States)",
                quality = VoiceQuality.HIGH,
                requiresNetwork = false
            )
        )

        TTSSettingsPanel(
            settings = TTSSettings(
                enabled = true,
                autoSpeak = true,
                speechRate = 1.2f,
                pitch = 0.9f,
                selectedVoice = "en-us-x-tpd-network"
            ),
            availableVoices = sampleVoices,
            isSpeaking = true,
            onToggleEnabled = {},
            onToggleAutoSpeak = {},
            onVoiceSelected = {},
            onSpeechRateChanged = {},
            onPitchChanged = {},
            onTestSpeak = {},
            onStopSpeaking = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
