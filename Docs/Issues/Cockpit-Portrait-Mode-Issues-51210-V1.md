# Cockpit MVP - Portrait Mode Issues Analysis

**Version:** 1.0
**Date:** 2025-12-10
**Module:** Cockpit MVP (cockpit-mvp)
**Platform:** Android
**Severity:** High (UX/Functionality bugs)
**Status:** Open - Analysis Complete

---

## Executive Summary

User testing in portrait mode revealed 5 critical UX and functionality issues:

1. **Maximize button acts like both maximize and minimize** (Bug)
2. **Maximize should fill screen minus 20px border** (Missing spec)
3. **Window title/URL not persisted for future use** (Missing feature)
4. **Hardwired webavanue.com URL + Enter key not working** (UX issues)
5. **Confusing 'Web App' terminology + missing Android/Widget UI** (UX/Terminology)

**Impact:** Medium-High - These issues significantly degrade the user experience in portrait mode and prevent productive workflow.

**Fix Complexity:** Medium - Most fixes are straightforward, Issue #3 requires architectural work

---

## Issue #1: Maximize/Minimize Button Behavior Bug

### Status
**Severity:** High
**Type:** Logic Bug
**Files:** `WindowControlBar.kt`, `WorkspaceViewModel.kt`, `WindowCard.kt`

### Symptoms
- Clicking **Maximize** button toggles both maximize AND minimize behavior
- Clicking **Minimize** button only minimizes
- After **Minimize** is clicked, **Maximize** button does not restore to normal size
- Window gets stuck in minimized state

### Root Cause Analysis (CoT)

**Step 1: Examine WindowControlBar.kt (Lines 82-96)**
```kotlin
// Maximize/Restore button
IconButton(
    onClick = {
        hapticManager.performMediumTap()
        onToggleSize()  // <-- This only calls toggleWindowSize
    },
    modifier = Modifier.size(40.dp)
) {
    Icon(
        imageVector = if (isLarge) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
        contentDescription = if (isLarge) "Restore window" else "Maximize window",
        tint = if (isLarge) OceanTheme.primary else OceanTheme.textSecondary,
        modifier = Modifier.size(18.dp)
    )
}
```

**Problem:** The maximize button calls `onToggleSize()`, which maps to `viewModel.toggleWindowSize(windowId)`.

**Step 2: Examine WorkspaceViewModel.kt (Lines 223-234)**
```kotlin
fun toggleWindowSize(windowId: String) {
    _windows.value = _windows.value.map { window ->
        if (window.id == windowId) {
            window.copy(
                isLarge = !window.isLarge,  // <-- Only toggles isLarge
                updatedAt = System.currentTimeMillis()
            )
        } else {
            window
        }
    }
}
```

**Problem:** This function ONLY toggles `isLarge`, it does NOT check or modify `isHidden`.

**Step 3: Examine WindowCard.kt (Lines 44-48)**
```kotlin
val animatedHeight by animateDpAsState(
    targetValue = if (window.isHidden) 48.dp else if (window.isLarge) 800.dp else OceanTheme.windowHeightDefault,
    animationSpec = tween(durationMillis = 300),
    label = "window_height"
)
```

**Problem:** The height calculation prioritizes `isHidden` BEFORE `isLarge`.

**Logic Flow:**
1. If `isHidden == true`, height = 48dp (collapsed)
2. Else if `isLarge == true`, height = 800dp (maximized)
3. Else height = default (400dp)

**Bug Scenario:**
1. User clicks **Minimize** → `isHidden = true`, `isLarge = false`
2. Window collapses to 48dp (correct)
3. User clicks **Maximize** → `isLarge = true`, `isHidden = true` (STILL TRUE!)
4. Height calculation: `if (isHidden) 48.dp` → stays at 48dp
5. Window remains collapsed because `isHidden` takes precedence

**Root Cause:** **Maximize button does NOT restore minimized windows. It only toggles `isLarge`, leaving `isHidden = true`.**

### Fix Plan

**Option 1: Maximize button restores minimized windows (Recommended)**
- When `isHidden == true`, maximize button should set `isHidden = false` AND `isLarge = true`
- This matches user expectations: "maximize means show me the full window"

