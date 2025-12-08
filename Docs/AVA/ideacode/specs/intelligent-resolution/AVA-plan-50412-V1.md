# Implementation Plan: Intelligent Resolution System - Phase 1

## Overview

| Property | Value |
|----------|-------|
| Feature | Intelligent App & Platform Resolution - Core Infrastructure |
| Platforms | Android (primary), iOS (future) |
| Spec | Developer-Manual-Chapter71-Intelligent-Resolution-System.md |
| Estimated Tasks | 12 |
| Swarm Recommended | No (single platform) |

---

## Reverse Order Technique (ROT) Analysis

### End Goal
When user says "send email to john@example.com", AVA:
1. Automatically resolves the best email app (Gmail, Outlook, etc.)
2. Never asks twice - remembers user choice forever
3. Works seamlessly with zero configuration

### Working Backward

```
[END STATE]
    User says "email john" → Email opens in preferred app
                ↑
    [STEP 4] Action handlers use AppResolverService
                ↑
    [STEP 3] UI shows app selection (only once per capability)
                ↑
    [STEP 2] AppResolverService scans & resolves apps
                ↑
    [STEP 1] CapabilityRegistry + UserPreferencesRepository exist
                ↑
[START STATE]
    No app resolution exists - handlers launch system chooser
```

---

## Dependency Graph

```
┌─────────────────────────────────────────────────────────────────┐
│                     Phase 1 Dependencies                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  UserPreferencesRepository ◄─── AppResolverService              │
│           │                           │                         │
│           │                           │                         │
│           ▼                           ▼                         │
│  AppPreferences.sq              CapabilityRegistry              │
│  (SQLDelight schema)            (static definitions)            │
│                                                                 │
│                                                                 │
│  PreferencePromptManager ◄─── AppResolverService                │
│           │                                                     │
│           ▼                                                     │
│  AppPreferenceBottomSheet                                       │
│  (Compose UI)                                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Phase 1 Tasks (Execution Order)

### Task 1: Create SQLDelight Schema
**File:** `common/core/Data/src/main/sqldelight/.../AppPreferences.sq`

| Item | Value |
|------|-------|
| Priority | P0 - Foundation |
| Depends On | None |
| Blocks | Task 2, Task 3 |

**Tables:**
- `AppPreferences` - Store user's preferred app per capability
- `UsagePatterns` - Track usage for learning (future)

**Schema:**
```sql
CREATE TABLE AppPreferences (
    capability TEXT PRIMARY KEY NOT NULL,
    package_name TEXT NOT NULL,
    app_name TEXT NOT NULL,
    set_at INTEGER NOT NULL,
    set_by TEXT NOT NULL DEFAULT 'user'
);
```

---

### Task 2: Create CapabilityRegistry
**File:** `common/core/Domain/src/main/kotlin/.../resolution/CapabilityRegistry.kt`

| Item | Value |
|------|-------|
| Priority | P0 - Foundation |
| Depends On | None |
| Blocks | Task 4 |

**Capabilities to define:**
| Capability | Android Intents | Known Apps |
|------------|-----------------|------------|
| email | ACTION_SENDTO (mailto:) | Gmail, Outlook, Yahoo Mail |
| sms | ACTION_SENDTO (sms:) | Messages, WhatsApp, Telegram |
| music | MEDIA_PLAY_FROM_SEARCH | Spotify, YouTube Music, Amazon Music |
| video | ACTION_VIEW (video/*) | YouTube, Netflix, Prime Video |
| maps | ACTION_VIEW (geo:) | Google Maps, Waze, Apple Maps |
| calendar | ACTION_INSERT (event) | Google Calendar, Outlook |
| browser | ACTION_VIEW (http:) | Chrome, Firefox, Brave, Edge |
| notes | ACTION_SEND (text/plain) | Keep, Evernote, OneNote |

---

### Task 3: Create UserPreferencesRepository
**File:** `common/core/Data/src/main/java/.../repository/AppPreferencesRepository.kt`

| Item | Value |
|------|-------|
| Priority | P0 - Foundation |
| Depends On | Task 1 (schema) |
| Blocks | Task 4 |

**Methods:**
```kotlin
interface AppPreferencesRepository {
    suspend fun getPreferredApp(capability: String): String?
    suspend fun setPreferredApp(capability: String, packageName: String, appName: String)
    suspend fun clearPreferredApp(capability: String)
    suspend fun getAllPreferences(): Map<String, AppPreference>
}
```

---

### Task 4: Create AppResolverService
**File:** `common/core/Domain/src/main/kotlin/.../resolution/AppResolverService.kt`

| Item | Value |
|------|-------|
| Priority | P0 - Core Logic |
| Depends On | Task 2, Task 3 |
| Blocks | Task 5, Task 6 |

**Core Logic (ROT-derived):**
```
resolveApp(capability) {
    1. Check saved preference → return if valid
    2. Scan installed apps for capability
    3. If 1 app → auto-save & return
    4. If 0 apps → return NoneAvailable
    5. If 2+ apps → return MultipleAvailable (triggers UI)
}
```

**Resolution Results:**
```kotlin
sealed class AppResolution {
    data class Resolved(packageName: String, appName: String, source: ResolutionSource)
    data class MultipleAvailable(capability: String, apps: List<InstalledApp>, recommended: Int)
    data class NoneAvailable(capability: String, suggestions: List<KnownApp>)
}
```

---

### Task 5: Create PreferencePromptManager
**File:** `common/core/Domain/src/main/kotlin/.../resolution/PreferencePromptManager.kt`

| Item | Value |
|------|-------|
| Priority | P1 - UI Coordination |
| Depends On | Task 4 |
| Blocks | Task 6 |

**Responsibility:**
- Queue preference prompts (never show multiple simultaneously)
- Coordinate between AppResolverService and UI
- Handle "always ask" option

---

### Task 6: Create AppPreferenceBottomSheet
**File:** `common/Chat/src/main/kotlin/.../ui/components/AppPreferenceBottomSheet.kt`

| Item | Value |
|------|-------|
| Priority | P1 - UI |
| Depends On | Task 4, Task 5 |
| Blocks | Task 7 |

**UI Elements:**
- App icon + name for each available app
- "Recommended" badge for most popular
- "Always ask me" link at bottom
- Remember choice by default

---

### Task 7: Create Hilt DI Module
**File:** `android/ava/src/main/kotlin/.../di/ResolutionModule.kt`

| Item | Value |
|------|-------|
| Priority | P1 - Integration |
| Depends On | Task 3, Task 4, Task 5 |
| Blocks | Task 8 |

**Provides:**
- `AppPreferencesRepository` (singleton)
- `AppResolverService` (singleton)
- `PreferencePromptManager` (singleton)

---

### Task 8: Update SendEmailActionHandler
**File:** `common/Actions/src/main/kotlin/.../handlers/CommunicationActionHandlers.kt`

| Item | Value |
|------|-------|
| Priority | P1 - Integration |
| Depends On | Task 4, Task 7 |
| Blocks | Task 9 |

**Changes:**
- Inject `AppResolverService`
- Call `resolveApp("email")` before launching
- Handle `MultipleAvailable` by triggering prompt
- Use resolved package in intent

---

### Task 9: Update SendTextActionHandler
**File:** `common/Actions/src/main/kotlin/.../handlers/CommunicationActionHandlers.kt`

| Item | Value |
|------|-------|
| Priority | P1 - Integration |
| Depends On | Task 8 |
| Blocks | Task 10 |

Same pattern as Task 8 for SMS capability.

---

### Task 10: Add Settings Screen Section
**File:** `android/ava/src/main/kotlin/.../ui/settings/SettingsScreen.kt`

| Item | Value |
|------|-------|
| Priority | P2 - Polish |
| Depends On | Task 4 |
| Blocks | None |

**Features:**
- List all capabilities with current preference
- Tap to change preference
- "Reset all" option

---

### Task 11: Write Unit Tests
**Files:** `common/core/Domain/src/test/.../resolution/`

| Item | Value |
|------|-------|
| Priority | P1 - Quality |
| Depends On | Task 4 |
| Blocks | Task 12 |

**Test Cases:**
- `AppResolverServiceTest` - Resolution logic
- `CapabilityRegistryTest` - Capability definitions
- `AppPreferencesRepositoryTest` - Persistence

---

### Task 12: Write Integration Test
**File:** `android/ava/src/androidTest/.../resolution/AppResolutionIntegrationTest.kt`

| Item | Value |
|------|-------|
| Priority | P2 - Quality |
| Depends On | Task 8, Task 9 |
| Blocks | None |

**Test Scenarios:**
- Single app installed → auto-resolved
- Multiple apps → prompt shown once
- Preference saved → no prompt on second use
- App uninstalled → re-prompt

---

## File Structure (Final State)

```
common/
├── core/
│   ├── Data/
│   │   └── src/main/
│   │       ├── sqldelight/.../db/
│   │       │   └── AppPreferences.sq        ← Task 1
│   │       └── java/.../repository/
│   │           └── AppPreferencesRepository.kt  ← Task 3
│   │
│   └── Domain/
│       └── src/main/kotlin/.../resolution/
│           ├── CapabilityRegistry.kt        ← Task 2
│           ├── AppResolverService.kt        ← Task 4
│           └── PreferencePromptManager.kt   ← Task 5
│
├── Actions/
│   └── src/main/kotlin/.../handlers/
│       └── CommunicationActionHandlers.kt   ← Task 8, 9 (modify)
│
└── Chat/
    └── src/main/kotlin/.../ui/components/
        └── AppPreferenceBottomSheet.kt      ← Task 6

