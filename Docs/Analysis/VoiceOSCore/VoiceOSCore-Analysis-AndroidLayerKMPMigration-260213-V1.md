# VoiceOSCore Analysis: Android App-Layer KMP Migration Potential

**Date:** 2026-02-13
**Branch:** IosVoiceOS-Development
**Scope:** `apps/avanues/src/main/kotlin/.../service/` — 9 remaining files
**Previous:** OverlayStateManager, OverlayItemGenerator, OverlayNumberingExecutor migrated (commit cfaf968a)

---

## Summary

| Verdict | Files | Description |
|---------|-------|-------------|
| **KMP-Ready (split)** | 2 | Logic extractable, platform wrapper stays |
| **Must Stay Android** | 7 | Deep Android API dependencies |

---

## Class-by-Class Analysis

### 1. ElementExtractor.kt (140 lines)

**Verdict: SPLIT — extract algorithm to KMP, keep Android adapter**

| Aspect | Detail |
|--------|--------|
| Android deps | `AccessibilityNodeInfo`, `android.graphics.Rect` |
| Pure logic | `isDynamicContainer()` — string matching against container type set |
| Pure logic | `dynamicContainerTypes` — static set of RecyclerView/ListView/etc. |
| Pure logic | Element-to-`ElementInfo` field mapping — pure data transformation |
| Android-only | Tree traversal via `node.getChild(i)`, `node.getBoundsInScreen(bounds)`, `child.recycle()` |
| Pure logic | Hash-based deduplication (`seenHashes`, `DuplicateInfo`) |

**What can move to KMP:**
- `isDynamicContainer(className: String): Boolean` — zero platform deps
- `dynamicContainerTypes` set — static data
- `DuplicateInfo` data class — pure model
- Deduplication logic — operates on `ElementInfo` (already KMP)

**What must stay Android:**
- `extractElements()` itself — deeply coupled to `AccessibilityNodeInfo` tree traversal. Every line reads from `node.className`, `node.getBoundsInScreen()`, `node.childCount`, `node.getChild(i)`, `child.recycle()`. This is the Android accessibility tree walker.
- `isPerformClickable()` extension — extends `AccessibilityNodeInfo`

**Migration approach:** Extract `isDynamicContainer()`, `dynamicContainerTypes`, and `DuplicateInfo` to a KMP utility file. The tree walker stays Android-only, but its iOS equivalent would use `UIAccessibility` APIs and feed into the same KMP `ElementInfo` model.

**Effort: Low** — Small extraction, main function stays.

---

### 2. ScreenCacheManager.kt (149 lines)

**Verdict: SPLIT — extract hashing algorithm to KMP, keep Android adapter**

| Aspect | Detail |
|--------|--------|
| Android deps | `AccessibilityNodeInfo`, `Resources` (for display metrics) |
| Pure logic | Hash construction algorithm: signature building, content digest, time normalization |
| Pure logic | `createScreenInfo()` — constructs a KMP `ScreenInfo` data class |
| Pure logic | Regex patterns for time/count normalization |
| Android-only | `collectElementSignatures()` — walks `AccessibilityNodeInfo` tree |
| Android-only | `node.getBoundsInScreen()`, `node.className`, `node.childCount`, `child.recycle()` |
| Android-only | `resources.displayMetrics.widthPixels/heightPixels` |

**What can move to KMP:**
- `createScreenInfo()` — already creates a KMP type, just uses `System.currentTimeMillis()` (replaceable with `kotlinx.datetime`)
- Regex normalization patterns (`timePattern`, `relativeTimePattern`, `countPattern`)
- Hash assembly logic (signature sorting, content digest first5/last5 strategy)

**What must stay Android:**
- `generateScreenHash(rootNode: AccessibilityNodeInfo)` — walks the tree
- `collectElementSignatures()` — reads `AccessibilityNodeInfo` properties
- Constructor dependency on `Resources` for screen dimensions

**Migration approach:** Extract a `ScreenHashAssembler` to KMP that takes pre-collected signatures + dimensions and produces a hash. The Android `ScreenCacheManager` collects signatures from `AccessibilityNodeInfo` and delegates to the KMP assembler. iOS would collect signatures from `UIAccessibility` and use the same assembler.

**Effort: Medium** — Algorithm decomposition needed.

---

### 3. DynamicCommandGenerator.kt (220 lines)

**Verdict: MUST STAY ANDROID**

