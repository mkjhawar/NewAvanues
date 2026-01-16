package com.augmentalis.voiceoscoreng.features

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Sample scaffold demonstrating VoiceOSCoreNG integration with TestModeFab.
 *
 * This composable provides a ready-to-use scaffold that includes:
 * - Test Mode FAB for quick access to all testing features
 * - Developer Settings bottom sheet
 * - Content slot for your app's main content
 *
 * Usage:
 * ```kotlin
 * VoiceOSCoreNGScaffold { paddingValues ->
 *     // Your main content here
 *     MyMainScreen(modifier = Modifier.padding(paddingValues))
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceOSCoreNGScaffold(
    modifier: Modifier = Modifier,
    showTestFab: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    var showDeveloperSettings by remember { mutableStateOf(false) }

    // Developer Settings Bottom Sheet
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (showDeveloperSettings) {
        ModalBottomSheet(
            onDismissRequest = { showDeveloperSettings = false },
            sheetState = sheetState,
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            DeveloperSettingsScreen(
                onDismiss = { showDeveloperSettings = false }
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Main content
        content(PaddingValues(0.dp))

        // Test Mode FAB
        if (showTestFab) {
            TestModeFab(
                onOpenSettings = { showDeveloperSettings = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Preview/Demo screen for testing VoiceOSCoreNG functionality.
 *
 * Shows current configuration status and provides quick actions
 * for testing without needing to integrate into a full app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceOSCoreNGDemoScreen() {
    var configSummary by remember {
        mutableStateOf(com.augmentalis.voiceoscoreng.features.LearnAppConfig.getSummary())
    }

    // Listen for config changes
    DisposableEffect(Unit) {
        val listener: (com.augmentalis.voiceoscoreng.features.LearnAppConfig.VariantConfig) -> Unit = {
            configSummary = com.augmentalis.voiceoscoreng.features.LearnAppConfig.getSummary()
        }
        com.augmentalis.voiceoscoreng.features.LearnAppConfig.addConfigChangeListener(listener)

        onDispose {
            com.augmentalis.voiceoscoreng.features.LearnAppConfig.removeConfigChangeListener(listener)
        }
    }

    VoiceOSCoreNGScaffold { _ ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("VoiceOSCoreNG Demo") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Current Configuration",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            configSummary,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                // Instructions Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "How to Use",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            """
                            1. Tap the FAB (🧪) in the bottom-right
                            2. Enable "Full Test Mode" to unlock all features
                            3. Run tests to verify functionality
                            4. Open "Developer Settings" to customize limits
                            5. The config summary above updates in real-time
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Quick Actions
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            com.augmentalis.voiceoscoreng.handlers.VoiceOSCoreNG.enableTestMode()
                            configSummary = com.augmentalis.voiceoscoreng.features.LearnAppConfig.getSummary()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Enable Test Mode")
                    }

                    OutlinedButton(
                        onClick = {
                            com.augmentalis.voiceoscoreng.handlers.VoiceOSCoreNG.reset()
                            configSummary = com.augmentalis.voiceoscoreng.features.LearnAppConfig.getSummary()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            com.augmentalis.voiceoscoreng.handlers.VoiceOSCoreNG.setTier(
                                com.augmentalis.voiceoscoreng.features.LearnAppDevToggle.Tier.LITE
                            )
                            configSummary = com.augmentalis.voiceoscoreng.features.LearnAppConfig.getSummary()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set LITE Tier")
                    }

                    FilledTonalButton(
                        onClick = {
                            com.augmentalis.voiceoscoreng.handlers.VoiceOSCoreNG.setTier(
                                com.augmentalis.voiceoscoreng.features.LearnAppDevToggle.Tier.DEV
                            )
                            configSummary = com.augmentalis.voiceoscoreng.features.LearnAppConfig.getSummary()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set DEV Tier")
                    }
                }
            }
        }
    }
}
