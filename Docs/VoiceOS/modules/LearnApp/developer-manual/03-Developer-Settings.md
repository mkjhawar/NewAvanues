# Chapter 3: Developer Settings

**Module**: LearnApp
**Package**: `com.augmentalis.voiceoscore.learnapp.settings`
**Last Updated**: 2025-12-08

---

## Developer Settings & Configurable Parameters (v2.1 - 2025-12-05)

### Overview

LearnApp provides **51 configurable settings** via `LearnAppDeveloperSettings`. All previously hardcoded values can now be adjusted at runtime without code changes.

**Files:**
- `settings/LearnAppDeveloperSettings.kt` - Settings backend (51 settings)
- `settings/ui/DeveloperSettingsActivity.kt` - Standalone activity (NEW)
- `settings/ui/DeveloperSettingsFragment.kt` - Settings UI fragment
- `settings/ui/DeveloperSettingsViewModel.kt` - UI ViewModel
- `settings/ui/SettingsAdapter.kt` - RecyclerView adapter
- `settings/ui/SettingItem.kt` - Data models

### Quick Start

```kotlin
val settings = LearnAppDeveloperSettings(context)

// Get current values
val maxDepth = settings.getMaxExplorationDepth()        // Default: 10
val timeout = settings.getExplorationTimeoutMs()        // Default: 5 minutes

// Modify values
settings.setMaxExplorationDepth(15)
settings.setExplorationTimeoutMs(600_000)  // 10 minutes

// Get all settings (for debugging)
val allSettings = settings.getAllSettings()

// Get settings by category (for UI)
val categories = settings.getSettingsByCategory()

// Reset to defaults
settings.resetToDefaults()
```

### Settings by Category

#### 1. Exploration Settings

| Setting | Method | Default | Range | Purpose |
|---------|--------|---------|-------|---------|
| Max Depth | `getMaxExplorationDepth()` | 10 | 1-50 | DFS traversal limit |
| Timeout | `getExplorationTimeoutMs()` | 300,000 | 30s-1hr | Max exploration time |
| Initial Screen Count | `getEstimatedInitialScreenCount()` | 20 | 1-100 | Progress estimation |
| Completeness Threshold | `getCompletenessThresholdPercent()` | 95% | 50-100% | "Fully learned" threshold |
| Similarity Threshold | `getScreenHashSimilarityThreshold()` | 0.85 | 0.5-1.0 | Screen revisit detection |

#### 2. Navigation Settings

| Setting | Method | Default | Range | Purpose |
|---------|--------|---------|-------|---------|
| Bounds Tolerance | `getBoundsTolerancePixels()` | 20 | 0-100 | Element refresh matching |
| Max Click Failures | `getMaxConsecutiveClickFailures()` | 5 | 1-20 | Screen abandonment threshold |
| Max Back Attempts | `getMaxBackNavigationAttempts()` | 3 | 1-10 | Recovery attempts |
| Min Alias Length | `getMinAliasTextLength()` | 3 | 1-10 | Element alias generation |

#### 3. Login & Consent Settings

| Setting | Method | Default | Range | Purpose |
|---------|--------|---------|-------|---------|
| Login Timeout | `getLoginTimeoutMs()` | 600,000 | 1-30min | Wait for user login |
| Permission Interval | `getPermissionCheckIntervalMs()` | 1,000 | 500ms-10s | Permission polling |
| Request Expiry | `getPendingRequestExpiryMs()` | 60,000 | 10s-5min | Consent timeout |

#### 4. Scrolling Settings

| Setting | Method | Default | Range | Purpose |
|---------|--------|---------|-------|---------|
| Max Attempts | `getMaxScrollAttempts()` | 5 | 1-20 | Scrolls per container |
| Scroll Delay | `getScrollDelayMs()` | 500 | 100-2000ms | Between-scroll delay |
| Elements Per Scrollable | `getMaxElementsPerScrollable()` | 20 | 5-100 | Per-container limit |
| Vertical Iterations | `getMaxVerticalScrollIterations()` | 50 | 5-200 | Max vertical scrolls |
| Horizontal Iterations | `getMaxHorizontalScrollIterations()` | 20 | 5-100 | Max horizontal scrolls |
| Container Depth | `getMaxScrollableContainerDepth()` | 2 | 1-10 | Nesting depth |
| Children Per Container | `getMaxChildrenPerScrollContainer()` | 50 | 10-200 | Child element limit |

#### 5. Click & Interaction Settings

| Setting | Method | Default | Range | Purpose |
|---------|--------|---------|-------|---------|
| Click Delay | `getClickDelayMs()` | 300 | 100-2000ms | Post-click settling delay (NEW) |
| Retry Attempts | `getClickRetryAttempts()` | 3 | 1-10 | Click failure retries |
| Retry Delay | `getClickRetryDelayMs()` | 200 | 50-1000ms | Between-retry delay |
| Screen Processing Delay | `getScreenProcessingDelayMs()` | 1000 | 100-5000ms | Screen transition delay (NEW) |

