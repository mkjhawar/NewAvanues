# ContextualUIManager Integration Guide

**Purpose**: Instructions for integrating app-aware contextual assistance
**Source**: AVA2 codebase (Apr 2025)
**Target**: AVA AI (Week 11-12 integration)
**Priority**: P1 (Unique Differentiator - Proactive Assistance)

---

## Overview

ContextualUIManager provides intelligent, app-aware assistance by:
- **Detecting current app** user is using
- **Suggesting contextual actions** relevant to that app
- **Displaying overlay UI elements** (chips, buttons, cards, mini-controls)
- **Adapting to user behavior** patterns

**Key Value**: Proactive assistance based on real-time context - unique differentiator for AVA AI.

---

## Source Files Location

**Base Path**: `/Users/manoj_mbpm14/Downloads/Coding/Ava-AI/AVA2/app/src/main/java/com/augmentalis/ava/`

### Core Files

**ContextualUIManager (Two Locations - Use ui/components/ version):**
```
ui/components/ContextualUIManager.kt        # Main implementation (399 lines)
ui/ContextualUIManager.kt                   # Duplicate/older version
```

**Recommendation**: Use `ui/components/ContextualUIManager.kt` (more complete).

### Supporting Files (Assumed, Not Listed)

**Likely exists:**
```
data/models/
├── AppContext.kt                           # Current app context model
└── ContextualAction.kt                     # Action definition

data/repositories/
└── AppUsageRepository.kt                   # Track app usage patterns

service/
└── AppMonitorService.kt                    # Background app detection service

di/
└── ContextualUIModule.kt                   # Hilt DI module
```

---

## ContextualUIManager.kt - Detailed Breakdown

### Key Classes

#### ElementType (Enum)
```kotlin
enum class ElementType {
    SUGGESTION_CHIP,       // Small suggestion chip with text
    ACTION_BUTTON,         // Action button with icon
    CONTEXT_CARD,          // Card with detailed contextual information
    MINI_CONTROL           // Small control for quick actions
}
```

#### ContextualElement (Data Class)
```kotlin
data class ContextualElement(
    val id: String,                    // Unique identifier
    val type: ElementType,             // UI element type
    val text: String,                  // Display text
    val iconResId: Int? = null,        // Optional icon resource
    val action: () -> Unit,            // Action to perform
    val priority: Int = 0,             // Higher priority = shown first
    val appPackage: String? = null     // Specific app this element is for
)
```

#### ContextualUIManager (Main Class)
```kotlin
class ContextualUIManager(
    private val context: Context
) : DefaultLifecycleObserver {

    // Properties
    private val scope: CoroutineScope
    private val windowManager: WindowManager
    private val activeElements: MutableMap<String, View>
    private var containerView: FrameLayout?

    // State flows
    val currentAppPackage: StateFlow<String?>
    val screenContext: StateFlow<Map<String, Any>>

    // Methods
    fun updateCurrentApp(packageName: String?)
    fun updateScreenContext(contextData: Map<String, Any>)
    fun showElement(element: ContextualElement)
    fun showElements(elements: List<ContextualElement>)
    fun removeElement(elementId: String)
    fun clearElements()
    fun createAppSpecificElements(packageName: String): List<ContextualElement>
}
```

### Key Methods

**1. updateCurrentApp(packageName) - Lines 93-95**
- Updates current foreground app
- Triggers element refresh via Flow

**2. updateScreenContext(contextData) - Lines 101-106**
- Updates screen context metadata
- Merges with existing context data

**3. showElement(element) - Lines 111-125**
- Creates view for element
- Adds to overlay container
- Tracks in activeElements map

**4. showElements(elements) - Lines 130-137**
- Batch display of multiple elements
- Sorts by priority (descending)
- Clears old elements first

**5. createAppSpecificElements(packageName) - Lines 160-171**
- Factory method for app-specific actions
- Returns relevant ContextualElement list
- Supports: Maps, Spotify, Camera, Dialer, generic

**6. App-Specific Element Creators** - Lines 173-265
- `createMapContextElements()` - "Navigate home", "Navigate to work"
- `createMusicContextElements()` - "Play/Pause", "Next Track"
- `createCameraContextElements()` - "Set timer"
- `createPhoneContextElements()` - "Speaker", "Mute"
- `createGenericContextElements()` - "How can I help?"

**7. setupContainer() - Lines 268-297**
- Creates overlay FrameLayout
- Adds to WindowManager
- Positions at bottom-right corner
- Requires SYSTEM_ALERT_WINDOW permission

