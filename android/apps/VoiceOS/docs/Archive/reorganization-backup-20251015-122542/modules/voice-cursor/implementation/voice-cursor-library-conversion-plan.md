# VVoiceCursor-Library-Conversion-Plan

**Created:** 2025-09-26 16:14:47 IST
**Author:** VOS4 Development Team
**Module:** VoiceCursor
**Type:** Service Conversion Plan
**Status:** Planning

## Overview

Convert VoiceCursor services (VoiceCursorOverlayService and VoiceCursorAccessibilityService) into regular classes while keeping all functionality within the VoiceCursor module. This allows other modules' accessibility services to use cursor functionality without running separate services.

## Current Service Architecture

### Services to Remove
```
/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/service/
├── VoiceCursorOverlayService.kt          # REMOVE: Foreground overlay service (823 lines)
└── VoiceCursorAccessibilityService.kt    # REMOVE: Dedicated accessibility service (325 lines)
```

### Current AndroidManifest.xml Services
```xml
<!-- REMOVE: VoiceCursor Overlay Service -->
<service
    android:name=".service.VoiceCursorOverlayService"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="cursor_overlay" />
</service>

<!-- REMOVE: VoiceCursor Accessibility Service -->
<service
    android:name=".service.VoiceCursorAccessibilityService"
    android:exported="true"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

## Target Class Architecture

### New Manager Classes Structure
```
/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/
├── manager/
│   ├── CursorOverlayManager.kt              # NEW: Replaces VoiceCursorOverlayService
│   └── CursorGestureHandler.kt              # NEW: Replaces VoiceCursorAccessibilityService
├── VoiceCursorAPI.kt                        # NEW: Public API for external modules
├── core/                                    # KEEP: All existing implementation
├── view/                                    # KEEP: All existing UI components
├── helper/                                  # KEEP: All existing utilities
└── filter/                                  # KEEP: All existing filters
```

## Implementation Strategy

### Phase 1: Create Manager Classes

#### 1.1 CursorOverlayManager Class
**File:** `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/manager/CursorOverlayManager.kt`

**Functionality Extracted from VoiceCursorOverlayService:**

| Original Method/Property | Lines | New Location |
|--------------------------|-------|--------------|
| `windowManager`, `cursorView`, `menuComposeView` | 90-92 | Class properties |
| `cursorConfig`, `isOverlayVisible` | 95-97 | Class properties |
| `imuIntegration` | 100 | Class property |
| `initializeCursorOverlay()` | 484-510 | `showCursor()` method |
| `createView()` | 515-573 | Private method |
| `centerCursor()` | 728-730 | Public method |
| `updateConfiguration()` | 743-749 | Public method |
| `removeOverlay()` | 810-822 | `hideCursor()` method |
| `loadCursorConfig()` | 288-324 | Private method |
| Menu management methods | 580-647 | Menu-related methods |

**Implementation:**
```kotlin
/**
 * Manages cursor overlay without being a service
 * Extracted from VoiceCursorOverlayService functionality
 */
class CursorOverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "CursorOverlayManager"
    }

    // Extracted from VoiceCursorOverlayService lines 90-100
    private var windowManager: WindowManager? = null
    private var cursorView: CursorView? = null
    private var menuComposeView: ComposeView? = null
    private var cursorConfig = CursorConfig()
    private var isOverlayVisible = false
    private var isMenuVisible = false
    private var imuIntegration: VoiceCursorIMUIntegration? = null

    // Reference to gesture handler for action dispatch
    private var gestureHandler: CursorGestureHandler? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Initialize overlay manager with accessibility service
     */
    fun initialize(accessibilityService: AccessibilityService): Boolean {
        return try {
            gestureHandler = CursorGestureHandler(accessibilityService)
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            Log.d(TAG, "CursorOverlayManager initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CursorOverlayManager", e)
            false
        }
    }

    /**
     * Show cursor overlay
     * Extracted from VoiceCursorOverlayService.initializeCursorOverlay() (lines 484-510)
     */
    fun showCursor(config: CursorConfig = CursorConfig()): Boolean {
        return try {
            if (isOverlayVisible) return true

            cursorConfig = config
            createView()
            initializeIMU()
            isOverlayVisible = true

            Log.d(TAG, "Cursor overlay shown successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing cursor overlay", e)
            false
        }
    }

    /**
     * Hide cursor overlay
     * Extracted from VoiceCursorOverlayService.removeOverlay() (lines 810-822)
     */
    fun hideCursor(): Boolean {
        return try {
            removeOverlay()
            isOverlayVisible = false
            Log.d(TAG, "Cursor overlay hidden")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding cursor overlay", e)
            false
        }
    }

    /**
     * Center cursor on screen
     * Extracted from VoiceCursorOverlayService.centerCursor() (lines 728-730)
     */
    fun centerCursor(): Boolean {
        return try {
            cursorView?.centerCursor()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error centering cursor", e)
            false
        }
    }

    /**
     * Execute cursor action
     */
    fun executeAction(action: CursorAction, position: CursorOffset?): Boolean {
        val targetPosition = position ?: getCurrentPosition()
        return gestureHandler?.executeAction(action, targetPosition) ?: false
    }

    /**
     * Update cursor configuration
     * Extracted from VoiceCursorOverlayService.updateConfiguration() (lines 743-749)
     */
    fun updateConfiguration(config: CursorConfig): Boolean {
        return try {
            cursorConfig = config
            cursorView?.updateCursorStyle(config)
            imuIntegration?.setSensitivity(config.speed / 10.0f)
            Log.d(TAG, "Configuration updated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating configuration", e)
            false
        }
    }

    /**
     * Get current cursor position
     */
    fun getCurrentPosition(): CursorOffset {
        return cursorView?.getClickPosition() ?: CursorOffset(0f, 0f)
    }

    /**
     * Check if cursor is visible
     */
    fun isVisible(): Boolean = isOverlayVisible

    /**
     * Dispose resources
     */
    fun dispose() {
        removeOverlay()
        imuIntegration?.dispose()
        serviceScope.cancel()
        gestureHandler = null
    }

    // Private methods extracted from VoiceCursorOverlayService

    /**
     * Create cursor view and add to window manager
     * Extracted from VoiceCursorOverlayService.createView() (lines 515-573)
     */
    private fun createView() {
        cursorView = CursorView(context).apply {
            updateCursorStyle(cursorConfig)

            // Set up callbacks
            onMenuRequest = { position ->
                showMenuAtPosition(position)
            }

            onCursorMove = { position ->
                // Update gesture handler with position
                gestureHandler?.updateCursorPosition(position)
            }

            onGazeAutoClick = { position ->
                Log.d(TAG, "Gaze auto-click at (${position.x}, ${position.y})")
                gestureHandler?.executeAction(CursorAction.SINGLE_CLICK, position)
            }
        }

        // Create parent layout - extracted implementation
        val cursorParentLayout = RelativeLayout(context).apply {
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }

        cursorView?.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )

        // Create WindowManager.LayoutParams
        val lp = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT

            layoutInDisplayCutoutMode = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                else -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        cursorParentLayout.addView(cursorView)
        windowManager?.addView(cursorParentLayout, lp)
        cursorView?.startTracking()
    }

    /**
     * Initialize IMU integration
     * Extracted from VoiceCursorOverlayService (lines 490-503)
     */
    private fun initializeIMU() {
        imuIntegration = VoiceCursorIMUIntegration.createModern(context).apply {
            setOnPositionUpdate { _ ->
                cursorView?.let { view ->
                    serviceScope.launch {
                        view.post {
                            // Position update handled internally by View
                        }
                    }
                }
            }
            start()
        }
    }

    /**
     * Remove overlay from window manager
     * Extracted from VoiceCursorOverlayService.removeOverlay() (lines 810-822)
     */
    private fun removeOverlay() {
        cursorView?.let { view ->
            view.stopTracking()
            try {
                windowManager?.removeView(view.parent as ViewGroup)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing cursor view", e)
            }
        }
        cursorView = null
        hideMenu()
    }

    /**
     * Show cursor menu at specified position
     * Extracted from VoiceCursorOverlayService.showMenuAtPosition() (lines 580-614)
     */
    private fun showMenuAtPosition(position: CursorOffset) {
        if (isMenuVisible) {
            hideMenu()
            return
        }

        isMenuVisible = true

        menuComposeView = ComposeView(context).apply {
            setContent {
                MenuView(
                    isVisible = isMenuVisible,
                    position = position,
                    onAction = { action ->
                        executeAction(action, position)
                        hideMenu()
                    },
                    onDismiss = {
                        hideMenu()
                    }
                )
            }
        }

        val menuParams = createMenuLayoutParams()
        windowManager?.addView(menuComposeView, menuParams)

        serviceScope.launch {
            delay(5000)
            hideMenu()
        }
    }

    /**
     * Hide cursor menu
     * Extracted from VoiceCursorOverlayService.hideMenu() (lines 637-647)
     */
    private fun hideMenu() {
        isMenuVisible = false
        menuComposeView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing menu view", e)
            }
        }
        menuComposeView = null
    }

    /**
     * Create layout parameters for menu overlay
     * Extracted from VoiceCursorOverlayService.createMenuLayoutParams() (lines 619-632)
     */
    private fun createMenuLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }
}
```

#### 1.2 CursorGestureHandler Class
**File:** `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/manager/CursorGestureHandler.kt`

**Functionality Extracted from VoiceCursorAccessibilityService:**

| Original Method/Property | Lines | New Location |
|--------------------------|-------|--------------|
| Gesture configuration constants | 61-64 | Class properties |
| `currentCursorPosition` | 67 | Class property |
| `updateCursorPosition()` | 103-105 | Public method |
| `executeAction()` | 110-152 | Public method |
| `performClick()` | 157-179 | Private method |
| `performDoubleClick()` | 184-192 | Private method |
| `performLongPress()` | 197-219 | Private method |
| `startDrag()`, `endDrag()` | 224-270 | Private methods |
| `performScroll()` | 275-303 | Private method |
| `calculateDragDuration()` | 308-314 | Private method |

**Implementation:**
```kotlin
/**
 * Handles cursor gestures using provided accessibility service
 * Extracted from VoiceCursorAccessibilityService functionality
 */
