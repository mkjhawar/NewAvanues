// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/settings/SettingsScreen.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.augmentalis.ava.BuildConfig

/**
 * Settings screen for AVA AI application
 *
 * Provides configuration options for:
 * - NLU settings (intent classification threshold, cache size)
 * - LLM settings (provider selection, model configuration)
 * - UI preferences (theme, language)
 * - Privacy settings (crash reporting, analytics)
 * - About information (version, credits)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ISettingsViewModel,
    onNavigateToModelDownload: () -> Unit = {},
    onNavigateToTestLauncher: () -> Unit = {},
    onNavigateToDeveloperSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            // Compact header - transparent, minimal height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // NLU Settings Section
            item {
                SettingsSectionHeader("Natural Language Understanding")
            }

            item {
                SwitchSettingItem(
                    title = "Enable NLU",
                    description = "Use on-device intent classification",
                    checked = uiState.nluEnabled,
                    onCheckedChange = { viewModel.setNluEnabled(it) },
                    icon = Icons.Default.Psychology
                )
            }

            // NLU Model Download
            item {
                NLUModelDownloadItem(
                    modelName = "mALBERT Multilingual",
                    modelSize = "41 MB",
                    isDownloaded = uiState.nluModelDownloaded,
                    downloadProgress = uiState.nluDownloadProgress,
                    onDownload = { viewModel.downloadNLUModel() },
                    onDelete = { viewModel.deleteNLUModel() }
                )
            }

            item {
                SliderSettingItem(
                    title = "Confidence Threshold",
                    description = "Minimum confidence for intent classification (${(uiState.nluConfidenceThreshold * 100).toInt()}%)",
                    value = uiState.nluConfidenceThreshold,
                    onValueChange = { viewModel.setNluConfidenceThreshold(it) },
                    enabled = uiState.nluEnabled,
                    valueRange = 0.5f..0.95f,
                    icon = Icons.Default.TrendingUp
                )
            }

            // ADR-014: Advanced NLU Thresholds (Developer Mode Only)
            if (uiState.developerModeEnabled) {
                item {
                    SliderSettingItem(
                        title = "Teach Threshold",
                        description = "Show 'Teach AVA' below this confidence (${(uiState.teachThreshold * 100).toInt()}%)",
                        value = uiState.teachThreshold,
                        onValueChange = { viewModel.setTeachThreshold(it) },
                        enabled = uiState.nluEnabled,
                        valueRange = 0.3f..0.7f,
                        icon = Icons.Default.School
                    )
                }

                item {
                    SliderSettingItem(
                        title = "LLM Fallback Threshold",
                        description = "Route to LLM below this confidence (${(uiState.llmFallbackThreshold * 100).toInt()}%)",
                        value = uiState.llmFallbackThreshold,
                        onValueChange = { viewModel.setLLMFallbackThreshold(it) },
                        enabled = uiState.nluEnabled,
                        valueRange = 0.5f..0.9f,
                        icon = Icons.Default.CloudSync
                    )
                }

                item {
                    SliderSettingItem(
                        title = "Self-Learning Threshold",
                        description = "Learn from LLM responses above this confidence (${(uiState.selfLearningThreshold * 100).toInt()}%)",
                        value = uiState.selfLearningThreshold,
                        onValueChange = { viewModel.setSelfLearningThreshold(it) },
                        enabled = uiState.nluEnabled,
                        valueRange = 0.5f..0.9f,
                        icon = Icons.Default.AutoAwesome
                    )
                }
            }

            // LLM Settings Section
            item {
                SettingsSectionHeader("Language Model")
            }

            item {
                DropdownSettingItem(
                    title = "LLM Provider",
                    description = uiState.llmProvider,
                    options = listOf("Local (On-Device)", "Anthropic (Claude)", "OpenRouter"),
                    selectedOption = uiState.llmProvider,
                    onOptionSelected = { viewModel.setLlmProvider(it) },
                    icon = Icons.Default.Cloud
                )
            }

            item {
                SwitchSettingItem(
                    title = "Enable Streaming",
                    description = "Stream LLM responses in real-time",
                    checked = uiState.llmStreamingEnabled,
                    onCheckedChange = { viewModel.setLlmStreamingEnabled(it) },
                    icon = Icons.Default.Stream
                )
            }

            // Chat Preferences Section
            item {
                SettingsSectionHeader("Chat Preferences")
            }

            item {
                DropdownSettingItem(
                    title = "Conversation Mode",
                    description = uiState.conversationMode,
                    options = listOf("Append", "New"),
                    selectedOption = uiState.conversationMode,
                    onOptionSelected = { viewModel.setConversationMode(it) },
                    icon = Icons.Default.Chat
                )
            }

            // App Preferences Section (Chapter 71: Intelligent Resolution)
            item {
                SettingsSectionHeader("Default Apps")
            }

            if (uiState.savedAppPreferences.isEmpty()) {
                item {
                    InfoSettingItem(
                        title = "No app preferences set",
                        value = "AVA will ask when needed",
                        icon = Icons.Default.Apps
                    )
                }
            }

            items(
                items = uiState.savedAppPreferences,
                key = { it.capability }
            ) { pref ->
                AppPreferenceSettingItem(
                    capability = pref.capabilityDisplayName,
                    appName = pref.appName,
                    onClear = { viewModel.clearAppPreference(pref.capability) }
                )
            }

            if (uiState.savedAppPreferences.isNotEmpty()) {
                item {
                    ClickableSettingItem(
                        title = "Reset All App Preferences",
                        description = "AVA will ask again for each capability",
                        onClick = { viewModel.clearAllAppPreferences() },
                        icon = Icons.Default.RestartAlt
                    )
                }
            }

            // Privacy Settings Section
            item {
                SettingsSectionHeader("Privacy & Data")
            }

            item {
                SwitchSettingItem(
                    title = "Crash Reporting",
                    description = "Send crash reports to improve AVA",
                    checked = uiState.crashReportingEnabled,
                    onCheckedChange = { viewModel.setCrashReportingEnabled(it) },
                    icon = Icons.Default.BugReport
                )
            }

            item {
                SwitchSettingItem(
                    title = "Analytics",
                    description = "Share anonymous usage statistics",
                    checked = uiState.analyticsEnabled,
                    onCheckedChange = { viewModel.setAnalyticsEnabled(it) },
                    icon = Icons.Default.Analytics
                )
            }

            // UI Preferences Section
            item {
                SettingsSectionHeader("Appearance")
            }

            item {
                DropdownSettingItem(
                    title = "Theme",
                    description = uiState.theme,
                    options = listOf("System Default", "Light", "Dark"),
                    selectedOption = uiState.theme,
                    onOptionSelected = { viewModel.setTheme(it) },
                    icon = Icons.Default.Palette
                )
            }

            // Storage Section
            item {
                SettingsSectionHeader("Storage")
            }

            item {
                ClickableSettingItem(
                    title = "Clear Cache",
                    description = "Free up ${uiState.cacheSize} MB",
                    onClick = { viewModel.clearCache() },
                    icon = Icons.Default.Delete
                )
            }

            item {
                ClickableSettingItem(
                    title = "Download Models",
                    description = "Manage offline AI models",
                    onClick = onNavigateToModelDownload,
                    icon = Icons.Default.Download,
                    modifier = Modifier.semantics { testTag = "downloadModelsButton" }
                )
            }

            // Developer Settings Section
            item {
                SettingsSectionHeader("Developer Settings")
            }

            item {
                ClickableSettingItem(
                    title = "Developer Options",
                    description = "Flash mode, verbose logging, performance metrics",
                    onClick = onNavigateToDeveloperSettings,
                    icon = Icons.Default.DeveloperMode,
                    modifier = Modifier.semantics { testTag = "developerOptionsButton" }
                )
            }

            item {
                ClickableSettingItem(
                    title = "Run Automated Tests",
                    description = "Test language detection, token sampling, and more",
                    onClick = onNavigateToTestLauncher,
                    icon = Icons.Default.BugReport,
                    modifier = Modifier.semantics { testTag = "runAutomatedTestsButton" }
                )
            }

            // Language-specific recommendation banner
            if (uiState.deviceLanguage != "en" && !uiState.selectedEmbeddingModel.contains("MULTI")) {
                item {
                    LanguageRecommendationBanner(
                        deviceLanguage = uiState.deviceLanguage,
                        recommendedModel = uiState.recommendedModel,
                        onDownloadClick = {
                            viewModel.setEmbeddingModel(uiState.recommendedModel)
                            viewModel.startModelDownload(uiState.recommendedModel)
                        }
                    )
                }
            }

            item {
                ModelSelectionSettingItem(
                    title = "RAG Embedding Model",
                    selectedModel = uiState.selectedEmbeddingModel,
                    onModelSelected = { viewModel.setEmbeddingModel(it) },
                    onShowModelInfo = { viewModel.showModelInfo(it) },
                    icon = Icons.Default.Memory
                )
            }

            // About Section
            item {
                SettingsSectionHeader("About")
            }

            item {
                InfoSettingItem(
                    title = "Version",
                    value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    icon = Icons.Default.Info
                )
            }

            item {
                InfoSettingItem(
                    title = "Build Type",
                    value = BuildConfig.BUILD_TYPE,
                    icon = Icons.Default.Build
                )
            }

            item {
                ClickableSettingItem(
                    title = "Licenses",
                    description = "Open source licenses",
                    onClick = { viewModel.openLicenses() },
                    icon = Icons.Default.Description
                )
            }

            // Spacer at bottom
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Show model info dialog if needed (outside LazyColumn, overlay on top)
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

@Composable
fun SettingsSectionHeader(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = Color(0xFF3B82F6), // CoralBlue
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SwitchSettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.38f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.38f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun SliderSettingItem(
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.38f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.38f)
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.padding(start = 40.dp, top = 8.dp)
        )
    }
}

@Composable
fun DropdownSettingItem(
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
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Expand",
            tint = Color.White.copy(alpha = 0.7f)
        )

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

@Composable
fun ClickableSettingItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open",
            tint = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun InfoSettingItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * Setting item for displaying and clearing app preferences
 *
 * Part of Intelligent Resolution System (Chapter 71)
 */
