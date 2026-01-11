# VoiceOSCoreNG Compose Support - Consolidated Implementation Plan

**Module:** VoiceOSCoreNG
**Date:** 2026-01-08
**Version:** 2.0 (Consolidated)
**Status:** Ready for Implementation
**Method:** CoT + Swarm + Auto

---

## Executive Summary

This plan consolidates all tasks from the analysis into an optimized execution order that:
1. Groups changes by **file proximity** to minimize context switching
2. Prioritizes **P0 → P1 → P2 → P3** (highest to lowest)
3. Ensures **no duplicate file touches** - each file is edited once with all changes

**Total Tasks:** 17 (consolidated from 20)
**Files Modified:** 8 (3 new, 5 existing)
**Estimated LOC:** ~550

---

## Chain of Thought Analysis

### Problem Decomposition

```
Current State: Compose apps work but with ~55% VUID accuracy
Target State: 95%+ VUID accuracy for Compose/M3 apps

Root Causes Identified:
├── C1: No COMPOSE in FrameworkType enum
├── C2: No ComposeHandler to detect/process Compose apps
├── C3: No Compose patterns in VUIDGenerator
├── C4: ElementInfo missing Compose semantic fields
└── C5: AccessibilityService not extracting Compose semantics

Solution Path:
1. Add COMPOSE framework type → enables detection
2. Create ComposeHandler → enables specialized processing
3. Add VUID patterns → enables accurate type codes
4. Extend ElementInfo → enables semantic storage
5. Extract semantics → enables role-based type mapping
```

### Dependency Graph

```
FrameworkInfo.kt (COMPOSE enum)
         ↓
ComposeHandler.kt (depends on COMPOSE enum)
         ↓
FrameworkHandler.kt (register handler)
         ↓
VUIDGenerator.kt (independent - can parallelize)
         ↓
ElementInfo.kt (add semantic fields)
         ↓
VoiceOSAccessibilityService.kt (extract semantics)
         ↓
Tests (validate all changes)
```

---

## Phase 1: Core Framework Detection (P0 - Critical)

**Priority:** P0 - Must complete first
**Files:** 4 (1 new, 3 modified)
**Dependency:** Sequential - each task depends on previous

### Task 1.1: Add COMPOSE to FrameworkType

**File:** `common/FrameworkInfo.kt:7-28`
**Change Type:** Add enum value + detector

```kotlin
// Line 7-28 - Add COMPOSE after REACT_NATIVE
enum class FrameworkType {
    NATIVE,
    FLUTTER,
    UNITY,
    UNREAL_ENGINE,
    REACT_NATIVE,
    COMPOSE,    // ← ADD THIS (Priority 85, between RN and WebView)
    WEBVIEW,
    UNKNOWN
}
```

**Also add to FrameworkDetector (line 58-170):**
```kotlin
// Add after REACT_NATIVE_PREFIXES
private val COMPOSE_PREFIXES = listOf(
    "androidx.compose.ui.platform.AndroidComposeView",
    "androidx.compose.ui.platform.ComposeView"
)

// Add detection between React Native and WebView checks (~line 135)
// 5. Check for Compose
val composeIndicators = findMatchingClasses(classNames, COMPOSE_PREFIXES)
if (composeIndicators.isNotEmpty()) {
    return FrameworkInfo(
        type = FrameworkType.COMPOSE,
        version = null,
        packageIndicators = composeIndicators
    )
}
```

**Acceptance:** `FrameworkType.COMPOSE` exists, `FrameworkDetector` detects Compose apps

---

### Task 1.2: Create ComposeHandler

**File:** `handlers/ComposeHandler.kt` (NEW)
**Lines:** ~120