class CursorGestureHandler(private val accessibilityService: AccessibilityService) {

    companion object {
        private const val TAG = "CursorGestureHandler"
    }

    // Extracted from VoiceCursorAccessibilityService lines 61-67
    private val clickDuration = 50L
    private val longPressDuration = 1000L
    private val dragStrokeWidth = 10f
    private val scrollDistance = 200f
    private var currentCursorPosition = CursorOffset(0f, 0f)
    private var dragStartPosition: CursorOffset? = null

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Update current cursor position for gesture dispatch
     * Extracted from VoiceCursorAccessibilityService.updateCursorPosition() (lines 103-105)
     */
    fun updateCursorPosition(position: CursorOffset) {
        currentCursorPosition = position
    }

    /**
     * Execute cursor action at specified position
     * Extracted from VoiceCursorAccessibilityService.executeAction() (lines 110-152)
     */
    fun executeAction(action: CursorAction, position: CursorOffset): Boolean {
        serviceScope.launch {
            try {
                when (action) {
                    CursorAction.SINGLE_CLICK -> performClick(position)
                    CursorAction.DOUBLE_CLICK -> performDoubleClick(position)
                    CursorAction.LONG_PRESS -> performLongPress(position)
                    CursorAction.DRAG_START -> startDrag(position)
                    CursorAction.DRAG_END -> endDrag(position)
                    CursorAction.SCROLL_UP -> performScroll(position, isUp = true)
                    CursorAction.SCROLL_DOWN -> performScroll(position, isUp = false)
                    CursorAction.CENTER_CURSOR -> {
                        Log.d(TAG, "Center cursor action - delegated to cursor view")
                    }
                    CursorAction.HIDE_CURSOR -> {
                        Log.d(TAG, "Hide cursor action - delegated to cursor view")
                    }
                    CursorAction.TOGGLE_COORDINATES -> {
                        Log.d(TAG, "Toggle coordinates action - delegated to cursor view")
                    }
                    else -> {
                        Log.d(TAG, "Action ${action.name} - delegated to cursor view")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing cursor action: ${action.name}", e)
            }
        }
        return true
    }

    /**
     * Perform single click gesture
     * Extracted from VoiceCursorAccessibilityService.performClick() (lines 157-179)
     */
    private fun performClick(position: CursorOffset) {
        val clickPath = Path().apply {
            moveTo(position.x, position.y)
        }

        val clickGesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(clickPath, 0, clickDuration))
            .build()

        val success = accessibilityService.dispatchGesture(clickGesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    Log.d(TAG, "Click gesture completed at (${position.x}, ${position.y})")
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    Log.w(TAG, "Click gesture cancelled")
                }
            }, null)

