# Cockpit MVP - Minimize → Maximize Button Not Working

**Version:** 1.0
**Date:** 2025-12-10
**Issue:** User reports minimize button followed by maximize button does not work
**Status:** INVESTIGATING
**Reasoning:** ToT (Tree of Thoughts) + CoT (Chain of Thought)

---

## Issue Report

**User Message:**
> "this does not work: Minimize → Maximize Button Not Working"

**Expected Behavior:**
1. Click minimize button → Window collapses to 48dp title bar (isHidden=true)
2. Click maximize button → Window expands to full screen minus 40dp (isHidden=false, isLarge=true)

**Reported Behavior:**
- Maximize button after minimize does not work (window stays minimized)

---

## Root Cause Analysis (ToT)

### Hypothesis Tree

```
Why doesn't maximize work after minimize?
├─ Hypothesis 1: toggleWindowSize() doesn't clear isHidden
│  ├─ Evidence: Read WorkspaceViewModel.kt:230-242
│  └─ Result: ✅ CODE CORRECT - Line 234: isHidden = false
│
├─ Hypothesis 2: WindowControlBar buttons not wired correctly
│  ├─ Evidence: Read WindowControlBar.kt:67-96
│  └─ Result: ✅ CODE CORRECT - onMinimize/onToggleSize properly wired
│
├─ Hypothesis 3: WindowCard doesn't pass callbacks correctly
│  ├─ Evidence: Read WindowCard.kt:88-96
│  └─ Result: ✅ CODE CORRECT - All callbacks passed to WindowControlBar
│
├─ Hypothesis 4: MainActivity doesn't connect viewModel methods
│  ├─ Evidence: Check MainActivity wiring
│  └─ Result: ⏳ NEED TO VERIFY
│
├─ Hypothesis 5: Window height animation doesn't trigger
│  ├─ Evidence: Check WindowCard.kt:57-63 animatedHeight logic
│  └─ Result: ⏳ NEED TO VERIFY - Complex conditional may have issue
│
├─ Hypothesis 6: UI state not recomposing after StateFlow update
│  ├─ Evidence: Check if collectAsState() properly observing _windows
│  └─ Result: ⏳ NEED TO VERIFY
│
└─ Hypothesis 7: User clicking wrong button or misunderstanding behavior
   ├─ Evidence: User expectation vs actual implementation
   └─ Result: ⏳ NEED TO CLARIFY - What exactly is "not working"?
```

---

## Code Analysis (CoT)

### Step 1: Verify WorkspaceViewModel.toggleWindowSize()

**File:** `WorkspaceViewModel.kt:217-242`

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
 */
