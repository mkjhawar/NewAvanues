# VoiceOSCoreNG Screen Scanning Analysis Report

**Date:** 2026-01-09
**Version:** 2.0 (Updated with Developer Troubleshooting Features)
**Analysis Method:** Swarm (.swarm) with Chain of Thought (.cot) and Tree of Thought (.tot)
**Agents Used:** 5 parallel exploration agents

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-09 | Initial analysis report |
| 2.0 | 2026-01-09 | Added: Rescan Current App, Rescan Everything, Screen Hash in Hierarchy |

---

## Executive Summary

**Current State:** Screen scanning is **manual-only** - triggered when user opens the slider drawer and taps "Scan App" button. The infrastructure for continuous monitoring exists but is **not activated**.

**Root Cause:** The `handleScreenChange()` method in `VoiceOSAccessibilityService.kt` has auto-exploration code **commented out** (line 237).

---

## 1. Current Implementation Analysis

### 1.1 What Currently Happens

```
User navigates to new screen
    ↓
TYPE_WINDOW_STATE_CHANGED event received
    ↓
onAccessibilityEvent() called (line 201-221)
    ↓
handleScreenChange(packageName) called (line 226-239)
    ↓
commandRegistry.clear()  ← Commands cleared
    ↓
Log "Screen changed to X - ready for exploration"
    ↓
⚠️ STOPS HERE - awaits manual user action
```

### 1.2 Where Continuous Monitoring Should Happen

**File:** `/Volumes/M-Drive/Coding/NewAvanues/android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/VoiceOSAccessibilityService.kt`

**Lines 226-239:**
```kotlin
private fun handleScreenChange(packageName: String?) {
    // Clear the shared command registry
    commandRegistry.clear()

    // Auto-explore new screen (COMMENTED OUT)
    if (packageName != null && packageName != "unknown") {
        Log.d(TAG, "Screen changed to $packageName - ready for exploration")
        // Uncomment to auto-explore on screen change:
        // performExploration()  ← THIS IS DISABLED
    }
}
```

---

## 2. Screen Hashing System

### 2.1 Existing Implementation (Complete)

**File:** `VoiceOSCoreNG/src/commonMain/kotlin/.../functions/ScreenFingerprinter.kt`

| Feature | Status | Location |
|---------|--------|----------|
| SHA-256 hash generation | ✓ | Lines 116-121 |
| Content normalization | ✓ | Lines 129-142 |
| Timestamp placeholders | ✓ | Replaces with `[TIME]` |
| Dynamic content detection | ✓ | Lines 147-161 |
| Structural hashing | ✓ | Lines 40-48 |
| Popup detection | ✓ | Lines 50-73 |

### 2.2 What's Missing

| Feature | Status | Issue |
|---------|--------|-------|
| Hash comparison on screen change | ❌ | Not wired to event handler |
| Database lookup for known screens | ❌ | No `hasScreen(hash)` query |
| App version checking | ❌ | No version-based rehash logic |
| Skip scan for known screens | ❌ | Always scans (when manual) |

---

## 3. Slider Drawer Implementation

### 3.1 Current UI Triggers

**File:** `VoiceOSAccessibilityService.kt` (companion object static methods)

```kotlin
// Called from OverlayService FAB menu
VoiceOSAccessibilityService.exploreCurrentApp()  // "Scan App" button
VoiceOSAccessibilityService.exploreAllApps()     // "Scan All" button
```

**User Flow:**
1. Start OverlayService from MainActivity
2. Floating FAB appears (right edge, draggable)
3. Tap FAB to open drawer menu
4. Select "Scan App" or "Scan All"
5. Manual exploration triggers

---

## 4. Settings System Analysis

### 4.1 Existing Settings

**File:** `VoiceOS/src/.../data/VoiceOSSettingsDataStore.kt`

| Setting | Type | Default | Purpose |
|---------|------|---------|---------|
| `voiceEngine` | String | "Default" | Speech engine selection |
| `continuousListening` | Boolean | **false** | **Exists but unused for scanning** |
| `visualFeedback` | Boolean | true | UI feedback toggle |
| `audioFeedback` | Boolean | true | Sound feedback toggle |

