# Implementation Plan: UniversalRPC

## Overview

| Attribute | Value |
|-----------|-------|
| Feature | UniversalRPC - Cross-Platform gRPC Module |
| Platforms | Android, iOS, Desktop (JVM), Web |
| Modules Affected | VoiceOS, AVA, Cockpit, WebAvanue, NLU |
| Swarm Recommended | **YES** (5 modules, 40+ tasks) |
| KMP Benefit | **HIGH** (>60% code reuse) |

---

## Chain of Thought (.cot) - Phase Ordering Reasoning

### Why This Order?

1. **Foundation First**: Create `Modules/UniversalRPC/` structure + Wire/gRPC dependencies
2. **Common Before Platform**: Proto files generate shared code for ALL platforms
3. **Android First**: Most complex (AIDL migration), validates approach
4. **VoiceOS Before AVA**: VoiceOS is the service provider, AVA is consumer
5. **Cockpit Integrates All**: Depends on VoiceOS + AVA working
6. **iOS/Desktop After Android**: Simpler (no AIDL migration, just clients)
7. **Web Last**: Depends on all services being gRPC-ready

### KMP Decision

| Condition | Check | Result |
|-----------|-------|--------|
| Android + iOS | ✅ Yes | KMP Required |
| Code reuse > 40% | ✅ ~65% | KMP Strongly Recommended |
| Shared protos | ✅ Yes | Single source of truth |

---

## Reasoning Over Time (.rot) - Approach Evolution

### Initial Approach: Expand UniversalIPC
- ❌ Rejected: UniversalIPC is nested in VoiceOS, limits scope

### Revised Approach: New Top-Level Module
- ✅ Selected: `Modules/UniversalRPC/` as independent module
- Reason: Serves ALL modules (VoiceOS, AVA, Cockpit, WebAvanue, NLU)

### Final Architecture Decision
- Common code in `Common/` subdirectory
- Platform code in `android/`, `ios/`, `desktop/`, `web/`
- App-specific code in `{platform}/{AppName}/`

---

## Swarm Configuration (.swarm)

### Agent Allocation

| Agent | Responsibility | Parallel Work |
|-------|----------------|---------------|
| Agent 1 | Proto definitions + Wire codegen | Phase 1-2 |
| Agent 2 | Android/VoiceOS AIDL migration | Phase 2-3 |
| Agent 3 | Android/AVA gRPC client | Phase 3 |
| Agent 4 | iOS + Desktop clients | Phase 4 |
| Agent 5 | IDEACODE updates + validation | Phase 1, 7 |

### Parallel Execution Points

```
Phase 1: Foundation (Single agent - sequential)
         ↓
Phase 2: ┌─ Agent 1: Proto definitions
         └─ Agent 5: IDEACODE registries
         ↓
Phase 3: ┌─ Agent 2: VoiceOS gRPC server
         ├─ Agent 3: AVA gRPC client
         └─ Agent 4: Cockpit bridge
         ↓
Phase 4: ┌─ Agent 2: iOS client
         └─ Agent 4: Desktop server
         ↓
Phase 5: ┌─ Agent 1: WebAvanue proto
         ├─ Agent 3: NLU proto
         └─ Agent 4: Web transport
         ↓
Phase 6: All agents: AIDL migration & cleanup
         ↓
Phase 7: Agent 5: Validation & documentation
```

---

## Phases

### Phase 1: Foundation & IDEACODE Updates

**Duration**: Day 1-2
**Agent**: Single (sequential setup required)

| # | Task | Priority | Files |
|---|------|----------|-------|
| 1.1 | Create `Modules/UniversalRPC/` folder structure | P0 | New directory |
| 1.2 | Create `Modules/UniversalRPC/build.gradle.kts` | P0 | New file |
| 1.3 | Create `Modules/UniversalRPC/Common/build.gradle.kts` | P0 | New file |
| 1.4 | Add Wire 4.9.9 to `gradle/libs.versions.toml` | P0 | Modify |
| 1.5 | Add grpc-kotlin 1.4.1 to `gradle/libs.versions.toml` | P0 | Modify |
| 1.6 | Add grpc-okhttp 1.62.2 to `gradle/libs.versions.toml` | P0 | Modify |
| 1.7 | Update `settings.gradle.kts` with module includes | P0 | Modify |
| 1.8 | Migrate `UniversalIPCEncoder.kt` to `Common/service/` | P1 | Move + Modify |
| 1.9 | Update `.ideacode/registries/FOLDER-REGISTRY.md` | P1 | Modify |
| 1.10 | Update `.ideacode/registries/MODULE-REGISTRY.md` | P1 | Modify |

