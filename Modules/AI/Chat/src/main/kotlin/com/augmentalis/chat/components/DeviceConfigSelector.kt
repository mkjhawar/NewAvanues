/**
 * Device Configuration Selector
 *
 * UI component for selecting device profile and model configuration.
 * Used in settings screen or first-launch setup.
 *
 * Features:
 * - Auto-detected device display
 * - Device override selection
 * - Model configuration selection with ROI indicators
 * - Available/unavailable model status
 *
 * Created: 2025-11-30
 */

package com.augmentalis.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.llm.config.ConfigurationType
import com.augmentalis.llm.config.DeviceProfile
import com.augmentalis.llm.config.ModelConfiguration

/**
 * Device configuration selector bottom sheet
 *
 * @param show Whether to show the selector
 * @param detectedDevice Auto-detected device profile
 * @param currentConfig Currently active configuration
 * @param availableConfigs Configurations with installed models
 * @param allConfigs All compatible configurations
 * @param onDismiss Called when user dismisses
 * @param onSelectConfig Called when user selects a configuration
 * @param onOverrideDevice Called when user overrides device selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceConfigSelector(
    show: Boolean,
    detectedDevice: DeviceProfile,
    currentConfig: ModelConfiguration?,
    availableConfigs: List<ModelConfiguration>,
    allConfigs: List<ModelConfiguration>,
    onDismiss: () -> Unit,
    onSelectConfig: (ModelConfiguration) -> Unit,
    onOverrideDevice: (DeviceProfile) -> Unit
) {
    if (!show) return

    var showDeviceOverride by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "Model Configuration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Detected device card
            DeviceInfoCard(
                device = detectedDevice,
                onChangeDevice = { showDeviceOverride = !showDeviceOverride },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Device override dropdown
            AnimatedVisibility(
                visible = showDeviceOverride,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                DeviceOverrideSelector(
                    currentDevice = detectedDevice,
                    onSelectDevice = { device ->
                        onOverrideDevice(device)
                        showDeviceOverride = false
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Configuration list header
            Text(
                text = "Available Configurations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Configuration list
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allConfigs) { config ->
                    val isAvailable = availableConfigs.any { it.id == config.id }
                    val isSelected = currentConfig?.id == config.id

                    ConfigurationCard(
                        config = config,
                        isAvailable = isAvailable,
                        isSelected = isSelected,
                        onClick = {
                            if (isAvailable && config.runtimeAvailable) {
                                onSelectConfig(config)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Card showing detected device information
 */
@Composable
private fun DeviceInfoCard(
    device: DeviceProfile,
    onChangeDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AvanueTheme.colors.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = AvanueTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Detected Device",
                        style = MaterialTheme.typography.labelMedium,
                        color = AvanueTheme.colors.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AvanueTheme.colors.onPrimaryContainer
                )
                Text(
                    text = "${device.ramGB}GB RAM • ${device.gpuType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            TextButton(onClick = onChangeDevice) {
                Text("Change")
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Change device"
                )
            }
        }
    }
}

/**
 * Device override dropdown selector
 */