| Aspect | Detail |
|--------|--------|
| Android deps | `AccessibilityNodeInfo`, `Resources` |
| Core method | `processScreen(rootNode: AccessibilityNodeInfo, ...)` — entry point requires Android node |
| Android-only | `calculateStructuralChangeRatio()` — walks `AccessibilityNodeInfo` tree |
| Android-only | `collectTopLevelSignatures()` — reads `node.className`, `node.viewIdResourceName`, `node.childCount`, `node.getChild(i)` |
| Already KMP | Delegates to: `OverlayItemGenerator` (KMP), `OverlayStateManager` (KMP), `OverlayNumberingExecutor` (KMP), `ElementLabels` (KMP) |

**Analysis:** This is the **orchestrator** that connects the Android accessibility tree to the KMP overlay pipeline. Its two private methods (`calculateStructuralChangeRatio`, `collectTopLevelSignatures`) both walk `AccessibilityNodeInfo` — they cannot be made cross-platform without abstracting the entire accessibility tree, which is overkill.

The good news: the heavy logic it delegates to (`ElementLabels.deriveElementLabels`, `OverlayItemGenerator.generateForListApp`, `OverlayNumberingExecutor.assignNumbers`, `OverlayStateManager.updateNumberedOverlayItems`) is already in KMP. This class is essentially an Android-specific thin glue layer. iOS would have its own equivalent using `UIAccessibility`.

**Effort: N/A** — stays as-is.

---

### 4. CommandOverlayService.kt (388 lines)

**Verdict: MUST STAY ANDROID**

| Aspect | Detail |
|--------|--------|
| Android deps | `Service`, `WindowManager`, `ComposeView`, `NotificationChannel`, `NotificationManager`, `PendingIntent`, `PixelFormat` |
| Lifecycle | `LifecycleOwner`, `SavedStateRegistryOwner` — Android component lifecycle |
| Rendering | Compose UI: `NumberBadge()`, `NumbersOverlayContent()`, `NumbersInstructionPanel()` |
| System | `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` — draws over other apps |

**Analysis:** This is the Android **rendering** layer for overlay badges. Every line is deeply Android:
- Foreground `Service` with notification
- `WindowManager.addView()` for system-level overlay
- Compose UI rendering for badge circles, labels, instruction panel
- `LifecycleOwner` + `SavedStateRegistryOwner` for Compose in a `Service` context

iOS would need a completely different implementation (likely a `UIWindow` + SwiftUI layer). The only shared abstraction is the data it observes — `OverlayStateManager` StateFlows — which are already KMP.

**Effort: N/A** — platform-specific by nature.

---

### 5. VoiceAvanueAccessibilityService.kt (559 lines)

**Verdict: MUST STAY ANDROID**

| Aspect | Detail |
|--------|--------|
| Android deps | `AccessibilityService` (subclass of VoiceOSAccessibilityService), `Settings`, `Intent`, `DataStore`, `Hilt` |
| Core role | Concrete `AccessibilityService` registered in AndroidManifest — Android's entry point for screen reading |
| Hilt DI | `@EntryPoint`, `EntryPointAccessors` — Android DI |
| Service wiring | Creates `OverlayNumberingExecutor`, `DynamicCommandGenerator`, `VoiceOSCore`, `CommandManager` |
| Event handling | `onServiceReady()`, `onCommandsUpdated()`, `onInAppNavigation()`, `onScrollSettled()` — all accessibility event callbacks |
| Bridge | Bridges VoiceOSCore speech results to voice command processing |

**Analysis:** This is the **app-level entry point** — the concrete `AccessibilityService` that Android requires to be declared in the manifest. It's the top-level wiring layer that connects:
- Android accessibility events → KMP VoiceOSCore
- KMP speech results → voice command execution
- DataStore settings → cursor overlay lifecycle
- Hilt dependency injection

Cannot move to KMP. iOS has no equivalent of `AccessibilityService` — it would use `UIAccessibility` observation from a different entry point.

**Effort: N/A** — fundamental Android component.

---

### 6. AccessibilityClickDispatcher.kt (46 lines)

**Verdict: MUST STAY ANDROID**

| Aspect | Detail |
|--------|--------|
| Android deps | `GestureDescription`, `Path`, `AccessibilityService.dispatchGesture()` |
| Interface | Implements KMP `ClickDispatcher` interface (from VoiceCursor) |
| Size | 46 lines — very small |

**Analysis:** Implements the KMP `ClickDispatcher` interface using Android's `GestureDescription` API. This is a textbook **KMP actual implementation** pattern — the interface is already cross-platform, only the dispatch mechanism is platform-specific. iOS would implement `ClickDispatcher` using `UIAccessibility` or coordinate-based tap injection.

**Effort: N/A** — already correctly structured as a platform implementation of a KMP interface.

---

### 7. BootReceiver.kt (60 lines)

**Verdict: MUST STAY ANDROID**

| Aspect | Detail |
|--------|--------|
| Android deps | `BroadcastReceiver`, `Intent.ACTION_BOOT_COMPLETED`, `DataStore`, `startForegroundService()` |
| Purpose | Starts CursorOverlayService on device boot if user preference is set |

