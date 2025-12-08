# VoiceUI Base Implementation Guide

## 🎨 Making VoiceUI the Base UI Layer for VoiceOS

This guide shows how to implement VoiceUI as the foundational UI layer for the entire VoiceOS application, replacing standard Android UI components.

## 📐 Architecture Overview

### Current Android UI Stack (To Replace)
```
┌─────────────────────────────────────┐
│         Android Activities          │
├─────────────────────────────────────┤
│      Compose / View System          │
├─────────────────────────────────────┤
│    Material Design Components       │
├─────────────────────────────────────┤
│      Android Window Manager         │
└─────────────────────────────────────┘
```

### New VoiceUI Base Stack
```
┌─────────────────────────────────────┐
│      VoiceOS Activities             │
├─────────────────────────────────────┤
│      VoiceUI Components             │
│  ┌────────────────────────────┐     │
│  │ • Spatial Windows          │     │
│  │ • Voice-First Navigation   │     │
│  │ • Gesture Controls         │     │
│  │ • HUD Overlays            │     │
│  └────────────────────────────┘     │
├─────────────────────────────────────┤
│      VoiceUI Theme Engine           │
├─────────────────────────────────────┤
│    VoiceUI Window Manager           │
└─────────────────────────────────────┘
```

## 🚀 Implementation Steps

### Step 1: Create VoiceUI Base Activity

```kotlin
// File: app/src/main/java/com/augmentalis/voiceos/ui/base/VoiceUIActivity.kt

package com.augmentalis.voiceos.ui.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.augmentalis.voiceos.VoiceOS
import com.augmentalis.voiceui.VoiceUIModule
import kotlinx.coroutines.launch

/**
 * Base activity that uses VoiceUI as the primary UI layer
 * All app activities should extend this
 */
abstract class VoiceUIActivity : ComponentActivity() {
    
    protected lateinit var voiceUI: VoiceUIModule
    
    // UI State
    private var isVoiceUIReady by mutableStateOf(false)
    private var currentTheme by mutableStateOf("arvision")
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get VoiceUI instance
        voiceUI = (application as? VoiceOS)?.voiceUI 
            ?: throw IllegalStateException("VoiceUI not initialized")
        
        // Initialize VoiceUI for this activity
        lifecycleScope.launch {
            initializeVoiceUI()
            isVoiceUIReady = true
            
            // Set content using VoiceUI
            setContent {
                VoiceUIContent()
            }
        }
    }
    
    private suspend fun initializeVoiceUI() {
        // Ensure VoiceUI is ready
        if (!voiceUI.isReady()) {
            voiceUI.initialize()
        }
        
        // Configure for this activity
        configureVoiceUI()
        
        // Register activity-specific commands
        registerVoiceCommands()
        
        // Setup gesture handlers
        setupGestureHandlers()
    }
    
    protected open fun configureVoiceUI() {
        // Set theme
        voiceUI.themeEngine.setTheme(currentTheme)
        
        // Enable gestures
        voiceUI.gestureManager.enableMultiTouch(true)
        
        // Configure HUD
        voiceUI.hudSystem.setVisible(true)
    }
    
    protected open fun registerVoiceCommands() {
        // Register common commands
        voiceUI.voiceCommandSystem.registerCommand(
            "go back", 
            "com.augmentalis.voiceos.NAVIGATE_BACK"
        )
        
        voiceUI.voiceCommandSystem.registerCommand(
            "go home",
            "com.augmentalis.voiceos.NAVIGATE_HOME"
        )
    }
    
    protected open fun setupGestureHandlers() {
        lifecycleScope.launch {
            voiceUI.gestureManager.gestureFlow.collect { gesture ->
                handleGesture(gesture)
            }
        }
    }
    
    protected open fun handleGesture(gesture: Any) {
        // Override in subclasses
    }
    
    @Composable
    protected open fun VoiceUIContent() {
        // Base VoiceUI layout
        VoiceUITheme(voiceUI.themeEngine) {
            VoiceUIScaffold(
                hudSystem = voiceUI.hudSystem,
                windowManager = voiceUI.windowManager
            ) {
                // Activity-specific content
                ActivityContent()
            }
        }
    }
    
    @Composable
    protected abstract fun ActivityContent()
}
```

### Step 2: Create VoiceUI Composables

