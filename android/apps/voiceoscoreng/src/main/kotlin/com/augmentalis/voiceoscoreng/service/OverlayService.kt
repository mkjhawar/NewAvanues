package com.augmentalis.voiceoscoreng.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.util.Log
import com.augmentalis.voiceoscoreng.MainActivity
import com.augmentalis.voiceoscoreng.app.R

private const val TAG = "OverlayService"

/**
 * Foreground service that displays a floating overlay FAB
 * on top of all apps for triggering UI exploration.
 */
class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    companion object {
        private const val CHANNEL_ID = "voiceos_overlay_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            Log.d(TAG, "start() called")
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Starting foreground service")
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        try {
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d(TAG, "startForeground done")
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            showOverlay()
            Log.d(TAG, "showOverlay done")
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeOverlay()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VoiceOS Scanner Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating scanner button"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showOverlay() {
        Log.d(TAG, "showOverlay() called, existing view: ${overlayView != null}")
        if (overlayView != null) return

        try {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.MATCH_PARENT,  // Full height so drawer handle is visible
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,  // Allow touches outside overlay
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL  // Right edge, centered vertically
            x = 0  // Flush to right edge
            y = 0
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                OverlayContent(
                    onClose = { stopSelf() }
                )
            }
        }

        // Make it draggable
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        composeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(composeView, params)
                    true
                }
                else -> false
            }
        }

        overlayView = composeView
        windowManager.addView(composeView, params)
        Log.d(TAG, "Overlay view added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay", e)
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }
}

/**
 * Check if accessibility service is enabled at the system level (fallback check)
 */
private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        ?: return false

    val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
        AccessibilityServiceInfo.FEEDBACK_ALL_MASK
    )

    return enabledServices.any { serviceInfo ->
        val serviceId = serviceInfo.id
        serviceId.contains("VoiceOSAccessibilityService") ||
        serviceInfo.resolveInfo?.serviceInfo?.packageName == context.packageName
    }
}

/**
 * Result of export operation
 */
data class ExportResult(
    val success: Boolean,
    val fileName: String,
    val fullPath: String,
    val message: String
)

/**
 * Export scan results to a markdown file in the app's external files directory
 */
