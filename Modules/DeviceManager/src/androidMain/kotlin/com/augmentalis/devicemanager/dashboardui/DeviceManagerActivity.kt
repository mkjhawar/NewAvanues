/**
 * DeviceManagerActivity.kt - Main UI for Device Manager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Comprehensive device management interface with glassmorphism design
 */
package com.augmentalis.devicemanager.dashboardui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.AvanueThemeProvider
import com.augmentalis.avanueui.theme.HydraColors
import com.augmentalis.avanueui.theme.HydraGlass
import com.augmentalis.avanueui.theme.HydraWater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main Activity for Device Manager
 */
class DeviceManagerActivity : ComponentActivity() {
    
    private val viewModel: DeviceViewModel by viewModels {
        DeviceViewModelFactory(this)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AvanueThemeProvider(
                colors = HydraColors,
                glass = HydraGlass,
                water = HydraWater,
                isDark = true
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AvanueTheme.colors.background
                ) {
                    DeviceManagerContent(viewModel)
                }
            }
        }
    }
}

/**
 * Main content composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagerContent(viewModel: DeviceViewModel) {
    val hardwareInfo by viewModel.hardwareInfo.observeAsState()
    val batteryInfo by viewModel.batteryInfo.observeAsState()
    val sensorsList by viewModel.sensorsList.observeAsState(emptyList())
    val networkInfo by viewModel.networkInfo.observeAsState()
    val audioDevices by viewModel.audioDevices.observeAsState(emptyList())
    val displayInfo by viewModel.displayInfo.observeAsState()
    val xrSupported by viewModel.xrSupported.observeAsState(false)
    val foldableState by viewModel.foldableState.collectAsStateWithLifecycle()
    val imuData by viewModel.imuData.collectAsStateWithLifecycle()
    val bluetoothDevices by viewModel.bluetoothDevices.observeAsState(emptyList())
    val wifiNetworks by viewModel.wifiNetworks.observeAsState(emptyList())
    val uwbSupported by viewModel.uwbSupported.observeAsState(false)
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val successMessage by viewModel.successMessage.observeAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Devices,
                            contentDescription = "Device Manager",
                            modifier = Modifier.size(28.dp),
                            tint = DeviceColors.TypePhone
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Device Manager",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${hardwareInfo?.model ?: "Loading..."}",
                                fontSize = 12.sp,
                                color = AvanueTheme.colors.textPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAllData() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = AvanueTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                contentColor = AvanueTheme.colors.textPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Hardware") },
                    icon = { Icon(Icons.Filled.Memory, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Sensors") },
                    icon = { Icon(Icons.Filled.Sensors, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Network") },
                    icon = { Icon(Icons.Filled.NetworkCheck, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Audio") },
                    icon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = { Text("Display") },
                    icon = { Icon(Icons.Filled.Monitor, contentDescription = null) }
                )
            }
            
            // Tab Content
            when (selectedTab) {
                0 -> OverviewTab(
                    hardwareInfo = hardwareInfo,
                    batteryInfo = batteryInfo,
                    networkInfo = networkInfo,
                    sensorsCount = sensorsList.size,
                    audioDevicesCount = audioDevices.size,
                    xrSupported = xrSupported,
                    uwbSupported = uwbSupported,
                    onRunDiagnostics = { viewModel.runDiagnostics() }
                )
                1 -> HardwareTab(
                    hardwareInfo = hardwareInfo,
                    batteryInfo = batteryInfo,
                    foldableState = foldableState
                )
                2 -> SensorsTab(
                    sensorsList = sensorsList,
                    imuData = imuData,
                    onTestSensors = { viewModel.testSensors() }
                )
                3 -> NetworkTab(
                    networkInfo = networkInfo,
                    bluetoothDevices = bluetoothDevices,
                    wifiNetworks = wifiNetworks,
                    uwbSupported = uwbSupported,
                    onScanBluetooth = { viewModel.scanBluetoothDevices() },
                    onScanWiFi = { viewModel.scanWiFiNetworks() }
                )
                4 -> AudioTab(
                    audioDevices = audioDevices
                )
                5 -> DisplayTab(
                    displayInfo = displayInfo,
                    xrSupported = xrSupported
                )
            }
        }
    }
    
    // Loading overlay
    if (isLoading) {
        LoadingOverlay()
    }
    
    // Error/Success messages
    errorMessage?.let { message ->
        ErrorSnackbar(
            message = message,
            onDismiss = { viewModel.clearError() }
        )
    }
    
    successMessage?.let { message ->
        SuccessSnackbar(
            message = message,
            onDismiss = { viewModel.clearSuccess() }
        )
    }
}

/**
 * Overview tab
 */
