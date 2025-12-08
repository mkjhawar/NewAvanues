# Implementation Plan: Component Parity (iOS + Web Mappers)

**Created:** 2025-12-01
**Updated:** 2025-12-02
**Status:** PHASE 1-3 iOS COMPLETE
**Platforms:** iOS, Web

---

## Overview

| Metric | Value |
|--------|-------|
| Platforms | iOS, Web |
| iOS Missing | 142 components (25% → 100%) |
| Web Missing | 118 components (38% → 100%) |
| Swarm Recommended | YES (2 platforms, 260+ tasks) |
| Estimated Tasks | 142 (iOS) + 118 (Web) = 260 mapper tasks |

---

## Current State (Updated 2025-12-02)

| Platform | Current | Target | Gap | Progress |
|----------|---------|--------|-----|----------|
| Android | 190/190 (100%) | 190 | 0 | ✅ |
| iOS | 120/190 (63%) | 190 | 70 | +72 mappers |
| Web | 72/190 (38%) | 190 | 118 | - |

---

## Phase Ordering

| Phase | Platform | Components | Rationale | Status |
|-------|----------|------------|-----------|--------|
| 1 | iOS Priority 1 | 29 | Layout + Chips + Buttons (high impact) | ✅ COMPLETE |
| 2 | Web Priority 1 | 14 | Lists + Cards (fills gaps) | ✅ COMPLETE |
| 3 | iOS Priority 2 | 43 | Lists + Cards + Display + Feedback + Nav | ✅ COMPLETE |
| 4 | Web Priority 2 | 43 | Display + Feedback + Nav | PENDING |
| 5 | iOS Priority 3 | 70 | Data + Input + Calendar + Animation | PENDING |
| 6 | Web Priority 3 | 61 | Data + Input + Calendar + Animation + Charts | PENDING |

---

## Phase 1: iOS High-Impact (29 components)

### Flutter Layout (10 mappers)

| # | Component | SwiftUI Mapping | Complexity |
|---|-----------|-----------------|------------|
| 1 | AlignComponent | Alignment + frame | Low |
| 2 | CenterComponent | frame(maxWidth/Height: .infinity) | Low |
| 3 | ConstrainedBoxComponent | frame(minWidth/maxWidth) | Low |
| 4 | ExpandedComponent | Spacer + layoutPriority | Low |
| 5 | FittedBoxComponent | scaledToFit() | Medium |
| 6 | FlexComponent | HStack/VStack with spacing | Low |
| 7 | FlexibleComponent | Spacer + frame | Low |
| 8 | PaddingComponent | .padding() modifier | Low |
| 9 | SizedBoxComponent | frame(width/height) | Low |
| 10 | WrapComponent | LazyVGrid with flexible columns | Medium |

**File:** `FlutterLayoutMappers.kt`

### Flutter Chips (5 mappers)

| # | Component | SwiftUI Mapping | Complexity |
|---|-----------|-----------------|------------|
| 1 | MagicFilter | Capsule + Toggle | Medium |
| 2 | MagicAction | Capsule + Button | Low |
| 3 | MagicChoice | Capsule + Picker | Medium |
| 4 | MagicInput | Capsule + TextField | Medium |
| 5 | MagicTag | Capsule + Text | Low |

**File:** `FlutterChipMappers.kt`

### Flutter Buttons (14 mappers)

| # | Component | SwiftUI Mapping | Complexity |
|---|-----------|-----------------|------------|
| 1 | FilledButton | Button + .buttonStyle(.borderedProminent) | Low |
| 2 | CloseButtonComponent | Button + xmark.circle | Low |
| 3 | ElevatedButton | Button + shadow | Low |
| 4 | FloatingActionButton | Button + circle + shadow | Medium |
| 5 | IconButton | Button + Image | Low |
| 6 | LoadingButton | Button + ProgressView | Medium |
| 7 | OutlinedButton | Button + .buttonStyle(.bordered) | Low |
| 8 | PopupMenuButton | Menu + Button | Medium |
| 9 | RefreshIndicator | .refreshable modifier | Medium |
| 10 | SegmentedButton | Picker + .pickerStyle(.segmented) | Low |
| 11 | SplitButton | HStack + Button + Menu | Medium |
| 12 | TextButton | Button + .buttonStyle(.plain) | Low |
| 13 | ButtonBar | HStack with buttons | Low |
| 14 | FilledTonalButton | Button + tinted background | Low |

