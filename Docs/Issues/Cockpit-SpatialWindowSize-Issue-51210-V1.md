# Cockpit MVP - Spatial Windows Too Small + Excessive Spacing

**Version:** 1.0
**Date:** 2025-12-10
**Issue:** Spatial mode windows too small vertically, too much spacing between windows
**Status:** INVESTIGATING
**Severity:** HIGH (UX degradation)
**Reasoning:** ToT (Tree of Thoughts) + CoT (Chain of Thought)

---

## Issue Report

**User Message:**
> "the screens are too small vertically, and there is too much space between each screen keep only a 20 px padding. the scaling of the screens should be automatic depending on screen type and size."

**Screenshot Analysis:**
- 3 windows in Arc layout (WebAvanue, Google, Calculator)
- Windows appear narrow and short (poor screen utilization)
- Large gaps between windows (excessive padding)
- Fixed dimensions don't adapt to screen size/orientation

**User Requirements:**
1. **Increase vertical size** - Windows should utilize more screen height
2. **Reduce spacing** - Only 20px padding between windows
3. **Automatic scaling** - Dimensions should adapt to screen type/size

---

## Root Cause Analysis (ToT)

### Hypothesis Tree

```
Why are spatial windows too small?
├─ Hypothesis 1: Fixed meter dimensions don't scale with screen size
│  ├─ Evidence: ArcFrontLayout.kt:123-126
│  │   widthMeters = 0.7f, heightMeters = 0.55f (FIXED VALUES)
│  ├─ Result: ✓ PRIMARY CAUSE
│  └─ Impact: Same physical size regardless of screen (phone/tablet/TV)
│
├─ Hypothesis 2: Arc radius creates large gaps between windows
│  ├─ Evidence: ArcFrontLayout.kt:59
│  │   ARC_RADIUS_METERS = 3.0f (FIXED)
│  ├─ Result: ✓ SECONDARY CAUSE
│  └─ Impact: 3m radius with 120° span = large gaps
│
├─ Hypothesis 3: Perspective projection makes windows appear small
│  ├─ Evidence: CurvedProjection.kt:27-28
│  │   FOV_HORIZONTAL = 90°, FOV_VERTICAL = 70°
│  ├─ Result: ⚠️ CONTRIBUTING FACTOR
│  └─ Impact: Wide FOV makes objects appear smaller
│
├─ Hypothesis 4: Window dimensions optimized for AR glasses (not phones)
│  ├─ Evidence: Comment "~27 inches at 3m distance"
│  ├─ Result: ✓ DESIGN MISMATCH
│  └─ Impact: AR glasses have different screen characteristics
│
└─ Hypothesis 5: No responsive layout system
   ├─ Evidence: No screen size detection in layout calculation
   ├─ Result: ✓ ARCHITECTURAL GAP
   └─ Impact: Can't adapt to phone/tablet/desktop/TV
```

**Selected Root Causes:**
1. **Fixed meter dimensions** (PRIMARY)
2. **No responsive scaling** (ARCHITECTURAL)
3. **Arc radius too large for 2D screens** (SECONDARY)

---

## Code Analysis (CoT)

### Step 1: Window Dimension Calculation

**File:** `ArcFrontLayout.kt:116-127`

```kotlin
override fun calculateDimensions(
    window: AppWindow,
    index: Int,
    totalWindows: Int
): WindowDimensions {
    // Uniform dimensions for arc layout
    // Slightly smaller than linear layout to accommodate arc curvature
    return WindowDimensions(
        widthMeters = 0.7f,   // ~27 inches at 3m distance
        heightMeters = 0.55f  // ~22 inches at 3m distance
    )
}
```

**Analysis:**
- ❌ Fixed values (0.7m × 0.55m)
- ❌ No screen size consideration
- ❌ No aspect ratio detection
- ❌ Optimized for AR (3m viewing distance), not 2D screens