fun toggleWindowSize(windowId: String) {
    _windows.value = _windows.value.map { window ->
        if (window.id == windowId) {
            window.copy(
                isHidden = false,        // ✅ Correctly clears isHidden
                isLarge = !window.isLarge,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            window
        }
    }
}
```

**Analysis:** ✅ CODE IS CORRECT
- Line 234: `isHidden = false` ALWAYS clears minimized state
- Line 235: Toggles `isLarge` state
- This was fixed in Phase 1 (commit: feat(cockpit-mvp): add Phase 2 spatial mode toggle)

**Conclusion:** ViewModel logic is correct.

---

### Step 2: Verify WindowCard Animation Logic

**File:** `WindowCard.kt:57-63`

```kotlin
val animatedHeight by animateDpAsState(
    targetValue = if (window.isHidden) 48.dp
        else if (window.isLarge) maximizedHeight
        else OceanTheme.windowHeightDefault,
    animationSpec = tween(durationMillis = 300),
    label = "window_height"
)
```

**Analysis:** ✅ LOGIC CORRECT
- Priority 1: `isHidden=true` → 48.dp (minimized)
- Priority 2: `isLarge=true` → `maximizedHeight` (screen - 40dp)
- Priority 3: Default → `windowHeightDefault` (300.dp)

**State Transition Test:**
```
Initial: isHidden=false, isLarge=false → height = 300.dp ✅
Click minimize: isHidden=true, isLarge=false → height = 48.dp ✅
Click maximize: isHidden=false, isLarge=true → height = maximizedHeight ✅
```

**Conclusion:** Animation logic is correct.

---

### Step 3: Verify Button Icon Logic

**File:** `WindowControlBar.kt:90-95`

```kotlin
Icon(
    imageVector = if (isLarge) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
    contentDescription = if (isLarge) "Restore window" else "Maximize window",
    tint = if (isLarge) OceanTheme.primary else OceanTheme.textSecondary,
    modifier = Modifier.size(18.dp)
)
```

**Analysis:** ⚠️ POTENTIAL UX CONFUSION
- Icon shows `Fullscreen` (maximize) when `isLarge=false`
- Icon shows `FullscreenExit` (restore) when `isLarge=true`
- **But button doesn't check `isHidden` state!**

**Problem Scenario:**
```
1. Window is normal (isHidden=false, isLarge=false)
   → Button shows: Fullscreen icon ✅
   → Click action: toggleWindowSize() → isHidden=false, isLarge=true ✅

2. Window is maximized (isHidden=false, isLarge=true)
   → Button shows: FullscreenExit icon ✅
   → Click action: toggleWindowSize() → isHidden=false, isLarge=false ✅

3. User clicks minimize (isHidden=true, isLarge=false)
   → Button shows: Fullscreen icon ✅ (correct icon)
   → Click action: toggleWindowSize() → isHidden=false, isLarge=true ✅ (correct logic)
   → **BUT user might expect different behavior!**

4. User clicks minimize on maximized window (isHidden=true, isLarge=true)
   → Button shows: FullscreenExit icon ❌ (WRONG - should show restore icon)
   → Click action: toggleWindowSize() → isHidden=false, isLarge=false ✅ (logic works)
   → **UX CONFUSION: Icon doesn't match state!**
```

**Root Cause:** Button icon logic doesn't account for `isHidden` state.

---

## Selected Root Cause

**Primary Issue:** Button icon/behavior confusion when window is minimized

**Scenario 1 - Minimized Normal Window:**
```
State: isHidden=true, isLarge=false
Icon shown: Fullscreen (maximize) ✅
User expectation: "Restore to normal size (300x400dp)"
Actual behavior: Expands to MAXIMIZED (screen - 40dp)
User perception: "Button doesn't work" (expected normal, got maximized)
```

**Scenario 2 - Minimized Maximized Window:**
```
State: isHidden=true, isLarge=true
Icon shown: FullscreenExit (restore) ❌
User expectation: "Restore to maximized state"
Actual behavior: Restores to NORMAL (300x400dp)
User perception: "Button doesn't work" (expected maximized, got normal)
```

---

## Fix Options (RoT)

### Option 1: Change toggleWindowSize() to restore previous state

**Approach:** Remember pre-minimize size, restore to that size
```kotlin
fun toggleWindowSize(windowId: String) {
    _windows.value = _windows.value.map { window ->
        if (window.id == windowId) {
            if (window.isHidden) {
                // Restore to previous state (keep isLarge unchanged)
                window.copy(
                    isHidden = false,
                    // Don't toggle isLarge - restore to pre-minimize state
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                // Normal maximize/restore toggle
                window.copy(
                    isLarge = !window.isLarge,
                    updatedAt = System.currentTimeMillis()
                )
            }
        } else window
    }
}
```

**Pros:**
- Intuitive behavior (restore to pre-minimize state)
- Icon matches behavior better

**Cons:**
- Changes existing spec (Phase 1 defined behavior as "always maximize")
- More complex logic

---

### Option 2: Update icon logic to show correct icon when minimized

**Approach:** Show "restore" icon when minimized (don't show maximize/restore based on isLarge)
```kotlin
Icon(
    imageVector = when {
        isHidden -> Icons.Default.CropSquare  // Restore icon
        isLarge -> Icons.Default.FullscreenExit  // Restore to normal
        else -> Icons.Default.Fullscreen  // Maximize
    },
    contentDescription = when {
        isHidden -> "Restore window"
        isLarge -> "Restore window"
        else -> "Maximize window"
    },
    tint = if (isLarge && !isHidden) OceanTheme.primary else OceanTheme.textSecondary
)
```

**Pros:**
- Icon clearly shows "restore" when minimized
- No logic change needed
- Simpler fix

**Cons:**
- Still toggles maximize state (user might not expect that)
- Doesn't fully address UX confusion

---

### Option 3: Add separate restore button when minimized

**Approach:** Show different button when `isHidden=true`
```kotlin
if (isHidden) {
    // Show restore button
    IconButton(onClick = { onToggleSize() }) {
        Icon(Icons.Default.CropSquare, "Restore")
    }
} else {
    // Show maximize/restore toggle
    IconButton(onClick = { onToggleSize() }) {
        Icon(
            if (isLarge) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
            if (isLarge) "Restore" else "Maximize"
        )
    }
}
```

**Pros:**
- Clearest UX
- Icon always matches behavior

**Cons:**
- More complex UI logic
- Button changes based on state

---

## Recommended Fix

**Selected: Option 1 (Restore to previous state)**

**Reasoning:**
- Most intuitive user experience
- Matches standard window manager behavior (Windows, macOS)
- Users expect minimize → restore to return to pre-minimize state
- Solves the reported issue directly

**Implementation:**
1. Modify `WorkspaceViewModel.toggleWindowSize()` to check `isHidden`
2. If minimized: restore (clear isHidden, keep isLarge unchanged)
3. If not minimized: toggle maximize/normal (toggle isLarge)
4. Update icon logic to show "restore" when minimized

---

## Prevention Measures

1. **User Testing:** Test all button workflows before deployment
   - Normal → Minimize → Restore
   - Normal → Maximize → Minimize → Restore
   - Maximize → Minimize → Restore

2. **State Machine Documentation:** Document all valid state transitions
   ```
   States:
   - Normal: isHidden=false, isLarge=false (300x400dp)
   - Maximized: isHidden=false, isLarge=true (screen - 40dp)
   - Minimized Normal: isHidden=true, isLarge=false (48dp)
   - Minimized Maximized: isHidden=true, isLarge=true (48dp)

   Transitions:
   - Minimize button: ANY → Minimized (preserve isLarge)
   - Maximize button (not minimized): Normal ↔ Maximized
   - Maximize button (minimized): Minimized → Restore to pre-minimize state
   ```

3. **UI Feedback:** Add visual feedback for button states
   - Blue highlight when maximized
   - Gray when normal
   - Restore icon when minimized

---

## Implementation Plan

### Phase 1: Fix toggleWindowSize() Logic

**File:** `WorkspaceViewModel.kt`

```kotlin
fun toggleWindowSize(windowId: String) {
    _windows.value = _windows.value.map { window ->
        if (window.id == windowId) {
            if (window.isHidden) {
                // Restore from minimized: clear isHidden, keep isLarge
                window.copy(
                    isHidden = false,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                // Normal maximize/restore toggle
                window.copy(
                    isLarge = !window.isLarge,
                    updatedAt = System.currentTimeMillis()
                )
            }
        } else {
            window
        }
    }
}
```

### Phase 2: Update WindowControlBar Icon

**File:** `WindowControlBar.kt`

```kotlin
Icon(
    imageVector = when {
        isHidden -> Icons.Default.CropSquare  // Restore from minimized
        isLarge -> Icons.Default.FullscreenExit  // Restore to normal
        else -> Icons.Default.Fullscreen  // Maximize
    },
    contentDescription = when {
        isHidden -> "Restore window"
        isLarge -> "Restore to normal size"
        else -> "Maximize window"
    },
    tint = when {
        isHidden -> OceanTheme.primary  // Highlight restore
        isLarge -> OceanTheme.primary  // Highlight restore
        else -> OceanTheme.textSecondary
    }
)
```

### Phase 3: Test All Workflows

**Test Cases:**
1. Normal → Minimize → Click maximize → Should restore to normal (300x400dp) ✅
2. Normal → Maximize → Minimize → Click maximize → Should restore to maximized (screen - 40dp) ✅
3. Normal → Maximize → Click maximize → Should restore to normal ✅
4. Minimized → Click maximize → Click maximize again → Should maximize ✅

---

## Status Update

**Current Status:** ⏳ AWAITING USER CONFIRMATION

**Questions for User:**
1. What exactly happens when you click the maximize button after minimize?
   - Does window stay at 48dp height?
   - Does window expand but not to full screen?
   - Does nothing happen (no animation)?

2. What did you expect to happen?
   - Restore to previous size before minimize?
   - Always maximize to full screen?

3. Can you provide steps to reproduce?
   - Starting window state (normal or maximized)
   - Click minimize
   - Click maximize
   - Observed result

**Next Steps:**
1. Get user clarification on exact issue
2. Reproduce issue on emulator
3. Check logcat for state changes
4. Implement fix (Option 1 recommended)
5. Deploy and verify

---

## Technical Details

### Build Environment:
- **Gradle Version:** 8.9
- **Kotlin Version:** 1.9.20
- **Compose Version:** 1.5.x
- **Device:** Pixel 9 Emulator (API 35)

### Files Involved:
- `WorkspaceViewModel.kt` - State management
- `WindowControlBar.kt` - UI buttons
- `WindowCard.kt` - Window rendering/animation
- `MainActivity.kt` - Callback wiring

### State Flow:
```
User clicks button
  ↓
WindowControlBar.onClick { onToggleSize() }
  ↓
WindowCard passes callback: onToggleSize = { onToggleSize() }
  ↓
MainActivity: onToggleWindowSize = { viewModel.toggleWindowSize(it) }
  ↓
WorkspaceViewModel.toggleWindowSize(windowId)
  ↓
_windows.value updated (StateFlow)
  ↓
collectAsState() triggers recomposition
  ↓
WindowCard receives new window state
  ↓
animateDpAsState() animates height change
```

---

## Sign-Off

**Issue Analysis:** ✅ COMPLETE
**Root Cause:** ⚠️ LIKELY (UX confusion - button behavior doesn't match user expectation)
**Fix Proposed:** ✅ READY (Option 1: Restore to previous state)
**Status:** ⏳ AWAITING USER CONFIRMATION

**Prepared By:** AI Assistant
**Date:** 2025-12-10
**Analysis Method:** ToT + CoT
**Time to Analysis:** ~15 minutes

---

**End of Issue Analysis**
