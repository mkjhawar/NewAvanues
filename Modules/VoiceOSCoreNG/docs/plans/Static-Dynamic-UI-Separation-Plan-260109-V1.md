# Implementation Plan: Static/Dynamic UI Separation

**Feature:** Separate static UI elements (menus, buttons) from dynamic content (email lists, chat messages) for intelligent persistence.

**Created:** 2026-01-09
**Version:** V1

---

## Overview

| Attribute | Value |
|-----------|-------|
| Platforms | Android (primary) |
| Estimated Tasks | 8 |
| Swarm Recommended | No (single platform) |
| KMP Benefit | Yes (reusable detection logic) |

---

## Chain of Thought Reasoning

### Problem Analysis

1. **Current Behavior:** All actionable elements are persisted to the database
2. **Issue:** Dynamic list items (emails, messages) flood the database with:
   - Huge text content ("Unread, , , American Airlines... 200+ chars")
   - Constantly changing data (new emails)
   - Same elementHash for structurally identical items
3. **Impact:** Database bloat, unusable voice commands, poor UX

### Design Decision: Detection Criteria

**Option A: Text Length Only**
- Simple: `if (text.length > 50) isDynamic = true`
- Problem: Misses short dynamic items, catches long button text

**Option B: Parent Container Detection (Selected)**
- Check if element is inside RecyclerView/ListView
- More accurate: structural detection
- Reusable across apps

**Option C: Hash Collision Detection**
- Multiple items with same elementHash = list items
- Problem: Requires post-processing, not real-time

**Selected Approach: Hybrid B + C + Text Heuristics**
- Primary: Parent container detection (RecyclerView, ListView, ViewPager)
- Secondary: Content pattern detection ("Unread, , ,", timestamps)
- Tertiary: Text length threshold (>100 chars)

### Persistence Strategy

| Element Type | In-Memory | Database | Voice Command |
|--------------|-----------|----------|---------------|
| Static (menus, buttons) | Yes | Yes | "click Compose" |
| Dynamic (list items) | Yes | No | "click first email" / "click Arby's" |
| Screen structure | No | Yes (hash only) | N/A |

---

## Phase Ordering

| Phase | Tasks | Rationale |
|-------|-------|-----------|
| 1 | Add detection properties to ElementInfo | Foundation |
| 2 | Implement Android detection in scraping | Detection layer |
| 3 | Modify CommandGenerator to flag dynamic | Generation layer |
| 4 | Update persistence to filter dynamic | Storage layer |
| 5 | Add index-based commands for lists | UX enhancement |
| 6 | Testing & validation | Quality assurance |

---

## Implementation Tasks

### Phase 1: ElementInfo Enhancement (KMP)

**Task 1.1: Add dynamic detection fields to ElementInfo**
- File: `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/ElementInfo.kt`
- Changes:
  - Add `isInDynamicContainer: Boolean = false`
  - Add `containerType: String = ""` (RecyclerView, ListView, etc.)
  - Add `listIndex: Int = -1` (position in list, -1 if not in list)
- Priority: P0
- Estimate: Simple

**Task 1.2: Add CommandGenerator filtering**
- File: `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/CommandGenerator.kt`
- Changes:
  - Add `shouldPersist()` method checking dynamic content
  - Filter commands by text length (>100 chars = dynamic)
  - Filter by "Unread, , ," pattern for email detection
- Priority: P0
- Estimate: Simple

### Phase 2: Android Detection Implementation

**Task 2.1: Implement container detection in element scraping**
- File: `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/VoiceOSAccessibilityService.kt`
- Changes:
  - In `traverseNode()`: detect RecyclerView/ListView ancestors
  - Track list index during traversal
  - Set `isInDynamicContainer` and `listIndex` on ElementInfo
- Priority: P0
- Estimate: Medium

**Task 2.2: Add content pattern detection**
- File: `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/VoiceOSAccessibilityService.kt`
- Changes:
  - Detect email patterns: "Unread, , ," prefix
  - Detect timestamp patterns: "at X:XX AM/PM"
  - Detect notification patterns with long preview text
- Priority: P1
- Estimate: Simple

### Phase 3: Persistence Layer Changes

**Task 3.1: Filter dynamic elements from database persistence**
- File: `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/VoiceOSAccessibilityService.kt`
- Changes:
  - In `generateCommands()`: separate static and dynamic commands
  - Only persist static commands to `commands_generated`
  - Keep dynamic commands in memory only
- Priority: P0
- Estimate: Medium

**Task 3.2: Add screen type tracking**
- File: Database schema + ScreenHashRepository
- Changes:
  - Add `isDynamicScreen: Boolean` to screen cache
  - Store screen type (list, detail, menu) for context
- Priority: P2
- Estimate: Simple

### Phase 4: Index-Based Commands for Lists

**Task 4.1: Generate index commands for dynamic lists**
- File: `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/CommandGenerator.kt`
- Changes:
  - Add `generateListCommands(elements, listName)` method
  - Generate: "click first", "click second", ... "click item 5"
  - Generate: "click [sender name]" for email items
- Priority: P1
- Estimate: Medium

**Task 4.2: Register index commands with speech engine**
- File: VoiceOSAccessibilityService.kt
- Changes:
  - Register generic list commands: "first", "second", "item 1-10"
  - Map sender names to click targets (short text only)
- Priority: P1
- Estimate: Simple

### Phase 5: Testing & Validation

**Task 5.1: Test with Gmail**
- Verify: Email list items NOT persisted
- Verify: Static menu items ARE persisted
- Verify: "click Arby's" or "click first" works
- Priority: P0
- Estimate: Testing

**Task 5.2: Test with other dynamic apps**
- Apps: Messages, Contacts, Settings
- Verify: List behavior consistent
- Priority: P1
- Estimate: Testing

---

## File Changes Summary

| File | Type | Changes |
|------|------|---------|
| ElementInfo.kt | KMP | Add 3 properties |
| CommandGenerator.kt | KMP | Add shouldPersist(), generateListCommands() |
| VoiceOSAccessibilityService.kt | Android | Container detection, filtering |
| ScreenHashRepository.kt | KMP | Add screen type field |

---

## Success Criteria

1. Gmail shows 5-6 persisted commands (menus only), not 15+ (emails)
2. "click Compose" works (static command)
3. "click first" or "click Arby's" works on email list (dynamic in-memory)
4. Database size stable regardless of email count
5. Revisiting Gmail loads static commands from cache, generates dynamic in-memory

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| False positives (static marked as dynamic) | Whitelist common resourceIds (compose_button, etc.) |
| False negatives (dynamic missed) | Multiple detection methods (container + pattern + length) |
| Voice recognition for sender names | Use NLU fallback for partial matches |

---

## Next Steps

After plan approval:
1. Generate TodoWrite tasks
2. Start Phase 1 implementation
3. Test incrementally with Gmail

---

**Version:** 1.0 | **Author:** Claude | **Status:** Ready for Review