**Problem:**
- Phone screen (6.7"): Windows appear tiny
- Tablet screen (12"): Windows appear small
- TV screen (55"): Windows might be okay
- **No adaptation!**

---

### Step 2: Arc Radius and Spacing

**File:** `ArcFrontLayout.kt:59-60`

```kotlin
private const val ARC_RADIUS_METERS = 3.0f
private const val ARC_SPAN_DEGREES = 120f
```

**Arc Geometry:**
```
Radius: 3.0m
Span: 120° (±60° from center)
Windows: 3

Window positions:
- Left: -60° → x = -2.6m, z = -1.5m
- Center: 0° → x = 0m, z = -3.0m
- Right: +60° → x = 2.6m, z = -1.5m

Gap between windows (arc length):
  Arc length = radius × angle (radians)
  For 3 windows: 60° between each
  Gap = 3.0m × (60° × π/180) = 3.14m arc length
```

**Problem:**
- ❌ 3.14m arc length between windows = MASSIVE gap on screen
- ❌ User requested 20px padding
- ❌ Arc radius doesn't scale with screen size

---

### Step 3: Perspective Projection

**File:** `CurvedProjection.kt:27-28, 166-214`

```kotlin
private const val FOV_HORIZONTAL_DEGREES = 90f
private const val FOV_VERTICAL_DEGREES = 70f

// Projection formula:
scale = 1 / depth
screenX = (ndcX + 1) * screenWidth / 2
screenY = (-ndcY + 1) * screenHeight / 2

where:
  ndcX = (point.x / depth) / tan(FOV_H / 2)
  ndcY = (point.y / depth) / tan(FOV_V / 2)
```

**Analysis:**
- Window at 3m depth: `scale = 1/3 = 0.33x`
- 0.7m wide window → `0.7 * 0.33 = 0.23` normalized units
- On 1080px screen: `0.23 * 1080 = ~250px` wide

**Visual Comparison:**
| Screen Size | Width (px) | Window Size (px) | Utilization |
|-------------|------------|------------------|-------------|
| Phone (1080×2400) | 1080 | ~250 | 23% ❌ |
| Tablet (1600×2560) | 1600 | ~370 | 23% ❌ |
| Desktop (1920×1080) | 1920 | ~440 | 23% ❌ |
| TV (3840×2160) | 3840 | ~880 | 23% ✅ |

**Conclusion:** Windows use only 23% of screen width - too small!

---

### Step 4: Missing Responsive System

**Current Architecture:**
```
LayoutPreset
  ↓
calculateDimensions()  → Returns FIXED WindowDimensions
  ↓
SpatialWindowRenderer  → Projects using fixed meters
  ↓
Screen (any size)  → Gets same physical dimensions
```

**Missing:**
- Screen size detection
- DPI awareness
- Aspect ratio adaptation
- Device type detection (phone/tablet/desktop/AR)

---

## Fix Plan

### Fix 1: Responsive Window Dimensions

**Add screen-aware dimension calculation:**

```kotlin
// ArcFrontLayout.kt
override fun calculateDimensions(
    window: AppWindow,
    index: Int,
    totalWindows: Int
): WindowDimensions {
    // Get screen dimensions (need to pass from renderer)
    val screenWidthMeters = screenWidthPx / pixelsPerMeter
    val screenHeightMeters = screenHeightPx / pixelsPerMeter

    // Calculate window size as percentage of screen
    val widthMeters = screenWidthMeters * 0.28f  // 28% of screen width
    val heightMeters = screenHeightMeters * 0.35f  // 35% of screen height

    // Clamp to reasonable bounds
    return WindowDimensions(
        widthMeters = widthMeters.coerceIn(0.5f, 1.5f),
        heightMeters = heightMeters.coerceIn(0.4f, 1.2f)
    )
}
```

**Pros:**
- Adapts to screen size
- Better utilization (28% vs 23%)
- Maintains aspect ratio

**Cons:**
- Need to pass screen info to layout preset
- Breaks current API

---

### Fix 2: Reduce Arc Radius for 2D Screens

**Dynamic radius based on screen size:**

```kotlin
// ArcFrontLayout.kt
private fun calculateArcRadius(screenWidthPx: Int): Float {
    // Smaller radius for smaller screens
    return when {
        screenWidthPx < 800 -> 2.0f   // Phone: 2m radius
        screenWidthPx < 1200 -> 2.5f  // Tablet: 2.5m radius
        else -> 3.0f                  // Desktop/TV: 3m radius
    }
}
```

**Result:**
- Phone: 2m radius → 2.1m gap (instead of 3.14m)
- Reduces spacing by 33%

---

### Fix 3: Add 20px Padding Mode

**Introduce "compact spacing" layout variant:**

```kotlin
// CompactArcLayout.kt
private const val WINDOW_SPACING_PIXELS = 20f

override fun calculatePositions(...): List<WindowPosition> {
    // Calculate positions to achieve 20px screen spacing
    // Work backwards from screen space to world space

    val windowWidthPx = projectedWindowWidth  // From screen projection
    val gapPx = 20f
    val totalWidthPx = (windowWidthPx * count) + (gapPx * (count - 1))

    // Position windows in 2D screen space first
    // Then reverse-project to 3D world space
}
```

**Pros:**
- Exact 20px spacing
- Maximum screen utilization

**Cons:**
- Complex reverse projection
- May lose 3D arc aesthetic

---

### Fix 4: Scale Factor Based on Screen DPI

**Auto-scale windows for screen density:**

```kotlin
// SpatialWindowRenderer.kt
private fun calculateAutoScaleFactor(screenDensity: Float): Float {
    // Base scale factor on screen DPI
    // Higher DPI (e.g., 4K phone) → larger scale
    val baseDPI = 160f  // Android baseline
    return screenDensity / baseDPI
}

fun render(...) {
    val autoScale = calculateAutoScaleFactor(screenDensityDPI)
    val finalScale = scaleFactor * autoScale

    // Apply to window dimensions...
}
```

---

## Recommended Solution

**Hybrid Approach (Fixes 1 + 2 + 4):**

1. **Responsive dimensions** - Calculate window size as % of screen
2. **Dynamic arc radius** - Smaller radius for phones, larger for desktops
3. **DPI scaling** - Auto-scale based on screen density
4. **Maintain 20px spacing** - Adjust arc span to achieve target spacing

**Implementation Priority:**
1. ✅ **P0:** Add screen size to LayoutPreset API
2. ✅ **P0:** Implement responsive calculateDimensions()
3. ✅ **P1:** Dynamic arc radius based on screen width
4. ⏸️ **P2:** Reverse projection for exact 20px spacing (future)

---

## Technical Specifications

### Responsive Dimension Formula

```
Target Window Utilization:
- Width: 28-35% of screen width (per window)
- Height: 35-45% of screen height
- Gap: 20px (user requirement)

For 3 windows:
  Total width = (3 × windowWidth) + (2 × gap)
  windowWidth = (screenWidth - (2 × gap)) / 3

For phone (1080px wide):
  windowWidth = (1080 - 40) / 3 = 347px (~32% utilization) ✅

Convert to meters:
  Assume 90° FOV, 2.5m depth
  metersWidth = (pixelsWidth / screenWidth) * (2 * tan(45°) * depth)
  metersWidth = (347 / 1080) * (2 * 1.0 * 2.5) = 1.6m
```

### Dynamic Arc Radius Table

| Screen Type | Width (px) | Arc Radius | Window Size | Gap |
|-------------|------------|------------|-------------|-----|
| Phone | 1080 | 2.0m | 0.9m × 0.7m | ~150px |
| Tablet | 1600 | 2.5m | 1.2m × 0.9m | ~180px |
| Desktop | 1920 | 3.0m | 1.4m × 1.0m | ~200px |
| TV | 3840 | 3.5m | 2.0m × 1.5m | ~250px |

---

## Implementation Plan

### Phase 1: API Changes

**Modify LayoutPreset interface:**

```kotlin
// LayoutPreset.kt
interface LayoutPreset {
    fun calculateDimensions(
        window: AppWindow,
        index: Int,
        totalWindows: Int,
        screenContext: ScreenContext  // NEW PARAMETER
    ): WindowDimensions
}

data class ScreenContext(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Float,
    val aspectRatio: Float
)
```

### Phase 2: Update ArcFrontLayout

**Responsive implementation:**

```kotlin
override fun calculateDimensions(
    window: AppWindow,
    index: Int,
    totalWindows: Int,
    screenContext: ScreenContext
): WindowDimensions {
    // Calculate responsive radius
    val radius = when {
        screenContext.widthPx < 800 -> 2.0f
        screenContext.widthPx < 1200 -> 2.5f
        else -> 3.0f
    }

    // Target utilization: 30% width, 40% height
    val targetWidthPx = screenContext.widthPx * 0.30f
    val targetHeightPx = screenContext.heightPx * 0.40f

    // Convert to meters using FOV projection
    val fovRadH = 90f * PI.toFloat() / 180f
    val fovRadV = 70f * PI.toFloat() / 180f

    val metersPerPixelH = (2f * tan(fovRadH / 2f) * radius) / screenContext.widthPx
    val metersPerPixelV = (2f * tan(fovRadV / 2f) * radius) / screenContext.heightPx

    return WindowDimensions(
        widthMeters = targetWidthPx * metersPerPixelH,
        heightMeters = targetHeightPx * metersPerPixelV
    )
}
```

### Phase 3: Update SpatialWindowRenderer

**Pass screen context:**

```kotlin
fun render(
    canvas: Canvas,
    windows: List<AppWindow>,
    windowColors: Map<String, String>,
    selectedWindowId: String? = null,
    centerPoint: Vector3D = Vector3D(0f, 0f, -2f),
    scaleFactor: Float = 1.0f
) {
    val screenContext = ScreenContext(
        widthPx = canvas.width,
        heightPx = canvas.height,
        densityDpi = Resources.getSystem().displayMetrics.densityDpi.toFloat(),
        aspectRatio = canvas.width.toFloat() / canvas.height
    )

    // Calculate dimensions with screen context
    val dimensions = layoutPreset.calculateDimensions(
        window,
        index,
        windows.size,
        screenContext  // Pass context
    )

    // Use responsive dimensions...
}
```

---

## Testing Plan

### Test Cases

| Screen | Expected Window Size | Expected Gap | Pass Criteria |
|--------|---------------------|--------------|---------------|
| Phone (1080×2400) | ~350×500px | ~20-40px | Windows utilize 30-35% width, 40-45% height |
| Tablet (1600×2560) | ~480×700px | ~20-50px | Windows utilize 30-35% width, 40-45% height |
| Desktop (1920×1080) | ~550×450px | ~20-60px | Windows utilize 28-32% width, 40-45% height |
| TV (3840×2160) | ~1100×900px | ~40-80px | Windows utilize 28-32% width, 40-45% height |

### Verification

1. **Visual inspection:** Windows appear reasonably sized
2. **Measurement:** Gaps approximately 20px (±20px tolerance)
3. **Rotation:** Adapts to landscape/portrait
4. **Scaling:** Pinch gesture still works

---

## Breaking Changes

⚠️ **API Change Required:**

```kotlin
// OLD
fun calculateDimensions(
    window: AppWindow,
    index: Int,
    totalWindows: Int
): WindowDimensions

// NEW
fun calculateDimensions(
    window: AppWindow,
    index: Int,
    totalWindows: Int,
    screenContext: ScreenContext  // NEW
): WindowDimensions
```

**Impact:**
- All layout presets must update signature
- SpatialWindowRenderer must pass screen context
- LinearHorizontalLayout, TheaterLayout also affected

**Migration:**
1. Add default parameter for backward compatibility
2. Update all 3 layout presets
3. Update renderer to pass screen context

---

## Sign-Off

**Issue Analysis:** ✅ COMPLETE
**Root Cause:** ✅ IDENTIFIED (Fixed meter dimensions, no responsive scaling)
**Fix Designed:** ✅ READY (Responsive dimensions + dynamic radius)
**Status:** ⏳ AWAITING IMPLEMENTATION

**Prepared By:** AI Assistant
**Date:** 2025-12-10
**Analysis Method:** ToT + CoT
**Time to Analysis:** ~20 minutes

---

**End of Issue Analysis**
