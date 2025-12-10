// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/settings/SettingsScreenNew.kt
// created: 2025-11-08
// author: AVA Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.augmentalis.ava.BuildConfig

/**
 * Redesigned Settings Screen with:
 * - Aesthetic card-based layout
 * - Dynamic responsiveness (portrait/landscape)
 * - Adaptive columns based on screen width
 * - Grouped sections with visual hierarchy
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenResponsive(
    viewModel: SettingsViewModel,
    onNavigateToModelDownload: () -> Unit = {},
    onNavigateToTestLauncher: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current

    // Determine layout based on screen width
    val screenWidthDp = configuration.screenWidthDp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Use columns only when in landscape or screen is wide enough
    val useColumns = isLandscape && screenWidthDp > 600

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val maxWidth = maxWidth

            if (useColumns && maxWidth > 600.dp) {
                // Two-column layout for landscape/wide screens
                TwoColumnSettingsLayout(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToModelDownload = onNavigateToModelDownload,
                    onNavigateToTestLauncher = onNavigateToTestLauncher
                )
            } else {
                // Single column layout for portrait/narrow screens
                SingleColumnSettingsLayout(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToModelDownload = onNavigateToModelDownload,
                    onNavigateToTestLauncher = onNavigateToTestLauncher
                )
            }
        }

        // Model info dialog (overlay)
        uiState.modelInfoToShow?.let { modelInfo ->
            if (uiState.showModelInfoDialog) {
                ModelInfoDialog(
                    modelInfo = modelInfo,
                    onDismiss = { viewModel.dismissModelInfoDialog() }
                )
            }
        }
    }
}

/**
 * Single column layout for portrait mode
 */
