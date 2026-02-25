// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/service/OverlayPermissionActivity.kt
// created: 2025-11-01 23:20:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 1 - Core Infrastructure
// agent: Engineer | mode: ACT

package com.augmentalis.overlay.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.AvanueThemeProvider
import com.augmentalis.avanueui.theme.HydraColors
import com.augmentalis.avanueui.theme.HydraGlass
import com.augmentalis.avanueui.theme.HydraWater
import com.augmentalis.avanueui.theme.MaterialMode

/**
 * Permission request activity for overlay and microphone access.
 *
 * Handles permission flow for:
 * 1. SYSTEM_ALERT_WINDOW (overlay permission)
 * 2. RECORD_AUDIO (microphone permission)
 *
 * Shows Material3 UI with explanations and request buttons.
 * Automatically starts OverlayService once permissions are granted.
 *
 * @author Manoj Jhawar
 */
class OverlayPermissionActivity : ComponentActivity() {

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasMicrophonePermission by mutableStateOf(false)

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicrophonePermission = granted
        checkAndStartService()
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(this)
        checkAndStartService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check initial permission status
        hasOverlayPermission = Settings.canDrawOverlays(this)
        hasMicrophonePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        // If already have permissions, start service immediately
        if (hasOverlayPermission && hasMicrophonePermission) {
            startOverlayService()
            finish()
            return
        }

        setContent {
            AvanueThemeProvider(
                colors = HydraColors,
                glass = HydraGlass,
                water = HydraWater,
                materialMode = MaterialMode.Water,
                isDark = true
            ) {
                PermissionRequestScreen(
                    hasOverlayPermission = hasOverlayPermission,
                    hasMicrophonePermission = hasMicrophonePermission,
                    onRequestOverlay = ::requestOverlayPermission,
                    onRequestMicrophone = ::requestMicrophonePermission,
                    onCancel = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check overlay permission when returning from Settings
        hasOverlayPermission = Settings.canDrawOverlays(this)
        checkAndStartService()
    }

    /**
     * Request overlay permission (opens Settings)
     */
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    /**
     * Request microphone permission (runtime permission)
     */
    private fun requestMicrophonePermission() {
        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Check if all permissions granted and start service
     */
    private fun checkAndStartService() {
        if (hasOverlayPermission && hasMicrophonePermission) {
            startOverlayService()
            finish()
        }
    }

    /**
     * Start the overlay service
     */
    private fun startOverlayService() {
        OverlayService.start(this)
    }

    companion object {
        /**
         * Check if all required permissions are granted
         */
        fun hasPermissions(context: Context): Boolean {
            val hasOverlay = Settings.canDrawOverlays(context)
            val hasMicrophone = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            return hasOverlay && hasMicrophone
        }

        /**
         * Launch permission activity
         */
        fun launch(context: Context) {
            val intent = Intent(context, OverlayPermissionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}

/**
 * Permission request UI screen
 */
@Composable
private fun PermissionRequestScreen(
    hasOverlayPermission: Boolean,
    hasMicrophonePermission: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestMicrophone: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AvanueTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "AVA Overlay Permissions",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "AVA needs the following permissions to provide voice assistance over any app:",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = AvanueTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Overlay permission card
            PermissionCard(
                title = "Display Over Other Apps",
                description = "Allows AVA to show the voice orb and assistant panel while you use other apps.",
                isGranted = hasOverlayPermission,
                onRequest = onRequestOverlay
            )

            // Microphone permission card
            PermissionCard(
                title = "Microphone Access",
                description = "Enables voice recognition so you can speak to AVA.",
                isGranted = hasMicrophonePermission,
                onRequest = onRequestMicrophone
            )

            Spacer(modifier = Modifier.weight(1f))

            // Cancel button
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

/**
 * Individual permission card
 */
@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                AvanueTheme.colors.surfaceVariant
            } else {
                AvanueTheme.colors.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                if (isGranted) {
                    Text(
                        text = "✓ Granted",
                        style = MaterialTheme.typography.labelMedium,
                        color = AvanueTheme.colors.primary
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = AvanueTheme.colors.textSecondary
            )

            if (!isGranted) {
                Button(
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}
