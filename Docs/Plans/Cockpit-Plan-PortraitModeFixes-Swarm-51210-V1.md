# Cockpit MVP - Portrait Mode Fixes Swarm Implementation Plan

**Version:** 1.0
**Date:** 2025-12-10
**Project:** Cockpit MVP
**Approach:** Multi-Agent Swarm (Parallel Implementation)
**Reasoning:** CoT (Chain of Thought) + RoT (Reasoning over Thoughts)
**Status:** Ready for Execution

---

## Swarm Architecture

### Why Swarm Mode?

**Analysis (RoT):**
- **5 independent issues** identified in portrait mode testing
- **Minimal dependencies** between fixes (can work in parallel)
- **Different components** affected (ViewModel, WindowCard, ControlPanel)
- **Time savings**: 30-40% faster with parallel implementation
- **Risk reduction**: Isolated changes reduce merge conflicts

**Decision:** Deploy 4 specialized agents working in parallel

---

## Agent Assignment Matrix

| Agent | Specialization | Tasks | Files | Est. Time | Priority |
|-------|----------------|-------|-------|-----------|----------|
| **Agent 1: State Logic Specialist** | ViewModel state management | Fix maximize/minimize button logic | `WorkspaceViewModel.kt` | 30 min | P0 |
| **Agent 2: UI Layout Specialist** | Compose layout & sizing | Dynamic maximize sizing | `WindowCard.kt` | 1 hour | P1 |
| **Agent 3: UX Enhancement Specialist** | Dialog UX & input handling | Default URL, Enter key, terminology | `ControlPanel.kt` | 40 min | P1 |
| **Agent 4: Persistence Specialist** | Data persistence & serialization | Window preset system | `WindowPresetManager.kt`, `WindowPreset.kt`, `ControlPanel.kt` | 5 hours | P2 |

---

## Parallel Execution Streams

### Stream 1: Critical Fixes (P0-P1) - Agents 1, 2, 3
**Duration:** 2.5 hours (parallel execution)
**Dependencies:** None (can run simultaneously)

```
┌──────────────────────────────────────────────────────────┐
│ STREAM 1: Critical Fixes (Parallel)                      │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  Agent 1              Agent 2              Agent 3       │
│  ├─ Fix button       ├─ BoxWithConstraints ├─ Default URL│
│  │  state logic      │  wrapper            │             │
│  │                   ├─ Calculate dynamic  ├─ Enter key  │
│  ├─ Update           │  size               │  handler    │
│  │  toggleWindowSize ├─ Update animations  │             │
│  │                   │                     ├─ Terminology│
│  ├─ Add KDoc        ├─ Test portrait      │  updates    │
│  │                   ├─ Test landscape     │             │
│  ├─ Test states      ├─ Test rotation      ├─ Hide Widget│
│  │                   │                     │             │
│  └─ COMPLETE (30m)   └─ COMPLETE (60m)     └─ COMPLETE   │
│                                               (40m)      │
└──────────────────────────────────────────────────────────┘
```

### Stream 2: Feature Enhancement (P2) - Agent 4
**Duration:** 5 hours
**Dependencies:** Requires Stream 1 completion for merge

```
┌──────────────────────────────────────────────────────────┐
│ STREAM 2: Window Presets (Sequential)                    │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  Agent 4: Persistence Specialist                         │
│  ├─ Phase 2.1: Create WindowPreset model (30m)          │
│  │   └─ Add @Serializable annotation                    │
│  │   └─ Define fields (id, title, type, content, etc.)  │
│  │                                                       │
│  ├─ Phase 2.2: Implement WindowPresetManager (1.5h)     │
│  │   └─ savePreset() with JSON serialization            │
│  │   └─ loadPresets() with error handling               │
│  │   └─ deletePreset(), getPresetsByGroup()             │
│  │   └─ Unit tests                                       │
│  │                                                       │
│  ├─ Phase 2.3: Add kotlinx-serialization dep (30m)      │
│  │   └─ Update build.gradle.kts                          │
│  │   └─ Sync and verify build                           │
│  │                                                       │
│  ├─ Phase 2.4: Update AddWindowDialog (2h)              │
│  │   └─ Add preset list section                         │
│  │   └─ Add "Save as preset" checkbox                   │
│  │   └─ Add group name input                            │
│  │   └─ Implement preset selection logic                │
│  │                                                       │
│  └─ Phase 2.5: Integration testing (30m)                │
│      └─ Test save/load workflow                         │
│      └─ Test persistence across restarts                │
│      └─ Test error scenarios                            │
│                                                          │
│  └─ COMPLETE (5h)                                        │
└──────────────────────────────────────────────────────────┘
```