@Composable
private fun SingleColumnSettingsLayout(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onNavigateToModelDownload: () -> Unit,
    onNavigateToTestLauncher: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // NLU Section
        item {
            SettingsSectionCard(
                title = "Natural Language Understanding",
                icon = Icons.Default.Psychology
            ) {
                SwitchSettingRow(
                    title = "Enable NLU",
                    description = "On-device intent classification",
                    checked = uiState.nluEnabled,
                    onCheckedChange = { viewModel.setNluEnabled(it) },
                    icon = Icons.Default.Lightbulb
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                SliderSettingRow(
                    title = "Confidence Threshold",
                    description = "Minimum confidence: ${(uiState.nluConfidenceThreshold * 100).toInt()}%",
                    value = uiState.nluConfidenceThreshold,
                    onValueChange = { viewModel.setNluConfidenceThreshold(it) },
                    enabled = uiState.nluEnabled,
                    valueRange = 0.5f..0.95f,
                    icon = Icons.Default.TrendingUp
                )
            }
        }

        // LLM Section
        item {
            SettingsSectionCard(
                title = "Language Model",
                icon = Icons.Default.Cloud
            ) {
                DropdownSettingRow(
                    title = "Provider",
                    description = uiState.llmProvider,
                    options = listOf("Local (On-Device)", "Anthropic (Claude)", "OpenRouter"),
                    selectedOption = uiState.llmProvider,
                    onOptionSelected = { viewModel.setLlmProvider(it) },
                    icon = Icons.Default.DeveloperMode
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                SwitchSettingRow(
                    title = "Enable Streaming",
                    description = "Stream responses in real-time",
                    checked = uiState.llmStreamingEnabled,
                    onCheckedChange = { viewModel.setLlmStreamingEnabled(it) },
                    icon = Icons.Default.Stream
                )
            }
        }

        // Privacy Section
        item {
            SettingsSectionCard(
                title = "Privacy & Data",
                icon = Icons.Default.Lock
            ) {
                SwitchSettingRow(
                    title = "Crash Reporting",
                    description = "Help improve AVA",
                    checked = uiState.crashReportingEnabled,
                    onCheckedChange = { viewModel.setCrashReportingEnabled(it) },
                    icon = Icons.Default.BugReport
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                SwitchSettingRow(
                    title = "Analytics",
                    description = "Anonymous usage statistics",
                    checked = uiState.analyticsEnabled,
                    onCheckedChange = { viewModel.setAnalyticsEnabled(it) },
                    icon = Icons.Default.Analytics
                )
            }
        }

        // Appearance Section
        item {
            SettingsSectionCard(
                title = "Appearance",
                icon = Icons.Default.Palette
            ) {
                DropdownSettingRow(
                    title = "Theme",
                    description = uiState.theme,
                    options = listOf("System Default", "Light", "Dark"),
                    selectedOption = uiState.theme,
                    onOptionSelected = { viewModel.setTheme(it) },
                    icon = Icons.Default.DarkMode
                )
            }
        }

        // Storage & Models Section
        item {
            SettingsSectionCard(
                title = "Storage & Models",
                icon = Icons.Default.Storage
            ) {
                ClickableSettingRow(
                    title = "Clear Cache",
                    description = "${uiState.cacheSize} MB used",
                    onClick = { viewModel.clearCache() },
                    icon = Icons.Default.Delete
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ClickableSettingRow(
                    title = "Download Models",
                    description = "Manage AI models",
                    onClick = onNavigateToModelDownload,
                    icon = Icons.Default.Download
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ModelSelectionRow(
                    title = "RAG Embedding Model",
                    selectedModel = uiState.selectedEmbeddingModel,
                    onModelSelected = { viewModel.setEmbeddingModel(it) },
                    onShowModelInfo = { viewModel.showModelInfo(it) },
                    icon = Icons.Default.Memory
                )
            }
        }

        // Developer Section
        item {
            SettingsSectionCard(
                title = "Developer",
                icon = Icons.Default.Code
            ) {
                ClickableSettingRow(
                    title = "Run Tests",
                    description = "Automated test suite",
                    onClick = onNavigateToTestLauncher,
                    icon = Icons.Default.PlayArrow
                )
            }
        }

        // About Section
        item {
            SettingsSectionCard(
                title = "About",
                icon = Icons.Default.Info
            ) {
                InfoRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow("Build Type", BuildConfig.BUILD_TYPE)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                ClickableSettingRow(
                    title = "Licenses",
                    description = "Open source licenses",
                    onClick = { viewModel.openLicenses() },
                    icon = Icons.Default.Description
                )
            }
        }

        // Language recommendation banner
        if (uiState.deviceLanguage != "en" && !uiState.selectedEmbeddingModel.contains("MULTI")) {
            item {
                LanguageRecommendationBanner(
                    deviceLanguage = uiState.deviceLanguage,
                    recommendedModel = uiState.recommendedModel,
                    onDownloadClick = {
                        viewModel.setEmbeddingModel(uiState.recommendedModel)
                    }
                )
            }
        }
    }
}

/**
 * Two-column layout for landscape/wide screens
 */
@Composable
private fun TwoColumnSettingsLayout(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onNavigateToModelDownload: () -> Unit,
    onNavigateToTestLauncher: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // NLU Section
        item {
            SettingsSectionCard(
                title = "Natural Language Understanding",
                icon = Icons.Default.Psychology
            ) {
                SwitchSettingRow(
                    title = "Enable NLU",
                    description = "On-device intent classification",
                    checked = uiState.nluEnabled,
                    onCheckedChange = { viewModel.setNluEnabled(it) },
                    icon = Icons.Default.Lightbulb
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                SliderSettingRow(
                    title = "Confidence Threshold",
                    description = "Minimum: ${(uiState.nluConfidenceThreshold * 100).toInt()}%",
                    value = uiState.nluConfidenceThreshold,
                    onValueChange = { viewModel.setNluConfidenceThreshold(it) },
                    enabled = uiState.nluEnabled,
                    valueRange = 0.5f..0.95f,
                    icon = Icons.Default.TrendingUp
                )
            }
        }

        // LLM Section
        item {
            SettingsSectionCard(
                title = "Language Model",
                icon = Icons.Default.Cloud
            ) {
                DropdownSettingRow(
                    title = "Provider",
                    description = uiState.llmProvider,
                    options = listOf("Local (On-Device)", "Anthropic (Claude)", "OpenRouter"),
                    selectedOption = uiState.llmProvider,
                    onOptionSelected = { viewModel.setLlmProvider(it) },
                    icon = Icons.Default.DeveloperMode
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                SwitchSettingRow(
                    title = "Enable Streaming",
                    description = "Stream responses",
                    checked = uiState.llmStreamingEnabled,
                    onCheckedChange = { viewModel.setLlmStreamingEnabled(it) },
                    icon = Icons.Default.Stream
                )
            }
        }

        // Privacy Section
        item {
            SettingsSectionCard(
                title = "Privacy & Data",
                icon = Icons.Default.Lock
            ) {
                SwitchSettingRow(
                    title = "Crash Reporting",
                    description = "Help improve AVA",
                    checked = uiState.crashReportingEnabled,
                    onCheckedChange = { viewModel.setCrashReportingEnabled(it) },
                    icon = Icons.Default.BugReport
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                SwitchSettingRow(
                    title = "Analytics",
                    description = "Usage statistics",
                    checked = uiState.analyticsEnabled,
                    onCheckedChange = { viewModel.setAnalyticsEnabled(it) },
                    icon = Icons.Default.Analytics
                )
            }
        }

        // Appearance Section
        item {
            SettingsSectionCard(
                title = "Appearance",
                icon = Icons.Default.Palette
            ) {
                DropdownSettingRow(
                    title = "Theme",
                    description = uiState.theme,
                    options = listOf("System Default", "Light", "Dark"),
                    selectedOption = uiState.theme,
                    onOptionSelected = { viewModel.setTheme(it) },
                    icon = Icons.Default.DarkMode
                )
            }
        }

        // Storage & Models Section (spans full width in 2-column layout)
        item {
            SettingsSectionCard(
                title = "Storage & Models",
                icon = Icons.Default.Storage
            ) {
                ClickableSettingRow(
                    title = "Clear Cache",
                    description = "${uiState.cacheSize} MB",
                    onClick = { viewModel.clearCache() },
                    icon = Icons.Default.Delete
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ClickableSettingRow(
                    title = "Download Models",
                    description = "Manage AI models",
                    onClick = onNavigateToModelDownload,
                    icon = Icons.Default.Download
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ModelSelectionRow(
                    title = "RAG Embedding",
                    selectedModel = uiState.selectedEmbeddingModel,
                    onModelSelected = { viewModel.setEmbeddingModel(it) },
                    onShowModelInfo = { viewModel.showModelInfo(it) },
                    icon = Icons.Default.Memory
                )
            }
        }

        // Developer Section
        item {
            SettingsSectionCard(
                title = "Developer",
                icon = Icons.Default.Code
            ) {
                ClickableSettingRow(
                    title = "Run Tests",
                    description = "Test suite",
                    onClick = onNavigateToTestLauncher,
                    icon = Icons.Default.PlayArrow
                )
            }
        }

        // About Section (spans both columns)
        item {
            SettingsSectionCard(
                title = "About",
                icon = Icons.Default.Info
            ) {
                InfoRow("Version", "${BuildConfig.VERSION_NAME}")
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow("Build", BuildConfig.BUILD_TYPE)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                ClickableSettingRow(
                    title = "Licenses",
                    description = "OSS licenses",
                    onClick = { viewModel.openLicenses() },
                    icon = Icons.Default.Description
                )
            }
        }
    }
}

/**
 * Settings section card with title and icon
 */
@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Section content
            content()
        }
    }
}

/**
 * Switch setting row (compact)
 */
@Composable
private fun SwitchSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

/**
 * Slider setting row
 */
@Composable
private fun SliderSettingRow(
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.padding(start = 32.dp, top = 8.dp)
        )
    }
}

/**
 * Dropdown setting row
 */
@Composable
private fun DropdownSettingRow(
    title: String,
    description: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                        leadingIcon = if (option == selectedOption) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }
        }
    }
}

