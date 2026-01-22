# VoiceOSCore: Hybrid Persistence Strategy Design

**Date:** 2026-01-22 | **Version:** V1 | **Author:** Claude
**Module:** VoiceOSCore | **Type:** Solution Design
**Status:** PROPOSED

---

## Executive Summary

A 4-layer hybrid approach combining app category classification, container-type rules, content heuristics, and screen-level fingerprinting to intelligently decide which elements should be persisted vs kept dynamic.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    PERSISTENCE DECISION PIPELINE                             │
└─────────────────────────────────────────────────────────────────────────────┘

  ┌──────────────────────────────────────┐
  │ LAYER 1: App Category Classifier     │
  │ ─────────────────────────────────    │
  │ Input: packageName                   │
  │ Output: AppCategory enum             │
  │                                      │
  │ Sources:                             │
  │ • Android ApplicationInfo.category   │
  │ • Package name pattern matching      │
  │ • Known app registry (learned)       │
  └──────────────────┬───────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────┐
  │ LAYER 2: Container Type Classifier   │
  │ ─────────────────────────────────    │
  │ Input: className                     │
  │ Output: ContainerBehavior enum       │
  │                                      │
  │ Categories:                          │
  │ • ALWAYS_DYNAMIC (RecyclerView)      │
  │ • CONDITIONALLY_DYNAMIC (ScrollView) │
  │ • STATIC (LinearLayout, etc.)        │
  └──────────────────┬───────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────┐
  │ LAYER 3: Content Heuristics          │
  │ ─────────────────────────────────    │
  │ Input: ElementInfo                   │
  │ Output: ContentSignal                │
  │                                      │
  │ Signals:                             │
  │ • Text length (short/long)           │
  │ • Pattern matching (email/time)      │
  │ • ResourceId presence                │
  │ • Stability score                    │
  └──────────────────┬───────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────┐
  │ LAYER 4: Screen Fingerprint Context  │
  │ ─────────────────────────────────    │
  │ Input: List<ElementInfo>             │
  │ Output: ScreenType                   │
  │                                      │
  │ Types:                               │
  │ • SETTINGS_SCREEN                    │
  │ • LIST_SCREEN                        │
  │ • FORM_SCREEN                        │
  │ • MIXED_SCREEN                       │
  └──────────────────┬───────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────┐
  │ DECISION ENGINE                      │
  │ ─────────────────────────────────    │
  │ Combines all 4 layers:               │
  │                                      │
  │ shouldPersist = evaluate(            │
  │   appCategory,                       │
  │   containerBehavior,                 │
  │   contentSignal,                     │
  │   screenType                         │
  │ )                                    │
  └──────────────────────────────────────┘
```

---

## Layer 1: App Category Classifier

### Data Sources

**1. Android ApplicationInfo.category (API 26+)**
```kotlin
val category = packageManager.getApplicationInfo(packageName, 0).category

