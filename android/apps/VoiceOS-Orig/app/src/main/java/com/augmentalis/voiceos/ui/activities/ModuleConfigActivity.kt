/**
 * ModuleConfigActivity.kt - Module Configuration & Management
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.augmentalis.voiceos.AccessibilitySetupHelper

/**
 * Module Configuration Activity - Manage all VOS4 modules
 */
class ModuleConfigActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ModuleConfigScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleConfigScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var modules by remember { mutableStateOf<List<ModuleInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        modules = getModuleInfo(context)
    }

    // Handle Android back button press
    BackHandler {
        activity?.finish()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Module Configuration") },
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
                    Icons.Default.Extension,
                    contentDescription = "Module Configuration",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Module Configuration",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Configure and manage VOS4 system modules",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Modules List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(modules) { module ->
                ModuleCard(module = module)
            }
        }
        }
    }
}

@Composable
fun ModuleCard(module: ModuleInfo) {
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
                    module.icon,
                    contentDescription = module.name,
                    tint = if (module.isActive) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        module.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        module.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (module.isActive) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = "Status",
                            tint = if (module.isActive) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (module.isActive) "Active" else "Inactive",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
                
                // Module details
                Text(
                    "Module Details",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                InfoRow("Version", module.version)
                InfoRow("Status", if (module.isActive) "Running" else "Stopped")
                InfoRow("Memory Impact", module.memoryImpact)
                
                if (module.capabilities.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Capabilities",
                        style = MaterialTheme.typography.titleSmall
                    )
                    module.capabilities.forEach { capability ->
                        Text(
                            "• $capability",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                // Module actions
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Restart module */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restart")
                    }
                    
                    if (module.hasSettings) {
                        Button(
                            onClick = { /* Open module settings */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Configure")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun getModuleInfo(context: android.content.Context): List<ModuleInfo> {
    val accessibilityHelper = AccessibilitySetupHelper(context)
    val isAccessibilityServiceEnabled = accessibilityHelper.isServiceEnabled()

    return listOf(
        ModuleInfo(
            id = "device_manager",
            name = "Device Manager",
            description = "Unified hardware control and management",
            version = "4.0.0",
            isActive = true,
            icon = Icons.Default.Devices,
            memoryImpact = "Low",
            capabilities = listOf(
                "Audio device control",
                "Display management",
                "Smart glasses integration",
                "XR/AR support"
            ),
            hasSettings = true
        ),
        ModuleInfo(
            id = "accessibility_service",
            name = "Accessibility Service",
            description = "Voice command execution and UI control",
            version = "4.0.0",
            isActive = isAccessibilityServiceEnabled,
            icon = Icons.Default.Accessibility,
            memoryImpact = "Medium",
            capabilities = listOf(
                "Voice command execution",
                "UI element interaction",
                "Navigation control",
                "System function access"
            ),
            hasSettings = true
        ),
    ModuleInfo(
        id = "localization",
        name = "Localization Manager",
        description = "Multi-language support and translations",
        version = "1.0.0",
        isActive = true,
        icon = Icons.Default.Language,
        memoryImpact = "Low",
        capabilities = listOf(
            "42+ language support",
            "Voice command translation",
            "UI localization",
            "Vivoka + Vosk engines"
        ),
        hasSettings = true
    ),
    ModuleInfo(
        id = "licensing",
        name = "License Manager", 
        description = "Subscription and license management",
        version = "1.0.0",
        isActive = true,
        icon = Icons.Default.Security,
        memoryImpact = "Low",
        capabilities = listOf(
            "License validation",
            "Trial management",
            "Premium features",
            "Subscription tracking"
        ),
        hasSettings = true
    ),
    ModuleInfo(
        id = "commands",
        name = "Commands Manager",
        description = "Voice command processing and routing",
        version = "4.0.0",
        isActive = true,
        icon = Icons.Default.RecordVoiceOver,
        memoryImpact = "Medium",
        capabilities = listOf(
            "70+ voice commands",
            "Direct execution",
            "Command validation",
            "Action registry"
        ),
        hasSettings = false
    )
)
}

data class ModuleInfo(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val isActive: Boolean,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val memoryImpact: String,
    val capabilities: List<String>,
    val hasSettings: Boolean
)