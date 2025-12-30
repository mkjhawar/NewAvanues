# Disabled Tests - Action Required

**Status:** NEEDS REVIEW (VOSFIX-005)
**Created:** 2025-12-27
**Backlog:** VoiceOS-Backlog-CodeAnalysis-251227-V1.md

---

## Why These Tests Are Disabled

This directory contains 22 test files that were disabled by renaming `java/` to `java.disabled/`.

**This is a code quality issue.** Disabled tests:
- Provide zero test coverage
- May hide regressions
- Create technical debt

---

## Files Requiring Review

| File | Category | Action Needed |
|------|----------|---------------|
| `BatchTransactionManagerTest.kt` | database | Review & re-enable |
| `SafeCursorManagerTest.kt` | database | Review & re-enable |
| `SafeNodeTraverserTest.kt` | lifecycle | Review & re-enable |
| `AccessibilityNodeManagerSimpleTest.kt` | lifecycle | Review & re-enable |
| `AccessibilityNodeManagerTest.kt` | lifecycle | Review & re-enable |
| `AsyncQueryManagerTest.kt` | lifecycle | Review & re-enable |
| `SafeNullHandlerTest.kt` | utils | Review & re-enable |
| `MockVoiceAccessibilityService.kt` | mocks | Move to test fixtures |
| `MockVoiceRecognitionManager.kt` | mocks | Move to test fixtures |
| `AccessibilityTreeProcessorTest.kt` | tree | Review & re-enable |
| `PerformanceTest.kt` | test | Review & re-enable |
| `VoiceCommandTestScenarios.kt` | test | Review & re-enable |
| `EndToEndVoiceTest.kt` | test | Review & re-enable |
| `CommandExecutionVerifier.kt` | test | Move to test utilities |
| `TestUtils.kt` | test | Move to test utilities |
| `ConfidenceOverlayTest.kt` | overlays | Review & re-enable |
| `OverlayManagerTest.kt` | overlays | Review & re-enable |
| `UUIDCreatorIntegrationTest.kt` | integration | Review & re-enable |
| `EventPriorityManagerTest.kt` | utils | Review & re-enable |
| `GestureHandlerTest.kt` | handlers | Review & re-enable |
| `DragHandlerTest.kt` | handlers | Review & re-enable |
| `GazeHandlerTest.kt` | handlers | Review & re-enable |

---

## How to Re-enable

1. Move files to `src/test/java/` directory
2. Fix any compilation errors
3. Run tests and fix failures
4. Delete this README when complete

---

## DO NOT

- Delete these tests without review
- Keep them disabled indefinitely
- Add more tests to this directory

---

**Owner:** VoiceOS Team
**Due:** Next Sprint