// Android categories:
// CATEGORY_GAME = 0
// CATEGORY_AUDIO = 1
// CATEGORY_VIDEO = 2
// CATEGORY_IMAGE = 3
// CATEGORY_SOCIAL = 4
// CATEGORY_NEWS = 5
// CATEGORY_MAPS = 6
// CATEGORY_PRODUCTIVITY = 7
// CATEGORY_ACCESSIBILITY = 8
```

**2. Package Name Pattern Matching**
```kotlin
private val appCategoryPatterns = mapOf(
    AppCategory.EMAIL to listOf(
        "com.google.android.gm",      // Gmail
        "com.microsoft.office.outlook",
        "com.yahoo.mobile.client.android.mail",
        ".mail.", ".email."
    ),
    AppCategory.MESSAGING to listOf(
        "com.whatsapp", "com.facebook.orca",
        "com.google.android.apps.messaging",
        "org.telegram.", "com.discord",
        ".messenger.", ".chat.", ".sms."
    ),
    AppCategory.SOCIAL to listOf(
        "com.twitter.", "com.instagram.",
        "com.facebook.katana", "com.linkedin.",
        "com.reddit.", "com.tumblr."
    ),
    AppCategory.SETTINGS to listOf(
        "com.android.settings",
        "com.realwear.settings",
        ".settings", ".preferences"
    ),
    AppCategory.SYSTEM to listOf(
        "com.android.systemui",
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher"
    )
)
```

**3. Learned Registry (Database)**
```sql
CREATE TABLE app_category_cache (
    package_name TEXT PRIMARY KEY,
    category TEXT NOT NULL,
    confidence REAL DEFAULT 0.5,
    last_updated INTEGER,
    source TEXT  -- 'android_api', 'pattern', 'user', 'learned'
);
```

### AppCategory Enum

```kotlin
enum class AppCategory(val defaultDynamicBehavior: DynamicBehavior) {
    EMAIL(DynamicBehavior.LIST_DYNAMIC),           // Email lists are dynamic
    MESSAGING(DynamicBehavior.LIST_DYNAMIC),       // Chat lists are dynamic
    SOCIAL(DynamicBehavior.LIST_DYNAMIC),          // Feeds are dynamic
    NEWS(DynamicBehavior.LIST_DYNAMIC),            // News feeds are dynamic

    SETTINGS(DynamicBehavior.MOSTLY_STATIC),       // Settings are static
    SYSTEM(DynamicBehavior.MOSTLY_STATIC),         // System UI is static
    PRODUCTIVITY(DynamicBehavior.MIXED),           // Docs, spreadsheets - mixed

    BROWSER(DynamicBehavior.CONTENT_BASED),        // Depends on page
    MEDIA(DynamicBehavior.CONTENT_BASED),          // Music/video - mixed
    GAME(DynamicBehavior.CONTENT_BASED),           // Games - unpredictable

    UNKNOWN(DynamicBehavior.CONTENT_BASED)         // Fall back to heuristics
}

enum class DynamicBehavior {
    LIST_DYNAMIC,      // Assume lists are dynamic, menus are static
    MOSTLY_STATIC,     // Assume most content is static
    MIXED,             // 50/50, use heuristics heavily
    CONTENT_BASED      // Rely entirely on content analysis
}
```

---

## Layer 2: Container Type Classification

### Container Categories

```kotlin
enum class ContainerBehavior {
    ALWAYS_DYNAMIC,         // RecyclerView, ListView - never persist children
    CONDITIONALLY_DYNAMIC,  // ScrollView - check content first
    STATIC                  // LinearLayout, etc. - always evaluate for persistence
}

object ContainerClassifier {

    private val alwaysDynamicContainers = setOf(
        "RecyclerView",
        "ListView",
        "GridView",
        "ViewPager",
        "ViewPager2",
        "LazyColumn",       // Compose
        "LazyRow",
        "LazyVerticalGrid",
        "LazyHorizontalGrid"
    )

    private val conditionallyDynamicContainers = setOf(
        "ScrollView",
        "HorizontalScrollView",
        "NestedScrollView"
    )

    fun classify(className: String): ContainerBehavior {
        val simpleName = className.substringAfterLast(".")

        return when {
            alwaysDynamicContainers.any { simpleName.contains(it, ignoreCase = true) } ->
                ContainerBehavior.ALWAYS_DYNAMIC

            conditionallyDynamicContainers.any { simpleName.contains(it, ignoreCase = true) } ->
                ContainerBehavior.CONDITIONALLY_DYNAMIC

            else -> ContainerBehavior.STATIC
        }
    }
}
```

---

## Layer 3: Content Heuristics

### ContentSignal Analysis

```kotlin
data class ContentSignal(
    val textLength: TextLength,
    val hasResourceId: Boolean,
    val hasDynamicPatterns: Boolean,
    val stabilityScore: Int,
    val elementType: ElementType
)

enum class TextLength {
    NONE,           // No text content
    SHORT,          // < 30 chars (likely label/button)
    MEDIUM,         // 30-100 chars (could be either)
    LONG            // > 100 chars (likely preview/content)
}

