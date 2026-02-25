// Author: Manoj Jhawar
// Purpose: Comprehensive UI for displaying device information from all managers

package com.augmentalis.devicemanager.dashboardui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.devicemanager.DeviceManager
import com.augmentalis.devicemanager.security.BiometricManager
import kotlinx.coroutines.launch

/**
 * Main Device Info UI Screen
 * Displays comprehensive information from all device managers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    deviceManager: DeviceManager = DeviceManager.getInstance(LocalContext.current)
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        deviceManager.initializeAll()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Device Information",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                deviceManager.initializeAll()
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = "Voice: click Refresh" }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row for different categories
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") },
                    modifier = Modifier.semantics { contentDescription = "Voice: click Overview tab" }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Network") },
                    modifier = Modifier.semantics { contentDescription = "Voice: click Network tab" }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Sensors") },
                    modifier = Modifier.semantics { contentDescription = "Voice: click Sensors tab" }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Security") },
                    modifier = Modifier.semantics { contentDescription = "Voice: click Security tab" }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Audio") },
                    modifier = Modifier.semantics { contentDescription = "Voice: click Audio tab" }
                )
            }
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> OverviewTab(deviceManager)
                1 -> NetworkTab(deviceManager)
                2 -> SensorsTab(deviceManager)
                3 -> SecurityTab(deviceManager)
                4 -> AudioTab(deviceManager)
            }
        }
    }
}

/**
 * Overview Tab - Shows general device capabilities
 */
@Composable
fun OverviewTab(deviceManager: DeviceManager) {
    val deviceInfo = deviceManager.getComprehensiveDeviceInfo()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            DeviceCard(
                title = "Device Information",
                icon = Icons.Default.PhoneAndroid
            ) {
                InfoRow("Manufacturer", deviceInfo.manufacturer)
                InfoRow("Model", deviceInfo.model)
                InfoRow("Android Version", "API ${deviceInfo.androidVersion}")
            }
        }
        
        item {
            CapabilitiesGrid(deviceManager)
        }
        
        item {
            StatusOverview(deviceManager)
        }
    }
}

/**
 * Network Tab - WiFi, Bluetooth, UWB
 */
