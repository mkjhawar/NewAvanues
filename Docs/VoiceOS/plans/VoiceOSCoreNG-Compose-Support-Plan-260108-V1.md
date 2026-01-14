# VoiceOSCoreNG Compose & Jetpack Support Implementation Plan

**Module:** VoiceOSCoreNG
**Date:** 2026-01-08
**Version:** 1.0
**Status:** Ready for Implementation
**Reference:** `VoiceOSCoreNG-Compose-Jetpack-Analysis-260108-V1.md`

---

## Overview

This plan addresses all gaps identified in the Compose/Jetpack analysis to achieve 95%+ VUID accuracy for modern Android UI frameworks.

**Scope:** 17 tasks across 4 phases
**Estimated Files:** 10 files (3 new, 7 modified)

---

## Phase 1: Framework Detection (P0 - Critical)

### Task 1.1: Add COMPOSE to FrameworkType Enum

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/FrameworkType.kt`

**Changes:**
```kotlin
enum class FrameworkType {
    NATIVE,
    FLUTTER,
    REACT_NATIVE,
    WEBVIEW,
    UNITY,
    COMPOSE,  // ADD THIS
    UNKNOWN
}
```

**Acceptance Criteria:**
- [ ] `FrameworkType.COMPOSE` exists
- [ ] No breaking changes to existing code

---

### Task 1.2: Create ComposeHandler

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/handlers/ComposeHandler.kt` (NEW)

**Implementation:**
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
 */
class ComposeHandler : FrameworkHandler {

    override val frameworkType: FrameworkType = FrameworkType.COMPOSE

    // Markers that identify a Compose application
    private val composeMarkers = setOf(
        "androidx.compose.ui.platform.AndroidComposeView",
        "AndroidComposeView",
        "ComposeView",
        "androidx.compose.ui.platform.ComposeView"
    )

    // Compose class name patterns (from accessibility)
    private val composePatterns = setOf(
        "androidx.compose.",
        "compose.material",
        "compose.material3"
    )

    // Semantic role to VUID type code mapping
    private val roleToTypeCode = mapOf(
        "Button" to VUIDTypeCode.BUTTON,
        "Checkbox" to VUIDTypeCode.CHECKBOX,
        "Switch" to VUIDTypeCode.SWITCH,
        "RadioButton" to VUIDTypeCode.CHECKBOX,
        "Tab" to VUIDTypeCode.TAB,
        "Slider" to VUIDTypeCode.SLIDER,
        "Image" to VUIDTypeCode.IMAGE,
        "DropdownList" to VUIDTypeCode.MENU,
        "ProgressIndicator" to VUIDTypeCode.ELEMENT
    )

