/**
 * DebugPanel.kt - Full debug interface for VoiceOS testing
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-02
 */
package com.augmentalis.voiceos.ui.overlays.debug

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.augmentalis.voiceos.ui.overlays.ComposeViewLifecycleOwner

data class RecognitionEntry(
    val timestamp: Long,
    val text: String,
    val confidence: Float,
    val isCommand: Boolean
)

data class A11yTreeNode(
    val id: String,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val isClickable: Boolean,
    val isFocusable: Boolean,
    val isForeground: Boolean,
    val depth: Int = 0
)

data class GeneratedCommand(
    val phrase: String,
    val action: String,
    val targetElement: String?
)

data class DebugPanelState(
    val isServiceRunning: Boolean = false,
    val isListening: Boolean = false,
    val currentApp: String = "",
    val currentActivity: String = "",
    val recognitionLog: List<RecognitionEntry> = emptyList(),
    val accessibilityTree: List<A11yTreeNode> = emptyList(),
    val generatedCommands: List<GeneratedCommand> = emptyList()
)

class DebugPanel(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ComposeViewLifecycleOwner? = null
    private var isShowing = false

    private var panelState by mutableStateOf(DebugPanelState())

    fun show() {
        if (overlayView == null) {
            overlayView = createOverlayView()
        }
        if (!isShowing) {
            windowManager.addView(overlayView, createLayoutParams())
            isShowing = true
        }
    }

    fun hide() {
        overlayView?.let {
            if (isShowing) {
                try {
                    windowManager.removeView(it)
                    isShowing = false
                } catch (e: IllegalArgumentException) { }
            }
        }
    }

    fun updateState(state: DebugPanelState) { panelState = state }

    fun updateServiceStatus(running: Boolean, listening: Boolean) {
        panelState = panelState.copy(isServiceRunning = running, isListening = listening)
    }

    fun updateAppInfo(packageName: String, activityName: String) {
        panelState = panelState.copy(currentApp = packageName, currentActivity = activityName)
    }

    fun addRecognitionEntry(entry: RecognitionEntry) {
        val updated = (listOf(entry) + panelState.recognitionLog).take(50)
        panelState = panelState.copy(recognitionLog = updated)
    }

    fun updateAccessibilityTree(nodes: List<A11yTreeNode>) {
        panelState = panelState.copy(accessibilityTree = nodes)
    }

    fun updateGeneratedCommands(commands: List<GeneratedCommand>) {
        panelState = panelState.copy(generatedCommands = commands)
    }

    fun isVisible(): Boolean = isShowing

    fun dispose() {
        hide()
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        overlayView = null
    }

    private fun createOverlayView(): ComposeView {
        val owner = ComposeViewLifecycleOwner().also {
            lifecycleOwner = it
            it.onCreate()
        }

        return ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                DebugPanelUI(state = panelState, onClose = { hide() })
            }
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
    }
}

@Composable
private fun DebugPanelUI(state: DebugPanelState, onClose: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Status", "Recognition", "A11y Tree", "Commands")

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f))) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("VoiceOS Debug Panel", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Service: ${if (state.isServiceRunning) "Running" else "Stopped"} | Listening: ${if (state.isListening) "Active" else "Idle"}",
                        color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusDot(if (state.isServiceRunning) Color(0xFF4CAF50) else Color(0xFFF44336), "Service")
                    StatusDot(if (state.isListening) Color(0xFF2196F3) else Color(0xFF9E9E9E), "Listening")
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Color.White) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> StatusTab(state)
                    1 -> RecognitionTab(state.recognitionLog)
                    2 -> AccessibilityTreeTab(state.accessibilityTree)
                    3 -> CommandsTab(state.generatedCommands)
                }
            }
        }
    }
}

@Composable
private fun StatusDot(color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp)
    }
}

@Composable
private fun StatusTab(state: DebugPanelState) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { StatusCard(Icons.Default.Apps, "Current App", state.currentApp.ifEmpty { "Unknown" }) }
        item { StatusCard(Icons.Default.Layers, "Current Activity", state.currentActivity.ifEmpty { "Unknown" }) }
        item { StatusCard(Icons.Default.Mic, "Wake Word", "Hey AVA") }
        item { StatusCard(Icons.Default.AccountTree, "Elements Found",
            "${state.accessibilityTree.size} (${state.accessibilityTree.count { it.isForeground }} FG / ${state.accessibilityTree.count { !it.isForeground }} BG)") }
        item { StatusCard(Icons.Default.Code, "Generated Commands", "${state.generatedCommands.size}") }
    }
}

@Composable
private fun StatusCard(icon: ImageVector, title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF2196F3), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RecognitionTab(entries: List<RecognitionEntry>) {
    if (entries.isEmpty()) {
        EmptyState(Icons.Default.MicOff, "No recognition history yet.\nSay \"Hey AVA\" to start.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(entries) { entry ->
                val timeAgo = remember(entry.timestamp) {
                    val diff = System.currentTimeMillis() - entry.timestamp
                    when { diff < 60000 -> "${diff / 1000}s ago"; diff < 3600000 -> "${diff / 60000}m ago"; else -> "${diff / 3600000}h ago" }
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (entry.isCommand) Color(0xFF1B5E20).copy(alpha = 0.3f) else Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (entry.isCommand) Icons.Default.Check else Icons.Default.Mic, null,
                        tint = if (entry.isCommand) Color(0xFF4CAF50) else Color(0xFF9E9E9E), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.text, color = Color.White, fontSize = 13.sp)
                        Text("Confidence: ${(entry.confidence * 100).toInt()}%", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Text(timeAgo, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun AccessibilityTreeTab(nodes: List<A11yTreeNode>) {
    if (nodes.isEmpty()) {
        EmptyState(Icons.Default.AccountTree, "No accessibility nodes found.\nEnsure VoiceOS service is running.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(nodes) { node ->
                val displayText = node.text ?: node.contentDescription ?: node.className.substringAfterLast(".")
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (node.isForeground) Color(0xFF1E1E1E) else Color(0xFF1E1E1E).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .border(1.dp, if (node.isForeground) Color.Transparent else Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = (8 + node.depth * 12).dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (node.isForeground) "FG" else "BG",
                        color = if (node.isForeground) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        fontSize = 8.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(
                            if (node.isForeground) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFFF9800).copy(alpha = 0.2f),
                            RoundedCornerShape(2.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayText, color = if (node.isForeground) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(node.className.substringAfterLast("."), color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (node.isClickable) {
                        Icon(Icons.Default.TouchApp, "Clickable", tint = Color(0xFF2196F3), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandsTab(commands: List<GeneratedCommand>) {
    if (commands.isEmpty()) {
        EmptyState(Icons.Default.Code, "No commands generated yet.\nCommands are created from accessible elements.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(commands) { command ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(4.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text("\"${command.phrase}\"", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Row {
                            Text("→ ${command.action}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            command.targetElement?.let {
                                Text(" on ", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Text(it, color = Color(0xFF2196F3), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