**Option 2: Disable maximize button when minimized**
- When `isHidden == true`, disable maximize button
- User must click minimize again to restore, then maximize
- Less intuitive UX

**Recommended: Option 1**

**Code Changes:**

**File: `WorkspaceViewModel.kt`** (Lines 223-234)
```kotlin
fun toggleWindowSize(windowId: String) {
    _windows.value = _windows.value.map { window ->
        if (window.id == windowId) {
            // If window is minimized, restore it first, then maximize
            val newIsHidden = if (window.isHidden) false else window.isHidden
            val newIsLarge = !window.isLarge

            window.copy(
                isHidden = newIsHidden,  // Restore if minimized
                isLarge = newIsLarge,    // Toggle maximize
                updatedAt = System.currentTimeMillis()
            )
        } else {
            window
        }
    }
}
```

**Alternative: Separate restore logic**
```kotlin
fun toggleWindowSize(windowId: String) {
    _windows.value = _windows.value.map { window ->
        if (window.id == windowId) {
            window.copy(
                isHidden = false,        // Always restore when maximizing
                isLarge = !window.isLarge,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            window
        }
    }
}
```

**Testing:**
1. Minimize window → verify height = 48dp
2. Click maximize → verify window restores to 800dp (large)
3. Click maximize again → verify window returns to 400dp (normal)
4. Minimize → maximize → verify window goes from 48dp → 800dp

---

## Issue #2: Maximize Should Fill Screen Minus 20px Border

### Status
**Severity:** Medium
**Type:** Missing Specification
**Files:** `WindowCard.kt`

### Symptoms
- Maximize button sets window to fixed 600x800dp
- Does not adapt to actual screen size
- User expects "maximize" to mean "fill available space"

### Root Cause Analysis (CoT)

**Current Implementation (WindowCard.kt Lines 39-48):**
```kotlin
val animatedWidth by animateDpAsState(
    targetValue = if (window.isLarge) 600.dp else OceanTheme.windowWidthDefault,
    animationSpec = tween(durationMillis = 300),
    label = "window_width"
)
val animatedHeight by animateDpAsState(
    targetValue = if (window.isHidden) 48.dp else if (window.isLarge) 800.dp else OceanTheme.windowHeightDefault,
    animationSpec = tween(durationMillis = 300),
    label = "window_height"
)
```

**Problem:** Hardcoded 600x800dp does not consider:
- Actual screen dimensions (varies by device)
- Orientation (portrait vs landscape)
- Navigation bar height
- Status bar height
- Safe area insets

**User Expectation:** "Maximize should fill the screen with a small border"

### Fix Plan

**Calculate dynamic maximize size based on screen bounds**

**Code Changes:**

**File: `WindowCard.kt`** (Lines 23-48)
```kotlin
@Composable
fun WindowCard(
    window: AppWindow,
    color: String,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onToggleSize: () -> Unit,
    onSelect: () -> Unit,
    onContentStateChange: (WindowContent) -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    isSelected: Boolean = false,
    freeformManager: FreeformWindowManager? = null
) {
    // Get screen dimensions
    BoxWithConstraints {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Maximize: fill screen minus 20dp border on all sides
        val maximizedWidth = screenWidth - 40.dp
        val maximizedHeight = screenHeight - 40.dp

        // Animated window size based on isLarge state
        val animatedWidth by animateDpAsState(
            targetValue = if (window.isLarge) maximizedWidth else OceanTheme.windowWidthDefault,
            animationSpec = tween(durationMillis = 300),
            label = "window_width"
        )
        val animatedHeight by animateDpAsState(
            targetValue = if (window.isHidden) 48.dp
                else if (window.isLarge) maximizedHeight
                else OceanTheme.windowHeightDefault,
            animationSpec = tween(durationMillis = 300),
            label = "window_height"
        )

        GlassmorphicCard(
            modifier = modifier
                .width(animatedWidth)
                .height(animatedHeight)
                .clickable { onSelect() },
            isFocused = isFocused,
            isSelected = isSelected
        ) {
            // ... rest of WindowCard content
        }
    }
}
```

