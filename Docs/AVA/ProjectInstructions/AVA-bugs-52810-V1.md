# AVA AI - Bug Tracking

**Last Updated**: 2025-01-28
**Phase**: 1.0 - MVP (Week 5 Complete)
**Framework**: IDEACODE v1.0

---

## Purpose

Track known bugs, issues, and technical debt discovered during AVA AI development. Use this for bug prioritization and resolution tracking.

---

## Active Bugs

_No active bugs at this time._

---

## Known Issues (Non-Blocking)

### ISSUE-001: Model Performance Not Validated on Device

**Discovered**: 2025-01-28 (Week 5)
**Reporter**: NLU integration session
**Severity**: Low
**Impact**: Unknown if NLU meets <100ms performance budget on real hardware

**Description**:
MobileBERT INT8 model has been tested on emulator only. Actual inference time on physical Android devices (especially low-end devices with 2GB RAM) is unknown.

**Expected Behavior**:
- First inference (cold start): 60-100ms
- Subsequent inferences (warm): 30-60ms
- NNAPI acceleration should provide 1.5-3x speedup on supported devices

**Actual Behavior**:
- Emulator performance unknown (emulators often slower than real devices)
- No performance data from physical hardware

**Reproduction Steps**:
1. Build app with model bundled in assets
2. Run on emulator
3. Observe: No physical device testing done yet

**Mitigation**:
- Week 6 device testing planned
- Expected to pass budget (emulators are typically slower)
- NNAPI fallback to CPU if hardware acceleration unavailable

**Resolution Plan**:
- Week 6: Test on low-end (2GB RAM), mid-range (4GB), high-end (8GB+) devices
- Run `ModelLoadingTest.testRealInferencePerformance()`
- Validate <100ms budget met

**Priority**: P2 (Medium)
**Target Resolution**: Week 6
**Status**: Open

---

### ISSUE-002: Teach-Ava Backend Integration Tests Pending

**Discovered**: 2025-01-28 (Week 5)
**Reporter**: Teach-Ava UI implementation session
**Severity**: Low
**Impact**: End-to-end classification flow not validated

**Description**:
Teach-Ava UI is complete, but end-to-end integration with NLU classification not yet tested:
1. User adds training example → stored in DB
2. User triggers classification → NLU uses training data
3. Low confidence → suggests adding to Teach-Ava

**Expected Behavior**:
- User adds "turn on lights" → control_lights
- Classification uses updated training data
- Low confidence (<0.5) → UI suggests Teach-Ava

**Actual Behavior**:
- Teach-Ava UI tests only ViewModel logic
- NLU tests mock training data
- No integration test connecting both systems

**Mitigation**:
- Repository layer tested (Week 3-4)
- NLU layer tested (Week 5)
- UI layer tested (Week 5)
- Integration test just needs to connect the layers

**Resolution Plan**:
- Week 6: Create end-to-end integration test
- Test: Add training example → classify utterance → verify intent match
- Validate: Low confidence triggers Teach-Ava suggestion in Chat UI

**Priority**: P2 (Medium)
**Target Resolution**: Week 6
**Status**: Open

---

### ISSUE-003: Compose UI Tests Not Set Up

**Discovered**: 2025-01-28 (Week 5)
**Reporter**: Teach-Ava UI implementation session
**Severity**: Low
**Impact**: No automated UI testing (only ViewModel unit tests)

**Description**:
Teach-Ava UI components (Screen, Dialogs, Cards, Content) have no Compose UI tests. Only ViewModel logic is unit-tested.

**Expected Behavior**:
- UI tests validate Compose rendering
- Test user interactions (clicks, text input, scrolling)
- Test dialog flows (open, dismiss, save)

**Actual Behavior**:
- Only ViewModel unit tests exist
- No `@Composable` function tests
- No UI interaction tests

**Mitigation**:
- ViewModel logic tested (90%+ coverage)
- Manual testing validates UI works
- Compose UI tests can be added later

**Resolution Plan**:
- Week 7-8: Set up Compose UI testing framework
- Add tests for critical UI flows (add example, edit, delete, filter)
- Target: 80%+ UI test coverage

**Priority**: P3 (Low)
**Target Resolution**: Week 7-8
**Status**: Open

---

## Resolved Bugs

_No resolved bugs yet._

---

## Technical Debt

### DEBT-001: Placeholder URLs in ModelManager

**Created**: 2025-01-28 (Week 5)
**Severity**: Low
**Impact**: Model download URLs may change

**Description**:
ModelManager uses hardcoded Hugging Face URLs:
```kotlin
private val mobileBertUrl =
    "https://huggingface.co/onnx-community/mobilebert-uncased-ONNX/resolve/main/onnx/model_int8.onnx"
```

These URLs are stable but could change if:
- Model repository moves
- Hugging Face changes URL structure
- Model is replaced with newer version

**Mitigation**:
- Current URLs stable (Hugging Face maintains backward compatibility)
- `copyModelFromAssets()` fallback always works (bundled in APK)

**Resolution Plan**:
- Phase 1: Use current URLs (acceptable)
- Phase 2+: Consider configuration file or remote config

**Priority**: P4 (Very Low)
**Target Resolution**: Phase 2+
**Status**: Tracked

---

### DEBT-002: Missing Performance Budgets for NLU Components

**Created**: 2025-01-28 (Week 5)
**Severity**: Low
**Impact**: No granular performance tracking

**Description**:
NLU performance budgets exist but not validated:
- Tokenization: <5ms (not measured)
- Inference: <50ms (not validated on device)
- Total: <60ms (not validated)

**Mitigation**:
- End-to-end budget (<100ms) likely sufficient
- Granular budgets nice-to-have for optimization

**Resolution Plan**:
- Week 6: Add granular performance logging
- Measure tokenization time separately
- Measure inference time (already logged)
- Validate all sub-budgets on device

**Priority**: P3 (Low)
**Target Resolution**: Week 6
**Status**: Tracked

---

## Bug Lifecycle

**Workflow:**
```
Discovered → Active Bugs (if blocking)
           → Known Issues (if non-blocking)
           → Technical Debt (if design issue)

Active Bugs → In Progress → Testing → Resolved

Known Issues → Scheduled for Fix → In Progress → Testing → Resolved

Technical Debt → Tracked → Scheduled → Resolved
```

**Severity Levels:**
- **Critical**: Blocks development, must fix immediately
- **High**: Major functionality broken, fix within 1 week
- **Medium**: Minor functionality broken, fix within 2-3 weeks
- **Low**: Cosmetic or edge case, fix when convenient

**Priority Levels:**
- **P0**: Immediate (blocks release)
- **P1**: High (fix this week)
- **P2**: Medium (fix next sprint)
- **P3**: Low (backlog)
- **P4**: Very Low (future phases)

---

## References

- **Progress Tracking**: `docs/ProjectInstructions/progress.md`
- **Architecture Decisions**: `docs/ProjectInstructions/decisions.md`
- **Implementation Notes**: `docs/ProjectInstructions/notes.md`
- **Constitution**: `.ideacode/memory/principles.md`

---

**Note**: This is a living document. Add new bugs as discovered. Update status as bugs are resolved. Archive resolved bugs quarterly.
