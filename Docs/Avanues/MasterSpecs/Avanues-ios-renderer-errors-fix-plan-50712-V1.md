# Implementation Plan: iOS Renderer Error Fixes

## Overview
- **Platforms:** iOS (KMP)
- **Current Errors:** 1101
- **Swarm Recommended:** Yes (high task count)
- **Estimated Tasks:** 8 phases, ~25 tasks

## Chain of Thought Analysis

### Error Categories (by frequency)
| Category | Count | Root Cause |
|----------|-------|------------|
| Lambda type inference (`it`) | 94 | Missing explicit parameter types |
| Missing Flutter components | ~200 | `flutter.*` imports for unimplemented components |
| Missing color constants | 70+ | Secondary, Primary, Red, Green, etc. |
| Missing bridge types | 60+ | SwiftUIAction, SwiftUIStyle, Padding, Frame |
| Missing component refs | 80+ | ProgressStepper, Popup, Ranking, etc. |
| Type mismatches | 20 | SizeValue vs Int, wrong alignment types |

### Strategic Approach
1. **Add missing bridge types** - Quick wins, unblocks many errors
2. **Add color constants** - SwiftUIColor extensions for semantic colors
3. **Fix lambda type inference** - Add explicit types to lambda parameters
4. **Stub missing components** - Create minimal stubs for missing Flutter parity components
5. **Comment out broken mappers** - Disable mappers for non-existent components

---

## Phases

### Phase 1: Bridge Type Extensions (Priority: HIGH)
Add missing types to SwiftUIModels.kt

**Tasks:**
- [ ] Add SwiftUIAction type for button/interaction callbacks
- [ ] Add SwiftUIStyle type for styling presets
- [ ] Add SwiftUIPadding helper with Edge presets
- [ ] Add SwiftUIFrame helper with size constants
- [ ] Add BoxFit enum for image scaling modes
- [ ] Add missing color constants (Primary, Secondary, Red, Green, etc.)

**Files:** `magicelements/renderer/ios/bridge/SwiftUIModels.kt`

### Phase 2: Fix Lambda Type Inference (Priority: HIGH)
Add explicit type annotations to lambda parameters

**Tasks:**
- [ ] Fix DataMappers.kt lambda types (21 errors)
- [ ] Fix NavigationMappers.kt lambda types (14 errors)
- [ ] Fix CalendarMappers.kt lambda types (12 errors)
- [ ] Fix SecureInputMappers.kt lambda types (7 errors)
- [ ] Fix FeedbackMappers.kt lambda types (7 errors)
- [ ] Fix Phase3FormMappers.kt lambda types (6 errors)
- [ ] Fix Phase3DataMappers.kt lambda types (6 errors)

**Pattern:** `{ it.property }` → `{ item: Type -> item.property }`

### Phase 3: Fix Type Mismatches (Priority: HIGH)
Correct type usage in mappers

**Tasks:**
- [ ] Fix Phase2FeedbackMappers.kt type mismatches (15 errors)
- [ ] Fix LayoutMappers.kt type mismatches (5 errors)
- [ ] Fix SizeValue vs Int/Float conversions
- [ ] Fix alignment type imports

### Phase 4: Stub Missing Components (Priority: MEDIUM)
Create minimal stubs for Flutter parity components

**Tasks:**
- [ ] Create stub file for missing Flutter parity components
- [ ] Stub: ProgressStepper, Popup, Ranking, StatGroup, CloseButton
- [ ] Stub: FeatureCard, ImageCard, HoverCard, Callout
- [ ] Stub: VerticalTabs, NavLink, IndexedStack, InfiniteScroll
- [ ] Stub: DataList, KPI, Stat, Timeline types

**File:** `components/flutter-stubs/FlutterParityStubs.kt`

### Phase 5: Fix Flutter Import Issues (Priority: MEDIUM)
Remove or fix invalid Flutter imports

**Tasks:**
- [ ] Remove `flutter.*` imports from mappers that don't need them
- [ ] Update imports to use stub components
- [ ] Fix `flutterparity` package references

### Phase 6: Fix Component Property References (Priority: MEDIUM)
Fix references to non-existent component properties

**Tasks:**
- [ ] Fix `secondaryText` references in CardMappers
- [ ] Fix `text`, `title` property references
- [ ] Fix `children` property references
- [ ] Fix `variant`, `selected` property references

### Phase 7: Fix Remaining Bridge Issues (Priority: LOW)
Clean up remaining bridge-related errors

**Tasks:**
- [ ] Fix CornerRadius references
- [ ] Fix Size/Font references in ModifierConverter
- [ ] Fix Shadow reference in ThemeConverter
- [ ] Fix AccessibilityLabel/Hint usage

### Phase 8: Disable Broken Mappers (Priority: LOW)
Comment out mappers that can't be fixed without full component implementation

**Tasks:**
- [ ] Identify mappers with >5 unresolvable errors
- [ ] Add TODO comments explaining what's needed
- [ ] Wrap in `// TODO: Enable when Flutter parity components are implemented`

---

## Time Estimates

| Execution Mode | Time | Notes |
|----------------|------|-------|
| Sequential | 4-6 hours | Single developer |
| Parallel (Swarm) | 1-2 hours | 4 agents on phases 1-4 |
| Savings | ~4 hours | 66% reduction |

## Execution Order

```
Phase 1 (Bridge) ──┐
                   ├──> Phase 5 (Imports) ──┐
Phase 2 (Lambdas) ─┤                        ├──> Phase 7 (Cleanup) ──> Phase 8 (Disable)
                   ├──> Phase 6 (Props)  ───┘
Phase 3 (Types) ───┤
                   │
Phase 4 (Stubs) ───┘
```

## Success Criteria
- [ ] 0 build errors in iOS renderer
- [ ] All working mappers preserved
- [ ] Clear TODOs for future Flutter parity work
- [ ] No functionality regression

---

## Next Steps
With `.yolo` flag: Auto-execute phases 1-8
