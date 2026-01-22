package com.augmentalis.webavanue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Security Dialogs for WebAvanue Browser
 *
 * Material Design 3 dialogs for security-critical user decisions:
 * 1. SSL Error Dialog - Invalid certificate warnings
 * 2. Permission Request Dialog - Camera/mic/location requests
 * 3. JavaScript Alert Dialog - `window.alert()`
 * 4. JavaScript Confirm Dialog - `window.confirm()`
 * 5. JavaScript Prompt Dialog - `window.prompt()`
 *
 * All dialogs follow Material Design 3 guidelines and accessibility standards.
 *
 * @see SecurityState for state management
 * @see CertificateUtils for certificate parsing
 */

/**
 * SSL Error Dialog - Warns user about invalid certificates
 *
 * Displays:
 * - Error type (expired, untrusted CA, hostname mismatch, etc.)
 * - Certificate details (issuer, validity period, fingerprint)
 * - Clear warning about security risks
 * - "Go Back" (primary) and "Proceed Anyway" (destructive) actions
 *
 * Design follows Chrome's SSL error page patterns.
 *
 * @param sslErrorInfo Error details from WebView
 * @param onGoBack User chose to navigate away (recommended)
 * @param onProceedAnyway User chose to proceed despite warning (dangerous)
 * @param onDismiss Dialog dismissed (treat as Go Back)
 */
@Composable
fun SslErrorDialog(
    sslErrorInfo: SslErrorInfo,
    onGoBack: () -> Unit,
    onProceedAnyway: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // TODO: Add icon when Material Icons Extended is available
        // icon = Info icon for SSL warnings
        title = {
            Text(
                text = "Your connection is not private",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary error message
                Text(
                    text = sslErrorInfo.primaryError,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )

                // Recommended action
                Text(
                    text = sslErrorInfo.errorType.getRecommendedAction(),
                    style = MaterialTheme.typography.bodyMedium
                )

                // URL
                Text(
                    text = "URL: ${sslErrorInfo.url}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Additional errors (if any)
                if (sslErrorInfo.additionalErrors.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Additional Issues:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    sslErrorInfo.additionalErrors.forEach { error ->
                        Text(
                            text = "• $error",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Certificate details (if available)
                sslErrorInfo.certificateInfo?.let { certInfo ->
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Certificate Details:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Issued to: ${certInfo.subject}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Issued by: ${certInfo.issuer}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Fingerprint: ${certInfo.fingerprint}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Warning callout
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "⚠️ Attackers might be trying to steal your information (for example, passwords, messages, or credit cards).",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onGoBack,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Go Back")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onProceedAnyway,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Proceed Anyway")
            }
        }
    )
}

/**
 * Permission Request Dialog - Request camera/mic/location permissions
 *
 * Displays:
 * - Website domain requesting permission
 * - Requested permissions (camera, microphone, location, etc.)
 * - Clear explanation of what permission allows
 * - "Allow" and "Deny" actions
 * - Optional "Remember my choice" checkbox
 *
 * @param permissionRequest Permission request details
 * @param onAllow User granted permission
 * @param onDeny User denied permission
 * @param onDismiss Dialog dismissed (treat as deny)
 */
@Composable
fun PermissionRequestDialog(
    permissionRequest: PermissionRequest,
    onAllow: (remember: Boolean) -> Unit,
    onDeny: () -> Unit,
    onDismiss: () -> Unit
) {
    var rememberChoice by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        // TODO: Add icon when Material Icons Extended is available
        // icon = Settings/Permission icon
        title = {
            Text(
                text = "Permission Request",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Domain
                Text(
                    text = permissionRequest.domain,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Permission request message
                Text(
                    text = buildString {
                        append("This site wants to use your:\n")
                        permissionRequest.permissions.forEach { permission ->
                            append("• ${permission.getUserFriendlyName()}\n")
                        }
                    }.trimEnd(),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Explanation
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = getPermissionExplanation(permissionRequest.permissions),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Remember choice checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it }
                    )
                    Text(
                        text = "Remember my choice for this site",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAllow(rememberChoice) }
            ) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text("Deny")
            }
        }
    )
}

/**
 * JavaScript Alert Dialog - `window.alert()`
 *
 * @param domain Website domain
 * @param message Alert message from JavaScript
 * @param onDismiss User clicked OK or dismissed
 */
@Composable
fun JavaScriptAlertDialog(
    domain: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = domain,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

/**
 * JavaScript Confirm Dialog - `window.confirm()`
 *
 * @param domain Website domain
 * @param message Confirmation question from JavaScript
 * @param onConfirm User clicked OK (returns true to JavaScript)
 * @param onCancel User clicked Cancel (returns false to JavaScript)
 * @param onDismiss Dialog dismissed (treat as cancel)
 */
@Composable
fun JavaScriptConfirmDialog(
    domain: String,
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = domain,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

/**
 * JavaScript Prompt Dialog - `window.prompt()`
 *
 * @param domain Website domain
 * @param message Prompt message from JavaScript
 * @param defaultValue Default input value (optional)
 * @param onConfirm User clicked OK with input (returns input to JavaScript)
 * @param onCancel User clicked Cancel (returns null to JavaScript)
 * @param onDismiss Dialog dismissed (treat as cancel)
 */
@Composable
fun JavaScriptPromptDialog(
    domain: String,
    message: String,
    defaultValue: String = "",
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(defaultValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = domain,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Enter value") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(input) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

/**
 * HTTP Authentication Dialog - HTTP Basic/Digest authentication
 *
 * Displays when server requires username/password authentication.
 * Supports HTTP Basic and Digest authentication schemes.
 *
 * @param authRequest Authentication request details (host, realm, scheme)
 * @param onAuthenticate User provided credentials (username, password)
 * @param onCancel User cancelled authentication
 * @param onDismiss Dialog dismissed (treat as cancel)
 */
@Composable
fun HttpAuthenticationDialog(
    authRequest: HttpAuthRequest,
    onAuthenticate: (HttpAuthCredentials) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Authentication Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Host and realm info
                Text(
                    text = authRequest.host,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "The server ${authRequest.host} requires a username and password.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (authRequest.realm.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Realm: ${authRequest.realm}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Username field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                    )
                )

                // Scheme info
                if (authRequest.scheme != "Basic") {
                    Text(
                        text = "Authentication scheme: ${authRequest.scheme}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAuthenticate(
                        HttpAuthCredentials(
                            username = username,
                            password = password
                        )
                    )
                },
                enabled = username.isNotBlank() && password.isNotBlank()
            ) {
                Text("Sign In")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Get explanation text for requested permissions
 */
private fun getPermissionExplanation(permissions: List<PermissionType>): String {
    return when {
        permissions.contains(PermissionType.CAMERA) && permissions.contains(PermissionType.MICROPHONE) ->
            "This will allow the site to access your camera and microphone for video calls or recording."
        permissions.contains(PermissionType.CAMERA) ->
            "This will allow the site to access your camera for taking photos or recording video."
        permissions.contains(PermissionType.MICROPHONE) ->
            "This will allow the site to access your microphone for recording audio."
        permissions.contains(PermissionType.LOCATION) ->
            "This will allow the site to access your current location."
        permissions.contains(PermissionType.PROTECTED_MEDIA) ->
            "This will allow the site to play protected media content (DRM)."
        else -> "This will grant the requested permissions to the website."
    }
}
