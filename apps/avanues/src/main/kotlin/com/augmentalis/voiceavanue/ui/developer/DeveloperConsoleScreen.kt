/**
 * DeveloperConsoleScreen.kt - Hidden developer/debug console
 *
 * Accessible via 5-tap on version number in Settings or 3-tap in About.
 * Comprehensive diagnostic display:
 * - Build info, service states, permissions
 * - Device hardware (CPU, RAM, GPU, storage)
 * - Memory/heap, battery, display metrics
 * - Peripherals (USB, cameras, input devices)
 * - DataStore raw values, database files, RPC ports
 *
 * Export: Share, Save to Downloads, Copy to Clipboard.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.developer

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.InputDevice
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.voiceavanue.BuildConfig
import androidx.datastore.preferences.core.edit
import com.augmentalis.voiceavanue.data.avanuesDataStore
import com.augmentalis.voiceavanue.service.VoiceAvanueAccessibilityService
import com.augmentalis.voiceoscore.StaticCommandRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// =============================================================================
// Data Models
// =============================================================================

data class ServiceDebugInfo(
    val name: String,
    val status: String,
    val isHealthy: Boolean,
    val details: Map<String, String> = emptyMap()
)

data class DatabaseFileInfo(
    val fileName: String,
    val sizeBytes: Long,
    val lastModified: Long
)

data class RpcPortInfo(
    val serviceName: String,
    val port: Int,
    val transport: String
)

// =============================================================================
// ViewModel
// =============================================================================

@HiltViewModel
class DeveloperConsoleViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _rawPreferences = MutableStateFlow<Map<String, String>>(emptyMap())
    val rawPreferences: StateFlow<Map<String, String>> = _rawPreferences

    private val _services = MutableStateFlow<List<ServiceDebugInfo>>(emptyList())
    val services: StateFlow<List<ServiceDebugInfo>> = _services

    val buildInfo: Map<String, String> = buildMap {
        put("Version", BuildConfig.VERSION_NAME)
        put("Version Code", BuildConfig.VERSION_CODE.toString())
        put("Application ID", BuildConfig.APPLICATION_ID)
        put("Build Type", BuildConfig.BUILD_TYPE)
        put("Min SDK", "29")
        put("Target SDK", "34")
        put("Device SDK", Build.VERSION.SDK_INT.toString())
        put("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
        put("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    }

    val rpcPorts: List<RpcPortInfo> = listOf(
        RpcPortInfo("PluginRegistry", 50050, "UDS/TCP"),
        RpcPortInfo("VoiceOS", 50051, "UDS/TCP"),
        RpcPortInfo("AVA", 50052, "UDS/TCP"),
        RpcPortInfo("Cockpit", 50053, "TCP"),
        RpcPortInfo("NLU", 50054, "UDS/TCP"),
        RpcPortInfo("WebAvanue", 50055, "UDS/TCP")
    )

    init {
        observeDataStore()
        refreshServices()
    }

    private fun observeDataStore() {
        viewModelScope.launch {
            context.avanuesDataStore.data.collect { prefs ->
                val map = mutableMapOf<String, String>()
                prefs.asMap().forEach { (key, value) ->
                    map[key.name] = value.toString()
                }
                _rawPreferences.value = map.toSortedMap()
            }
        }
    }

    fun refreshServices() {
        viewModelScope.launch {
            val states = mutableListOf<ServiceDebugInfo>()

            val accessibilityOn = VoiceAvanueAccessibilityService.isEnabled(context)
            states.add(
                ServiceDebugInfo(
                    name = "Accessibility Service",
                    status = if (accessibilityOn) "ENABLED" else "DISABLED",
                    isHealthy = accessibilityOn,
                    details = mapOf("Class" to "VoiceAvanueAccessibilityService")
                )
            )

            val overlayOn = Settings.canDrawOverlays(context)
            states.add(
                ServiceDebugInfo(
                    name = "Overlay Permission",
                    status = if (overlayOn) "GRANTED" else "DENIED",
                    isHealthy = overlayOn
                )
            )

            val notifEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as? android.app.NotificationManager
                nm?.areNotificationsEnabled() ?: false
            } else true
            states.add(
                ServiceDebugInfo(
                    name = "Notifications",
                    status = if (notifEnabled) "ENABLED" else "BLOCKED",
                    isHealthy = notifEnabled
                )
            )

            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val batteryExempt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            } else true
            states.add(
                ServiceDebugInfo(
                    name = "Battery Optimization",
                    status = if (batteryExempt) "EXEMPT" else "OPTIMIZED",
                    isHealthy = batteryExempt,
                    details = mapOf("Desired" to "Exempt (unrestricted)")
                )
            )

            _services.value = states
        }
    }

    // ── Device Hardware ──

    fun getDeviceHardwareInfo(): Map<String, String> = buildMap {
        put("Manufacturer", Build.MANUFACTURER)
        put("Model", Build.MODEL)
        put("Brand", Build.BRAND)
        put("Device", Build.DEVICE)
        put("Board", Build.BOARD)
        put("Hardware", Build.HARDWARE)
        put("Product", Build.PRODUCT)
        put("CPU Cores", Runtime.getRuntime().availableProcessors().toString())
        put("Architecture", Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
        put("ABIs", Build.SUPPORTED_ABIS.joinToString(", "))
        put("Bootloader", Build.BOOTLOADER)
        put("Fingerprint", Build.FINGERPRINT)

        // CPU frequency
        try {
            val maxFreqFile = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            if (maxFreqFile.exists()) {
                val maxMhz = maxFreqFile.readText().trim().toLongOrNull()?.div(1000) ?: 0
                put("CPU Max Freq", "${maxMhz} MHz")
            }
        } catch (_: Exception) {}

        // RAM
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (am != null) {
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            val totalGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
            put("Total RAM", "%.1f GB".format(totalGb))
            put("Memory Class", "${am.memoryClass} MB (large: ${am.largeMemoryClass} MB)")
            put("Low RAM Device", am.isLowRamDevice.toString())
        }

        // Storage
        try {
            val stats = StatFs(Environment.getDataDirectory().path)
            val totalGb = stats.totalBytes / (1024.0 * 1024.0 * 1024.0)
            val availGb = stats.availableBytes / (1024.0 * 1024.0 * 1024.0)
            put("Internal Storage", "%.1f GB total, %.1f GB free".format(totalGb, availGb))
        } catch (_: Exception) {}
    }

    // ── Memory & Heap ──

    fun getMemoryInfo(): Map<String, String> = buildMap {
        val rt = Runtime.getRuntime()
        val maxMb = rt.maxMemory() / (1024 * 1024)
        val totalMb = rt.totalMemory() / (1024 * 1024)
        val freeMb = rt.freeMemory() / (1024 * 1024)
        val usedMb = totalMb - freeMb
        put("Heap Max", "$maxMb MB")
        put("Heap Allocated", "$totalMb MB")
        put("Heap Used", "$usedMb MB")
        put("Heap Free", "$freeMb MB")
        put("Heap Usage", "${(usedMb * 100) / maxMb}%")

        // System memory
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (am != null) {
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            val availMb = memInfo.availMem / (1024 * 1024)
            val totalMbSys = memInfo.totalMem / (1024 * 1024)
            val usedMbSys = totalMbSys - availMb
            put("System RAM Used", "$usedMbSys / $totalMbSys MB")
            put("Low Memory", memInfo.lowMemory.toString())
            put("Low Threshold", "${memInfo.threshold / (1024 * 1024)} MB")
        }
    }

    // ── Battery ──

    fun getBatteryInfo(): Map<String, String> = buildMap {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)
        if (batteryStatus != null) {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val pct = if (scale > 0) (level * 100) / scale else -1
            put("Level", "$pct%")

            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            put("Charging", if (charging) "Yes" else "No")

            val plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val plugType = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "None"
            }
            put("Plug Type", plugType)

            val health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val healthStr = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Unknown"
            }
            put("Health", healthStr)

            val temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            if (temp > 0) put("Temperature", "%.1f\u00B0C".format(temp / 10.0))

            val voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            if (voltage > 0) put("Voltage", "%.2f V".format(voltage / 1000.0))

            val tech = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
            if (!tech.isNullOrEmpty()) put("Technology", tech)
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        if (pm != null) {
            put("Power Save Mode", pm.isPowerSaveMode.toString())
            put("Interactive", pm.isInteractive.toString())
        }
    }

    // ── Display ──

    fun getDisplayInfo(): Map<String, String> = buildMap {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (wm != null) {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            put("Resolution", "${metrics.widthPixels} x ${metrics.heightPixels}")
            put("Density", "${metrics.density}x (${metrics.densityDpi} dpi)")
            put("Density Bucket", when {
                metrics.densityDpi <= DisplayMetrics.DENSITY_LOW -> "ldpi"
                metrics.densityDpi <= DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
                metrics.densityDpi <= DisplayMetrics.DENSITY_HIGH -> "hdpi"
                metrics.densityDpi <= DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
                metrics.densityDpi <= DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi"
                else -> "xxxhdpi"
            })

            val widthInches = metrics.widthPixels / metrics.xdpi
            val heightInches = metrics.heightPixels / metrics.ydpi
            val diagInches = Math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())
            put("Physical Size", "%.1f\" diagonal".format(diagInches))
            put("Scaled Density", metrics.scaledDensity.toString())

            @Suppress("DEPRECATION")
            val refreshRate = wm.defaultDisplay.refreshRate
            put("Refresh Rate", "%.0f Hz".format(refreshRate))

            @Suppress("DEPRECATION")
            val rotation = wm.defaultDisplay.rotation
            put("Rotation", when (rotation) {
                0 -> "Portrait (0\u00B0)"
                1 -> "Landscape (90\u00B0)"
                2 -> "Reverse Portrait (180\u00B0)"
                3 -> "Reverse Landscape (270\u00B0)"
                else -> rotation.toString()
            })
        }
    }

    // ── Peripherals ──

    fun getPeripheralInfo(): Map<String, String> = buildMap {
        // USB devices
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        val usbDevices = usbManager?.deviceList ?: emptyMap()
        put("USB Devices", usbDevices.size.toString())
        usbDevices.values.forEachIndexed { i, dev ->
            val name = dev.productName ?: dev.deviceName
            put("USB[$i]", "$name (VID:${dev.vendorId} PID:${dev.productId})")
        }

        // Cameras
        try {
            val camManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraIds = camManager?.cameraIdList ?: emptyArray()
            put("Cameras", cameraIds.size.toString())
            cameraIds.forEachIndexed { i, id ->
                val chars = camManager?.getCameraCharacteristics(id)
                val facing = when (chars?.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                    CameraCharacteristics.LENS_FACING_BACK -> "Back"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                    else -> "Unknown"
                }
                val sizes = chars?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?.getOutputSizes(android.graphics.ImageFormat.JPEG)
                val maxSize = sizes?.maxByOrNull { it.width * it.height }
                val mp = if (maxSize != null) "%.1f MP".format(maxSize.width * maxSize.height / 1_000_000.0) else ""
                put("Camera[$i]", "$facing $mp")
            }
        } catch (_: Exception) {
            put("Cameras", "Error reading")
        }

        // Input devices — explicit loop avoids IntArray.mapNotNull type inference issues
        val inputIds = InputDevice.getDeviceIds()
        val physicalInputs = mutableListOf<InputDevice>()
        for (id in inputIds) {
            val dev: InputDevice = InputDevice.getDevice(id) ?: continue
            val src: Int = dev.sources
            if ((src and InputDevice.SOURCE_KEYBOARD) != 0 ||
                (src and InputDevice.SOURCE_TOUCHSCREEN) != 0 ||
                (src and InputDevice.SOURCE_MOUSE) != 0 ||
                (src and InputDevice.SOURCE_GAMEPAD) != 0) {
                physicalInputs.add(dev)
            }
        }
        put("Input Devices", physicalInputs.size.toString())
        physicalInputs.take(8).forEachIndexed { i, dev ->
            val src: Int = dev.sources
            val type = when {
                (src and InputDevice.SOURCE_GAMEPAD) != 0 -> "Gamepad"
                (src and InputDevice.SOURCE_MOUSE) != 0 -> "Mouse"
                (src and InputDevice.SOURCE_TOUCHSCREEN) != 0 -> "Touch"
                (src and InputDevice.SOURCE_KEYBOARD) != 0 -> "Keyboard"
                else -> "Other"
            }
            put("Input[$i]", "${dev.name} ($type)")
        }
    }

    // ── Voice Commands ──

    fun getCommandCounts(): Map<String, String> = buildMap {
        val allStatic = StaticCommandRegistry.all()
        val grouped = allStatic.groupBy { it.category }
        put("Total Static", allStatic.size.toString())
        grouped.forEach { (cat, cmds) ->
            val catName = cat.name.lowercase().replace('_', ' ')
                .replaceFirstChar { it.uppercaseChar() }
            put("  $catName", cmds.size.toString())
        }
        // Dynamic/custom counts from running service
        val svc = VoiceAvanueAccessibilityService.getInstance()
        val dynamicCount = svc?.let { "connected" } ?: "service not running"
        put("Service Status", dynamicCount)
    }

    // ── Database Files ──

    fun getDatabaseFiles(): List<DatabaseFileInfo> {
        val dbDir = context.getDatabasePath("_probe").parentFile ?: return emptyList()
        if (!dbDir.exists()) return emptyList()
        return dbDir.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".db") || it.name.endsWith(".db-shm") || it.name.endsWith(".db-wal")) }
            ?.sortedBy { it.name }
            ?.map { file ->
                DatabaseFileInfo(
                    fileName = file.name,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified()
                )
            } ?: emptyList()
    }

    fun clearDataStore() {
        viewModelScope.launch {
            context.avanuesDataStore.edit { it.clear() }
        }
    }

    // ── Diagnostic Report ──

    fun generateDiagnosticReport(): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val sb = StringBuilder()
        sb.appendLine("=".repeat(60))
        sb.appendLine("AVANUES DIAGNOSTIC REPORT")
        sb.appendLine("Generated: $timestamp")
        sb.appendLine("=".repeat(60))

        fun section(title: String, data: Map<String, String>) {
            sb.appendLine()
            sb.appendLine("--- $title ---")
            data.forEach { (k, v) -> sb.appendLine("  $k: $v") }
        }

        section("Build Info", buildInfo)
        section("Device Hardware", getDeviceHardwareInfo())
        section("Memory & Heap", getMemoryInfo())
        section("Battery", getBatteryInfo())
        section("Display", getDisplayInfo())
        section("Peripherals", getPeripheralInfo())
        section("Voice Commands", getCommandCounts())

        sb.appendLine()
        sb.appendLine("--- Service States ---")
        _services.value.forEach { svc ->
            sb.appendLine("  ${svc.name}: ${svc.status}")
            svc.details.forEach { (k, v) -> sb.appendLine("    $k: $v") }
        }

        sb.appendLine()
        sb.appendLine("--- DataStore Preferences ---")
        val prefs = _rawPreferences.value
        if (prefs.isEmpty()) {
            sb.appendLine("  (all defaults)")
        } else {
            prefs.forEach { (k, v) -> sb.appendLine("  $k = $v") }
        }

        sb.appendLine()
        sb.appendLine("--- RPC Ports ---")
        rpcPorts.forEach { rpc ->
            sb.appendLine("  ${rpc.serviceName}: ${rpc.port} (${rpc.transport})")
        }

        val dbFiles = getDatabaseFiles()
        sb.appendLine()
        sb.appendLine("--- Database Files ---")
        if (dbFiles.isEmpty()) {
            sb.appendLine("  (none)")
        } else {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            dbFiles.forEach { db ->
                sb.appendLine("  ${db.fileName}  ${formatFileSize(db.sizeBytes)}  ${dateFormat.format(Date(db.lastModified))}")
            }
        }

        sb.appendLine()
        sb.appendLine("=".repeat(60))
        sb.appendLine("End of report")
        return sb.toString()
    }

    fun shareReport() {
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) { generateDiagnosticReport() }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Avanues Diagnostic Report")
                putExtra(Intent.EXTRA_TEXT, report)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Diagnostic Report")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun saveReportToDownloads() {
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) { generateDiagnosticReport() }
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
            val fileName = "avanues-diagnostic-$timestamp.txt"

            try {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                )
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(report.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun copyReportToClipboard() {
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) { generateDiagnosticReport() }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Avanues Diagnostic Report", report))
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// =============================================================================
// Screen
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperConsoleScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeveloperConsoleViewModel = hiltViewModel()
) {
    val rawPrefs by viewModel.rawPreferences.collectAsState()
    val services by viewModel.services.collectAsState()
    val dbFiles = remember { viewModel.getDatabaseFiles() }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Developer Console", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Debug & diagnostics",
                            style = MaterialTheme.typography.bodySmall,
                            color = AvanueTheme.colors.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshServices() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share Report") },
                                leadingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp)) },
                                onClick = { showExportMenu = false; viewModel.shareReport() }
                            )
                            DropdownMenuItem(
                                text = { Text("Save to Downloads") },
                                leadingIcon = { Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp)) },
                                onClick = { showExportMenu = false; viewModel.saveReportToDownloads() }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy to Clipboard") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp)) },
                                onClick = { showExportMenu = false; viewModel.copyReportToClipboard() }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Build Info
            item {
                ConsoleSection(title = "Build Info", icon = Icons.Default.Build) {
                    viewModel.buildInfo.forEach { (key, value) ->
                        KeyValueRow(key = key, value = value)
                    }
                }
            }

            // Device Hardware
            item {
                val hw = remember { viewModel.getDeviceHardwareInfo() }
                ConsoleSection(title = "Device Hardware", icon = Icons.Default.Memory) {
                    hw.forEach { (key, value) -> KeyValueRow(key = key, value = value) }
                }
            }

            // Memory & Heap
            item {
                val mem = viewModel.getMemoryInfo()
                ConsoleSection(title = "Memory & Heap", icon = Icons.Default.DataUsage) {
                    mem.forEach { (key, value) -> KeyValueRow(key = key, value = value, mono = true) }
                }
            }

            // Battery
            item {
                val battery = remember { viewModel.getBatteryInfo() }
                ConsoleSection(title = "Battery", icon = Icons.Default.BatteryStd) {
                    battery.forEach { (key, value) -> KeyValueRow(key = key, value = value) }
                }
            }

            // Display
            item {
                val display = remember { viewModel.getDisplayInfo() }
                ConsoleSection(title = "Display", icon = Icons.Default.Smartphone) {
                    display.forEach { (key, value) -> KeyValueRow(key = key, value = value) }
                }
            }

            // Peripherals
            item {
                val peripherals = remember { viewModel.getPeripheralInfo() }
                ConsoleSection(title = "Peripherals", icon = Icons.Default.Usb) {
                    peripherals.forEach { (key, value) -> KeyValueRow(key = key, value = value) }
                }
            }

            // Voice Commands
            item {
                val commands = remember { viewModel.getCommandCounts() }
                ConsoleSection(title = "Voice Commands", icon = Icons.Default.RecordVoiceOver) {
                    commands.forEach { (key, value) -> KeyValueRow(key = key, value = value) }
                }
            }

            // Service States
            item {
                ConsoleSection(title = "Service States", icon = Icons.Default.HealthAndSafety) {
                    services.forEach { svc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (svc.isHealthy) Icons.Default.CheckCircle
                                else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (svc.isHealthy) AvanueTheme.colors.success
                                else AvanueTheme.colors.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                svc.name,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp
                            )
                            Text(
                                svc.status,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (svc.isHealthy) AvanueTheme.colors.success
                                else AvanueTheme.colors.error
                            )
                        }
                        svc.details.forEach { (k, v) ->
                            Text(
                                "  $k: $v",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = AvanueTheme.colors.textSecondary,
                                modifier = Modifier.padding(start = 26.dp)
                            )
                        }
                    }
                }
            }

            // DataStore Raw Values
            item {
                ConsoleSection(
                    title = "DataStore (avanues_settings)",
                    icon = Icons.Default.Storage,
                    action = {
                        TextButton(onClick = { showResetConfirm = true }) {
                            Text("RESET", color = AvanueTheme.colors.error, fontSize = 12.sp)
                        }
                    }
                ) {
                    if (rawPrefs.isEmpty()) {
                        Text(
                            "(empty \u2014 all defaults)",
                            color = AvanueTheme.colors.textSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    } else {
                        rawPrefs.forEach { (key, value) ->
                            KeyValueRow(key = key, value = value, mono = true)
                        }
                    }
                }
            }

            // RPC Ports
            item {
                ConsoleSection(title = "RPC Port Allocation", icon = Icons.Default.Hub) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Text(
                            "Service",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AvanueTheme.colors.textSecondary
                        )
                        Text(
                            "Port",
                            modifier = Modifier.width(60.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AvanueTheme.colors.textSecondary
                        )
                        Text(
                            "Transport",
                            modifier = Modifier.width(72.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AvanueTheme.colors.textSecondary
                        )
                    }
                    HorizontalDivider(color = AvanueTheme.colors.textSecondary.copy(alpha = 0.3f))
                    viewModel.rpcPorts.forEach { rpc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Text(
                                rpc.serviceName,
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp
                            )
                            Text(
                                rpc.port.toString(),
                                modifier = Modifier.width(60.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = AvanueTheme.colors.info
                            )
                            Text(
                                rpc.transport,
                                modifier = Modifier.width(72.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = AvanueTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Database Files
            item {
                ConsoleSection(title = "Database Files", icon = Icons.Default.FolderOpen) {
                    if (dbFiles.isEmpty()) {
                        Text(
                            "No database files found",
                            color = AvanueTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    } else {
                        val dateFormat = remember {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                        }
                        dbFiles.forEach { db ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        db.fileName,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "Modified: ${dateFormat.format(Date(db.lastModified))}",
                                        fontSize = 11.sp,
                                        color = AvanueTheme.colors.textSecondary
                                    )
                                }
                                Text(
                                    formatFileSize(db.sizeBytes),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = AvanueTheme.colors.info
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Path: /data/data/${BuildConfig.APPLICATION_ID}/databases/",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = AvanueTheme.colors.textSecondary,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Reset DataStore confirmation dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Reset DataStore?") },
            text = {
                Text("This will clear ALL persisted preferences and restore defaults. " +
                        "The app may behave unexpectedly until restarted.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearDataStore()
                    showResetConfirm = false
                }) {
                    Text("Reset", color = AvanueTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
// Reusable Components
// =============================================================================

@Composable
private fun ConsoleSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AvanueCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AvanueTheme.colors.info,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            action?.invoke()
        }
        content()
    }
}

@Composable
private fun KeyValueRow(key: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            key,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = AvanueTheme.colors.textSecondary
        )
        Text(
            value,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}