```kotlin
// File: app/src/main/java/com/augmentalis/voiceos/ui/components/VoiceUIComponents.kt

package com.augmentalis.voiceos.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.augmentalis.voiceui.theme.ThemeEngine
import com.augmentalis.voiceui.hud.HUDSystem
import com.augmentalis.voiceui.windows.WindowManager

@Composable
fun VoiceUITheme(
    themeEngine: ThemeEngine,
    content: @Composable () -> Unit
) {
    // Apply VoiceUI theme
    themeEngine.VoiceUITheme {
        content()
    }
}

@Composable
fun VoiceUIScaffold(
    hudSystem: HUDSystem,
    windowManager: WindowManager,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content layer
        content()
        
        // HUD overlay layer
        VoiceUIHUD(hudSystem)
        
        // Window layer
        VoiceUIWindows(windowManager)
    }
}

@Composable
fun VoiceUIHUD(hudSystem: HUDSystem) {
    val hudElements by hudSystem.hudElements.collectAsState()
    
    hudElements.forEach { element ->
        // Render HUD elements
        when (element.position) {
            HUDSystem.HUDPosition.TOP_LEFT -> {
                Box(modifier = Modifier.align(Alignment.TopStart)) {
                    element.content()
                }
            }
            // ... other positions
        }
    }
}

@Composable
fun VoiceUIWindows(windowManager: WindowManager) {
    val windows by windowManager.windows.collectAsState()
    
    windows.forEach { window ->
        if (window.isVisible) {
            VoiceUISpatialWindow(window)
        }
    }
}

@Composable
fun VoiceUISpatialWindow(window: WindowManager.Window) {
    // Render spatial window with 3D positioning
    Box(
        modifier = Modifier
            .offset(x = window.x.dp, y = window.y.dp)
            .size(width = window.width.dp, height = window.height.dp)
            .graphicsLayer {
                // Apply Z-depth for spatial effect
                translationZ = window.z
            }
    ) {
        window.content?.invoke()
    }
}
```

### Step 3: Convert MainActivity to VoiceUI

```kotlin
// File: app/src/main/java/com/augmentalis/voiceos/MainActivity.kt

package com.augmentalis.voiceos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceos.ui.base.VoiceUIActivity

class MainActivity : VoiceUIActivity() {
    
    override fun configureVoiceUI() {
        super.configureVoiceUI()
        
        // Main activity specific configuration
        voiceUI.themeEngine.setTheme("arvision")
        
        // Show welcome HUD
        voiceUI.hudSystem.showNotification(
            "Welcome to VoiceOS",
            duration = 3000
        )
    }
    
    override fun registerVoiceCommands() {
        super.registerVoiceCommands()
        
        // Main menu commands
        voiceUI.voiceCommandSystem.registerCommand(
            "open settings",
            "com.augmentalis.voiceos.OPEN_SETTINGS"
        )
        
        voiceUI.voiceCommandSystem.registerCommand(
            "show modules",
            "com.augmentalis.voiceos.SHOW_MODULES"
        )
    }
    
    @Composable
    override fun ActivityContent() {
        // Main screen using VoiceUI components
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VoiceUIHeader()
            Spacer(modifier = Modifier.height(32.dp))
            VoiceUIMainMenu()
        }
    }
    
    @Composable
    private fun VoiceUIHeader() {
        // Spatial header with depth
        VoiceUICard(
            depth = -1f,
            onClick = { 
                voiceUI.windowManager.createSpatialWindow(
                    title = "About",
                    content = { AboutContent() }
                )
            }
        ) {
            Text(
                "VoiceOS",
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
    
    @Composable
    private fun VoiceUIMainMenu() {
        Column(spacing = 16.dp) {
            VoiceUIButton(
                text = "Speech Recognition",
                voiceCommand = "open speech",
                onClick = { openSpeechRecognition() }
            )
            
            VoiceUIButton(
                text = "Device Manager",
                voiceCommand = "open devices",
                onClick = { openDeviceManager() }
            )
            
            VoiceUIButton(
                text = "Settings",
                voiceCommand = "open settings",
                onClick = { openSettings() }
            )
        }
    }
}
```

### Step 4: Create VoiceUI Custom Components

```kotlin
// File: app/src/main/java/com/augmentalis/voiceos/ui/components/VoiceUIWidgets.kt

@Composable
fun VoiceUIButton(
    text: String,
    voiceCommand: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val voiceUI = LocalVoiceUI.current
    
    // Register voice command for this button
    DisposableEffect(voiceCommand) {
        voiceCommand?.let {
            val id = voiceUI.voiceCommandSystem.registerTarget(
                VoiceCommandSystem.VoiceTarget(
                    name = text,
                    type = "button",
                    actions = mapOf("click" to { _ -> onClick() })
                )
            )
            
            onDispose {
                voiceUI.voiceCommandSystem.unregisterTarget(id)
            }
        }
    }
    
    // Gesture-enabled button
    Box(
        modifier = modifier
            .voiceUIGestures(
                onTap = onClick,
                onLongPress = {
                    voiceUI.hudSystem.showNotification(
                        "Say: $voiceCommand",
                        duration = 2000
                    )
                }
            )
    ) {
        Card(
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun VoiceUICard(
    depth: Float = 0f,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .graphicsLayer {
                translationZ = depth * 10f
                shadowElevation = abs(depth) * 8f
            }
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

// Gesture modifier extension
fun Modifier.voiceUIGestures(
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null
): Modifier = composed {
    val voiceUI = LocalVoiceUI.current
    
    pointerInput(Unit) {
        detectGestures(
            onTap = { onTap?.invoke() },
            onLongPress = { onLongPress?.invoke() },
            // Custom swipe detection
        )
    }
}
```

### Step 5: Application-Wide VoiceUI Configuration