android/ava/
└── src/main/kotlin/.../
    ├── di/
    │   └── ResolutionModule.kt              ← Task 7
    └── ui/settings/
        └── SettingsScreen.kt                ← Task 10 (modify)
```

---

## Critical Path

```
Task 1 (Schema) ─┬─► Task 3 (Repository) ─┬─► Task 4 (Service) ─► Task 8 (Email Handler)
                 │                         │
Task 2 (Registry)┴─────────────────────────┘
```

**Minimum viable path:** Tasks 1, 2, 3, 4, 7, 8 = Core resolution working for email

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Package manager query returns too many apps | Filter by known apps first, then intent query |
| System chooser still appears | Set explicit package on intent |
| SQLDelight migration needed | Use new table, no migration (fresh install) |
| UI blocking main thread | All resolution in suspend functions |

---

## Success Criteria

| Criteria | Measurement |
|----------|-------------|
| Single app auto-resolves | No prompt shown when only 1 email app |
| Multiple apps prompts once | Prompt shown first time, never again |
| Preference persists | Survives app restart |
| Settings shows preferences | All capabilities visible in settings |
| Tests pass | 90%+ coverage on resolution logic |

---

## Next Steps

After plan approval:
1. `/iimplement` to start implementation
2. Or `/itasks` to generate TodoWrite task list

---

## Author

Manoj Jhawar

## Metadata

| Field | Value |
|-------|-------|
| Created | 2025-12-04 |
| Spec Source | Developer-Manual-Chapter71 |
| Method | Reverse Order Technique (ROT) |
