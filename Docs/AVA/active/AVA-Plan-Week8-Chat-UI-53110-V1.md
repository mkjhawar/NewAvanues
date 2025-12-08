# Plan: Week 8 - Chat UI Integration

**Date**: 2025-10-31 12:15 PDT
**Phase**: Week 8 - Chat UI + NLU Integration
**Duration**: 7 days
**Status**: 🔄 In Progress

---

## 🎯 Objectives

Integrate the dual NLU system (MobileBERT + mALBERT) with a chat-based UI, creating a complete conversational interface for AVA.

**Key Goals**:
1. Build Material 3 chat UI with Jetpack Compose
2. Integrate NLU classification into chat flow
3. Implement low-confidence → Teach-Ava suggestion flow
4. End-to-end testing (UI → NLU → Response)
5. Performance validation on physical devices

---

## 📋 Day-by-Day Breakdown

### Day 1-2: Chat UI Components

#### MessageBubble Component
**File**: `features/chat/src/androidMain/kotlin/.../ui/components/MessageBubble.kt`

**Features**:
- User vs assistant message styling
- Timestamp display
- Confidence indicator (for assistant messages)
- Intent label (optional, for debugging)
- Material 3 design (cards, colors, typography)
- Smooth animations (enter/exit)

**Design**:
```
User Message:
┌─────────────────────────────┐
│ Turn on the lights          │  ← Blue bubble, right-aligned
│                    10:23 AM │
└─────────────────────────────┘

Assistant Message:
┌─────────────────────────────┐
│ 💡 Turning on the lights    │  ← Gray bubble, left-aligned
│ (Intent: turn_on_lights)    │
│ 10:23 AM                    │
└─────────────────────────────┘
```

#### ConversationList Component
**File**: `features/chat/src/androidMain/kotlin/.../ui/components/ConversationList.kt`

**Features**:
- LazyColumn for efficient scrolling
- Auto-scroll to latest message
- Pull-to-refresh (load history)
- Empty state (welcome message)
- Loading indicator

#### ChatInputField Component
**File**: `features/chat/src/androidMain/kotlin/.../ui/components/ChatInputField.kt`

**Features**:
- Text field with send button
- Voice input button (placeholder for Phase 2)
- Character counter (optional)
- Loading state (while classifying)
- Error state
- Keyboard actions (send on enter)

#### ChatScreen Composable
**File**: `features/chat/src/androidMain/kotlin/.../ui/ChatScreen.kt`

**Features**:
- Main chat screen layout
- Scaffold with top bar
- Message list
- Input field at bottom
- ViewModel integration
- Navigation support

### Day 3-4: NLU Integration

#### ChatViewModel
**File**: `features/chat/src/androidMain/kotlin/.../viewmodel/ChatViewModel.kt`

**Responsibilities**:
- Manage chat state (messages, loading, errors)
- Call `ClassifyIntentUseCase` for NLU
- Handle classification results
- Update UI state
- Persist conversation (via repository)

**State Management**:
```kotlin
data class ChatUiState(
    val messages: List<Message>,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentInput: String = "",
    val nluModelInfo: NLUModelMetadata? = null
)
```

#### NLU Classification Flow
```
User Input
    ↓
ChatViewModel.sendMessage()
    ↓
ClassifyIntentUseCase.execute()
    ↓
┌─────────────────────┬──────────────────────┐
│ High Confidence     │ Low Confidence       │
│ (>0.7)              │ (<0.7)               │
├─────────────────────┼──────────────────────┤
│ Execute Intent      │ Show Teach-Ava       │
│ Show Response       │ Suggestion Dialog    │
└─────────────────────┴──────────────────────┘
```

#### Intent Response Generator
**File**: `features/chat/src/androidMain/kotlin/.../domain/IntentResponseGenerator.kt`

**Purpose**: Map intents to user-friendly responses

**Examples**:
- `turn_on_lights` → "💡 Turning on the lights"
- `play_music` → "🎵 Playing music"
- `weather_query` → "☁️ Checking the weather..."

### Day 5: Teach-Ava Integration

#### Teach-Ava Dialog
**File**: `features/chat/src/androidMain/kotlin/.../ui/components/TeachAvaDialog.kt`

**Trigger**: Classification confidence <0.7

**UI Flow**:
```
┌─────────────────────────────────────────┐
│ 🤔 I'm not sure what you mean          │
│                                          │
│ You said: "Turn on the lights"          │
│                                          │
│ Did you mean one of these?              │
│  ○ Turn on lights                       │
│  ○ Turn off lights                      │
│  ○ Dim lights                           │
│  ○ Something else                       │
│                                          │
│ [Cancel]              [Teach AVA →]     │
└─────────────────────────────────────────┘
```