**8. monitorContextChanges() - Lines 330-344**
- Observes currentAppPackage Flow
- Debounces for 300ms (prevents rapid updates)
- Auto-refreshes elements on app change

**9. ScreenContextExtractor (Nested Class) - Lines 362-398**
- Extracts context from Activity
- Gets package name, activity name, window properties
- Counts view hierarchy size

---

## Integration Steps (Week 11-12)

### Step 1: Copy ContextualUIManager to AVA AI

```bash
# Create contextual UI feature module
mkdir -p "/Volumes/M Drive/Coding/AVA AI/features/contextual/src/main/java/com/augmentalis/ava/features/contextual"

# Copy main file
cp "/Users/manoj_mbpm14/Downloads/Coding/Ava-AI/AVA2/app/src/main/java/com/augmentalis/ava/ui/components/ContextualUIManager.kt" \
   "/Volumes/M Drive/Coding/AVA AI/features/contextual/src/main/java/com/augmentalis/ava/features/contextual/"
```

### Step 2: Update Package Names
Change:
```kotlin
package com.augmentalis.ava.ui.components
```
To:
```kotlin
package com.augmentalis.ava.features.contextual
```

### Step 3: Create Contextual Module build.gradle.kts

```kotlin
// features/contextual/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.augmentalis.ava.features.contextual"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core modules
    implementation(project(":core:common"))
    implementation(project(":core:domain"))

    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-common:2.6.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")

    // Timber (logging)
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
}
```

### Step 4: Add to settings.gradle
```kotlin
// settings.gradle
include(":features:contextual")
```

### Step 5: Add System Alert Window Permission

```xml
<!-- platform/app/src/main/AndroidManifest.xml -->
<manifest>
    <!-- Required for overlay UI -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <!-- Optional: For detecting current app (requires usage stats permission) -->
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
        tools:ignore="ProtectedPermissions" />

    <application>
        <!-- ... -->
    </application>
</manifest>
```

### Step 6: Request Overlay Permission at Runtime

```kotlin
// features/contextual/src/main/java/com/augmentalis/ava/features/contextual/PermissionHelper.kt
object PermissionHelper {
    fun requestOverlayPermission(activity: Activity) {
        if (!Settings.canDrawOverlays(activity)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY)
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private const val REQUEST_CODE_OVERLAY = 1234
}
```

### Step 7: Create App Detection Service

```kotlin
// features/contextual/src/main/java/com/augmentalis/ava/features/contextual/AppDetectionService.kt
class AppDetectionService(
    private val context: Context,
    private val contextualUIManager: ContextualUIManager
) {
    private val usageStatsManager = context.getSystemService(
        Context.USAGE_STATS_SERVICE
    ) as UsageStatsManager

    fun startMonitoring() {
        scope.launch {
            while (isActive) {
                val currentApp = getCurrentForegroundApp()
                contextualUIManager.updateCurrentApp(currentApp)
                delay(1000)  // Check every second
            }
        }
    }

    private fun getCurrentForegroundApp(): String? {
        val time = System.currentTimeMillis()
        val usageEvents = usageStatsManager.queryEvents(
            time - 1000,  // Last 1 second
            time
        )

        var lastEvent: UsageEvents.Event? = null
        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)

            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastEvent = event
            }
        }

        return lastEvent?.packageName
    }
}
```

### Step 8: Extend App-Specific Element Providers

**Add AVA-Specific Apps:**

