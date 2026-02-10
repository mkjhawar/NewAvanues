package com.augmentalis.webavanue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.augmentalis.avanueui.theme.AvanueTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * XR Permission Explanation Dialog - Shows user-friendly explanations before requesting permissions.
 *
 * REQ-XR-002: Camera Permission Management
 * Improves permission grant rate by explaining why permissions are needed.
 */
@Composable
fun XRPermissionDialog(
    sessionMode: String, // "immersive-ar" or "immersive-vr"
    permissionType: String, // "camera", "sensors", "all"
    onGrant: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDeny) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            color = AvanueTheme.colors.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Icon(
                    imageVector = when (permissionType) {
                        "camera" -> Icons.Default.Info
                        "sensors" -> Icons.Default.Info
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when (sessionMode) {
                        "immersive-ar" -> Color(0xFF4CAF50) // Green for AR
                        else -> Color(0xFF2196F3) // Blue for VR
                    },
                    modifier = Modifier.size(64.dp)
                )

                // Title
                Text(
                    text = getDialogTitle(sessionMode, permissionType),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Explanation
                Text(
                    text = getDialogExplanation(sessionMode, permissionType),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = AvanueTheme.colors.textSecondary
                )

                // Features list
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    getFeaturesList(sessionMode, permissionType).forEach { feature ->
                        FeatureItem(feature)
                    }
                }

                // Privacy note
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AvanueTheme.colors.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Privacy",
                            tint = AvanueTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        Text(
                            text = "Your privacy is protected. Data stays on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AvanueTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDeny,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Not Now")
                    }

                    Button(
                        onClick = onGrant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Allow")
                    }
                }
            }
        }
    }
}

/**
 * XR Permission Denied Dialog - Shows when permission is permanently denied.
 */
@Composable
fun XRPermissionDeniedDialog(
    permissionType: String,
    isPermanent: Boolean,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            color = AvanueTheme.colors.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning icon
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AvanueTheme.colors.error,
                    modifier = Modifier.size(64.dp)
                )

                // Title
                Text(
                    text = if (isPermanent) "Permission Required" else "Permission Denied",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Message
                Text(
                    text = if (isPermanent) {
                        "WebXR features require ${permissionType} permission. " +
                        "Please enable it in Settings to use AR/VR experiences."
                    } else {
                        "${permissionType.capitalize()} permission is required for WebXR. " +
                        "Please grant permission to continue."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = AvanueTheme.colors.textSecondary
                )

                // Steps to enable (if permanent)
                if (isPermanent) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "How to enable:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        listOf(
                            "1. Tap 'Open Settings' below",
                            "2. Select 'Permissions'",
                            "3. Enable '${permissionType.capitalize()}'",
                            "4. Return to WebAvanue"
                        ).forEach { step ->
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = AvanueTheme.colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons
                if (isPermanent) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Settings")
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Maybe Later")
                        }
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

// Helper functions

private fun getDialogTitle(sessionMode: String, permissionType: String): String {
    return when (permissionType) {
        "camera" -> when (sessionMode) {
            "immersive-ar" -> "Enable AR Camera"
            else -> "Enable VR Camera"
        }
        "sensors" -> "Enable Motion Sensors"
        else -> "Enable WebXR Permissions"
    }
}

private fun getDialogExplanation(sessionMode: String, permissionType: String): String {
    return when (permissionType) {
        "camera" -> when (sessionMode) {
            "immersive-ar" -> "Camera access is required to display augmented reality content. " +
                    "This allows websites to overlay virtual objects on your real-world view."
            else -> "This VR experience requires camera access for enhanced tracking features."
        }
        "sensors" -> "Motion sensor access is required for head tracking and orientation in VR/AR experiences."
        else -> "WebXR requires camera and sensor access to provide immersive AR/VR experiences."
    }
}

private fun getFeaturesList(sessionMode: String, permissionType: String): List<String> {
    return when (permissionType) {
        "camera" -> when (sessionMode) {
            "immersive-ar" -> listOf(
                "See virtual objects in your real environment",
                "Place and interact with 3D models",
                "Experience immersive AR games and apps",
                "Camera only used during AR sessions"
            )
            else -> listOf(
                "Enhanced VR tracking",
                "Improved immersion",
                "Camera only used during VR sessions"
            )
        }
        "sensors" -> listOf(
            "Natural head movement tracking",
            "Smooth 360° viewing",
            "Realistic orientation sensing",
            "No data leaves your device"
        )
        else -> listOf(
            "Full AR/VR capabilities",
            "Immersive web experiences",
            "Secure and private",
            "Control permissions anytime"
        )
    }
}
