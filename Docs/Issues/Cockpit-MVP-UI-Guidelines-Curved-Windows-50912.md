# Issue: Cockpit MVP - UI Guidelines Compliance & Curved Window Rendering

## Status
| Field | Value |
|-------|-------|
| Module | Cockpit MVP (android/apps/cockpit-mvp) |
| Severity | High |
| Status | Investigating |
| Created | 2025-12-09 |
| Affects | WindowCard.kt, WorkspaceView.kt, Spatial Rendering |

---

## Symptoms

### Issue 1: UI Guidelines Violations
**Observed Behavior:**
- Windows (WindowCard.kt) use solid colors instead of glassmorphic surfaces
- No wrapper component pattern for MagicUI migration
- Inconsistent theme token usage (1/6 tokens vs 3/6 in other components)
- Missing hover/pressed/focus state variations
- Hard-coded design values instead of centralized theme tokens
- Cannot switch themes at runtime (Ocean → AvaMagic → MagicCode)

**Expected Behavior (Per UI Guidelines):**
- Windows should use `GlassmorphicSurface` wrapper component
- Glassmorphic appearance: `Color.White.copy(alpha = 0.08f)` with blur and borders
- All styling from `OceanTheme.*` tokens for consistency
- Interactive states: hover (0.12f), pressed (0.16f), focus (border 0.30f)
- Theme switching capability for MagicUI migration readiness

### Issue 2: Flat 2D Windows (Missing Curved/Spatial Rendering)
**Observed Behavior:**
- Windows rendered as flat 2D Compose components
- No spatial depth or curved projection
- Limited screen real estate utilization
- Infrastructure exists (SpatialRenderer, Vector3D, Quaternion) but NOT connected

**Expected Behavior (Per Cockpit Specs):**
- Curved/cylindrical window projection for more screen real estate
- 3D spatial positioning with depth
- Curvature radius: 3.0 meters (from PerspectiveStyle.curvatureRadius)
- Arc layout presets (ARC_3_FRONT, THEATER mode)
- Integration with VoiceOS SpatialRenderer + HUDRenderer

---

## Root Cause (ToT Analysis)

### Issue 1: UI Guidelines Violations

#### Hypothesis 1: Architecture - Missing Wrapper Pattern ⭐⭐⭐ (HIGH)
**Evidence:**
- `WindowCard.kt:27` uses `Card()` directly instead of `GlassmorphicSurface()`
- No semantic component layer between business logic and UI framework
- Tight coupling to Material3 primitives
- **Impact:** Prevents easy MagicUI migration, theme switching limited

#### Hypothesis 2: Design System - Missing Theme Abstraction ⭐⭐⭐ (HIGH)
**Evidence:**
- `WindowCard.kt:34` uses `Color(android.graphics.Color.parseColor(color))` - external color parsing
- OceanTheme.kt exists but only partially used (glassBorder on line 36)
- No centralized ThemeProvider/ThemeState for runtime switching
- **Impact:** Cannot switch themes at runtime, inconsistent styling

#### Hypothesis 3: Implementation - Hard-Coded Design Tokens ⭐⭐ (MEDIUM)
**Evidence:**
- `WindowCard.kt:29-30`: `width(240.dp).height(180.dp)` - magic numbers
- `WindowCard.kt:32`: `RoundedCornerShape(12.dp)` - should be 16.dp per guidelines
- `WindowCard.kt:31`: `.shadow(8.dp)` - not using `OceanTheme.glassShadow`
- Mixed opacity values: 0.8f, 0.7f, 0.6f - not from theme tokens
- **Impact:** Maintenance burden, inconsistent with other components

#### Hypothesis 4: Future-Proofing - No MagicUI Migration Path ⭐⭐⭐ (HIGH)
**Evidence:**
- No component naming convention following `GlassmorphicSurface` → `MagicSurface` pattern
- Direct Material3 imports, no abstraction layer
- No theme switching mechanism in place
- **Impact:** Major refactor required when MagicUI ships, technical debt

---

### Issue 2: Missing Curved/Spatial Rendering

#### Hypothesis 1: Rendering Pipeline - 2D Only (No 3D Integration) ⭐⭐⭐ (HIGH)
**Evidence:**
- Current: Jetpack Compose 2D Canvas rendering
- Available: SpatialRenderer.kt (600 lines) with 3D transformation matrices, NOT USED
- Available: HUDRenderer.kt (600 lines) with SPATIAL_AR mode, NOT CONNECTED
- **Impact:** Cannot render curved/cylindrical windows, limited screen real estate