        if (!success) {
            Log.e(TAG, "Failed to dispatch click gesture")
        }
    }

    /**
     * Perform double click gesture
     * Extracted from VoiceCursorAccessibilityService.performDoubleClick() (lines 184-192)
     */
    private fun performDoubleClick(position: CursorOffset) {
        performClick(position)
        handler.postDelayed({
            performClick(position)
        }, 100)
    }

    /**
     * Perform long press gesture
     * Extracted from VoiceCursorAccessibilityService.performLongPress() (lines 197-219)
     */
    private fun performLongPress(position: CursorOffset) {
        val longPressPath = Path().apply {
            moveTo(position.x, position.y)
        }

        val longPressGesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(longPressPath, 0, longPressDuration))
            .build()

        val success = accessibilityService.dispatchGesture(longPressGesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    Log.d(TAG, "Long press gesture completed at (${position.x}, ${position.y})")
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    Log.w(TAG, "Long press gesture cancelled")
                }
            }, null)

        if (!success) {
            Log.e(TAG, "Failed to dispatch long press gesture")
        }
    }

    /**
     * Start drag operation
     * Extracted from VoiceCursorAccessibilityService.startDrag() (lines 224-228)
     */
    private fun startDrag(position: CursorOffset) {
        dragStartPosition = position
        Log.d(TAG, "Drag started at (${position.x}, ${position.y})")
    }

    /**
     * End drag operation
     * Extracted from VoiceCursorAccessibilityService.endDrag() (lines 233-270)
     */
    private fun endDrag(position: CursorOffset) {
        val startPos = dragStartPosition ?: run {
            Log.w(TAG, "Drag end called without drag start")
            return
        }

        val dragPath = Path().apply {
            moveTo(startPos.x, startPos.y)
            lineTo(position.x, position.y)
        }

        val dragGesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    dragPath,
                    0,
                    calculateDragDuration(startPos, position)
                )
            )
            .build()

        val success = accessibilityService.dispatchGesture(dragGesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    Log.d(TAG, "Drag gesture completed from (${startPos.x}, ${startPos.y}) to (${position.x}, ${position.y})")
                    dragStartPosition = null
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    Log.w(TAG, "Drag gesture cancelled")
                    dragStartPosition = null
                }
            }, null)

        if (!success) {
            Log.e(TAG, "Failed to dispatch drag gesture")
            dragStartPosition = null
        }
    }

    /**
     * Perform scroll gesture
     * Extracted from VoiceCursorAccessibilityService.performScroll() (lines 275-303)
     */
    private fun performScroll(position: CursorOffset, isUp: Boolean) {
        val scrollPath = Path().apply {
            moveTo(position.x, position.y)
            if (isUp) {
                lineTo(position.x, position.y - scrollDistance)
            } else {
                lineTo(position.x, position.y + scrollDistance)
            }
        }

        val scrollGesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(scrollPath, 0, 300))
            .build()

        val success = accessibilityService.dispatchGesture(scrollGesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    val direction = if (isUp) "up" else "down"
                    Log.d(TAG, "Scroll $direction gesture completed at (${position.x}, ${position.y})")
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    Log.w(TAG, "Scroll gesture cancelled")
                }
            }, null)

        if (!success) {
            Log.e(TAG, "Failed to dispatch scroll gesture")
        }
    }

    /**
     * Calculate drag duration based on distance
     * Extracted from VoiceCursorAccessibilityService.calculateDragDuration() (lines 308-314)
     */
    private fun calculateDragDuration(start: CursorOffset, end: CursorOffset): Long {
        val distance = kotlin.math.sqrt(
            (end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)
        )
        return (200 + distance.toLong()).coerceAtMost(2000)
    }

    /**
     * Check if gesture dispatch is available
     */
    fun isGestureDispatchAvailable(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        serviceScope.cancel()
    }
}
```

#### 1.3 Public API Class
**File:** `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/VoiceCursorAPI.kt`

```kotlin
/**
 * Public API for external modules to use VoiceCursor functionality
 * Provides access to cursor features without requiring VoiceCursor services
 */