---

### Phase 2: VoiceOS Proto Definitions

**Duration**: Day 2-3
**Agents**: 2 parallel (protos + IDEACODE)

| # | Task | Priority | Proto File |
|---|------|----------|------------|
| 2.1 | Define `cursor.proto` - VoiceCursor service | P0 | `Common/proto/cursor.proto` |
| 2.2 | Define `recognition.proto` - Voice recognition | P0 | `Common/proto/recognition.proto` |
| 2.3 | Define `vuid.proto` - VUID element service | P0 | `Common/proto/vuid.proto` |
| 2.4 | Define `exploration.proto` - JIT learning | P0 | `Common/proto/exploration.proto` |
| 2.5 | Define `voiceos.proto` - Main accessibility service | P0 | `Common/proto/voiceos.proto` |
| 2.6 | Configure Wire code generation in build.gradle.kts | P0 | `Common/build.gradle.kts` |
| 2.7 | Verify generated Kotlin code compiles | P0 | Build verification |
| 2.8 | Write unit tests for generated messages | P1 | `Common/test/` |

---

### Phase 3: Android VoiceOS + AVA Integration

**Duration**: Day 3-5
**Agents**: 3 parallel

| # | Task | Agent | Priority | Files |
|---|------|-------|----------|-------|
| 3.1 | Create `android/VoiceOS/` module structure | A2 | P0 | New directory |
| 3.2 | Implement Unix Domain Socket transport | A2 | P0 | `AndroidTransport.kt` |
| 3.3 | Create VoiceCursorService gRPC server | A2 | P0 | `VoiceCursorGrpcServer.kt` |
| 3.4 | Create `android/AVA/` module structure | A3 | P0 | New directory |
| 3.5 | Implement AVA gRPC client | A3 | P0 | `AvaGrpcClient.kt` |
| 3.6 | Update VoiceOSCore build.gradle.kts | A2 | P1 | Add UniversalRPC dep |
| 3.7 | Update AVA/Actions build.gradle.kts | A3 | P1 | Add UniversalRPC dep |
| 3.8 | Define `ava.proto` - AVA services | A3 | P0 | `Common/proto/ava.proto` |
| 3.9 | Define `cockpit.proto` - Cockpit management | A4 | P0 | `Common/proto/cockpit.proto` |
| 3.10 | Update `Common/Cockpit/VoiceOSBridge.kt` → gRPC | A4 | P1 | Modify implementation |
| 3.11 | Integration test: VoiceOS ↔ AVA via gRPC | A2+A3 | P0 | Test file |

---

### Phase 4: Cross-Platform Clients (iOS + Desktop)

**Duration**: Day 5-7
**Agents**: 2 parallel

| # | Task | Agent | Priority | Files |
|---|------|-------|----------|-------|
| 4.1 | Create `ios/` module structure | A2 | P0 | New directory |
| 4.2 | Configure Wire Swift generation | A2 | P0 | `ios/build.gradle.kts` |
| 4.3 | Implement iOS gRPC client | A2 | P0 | `IOSGrpcClient.kt` |
| 4.4 | Create `desktop/` module structure | A4 | P0 | New directory |
| 4.5 | Implement Cockpit gRPC server (JVM) | A4 | P0 | `CockpitGrpcServer.kt` |
| 4.6 | Implement Desktop gRPC client | A4 | P1 | `DesktopGrpcClient.kt` |
| 4.7 | Integration test: Android ↔ Desktop | A2+A4 | P0 | Test file |

---

### Phase 5: WebAvanue, NLU & Service Discovery

**Duration**: Day 7-9
**Agents**: 3 parallel

| # | Task | Agent | Priority | Files |
|---|------|-------|----------|-------|
| 5.1 | Define `webavanue.proto` - Browser commands | A1 | P0 | `Common/proto/webavanue.proto` |
| 5.2 | Define `nlu.proto` - NLU classification | A3 | P0 | `Common/proto/nlu.proto` |
| 5.3 | Create `web/` module structure | A4 | P0 | New directory |
| 5.4 | Implement gRPC-Web transport | A4 | P0 | `GrpcWebTransport.kt` |
| 5.5 | Implement mDNS service discovery | A1 | P1 | `ServiceRegistry.kt` |
| 5.6 | Add API key authentication | A1 | P1 | `AuthInterceptor.kt` |
| 5.7 | Update WebAvanue module dependencies | A4 | P1 | `build.gradle.kts` |
| 5.8 | Integration test: Browser ↔ Android | A4 | P1 | Test file |

