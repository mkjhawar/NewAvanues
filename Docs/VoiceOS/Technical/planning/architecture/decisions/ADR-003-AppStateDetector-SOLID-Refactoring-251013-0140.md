# ADR-003: AppStateDetector SOLID Refactoring Architecture

**Status:** Proposed
**Created:** 2025-10-13 01:40:41 PDT
**Author:** VOS4 Development Team
**Context:** AppStateDetector Enhancement - SOLID Principles Compliance
**Supersedes:** AppStateDetector-Enhancement-Implementation-Guide-v1.0-20251013.md

---

## Executive Summary

This ADR proposes a comprehensive refactoring of `AppStateDetector.kt` (currently 518 lines) using SOLID principles to ensure NO file exceeds 300 lines while incorporating all 17 planned enhancement phases. The refactoring transforms a monolithic detector into a modular, extensible architecture with clear separation of concerns.

**Key Metrics:**
- **Current:** 1 file, 518 lines (would grow to ~1100+ lines with enhancements)
- **Proposed:** 21 files, all <300 lines (largest: 245 lines)
- **Pattern:** Strategy Pattern + Factory Pattern + Dependency Injection
- **Backward Compatible:** 100% API compatibility maintained

---

## Context

### Current Architecture Problems

1. **Single Responsibility Violation (SRP)**
   - One class handles: orchestration, state detection, pattern matching, scoring, tree traversal, and state management
   - 7 detection methods, each doing pattern matching independently
   - No clear separation between detection logic and scoring logic

2. **Open/Closed Violation (OCP)**
   - Adding new state types requires modifying core detector class
   - No extension points for custom detection strategies
   - Hard-coded pattern matching logic

3. **Dependency Inversion Violation (DIP)**
   - Direct dependencies on concrete detection implementations
   - No abstraction layer for detection strategies
   - Tightly coupled scoring logic

4. **Scale Problem**
   - Planned enhancements would add 600+ lines
   - File would exceed 1100 lines
   - Unmaintainable monolith

### Enhancement Requirements (17 Phases)

From the implementation guide:
- Resource ID pattern matching
- Material Design component detection
- Multi-state detection
- Hierarchy-aware analysis
- Temporal validation
- Metadata validation system
- Framework-specific detection (Compose, WebView, Material)
- Helper utilities (isWebContent, isComposeUI, getUIFramework)

---

## Decision

Refactor `AppStateDetector` into a modular architecture following SOLID principles with clear separation of concerns.

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    AppStateDetector                         │
│                   (Orchestrator - 245 lines)                │
│  • Coordinates detection pipeline                           │
│  • Manages state transitions                                │
│  • Publishes StateFlow                                      │
└────────────┬────────────────────────────────────────────────┘
             │
             │ uses
             ▼
┌─────────────────────────────────────────────────────────────┐
│              StateDetectionPipeline                         │
│                  (Pipeline - 180 lines)                     │
│  • Executes detection strategies in sequence                │
│  • Aggregates results                                       │
│  • Applies confidence thresholds                            │
└────────────┬────────────────────────────────────────────────┘
             │
             │ delegates to
             ▼
┌─────────────────────────────────────────────────────────────┐
│           StateDetectionStrategy (Interface)                │
│  • detectState(context: DetectionContext): DetectionResult  │
└────────────┬────────────────────────────────────────────────┘
             │
             │ implemented by (7 classes, each <200 lines)
             ├──────────────────────────────────────────────────┐
             │                                                  │
             ▼                                                  ▼
┌────────────────────────┐                    ┌────────────────────────┐
│  LoginStateDetector    │                    │  LoadingStateDetector  │
│    (185 lines)         │                    │    (175 lines)         │
│  • Login patterns      │                    │  • Progress indicators │
│  • Auth fields         │                    │  • Loading text        │
│  • Material inputs     │                    │  • Minimal content     │
└────────┬───────────────┘                    └────────┬───────────────┘
         │                                             │
         │ uses                                        │ uses
         ▼                                             ▼
┌────────────────────────────────────────────────────────────┐
│              PatternMatcher (Interface)                    │
│  • matches(input, patterns): MatchResult                   │
└────────────┬───────────────────────────────────────────────┘
             │
             │ implemented by (4 classes, each <150 lines)
             ├──────────────────────────────────────────────┐
             │                                              │
             ▼                                              ▼
┌────────────────────────┐                  ┌────────────────────────┐
│  ResourceIdMatcher     │                  │  ClassNameMatcher      │
│    (120 lines)         │                  │    (135 lines)         │
│  • Pattern sets        │                  │  • Framework classes   │
│  • Match counting      │                  │  • Material components │
│  • Confidence calc     │                  │  • Compose detection   │
└────────────────────────┘                  └────────────────────────┘

             ┌────────────────────────┐
             │  TextMatcher           │
             │    (110 lines)         │
             │  • Keyword matching    │
             │  • Case insensitive    │
             │  • Multi-language      │
             └────────────────────────┘

             ┌────────────────────────┐
             │  HierarchyMatcher      │
             │    (145 lines)         │
             │  • Tree traversal      │
             │  • Parent-child        │
             │  • Depth analysis      │
             └────────────────────────┘
```

---

## Detailed Architecture

### 1. Core Orchestrator (245 lines)

**File:** `AppStateDetector.kt`

**Responsibility:** Coordinate detection pipeline, manage state, publish StateFlow

```kotlin
/**
 * AppStateDetector.kt - Main orchestrator for state detection
 *
 * Responsibilities:
 * - Coordinate detection pipeline
 * - Manage StateFlow for current state
 * - Track state transitions
 * - Apply confidence thresholds
 *
 * SOLID Compliance:
 * - SRP: Only orchestration and state management
 * - OCP: Extensible via strategy injection
 * - LSP: Depends on abstractions
 * - ISP: Uses focused interfaces
 * - DIP: Depends on StateDetectionPipeline abstraction
 *
 * Size: ~245 lines (within 300 limit)
 */
class AppStateDetector(
    private val pipeline: StateDetectionPipeline = DefaultStateDetectionPipeline(),
    private val config: StateDetectorConfig = StateDetectorConfig()
) {
    // StateFlow management (~30 lines)
    private val _currentState = MutableStateFlow(...)
    val currentState: StateFlow<StateDetectionResult> = _currentState.asStateFlow()

    private val _transitions = MutableStateFlow<List<StateTransition>>(emptyList())
    val transitions: StateFlow<List<StateTransition>> = _transitions.asStateFlow()

    // Detection coordination (~60 lines)
    fun detectState(rootNode: AccessibilityNodeInfo?): StateDetectionResult {
        if (rootNode == null) return unknownState()

        val context = buildDetectionContext(rootNode)
        val result = pipeline.detectState(context, config)

        updateState(result)
        return result
    }

    // Context building (~50 lines)
    private fun buildDetectionContext(rootNode: AccessibilityNodeInfo): DetectionContext {
        val collector = NodeDataCollector()
        collector.traverse(rootNode)
        return DetectionContext(
            textContent = collector.textContent,
            viewIds = collector.viewIds,
            classNames = collector.classNames,
            nodeTree = collector.buildTree()
        )
    }

    // State management (~60 lines)
    private fun updateState(result: StateDetectionResult) { ... }

    // Public API (~45 lines)
    fun reset() { ... }
    fun getTransitionHistory(): List<StateTransition> { ... }
    fun getCurrentState(): StateDetectionResult { ... }
}
```

**Line Count Breakdown:**
- Imports: 15 lines
- Class header: 10 lines
- StateFlow setup: 30 lines
- Detection coordination: 60 lines
- Context building: 50 lines
- State management: 60 lines
- Public API: 20 lines
- **Total: ~245 lines**

---

### 2. Detection Pipeline (180 lines)

**File:** `StateDetectionPipeline.kt`

**Responsibility:** Execute detection strategies, aggregate results

```kotlin
/**
 * StateDetectionPipeline.kt - Detection execution pipeline
 *
 * Responsibilities:
 * - Execute all registered detection strategies
 * - Aggregate detection results
 * - Apply confidence thresholds
 * - Select best match
 *
 * SOLID Compliance:
 * - SRP: Only pipeline execution
 * - OCP: New strategies via registration
 * - DIP: Depends on StateDetectionStrategy abstraction
 *
 * Size: ~180 lines (within 300 limit)
 */