**Testing:**
1. Portrait mode: Maximize → verify window fills screen minus 20dp border
2. Landscape mode: Maximize → verify window fills screen minus 20dp border
3. Rotate device while maximized → verify window adapts to new orientation
4. Different screen sizes (phone, tablet) → verify 20dp border maintained

---

## Issue #3: Window Title/URL Not Persisted for Future Use

### Status
**Severity:** Medium
**Type:** Missing Feature
**Files:** `ControlPanel.kt`, `WorkspaceViewModel.kt`, New: `WindowPresetManager.kt`

### Symptoms
- User can add windows via "Add Window" dialog
- Window title and URL/package are NOT saved for reuse
- User must re-type title and URL every time
- No way to save "workspace presets" (groups of windows)

### Root Cause Analysis (CoT)

**Current Flow:**
1. User clicks + button
2. AddWindowDialog appears (ControlPanel.kt Lines 121-212)
3. User enters title, selects type, enters URL/package
4. Window is created with `addWindow(title, type, color, content)`
5. **Window is added to workspace StateFlow**
6. **Title/URL/content is NOT persisted anywhere**

**No Persistence Layer:**
- WorkspaceViewModel stores windows in `MutableStateFlow<List<AppWindow>>`
- StateFlow is in-memory only
- No serialization to disk
- No concept of "window presets" or "workspace templates"

**User Expectations:**
1. **Window Presets:** Save frequently-used windows (e.g., "Gmail", "Google Docs", "YouTube")
2. **Workspace Groups:** Save groups of windows (e.g., "Work Setup", "Research Mode", "Entertainment")
3. **Quick Add:** Tap preset to instantly add window without typing

### Fix Plan

**Two-Phase Approach:**

**Phase A: Window Presets (Immediate)**
- Create `WindowPreset` data class
- Add preset management to ViewModel
- Save presets to SharedPreferences or Room database
- Update AddWindowDialog to show "Save as preset" checkbox
- Add "Load Preset" section to AddWindowDialog

**Phase B: Workspace Groups (Future - Phase 6)**
- Create `WorkspaceTemplate` data class
- Save entire workspace configurations
- Add "Save Workspace" and "Load Workspace" buttons
- Serialize to JSON or Protocol Buffers

**Recommended: Implement Phase A now (2-3 hours), Phase B later**

**Code Changes:**

**New File: `Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/presets/WindowPreset.kt`**
```kotlin
package com.avanues.cockpit.presets

import com.avanues.cockpit.core.window.WindowContent
import com.avanues.cockpit.core.window.WindowType

/**
 * Window preset for quick window creation
 * Stores window title, type, and content configuration
 */
data class WindowPreset(
    val id: String,
    val title: String,
    val type: WindowType,
    val content: WindowContent,
    val groupName: String = "default",  // For organizing presets
    val color: String = "#4ECDC4",
    val createdAt: Long = 0L
)
```

**New File: `android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/WindowPresetManager.kt`**
```kotlin
package com.augmentalis.cockpit.mvp

import android.content.Context
import android.content.SharedPreferences
import com.avanues.cockpit.core.window.WindowContent
import com.avanues.cockpit.core.window.WindowType
import com.avanues.cockpit.presets.WindowPreset
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Manages window presets (save/load frequently used windows)
 * Persists to SharedPreferences as JSON
 */
class WindowPresetManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("window_presets", Context.MODE_PRIVATE)
    private val json = Json { prettyPrint = true }

    /**
     * Save a window preset
     */
    fun savePreset(title: String, type: WindowType, content: WindowContent, groupName: String = "default", color: String = "#4ECDC4") {
        val preset = WindowPreset(
            id = UUID.randomUUID().toString(),
            title = title,
            type = type,
            content = content,
            groupName = groupName,
            color = color,
            createdAt = System.currentTimeMillis()
        )

        val presets = loadPresets().toMutableList()
        presets.add(preset)

        val presetsJson = json.encodeToString(presets)
        prefs.edit().putString("presets", presetsJson).apply()
    }

    /**
     * Load all window presets
     */
    fun loadPresets(): List<WindowPreset> {
        val presetsJson = prefs.getString("presets", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<WindowPreset>>(presetsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Delete a preset by ID
     */
    fun deletePreset(id: String) {
        val presets = loadPresets().filter { it.id != id }
        val presetsJson = json.encodeToString(presets)
        prefs.edit().putString("presets", presetsJson).apply()
    }

    /**
     * Get presets by group name
     */
    fun getPresetsByGroup(groupName: String): List<WindowPreset> {
        return loadPresets().filter { it.groupName == groupName }
    }

    /**
     * Get all group names
     */
    fun getGroupNames(): List<String> {
        return loadPresets().map { it.groupName }.distinct().sorted()
    }
}
```

