/**
 * LicenseManagerActivity.kt - Main UI for License Management
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Provides comprehensive license management interface with glassmorphism design
 * 
 * UI Layout:
 * ┌─────────────────────────────────────────┐
 * │ ╔═══════════════════════════════════════╗ │
 * │ ║           LICENSE MANAGER             ║ │
 * │ ╚═══════════════════════════════════════╝ │
 * │                                         │
 * │ ┌─────────────────────────────────────┐ │
 * │ │        LICENSE STATUS CARD          │ │
 * │ │ [●] Status: Active/Trial/Free       │ │
 * │ │     Type: Premium/Enterprise        │ │
 * │ │     Expires: 2025-12-31             │ │
 * │ └─────────────────────────────────────┘ │
 * │                                         │
 * │ ┌─────────────────────────────────────┐ │
 * │ │        TRIAL STATUS CARD            │ │
 * │ │     Days Remaining: 15/30           │ │
 * │ │ [████████░░] 50% Complete           │ │
 * │ └─────────────────────────────────────┘ │
 * │                                         │
 * │ ┌─────────────────────────────────────┐ │
 * │ │         ACTION BUTTONS              │ │
 * │ │ [Start Trial]  [Activate License]   │ │
 * │ │ [Purchase Pro] [Contact Support]    │ │
 * │ └─────────────────────────────────────┘ │
 * │                                         │
 * │ ┌─────────────────────────────────────┐ │
 * │ │        LICENSE VALIDATION           │ │
 * │ │ Last Check: 2 hours ago             │ │
 * │ │ [Refresh Now]                       │ │
 * │ └─────────────────────────────────────┘ │
 * └─────────────────────────────────────────┘
 */
package com.augmentalis.licensemanager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import com.augmentalis.licensemanager.LicensingModule
import com.augmentalis.licensemanager.SubscriptionState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main License Manager Activity
 */
class LicenseManagerActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            LicenseManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF000000) // Dark background for glassmorphism
                ) {
                    LicenseManagerScreen()
                }
            }
        }
    }
}

/**
 * License Manager Theme
 */
@Composable
fun LicenseManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6200EA),
            onPrimary = Color.White,
            surface = Color(0xFF121212),
            onSurface = Color.White
        ),
        content = content
    )
}

/**
 * Main License Manager Screen
 */