@Composable
fun NetworkTab(deviceManager: DeviceManager) {
    val bluetoothState = deviceManager.bluetooth?.bluetoothState?.collectAsStateWithLifecycle()?.value ?: com.augmentalis.devicemanager.network.BluetoothManager.BluetoothState()
    val wifiState = deviceManager.wifi?.wifiState?.collectAsStateWithLifecycle()?.value ?: com.augmentalis.devicemanager.network.WiFiManager.WiFiState()
    val uwbState = deviceManager.uwb?.uwbState?.collectAsStateWithLifecycle()?.value ?: com.augmentalis.devicemanager.network.UwbManager.UwbState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bluetooth Section
        item {
            DeviceCard(
                title = "Bluetooth",
                icon = Icons.Default.Bluetooth,
                enabled = bluetoothState.isEnabled
            ) {
                InfoRow("Status", if (bluetoothState.isEnabled) "Enabled" else "Disabled")
                InfoRow("Version", bluetoothState.bluetoothVersion.toString())
                InfoRow("Scanning", if (bluetoothState.isScanning) "Active" else "Inactive")
                
                bluetoothState.leCapabilities?.let { caps ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Capabilities", style = MaterialTheme.typography.labelMedium)
                    InfoRow("LE Support", "Yes")
                    InfoRow("LE Audio", if (caps.leAudioSupported) "Yes" else "No")
                    InfoRow("Mesh", if (bluetoothState.meshSupported) "Yes" else "No")
                    InfoRow("Codecs", bluetoothState.supportedCodecs.map { it.name }.joinToString(", "))
                }
                
                if (bluetoothState.isEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { deviceManager.bluetooth?.startDiscovery() },
                            enabled = !bluetoothState.isScanning,
                            modifier = Modifier.semantics { contentDescription = "Voice: click Start Bluetooth Scan" }
                        ) {
                            Text("Start Scan")
                        }
                        Button(
                            onClick = { deviceManager.bluetooth?.stopDiscovery() },
                            enabled = bluetoothState.isScanning,
                            modifier = Modifier.semantics { contentDescription = "Voice: click Stop Bluetooth Scan" }
                        ) {
                            Text("Stop Scan")
                        }
                    }
                }
            }
        }
        
        // WiFi Section
        item {
            DeviceCard(
                title = "WiFi",
                icon = Icons.Default.Wifi,
                enabled = wifiState.isEnabled
            ) {
                InfoRow("Status", if (wifiState.isEnabled) "Enabled" else "Disabled")
                InfoRow("Connected", if (wifiState.isConnected) "Yes" else "No")
                wifiState.currentNetwork?.let {
                    InfoRow("Network", it.ssid)
                    InfoRow("Security", it.securityType.toString())
                    InfoRow("Speed", "${it.maxLinkSpeed} Mbps")
                }
                
                wifiState.capabilities?.let { caps ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Capabilities", style = MaterialTheme.typography.labelMedium)
                    InfoRow("WiFi 6", if (caps.supportsWiFi6) "Yes" else "No")
                    InfoRow("WiFi 6E", if (caps.supportsWiFi6E) "Yes" else "No")
                    InfoRow("WiFi 7", if (caps.supportsWiFi7) "Yes" else "No")
                    InfoRow("5GHz Band", if (caps.supports5GHz) "Yes" else "No")
                    InfoRow("6GHz Band", if (caps.supports6GHz) "Yes" else "No")
                    InfoRow("WiFi Direct", if (caps.supportsP2P) "Yes" else "No")
                    InfoRow("WiFi Aware", if (caps.supportsAware) "Yes" else "No")
                    InfoRow("WiFi RTT", if (caps.supportsRtt) "Yes" else "No")
                }
                
                if (wifiState.isEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { deviceManager.wifi?.startScan() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Voice: click Scan WiFi Networks" }
                    ) {
                        Text("Scan Networks")
                    }
                }
            }
        }
        
        // UWB Section
        item {
            DeviceCard(
                title = "Ultra-Wideband (UWB)",
                icon = Icons.Default.Radar,
                enabled = uwbState.isSupported
            ) {
                InfoRow("Supported", if (uwbState.isSupported) "Yes" else "No")
                InfoRow("Enabled", if (uwbState.isEnabled) "Yes" else "No")
                InfoRow("Ranging", if (uwbState.isRanging) "Active" else "Inactive")
                
                uwbState.capabilities?.let { caps ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Capabilities", style = MaterialTheme.typography.labelMedium)
                    InfoRow("Max Range", "${caps.maxRangingDistance}m")
                    InfoRow("Accuracy", "${caps.rangingAccuracy}m")
                    InfoRow("AoA Support", if (caps.supportsAoA) "Yes" else "No")
                    caps.angleAccuracy?.let {
                        InfoRow("Angle Accuracy", "${it}°")
                    }
                    InfoRow("Channels", caps.supportedChannels.joinToString(", "))
                    caps.chipsetInfo?.let { chip ->
                        InfoRow("Chipset", "${chip.manufacturer} ${chip.model}")
                    }
                }
                
                if (uwbState.isEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { deviceManager.uwb?.startDiscovery() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Voice: click Discover UWB Devices" }
                    ) {
                        Text("Discover Devices")
                    }
                }
            }
        }
    }
}

/**
 * Sensors Tab - LiDAR, Camera, etc.
 */
