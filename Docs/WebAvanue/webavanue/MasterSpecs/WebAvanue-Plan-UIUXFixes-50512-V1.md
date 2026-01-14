# WebAvanue UI/UX Fixes Implementation Plan

**Version:** 1.0
**Date:** 2025-12-05
**Spec Reference:** WebAvanue-Spec-UIUXFixes-50512-V1.md
**Mode:** YOLO (Auto-implement)

---

## Implementation Tasks

### Phase 1: State Management Fixes (Critical Path)

#### Task 1.1: Fix Command Bar Toggle State Sync (FR-008)
**File:** `BrowserScreen.kt`
**Lines:** 116-125, 368-430

**Changes:**
1. Replace voice mode toggle logic with direct visibility toggle
2. Update AddressBar to pass `isCommandBarVisible` instead of deriving from `isVoiceMode`
3. Change toggle icon to show bar visibility state (not voice mode)

**Code Pattern:**
```kotlin
// Replace mic toggle with visibility toggle
IconButton(onClick = { isCommandBarVisible = !isCommandBarVisible }) {
    Icon(
        imageVector = if (isCommandBarVisible)
            Icons.Default.KeyboardArrowDown
        else
            Icons.Default.KeyboardArrowUp,
        contentDescription = if (isCommandBarVisible) "Hide command bar" else "Show command bar"
    )
}
```

**File:** `AddressBar.kt`
**Lines:** 369-397

**Changes:**
1. Rename `isVoiceMode` param to `isCommandBarVisible`
2. Update toggle icon logic
3. Keep separate mic button for voice activation

---

### Phase 2: Visual Styling Fixes

#### Task 2.1: FAB Glass-Like Style (FR-001)
**File:** `BrowserScreen.kt`
**Lines:** 577-607

**Changes:**
1. Replace `Surface` with `GlassCard` component
2. Use `GlassLevel.MEDIUM` for blur effect
3. Adjust icon color for visibility on glass background

**Code Pattern:**
```kotlin
GlassCard(
    modifier = Modifier
        .size(48.dp)
        .zIndex(10f),
    glassLevel = GlassLevel.MEDIUM,
    shape = CircleShape,
    border = GlassDefaults.borderSubtle
) {
    Icon(
        imageVector = Icons.Default.HelpOutline,
        contentDescription = "Voice commands help",
        tint = OceanTheme.textPrimary,
        modifier = Modifier.padding(12.dp).size(24.dp)
    )
}
```

#### Task 2.2: Tab Switcher Grid/List Icons (FR-005)
**File:** `TabSwitcherView.kt`
**Lines:** 222-258

**Changes:**
1. Create custom icon composables for Grid (2x2 squares) and List (3 horizontal lines)
2. Replace Material icons with custom implementations

**Code Pattern:**
```kotlin
// Custom Grid Icon
@Composable
fun GridViewIcon(tint: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val boxSize = size.width / 2.5f
        val gap = size.width / 8f
        // Draw 4 rounded squares in 2x2 grid
        drawRoundRect(color = tint, topLeft = Offset(0f, 0f), size = Size(boxSize, boxSize), cornerRadius = CornerRadius(2.dp.toPx()))
        drawRoundRect(color = tint, topLeft = Offset(boxSize + gap, 0f), size = Size(boxSize, boxSize), cornerRadius = CornerRadius(2.dp.toPx()))
        drawRoundRect(color = tint, topLeft = Offset(0f, boxSize + gap), size = Size(boxSize, boxSize), cornerRadius = CornerRadius(2.dp.toPx()))
        drawRoundRect(color = tint, topLeft = Offset(boxSize + gap, boxSize + gap), size = Size(boxSize, boxSize), cornerRadius = CornerRadius(2.dp.toPx()))
    }
}
```

---

### Phase 3: Layout Adaptations

#### Task 3.1: VoiceCommandDialog Horizontal Columns (FR-002)
**File:** `VoiceCommandsDialog.kt`
**Lines:** 186-204 (CategoriesView), 259-293 (CommandsView)

**Changes:**
1. Wrap content in `BoxWithConstraints`
2. Calculate `isLandscape` from `maxWidth > maxHeight`
3. Replace `Column` with `LazyVerticalGrid` when in landscape
4. Set fixed columns: `GridCells.Fixed(if (isLandscape) 3 else 1)`

**Code Pattern:**
```kotlin
BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isLandscape = maxWidth > maxHeight
    val columns = if (isLandscape) 3 else 1

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(VoiceCommandCategory.entries) { category ->
            CategoryButton(category = category, onClick = { onCategorySelected(category) })
        }
    }
}
```

#### Task 3.2: CommandItemCard Fixed Width (FR-002 continued)
**File:** `VoiceCommandsDialog.kt`
**Lines:** 298-337

**Changes:**
1. Add fixed minimum width to command badge Surface
2. Ensure consistent sizing across all command types

**Code Pattern:**
```kotlin
Surface(
    modifier = Modifier.widthIn(min = 120.dp),  // Fixed minimum width
    color = OceanTheme.primary,
    shape = MaterialTheme.shapes.small
) { ... }
```

---

### Phase 4: Settings Integration

#### Task 4.1: Add Voice Dialog Auto-Timeout Setting (FR-003)
**File:** `SettingsScreen.kt`

**Changes:**
1. Add "Voice Dialog" subsection under "Voice & AI Settings"
2. Add toggle for `voiceDialogAutoClose`
3. Add slider for `voiceDialogAutoCloseDelayMs` (visible only when toggle is ON)