private fun exportResultsToMarkdown(context: Context, result: ExplorationResult): ExportResult {
    return try {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        val fileName = "scan_results_$timestamp.md"

        val markdown = buildString {
            appendLine("# VoiceOSCoreNG Scan Results")
            appendLine()
            appendLine("**Scan Time:** ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(result.timestamp))}")
            appendLine("**Duration:** ${result.duration}ms")
            appendLine("**Package(s):** ${result.packageName}")
            appendLine()

            appendLine("## Summary")
            appendLine()
            appendLine("| Metric | Value |")
            appendLine("|--------|-------|")
            appendLine("| Total Elements | ${result.totalElements} |")
            appendLine("| Clickable | ${result.clickableElements} |")
            appendLine("| Scrollable | ${result.scrollableElements} |")
            appendLine("| Unique Hashes | ${result.deduplicationStats.uniqueHashes} |")
            appendLine("| Duplicates | ${result.deduplicationStats.duplicateCount} |")
            appendLine("| Commands Generated | ${result.commands.size} |")
            appendLine()

            appendLine("## VUIDs (${result.vuids.size} elements)")
            appendLine()
            appendLine("| # | Label | VUID | Type | Clickable |")
            appendLine("|---|-------|------|------|-----------|")
            result.vuids.forEachIndexed { index, vuidInfo ->
                // Use derived label from elementLabels map (includes labels from children)
                val label = result.elementLabels[index]
                    ?: vuidInfo.element.voiceLabel.ifBlank { vuidInfo.element.className.substringAfterLast(".") }
                val type = vuidInfo.element.className.substringAfterLast(".")
                val clickable = if (vuidInfo.element.isClickable) "✓" else ""
                appendLine("| $index | ${label.take(30)} | `${vuidInfo.vuid}` | $type | $clickable |")
            }
            appendLine()

            appendLine("## Hierarchy")
            appendLine()
            appendLine("```")
            result.hierarchy.forEach { node ->
                val indent = "  ".repeat(node.depth)
                val marker = if (node.childCount > 0) "▼" else "•"
                val element = result.elements.getOrNull(node.index)
                // Use derived label from elementLabels map
                val label = result.elementLabels[node.index]
                    ?: element?.voiceLabel?.ifBlank { null } ?: ""
                val vuid = result.vuids.getOrNull(node.index)?.vuid ?: ""
                val extra = buildString {
                    if (label.isNotBlank() && label != node.className) append(" \"$label\"")
                    if (element?.isClickable == true) append(" [click]")
                    if (vuid.isNotBlank()) append(" → $vuid")
                }
                appendLine("$indent$marker ${node.className}$extra")
            }
            appendLine("```")
            appendLine()

            appendLine("## Commands (${result.commands.size})")
            appendLine()
            if (result.commands.isNotEmpty()) {
                appendLine("| Voice Command | Element Type | Label Source | Target VUID |")
                appendLine("|---------------|--------------|--------------|-------------|")
                result.commands.forEach { cmd ->
                    val elemType = cmd.element.className.substringAfterLast(".")
                    val labelSource = when {
                        cmd.element.text.isNotBlank() -> "text"
                        cmd.element.contentDescription.isNotBlank() -> "contentDesc"
                        cmd.element.resourceId.isNotBlank() -> "resourceId"
                        cmd.derivedLabel.isNotBlank() -> "child text"
                        else -> "unknown"
                    }
                    appendLine("| \"${cmd.phrase}\" | $elemType | $labelSource | `${cmd.targetVuid}` |")
                }
            } else {
                appendLine("*No commands generated*")
            }
            appendLine()

            if (result.deduplicationStats.duplicateElements.isNotEmpty()) {
                appendLine("## Duplicates (${result.deduplicationStats.duplicateCount})")
                appendLine()
                appendLine("| Element | Hash | First Seen Index |")
                appendLine("|---------|------|------------------|")
                result.deduplicationStats.duplicateElements.forEach { dup ->
                    // Use derived label from elementLabels using firstSeenIndex
                    val label = result.elementLabels[dup.firstSeenIndex]
                        ?: dup.element.voiceLabel.ifBlank { dup.element.className.substringAfterLast(".") }
                    appendLine("| $label | `${dup.hash.take(16)}` | ${dup.firstSeenIndex} |")
                }
                appendLine()
            }

            appendLine("## AVU Output")
            appendLine()
            appendLine("```yaml")
            appendLine(result.avuOutput)
            appendLine("```")
            appendLine()

            appendLine("---")
            appendLine("*Generated by VoiceOSCoreNG Scanner*")
        }

        // Write to app's external files directory (accessible via file manager)
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = java.io.File(dir, fileName)
        file.writeText(markdown)

        Log.d(TAG, "Exported scan results to: ${file.absolutePath}")
        ExportResult(
            success = true,
            fileName = fileName,
            fullPath = file.absolutePath,
            message = "Scan results exported successfully"
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to export results", e)
        ExportResult(
            success = false,
            fileName = "",
            fullPath = "",
            message = "Export failed: ${e.message}"
        )
    }
}

@Composable
private fun OverlayContent(onClose: () -> Unit) {
    var drawerOpen by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var testModeEnabled by remember { mutableStateOf(true) }
    var showConfigPanel by remember { mutableStateOf(false) }
    var showDevSettings by remember { mutableStateOf(false) }
    var devSettingsExpanded by remember { mutableStateOf(false) }  // Expandable section in drawer

    // Developer settings state
    var debugLogging by remember { mutableStateOf(true) }
    var showVuidsOverlay by remember { mutableStateOf(false) }
    var autoMinimize by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val isConnectedFromService by VoiceOSAccessibilityService.isConnected.collectAsState()
    val isConnectedFromSystem = remember { isAccessibilityServiceEnabled(context) }

    // Use either service StateFlow OR system-level check
    val isConnected = isConnectedFromService || isConnectedFromSystem

    val explorationResults by VoiceOSAccessibilityService.explorationResults.collectAsState()
    val lastError by VoiceOSAccessibilityService.lastError.collectAsState()

    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        // Use Box with proper alignment for the drawer handle on right edge
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd  // Align to right edge, centered vertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Results panel (shown when has results)
            if (showResults && explorationResults != null) {
                ResultsPanel(
                    result = explorationResults!!,
                    onClose = { showResults = false }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Sliding drawer panel
            if (drawerOpen) {
                Card(
                    modifier = Modifier
                        .width(260.dp)
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)  // Light gray background
                    ),
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Status indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isConnected) Color(0xFF10B981) else Color(0xFFDC2626),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isConnected) "Service Connected" else "Enable Accessibility",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        // Test Mode Toggle
                        DrawerActionButton(
                            text = if (testModeEnabled) "Disable Test Mode" else "Enable Test Mode",
                            icon = Icons.Default.Science,
                            iconColor = Color(0xFFDC2626),  // Red
                            onClick = { testModeEnabled = !testModeEnabled }
                        )

                        // Scan / Run All Tests
                        DrawerActionButton(
                            text = "Scan Current App",
                            icon = Icons.Default.PlayArrow,
                            iconColor = Color(0xFF3B82F6),  // Blue
                            enabled = isConnected,
                            onClick = {
                                VoiceOSAccessibilityService.exploreCurrentApp()
                                showResults = true
                            }
                        )

                        // Scan All Windows / Test Exploration
                        DrawerActionButton(
                            text = "Scan All Windows",
                            icon = Icons.Default.Explore,
                            iconColor = Color(0xFF6B7280),  // Gray
                            enabled = isConnected,
                            onClick = {
                                VoiceOSAccessibilityService.exploreAllApps()
                                showResults = true
                            }
                        )

                        // Show/Hide Results
                        if (explorationResults != null) {
                            DrawerActionButton(
                                text = if (showResults) "Hide Results" else "Show Results",
                                icon = Icons.Default.Visibility,
                                iconColor = Color(0xFFF59E0B),  // Amber
                                onClick = { showResults = !showResults }
                            )
                        }

                        // View Config
                        DrawerActionButton(
                            text = "View Config",
                            icon = Icons.Default.Info,
                            iconColor = Color(0xFF6366F1),  // Indigo
                            onClick = { showConfigPanel = true }
                        )

                        // Developer Settings - Expandable Section
                        DrawerActionButton(
                            text = if (devSettingsExpanded) "Hide Dev Settings" else "Developer Settings",
                            icon = if (devSettingsExpanded) Icons.Default.ExpandLess else Icons.Default.Settings,
                            iconColor = Color(0xFF1E3A5F),  // Dark blue
                            onClick = { devSettingsExpanded = !devSettingsExpanded }
                        )

                        // Expandable Developer Settings Content
                        if (devSettingsExpanded) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE5E7EB)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    // Debug Logging Toggle
                                    DrawerSettingsToggle(
                                        label = "Debug Logging",
                                        checked = debugLogging,
                                        onCheckedChange = { debugLogging = it }
                                    )

                                    // Show VUIDs Toggle
                                    DrawerSettingsToggle(
                                        label = "Show VUIDs Overlay",
                                        checked = showVuidsOverlay,
                                        onCheckedChange = { showVuidsOverlay = it }
                                    )

                                    // Auto-minimize Toggle
                                    DrawerSettingsToggle(
                                        label = "Auto-minimize App",
                                        checked = autoMinimize,
                                        onCheckedChange = { autoMinimize = it }
                                    )

                                    // Test Mode indicator
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Test Mode", fontSize = 11.sp, color = Color(0xFF374151))
                                        Text(
                                            text = if (testModeEnabled) "ON" else "OFF",
                                            fontSize = 11.sp,
                                            color = if (testModeEnabled) Color(0xFF10B981) else Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Close Overlay button at bottom
                        DrawerActionButton(
                            text = "Close Overlay",
                            icon = Icons.Default.Close,
                            iconColor = Color(0xFF6B7280),
                            onClick = onClose
                        )
                    }
                }
            }

            // Drawer handle/tab
            Card(
                modifier = Modifier
                    .width(40.dp)
                    .height(100.dp)
                    .clickable { drawerOpen = !drawerOpen },
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) Color(0xFF1E3A5F) else Color(0xFFDC2626)
                ),
                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (drawerOpen) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                        contentDescription = "Toggle drawer",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = Icons.Default.Scanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Error indicator
            lastError?.let { error ->
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = error,
                    fontSize = 10.sp,
                    color = Color.Red,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                )
            }
            }  // End Row

            // Config Panel Overlay
            if (showConfigPanel) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showConfigPanel = false },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .width(320.dp)
                            .padding(16.dp)
                            .clickable(enabled = false) {},
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("LearnApp Configuration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                IconButton(onClick = { showConfigPanel = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            ConfigRow("Variant", "LearnApp Lite")
                            ConfigRow("Tier", "LITE")
                            ConfigRow("Test Mode", if (testModeEnabled) "ENABLED" else "DISABLED")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Processing", color = Color.Gray, fontSize = 11.sp)
                            ConfigRow("Mode", "IMMEDIATE")
                            ConfigRow("Max Elements", "Unlimited")
                            ConfigRow("Max Apps", "Unlimited")
                            ConfigRow("Batch Timeout", "3000ms")
                            ConfigRow("Exploration Depth", "5")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Features", color = Color.Gray, fontSize = 11.sp)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                FeatureBadge("AI", true)
                                FeatureBadge("NLU", true)
                                FeatureBadge("Exploration", true)
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                FeatureBadge("Detection", true)
                                FeatureBadge("Caching", true)
                                FeatureBadge("Analytics", true)
                            }
                        }
                    }
                }
            }

            // Developer Settings Panel Overlay
            if (showDevSettings) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showDevSettings = false },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .width(320.dp)
                            .padding(16.dp)
                            .clickable(enabled = false) {},
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Developer Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                IconButton(onClick = { showDevSettings = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Debug logging toggle
                            SettingsToggle(
                                label = "Debug Logging",
                                description = "Enable verbose logging",
                                checked = true,
                                onCheckedChange = { }
                            )

                            // Show VUIDs toggle
                            SettingsToggle(
                                label = "Show VUIDs in Overlay",
                                description = "Display VUIDs on scanned elements",
                                checked = false,
                                onCheckedChange = { }
                            )

                            // Auto-minimize toggle
                            SettingsToggle(
                                label = "Auto-minimize App",
                                description = "Minimize after starting overlay",
                                checked = true,
                                onCheckedChange = { }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Scan Settings", color = Color.Gray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            ConfigRow("Scan Delay", "0ms")
                            ConfigRow("Max Depth", "10")
                            ConfigRow("Hash Algorithm", "MD5")
                        }
                    }
                }
            }
        }  // End Box
    }  // End MaterialTheme
}

