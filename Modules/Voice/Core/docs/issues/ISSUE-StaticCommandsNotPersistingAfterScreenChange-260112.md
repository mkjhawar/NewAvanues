# Issue Report: Static Commands Not Working After Screen Change

**Issue ID:** VOCNG-001
**Date:** 2026-01-12
**Severity:** High
**Component:** VoiceOSAccessibilityService, Speech Engine Integration
**Status:** Identified, Fix Pending

---

## Executive Summary

Static voice commands (e.g., "go back", "scroll down", "open settings") work only once when the accessibility service starts. After any screen change, static commands stop working because the speech engine's command grammar is **replaced** with only dynamic commands, **not merged** with static commands.

---

## Chain of Thought Analysis (.cot)

### Step 1: Understanding Expected Behavior

**Expected:**
- Static commands should ALWAYS work regardless of screen context
- Dynamic commands should be ADDED to the speech engine on screen change
- Speech engine grammar = Static Commands + Dynamic Commands (for current screen)

**Actual:**
- Static commands work on initial service connection
- After first screen change, static commands stop being recognized
- Only dynamic commands from current screen are recognized

### Step 2: Tracing the Registration Flow

**Initial Registration (Works):**
```
VoiceOSAccessibilityService.onServiceConnected()
  → VoiceOSCoreNG.createForAndroid()
  → VoiceOSCoreNG.initialize()
    → speechEngine.updateCommands(staticPhrases)  ✓ Static commands registered
```

**On Screen Change (Bug):**
```
handleScreenChange(packageName)
  → performExplorationWithCache()
    → registerCommandsWithEngine(elements)
      → voiceOSCore.updateCommands(commandPhrases)  ✗ REPLACES all commands
```

### Step 3: Identifying Root Cause

**Location:** `VoiceOSAccessibilityService.kt:1493-1506`

```kotlin
// Update speech engine grammar (Vivoka SDK) so it recognizes ALL phrases
// Includes: element commands, index commands ("first", "second"), and label commands ("Lifemiles")
val commandPhrases = allCommands.map { it.phrase } +
    indexCommands.map { it.phrase } +
    labelCommands.map { it.phrase }
serviceScope.launch {
    try {
        voiceOSCore?.updateCommands(commandPhrases)  // ← BUG: Does NOT include static commands
        ...
    }
}
```

**Problem:** `commandPhrases` only contains dynamic commands from the current screen. Static commands (from `StaticCommandRegistry`) are NOT included when calling `voiceOSCore.updateCommands()`.

### Step 4: Why It Worked Initially

During `VoiceOSCoreNG.initialize()` (lines 187-211):

```kotlin
// Register static commands with speech engine for voice recognition
if (initResult?.isSuccess == true) {
    val staticPhrases = if (staticCommandPersistence != null) {
        staticCommandPersistence.getAllPhrases()
    } else {
        StaticCommandRegistry.allPhrases()
    }

    if (staticPhrases.isNotEmpty()) {
        val updateResult = speechEngine?.updateCommands(staticPhrases)
        // ✓ Static commands registered here
    }
}
```

But this registration is **overwritten** on the first `handleScreenChange()` call because `updateCommands()` **replaces** the grammar, it doesn't **merge**.

---

## Impact Analysis

| Command Type | Initial State | After Screen Change |
|--------------|---------------|---------------------|
| Static (back, home, scroll) | ✓ Working | ✗ Not recognized |
| Dynamic (click X, tap Y) | N/A | ✓ Working |
| Index (first, second) | N/A | ✓ Working |

**User Experience:**
- User starts VoiceOS, says "go back" → Works
- User navigates to a new screen
- User says "go back" → Fails (command not recognized)
- User says "click Settings" (dynamic) → Works

---

## Solution Design

### Option A: Merge Static + Dynamic on Every Update (Recommended)

Modify `VoiceOSAccessibilityService.registerCommandsWithEngine()` to always include static commands:

```kotlin
val staticPhrases = StaticCommandRegistry.allPhrases()
val commandPhrases = staticPhrases +  // ← ADD STATIC FIRST
    allCommands.map { it.phrase } +
    indexCommands.map { it.phrase } +
    labelCommands.map { it.phrase }

voiceOSCore?.updateCommands(commandPhrases)
```

**Pros:**
- Simple fix, minimal code change
- Static commands always included
- No API changes needed

**Cons:**
- Slight overhead from including static phrases every time
- Duplicate phrases if static phrase matches dynamic element

### Option B: Separate Static and Dynamic Grammars in Speech Engine

Modify the speech engine interface to support two command sets:
- `setStaticCommands(phrases)` - Called once on init, never replaced
- `setDynamicCommands(phrases)` - Updated on screen change

**Pros:**
- Cleaner separation of concerns
- No redundant phrase transmission

**Cons:**
- Requires speech engine API changes
- More complex implementation for Vivoka SDK

### Recommendation: Option A

Option A provides the simplest fix with immediate impact. Option B can be implemented later as an optimization if needed.

---

## Files to Modify

| File | Change |
|------|--------|
| `VoiceOSAccessibilityService.kt` | Add static phrases to `updateCommands()` call |

---

## Test Plan

1. **Start VoiceOS Accessibility Service**
   - Expected: Static commands work (say "go back" → navigates back)

2. **Navigate to a new app (e.g., Settings)**
   - Expected: Dynamic commands generated for Settings screen

3. **Say a static command (e.g., "scroll down")**
   - Expected: Screen scrolls down
   - Bug behavior: Command not recognized

4. **Say a dynamic command (e.g., "click Network")**
   - Expected: Click action works
   - Current behavior: Works correctly

5. **Navigate to another screen**
   - Expected: Both static AND dynamic commands work
   - Bug behavior: Only dynamic commands work

---

## Implementation Checklist

- [ ] Add `StaticCommandRegistry.allPhrases()` to command phrases list
- [ ] Verify no duplicate phrase handling issues
- [ ] Test static commands across multiple screen changes
- [ ] Test dynamic commands still work
- [ ] Test index commands ("first", "second") still work
- [ ] Verify performance (no noticeable delay)

---

**End of Issue Report**