```kotlin
package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkType
import com.augmentalis.voiceoscoreng.common.VUIDTypeCode

/**
 * Handler for Jetpack Compose applications.
 *
 * Compose apps render via AndroidComposeView and expose UI elements
 * through accessibility semantics rather than traditional View classes.
 *
 * Detection: AndroidComposeView, ComposeView, androidx.compose.*
 * Priority: 90 (between Flutter:100 and ReactNative:80)
 */
class ComposeHandler : FrameworkHandler {

    override val frameworkType: FrameworkType = FrameworkType.COMPOSE

    // Markers that identify a Compose application container
    private val composeContainers = setOf(
        "androidx.compose.ui.platform.AndroidComposeView",
        "AndroidComposeView",
        "ComposeView",
        "androidx.compose.ui.platform.ComposeView"
    )

    // Compose class name patterns in accessibility tree
    private val composePatterns = setOf(
        "androidx.compose.",
        "compose.material",
        "compose.material3"
    )

    // Semantic role to VUID type code mapping (from Compose Role enum)
    private val roleToTypeCode = mapOf(
        "Button" to VUIDTypeCode.BUTTON,
        "Checkbox" to VUIDTypeCode.CHECKBOX,
        "Switch" to VUIDTypeCode.SWITCH,
        "RadioButton" to VUIDTypeCode.CHECKBOX,
        "Tab" to VUIDTypeCode.TAB,
        "Slider" to VUIDTypeCode.SLIDER,
        "Image" to VUIDTypeCode.IMAGE,
        "DropdownList" to VUIDTypeCode.MENU,
        "ProgressBar" to VUIDTypeCode.ELEMENT
    )

    override fun canHandle(elements: List<ElementInfo>): Boolean {
        return elements.any { element ->
            composeContainers.any { marker ->
                element.className.contains(marker, ignoreCase = true)
            } || composePatterns.any { pattern ->
                element.className.startsWith(pattern, ignoreCase = true)
            }
        }
    }

    override fun processElements(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { isRelevantComposeElement(it) }
    }

    override fun getSelectors(): List<String> {
        return composeContainers.toList() + composePatterns.toList()
    }

    override fun isActionable(element: ElementInfo): Boolean {
        return element.isClickable ||
               element.isLongClickable ||
               element.isScrollable ||
               element.hasVoiceContent
    }

    override fun getPriority(): Int = 90 // Between Flutter (100) and ReactNative (80)

    /**
     * Get VUID type code for a Compose element.
     * Uses semantic role if available, falls back to class name analysis.
     */
    fun getComposeTypeCode(element: ElementInfo, role: String = ""): VUIDTypeCode {
        // Try semantic role first
        if (role.isNotBlank()) {
            roleToTypeCode[role]?.let { return it }
        }

        // Fall back to class name analysis
        val className = element.className.lowercase()
        return when {
            className.contains("button") -> VUIDTypeCode.BUTTON
            className.contains("textfield") -> VUIDTypeCode.INPUT
            className.contains("checkbox") -> VUIDTypeCode.CHECKBOX
            className.contains("switch") -> VUIDTypeCode.SWITCH
            className.contains("slider") -> VUIDTypeCode.SLIDER
            className.contains("lazycolumn") || className.contains("lazyrow") -> VUIDTypeCode.SCROLL
            className.contains("lazygrid") -> VUIDTypeCode.SCROLL
            className.contains("navigationbar") -> VUIDTypeCode.MENU
            className.contains("tabrow") -> VUIDTypeCode.TAB
            className.contains("dialog") || className.contains("bottomsheet") -> VUIDTypeCode.DIALOG
            className.contains("card") -> VUIDTypeCode.CARD
            className.contains("image") || className.contains("icon") -> VUIDTypeCode.IMAGE
            className.contains("column") || className.contains("row") -> VUIDTypeCode.LAYOUT
            className.contains("box") || className.contains("scaffold") -> VUIDTypeCode.LAYOUT
            else -> VUIDTypeCode.ELEMENT
        }
    }

    /**
     * Check if element is relevant for voice commands.
     */
    private fun isRelevantComposeElement(element: ElementInfo): Boolean {
        // Skip the container itself
        if (composeContainers.any { element.className.contains(it) }) {
            return false
        }

        // Skip layout containers without content
        if (isLayoutContainer(element) && !element.hasVoiceContent && !element.isClickable) {
            return false
        }

        return element.hasVoiceContent || element.isActionable
    }

    /**
     * Check if element is a Compose layout container.
     */
    private fun isLayoutContainer(element: ElementInfo): Boolean {
        val className = element.className.lowercase()
        return className.contains("column") ||
               className.contains("row") ||
               className.contains("box") ||
               className.contains("surface") ||
               className.contains("scaffold")
    }

    /**
     * Check if this is the root Compose container.
     */
    fun isComposeContainer(element: ElementInfo): Boolean {
        return composeContainers.any { element.className.contains(it, ignoreCase = true) }
    }
}
```