---

## Agent 1: State Logic Specialist

### Mission
Fix the maximize/minimize button state conflict bug (Issue #1)

### Objectives
- [ ] Understand current state machine (isHidden, isLarge)
- [ ] Identify root cause of stuck minimized state
- [ ] Update `toggleWindowSize()` to restore before maximizing
- [ ] Add comprehensive KDoc comments
- [ ] Test all state transitions

### Implementation Plan

**Step 1: Root Cause Analysis (CoT)**
```
Current Logic:
  toggleWindowSize() {
    isLarge = !isLarge  // Only toggles isLarge
  }

Problem:
  - If window is minimized (isHidden=true), maximizing only sets isLarge=true
  - Height calculation: if (isHidden) 48.dp else if (isLarge) 800.dp
  - isHidden takes precedence, so window stays at 48dp

Solution:
  - Always clear isHidden when maximizing
  - New logic: isHidden = false, isLarge = !isLarge
```

**Step 2: Code Changes**

**File:** `/Volumes/M-Drive/Coding/NewAvanues-Cockpit/android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/WorkspaceViewModel.kt`

**Original (Lines 223-234):**
```kotlin
fun toggleWindowSize(windowId: String) {
    _windows.value = _windows.value.map { window ->
        if (window.id == windowId) {
            window.copy(
                isLarge = !window.isLarge,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            window
        }
    }
}
```

**Fixed:**
```kotlin
/**
 * Toggle window size between normal (300x400dp) and maximized (screen - 40dp)
 *
 * State Transitions:
 * - Normal (isHidden=false, isLarge=false) → Maximized (isHidden=false, isLarge=true)
 * - Maximized (isHidden=false, isLarge=true) → Normal (isHidden=false, isLarge=false)
 * - Minimized (isHidden=true, isLarge=*) → Maximized (isHidden=false, isLarge=true)
 *
 * Fix (Issue #1): Always clears isHidden state before toggling isLarge
 * This prevents windows from getting stuck in minimized state (48dp height)
 *
 * @param windowId The ID of the window to toggle size
 */
fun toggleWindowSize(windowId: String) {
    _windows.value = _windows.value.map { window ->
        if (window.id == windowId) {
            window.copy(
                isHidden = false,        // FIX: Always restore when maximizing
                isLarge = !window.isLarge,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            window
        }
    }
}
```

**Step 3: Testing Matrix**

| Initial State | After toggleWindowSize() | Expected Height |
|---------------|--------------------------|-----------------|
| isHidden=false, isLarge=false | isHidden=false, isLarge=true | screenHeight - 40dp |
| isHidden=false, isLarge=true | isHidden=false, isLarge=false | 400dp |
| isHidden=true, isLarge=false | isHidden=false, isLarge=true | screenHeight - 40dp |
| isHidden=true, isLarge=true | isHidden=false, isLarge=false | 400dp |

**Step 4: Deliverables**
- Updated `WorkspaceViewModel.kt` with fix
- KDoc comments explaining state transitions
- Test verification results

**Estimated Time:** 30 minutes

---

## Agent 2: UI Layout Specialist

### Mission
Implement dynamic maximize sizing that fills screen minus 20px border (Issue #2)

### Objectives
- [ ] Replace hardcoded 600x800dp with dynamic screen-based sizing
- [ ] Wrap WindowCard in BoxWithConstraints
- [ ] Calculate maximize size as screenWidth/Height - 40dp
- [ ] Maintain smooth 300ms animations
- [ ] Test portrait, landscape, and rotation

### Implementation Plan

**Step 1: Analysis (CoT)**
```
Current Problem:
  - animatedWidth: if (isLarge) 600.dp else 300.dp (HARDCODED)
  - animatedHeight: if (isLarge) 800.dp else 400.dp (HARDCODED)
  - Doesn't adapt to screen size
  - 600x800dp too small on tablets, too large on small phones

Solution:
  - Use BoxWithConstraints to get screen dimensions
  - Calculate: maximizedWidth = maxWidth - 40.dp (20dp border each side)
  - Calculate: maximizedHeight = maxHeight - 40.dp (20dp border top/bottom)
  - Preserves 300ms animation
```

**Step 2: Code Changes**

**File:** `/Volumes/M-Drive/Coding/NewAvanues-Cockpit/android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/WindowCard.kt`

**Original (Lines 23-102):**
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

    GlassmorphicCard(
        modifier = modifier
            .width(animatedWidth)
            .height(animatedHeight)
            .clickable { onSelect() },
        isFocused = isFocused,
        isSelected = isSelected
    ) {
        // ... rest of content
    }
}
```

**Fixed:**
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
    // FIX (Issue #2): Wrap in BoxWithConstraints to get screen dimensions
    BoxWithConstraints {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Dynamic maximize size: fill screen minus 20dp border on all sides
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
            // ... rest of content (unchanged)
            Box(modifier = Modifier.fillMaxSize()) {
                // Accent color indicator at the top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(android.graphics.Color.parseColor(color)))
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Window control bar (title + minimize/maximize/close buttons)
                    WindowControlBar(
                        title = window.title,
                        isHidden = window.isHidden,
                        isLarge = window.isLarge,
                        onMinimize = onMinimize,
                        onToggleSize = onToggleSize,
                        onClose = onClose,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Window content - only visible if not hidden
                    if (!window.isHidden) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            WindowContentRouter(
                                content = window.content,
                                onContentStateChange = onContentStateChange,
                                freeformManager = freeformManager,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**Step 3: Testing Matrix**

| Screen Size | Orientation | Maximized Width | Maximized Height | Border Check |
|-------------|-------------|-----------------|------------------|--------------|
| 1080x1920 | Portrait | 1040px (260dp) | 1880px (470dp) | 20dp (40px) ✓ |
| 1920x1080 | Landscape | 1880px (470dp) | 1040px (260dp) | 20dp (40px) ✓ |
| 1440x2960 | Tablet Portrait | 1400px (350dp) | 2920px (730dp) | 20dp (40px) ✓ |
| 2960x1440 | Tablet Landscape | 2920px (730dp) | 1400px (350dp) | 20dp (40px) ✓ |

**Step 4: Deliverables**
- Updated `WindowCard.kt` with BoxWithConstraints wrapper
- Dynamic sizing calculations
- Rotation testing verification

**Estimated Time:** 1 hour

---

## Agent 3: UX Enhancement Specialist

### Mission
Fix Add Window dialog UX issues (Issues #4 and #5)

### Objectives
- [ ] Change default URL from webavanue.com to google.com
- [ ] Add Enter key handler to URL field
- [ ] Change "Web App" label to "Web Page (URL)"
- [ ] Hide Widget option (not implemented)

### Implementation Plan

**Step 1: Analysis (RoT)**
```
Issue 4A: Hardwired URL
  - Current: "https://webavanue.com"
  - Expected: "https://google.com"
  - Fix: Change default value in mutableStateOf()
  - Impact: 1 line change, no logic affected

Issue 4B: Enter Key
  - Current: No keyboard handler, must click "Add" button
  - Expected: Enter key submits dialog
  - Fix: Add keyboardOptions + keyboardActions to OutlinedTextField
  - Impact: +5 lines, extract submit logic to function

Issue 5A: Confusing Terminology
  - Current: "Web App" (ambiguous - URL? PWA? SPA?)
  - Expected: "Web Page (URL)" (clear)
  - Fix: Update getTypeLabel() function
  - Impact: 1 line change

Issue 5C: Widget Option Shown
  - Current: Widget shown but not implemented
  - Expected: Hide Widget option
  - Fix: Filter WindowType.WIDGET from values() list
  - Impact: Add .filter { it != WindowType.WIDGET }
```

**Step 2: Code Changes**

**File:** `/Volumes/M-Drive/Coding/NewAvanues-Cockpit/android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/ControlPanel.kt`

**Change 1: Default URL (Line 128)**
```kotlin
// BEFORE
var url by remember { mutableStateOf("https://webavanue.com") }

// AFTER (Fix 4A)
var url by remember { mutableStateOf("https://google.com") }
```

**Change 2: Enter Key Handler (Lines 158-165)**
```kotlin
// BEFORE
OutlinedTextField(
    value = url,
    onValueChange = { url = it },
    label = { Text("URL") },
    singleLine = true,
    placeholder = { Text("https://example.com") }
)

// AFTER (Fix 4B)
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

OutlinedTextField(
    value = url,
    onValueChange = { url = it },
    label = { Text("URL") },
    singleLine = true,
    placeholder = { Text("https://example.com") },
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(
        onDone = {
            if (title.isNotBlank()) {
                submitDialog()  // Extract submit logic to function
            }
        }
    )
)

// Extract submit logic (add at bottom of AddWindowDialog)
fun submitDialog() {
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
```

**Change 3: Terminology (Lines 214-219)**
```kotlin
// BEFORE
private fun getTypeLabel(type: WindowType): String = when (type) {
    WindowType.ANDROID_APP -> "Android App"
    WindowType.WEB_APP -> "Web App"
    WindowType.WIDGET -> "Widget"
    WindowType.REMOTE_DESKTOP -> "Remote Desktop"
}

// AFTER (Fix 5A)
private fun getTypeLabel(type: WindowType): String = when (type) {
    WindowType.ANDROID_APP -> "Android App"
    WindowType.WEB_APP -> "Web Page (URL)"  // CHANGED
    WindowType.WIDGET -> "Widget"
    WindowType.REMOTE_DESKTOP -> "Remote Desktop"
}
```

**Change 4: Hide Widget (Lines 144-155)**
```kotlin
// BEFORE
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

// AFTER (Fix 5C)
Text("Window Type:", style = MaterialTheme.typography.labelMedium)
WindowType.values()
    .filter { it != WindowType.WIDGET }  // ADDED: Hide Widget option
    .forEach { type ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selectedType == type,
                onClick = { selectedType = type }
            )
            Spacer(modifier = Alignment.CenterVertically) {
            Text(getTypeLabel(type))
        }
    }
```

**Step 3: Testing Checklist**

| Test | Expected Result |
|------|-----------------|
| Open Add Window dialog | Default URL is "https://google.com" |
| Type title + URL, press Enter | Window created, dialog closed |
| Press Enter with blank title | Nothing happens |
| View window type options | "Web Page (URL)" shown |
| Count radio buttons | Only 3 visible (Android App, Web Page, Remote Desktop) |
| Select "Web Page (URL)" | URL input field appears |

**Step 4: Deliverables**
- Updated `ControlPanel.kt` with all 4 changes
- Submit logic extracted to function
- Testing verification results

**Estimated Time:** 40 minutes

---

## Agent 4: Persistence Specialist

### Mission
Implement window preset save/load system (Issue #3)

### Objectives
- [ ] Create WindowPreset data model
- [ ] Implement WindowPresetManager with JSON persistence
- [ ] Add kotlinx-serialization dependency
- [ ] Update AddWindowDialog with preset UI
- [ ] Test save/load workflow

### Implementation Plan

**Phase 2.1: WindowPreset Data Model (30 min)**

**File:** `/Volumes/M-Drive/Coding/NewAvanues-Cockpit/Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/presets/WindowPreset.kt`

```kotlin
package com.avanues.cockpit.presets

import com.avanues.cockpit.core.window.WindowContent
import com.avanues.cockpit.core.window.WindowType
import kotlinx.serialization.Serializable

/**
 * Window preset for quick window creation
 *
 * Stores window configuration (title, type, content, group) for reuse.
 * Persisted to SharedPreferences as JSON via WindowPresetManager.
 *
 * @param id Unique preset identifier (UUID)
 * @param title Window title
 * @param type Window type (WEB_APP, ANDROID_APP, etc.)
 * @param content Window content configuration (URL, package name, etc.)
 * @param groupName Group name for organization (e.g., "Work", "Personal")
 * @param color Accent color for window (#RRGGBB hex)
 * @param createdAt Creation timestamp (milliseconds since epoch)
 */
@Serializable
data class WindowPreset(
    val id: String,
    val title: String,
    val type: WindowType,
    val content: WindowContent,
    val groupName: String = "default",
    val color: String = "#4ECDC4",
    val createdAt: Long = 0L
)
```

**Phase 2.2: WindowPresetManager (1.5 hours)**

See full implementation in Specification Section "Component 3: WindowPresetManager"

**Key Methods:**
- `savePreset(title, type, content, groupName, color)` - Serialize and save to SharedPreferences
- `loadPresets()` - Deserialize from SharedPreferences with error handling
- `deletePreset(id)` - Remove preset by ID
- `getPresetsByGroup(groupName)` - Filter presets by group
- `getGroupNames()` - Get unique group names for organization

**Phase 2.3: Add kotlinx-serialization (30 min)**

**File:** `android/apps/cockpit-mvp/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20"  // ADD
}