### 4.2 Missing Settings

| Setting Needed | Purpose |
|----------------|---------|
| `continuousScanningEnabled` | Enable/disable auto-scan on screen change |
| `showSliderDrawer` | Developer toggle for slider visibility |

### 4.3 Developer Troubleshooting Actions (NEW)

| Action | Purpose | Scope |
|--------|---------|-------|
| `Rescan Current App` | Clear cached screens for current package only | Single app |
| `Rescan Everything` | Clear ALL cached screens system-wide | All apps |

**Reasoning (.cot):** Two separate actions provide granular control:
- "Rescan Current App" - Quick troubleshooting for specific app issues
- "Rescan Everything" - Nuclear option for major updates or corruption

---

## 5. Required Changes

### 5.1 Phase 1: Enable Continuous Monitoring

**File:** `VoiceOSAccessibilityService.kt`

**Change:** Uncomment and enhance `handleScreenChange()`:

```kotlin
private fun handleScreenChange(packageName: String?) {
    if (!isContinuousScanningEnabled()) {
        commandRegistry.clear()
        return  // Manual mode - wait for user
    }

    serviceScope.launch {
        val screenHash = generateScreenHash()

        // Check if screen is known
        val isKnown = screenHashRepository.hasScreen(screenHash)
        val appVersion = getAppVersion(packageName)
        val storedVersion = screenHashRepository.getAppVersion(screenHash)

        if (isKnown && appVersion == storedVersion) {
            // Load cached commands
            loadCachedCommands(screenHash)
            Log.d(TAG, "Screen known - loaded from cache")
        } else {
            // New or updated screen - full scan
            performExploration()
            screenHashRepository.saveScreen(screenHash, packageName, appVersion)
            Log.d(TAG, "Screen scanned and cached")
        }
    }
}
```

### 5.2 Phase 2: Add Screen Hash Repository

**New File:** `VoiceOSCoreNG/.../persistence/ScreenHashRepository.kt`

```kotlin
interface ScreenHashRepository {
    suspend fun hasScreen(hash: String): Boolean
    suspend fun saveScreen(hash: String, packageName: String, appVersion: String)
    suspend fun getAppVersion(hash: String): String?
    suspend fun getCommandsForScreen(hash: String): List<QuantizedCommand>
    suspend fun saveCommandsForScreen(hash: String, commands: List<QuantizedCommand>)
    suspend fun clearScreen(hash: String)
}
```

### 5.3 Phase 3: Add Settings

**File:** `VoiceOSSettingsDataStore.kt`

Add to `VoiceOSSettings` data class:
```kotlin
data class VoiceOSSettings(
    // Existing
    val voiceEngine: String = "Default",
    val continuousListening: Boolean = false,
    val visualFeedback: Boolean = true,
    val audioFeedback: Boolean = true,
    // NEW
    val continuousScanningEnabled: Boolean = true,  // Default ON
    val showSliderDrawer: Boolean = false,          // Developer setting
    val developerModeEnabled: Boolean = false       // Unlocks developer settings
)
```

### 5.4 Phase 4: Update Settings UI

**File:** `SettingsScreen.kt`

Add new section:
```
┌─────────────────────────────────────────────┐
│ 🔍 Scanning                                 │
├─────────────────────────────────────────────┤
│ ○ Continuous Monitoring           [ON/OFF] │
│   "Auto-scan when screen changes"          │
│                                             │
│ ⚙️ Developer Options (hidden by default)   │
│   ○ Show Slider Drawer             [OFF]   │
│   ─────────────────────────────────────────│
│   ▶ Rescan Current App              [TAP]  │
│     "Clear cache for current app only"     │
│   ▶ Rescan Everything               [TAP]  │
│     "Clear ALL cached screens"             │
└─────────────────────────────────────────────┘
```

