/**
 * DeveloperSettingsScreen.kt - Tunable developer settings
 *
 * Accessible via 4-tap on "Avanues" title in HubDashboardScreen.
 * Provides sliders, switches, and dropdowns to adjust all developer-tunable
 * parameters that are normally hardcoded in ServiceConfiguration.
 *
 * Sections:
 * 1. Voice Timings (STT timeout, end-of-speech delay, partial interval, confidence)
 * 2. Feature Flags (debug, verbose, overlay, scanner verbosity, auto-start, synonyms)
 * 3. Engine Selection (STT engine, voice language)
 * 4. Timing / Debounce (content change, scroll event, screen change delay)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.developer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.augmentalis.avanueui.components.settings.SettingsDropdownRow
import com.augmentalis.avanueui.components.settings.SettingsGroupCard
import com.augmentalis.avanueui.components.settings.SettingsSectionHeader
import com.augmentalis.avanueui.components.settings.SettingsSliderRow
import com.augmentalis.avanueui.components.settings.SettingsSwitchRow
import com.augmentalis.avanueui.theme.AvanueTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeveloperSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset to defaults",
                            tint = AvanueTheme.colors.warning
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AvanueTheme.colors.background,
                    titleContentColor = AvanueTheme.colors.textPrimary,
                    navigationIconContentColor = AvanueTheme.colors.textPrimary
                )
            )
        },
        containerColor = AvanueTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Section 1: Voice Timings ──
            SettingsSectionHeader(title = "VOICE TIMINGS")
            SettingsGroupCard {
                SettingsSliderRow(
                    title = "STT Timeout",
                    subtitle = "Max time waiting for speech input",
                    icon = Icons.Default.Timer,
                    value = settings.sttTimeoutMs.toFloat(),
                    valueRange = 3000f..30000f,
                    steps = 26,
                    valueLabel = "${settings.sttTimeoutMs / 1000}s",
                    onValueChange = { viewModel.updateSttTimeout(it.toLong()) }
                )

                SettingsSliderRow(
                    title = "End of Speech Delay",
                    subtitle = "Silence before finalizing recognition",
                    icon = Icons.Default.Mic,
                    value = settings.endOfSpeechDelayMs.toFloat(),
                    valueRange = 500f..5000f,
                    steps = 17,
                    valueLabel = "${settings.endOfSpeechDelayMs}ms",
                    onValueChange = { viewModel.updateEndOfSpeechDelay(it.toLong()) }
                )

                SettingsSliderRow(
                    title = "Partial Result Interval",
                    subtitle = "Update frequency for partial recognition",
                    icon = Icons.Default.Speed,
                    value = settings.partialResultIntervalMs.toFloat(),
                    valueRange = 100f..1000f,
                    steps = 8,
                    valueLabel = "${settings.partialResultIntervalMs}ms",
                    onValueChange = { viewModel.updatePartialResultInterval(it.toLong()) }
                )

                SettingsSliderRow(
                    title = "Confidence Threshold",
                    subtitle = "Minimum confidence to accept a command",
                    icon = Icons.Default.Speed,
                    value = settings.confidenceThreshold,
                    valueRange = 0.1f..1.0f,
                    steps = 8,
                    valueLabel = "%.1f".format(settings.confidenceThreshold),
                    onValueChange = { viewModel.updateConfidenceThreshold(it) }
                )
            }

            // ── Section 2: Feature Flags ──
            SettingsSectionHeader(title = "FEATURE FLAGS")
            SettingsGroupCard {
                SettingsSwitchRow(
                    title = "Debug Mode",
                    subtitle = "Enable debug logging and diagnostics",
                    icon = Icons.Default.BugReport,
                    checked = settings.debugMode,
                    onCheckedChange = { viewModel.updateDebugMode(it) }
                )

                SettingsSwitchRow(
                    title = "Verbose Logging",
                    subtitle = "Extra-detailed log output",
                    icon = Icons.Default.BugReport,
                    checked = settings.verboseLogging,
                    onCheckedChange = { viewModel.updateVerboseLogging(it) }
                )

                SettingsSwitchRow(
                    title = "Debug Overlay",
                    subtitle = "Show element bounds on screen",
                    icon = Icons.Default.Visibility,
                    checked = settings.debugOverlay,
                    onCheckedChange = { viewModel.updateDebugOverlay(it) }
                )

                SettingsSliderRow(
                    title = "Scanner Verbosity",
                    subtitle = "Accessibility scanner detail level",
                    icon = Icons.Default.Settings,
                    value = settings.scannerVerbosity.toFloat(),
                    valueRange = 0f..3f,
                    steps = 2,
                    valueLabel = "${settings.scannerVerbosity}",
                    onValueChange = { viewModel.updateScannerVerbosity(it.toInt()) }
                )

                SettingsSwitchRow(
                    title = "Auto-Start Listening",
                    subtitle = "Begin voice recognition on service start",
                    icon = Icons.Default.Mic,
                    checked = settings.autoStartListening,
                    onCheckedChange = { viewModel.updateAutoStartListening(it) }
                )

                SettingsSwitchRow(
                    title = "Synonyms Enabled",
                    subtitle = "Allow alias phrases for commands",
                    icon = Icons.Default.Language,
                    checked = settings.synonymsEnabled,
                    onCheckedChange = { viewModel.updateSynonymsEnabled(it) }
                )
            }

            // ── Section 3: Engine Selection ──
            SettingsSectionHeader(title = "ENGINE SELECTION")
            SettingsGroupCard {
                SettingsDropdownRow(
                    title = "STT Engine",
                    subtitle = "Speech-to-text recognition engine",
                    icon = Icons.Default.Mic,
                    selected = settings.sttEngine,
                    options = listOf("VIVOKA", "ANDROID", "WHISPER"),
                    optionLabel = { engine ->
                        when (engine) {
                            "VIVOKA" -> "Vivoka (Offline)"
                            "ANDROID" -> "Android (Google)"
                            "WHISPER" -> "Whisper (Local AI)"
                            else -> engine
                        }
                    },
                    onSelected = { viewModel.updateSttEngine(it) }
                )

                SettingsDropdownRow(
                    title = "Voice Language",
                    subtitle = "Recognition language/locale",
                    icon = Icons.Default.Language,
                    selected = settings.voiceLanguage,
                    options = listOf("en-US", "en-GB", "es-ES", "fr-FR", "de-DE", "ja-JP", "zh-CN"),
                    optionLabel = { lang ->
                        when (lang) {
                            "en-US" -> "English (US)"
                            "en-GB" -> "English (UK)"
                            "es-ES" -> "Spanish"
                            "fr-FR" -> "French"
                            "de-DE" -> "German"
                            "ja-JP" -> "Japanese"
                            "zh-CN" -> "Chinese (Simplified)"
                            else -> lang
                        }
                    },
                    onSelected = { viewModel.updateVoiceLanguage(it) }
                )
            }

            // ── Section 4: Timing / Debounce ──
            SettingsSectionHeader(title = "TIMING / DEBOUNCE")
            SettingsGroupCard {
                SettingsSliderRow(
                    title = "Content Change Debounce",
                    subtitle = "Delay after content change before rescan",
                    icon = Icons.Default.Timer,
                    value = settings.contentChangeDebounceMs.toFloat(),
                    valueRange = 100f..1000f,
                    steps = 8,
                    valueLabel = "${settings.contentChangeDebounceMs}ms",
                    onValueChange = { viewModel.updateContentChangeDebounce(it.toLong()) }
                )

                SettingsSliderRow(
                    title = "Scroll Event Debounce",
                    subtitle = "Delay between scroll event processing",
                    icon = Icons.Default.Timer,
                    value = settings.scrollEventDebounceMs.toFloat(),
                    valueRange = 50f..500f,
                    steps = 8,
                    valueLabel = "${settings.scrollEventDebounceMs}ms",
                    onValueChange = { viewModel.updateScrollEventDebounce(it.toLong()) }
                )

                SettingsSliderRow(
                    title = "Screen Change Delay",
                    subtitle = "Wait time after screen transition",
                    icon = Icons.Default.Timer,
                    value = settings.screenChangeDelayMs.toFloat(),
                    valueRange = 50f..1000f,
                    steps = 18,
                    valueLabel = "${settings.screenChangeDelayMs}ms",
                    onValueChange = { viewModel.updateScreenChangeDelay(it.toLong()) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