object VoiceCursorAPI {

    private const val TAG = "VoiceCursorAPI"
    private var overlayManager: CursorOverlayManager? = null

    /**
     * Initialize cursor system with accessibility service and context
     * This must be called before using any other cursor functionality
     */
    fun initialize(context: Context, accessibilityService: AccessibilityService): Boolean {
        return try {
            dispose() // Cleanup any existing instance

            overlayManager = CursorOverlayManager(context).apply {
                initialize(accessibilityService)
            }

            Log.d(TAG, "VoiceCursor API initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VoiceCursor API", e)
            false
        }
    }

    /**
     * Show cursor overlay with optional configuration
     */
    fun showCursor(config: CursorConfig = CursorConfig()): Boolean {
        return overlayManager?.showCursor(config) ?: run {
            Log.w(TAG, "VoiceCursor API not initialized")
            false
        }
    }

    /**
     * Hide cursor overlay
     */
    fun hideCursor(): Boolean {
        return overlayManager?.hideCursor() ?: run {
            Log.w(TAG, "VoiceCursor API not initialized")
            false
        }
    }

    /**
     * Center cursor on screen
     */
    fun centerCursor(): Boolean {
        return overlayManager?.centerCursor() ?: run {
            Log.w(TAG, "VoiceCursor API not initialized")
            false
        }
    }

    /**
     * Execute cursor action at specified position
     */
    fun executeAction(action: CursorAction, position: CursorOffset? = null): Boolean {
        return overlayManager?.executeAction(action, position) ?: run {
            Log.w(TAG, "VoiceCursor API not initialized")
            false
        }
    }

    /**
     * Update cursor configuration
     */
    fun updateConfiguration(config: CursorConfig): Boolean {
        return overlayManager?.updateConfiguration(config) ?: run {
            Log.w(TAG, "VoiceCursor API not initialized")
            false
        }
    }

    /**
     * Get current cursor position
     */
    fun getCurrentPosition(): CursorOffset? {
        return overlayManager?.getCurrentPosition() ?: run {
            Log.w(TAG, "VoiceCursor API not initialized")
            null
        }
    }

    /**
     * Check if cursor is currently visible
     */
    fun isVisible(): Boolean {
        return overlayManager?.isVisible() ?: false
    }

    /**
     * Check if API is initialized
     */
    fun isInitialized(): Boolean {
        return overlayManager != null
    }

    /**
     * Dispose cursor system and cleanup resources
     */
    fun dispose() {
        overlayManager?.dispose()
        overlayManager = null
        Log.d(TAG, "VoiceCursor API disposed")
    }
}
```

### Phase 2: Remove Original Services

#### 2.1 Delete Service Files
```bash
# Remove the service files
rm /modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/service/VoiceCursorOverlayService.kt
rm /modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/service/VoiceCursorAccessibilityService.kt
```

#### 2.2 Update AndroidManifest.xml
**File:** `/modules/apps/VoiceCursor/src/main/AndroidManifest.xml`

**Remove service declarations:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:tools="http://schemas.android.com/tools"
    xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Keep permissions that are still needed -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.HIGH_SAMPLING_RATE_SENSORS" tools:ignore="HighSamplingRate" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <!-- Keep hardware features -->
    <uses-feature android:name="android.hardware.sensor.accelerometer" android:required="false" />
    <uses-feature android:name="android.hardware.sensor.gyroscope" android:required="false" />

    <application>
        <!-- REMOVED: VoiceCursorOverlayService -->
        <!-- REMOVED: VoiceCursorAccessibilityService -->

        <!-- Keep Settings Activity if still needed -->
        <activity
            android:name=".ui.VoiceCursorSettingsActivity"
            android:exported="true"
            android:theme="@style/AppTheme.Settings"
            android:label="@string/settings_title">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### Phase 3: Integration with VoiceAccessibility

#### 3.1 Add VoiceCursor Dependency
**File:** `/modules/apps/VoiceAccessibility/build.gradle.kts`

```kotlin
dependencies {
    // Add VoiceCursor dependency for cursor functionality
    implementation(project(":modules:apps:VoiceCursor"))

    // ... existing dependencies
}
```

#### 3.2 Integrate in VoiceAccessibility Service
**File:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/VoiceOSService.kt`