/**
 * Clickable setting row
 */
@Composable
private fun ClickableSettingRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Model selection row
 */
@Composable
private fun ModelSelectionRow(
    title: String,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    onShowModelInfo: (ModelInfo) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            val modelInfo = getModelInfo(selectedModel)
            Text(
                text = "${modelInfo.displayName} • ${modelInfo.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                getAvailableModels().forEach { modelInfo ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = modelInfo.displayName,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "${modelInfo.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onModelSelected(modelInfo.modelId)
                            expanded = false
                        },
                        leadingIcon = if (modelInfo.modelId == selectedModel) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onShowModelInfo(modelInfo)
                                    expanded = false
                                }
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Info row (label + value)
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

/**
 * Get available embedding models
 */
private fun getAvailableModels(): List<ModelInfo> {
    return listOf(
        ModelInfo(
            modelId = "AVA-ONX-384-BASE-INT8",
            displayName = "English Base (INT8) ⭐",
            languages = "English",
            size = "22 MB",
            dimensions = "384",
            quality = "High (95%)",
            description = "Recommended for most users. Quantized version with minimal quality loss.",
            downloadInstructions = "See model download screen",
            recommended = true
        ),
        ModelInfo(
            modelId = "AVA-ONX-384-MULTI-INT8",
            displayName = "Multilingual (INT8)",
            languages = "50+ languages",
            size = "117 MB",
            dimensions = "384",
            quality = "High (95%)",
            description = "Supports 50+ languages. Quantized for 75% size reduction.",
            downloadInstructions = "See model download screen"
        ),
        ModelInfo(
            modelId = "AVA-ONX-384-FAST-INT8",
            displayName = "Fast & Small (INT8)",
            languages = "English",
            size = "15 MB",
            dimensions = "384",
            quality = "Medium (85%)",
            description = "Smallest model. Fast but lower quality.",
            downloadInstructions = "See model download screen"
        )
    )
}

/**
 * Get model info by ID
 */
private fun getModelInfo(modelId: String): ModelInfo {
    return getAvailableModels().find { it.modelId == modelId }
        ?: getAvailableModels().first()
}
