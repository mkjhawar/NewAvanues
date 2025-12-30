/**
 * DeveloperSettingsActivity.kt - Developer settings activity with subscription control
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * Provides developer settings UI for testing subscription tiers and feature gates.
 * Follows Material 3 design guidelines and SOLID principles.
 */

package com.augmentalis.voiceoscore.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.augmentalis.voiceoscore.ui.ContentCaptureSafeComposeActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppPreferences
import com.augmentalis.voiceoscore.learnapp.subscription.DeveloperSubscriptionProvider
import com.augmentalis.voiceoscore.learnapp.subscription.FeatureGateManager
import com.augmentalis.voiceoscore.learnapp.subscription.LearningMode
import com.augmentalis.voiceoscore.learnapp.subscription.SubscriptionTier
import kotlinx.coroutines.launch

/**
 * Developer Settings Activity
 *
 * Activity for testing subscription tiers and feature gates.
 * Provides UI controls for:
 * - Developer override toggle (default: all features unlocked)
 * - Subscription status testing (LearnAppLite, LearnAppPro)
 * - Current mode display
 * - Force rescan button
 */
class DeveloperSettingsActivity : ContentCaptureSafeComposeActivity() {

    companion object {
        private const val TAG = "DeveloperSettings"

        /**
         * Create intent to launch developer settings
         */
        fun createIntent(context: Context): Intent {
            return Intent(context, DeveloperSettingsActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        val featureGateManager = FeatureGateManager(applicationContext)
        val subscriptionProvider = DeveloperSubscriptionProvider(applicationContext)

        setContentSafely {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DeveloperSettingsScreen(
                        featureGateManager = featureGateManager,
                        subscriptionProvider = subscriptionProvider,
                        onNavigateBack = { finish() },
                        onForceRescan = { handleForceRescan() }
                    )
                }
            }
        }
    }

    /**
     * Handle force rescan request
     */
    private fun handleForceRescan() {
        Toast.makeText(
            this,
            "Force rescan triggered - feature not yet implemented",
            Toast.LENGTH_SHORT
        ).show()
        // TODO: Implement force rescan via ExplorationEngine
        // This will trigger a full re-exploration of the current app
    }
}

/**
 * Developer Settings Screen
 *
 * Main composable for developer settings UI.
 * Displays subscription controls, feature gates, and current mode.
 *
 * @param featureGateManager Manager for feature access control
 * @param subscriptionProvider Provider for subscription status
 * @param onNavigateBack Callback when back button is pressed
 * @param onForceRescan Callback when force rescan is requested
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    featureGateManager: FeatureGateManager,
    subscriptionProvider: DeveloperSubscriptionProvider,
    onNavigateBack: () -> Unit = {},
    onForceRescan: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // State for current mode
    var currentMode by remember { mutableStateOf<LearningMode?>(null) }

    // State for developer override
    var isDeveloperOverrideEnabled by remember {
        mutableStateOf(featureGateManager.isDeveloperOverrideEnabled())
    }

    // State for subscription status
    var isLiteActive by remember { mutableStateOf(false) }
    var isProActive by remember { mutableStateOf(false) }

    // Load initial subscription status
    LaunchedEffect(Unit) {
        isLiteActive = subscriptionProvider.hasActiveSubscription(SubscriptionTier.LITE)
        isProActive = subscriptionProvider.hasActiveSubscription(SubscriptionTier.PRO)
        currentMode = featureGateManager.getHighestAccessibleMode()
    }

    // Update current mode when settings change
    LaunchedEffect(isDeveloperOverrideEnabled, isLiteActive, isProActive) {
        currentMode = featureGateManager.getHighestAccessibleMode()
    }

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Developer Override Section
            DeveloperOverrideSection(
                isEnabled = isDeveloperOverrideEnabled,
                onToggle = { enabled ->
                    featureGateManager.setDeveloperOverride(enabled)
                    isDeveloperOverrideEnabled = enabled
                    Toast.makeText(
                        context,
                        if (enabled) "All features unlocked" else "Subscription enforcement enabled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            HorizontalDivider()

            // LearnApp Mode Controls Section
            LearnAppModeControlsSection(context = context)

            HorizontalDivider()

            // Subscription Testing Section
            SubscriptionTestingSection(
                isLiteActive = isLiteActive,
                isProActive = isProActive,
                isDeveloperOverrideEnabled = isDeveloperOverrideEnabled,
                onLiteToggle = { active ->
                    subscriptionProvider.setSubscriptionStatus(SubscriptionTier.LITE, active)
                    isLiteActive = active
                    scope.launch {
                        currentMode = featureGateManager.getHighestAccessibleMode()
                    }
                },
                onProToggle = { active ->
                    subscriptionProvider.setSubscriptionStatus(SubscriptionTier.PRO, active)
                    isProActive = active
                    scope.launch {
                        currentMode = featureGateManager.getHighestAccessibleMode()
                    }
                }
            )

            HorizontalDivider()

            // Current Mode Section
            CurrentModeSection(currentMode = currentMode)

            HorizontalDivider()

            // Actions Section
            ActionsSection(onForceRescan = onForceRescan)
        }
    }
}

/**
 * Developer Override Section
 *
 * Toggle for enabling/disabling all features (default: ON)
 */