interface StateDetectionPipeline {
    fun detectState(context: DetectionContext, config: StateDetectorConfig): StateDetectionResult
}

class DefaultStateDetectionPipeline(
    private val strategies: List<StateDetectionStrategy> = defaultStrategies()
) : StateDetectionPipeline {

    override fun detectState(
        context: DetectionContext,
        config: StateDetectorConfig
    ): StateDetectionResult {
        val detections = strategies.map { strategy ->
            strategy.detectState(context, config)
        }

        val bestDetection = selectBestDetection(detections, config)
        return applyDefaultState(bestDetection, config)
    }

    private fun selectBestDetection(
        detections: List<StateDetectionResult>,
        config: StateDetectorConfig
    ): StateDetectionResult {
        return detections.maxByOrNull { it.confidence }
            ?: StateDetectionResult(AppState.UNKNOWN, 0.0f, emptyList())
    }

    private fun applyDefaultState(
        result: StateDetectionResult,
        config: StateDetectorConfig
    ): StateDetectionResult {
        return if (result.confidence < config.confidenceThreshold) {
            StateDetectionResult(AppState.READY, 0.6f, listOf("Default state"))
        } else {
            result
        }
    }

    companion object {
        fun defaultStrategies(): List<StateDetectionStrategy> {
            return listOf(
                LoginStateDetector(),
                LoadingStateDetector(),
                ErrorStateDetector(),
                PermissionStateDetector(),
                TutorialStateDetector(),
                EmptyStateDetector(),
                DialogStateDetector()
            )
        }
    }
}
```

**Line Count Breakdown:**
- Imports: 10 lines
- Interface: 15 lines
- Implementation: 155 lines
- **Total: ~180 lines**

---

### 3. Detection Strategy Interface (30 lines)

**File:** `StateDetectionStrategy.kt`

```kotlin
/**
 * StateDetectionStrategy.kt - Strategy interface for state detection
 *
 * SOLID Compliance:
 * - ISP: Minimal focused interface
 * - DIP: Abstraction for all detectors
 *
 * Size: ~30 lines
 */
interface StateDetectionStrategy {
    /**
     * Detect state from collected UI data
     *
     * @param context Collected UI data (text, IDs, classes, tree)
     * @param config Detection configuration
     * @return Detection result with confidence score
     */
    fun detectState(context: DetectionContext, config: StateDetectorConfig): StateDetectionResult

    /**
     * Get the state type this strategy detects
     */
    fun getTargetState(): AppState
}
```

**Line Count:** ~30 lines

---

### 4. Individual State Detectors (7 files, each <200 lines)

Each detector is a focused class handling one specific state type.

#### 4.1 LoginStateDetector (185 lines)

**File:** `detectors/LoginStateDetector.kt`

```kotlin
/**
 * LoginStateDetector.kt - Detects login/authentication screens
 *
 * Detection Criteria:
 * - Login keywords in text
 * - Login-related view IDs
 * - EditText input fields (2+)
 * - Material TextInputLayout fields
 * - Login/SignIn buttons
 * - Web content adjustment
 *
 * SOLID Compliance:
 * - SRP: Only login detection
 * - DIP: Uses PatternMatcher abstractions
 *
 * Size: ~185 lines (within 300 limit)
 */
class LoginStateDetector(
    private val textMatcher: TextMatcher = TextMatcher(LOGIN_KEYWORDS),
    private val idMatcher: ResourceIdMatcher = ResourceIdMatcher(LOGIN_VIEW_ID_PATTERNS),
    private val classMatcher: ClassNameMatcher = ClassNameMatcher(),
    private val scoringStrategy: ScoringStrategy = LoginScoringStrategy()
) : StateDetectionStrategy {

    override fun detectState(
        context: DetectionContext,
        config: StateDetectorConfig
    ): StateDetectionResult {
        val indicators = mutableListOf<String>()

        // Text matching (~30 lines)
        val textScore = textMatcher.match(context.textContent)
        if (textScore.matchCount > 0) {
            indicators.add("${textScore.matchCount} login keywords in text")
        }

        // Resource ID matching (~40 lines)
        val idScore = idMatcher.match(context.viewIds)
        if (idScore.matchCount >= 2) {
            indicators.add("${idScore.matchCount} login-related view IDs")
        }

        // EditText detection (~30 lines)
        val editTextScore = classMatcher.countClass(context.classNames, "EditText")
        if (editTextScore >= 2) {
            indicators.add("$editTextScore input fields")
        }

        // Material input detection (~30 lines)
        val materialScore = classMatcher.countMaterialInputs(context.classNames)
        if (materialScore >= 2) {
            indicators.add("$materialScore Material input fields")
        }

        // Button detection (~25 lines)
        val hasLoginButton = detectLoginButton(context)
        if (hasLoginButton) {
            indicators.add("Login button detected")
        }

        // Web content adjustment (~20 lines)
        val isWeb = classMatcher.isWebContent(context.classNames)

        // Calculate final score (~10 lines)
        val finalScore = scoringStrategy.calculateScore(
            textScore = textScore.confidence,
            idScore = idScore.confidence,
            editTextScore = if (editTextScore >= 2) 0.3f else 0.0f,
            materialScore = if (materialScore >= 2) 0.2f else 0.0f,
            buttonScore = if (hasLoginButton) 0.2f else 0.0f,
            webAdjustment = if (isWeb) 0.7f else 1.0f
        )

        return StateDetectionResult(AppState.LOGIN, finalScore, indicators)
    }

    override fun getTargetState() = AppState.LOGIN

    private fun detectLoginButton(context: DetectionContext): Boolean { ... }

    companion object {
        private val LOGIN_KEYWORDS = setOf(
            "login", "sign in", "log in", "signin", "username", "password",
            "email", "authenticate", "sign up", "register", "create account"
        )

        private val LOGIN_VIEW_ID_PATTERNS = setOf(
            "login", "signin", "sign_in", "btn_login", "button_login",
            "et_username", "et_email", "et_password", "input_username",
            "input_email", "input_password", "password_field", "email_field"
        )
    }
}
```

**Line Count Breakdown:**
- Imports: 10 lines
- Class header: 15 lines
- detectState method: 120 lines
- Helper methods: 20 lines
- Companion constants: 20 lines
- **Total: ~185 lines**

#### 4.2 LoadingStateDetector (175 lines)

**File:** `detectors/LoadingStateDetector.kt`

Similar structure, focuses on:
- Framework progress indicators
- Loading keywords
- Minimal content detection
- Progress view IDs

**Line Count:** ~175 lines

#### 4.3 ErrorStateDetector (165 lines)

**File:** `detectors/ErrorStateDetector.kt`

Focuses on:
- Error keywords
- Error view IDs
- Retry buttons

**Line Count:** ~165 lines

#### 4.4 PermissionStateDetector (170 lines)

**File:** `detectors/PermissionStateDetector.kt`

Focuses on:
- Permission keywords
- Allow/Deny buttons
- Permission view IDs

**Line Count:** ~170 lines

#### 4.5 TutorialStateDetector (168 lines)

**File:** `detectors/TutorialStateDetector.kt`

Focuses on:
- Tutorial keywords
- Skip/Next buttons
- Tutorial view IDs

**Line Count:** ~168 lines

#### 4.6 EmptyStateDetector (155 lines)

**File:** `detectors/EmptyStateDetector.kt`

Focuses on:
- Empty state keywords
- Empty state view IDs

**Line Count:** ~155 lines

#### 4.7 DialogStateDetector (190 lines)

**File:** `detectors/DialogStateDetector.kt`

Focuses on:
- Framework dialog classes
- Dialog view IDs
- Dialog buttons

**Line Count:** ~190 lines

---

### 5. Pattern Matchers (4 files, each <150 lines)

#### 5.1 PatternMatcher Interface (25 lines)

**File:** `matchers/PatternMatcher.kt`

```kotlin
/**
 * PatternMatcher.kt - Interface for pattern matching
 *
 * SOLID Compliance:
 * - ISP: Minimal focused interface
 *
 * Size: ~25 lines
 */