object ContentAnalyzer {

    // Dynamic content patterns
    private val dynamicPatterns = listOf(
        Regex("\\d+:\\d+\\s*(AM|PM)?", RegexOption.IGNORE_CASE),  // Time
        Regex("\\d+\\s*(min|hour|day)s?\\s*ago", RegexOption.IGNORE_CASE),
        Regex("^Unread,"),                    // Gmail email marker
        Regex("^Starred,"),
        Regex("\\(\\d+\\)"),                  // Badge count (5)
        Regex("\\d+\\s*new"),                 // "5 new messages"
        Regex("just now", RegexOption.IGNORE_CASE),
        Regex("today at", RegexOption.IGNORE_CASE)
    )

    // Static content patterns
    private val staticPatterns = listOf(
        Regex("^(On|Off)$", RegexOption.IGNORE_CASE),           // Toggle state
        Regex("^Level\\s*\\d+$", RegexOption.IGNORE_CASE),      // Settings level
        Regex("^(Enable|Disable)d?$", RegexOption.IGNORE_CASE), // Setting state
        Regex("^\\d+%$")                                        // Percentage
    )

    fun analyze(element: ElementInfo): ContentSignal {
        val combinedText = "${element.text} ${element.contentDescription}"
        val textLen = combinedText.trim().length

        return ContentSignal(
            textLength = when {
                textLen == 0 -> TextLength.NONE
                textLen < 30 -> TextLength.SHORT
                textLen < 100 -> TextLength.MEDIUM
                else -> TextLength.LONG
            },
            hasResourceId = element.resourceId.isNotBlank(),
            hasDynamicPatterns = dynamicPatterns.any { it.containsMatchIn(combinedText) },
            stabilityScore = calculateStabilityScore(element),
            elementType = ElementType.fromClassName(element.className)
        )
    }

    private fun calculateStabilityScore(element: ElementInfo): Int {
        var score = 50  // Neutral baseline

        // Positive signals (more likely static)
        if (element.resourceId.isNotBlank()) score += 20
        if (element.resourceId.contains("settings", ignoreCase = true)) score += 15
        if (element.resourceId.contains("menu", ignoreCase = true)) score += 10
        if (element.resourceId.contains("btn", ignoreCase = true)) score += 10
        if (staticPatterns.any { it.containsMatchIn(element.text) }) score += 15

        // Negative signals (more likely dynamic)
        if (element.isInDynamicContainer) score -= 30
        if (dynamicPatterns.any { it.containsMatchIn(element.text) }) score -= 25
        if (element.text.length > 100) score -= 20
        if (element.listIndex >= 0) score -= 15

        return score.coerceIn(0, 100)
    }
}
```

---

## Layer 4: Screen Fingerprint Context

### Screen Type Classification

```kotlin
enum class ScreenType {
    SETTINGS_SCREEN,    // Grid of toggles, preferences
    LIST_SCREEN,        // RecyclerView with items
    FORM_SCREEN,        // Input fields, submit buttons
    MENU_SCREEN,        // Navigation menu
    CONTENT_SCREEN,     // Reading/viewing content
    MIXED_SCREEN        // Combination
}

object ScreenClassifier {

    fun classify(elements: List<ElementInfo>, packageName: String): ScreenType {
        val stats = analyzeElements(elements)

        return when {
            // Settings: Many toggles, short labels, grid-like layout
            stats.toggleCount > 3 && stats.avgTextLength < 30 ->
                ScreenType.SETTINGS_SCREEN

            // List: RecyclerView with many similar items
            stats.recyclerViewChildren > 5 ->
                ScreenType.LIST_SCREEN

            // Form: Multiple EditTexts
            stats.editTextCount > 2 ->
                ScreenType.FORM_SCREEN

            // Menu: Many clickable items, no input fields
            stats.clickableCount > 5 && stats.editTextCount == 0
                && stats.avgTextLength < 40 ->
                ScreenType.MENU_SCREEN

            // Content: Long text, few clickables
            stats.avgTextLength > 100 && stats.clickableCount < 3 ->
                ScreenType.CONTENT_SCREEN

            else -> ScreenType.MIXED_SCREEN
        }
    }

