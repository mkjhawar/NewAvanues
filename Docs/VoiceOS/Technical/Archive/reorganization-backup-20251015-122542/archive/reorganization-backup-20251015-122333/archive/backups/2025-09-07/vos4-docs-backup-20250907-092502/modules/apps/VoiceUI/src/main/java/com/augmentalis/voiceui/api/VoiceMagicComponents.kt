package com.augmentalis.voiceui.api

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp

/**
 * Voice Magic Components - Baseline Implementation
 * These are voice-enabled magic components for rapid prototyping
 */

@Composable
fun VoiceMagicEmail(label: String = "Email"): String {
    var value by remember { mutableStateOf("") }
    
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
    
    return value
}

@Composable
fun VoiceMagicPassword(label: String = "Password"): String {
    var value by remember { mutableStateOf("") }
    
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
    
    return value
}

@Composable
fun VoiceMagicButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}

@Composable
fun VoiceMagicCard(
    title: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            content()
        }
    }
}

@Composable
fun VoiceMagicLoginScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        VoiceMagicCard(title = "Login") {
            VoiceMagicEmail()
            Spacer(modifier = Modifier.height(8.dp))
            VoiceMagicPassword()
            Spacer(modifier = Modifier.height(16.dp))
            VoiceMagicButton("Sign In") {
                // Login action
            }
        }
    }
}