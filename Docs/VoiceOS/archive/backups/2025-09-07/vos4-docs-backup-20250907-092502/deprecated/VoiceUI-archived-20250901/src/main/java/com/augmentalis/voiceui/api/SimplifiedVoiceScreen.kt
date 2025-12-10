/**
 * SimplifiedVoiceScreen.kt - Screen-level components for VoiceUI
 * 
 * Provides screen containers and common UI components with voice support
 */

package com.augmentalis.voiceui.api

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.augmentalis.uuidmanager.UUIDManager

/**
 * Voice-enabled screen container - supports both direct content and DSL style
 */
@Composable
fun VoiceScreen(
    name: String,
    locale: String? = null,
    aiContext: AIContext? = null,
    content: @Composable VoiceScreenScope.() -> Unit
) {
    val uuid = remember { UUIDManager.generate() }
    
    // Register screen context
    LaunchedEffect(name, aiContext) {
        aiContext?.let {
            AIContextManager.setContext(uuid, it)
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Support both DSL scope and direct content
            val scope = VoiceScreenScope()
            scope.content()
        }
    }
}

/**
 * Voice-enabled text component
 */
@Composable
fun text(
    text: String,
    locale: String? = null,
    aiContext: AIContext? = null
) {
    val uuid = remember { UUIDManager.generate() }
    
    LaunchedEffect(text, aiContext) {
        aiContext?.let {
            AIContextManager.setContext(uuid, it)
        }
    }
    
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * Voice-enabled input field
 */
@Composable
fun input(
    label: String,
    value: String = "",
    locale: String? = null,
    aiContext: AIContext? = null,
    onValueChange: ((String) -> Unit)? = null
) {
    val uuid = remember { UUIDManager.generate() }
    var textValue by remember { mutableStateOf(value) }
    
    LaunchedEffect(label, aiContext) {
        aiContext?.let {
            AIContextManager.setContext(uuid, it)
        }
    }
    
    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            textValue = newValue
            onValueChange?.invoke(newValue)
        },
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        singleLine = true
    )
}

/**
 * Voice-enabled password field
 */
@Composable
fun password(
    label: String = "Password",
    value: String = "",
    locale: String? = null,
    aiContext: AIContext? = null,
    onValueChange: ((String) -> Unit)? = null
) {
    val uuid = remember { UUIDManager.generate() }
    var passwordValue by remember { mutableStateOf(value) }
    
    LaunchedEffect(label, aiContext) {
        aiContext?.let {
            AIContextManager.setContext(uuid, it)
        }
    }
    
    OutlinedTextField(
        value = passwordValue,
        onValueChange = { newValue ->
            passwordValue = newValue
            onValueChange?.invoke(newValue)
        },
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        singleLine = true
    )
}

/**
 * Voice-enabled button
 */
@Composable
fun button(
    text: String,
    locale: String? = null,
    aiContext: AIContext? = null,
    onClick: (() -> Unit)? = null
) {
    val uuid = remember { UUIDManager.generate() }
    
    LaunchedEffect(text, aiContext) {
        aiContext?.let {
            AIContextManager.setContext(uuid, it)
        }
    }
    
    Button(
        onClick = { onClick?.invoke() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text)
    }
}

/**
 * Voice-enabled card component
 */
@Composable
fun card(
    title: String? = null,
    locale: String? = null,
    aiContext: AIContext? = null,
    content: @Composable () -> Unit
) {
    val uuid = remember { UUIDManager.generate() }
    
    LaunchedEffect(title, aiContext) {
        aiContext?.let {
            AIContextManager.setContext(uuid, it)
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            content()
        }
    }
}

/**
 * Voice-enabled section divider
 */
@Composable
fun section(
    title: String,
    locale: String? = null,
    aiContext: AIContext? = null,
    content: @Composable () -> Unit
) {
    val uuid = remember { UUIDManager.generate() }
    
    LaunchedEffect(title, aiContext) {
        aiContext?.let {
            AIContextManager.setContext(uuid, it)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}

/**
 * Voice-enabled spacer
 */
@Composable
fun spacer(
    height: Int = 16
) {
    Spacer(modifier = Modifier.height(height.dp))
}

/**
 * Voice-enabled row layout
 */
@Composable
fun row(
    locale: String? = null,
    aiContext: AIContext? = null,
    content: @Composable RowScope.() -> Unit
) {
    val uuid = remember { UUIDManager.generate() }
    
    LaunchedEffect(aiContext) {
        aiContext?.let {
            AIContextManager.setContext(uuid, it)
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

/**
 * Voice-enabled column layout
 */
@Composable
fun column(
    locale: String? = null,
    aiContext: AIContext? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val uuid = remember { UUIDManager.generate() }
    
    LaunchedEffect(aiContext) {
        aiContext?.let {
            AIContextManager.setContext(uuid, it)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        content()
    }
}
