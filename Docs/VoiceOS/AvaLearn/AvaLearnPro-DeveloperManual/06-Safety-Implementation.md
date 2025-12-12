# Chapter 6: Safety System Implementation

**Document:** VoiceOS-AvaLearnPro-DeveloperManual-Ch06
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 6.1 Safety Architecture

### 6.1.1 Safety Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Safety Manager                            │
│  (Orchestrates all safety checks)                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  DoNotClick  │  │    Login     │  │   Dynamic    │      │
│  │   Manager    │  │   Detector   │  │   Detector   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │     Loop     │  │    Menu      │                        │
│  │  Prevention  │  │  Discovery   │                        │
│  └──────────────┘  └──────────────┘                        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 6.1.2 Safety Check Flow

```kotlin
fun processElement(element: ElementInfo): SafetyResult {
    // 1. DoNotClick check
    val dncReason = doNotClickManager.checkElement(element)
    if (dncReason != null) {
        return SafetyResult.Blocked(dncReason)
    }

    // 2. Dynamic region check
    if (dynamicRegionDetector.isInDynamicRegion(element)) {
        return SafetyResult.SkipDynamic
    }

    // 3. All checks passed
    return SafetyResult.Safe
}

sealed class SafetyResult {
    object Safe : SafetyResult()
    data class Blocked(val reason: DoNotClickReason) : SafetyResult()
    object SkipDynamic : SafetyResult()
    object LoginDetected : SafetyResult()
}
```

---

## 6.2 DoNotClick Implementation

### 6.2.1 Complete DoNotClickManager

```kotlin
package com.augmentalis.learnappcore.safety

class DoNotClickManager {

    data class DNCResult(
        val isBlocked: Boolean,
        val reason: DoNotClickReason?,
        val matchedKeyword: String?,
        val confidence: Float
    )

    fun checkElement(element: ElementInfo): DNCResult {
        val text = element.displayName.lowercase()
        val resourceId = element.resourceId.lowercase()
        val className = element.className.lowercase()

        // Check destructive
        for (keyword in DESTRUCTIVE_KEYWORDS) {
            if (text.contains(keyword) || resourceId.contains(keyword)) {
                return DNCResult(
                    isBlocked = true,
                    reason = DoNotClickReason.DESTRUCTIVE,
                    matchedKeyword = keyword,
                    confidence = calculateConfidence(keyword, text, resourceId)
                )
            }
        }

        // Check financial
        for (keyword in FINANCIAL_KEYWORDS) {
            if (text.contains(keyword) || resourceId.contains(keyword)) {
                return DNCResult(
                    isBlocked = true,
                    reason = DoNotClickReason.FINANCIAL,
                    matchedKeyword = keyword,
                    confidence = calculateConfidence(keyword, text, resourceId)
                )
            }
        }

        // Check account
        for (keyword in ACCOUNT_KEYWORDS) {
            if (text.contains(keyword) || resourceId.contains(keyword)) {
                return DNCResult(
                    isBlocked = true,
                    reason = DoNotClickReason.ACCOUNT,
                    matchedKeyword = keyword,
                    confidence = calculateConfidence(keyword, text, resourceId)
                )
            }
        }

        // Check system
        for (keyword in SYSTEM_KEYWORDS) {
            if (text.contains(keyword) || resourceId.contains(keyword)) {
                return DNCResult(
                    isBlocked = true,
                    reason = DoNotClickReason.SYSTEM,
                    matchedKeyword = keyword,
                    confidence = calculateConfidence(keyword, text, resourceId)
                )
            }
        }

        // Check confirmation dialogs
        if (isConfirmationDialog(element, text)) {
            return DNCResult(
                isBlocked = true,
                reason = DoNotClickReason.CONFIRMATION,
                matchedKeyword = "confirmation",
                confidence = 0.8f
            )
        }

        return DNCResult(
            isBlocked = false,
            reason = null,
            matchedKeyword = null,
            confidence = 0f
        )
    }

    private fun calculateConfidence(
        keyword: String,
        text: String,
        resourceId: String
    ): Float {
        var confidence = 0.5f

        // Exact match in text
        if (text == keyword) confidence += 0.3f

        // Match in resource ID
        if (resourceId.contains(keyword)) confidence += 0.15f

        // Word boundary match
        if (text.contains(" $keyword ") ||
            text.startsWith("$keyword ") ||
            text.endsWith(" $keyword")) {
            confidence += 0.1f
        }

        return confidence.coerceAtMost(1.0f)
    }

    private fun isConfirmationDialog(element: ElementInfo, text: String): Boolean {
        return CONFIRMATION_PATTERNS.any { pattern ->
            text.contains(pattern)
        }
    }

    companion object {
        val DESTRUCTIVE_KEYWORDS = listOf(
            "delete", "remove", "erase", "clear", "wipe",
            "destroy", "discard", "trash", "eliminate", "purge",
            "empty", "reset", "permanently", "forever"
        )

        val FINANCIAL_KEYWORDS = listOf(
            "pay", "purchase", "buy", "subscribe", "checkout",
            "order", "confirm payment", "add to cart",
            "proceed to payment", "complete purchase",
            "submit order", "place order"
        )

        val ACCOUNT_KEYWORDS = listOf(
            "logout", "log out", "sign out", "signout",
            "deactivate", "disable account", "close account",
            "delete account", "remove account", "terminate"
        )

        val SYSTEM_KEYWORDS = listOf(
            "uninstall", "factory reset", "format", "wipe device",
            "reset all", "clear all data", "restore defaults",
            "system reset"
        )

        val CONFIRMATION_PATTERNS = listOf(
            "yes, delete", "confirm delete", "yes, remove",
            "yes, clear", "confirm payment", "yes, uninstall"
        )
    }
}

enum class DoNotClickReason {
    DESTRUCTIVE,
    FINANCIAL,
    ACCOUNT,
    SYSTEM,
    CONFIRMATION
}
```