**Acceptance:** ComposeHandler created, canHandle() works, getPriority() returns 90

---

### Task 1.3: Register ComposeHandler

**File:** `handlers/FrameworkHandler.kt:107-114`
**Change Type:** Add registration

```kotlin
fun registerDefaults() {
    clear()
    register(FlutterHandler())      // Priority 100
    register(ComposeHandler())      // Priority 90  ← ADD THIS
    register(UnityHandler())        // Priority 60
    register(ReactNativeHandler())  // Priority 80
    register(WebViewHandler())      // Priority 70
    register(NativeHandler())       // Priority 0 (fallback)
}
```

**Acceptance:** ComposeHandler registered, priority order maintained

---

### Task 1.4: Update NativeHandler Selectors

**File:** `handlers/NativeHandler.kt:16-22`
**Change Type:** Add patterns

```kotlin
private val androidWidgets = setOf(
    "android.widget.",
    "android.view.",
    "androidx.appcompat.",
    "androidx.recyclerview.",
    "androidx.constraintlayout.",     // ADD
    "androidx.coordinatorlayout.",    // ADD
    "androidx.viewpager2.",           // ADD
    "com.google.android.material.",
    "androidx.compose."               // ADD (fallback for leaked Compose elements)
)
```

**Acceptance:** New selectors added, no breaking changes

---

## Phase 2: VUID Pattern Updates (P1 - High)

**Priority:** P1 - After Phase 1
**Files:** 1 (`VUIDGenerator.kt`)
**Change Type:** Add patterns + update validation
**Note:** ALL pattern changes in single file edit

### Task 2.1-2.9: Complete VUIDGenerator Update

**File:** `common/VUIDGenerator.kt`
**Lines Modified:** ~98-145, ~95, ~215-227

**All changes in one edit:**