#### Hypothesis 2: Missing OpenGL/Vulkan Integration ⭐⭐⭐ (HIGH)
**Evidence:**
- No GLSurfaceView rendering in Cockpit
- No OpenGL shaders or vertex buffers
- Canvas-based only (CPU rendering, not GPU 3D)
- XRManager detects ARCore/Vulkan capabilities but not used
- **Impact:** No hardware-accelerated 3D rendering, no curved projection

#### Hypothesis 3: Unimplemented Layout Presets ⭐⭐ (MEDIUM)
**Evidence:**
- `LinearHorizontalLayout` - ✅ Implemented (flat horizontal)
- `ARC_3_FRONT` - ❌ Planned (curved arc)
- `THEATER` - ❌ Planned (cinema-style)
- `GRID_2x2` - ❌ Planned (grid layout)
- PerspectiveStyle.curvatureRadius = 3f defined but not rendered
- **Impact:** Limited layout options, no curved arrangements

#### Hypothesis 4: Infrastructure Exists but Disconnected ⭐⭐⭐ (HIGH)
**Evidence:**
- Vector3D.kt + Quaternion.kt - 3D math primitives ✅
- SpatialRenderer.kt - 3D transformation matrices ✅
- PerspectiveStyle.kt - 3D parameters (tilt, curve, distance) ✅
- HUDRenderer.kt - SPATIAL_AR render mode ✅
- **None connected to Cockpit WindowCard rendering**
- **Impact:** All infrastructure ready but isolated, requires integration work

---

## Selected Root Causes (CoT Verification)

### Issue 1: UI Guidelines Violations
**Primary Cause:** Missing Wrapper Component Architecture + Incomplete Theme Abstraction

**CoT Verification Steps:**

**Step 1:** Check if wrapper pattern exists elsewhere
- TopNavigationBar.kt:49 - Uses `Surface()` directly, applies OceanTheme tokens manually ✅
- ControlPanel.kt:42 - Uses `Surface()` directly, applies OceanTheme tokens manually ✅
- **Finding:** Wrapper pattern **NOT implemented** anywhere in cockpit-mvp

**Step 2:** Verify theme token usage consistency
- OceanTheme.kt defines comprehensive tokens ✅
- TopNavigationBar.kt uses: `glassSurface`, `glassBorder`, `success` ✅
- ControlPanel.kt uses: `glassSurface`, `glassBorder` ✅
- WindowCard.kt uses: ONLY `glassBorder` ❌ (1/6 tokens vs 3/6 in other components)
- **Finding:** WindowCard has **inconsistent token usage**

**Step 3:** Assess MagicUI readiness
- Per guidelines: "When MagicUI ready: swap imports" requires semantic wrappers
- Current: Direct `Card()` → Cannot swap with `MagicSurface()` without refactor
- **Finding:** **Zero migration readiness** - will require full component rewrite

**Step 4:** Evaluate theme switching capability
- No `ThemeProvider` or `ThemeState` in codebase
- OceanTheme is object (singleton), not composable provider
- **Finding:** **No runtime theme switching** support

**✅ Confirmed Root Cause:**
Combined failure of:
1. No wrapper component architecture (Hypothesis 1 ✓)
2. Partial theme token usage - missing centralized provider (Hypothesis 2 ✓)
3. Hard-coded values mixed with tokens (Hypothesis 3 ✓)
4. Zero MagicUI migration readiness (Hypothesis 4 ✓)

---

### Issue 2: Missing Curved/Spatial Rendering
**Primary Cause:** 2D Compose Rendering Pipeline (No 3D/OpenGL Integration)

**CoT Verification Steps:**

**Step 1:** Verify current rendering architecture
- Cockpit MVP uses: Jetpack Compose + Material 3 (2D Canvas)
- No GLSurfaceView, no OpenGL shaders, no 3D rendering
- **Finding:** Purely 2D rendering pipeline

**Step 2:** Check available 3D infrastructure
- SpatialRenderer.kt exists in `/Modules/VoiceOS/managers/HUDManager/` ✅
  - 3D transformation matrices (view, projection, model)
  - Perspective projection with FOV
  - Depth sorting and atmospheric perspective