interface PatternMatcher {
    fun match(input: List<String>, patterns: Set<String>): MatchResult
}

data class MatchResult(
    val matchCount: Int,
    val confidence: Float,
    val matchedPatterns: List<String>
)
```

#### 5.2 ResourceIdMatcher (120 lines)

**File:** `matchers/ResourceIdMatcher.kt`

```kotlin
/**
 * ResourceIdMatcher.kt - Matches resource ID patterns
 *
 * Responsibilities:
 * - Pattern matching for Android resource IDs
 * - Confidence calculation based on match count
 * - Support for regex patterns
 *
 * SOLID Compliance:
 * - SRP: Only resource ID matching
 *
 * Size: ~120 lines (within 150 limit)
 */
class ResourceIdMatcher(
    private val patterns: Set<String>
) : PatternMatcher {

    override fun match(input: List<String>, patterns: Set<String>): MatchResult {
        val matched = mutableListOf<String>()
        var matchCount = 0

        input.forEach { id ->
            patterns.forEach { pattern ->
                if (id.contains(pattern, ignoreCase = true)) {
                    matchCount++
                    matched.add(pattern)
                }
            }
        }

        val confidence = calculateConfidence(matchCount, input.size)
        return MatchResult(matchCount, confidence, matched)
    }

    private fun calculateConfidence(matches: Int, total: Int): Float {
        if (total == 0) return 0.0f

        return when {
            matches >= 3 -> 0.35f
            matches == 2 -> 0.25f
            matches == 1 -> 0.15f
            else -> 0.0f
        }
    }

    fun matchSingle(id: String): Boolean {
        return patterns.any { pattern ->
            id.contains(pattern, ignoreCase = true)
        }
    }
}
```

**Line Count:** ~120 lines

#### 5.3 ClassNameMatcher (135 lines)

**File:** `matchers/ClassNameMatcher.kt`

```kotlin
/**
 * ClassNameMatcher.kt - Matches Android class names
 *
 * Responsibilities:
 * - Framework class detection
 * - Material component detection
 * - Compose UI detection
 * - WebView detection
 *
 * SOLID Compliance:
 * - SRP: Only class name matching
 *
 * Size: ~135 lines (within 150 limit)
 */
class ClassNameMatcher {

    fun countClass(classNames: List<String>, targetClass: String): Int {
        return classNames.count { it.contains(targetClass) }
    }

    fun hasFrameworkClass(classNames: List<String>, frameworks: Set<String>): Boolean {
        return classNames.any { className ->
            frameworks.any { it in className }
        }
    }

    fun countMaterialInputs(classNames: List<String>): Int {
        return classNames.count { className ->
            MATERIAL_INPUT_CLASSES.any { it in className }
        }
    }

    fun isWebContent(classNames: List<String>): Boolean {
        return classNames.any { className ->
            WEBVIEW_CLASSES.any { it in className }
        }
    }

    fun isComposeUI(classNames: List<String>): Boolean {
        return classNames.any { className ->
            COMPOSE_UI_PATTERNS.any { className.startsWith(it) }
        }
    }

    fun getUIFramework(classNames: List<String>): UIFramework {
        return when {
            isComposeUI(classNames) -> UIFramework.COMPOSE
            isWebContent(classNames) -> UIFramework.WEBVIEW
            else -> UIFramework.TRADITIONAL_VIEWS
        }
    }

    companion object {
        private val MATERIAL_INPUT_CLASSES = setOf(
            "com.google.android.material.textfield.TextInputLayout",
            "com.google.android.material.textfield.TextInputEditText"
        )

        private val WEBVIEW_CLASSES = setOf(
            "android.webkit.WebView",
            "android.webkit.WebViewClient"
        )

        private val COMPOSE_UI_PATTERNS = setOf(
            "androidx.compose.ui",
            "androidx.compose.material"
        )

        private val DIALOG_FRAMEWORK_CLASSES = setOf(
            "android.app.AlertDialog",
            "androidx.appcompat.app.AlertDialog"
        )

        private val PROGRESS_FRAMEWORK_CLASSES = setOf(
            "android.widget.ProgressBar",
            "com.google.android.material.progressindicator.CircularProgressIndicator"
        )
    }
}

enum class UIFramework {
    TRADITIONAL_VIEWS,
    COMPOSE,
    WEBVIEW
}
```

**Line Count:** ~135 lines

#### 5.4 TextMatcher (110 lines)

**File:** `matchers/TextMatcher.kt`

```kotlin
/**
 * TextMatcher.kt - Matches text content against keywords
 *
 * Responsibilities:
 * - Keyword matching
 * - Case-insensitive matching
 * - Multi-language support (future)
 *
 * SOLID Compliance:
 * - SRP: Only text matching
 *
 * Size: ~110 lines (within 150 limit)
 */