**Features**:
- Show original user text
- Suggest top 3 candidate intents
- Link to Teach-Ava screen
- Option to dismiss
- Save correction to training examples

### Day 6: Testing

#### Unit Tests
**File**: `features/chat/src/test/.../ChatViewModelTest.kt`

**Test Coverage**:
- Message sending
- NLU classification integration
- State updates
- Error handling
- Confidence threshold logic

#### UI Tests
**File**: `features/chat/src/androidTest/.../ChatScreenTest.kt`

**Test Coverage**:
- Message rendering (user vs assistant)
- Input field interaction
- Send button click
- Loading states
- Error states
- Empty state

#### Integration Tests
**File**: `features/chat/src/androidTest/.../ChatNLUIntegrationTest.kt`

**Test Coverage**:
- End-to-end: user input → NLU → response
- Multilingual input (Full variant)
- Low-confidence Teach-Ava trigger
- Conversation persistence

### Day 7: Performance & Polish

#### Performance Validation
1. **Measure Inference Time**:
   - MobileBERT: Target <50ms
   - mALBERT: Target <80ms
   - Device: Pixel 6a, Samsung Galaxy A53

2. **Measure End-to-End Time**:
   - User input → Response: Target <500ms
   - Breakdown: Input (0ms) → NLU (50-80ms) → Response (10ms) → UI (10ms)

3. **Memory Profiling**:
   - Peak memory usage
   - Memory leaks check
   - GC pressure

4. **Battery Usage**:
   - Active conversation (10 min)
   - Idle with app open
   - Background usage

#### Polish
- Smooth animations
- Haptic feedback
- Accessibility (TalkBack, large text)
- Dark mode support
- Error messages (user-friendly)
- Empty states
- Loading skeletons

---

## 📁 Module Structure

```
features/chat/
├── src/
│   ├── androidMain/kotlin/
│   │   └── com/augmentalis/ava/features/chat/
│   │       ├── ui/
│   │       │   ├── ChatScreen.kt
│   │       │   └── components/
│   │       │       ├── MessageBubble.kt
│   │       │       ├── ConversationList.kt
│   │       │       ├── ChatInputField.kt
│   │       │       └── TeachAvaDialog.kt
│   │       ├── viewmodel/
│   │       │   └── ChatViewModel.kt
│   │       ├── domain/
│   │       │   ├── IntentResponseGenerator.kt
│   │       │   └── SendMessageUseCase.kt
│   │       └── navigation/
│   │           └── ChatNavigation.kt
│   ├── commonMain/kotlin/
│   │   └── com/augmentalis/ava/features/chat/
│   │       └── domain/
│   │           └── models/
│   │               └── ChatMessage.kt
│   ├── test/kotlin/
│   │   └── ChatViewModelTest.kt
│   └── androidTest/kotlin/
│       ├── ChatScreenTest.kt
│       └── ChatNLUIntegrationTest.kt
├── build.gradle.kts
└── README.md
```

---

## 🎨 UI Design

### Color Scheme (Material 3)

```kotlin
// User messages
UserMessageBackground = MaterialTheme.colorScheme.primaryContainer
UserMessageText = MaterialTheme.colorScheme.onPrimaryContainer

// Assistant messages
AssistantMessageBackground = MaterialTheme.colorScheme.secondaryContainer
AssistantMessageText = MaterialTheme.colorScheme.onSecondaryContainer

// Confidence indicators
HighConfidence = MaterialTheme.colorScheme.primary (>0.7)
MediumConfidence = MaterialTheme.colorScheme.tertiary (0.5-0.7)
LowConfidence = MaterialTheme.colorScheme.error (<0.5)
```

### Typography

```kotlin
// Message text
MessageBody = MaterialTheme.typography.bodyLarge

// Timestamp
Timestamp = MaterialTheme.typography.labelSmall

// Intent label
IntentLabel = MaterialTheme.typography.labelMedium
```

---

## 🔌 Dependencies

### New Dependencies (add to `features/chat/build.gradle.kts`)

```kotlin
dependencies {
    // Existing projects
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":data"))
    implementation(project(":features:nlu"))

    // Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")

    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
```

---

## 🎯 Acceptance Criteria

### Functional Requirements ✅
- [ ] User can send text messages
- [ ] Assistant responds with intent classification
- [ ] Messages display with correct styling (user vs assistant)
- [ ] Confidence scores visible (optional debug mode)
- [ ] Low-confidence triggers Teach-Ava dialog
- [ ] Conversation persists across app restarts
- [ ] Empty state shows welcome message
- [ ] Error states handled gracefully