### 6.2.2 DNC Testing

```kotlin
class DoNotClickManagerTest {

    private val manager = DoNotClickManager()

    @Test
    fun `blocks delete button`() {
        val element = ElementInfo(
            uuid = "test",
            displayName = "Delete Message",
            className = "Button",
            // ... other fields
        )
        val result = manager.checkElement(element)
        assertTrue(result.isBlocked)
        assertEquals(DoNotClickReason.DESTRUCTIVE, result.reason)
    }

    @Test
    fun `blocks pay button`() {
        val element = ElementInfo(
            uuid = "test",
            displayName = "Pay Now",
            className = "Button",
            // ... other fields
        )
        val result = manager.checkElement(element)
        assertTrue(result.isBlocked)
        assertEquals(DoNotClickReason.FINANCIAL, result.reason)
    }

    @Test
    fun `allows normal button`() {
        val element = ElementInfo(
            uuid = "test",
            displayName = "Next",
            className = "Button",
            // ... other fields
        )
        val result = manager.checkElement(element)
        assertFalse(result.isBlocked)
    }
}
```

---

## 6.3 Login Detection Implementation

### 6.3.1 Complete LoginDetector

```kotlin
package com.augmentalis.learnappcore.safety

class LoginDetector {

    data class LoginAnalysis(
        val isLoginScreen: Boolean,
        val loginType: LoginType?,
        val confidence: Float,
        val signals: List<String>
    )

    fun analyze(screenInfo: ScreenInfo): LoginAnalysis {
        var score = 0f
        var detectedType: LoginType? = null
        val signals = mutableListOf<String>()

        // Analyze elements
        screenInfo.elements.forEach { element ->
            val result = analyzeElement(element)
            score += result.score
            if (result.loginType != null && detectedType == null) {
                detectedType = result.loginType
            }
            signals.addAll(result.signals)
        }

        // Analyze activity name
        val activityScore = analyzeActivityName(screenInfo.activityName)
        score += activityScore.score
        signals.addAll(activityScore.signals)

        // Determine if login screen
        val isLogin = score >= LOGIN_THRESHOLD

        return LoginAnalysis(
            isLoginScreen = isLogin,
            loginType = if (isLogin) detectedType ?: LoginType.PASSWORD else null,
            confidence = (score / MAX_SCORE).coerceAtMost(1.0f),
            signals = signals
        )
    }

    private data class ElementAnalysis(
        val score: Float,
        val loginType: LoginType?,
        val signals: List<String>
    )

    private fun analyzeElement(element: ElementInfo): ElementAnalysis {
        var score = 0f
        var loginType: LoginType? = null
        val signals = mutableListOf<String>()

        val text = element.displayName.lowercase()
        val hint = element.contentDescription.lowercase()
        val resourceId = element.resourceId.lowercase()

        // Password field detection
        if (element.className.contains("EditText", ignoreCase = true)) {
            if (resourceId.contains("password") ||
                hint.contains("password") ||
                text.contains("password")) {
                score += 3.0f
                loginType = LoginType.PASSWORD
                signals.add("Password field detected")
            }

            if (resourceId.contains("username") ||
                resourceId.contains("email") ||
                hint.contains("username") ||
                hint.contains("email")) {
                score += 2.0f
                signals.add("Username/email field detected")
            }

            if (resourceId.contains("pin") || hint.contains("pin")) {
                score += 3.0f
                loginType = LoginType.PIN
                signals.add("PIN field detected")
            }
        }

        // Login button detection
        if (element.className.contains("Button", ignoreCase = true)) {
            LOGIN_BUTTON_PATTERNS.forEach { pattern ->
                if (text.contains(pattern)) {
                    score += 2.0f
                    signals.add("Login button: $text")
                }
            }
        }

        // Biometric detection
        if (element.className.contains("Fingerprint", ignoreCase = true) ||
            text.contains("fingerprint") ||
            text.contains("face id") ||
            text.contains("biometric")) {
            score += 3.0f
            loginType = LoginType.BIOMETRIC
            signals.add("Biometric prompt detected")
        }

        return ElementAnalysis(score, loginType, signals)
    }

    private data class ActivityAnalysis(
        val score: Float,
        val signals: List<String>
    )

    private fun analyzeActivityName(activityName: String): ActivityAnalysis {
        val name = activityName.lowercase()
        var score = 0f
        val signals = mutableListOf<String>()

        LOGIN_ACTIVITY_PATTERNS.forEach { pattern ->
            if (name.contains(pattern)) {
                score += 2.0f
                signals.add("Activity name contains: $pattern")
            }
        }

        return ActivityAnalysis(score, signals)
    }

    companion object {
        private const val LOGIN_THRESHOLD = 3.0f
        private const val MAX_SCORE = 10.0f

        val LOGIN_BUTTON_PATTERNS = listOf(
            "login", "log in", "sign in", "signin",
            "authenticate", "verify", "continue to",
            "submit", "enter"
        )

        val LOGIN_ACTIVITY_PATTERNS = listOf(
            "login", "signin", "auth", "credential",
            "password", "security", "verification",
            "authenticate"
        )
    }
}

enum class LoginType {
    PASSWORD,
    BIOMETRIC,
    PIN,
    PATTERN,
    TWO_FACTOR
}
```

