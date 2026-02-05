# Recovery Roadmap - Module Consolidation (2026-02-05)

## Overview
This document tracks the recovery and implementation of lost work during module consolidation.

## Phase 1: VoiceAvanue Unified Module ✅ COMPLETED

### 1.1 VoiceAvanue Module Created
**Status**: ✅ COMPLETED (2026-02-05)
**Location**: `Modules/VoiceAvanue/`
**Features**:
- Combined VoiceOSCore + WebAvanue into single unified module
- Unified command model (`UnifiedCommand.kt`)
- SharedFlow-based event bus (`EventBus.kt`)
- Shared configuration and initialization
- Platform-specific implementations (Android, Desktop, iOS)

### 1.2 Foundation Module Renamed
**Status**: ✅ COMPLETED (2026-02-05)
**Actions completed**:
- Moved `Modules/Shared/Foundation/` to `Modules/Foundation/`
- Updated `settings.gradle.kts` include path
- Updated VoiceOSCore and WebAvanue dependencies

### 1.3 Gaze Module Scaffold Created
**Status**: ✅ COMPLETED (2026-02-05)
**Location**: `Modules/Gaze/`
**Features**:
- `GazeTypes.kt` - GazePoint, GazeSample, CalibrationState, etc.
- `GazeTracker.kt` - IGazeTracker interface with platform factory
- Platform stubs for Android and Desktop
- Integration with VoiceCursor for dwell click

## Phase 2: Cherry-Pick Historical Code

### 2.1 VoiceOSCore RPC Services (from commit 1c7c8d7d)
**Status**: ✅ COMPLETED (2026-02-05)
**Files to recover**:
- `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/rpc/VoiceOSAvuRpcServer.kt`
- `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/rpc/VoiceOSJsonRpcServer.kt`
- `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/rpc/VoiceOSRpcServer.kt`
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/rpc/AvuProtocol.kt`
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/rpc/VoiceOSService.kt`
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/rpc/messages/VoiceOSMessages.kt`
- `Modules/VoiceOSCore/src/desktopMain/kotlin/com/augmentalis/voiceoscore/rpc/VoiceOSRpcServer.kt`

### 2.2 AVA Unified App (from commit 1c7c8d7d)
**Status**: ✅ COMPLETED (2026-02-05)
**Location**: `apps/ava-unified/`
**Files to recover**:
- Build configuration and manifests
- Application entry point
- Services (Accessibility, VoiceRecognition, RPC, Cursor overlay)
- UI screens (Home, Browser, Settings)
- DI modules

## Phase 3: App Integration

### 3.1 Create New WebAvanue App
**Status**: ✅ COMPLETED (2026-02-05)
**Actions**:
- Current `android/apps/webavanue` was renamed to `webavanue-ipc-legacy`
- Create new `android/apps/webavanue` with RPC-based IPC (not broadcast)
- Integrate with VoiceAvanue module

## Phase 4: Integration & Wiring

### 4.1 gRPC/RPC Client Connections
**Status**: Pending
**Actions**:
- VoiceAvanue <-> WebAvanue bidirectional communication
- VoiceAvanue <-> VoiceCursor cursor control
- Event routing through EventBus

---

## New Module Structure

```
Modules/
├── Foundation/              # ✅ Common utilities (StateFlow, ViewModel, NumberToWords)
├── VoiceAvanue/             # ✅ NEW: Unified Voice + Browser module
│   ├── command/             #    UnifiedCommand, CommandCategory, CommandActionType
│   ├── event/               #    EventBus, VoiceAvanueEvent types
│   └── rpc/                 #    (to be added from historical commits)
├── Gaze/                    # ✅ NEW: Eye tracking module scaffold
│   ├── GazeTypes.kt         #    GazePoint, GazeSample, CalibrationState
│   └── GazeTracker.kt       #    IGazeTracker interface + factory
├── VoiceCursor/             # ✅ KMP cursor control (already migrated)
├── VoiceOSCore/             # Legacy (to be deprecated for VoiceAvanue)
└── WebAvanue/               # Legacy (to be deprecated for VoiceAvanue)
```

## Execution Order

1. ✅ Create VoiceAvanue unified module
2. ✅ Rename Foundation module
3. ✅ Create Gaze module scaffold
4. ✅ Cherry-pick VoiceOSCore RPC services
5. ✅ Cherry-pick ava-unified app
6. ✅ Create new webavanue app with RPC
7. **[NEXT]** Wire RPC connections between modules
8. Deprecate legacy modules

Each step committed and pushed separately.

## Summary of Commits (2026-02-05)

1. `c6cc646b` - feat: Add VoiceCursor KMP module and WebAvanue RPC services
2. `7bd59833` - feat: Add VoiceAvanue unified module, Gaze scaffold, and rename Foundation
3. `97c6a07c` - feat: Add VoiceOSCore RPC services (cherry-picked from 1c7c8d7d)
4. `0498ba4c` - feat: Add ava-unified app (cherry-picked from 1c7c8d7d)
5. `6e518cc5` - feat: Create new webavanue app with RPC integration