- HUDRenderer.kt exists with SPATIAL_AR mode ✅
  - 90-120 FPS render loop
  - Canvas-based (not OpenGL, but supports 3D transforms)
- **Finding:** Infrastructure ready but in different module, not connected

**Step 3:** Assess curved window specifications
- PerspectiveStyle.curvatureRadius = 3f (3 meters) defined ✅
- PerspectiveStyle.curvatureEnabled = true (flag set) ✅
- Arc layout presets (ARC_3_FRONT, THEATER) interfaces defined ✅
- **Finding:** Specifications exist, implementations MISSING

**Step 4:** Check OpenGL/ARCore capabilities
- XRManager.kt detects ARCore, VR tracking, Vulkan support ✅
- VideoManager.kt provides camera access ✅
- No actual OpenGL rendering implementation ❌
- **Finding:** Device capabilities detected but not utilized

**✅ Confirmed Root Cause:**
Infrastructure exists but disconnected:
1. 2D-only Compose rendering in Cockpit (Hypothesis 1 ✓)
2. No OpenGL/Vulkan integration (Hypothesis 2 ✓)
3. Arc layout presets unimplemented (Hypothesis 3 ✓)
4. SpatialRenderer + HUDRenderer exist but not connected to Cockpit (Hypothesis 4 ✓)

---

## Impact Analysis

### Immediate Impact
| Issue | Impact | Severity |
|-------|--------|----------|
| Solid colored windows | Violates UI guidelines, inconsistent branding | Medium |
| No glassmorphic styling | Poor visual quality, not production-ready | High |
| Flat 2D windows | Limited screen real estate, no spatial depth | High |
| No curved projection | Missing key Cockpit feature (per specs) | High |

### Short-Term Impact
| Issue | Impact | Severity |
|-------|--------|----------|
| Cannot switch themes | Stuck with hard-coded Ocean theme | High |
| No MagicUI readiness | Will require full refactor later | High |
| No Arc layout presets | Limited window arrangements | Medium |
| Infrastructure unused | Wasted development effort (SpatialRenderer exists) | Medium |

### Long-Term Impact
| Issue | Impact | Severity |
|-------|--------|----------|
| Technical debt | Major refactor needed for MagicUI migration | Critical |
| Maintenance burden | Hard-coded values scattered across components | High |
| Missing XR features | Cannot compete with spatial computing platforms | Critical |
| No hardware acceleration | Performance issues with many windows | High |

---

## Fix Plan

### Phase 1: UI Guidelines Compliance (1-2 days)

#### 1.1 Create Wrapper Components
**Files to create:**
- `src/main/java/com/augmentalis/cockpit/mvp/components/GlassmorphicSurface.kt`
- `src/main/java/com/augmentalis/cockpit/mvp/components/OceanButton.kt`
- `src/main/java/com/augmentalis/cockpit/mvp/components/OceanCard.kt`

**Pattern:**
```kotlin
@Composable
fun GlassmorphicSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    border: BorderStroke? = BorderStroke(1.dp, OceanTheme.glassBorder),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = OceanTheme.glassSurface,
        shape = shape,
        border = border,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) { content() }
}
```

**Migration Note:** When MagicUI ships, replace with:
```kotlin
// import com.avanues.magicui.MagicSurface
// Single line swap - no business logic changes
```

---

#### 1.2 Enhance OceanTheme.kt
**Add missing tokens:**
```kotlin
object OceanTheme {
    // Shapes
    val glassShape = RoundedCornerShape(16.dp)
    val glassShapeSmall = RoundedCornerShape(12.dp)
    val glassShapeLarge = RoundedCornerShape(20.dp)

    // Elevations
    val glassElevationDefault = 4.dp
    val glassElevationHigh = 6.dp
    val glassShadowElevation = 8.dp

    // Sizes
    val windowWidthDefault = 240.dp
    val windowHeightDefault = 180.dp
    val buttonSizeDefault = 40.dp
    val iconSizeDefault = 24.dp

    // Interactive states (already defined, document usage)
    // glassSurface = 0.08f (default)
    // glassSurfaceHover = 0.12f (on hover)
    // glassSurfacePressed = 0.16f (on press)
    // glassBorderFocus = 0.30f (on focus)
}
```

---