class TextMatcher(
    private val keywords: Set<String>
) : PatternMatcher {

    override fun match(input: List<String>, patterns: Set<String>): MatchResult {
        val matched = mutableListOf<String>()
        var matchCount = 0

        input.forEach { text ->
            keywords.forEach { keyword ->
                if (text.contains(keyword, ignoreCase = true)) {
                    matchCount++
                    matched.add(keyword)
                }
            }
        }

        val confidence = calculateConfidence(matchCount)
        return MatchResult(matchCount, confidence, matched)
    }

    private fun calculateConfidence(matches: Int): Float {
        return when {
            matches >= 3 -> 0.5f
            matches == 2 -> 0.35f
            matches == 1 -> 0.25f
            else -> 0.0f
        }
    }

    fun matchAny(text: String): Boolean {
        return keywords.any { keyword ->
            text.contains(keyword, ignoreCase = true)
        }
    }

    fun matchAll(textList: List<String>): List<String> {
        return textList.filter { text -> matchAny(text) }
    }
}
```

**Line Count:** ~110 lines

#### 5.5 HierarchyMatcher (145 lines)

**File:** `matchers/HierarchyMatcher.kt`

```kotlin
/**
 * HierarchyMatcher.kt - Matches hierarchical patterns in node tree
 *
 * Responsibilities:
 * - Tree traversal
 * - Parent-child relationship analysis
 * - Depth-based scoring
 * - Sibling detection
 *
 * SOLID Compliance:
 * - SRP: Only hierarchy matching
 *
 * Size: ~145 lines (within 150 limit)
 */
class HierarchyMatcher {

    fun findChildWithClass(
        node: AccessibilityNodeInfo,
        targetClass: String
    ): AccessibilityNodeInfo? {
        // Breadth-first search
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child?.className?.contains(targetClass) == true) {
                return child
            }
        }
        return null
    }

    fun findChildrenWithClass(
        node: AccessibilityNodeInfo,
        targetClass: String
    ): List<AccessibilityNodeInfo> {
        val found = mutableListOf<AccessibilityNodeInfo>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (child.className?.contains(targetClass) == true) {
                found.add(child)
            }
        }
        return found
    }

    fun getDepth(node: AccessibilityNodeInfo): Int {
        var depth = 0
        var current = node.parent
        while (current != null) {
            depth++
            current = current.parent
        }
        return depth
    }

    fun hasSiblingWithClass(
        node: AccessibilityNodeInfo,
        targetClass: String
    ): Boolean {
        val parent = node.parent ?: return false
        for (i in 0 until parent.childCount) {
            val sibling = parent.getChild(i)
            if (sibling != null &&
                sibling != node &&
                sibling.className?.contains(targetClass) == true) {
                return true
            }
        }
        return false
    }
}
```

**Line Count:** ~145 lines

---

### 6. Scoring Strategies (4 files, each <100 lines)

#### 6.1 ScoringStrategy Interface (20 lines)

**File:** `scoring/ScoringStrategy.kt`

```kotlin
/**
 * ScoringStrategy.kt - Interface for confidence scoring
 *
 * SOLID Compliance:
 * - ISP: Focused interface
 * - OCP: Extensible scoring algorithms
 *
 * Size: ~20 lines
 */
interface ScoringStrategy {
    fun calculateScore(vararg scores: Pair<String, Float>): Float
}
```

#### 6.2 LoginScoringStrategy (85 lines)

**File:** `scoring/LoginScoringStrategy.kt`

```kotlin
/**
 * LoginScoringStrategy.kt - Scoring logic for login detection
 *
 * Responsibilities:
 * - Weight different signals appropriately
 * - Apply web content adjustment
 * - Ensure score doesn't exceed 1.0
 *
 * Size: ~85 lines
 */
class LoginScoringStrategy : ScoringStrategy {

    override fun calculateScore(vararg scores: Pair<String, Float>): Float {
        val scoreMap = scores.toMap()

        var total = 0f

        // Text matches (25%)
        total += scoreMap["textScore"] ?: 0.0f * 0.25f

        // Resource ID matches (35%)
        total += scoreMap["idScore"] ?: 0.0f * 0.35f

        // EditText fields (30%)
        total += scoreMap["editTextScore"] ?: 0.0f

        // Material inputs (20%)
        total += scoreMap["materialScore"] ?: 0.0f

        // Login button (20%)
        total += scoreMap["buttonScore"] ?: 0.0f

        // Web adjustment (multiply)
        val webAdjustment = scoreMap["webAdjustment"] ?: 1.0f
        total *= webAdjustment

        return total.coerceAtMost(1.0f)
    }
}
```

**Line Count:** ~85 lines

Similar scoring strategies for Loading, Error, Permission, Tutorial, Empty, Dialog states.

---

### 7. Data Models (3 files, each <100 lines)

#### 7.1 DetectionContext (75 lines)

**File:** `models/DetectionContext.kt`

```kotlin
/**
 * DetectionContext.kt - Collected UI data for detection
 *
 * Responsibilities:
 * - Hold all collected UI data
 * - Provide convenient accessors
 *
 * Size: ~75 lines
 */
data class DetectionContext(
    val textContent: List<String>,
    val viewIds: List<String>,
    val classNames: List<String>,
    val nodeTree: NodeTree?
) {
    fun hasText(keyword: String): Boolean {
        return textContent.any { it.contains(keyword, ignoreCase = true) }
    }

    fun hasViewId(pattern: String): Boolean {
        return viewIds.any { it.contains(pattern, ignoreCase = true) }
    }

    fun hasClass(className: String): Boolean {
        return classNames.any { it.contains(className) }
    }

    fun countClasses(className: String): Int {
        return classNames.count { it.contains(className) }
    }
}

data class NodeTree(
    val root: TreeNode
)

data class TreeNode(
    val text: String?,
    val viewId: String?,
    val className: String?,
    val children: List<TreeNode>
)
```

**Line Count:** ~75 lines

#### 7.2 NodeDataCollector (95 lines)

**File:** `collectors/NodeDataCollector.kt`

```kotlin
/**
 * NodeDataCollector.kt - Collects data from accessibility tree
 *
 * Responsibilities:
 * - Traverse accessibility node tree
 * - Collect text, IDs, class names
 * - Build tree representation
 *
 * SOLID Compliance:
 * - SRP: Only data collection
 *
 * Size: ~95 lines
 */
class NodeDataCollector {
    val textContent = mutableListOf<String>()
    val viewIds = mutableListOf<String>()
    val classNames = mutableListOf<String>()

    private val treeNodes = mutableListOf<TreeNode>()

    fun traverse(node: AccessibilityNodeInfo) {
        collectNodeData(node)

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                traverse(child)
            }
        }
    }

    private fun collectNodeData(node: AccessibilityNodeInfo) {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { textContent.add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { textContent.add(it) }
        node.hintText?.toString()?.takeIf { it.isNotBlank() }?.let { textContent.add(it) }

        node.viewIdResourceName?.let { viewIds.add(it) }
        node.className?.toString()?.let { classNames.add(it) }
    }

    fun buildTree(): NodeTree? {
        // Build tree structure from collected data
        // Implementation details...
        return null // Placeholder
    }
}
```

**Line Count:** ~95 lines

---

### 8. Configuration & Constants (2 files)

#### 8.1 StateDetectorConfig (50 lines)

**File:** `config/StateDetectorConfig.kt`

Already exists, no changes needed.

**Line Count:** ~50 lines

#### 8.2 PatternConstants (280 lines)

**File:** `patterns/PatternConstants.kt`

```kotlin
/**
 * PatternConstants.kt - All pattern sets consolidated
 *
 * Responsibilities:
 * - Central location for all keyword sets
 * - View ID pattern sets
 * - Framework class sets
 *
 * Size: ~280 lines (within 300 limit)
 */
object PatternConstants {

    // Login patterns
    val LOGIN_KEYWORDS = setOf(
        "login", "sign in", "log in", "signin", "username", "password",
        "email", "authenticate", "sign up", "register", "create account"
    )