@Composable
private fun DrawerActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Text label with rounded background
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                color = if (enabled) Color(0xFF374151) else Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Icon with colored background
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (enabled) iconColor else Color.Gray,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            )
        }
    }
}

@Composable
private fun DrawerSettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = Color(0xFF374151))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(20.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF10B981),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}

@Composable
private fun ConfigRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FeatureBadge(name: String, enabled: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
        modifier = Modifier.padding(end = 4.dp, top = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (enabled) "✓" else "✗",
                color = if (enabled) Color(0xFF10B981) else Color.Gray,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(name, color = Color.White, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 12.sp)
            Text(description, color = Color.Gray, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF10B981),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
private fun MenuButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp)
            .then(if (!enabled) Modifier else Modifier)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color.White else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (enabled) Color.White else Color.Gray
        )
    }
}

@Composable
private fun ResultsPanel(
    result: ExplorationResult,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var isExpanded by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    val context = LocalContext.current
    val tabs = listOf("Summary", "VUIDs", "Hierarchy", "Duplicates", "Commands", "AVU")

    // Export result card (overlay instead of dialog - dialogs don't work in Service context)
    if (showExportDialog && exportResult != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showExportDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(300.dp)
                    .padding(16.dp)
                    .clickable(enabled = false) {},  // Consume clicks
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (exportResult!!.success) "Export Successful" else "Export Failed",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (exportResult!!.success) {
                        Text("File saved:", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = exportResult!!.fileName,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Location:", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = exportResult!!.fullPath,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = exportResult!!.message,
                            color = Color(0xFFEF4444)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showExportDialog = false }) {
                            Text("OK", color = Color(0xFF10B981))
                        }
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .then(
                if (isExpanded) {
                    Modifier.fillMaxWidth().fillMaxHeight(0.85f)
                } else {
                    Modifier.width(320.dp).heightIn(max = 400.dp)
                }
            )
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header with expand and export buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scan Results",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row {
                    // Export button
                    IconButton(
                        onClick = {
                            exportResult = exportResultsToMarkdown(context, result)
                            showExportDialog = true
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Export",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Expand/collapse button
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 11.sp) }
                    )
                }
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp)
            ) {
                when (selectedTab) {
                    0 -> SummaryTab(result)
                    1 -> VUIDsTab(result)
                    2 -> HierarchyTab(result)
                    3 -> DuplicatesTab(result)
                    4 -> CommandsTab(result)
                    5 -> AVUTab(result)
                }
            }
        }
    }
}

