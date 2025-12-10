# LearnApp - Alias Validation and BACK Navigation Errors

**Date:** 2025-10-29 23:07 PDT
**Status:** 🔴 CRITICAL - Exploration failing
**App Tested:** Microsoft Teams
**Priority:** P0 - Blocks exploration completion

---

## Error Summary

### Error 1: Alias Validation Failure (Line 542)
```
W  Failed to add alias for com.microsoft.teams.v14161.0.0.text-0c24f16478cb:
   Alias must start with letter and contain only lowercase alphanumeric + underscores
```

**Frequency:** Multiple occurrences per screen
**Impact:** Elements registered without aliases (voice commands may fail)

---

### Error 2: BACK Navigation Failure (Lines 382-411)
```
W  BACK navigation anomaly! Expected 39cf85b4fbbfd8071e8deb932aea54849e568515f0839c9a6f63a44cef687286,
   got 3ec2211542cbe6b114c1ff993bc01078186bd678ea3d250dfbf86ca743671d94

E  Unable to recover original screen. Stopping element exploration.
```

**Frequency:** Occurs after several element clicks
**Impact:** ❌ Exploration stops prematurely (same issue as before)

---

## Root Cause Analysis

### Problem 1: Alias Sanitization Too Weak

**Current Code (sanitizeAlias):**
```kotlin
private fun sanitizeAlias(alias: String): String {
    val sanitized = alias
        .replace(Regex("[^a-zA-Z0-9_-]"), "_")  // Replace invalid chars
        .replace(Regex("_+"), "_")               // Collapse multiple underscores
        .trim('_')                               // Remove leading/trailing underscores

    return when {
        sanitized.length < 3 -> sanitized.padEnd(3, '_')
        sanitized.length > 50 -> sanitized.substring(0, 50)
        else -> sanitized
    }
}
```

**Problem:**
- ✅ Replaces invalid characters
- ❌ Does NOT ensure alias starts with a letter
- ❌ Does NOT convert to lowercase
- ❌ Allows hyphens (but AliasManager requires only underscores)

**AliasManager Requirements (from error message):**
1. Must start with a letter (a-z, A-Z)
2. Must contain only lowercase alphanumeric characters + underscores
3. No hyphens allowed
4. Length 3-50 characters

**Examples of Current Failures:**
- `"3_element"` → Starts with number ❌
- `"_button"` → Starts with underscore ❌
- `"Click-Here"` → Contains hyphen + uppercase ❌
- `""` (empty) → No fallback letter ❌

---

### Problem 2: BACK Navigation Not Handling Dynamic Screens

**Current Code (BACK verification):**
```kotlin
// After pressing BACK
val currentRootNode = accessibilityService.rootInActiveWindow
if (currentRootNode != null) {
    val currentScreenState = screenStateManager.captureScreenState(
        currentRootNode, packageName, depth
    )
    if (currentScreenState.hash != originalScreenHash) {
        android.util.Log.w("ExplorationEngine",
            "BACK navigation anomaly! Expected $originalScreenHash, got ${currentScreenState.hash}")

        // Try second BACK press
        pressBack()
        delay(1000)

        // Check again
        val retryRootNode = accessibilityService.rootInActiveWindow
        if (retryRootNode != null) {
            val retryScreenState = screenStateManager.captureScreenState(
                retryRootNode, packageName, depth
            )
            if (retryScreenState.hash != originalScreenHash) {
                android.util.Log.e("ExplorationEngine",
                    "Unable to recover original screen. Stopping element exploration.")
                break  // Exit loop
            }
        }
    }
}
```

**Problem:**
- Some apps have dynamic content (animations, live updates, notifications)
- Screen hash changes even though we're on the "same" screen
- Two BACK presses in a row may go back TWO screens (too aggressive)
- No way to distinguish "screen changed slightly" vs "navigation failed"

**Why Microsoft Teams Fails:**
- Live chat notifications update screen
- Presence status changes (online/offline)
- Timestamps update
- Screen hash changes → verification fails → exploration stops

---

## Impact Assessment

### Alias Validation Failures
**Severity:** MEDIUM
- Elements still registered (UUID works)
- Voice commands may fail (no alias)
- User experience degraded

### BACK Navigation Failures
**Severity:** CRITICAL
- Exploration stops prematurely
- Same issue as original premature termination bug
- Only explores 2-3 elements before stopping
- Database incomplete