```kotlin
// features/contextual/src/main/java/com/augmentalis/ava/features/contextual/AppElementProviders.kt
fun ContextualUIManager.createExtendedAppElements(packageName: String): List<ContextualElement> {
    return when (packageName) {
        // Existing apps (Maps, Spotify, Camera, Dialer)
        "com.google.android.apps.maps" -> createMapContextElements()
        "com.spotify.music" -> createMusicContextElements()
        "com.android.camera" -> createCameraContextElements()
        "com.android.dialer" -> createPhoneContextElements()

        // NEW: AVA-specific integrations
        "com.google.android.gm" -> createGmailContextElements()  // Email
        "com.google.android.calendar" -> createCalendarContextElements()  // Calendar
        "com.whatsapp" -> createMessagingContextElements()  // Messaging
        "com.android.chrome" -> createBrowserContextElements()  // Browser
        "com.amazon.kindle" -> createReadingContextElements()  // Reading
        "com.android.settings" -> createSettingsContextElements()  // Settings

        // Generic
        else -> createGenericContextElements()
    }
}

// Gmail context
private fun createGmailContextElements(): List<ContextualElement> {
    return listOf(
        ContextualElement(
            id = "gmail_compose",
            type = ElementType.ACTION_BUTTON,
            text = "Compose email",
            priority = 5,
            action = { /* Open compose via intent */ }
        ),
        ContextualElement(
            id = "gmail_check",
            type = ElementType.SUGGESTION_CHIP,
            text = "Check unread emails",
            priority = 4,
            action = { /* Query Gmail API */ }
        )
    )
}

// Calendar context
private fun createCalendarContextElements(): List<ContextualElement> {
    return listOf(
        ContextualElement(
            id = "calendar_next",
            type = ElementType.CONTEXT_CARD,
            text = "Next meeting at 2 PM",
            priority = 5,
            action = { /* Show meeting details */ }
        ),
        ContextualElement(
            id = "calendar_add",
            type = ElementType.ACTION_BUTTON,
            text = "Create event",
            priority = 4,
            action = { /* Open event creation */ }
        )
    )
}

// Browser context
private fun createBrowserContextElements(): List<ContextualElement> {
    return listOf(
        ContextualElement(
            id = "browser_read",
            type = ElementType.ACTION_BUTTON,
            text = "Read page aloud",
            priority = 5,
            action = { /* Use TTS to read page */ }
        ),
        ContextualElement(
            id = "browser_search",
            type = ElementType.SUGGESTION_CHIP,
            text = "Search with AVA",
            priority = 4,
            action = { /* Voice search */ }
        )
    )
}
```

### Step 9: Integrate with Chat UI

```kotlin
// features/chat/src/main/java/com/augmentalis/ava/features/chat/ChatViewModel.kt
class ChatViewModel(
    private val contextualUIManager: ContextualUIManager,
    private val appDetectionService: AppDetectionService
) : ViewModel() {

    init {
        // Start monitoring current app
        appDetectionService.startMonitoring()

        // Observe contextual elements
        viewModelScope.launch {
            contextualUIManager.currentAppPackage.collect { packageName ->
                packageName?.let {
                    // Update suggested actions in chat
                    val suggestions = contextualUIManager.createAppSpecificElements(it)
                    _suggestedActions.value = suggestions.map { it.text }
                }
            }
        }
    }

    fun handleSuggestedAction(actionText: String) {
        // Find matching element and execute action
        val element = currentElements.find { it.text == actionText }
        element?.action?.invoke()
    }
}
```

### Step 10: Create Smart Glasses Adaptation

```kotlin
// features/contextual/src/main/java/com/augmentalis/ava/features/contextual/SmartGlassesAdapter.kt
class SmartGlassesAdapter(
    private val contextualUIManager: ContextualUIManager
) {
    fun adaptForSmartGlasses(elements: List<ContextualElement>): List<ContextualElement> {
        // For smart glasses, convert visual elements to voice announcements
        return elements.map { element ->
            element.copy(
                // Simplify text for voice
                text = simplifyForVoice(element.text),
                // Remove icons (not visible on audio-only devices)
                iconResId = null
            )
        }
    }

    private fun simplifyForVoice(text: String): String {
        // Remove UI-specific language
        return text
            .replace("Navigate to", "Go to")
            .replace("Set", "")
            .trim()
    }

    fun announceElements(elements: List<ContextualElement>) {
        // Use TTS to announce available actions
        val announcement = "Available actions: ${elements.joinToString(", ") { it.text }}"
        // TTS.speak(announcement)
    }
}
```

---

## Use Cases for AVA AI

### 1. Proactive Email Assistance
**Context**: User opens Gmail
**AVA**: Shows "Compose email", "Check unread emails" chips
**User**: Taps "Check unread emails" or says "Check unread"
**AVA**: Queries Gmail API, reads unread email subjects

### 2. Navigation Quick Actions
**Context**: User opens Google Maps
**AVA**: Shows "Navigate home", "Navigate to work" buttons
**User**: Taps "Navigate home"
**AVA**: Starts navigation via Maps intent

### 3. Music Control
**Context**: User opens Spotify
**AVA**: Shows "Play/Pause", "Next Track" mini-controls
**User**: Can control music without unlocking phone

### 4. Meeting Reminders
**Context**: User opens Calendar
**AVA**: Shows "Next meeting at 2 PM" card
**User**: Taps to see details
**AVA**: Shows meeting info, participants, location

### 5. Reading Assistance
**Context**: User opens browser
**AVA**: Shows "Read page aloud" action
**User**: Activates action
**AVA**: Uses TTS to read webpage content

---

## Overlay UI Customization

### Visual Design (for sighted users)