@Composable
fun LicenseManagerScreen(
    viewModel: LicenseViewModel = viewModel(
        factory = LicenseViewModelFactory(LocalContext.current)
    )
) {
    val subscriptionState by viewModel.subscriptionState.observeAsState(SubscriptionState())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadLicenseState()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        HeaderSection()
        
        // Error message
        errorMessage?.let { error ->
            ErrorCard(
                message = error,
                onDismiss = { viewModel.clearError() }
            )
        }
        
        // License Status Card
        LicenseStatusCard(
            subscriptionState = subscriptionState,
            onRefresh = { viewModel.validateLicense() }
        )
        
        // Trial Status Card (if applicable)
        AnimatedVisibility(
            visible = subscriptionState.licenseType == LicensingModule.LICENSE_TRIAL,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            TrialStatusCard(subscriptionState = subscriptionState)
        }
        
        // Action Buttons
        ActionButtonsCard(
            subscriptionState = subscriptionState,
            isLoading = isLoading,
            onStartTrial = { viewModel.startTrial() },
            onActivateLicense = { licenseKey -> viewModel.activateLicense(licenseKey) },
            onPurchasePro = { viewModel.openPurchasePage() },
            onContactSupport = { viewModel.openSupportPage() }
        )
        
        // Validation Info
        ValidationInfoCard(
            subscriptionState = subscriptionState,
            onRefresh = { viewModel.validateLicense() }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Header Section
 */
@Composable
internal fun HeaderSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(
                config = LicenseGlassConfigs.Primary,
                depth = DepthLevel(0.8f)
            )*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "License Manager",
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF6200EA)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "License Manager",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Manage your VOS4 subscription and licensing",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Error Card
 */
@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(
                config = LicenseGlassConfigs.Error,
                depth = DepthLevel(0.7f)
            )*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = LicenseColors.StatusError,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * License Status Card
 */
@Composable
internal fun LicenseStatusCard(
    subscriptionState: SubscriptionState,
    onRefresh: () -> Unit
) {
    val statusColor = when (subscriptionState.licenseType) {
        LicensingModule.LICENSE_PREMIUM, LicensingModule.LICENSE_ENTERPRISE -> LicenseColors.StatusActive
        LicensingModule.LICENSE_TRIAL -> LicenseColors.StatusWarning
        else -> LicenseColors.StatusInfo
    }
    
    val statusText = when (subscriptionState.licenseType) {
        LicensingModule.LICENSE_PREMIUM -> "Premium Active"
        LicensingModule.LICENSE_ENTERPRISE -> "Enterprise Active"
        LicensingModule.LICENSE_TRIAL -> "Trial Active"
        else -> "Free Version"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(
                config = LicenseGlassConfigs.Success.copy(tintColor = statusColor),
                depth = DepthLevel(0.6f)
            )*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "License Status",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status Row
            StatusRow(
                icon = Icons.Default.Circle,
                label = "Status",
                value = statusText,
                valueColor = statusColor
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // License Type Row
            StatusRow(
                icon = Icons.Default.Star,
                label = "License Type",
                value = subscriptionState.licenseType.uppercase(),
                valueColor = Color.White
            )
            
            // Expiry Date (if applicable)
            subscriptionState.expiryDate?.let { expiry ->
                Spacer(modifier = Modifier.height(12.dp))
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                StatusRow(
                    icon = Icons.Default.Schedule,
                    label = "Expires",
                    value = dateFormat.format(Date(expiry)),
                    valueColor = Color.White
                )
            }
            
            // Trial End Date (if trial)
            if (subscriptionState.licenseType == LicensingModule.LICENSE_TRIAL && subscriptionState.trialEndDate > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                StatusRow(
                    icon = Icons.Default.Schedule,
                    label = "Trial Ends",
                    value = dateFormat.format(Date(subscriptionState.trialEndDate)),
                    valueColor = LicenseColors.StatusWarning
                )
            }
        }
    }
}

/**
 * Status Row Component
 */
@Composable
private fun StatusRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = valueColor
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Trial Status Card
 */
@Composable
internal fun TrialStatusCard(subscriptionState: SubscriptionState) {
    val trialDays = 30L // Total trial days
    val endDate = subscriptionState.trialEndDate
    
    val daysRemaining = if (endDate > System.currentTimeMillis()) {
        ((endDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
    } else 0
    
    val daysUsed = (trialDays - daysRemaining).toInt().coerceAtLeast(0)
    val progress = (daysUsed.toFloat() / trialDays.toFloat()).coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(
                config = LicenseGlassConfigs.Warning,
                depth = DepthLevel(0.6f)
            )*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Trial Status",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Days Remaining",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                
                Text(
                    text = "$daysRemaining/$trialDays",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (daysRemaining > 7) Color.White else LicenseColors.StatusError,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    daysRemaining > 7 -> LicenseColors.StatusActive
                    daysRemaining > 3 -> LicenseColors.StatusWarning
                    else -> LicenseColors.StatusError
                },
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${(progress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Action Buttons Card
 */
@Composable
internal fun ActionButtonsCard(
    subscriptionState: SubscriptionState,
    isLoading: Boolean,
    onStartTrial: () -> Unit,
    onActivateLicense: (String) -> Unit,
    onPurchasePro: () -> Unit,
    onContactSupport: () -> Unit
) {
    var showLicenseDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(
                config = LicenseGlassConfigs.Info,
                depth = DepthLevel(0.6f)
            )*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons based on license state
            when (subscriptionState.licenseType) {
                LicensingModule.LICENSE_FREE -> {
                    ActionButton(
                        text = "Start Free Trial",
                        icon = Icons.Default.PlayArrow,
                        color = LicenseColors.StatusActive,
                        isLoading = isLoading,
                        onClick = onStartTrial
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ActionButton(
                        text = "Activate License",
                        icon = Icons.Default.Key,
                        color = LicenseColors.LicensePremium,
                        isLoading = false,
                        onClick = { showLicenseDialog = true }
                    )
                }
                
                LicensingModule.LICENSE_TRIAL -> {
                    ActionButton(
                        text = "Upgrade to Premium",
                        icon = Icons.Default.Star,
                        color = LicenseColors.LicensePremium,
                        isLoading = false,
                        onClick = onPurchasePro
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ActionButton(
                        text = "Activate License",
                        icon = Icons.Default.Key,
                        color = LicenseColors.StatusInfo,
                        isLoading = false,
                        onClick = { showLicenseDialog = true }
                    )
                }
                
                else -> {
                    ActionButton(
                        text = "Purchase Additional Licenses",
                        icon = Icons.Default.ShoppingCart,
                        color = LicenseColors.StatusInfo,
                        isLoading = false,
                        onClick = onPurchasePro
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ActionButton(
                text = "Contact Support",
                icon = Icons.Default.Support,
                color = Color.White.copy(alpha = 0.3f),
                isLoading = false,
                onClick = onContactSupport
            )
        }
    }
    
    // License Activation Dialog
    if (showLicenseDialog) {
        LicenseActivationDialog(
            onDismiss = { showLicenseDialog = false },
            onActivate = { licenseKey ->
                onActivateLicense(licenseKey)
                showLicenseDialog = false
            }
        )
    }
}

/**
 * Action Button Component
 */
@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.2f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Validation Info Card
 */
@Composable
internal fun ValidationInfoCard(
    subscriptionState: SubscriptionState,
    onRefresh: () -> Unit
) {
    val lastValidation = if (subscriptionState.lastValidation > 0) {
        val timeDiff = System.currentTimeMillis() - subscriptionState.lastValidation
        val hours = timeDiff / (60 * 60 * 1000)
        when {
            hours < 1 -> "Just now"
            hours < 24 -> "$hours hours ago"
            else -> "${hours / 24} days ago"
        }
    } else "Never"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(
                config = LicenseGlassConfigs.Primary.copy(backgroundOpacity = 0.05f),
                depth = DepthLevel(0.4f)
            )*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "License Validation",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Last check: $lastValidation",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            OutlinedButton(
                onClick = onRefresh,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    Color.White.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh")
            }
        }
    }
}

/**
 * License Activation Dialog
 */
@Composable
internal fun LicenseActivationDialog(
    onDismiss: () -> Unit,
    onActivate: (String) -> Unit
) {
    var licenseKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Activate License",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter your license key to activate premium features:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = licenseKey,
                    onValueChange = { licenseKey = it },
                    label = { Text("License Key") },
                    placeholder = { Text("PREMIUM-XXXX-XXXX-XXXX") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = """Example formats:
• PREMIUM-XXXX-XXXX-XXXX
• ENTERPRISE-XXXX-XXXX-XXXX""",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (licenseKey.isNotBlank()) {
                        isLoading = true
                        onActivate(licenseKey.trim())
                    }
                },
                enabled = licenseKey.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Activate")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