@Composable
private fun DeviceOverrideSelector(
    currentDevice: DeviceProfile,
    onSelectDevice: (DeviceProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AvanueTheme.colors.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Select Device Profile",
                style = MaterialTheme.typography.labelMedium,
                color = AvanueTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Group by manufacturer
            val devicesByManufacturer = DeviceProfile.values()
                .filter { !it.name.startsWith("GENERIC") }
                .groupBy { it.manufacturer }

            devicesByManufacturer.forEach { (manufacturer, devices) ->
                Text(
                    text = manufacturer,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AvanueTheme.colors.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                devices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectDevice(device) }
                            .background(
                                if (device == currentDevice)
                                    AvanueTheme.colors.primaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.displayName.removePrefix("$manufacturer "),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${device.ramGB}GB • ${device.gpuType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AvanueTheme.colors.textPrimary.copy(alpha = 0.6f)
                            )
                        }
                        if (device == currentDevice) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = AvanueTheme.colors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Configuration card showing model details and ROI
 */
@Composable
private fun ConfigurationCard(
    config: ModelConfiguration,
    isAvailable: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected -> AvanueTheme.colors.primary
        !isAvailable || !config.runtimeAvailable -> AvanueTheme.colors.border.copy(alpha = 0.3f)
        else -> AvanueTheme.colors.border.copy(alpha = 0.5f)
    }

    val backgroundColor = when {
        isSelected -> AvanueTheme.colors.primaryContainer.copy(alpha = 0.3f)
        !isAvailable || !config.runtimeAvailable -> AvanueTheme.colors.surface.copy(alpha = 0.5f)
        else -> AvanueTheme.colors.surface
    }

    val contentAlpha = if (isAvailable && config.runtimeAvailable) 1f else 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                enabled = isAvailable && config.runtimeAvailable,
                onClick = onClick
            )
            .semantics {
                contentDescription = "${config.displayName}. ${config.description}. " +
                    "ROI: ${config.getRoiDisplay()}. " +
                    if (!isAvailable) "Model not installed."
                    else if (!config.runtimeAvailable) "Runtime not available."
                    else "Available."
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header row with name and type badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AvanueTheme.colors.textPrimary.copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Type badge
                val typeColor = when (config.type) {
                    ConfigurationType.BASE -> AvanueTheme.colors.tertiary
                    ConfigurationType.MULTILINGUAL -> AvanueTheme.colors.secondary
                    ConfigurationType.GEMMA3N -> AvanueTheme.colors.primary
                }
                Surface(
                    color = typeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = config.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = config.description,
                style = MaterialTheme.typography.bodySmall,
                color = AvanueTheme.colors.textPrimary.copy(alpha = 0.7f * contentAlpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Model info
                Column {
                    Text(
                        text = "LLM: ${config.llmModel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AvanueTheme.colors.textPrimary.copy(alpha = 0.6f * contentAlpha)
                    )
                    Text(
                        text = "${config.totalMemoryGB}GB • ${config.estimatedSpeed}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AvanueTheme.colors.textPrimary.copy(alpha = 0.6f * contentAlpha)
                    )
                }

                // ROI indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoiIndicator(
                        score = config.roiScore,
                        alpha = contentAlpha
                    )
                }
            }

            // Status badges
            if (!isAvailable || !config.runtimeAvailable || isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSelected) {
                        StatusBadge(
                            text = "Active",
                            color = AvanueTheme.colors.primary
                        )
                    }
                    if (!config.runtimeAvailable) {
                        StatusBadge(
                            text = "${config.llmRuntime} (Coming Soon)",
                            color = AvanueTheme.colors.border
                        )
                    } else if (!isAvailable) {
                        StatusBadge(
                            text = "Model Not Installed",
                            color = AvanueTheme.colors.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * ROI score indicator
 */
@Composable
private fun RoiIndicator(
    score: Int,
    alpha: Float = 1f
) {
    val color = when {
        score >= 9 -> Color(0xFF4CAF50)  // Green
        score >= 7 -> Color(0xFFFFC107)  // Amber
        else -> Color(0xFF9E9E9E)        // Gray
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { index ->
            val filled = index < (score + 1) / 2
            Icon(
                imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = color.copy(alpha = if (filled) alpha else 0.3f * alpha),
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = when {
                score >= 10 -> "Optimal"
                score >= 9 -> "Excellent"
                score >= 8 -> "Very Good"
                score >= 7 -> "Good"
                else -> "Basic"
            },
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = alpha)
        )
    }
}

/**
 * Small status badge
 */
@Composable
private fun StatusBadge(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ==================== Previews ====================

@Preview(showBackground = true)
@Composable
private fun DeviceInfoCardPreview() {
    MaterialTheme {
        DeviceInfoCard(
            device = DeviceProfile.NAVIGATOR_500,
            onChangeDevice = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfigurationCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Available and selected
            ConfigurationCard(
                config = ModelConfiguration(
                    id = "navigator-base",
                    displayName = "Navigator Base English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE2-2B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.97f,
                    estimatedSpeed = "18 t/s",
                    roiScore = 9,
                    runtimeAvailable = true,
                    description = "Recommended. Best quality-per-GB for technical guidance."
                ),
                isAvailable = true,
                isSelected = true,
                onClick = {}
            )

            // Available but not selected
            ConfigurationCard(
                config = ModelConfiguration(
                    id = "navigator-multilingual",
                    displayName = "Navigator Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-17B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 2.25f,
                    estimatedSpeed = "22 t/s",
                    roiScore = 8,
                    runtimeAvailable = true,
                    description = "Excellent multilingual. 85%+ accuracy in 100 languages."
                ),
                isAvailable = true,
                isSelected = false,
                onClick = {}
            )

            // Future (runtime not available)
            ConfigurationCard(
                config = ModelConfiguration(
                    id = "navigator-gemma3n",
                    displayName = "Navigator Gemma 3n (Future)",
                    type = ConfigurationType.GEMMA3N,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE3N-2B",
                    llmRuntime = "LiteRT",
                    totalMemoryGB = 2.45f,
                    estimatedSpeed = "20 t/s",
                    roiScore = 10,
                    runtimeAvailable = false,
                    description = "Future: Best quality when LiteRT available."
                ),
                isAvailable = false,
                isSelected = false,
                onClick = {}
            )
        }
    }
}