@Composable
fun OverviewTab(
    hardwareInfo: HardwareInfo?,
    batteryInfo: BatteryInfo?,
    networkInfo: NetworkConnectionInfo?,
    sensorsCount: Int,
    audioDevicesCount: Int,
    xrSupported: Boolean,
    uwbSupported: Boolean,
    onRunDiagnostics: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device status card
        item {
            DeviceStatusCard(
                model = hardwareInfo?.model ?: "Unknown",
                androidVersion = hardwareInfo?.androidVersion ?: "Unknown",
                batteryLevel = batteryInfo?.level ?: 0,
                isCharging = batteryInfo?.isCharging ?: false,
                networkType = networkInfo?.type ?: "None",
                isConnected = networkInfo?.isConnected ?: false
            )
        }
        
        // Quick stats grid
        item {
            QuickStatsGrid(
                sensorsCount = sensorsCount,
                audioDevicesCount = audioDevicesCount,
                xrSupported = xrSupported,
                uwbSupported = uwbSupported
            )
        }
        
        // Diagnostics card
        item {
            DiagnosticsCard(
                onRunDiagnostics = onRunDiagnostics
            )
        }
        
        // System capabilities
        item {
            SystemCapabilitiesCard(
                xrSupported = xrSupported,
                uwbSupported = uwbSupported,
                foldableSupported = false
            )
        }
    }
}

/**
 * Hardware tab
 */
@Composable
fun HardwareTab(
    hardwareInfo: HardwareInfo?,
    batteryInfo: BatteryInfo?,
    foldableState: String
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hardware details card
        item {
            HardwareDetailsCard(hardwareInfo)
        }
        
        // Battery card
        item {
            BatteryCard(batteryInfo)
        }
        
        // Foldable status
        if (foldableState != "UNSUPPORTED") {
            item {
                FoldableStatusCard(foldableState)
            }
        }
    }
}

/**
 * Sensors tab
 */
@Composable
fun SensorsTab(
    sensorsList: List<SensorInfo>,
    imuData: Triple<Float, Float, Float>,
    onTestSensors: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // IMU data card
        item {
            IMUDataCard(imuData)
        }
        
        // Test sensors button
        item {
            TestSensorsCard(onTestSensors)
        }
        
        // Sensors list
        items(sensorsList) { sensor ->
            SensorItemCard(sensor)
        }
    }
}

/**
 * Network tab
 */
@Composable
fun NetworkTab(
    networkInfo: NetworkConnectionInfo?,
    bluetoothDevices: List<String>,
    wifiNetworks: List<String>,
    uwbSupported: Boolean,
    onScanBluetooth: () -> Unit,
    onScanWiFi: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Network status card
        item {
            NetworkStatusCard(networkInfo)
        }
        
        // WiFi section
        item {
            WiFiSection(
                networks = wifiNetworks,
                onScan = onScanWiFi
            )
        }
        
        // Bluetooth section
        item {
            BluetoothSection(
                devices = bluetoothDevices,
                onScan = onScanBluetooth
            )
        }
        
        // UWB status
        if (uwbSupported) {
            item {
                UWBStatusCard()
            }
        }
    }
}

/**
 * Audio tab
 */
@Composable
fun AudioTab(
    audioDevices: List<AudioDeviceInfo>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(audioDevices) { device ->
            AudioDeviceCard(device)
        }
    }
}

/**
 * Display tab
 */
@Composable
fun DisplayTab(
    displayInfo: DisplayInfo?,
    xrSupported: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Display specs card
        item {
            DisplaySpecsCard(displayInfo)
        }
        
        // XR capabilities
        if (xrSupported) {
            item {
                XRCapabilitiesCard()
            }
        }
    }
}

// Card Components