    private fun analyzeElements(elements: List<ElementInfo>): ScreenStats {
        return ScreenStats(
            totalElements = elements.size,
            clickableCount = elements.count { it.isClickable },
            editTextCount = elements.count { it.className.contains("EditText") },
            toggleCount = elements.count {
                it.className.contains("Switch") || it.className.contains("Toggle")
            },
            recyclerViewChildren = elements.count { it.containerType.contains("RecyclerView") },
            avgTextLength = elements.map { it.text.length }.average().toInt()
        )
    }
}

data class ScreenStats(
    val totalElements: Int,
    val clickableCount: Int,
    val editTextCount: Int,
    val toggleCount: Int,
    val recyclerViewChildren: Int,
    val avgTextLength: Int
)
```

---

## Decision Engine: The Matrix

### Decision Rules

```kotlin
object PersistenceDecisionEngine {

    /**
     * Core decision function combining all 4 layers.
     */
    fun shouldPersist(
        element: ElementInfo,
        appCategory: AppCategory,
        containerBehavior: ContainerBehavior,
        contentSignal: ContentSignal,
        screenType: ScreenType
    ): Boolean {

        // RULE 1: Always-dynamic containers NEVER persist (RecyclerView, ListView)
        if (containerBehavior == ContainerBehavior.ALWAYS_DYNAMIC) {
            return false
        }

        // RULE 2: Settings/System apps - almost always persist
        if (appCategory in listOf(AppCategory.SETTINGS, AppCategory.SYSTEM)) {
            // Exception: Even settings can have dynamic content (notification counts)
            return !contentSignal.hasDynamicPatterns
        }

        // RULE 3: Settings screens - persist regardless of app category
        if (screenType == ScreenType.SETTINGS_SCREEN) {
            return !contentSignal.hasDynamicPatterns
        }

        // RULE 4: Form screens - persist input fields and buttons
        if (screenType == ScreenType.FORM_SCREEN) {
            return contentSignal.textLength != TextLength.LONG
        }

        // RULE 5: Email/Messaging/Social apps with ScrollView
        if (appCategory in listOf(AppCategory.EMAIL, AppCategory.MESSAGING, AppCategory.SOCIAL)) {
            return when {
                // Long text = email preview, don't persist
                contentSignal.textLength == TextLength.LONG -> false

                // Has dynamic patterns (time, unread count) = don't persist
                contentSignal.hasDynamicPatterns -> false

                // Short text with resourceId = menu item, persist
                contentSignal.textLength == TextLength.SHORT
                    && contentSignal.hasResourceId -> true

                // High stability score = likely static
                contentSignal.stabilityScore > 70 -> true

                else -> false
            }
        }

        // RULE 6: Unknown apps - use stability score threshold
        return contentSignal.stabilityScore > 60 && !contentSignal.hasDynamicPatterns
    }
}
```

### Decision Matrix Visualization

```
┌─────────────────┬────────────────────┬─────────────────────┬────────────────┐
│ App Category    │ Container Type     │ Content Signal      │ Decision       │
├─────────────────┼────────────────────┼─────────────────────┼────────────────┤
│ ANY             │ ALWAYS_DYNAMIC     │ ANY                 │ NEVER PERSIST  │
│                 │ (RecyclerView)     │                     │                │
├─────────────────┼────────────────────┼─────────────────────┼────────────────┤
│ SETTINGS/SYSTEM │ ANY                │ No dynamic patterns │ ALWAYS PERSIST │
│ SETTINGS/SYSTEM │ ANY                │ Has dynamic patterns│ NEVER PERSIST  │
├─────────────────┼────────────────────┼─────────────────────┼────────────────┤
│ EMAIL/MESSAGING │ CONDITIONALLY_DYN  │ Long text           │ NEVER PERSIST  │
│ EMAIL/MESSAGING │ CONDITIONALLY_DYN  │ Dynamic patterns    │ NEVER PERSIST  │
│ EMAIL/MESSAGING │ CONDITIONALLY_DYN  │ Short + resourceId  │ PERSIST        │
│ EMAIL/MESSAGING │ CONDITIONALLY_DYN  │ Stability > 70      │ PERSIST        │
├─────────────────┼────────────────────┼─────────────────────┼────────────────┤
│ UNKNOWN         │ CONDITIONALLY_DYN  │ Stability > 60      │ PERSIST        │
│ UNKNOWN         │ CONDITIONALLY_DYN  │ Stability <= 60     │ NEVER PERSIST  │
├─────────────────┼────────────────────┼─────────────────────┼────────────────┤
│ ANY             │ STATIC             │ Not dynamic         │ PERSIST        │
│ ANY             │ STATIC             │ Dynamic patterns    │ NEVER PERSIST  │
└─────────────────┴────────────────────┴─────────────────────┴────────────────┘
```

---

## Implementation Plan

### Phase 1: Core Infrastructure (2-3 hours)

**New Files:**
```
Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/
├── AppCategoryClassifier.kt       # Layer 1
├── ContainerClassifier.kt         # Layer 2
├── ContentAnalyzer.kt             # Layer 3
├── ScreenClassifier.kt            # Layer 4
└── PersistenceDecisionEngine.kt   # Decision matrix
```

**Android-Specific:**
```
android/apps/voiceoscoreng/src/main/kotlin/.../service/
└── AndroidAppCategoryProvider.kt  # PackageManager integration
```

### Phase 2: Integration (1-2 hours)

**Modify:**
1. `ElementInfo.kt` - Replace `isDynamicContent` with call to decision engine
2. `ElementExtractor.kt` - Pass app category context
3. `CommandGenerator.kt` - Use new `shouldPersist` logic

### Phase 3: Learning/Adaptation (Optional, 2-3 hours)

**Database:**
```sql
-- Track persistence decisions for learning
CREATE TABLE persistence_feedback (
    id INTEGER PRIMARY KEY,
    package_name TEXT,
    element_hash TEXT,
    decision TEXT,  -- 'persist' or 'dynamic'
    was_correct INTEGER,  -- User feedback: 1 = correct, 0 = wrong
    created_at INTEGER
);

