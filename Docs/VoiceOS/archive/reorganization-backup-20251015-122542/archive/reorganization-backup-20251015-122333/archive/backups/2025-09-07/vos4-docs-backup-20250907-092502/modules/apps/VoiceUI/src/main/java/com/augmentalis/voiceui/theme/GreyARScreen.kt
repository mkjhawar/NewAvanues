package com.augmentalis.voiceui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceui.dsl.*
import com.augmentalis.voiceui.api.*

/**
 * GreyAR Themed VoiceUI Screen
 * This recreates the exact look from the AR glasses interface
 */

/**
 * Complete example matching the "Build websites easier" card
 */
@Composable
fun GreyARWebsiteBuilderScreen() {
    GreyARTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent), // Transparent for AR overlay
            contentAlignment = Alignment.Center
        ) {
            GreyARCard(
                modifier = Modifier
                    .width(450.dp)
                    .wrapContentHeight(),
                title = "Build websites easier\nwith Beta Experiment",
                subtitle = "A CSS framework that helps streamline the design process, save time, and create more consistent user interfaces."
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Get started button
                GreyARButton(
                    text = "Get started",
                    onClick = { /* Action */ }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Footer links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GreyARLinkText(
                        text = "Download",
                        onClick = { /* Download action */ }
                    )
                    
                    GreyARFooterText(
                        text = " · ",
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    
                    GreyARLinkText(
                        text = "Privacy Policy",
                        onClick = { /* Privacy policy */ }
                    )
                    
                    GreyARFooterText(
                        text = " · ",
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    
                    GreyARLinkText(
                        text = "Legal Notice",
                        onClick = { /* Legal notice */ }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Copyright text
                GreyARFooterText(
                    text = "COPYRIGHT © 2024 JOHN ALEXANDER. ALL RIGHTS RESERVED.",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Login Screen with GreyAR Theme using VoiceUI Magic
 */
@Composable
fun GreyARLoginScreen(
    onLogin: (String, String) -> Unit = { _, _ -> },
    onForgotPassword: () -> Unit = {},
    onRegister: () -> Unit = {}
) {
    GreyARTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            GreyARCard(
                modifier = Modifier
                    .width(400.dp)
                    .wrapContentHeight(),
                title = "Welcome Back",
                subtitle = "Sign in to continue to your account"
            ) {
                // Using VoiceUI magic components with GreyAR styling
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var rememberMe by remember { mutableStateOf(false) }
                
                // Email field
                GreyARTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    placeholder = "Enter your email"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Password field
                GreyARTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    placeholder = "Enter your password"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Remember me checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GreyARColors.AccentBlue,
                            uncheckedColor = GreyARColors.Border,
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Remember me",
                        style = GreyARTypography.bodyMedium,
                        color = GreyARColors.TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Sign in button
                GreyARButton(
                    text = "Sign In",
                    onClick = { onLogin(email, password) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Forgot password link
                GreyARTextButton(
                    text = "Forgot Password?",
                    onClick = onForgotPassword,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                GreyARDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Register section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account?",
                        style = GreyARTypography.bodyMedium,
                        color = GreyARColors.TextTertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    GreyARTextButton(
                        text = "Sign Up",
                        onClick = onRegister
                    )
                }
            }
        }
    }
}

/**
 * VoiceUI Magic Screen with GreyAR Theme
 * This shows how to use the magic API with the theme
 */
@Composable
fun GreyARMagicScreen(
    description: String? = null,
    content: (@Composable MagicScope.() -> Unit)? = null
) {
    GreyARTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            // The MagicScreen content wrapped in GreyAR card
            GreyARCard(
                modifier = Modifier
                    .width(450.dp)
                    .wrapContentHeight()
            ) {
                MagicScreen(
                    description = description,
                    content = content
                )
            }
        }
    }
}

/**
 * Example: Creating the exact "Build websites easier" screen with VoiceUI magic
 */
@Composable
fun GreyARMagicWebsiteBuilder() {
    GreyARMagicScreen {
        // Using VoiceUI magic components
        text("Build websites easier")
        text("with Beta Experiment")
        
        spacer(16)
        
        text("A CSS framework that helps streamline the design process, save time, and create more consistent user interfaces.")
        
        spacer(24)
        
        button("Get started") {
            // Action
        }
        
        spacer(24)
        
        row {
            textButton("Download") { }
            text(" · ")
            textButton("Privacy Policy") { }
            text(" · ")
            textButton("Legal Notice") { }
        }
        
        spacer(8)
        
        text("COPYRIGHT © 2024 JOHN ALEXANDER. ALL RIGHTS RESERVED.")
    }
}

/**
 * Settings screen with GreyAR theme
 */
@Composable
fun GreyARSettingsScreen() {
    GreyARTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Multiple cards for different sections
            GreyARCard(
                title = "Display Settings",
                subtitle = "Customize your visual experience"
            ) {
                var brightness by remember { mutableStateOf(0.7f) }
                var darkMode by remember { mutableStateOf(true) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Dark Mode",
                        style = GreyARTypography.bodyLarge,
                        color = GreyARColors.TextPrimary
                    )
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { darkMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GreyARColors.AccentBlue,
                            uncheckedThumbColor = GreyARColors.TextTertiary,
                            uncheckedTrackColor = GreyARColors.CardBackgroundLight
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Brightness",
                    style = GreyARTypography.bodyLarge,
                    color = GreyARColors.TextPrimary
                )
                
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    colors = SliderDefaults.colors(
                        thumbColor = GreyARColors.AccentBlue,
                        activeTrackColor = GreyARColors.AccentBlue,
                        inactiveTrackColor = GreyARColors.CardBackgroundLight
                    )
                )
            }
            
            GreyARCard(
                title = "Notifications",
                subtitle = "Manage your alerts and sounds"
            ) {
                var pushEnabled by remember { mutableStateOf(true) }
                var soundEnabled by remember { mutableStateOf(false) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Push Notifications",
                        style = GreyARTypography.bodyLarge,
                        color = GreyARColors.TextPrimary
                    )
                    Switch(
                        checked = pushEnabled,
                        onCheckedChange = { pushEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GreyARColors.AccentBlue,
                            uncheckedThumbColor = GreyARColors.TextTertiary,
                            uncheckedTrackColor = GreyARColors.CardBackgroundLight
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sound Effects",
                        style = GreyARTypography.bodyLarge,
                        color = GreyARColors.TextPrimary
                    )
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GreyARColors.AccentBlue,
                            uncheckedThumbColor = GreyARColors.TextTertiary,
                            uncheckedTrackColor = GreyARColors.CardBackgroundLight
                        )
                    )
                }
            }
        }
    }
}