**Analysis:** Pure Android lifecycle component. No equivalent concept in iOS (iOS uses `UIApplication` delegate methods). Too small (60 lines) and too platform-specific to warrant abstraction.

**Effort: N/A** — Android-only lifecycle.

---

### 8. RpcServerService.kt (87 lines)

**Verdict: MUST STAY ANDROID**

| Aspect | Detail |
|--------|--------|
| Android deps | `Service`, `Notification`, `NotificationChannel`, `startForeground()` |
| Purpose | Keeps RPC server process alive with foreground notification |
| Logic | Zero business logic — pure Android service boilerplate |

**Analysis:** A thin foreground service wrapper — no extractable logic. The actual RPC server logic is elsewhere.

**Effort: N/A** — Android boilerplate.

---

### 9. VoiceRecognitionService.kt (104 lines)

**Verdict: MUST STAY ANDROID**

| Aspect | Detail |
|--------|--------|
| Android deps | `Service`, `Notification`, `NotificationChannel`, `startForeground()` |
| Purpose | Keeps voice recognition process alive with foreground notification |
| Logic | Minimal — delegates to `VoiceAvanueAccessibilityService` |

**Analysis:** Another thin foreground service wrapper. Actual speech recognition is in VoiceOSCore (KMP). This just maintains the Android foreground service lifecycle.

**Effort: N/A** — Android boilerplate.

---

## Migration Priority Matrix

| Priority | Class | Action | Effort | iOS Value |
|----------|-------|--------|--------|-----------|
| 1 | `ElementExtractor` | Extract `isDynamicContainer()`, `DuplicateInfo`, container type set to KMP | Low | Medium — iOS needs same container classification |
| 2 | `ScreenCacheManager` | Extract hash assembly + normalization to KMP `ScreenHashAssembler` | Medium | High — iOS needs identical screen change detection |
| — | All others | Stay Android | N/A | N/A |

---

## Architecture After Full Migration

```
┌─────────────────────────────────────────────────────┐
│                    KMP commonMain                     │
│                                                       │
│  ElementInfo, HierarchyNode, Bounds, ElementLabels   │
│  OverlayStateManager, OverlayItemGenerator           │
│  OverlayNumberingExecutor, NumbersOverlayMode        │
│  NumberOverlayItem, BadgeTheme, TARGET_APPS          │
│  isDynamicContainer(), DuplicateInfo                  │  ← NEW (from ElementExtractor)
│  ScreenHashAssembler, regex normalizers               │  ← NEW (from ScreenCacheManager)
│  NumbersOverlayExecutor interface                     │
│  ClickDispatcher interface                            │
│                                                       │
├──────────────────────┬────────────────────────────────┤
│   Android (stays)    │        iOS (future)            │
│                      │                                │
│  ElementExtractor    │   IosElementExtractor          │
│    (AccessibilityNodeInfo tree walker)                │
│  ScreenCacheManager  │   IosScreenCacheManager        │
│    (Android adapter → KMP assembler)                  │
│  DynamicCommandGen   │   IosDynamicCommandGen         │
│    (orchestrator)    │     (orchestrator)             │
│  CommandOverlaySvc   │   IosOverlayRenderer           │
│    (WindowManager)   │     (UIWindow+SwiftUI)        │
│  VoiceAvanueA11ySvc  │   IosVoiceOSService           │
│    (AccessibilityService)                             │
│  AccessibilityClick  │   IosClickDispatcher           │
│    Dispatcher        │     (UIAccessibility)         │
│  BootReceiver        │   (AppDelegate)               │
│  RpcServerService    │   (no equivalent)             │
│  VoiceRecognitionSvc │   (no equivalent)             │
└──────────────────────┴────────────────────────────────┘
```

---

## Conclusion

After the overlay migration (commit cfaf968a), **7 of 9 remaining files must stay Android-only** because they are deeply coupled to Android platform APIs (`AccessibilityNodeInfo`, `Service`, `WindowManager`, `GestureDescription`, `BroadcastReceiver`).

**2 files have extractable KMP logic:**
1. **ElementExtractor** — `isDynamicContainer()` and `DuplicateInfo` (low effort)
2. **ScreenCacheManager** — hash assembly algorithm and regex normalizers (medium effort)

These are worth doing because iOS will need:
- The same container classification logic (which containers are scrollable/dynamic)
- The same screen-change detection algorithm (so scroll vs. navigation works identically)

The Android app layer is now primarily a **platform adapter layer**: it walks the Android accessibility tree, collects data into KMP types, and delegates all decisions to KMP logic. This is the correct architecture for KMP — platform code does I/O, shared code does computation.