@Composable
fun SensorsTab(deviceManager: DeviceManager) {
    val lidarState = deviceManager.lidar?.lidarState?.collectAsStateWithLifecycle()?.value ?: com.augmentalis.devicemanager.LidarManager.LidarState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // LiDAR Section
        item {
            DeviceCard(
                title = "LiDAR / Depth Sensing",
                icon = Icons.Default.Sensors,
                enabled = lidarState.isAvailable
            ) {
                InfoRow("Available", if (lidarState.isAvailable) "Yes" else "No")
                InfoRow("Active", if (lidarState.isActive) "Yes" else "No")
                InfoRow("Technology", lidarState.technology ?: "None")
                
                lidarState.capabilities?.let { caps ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Capabilities", style = MaterialTheme.typography.labelMedium)
                    InfoRow("Max Range", "${caps.maxRange}m")
                    InfoRow("Min Range", "${caps.minRange}m")
                    InfoRow("Accuracy", "${caps.accuracy}m")
                    InfoRow("Resolution", "${caps.resolution.width}x${caps.resolution.height}")
                    InfoRow("Frame Rate", "${caps.frameRate.lower}-${caps.frameRate.upper} FPS")
                    InfoRow("Point Cloud", if (caps.supportsPointCloud) "Yes" else "No")
                    InfoRow("Mesh Generation", if (caps.supportsMeshGeneration) "Yes" else "No")
                    InfoRow("Motion Tracking", if (caps.supportsMotionTracking) "Yes" else "No")
                }
                
                if (lidarState.isAvailable && !lidarState.isActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { deviceManager.lidar?.startScanning() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Voice: click Start LiDAR Scanning" }
                    ) {
                        Text("Start Scanning")
                    }
                }
            }
        }
    }
}

/**
 * Security Tab - Biometrics
 */
@Composable
fun SecurityTab(deviceManager: DeviceManager) {
    val biometricState = deviceManager.biometric?.biometricState?.collectAsStateWithLifecycle()?.value ?: com.augmentalis.devicemanager.security.BiometricManager.BiometricState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            DeviceCard(
                title = "Biometric Authentication",
                icon = Icons.Default.Fingerprint,
                enabled = biometricState.isHardwareAvailable
            ) {
                InfoRow("Hardware Available", if (biometricState.isHardwareAvailable) "Yes" else "No")
                InfoRow("Enrolled", if (biometricState.isEnrolled) "Yes" else "No")
                InfoRow("Security Level", biometricState.securityLevel.toString())
                
                if (biometricState.availableTypes.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Available Types", style = MaterialTheme.typography.labelMedium)
                    biometricState.availableTypes.forEach { type ->
                        BiometricTypeCard(type)
                    }
                }
                
                biometricState.capabilities?.let { caps ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Capabilities", style = MaterialTheme.typography.labelMedium)
                    InfoRow("Max Fingerprints", caps.maxFingerprints.toString())
                    InfoRow("Max Faces", caps.maxFaces.toString())
                    InfoRow("Crypto Support", if (caps.supportsCryptoObject) "Yes" else "No")
                    InfoRow("Iris Recognition", if (caps.supportsIrisRecognition) "Yes" else "No")
                    InfoRow("Voice Recognition", if (caps.supportsVoiceRecognition) "Yes" else "No")
                }
            }
        }
    }
}

/**
 * Audio Tab - Audio devices and capabilities
 */
@Composable
fun AudioTab(@Suppress("UNUSED_PARAMETER") deviceManager: DeviceManager) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            DeviceCard(
                title = "Audio System",
                icon = Icons.AutoMirrored.Filled.VolumeUp
            ) {
                Text("Audio manager information would go here")
                // TODO: Add audio manager state when available
            }
        }
    }
}

/**
 * Capabilities Grid Component
 */