**Update File: `ControlPanel.kt`** (Lines 110-212)
```kotlin
if (showAddDialog) {
    AddWindowDialog(
        presetManager = WindowPresetManager(context),  // Pass preset manager
        onDismiss = { showAddDialog = false },
        onConfirm = { title, type, color, content ->
            onAddWindow(title, type, color, content)
            showAddDialog = false
        }
    )
}

@Composable
private fun AddWindowDialog(
    presetManager: WindowPresetManager,
    onDismiss: () -> Unit,
    onConfirm: (String, WindowType, String, WindowContent) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(WindowType.WEB_APP) }
    var url by remember { mutableStateOf("https://google.com") }  // Changed from webavanue.com
    var packageName by remember { mutableStateOf("com.android.calculator2") }
    var saveAsPreset by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("default") }

    val presets by remember { mutableStateOf(presetManager.loadPresets()) }
    var selectedPreset by remember { mutableStateOf<WindowPreset?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Window") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Preset selection section
                if (presets.isNotEmpty()) {
                    Text("Load Preset:", style = MaterialTheme.typography.labelMedium)
                    presets.forEach { preset ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                selectedPreset = preset
                                title = preset.title
                                selectedType = preset.type
                                when (val content = preset.content) {
                                    is WindowContent.WebContent -> url = content.url
                                    is WindowContent.FreeformAppContent -> packageName = content.packageName
                                    else -> {}
                                }
                            }
                        ) {
                            RadioButton(
                                selected = selectedPreset?.id == preset.id,
                                onClick = { /* handled by row click */ }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${preset.title} (${preset.groupName})")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Manual entry section
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Window Title") },
                    singleLine = true
                )

                Text("Window Type:", style = MaterialTheme.typography.labelMedium)
                WindowType.values().forEach { type ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(getTypeLabel(type))
                    }
                }

                // Type-specific inputs
                if (selectedType == WindowType.WEB_APP) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        singleLine = true,
                        placeholder = { Text("https://example.com") }
                    )
                }

                if (selectedType == WindowType.ANDROID_APP) {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package Name") },
                        singleLine = true,
                        placeholder = { Text("com.example.app") }
                    )
                }

                // Save as preset checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = saveAsPreset,
                        onCheckedChange = { saveAsPreset = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save as preset")
                }

                if (saveAsPreset) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        placeholder = { Text("e.g., Work, Personal, Research") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val color = when (selectedType) {
                            WindowType.ANDROID_APP -> "#FF6B9D"
                            WindowType.WEB_APP -> "#4ECDC4"
                            WindowType.WIDGET -> "#95E1D3"
                            WindowType.REMOTE_DESKTOP -> "#FFD93D"
                        }

                        val content = when (selectedType) {
                            WindowType.WEB_APP -> WindowContent.WebContent(url)
                            WindowType.ANDROID_APP -> WindowContent.FreeformAppContent(packageName)
                            else -> WindowContent.MockContent
                        }

                        // Save as preset if checked
                        if (saveAsPreset) {
                            presetManager.savePreset(title, selectedType, content, groupName, color)
                        }

                        onConfirm(title, selectedType, color, content)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

**Testing:**
1. Add window with "Save as preset" checked → verify preset saved
2. Open dialog again → verify preset appears in list
3. Select preset → verify title/URL auto-populated
4. Add window from preset → verify window created correctly
5. Restart app → verify presets persisted across launches

---

## Issue #4: Hardwired webavanue.com URL + Enter Key Not Working

### Status
**Severity:** Medium
**Type:** UX Issues (2 separate bugs)
**Files:** `ControlPanel.kt`, `WorkspaceViewModel.kt`

### Symptoms

**4A: Hardwired webavanue.com URL**
- Default URL in AddWindowDialog is "https://webavanue.com"
- User expects default to be "https://google.com" or empty
- User wants setting to configure default URL

**4B: Enter key does not execute**
- User types URL and hits Enter/Return
- Nothing happens - must click "Add" button
- Expected: Enter key should submit dialog

### Root Cause Analysis (CoT)

**4A: Hardwired URL (ControlPanel.kt Line 128)**
```kotlin
var url by remember { mutableStateOf("https://webavanue.com") }
```

**Problem:** Hardcoded default URL, no user configuration

**4B: Enter Key (ControlPanel.kt Lines 158-165)**
```kotlin
OutlinedTextField(
    value = url,
    onValueChange = { url = it },
    label = { Text("URL") },
    singleLine = true,
    placeholder = { Text("https://example.com") }
)
```

**Problem:** No `keyboardOptions` or `keyboardActions` to handle Enter key

### Fix Plan

**4A: Change default URL to google.com + add user setting**

**4B: Add Enter key handler to URL field**

**Code Changes:**

**File: `ControlPanel.kt`** (Lines 128, 158-165)

**Fix 4A:**
```kotlin
// Change default URL
var url by remember { mutableStateOf("https://google.com") }  // Changed from webavanue.com
```

**Fix 4B:**
```kotlin
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