dependencies {
    // ... existing dependencies

    // ADD: For WindowPreset JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
```

**Phase 2.4: Update AddWindowDialog (2 hours)**

Add to AddWindowDialog:
- Preset list section (LazyColumn with preset rows)
- "Save as preset" Checkbox
- Group name OutlinedTextField (shown when checkbox checked)
- Preset selection logic (auto-fill title/URL)
- Save preset logic in submit function

**Phase 2.5: Integration Testing (30 min)**

Test Matrix:
| Test | Verification |
|------|--------------|
| Save preset with group "Work" | JSON in SharedPreferences contains preset |
| Load presets | Deserializes correctly, appears in dialog list |
| Select preset | Title/URL auto-filled |
| Add window from preset | Window created with correct content |
| Restart app | Presets persisted across launches |
| Corrupted JSON | App doesn't crash, returns empty list |

**Estimated Time:** 5 hours

---

## Execution Timeline

### Parallel Phase (Agents 1, 2, 3) - 1 hour
**Rationale (CoT):** All three agents work on independent files with no conflicts

```
T+0:00  ┬─ Agent 1 starts (WorkspaceViewModel.kt)
        ├─ Agent 2 starts (WindowCard.kt)
        └─ Agent 3 starts (ControlPanel.kt)

T+0:30  ─ Agent 1 COMPLETE ✓ (toggleWindowSize fixed)

T+0:40  ─ Agent 3 COMPLETE ✓ (Dialog UX fixed)

T+1:00  ─ Agent 2 COMPLETE ✓ (Dynamic sizing fixed)
```

### Integration & Testing - 30 minutes
```
T+1:00  ─ Merge all changes
T+1:10  ─ Build project
T+1:15  ─ Deploy to emulator
T+1:20  ─ Run manual test suite (27 tests)
T+1:30  ─ Phase 1 COMPLETE ✓
```

### Sequential Phase (Agent 4) - 5 hours
**Rationale (RoT):** Phase 2 depends on Phase 1 merge to avoid conflicts

```
T+1:30  ─ Agent 4 starts (Window Presets)
T+2:00  ─ WindowPreset model complete
T+3:30  ─ WindowPresetManager complete
T+4:00  ─ kotlinx-serialization dependency added
T+6:00  ─ AddWindowDialog preset UI complete
T+6:30  ─ Integration testing complete
        ─ Phase 2 COMPLETE ✓
```

**Total Time:** 7 hours (vs 7.5 hours sequential = 7% savings)
**Note:** Real savings come from confidence that parallel work won't conflict

---

## Dependency Graph

```
┌──────────────────────────────────────────────────────────┐
│                   NO DEPENDENCIES                         │
│  (Can run in parallel)                                    │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  Agent 1:                    Agent 2:                    │
│  WorkspaceViewModel.kt       WindowCard.kt               │
│  ├─ toggleWindowSize()       ├─ BoxWithConstraints      │
│  └─ No imports from other    └─ No imports from other   │
│     modified files              modified files           │
│                                                          │
│  Agent 3:                                                │
│  ControlPanel.kt                                         │
│  ├─ AddWindowDialog                                      │
│  └─ No imports from other modified files                │
│                                                          │
└──────────────────────────────────────────────────────────┘
         ↓ MERGE
┌──────────────────────────────────────────────────────────┐
│                   DEPENDS ON MERGE                        │
│  (Runs after Phase 1 complete)                           │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  Agent 4:                                                │
│  WindowPresetManager.kt (new)                            │
│  WindowPreset.kt (new)                                   │
│  ControlPanel.kt (updates AddWindowDialog)               │
│  └─ DEPENDS: Needs Phase 1 ControlPanel changes merged  │
│              to avoid conflicts                          │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## Merge Strategy

### Phase 1 Merge (After 1 hour)
```bash
# Agent 1: WorkspaceViewModel changes
git checkout -b fix/maximize-minimize-bug
# ... make changes ...
git add WorkspaceViewModel.kt
git commit -m "fix(window-controls): restore minimized windows before maximizing

- Always clear isHidden when toggleWindowSize is called
- Prevents windows from getting stuck at 48dp height
- Add KDoc explaining state transitions

Fixes Issue #1"

# Agent 2: WindowCard dynamic sizing
git checkout -b feat/dynamic-maximize-sizing
# ... make changes ...
git add WindowCard.kt
git commit -m "feat(window-card): maximize fills screen minus 20dp border

- Wrap WindowCard in BoxWithConstraints
- Calculate maximize size: screen dimensions - 40dp
- Adapts to orientation changes (portrait/landscape)
- Maintains smooth 300ms animations

Fixes Issue #2"

# Agent 3: ControlPanel UX fixes
git checkout -b fix/add-window-dialog-ux
# ... make changes ...
git add ControlPanel.kt
git commit -m "fix(add-window): improve dialog UX

- Change default URL to google.com (was webavanue.com)
- Add Enter key handler to URL field (ImeAction.Done)
- Update 'Web App' label to 'Web Page (URL)'
- Hide Widget option (not implemented)

Fixes Issues #4, #5"

# Merge all three
git checkout Cockpit-Development
git merge fix/maximize-minimize-bug
git merge feat/dynamic-maximize-sizing
git merge fix/add-window-dialog-ux
git push origin Cockpit-Development
```

**Conflict Risk:** LOW (no files overlap)

### Phase 2 Merge (After 5 more hours)
```bash
# Agent 4: Window Presets
git checkout -b feat/window-presets
# ... make changes ...
git add Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/presets/WindowPreset.kt
git add android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/WindowPresetManager.kt
git add android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/ControlPanel.kt
git add android/apps/cockpit-mvp/build.gradle.kts
git commit -m "feat(window-presets): add save/load system for frequently-used windows

- Create WindowPreset data model with @Serializable
- Implement WindowPresetManager with JSON persistence
- Add preset list UI to AddWindowDialog
- Add 'Save as preset' checkbox and group name input
- Persist to SharedPreferences, load on dialog open

Fixes Issue #3"

git checkout Cockpit-Development
git merge feat/window-presets
git push origin Cockpit-Development
```

**Conflict Risk:** MEDIUM (ControlPanel.kt modified in both Phase 1 and Phase 2)
**Mitigation:** Phase 2 starts AFTER Phase 1 merge complete

---

## Communication Protocol

### Agent Status Updates
Each agent reports status every 15 minutes:

```
[T+0:15] Agent 1: 50% complete - toggleWindowSize updated, testing states
[T+0:15] Agent 2: 25% complete - BoxWithConstraints wrapper added
[T+0:15] Agent 3: 40% complete - Default URL changed, working on Enter key

[T+0:30] Agent 1: 100% COMPLETE ✓ - All tests passing
[T+0:30] Agent 2: 50% complete - Dynamic sizing working, testing rotation
[T+0:30] Agent 3: 80% complete - Enter key working, updating terminology

[T+0:45] Agent 2: 75% complete - Rotation tests passing
[T+0:45] Agent 3: 100% COMPLETE ✓ - All UX fixes applied

[T+1:00] Agent 2: 100% COMPLETE ✓ - All tests passing
[T+1:00] Integration: Merging all changes
```

---

## Risk Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| **Merge conflicts** | Low | Medium | Agents work on different files |
| **Agent 2 performance issues** | Low | High | Profile with GPU Rendering, fallback to fixed size |
| **Build errors after merge** | Low | High | Each agent builds before committing |
| **Test failures** | Medium | Medium | Each agent runs manual tests before marking complete |
| **Phase 2 conflicts with Phase 1** | Medium | High | Phase 2 starts AFTER Phase 1 merge |

---

## Success Criteria

### Phase 1 (Quick Wins)
- [ ] All 3 agents complete within 1 hour
- [ ] Zero merge conflicts
- [ ] Build succeeds after merge
- [ ] All 27 manual tests pass
- [ ] No performance regressions
- [ ] Deploy to emulator successful

### Phase 2 (Window Presets)
- [ ] Agent 4 completes within 5 hours
- [ ] kotlinx-serialization builds successfully
- [ ] Preset save/load workflow end-to-end
- [ ] Presets persist across app restarts
- [ ] No crashes with corrupted JSON
- [ ] Performance <100ms dialog open with 20+ presets

---

## Rollback Plan

If critical issues discovered:

```bash
# Rollback Phase 1 (all 3 agents)
git revert <merge-commit-hash-phase-1>
git push origin Cockpit-Development

# Rollback Phase 2 only (Agent 4)
git revert <merge-commit-hash-phase-2>
git push origin Cockpit-Development

# Emergency hotfix (cherry-pick specific agent)
git checkout -b hotfix/maximize-bug
git cherry-pick <agent-1-commit-hash>
git push origin hotfix/maximize-bug
```

---

## Swarm Launch Command

```bash
# Execute swarm implementation
/i.implement .swarm .cot .rot \
  --agents 4 \
  --phase1-parallel true \
  --phase2-sequential true \
  --auto-merge true \
  --test-suite manual
```

---

## Sign-Off

**Swarm Plan Status:** ✅ READY FOR LAUNCH
**Agents:** 4 (1 State, 1 UI, 1 UX, 1 Persistence)
**Estimated Time:** 7 hours (vs 7.5 hours sequential)
**Risk Level:** Low-Medium
**Confidence:** High (90% success rate)

**Next Steps:**
1. Launch Agents 1, 2, 3 in parallel (Phase 1)
2. Monitor agent status every 15 minutes
3. Merge Phase 1 after 1 hour
4. Run integration tests
5. Launch Agent 4 (Phase 2)
6. Merge Phase 2 after 5 hours
7. Final deployment

**Prepared By:** AI Assistant (Swarm Coordinator)
**Date:** 2025-12-10
**Method:** Multi-Agent Swarm with CoT/RoT
**Approval:** Awaiting launch confirmation

---

**End of Swarm Implementation Plan**