#### 1.3 Refactor WindowCard.kt
**Changes:**
```kotlin
@Composable
fun WindowCard(
    window: AppWindow,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isHovered: Boolean = false,
    isFocused: Boolean = false
) {
    val surfaceColor = when {
        isHovered -> OceanTheme.glassSurfaceHover
        else -> OceanTheme.glassSurface
    }

    val borderColor = if (isFocused) {
        OceanTheme.glassBorderFocus
    } else {
        OceanTheme.glassBorder
    }

    GlassmorphicSurface(
        modifier = modifier
            .width(OceanTheme.windowWidthDefault)
            .height(OceanTheme.windowHeightDefault),
        shape = OceanTheme.glassShape,
        border = BorderStroke(2.dp, borderColor),
        surfaceColor = surfaceColor  // Pass state-aware color
    ) {
        // Existing content...
    }
}
```

**Remove:**
- Line 34: `Color(android.graphics.Color.parseColor(color))` - solid colors
- Lines 29-30: Hard-coded `240.dp`, `180.dp`
- Line 32: Hard-coded `RoundedCornerShape(12.dp)`

---

#### 1.4 Create ThemeProvider (Future MagicUI Readiness)
**File:** `src/main/java/com/augmentalis/cockpit/mvp/theme/ThemeProvider.kt`

```kotlin
enum class AppTheme {
    OCEAN,      // Default glassmorphic
    AVAMAGIC,   // AVA branding
    MAGICCODE,  // Developer UI
    NATIVE      // Platform default
}

@Composable
fun CockpitThemeProvider(
    theme: AppTheme = AppTheme.OCEAN,
    content: @Composable () -> Unit
) {
    val colors = when (theme) {
        AppTheme.OCEAN -> OceanTheme
        AppTheme.AVAMAGIC -> AvaMagicTheme // Future
        AppTheme.MAGICCODE -> MagicCodeTheme // Future
        AppTheme.NATIVE -> MaterialTheme.colorScheme // Platform
    }

    CompositionLocalProvider(
        LocalTheme provides colors
    ) {
        content()
    }
}

val LocalTheme = compositionLocalOf<Any> { OceanTheme }
```

---

### Phase 2: Curved Window Rendering (3-5 days)

#### 2.1 Integrate SpatialRenderer into Cockpit
**Steps:**
1. Add HUDManager dependency to cockpit-mvp
   ```kotlin
   // build.gradle.kts
   implementation(project(":Modules:VoiceOS:managers:HUDManager"))
   ```

2. Create SpatialWindowRenderer.kt
   - Extend SpatialRenderer from HUDManager
   - Implement curved window projection
   - Use cylindrical projection math

3. Connect to WorkspaceView
   - Replace flat Compose layout with spatial rendering
   - Use AndroidView wrapper for custom rendering

---

#### 2.2 Implement Curved Projection Math
**File:** `src/main/java/com/augmentalis/cockpit/mvp/rendering/CurvedProjection.kt`

```kotlin
class CurvedProjection(
    val curvatureRadius: Float = 3.0f,  // meters
    val viewDistance: Float = 2.0f       // meters
) {
    /**
     * Convert 3D world position to curved screen coordinates
     * Uses cylindrical projection for horizontal curvature
     */
    fun projectToCurvedScreen(position: Vector3D): Pair<Float, Float> {
        // Cylindrical projection formula
        val theta = atan2(position.x, position.z + viewDistance)
        val curvedX = curvatureRadius * theta
        val curvedY = position.y

        // Apply perspective for depth scaling
        val scale = viewDistance / (viewDistance + position.z)
        return Pair(curvedX * scale, curvedY * scale)
    }
}
```

---

#### 2.3 Implement Arc Layout Presets
**File:** `Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/layout/presets/ArcFrontLayout.kt`

```kotlin
object ArcFrontLayout : LayoutPreset {
    override fun calculatePositions(
        windows: List<AppWindow>,
        centerPoint: Vector3D
    ): List<WindowPosition> {
        val radius = 3.0f  // Arc radius (meters)
        val arcAngle = 120f  // Total arc span (degrees)
        val angleStep = arcAngle / (windows.size - 1)

        return windows.mapIndexed { index, window ->
            val angle = (index * angleStep - arcAngle / 2) * (PI / 180f)
            val x = radius * sin(angle)
            val z = centerPoint.z - radius * (1 - cos(angle))
            val rotationY = -angle * (180f / PI)

            WindowPosition(
                windowId = window.id,
                position = Vector3D(x, centerPoint.y, z),
                rotationY = rotationY.toFloat(),
                rotationX = 0f,
                rotationZ = 0f
            )
        }
    }
}
```