---

## Solution Requirements

### For Alias Validation:
1. ✅ Ensure alias starts with letter (prepend if needed)
2. ✅ Convert to lowercase
3. ✅ Remove hyphens (replace with underscores)
4. ✅ Validate against AliasManager requirements
5. ✅ Provide robust fallback (e.g., "elem_button")

### For BACK Navigation:
1. ✅ Add tolerance for minor screen changes (dynamic content)
2. ✅ Use screen structure similarity, not exact hash match
3. ✅ Avoid double BACK presses (too aggressive)
4. ✅ Log what changed (for debugging)
5. ✅ Only stop exploration if app actually closed or crashed

---

## Proposed Fixes

### Fix 1: Enhanced Alias Sanitization

**New sanitizeAlias() function:**
```kotlin
private fun sanitizeAlias(alias: String): String {
    // 1. Convert to lowercase
    var sanitized = alias.lowercase()

    // 2. Replace invalid characters with underscores
    sanitized = sanitized.replace(Regex("[^a-z0-9_]"), "_")

    // 3. Collapse multiple underscores
    sanitized = sanitized.replace(Regex("_+"), "_")

    // 4. Remove leading/trailing underscores
    sanitized = sanitized.trim('_')

    // 5. Ensure starts with letter (prepend if needed)
    if (sanitized.isEmpty() || !sanitized[0].isLetter()) {
        sanitized = "elem_$sanitized"
    }

    // 6. Ensure minimum 3 characters
    if (sanitized.length < 3) {
        sanitized = sanitized.padEnd(3, 'x')
    }

    // 7. Truncate to 50 characters
    if (sanitized.length > 50) {
        sanitized = sanitized.substring(0, 50)
    }

    return sanitized
}
```

**Validation:**
- Starts with letter ✅
- Lowercase only ✅
- No hyphens ✅
- Alphanumeric + underscores only ✅
- Length 3-50 ✅

---

### Fix 2: Screen Structure Similarity Check

**Replace exact hash match with structure similarity:**

```kotlin
// After pressing BACK
val currentRootNode = accessibilityService.rootInActiveWindow
if (currentRootNode != null) {
    val currentScreenState = screenStateManager.captureScreenState(
        currentRootNode, packageName, depth
    )

    // Check if screen STRUCTURE is similar (not exact hash)
    val isSimilarScreen = screenStateManager.areScreensSimilar(
        originalScreenHash,
        currentScreenState.hash,
        similarityThreshold = 0.85  // 85% similar
    )

    if (!isSimilarScreen) {
        // Check if app is still running
        if (currentScreenState.packageName != packageName) {
            android.util.Log.e("ExplorationEngine",
                "App closed or switched. Stopping exploration.")
            break
        }

        android.util.Log.w("ExplorationEngine",
            "BACK navigation anomaly! Expected similar to $originalScreenHash, " +
            "got ${currentScreenState.hash} (similarity below 85%)")

        // Try ONE more BACK press (not two)
        pressBack()
        delay(1000)

        // Check if we recovered
        val retryRootNode = accessibilityService.rootInActiveWindow
        if (retryRootNode != null) {
            val retryScreenState = screenStateManager.captureScreenState(
                retryRootNode, packageName, depth
            )
            val retryIsSimilar = screenStateManager.areScreensSimilar(
                originalScreenHash,
                retryScreenState.hash,
                similarityThreshold = 0.85
            )

            if (!retryIsSimilar) {
                android.util.Log.w("ExplorationEngine",
                    "Unable to recover original screen structure. Continuing with exploration.")
                // DON'T break - just log and continue
                // The navigation graph will handle the new path
            }
        }
    }
}
```

**Key Changes:**
1. Use similarity check instead of exact hash match
2. Tolerate 15% difference (dynamic content)
3. Only ONE retry BACK press (not two)
4. Don't stop exploration unless app closed
5. Log differences for debugging

---

### Fix 3: Add ScreenStateManager.areScreensSimilar()

