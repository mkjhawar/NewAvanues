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
