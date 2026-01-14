# AVA AI Overlay System — Design & Implementation Document

**Project**: AVA AI Assistant
**Feature**: Transparent AI Overlay System
**Author**: Manoj Jhawar (manoj@ideahq.net)
**Created**: 2025-11-01
**Status**: Design → Implementation
**Protocol**: IdeaCode

---

## 1. Executive Summary

Transform AVA from a standalone chat application into a **universal AI overlay** that floats transparently over any running Android application. This overlay provides contextual assistance, voice interaction, and quick actions without disrupting the user's current task.

**Core Value Proposition**:
- Users can access AVA's intelligence from any app
- Context-aware suggestions based on active application
- Non-intrusive glassmorphic design matching AVA's VisionOS aesthetic
- Voice-first interaction with visual feedback
- Zero-friction activation (voice, gesture, or quick tap)

---

## 2. Architecture Overview

### 2.1 Module Structure

```
AVA-AI/
├── features/
│   └── overlay/          ← NEW MODULE
│       ├── service/
│       │   ├── OverlayService.kt
│       │   ├── OverlayPermissionActivity.kt
│       │   └── OverlayNotificationManager.kt
│       ├── ui/
│       │   ├── OverlayComposables.kt
│       │   ├── GlassMorphicPanel.kt
│       │   ├── VoiceOrb.kt
│       │   └── SuggestionChips.kt
│       ├── theme/
│       │   ├── OverlayTheme.kt
│       │   ├── GlassEffects.kt
│       │   └── AnimationSpecs.kt
│       ├── controller/
│       │   ├── OverlayController.kt
│       │   ├── VoiceRecognizer.kt
│       │   └── GestureHandler.kt
│       ├── integration/
│       │   ├── AvaIntegrationBridge.kt
│       │   ├── NluConnector.kt
│       │   ├── ChatConnector.kt
│       │   └── ContextEngine.kt
│       └── data/
│           ├── OverlayPreferences.kt
│           └── ContextData.kt
```

### 2.2 Data Flow Architecture

```
User Interaction (Voice/Touch)
        ↓
VoiceRecognizer / GestureHandler
        ↓
OverlayController (State Management)
        ↓
AvaIntegrationBridge
        ↓
┌─────────────┬─────────────┬─────────────┐
│             │             │             │
NluConnector  ChatConnector ContextEngine
│             │             │             │
features:nlu  features:chat Active App Context
        ↓           ↓             ↓
IntentClassification + Response Generation
        ↓
OverlayUI Update (Glass Panel + Suggestions)
```

### 2.3 Integration Points

| Component | Integration | Purpose |
|-----------|-------------|---------|
| **NLU Connector** | `features:nlu` → IntentClassifier | Classify user voice input |
| **Chat Connector** | `features:chat` → ChatViewModel | Generate AI responses |
| **Context Engine** | Android AccessibilityService | Detect active app context |
| **Theme System** | AVA's existing Material3 theme | Consistent glassmorphic styling |
| **Data Layer** | `core:domain` + `core:data` | Persist overlay preferences |

---

## 3. Design Specifications

### 3.1 Visual Design — Glassmorphic Overlay

**Design Philosophy**: VisionOS-inspired transparent glass with adaptive blur

| Element | Specification | Rationale |
|---------|---------------|-----------|
| **Base Opacity** | 0.75-0.85 (collapsed), 0.65-0.75 (expanded) | Maintain readability while seeing underlying app |
| **Blur Radius** | 24-30px | Soft depth without heavy GPU load |
| **Corner Radius** | 24dp | Match AVA's existing message bubbles |
| **Shadow** | 0dp offset, 10dp blur, 25% alpha | Subtle elevation |
| **Border** | 1dp, white 20% opacity | Glass edge definition |
| **Accent Color** | Adaptive from Material3 primary | Match AVA's brand |
| **Animation** | 220ms ease-out cubic | Smooth, responsive feel |

### 3.2 Component Specifications

#### Voice Orb (Collapsed State)
```
Size: 64dp diameter
Position: Draggable, starts at (24dp, 320dp)
Background: Glass panel with 0.8 alpha
Icon: Microphone (Material Icons)
States:
  - Idle: Static icon, gentle pulse
  - Listening: Animated waveform
  - Processing: Rotating spinner
  - Speaking: Pulsing glow
Interaction: Tap to expand, long-press for settings, drag to reposition
```