---

## 6.4 Loop Prevention Implementation

### 6.4.1 Complete LoopPrevention

```kotlin
package com.augmentalis.learnappcore.safety

class LoopPrevention {

    private val visitCounts = mutableMapOf<String, Int>()
    private val visitHistory = mutableListOf<VisitRecord>()

    data class VisitRecord(
        val screenHash: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class LoopAnalysis(
        val isLoop: Boolean,
        val loopType: LoopType?,
        val visitCount: Int,
        val suggestion: String
    )

    fun recordVisit(screenHash: String): LoopAnalysis {
        // Increment visit count
        val count = visitCounts.getOrDefault(screenHash, 0) + 1
        visitCounts[screenHash] = count

        // Record in history
        visitHistory.add(VisitRecord(screenHash))

        // Trim history
        if (visitHistory.size > MAX_HISTORY) {
            visitHistory.removeAt(0)
        }

        // Check for loops
        return when {
            count > CRITICAL_THRESHOLD -> {
                LoopAnalysis(
                    isLoop = true,
                    loopType = LoopType.CRITICAL,
                    visitCount = count,
                    suggestion = "Stop exploration - screen visited $count times"
                )
            }
            count > WARNING_THRESHOLD -> {
                LoopAnalysis(
                    isLoop = true,
                    loopType = LoopType.WARNING,
                    visitCount = count,
                    suggestion = "Consider skipping this screen"
                )
            }
            detectRapidLoop() -> {
                LoopAnalysis(
                    isLoop = true,
                    loopType = LoopType.RAPID,
                    visitCount = count,
                    suggestion = "Rapid navigation detected - slow down"
                )
            }
            else -> {
                LoopAnalysis(
                    isLoop = false,
                    loopType = null,
                    visitCount = count,
                    suggestion = ""
                )
            }
        }
    }

    private fun detectRapidLoop(): Boolean {
        if (visitHistory.size < RAPID_CHECK_WINDOW) return false

        val recent = visitHistory.takeLast(RAPID_CHECK_WINDOW)
        val timeSpan = recent.last().timestamp - recent.first().timestamp

        // More than X visits in Y seconds
        return timeSpan < RAPID_TIME_THRESHOLD
    }

    fun getVisitCount(screenHash: String): Int {
        return visitCounts[screenHash] ?: 0
    }

    fun getLoopedScreens(): List<String> {
        return visitCounts
            .filter { it.value > WARNING_THRESHOLD }
            .keys
            .toList()
    }

    fun reset() {
        visitCounts.clear()
        visitHistory.clear()
    }

    companion object {
        private const val WARNING_THRESHOLD = 3
        private const val CRITICAL_THRESHOLD = 5
        private const val MAX_HISTORY = 100
        private const val RAPID_CHECK_WINDOW = 10
        private const val RAPID_TIME_THRESHOLD = 5000L  // 5 seconds
    }
}

enum class LoopType {
    WARNING,   // Visited 3-4 times
    CRITICAL,  // Visited 5+ times
    RAPID      // Too many visits too quickly
}
```

