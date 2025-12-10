# Voice Input - VoiceOS Integration

**Status:** STUB - Awaiting VoiceOS Integration
**Target Phase:** Phase 4.0
**Last Updated:** 2025-11-23

---

## Overview

This directory contains a stub interface for VoiceOS voice input integration. The Android SpeechRecognizer implementation has been removed to prevent architectural conflicts when VoiceOS is merged.

## History

### Phase 1.2 - Android SpeechRecognizer (REMOVED)
**Date:** 2025-11-22
**Status:** ❌ Removed 2025-11-23

The following files were implemented but removed due to architectural concerns:
- `VoiceInputManager.kt` (350 LOC) - Android SpeechRecognizer wrapper
- `VoiceInputViewModel.kt` (215 LOC) - Voice input state management
- `VoiceInputButton.kt` (331 LOC) - Voice input UI component

**Reason for Removal:**
- Used Android-specific `SpeechRecognizer` API (not KMP-compatible)
- Network-dependent (Google's cloud-based recognition)
- Would conflict with VoiceOS architecture (on-device, KMP-based)
- Created technical debt for future VoiceOS integration

**Total Removed:** 893 LOC production code

---

## Current Implementation

### VoiceOSStub.kt
**Status:** ✅ Active
**Purpose:** Placeholder for VoiceOS integration

**Features:**
- `VoiceInputProvider` interface - Contract for future implementation
- `VoiceOSStub` class - Returns "not available" for all operations
- Error handling with user-friendly messages
- Clean migration path for VoiceOS integration

**Key Interface:**
```kotlin
interface VoiceInputProvider {
    fun startListening(callback: VoiceInputCallback, language: String = "en-US")
    fun stopListening()
    fun cancel()
    fun isActive(): Boolean
    fun isAvailable(): Boolean
    fun release()
}
```

---

## VoiceOS Integration Plan (Phase 4.0)

### Feature Comparison

| Feature | Android SpeechRecognizer (Removed) | VoiceOS (Planned) |
|---------|-----------------------------------|-------------------|
| Platform Support | Android only | Android, iOS, Desktop (KMP) |
| Recognition Type | Cloud-based (Google) | On-device |
| Latency | 500-1000ms | <100ms |
| Network Required | Yes | No |
| Privacy | Data sent to Google | Fully local |
| Wake Word | No | Yes (custom) |
| Custom Commands | No | Yes (via DSL) |
| Language Support | 100+ | English (initial), expandable |

### Migration Checklist

When VoiceOS is ready for integration:

- [ ] Add VoiceOS SDK dependency to `Chat/build.gradle.kts`
- [ ] Create `VoiceOSProvider` implementing `VoiceInputProvider`
- [ ] Add Hilt binding for `VoiceInputProvider` → `VoiceOSProvider`
- [ ] Implement all interface methods using VoiceOS SDK
- [ ] Add KMP support (commonMain, androidMain, iosMain)
- [ ] Create UI components (can reference removed `VoiceInputButton.kt`)
- [ ] Add integration tests
- [ ] Update documentation

---

## Design Decisions

### Why Stub Instead of Android SpeechRecognizer?

**Problem:**
- VoiceOS uses on-device, KMP-based architecture
- Android SpeechRecognizer is cloud-based and Android-specific
- Keeping both would create:
  - Duplicate voice input systems
  - Merge conflicts during VoiceOS integration
  - Confusion about which system to use
  - Technical debt

**Solution:**
- Remove Android SpeechRecognizer completely
- Use stub until VoiceOS is ready
- Clean migration path without conflicts
- Single voice input system (VoiceOS)

---

## Timeline

| Phase | Date | Status | Description |
|-------|------|--------|-------------|
| Phase 1.2 | 2025-11-22 | ❌ Removed | Android SpeechRecognizer implementation |
| Phase 3.0 | 2025-11-23 | ✅ Complete | Removed Android impl, created VoiceOS stub |
| Phase 4.0 | TBD | ⏳ Planned | VoiceOS SDK integration |
| Phase 5.0 | TBD | ⏳ Planned | Advanced voice features (wake words, custom commands) |

---

**Version:** 2.0
**Type:** Architecture Documentation
**Last Updated:** 2025-11-23
