/**
 * SettingsActivity.kt - Main VOS4 Settings Hub
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-22
 */

package com.augmentalis.voiceos.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Main Settings Activity - VOS4 Control Center
 */
class SettingsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // Handle Android back button press
    BackHandler {
        activity?.finish()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VoiceOS Settings") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "VoiceOS Settings",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Voice Operating System Control Center",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // System Status
            SystemStatusCard()

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Categories
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(getSettingsCategories()) { category ->
                    SettingsCategoryCard(
                        category = category,
                        onClick = { handleCategoryClick(context, category) }
                    )
                }
            }
        }
    }
}

@Composable
fun SystemStatusCard() {
    var systemStatus by remember { mutableStateOf("Checking...") }
    var isOnline by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Check system status - TODO: Implement VoiceOSCore
        // val core = VoiceOSCore.getInstance()
        // isOnline = core != null
        // systemStatus = when {
        //     core == null -> "VoiceOS Core: Offline"
        //     core.systemState.value.name == "READY" -> "System: Ready"
        //     else -> "System: ${core.systemState.value.name}"
        // }
        // For now, set default status
        isOnline = true
        systemStatus = "System: Ready"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isOnline) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = "System Status",
                tint = if (isOnline) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                systemStatus,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun SettingsCategoryCard(
    category: SettingsCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                category.icon,
                contentDescription = category.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun handleCategoryClick(context: android.content.Context, category: SettingsCategory) {
    val intent = when (category.id) {
        "onboarding" -> Intent(context, OnboardingActivity::class.java)
        "accessibility" -> Intent(context, AccessibilitySetupActivity::class.java)
        "modules" -> Intent(context, ModuleConfigActivity::class.java)
        "voice_training" -> Intent(context, VoiceTrainingActivity::class.java)
        "diagnostics" -> Intent(context, DiagnosticsActivity::class.java)
        "help" -> Intent(context, HelpActivity::class.java)
        else -> null
    }
    intent?.let { context.startActivity(it) }
}

private fun getSettingsCategories(): List<SettingsCategory> = listOf(
    SettingsCategory(
        id = "onboarding",
        title = "Setup Wizard",
        description = "Complete VoiceOS setup and configuration",
        icon = Icons.Default.PlayArrow
    ),
    SettingsCategory(
        id = "accessibility",
        title = "Accessibility Service",
        description = "Configure voice commands and accessibility features",
        icon = Icons.Default.Accessibility
    ),
    SettingsCategory(
        id = "modules",
        title = "Module Configuration",
        description = "Configure speech recognition, device management, and more",
        icon = Icons.Default.Extension
    ),
    SettingsCategory(
        id = "voice_training",
        title = "Voice Training",
        description = "Train voice commands and improve recognition accuracy",
        icon = Icons.Default.RecordVoiceOver
    ),
    SettingsCategory(
        id = "diagnostics",
        title = "System Diagnostics",
        description = "Performance monitoring, logs, and troubleshooting",
        icon = Icons.Default.Analytics
    ),
    SettingsCategory(
        id = "help",
        title = "Help & Instructions",
        description = "User guides, tutorials, and support resources",
        icon = Icons.AutoMirrored.Default.Help
    )
)

data class SettingsCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)