**Code Pattern:**
```kotlin
// Voice Dialog Settings
SettingItem(
    title = "Auto-close voice dialog",
    subtitle = "Automatically close after executing command",
    trailing = {
        Switch(
            checked = settings.voiceDialogAutoClose,
            onCheckedChange = { viewModel.updateSetting { it.copy(voiceDialogAutoClose = it) } }
        )
    }
)

if (settings.voiceDialogAutoClose) {
    SettingItem(
        title = "Auto-close delay",
        subtitle = "${settings.voiceDialogAutoCloseDelayMs}ms",
        trailing = {
            Slider(
                value = settings.voiceDialogAutoCloseDelayMs.toFloat(),
                onValueChange = { viewModel.updateSetting { it.copy(voiceDialogAutoCloseDelayMs = it.toLong()) } },
                valueRange = 500f..5000f,
                steps = 9
            )
        }
    )
}
```

---

### Phase 5: Feature Additions

#### Task 5.1: Change Star Icon to History (FR-007)
**File:** `AddressBar.kt`
**Lines:** 263-280 (portrait), 537-555 (landscape)

**Changes:**
1. Change `Icons.Default.Star` to `Icons.Default.History`
2. Change `onClick` from `onAddFavorite` to `onHistoryClick`
3. Change `onLongClick` to show recent history dropdown
4. Update content description

**Code Pattern:**
```kotlin
Box(
    modifier = Modifier
        .size(24.dp)
        .clip(RoundedCornerShape(6.dp))
        .combinedClickable(
            onClick = onHistoryClick,  // Navigate to History
            onLongClick = { showRecentHistoryDropdown = true }  // Show recent items
        ),
    contentAlignment = Alignment.Center
) {
    Icon(
        imageVector = Icons.Default.History,
        contentDescription = "History",
        tint = OceanTheme.textSecondary,
        modifier = Modifier.size(14.dp)
    )
}
```

#### Task 5.2: Wire Network Status to BrowserScreen (FR-006)
**File:** `BrowserScreen.kt`

**Changes:**
1. Import `NetworkStatusIndicator` and `NetworkStatus`
2. Add `networkStatus` state variable
3. Add `NetworkStatusIndicator` at top of Box overlay
4. Create platform expect/actual for monitoring

**New File:** `NetworkStatusMonitor.kt` (expect)
```kotlin
expect class NetworkStatusMonitor {
    fun startMonitoring(onStatusChange: (NetworkStatus) -> Unit)
    fun stopMonitoring()
}
```

**New File:** `NetworkStatusMonitor.android.kt` (actual)
```kotlin
actual class NetworkStatusMonitor(private val context: Context) {
    private var callback: ConnectivityManager.NetworkCallback? = null

    actual fun startMonitoring(onStatusChange: (NetworkStatus) -> Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onStatusChange(NetworkStatus.CONNECTED)
            }
            override fun onLost(network: Network) {
                onStatusChange(NetworkStatus.DISCONNECTED)
            }
        }
        cm.registerDefaultNetworkCallback(callback!!)
    }

    actual fun stopMonitoring() {
        callback?.let { cm.unregisterNetworkCallback(it) }
    }
}
```

#### Task 5.3: Implement Headless Browser Mode (FR-004)
**File:** `BrowserScreen.kt`

**Changes:**
1. Add `isHeadlessMode` state
2. Wrap AddressBar in `AnimatedVisibility(!isHeadlessMode)`
3. Wrap FAB in `AnimatedVisibility(!isHeadlessMode)`
4. Pass `isHeadlessMode` to BottomCommandBar
5. Add toggle in MENU level of command bar

**File:** `BottomCommandBar.kt`
**Lines:** 748-821 (MenuCommandBarFlat)

**Changes:**
1. Add `onToggleHeadlessMode` callback
2. Add "Fullscreen" button with `Icons.Default.Fullscreen` / `FullscreenExit`

**Code Pattern:**
```kotlin
// In BrowserScreen
var isHeadlessMode by rememberSaveable { mutableStateOf(false) }

// AddressBar visibility
AnimatedVisibility(visible = !isHeadlessMode) {
    AddressBar(...)
}

// FAB visibility
AnimatedVisibility(visible = !isHeadlessMode) {
    GlassCard(...) { /* FAB content */ }
}

// Pass to command bar
HorizontalCommandBarLayout(
    isHeadlessMode = isHeadlessMode,
    onToggleHeadlessMode = { isHeadlessMode = !isHeadlessMode },
    ...
)
```

---

## Implementation Order

| # | Task | Est. Lines | Complexity |
|---|------|------------|------------|
| 1 | Command Bar Toggle Sync | 30 | Medium |
| 2 | FAB Glass Style | 15 | Low |
| 3 | Tab Icons | 40 | Low |
| 4 | VoiceCommandDialog Columns | 50 | Medium |
| 5 | Auto-Timeout Setting | 30 | Low |
| 6 | Star → History | 20 | Low |
| 7 | Network Status | 80 | High |
| 8 | Headless Mode | 60 | Medium |

**Total:** ~325 lines of changes

---

## Verification Steps

| Task | Verification |
|------|--------------|
| 1 | Toggle bar → icon changes, bar shows/hides correctly |
| 2 | FAB has blur effect, maintains touch target |
| 3 | Grid/List icons visually distinct |
| 4 | Landscape shows 3 columns, no scroll needed |
| 5 | Setting persists after app restart |
| 6 | Tap star → History screen opens |
| 7 | Disable WiFi → red alert appears |
| 8 | Toggle fullscreen → AddressBar + FAB hide |

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| NetworkCallback may not fire immediately | Add manual check on screen resume |
| Headless mode gesture conflicts | Use double-tap on specific area only |
| Glass blur performance | Use MEDIUM level, not HEAVY |
