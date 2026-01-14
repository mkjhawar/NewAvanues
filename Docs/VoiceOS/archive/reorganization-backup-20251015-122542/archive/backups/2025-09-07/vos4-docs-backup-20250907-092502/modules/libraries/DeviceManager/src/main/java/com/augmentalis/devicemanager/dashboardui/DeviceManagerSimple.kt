// Simple Device Manager UI that works properly
package com.augmentalis.devicemanager.dashboardui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.devicemanager.DeviceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDeviceManagerUI(
    deviceManager: DeviceManager,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Device Manager",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Device Info",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val deviceInfo = deviceManager.getComprehensiveDeviceInfo()
                        Text("Manufacturer: ${deviceInfo.manufacturer}")
                        Text("Model: ${deviceInfo.model}")
                        Text("Android Version: API ${deviceInfo.androidVersion}")
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Hardware Capabilities",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("WiFi")
                            Text(if (deviceManager.wifi?.wifiState?.value?.capabilities != null) "✓" else "✗")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bluetooth")
                            Text(if (deviceManager.bluetooth?.bluetoothState?.value?.isEnabled == true) "✓" else "✗")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("NFC")
                            Text(if (deviceManager.hasNFC()) "✓" else "✗")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Biometric")
                            Text(if (deviceManager.biometric?.biometricState?.value?.isHardwareAvailable == true) "✓" else "✗")
                        }
                    }
                }
            }
        }
    }
}
