# Renderers:Android Refactoring Plan

**Status:** Temporarily Disabled
**Estimated Effort:** 3-4 days
**Priority:** High (needed for AVAMagic 1.0)

## Summary

The Renderers:Android module has architectural mismatches between mapper expectations and component definitions. This document outlines the required refactoring work.

## Issues to Fix

### 1. Package Import Mismatches

Mappers import types from incorrect packages:

```kotlin
// Current (wrong)
import com.augmentalis.avanues.avamagic.ui.core.feedback.AppBar
import com.augmentalis.avanues.avamagic.ui.core.feedback.BottomNav

// Should be
import com.augmentalis.avanues.avamagic.ui.core.navigation.AppBar
import com.augmentalis.avanues.avamagic.ui.core.navigation.BottomNav
```

**Files affected:** All mapper files in `/mappers/` directory

### 2. Component Property Mismatches

Mappers expect different properties than what components define:

#### ColumnComponent / RowComponent
- **Mapper expects:** `arrangement` (Arrangement enum)
- **Component has:** `spacing`, `horizontalAlignment`, `verticalAlignment`

#### ScrollViewComponent
- **Mapper expects:** `orientation`, `child`
- **Component has:** `direction`, `children`

#### TextComponent
- **Mapper expects:** `font`, `TextScope` enum values
- **Component has:** `fontSize`, `fontWeight`, `TextAlign` enum

#### ButtonComponent
- **Mapper expects:** `buttonStyle`, `ButtonScope` enum
- **Component has:** `variant` (ButtonVariant)

#### TextFieldComponent
- **Mapper expects:** `isError`, `errorMessage`
- **Component has:** `errorText`

#### CheckboxComponent / SwitchComponent
- **Mapper expects:** `onCheckedChange`
- **Component has:** No callback (needs adding)

### 3. Missing Component Types

The following types don't exist or don't implement Component:

- **Avatar** - Not defined
- **Badge** - Not defined
- **Chip** - Not defined
- **Dialog** - Not defined
- **ProgressBar** - Not defined

### 4. Missing Enums and Properties

#### AlertComponent needs:
- `variant: AlertVariant`
- `actions: List<AlertAction>`
- `closeable: Boolean`
- `onClose: (() -> Unit)?`

#### AppBarComponent needs:
- `variant: AppBarVariant`
- `scrollBehavior: ScrollBehavior`

#### ToastComponent needs:
- `type: ToastType` (different from severity)
- `position: ToastPosition` (different from Position)
- `onDismiss: (() -> Unit)?`

### 5. OpenGL Renderer Issues

- Missing `glPolygonMode`, `GL_LINE`, `GL_FILL` references
- Non-exhaustive when expression for `Mesh3D`

## Refactoring Strategy

### Phase 1: Update Component Definitions (1 day)
1. Add missing properties to layout components (arrangement)
2. Add missing callbacks to form components (onCheckedChange)
3. Create missing component types (Avatar, Badge, Chip, Dialog, ProgressBar)
4. Add missing enums (AlertVariant, ToastType, etc.)

### Phase 2: Fix Mapper Imports (0.5 day)
1. Update all mapper imports to use correct packages
2. Use sed/find-replace for bulk updates

### Phase 3: Align Mapper Logic (1.5 days)
1. Update each mapper to use correct component properties
2. Fix enum comparisons
3. Add proper @Composable annotations
4. Fix lambda/callback handling

### Phase 4: Testing (1 day)
1. Build and fix remaining issues
2. Create basic unit tests
3. Manual testing of component rendering

## Files to Modify

### Component Files (Phase 1)
- `UI/Core/.../layout/LayoutComponents.kt`
- `UI/Core/.../form/FormComponents.kt`
- `UI/Core/.../display/DisplayComponents.kt`
- `UI/Core/.../feedback/Phase3Types.kt`
- `UI/Core/.../navigation/NavigationTypes.kt`

### Mapper Files (Phase 2 & 3)
- All 38+ files in `/mappers/` directory
- All 12 files in `/mappers/input/` directory

## Progress Tracking

### Phase 1: Component Definitions ✅ COMPLETED
- [x] Added `arrangement: Arrangement` to ColumnComponent and RowComponent
- [x] Added `orientation: Orientation` and `child` to ScrollViewComponent
- [x] Added `onCheckedChange` to CheckboxComponent and SwitchComponent
- [x] Added `buttonStyle: ButtonScope` to ButtonComponent
- [x] Added `isError`, `errorMessage` to TextFieldComponent
- [x] Added `font: TextFont?` and `TextScope` to TextComponent
- [x] Updated AlertComponent with variant, actions, closeable, onClose
- [x] Avatar, Badge, Chip already exist and implement Component
- [x] ProgressBar, Dialog exist in feedback package
- [x] Added typealiases (Slider, DatePicker, TimePicker, RadioButton, Dropdown)

### Phase 2: Mapper Imports ✅ COMPLETED
- [x] Fixed feedback → navigation (AppBar, BottomNav, Breadcrumb, Pagination)
- [x] Fixed feedback → display (Avatar, Badge, Chip, ProgressBar, Dialog)
- [x] Fixed feedback → form (Slider, DatePicker, TimePicker, RadioButton, Dropdown)

### Phase 3: Mapper Logic Alignment ❌ PENDING (Major Work)
The mappers have fundamental mismatches with component properties:

#### Critical Issues (90+ compilation errors):
1. **ComposeRenderer.kt** - @Composable annotation missing on function
2. **MagicElementsCompose.kt** - Unresolved reference: AvaUI
3. **OpenGLRenderer.kt** - Missing OpenGL ES functions (glPolygonMode, GL_LINE, GL_FILL)
4. **ColumnMapper.kt** - Enum type mismatches (Arrangement, HorizontalAlignment)
5. **ButtonMapper.kt** - References ButtonStyle instead of ButtonScope
6. **AppBarMapper.kt** - Missing variant, scrollBehavior properties
7. **BottomNavMapper.kt** - Missing enabled, selectedIcon properties
8. **BreadcrumbMapper.kt** - Missing maxItems, onItemClick, id properties
9. **ConfirmMapper.kt** - Missing open, confirmText, cancelText properties
10. **Many more mappers** - Property name mismatches

#### Root Causes:
1. **Mapper written for different API** - Mappers expect a different component API than what exists
2. **Enum value mismatches** - Mappers compare against enum values that don't exist
3. **Two incompatible Arrangement types** - `layout.Arrangement` vs `components.core.Arrangement`
4. **Composable context issues** - Functions need @Composable annotation

### Recommended Next Steps

#### Option A: Full Mapper Rewrite (5+ days)
Rewrite all 50+ mappers to use the actual component API.

#### Option B: Component API Redesign (3+ days)
Modify component definitions to match what mappers expect.

#### Option C: Parallel Implementation (2+ days)
Create adapter layer between components and mappers.

**Estimated remaining effort:** 3-5 days depending on approach

## Re-enabling

After refactoring is complete:

1. Uncomment in `settings.gradle.kts`:
   ```kotlin
   include(":modules:AVAMagic:Components:Renderers:Android")
   ```

2. Run build:
   ```bash
   ./gradlew :modules:AVAMagic:Components:Renderers:Android:build
   ```

3. Run tests to verify functionality

---
**Created:** 2025-11-18
**Author:** Claude (IDEACODE v8)