### Performance Requirements ✅
- [ ] NLU classification: <100ms (max)
- [ ] End-to-end response: <500ms
- [ ] UI animations: 60fps
- [ ] Memory usage: <512MB peak
- [ ] No memory leaks
- [ ] Smooth scrolling (large conversations)

### Quality Requirements ✅
- [ ] Unit test coverage: ≥80%
- [ ] UI tests cover main flows
- [ ] Integration tests validate end-to-end
- [ ] Accessibility: TalkBack support
- [ ] Dark mode support
- [ ] Responsive layout (various screen sizes)

---

## 🧪 Testing Strategy

### Unit Tests (ViewModel)
```kotlin
class ChatViewModelTest {
    @Test
    fun `sendMessage updates state with user message`()

    @Test
    fun `classification success adds assistant response`()

    @Test
    fun `classification error shows error message`()

    @Test
    fun `low confidence triggers teach-ava dialog`()

    @Test
    fun `conversation persists via repository`()
}
```

### UI Tests (Compose)
```kotlin
class ChatScreenTest {
    @Test
    fun `displays welcome message when empty`()

    @Test
    fun `sends message on button click`()

    @Test
    fun `displays user and assistant messages correctly`()

    @Test
    fun `shows loading indicator while classifying`()
}
```

### Integration Tests
```kotlin
class ChatNLUIntegrationTest {
    @Test
    fun `end-to-end chat flow with NLU classification`()

    @Test
    fun `multilingual input works in Full variant`()

    @Test
    fun `teach-ava dialog appears for low confidence`()
}
```

---

## 📊 Performance Budgets

| Metric | Budget | Measurement |
|--------|--------|-------------|
| NLU Inference | <100ms | Measure in ChatViewModel |
| End-to-End | <500ms | User input → UI update |
| UI Render | 16ms/frame | Android Profiler |
| Memory (Idle) | <200MB | Android Profiler |
| Memory (Active) | <512MB | Android Profiler |
| Battery (10min) | <5% | Battery Historian |

---

## 🚀 Deployment

### Build Commands

```bash
# Build with Lite variant (MobileBERT)
./gradlew :features:chat:assembleLiteDebug

# Build with Full variant (mALBERT)
./gradlew :features:chat:assembleFullDebug

# Run tests
./gradlew :features:chat:testDebugUnitTest
./gradlew :features:chat:connectedDebugAndroidTest

# Install on device
./gradlew installLiteDebug
adb shell am start -n com.augmentalis.ava/.MainActivity
```

---

## 📝 Implementation Checklist

### Day 1-2: UI Components
- [ ] Create `MessageBubble.kt`
- [ ] Create `ConversationList.kt`
- [ ] Create `ChatInputField.kt`
- [ ] Create `ChatScreen.kt`
- [ ] Design system (colors, typography, shapes)
- [ ] Preview functions for UI testing

### Day 3-4: NLU Integration
- [ ] Create `ChatViewModel.kt`
- [ ] Implement state management
- [ ] Integrate `ClassifyIntentUseCase`
- [ ] Create `IntentResponseGenerator.kt`
- [ ] Handle loading/error states
- [ ] Persist conversations

### Day 5: Teach-Ava
- [ ] Create `TeachAvaDialog.kt`
- [ ] Implement confidence threshold logic
- [ ] Link to Teach-Ava screen
- [ ] Save user corrections

### Day 6: Testing
- [ ] Write ViewModel unit tests (10+)
- [ ] Write UI tests (8+)
- [ ] Write integration tests (5+)
- [ ] Manual testing on devices

### Day 7: Performance & Polish
- [ ] Performance profiling
- [ ] Memory leak detection
- [ ] Battery usage testing
- [ ] Accessibility audit
- [ ] Dark mode testing
- [ ] Bug fixes

---

## 🔗 References

### Internal Docs
- Week 7 Status: `docs/active/Status-Week7-Complete-251031-1200.md`
- NLU Module: `features/nlu/README.md`
- Dual NLU Plan: `docs/active/Plan-Dual-NLU-Strategy-251031-0050.md`

### External Resources
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material 3 Design](https://m3.material.io/)
- [ViewModel Guide](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [Compose Testing](https://developer.android.com/jetpack/compose/testing)

---

**Created**: 2025-10-31 12:15 PDT
**Status**: 🔄 In Progress
**Author**: AVA Team
