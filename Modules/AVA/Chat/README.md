# AVA Chat Module - VisionOS Style

**Apple-inspired conversational interface** with glassmorphism and dual NLU integration

---

## 🎯 Overview

The Chat module provides a modern, iOS Messages-inspired interface for interacting with AVA's AI assistant. Features include:

- **VisionOS Design**: Glassmorphism, depth, blur effects
- **Dual NLU**: MobileBERT (English) + mALBERT (52 languages)
- **Smart Responses**: Intent-based responses with confidence scoring
- **Teach-Ava Integration**: Low-confidence training flow
- **Smooth Animations**: Spring physics, fade/scale effects

---

## 🎨 Design System

### VisionOS/Liquid UI Aesthetic

```
User Message:
┌─────────────────────────────┐
│ Turn on the lights          │  ← iOS blue gradient
│                    10:23 AM │     Right-aligned
└─────────────────────────────┘

Assistant Message:
┌─────────────────────────────┐
│ 💡 Turning on the lights    │  ← Gray gradient
│ ● 92%              10:23 AM │     Left-aligned
└─────────────────────────────┘     Confidence
```

### Key Visual Elements

- **Glassmorphism**: Frosted glass surfaces with semi-transparency
- **Gradients**: Smooth color transitions (blue → teal, gray → white)
- **Shadows**: 4 elevation levels (2dp, 4dp, 8dp, 16dp)
- **Animations**: Spring physics, smooth entrance/exit
- **Typography**: SF Pro-inspired (17sp body, 22sp line height)

---

## 🚀 Quick Start

### Basic Usage

```kotlin
@Composable
fun MyApp() {
    val viewModel = viewModel<ChatViewModel>()

    ChatScreen(
        viewModel = viewModel,
        onNavigateToTeachAva = { /* Navigate to Teach-Ava screen */ }
    )
}
```

### With Navigation

```kotlin
NavHost(navController = navController, startDestination = "chat") {
    composable("chat") {
        ChatScreen(
            viewModel = viewModel(),
            onNavigateToTeachAva = {
                navController.navigate("teachava")
            }
        )
    }
}
```

---

## 📦 Components

### 1. MessageBubble
User and assistant message bubbles with glassmorphism.

```kotlin
MessageBubble(
    message = ChatMessage(
        content = "Turn on the lights",
        role = MessageRole.USER,
        // ...
    ),
    showIntent = false,
    showConfidence = true
)
```

**Features**:
- Animated entrance (fade + scale + slide)
- Confidence indicator (colored dot + percentage)
- Timestamp formatting
- Intent labels (debug mode)

### 2. ChatInputField
Input field with voice and send buttons.

```kotlin
ChatInputField(
    value = inputText,
    onValueChange = { text -> },
    onSend = { /* Send message */ },
    isLoading = false
)
```

**Features**:
- Glassmorphism background
- Animated send button
- Loading indicator
- Voice input button (placeholder)

### 3. ConversationList
Scrollable message list with auto-scroll.

```kotlin
ConversationList(
    messages = messageList,
    isLoading = false,
    showIntent = false,
    showConfidence = true
)
```

**Features**:
- Lazy loading
- Auto-scroll to latest
- Empty state ("Hi, I'm AVA")
- Pull-to-refresh (TODO)

### 4. TeachAvaDialog
Low-confidence dialog prompting user training.

```kotlin
TeachAvaDialog(
    userText = "Turn on the lights",
    onDismiss = { /* Dismiss dialog */ },
    onTeachAva = { /* Navigate to Teach-Ava */ }
)
```

**Features**:
- VisionOS alert style
- Glassmorphism card
- Two-button layout
- Smooth animations

### 5. ChatScreen
Main chat interface with top bar and scaffold.

```kotlin
ChatScreen(
    viewModel = viewModel(),
    onNavigateToTeachAva = { /* Navigate */ }
)
```

**Features**:
- Top bar with model info
- Message list (scrollable)
- Input field (fixed bottom)
- Teach-Ava dialog overlay

---

## 🧠 ViewModel & State

### ChatViewModel

```kotlin
class ChatViewModel(context: Context) : ViewModel() {
    val uiState: StateFlow<ChatUiState>

    fun onInputChange(text: String)
    fun sendMessage()
    fun dismissTeachAvaDialog()
    fun openTeachAva()
}
```