**New method in ScreenStateManager:**
```kotlin
/**
 * Check if two screens are structurally similar
 *
 * Compares element counts, types, and layout to determine if screens
 * are the same despite minor content changes (e.g., timestamps, notifications).
 *
 * @param hash1 First screen hash
 * @param hash2 Second screen hash
 * @param similarityThreshold Minimum similarity (0.0-1.0)
 * @return true if screens are similar
 */
fun areScreensSimilar(
    hash1: String,
    hash2: String,
    similarityThreshold: Double = 0.85
): Boolean {
    // Exact match (fast path)
    if (hash1 == hash2) return true

    // Get screen states from cache/database
    val screen1 = getScreenState(hash1) ?: return false
    val screen2 = getScreenState(hash2) ?: return false

    // Compare element counts (should be similar)
    val countSimilarity = minOf(
        screen1.elementCount.toDouble() / screen2.elementCount.toDouble(),
        screen2.elementCount.toDouble() / screen1.elementCount.toDouble()
    )

    // Compare element types (should match)
    val type1 = screen1.elementTypes.sorted()
    val type2 = screen2.elementTypes.sorted()
    val typeMatches = type1.intersect(type2.toSet()).size
    val typeSimilarity = typeMatches.toDouble() / maxOf(type1.size, type2.size)

    // Combined similarity
    val similarity = (countSimilarity * 0.5) + (typeSimilarity * 0.5)

    return similarity >= similarityThreshold
}
```

---

## Visual Logging/Debugging System

### Problem: Can't See What LearnApp Is Seeing

Current logging only shows:
- Element text
- Screen hash
- Navigation paths

**Missing:**
- Visual representation of screen
- What elements look like
- Where clicks are happening
- Screen structure

---

### Solution: Multi-Level Visual Debugging

#### **Level 1: Enhanced Text Logging**

Add detailed element information:
```kotlin
android.util.Log.d("ExplorationEngine-Visual",
    """
    |=== SCREEN STATE ===
    |Hash: ${screenState.hash}
    |Package: ${screenState.packageName}
    |Element Count: ${elements.size}
    |
    |=== ELEMENTS ===
    |${elements.mapIndexed { i, elem ->
        """
        |[$i] ${elem.className.substringAfterLast('.')}
        |    Text: "${elem.text}"
        |    ContentDesc: "${elem.contentDescription}"
        |    Bounds: ${elem.bounds}
        |    Clickable: ${elem.isClickable}
        |    Classification: ${elem.classification}
        |    UUID: ${elem.uuid}
        |    Alias: ${elem.alias}
        """.trimMargin()
    }.joinToString("\n")}
    |=== END SCREEN ===
    """.trimMargin()
)
```

---

#### **Level 2: Screenshot Capture**

**New: ScreenshotService**
```kotlin
class ScreenshotService(private val context: Context) {

    /**
     * Capture screenshot during exploration
     *
     * Saves to: /sdcard/Android/data/com.augmentalis.voiceos/files/learnapp_screenshots/
     */
    fun captureScreenshot(screenHash: String, timestamp: Long): File? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            android.util.Log.w("ScreenshotService", "Screenshots require Android 11+")
            return null
        }

        val screenshotDir = File(
            context.getExternalFilesDir(null),
            "learnapp_screenshots"
        )
        screenshotDir.mkdirs()

        val screenshotFile = File(
            screenshotDir,
            "screen_${screenHash.substring(0, 8)}_$timestamp.png"
        )

        // Use MediaProjection API to capture screenshot
        // (requires user permission - one-time grant)
        try {
            // Implementation using MediaProjection
            // ...
            android.util.Log.d("ScreenshotService", "Screenshot saved: ${screenshotFile.absolutePath}")
            return screenshotFile
        } catch (e: Exception) {
            android.util.Log.e("ScreenshotService", "Failed to capture screenshot", e)
            return null
        }
    }
}
```

---

#### **Level 3: Element Overlay Visualization**

**New: AccessibilityOverlayService**

Draw visual overlay showing:
- Red boxes around dangerous elements
- Green boxes around safe clickable elements
- Blue boxes around login fields
- Yellow boxes around disabled elements
- Element numbers for reference