```kotlin
// File: app/src/main/java/com/augmentalis/voiceos/VoiceOS.kt
// Add to existing VoiceOS class

class VoiceOS : Application() {
    // ... existing code ...
    
    fun setupVoiceUIAsBase() {
        // Configure VoiceUI as default UI system
        voiceUI.apply {
            // Set default theme
            themeEngine.setTheme("arvision")
            
            // Enable all gesture types
            gestureManager.configure(
                GestureManager.GestureConfig(
                    enableMultiTouch = true,
                    enableForceTouch = true,
                    enableAirTap = true
                )
            )
            
            // Configure notification system to replace Android defaults
            notificationSystem.replaceAndroidDefaults = true
            
            // Setup spatial window management
            windowManager.setSpatialMode(true)
            
            // Initialize HUD for all activities
            hudSystem.setGlobalEnabled(true)
        }
    }
}
```

## 🎯 Key Integration Points

### 1. Replace Android Components

| Android Component | VoiceUI Replacement |
|------------------|-------------------|
| Toast | `voiceUI.notificationSystem.showToast()` |
| Snackbar | `voiceUI.notificationSystem.showSnackbar()` |
| AlertDialog | `voiceUI.notificationSystem.showAlert()` |
| BottomSheet | `voiceUI.notificationSystem.showBottomSheet()` |
| PopupMenu | `voiceUI.notificationSystem.showPopupMenu()` |
| ProgressDialog | `voiceUI.notificationSystem.showProgress()` |
| Activity Transitions | `voiceUI.windowManager` spatial transitions |
| Navigation | Voice commands + gestures |

### 2. Theme Integration

```kotlin
// Replace Material Theme with VoiceUI Theme
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val voiceUI = LocalVoiceUI.current
    voiceUI.themeEngine.VoiceUITheme {
        content()
    }
}
```

### 3. Navigation Pattern

```kotlin
// Voice-first navigation
class VoiceUINavigation {
    fun setupNavigation(navController: NavController) {
        // Register voice commands for navigation
        voiceUI.voiceCommandSystem.registerCommand(
            "go to home",
            { navController.navigate("home") }
        )
        
        // Gesture navigation
        voiceUI.gestureManager.registerPattern(
            "swipe_right",
            { navController.popBackStack() }
        )
    }
}
```

## 📱 Complete Example: Settings Screen

```kotlin
class SettingsActivity : VoiceUIActivity() {
    
    @Composable
    override fun ActivityContent() {
        VoiceUISettingsScreen()
    }
    
    @Composable
    private fun VoiceUISettingsScreen() {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                VoiceUISettingSection("Theme") {
                    ThemeSelector()
                }
            }
            
            item {
                VoiceUISettingSection("Gestures") {
                    GestureSettings()
                }
            }
            
            item {
                VoiceUISettingSection("Voice") {
                    VoiceSettings()
                }
            }
        }
    }
    
    @Composable
    private fun ThemeSelector() {
        val themes = listOf("arvision", "material", "visionos")
        val currentTheme by voiceUI.themeEngine.currentTheme.collectAsState()
        
        themes.forEach { theme ->
            VoiceUIRadioButton(
                text = theme.capitalize(),
                selected = currentTheme == theme,
                voiceCommand = "select $theme theme",
                onClick = {
                    voiceUI.themeEngine.setTheme(theme)
                }
            )
        }
    }
}
```

## 🔄 Migration Strategy

### Phase 1: Hybrid Approach
- Keep existing activities
- Add VoiceUI components gradually
- Test voice commands and gestures

### Phase 2: Full Conversion
- Convert all activities to VoiceUIActivity
- Replace all Android UI components
- Remove Material Design dependencies

### Phase 3: Optimization
- Tune for AR glasses
- Optimize spatial rendering
- Add advanced voice features

## 📊 Benefits of VoiceUI Base

1. **Unified Experience**
   - Consistent UI across all screens
   - Single theme system
   - Centralized gesture handling

2. **Voice-First Design**
   - Every UI element voice-accessible
   - Natural language navigation
   - Hands-free operation

3. **Spatial Computing Ready**
   - 3D window positioning
   - Depth-based layouts
   - AR/VR compatible

4. **Performance**
   - Single UI system
   - Reduced memory footprint
   - Optimized rendering

## 🎮 Advanced Features

### Spatial Layout
```kotlin
@Composable
fun SpatialGrid(
    items: List<Item>,
    depth: Float = -2f
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3)
    ) {
        itemsIndexed(items) { index, item ->
            val itemDepth = depth + (index * 0.1f)
            VoiceUICard(
                depth = itemDepth,
                content = { ItemContent(item) }
            )
        }
    }
}
```

### Voice Macros
```kotlin
// Register complex voice macros
voiceUI.voiceCommandSystem.registerMacro(
    "setup for meeting",
    listOf(
        "mute notifications",
        "dim screen",
        "enable focus mode"
    )
)
```

### Gesture Combinations
```kotlin
voiceUI.gestureManager.registerCombo(
    gestures = listOf(
        GestureType.TWO_FINGER_TAP,
        GestureType.SWIPE_UP
    ),
    action = { openQuickSettings() }
)
```

---
**Implementation Status:** Ready  
**Complexity:** Medium-High  
**Time Estimate:** 2-3 weeks for full conversion  
**Last Updated:** 2025-08-24