---

## 6.5 Dynamic Region Detection

### 6.5.1 DynamicRegionDetector

```kotlin
package com.augmentalis.learnappcore.safety

class DynamicRegionDetector {

    private val knownDynamicRegions = mutableMapOf<String, MutableList<DynamicRegion>>()

    data class DynamicRegion(
        val regionId: String,
        val bounds: Bounds,
        val changeFrequency: Int,
        val lastContent: String,
        val lastObserved: Long = System.currentTimeMillis()
    )

    fun detect(
        previous: ScreenInfo,
        current: ScreenInfo
    ): List<DynamicRegion> {
        if (previous.screenHash != current.screenHash) {
            return emptyList()  // Different screens
        }

        val detectedRegions = mutableListOf<DynamicRegion>()

        // Compare element trees
        val changes = findChangedElements(previous.elements, current.elements)

        changes.forEach { (prevElement, currElement) ->
            // Same position, different content = dynamic
            if (prevElement.bounds == currElement.bounds &&
                prevElement.text != currElement.text) {

                val region = DynamicRegion(
                    regionId = "dr_${prevElement.uuid.take(8)}",
                    bounds = prevElement.bounds,
                    changeFrequency = 1,
                    lastContent = currElement.text
                )

                detectedRegions.add(region)

                // Track for this screen
                val screenRegions = knownDynamicRegions
                    .getOrPut(current.screenHash) { mutableListOf() }
                screenRegions.add(region)
            }
        }

        return detectedRegions
    }

    private fun findChangedElements(
        previous: List<ElementInfo>,
        current: List<ElementInfo>
    ): List<Pair<ElementInfo, ElementInfo>> {
        val changes = mutableListOf<Pair<ElementInfo, ElementInfo>>()

        previous.forEach { prev ->
            val curr = current.find { it.bounds == prev.bounds }
            if (curr != null && prev.text != curr.text) {
                changes.add(prev to curr)
            }
        }

        return changes
    }

    fun isInDynamicRegion(element: ElementInfo): Boolean {
        val screenRegions = knownDynamicRegions[element.screenHash] ?: return false

        return screenRegions.any { region ->
            region.bounds.contains(element.bounds.centerX, element.bounds.centerY)
        }
    }

    fun getDynamicRegions(screenHash: String): List<DynamicRegion> {
        return knownDynamicRegions[screenHash]?.toList() ?: emptyList()
    }

    fun reset() {
        knownDynamicRegions.clear()
    }
}
```

---

## 6.6 Safety Callback Integration

### 6.6.1 Implementing SafetyCallback

```kotlin
class ExplorationActivity : ComponentActivity(), SafetyCallback {

    private lateinit var safetyManager: SafetyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        safetyManager = SafetyManager(callback = this)
    }

    override fun onDangerousElement(
        element: ElementInfo,
        reason: DoNotClickReason
    ) {
        addLog(
            LogLevel.WARN,
            "SAFETY",
            "Blocked ${element.displayName}: ${reason.name}"
        )

        uiState.value = uiState.value.copy(
            dangerousElementsSkipped = uiState.value.dangerousElementsSkipped + 1
        )
    }

    override fun onLoginDetected(loginType: LoginType, screenHash: String) {
        addLog(
            LogLevel.WARN,
            "SAFETY",
            "Login detected: ${loginType.name}"
        )

        uiState.value = uiState.value.copy(
            isOnLoginScreen = true,
            loginType = loginType.name
        )

        // Optionally pause exploration
        if (autohandleLogin) {
            pauseExploration()
        }
    }

    override fun onDynamicRegionConfirmed(region: DynamicRegion) {
        addLog(
            LogLevel.INFO,
            "SAFETY",
            "Dynamic region: ${region.regionId}"
        )

        uiState.value = uiState.value.copy(
            dynamicRegionsDetected = uiState.value.dynamicRegionsDetected + 1
        )
    }

    override fun onLoopDetected(screenHash: String, visitCount: Int) {
        addLog(
            LogLevel.WARN,
            "SAFETY",
            "Loop detected: screen visited $visitCount times"
        )
    }
}
```

---

## 6.7 Next Steps

Continue to [Chapter 7: AVU Export System](./07-AVU-Export-System.md) for export implementation details.

---

**End of Chapter 6**