**UI Design (.tot Analysis):**
```
Tree of Thought: Developer Actions UI
├── Option A: Single "Rescan" with popup
│   └── Rejected: Extra tap, less direct
├── Option B: Two separate action buttons ✓ SELECTED
│   └── Clear intent, direct action
└── Option C: Dropdown selection
    └── Rejected: Non-standard for actions
```

**Implementation Notes:**
- Both actions are **buttons** (not toggles) - they trigger immediate actions
- Confirmation dialog before "Rescan Everything" (destructive action)
- No confirmation for "Rescan Current App" (limited scope)
- Show toast with count of cleared screens after action

### 5.5 Phase 5: Add Continuous Mode Button

**File:** `OverlayService.kt` FAB Menu

Add button alongside existing Scan App/Scan All:
```kotlin
FabMenuItem(
    icon = Icons.Default.PlayCircle,  // or Icons.Default.Sync
    text = if (isContinuousMode) "Stop Monitoring" else "Start Monitoring",
    iconColor = if (isContinuousMode) Color.Red else Color.Green,
    onClick = {
        toggleContinuousMonitoring()
    }
)
```

### 5.6 Phase 6: Add Screen Hash to Hierarchy Page (NEW)

**File:** `DebugPanel.kt` or `HierarchyScreen.kt`

**Purpose:** Display the current screen's hash in the hierarchy/debug view for troubleshooting.

**UI Design (.tot Analysis):**
```
Tree of Thought: Hash Display Location
├── Option A: Header only
│   └── Hash visible at top
│   └── Con: No additional context
├── Option B: Collapsible Info Section ✓ SELECTED
│   └── Expandable "Screen Info" card
│   └── Pro: Rich context without clutter
└── Option C: Toast on scan
    └── Rejected: Transient, can't copy
```

**Proposed UI:**
```
┌─────────────────────────────────────────────┐
│ 📊 Screen Info                    [▼ EXPAND]│
├─────────────────────────────────────────────┤
│ Hash:     a3f2e1c4b9d7...         [📋 COPY]│
│ Package:  com.instagram.android            │
│ Activity: MainActivity                     │
│ Elements: 47 (32 actionable)               │
│ Scanned:  2026-01-09 14:32:15              │
│ Status:   ✓ Cached / ⚡ New                │
└─────────────────────────────────────────────┘
│                                             │
│ 🌳 Element Hierarchy                        │
│ ├── FrameLayout                            │
│ │   ├── RecyclerView (scrollable)          │
│ │   │   ├── PostCard [VUID: 4ffd8c...]     │
│ ...
```

**Data Model:**
```kotlin
data class ScreenInfo(
    val hash: String,                    // SHA-256 screen fingerprint
    val packageName: String,
    val activityName: String?,
    val elementCount: Int,
    val actionableCount: Int,
    val scannedAt: Long,
    val isCached: Boolean                // true if loaded from DB
)
```

**Implementation:**
```kotlin
// In DebugPanel.kt or HierarchyScreen.kt
@Composable
fun ScreenInfoCard(
    screenInfo: ScreenInfo,
    onCopyHash: (String) -> Unit,
    expanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(expanded) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, "Screen Info")
                Spacer(Modifier.width(8.dp))
                Text("Screen Info", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    "Toggle"
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Hash with copy button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Hash: ", style = MaterialTheme.typography.labelMedium)
                        Text(
                            screenInfo.hash.take(16) + "...",
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onCopyHash(screenInfo.hash) }) {
                            Icon(Icons.Default.ContentCopy, "Copy Hash")
                        }
                    }

                    // Other info rows...
                    InfoRow("Package", screenInfo.packageName)
                    InfoRow("Activity", screenInfo.activityName ?: "Unknown")
                    InfoRow("Elements", "${screenInfo.elementCount} (${screenInfo.actionableCount} actionable)")
                    InfoRow("Scanned", formatTimestamp(screenInfo.scannedAt))
                    InfoRow("Status", if (screenInfo.isCached) "✓ Cached" else "⚡ New")
                }
            }
        }
    }
}
```

