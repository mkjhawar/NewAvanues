/**
 * MainActivity.kt - VOS4 Main Entry Point
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-22
 */

package com.augmentalis.voiceos.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceos.ui.activities.SettingsActivity
import com.augmentalis.voiceoscore.permissions.PermissionManager
import com.augmentalis.voiceoscore.permissions.PermissionRequestActivity

/**
 * Main Activity - VOS4 Entry Point
 * Direct access to VoiceOS modules
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // VoiceOS modules are initialized by the Application class
        // No need for CoreManager initialization

        // Request storage permissions if needed (for speech recognition models)
        requestStoragePermissionsIfNeeded()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    /**
     * Request storage permissions on first launch
     * Allows speech recognition models to be accessed from external storage
     */
    private fun requestStoragePermissionsIfNeeded() {
        val permissionManager = PermissionManager(this)

        // Skip if permission already granted
        if (permissionManager.hasStoragePermission()) {
            return
        }

        // FIX: Always show permission request activity, even if "don't ask again" was previously set
        // The PermissionRequestActivity will handle the "don't ask again" case by directing
        // user to Settings if needed
        val intent = Intent(this, PermissionRequestActivity::class.java)
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // VoiceOS Logo/Icon
        Icon(
            Icons.Default.RecordVoiceOver,
            contentDescription = "VoiceOS",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "VoiceOS",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center
        )
        
        Text(
            "Voice Operating System 4.0",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Control your Android device with natural voice commands",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Main Action Buttons
        Button(
            onClick = {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Open Settings",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = {
                // Quick setup - could launch onboarding directly
                context.startActivity(Intent(context, SettingsActivity::class.java).apply {
                    putExtra("open_onboarding", true)
                })
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Quick Setup",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Status indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Status",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "VoiceOS Ready",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