```kotlin
// Line 95 - Update regex to include new type codes (k, w, z, r, a)
private val vuidPattern = Regex("^[0-9a-f]{6}-[bisteclmdgkwzra][0-9a-f]{8}$")

// Lines 98-101 - Update buttonPatterns
private val buttonPatterns = setOf(
    // Existing
    "button", "appcompatbutton", "materialbutton", "imagebutton",
    "floatingactionbutton", "fab", "extendedFloatingActionButton",
    // Compose M3 additions
    "iconbutton", "icontogglebutton", "filledbutton", "filledtonalbutton",
    "outlinedbutton", "elevatedbutton", "textbutton",
    "smallfloatingactionbutton", "largefloatingactionbutton", "segmentedbutton"
)

// Lines 103-106 - Update inputPatterns
private val inputPatterns = setOf(
    // Existing
    "edittext", "textinputedittext", "autocompletetextview", "searchview",
    "textfield", "textinputlayout", "searchbar",
    // Compose M3 additions
    "outlinedtextfield", "basictextfield", "decoratedtextfield",
    "outlinedsearchbar", "dockedsearchbar"
)

// Lines 119-121 - Update cardPatterns
private val cardPatterns = setOf(
    // Existing
    "cardview", "materialcardview", "card",
    // M3 additions
    "elevatedcard", "outlinedcard", "filledcard"
)

// Lines 129-132 - Update menuPatterns
private val menuPatterns = setOf(
    // Existing
    "menu", "popupmenu", "contextmenu", "dropdownmenu", "navigationmenu",
    "overflowmenu", "optionsmenu", "spinner",
    // Compose M3 additions
    "exposeddropdownmenu", "exposeddropdownmenubox", "dropdownmenuitem"
)

// NEW after line 142 - Add chipPatterns
private val chipPatterns = setOf(
    "chip", "filterchip", "inputchip", "assistchip", "suggestionchip",
    "elevatedfilterchip", "elevatedassistchip", "elevatedsuggestionchip"
)

// NEW - Add navigationPatterns
private val navigationPatterns = setOf(
    "navigationbar", "navigationrail", "navigationdrawer",
    "navigationbaritem", "navigationrailitem", "navigationdraweritem",
    "bottomnavigation", "bottomnavigationview"
)

// NEW - Add tabPatterns
private val tabPatterns = setOf(
    "tab", "tabrow", "scrollabletabrow", "primarytabrow", "secondarytabrow",
    "tablayout", "tabitem", "leadingicontab", "texttab"
)

// NEW - Add appBarPatterns
private val appBarPatterns = setOf(
    "topappbar", "centeralignedtopappbar", "mediumtopappbar", "largetopappbar",
    "bottomappbar", "toolbar", "actionbar", "collapsingtoolbarlayout"
)

// NEW - Add checkboxPatterns
private val checkboxPatterns = setOf(
    "checkbox", "appcompatcheckbox", "materialcheckbox",
    "tristatecheckbox", "checkboxitem"
)

// NEW - Add switchPatterns
private val switchPatterns = setOf(
    "switch", "switchcompat", "switchmaterial",
    "togglebutton", "togglegroup"
)

// NEW - Add sliderPatterns
private val sliderPatterns = setOf(
    "slider", "seekbar", "rangeslider", "discreteslider"
)

// Lines 210-227 - Update getTypeCode function
fun getTypeCode(className: String): VUIDTypeCode {
    val normalizedName = className.trim().lowercase()

    if (normalizedName.isEmpty()) return VUIDTypeCode.ELEMENT

    return when {
        buttonPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.BUTTON
        inputPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.INPUT
        scrollPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.SCROLL
        textPatterns.any { normalizedName == it || normalizedName.endsWith(it) } -> VUIDTypeCode.TEXT
        cardPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.CARD
        layoutPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.LAYOUT
        menuPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.MENU
        dialogPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.DIALOG
        imagePatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.IMAGE
        // NEW pattern checks
        chipPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.BUTTON
        navigationPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.MENU
        tabPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.TAB
        appBarPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.LAYOUT
        checkboxPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.CHECKBOX
        switchPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.SWITCH
        sliderPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.SLIDER
        else -> VUIDTypeCode.ELEMENT
    }
}
```

**Acceptance:** All M3/Compose patterns recognized, regex validates all type codes

---

## Phase 3: Semantics Extraction (P2 - Medium)

**Priority:** P2 - After Phase 2
**Files:** 3 (2 modified, 1 existing modification)

### Task 3.1: Extend ElementInfo

**File:** `common/ElementInfo.kt:51-129`
**Change Type:** Add fields with defaults (non-breaking)

```kotlin
data class ElementInfo(
    val className: String,
    val resourceId: String = "",
    val text: String = "",
    val contentDescription: String = "",
    val bounds: Bounds = Bounds.EMPTY,
    val isClickable: Boolean = false,
    val isLongClickable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEnabled: Boolean = true,
    val packageName: String = "",
    // NEW Compose semantics fields (with defaults for compatibility)
    val role: String = "",              // Compose semantic role (Button, Checkbox, etc.)
    val stateDescription: String = "",  // Toggle state, progress value, etc.
    val roleDescription: String = "",   // Custom role description
    val hintText: String = "",          // Input hint/placeholder
    val error: String = "",             // Validation error state
    val metadata: Map<String, String> = emptyMap()  // Additional key-value data
) {
    // Existing computed properties...

    /**
     * Check if element has Compose semantics information
     */
    val hasComposeSemantics: Boolean
        get() = role.isNotBlank() || stateDescription.isNotBlank()
}
```

**Acceptance:** New fields added, all existing code compiles unchanged

---

### Task 3.2: Extract Semantics in AccessibilityService

**File:** `android/apps/voiceoscoreng/service/VoiceOSAccessibilityService.kt`
**Location:** extractElements function (~line 404)