@Composable
fun DeviceStatusCard(
    model: String,
    androidVersion: String,
    batteryLevel: Int,
    isCharging: Boolean,
    networkType: String,
    isConnected: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Primary)*/
            .testTag("device_status_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Device Status",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusItem(
                    icon = Icons.Filled.PhoneAndroid,
                    label = "Model",
                    value = model,
                    color = DeviceColors.TypePhone
                )
                StatusItem(
                    icon = Icons.Filled.Android,
                    label = "Android",
                    value = androidVersion,
                    color = DeviceColors.StatusConnected
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusItem(
                    icon = if (isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.Battery4Bar,
                    label = "Battery",
                    value = "$batteryLevel%",
                    color = getBatteryColor(batteryLevel)
                )
                StatusItem(
                    icon = if (isConnected) Icons.Filled.NetworkWifi else Icons.Filled.NetworkWifi3Bar,
                    label = "Network",
                    value = networkType,
                    color = if (isConnected) DeviceColors.StatusConnected else DeviceColors.StatusDisconnected
                )
            }
        }
    }
}

@Composable
fun QuickStatsGrid(
    sensorsCount: Int,
    audioDevicesCount: Int,
    xrSupported: Boolean,
    uwbSupported: Boolean
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            QuickStatCard(
                icon = Icons.Filled.Sensors,
                title = "Sensors",
                value = sensorsCount.toString(),
                color = DeviceColors.SensorAccelerometer
            )
        }
        item {
            QuickStatCard(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Audio",
                value = audioDevicesCount.toString(),
                color = DeviceColors.AudioSpeaker
            )
        }
        item {
            QuickStatCard(
                icon = Icons.Filled.ViewInAr,
                title = "XR",
                value = if (xrSupported) "Supported" else "Not Supported",
                color = if (xrSupported) DeviceColors.TypeXR else DeviceColors.StatusDisconnected
            )
        }
        item {
            QuickStatCard(
                icon = Icons.Filled.Radar,
                title = "UWB",
                value = if (uwbSupported) "Supported" else "Not Supported",
                color = if (uwbSupported) DeviceColors.NetworkUWB else DeviceColors.StatusDisconnected
            )
        }
    }
}

@Composable
fun DiagnosticsCard(
    onRunDiagnostics: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Status)*/
        .testTag("diagnostics_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.BugReport,
                contentDescription = "Diagnostics",
                modifier = Modifier.size(48.dp),
                tint = DeviceColors.StatusWarning
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "System Diagnostics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Run comprehensive device tests",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onRunDiagnostics,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeviceColors.StatusWarning.copy(alpha = 0.2f),
                    contentColor = DeviceColors.StatusWarning
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run Diagnostics")
            }
        }
    }
}

@Composable
fun SystemCapabilitiesCard(
    xrSupported: Boolean,
    uwbSupported: Boolean,
    foldableSupported: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Primary),*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "System Capabilities",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CapabilityRow(
                icon = Icons.Filled.ViewInAr,
                name = "Extended Reality (XR)",
                supported = xrSupported
            )
            CapabilityRow(
                icon = Icons.Filled.Radar,
                name = "Ultra-Wideband (UWB)",
                supported = uwbSupported
            )
            CapabilityRow(
                icon = Icons.Filled.DevicesFold,
                name = "Foldable Display",
                supported = foldableSupported
            )
        }
    }
}

@Composable
fun HardwareDetailsCard(hardwareInfo: HardwareInfo?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Hardware),*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Hardware Details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            hardwareInfo?.let { info ->
                HardwareRow("Model", info.model)
                HardwareRow("Manufacturer", info.manufacturer)
                HardwareRow("Brand", info.brand)
                HardwareRow("Device", info.device)
                HardwareRow("Board", info.board)
                HardwareRow("Hardware", info.hardware)
                HardwareRow("Product", info.product)
                HardwareRow("Android Version", info.androidVersion)
                HardwareRow("API Level", info.apiLevel.toString())
                HardwareRow("Build ID", info.buildId)
                HardwareRow("Kernel", info.kernelVersion)
            }
        }
    }
}

@Composable
fun BatteryCard(batteryInfo: BatteryInfo?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Battery),*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Battery",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                batteryInfo?.let { info ->
                    Icon(
                        imageVector = if (info.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.Battery4Bar,
                        contentDescription = "Battery",
                        modifier = Modifier.size(32.dp),
                        tint = getBatteryColor(info.level)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            batteryInfo?.let { info ->
                // Battery level bar
                LinearProgressIndicator(
                    progress = { info.level / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = getBatteryColor(info.level),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BatteryStatItem("Level", "${info.level}%")
                    BatteryStatItem("Status", if (info.isCharging) "Charging" else "Discharging")
                    BatteryStatItem("Health", info.health)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BatteryStatItem("Temperature", "${info.temperature}°C")
                    BatteryStatItem("Voltage", "${info.voltage}mV")
                    BatteryStatItem("Technology", info.technology ?: "Unknown")
                }
            }
        }
    }
}