@Composable
fun CapabilitiesGrid(deviceManager: DeviceManager) {
    val capabilities = listOf(
        CapabilityItem("WiFi", Icons.Default.Wifi, deviceManager.wifi?.wifiState?.value?.capabilities != null),
        CapabilityItem("Bluetooth", Icons.Default.Bluetooth, deviceManager.bluetooth?.bluetoothState?.value?.isEnabled == true),
        CapabilityItem("UWB", Icons.Default.Radar, deviceManager.uwb?.uwbState?.value?.isSupported == true),
        CapabilityItem("LiDAR", Icons.Default.Sensors, deviceManager.lidar?.lidarState?.value?.isAvailable == true),
        CapabilityItem("Biometric", Icons.Default.Fingerprint, deviceManager.biometric?.biometricState?.value?.isHardwareAvailable == true),
        CapabilityItem("NFC", Icons.Default.Sensors, deviceManager.hasNFC())
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Device Capabilities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Grid replaced with Row for compatibility
            capabilities.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { capability ->
                        Box(modifier = Modifier.weight(1f)) {
                            CapabilityChip(capability)
                        }
                    }
                    // Fill empty cells
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

data class CapabilityItem(
    val name: String,
    val icon: ImageVector,
    val available: Boolean
)

@Composable
fun CapabilityChip(capability: CapabilityItem) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (capability.available)
            AvanueTheme.colors.surfaceVariant
        else
            AvanueTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = capability.icon,
                contentDescription = capability.name,
                modifier = Modifier.size(20.dp),
                tint = if (capability.available)
                    AvanueTheme.colors.primary
                else
                    AvanueTheme.colors.textSecondary
            )
            Text(
                text = capability.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Status Overview Component
 */
@Composable
fun StatusOverview(deviceManager: DeviceManager) {
    val bluetoothState = deviceManager.bluetooth?.bluetoothState?.collectAsStateWithLifecycle()?.value ?: com.augmentalis.devicemanager.network.BluetoothManager.BluetoothState()
    val wifiState = deviceManager.wifi?.wifiState?.collectAsStateWithLifecycle()?.value ?: com.augmentalis.devicemanager.network.WiFiManager.WiFiState()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Active Connections",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bluetooth connections
            if (bluetoothState.isEnabled) {
                val connectedDevices = deviceManager.bluetooth?.connectedDevices?.collectAsStateWithLifecycle()?.value ?: emptyList()
                ConnectionRow(
                    icon = Icons.Default.Bluetooth,
                    label = "Bluetooth",
                    count = connectedDevices.size
                )
            }
            
            // WiFi connection
            if (wifiState.isConnected) {
                ConnectionRow(
                    icon = Icons.Default.Wifi,
                    label = wifiState.currentNetwork?.ssid ?: "WiFi",
                    count = 1
                )
            }
        }
    }
}

/**
 * Reusable Components
 */
@Composable
fun DeviceCard(
    title: String,
    icon: ImageVector,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (enabled)
                        AvanueTheme.colors.primary
                    else
                        AvanueTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (!enabled) {
                    Surface(
                        shape = CircleShape,
                        color = AvanueTheme.colors.error.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Disabled",
                            modifier = Modifier
                                .size(16.dp)
                                .padding(2.dp),
                            tint = AvanueTheme.colors.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AvanueTheme.colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ConnectionRow(icon: ImageVector, label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = AvanueTheme.colors.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.weight(1f))
        Badge {
            Text(count.toString())
        }
    }
}

@Composable
fun BiometricTypeCard(type: BiometricManager.BiometricType) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (type.type) {
                "fingerprint" -> Icons.Default.Fingerprint
                "face" -> Icons.Default.Face
                else -> Icons.Default.Security
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = type.type.replaceFirstChar { char: Char -> if (char.isLowerCase()) char.titlecase() else char.toString() },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Security: ${type.securityLevel}",
                style = MaterialTheme.typography.labelSmall,
                color = AvanueTheme.colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (type.isEnrolled) {
            Surface(
                shape = CircleShape,
                color = AvanueTheme.colors.surfaceVariant
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Enrolled",
                    modifier = Modifier
                        .size(16.dp)
                        .padding(2.dp),
                    tint = AvanueTheme.colors.primary
                )
            }
        }
    }
}