```kotlin
// In extractElements function, update ElementInfo creation:
val element = ElementInfo(
    className = node.className?.toString() ?: "",
    resourceId = node.viewIdResourceName ?: "",
    text = node.text?.toString() ?: "",
    contentDescription = node.contentDescription?.toString() ?: "",
    bounds = Bounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
    isClickable = node.isClickable,
    isLongClickable = node.isLongClickable,
    isScrollable = node.isScrollable,
    isEnabled = node.isEnabled,
    packageName = node.packageName?.toString() ?: "",
    // NEW: Extract Compose semantics (API 30+)
    role = extractRole(node),
    stateDescription = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        node.stateDescription?.toString() ?: ""
    } else "",
    roleDescription = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        node.roleDescription?.toString() ?: ""
    } else "",
    hintText = node.hintText?.toString() ?: "",
    error = node.error?.toString() ?: ""
)

// Add helper function:
private fun extractRole(node: AccessibilityNodeInfo): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // roleDescription provides role info in Compose apps
        node.roleDescription?.toString() ?: ""
    } else {
        ""
    }
}
```

**Acceptance:** Compose semantics extracted on API 30+, backwards compatible

---

### Task 3.3: Use Semantics in CommandGenerator (Optional Enhancement)

**File:** `common/CommandGenerator.kt` (if exists)
**Change:** Use element.role for better action type derivation

```kotlin
private fun deriveActionType(element: ElementInfo): CommandActionType {
    // Check Compose semantic role first
    if (element.role.isNotBlank()) {
        return when (element.role.lowercase()) {
            "button" -> CommandActionType.CLICK
            "checkbox", "switch", "radiobutton" -> CommandActionType.CLICK
            "slider" -> CommandActionType.CLICK
            "textfield" -> CommandActionType.TYPE
            else -> CommandActionType.CLICK
        }
    }
    // Existing fallback logic...
}
```

**Acceptance:** Role-based action derivation works when available

---

## Phase 4: Testing (P3 - Low)

**Priority:** P3 - After all implementation
**Files:** 3 (all new)

### Task 4.1: ComposeHandler Unit Tests

**File:** `commonTest/handlers/ComposeHandlerTest.kt` (NEW)

```kotlin
class ComposeHandlerTest {
    private val handler = ComposeHandler()

    @Test
    fun `canHandle returns true for AndroidComposeView`() {
        val elements = listOf(
            ElementInfo(className = "androidx.compose.ui.platform.AndroidComposeView")
        )
        assertTrue(handler.canHandle(elements))
    }

    @Test
    fun `canHandle returns false for native only`() {
        val elements = listOf(
            ElementInfo(className = "android.widget.Button")
        )
        assertFalse(handler.canHandle(elements))
    }

    @Test
    fun `getComposeTypeCode returns BUTTON for button role`() {
        val element = ElementInfo(className = "android.view.View")
        assertEquals(VUIDTypeCode.BUTTON, handler.getComposeTypeCode(element, "Button"))
    }

    @Test
    fun `getComposeTypeCode returns INPUT for TextField`() {
        val element = ElementInfo(className = "TextField")
        assertEquals(VUIDTypeCode.INPUT, handler.getComposeTypeCode(element))
    }

    @Test
    fun `priority is 90`() {
        assertEquals(90, handler.getPriority())
    }
}
```

---

### Task 4.2: VUIDGenerator Pattern Tests

**File:** `commonTest/common/VUIDGeneratorTest.kt` (extend existing or create)

```kotlin
// Add tests for new patterns
@Test
fun `getTypeCode returns BUTTON for IconButton`() {
    assertEquals(VUIDTypeCode.BUTTON, VUIDGenerator.getTypeCode("IconButton"))
}

@Test
fun `getTypeCode returns INPUT for OutlinedTextField`() {
    assertEquals(VUIDTypeCode.INPUT, VUIDGenerator.getTypeCode("OutlinedTextField"))
}

@Test
fun `getTypeCode returns BUTTON for FilterChip`() {
    assertEquals(VUIDTypeCode.BUTTON, VUIDGenerator.getTypeCode("FilterChip"))
}

@Test
fun `getTypeCode returns TAB for TabRow`() {
    assertEquals(VUIDTypeCode.TAB, VUIDGenerator.getTypeCode("TabRow"))
}

@Test
fun `getTypeCode returns MENU for NavigationBar`() {
    assertEquals(VUIDTypeCode.MENU, VUIDGenerator.getTypeCode("NavigationBar"))
}
```