**Copy-to-Clipboard Integration:**
```kotlin
val clipboardManager = LocalClipboardManager.current
onCopyHash = { hash ->
    clipboardManager.setText(AnnotatedString(hash))
    // Show toast: "Hash copied to clipboard"
}
```

---

## 6. Data Flow: Proposed Continuous Monitoring

```
Screen Change Event
    ↓
onAccessibilityEvent(TYPE_WINDOW_STATE_CHANGED)
    ↓
handleScreenChange(packageName)
    ↓
┌─────────────────────────────────────────────┐
│ Check: isContinuousScanningEnabled()?       │
└─────────────────┬───────────────────────────┘
                  │
    ┌─────────────┴─────────────┐
    │ NO                        │ YES
    ↓                           ↓
Clear registry              Generate screen hash
Return (manual mode)            ↓
                         Query database
                            ↓
              ┌─────────────┴─────────────┐
              │ KNOWN                     │ NEW
              ↓                           ↓
    Check app version               performExploration()
              ↓                           ↓
    ┌─────────┴─────────┐         Save hash + commands
    │ SAME              │ CHANGED       ↓
    ↓                   ↓         Update registry
Load cached         Clear & rescan      ↓
commands                ↓         Ready for voice
    ↓              performExploration()
Update registry         ↓
    ↓              Save new version
Ready for voice         ↓
                   Update registry
```

---

## 7. Files to Modify

| File | Changes |
|------|---------|
| `VoiceOSAccessibilityService.kt` | Enable auto-exploration, add hash comparison |
| `VoiceOSSettingsDataStore.kt` | Add continuous scanning + developer settings |
| `SettingsViewModel.kt` | Add setters for new settings |
| `SettingsScreen.kt` | Add Scanning section + Developer options |
| `OverlayService.kt` | Add continuous mode toggle button |
| **NEW** `ScreenHashRepository.kt` | Database interface for screen hashes |
| **NEW** `ScreenHashRepositoryImpl.kt` | SQLDelight implementation |
| `VoiceOSCoreNG.kt` | Add `enableContinuousMonitoring(Boolean)` API |
| `DebugPanel.kt` | Add ScreenInfoCard with hash display (NEW) |
| **NEW** `ScreenInfo.kt` | Data class for screen metadata |

### 7.1 New Repository Methods for Rescan Actions

**File:** `ScreenHashRepository.kt`

```kotlin
interface ScreenHashRepository {
    // Existing methods...
    suspend fun hasScreen(hash: String): Boolean
    suspend fun saveScreen(hash: String, packageName: String, appVersion: String)
    suspend fun getAppVersion(hash: String): String?
    suspend fun getCommandsForScreen(hash: String): List<QuantizedCommand>
    suspend fun saveCommandsForScreen(hash: String, commands: List<QuantizedCommand>)
    suspend fun clearScreen(hash: String)

    // NEW: Rescan action support
    suspend fun clearScreensForPackage(packageName: String): Int  // Returns count
    suspend fun clearAllScreens(): Int                             // Returns count
    suspend fun getScreenCount(): Int
    suspend fun getScreenCountForPackage(packageName: String): Int
}
```

---

## 8. Implementation Priority

| Priority | Task | Effort |
|----------|------|--------|
| **P0** | Uncomment `performExploration()` in handleScreenChange | 5 min |
| **P1** | Add screen hash comparison to skip known screens | 2-3 hrs |
| **P2** | Add app version checking for rehash | 1-2 hrs |
| **P3** | Add `continuousScanningEnabled` setting | 1 hr |
| **P4** | Add developer setting for slider visibility | 30 min |
| **P5** | Add continuous mode toggle button in FAB | 30 min |
| **P6** | Add Settings UI for new options | 1 hr |
| **P7** | Add "Rescan Current App" action button | 45 min |
| **P8** | Add "Rescan Everything" action button with confirmation | 45 min |
| **P9** | Add Screen Hash display to Hierarchy Page | 1.5 hrs |

**Total Estimated Effort:** ~10-11 hours

### Priority Groupings

