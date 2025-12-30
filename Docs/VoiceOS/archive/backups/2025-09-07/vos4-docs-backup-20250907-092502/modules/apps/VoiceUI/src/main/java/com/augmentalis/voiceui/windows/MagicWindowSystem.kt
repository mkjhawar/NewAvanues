package com.augmentalis.voiceui.windows

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import com.augmentalis.voiceui.core.MagicUUIDIntegration
import com.augmentalis.voiceui.theme.MagicThemeData
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

/**
 * MagicWindowSystem - Freeform windows with theme inheritance
 * 
 * Features:
 * - 🪟 Draggable, resizable windows
 * - 🎨 Inherits theme from parent
 * - 🆔 UUID tracking for each window
 * - 🎤 Voice command control
 * - ✨ Smooth animations
 * - 📱 Responsive to screen size
 * - 🎯 Window snapping
 * - 📊 Z-order management
 */

// Window Manager Singleton
object MagicWindowManager {
    private val windows = mutableStateMapOf<String, MagicWindowState>()
    private var focusedWindowId by mutableStateOf<String?>(null)
    private var nextZIndex = 1000
    
    fun createWindow(
        id: String = MagicUUIDIntegration.generateComponentUUID("window"),
        title: String = "Magic Window",
        initialPosition: Offset = Offset(100f, 100f),
        initialSize: DpSize = DpSize(400.dp, 300.dp),
        config: MagicWindowConfig = MagicWindowConfig()
    ): String {
        windows[id] = MagicWindowState(
            id = id,
            title = title,
            position = mutableStateOf(initialPosition),
            size = mutableStateOf(initialSize),
            zIndex = nextZIndex++,
            config = config
        )
        focusedWindowId = id
        return id
    }
    
    fun closeWindow(id: String) {
        windows.remove(id)
        if (focusedWindowId == id) {
            focusedWindowId = windows.keys.lastOrNull()
        }
    }
    
    fun focusWindow(id: String) {
        windows[id]?.let { window ->
            window.zIndex = nextZIndex++
            focusedWindowId = id
        }
    }
    
    fun minimizeWindow(id: String) {
        windows[id]?.let { window ->
            window.isMinimized.value = true
        }
    }
    
    fun maximizeWindow(id: String) {
        windows[id]?.let { window ->
            window.isMaximized.value = !window.isMaximized.value
        }
    }
    
    fun getWindow(id: String): MagicWindowState? = windows[id]
    
    fun getAllWindows(): List<MagicWindowState> = windows.values.toList()
    
    fun clearAllWindows() {
        windows.clear()
        focusedWindowId = null
        nextZIndex = 1000
    }
}

// Window State
data class MagicWindowState(
    val id: String,
    val title: String,
    val position: MutableState<Offset>,
    val size: MutableState<DpSize>,
    var zIndex: Int,
    val isMinimized: MutableState<Boolean> = mutableStateOf(false),
    val isMaximized: MutableState<Boolean> = mutableStateOf(false),
    val isDragging: MutableState<Boolean> = mutableStateOf(false),
    val isResizing: MutableState<Boolean> = mutableStateOf(false),
    val config: MagicWindowConfig = MagicWindowConfig()
)

// Window Configuration
data class MagicWindowConfig(
    val resizable: Boolean = true,
    val draggable: Boolean = true,
    val closeable: Boolean = true,
    val minimizable: Boolean = true,
    val maximizable: Boolean = true,
    val alwaysOnTop: Boolean = false,
    val transparency: Float = 1f,
    val showTitleBar: Boolean = true,
    val enableSnapToEdge: Boolean = true,
    val snapThreshold: Dp = 20.dp,
    val minSize: DpSize = DpSize(200.dp, 150.dp),
    val maxSize: DpSize? = null,
    val enableShadow: Boolean = true,
    val enableAnimation: Boolean = true,
    val animationDuration: Int = 300
)

