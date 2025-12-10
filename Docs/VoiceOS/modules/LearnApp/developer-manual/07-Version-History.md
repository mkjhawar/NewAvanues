# Chapter 7: Version History

**Module**: LearnApp
**Last Updated**: 2025-12-08

This chapter documents the complete version history and changelog for LearnApp.

---

## Version History

- **1.0.0** (2025-10-08): Initial release
  - DFS exploration engine
  - Consent management
  - SQLDelight database integration
  - UUID integration

- **1.0.1** (2025-10-23): Threading fixes
  - Fixed ConsentDialogManager threading issues
  - Added `withContext(Dispatchers.Main)` for UI operations

- **1.4.0** (2025-12-04): Hybrid C-Lite exploration
  - Implemented fresh-scrape exploration strategy
  - 98% click success rate (up from 50%)
  - Stability-based element sorting

- **1.5.0** (2025-12-05): Critical bug fixes
  - Added "power down" pattern to DangerousElementDetector (CRITICAL)
  - Fixed intent relaunch recovery premature termination (HIGH)
  - Added 51 configurable developer settings
  - Wired verbose logging to 158 Log.d() calls

- **1.6.0** (2025-12-05): Global safety enhancements (all apps)
  - Added communication patterns: audio/video call, dial, make call (CRITICAL)
  - Added admin action patterns: demote, promote, remove member (CRITICAL)
  - Added audio settings patterns: microphone, dictation (CRITICAL)
  - Fixed duplicate command generation with UNIQUE constraint
  - Added `exists` and `insertIfNotExists` queries for deduplication

- **1.7.0** (2025-12-06): Data integrity fix (CRITICAL)
  - Fixed incorrect LEARNED status for partial explorations (<95%)
  - Added `completeness` field to ExplorationStats
  - Made status/progress conditional based on actual completeness
  - Consent dialog now shows for partially learned apps

- **1.8.0** (2025-12-06): UI blocking fix (bottom command bar) - CRITICAL
  - Replaced full-screen overlay with 48dp bottom command bar (Material Design 3)
  - Added pause/resume functionality to ExplorationEngine
  - Added auto-detection of permission dialogs and login screens
  - Added auto-pause when blocked states detected (permission/login)
  - Added swipe-to-dismiss gesture and background notification
  - Added pause state persistence to database (survives app restart)
  - Material Design 3 compliance (48dp touch targets, Material Motion animations)
  - Test coverage: 81% (42 automated tests + 10 manual scenarios)
  - Expected impact: Apps requiring permissions/login reach 95%+ completeness (up from 10-24%)
  - Commit: 2c5e9a1e
  - Files: 6 modified, 16 created (22 files total, 3861 insertions)

- **1.9.0** (2025-12-08): Cumulative VUID tracking fix (CRITICAL)
  - Fixed 10% completion when 50-75% of screens explored
  - Root cause: `clickTracker.clear()` on intent relaunch destroyed exploration progress
  - Added class-level cumulative tracking sets: `cumulativeDiscoveredVuids`, `cumulativeClickedVuids`
  - These survive intent relaunches but clear at start of new exploration session
  - clickTracker still used for per-screen fresh-scrape element selection
  - Final completion % now uses cumulative stats (not clickTracker stats)
  - Log messages to watch: "Final Stats (CUMULATIVE)" vs "Final Stats (clickTracker)"
  - Commit: 43f0c480
  - Files: ExplorationEngine.kt (79 insertions, 25 deletions)

- **1.9.1** (2025-12-08): Enhanced call/meeting blocking patterns
  - Added Teams-specific call patterns: "Make a call", "Call" button, "Join call", "Answer"
  - Added meeting patterns: "New meeting", "Schedule meeting", "Instant meeting"
  - Added "Reply" to critical blocklist (can send messages in Teams/messaging apps)
  - Fixed regex: `make\s*call` → `make.*call` to catch "Make a call"
  - Added resource ID patterns: `call_control`, `call_end`, `calls_call`, `fab`, `reply`
  - Files: DangerousElementDetector.kt, ExplorationEngine.kt

- **1.10.0** (2025-12-08): Blocked vs non-blocked stats display
  - User request: Show separate stats for blocked vs non-blocked items
  - New format: "XX% of non-blocked items (YY/ZZ clicked), WW blocked"
  - Added `cumulativeBlockedVuids` tracking set for blocked elements
  - Added fields to ExplorationStats: `clickedElements`, `nonBlockedElements`, `blockedElements`
  - Added `formatCompletion()` method to ExplorationStats
  - Updated toast notification to show new format
  - Updated final stats logging with breakdown
  - Files: ExplorationEngine.kt, ExplorationStats.kt, LearnAppIntegration.kt

- **1.10.1** (2025-12-08): Debug overlay complete rewrite (CRITICAL)
  - User request: "Floating overlay not working, REWRITE from scratch"
  - Problem: Old Canvas-based overlay only showed current screen elements, not a scrollable LIST
  - Solution: Complete rewrite with scrollable item list tracking ALL exploration items
  - **Deleted files:**
    - LearnAppDebugOverlay.kt (old Canvas-based drawing)
    - DebugOverlayState.kt (old state tracking)
    - Old DebugOverlayManager.kt (verbosity-based)
  - **Created files:**
    - ExplorationItemData.kt - Data models (ExplorationItem, ExplorationSummary, ItemStatus enum)
    - ExplorationItemTracker.kt - Central tracker for ALL items across ALL screens
    - DebugOverlayView.kt - Scrollable LinearLayout with filter buttons
    - DebugOverlayManager.kt - New lifecycle manager (complete rewrite)
    - README-DEBUG-OVERLAY.md - New documentation
  - **Modified files:**
    - ExplorationEngine.kt - Added onElementClicked() and onElementBlocked() callbacks
    - LearnAppIntegration.kt - Updated to use new debug overlay API
    - FloatingProgressWidget.kt - Fixed cycleVerbosity() to use toggleDebugOverlay()
  - **New features:**
    - Scrollable list showing ALL items discovered during exploration
    - Filter buttons: All/Screens/Clicked/Blocked/Stats
    - Status icons: ⚪ (discovered), ✅ (clicked), 🚫 (blocked), 🔄 (exploring)
    - Persistent tracking survives show/hide cycles
    - Thread-safe ConcurrentHashMap storage
    - Draggable, collapsible overlay window
    - Export to markdown functionality
  - Files: 4 deleted, 5 created, 3 modified

---
## Related Documentation

- [User Manual](./user-manual.md)
- [Architecture Guide](./architecture/overview.md)
- [API Reference](./reference/api-reference.md)
- [Testing Guide](./testing/testing-guide.md)

---

**End of Developer Manual**

---

**Navigation**: [← Previous: Troubleshooting](./06-Troubleshooting.md) | [Index](./00-Index.md)