**Core Functionality (P0-P2):** ~4-5 hrs
- Enable continuous monitoring
- Screen hash caching

**Settings Infrastructure (P3-P6):** ~3 hrs
- New settings + UI

**Developer Troubleshooting (P7-P9):** ~3 hrs
- Rescan actions + hash display

---

## 9. Recommendations

### 9.1 Default Behavior
- **Continuous monitoring ON by default** (as requested)
- Slider drawer **hidden by default** (developer setting)
- Known screens **loaded from cache** (performance)
- App version change triggers **full rescan**

### 9.2 Developer Options
- Toggle slider visibility via developer settings
- Option to force rescan known screens
- Debug panel shows scan events + hash matches

### 9.3 Performance Considerations
- Add debounce (300-500ms) for rapid screen changes
- Batch database writes
- Use screen hash as cache key
- LRU cache for frequently accessed screens

---

## 10. Summary

| Aspect | Current | Proposed |
|--------|---------|----------|
| Scan trigger | Manual only (Slider) | Automatic on screen change |
| Screen hash usage | Generated but unused | Compare to skip known screens |
| App version check | None | Rehash on version change |
| Default mode | Manual | Continuous monitoring |
| Slider drawer | Always visible | Developer toggle |
| Settings | 4 options | 7 options (3 new) |
| **Developer Actions** | None | Rescan Current App, Rescan Everything |
| **Hash Visibility** | Hidden | Displayed in Hierarchy Page |

### 10.1 Developer Troubleshooting Features (NEW)

| Feature | Purpose | Implementation |
|---------|---------|----------------|
| **Rescan Current App** | Clear cache for single app | `clearScreensForPackage(packageName)` |
| **Rescan Everything** | Nuclear option - clear all | `clearAllScreens()` with confirmation |
| **Screen Hash Display** | View current screen's fingerprint | ScreenInfoCard in DebugPanel |
| **Copy Hash** | Clipboard integration | One-tap copy for sharing/debugging |

---

## 11. Key Code Locations

### 11.1 Screen Hashing
- `ScreenFingerprinter.kt` - Hash generation (lines 116-121)
- `HashUtils.kt` - Element hashing utilities

### 11.2 VUID System
- `VUIDGenerator.kt` - VUID format: `{pkgHash6}-{typeCode}{hash8}`
- `AndroidUIExecutor.kt` - VUID lookup and click execution (lines 151-162, 320-387)

### 11.3 Command Registry
- `CommandRegistry.kt` - Thread-safe dynamic command storage
- `StaticCommandRegistry.kt` - Predefined system commands
- `ActionCoordinator.kt` - Command execution priority (lines 306-460)

### 11.4 Accessibility Service
- `VoiceOSAccessibilityService.kt` - Main event handler
- `OverlayService.kt` - FAB menu and slider drawer

### 11.5 Settings
- `VoiceOSSettingsDataStore.kt` - DataStore-based persistence
- `SettingsViewModel.kt` - Settings business logic
- `SettingsScreen.kt` - Settings UI

---

## 12. Database Schema Requirements

### 12.1 New Table: `screen_hashes`

```sql
CREATE TABLE screen_hashes (
    hash TEXT PRIMARY KEY,
    package_name TEXT NOT NULL,
    activity_name TEXT,
    app_version TEXT NOT NULL,
    element_count INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX idx_screen_hashes_package ON screen_hashes(package_name);
```

### 12.2 New Table: `screen_commands`

```sql
CREATE TABLE screen_commands (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    screen_hash TEXT NOT NULL,
    vuid TEXT NOT NULL,
    phrase TEXT NOT NULL,
    action_type TEXT NOT NULL,
    confidence REAL DEFAULT 0.0,
    metadata TEXT,
    FOREIGN KEY (screen_hash) REFERENCES screen_hashes(hash) ON DELETE CASCADE
);

CREATE INDEX idx_screen_commands_hash ON screen_commands(screen_hash);
CREATE INDEX idx_screen_commands_vuid ON screen_commands(vuid);
```

---

**End of Report**
