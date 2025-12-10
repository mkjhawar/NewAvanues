/**
 * PermissionRequestActivity.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/ui/PermissionRequestActivity.kt
 * 
 * Created: 2025-01-26 01:15 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Handle overlay and accessibility permission requests for VoiceCursor
 * Module: VoiceCursor System
 * 
 * Changelog:
 * - v1.0.0 (2025-01-26 01:15 PST): Initial creation with permission request handling
 */

package com.augmentalis.voiceos.cursor.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.augmentalis.voiceos.cursor.R
// import com.augmentalis.voiceuielements.themes.arvision.glassMorphism
// import com.augmentalis.voiceuielements.themes.arvision.GlassMorphismConfig
// import com.augmentalis.voiceuielements.themes.arvision.DepthLevel

// Import theme utils for validation
import com.augmentalis.licensemanager.ui.glassMorphism
import com.augmentalis.licensemanager.ui.GlassMorphismConfig
import com.augmentalis.licensemanager.ui.DepthLevel

/**
 * Permission request activity with ARVision dialog styling
 * Handles overlay and accessibility permission flows
 */
class PermissionRequestActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_PERMISSION_TYPE = "permission_type"
        const val PERMISSION_TYPE_OVERLAY = "overlay"
        const val PERMISSION_TYPE_ACCESSIBILITY = "accessibility"
        const val PERMISSION_TYPE_BOTH = "both"
    }
    
    // Activity result launcher for overlay permission
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                onOverlayPermissionGranted()
            } else {
                onOverlayPermissionDenied()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val permissionType = intent.getStringExtra(EXTRA_PERMISSION_TYPE) ?: PERMISSION_TYPE_BOTH
        
        setContent {
            PermissionRequestDialog(
                permissionType = permissionType,
                onOverlayRequest = { requestOverlayPermission() },
                onAccessibilityRequest = { requestAccessibilityPermission() },
                onDismiss = { finish() }
            )
        }
    }
    
    /**
     * Request overlay permission
     */
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                onOverlayPermissionGranted()
                return
            }
            
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    getString(R.string.error_permission_denied),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        } else {
            onOverlayPermissionGranted()
        }
    }
    
    /**
     * Request accessibility permission
     */
    private fun requestAccessibilityPermission() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            
            Toast.makeText(
                this,
                getString(R.string.permission_accessibility_message),
                Toast.LENGTH_LONG
            ).show()
            
            finish()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.error_permission_denied),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
    
    /**
     * Handle overlay permission granted
     */
    private fun onOverlayPermissionGranted() {
        setResult(Activity.RESULT_OK)
        
        val permissionType = intent.getStringExtra(EXTRA_PERMISSION_TYPE)
        if (permissionType == PERMISSION_TYPE_BOTH) {
            // Request accessibility permission next
            requestAccessibilityPermission()
        } else {
            finish()
        }
    }
    
    /**
     * Handle overlay permission denied
     */
    private fun onOverlayPermissionDenied() {
        Toast.makeText(
            this,
            getString(R.string.error_permission_denied),
            Toast.LENGTH_LONG
        ).show()
        
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}

/**
 * Permission request dialog with ARVision styling
 */
@Composable
private fun PermissionRequestDialog(
    permissionType: String,
    onOverlayRequest: () -> Unit,
    onAccessibilityRequest: () -> Unit,
    onDismiss: () -> Unit
) {
    @Suppress("UNUSED_VARIABLE")
    val context = LocalContext.current
    
    // Glass morphism configuration
    val glassMorphismConfig = remember {
        GlassMorphismConfig(
            cornerRadius = 24.dp,
            backgroundOpacity = 0.9f,
            borderOpacity = 0.8f,
            borderWidth = 1.dp,
            tintColor = Color(0xFF007AFF),
            tintOpacity = 0.05f
        )
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .glassMorphism(
                    config = glassMorphismConfig,
                    depth = DepthLevel(1.5f)
                )
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = when (permissionType) {
                        PermissionRequestActivity.PERMISSION_TYPE_OVERLAY -> 
                            stringResource(R.string.permission_overlay_title)
                        PermissionRequestActivity.PERMISSION_TYPE_ACCESSIBILITY -> 
                            stringResource(R.string.permission_accessibility_title)
                        else -> "VoiceCursor Permissions"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1D1D1F),
                    textAlign = TextAlign.Center
                )
                
                // Message
                Text(
                    text = when (permissionType) {
                        PermissionRequestActivity.PERMISSION_TYPE_OVERLAY -> 
                            stringResource(R.string.permission_overlay_message)
                        PermissionRequestActivity.PERMISSION_TYPE_ACCESSIBILITY -> 
                            stringResource(R.string.permission_accessibility_message)
                        else -> "VoiceCursor needs system permissions to display cursor overlay and perform touch gestures."
                    },
                    fontSize = 16.sp,
                    color = Color(0xFF48484A),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (permissionType) {
                        PermissionRequestActivity.PERMISSION_TYPE_OVERLAY -> {
                            PermissionButton(
                                text = "Grant Overlay Permission",
                                onClick = onOverlayRequest
                            )
                        }
                        PermissionRequestActivity.PERMISSION_TYPE_ACCESSIBILITY -> {
                            PermissionButton(
                                text = "Open Accessibility Settings",
                                onClick = onAccessibilityRequest
                            )
                        }
                        else -> {
                            PermissionButton(
                                text = "Grant Overlay Permission",
                                onClick = onOverlayRequest
                            )
                            
                            PermissionButton(
                                text = "Open Accessibility Settings",
                                onClick = onAccessibilityRequest,
                                isSecondary = true
                            )
                        }
                    }
                    
                    // Cancel button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF8E8E93),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * ARVision-styled permission button
 */
@Composable
private fun PermissionButton(
    text: String,
    onClick: () -> Unit,
    isSecondary: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSecondary) {
                Color(0x1A007AFF)
            } else {
                Color(0xFF007AFF)
            }
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSecondary) 0.dp else 2.dp
        )
    ) {
        Text(
            text = text,
            color = if (isSecondary) Color(0xFF007AFF) else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}