@Composable
private fun SummaryTab(result: ExplorationResult) {
    LazyColumn {
        item {
            StatRow("Package", result.packageName)
            StatRow("Total Elements", result.totalElements.toString())
            StatRow("Clickable", result.clickableElements.toString())
            StatRow("Scrollable", result.scrollableElements.toString())
            StatRow("Scan Time", "${result.duration}ms")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Deduplication", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            StatRow("Unique Hashes", result.deduplicationStats.uniqueHashes.toString())
            StatRow("Duplicates Found", result.deduplicationStats.duplicateCount.toString())
            Spacer(modifier = Modifier.height(8.dp))
            Text("Commands", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            StatRow("Generated", result.commands.size.toString())
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun VUIDsTab(result: ExplorationResult) {
    LazyColumn {
        val vuidsToShow = result.vuids.take(50)
        items(vuidsToShow.size) { index ->
            val vuidInfo = vuidsToShow[index]
            // Use derived label from elementLabels map
            val label = result.elementLabels[index]
                ?: vuidInfo.element.voiceLabel.ifBlank { vuidInfo.element.className.substringAfterLast(".") }
            val isClickable = vuidInfo.element.isClickable
            val isScrollable = vuidInfo.element.isScrollable

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        color = when {
                            isClickable -> Color(0xFF10B981)  // Green for clickable
                            isScrollable -> Color(0xFF3B82F6)  // Blue for scrollable
                            else -> Color.White
                        },
                        fontSize = 11.sp,
                        fontWeight = if (isClickable || isScrollable) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isClickable) {
                        Text(
                            text = "[TAP]",
                            color = Color(0xFF10B981),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    if (isScrollable) {
                        Text(
                            text = "[SCROLL]",
                            color = Color(0xFF3B82F6),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                Text(
                    text = "VUID: ${vuidInfo.vuid}",
                    color = Color(0xFF6366F1),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                // Show the command if this element has one
                result.commands.find { it.element == vuidInfo.element }?.let { cmd ->
                    Text(
                        text = "Voice: \"${cmd.derivedLabel}\" → ${cmd.action}",
                        color = Color(0xFFF59E0B),
                        fontSize = 9.sp
                    )
                }
                Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (result.vuids.size > 50) {
            item {
                Text(
                    text = "... and ${result.vuids.size - 50} more",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun HierarchyTab(result: ExplorationResult) {
    LazyColumn {
        items(result.hierarchy.take(100)) { node ->
            val element = result.elements.getOrNull(node.index)
            val vuidInfo = result.vuids.getOrNull(node.index)
            val command = result.commands.find { it.element == element }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (node.depth * 12).dp, top = 4.dp, bottom = 4.dp)
            ) {
                // Main node row with class name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (node.childCount > 0) "▼" else "•",
                        color = if (node.childCount > 0) Color.White else Color.Gray,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = node.className,
                        color = when {
                            element?.isClickable == true -> Color(0xFF10B981) // Green for clickable
                            element?.isScrollable == true -> Color(0xFF3B82F6) // Blue for scrollable
                            node.childCount > 0 -> Color.White
                            else -> Color.Gray
                        },
                        fontSize = 10.sp,
                        fontWeight = if (element?.isClickable == true) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                    // Clickable/Scrollable badges
                    if (element?.isClickable == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "[TAP]",
                            color = Color(0xFF10B981),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (element?.isScrollable == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "[SCROLL]",
                            color = Color(0xFF3B82F6),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Label - use derived label from elementLabels map
                val derivedLabel = result.elementLabels[node.index]
                if (derivedLabel != null && derivedLabel != node.className) {
                    Text(
                        text = "\"$derivedLabel\"",
                        color = Color(0xFFF59E0B),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }

                // VUID
                vuidInfo?.let { info ->
                    Text(
                        text = "VUID: ${info.vuid}",
                        color = Color(0xFF6366F1),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }

                // Command (if generated)
                command?.let { cmd ->
                    Text(
                        text = "→ \"${cmd.phrase}\"",
                        color = Color(0xFF10B981),
                        fontSize = 8.sp,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }
            }
        }
        if (result.hierarchy.size > 100) {
            item {
                Text(
                    text = "... and ${result.hierarchy.size - 100} more nodes",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun DuplicatesTab(result: ExplorationResult) {
    val stats = result.deduplicationStats

    LazyColumn {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D3D))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Deduplication Stats", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    StatRow("Total Hashes", stats.totalHashes.toString())
                    StatRow("Unique", stats.uniqueHashes.toString())
                    StatRow("Duplicates", stats.duplicateCount.toString())
                    if (stats.totalHashes > 0) {
                        val ratio = (stats.uniqueHashes.toFloat() / stats.totalHashes * 100).toInt()
                        StatRow("Uniqueness", "$ratio%")
                    }
                }
            }
        }

        if (stats.duplicateElements.isEmpty()) {
            item {
                Text("No duplicates found!", color = Color.Green, fontSize = 11.sp)
            }
        } else {
            items(stats.duplicateElements.take(20)) { dup ->
                // Get label from elementLabels using firstSeenIndex (original element)
                val label = result.elementLabels[dup.firstSeenIndex]
                    ?: dup.element.className.substringAfterLast(".")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        color = Color(0xFFF59E0B),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Type: ${dup.element.className.substringAfterLast(".")}",
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "Hash: ${dup.hash.take(16)}...",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "First seen at index: ${dup.firstSeenIndex}",
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                    Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun CommandsTab(result: ExplorationResult) {
    LazyColumn {
        items(result.commands.take(30)) { cmd ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "\"${cmd.phrase}\"",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Action: ${cmd.action} → ${cmd.targetVuid}",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (cmd.alternates.isNotEmpty()) {
                    Text(
                        text = "Alt: ${cmd.alternates.take(2).joinToString(", ")}",
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 9.sp
                    )
                }
                Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (result.commands.size > 30) {
            item {
                Text(
                    text = "... and ${result.commands.size - 30} more commands",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun AVUTab(result: ExplorationResult) {
    LazyColumn {
        item {
            Text(
                text = result.avuOutput.take(2000),
                color = Color(0xFF6EE7B7),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 12.sp
            )
            if (result.avuOutput.length > 2000) {
                Text(
                    text = "\n... truncated (${result.avuOutput.length} chars total)",
                    color = Color.Gray,
                    fontSize = 9.sp
                )
            }
        }
    }
}