@Composable
fun AppPreferenceSettingItem(
    capability: String,
    appName: String,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = null,
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = capability,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        IconButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear preference",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Model selection setting item with info button
 */
@Composable
fun ModelSelectionSettingItem(
    title: String,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    onShowModelInfo: (ModelInfo) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                val modelInfo = getModelInfo(selectedModel)
                Text(
                    text = "${modelInfo.displayName} • ${modelInfo.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Expand",
                tint = Color.White.copy(alpha = 0.7f)
            )

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
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${modelInfo.languages} • ${modelInfo.size}",
                                    style = MaterialTheme.typography.bodySmall,
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
                                    contentDescription = "Model Info",
                                    tint = MaterialTheme.colorScheme.primary
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
 * Model information dialog
 */
@Composable
fun ModelInfoDialog(
    modelInfo: ModelInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Memory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(modelInfo.displayName)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Model Details
                InfoRow("Model ID", modelInfo.modelId)
                InfoRow("Languages", modelInfo.languages)
                InfoRow("File Size", modelInfo.size)
                InfoRow("Dimensions", modelInfo.dimensions)
                InfoRow("Quality", modelInfo.quality)

                Divider()

                // Description
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = modelInfo.description,
                    style = MaterialTheme.typography.bodyMedium
                )

                Divider()

                // Download Instructions
                Text(
                    text = "Download Instructions",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = modelInfo.downloadInstructions,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
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
 * Model information data class
 */
data class ModelInfo(
    val modelId: String,
    val displayName: String,
    val languages: String,
    val size: String,
    val dimensions: String,
    val quality: String,
    val description: String,
    val downloadInstructions: String,
    val recommended: Boolean = false
)

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
            description = "Recommended for most users. Quantized version of all-MiniLM-L6-v2 with 75% size reduction and minimal quality loss (3-5%). Best balance of size, speed, and accuracy for English documents.",
            downloadInstructions = """
                1. Download: curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx -o model.onnx
                2. Quantize: python3 scripts/required/quantize-models.py model.onnx AVA-ONX-384-BASE-INT8.AON int8
                3. Push: adb push AVA-ONX-384-BASE-INT8.AON /sdcard/Android/data/com.augmentalis.ava/files/models/
            """.trimIndent(),
            recommended = true
        ),
        ModelInfo(
            modelId = "AVA-ONX-384-MULTI-INT8",
            displayName = "Multilingual (INT8)",
            languages = "50+ languages",
            size = "117 MB",
            dimensions = "384",
            quality = "High (95%)",
            description = "Supports 50+ languages including English, Spanish, French, German, Chinese, Japanese, Arabic, Hindi, and more. Enables cross-lingual search (query in one language, find results in another). Quantized for 75% size reduction.",
            downloadInstructions = """
                1. Download: curl -L https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx -o model.onnx
                2. Quantize: python3 scripts/required/quantize-models.py model.onnx AVA-ONX-384-MULTI-INT8.AON int8
                3. Push: adb push AVA-ONX-384-MULTI-INT8.AON /sdcard/Android/data/com.augmentalis.ava/files/models/
            """.trimIndent()
        ),
        ModelInfo(
            modelId = "AVA-ONX-384-FAST-INT8",
            displayName = "Fast & Small (INT8)",
            languages = "English",
            size = "15 MB",
            dimensions = "384",
            quality = "Medium (85%)",
            description = "Smallest embedding model. Lower quality but very fast. Best for memory-constrained devices or quick prototyping. Based on paraphrase-MiniLM-L3-v2.",
            downloadInstructions = """
                1. Download: curl -L https://huggingface.co/sentence-transformers/paraphrase-MiniLM-L3-v2/resolve/main/onnx/model.onnx -o model.onnx
                2. Quantize: python3 scripts/required/quantize-models.py model.onnx AVA-ONX-384-FAST-INT8.AON int8
                3. Push: adb push AVA-ONX-384-FAST-INT8.AON /sdcard/Android/data/com.augmentalis.ava/files/models/
            """.trimIndent()
        ),
        ModelInfo(
            modelId = "AVA-ONX-768-QUAL-INT8",
            displayName = "High Quality (INT8)",
            languages = "English",
            size = "105 MB",
            dimensions = "768",
            quality = "Very High (98%)",
            description = "Best quality English embeddings. Higher dimensional embeddings (768 vs 384) provide better semantic understanding. Based on all-mpnet-base-v2. Use for production applications requiring maximum accuracy.",
            downloadInstructions = """
                1. Download: curl -L https://huggingface.co/sentence-transformers/all-mpnet-base-v2/resolve/main/onnx/model.onnx -o model.onnx
                2. Quantize: python3 scripts/required/quantize-models.py model.onnx AVA-ONX-768-QUAL-INT8.AON int8
                3. Push: adb push AVA-ONX-768-QUAL-INT8.AON /sdcard/Android/data/com.augmentalis.ava/files/models/
            """.trimIndent()
        ),
        ModelInfo(
            modelId = "AVA-ONX-384-BASE",
            displayName = "English Base (FP32)",
            languages = "English",
            size = "86 MB",
            dimensions = "384",
            quality = "Very High (100%)",
            description = "Original unquantized version. Use only if quantized version shows quality issues. 4x larger than INT8 version with minimal quality difference.",
            downloadInstructions = """
                1. Download: curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx -o AVA-ONX-384-BASE.AON
                2. Push: adb push AVA-ONX-384-BASE.AON /sdcard/Android/data/com.augmentalis.ava/files/models/
            """.trimIndent()
        ),
        ModelInfo(
            modelId = "AVA-ONX-384-MULTI",
            displayName = "Multilingual (FP32)",
            languages = "50+ languages",
            size = "470 MB",
            dimensions = "384",
            quality = "Very High (100%)",
            description = "Original unquantized multilingual model. Use only if quantized version shows quality issues. 4x larger than INT8 version.",
            downloadInstructions = """
                1. Download: curl -L https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx -o AVA-ONX-384-MULTI.AON
                2. Push: adb push AVA-ONX-384-MULTI.AON /sdcard/Android/data/com.augmentalis.ava/files/models/
            """.trimIndent()
        )
    )
}