@Composable
fun IMUDataCard(imuData: Triple<Float, Float, Float>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Sensors),*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "IMU Data (Live)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IMUAxisDisplay("X", imuData.first, DeviceColors.SensorAccelerometer)
                IMUAxisDisplay("Y", imuData.second, DeviceColors.SensorGyroscope)
                IMUAxisDisplay("Z", imuData.third, DeviceColors.SensorMagnetometer)
            }
        }
    }
}

@Composable
fun NetworkStatusCard(networkInfo: NetworkConnectionInfo?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Network),*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Network Status",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            networkInfo?.let { info ->
                NetworkInfoRow("Type", info.type, getNetworkIcon(info.type))
                NetworkInfoRow("Status", if (info.isConnected) "Connected" else "Disconnected", Icons.Filled.NetworkCheck)
                NetworkInfoRow("Metered", if (info.isMetered) "Yes" else "No", Icons.Filled.DataUsage)
                NetworkInfoRow("Bandwidth", "${info.bandwidth} Kbps", Icons.Filled.Speed)
                NetworkInfoRow("Signal", "${info.signalStrength} dBm", Icons.Filled.SignalCellularAlt)
                
                if (info.capabilities.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Capabilities: ${info.capabilities.joinToString(", ")}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// Helper Components

@Composable
fun StatusItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun QuickStatCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = DeviceColors.TypePhone
        )
    }
}

@Composable
fun ErrorSnackbar(
    message: String,
    onDismiss: () -> Unit
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "Error",
                modifier = Modifier.size(20.dp),
                tint = DeviceColors.StatusError
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message)
        }
    }
}

@Composable
fun SuccessSnackbar(
    message: String,
    onDismiss: () -> Unit
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier.size(20.dp),
                tint = DeviceColors.StatusConnected
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message)
        }
    }
}

// Additional helper components

@Composable
fun CapabilityRow(
    icon: ImageVector,
    name: String,
    supported: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = name,
            modifier = Modifier.size(24.dp),
            tint = if (supported) DeviceColors.StatusConnected else DeviceColors.StatusDisconnected
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (supported) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = if (supported) "Supported" else "Not Supported",
            modifier = Modifier.size(20.dp),
            tint = if (supported) DeviceColors.StatusConnected else DeviceColors.StatusDisconnected
        )
    }
}

@Composable
fun HardwareRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BatteryStatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun IMUAxisDisplay(axis: String, value: Float, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = axis,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format("%.2f", value),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun NetworkInfoRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = DeviceColors.NetworkWiFi
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

// Additional cards for other tabs

@Composable
fun FoldableStatusCard(state: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Primary),*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Foldable Display",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "State: $state",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Filled.DevicesFold,
                contentDescription = "Foldable",
                modifier = Modifier.size(32.dp),
                tint = DeviceColors.TypeFoldable
            )
        }
    }
}

@Composable
fun TestSensorsCard(onTest: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Sensors)*/
          .clickable { onTest() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Test All Sensors",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Test",
                tint = DeviceColors.SensorAccelerometer
            )
        }
    }
}

@Composable
fun SensorItemCard(sensor: SensorInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(
                GlassMorphismConfig(
                    tintColor = getSensorColor(sensor.type),
                    cornerRadius = 12.dp,
                    backgroundOpacity = 0.08f
                )
            ),*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sensor.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = sensor.vendor,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = getSensorIcon(sensor.type),
                    contentDescription = sensor.name,
                    modifier = Modifier.size(24.dp),
                    tint = getSensorColor(sensor.type)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SensorSpec("Range", "${sensor.maximumRange}")
                SensorSpec("Power", "${sensor.power}mA")
                SensorSpec("Version", "v${sensor.version}")
            }
        }
    }
}

@Composable
fun WiFiSection(
    networks: List<String>,
    onScan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Network),*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WiFi Networks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onScan) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Scan",
                        tint = DeviceColors.NetworkWiFi
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (networks.isEmpty()) {
                Text(
                    text = "No networks found. Tap refresh to scan.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            } else {
                networks.forEach { network ->
                    NetworkItem(network, Icons.Filled.Wifi)
                }
            }
        }
    }
}

@Composable
fun BluetoothSection(
    devices: List<String>,
    onScan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Network),*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bluetooth Devices",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onScan) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                        contentDescription = "Scan",
                        tint = DeviceColors.NetworkBluetooth
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (devices.isEmpty()) {
                Text(
                    text = "No devices found. Tap scan to search.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            } else {
                devices.forEach { device ->
                    NetworkItem(device, Icons.Filled.Bluetooth)
                }
            }
        }
    }
}