#### Glass Context Panel (Expanded State)
```
Width: MATCH_PARENT - 32dp margin
Height: WRAP_CONTENT (max 60% screen height)
Layout:
  ┌─────────────────────────────────┐
  │ [Icon] AVA Assistant      [×]  │ ← Title bar (56dp height)
  ├─────────────────────────────────┤
  │ Transcript/Response text...    │ ← Content area (flexible)
  │                                 │
  ├─────────────────────────────────┤
  │ [Copy] [Translate] [Search]    │ ← Suggestion chips (48dp height)
  └─────────────────────────────────┘
Padding: 16dp all sides
Item spacing: 12dp vertical, 8dp horizontal
```

#### Suggestion Chips
```
Style: Outlined AssistChip (Material3)
Height: 32dp
Padding: 12dp horizontal
Border: Accent color with 70% alpha
Background: Transparent
Text: Body small, white 90% alpha
Spacing: 8dp horizontal gap in FlowRow
Max visible: 4 chips, horizontal scroll if more
```

### 3.3 Behavior Modes

| Mode | Trigger | Visual State | Auto-hide |
|------|---------|--------------|-----------|
| **Docked** | App start | Voice orb only, 64dp bubble | Never |
| **Listening** | Voice/tap activation | Orb pulsing + "Listening..." hint | 30s idle |
| **Responding** | NLU processing complete | Panel expands, shows response | 45s idle |
| **Minimized** | Close button | Collapse to orb with fade | Immediate |
| **Dragging** | User drag gesture | Orb follows touch, semi-transparent | On release |

### 3.4 Permissions Required

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE"/>
```

---

## 4. Implementation Phases

### Phase 1: Core Infrastructure (Week 1)
**Goal**: Functional overlay service with basic UI

**Tasks**:
1. Create `features:overlay` module in Gradle
2. Implement `OverlayService` with foreground notification
3. Implement `OverlayPermissionActivity` for permission flow
4. Create basic `OverlayComposables` with voice orb
5. Add draggable touch handling via `GestureHandler`
6. Wire up `VoiceRecognizer` with Android SpeechRecognizer API

**Deliverables**:
- ✅ Overlay appears on screen
- ✅ Voice orb is draggable
- ✅ Tap orb triggers voice recognition
- ✅ Permissions flow works correctly

**Acceptance Criteria**:
- `./gradlew assembleDebug` builds successfully
- Overlay appears over other apps when service starts
- Voice input is captured and logged
- No crashes, no memory leaks

---

### Phase 2: Glassmorphic UI (Week 2)
**Goal**: Beautiful, production-ready overlay UI

**Tasks**:
1. Extract existing glass theme patterns from `features:chat`
2. Create `GlassEffects.kt` with blur, shadow, border modifiers
3. Implement `GlassMorphicPanel.kt` with expand/collapse animation
4. Create `SuggestionChips.kt` with Material3 AssistChip styling
5. Add `AnimationSpecs.kt` for consistent motion design
6. Implement adaptive opacity based on underlying app brightness

**Deliverables**:
- ✅ Glass panel matches AVA's VisionOS aesthetic
- ✅ Smooth expand/collapse animations
- ✅ Suggestion chips render correctly
- ✅ Orb pulse animation during listening

**Acceptance Criteria**:
- Visual design approved by UX review
- Animations are 60fps on mid-range devices
- No visual glitches during state transitions
- Accessible with TalkBack enabled

---

### Phase 3: AVA Integration (Week 3)
**Goal**: Connect overlay to AVA's NLU and Chat systems

**Tasks**:
1. Create `AvaIntegrationBridge.kt` as central integration point
2. Implement `NluConnector.kt` to use `features:nlu` IntentClassifier
3. Implement `ChatConnector.kt` to invoke `features:chat` ChatViewModel
4. Wire voice input → NLU → Chat → UI update flow
5. Add error handling for NLU failures
6. Implement response streaming if ChatViewModel supports it

**Deliverables**:
- ✅ Voice input is classified by AVA's NLU
- ✅ Responses appear in overlay panel
- ✅ Suggestions are contextual to classified intent
- ✅ Error states are handled gracefully

**Acceptance Criteria**:
- "Open settings" correctly triggers Settings app
- "What's the weather?" generates appropriate response
- Invalid/unknown input shows helpful error message
- Integration doesn't break existing chat functionality

---

### Phase 4: Context Engine (Week 4)
**Goal**: Make overlay contextually aware of active app

**Tasks**:
1. Implement `ContextEngine.kt` to detect active app
2. Add optional AccessibilityService integration for text extraction
3. Create context-specific suggestion templates
4. Implement smart fallback when context unavailable
5. Add privacy controls for context access

**Deliverables**:
- ✅ Overlay knows which app is active
- ✅ Suggestions adapt to app context (e.g., "Translate this" in Chrome)
- ✅ Privacy settings control context access
- ✅ Works gracefully without accessibility permission

**Acceptance Criteria**:
- Context detection works on 90% of popular apps
- No performance degradation from context monitoring
- User can disable context features entirely
- Complies with Android privacy guidelines

---

## 5. Technical Specifications

### 5.1 Android Overlay Implementation

**Window Type**: `TYPE_APPLICATION_OVERLAY` (API 26+)

```kotlin
WindowManager.LayoutParams(
    width = MATCH_PARENT,
    height = MATCH_PARENT,
    type = if (Build.VERSION.SDK_INT >= O)
        TYPE_APPLICATION_OVERLAY
      else
        TYPE_PHONE,
    flags = FLAG_NOT_FOCUSABLE or
            FLAG_LAYOUT_IN_SCREEN or
            FLAG_HARDWARE_ACCELERATED,
    format = PixelFormat.TRANSLUCENT
).apply {
    gravity = Gravity.TOP or Gravity.START
}
```

**Performance Constraints**:
- Launch time: < 200ms from service start
- Animation frame rate: 60fps minimum
- Memory footprint: < 50MB
- CPU usage: < 5% idle, < 15% active

### 5.2 Voice Recognition

**Engine**: Android SpeechRecognizer with partial results

```kotlin
RecognizerIntent(ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM)
    putExtra(EXTRA_PARTIAL_RESULTS, true)
    putExtra(EXTRA_MAX_RESULTS, 1)
    putExtra(EXTRA_LANGUAGE, "en-US")
}
```

**Handling**:
- Partial results update UI live
- Final result triggers NLU classification
- Timeout: 30 seconds max listening
- Auto-stop on silence detection

### 5.3 State Management

**Flow-based architecture** using Kotlin StateFlow:

```kotlin
class OverlayController {
    private val _state = MutableStateFlow(OverlayState.Docked)
    val state: StateFlow<OverlayState> = _state.asStateFlow()