// Add Enter key handler to URL field
OutlinedTextField(
    value = url,
    onValueChange = { url = it },
    label = { Text("URL") },
    singleLine = true,
    placeholder = { Text("https://example.com") },
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(
        onDone = {
            // Submit dialog when Enter is pressed
            if (title.isNotBlank()) {
                val color = when (selectedType) {
                    WindowType.ANDROID_APP -> "#FF6B9D"
                    WindowType.WEB_APP -> "#4ECDC4"
                    WindowType.WIDGET -> "#95E1D3"
                    WindowType.REMOTE_DESKTOP -> "#FFD93D"
                }

                val content = when (selectedType) {
                    WindowType.WEB_APP -> WindowContent.WebContent(url)
                    WindowType.ANDROID_APP -> WindowContent.FreeformAppContent(packageName)
                    else -> WindowContent.MockContent
                }

                onConfirm(title, selectedType, color, content)
            }
        }
    )
)
```

**Future Enhancement: User Settings for Default URL**

**New File: `android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/UserSettings.kt`**
```kotlin
package com.augmentalis.cockpit.mvp

import android.content.Context
import android.content.SharedPreferences

/**
 * User settings manager
 * Stores user preferences like default URL, theme, layout
 */
class UserSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

    var defaultUrl: String
        get() = prefs.getString("default_url", "https://google.com") ?: "https://google.com"
        set(value) = prefs.edit().putString("default_url", value).apply()

    var defaultWindowType: String
        get() = prefs.getString("default_window_type", "WEB_APP") ?: "WEB_APP"
        set(value) = prefs.edit().putString("default_window_type", value).apply()
}
```

**Update ControlPanel.kt to use UserSettings:**
```kotlin
val userSettings = remember { UserSettings(context) }
var url by remember { mutableStateOf(userSettings.defaultUrl) }
```

**Testing:**
1. Open AddWindowDialog → verify default URL is google.com
2. Change URL and hit Enter → verify dialog submits
3. (Future) Change default URL in settings → verify new default appears

---

## Issue #5: Confusing 'Web App' Terminology + Missing Android/Widget UI

### Status
**Severity:** Low-Medium
**Type:** UX/Terminology Issues
**Files:** `ControlPanel.kt`

### Symptoms

**5A: "Web App" is confusing**
- User sees "Web App" option in dialog
- User asks: "Do you mean a URL? Is this for PWAs?"
- Terminology is unclear

**5B: How to specify Android app?**
- User selects "Android App" radio button
- Package name field appears (correct)
- User asks: "How do I know the package name?"
- No package picker or app list

**5C: How to add a widget?**
- User selects "Widget" option
- No UI appears - just shows "Mock Content" after adding
- User asks: "How do I actually add a widget?"

### Root Cause Analysis (CoT)

**5A: Terminology Confusion**

Current labels (ControlPanel.kt Lines 214-219):
```kotlin
private fun getTypeLabel(type: WindowType): String = when (type) {
    WindowType.ANDROID_APP -> "Android App"
    WindowType.WEB_APP -> "Web App"
    WindowType.WIDGET -> "Widget"
    WindowType.REMOTE_DESKTOP -> "Remote Desktop"
}
```

**Problem:** "Web App" is ambiguous
- Could mean: URL, PWA, web-based app, SPA
- User expects: "URL" or "Web Page"

**5B: Android App Package Name**

Current UI (ControlPanel.kt Lines 169-177):
```kotlin
if (selectedType == WindowType.ANDROID_APP) {
    OutlinedTextField(
        value = packageName,
        onValueChange = { packageName = it },
        label = { Text("Package Name") },
        singleLine = true,
        placeholder = { Text("com.example.app") }
    )
}
```

**Problem:** User must manually type package name
- Most users don't know package names
- No app picker/browser

**5C: Widget Implementation**

Current behavior:
- Widget type creates window with MockContent
- No actual widget renderer implemented

**Problem:** Feature not implemented, but option is shown

### Fix Plan

**5A: Change "Web App" to "Web Page (URL)"**

**5B: Add app picker button for Android apps**

**5C: Either implement widget support OR hide the option**

**Code Changes:**

**Fix 5A: Better Labels (ControlPanel.kt Lines 214-219)**
```kotlin
private fun getTypeLabel(type: WindowType): String = when (type) {
    WindowType.ANDROID_APP -> "Android App"
    WindowType.WEB_APP -> "Web Page (URL)"  // Changed
    WindowType.WIDGET -> "Widget"
    WindowType.REMOTE_DESKTOP -> "Remote Desktop"
}
```

**Fix 5B: Add App Picker (ControlPanel.kt Lines 169-177)**
```kotlin
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

