package com.augmentalis.ava.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.ava.core.data.prefs.DeveloperPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Developer Settings screen.
 *
 * Manages developer-specific settings like flash mode, verbose logging, performance metrics.
 *
 * @param developerPreferences Preferences manager for developer settings
 */
@HiltViewModel
class DeveloperSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val developerPreferences = DeveloperPreferences(context)

    /**
     * Flash mode enabled state (REQ-007).
     * When true, StatusIndicator pulses during active NLU/LLM processing.
     */
    val isFlashModeEnabled: StateFlow<Boolean> = developerPreferences.isFlashModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Verbose logging enabled state (future feature).
     */
    val isVerboseLoggingEnabled: StateFlow<Boolean> = developerPreferences.isVerboseLoggingEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Performance metrics display enabled state (future feature).
     */
    val isShowPerformanceMetrics: StateFlow<Boolean> = developerPreferences.isShowPerformanceMetrics
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Toggle flash mode on/off (REQ-007).
     */
    fun setFlashModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            developerPreferences.setFlashModeEnabled(enabled)
        }
    }

    /**
     * Toggle verbose logging on/off (future feature).
     */
    fun setVerboseLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            developerPreferences.setVerboseLoggingEnabled(enabled)
        }
    }

    /**
     * Toggle performance metrics display on/off (future feature).
     */
    fun setShowPerformanceMetrics(enabled: Boolean) {
        viewModelScope.launch {
            developerPreferences.setShowPerformanceMetrics(enabled)
        }
    }

    /**
     * Reset all developer settings to defaults.
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            developerPreferences.clearAll()
        }
    }
}

/**
 * Developer Settings screen (REQ-007).
 *
 * Provides toggles for developer/QA testing features:
 * - Flash Mode: Real-time visual feedback during NLU/LLM processing
 * - Verbose Logging: Detailed debug logs (future)
 * - Performance Metrics: FPS, memory, CPU overlay (future)
 *
 * @param onNavigateBack Callback when user taps back button
 * @param viewModel ViewModel for managing settings state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeveloperSettingsViewModel = hiltViewModel()
) {
    // Collect state from ViewModel
    val isFlashModeEnabled by viewModel.isFlashModeEnabled.collectAsState()
    val isVerboseLoggingEnabled by viewModel.isVerboseLoggingEnabled.collectAsState()
    val isShowPerformanceMetrics by viewModel.isShowPerformanceMetrics.collectAsState()

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Section: Visual Feedback
            Text(
                text = "Visual Feedback",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Flash Mode Toggle (REQ-007)
            SettingItem(
                title = "Flash Mode",
                description = "Show real-time pulsing indicators during NLU/LLM processing. Useful for developers and QA to see which system is actively working.",
                checked = isFlashModeEnabled,
                onCheckedChange = { viewModel.setFlashModeEnabled(it) }
            )

            // Section: Logging (Future)
            Text(
                text = "Logging",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            SettingItem(
                title = "Verbose Logging",
                description = "Enable detailed debug logging for all modules. Helps diagnose issues during testing. (Coming Soon)",
                checked = isVerboseLoggingEnabled,
                onCheckedChange = { viewModel.setVerboseLoggingEnabled(it) },
                enabled = false // Disabled until implemented
            )

            // Section: Performance (Future)
            Text(
                text = "Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            SettingItem(
                title = "Show Performance Metrics",
                description = "Display real-time FPS, memory, CPU, and inference time overlay. (Coming Soon)",
                checked = isShowPerformanceMetrics,
                onCheckedChange = { viewModel.setShowPerformanceMetrics(it) },
                enabled = false // Disabled until implemented
            )

            // Warning text
            Text(
                text = "⚠️ Developer settings are intended for testing and debugging. Some features may impact performance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

/**
 * Individual setting item with toggle switch.
 *
 * @param title Setting name
 * @param description Detailed explanation of what the setting does
 * @param checked Current state of the toggle
 * @param onCheckedChange Callback when toggle is changed
 * @param enabled Whether the setting can be toggled
 */
@Composable
private fun SettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