/**
 * Dashboard with three containers as requested
 */
@Composable
fun GreyARDashboardWithThreeContainers() {
    GreyARTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Container 1: Login Information
            GreyARCard(
                title = "Login Information",
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Username: john.doe@example.com", style = GreyARTypography.bodyLarge)
                Text("Last Login: 2 hours ago", style = GreyARTypography.bodyMedium)
                Text("Session Duration: 45 minutes", style = GreyARTypography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                GreyARButton(
                    text = "Logout",
                    onClick = { /* Logout action */ }
                )
            }
            
            // Container 2: System Information
            GreyARCard(
                title = "System Information",
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Device: Pixel 8 Pro", style = GreyARTypography.bodyLarge)
                Text("OS Version: Android 14", style = GreyARTypography.bodyMedium)
                Text("App Version: 1.0.0", style = GreyARTypography.bodyMedium)
                Text("Memory: 8GB RAM", style = GreyARTypography.bodyMedium)
                Text("Storage: 128GB (64GB Available)", style = GreyARTypography.bodyMedium)
            }
            
            // Container 3: Random Stuff
            GreyARCard(
                title = "Random Stuff",
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Daily Quote: 'Innovation distinguishes between a leader and a follower.'", 
                    style = GreyARTypography.bodyLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Weather: 72°F, Partly Cloudy", style = GreyARTypography.bodyMedium)
                Text("Time: ${java.time.LocalTime.now()}", style = GreyARTypography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GreyARButton(
                        text = "Action 1",
                        onClick = { },
                        modifier = Modifier.weight(1f)
                    )
                    GreyARButton(
                        text = "Action 2",
                        onClick = { },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}