---

### Phase 6: Full AIDL Migration

**Duration**: Day 9-14
**Agents**: All (swarm)

| # | Task | Priority | AIDL Files | Proto Replacement |
|---|------|----------|------------|-------------------|
| 6.1 | Migrate VoiceRecognitionService | P0 | 3 files | `recognition.proto` |
| 6.2 | Migrate VoiceOSService | P0 | 3 files | `voiceos.proto` |
| 6.3 | Migrate JITLearning | P0 | 8 files | `exploration.proto` |
| 6.4 | Migrate UUIDCreator | P0 | 3 files | `vuid.proto` |
| 6.5 | Migrate VoiceCursor | P0 | 3 files | `cursor.proto` |
| 6.6 | Migrate AVA/Actions | P0 | 3 files | `ava.proto` |
| 6.7 | Delete all AIDL files (26 files) | P1 | After verification |
| 6.8 | Delete `VoiceOSServiceBinder.java` | P1 | After verification |
| 6.9 | Delete old `UniversalIPC` library | P1 | After verification |
| 6.10 | Update all namespace imports | P0 | Across all modules |

---

### Phase 7: Validation & Documentation

**Duration**: Day 14-16
**Agent**: Single (documentation)

| # | Task | Priority | Files |
|---|------|----------|-------|
| 7.1 | Add IDEACODE validation: No AIDL in new code | P0 | Validation rules |
| 7.2 | Add IDEACODE validation: Proto in Common/ only | P0 | Validation rules |
| 7.3 | Add IDEACODE validation: Platform folder compliance | P0 | Validation rules |
| 7.4 | Update CROSS-MODULE-DEPENDENCIES.md | P1 | Registry file |
| 7.5 | Create tech stack documentation | P1 | `LD-NewAvanues-TechStack-*.md` |
| 7.6 | Performance benchmark: gRPC vs AIDL | P1 | Test results |
| 7.7 | Cross-platform integration test suite | P0 | Test files |
| 7.8 | Update Living Docs architecture | P2 | `LD-IDEACODE-Architecture-*.md` |

---

## Time Estimates

| Mode | Duration | Notes |
|------|----------|-------|
| **Sequential** | 16 days | Single developer |
| **Parallel (Swarm)** | 8 days | 5 agents |
| **Savings** | 8 days (50%) | Swarm recommended |

---

## Task Summary

| Phase | Tasks | Critical | Agent Days |
|-------|-------|----------|------------|
| Phase 1 | 10 | 7 | 2 |
| Phase 2 | 8 | 6 | 2 |
| Phase 3 | 11 | 8 | 3 |
| Phase 4 | 7 | 5 | 2 |
| Phase 5 | 8 | 5 | 2 |
| Phase 6 | 10 | 7 | 5 |
| Phase 7 | 8 | 4 | 2 |
| **TOTAL** | **62** | **42** | **18** |

---

## Files Summary

### New Files to Create
| Category | Count |
|----------|-------|
| Proto files | 9 |
| Gradle build files | 8 |
| Transport implementations | 6 |
| Service implementations | 10 |
| Test files | 8 |
| **Total** | **41** |

### Files to Modify
| Category | Count |
|----------|-------|
| Root gradle files | 2 |
| Module build.gradle.kts | 9 |
| IDEACODE registries | 3 |
| Bridge implementations | 2 |
| **Total** | **16** |

### Files to Delete (After Migration)
| Category | Count |
|----------|-------|
| AIDL files | 26 |
| Java binders | 1 |
| Old UniversalIPC | 1 directory |
| **Total** | **28** |

---

## YOLO Mode Execution

With `.yolo` flag, this plan auto-chains:

```
Plan Created ✓
    ↓
Tasks Generated → TodoWrite (62 tasks)
    ↓
Implementation Started
    ↓
Tests Run (per phase)
    ↓
Commit (per phase completion)
```

---

## Metadata

| Attribute | Value |
|-----------|-------|
| Plan ID | `VoiceOS-Plan-UniversalRPC-251228-V1` |
| Created | 2025-12-28 |
| Author | Claude (IDEACODE v10.3) |
| Spec Reference | N/A (derived from exploration) |
| Status | PENDING APPROVAL |

---

*Generated by /i.plan .rot .cot .tasks .swarm .yolo*