**File:** `FlutterButtonMappers.kt`

---

## Phase 2: Web Gap Fillers (14 components)

### Flutter Lists (4 mappers)

| # | Component | React Mapping | Complexity |
|---|-----------|---------------|------------|
| 1 | ExpansionTile | Accordion/Collapsible | Medium |
| 2 | CheckboxListTile | List item + Checkbox | Low |
| 3 | SwitchListTile | List item + Switch | Low |
| 4 | RadioListTile | List item + Radio | Low |

**File:** `flutter-list-mappers.tsx`

### Flutter Cards (8 mappers)

| # | Component | React Mapping | Complexity |
|---|-----------|---------------|------------|
| 1 | PricingCard | Card + price layout | Medium |
| 2 | FeatureCard | Card + icon + features | Medium |
| 3 | TestimonialCard | Card + avatar + quote | Low |
| 4 | ProductCard | Card + image + price | Medium |
| 5 | ArticleCard | Card + image + title | Low |
| 6 | ImageCard | Card + full-bleed image | Low |
| 7 | HoverCard | Card + hover state | Medium |
| 8 | ExpandableCard | Card + collapse | Medium |

**File:** `flutter-card-mappers.tsx`

### Web Missing Layout (2 mappers)

| # | Component | React Mapping | Complexity |
|---|-----------|---------------|------------|
| 1 | CenterComponent | flex + justify-center | Low |
| 2 | ConstrainedBoxComponent | max-w, min-w | Low |

---

## Phase 3: iOS Display + Feedback (43 components)

### Flutter Lists iOS (4)
ExpansionTile, CheckboxListTile, SwitchListTile, RadioListTile

### Flutter Cards iOS (8)
PricingCard, FeatureCard, TestimonialCard, ProductCard, ArticleCard, ImageCard, HoverCard, ExpandableCard

### Flutter Display iOS (12)
AvatarGroup, SkeletonText, SkeletonCircle, ProgressCircle, LoadingOverlay, Popover, ErrorState, NoData, ImageCarousel, LazyImage, ImageGallery, Lightbox

### Flutter Feedback iOS (10)
Popup, Callout, Disclosure, InfoPanel, ErrorPanel, WarningPanel, SuccessPanel, FullPageLoading, AnimatedCheck, AnimatedError

### Flutter Nav iOS (9)
Menu, Sidebar, NavLink, ProgressStepper, MenuBar, SubMenu, VerticalTabs, MasonryGrid, AspectRatio

---

## Phase 4: Web Display + Feedback (43 components)

Same categories as Phase 3 but for Web/React

---

## Phase 5: iOS Advanced (70 components)

| Category | Count |
|----------|-------|
| Data | 13 |
| Input Advanced | 11 |
| Calendar | 5 |
| Scrolling | 7 |
| Animation | 8 |
| Transitions | 11 |
| Slivers | 4 |
| Other | 9 |
| Web-only Buttons | 2 |

---

## Phase 6: Web Advanced (61 components)

| Category | Count |
|----------|-------|
| Data | 12 |
| Input Advanced | 11 |
| Calendar | 5 |
| Scrolling | 7 |
| Animation | 8 |
| Transitions | 11 |
| Slivers | 4 |
| Other | 9 |
| Charts | 11 |

---

## File Organization

### iOS (Kotlin)

```
Renderers/iOS/src/iosMain/kotlin/com/augmentalis/magicelements/renderer/ios/mappers/
├── FlutterLayoutMappers.kt      # Phase 1: 10 components
├── FlutterChipMappers.kt        # Phase 1: 5 components
├── FlutterButtonMappers.kt      # Phase 1: 14 components
├── FlutterListMappers.kt        # Phase 3: 4 components
├── FlutterCardMappers.kt        # Phase 3: 8 components
├── FlutterDisplayMappers.kt     # Phase 3: 12 components
├── FlutterFeedbackMappers.kt    # Phase 3: 10 components
├── FlutterNavMappers.kt         # Phase 3: 9 components
├── FlutterDataMappers.kt        # Phase 5: 13 components
├── FlutterInputMappers.kt       # Phase 5: 11 components
├── FlutterCalendarMappers.kt    # Phase 5: 5 components
├── FlutterScrollMappers.kt      # Phase 5: 7 components
├── FlutterAnimationMappers.kt   # Phase 5: 8 components
├── FlutterTransitionMappers.kt  # Phase 5: 11 components
├── FlutterSliverMappers.kt      # Phase 5: 4 components
└── FlutterOtherMappers.kt       # Phase 5: 9 components
```

