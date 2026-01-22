# Implementation Plan: PluginSystem Next Phase & Related Work

**Generated:** 2026-01-22
**Mode:** .yolo .tasks .cot
**Priority Order:** E2E Verification → Phase 5 → WebAvanue → Docs → VoiceOSCoreNG

---

## Overview

| Attribute | Value |
|-----------|-------|
| Platforms | Android, KMP (shared), Web |
| Work Streams | 5 |
| Estimated Tasks | 28 |
| Swarm Recommended | Yes |
| Dependencies | PluginSystem Phase 4 (complete) |

---

## Phase 1: End-to-End Verification

**Rationale:** Verify completed PluginSystem work before building more features.

### Tasks

1.1. **Build and run voiceoscoreng app**
   - Verify Gradle sync succeeds
   - Build debug APK
   - Install on device/emulator

1.2. **Verify plugin system initialization**
   - Check Logcat for "Plugin system initialized" message
   - Verify no crash on startup
   - Confirm PluginSystemSetupResult.success = true

1.3. **Test handler plugin routing**
   - Issue voice command (tap, scroll, navigate)
   - Verify command routed to correct handler
   - Check action executed on UI element

1.4. **Test plugin lifecycle**
   - Background app → verify plugins pause
   - Foreground app → verify plugins resume
   - Force stop → verify clean shutdown

1.5. **Document verification results**
   - Create QA checklist with pass/fail
   - Note any issues for Phase 5

---

## Phase 2: PluginSystem Phase 5 - Advanced Features

**Rationale:** Complete deferred features from Phase 3-4.

### Tasks

2.1. **Hot Reload Foundation** (Deferred from Phase 4)
   - Design file watcher for plugin changes
   - Implement PluginHotReloader (was deleted)
   - Add dev-mode toggle in settings

2.2. **Remote Plugin Support**
   - Define remote plugin manifest format
   - Implement plugin download/verification
   - Add plugin update checking

2.3. **Plugin Marketplace Prep**
   - Define plugin submission format
   - Create plugin validation rules
   - Design discovery API contract

2.4. **Performance Optimization**
   - Profile plugin initialization time
   - Implement lazy loading for non-critical plugins
   - Add plugin memory monitoring

2.5. **Security Hardening**
   - Implement plugin sandboxing
   - Add permission escalation detection
   - Create security audit logging

---

## Phase 3: WebAvanue Chrome Parity

**Rationale:** Bring WebAvanue to feature parity with Chrome browser.

### Tasks (from Docs/WebAvanue/plans/)

3.1. **Context Menu Integration**
   - Implement ContextMenuHandler.android.kt
   - Add ContextMenuTarget data class
   - Wire up to accessibility service

3.2. **Incognito Mode**
   - Create IncognitoIndicator component
   - Implement private browsing state
   - Ensure no history/cookies in incognito

3.3. **Reading List**
   - Implement ReadingListItem data class
   - Create reading list repository
   - Add save for later UI

3.4. **Tab Management**
   - Tab groups implementation
   - Tab search/filter
   - Recently closed tabs

3.5. **Downloads Manager**
   - Download progress tracking
   - Pause/resume downloads
   - Download history

---

## Phase 4: Documentation

**Rationale:** Capture completed work for team continuity.

### Tasks

4.1. **Update AI Handover Document**
   - Document Phase 4 completion
   - Add test coverage summary
   - Include API changes

4.2. **Create PluginSystem Developer Guide**
   - Plugin development tutorial
   - API reference
   - Best practices

4.3. **Update Architecture Decision Records**
   - ADR for AVID migration
   - ADR for plugin architecture
   - ADR for test strategy

4.4. **Create Integration Test Guide**
   - Test patterns documentation
   - TestUtils API reference
   - Adding new tests guide

---

## Phase 5: VoiceOSCoreNG Module

**Rationale:** Address module reference change (VoiceOSCore → VoiceOSCoreNG).

### Tasks

5.1. **Investigate Module Change**
   - Determine if VoiceOSCoreNG is rename or new module
   - Check for existing VoiceOSCoreNG code
   - Identify breaking changes

5.2. **Module Migration (if needed)**
   - Create VoiceOSCoreNG module structure
   - Migrate shared code
   - Update all module references

5.3. **Dependency Resolution**
   - Fix build.gradle.kts references
   - Update settings.gradle.kts
   - Verify all imports resolve

5.4. **Verification**
   - Build all modules
   - Run tests
   - Document changes

---

## Execution Strategy

### Swarm Configuration

| Agent | Responsibility |
|-------|---------------|
| E2E-Tester | Phase 1 verification |
| Plugin-Dev | Phase 2 features |
| WebAvanue-Dev | Phase 3 Chrome parity |
| Doc-Writer | Phase 4 documentation |
| Architect | Phase 5 module work |

### Parallel Execution

```
[Phase 1: E2E] ─────────────────┐
                                ├──→ [Phase 2: Plugin Phase 5]
[Phase 4: Docs] ───────────────┘

[Phase 3: WebAvanue] ──────────────→ (independent)

[Phase 5: VoiceOSCoreNG] ──────────→ (after investigation)
```

### Quality Gates

| Phase | Gate |
|-------|------|
| Phase 1 | All verification checks pass |
| Phase 2 | Tests pass, no regressions |
| Phase 3 | Feature tests pass |
| Phase 4 | Docs reviewed |
| Phase 5 | Build succeeds |

---

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| VoiceOSCoreNG unknown | Investigate first before other work |
| Plugin hot reload complex | Start with file watcher only |
| WebAvanue scope creep | Limit to specified Chrome features |

---

## Success Criteria

- [ ] App runs with plugin system active
- [ ] All 140 existing tests still pass
- [ ] Phase 5 features have test coverage
- [ ] WebAvanue features implemented
- [ ] Documentation current
- [ ] VoiceOSCoreNG resolved