// Main Window Composable
@Composable
fun MagicWindow(
    windowId: String? = null,
    title: String = "Magic Window",
    position: Offset = Offset(100f, 100f),
    size: DpSize = DpSize(400.dp, 300.dp),
    config: MagicWindowConfig = MagicWindowConfig(),
    theme: MagicThemeData? = null,
    onClose: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val id = remember { 
        windowId ?: MagicWindowManager.createWindow(
            title = title,
            initialPosition = position,
            initialSize = size,
            config = config
        )
    }
    
    val windowState = MagicWindowManager.getWindow(id) ?: return
    val currentTheme = theme ?: MagicThemeData.default()
    
    DisposableEffect(id) {
        onDispose {
            if (windowId == null) {
                MagicWindowManager.closeWindow(id)
            }
        }
    }
    
    AnimatedVisibility(
        visible = !windowState.isMinimized.value,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(windowState.position.value.x.toInt(), windowState.position.value.y.toInt()) }
                .size(
                    if (windowState.isMaximized.value) {
                        val configuration = LocalConfiguration.current
                        DpSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)
                    } else {
                        windowState.size.value
                    }
                )
                .zIndex(windowState.zIndex.toFloat())
                .then(
                    if (config.enableShadow) {
                        Modifier.shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(
                                if (windowState.isMaximized.value) 0.dp else currentTheme.cardCornerRadius.dp
                            )
                        )
                    } else Modifier
                )
                .clip(
                    RoundedCornerShape(
                        if (windowState.isMaximized.value) 0.dp else currentTheme.cardCornerRadius.dp
                    )
                )
                .background(
                    currentTheme.cardBackground.copy(
                        alpha = config.transparency
                    )
                )
                .then(
                    if (config.draggable && !windowState.isMaximized.value) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { 
                                    MagicWindowManager.focusWindow(id)
                                    windowState.isDragging.value = true
                                },
                                onDragEnd = {
                                    windowState.isDragging.value = false
                                    if (config.enableSnapToEdge) {
                                        snapToEdges(windowState)
                                    }
                                },
                                onDrag = { _, dragAmount ->
                                    windowState.position.value = Offset(
                                        windowState.position.value.x + dragAmount.x,
                                        windowState.position.value.y + dragAmount.y
                                    )
                                }
                            )
                        }
                    } else Modifier
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    MagicWindowManager.focusWindow(id)
                }
        ) {
            Column {
                // Title Bar
                if (config.showTitleBar) {
                    MagicWindowTitleBar(
                        title = title,
                        windowState = windowState,
                        config = config,
                        theme = currentTheme,
                        onClose = {
                            onClose()
                            MagicWindowManager.closeWindow(id)
                        },
                        onMinimize = {
                            MagicWindowManager.minimizeWindow(id)
                        },
                        onMaximize = {
                            MagicWindowManager.maximizeWindow(id)
                        }
                    )
                }
                
                // Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            if (config.showTitleBar) {
                                PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            } else {
                                PaddingValues(8.dp)
                            }
                        )
                ) {
                    content()
                    
                    // Resize Handle
                    if (config.resizable && !windowState.isMaximized.value) {
                        MagicResizeHandle(
                            windowState = windowState,
                            theme = currentTheme
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MagicWindowTitleBar(
    title: String,
    windowState: MagicWindowState,
    config: MagicWindowConfig,
    theme: MagicThemeData,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        theme.gradientStart.copy(alpha = 0.8f),
                        theme.gradientEnd.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        
        // Window Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (config.minimizable) {
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Minimize",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            if (config.maximizable) {
                IconButton(
                    onClick = onMaximize,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (windowState.isMaximized.value) {
                            Icons.Default.Settings
                        } else {
                            Icons.Default.Settings
                        },
                        contentDescription = "Maximize",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            if (config.closeable) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.MagicResizeHandle(
    windowState: MagicWindowState,
    theme: MagicThemeData
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .size(20.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        windowState.isResizing.value = true
                    },
                    onDragEnd = {
                        windowState.isResizing.value = false
                    },
                    onDrag = { _, dragAmount ->
                        val newSize = DpSize(
                            (windowState.size.value.width.value + dragAmount.x).dp.coerceAtLeast(200.dp),
                            (windowState.size.value.height.value + dragAmount.y).dp.coerceAtLeast(150.dp)
                        )
                        windowState.size.value = newSize
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPath(
                path = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                },
                color = theme.primary.copy(alpha = 0.3f)
            )
            
            // Draw resize grip lines
            val lineColor = theme.primary.copy(alpha = 0.6f)
            drawLine(
                color = lineColor,
                start = Offset(size.width - 4.dp.toPx(), size.height - 12.dp.toPx()),
                end = Offset(size.width - 12.dp.toPx(), size.height - 4.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = lineColor,
                start = Offset(size.width - 4.dp.toPx(), size.height - 8.dp.toPx()),
                end = Offset(size.width - 8.dp.toPx(), size.height - 4.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

// Window Container for managing multiple windows
@Composable
fun MagicWindowContainer(
    modifier: Modifier = Modifier,
    theme: MagicThemeData = MagicThemeData.default(),
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Main content
        content()
        
        // Render all windows
        val windows = remember { MagicWindowManager.getAllWindows() }
        windows.forEach { windowState ->
            key(windowState.id) {
                // Windows are rendered based on their state
                // This is a placeholder - actual window rendering happens via MagicWindow composable
            }
        }
        
        // Minimized windows dock
        MagicMinimizedDock(theme = theme)
    }
}

@Composable
private fun MagicMinimizedDock(
    theme: MagicThemeData
) {
    val minimizedWindows = MagicWindowManager.getAllWindows()
        .filter { it.isMinimized.value }
    
    if (minimizedWindows.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(
                    theme.surface.copy(alpha = 0.9f),
                    RoundedCornerShape(8.dp)
                )
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            minimizedWindows.forEach { window ->
                MinimizedWindowIcon(
                    window = window,
                    theme = theme,
                    onClick = {
                        window.isMinimized.value = false
                        MagicWindowManager.focusWindow(window.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun MinimizedWindowIcon(
    window: MagicWindowState,
    theme: MagicThemeData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(width = 120.dp, height = 32.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = theme.primary.copy(alpha = 0.2f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = window.title,
                fontSize = 12.sp,
                color = theme.textPrimary,
                maxLines = 1
            )
        }
    }
}

// Utility functions
private fun snapToEdges(windowState: MagicWindowState) {
    // Implement edge snapping logic
    val threshold = 20f
    val screenBounds = getScreenBounds()
    
    var newX = windowState.position.value.x
    var newY = windowState.position.value.y
    
    // Snap to left edge
    if (abs(newX) < threshold) {
        newX = 0f
    }
    // Snap to right edge
    if (abs(newX + windowState.size.value.width.value - screenBounds.width) < threshold) {
        newX = screenBounds.width - windowState.size.value.width.value
    }
    // Snap to top edge
    if (abs(newY) < threshold) {
        newY = 0f
    }
    // Snap to bottom edge
    if (abs(newY + windowState.size.value.height.value - screenBounds.height) < threshold) {
        newY = screenBounds.height - windowState.size.value.height.value
    }
    
    windowState.position.value = Offset(newX, newY)
}

private fun getScreenBounds(): Size {
    // This would need proper implementation based on actual screen size
    return Size(1920f, 1080f)
}

// Voice command integration
fun registerWindowVoiceCommands() {
    val commands = mapOf(
        "minimize window" to { windowId: String -> MagicWindowManager.minimizeWindow(windowId) },
        "maximize window" to { windowId: String -> MagicWindowManager.maximizeWindow(windowId) },
        "close window" to { windowId: String -> MagicWindowManager.closeWindow(windowId) },
        "focus window" to { windowId: String -> MagicWindowManager.focusWindow(windowId) }
    )
    
    // Register with voice command system
    commands.forEach { (command, _) ->
        MagicUUIDIntegration.generateVoiceCommandUUID(
            command = command,
            targetUUID = "window_system",
            action = command
        )
    }
}

// Window animations
enum class WindowAnimation {
    FADE,
    SCALE,
    SLIDE,
    BOUNCE,
    MORPH
}

@Composable
fun AnimatedMagicWindow(
    animation: WindowAnimation = WindowAnimation.SCALE,
    windowId: String? = null,
    title: String = "Magic Window",
    position: Offset = Offset(100f, 100f),
    size: DpSize = DpSize(400.dp, 300.dp),
    config: MagicWindowConfig = MagicWindowConfig(),
    theme: MagicThemeData? = null,
    onClose: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val transition = updateTransition(targetState = visible, label = "window")
    
    val alpha by transition.animateFloat(
        label = "alpha",
        transitionSpec = { tween(config.animationDuration) }
    ) { isVisible ->
        if (isVisible) 1f else 0f
    }
    
    val scale by transition.animateFloat(
        label = "scale",
        transitionSpec = { 
            when (animation) {
                WindowAnimation.BOUNCE -> spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
                else -> tween(config.animationDuration)
            }
        }
    ) { isVisible ->
        if (isVisible) 1f else 0.8f
    }
    
    Box(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
    ) {
        MagicWindow(
            windowId = windowId,
            title = title,
            position = position,
            size = size,
            config = config,
            theme = theme,
            onClose = {
                visible = false
                onClose()
            },
            content = content
        )
    }
}

// Window presets for common use cases
object MagicWindowPresets {
    val dialog = MagicWindowConfig(
        resizable = false,
        maximizable = false,
        minSize = DpSize(300.dp, 200.dp)
    )
    
    val toolWindow = MagicWindowConfig(
        alwaysOnTop = true,
        maximizable = false,
        transparency = 0.95f,
        minSize = DpSize(250.dp, 400.dp)
    )
    
    val fullFeature = MagicWindowConfig(
        resizable = true,
        draggable = true,
        closeable = true,
        minimizable = true,
        maximizable = true,
        enableSnapToEdge = true
    )
    
    val notification = MagicWindowConfig(
        resizable = false,
        draggable = false,
        minimizable = false,
        maximizable = false,
        showTitleBar = false,
        transparency = 0.9f,
        enableShadow = true
    )
    
    val palette = MagicWindowConfig(
        alwaysOnTop = true,
        resizable = false,
        maximizable = false,
        transparency = 0.95f,
        minSize = DpSize(200.dp, 300.dp)
    )
}