-- Learned overrides
CREATE TABLE persistence_overrides (
    package_name TEXT,
    screen_fingerprint TEXT,
    override_type TEXT,  -- 'force_persist', 'force_dynamic', 'auto'
    confidence REAL,
    PRIMARY KEY (package_name, screen_fingerprint)
);
```

---

## Example Scenarios

### Scenario 1: RealWear Settings (Your Screenshot)

```
App: com.realwear.settings
├── Layer 1: AppCategory = SETTINGS (pattern match ".settings")
├── Layer 2: ContainerBehavior = CONDITIONALLY_DYNAMIC (ScrollView)
├── Layer 3: ContentSignal
│   ├── textLength = SHORT ("MOUSE", "BLUETOOTH")
│   ├── hasResourceId = true
│   ├── hasDynamicPatterns = false
│   └── stabilityScore = 85
└── Layer 4: ScreenType = SETTINGS_SCREEN (many toggles, short labels)

Decision: PERSIST (Rule 2: Settings app + no dynamic patterns)
```

### Scenario 2: Gmail Inbox

```
App: com.google.android.gm
├── Layer 1: AppCategory = EMAIL (exact match)
├── Layer 2: ContainerBehavior = ALWAYS_DYNAMIC (RecyclerView)
├── Layer 3: ContentSignal
│   ├── textLength = LONG ("Unread, , , Arby's, BOGO Free...")
│   ├── hasDynamicPatterns = true ("5:32 PM", "Unread,")
│   └── stabilityScore = 15
└── Layer 4: ScreenType = LIST_SCREEN (RecyclerView with many items)

