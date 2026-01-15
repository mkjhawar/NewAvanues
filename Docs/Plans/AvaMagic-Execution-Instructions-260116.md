# AvaMagic Integration - Execution Instructions

**Date:** 2026-01-16 | **For:** Multi-Terminal AI Execution

---

## Quick Start

### Prerequisites

1. Ensure CodeAvenue API is running:
   ```bash
   curl http://localhost:3850/health
   ```

2. Pull latest from branch:
   ```bash
   cd /Volumes/M-Drive/Coding/NewAvanues
   git pull origin Refactor-TempAll
   ```

---

## Terminal Allocation

| Terminal | Stream | Focus | Est. Time |
|----------|--------|-------|-----------|
| A | Stream 1 | NLU Integration | 2-3 hours |
| B | Stream 2 | LLM Fallback | 2-3 hours |
| C | Stream 3 | IPC Implementation | 3-4 hours |
| D | Stream 4 | Intelligent Scanning | 2-3 hours |
| E | Stream 5 | VoiceIntegration | 2-3 hours |
| F | Stream 6 | Speech Engines | 4-5 hours |

---

## Execution Commands

### Terminal A - NLU Integration

```bash
# Create worktree
cd /Volumes/M-Drive/Coding/NewAvanues
git worktree add ../worktrees/avamagic-nlu Refactor-TempAll
cd ../worktrees/avamagic-nlu

# Start Claude session
claude "Execute Stream 1 (NLU Integration) from Docs/plans/AvaMagic-Integration-Plan-260116-V1.md.
Focus on:
1. Create NluBridge.kt in voiceoscoreng
2. Inject EnhancedNluService
3. Wire NLU processing into AccessibilityService
4. Add confidence threshold handling"
```

### Terminal B - LLM Fallback

```bash
# Create worktree
cd /Volumes/M-Drive/Coding/NewAvanues
git worktree add ../worktrees/avamagic-llm Refactor-TempAll
cd ../worktrees/avamagic-llm

# Start Claude session
claude "Execute Stream 2 (LLM Fallback) from Docs/plans/AvaMagic-Integration-Plan-260116-V1.md.
Focus on:
1. Create LlmFallbackHandler.kt
2. Inject LocalLLMProvider and CloudLLMProvider
3. Implement confidence-based routing
4. Wire into NLU bridge"
```

### Terminal C - IPC Implementation

```bash
# Create worktree
cd /Volumes/M-Drive/Coding/NewAvanues
git worktree add ../worktrees/avamagic-ipc Refactor-TempAll
cd ../worktrees/avamagic-ipc

# Start Claude session
claude "Execute Stream 3 (IPC Implementation) from Docs/plans/AvaMagic-Integration-Plan-260116-V1.md.
Focus on:
1. Complete IPCManager.android.kt - all 5 stub methods
2. Create AIDL interface for cross-app communication
3. Create VoiceOSConnection.kt in VoiceUI app
4. Enable VoiceUI to command AccessibilityService"
```

### Terminal D - Intelligent Scanning

```bash
# Create worktree
cd /Volumes/M-Drive/Coding/NewAvanues
git worktree add ../worktrees/avamagic-scanning Refactor-TempAll
cd ../worktrees/avamagic-scanning

# Start Claude session
claude "Execute Stream 4 (Intelligent Scanning) from Docs/plans/AvaMagic-Integration-Plan-260116-V1.md.
Focus on:
1. Create ScanOptimizer.kt for partial tree scanning
2. Implement element-level change detection
3. Create BatteryAwareScanner.kt
4. Add WorkManager for idle-time scanning"
```

### Terminal E - VoiceIntegration

```bash
# Create worktree
cd /Volumes/M-Drive/Coding/NewAvanues
git worktree add ../worktrees/avamagic-voice Refactor-TempAll
cd ../worktrees/avamagic-voice

# Start Claude session
claude "Execute Stream 5 (VoiceIntegration) from Docs/plans/AvaMagic-Integration-Plan-260116-V1.md.
Focus on:
1. Resolve 14 TODOs in VoiceIntegration.kt
2. Implement voice command registration
3. Implement command routing
4. Wire to AccessibilityService"
```

### Terminal F - Speech Engines

```bash
# Create worktree
cd /Volumes/M-Drive/Coding/NewAvanues
git worktree add ../worktrees/avamagic-speech Refactor-TempAll
cd ../worktrees/avamagic-speech

# Start Claude session
claude "Execute Stream 6 (Speech Engines) from Docs/plans/AvaMagic-Integration-Plan-260116-V1.md.
Focus on:
1. Complete GoogleAuth.kt - OAuth2 implementation
2. Complete GoogleNetwork.kt - streaming
3. Complete WhisperNative.kt - JNI bindings
4. Test on-device recognition"
```

---

## Merge Procedure

After all terminals complete:

```bash
# Return to main repo
cd /Volumes/M-Drive/Coding/NewAvanues

# Merge each worktree
git worktree list

# For each worktree, commit and merge:
cd ../worktrees/avamagic-nlu
git add -A && git commit -m "feat(nlu): Integrate NLU into AccessibilityService"
git push origin Refactor-TempAll

# Repeat for each worktree...

# Clean up worktrees
git worktree remove ../worktrees/avamagic-nlu
git worktree remove ../worktrees/avamagic-llm
# etc.
```

---

## Verification Steps

### Build Test
```bash
cd /Volumes/M-Drive/Coding/NewAvanues
./gradlew :android:apps:voiceoscoreng:assembleDebug
./gradlew :android:apps:VoiceUI:assembleDebug
```

### Install and Test
```bash
adb install android/apps/voiceoscoreng/build/outputs/apk/debug/voiceoscoreng-debug.apk
adb install android/apps/VoiceUI/build/outputs/apk/debug/VoiceUI-debug.apk

# Enable accessibility service
adb shell settings put secure enabled_accessibility_services com.augmentalis.voiceoscoreng/.service.VoiceOSAccessibilityService

# Test voice command
adb shell am broadcast -a com.augmentalis.voiceoscoreng.VOICE_COMMAND --es command "open settings"
```

### Integration Test
```bash
./gradlew :Modules:AvaMagic:test
./gradlew :android:apps:voiceoscoreng:connectedAndroidTest
```

---

## Troubleshooting

### Merge Conflicts
If conflicts occur during merge:
1. Identify conflicting files
2. Use coordinating terminal to resolve
3. Run verification tests

### Build Failures
1. Check gradle sync
2. Verify dependency versions in `build.gradle.kts`
3. Clean build: `./gradlew clean`

### Service Not Starting
1. Check AndroidManifest.xml for service declaration
2. Verify accessibility settings
3. Check logcat: `adb logcat -s VoiceOSA11yService`

---

## Contact Points

- Analysis Report: `Docs/analysis/AvaMagic-Integration-Analysis-260116-V1.md`
- Implementation Plan: `Docs/plans/AvaMagic-Integration-Plan-260116-V1.md`
- Master Documentation: `Docs/MasterDocs/AvaMagic/Avanues-Suite-Master-Documentation-V1.md`

---

**Instructions Created:** 2026-01-16