```kotlin
class AccessibilityOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayView? = null

    fun showElementOverlay(elements: List<ElementInfo>) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )

        overlayView = OverlayView(this, elements)
        windowManager.addView(overlayView, params)

        // Auto-hide after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            hideOverlay()
        }, 3000)
    }

    fun hideOverlay() {
        overlayView?.let { view ->
            windowManager.removeView(view)
            overlayView = null
        }
    }
}

class OverlayView(
    context: Context,
    private val elements: List<ElementInfo>
) : View(context) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        elements.forEachIndexed { index, element ->
            // Choose color based on classification
            paint.color = when (element.classification) {
                "safe_clickable" -> Color.GREEN
                "dangerous" -> Color.RED
                "login" -> Color.BLUE
                "disabled" -> Color.YELLOW
                else -> Color.GRAY
            }

            // Draw rectangle around element
            canvas.drawRect(element.bounds, paint)

            // Draw element number
            paint.style = Paint.Style.FILL
            paint.textSize = 24f
            canvas.drawText(
                "#$index",
                element.bounds.left.toFloat(),
                element.bounds.top.toFloat() - 8,
                paint
            )
            paint.style = Paint.Style.STROKE
        }
    }
}
```

---

#### **Level 4: HTML Report Generation**

**New: ExplorationReportGenerator**

Generate visual HTML report after exploration:

```html
<!DOCTYPE html>
<html>
<head>
    <title>LearnApp Exploration Report - Microsoft Teams</title>
    <style>
        .screen { border: 1px solid #ccc; margin: 20px; padding: 10px; }
        .element { padding: 5px; margin: 5px; border-left: 3px solid; }
        .safe { border-color: green; }
        .dangerous { border-color: red; }
        .login { border-color: blue; }
        img { max-width: 300px; }
    </style>
</head>
<body>
    <h1>Exploration Report</h1>
    <h2>Package: com.microsoft.teams</h2>
    <h3>Total Screens: 5</h3>
    <h3>Total Elements: 87</h3>

    <div class="screen">
        <h3>Screen 1: Hash 39cf85b4...</h3>
        <img src="screen_39cf85b4_1234567890.png">
        <h4>Elements (18):</h4>
        <div class="element safe">
            [0] Button - "Chat"
            UUID: com.microsoft.teams...
            Alias: chat_button
            Clicked: Yes → Screen 2
        </div>
        <div class="element dangerous">
            [1] Button - "Sign Out"
            UUID: com.microsoft.teams...
            Alias: sign_out_button
            Clicked: No (dangerous)
        </div>
        <!-- ... more elements ... -->
    </div>
    <!-- ... more screens ... -->
</body>
</html>
```

---

## Implementation Plan

### Phase 1: Fix Critical Errors (Agents 1-3)
**Duration:** 15-20 minutes

**Agent 1: Kotlin Expert - Alias Validation Specialist**
- Fix `sanitizeAlias()` function
- Add validation tests
- Ensure AliasManager compatibility

**Agent 2: Android Navigation Expert - BACK Handler Specialist**
- Implement `areScreensSimilar()` in ScreenStateManager
- Modify BACK navigation verification logic
- Add tolerance for dynamic content

**Agent 3: Android Accessibility Expert - Error Recovery Specialist**
- Improve error handling in click loop
- Add graceful degradation
- Prevent premature exploration termination

---

### Phase 2: Visual Logging System (Agents 4-5)
**Duration:** 30-40 minutes

**Agent 4: Android UI Expert - Overlay Visualization Specialist**
- Implement AccessibilityOverlayService
- Create OverlayView with element highlighting
- Add user setting to enable/disable overlay

**Agent 5: Android Developer - Screenshot & Reporting Specialist**
- Implement ScreenshotService
- Create ExplorationReportGenerator
- Add HTML report export

---

## Testing Strategy

### Phase 1 Testing:
1. **Microsoft Teams** - Verify alias validation works
2. **Microsoft Teams** - Verify BACK navigation doesn't stop exploration
3. **Settings** - Verify no regressions
4. **Calculator** - Verify similarity check doesn't break simple apps

### Phase 2 Testing:
1. Enable overlay → See element classifications visually
2. Review screenshots → Confirm what LearnApp sees
3. Review HTML report → Complete exploration summary

---

## Priority

**IMMEDIATE (Phase 1):**
- Fix alias validation (blocks voice commands)
- Fix BACK navigation (blocks exploration completion)

**SHORT-TERM (Phase 2):**
- Add visual debugging (helps future troubleshooting)

---

**Created:** 2025-10-29 23:07 PDT
**Status:** Ready for parallel agent implementation
**Next:** Deploy 5 specialist agents (3 for fixes, 2 for visual logging)