    private val _transcript = MutableStateFlow<String?>(null)
    val transcript: StateFlow<String?> = _transcript

    private val _response = MutableStateFlow<String?>(null)
    val response: StateFlow<String?> = _response

    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions
}
```

**States**:
- `Docked`: Orb only, idle state
- `Listening`: Voice active, waveform animation
- `Processing`: NLU classification in progress
- `Responding`: Showing AI response
- `Error`: Error message displayed

### 5.4 Dependency Injection

**Integration with existing AVA architecture**:

```kotlin
// Overlay module depends on:
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:common"))
    implementation(project(":features:nlu"))
    implementation(project(":features:chat"))

    // Android overlay essentials
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-service")

    // Voice recognition
    // Uses system SpeechRecognizer - no extra deps
}
```

---

## 6. User Experience Flow

### 6.1 First-Time Setup

1. User opens AVA app
2. Settings screen offers "Enable AI Overlay"
3. Tap → `OverlayPermissionActivity` launches
4. Request SYSTEM_ALERT_WINDOW permission
5. Request RECORD_AUDIO permission
6. Start `OverlayService` in foreground
7. Voice orb appears at default position (24dp, 320dp)
8. Quick tutorial tooltip: "Tap for voice, drag to move"

### 6.2 Daily Usage — Voice Interaction

**Scenario**: User is browsing Chrome, wants to translate selected text

1. User says "Hey AVA" or taps voice orb
2. Orb pulses → "Listening..." hint appears
3. User: "Translate this to Spanish"
4. Orb switches to processing spinner
5. NLU classifies intent: `translate_text`
6. Panel expands showing:
   - Transcript: "Translate this to Spanish"
   - Response: "I'll translate the selected text. Which text would you like translated?"
   - Suggestions: [Translate] [Copy Translation] [Close]
7. User taps [Translate] chip → action executes
8. After 45s idle → panel auto-collapses to orb

### 6.3 Daily Usage — Contextual Suggestions

**Scenario**: User receives WhatsApp message while gaming

1. WhatsApp notification appears
2. Context Engine detects messaging app activity
3. Orb shows subtle badge (1 unread)
4. User taps orb
5. Panel shows context-aware suggestions:
   - "Reply to [Contact]"
   - "Read message aloud"
   - "Remind me in 10 minutes"
6. User selects action → overlay executes it
7. Returns to game seamlessly

---

## 7. Privacy & Security

### 7.1 Data Handling

| Data Type | Usage | Storage | Sharing |
|-----------|-------|---------|---------|
| Voice input | NLU classification | In-memory only | Never |
| Transcripts | UI display | Cleared on close | Never |
| Active app name | Context suggestions | Session only | Never |
| Screen text (Accessibility) | Contextual analysis | In-memory only | Never |
| Overlay position | User preference | Local DataStore | Never |

**Privacy Guarantees**:
- No cloud transmission of overlay data
- All NLU processing is on-device
- Screen text never logged or persisted
- User can disable context features entirely

### 7.2 Permission Justification

| Permission | Why Needed | User-Facing Explanation |
|------------|------------|-------------------------|
| SYSTEM_ALERT_WINDOW | Display overlay over apps | "Show AVA's assistant on top of other apps" |
| RECORD_AUDIO | Voice commands | "Listen to your voice commands" |
| POST_NOTIFICATIONS | Foreground service notification | "Keep AVA's assistant running in background" |
| BIND_ACCESSIBILITY_SERVICE | (Optional) Screen text | "Understand what's on your screen for better suggestions" |

---

## 8. Testing Strategy

### 8.1 Unit Tests

**Coverage Target**: 80% code coverage

- `OverlayControllerTest` — State management
- `GestureHandlerTest` — Drag and tap detection
- `NluConnectorTest` — Intent classification flow
- `ChatConnectorTest` — Response generation
- `ContextEngineTest` — App detection logic

### 8.2 Integration Tests

- Overlay service lifecycle (start, stop, restart)
- Permission flow (grant, deny, revoke)
- Voice recognition end-to-end
- NLU → Chat → UI pipeline
- Theme consistency across states

### 8.3 UI Tests

- Orb draggability on various screen sizes
- Panel expansion/collapse animations
- Suggestion chip interactions
- Accessibility compliance (TalkBack)
- Performance on low-end devices

### 8.4 Manual QA Checklist

- [ ] Overlay appears over 20+ popular apps
- [ ] No interference with underlying app touch events
- [ ] Voice recognition works in noisy environments
- [ ] Battery impact < 5% over 8 hours
- [ ] No memory leaks after 100 open/close cycles
- [ ] Graceful degradation when permissions denied

---

## 9. Success Metrics

### 9.1 Technical Metrics

- **Launch Time**: < 200ms (target: 150ms)
- **Frame Rate**: 60fps (99th percentile)
- **Memory**: < 50MB (target: 35MB)
- **CPU**: < 5% idle, < 15% active
- **Battery**: < 2% drain per hour

### 9.2 User Metrics

- **Activation Rate**: 60% of AVA users enable overlay within 7 days
- **Daily Usage**: Average 10+ overlay interactions per active user
- **Voice Success Rate**: 85% of voice commands correctly understood
- **Retention**: 70% of users keep overlay enabled after 30 days

---

## 10. Future Enhancements

**Post-Launch Roadmap**:

1. **Smart Glasses Integration** (Q2 2026)
   - AR world-anchored UI
   - Gaze-based selection
   - Spatial audio feedback

2. **Proactive Suggestions** (Q3 2026)
   - ML-based prediction of user needs
   - "You might want to..." prompts
   - Calendar/location awareness

3. **Multi-Language Support** (Q4 2026)
   - Leverage mALBERT's 52-language support
   - Auto-detect user language
   - Real-time translation in overlay

4. **Developer API** (2027)
   - Third-party apps can register overlay actions
   - Custom suggestion providers
   - White-label overlay SDK

---

## 11. Appendix

### 11.1 File Naming Convention

All files follow IdeaCode protocol:

```
filename: ComponentName.kt
created: YYYY-MM-DD HH:MM:SS TZ
author: Manoj Jhawar
© Augmentalis Inc, Intelligent Devices LLC
TCR: [status]
agent: [role] | mode: [PLAN|ACT|DEFEND]
```

### 11.2 References

- [Android Overlay Permission](https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_OVERLAY_PERMISSION)
- [SpeechRecognizer API](https://developer.android.com/reference/android/speech/SpeechRecognizer)
- [Material3 Glassmorphism](https://m3.material.io/styles/color/dynamic-color/overview)
- [AVA NLU Architecture](../architecture/NLU-Design.md)
- [AVA Chat System](../architecture/Chat-Design.md)

---

**Document Status**: ✅ Design Complete → Ready for Implementation
**Next Step**: Create `features:overlay` module structure
**Assigned**: Manoj Jhawar
**Target Completion**: 2025-11-22

---

*This document is a living specification. Updates will be versioned via Git commits.*