@Composable
fun DeveloperOverrideSection(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Developer Override",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            SwitchPreference(
                title = "Unlock All Features",
                summary = if (isEnabled) {
                    "Default ON - All features unlocked for testing. Turn OFF to test subscription tiers."
                } else {
                    "Subscription enforcement active. Only subscribed features are accessible."
                },
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

/**
 * Subscription Testing Section
 *
 * Checkboxes for testing subscription status
 */
@Composable
fun SubscriptionTestingSection(
    isLiteActive: Boolean,
    isProActive: Boolean,
    isDeveloperOverrideEnabled: Boolean,
    onLiteToggle: (Boolean) -> Unit,
    onProToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Subscription Testing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isDeveloperOverrideEnabled) {
                Text(
                    text = "Note: Developer Override is ON. These settings have no effect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            CheckboxPreference(
                title = "LearnAppLite Subscription",
                summary = "Mid-tier: Menu/drawer deep scan ($2.99/month or $20/year)",
                checked = isLiteActive,
                enabled = !isDeveloperOverrideEnabled,
                onCheckedChange = onLiteToggle
            )

            CheckboxPreference(
                title = "LearnAppPro Subscription",
                summary = "Premium: Full exploration + export ($9.99/month or $80/year)",
                checked = isProActive,
                enabled = !isDeveloperOverrideEnabled,
                onCheckedChange = onProToggle
            )
        }
    }
}

/**
 * Current Mode Section
 *
 * Displays the highest accessible learning mode
 */
@Composable
fun CurrentModeSection(currentMode: LearningMode?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (currentMode) {
                LearningMode.PRO -> MaterialTheme.colorScheme.primaryContainer
                LearningMode.LITE -> MaterialTheme.colorScheme.tertiaryContainer
                LearningMode.JIT -> MaterialTheme.colorScheme.secondaryContainer
                null -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Current Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Highest Accessible Mode:",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = when (currentMode) {
                        LearningMode.PRO -> "LearnAppPro"
                        LearningMode.LITE -> "LearnAppLite"
                        LearningMode.JIT -> "JIT (Free)"
                        null -> "Loading..."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = when (currentMode) {
                        LearningMode.PRO -> MaterialTheme.colorScheme.primary
                        LearningMode.LITE -> MaterialTheme.colorScheme.tertiary
                        LearningMode.JIT -> MaterialTheme.colorScheme.secondary
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Text(
                text = when (currentMode) {
                    LearningMode.PRO -> "Full exploration with all features enabled"
                    LearningMode.LITE -> "Mid-tier exploration with menu/drawer scanning"
                    LearningMode.JIT -> "Quick learning mode (passive, free)"
                    null -> "Calculating current mode..."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Actions Section
 *
 * Action buttons for testing features
 */
@Composable
fun ActionsSection(onForceRescan: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onForceRescan,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Force Rescan Current App")
            }
        }
    }
}

/**
 * Switch Preference Composable
 *
 * Displays a preference with a switch control
 */
@Composable
fun SwitchPreference(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
 * Checkbox Preference Composable
 *
 * Displays a preference with a checkbox control
 */
@Composable
fun CheckboxPreference(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 1f else 0.5f
                )
            )
        }

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

/**
 * LearnApp Mode Controls Section
 *
 * Toggles for JIT Learning, LearnApp Dev Mode, and Exploration
 */
@Composable
fun LearnAppModeControlsSection(context: Context) {
    val learnAppPrefs = remember { LearnAppPreferences(context) }
    val devSettings = remember { LearnAppDeveloperSettings(context) }

    var isJitEnabled by remember { mutableStateOf(learnAppPrefs.isJitLearningEnabled) }
    var isDevModeEnabled by remember { mutableStateOf(devSettings.isDeveloperModeEnabled()) }
    var isExplorationEnabled by remember { mutableStateOf(learnAppPrefs.isExplorationEnabled) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "LearnApp Mode Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Control which LearnApp modes are active",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // JIT Learning Toggle
            SwitchPreference(
                title = "JIT Learning Mode",
                summary = if (isJitEnabled) {
                    "ON - Passive learning from accessibility events (always free)"
                } else {
                    "OFF - No automatic learning"
                },
                checked = isJitEnabled,
                onCheckedChange = { enabled ->
                    learnAppPrefs.isJitLearningEnabled = enabled
                    isJitEnabled = enabled
                    Toast.makeText(
                        context,
                        if (enabled) "JIT Learning enabled" else "JIT Learning disabled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            // Developer Mode Toggle
            SwitchPreference(
                title = "Developer Mode",
                summary = if (isDevModeEnabled) {
                    "ON - Debug overlays, verbose logging, and developer tools enabled"
                } else {
                    "OFF - Standard user mode"
                },
                checked = isDevModeEnabled,
                onCheckedChange = { enabled ->
                    devSettings.setDeveloperModeEnabled(enabled)
                    isDevModeEnabled = enabled
                    Toast.makeText(
                        context,
                        if (enabled) "Developer mode enabled" else "Developer mode disabled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            // Exploration Toggle
            SwitchPreference(
                title = "Active Exploration",
                summary = if (isExplorationEnabled) {
                    "ON - Active exploration and deep scanning enabled"
                } else {
                    "OFF - Only passive JIT learning"
                },
                checked = isExplorationEnabled,
                onCheckedChange = { enabled ->
                    learnAppPrefs.isExplorationEnabled = enabled
                    isExplorationEnabled = enabled
                    Toast.makeText(
                        context,
                        if (enabled) "Exploration enabled" else "Exploration disabled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}