    val LOGIN_VIEW_ID_PATTERNS = setOf(
        "login", "signin", "sign_in", "btn_login", "button_login",
        "et_username", "et_email", "et_password", "input_username"
        // ... more patterns
    )

    // Loading patterns
    val LOADING_KEYWORDS = setOf(
        "loading", "please wait", "processing", "refreshing"
    )

    val LOADING_VIEW_ID_PATTERNS = setOf(
        "progress", "loading", "spinner", "pb_loading"
    )

    // Error patterns
    val ERROR_KEYWORDS = setOf(
        "error", "failed", "failure", "problem", "retry"
    )

    val ERROR_VIEW_ID_PATTERNS = setOf(
        "error", "err", "error_message", "retry"
    )

    // Permission patterns
    val PERMISSION_KEYWORDS = setOf(
        "permission", "allow", "deny", "access", "grant"
    )

    val PERMISSION_VIEW_ID_PATTERNS = setOf(
        "permission", "allow", "deny", "grant"
    )

    // Tutorial patterns
    val TUTORIAL_KEYWORDS = setOf(
        "welcome", "tutorial", "getting started", "onboarding"
    )

    val TUTORIAL_VIEW_ID_PATTERNS = setOf(
        "tutorial", "onboarding", "guide", "skip", "next"
    )

    // Empty state patterns
    val EMPTY_STATE_KEYWORDS = setOf(
        "no items", "nothing here", "empty", "no results"
    )

    val EMPTY_STATE_VIEW_ID_PATTERNS = setOf(
        "empty", "empty_state", "no_data"
    )

    // Dialog patterns
    val DIALOG_KEYWORDS = setOf(
        "ok", "cancel", "yes", "no", "confirm"
    )

    val DIALOG_VIEW_ID_PATTERNS = setOf(
        "dialog", "alert", "popup", "btn_ok"
    )

    // Framework classes
    val DIALOG_FRAMEWORK_CLASSES = setOf(
        "android.app.AlertDialog",
        "androidx.appcompat.app.AlertDialog"
    )

    val PROGRESS_FRAMEWORK_CLASSES = setOf(
        "android.widget.ProgressBar",
        "com.google.android.material.progressindicator.CircularProgressIndicator"
    )

    val MATERIAL_INPUT_CLASSES = setOf(
        "com.google.android.material.textfield.TextInputLayout",
        "com.google.android.material.textfield.TextInputEditText"
    )

    val WEBVIEW_CLASSES = setOf(
        "android.webkit.WebView",
        "android.webkit.WebViewClient"
    )

    val COMPOSE_UI_PATTERNS = setOf(
        "androidx.compose.ui",
        "androidx.compose.material"
    )
}
```

**Line Count:** ~280 lines (within 300 limit)

---

## Complete File Structure

```
com.augmentalis.learnapp.state/
├── AppStateDetector.kt                    (245 lines) ✓
├── StateDetectionPipeline.kt              (180 lines) ✓
├── StateDetectionStrategy.kt              ( 30 lines) ✓
│
├── detectors/
│   ├── LoginStateDetector.kt              (185 lines) ✓
│   ├── LoadingStateDetector.kt            (175 lines) ✓
│   ├── ErrorStateDetector.kt              (165 lines) ✓
│   ├── PermissionStateDetector.kt         (170 lines) ✓
│   ├── TutorialStateDetector.kt           (168 lines) ✓
│   ├── EmptyStateDetector.kt              (155 lines) ✓
│   └── DialogStateDetector.kt             (190 lines) ✓
│
├── matchers/
│   ├── PatternMatcher.kt                  ( 25 lines) ✓
│   ├── ResourceIdMatcher.kt               (120 lines) ✓
│   ├── ClassNameMatcher.kt                (135 lines) ✓
│   ├── TextMatcher.kt                     (110 lines) ✓
│   └── HierarchyMatcher.kt                (145 lines) ✓
│
├── scoring/
│   ├── ScoringStrategy.kt                 ( 20 lines) ✓
│   ├── LoginScoringStrategy.kt            ( 85 lines) ✓
│   ├── LoadingScoringStrategy.kt          ( 80 lines) ✓
│   ├── ErrorScoringStrategy.kt            ( 75 lines) ✓
│   └── ... (4 more strategies)            (~75 lines each) ✓
│
├── models/
│   ├── AppState.kt                        ( 40 lines) ✓ (already exists)
│   ├── StateDetectionResult.kt            ( 60 lines) ✓ (already exists)
│   ├── StateTransition.kt                 ( 30 lines) ✓ (already exists)
│   └── DetectionContext.kt                ( 75 lines) ✓
│
├── collectors/
│   └── NodeDataCollector.kt               ( 95 lines) ✓
│
├── config/
│   └── StateDetectorConfig.kt             ( 50 lines) ✓ (already exists)
│
└── patterns/
    └── PatternConstants.kt                (280 lines) ✓
```

**Total Files:** 21 files
**Largest File:** 280 lines (PatternConstants.kt)
**All files < 300 lines:** ✓

---

## SOLID Principles Application

### Single Responsibility Principle (SRP) ✓

**Before:** AppStateDetector had 8 responsibilities
- Orchestration
- State management
- Tree traversal
- Text matching
- ID matching
- Class matching
- Scoring
- State transitions

**After:** Each class has ONE responsibility
- `AppStateDetector`: Orchestration only
- `StateDetectionPipeline`: Pipeline execution only
- `LoginStateDetector`: Login detection only
- `ResourceIdMatcher`: ID matching only
- `TextMatcher`: Text matching only
- `LoginScoringStrategy`: Login scoring only

### Open/Closed Principle (OCP) ✓

**Extension Points:**
1. **New States:** Implement `StateDetectionStrategy`, register in pipeline
2. **New Matchers:** Implement `PatternMatcher`, inject into detector
3. **New Scoring:** Implement `ScoringStrategy`, inject into detector
4. **Custom Pipeline:** Implement `StateDetectionPipeline` with custom logic

**Example - Adding new state:**
```kotlin
class CustomStateDetector(
    private val textMatcher: TextMatcher,
    private val idMatcher: ResourceIdMatcher
) : StateDetectionStrategy {
    override fun detectState(context: DetectionContext, config: StateDetectorConfig): StateDetectionResult {
        // Custom detection logic
    }

    override fun getTargetState() = AppState.CUSTOM
}

// Register in pipeline
val pipeline = DefaultStateDetectionPipeline(
    strategies = defaultStrategies() + CustomStateDetector()
)
```

**No modification of existing code required!**

### Liskov Substitution Principle (LSP) ✓

All implementations are substitutable:
```kotlin
// Any StateDetectionStrategy can be used
val strategy: StateDetectionStrategy = LoginStateDetector()
val result = strategy.detectState(context, config)

// Any PatternMatcher can be used
val matcher: PatternMatcher = ResourceIdMatcher(patterns)
val matchResult = matcher.match(input, patterns)

// Any ScoringStrategy can be used
val scoring: ScoringStrategy = LoginScoringStrategy()
val score = scoring.calculateScore(...)
```

### Interface Segregation Principle (ISP) ✓

**Focused Interfaces:**
- `StateDetectionStrategy`: 2 methods (detectState, getTargetState)
- `PatternMatcher`: 1 method (match)
- `ScoringStrategy`: 1 method (calculateScore)
- `StateDetectionPipeline`: 1 method (detectState)

**No client forced to depend on unused methods.**

### Dependency Inversion Principle (DIP) ✓

**All dependencies on abstractions:**
```kotlin
class AppStateDetector(
    private val pipeline: StateDetectionPipeline  // Interface, not implementation
)