### Web (TypeScript/React)

```
Renderers/Web/src/mappers/flutter/
├── flutter-layout-mappers.tsx   # 3 remaining
├── flutter-list-mappers.tsx     # 4 components
├── flutter-card-mappers.tsx     # 8 components
├── flutter-display-mappers.tsx  # 12 components
├── flutter-feedback-mappers.tsx # 10 components
├── flutter-nav-mappers.tsx      # 9 components
├── flutter-data-mappers.tsx     # 12 components
├── flutter-input-mappers.tsx    # 11 components
├── flutter-calendar-mappers.tsx # 5 components
├── flutter-scroll-mappers.tsx   # 7 components
├── flutter-anim-mappers.tsx     # 8 components
├── flutter-trans-mappers.tsx    # 11 components
├── flutter-sliver-mappers.tsx   # 4 components
├── flutter-other-mappers.tsx    # 9 components
└── chart-mappers.tsx            # 11 components
```

---

## Time Estimates

| Scenario | iOS | Web | Total |
|----------|-----|-----|-------|
| Sequential | 142h | 118h | 260h |
| Swarm (parallel) | 48h | 40h | 48h |
| Savings | - | - | 212h (82%) |

**Swarm Configuration:**
- 3 iOS agents (Phase 1, 3, 5)
- 3 Web agents (Phase 2, 4, 6)
- 1 Integration agent (SwiftUIRenderer + WebRenderer updates)

---

## Dependencies

| Dependency | Status |
|------------|--------|
| iOS Renderer builds | ✅ COMPLETE |
| Component definitions exist | ✅ Android has all 190 |
| SwiftUI Bridge types | ✅ WORKING |
| Web Renderer exists | ✅ WORKING |

---

## Quality Gates

| Gate | Requirement |
|------|-------------|
| Build | Zero errors |
| Type Safety | Full Kotlin/TypeScript typing |
| Consistency | Match Android mapper patterns |
| Documentation | KDoc/JSDoc for each mapper |

---

## Recommended Execution

```
/implement .yolo .swarm specs/plan-component-parity-ios-web.md
```

Or phase-by-phase:

```
# Phase 1: iOS High-Impact
/implement .yolo "FlutterLayoutMappers.kt, FlutterChipMappers.kt, FlutterButtonMappers.kt"

# Phase 2: Web Gap Fillers
/implement .yolo "flutter-list-mappers.tsx, flutter-card-mappers.tsx"
```

---

## Completion Log

### Phase 1: iOS Layout+Chips+Buttons (2025-12-02) ✅

| Mapper File | Components | Status |
|-------------|------------|--------|
| FlutterLayoutMappers.kt | 10 | ✅ |
| FlutterChipMappers.kt | 5 | ✅ |
| FlutterButtonMappers.kt | 14 | ✅ |

### Phase 2: Web Gap Fillers (2025-12-02) ✅

| Mapper File | Components | Status |
|-------------|------------|--------|
| flutter-list-mappers.tsx | 4 | ✅ |
| flutter-card-mappers.tsx | 8 | ✅ |
| Web layout fixes | 2 | ✅ |

### Phase 3: iOS Display+Feedback+Nav (2025-12-02) ✅

| Mapper File | Components | Status |
|-------------|------------|--------|
| FlutterListMappers.kt | 4 | ✅ |
| FlutterCardMappers.kt | 8 | ✅ |
| FlutterDisplayMappers.kt | 12 | ✅ |
| FlutterFeedbackMappers.kt | 10 | ✅ |
| FlutterNavMappers.kt | 9 | ✅ |

**Build Verification:** `./gradlew :Universal:Libraries:AvaElements:Renderers:iOS:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL

---

## Success Criteria

| Metric | Target | Current |
|--------|--------|---------|
| iOS Parity | 190/190 (100%) | 120/190 (63%) |
| Web Parity | 190/190 (100%) | 72/190 (38%) |
| Build Status | All platforms compile | ✅ iOS PASS |
| Documentation | Living doc updated | ✅ v1.3.0 |

---

**Author:** Engineering Team
**Version:** 1.1
**Updated:** 2025-12-02