### ChatUiState

```kotlin
data class ChatUiState(
    val messages: List<ChatMessage>,
    val currentInput: String,
    val isLoading: Boolean,
    val error: String?,
    val nluModelInfo: NLUModelMetadata?,
    val showTeachAvaDialog: Boolean,
    val lowConfidenceMessage: ChatMessage?
)
```

---

## 🤖 NLU Integration

### Classification Flow

1. User enters text → `sendMessage()`
2. ViewModel calls `ClassifyIntentUseCase.execute(text, language)`
3. If confidence >= 0.7:
   - Generate response
   - Add assistant message
4. If confidence < 0.7:
   - Show Teach-Ava dialog
   - Prompt user training

### Confidence Threshold

```kotlin
private val lowConfidenceThreshold = 0.7f  // 70%
```

### Response Generation

```kotlin
// Placeholder emoji-based responses
when (intent) {
    "turn_on" -> "💡 Turning on..."
    "play" -> "🎵 Playing..."
    "weather" -> "☁️ Checking the weather..."
    // ...
}
```

**TODO**: Replace with actual intent response generator.

---

## 🎨 Customization

### Colors

```kotlin
object VisionOSColors {
    val UserBubbleLight = Color(0xE0007AFF)  // iOS blue
    val AssistantBubbleLight = Color(0xE0E5E5EA)  // Gray
    val AccentBlue = Color(0xFF007AFF)
    // ...
}
```

### Typography

```kotlin
object VisionOSTypography {
    val MessageBody = TextStyle(
        fontSize = 17.sp,
        lineHeight = 22.sp
    )
    // ...
}
```

### Animations

```kotlin
fadeIn(300ms) +
scaleIn(0.8f → 1.0f) +
slideInHorizontally()
```

---

## 🧪 Testing

### Compose Previews

```kotlin
@Preview
@Composable
fun PreviewMessageBubble() {
    MessageBubble(
        message = sampleMessage,
        showIntent = true
    )
}
```

### Unit Tests (TODO)

```kotlin
class ChatViewModelTest {
    @Test
    fun `sendMessage updates state with user message`()

    @Test
    fun `low confidence triggers teach-ava dialog`()
}
```

---

## 📊 Performance

### Targets
- NLU Classification: <100ms
- UI Render: 60fps (16ms/frame)
- Memory: <512MB peak
- Smooth animations: Spring physics

### Optimization
- Lazy loading (LazyColumn)
- Efficient state updates
- Minimal recomposition
- Hardware acceleration

---

## 🐛 Known Issues

1. **Response Generation**: Placeholder emoji responses (TODO: Connect to device actions)
2. **Conversation Persistence**: No database integration yet (Week 9)
3. **Voice Input**: Placeholder button (VOS4 integration pending)
4. **Device Testing**: Performance metrics are estimates (Week 9 validation)

---

## 📚 Documentation

- **Week 8 Status**: `docs/active/Status-Week8-Complete-251031-1230.md`
- **Week 8 Plan**: `docs/active/Plan-Week8-Chat-UI-251031-1215.md`
- **Week 7 Status**: `docs/active/Status-Week7-Complete-251031-1200.md`
- **NLU Module**: `features/nlu/README.md`

---

## 🔗 Dependencies

```kotlin
// Jetpack Compose
implementation("androidx.compose.ui:ui:1.5.4")
implementation("androidx.compose.material3:material3:1.1.2")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.5")

// NLU Integration
implementation(project(":features:nlu"))
```

---

## 🎯 Next Steps

### Week 9
- Device testing (Pixel 6a, Samsung Galaxy A53)
- Performance validation (NLU, UI, memory, battery)
- Database integration (conversation persistence)
- Polish (haptics, accessibility, dark mode)

### Phase 2
- Voice input (VOS4 integration)
- Intent actions (device controls)
- Multi-language auto-detection
- Advanced animations

---

**Created**: 2025-10-31
**Status**: ✅ Week 8 Complete
**Design**: Apple VisionOS / Liquid UI Inspired
**Author**: AVA Team

**Enjoy the beautiful chat interface! 🎉**