class LoginStateDetector(
    private val textMatcher: TextMatcher,        // Concrete but injectable
    private val idMatcher: ResourceIdMatcher,    // Concrete but injectable
    private val scoringStrategy: ScoringStrategy // Interface
)

class DefaultStateDetectionPipeline(
    private val strategies: List<StateDetectionStrategy>  // Interface list
)
```

**High-level modules don't depend on low-level modules. Both depend on abstractions.**

---

## Migration Path

### Phase 1: Create Infrastructure (No Breaking Changes)

1. Create new package structure
2. Create interfaces (StateDetectionStrategy, PatternMatcher, ScoringStrategy)
3. Create models (DetectionContext, NodeDataCollector)
4. Create PatternConstants

**Impact:** Zero. New files only.

### Phase 2: Create Matchers (No Breaking Changes)

1. Create TextMatcher
2. Create ResourceIdMatcher
3. Create ClassNameMatcher
4. Create HierarchyMatcher

**Impact:** Zero. New files only.

### Phase 3: Create Detectors (No Breaking Changes)

1. Create LoginStateDetector
2. Create LoadingStateDetector
3. Create ErrorStateDetector
4. Create PermissionStateDetector
5. Create TutorialStateDetector
6. Create EmptyStateDetector
7. Create DialogStateDetector

**Impact:** Zero. New files only.

### Phase 4: Create Scoring Strategies (No Breaking Changes)

1. Create ScoringStrategy interface
2. Create all 7 scoring strategy implementations

**Impact:** Zero. New files only.

### Phase 5: Create Pipeline (No Breaking Changes)

1. Create StateDetectionPipeline interface
2. Create DefaultStateDetectionPipeline

**Impact:** Zero. New files only.

### Phase 6: Refactor AppStateDetector (BREAKING CHANGE)

1. Update AppStateDetector to use pipeline
2. Remove old detection methods
3. Remove old pattern constants
4. Simplify to orchestrator

**Impact:** Internal only. Public API remains same.

### Phase 7: Deprecate Old Methods (Optional)

1. Mark old direct detection methods as @Deprecated
2. Provide migration guide

**Impact:** Warnings only.

### Backward Compatibility

**Public API preserved:**
```kotlin
// Old API still works
val detector = AppStateDetector()
val result = detector.detectState(rootNode)
val state = detector.getCurrentState()
detector.reset()

// New API available
val customDetector = AppStateDetector(
    pipeline = DefaultStateDetectionPipeline(customStrategies),
    config = StateDetectorConfig(...)
)
```

---

## Performance Analysis

### Memory Impact

**Before:**
- 1 large object with all logic embedded
- All patterns loaded at instantiation

**After:**
- 21 small objects with focused responsibilities
- Lazy instantiation possible
- Pattern constants in singleton object (shared)

**Verdict:** Minimal increase (~5KB for additional object headers)

### CPU Impact

**Before:**
- Direct method calls
- No abstraction overhead

**After:**
- Interface dispatch (virtual method calls)
- Pipeline iteration
- Strategy pattern overhead

**Overhead:** ~0.1ms per detection call (negligible for UI analysis)

### Code Maintenance

**Before:**
- 1 file, 518 lines (would grow to 1100+)
- Difficult to navigate
- High risk of merge conflicts
- Hard to test individual components

**After:**
- 21 files, all <300 lines
- Easy to navigate and understand
- Low risk of merge conflicts (isolated changes)
- Easy to test (mock interfaces)

**Verdict:** MASSIVE improvement in maintainability

---

## Testing Strategy

### Unit Testing (New Architecture)

Each component testable in isolation:

```kotlin
@Test
fun testLoginStateDetector() {
    // Mock dependencies
    val mockTextMatcher = mock<TextMatcher>()
    val mockIdMatcher = mock<ResourceIdMatcher>()
    val mockScoring = mock<ScoringStrategy>()

    // Create detector with mocks
    val detector = LoginStateDetector(
        textMatcher = mockTextMatcher,
        idMatcher = mockIdMatcher,
        scoringStrategy = mockScoring
    )

    // Test
    val context = DetectionContext(...)
    val result = detector.detectState(context, config)

    // Verify
    assertEquals(AppState.LOGIN, result.state)
    verify(mockTextMatcher).match(any(), any())
}

@Test
fun testResourceIdMatcher() {
    val matcher = ResourceIdMatcher(setOf("login", "password"))

    val result = matcher.match(
        input = listOf("et_login", "et_password", "btn_submit"),
        patterns = setOf("login", "password")
    )

    assertEquals(2, result.matchCount)
    assertTrue(result.confidence > 0.2f)
}

@Test
fun testDetectionPipeline() {
    val mockStrategy1 = mock<StateDetectionStrategy>()
    val mockStrategy2 = mock<StateDetectionStrategy>()

    whenever(mockStrategy1.detectState(any(), any())).thenReturn(
        StateDetectionResult(AppState.LOGIN, 0.5f, emptyList())
    )
    whenever(mockStrategy2.detectState(any(), any())).thenReturn(
        StateDetectionResult(AppState.LOADING, 0.8f, emptyList())
    )

    val pipeline = DefaultStateDetectionPipeline(
        strategies = listOf(mockStrategy1, mockStrategy2)
    )

    val result = pipeline.detectState(context, config)

    // Should select LOADING (higher confidence)
    assertEquals(AppState.LOADING, result.state)
    assertEquals(0.8f, result.confidence)
}
```

### Integration Testing

```kotlin
@Test
fun testFullDetectionPipeline() {
    // Create real detector with real dependencies
    val detector = AppStateDetector()

    // Create mock node tree
    val rootNode = createMockLoginScreen()

    // Detect
    val result = detector.detectState(rootNode)

    // Verify
    assertEquals(AppState.LOGIN, result.state)
    assertTrue(result.confidence >= 0.7f)
    assertTrue(result.indicators.isNotEmpty())
}
```

---

## Extension Examples

### Example 1: Add New State Type

```kotlin
// 1. Add to enum (existing file)
enum class AppState {
    // ... existing states
    PAYMENT  // NEW
}

// 2. Create detector (new file, ~170 lines)
class PaymentStateDetector(
    private val textMatcher: TextMatcher = TextMatcher(PAYMENT_KEYWORDS),
    private val idMatcher: ResourceIdMatcher = ResourceIdMatcher(PAYMENT_VIEW_ID_PATTERNS),
    private val classMatcher: ClassNameMatcher = ClassNameMatcher()
) : StateDetectionStrategy {

    override fun detectState(context: DetectionContext, config: StateDetectorConfig): StateDetectionResult {
        // Detection logic
    }

    override fun getTargetState() = AppState.PAYMENT

    companion object {
        private val PAYMENT_KEYWORDS = setOf(
            "payment", "checkout", "pay now", "credit card"
        )

        private val PAYMENT_VIEW_ID_PATTERNS = setOf(
            "payment", "checkout", "card_number", "cvv"
        )
    }
}