/**
 * Get model info by ID
 */
private fun getModelInfo(modelId: String): ModelInfo {
    return getAvailableModels().find { it.modelId == modelId }
        ?: getAvailableModels().first() // Default to first model
}

/**
 * Language recommendation banner
 *
 * Shows when device language is not English, suggesting multilingual model
 */
@Composable
fun LanguageRecommendationBanner(
    deviceLanguage: String,
    recommendedModel: String,
    onDownloadClick: () -> Unit
) {
    val languageName = getLanguageName(deviceLanguage)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Multilingual Model Recommended",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Device language: $languageName. Download multilingual model (113 MB) for better results in your language.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onDownloadClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download")
            }
        }
    }
}

/**
 * Get human-readable language name from ISO code
 */
private fun getLanguageName(languageCode: String): String {
    return when (languageCode) {
        "zh" -> "Chinese (中文)"
        "ja" -> "Japanese (日本語)"
        "ko" -> "Korean (한국어)"
        "hi" -> "Hindi (हिन्दी)"
        "ru" -> "Russian (Русский)"
        "ar" -> "Arabic (العربية)"
        "es" -> "Spanish (Español)"
        "fr" -> "French (Français)"
        "de" -> "German (Deutsch)"
        "it" -> "Italian (Italiano)"
        "pt" -> "Portuguese (Português)"
        else -> java.util.Locale(languageCode).displayLanguage
    }
}

/**
 * NLU Model Download Item
 *
 * Shows download/delete button with progress indicator
 */
@Composable
fun NLUModelDownloadItem(
    modelName: String,
    modelSize: String,
    isDownloaded: Boolean,
    downloadProgress: Float,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = modelName,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            if (downloadProgress > 0f && downloadProgress < 1f) {
                // Show progress
                Text(
                    text = "Downloading: ${(downloadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                LinearProgressIndicator(
                    progress = downloadProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = Color(0xFF3B82F6)
                )
            } else {
                // Show size or status
                val statusText = when {
                    isDownloaded -> "Downloaded ($modelSize)"
                    else -> "Available for download ($modelSize)"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Download or Delete button
        if (isDownloaded) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete model",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        } else if (downloadProgress > 0f && downloadProgress < 1f) {
            // Downloading - show cancel button (future enhancement)
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFF3B82F6),
                strokeWidth = 2.dp
            )
        } else {
            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                )
            ) {
                Text("Download")
            }
        }
    }
}