if (selectedType == WindowType.ANDROID_APP) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // App picker button
        Button(
            onClick = {
                // Show app picker dialog
                val pm = context.packageManager
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }  // Filter out system apps
                    .sortedBy { pm.getApplicationLabel(it).toString() }

                // Show dialog with app list
                // TODO: Implement app picker dialog
            }
        ) {
            Text("Browse Installed Apps")
        }

        // Manual entry fallback
        OutlinedTextField(
            value = packageName,
            onValueChange = { packageName = it },
            label = { Text("Package Name (or browse above)") },
            singleLine = true,
            placeholder = { Text("com.example.app") }
        )
    }
}
```

**Fix 5C: Hide Widget Option (ControlPanel.kt Lines 144-155)**
```kotlin
Text("Window Type:", style = MaterialTheme.typography.labelMedium)
WindowType.values()
    .filter { it != WindowType.WIDGET }  // Hide Widget option until implemented
    .forEach { type ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selectedType == type,
                onClick = { selectedType = type }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(getTypeLabel(type))
        }
    }
```

**Alternative 5C: Add Widget Info**
```kotlin
if (selectedType == WindowType.WIDGET) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Note: Widget support is coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "For now, widgets will show as placeholder content",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
```

**Testing:**
1. Open dialog → verify "Web Page (URL)" label
2. Select Android App → verify "Browse Installed Apps" button appears
3. Click browse button → verify app list shown (future implementation)
4. Select Widget → verify info message OR option hidden

---

## Priority & Implementation Order

| Issue | Priority | Est. Time | Dependencies |
|-------|----------|-----------|--------------|
| **#1: Maximize/Minimize Bug** | P0 (Critical) | 30 min | None |
| **#4B: Enter Key** | P1 (High) | 15 min | None |
| **#4A: Default URL** | P1 (High) | 10 min | None |
| **#2: Maximize Fill Screen** | P1 (High) | 1 hour | None |
| **#5A: Terminology** | P2 (Medium) | 10 min | None |
| **#5C: Hide Widget** | P2 (Medium) | 5 min | None |
| **#3: Window Presets** | P2 (Medium) | 3 hours | kotlinx-serialization |
| **#5B: App Picker** | P3 (Low) | 2 hours | None |

**Recommended Implementation Order:**
1. Fix #1 (30 min) - Critical bug blocking UX
2. Fix #4B + #4A (25 min) - Quick UX wins
3. Fix #2 (1 hour) - Maximize behavior improvement
4. Fix #5A + #5C (15 min) - Terminology cleanup
5. Fix #3 (3 hours) - Feature enhancement (can be separate sprint)
6. Fix #5B (2 hours) - Polish (can be separate sprint)

**Total Time (P0-P2):** ~2.5 hours
**Total Time (All):** ~7 hours

---

## Testing Plan

### Manual Testing

**Test Suite: Portrait Mode Issues**

| Test ID | Description | Expected Result |
|---------|-------------|-----------------|
| PM-1.1 | Minimize window, then maximize | Window goes from 48dp → full screen minus 20px |
| PM-1.2 | Maximize window, then minimize | Window goes from full → 48dp |
| PM-1.3 | Normal → Maximize → Minimize → Maximize | Window restores to full screen |
| PM-2.1 | Maximize in portrait mode | Window fills screen with 20dp border |
| PM-2.2 | Maximize in landscape mode | Window fills screen with 20dp border |
| PM-2.3 | Rotate while maximized | Window adapts to new orientation |
| PM-3.1 | Add window, save as preset | Preset appears in list on next open |
| PM-3.2 | Load preset from list | Title/URL auto-populated |
| PM-3.3 | Restart app, open dialog | Presets persisted |
| PM-4.1 | Open dialog | Default URL is google.com |
| PM-4.2 | Type URL, hit Enter | Dialog submits |
| PM-4.3 | Type title, type URL, hit Enter | Window created |
| PM-5.1 | Open dialog | "Web Page (URL)" label shown |
| PM-5.2 | Select Widget | Option hidden OR info message shown |

---

## Prevention Measures

### Code Review Checklist
- [ ] All button actions restore conflicting states (e.g., maximize clears minimize)
- [ ] Dynamic sizing uses screen dimensions, not hardcoded values
- [ ] User data (presets, settings) persisted to disk
- [ ] All text inputs handle Enter key properly
- [ ] Terminology clear and user-friendly
- [ ] Incomplete features either hidden or marked "Coming Soon"

### Architecture Improvements
- [ ] Add state machine for window states (normal, minimized, maximized)
- [ ] Add persistence layer abstraction (Room database or DataStore)
- [ ] Add settings management system (UserSettings class)
- [ ] Add preset management system (WindowPresetManager class)

### Documentation Updates
- [ ] Update sprint spec with maximize behavior (fill screen minus 20px)
- [ ] Document window state transitions (normal ↔ minimized ↔ maximized)
- [ ] Add user guide for window presets
- [ ] Add developer guide for adding new window types

---

## Related Issues

- **Phase 6: Workspace Persistence** - Will implement full workspace save/load
- **Phase 5: Window Drag & Drop** - Will add window repositioning
- **Legacy Gap #4: Window Numbering** - Will help with voice commands

---

## Sign-Off

**Analysis Status:** ✅ COMPLETE
**Root Causes:** ✅ IDENTIFIED
**Fix Plans:** ✅ DOCUMENTED
**Priority:** ✅ ASSIGNED

**Next Steps:**
1. Review fix plans with stakeholders
2. Implement P0-P1 fixes (1.5 hours)
3. Test in portrait mode on physical device
4. Deploy updated build
5. Implement P2 fixes in next iteration

**Prepared By:** AI Assistant
**Date:** 2025-12-10
**Analysis Method:** CoT (Chain of Thought) + RoT (Reasoning over Thoughts)
**Total Analysis Time:** ~45 minutes

---

**End of Issue Analysis**