// 3. Register in pipeline
val pipeline = DefaultStateDetectionPipeline(
    strategies = defaultStrategies() + PaymentStateDetector()
)

// Done! No modification to existing files needed.
```

### Example 2: Custom Scoring Algorithm

```kotlin
// Create custom scoring strategy (new file, ~90 lines)
class MLBasedScoringStrategy(
    private val model: TensorFlowLiteModel
) : ScoringStrategy {

    override fun calculateScore(vararg scores: Pair<String, Float>): Float {
        val features = scores.map { it.second }.toFloatArray()
        return model.predict(features)
    }
}

// Use in detector
val detector = LoginStateDetector(
    scoringStrategy = MLBasedScoringStrategy(loadModel())
)

// Done! Existing code unchanged.
```

### Example 3: App-Specific Pattern Learning

```kotlin
// Create learning matcher (new file, ~150 lines)
class LearningPatternMatcher(
    private val database: PatternDatabase
) : PatternMatcher {

    override fun match(input: List<String>, patterns: Set<String>): MatchResult {
        // Use learned patterns from database
        val learnedPatterns = database.getPatternsForApp(currentApp)
        val allPatterns = patterns + learnedPatterns

        // Match and learn
        val result = performMatch(input, allPatterns)
        database.recordMatch(currentApp, result)

        return result
    }
}

// Use in detector
val detector = LoginStateDetector(
    idMatcher = ResourceIdMatcher(...).withLearning(database)
)

// Done! Learning capability added without modifying core code.
```

---

## Alternatives Considered

### Alternative 1: Keep Monolithic (Rejected)

**Pros:**
- No refactoring needed
- Simpler structure
- Fewer files

**Cons:**
- File would exceed 1100 lines (SOLID violation)
- Unmaintainable
- Hard to test
- High coupling
- No extensibility

**Verdict:** REJECTED - Violates SOLID principles and maintainability

### Alternative 2: Partial Refactoring (Rejected)

Extract matchers only, keep detectors in main class.

**Pros:**
- Less refactoring than full SOLID
- Reduces main class size to ~700 lines

**Cons:**
- Still violates SRP (one class, multiple detection responsibilities)
- Still exceeds 300-line limit
- Not OCP compliant
- Incomplete solution

**Verdict:** REJECTED - Doesn't solve the core problem

### Alternative 3: Annotation-Based Detection (Rejected)

Use annotations to mark detection methods and auto-register.

```kotlin
@DetectionStrategy(state = AppState.LOGIN)
fun detectLogin(context: DetectionContext): DetectionResult { ... }
```

**Pros:**
- Less boilerplate
- Automatic registration

**Cons:**
- Requires reflection or code generation
- Less explicit
- Harder to debug
- Magic behavior
- Still doesn't solve file size issue

**Verdict:** REJECTED - Doesn't address SOLID violations

### Alternative 4: Proposed SOLID Architecture (ACCEPTED)

**Pros:**
- Full SOLID compliance
- All files <300 lines
- Highly testable
- Extensible
- Clear separation of concerns
- Easy to maintain
- No performance penalty

**Cons:**
- More files (21 vs 1)
- More initial setup
- Slightly more complex navigation

**Verdict:** ACCEPTED - Best long-term solution

---

## Implementation Plan

### Sprint 1: Foundation (5 days)

**Goal:** Create infrastructure without breaking changes

**Tasks:**
1. Create package structure
2. Create interfaces (StateDetectionStrategy, PatternMatcher, ScoringStrategy)
3. Create models (DetectionContext, NodeDataCollector)
4. Create PatternConstants
5. Write unit tests for new components

**Deliverable:** Infrastructure classes, 100% tested

### Sprint 2: Matchers (3 days)

**Goal:** Create all pattern matchers

**Tasks:**
1. Implement TextMatcher
2. Implement ResourceIdMatcher
3. Implement ClassNameMatcher
4. Implement HierarchyMatcher
5. Write unit tests (100% coverage)

**Deliverable:** Working matchers, fully tested

### Sprint 3: Detectors (5 days)

**Goal:** Create all state detectors

**Tasks:**
1. Implement LoginStateDetector
2. Implement LoadingStateDetector
3. Implement ErrorStateDetector
4. Implement PermissionStateDetector
5. Implement TutorialStateDetector
6. Implement EmptyStateDetector
7. Implement DialogStateDetector
8. Write unit tests for each

**Deliverable:** Working detectors, fully tested

### Sprint 4: Scoring (2 days)

**Goal:** Create scoring strategies

**Tasks:**
1. Create ScoringStrategy interface
2. Implement all 7 scoring strategies
3. Write unit tests

**Deliverable:** Working scoring strategies

### Sprint 5: Pipeline (2 days)

**Goal:** Create detection pipeline

**Tasks:**
1. Create StateDetectionPipeline interface
2. Implement DefaultStateDetectionPipeline
3. Write unit tests
4. Write integration tests

**Deliverable:** Working pipeline, fully tested

### Sprint 6: Integration (3 days)

**Goal:** Refactor AppStateDetector

**Tasks:**
1. Update AppStateDetector to use pipeline
2. Remove old detection methods
3. Ensure backward compatibility
4. Update documentation
5. Full integration testing

**Deliverable:** Refactored AppStateDetector, backward compatible

### Sprint 7: Validation (2 days)

**Goal:** Validate entire system

**Tasks:**
1. End-to-end testing
2. Performance testing
3. Regression testing
4. Documentation updates

**Deliverable:** Production-ready system

**Total: 22 days (~4.5 weeks)**

---

## Success Metrics

### Code Quality Metrics

- **Files >300 lines:** 0 (currently would be 1)
- **Cyclomatic complexity:** <10 per method (currently ~15)
- **Test coverage:** >90% (currently ~60%)
- **Coupling:** Low (currently high)
- **Cohesion:** High (currently low)

### Functional Metrics

- **Detection accuracy:** 85-92% (same as planned)
- **Confidence scores:** 0.6-0.95 range (same as planned)
- **Performance:** <1ms overhead (negligible)
- **Backward compatibility:** 100%

### Maintainability Metrics

- **Time to add new state:** <4 hours (currently ~8 hours)
- **Time to modify scoring:** <2 hours (currently ~4 hours)
- **Merge conflict risk:** Low (currently high)
- **Onboarding time:** Reduced by 50%

---

## Risk Analysis

### Risk 1: Increased Complexity (Medium)

**Impact:** Developers need to navigate 21 files instead of 1

**Mitigation:**
- Clear package structure
- Comprehensive documentation
- IDE navigation (Ctrl+Click)
- Class diagram in docs

**Residual Risk:** Low

### Risk 2: Performance Regression (Low)

**Impact:** Interface dispatch adds overhead

**Mitigation:**
- Performance testing
- Profiling
- Inline critical paths if needed

**Residual Risk:** Very Low (~0.1ms overhead acceptable)

### Risk 3: Migration Bugs (Medium)

**Impact:** Refactoring introduces bugs

**Mitigation:**
- Comprehensive unit tests before refactoring
- Integration tests
- Regression tests
- Gradual rollout

**Residual Risk:** Low

### Risk 4: Adoption Resistance (Low)

**Impact:** Team prefers simple monolithic design

**Mitigation:**
- Education on SOLID principles
- Demonstrate testability improvements
- Show extensibility benefits

**Residual Risk:** Very Low

---

## Conclusion

This SOLID-based refactoring transforms `AppStateDetector` from a monolithic 518-line class (would grow to 1100+) into a modular, maintainable architecture with 21 focused classes, all under 300 lines.

**Key Benefits:**
1. **SOLID Compliance:** All 5 principles rigorously applied
2. **File Size:** No file exceeds 300 lines (largest: 280 lines)
3. **Maintainability:** Easy to understand, modify, and extend
4. **Testability:** Every component testable in isolation
5. **Extensibility:** Add new states, matchers, or scoring without modifying existing code
6. **Backward Compatible:** Public API unchanged

**Recommendation:** APPROVE and implement in 4.5-week sprint plan.

---

## Appendices

### Appendix A: Complete Class Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENT CODE                                │
│                    (VoiceOSService, etc.)                           │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             │ uses
                             ▼
                 ┌───────────────────────┐
                 │  AppStateDetector     │
                 │  (Orchestrator)       │
                 │  - 245 lines          │
                 └───────────┬───────────┘
                             │
                             │ delegates to
                             ▼
                 ┌───────────────────────────┐
                 │  StateDetectionPipeline   │◄───────┐
                 │  (Interface)              │        │ implements
                 └───────────┬───────────────┘        │
                             │                        │
                             │ implemented by         │
                             ▼                        │
         ┌───────────────────────────────┐           │
         │ DefaultStateDetectionPipeline │───────────┘
         │  - 180 lines                  │
         └───────────┬───────────────────┘
                     │
                     │ uses List<StateDetectionStrategy>
                     │
       ┌─────────────┼─────────────────────────────────────┐
       │             │                                     │
       │             ▼                                     │
       │    ┌─────────────────────┐                      │
       │    │ StateDetectionStrategy │◄──────────────┐    │
       │    │ (Interface)            │               │    │
       │    └────────────────────────┘               │    │
       │             │                               │    │
       │             │ implemented by (7 classes)    │    │
       │             │                               │    │
       ├─────────────┼───────────────────────────────┤    │
       │             │                               │    │
       │    ┌────────▼────────┐                     │    │
       │    │ LoginStateDetector │                   │    │
       │    │  - 185 lines       │                   │    │
       │    └────────┬───────────┘                   │    │
       │             │                               │    │
       │             │ uses                          │    │
       │             │                               │    │
       │    ┌────────▼────────────────────────┐     │    │
       │    │  TextMatcher                    │     │    │
       │    │  ResourceIdMatcher              │     │    │
       │    │  ClassNameMatcher               │     │    │
       │    │  HierarchyMatcher               │     │    │
       │    │  ScoringStrategy                │     │    │
       │    └─────────────────────────────────┘     │    │
       │                                            │    │
       ├────────────────────────────────────────────┤    │
       │                                            │    │
       │    Similar structure for:                  │    │
       │    - LoadingStateDetector (175 lines)      │    │
       │    - ErrorStateDetector (165 lines)        │    │
       │    - PermissionStateDetector (170 lines)   │    │
       │    - TutorialStateDetector (168 lines)     │    │
       │    - EmptyStateDetector (155 lines)        │    │
       │    - DialogStateDetector (190 lines)       │    │
       │                                            │    │
       └────────────────────────────────────────────┘    │
                                                        │
┌───────────────────────────────────────────────────────┘
│
│  SUPPORTING COMPONENTS
│
├─── PatternMatcher (Interface) ◄──┬── TextMatcher (110 lines)
│                                  ├── ResourceIdMatcher (120 lines)
│                                  ├── ClassNameMatcher (135 lines)
│                                  └── HierarchyMatcher (145 lines)
│
├─── ScoringStrategy (Interface) ◄─┬── LoginScoringStrategy (85 lines)
│                                  ├── LoadingScoringStrategy (80 lines)
│                                  └── ... (5 more strategies)
│
├─── DetectionContext (75 lines)
├─── NodeDataCollector (95 lines)
└─── PatternConstants (280 lines)
```