@Composable
fun UWBStatusCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Network),*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ultra-Wideband",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Precision ranging enabled",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Filled.Radar,
                contentDescription = "UWB",
                modifier = Modifier.size(32.dp),
                tint = DeviceColors.NetworkUWB
            )
        }
    }
}

@Composable
fun AudioDeviceCard(device: AudioDeviceInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Audio),*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (device.isInput) Icons.Filled.Mic else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = device.type,
                modifier = Modifier.size(32.dp),
                tint = DeviceColors.AudioSpeaker
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${device.type} • ${device.sampleRates.first()} Hz",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun DisplaySpecsCard(displayInfo: DisplayInfo?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.Primary),*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Display Specifications",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            displayInfo?.let { info ->
                DisplaySpecRow("Resolution", "${info.width} × ${info.height}")
                DisplaySpecRow("Density", "${info.density} dpi")
                DisplaySpecRow("Refresh Rate", "${info.refreshRate.toInt()} Hz")
                DisplaySpecRow("HDR", if (info.hdrCapabilities.isNotEmpty()) "Supported" else "Not Supported")
                DisplaySpecRow("Shape", if (info.isRound) "Round" else "Rectangle")
                info.cutoutInfo?.let {
                    DisplaySpecRow("Display Cutout", it)
                }
            }
        }
    }
}

@Composable
fun XRCapabilitiesCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            /*.glassMorphism(DeviceGlassConfigs.XR),*/,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.ViewInAr,
                    contentDescription = "XR",
                    modifier = Modifier.size(32.dp),
                    tint = DeviceColors.TypeXR
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "XR Capabilities",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Extended Reality Ready",
                        fontSize = 12.sp,
                        color = DeviceColors.TypeXR
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            XRFeatureRow("Spatial Tracking", true)
            XRFeatureRow("Hand Tracking", true)
            XRFeatureRow("Eye Tracking", false)
            XRFeatureRow("6DOF Support", true)
        }
    }
}

// Helper functions and additional components

@Composable
fun SensorSpec(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun NetworkItem(name: String, icon: ImageVector) {
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
            tint = DeviceColors.NetworkWiFi
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            fontSize = 14.sp,
            color = Color.White
        )
    }
}

@Composable
fun DisplaySpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun XRFeatureRow(feature: String, supported: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            fontSize = 14.sp,
            color = Color.White
        )
        Icon(
            imageVector = if (supported) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = if (supported) "Supported" else "Not Supported",
            modifier = Modifier.size(20.dp),
            tint = if (supported) DeviceColors.StatusConnected else DeviceColors.StatusDisconnected
        )
    }
}

// Utility functions

fun getBatteryColor(level: Int): Color {
    return when {
        level > 75 -> DeviceColors.BatteryFull
        level > 50 -> DeviceColors.BatteryMedium
        level > 25 -> DeviceColors.BatteryLow
        else -> DeviceColors.BatteryCritical
    }
}

fun getSensorColor(type: Int): Color {
    return when (type) {
        1 -> DeviceColors.SensorAccelerometer
        4 -> DeviceColors.SensorGyroscope
        2 -> DeviceColors.SensorMagnetometer
        8 -> DeviceColors.SensorProximity
        5 -> DeviceColors.SensorLight
        13 -> DeviceColors.SensorTemperature
        6 -> DeviceColors.SensorPressure
        else -> DeviceColors.SensorAccelerometer
    }
}

fun getSensorIcon(type: Int): ImageVector {
    return when (type) {
        1 -> Icons.Filled.Speed
        4 -> Icons.AutoMirrored.Filled.RotateRight
        2 -> Icons.Filled.Explore
        8 -> Icons.Filled.PersonPin
        5 -> Icons.Filled.LightMode
        13 -> Icons.Filled.Thermostat
        6 -> Icons.Filled.Compress
        else -> Icons.Filled.Sensors
    }
}

fun getNetworkIcon(type: String): ImageVector {
    return when (type) {
        "WiFi" -> Icons.Filled.Wifi
        "Cellular" -> Icons.Filled.NetworkCell
        "Bluetooth" -> Icons.Filled.Bluetooth
        "Ethernet" -> Icons.Filled.Cable
        else -> Icons.Filled.NetworkCheck
    }
}