#### 6. UI Detection Settings

| Setting | Method | Default | Range | Purpose |
|---------|--------|---------|-------|---------|
| Touch Target Size | `getMinTouchTargetSizePixels()` | 48 | 24-96 | Material Design minimum |
| Bottom Region Threshold | `getBottomScreenRegionThreshold()` | 1600 | 500-3000 | Bottom nav detection |
| Expansion Wait | `getExpansionWaitDelayMs()` | 500 | 100-2000ms | Animation wait |
| Expansion Confidence | `getExpansionConfidenceThreshold()` | 0.65 | 0.3-1.0 | Detection threshold |
| Launch Retries | `getMaxAppLaunchEmitRetries()` | 3 | 1-10 | Event emit retries |

#### 7. JIT Learning Settings

| Setting | Method | Default | Range | Purpose |
|---------|--------|---------|-------|---------|
| Capture Timeout | `getJitCaptureTimeoutMs()` | 200 | 50-1000ms | Per-capture timeout |
| Traversal Depth | `getJitMaxTraversalDepth()` | 10 | 1-30 | Tree traversal limit |
| Max Elements | `getJitMaxElementsCaptured()` | 100 | 10-500 | Per-capture element limit |

#### 8. State Detection Settings

| Setting | Method | Default | Range | Purpose |
|---------|--------|---------|-------|---------|
| Transient Duration | `getTransientStateDurationMs()` | 500 | 100-2000ms | Transient state threshold |
| Flicker Interval | `getFlickerStateIntervalMs()` | 200 | 50-1000ms | Flicker detection interval |
| Stable Duration | `getStableStateDurationMs()` | 2000 | 500-10000ms | Stable state threshold |
| Flicker Occurrences | `getMinFlickerOccurrences()` | 3 | 2-10 | Min flicker count |
| Detection Window | `getFlickerDetectionWindowMs()` | 5000 | 1-30s | Flicker window |
| Major Penalty | `getPenaltyMajorContradiction()` | 0.3 | 0.0-1.0 | Confidence penalty |
| Moderate Penalty | `getPenaltyModerateContradiction()` | 0.15 | 0.0-1.0 | Confidence penalty |
| Minor Penalty | `getPenaltyMinorContradiction()` | 0.05 | 0.0-1.0 | Confidence penalty |
| Secondary Threshold | `getSecondaryStateConfidenceThreshold()` | 0.5 | 0.1-1.0 | Detection threshold |

#### 9. Quality & Processing Settings

| Setting | Method | Default | Range | Purpose |
|---------|--------|---------|-------|---------|
| Text Weight | `getQualityWeightText()` | 0.3 | 0.0-1.0 | Quality scoring |
| Content Desc Weight | `getQualityWeightContentDesc()` | 0.25 | 0.0-1.0 | Quality scoring |
| Resource ID Weight | `getQualityWeightResourceId()` | 0.3 | 0.0-1.0 | Quality scoring |
| Actionable Weight | `getQualityWeightActionable()` | 0.15 | 0.0-1.0 | Quality scoring |
| Batch Size | `getMaxCommandBatchSize()` | 200 | 10-1000 | Database batch limit |
| Min Label Length | `getMinGeneratedLabelLength()` | 2 | 1-10 | Label generation |

#### 10. UI & Debug Settings

| Setting | Method | Default | Range | Purpose |
|---------|--------|---------|-------|---------|
| Overlay Auto-Hide | `getOverlayAutoHideDelayMs()` | 5000 | 1-30s | Overlay timeout |
| Verbose Logging | `isVerboseLoggingEnabled()` | false | - | Debug logs |
| Screenshot on Screen | `isScreenshotOnScreenEnabled()` | false | - | Screen capture |

### Developer Settings UI

#### Launch via Activity (Recommended)

```kotlin
// From anywhere in VoiceOS - uses standalone activity
LearnAppIntegration.openDeveloperSettings(context)
```

This launches `DeveloperSettingsActivity` which hosts the fragment with proper back navigation.

#### Launch via Fragment (Embedded)

```kotlin
// Embed in existing activity
supportFragmentManager.beginTransaction()
    .replace(R.id.container, DeveloperSettingsFragment.newInstance())
    .addToBackStack(null)
    .commit()
```

**UI Features:**
- Tabbed interface by category (10 tabs)
- Number inputs with validation
- Toggle switches for booleans
- Sliders for percentages/thresholds
- Reset to defaults button
- Standalone activity with toolbar

### Integration Points

Settings are wired into these components:

| Component | Settings Used |
|-----------|---------------|
| ExplorationEngine | exploration, navigation, state detection |
| ScreenExplorer | scrolling, UI detection |
| ScrollExecutor | scrolling settings |
| ElementClassifier | UI detection |
| ExpandableControlDetector | detection settings |
| AppLaunchDetector | detection settings |
| JitElementCapture | JIT settings |
| TemporalStateValidator | state detection |
| NegativeIndicatorAnalyzer | state detection |
| MultiStateDetectionEngine | state detection |
| MetadataQuality | quality weights |
| LearnAppCore | processing settings |
| ConsentDialogManager | consent settings |

### Storage

Settings persist via SharedPreferences (`learnapp_developer_settings`)

### Delay Wiring (v2.1 Update)

All 24 hardcoded `delay()` calls have been replaced with configurable settings:

| File | Delays Replaced | Settings Used |
|------|----------------|---------------|
| ExplorationEngine.kt | 20 | Click, Scroll, Screen Processing |
| ScrollExecutor.kt | 4 | Scroll, Click Retry |
| ScreenExplorer.kt | 0 (no delays) | - |
| JitElementCapture.kt | 0 (already wired) | JIT Capture Timeout |

**Before (hardcoded):**
```kotlin
delay(300)  // Magic number
delay(500)  // Another magic number
```

**After (configurable):**
```kotlin
delay(developerSettings.getClickDelayMs())
delay(developerSettings.getScrollDelayMs())
```

### Verbose Logging (v2.1 Update)

Debug logging is now controllable via `isVerboseLoggingEnabled()`:

| File | Logs Wrapped | Log Types |
|------|--------------|-----------|
| ExplorationEngine.kt | 27 | Performance, C-Lite, Clicks |
| ScreenExplorer.kt | 5 | Element classification |

**Usage:**
```kotlin
if (developerSettings.isVerboseLoggingEnabled()) {
    Log.d(TAG, "Detailed debug info: $details")
}
```

Error and warning logs are NOT wrapped (always visible).

---
## AI Context Serialization (2025-12-05) ⭐ NEW

### Overview

`AIContextSerializer` now supports full round-trip serialization with the `deserializeContext()` method.

**File:** `ai/AIContextSerializer.kt`

### AVU Format

LearnApp uses AVU (Avanues Universal Format), a line-based text format:

| IPC Code | Data | Purpose |
|----------|------|---------|
| APP | package:name:timestamp | App metadata |
| STA | screens:elements:paths:avg:depth:coverage | Statistics |
| SCR | hash:activity:timestamp:count | Screen definition |
| ELM | uuid:label:type:actions:location | Element |
| NAV | from:to:uuid:label:timestamp | Navigation path |

### Usage

```kotlin
val serializer = AIContextSerializer(context)

// Save context
serializer.saveToFile(aiContext)

// Load context (now works!)
val restored = serializer.loadFromFile(packageName)
```

### Implementation Details

- Parses line-by-line with IPC code routing
- Handles missing fields with safe defaults
- Validates required APP entry
- Generates fallback stats if STA missing
- Skips comments (#) and separators (---)

---
## Scrollable Container Tracking (2025-12-05)

### Overview

ExplorationEngine now tracks scrollable containers discovered during exploration. Previously this stat was hardcoded to 0.

### Implementation

| Component | Change |
|-----------|--------|
| `ExplorationEngine` | Added `scrollableContainersFound` counter |
| `ScreenExplorer` | Returns `CollectionResult` with element count + scrollable count |
| `ExplorationStats` | `scrollableContainersFound` now populated |

### Code Flow

```
ExplorationEngine.startExploration()
  └── scrollableContainersFound = 0  // Reset counter

ScreenExplorer.exploreScreen()
  └── collectAllElements()
      └── scrollDetector.findScrollableContainers()
          └── Returns count in CollectionResult

ExplorationEngine (on each new screen)
  └── scrollableContainersFound += result.scrollableContainerCount

ExplorationEngine.createExplorationStats()
  └── scrollableContainersFound = this.scrollableContainersFound  // Actual value
```

---
## App Completion Marking (2025-12-05) ⭐ NEW

### Overview

New `markAppAsFullyLearned()` method in LearnAppRepository marks apps as 100% complete.

**File:** `database/repository/LearnAppRepository.kt`

### Usage

```kotlin
repository.markAppAsFullyLearned(packageName, System.currentTimeMillis())
```

### What It Does

1. Updates `status` field to `"LEARNED"`
2. Updates `exploration_status` to `COMPLETE` (legacy compatibility)
3. Sets `progress` to `100%`
4. Updates `screens_explored` with actual count
5. Sets `last_updated_at` timestamp

### Trigger Condition

Called in ExplorationEngine when `clickStats.overallCompleteness >= 95%`.

---

---

**Navigation**: [← Previous: Exploration Engine](./02-Exploration-Engine.md) | [Index](./00-Index.md) | [Next: Database & Persistence →](./04-Database-Persistence.md)