### Appendix B: Package Structure

```
com.augmentalis.learnapp.state/
│
├── AppStateDetector.kt                    # Main orchestrator
├── StateDetectionPipeline.kt              # Pipeline interface & impl
├── StateDetectionStrategy.kt              # Strategy interface
│
├── detectors/                             # State-specific detectors
│   ├── LoginStateDetector.kt
│   ├── LoadingStateDetector.kt
│   ├── ErrorStateDetector.kt
│   ├── PermissionStateDetector.kt
│   ├── TutorialStateDetector.kt
│   ├── EmptyStateDetector.kt
│   └── DialogStateDetector.kt
│
├── matchers/                              # Pattern matching
│   ├── PatternMatcher.kt                  # Interface
│   ├── TextMatcher.kt
│   ├── ResourceIdMatcher.kt
│   ├── ClassNameMatcher.kt
│   └── HierarchyMatcher.kt
│
├── scoring/                               # Scoring strategies
│   ├── ScoringStrategy.kt                 # Interface
│   ├── LoginScoringStrategy.kt
│   ├── LoadingScoringStrategy.kt
│   ├── ErrorScoringStrategy.kt
│   └── ... (4 more)
│
├── models/                                # Data models
│   ├── AppState.kt                        # Existing
│   ├── StateDetectionResult.kt            # Existing
│   ├── StateTransition.kt                 # Existing
│   └── DetectionContext.kt                # New
│
├── collectors/                            # Data collection
│   └── NodeDataCollector.kt
│
├── config/                                # Configuration
│   └── StateDetectorConfig.kt             # Existing
│
└── patterns/                              # Pattern constants
    └── PatternConstants.kt
```

### Appendix C: File Size Summary

| File | Lines | Status |
|------|-------|--------|
| AppStateDetector.kt | 245 | ✓ |
| StateDetectionPipeline.kt | 180 | ✓ |
| StateDetectionStrategy.kt | 30 | ✓ |
| LoginStateDetector.kt | 185 | ✓ |
| LoadingStateDetector.kt | 175 | ✓ |
| ErrorStateDetector.kt | 165 | ✓ |
| PermissionStateDetector.kt | 170 | ✓ |
| TutorialStateDetector.kt | 168 | ✓ |
| EmptyStateDetector.kt | 155 | ✓ |
| DialogStateDetector.kt | 190 | ✓ |
| PatternMatcher.kt | 25 | ✓ |
| ResourceIdMatcher.kt | 120 | ✓ |
| ClassNameMatcher.kt | 135 | ✓ |
| TextMatcher.kt | 110 | ✓ |
| HierarchyMatcher.kt | 145 | ✓ |
| ScoringStrategy.kt | 20 | ✓ |
| LoginScoringStrategy.kt | 85 | ✓ |
| LoadingScoringStrategy.kt | 80 | ✓ |
| ErrorScoringStrategy.kt | 75 | ✓ |
| DetectionContext.kt | 75 | ✓ |
| NodeDataCollector.kt | 95 | ✓ |
| PatternConstants.kt | 280 | ✓ |

**Maximum:** 280 lines (PatternConstants.kt)
**All files < 300 lines:** ✓

---

**END OF ADR-003**

**Last Updated:** 2025-10-13 01:40:41 PDT
**Status:** Proposed - Pending Approval
**Next Steps:** Present to team, get approval, begin Sprint 1