**Add imports:**
```kotlin
import com.augmentalis.voiceos.cursor.VoiceCursorAPI
import com.augmentalis.voiceos.cursor.core.CursorConfig
import com.augmentalis.voiceos.cursor.core.CursorType
import com.augmentalis.voiceos.cursor.core.CursorOffset
import com.augmentalis.voiceos.cursor.view.CursorAction
```

**Modify VoiceOSService class:**
```kotlin
class VoiceOSService : VoiceAccessibilityService() {

    // Remove existing cursor implementation (lines 101-103)
    // private val cursorManagerInstance by lazy { ... }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Initialize cursor functionality
        VoiceCursorAPI.initialize(this, this)

        // Show cursor if enabled in settings
        val sharedPrefs = getSharedPreferences("voice_cursor_prefs", Context.MODE_PRIVATE)
        if (sharedPrefs.getBoolean("cursor_enabled", false)) {
            val config = loadCursorConfigFromPrefs(sharedPrefs)
            VoiceCursorAPI.showCursor(config)
        }

        // Existing initialization...
        config = ServiceConfiguration.loadFromPreferences(this)
        // ... rest of existing initialization
    }

    // Add cursor command handling
    private fun handleCursorVoiceCommand(command: String) {
        when (command.lowercase().trim()) {
            "show cursor" -> VoiceCursorAPI.showCursor()
            "hide cursor" -> VoiceCursorAPI.hideCursor()
            "center cursor" -> VoiceCursorAPI.centerCursor()
            "cursor click" -> {
                VoiceCursorAPI.getCurrentPosition()?.let { position ->
                    VoiceCursorAPI.executeAction(CursorAction.SINGLE_CLICK, position)
                }
            }
            "cursor double click" -> {
                VoiceCursorAPI.getCurrentPosition()?.let { position ->
                    VoiceCursorAPI.executeAction(CursorAction.DOUBLE_CLICK, position)
                }
            }
            "cursor long press" -> {
                VoiceCursorAPI.getCurrentPosition()?.let { position ->
                    VoiceCursorAPI.executeAction(CursorAction.LONG_PRESS, position)
                }
            }
            "cursor scroll up" -> {
                VoiceCursorAPI.getCurrentPosition()?.let { position ->
                    VoiceCursorAPI.executeAction(CursorAction.SCROLL_UP, position)
                }
            }
            "cursor scroll down" -> {
                VoiceCursorAPI.getCurrentPosition()?.let { position ->
                    VoiceCursorAPI.executeAction(CursorAction.SCROLL_DOWN, position)
                }
            }
        }
    }

    // Integrate cursor commands with existing voice command processing
    private fun handleVoiceCommand(command: String, confidence: Float) {
        if (confidence < 0.5f) return

        val normalizedCommand = command.lowercase().trim()

        // Check if it's a cursor command
        if (normalizedCommand.startsWith("cursor ") ||
            normalizedCommand.contains("cursor")) {
            handleCursorVoiceCommand(normalizedCommand)
            return
        }

        // Existing command processing...
        commandCache[normalizedCommand]?.let { cached ->
            if (cached) {
                executeCommand(normalizedCommand)
            }
            return
        }

        // ... rest of existing voice command handling
    }

    private fun loadCursorConfigFromPrefs(sharedPrefs: SharedPreferences): CursorConfig {
        val typeString = sharedPrefs.getString("cursor_type", "Normal") ?: "Normal"
        val type = when (typeString) {
            "Hand" -> CursorType.Hand
            "Custom" -> CursorType.Custom
            else -> CursorType.Normal
        }

        return CursorConfig(
            type = type,
            size = sharedPrefs.getInt("cursor_size", 48),
            color = sharedPrefs.getInt("cursor_color", Color.BLUE),
            speed = sharedPrefs.getInt("cursor_speed", 8),
            gazeClickDelay = if (sharedPrefs.getBoolean("gaze_enabled", false)) {
                sharedPrefs.getLong("gaze_delay", 1500L)
            } else {
                0L
            },
            showCoordinates = sharedPrefs.getBoolean("show_coordinates", false),
            jitterFilterEnabled = sharedPrefs.getBoolean("jitter_filter_enabled", true),
            motionSensitivity = sharedPrefs.getFloat("motion_sensitivity", 0.7f)
        )
    }

    override fun onDestroy() {
        // Dispose cursor resources
        VoiceCursorAPI.dispose()

        // Existing cleanup...
        uiScrapingEngine.destroy()
        appCommandManager.destroy()
        serviceScope.cancel()
        commandCache.clear()
        nodeCache.clear()
        eventDebouncer.clearAll()

        super<VoiceAccessibilityService>.onDestroy()
        Log.i(TAG, "VoiceOSAccessibility destroyed")
    }
}
```

