/**
 * HelpActivity.kt - Help System & User Guides
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-22
 */

package com.augmentalis.voiceos.ui.activities

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
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Help Activity - User guides, tutorials, and support
 */
class HelpActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HelpScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val helpSections = getHelpSections()

    // Handle Android back button press
    BackHandler {
        activity?.finish()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support") },
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
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.AutoMirrored.Default.Help,
                    contentDescription = "Help",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "VoiceOS Help Center",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Guides, tutorials, and support resources",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Help Sections
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(helpSections) { section ->
                HelpSectionCard(section = section)
            }
        }
        }
    }
}

@Composable
fun HelpSectionCard(section: HelpSection) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    section.icon,
                    contentDescription = section.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        section.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                section.content.forEach { item ->
                    Text(
                        item,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun getHelpSections(): List<HelpSection> = listOf(
    HelpSection(
        title = "Getting Started",
        description = "Basic setup and first steps",
        icon = Icons.Default.PlayArrow,
        content = listOf(
            "1. Enable VoiceOS Accessibility Service in Android Settings",
            "2. Grant microphone permission for voice recognition",
            "3. Complete the onboarding wizard",
            "4. Test basic commands like 'go back' and 'go home'",
            "5. Practice voice commands in the training section"
        )
    ),
    HelpSection(
        title = "Voice Commands",
        description = "Complete command reference",
        icon = Icons.Default.RecordVoiceOver,
        content = listOf(
            "Navigation: 'back', 'home', 'recent apps', 'notifications'",
            "Volume: 'volume up', 'volume down', 'mute'",
            "Interaction: 'click [text]', 'scroll up', 'scroll down'",
            "System: 'open settings', 'power menu'",
            "Audio: 'speaker on', 'speaker off'"
        )
    ),
    HelpSection(
        title = "Troubleshooting",
        description = "Common issues and solutions",
        icon = Icons.Default.Build,
        content = listOf(
            "Commands not working? Check accessibility service is enabled",
            "Voice not recognized? Try voice training in settings",
            "App not responding? Check system diagnostics",
            "Permissions denied? Grant microphone access",
            "Service stopped? Restart from accessibility settings"
        )
    ),
    HelpSection(
        title = "Advanced Features",
        description = "Smart glasses and XR support",
        icon = Icons.Default.Visibility,
        content = listOf(
            "Smart glasses integration for hands-free control",
            "Android XR spatial computing support",
            "Multi-language voice commands (42+ languages)",
            "Custom command creation and training",
            "Performance optimization settings"
        )
    ),
    HelpSection(
        title = "Privacy & Security",
        description = "Data protection and security",
        icon = Icons.Default.Security,
        content = listOf(
            "Voice processing happens locally on device",
            "No voice data sent to external servers",
            "Accessibility service only accesses UI elements",
            "All data encrypted using Android keystore",
            "Optional analytics can be disabled in settings"
        )
    ),
    HelpSection(
        title = "Support",
        description = "Get help and report issues",
        icon = Icons.AutoMirrored.Default.ContactSupport,
        content = listOf(
            "Visit our website for latest documentation",
            "Report bugs through the diagnostics screen",
            "Check community forums for tips and tricks",
            "Contact support for enterprise customers",
            "Regular updates improve recognition accuracy"
        )
    )
)

data class HelpSection(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val content: List<String>
)