    override fun canHandle(elements: List<ElementInfo>): Boolean {
        return elements.any { element ->
            composeMarkers.any { marker ->
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
        return composeMarkers.toList() + composePatterns.toList()
    }

    override fun isActionable(element: ElementInfo): Boolean {
        // Compose elements are actionable if clickable or have actions
        return element.isClickable ||
               element.isLongClickable ||
               element.isScrollable ||
               element.hasVoiceContent
    }

    override fun getPriority(): Int = 90 // Between Flutter (100) and WebView (70)

    /**
     * Get the appropriate VUID type code for a Compose element.
     * Uses semantic role if available, falls back to class name analysis.
     */
    fun getComposeTypeCode(element: ElementInfo): VUIDTypeCode {
        // Try semantic role first (if captured)
        val role = element.metadata["role"] ?: ""
        roleToTypeCode[role]?.let { return it }

        // Fall back to class name analysis
        val className = element.className.lowercase()
        return when {
            // Buttons
            className.contains("button") -> VUIDTypeCode.BUTTON
            // Input fields
            className.contains("textfield") -> VUIDTypeCode.INPUT
            className.contains("searchbar") -> VUIDTypeCode.INPUT
            // Toggles
            className.contains("checkbox") -> VUIDTypeCode.CHECKBOX
            className.contains("switch") -> VUIDTypeCode.SWITCH
            // Scrollables
            className.contains("lazycolumn") -> VUIDTypeCode.SCROLL
            className.contains("lazyrow") -> VUIDTypeCode.SCROLL
            className.contains("lazygrid") -> VUIDTypeCode.SCROLL
            // Navigation
            className.contains("navigationbar") -> VUIDTypeCode.MENU
            className.contains("tabrow") -> VUIDTypeCode.TAB
            // Dialogs
            className.contains("dialog") -> VUIDTypeCode.DIALOG
            className.contains("bottomsheet") -> VUIDTypeCode.DIALOG
            // Cards
            className.contains("card") -> VUIDTypeCode.CARD
            // Images
            className.contains("image") -> VUIDTypeCode.IMAGE
            className.contains("icon") -> VUIDTypeCode.IMAGE
            // Layouts
            className.contains("column") -> VUIDTypeCode.LAYOUT
            className.contains("row") -> VUIDTypeCode.LAYOUT
            className.contains("box") -> VUIDTypeCode.LAYOUT
            className.contains("scaffold") -> VUIDTypeCode.LAYOUT
            // Default
            else -> VUIDTypeCode.ELEMENT
        }
    }

    /**
     * Check if element is a relevant Compose element worth processing.
     */
    private fun isRelevantComposeElement(element: ElementInfo): Boolean {
        // Skip the container itself
        if (composeMarkers.any { element.className.contains(it) }) {
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
        return composeMarkers.any { element.className.contains(it, ignoreCase = true) }
    }
}
```

**Acceptance Criteria:**
- [ ] ComposeHandler created
- [ ] canHandle() detects Compose apps
- [ ] getComposeTypeCode() maps to correct VUIDTypeCode
- [ ] Priority is 90

---

### Task 1.3: Register ComposeHandler in Registry

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/handlers/FrameworkHandler.kt`

**Changes (around line 59-70):**
```kotlin
object FrameworkHandlerRegistry {
    private val handlers = mutableListOf<FrameworkHandler>()

    fun registerDefaults() {
        clear()
        register(FlutterHandler())      // Priority 100
        register(ComposeHandler())      // Priority 90  <-- ADD THIS
        register(ReactNativeHandler())  // Priority 80
        register(WebViewHandler())      // Priority 70
        register(UnityHandler())        // Priority 60
        register(NativeHandler())       // Priority 0 (fallback)
    }
    // ...
}
```

**Acceptance Criteria:**
- [ ] ComposeHandler registered
- [ ] Priority order maintained (Flutter > Compose > ReactNative > WebView > Unity > Native)

---

### Task 1.4: Update NativeHandler Selectors

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/handlers/NativeHandler.kt`

**Changes (line 16-22):**
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
    "androidx.compose."               // ADD (for any Compose elements that leak through)
)
```

**Acceptance Criteria:**
- [ ] New AndroidX selectors added
- [ ] Compose pattern added as fallback

---

## Phase 2: VUID Pattern Updates (P1 - High)

### Task 2.1: Add Compose Button Patterns

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/VUIDGenerator.kt`

**Changes (line 98-101):**
```kotlin
private val buttonPatterns = setOf(
    // Existing
    "button", "appcompatbutton", "materialbutton", "imagebutton",
    "floatingactionbutton", "fab", "extendedFloatingActionButton",
    // Compose M3 additions
    "iconbutton", "icontogglebutton", "filledbutton", "filledtonalbutton",
    "outlinedbutton", "elevatedbutton", "textbutton",
    "extendedFloatingActionButton", "smallfloatingactionbutton",
    "largefloatingactionbutton", "segmentedbutton"
)
```

**Acceptance Criteria:**
- [ ] All M3 button variants recognized
- [ ] Tests pass

---

### Task 2.2: Add Compose Input Patterns

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/VUIDGenerator.kt`

**Changes (line 103-106):**
```kotlin
private val inputPatterns = setOf(
    // Existing
    "edittext", "textinputedittext", "autocompletetextview", "searchview",
    "textfield", "textinputlayout", "searchbar",
    // Compose M3 additions
    "outlinedtextfield", "basictextfield", "decoratedtextfield",
    "outlinedsearchbar", "dockedsearchbar"
)
```

**Acceptance Criteria:**
- [ ] Compose TextField variants recognized
- [ ] SearchBar variants recognized

---

### Task 2.3: Add Chip Patterns (NEW)

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/VUIDGenerator.kt`

**Add new pattern set (after line 142):**
```kotlin
private val chipPatterns = setOf(
    "chip", "filterchip", "inputchip", "assistchip", "suggestionchip",
    "elevatedfilterchip", "elevatedassistchip", "elevatedsuggestionchip"
)
```

**Add new VUIDTypeCode (if needed) or map to BUTTON:**
```kotlin
// In getTypeCode() function around line 215
chipPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.BUTTON
```

**Acceptance Criteria:**
- [ ] All chip variants recognized as BUTTON
- [ ] Pattern added to getTypeCode()

---

### Task 2.4: Add Navigation Patterns (NEW)

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/VUIDGenerator.kt`

**Add new pattern set:**
```kotlin
private val navigationPatterns = setOf(
    "navigationbar", "navigationrail", "navigationdrawer",
    "navigationbaritem", "navigationrailitem", "navigationdraweritem",
    "bottomnavigation", "bottomnavigationview"
)
```

**Map to MENU in getTypeCode():**
```kotlin
navigationPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.MENU
```

**Acceptance Criteria:**
- [ ] Navigation components recognized as MENU
- [ ] Pattern added to getTypeCode()

---

### Task 2.5: Add Tab Patterns (NEW)

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/VUIDGenerator.kt`

**Add new pattern set:**
```kotlin
private val tabPatterns = setOf(
    "tab", "tabrow", "scrollabletabrow", "primarytabrow", "secondarytabrow",
    "tablayout", "tabitem", "leadingicontab", "texttab"
)
```

**Map to TAB in getTypeCode():**
```kotlin
tabPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.TAB
```

**Acceptance Criteria:**
- [ ] Tab components recognized
- [ ] Uses TAB type code

---

### Task 2.6: Add AppBar Patterns (NEW)

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/VUIDGenerator.kt`

**Add new pattern set:**
```kotlin
private val appBarPatterns = setOf(
    "topappbar", "centeralignedtopappbar", "mediumtopappbar", "largetopappbar",
    "bottomappbar", "toolbar", "actionbar", "collapsingtoolbarlayout"
)
```

**Map to LAYOUT in getTypeCode():**
```kotlin
appBarPatterns.any { normalizedName.contains(it) } -> VUIDTypeCode.LAYOUT
```

**Acceptance Criteria:**
- [ ] AppBar components recognized
- [ ] Uses LAYOUT type code

---

### Task 2.7: Update Card Patterns

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/VUIDGenerator.kt`

**Changes (line 119-121):**
```kotlin
private val cardPatterns = setOf(
    // Existing
    "cardview", "materialcardview", "card",
    // M3 additions
    "elevatedcard", "outlinedcard", "filledcard"
)
```

**Acceptance Criteria:**
- [ ] M3 card variants recognized

---

### Task 2.8: Update Menu Patterns

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/VUIDGenerator.kt`

**Changes (line 129-132):**
```kotlin
private val menuPatterns = setOf(
    // Existing
    "menu", "popupmenu", "contextmenu", "dropdownmenu", "navigationmenu",
    "overflowmenu", "optionsmenu", "spinner",
    // Compose M3 additions
    "exposeddropdownmenu", "exposeddropdownmenubox", "dropdownmenuitem"
)
```

**Acceptance Criteria:**
- [ ] Compose dropdown variants recognized

---

### Task 2.9: Update VUID Regex Validation

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/VUIDGenerator.kt`

**Changes (line 95):**
```kotlin
// Add any new type codes to regex if VUIDTypeCode enum was extended
private val vuidPattern = Regex("^[0-9a-f]{6}-[bisteclmdgkwzra][0-9a-f]{8}$")
// Note: k=CHECKBOX, w=SWITCH, z=LIST, r=SLIDER, a=TAB already exist
```

**Acceptance Criteria:**
- [ ] Regex matches all valid type codes

---

## Phase 3: Semantics Extraction (P2 - Medium)

### Task 3.1: Extend ElementInfo with Compose Semantics

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/ElementInfo.kt`

**Add new fields:**
```kotlin
data class ElementInfo(
    val className: String,
    val resourceId: String,
    val text: String,
    val contentDescription: String,
    val bounds: Bounds,
    val isClickable: Boolean,
    val isLongClickable: Boolean,
    val isScrollable: Boolean,
    val isEnabled: Boolean,
    val packageName: String,
    // NEW Compose semantics fields
    val role: String = "",              // Compose semantic role (Button, Checkbox, etc.)
    val stateDescription: String = "",  // Toggle state, progress value, etc.
    val roleDescription: String = "",   // Custom role description
    val hintText: String = "",          // Input hint/placeholder
    val error: String = ""              // Validation error state
) {
    // Existing computed properties...

    // NEW: Check if element has Compose semantics
    val hasComposeSemantics: Boolean
        get() = role.isNotBlank() || stateDescription.isNotBlank()
}
```

**Acceptance Criteria:**
- [ ] New fields added with defaults
- [ ] hasComposeSemantics computed property works
- [ ] No breaking changes to existing code

---

### Task 3.2: Extract Compose Semantics in AccessibilityService

**File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/VoiceOSAccessibilityService.kt`

**Changes (in extractElements function, around line 404-415):**
```kotlin
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
    // NEW: Extract Compose semantics
    role = extractRole(node),
    stateDescription = node.stateDescription?.toString() ?: "",
    roleDescription = node.roleDescription?.toString() ?: "",
    hintText = node.hintText?.toString() ?: "",
    error = node.error?.toString() ?: ""
)

// Helper function
private fun extractRole(node: AccessibilityNodeInfo): String {
    // Android API 30+ has roleDescription
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        node.roleDescription?.toString() ?: ""
    } else {
        ""
    }
}
```

**Acceptance Criteria:**
- [ ] Compose semantics extracted
- [ ] Backwards compatible with older Android versions

---

### Task 3.3: Use Semantics in CommandGenerator

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/CommandGenerator.kt`

**Changes (in deriveActionType, around line 102-112):**
```kotlin
private fun deriveActionType(element: ElementInfo): CommandActionType {
    // Check Compose semantic role first
    if (element.role.isNotBlank()) {
        return when (element.role.lowercase()) {
            "button" -> CommandActionType.CLICK
            "checkbox", "switch", "radiobutton" -> CommandActionType.CLICK
            "slider" -> CommandActionType.CLICK  // Could be SCROLL
            "textfield" -> CommandActionType.TYPE
            "image" -> CommandActionType.CLICK
            else -> CommandActionType.CLICK
        }
    }

    // Existing class name fallback
    val className = element.className.lowercase()
    return when {
        className.contains("edittext") || className.contains("textfield") -> CommandActionType.TYPE
        className.contains("checkbox") || className.contains("switch") -> CommandActionType.CLICK
        className.contains("button") -> CommandActionType.CLICK
        element.isScrollable -> CommandActionType.CLICK
        element.isClickable -> CommandActionType.CLICK
        else -> CommandActionType.CLICK
    }
}
```

**Acceptance Criteria:**
- [ ] Compose roles used for action type derivation
- [ ] Fallback to className still works

---

## Phase 4: Testing & Documentation (P3 - Low)

### Task 4.1: Create ComposeHandler Unit Tests

**File:** `Modules/VoiceOSCoreNG/src/commonTest/kotlin/com/augmentalis/voiceoscoreng/handlers/ComposeHandlerTest.kt` (NEW)

**Test Cases:**
```kotlin
class ComposeHandlerTest {
    @Test fun `canHandle returns true for AndroidComposeView`()
    @Test fun `canHandle returns true for ComposeView`()
    @Test fun `canHandle returns false for native only elements`()
    @Test fun `getComposeTypeCode returns BUTTON for Button role`()
    @Test fun `getComposeTypeCode returns INPUT for TextField`()
    @Test fun `getComposeTypeCode returns CHECKBOX for Checkbox`()
    @Test fun `getComposeTypeCode returns SCROLL for LazyColumn`()
    @Test fun `processElements filters layout containers`()
    @Test fun `isActionable returns true for clickable elements`()
    @Test fun `priority is 90`()
}
```

**Acceptance Criteria:**
- [ ] All test cases implemented
- [ ] Tests pass

---

### Task 4.2: Create Compose Integration Tests

**File:** `Modules/VoiceOSCoreNG/src/commonTest/kotlin/com/augmentalis/voiceoscoreng/integration/ComposeIntegrationTest.kt` (NEW)

**Test Cases:**
```kotlin
class ComposeIntegrationTest {
    @Test fun `full pipeline processes Compose Button correctly`()
    @Test fun `full pipeline processes Compose TextField correctly`()
    @Test fun `full pipeline processes LazyColumn items`()
    @Test fun `framework detection picks ComposeHandler over NativeHandler`()
    @Test fun `VUID generation uses correct type codes for M3 components`()
    @Test fun `command matching works for Compose generated commands`()
}
```

**Acceptance Criteria:**
- [ ] End-to-end tests for Compose pipeline
- [ ] Tests pass

---

### Task 4.3: Update FrameworkInfoTest

**File:** `Modules/VoiceOSCoreNG/src/commonTest/kotlin/com/augmentalis/voiceoscoreng/common/FrameworkInfoTest.kt`

**Update existing test (around line 404-407):**
```kotlin
@Test
fun `FrameworkDetector returns COMPOSE for Jetpack Compose app`() {
    val elements = listOf(
        createTestElement(
            className = "androidx.compose.ui.platform.AndroidComposeView"
        ),
        createTestElement(
            className = "android.view.View",
            text = "Submit"
        )
    )

    val handler = FrameworkHandlerRegistry.findHandler(elements)
    assertEquals(FrameworkType.COMPOSE, handler?.frameworkType)
}
```

**Acceptance Criteria:**
- [ ] Compose framework detection tested
- [ ] Tests pass

---

### Task 4.4: Update Documentation

**File:** `Modules/VoiceOSCoreNG/README.md`

**Add section:**
```markdown
## Supported Frameworks

| Framework | Handler | Detection |
|-----------|---------|-----------|
| Native Android | NativeHandler | Default fallback |
| Jetpack Compose | ComposeHandler | AndroidComposeView |
| Flutter | FlutterHandler | io.flutter.*, SemanticsNode |
| React Native | ReactNativeHandler | ReactViewGroup |
| WebView/Hybrid | WebViewHandler | WebView, Cordova |
| Unity | UnityHandler | Unity accessibility |

### Jetpack Compose Support

VoiceOSCoreNG fully supports Jetpack Compose applications including:
- Material3 (M3) components
- Compose semantic roles
- LazyColumn/LazyRow/LazyGrid
- All button variants
- All input variants
- Navigation components
- Dialogs and Bottom Sheets
```

**Acceptance Criteria:**
- [ ] README updated with Compose support info

---

## Task Summary

| Phase | Tasks | Priority | Estimated LOC |
|-------|-------|----------|---------------|
| Phase 1: Framework Detection | 4 | P0 | ~200 |
| Phase 2: VUID Patterns | 9 | P1 | ~80 |
| Phase 3: Semantics Extraction | 3 | P2 | ~60 |
| Phase 4: Testing & Docs | 4 | P3 | ~300 |
| **Total** | **20** | - | **~640** |

---

## Implementation Order

```
1.1 Add COMPOSE to FrameworkType
    ↓
1.2 Create ComposeHandler
    ↓
1.3 Register in FrameworkHandlerRegistry
    ↓
1.4 Update NativeHandler selectors
    ↓
2.1-2.9 Add VUID patterns (can be parallel)
    ↓
3.1 Extend ElementInfo
    ↓
3.2 Extract semantics in Service
    ↓
3.3 Use semantics in CommandGenerator
    ↓
4.1-4.4 Tests and documentation
```

---

## Success Metrics

| Metric | Before | After | Target |
|--------|--------|-------|--------|
| Compose Detection | 0% | 100% | 100% |
| VUID Type Accuracy | ~55% | 95%+ | 95% |
| M3 Component Recognition | ~20% | 95%+ | 95% |
| Test Coverage | 0 tests | 20+ tests | 15+ |

---

## Rollback Plan

If issues arise:
1. Remove ComposeHandler registration from FrameworkHandlerRegistry
2. Compose apps will fall back to NativeHandler (existing behavior)
3. No data loss or breaking changes

---

## Approval

- [ ] Technical Review
- [ ] Code Review Required
- [ ] Ready for Implementation

---

**Document Status:** Ready
**Created:** 2026-01-08
**Author:** Claude Code