## Benefits of Service-to-Class Conversion

### Resource Efficiency
- ✅ **Single Accessibility Service**: Only VoiceOSService runs
- ✅ **No Foreground Service**: Eliminates VoiceCursorOverlayService overhead
- ✅ **Reduced Memory Usage**: No separate service processes
- ✅ **Lower CPU Usage**: Direct method calls instead of service communication

### Code Organization
- ✅ **Maintained Structure**: All VoiceCursor code stays in VoiceCursor module
- ✅ **Clean Separation**: Services converted to manager classes
- ✅ **Public API**: Clean interface for external modules
- ✅ **Reusable Components**: Any accessibility service can use cursor functionality

### Integration Benefits
- ✅ **Direct Control**: Cursor functionality directly accessible from VoiceOSService
- ✅ **Unified Commands**: Voice and cursor commands in same service
- ✅ **Real-time Communication**: No service binding overhead
- ✅ **Simplified Architecture**: No IPC complexity

## Implementation Timeline

| Phase | Duration | Tasks |
|-------|----------|-------|
| Phase 1 | 2 days | Create manager classes (CursorOverlayManager, CursorGestureHandler, VoiceCursorAPI) |
| Phase 2 | 0.5 day | Remove original services and update AndroidManifest.xml |
| Phase 3 | 1 day | Integrate with VoiceAccessibility service |
| Phase 4 | 0.5 day | Testing and validation |
| **Total** | **4 days** | **Complete service-to-class conversion** |

## Risk Assessment

### Low Risk Items
- ✅ Creating manager classes (standard class creation)
- ✅ Removing services (clean deletion)
- ✅ API class creation (wrapper pattern)

### Medium Risk Items
- ⚠️ Context handling in manager classes
- ⚠️ Lifecycle management without service framework
- ⚠️ Permission handling (SYSTEM_ALERT_WINDOW) in regular classes

### High Risk Items
- 🔴 Window management without service context
- 🔴 IMU integration lifecycle in regular classes
- 🔴 Memory management without service cleanup

### Mitigation Strategies
- **Context Management**: Pass accessibility service context to all managers
- **Lifecycle Handling**: Implement proper initialization and disposal patterns
- **Permission Checks**: Add permission validation in manager classes
- **Testing Strategy**: Comprehensive testing with VoiceAccessibility integration

## Success Metrics

### Technical Success
- ✅ Manager classes build and function correctly
- ✅ All cursor functionality preserved
- ✅ VoiceAccessibility successfully uses cursor features
- ✅ No regression in cursor performance
- ✅ Services successfully removed

### Performance Success
- ✅ Single accessibility service running
- ✅ Reduced memory usage (no separate services)
- ✅ Faster gesture dispatch (direct method calls)
- ✅ No service binding overhead

### Integration Success
- ✅ Voice commands control cursor functionality
- ✅ Cursor settings work through VoiceAccessibility
- ✅ IMU integration functions properly
- ✅ All cursor actions work correctly

---

**Next Steps:**
1. Create `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/manager/` directory
2. Implement CursorOverlayManager class with extracted functionality
3. Implement CursorGestureHandler class with extracted functionality
4. Create VoiceCursorAPI public interface
5. Remove original service files
6. Update AndroidManifest.xml
7. Integrate with VoiceAccessibility service
8. Test all cursor functionality

**Critical Success Factors:**
- Proper context passing to manager classes
- Correct lifecycle management in accessibility service integration
- Thorough testing of all cursor functionality after conversion
- Validation that all gesture dispatch works through host accessibility service