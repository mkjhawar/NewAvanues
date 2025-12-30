package com.augmentalis.webavanue.ui.screen.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * BasicAuthDialog - HTTP Basic Authentication dialog
 *
 * Features:
 * - Username and password fields
 * - "Remember credentials" checkbox
 * - Submit and Cancel buttons
 * - Dark 3D theme matching command bar
 *
 * @param visible Whether dialog is visible
 * @param url The URL requesting authentication
 * @param realm The authentication realm (optional)
 * @param onAuthenticate Callback with username and password
 * @param onCancel Callback when user cancels
 * @param modifier Modifier for customization
 */
@Composable
fun BasicAuthDialog(
    visible: Boolean,
    url: String,
    realm: String? = null,
    onAuthenticate: (username: String, password: String, remember: Boolean) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberCredentials by remember { mutableStateOf(false) }

    // Dark 3D theme colors
    val bgDialog = Color(0xFF0F3460).copy(alpha = 0.98f)
    val bgSurface = Color(0xFF16213E)
    val accentColor = Color(0xFF60A5FA)

    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = modifier.width(340.dp),
            shape = RoundedCornerShape(16.dp),
            color = bgDialog,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Authentication Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE8E8E8)
                )

                // Realm and URL info
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (realm != null) {
                        Text(
                            text = "Realm: $realm",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA0A0A0)
                        )
                    }
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0A0A0),
                        maxLines = 2
                    )
                }

                // Username field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = accentColor
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF2D4A6F),
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = Color(0xFFA0A0A0),
                        focusedTextColor = Color(0xFFE8E8E8),
                        unfocusedTextColor = Color(0xFFE8E8E8),
                        cursorColor = accentColor
                    )
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = accentColor
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF2D4A6F),
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = Color(0xFFA0A0A0),
                        focusedTextColor = Color(0xFFE8E8E8),
                        unfocusedTextColor = Color(0xFFE8E8E8),
                        cursorColor = accentColor
                    )
                )

                // Remember credentials checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = rememberCredentials,
                        onCheckedChange = { rememberCredentials = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColor,
                            uncheckedColor = Color(0xFF2D4A6F),
                            checkmarkColor = Color.White
                        )
                    )
                    Text(
                        text = "Remember credentials",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE8E8E8)
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    // Cancel button
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFA0A0A0)
                        )
                    ) {
                        Text("Cancel")
                    }

                    // Submit button
                    Button(
                        onClick = {
                            if (username.isNotBlank() && password.isNotBlank()) {
                                onAuthenticate(username, password, rememberCredentials)
                            }
                        },
                        enabled = username.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF2D4A6F),
                            disabledContentColor = Color(0xFF6C6C6C)
                        )
                    ) {
                        Text("Login")
                    }
                }
            }
        }
    }
}
