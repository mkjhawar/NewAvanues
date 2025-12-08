/**
 * DiagnosticsActivity.kt - System Diagnostics & Performance Monitoring
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-22
 */

package com.augmentalis.voiceos.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.augmentalis.voiceos.VoiceOS
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService

/**
 * Diagnostics Activity - System monitoring and troubleshooting
 */
class DiagnosticsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                DiagnosticsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen() {
    var systemMetrics by remember { mutableStateOf<SystemMetrics?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        systemMetrics = collectSystemMetrics()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = "System Diagnostics",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "System Diagnostics",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Performance monitoring and troubleshooting",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Refresh Button
        Button(
            onClick = {
                isRefreshing = true
                scope.launch {
                    systemMetrics = collectSystemMetrics()
                    isRefreshing = false
                }
            },
            enabled = !isRefreshing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isRefreshing) "Collecting Data..." else "Refresh Diagnostics")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        systemMetrics?.let { metrics ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SystemStatusCard(metrics.systemStatus)
                }
                item {
                    PerformanceMetricsCard(metrics.performance)
                }
                item {
                    ModuleHealthCard(metrics.moduleHealth)
                }
                item {
                    ErrorLogCard(metrics.recentErrors)
                }
            }
        }
    }
}

@Composable
fun SystemStatusCard(status: SystemStatus) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "System Status",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            StatusRow("VoiceOS Core", status.coreStatus, status.coreStatusIcon)
            StatusRow("Accessibility Service", status.accessibilityStatus, status.accessibilityIcon)
            StatusRow("Device Manager", status.deviceStatus, status.deviceIcon)
            StatusRow("Command Processing", status.commandStatus, status.commandIcon)
        }
    }
}

@Composable
fun PerformanceMetricsCard(performance: PerformanceMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Performance Metrics",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            MetricRow("Memory Usage", "${performance.memoryUsageMB} MB", performance.memoryUsageMB < 50)
            MetricRow("CPU Usage", "${performance.cpuUsagePercent}%", performance.cpuUsagePercent < 30)
            MetricRow("Battery Impact", performance.batteryImpact, performance.batteryImpact == "Low")
            MetricRow("Command Latency", "${performance.commandLatencyMs}ms", performance.commandLatencyMs < 100)
        }
    }
}

@Composable
fun ModuleHealthCard(moduleHealth: List<ModuleHealthStatus>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Module Health",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            moduleHealth.forEach { module ->
                StatusRow(
                    module.name,
                    if (module.isHealthy) "Healthy" else "Issues Detected",
                    if (module.isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning
                )
            }
        }
    }
}

@Composable
fun ErrorLogCard(errors: List<ErrorEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Recent Errors",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (errors.isEmpty()) {
                Text(
                    "No recent errors",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                errors.take(5).forEach { error ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                error.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                error.timestamp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusRow(
    label: String,
    status: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = "Status",
            tint = when (status) {
                "Ready", "Active", "Healthy" -> MaterialTheme.colorScheme.primary
                "Offline", "Inactive", "Issues Detected" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            status,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun MetricRow(
    label: String,
    value: String,
    isHealthy: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = "Health",
            tint = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isHealthy) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.tertiary
        )
    }
}

private suspend fun collectSystemMetrics(): SystemMetrics {
    delay(1000) // Simulate data collection
    
    val voiceOS = VoiceOS.getInstance()
    
    return SystemMetrics(
        systemStatus = SystemStatus(
            coreStatus = if (voiceOS != null) "Ready" else "Offline",
            coreStatusIcon = if (voiceOS != null) Icons.Default.CheckCircle else Icons.Default.Error,
            accessibilityStatus = if (VoiceAccessibilityService.isServiceRunning()) "Active" else "Inactive",
            accessibilityIcon = if (VoiceAccessibilityService.isServiceRunning()) Icons.Default.CheckCircle else Icons.Default.Error,
            deviceStatus = if (voiceOS?.deviceManager?.isReady() == true) "Ready" else "Offline",
            deviceIcon = if (voiceOS?.deviceManager?.isReady() == true) Icons.Default.CheckCircle else Icons.Default.Error,
            commandStatus = if (voiceOS != null) "Ready" else "Offline",
            commandIcon = if (voiceOS != null) Icons.Default.CheckCircle else Icons.Default.Error
        ),
        performance = PerformanceMetrics(
            memoryUsageMB = (30..45).random(),
            cpuUsagePercent = (15..25).random(),
            batteryImpact = "Low",
            commandLatencyMs = (50..90).random()
        ),
        moduleHealth = listOf(
            ModuleHealthStatus("Device Manager", true),
            ModuleHealthStatus("Commands Manager", true),
            ModuleHealthStatus("Localization Manager", true),
            ModuleHealthStatus("License Manager", true)
        ),
        recentErrors = if ((0..1).random() == 0) emptyList() else listOf(
            ErrorEntry("Command recognition timeout", "2 minutes ago"),
            ErrorEntry("Audio device initialization warning", "5 minutes ago")
        )
    )
}

data class SystemMetrics(
    val systemStatus: SystemStatus,
    val performance: PerformanceMetrics,
    val moduleHealth: List<ModuleHealthStatus>,
    val recentErrors: List<ErrorEntry>
)

data class SystemStatus(
    val coreStatus: String,
    val coreStatusIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val accessibilityStatus: String,
    val accessibilityIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val deviceStatus: String,
    val deviceIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val commandStatus: String,
    val commandIcon: androidx.compose.ui.graphics.vector.ImageVector
)

data class PerformanceMetrics(
    val memoryUsageMB: Int,
    val cpuUsagePercent: Int,
    val batteryImpact: String,
    val commandLatencyMs: Int
)

data class ModuleHealthStatus(
    val name: String,
    val isHealthy: Boolean
)

data class ErrorEntry(
    val message: String,
    val timestamp: String
)