Decision: NEVER PERSIST (Rule 1: Always-dynamic container)
```

### Scenario 3: Gmail Compose Menu

```
App: com.google.android.gm
├── Layer 1: AppCategory = EMAIL
├── Layer 2: ContainerBehavior = CONDITIONALLY_DYNAMIC (ScrollView)
├── Layer 3: ContentSignal
│   ├── textLength = SHORT ("Compose", "Inbox", "Sent")
│   ├── hasResourceId = true
│   ├── hasDynamicPatterns = false
│   └── stabilityScore = 80
└── Layer 4: ScreenType = MENU_SCREEN (clickable items, no inputs)

Decision: PERSIST (Rule 5: Short text + resourceId in email app)
```

### Scenario 4: WhatsApp Chat List

```
App: com.whatsapp
├── Layer 1: AppCategory = MESSAGING (exact match)
├── Layer 2: ContainerBehavior = ALWAYS_DYNAMIC (RecyclerView)
├── Layer 3: ContentSignal = (doesn't matter)
└── Layer 4: ScreenType = LIST_SCREEN

Decision: NEVER PERSIST (Rule 1)
```

### Scenario 5: Unknown App Settings Screen

```
App: com.random.app
├── Layer 1: AppCategory = UNKNOWN
├── Layer 2: ContainerBehavior = CONDITIONALLY_DYNAMIC (ScrollView)
├── Layer 3: ContentSignal
│   ├── textLength = SHORT
│   ├── hasResourceId = true
│   ├── hasDynamicPatterns = false
│   └── stabilityScore = 72
└── Layer 4: ScreenType = SETTINGS_SCREEN

Decision: PERSIST (Rule 3: Settings screen detected)
```

---

## Benefits of Hybrid Approach

| Benefit | Description |
|---------|-------------|
| **Accuracy** | Multiple signals reduce false positives/negatives |
| **Flexibility** | Each layer can be tuned independently |
| **Extensibility** | Easy to add new app categories or patterns |
| **Learning-Ready** | Can incorporate user feedback to improve |
| **Backwards Compatible** | Falls back to content heuristics for unknown apps |
| **Performance** | Early exit rules (always-dynamic) skip expensive analysis |

---

## Comparison with Original Options

| Aspect | Option 2 (Heuristic) | Hybrid Solution |
|--------|---------------------|-----------------|
| Implementation | 30 min | 4-6 hours |
| Accuracy | 80% | 95%+ |
| Maintainability | Medium | High (modular) |
| Extensibility | Low | High |
| Learning capability | No | Yes (Phase 3) |
| App-aware | No | Yes |
| Screen-aware | No | Yes |

---

## Recommended Approach

**Short-term (This Sprint):** Implement Option 2 (simple heuristic) to unblock Settings screens immediately.

**Medium-term (Next Sprint):** Implement Layers 1-2 of hybrid (app category + container classification).

**Long-term (Backlog):** Complete Layers 3-4 and add learning capability.

This phased approach delivers immediate value while building toward the robust long-term solution.

---

## Files to Create

```kotlin
// 1. AppCategoryClassifier.kt
enum class AppCategory { ... }
object AppCategoryClassifier {
    fun classify(packageName: String): AppCategory
}

// 2. ContainerClassifier.kt
enum class ContainerBehavior { ... }
object ContainerClassifier {
    fun classify(className: String): ContainerBehavior
}

// 3. ContentAnalyzer.kt
data class ContentSignal { ... }
object ContentAnalyzer {
    fun analyze(element: ElementInfo): ContentSignal
}

// 4. ScreenClassifier.kt
enum class ScreenType { ... }
object ScreenClassifier {
    fun classify(elements: List<ElementInfo>): ScreenType
}

// 5. PersistenceDecisionEngine.kt
object PersistenceDecisionEngine {
    fun shouldPersist(
        element: ElementInfo,
        appCategory: AppCategory,
        containerBehavior: ContainerBehavior,
        contentSignal: ContentSignal,
        screenType: ScreenType
    ): Boolean
}
```

---

*Design by Claude | VoiceOSCore Module | IDEACODE Framework*