**Glassmorphism Style** (matches VoiceAvenue MagicDreamTheme):
```kotlin
// features/contextual/src/main/java/com/augmentalis/ava/features/contextual/ui/OverlayStyles.kt
fun View.applyGlassmorphism() {
    // Semi-transparent background
    background = GradientDrawable().apply {
        setColor(Color.parseColor("#80FFFFFF"))  // 50% white
        cornerRadius = 16f
    }

    // Blur effect (requires RenderEffect API 31+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setRenderEffect(
            RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
        )
    }

    // Elevation
    elevation = 8f
}
```

### Voice-Only Mode (for smart glasses)

```kotlin
// Disable visual overlays, use voice announcements only
val isVoiceOnlyMode = appPreferences.getSmartGlassesMode() == GlassesMode.AUDIO_ONLY

if (isVoiceOnlyMode) {
    // Don't show overlay
    contextualUIManager.clearElements()

    // Announce actions via TTS
    smartGlassesAdapter.announceElements(elements)
} else {
    // Show visual overlay
    contextualUIManager.showElements(elements)
}
```

---

## Privacy Considerations

### 1. App Usage Tracking
**Concern**: Requires PACKAGE_USAGE_STATS permission (sensitive)
**Mitigation**:
- Request permission with clear explanation
- Allow users to disable app detection
- Don't log app usage history (only current app)

### 2. Screen Context Extraction
**Concern**: Reading view hierarchy may capture sensitive data
**Mitigation**:
- Only extract metadata (view count, activity name)
- Don't capture text content or screenshots
- Respect app-specific privacy flags

### 3. Overlay Permission
**Concern**: SYSTEM_ALERT_WINDOW can be abused
**Mitigation**:
- Only show overlays when AVA is active
- Respect user's "Do Not Disturb" mode
- Auto-hide overlays after timeout

---

## Testing Checklist

- [ ] Overlay permission requested and granted
- [ ] App detection service detects foreground app
- [ ] Elements update when app changes (debounced)
- [ ] Action buttons trigger correct intents
- [ ] Overlay doesn't block critical UI elements
- [ ] Memory usage is minimal (<50MB for overlay)
- [ ] Works on smart glasses (voice-only mode)
- [ ] Respects user preferences (enable/disable per app)

---

## Performance Targets

| Metric | Target | Notes |
|--------|--------|-------|
| App Detection Latency | <1s | Update every 1 second |
| Element Refresh Time | <300ms | Debounced via Flow |
| Memory Overhead | <50MB | For overlay + manager |
| Battery Impact | <5%/hour | Additional drain from monitoring |

---

## Dependencies Required

```kotlin
// AndroidX
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Hilt (DI)
implementation("com.google.dagger:hilt-android:2.48")
ksp("com.google.dagger:hilt-compiler:2.48")
```

---

## Migration Strategy

**Phase 1** (Week 11): Copy ContextualUIManager, set up module
**Phase 2** (Week 11): Implement app detection service
**Phase 3** (Week 11): Request overlay permission, test on phone
**Phase 4** (Week 12): Add 10+ app-specific element providers
**Phase 5** (Week 12): Integrate with smart glasses (voice-only mode)
**Phase 6** (Week 12): User preference system (enable/disable per app)

---

## Future Enhancements

### 1. Machine Learning-Based Suggestions
- Learn user behavior patterns
- Predict next action based on time/location/app
- Suggest actions before user asks

### 2. Multi-App Workflows
- Detect cross-app workflows ("Book Uber after reading calendar")
- Suggest chained actions

### 3. Contextual RAG Integration
- Use local RAG to answer app-specific questions
- Example: "Summarize this email thread" (Gmail context)

### 4. Gesture Control (Smart Glasses)
- Detect head gestures (nod = confirm)
- Eye tracking (gaze at action to select)

---

## References

- ContextualUIManager Source: `/Users/manoj_mbpm14/Downloads/Coding/Ava-AI/AVA2/app/src/main/java/com/augmentalis/ava/ui/components/ContextualUIManager.kt`
- Android Overlay Docs: https://developer.android.com/develop/ui/views/layout/overlay
- Usage Stats API: https://developer.android.com/reference/android/app/usage/UsageStatsManager
- Legacy Analysis: `docs/active/Analysis-Legacy-Codebases-251030-0210.md`

---

**Created**: 2025-10-30 02:25 PDT
**Next Review**: After ARManager integration (Week 11)
**Priority**: Integrate in Week 11-12 (after AR)

Created by Manoj Jhawar, manoj@ideahq.net