---

#### 2.4 OpenGL Integration (Optional - Advanced)
**For hardware-accelerated rendering:**

1. Create GLSurfaceView renderer
2. Implement vertex shaders for curved projection
3. Texture mapping for window content
4. Fragment shaders for glassmorphic effects

**Alternative:** Use HUDRenderer's Canvas-based SPATIAL_AR mode (faster to implement)

---

### Phase 3: Testing & Validation (1-2 days)

#### 3.1 UI Guidelines Compliance Check
- [ ] All components use wrapper pattern (GlassmorphicSurface)
- [ ] All styling from OceanTheme tokens
- [ ] Hover/focus states functional
- [ ] Theme switching works (Ocean ↔ Native)
- [ ] MagicUI migration path clear (documented)

#### 3.2 Curved Rendering Validation
- [ ] ArcFrontLayout renders curved arc
- [ ] Cylindrical projection accurate
- [ ] Window positions calculated correctly
- [ ] Performance: 60+ FPS with 6 windows
- [ ] Depth sorting works (near windows in front)

#### 3.3 Integration Testing
- [ ] Head-based navigation works with curved windows
- [ ] Dwell selection accurate in 3D space
- [ ] Window focus/hover states in curved layout
- [ ] Orientation changes (portrait/landscape) handled

---

## Prevention Measures

### Code Standards
1. **Mandatory wrapper pattern** for all UI components
2. **No hard-coded values** - all from theme tokens
3. **Theme tokens first** - create token before using value
4. **Migration readiness** - semantic naming for future MagicUI swap

### Architecture Guidelines
1. **Separation of concerns** - rendering logic separate from business logic
2. **Reusable infrastructure** - use existing SpatialRenderer, don't rebuild
3. **Platform-aware** - support flat 2D (phones) and curved 3D (XR glasses)
4. **Performance targets** - 60 FPS minimum, 90-120 FPS for XR

### Review Checklist
Before merging any UI component:
- [ ] Uses wrapper component (not raw Material3)
- [ ] All values from theme tokens
- [ ] Interactive states implemented (hover/focus/press)
- [ ] Theme switching tested
- [ ] Documentation updated with migration path

---

## Related Files

### Issue 1: UI Guidelines
- `/android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/WindowCard.kt` (violations)
- `/android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/OceanTheme.kt` (partial tokens)
- `/android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/TopNavigationBar.kt` (reference)
- `/android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/ControlPanel.kt` (reference)
- `/.ideacode/living-docs/LD-IDEACODE-UI-Guidelines-V2.md` (spec)
- `/.ideacode/living-docs/LD-NewAvanues-UI-Guidelines-V1.md` (spec)

### Issue 2: Curved Rendering
- `/Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/layout/presets/LinearHorizontalLayout.kt` (flat layout)
- `/Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/core/workspace/Vector3D.kt` (3D math)
- `/Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/ui/theme/WindowStyle.kt` (PerspectiveStyle)
- `/Modules/VoiceOS/managers/HUDManager/src/main/java/com/augmentalis/hudmanager/spatial/SpatialRenderer.kt` (infrastructure)
- `/Modules/VoiceOS/managers/HUDManager/src/main/java/com/augmentalis/hudmanager/rendering/HUDRenderer.kt` (SPATIAL_AR)
- `/Modules/VoiceOS/libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/smartglasses/XRManager.kt` (capabilities)

---

## Next Steps

### Immediate (Today)
1. Create wrapper components (GlassmorphicSurface, OceanCard)
2. Refactor WindowCard.kt to use wrappers and full theme tokens
3. Test UI guidelines compliance

### Short-Term (This Week)
4. Enhance OceanTheme.kt with complete token set
5. Create ThemeProvider for runtime switching
6. Add HUDManager dependency
7. Create SpatialWindowRenderer prototype

### Medium-Term (Next Sprint)
8. Implement ArcFrontLayout preset
9. Integrate curved projection math
10. Performance optimization (target 60+ FPS)
11. Full integration testing with head navigation

---

**Issue Status:** Ready for implementation
**Priority:** High (blocks production readiness + missing core feature)
**Estimated Effort:** 4-7 days (Phase 1: 1-2 days, Phase 2: 3-5 days)