---

### Task 4.3: Integration Tests

**File:** `commonTest/integration/ComposeIntegrationTest.kt` (NEW)

```kotlin
class ComposeIntegrationTest {
    @Test
    fun `framework detection picks ComposeHandler over NativeHandler`() {
        FrameworkHandlerRegistry.registerDefaults()
        val elements = listOf(
            ElementInfo(className = "androidx.compose.ui.platform.AndroidComposeView"),
            ElementInfo(className = "android.view.View", text = "Submit")
        )
        val handler = FrameworkHandlerRegistry.findHandler(elements)
        assertEquals(FrameworkType.COMPOSE, handler?.frameworkType)
    }

    @Test
    fun `VUID generation uses correct type code for M3 Button`() {
        val typeCode = VUIDGenerator.getTypeCode("FilledTonalButton")
        assertEquals(VUIDTypeCode.BUTTON, typeCode)
    }
}
```

---

## Implementation Order (Optimized)

```
Phase 1 (Sequential - dependencies)
├── 1.1 FrameworkInfo.kt: Add COMPOSE enum + detector
├── 1.2 ComposeHandler.kt: Create new file
├── 1.3 FrameworkHandler.kt: Register handler
└── 1.4 NativeHandler.kt: Add selectors

Phase 2 (Single file - batch all changes)
└── 2.1-2.9 VUIDGenerator.kt: All pattern updates in ONE edit

Phase 3 (Sequential - dependencies)
├── 3.1 ElementInfo.kt: Add semantic fields
├── 3.2 VoiceOSAccessibilityService.kt: Extract semantics
└── 3.3 CommandGenerator.kt: Use semantics (optional)

Phase 4 (Parallel - independent)
├── 4.1 ComposeHandlerTest.kt
├── 4.2 VUIDGeneratorTest.kt
└── 4.3 ComposeIntegrationTest.kt
```

---

## File Touch Summary (Optimized)

| File | Touches | Changes |
|------|---------|---------|
| `FrameworkInfo.kt` | 1 | Add enum + detector |
| `ComposeHandler.kt` | 1 | Create new |
| `FrameworkHandler.kt` | 1 | Add registration |
| `NativeHandler.kt` | 1 | Add patterns |
| `VUIDGenerator.kt` | 1 | Add ALL patterns (batch) |
| `ElementInfo.kt` | 1 | Add fields |
| `VoiceOSAccessibilityService.kt` | 1 | Extract semantics |
| Tests (3 files) | 3 | Create new |
| **Total** | **10** | |

---

## Success Metrics

| Metric | Before | After | Target |
|--------|--------|-------|--------|
| Compose App Detection | 0% | 100% | 100% |
| VUID Type Accuracy | ~55% | 95%+ | 95% |
| M3 Component Recognition | ~20% | 95%+ | 95% |
| Test Coverage | 0 | 15+ | 15+ |

---

## Swarm Agent Assignment

| Agent | Focus | Files |
|-------|-------|-------|
| Agent 1 | Framework Detection (P0) | FrameworkInfo, ComposeHandler, FrameworkHandler, NativeHandler |
| Agent 2 | Pattern Updates (P1) | VUIDGenerator |
| Agent 3 | Semantics (P2) | ElementInfo, AccessibilityService |
| Agent 4 | Testing (P3) | All test files |

---

## Rollback Plan

1. Remove ComposeHandler registration from FrameworkHandler.kt
2. Compose apps fall back to NativeHandler (existing behavior)
3. No data loss, no breaking changes

---

**Document Status:** Ready for Implementation
**Created:** 2026-01-08
**Method:** CoT + Swarm + Auto
**Author:** Claude Code
