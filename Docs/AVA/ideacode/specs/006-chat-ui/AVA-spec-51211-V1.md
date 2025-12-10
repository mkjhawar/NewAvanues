# Feature Specification: Chat UI with NLU Integration

**Feature ID**: 006-chat-ui
**Version**: 1.1.0 (Remediated)
**Created**: 2025-01-28
**Updated**: 2025-11-12 (Feature 007 Remediation)
**Profile**: android-app
**Complexity**: Tier 3 (4+ hours, 6+ files, high risk)
**Phase**: 1.0 - MVP (Week 6-7)

---

## Executive Summary

Implement the core conversational UI for AVA AI, enabling users to chat with AVA via text and voice, with real-time NLU intent classification, confidence-based feedback, and integrated Teach-AVA training flow. This feature completes the end-to-end user experience: user input → NLU classification → response generation → optional training.

**Key Differentiators**:
- Voice-first design with live transcription and confidence indicators
- Transparent AI decision-making (shows intent + confidence)
- Seamless Teach-AVA integration (low confidence → auto-prompt to teach)
- Privacy-first (95%+ local processing, cloud opt-in)

---

## Problem Statement

### Current State (Week 5)

**What Exists**:
- ✅ Database layer (Conversation, Message entities)
- ✅ ONNX NLU engine (MobileBERT INT8, 25.5 MB)
- ✅ Teach-AVA UI (standalone screen for training examples)
- ✅ 92% test coverage, Clean Architecture validated

**What's Missing**:
- ❌ No chat interface (users can't interact with AVA)
- ❌ No NLU integration in UI (classification happens but nowhere to trigger it)
- ❌ No Teach-AVA integration from conversations (can't teach from low-confidence messages)
- ❌ No conversation history access (database has data but no UI)
- ❌ No voice input (constitution mentions voice-first, but no speech integration)

### Pain Points

1. **No User Interaction**: Users can't test the NLU system they're training
2. **Disconnected Experience**: Teach-AVA is separate from conversations (no context)
3. **Hidden AI Decisions**: Users don't see what AVA understood (confidence, intent)
4. **No Feedback Loop**: Can't correct AVA when she misunderstands

### Desired State (Week 6 Complete)

**User Experience**:
1. User opens AVA → jumps into conversation (recent or new, based on setting)
2. User types/speaks message → AVA shows live transcription + confidence
3. AVA classifies intent → responds with template + confidence badge
4. Low confidence → AVA auto-prompts "Would you like to teach me?"
5. User can long-press any message → "Teach AVA this"
6. Teach-AVA bottom sheet appears → user corrects intent → saves to training
7. User says "show history" → side panel overlay appears → navigate conversations

**Technical Flow**:
```
User Input (text/voice)
    ↓
BertTokenizer (tokenize utterance)
    ↓
IntentClassifier (classify with ONNX)
    ↓
Result<IntentClassification>
    ↓
if (confidence >= 0.7) → Template response (high confidence)
else if (confidence >= 0.5) → Confirm + Template (medium)
else → Auto-prompt "Teach AVA" (low confidence)
    ↓
Save Message to Room DB
    ↓
Display in Chat UI with confidence badge
```

---

## Requirements

### Functional Requirements

#### FR-006-001: Message Bubbles with Confidence Indicators

**Description**: Display user and AVA messages in a scrollable chat interface with visual confidence indicators.

**Acceptance Criteria**:
- **AC-001**: User messages appear right-aligned with blue background
- **AC-002**: AVA messages appear left-aligned with grey background
- **AC-003**: AVA messages display confidence badge (format: emoji + percentage):
  - 🟢 Green badge (>70%): "🟢 85%" format, no action needed
  - 🟡 Yellow badge (50-70%): "🟡 65%" format + "Confirm?" button
  - 🔴 Red badge (<50%): "🔴 35%" format + "Teach AVA" button
- **AC-004**: Messages scroll smoothly (LazyColumn with reverse layout)
- **AC-005**: Latest message visible at bottom (auto-scroll on new message)
- **AC-006**: Timestamps shown in human-readable format ("2 minutes ago", "Yesterday 3:15 PM")

**Dependencies**:
- Conversation/Message repositories (Week 3-4)
- IntentClassifier confidence scores (Week 5)

**Priority**: P0 (Critical)

---

#### FR-006-002: Text Input with NLU Classification

**Description**: Allow users to type messages, classify intent using ONNX NLU, and save to database.

**Acceptance Criteria**:
- **AC-001**: Text input field at bottom of screen (OutlinedTextField)
- **AC-002**: Send button (enabled only when text non-empty)
- **AC-003**: On send:
  1. Create user message → save to Room DB
  2. Tokenize utterance (BertTokenizer)
  3. Classify intent (IntentClassifier with candidate intents)
  4. Generate response (template-based)
  5. Create AVA message with confidence → save to Room DB
  6. Display both messages in chat
- **AC-004**: Input field clears after send
- **AC-005**: Classification completes in <100ms (on-device)
- **AC-006**: Candidate intents loaded from:
  1. Built-in intents (hardcoded list)
  2. User-taught intents (TrainExample table, unique intents)
  3. Hybrid: combine both, deduplicate
- **AC-007**: Handle errors gracefully (show error message if NLU fails)

**Intent Response Templates**:

*See plan.md Technical Decisions section for complete template mapping.*
*Implementation source of truth: `IntentTemplates.kt`*

**Priority**: P0 (Critical)

---

#### FR-006-003: Auto-Prompt on Low Confidence

**Description**: When NLU confidence is <0.5, automatically prompt user to teach AVA.

**Acceptance Criteria**:
- **AC-001**: AVA message with confidence <50% displays:
  - Text: "I'm not sure I understood. Would you like to teach me?"
  - Red badge: 🔴 with percentage
  - Button: "Teach AVA" (prominent, primary color)
- **AC-002**: Tapping "Teach AVA" opens Teach-AVA bottom sheet (FR-006-004)
- **AC-003**: User can dismiss prompt (no teaching required)
- **AC-004**: Threshold configurable (default 0.5, stored in preferences)

**Priority**: P0 (Critical)

---

#### FR-006-004: Teach-AVA Bottom Sheet Integration

**Description**: Allow users to correct AVA's understanding via bottom sheet overlay from any message.

**Acceptance Criteria**:
- **AC-001**: Long-press any message → context menu appears
- **AC-002**: Context menu options:
  - "Teach AVA this" (always available)
  - "Copy text" (standard Android)
  - "Delete message" (user messages only)
- **AC-003**: Tapping "Teach AVA" or auto-prompt button → opens bottom sheet
- **AC-004**: Bottom sheet UI (33% screen height, max 40%):
  - Header: "🎓 Teach AVA"
  - Section 1: "You said: [user utterance]"
  - Section 2: "I understood: [detected intent] ([confidence]%)"
  - Section 3: "What did you mean?"
    - List of suggested intents (from TrainExample + built-in, top 5)
    - Text field: "Or type a new intent..."
  - Actions: [Cancel] [Save]
- **AC-005**: Chat history remains visible above bottom sheet
- **AC-006**: On save:
  1. Create TrainExample entry (utterance, intent, locale, hash)
  2. Show success toast: "AVA learned: [utterance] → [intent]"
  3. Dismiss bottom sheet
  4. Optionally re-classify message with new training data
- **AC-007**: Voice command alternative: "Teach AVA that '[utterance]' means '[intent]'"
  - AVA confirms: "Got it. I've learned this."
  - Saves to TrainExample table
  - No UI interaction needed

**Dependencies**:
- TrainExampleRepository (Week 3-4)
- MD5 hash deduplication (Week 3-4)

**Priority**: P0 (Critical)

---

#### FR-006-005: Voice Input with Live Transcription (Optional Week 6)

**Description**: Enable voice input with live transcription and real-time confidence feedback.

**Acceptance Criteria**:
- **AC-001**: Floating microphone button (always visible, bottom-right)
- **AC-002**: Tap microphone → voice input modal appears
- **AC-003**: Voice input modal UI:
  - Pulsing microphone animation (blue)
  - Waveform visualization (placeholder bars, simple animation - no external library required)
  - Live transcription: "Turn on the lights" (updates as user speaks)
  - Real-time confidence: "🟢 High (85%)" (updates during speech)
- **AC-004**: On speech end (silence detection or stop button):
  1. Finalize transcription
  2. Process same as text input (FR-006-002)
  3. Dismiss voice modal
  4. Show message in chat
- **AC-005**: Speech recognition via VOS4 SpeechRecognitionManager:
  - Vosk (offline, privacy-first)
  - AndroidSTT (fallback)
  - Vivoka (optional, if available)
- **AC-006**: Handle errors:
  - No speech detected: "I didn't hear anything. Try again?"
  - Recognition failed: Fall back to text input

**Dependencies**:
- VOS4 SpeechRecognitionManager (external/vos4)
- Phase 4 VOS4 integration (may defer to Week 8)

**Priority**: P1 (High, but can defer if VOS4 not ready)

---

#### FR-006-006: Conversation History Overlay

**Description**: Show conversation history in a side panel overlay, accessible via voice command.

**Acceptance Criteria**:
- **AC-001**: Voice trigger: "AVA, show history" or "show transcript"
- **AC-002**: Side panel slides in from right:
  - Width: 25% screen width (vertical side panel)
  - Header: "📜 History"
  - Scrollable list of conversations (sorted by updatedAt desc)
- **AC-003**: Conversation list items display:
  - Title (if set, else "Conversation at [timestamp]")
  - Last message preview (first 50 characters)
  - Timestamp (relative: "2 hours ago", "Yesterday")
  - Active indicator (✓ if current conversation)
- **AC-004**: Tap conversation → switch to that conversation
- **AC-005**: Voice navigation:
  - "Previous conversation" → load older conversation
  - "Next conversation" → load newer conversation
  - "Close history" → dismiss overlay
- **AC-006**: Swipe right on overlay → dismiss (returns to chat)

**Priority**: P0 (Critical, user requirement)

---

#### FR-006-007: Conversation Mode Setting

**Description**: Allow users to control conversation behavior (append vs new).

**Acceptance Criteria**:
- **AC-001**: Existing SettingsScreen (`apps/ava-standalone/.../ui/settings/SettingsScreen.kt`) includes "Conversation Mode" option
- **AC-002**: Three modes:
  - "Append to recent" (default): Always append to last conversation
  - "New conversation each session": Create new on app launch
  - "Ask me each time": Prompt user on app open
- **AC-003**: Voice command: "New conversation" → starts fresh conversation
- **AC-004**: Active conversation ID stored in SharedPreferences
- **AC-005**: On app launch:
  - Mode "Append": Load most recent conversation (by updatedAt)
  - Mode "New": Create new conversation, load it
  - Mode "Ask": Show dialog with [Continue Recent] [Start New]

**Priority**: P1 (High)

---

### Non-Functional Requirements

#### NFR-006-001: Performance

**Requirements**:
- **NLU Classification**:
  - **Target (p50)**: <50ms (ideal performance, 50th percentile latency)
  - **Maximum (p95)**: <100ms (acceptance threshold, 95th percentile latency)
  - **Failure threshold**: p95 ≥100ms blocks task completion
- **Message Rendering**: 60 FPS scroll (LazyColumn optimization)
- **Database Queries**: <40ms for last 100 messages (validated Week 3-4)
- **Voice Recognition**: <2 seconds latency (speech end → transcription shown)
- **End-to-End**: <500ms (user sends → AVA responds displayed)

**Validation**:
- Run on low-end device (2GB RAM)
- Measure with Android Profiler (CPU, memory, network)
- Log both p50 and p95 latency (100 classification runs, cold/warm states)
- **Success criteria**: p50 <50ms AND p95 <100ms
- **Acceptable**: p95 <100ms (warn if p50 >50ms)
- **Failure**: p95 ≥100ms

---

#### NFR-006-002: Accessibility

**Requirements**:
- **Screen Reader**: All UI elements have contentDescription
- **Touch Targets**: Minimum 48dp (Material Design guidelines)
- **Color Contrast**: WCAG 2.1 AA compliance (4.5:1 text, 3:1 UI)
- **Font Scaling**: Support up to 200% font size (test with accessibility scanner)
- **Voice Control**: All actions accessible via voice (Teach AVA, navigation, send)

**Validation**:
- Run Accessibility Scanner (Android Studio)
- Test with TalkBack enabled
- Manual testing with large fonts (Settings → Display → Font size)

---

#### NFR-006-003: Privacy

**Requirements**:
- **95%+ Local Processing**:
  - NLU classification: 100% local (ONNX Runtime)
  - Message storage: 100% local (Room database)
  - Voice recognition: 100% local (Vosk via VOS4)
  - No cloud by default
- **Cloud Opt-In** (Phase 6):
  - User must explicitly enable cloud sync
  - Clear consent dialog explaining what's synced
  - Easy to disable (Settings → Privacy → Cloud Sync)
- **No Telemetry**: No analytics, no crash reporting (Phase 1)

**Validation**:
- Network monitor shows 0 outgoing requests (Phase 1)
- All data in `adb shell run-as com.augmentalis.ava`

---

#### NFR-006-004: Testing

**Requirements**:
- **Test Coverage**: 80%+ (enforced by IDEACODE profile)
- **Unit Tests**:
  - ChatViewModel logic (message handling, state management)
  - Intent template mapping
  - Confidence threshold logic
- **Integration Tests**:
  - End-to-end: User input → NLU → Response → DB save → UI display
  - Teach-AVA flow: Low confidence → prompt → save training example
  - Conversation switching (history overlay)
- **UI Tests** (Compose Testing):
  - Message bubbles render correctly
  - Input field + send button work
  - Bottom sheet appears on long-press
  - History overlay slides in/out

**Validation**:
- `./gradlew testDebugUnitTestCoverage` shows 80%+
- All tests pass: `./gradlew test connectedAndroidTest`

---

### Success Criteria

**Week 6 Complete When**:
1. ✅ User can send text message → AVA responds with intent classification
2. ✅ Confidence badges visible (green/yellow/red) on AVA messages
3. ✅ Low confidence (<50%) auto-prompts "Teach AVA"
4. ✅ Long-press message → "Teach AVA" bottom sheet
5. ✅ Training example saved → improves future classifications
6. ✅ Voice command "show history" → side panel with conversations
7. ✅ Navigate conversations via "previous/next conversation"
8. ✅ End-to-end <500ms (user send → AVA response displayed)
9. ✅ 80%+ test coverage maintained
10. ✅ All acceptance criteria met (verified by tests)

---

## User Stories

### Story 1: Basic Chat Interaction

**As a** user
**I want to** send a message to AVA and get a response
**So that** I can test if AVA understands my commands

**Acceptance Criteria**:
- Given I'm on the chat screen
- When I type "Turn on the lights" and tap send
- Then I see my message appear in the chat (blue bubble, right-aligned)
- And I see AVA's response "I'll control the lights." (grey bubble, left-aligned)
- And the response shows a green badge "🟢 85%" (high confidence)
- And the entire interaction completes in <500ms

---

### Story 2: Teaching AVA from Low Confidence

**As a** user
**I want to** teach AVA when she doesn't understand me
**So that** she improves over time

**Acceptance Criteria**:
- Given I send a message "Open my schedule"
- When AVA classifies with low confidence (<50%)
- Then I see AVA's message "I'm not sure I understood. Would you like to teach me?"
- And I see a red badge "🔴 35%" with a "Teach AVA" button
- When I tap "Teach AVA"
- Then a bottom sheet appears showing:
  - "You said: Open my schedule"
  - "I understood: OPEN_CALENDAR (35%)"
  - "What did you mean?"
  - List of suggested intents + text input
- When I select "SHOW_AGENDA" and tap "Save"
- Then I see a success toast "AVA learned: Open my schedule → SHOW_AGENDA"
- And the training example is saved to the database
- And next time I say "Open my schedule", AVA classifies as SHOW_AGENDA with higher confidence

---

### Story 3: Correcting High Confidence Mistakes

**As a** user
**I want to** correct AVA even when she's confident
**So that** I can fix mistakes before they become habits

**Acceptance Criteria**:
- Given I send "Set a timer for 10 minutes"
- When AVA responds with high confidence (>70%) but wrong intent
- Then I can long-press AVA's message
- And a context menu appears with "Teach AVA this"
- When I tap "Teach AVA this"
- Then the Teach-AVA bottom sheet opens (same as Story 2)
- And I can correct the intent even though confidence was high

---

### Story 4: Voice Input with Transcription

**As a** user
**I want to** use voice input instead of typing
**So that** I can interact hands-free (voice-first design)

**Acceptance Criteria**:
- Given I'm on the chat screen
- When I tap the microphone button
- Then a voice input modal appears with:
  - Pulsing microphone animation
  - Waveform showing my voice
  - Live transcription updating as I speak
  - Real-time confidence indicator
- When I say "Turn on the lights" and stop speaking
- Then the modal processes my speech (silence detection)
- And the transcription is sent as a message (same flow as text input)
- And the modal dismisses
- And I see the conversation continue as normal

---

### Story 5: Accessing Conversation History

**As a** user
**I want to** view my past conversations
**So that** I can reference previous interactions or continue old chats

**Acceptance Criteria**:
- Given I have 5+ past conversations in the database
- When I say "AVA, show history" or tap a history button
- Then a side panel slides in from the right (25% screen width)
- And I see a scrollable list of conversations showing:
  - Title or timestamp
  - Last message preview
  - Active indicator (✓ on current conversation)
- When I tap a conversation
- Then the main chat switches to that conversation
- And the history panel remains open (can switch again)
- When I say "Close history" or swipe right on the panel
- Then the panel slides out

---

## Technical Constraints

### Android Platform

**Requirements** (from android-app profile):
- API Level: 24-34 (Android 7.0-14)
- Min SDK: 24 (Nougat, 84% market coverage)
- Target SDK: 34 (Android 14, latest)
- Jetpack Compose: Material 3
- Kotlin: 1.9.0+
- Gradle: 8.5+ (version catalog)

### ONNX Runtime

**Requirements** (from Week 5):
- ONNX Runtime Mobile: 1.17.0
- Model: MobileBERT INT8 (25.5 MB)
- Inference target: <50ms (device-dependent)
- Hardware acceleration: NNAPI enabled (GPU/NPU offload)

### Room Database

**Requirements** (from Week 3-4):
- Room: 2.6.1 with KSP
- Entities: Conversation, Message
- Performance: <40ms for 100 message query (validated)
- Migrations: Not needed (fresh install)

### VOS4 Integration (Optional Week 6)

**Requirements** (from Constitution):
- VOS4 submodule: `external/vos4`
- Speech recognition: VOS4 SpeechRecognitionManager
- Vosk (offline), AndroidSTT (fallback), Vivoka (optional)
- Phase 4 integration: Full plugin mode
- Phase 1: Standalone mode (may use VOS4 components if available)

---

## Dependencies

### Internal Dependencies (AVA AI)

- ✅ **Conversation/Message repositories** (Week 3-4, 100% complete)
- ✅ **TrainExample repository** (Week 3-4, 100% complete)
- ✅ **ONNX NLU classifier** (Week 5, 100% complete)
- ✅ **BertTokenizer** (Week 5, 100% complete)
- ✅ **ModelManager** (Week 5, 100% complete)
- ⏳ **VOS4 SpeechRecognitionManager** (Phase 4, optional for Week 6)

### External Dependencies

- ✅ **ONNX Runtime Mobile** 1.17.0 (configured)
- ✅ **Room** 2.6.1 with KSP (configured)
- ✅ **Jetpack Compose** Material 3 (configured)
- ⏳ **VOS4** (submodule, optional for voice input)

---

## Out of Scope (Week 6)

**Deferred to Future Phases**:

1. **LLM-Generated Responses** (Week 9):
   - MLC LLM + Gemma 2B integration
   - Natural language responses (vs templates)
   - Verbose mode (user setting for detailed responses)

2. **Auto-Generated Conversation Titles** (Week 9):
   - LLM summarization of conversation
   - Auto-title on conversation end
   - Currently: Title remains null or user-set

3. **Smart Glasses UI** (Phase 5):
   - Minimal HUD (80x40dp top-right)
   - Voice-only mode (no touch)
   - Haptic feedback for low confidence

4. **Cloud Sync** (Phase 6):
   - Supabase integration
   - Cross-device conversation sync
   - Multi-tenant support

5. **Constitutional AI** (Phase 3):
   - Self-critique system
   - 7 principles evaluation
   - >90% adherence scoring

6. **Advanced Voice Features** (Phase 4+):
   - Wake word detection ("Hey AVA")
   - Emotion detection in voice
   - Interrupt capability (cut off AVA mid-response)

7. **Data Export** (Phase 1.1 / Week 8):
   - Export conversations to JSON format
   - User-triggered from settings
   - Privacy compliance (local file only)

---

## Risks & Mitigation

### Risk 1: Voice Input Not Ready (VOS4 Dependency)

**Probability**: Medium
**Impact**: Low (text input sufficient for Week 6)

**Mitigation**:
- Make voice input optional (FR-006-005 is P1, not P0)
- Week 6 focuses on text input + NLU integration
- Voice input can be added in Week 8 if VOS4 ready

---

### Risk 2: NLU Performance on Low-End Devices

**Probability**: Medium
**Impact**: High (user experience)

**Mitigation**:
- Test on physical device (2GB RAM) early in Week 6
- If inference >100ms, optimize:
  - Reduce max sequence length (128 → 64 tokens)
  - Use INT8 quantization (already done)
  - Enable NNAPI (already configured)
- Fallback: Show "Processing..." indicator for slow devices

---

### Risk 3: Teach-AVA UI Complexity

**Probability**: Low
**Impact**: Medium (user confusion)

**Mitigation**:
- Follow established patterns (bottom sheet from research)
- User testing with 3-5 external testers (Week 7)
- Iterate on feedback before Week 8
- Voice command alternative (lower complexity)

---

## Implementation Notes

### Phased Approach (Week 6 → Week 7)

**Day 1-2** (UI Foundation):
- Message bubbles with LazyColumn
- Text input field + send button
- Basic message display (no NLU yet)

**Day 3-4** (NLU Integration):
- Connect IntentClassifier to send button
- Implement confidence badges
- Template-based responses
- Auto-prompt on low confidence

**Day 5-6** (Teach-AVA Integration):
- Long-press context menu
- Teach-AVA bottom sheet
- Save to TrainExample repository
- Voice command: "Teach AVA that..."

**Day 7** (History Overlay):
- Side panel conversation list
- Voice trigger: "show history"
- Navigation: previous/next conversation
- Conversation switching

**Week 7** (Polish + Testing):
- Voice input (if VOS4 ready)
- User settings (conversation mode)
- Performance optimization
- Integration tests
- Bug fixes

---

## Testing Strategy

### Unit Tests (ChatViewModel)

```kotlin
@Test
fun `when user sends message with high confidence, show green badge`() {
    // Given: Mock classifier returns 0.85 confidence
    val classifier = mockk<IntentClassifier> {
        every { classifyIntent(any(), any()) } returns Result.Success(
            IntentClassification("control_lights", 0.85f, 45L)
        )
    }

    // When: User sends "Turn on lights"
    viewModel.sendMessage("Turn on lights")

    // Then: AVA message shows green badge
    val messages = viewModel.messages.value
    assertEquals(2, messages.size) // User + AVA
    assertEquals(0.85f, messages[1].confidence)
    assertEquals(ConfidenceBadge.GREEN, messages[1].badge)
}

@Test
fun `when confidence below 0.5, auto-prompt teach AVA`() {
    // Given: Mock classifier returns 0.35 confidence
    val classifier = mockk<IntentClassifier> {
        every { classifyIntent(any(), any()) } returns Result.Success(
            IntentClassification("unknown", 0.35f, 50L)
        )
    }

    // When: User sends message
    viewModel.sendMessage("Open my schedule")

    // Then: AVA message prompts teaching
    val avaMessage = viewModel.messages.value.last()
    assertTrue(avaMessage.content.contains("Would you like to teach me?"))
    assertEquals(ConfidenceBadge.RED, avaMessage.badge)
    assertTrue(avaMessage.showTeachButton)
}
```

### Integration Tests (End-to-End)

```kotlin
@Test
fun `end_to_end_user_sends_message_ava_responds`() = runTest {
    // Given: Real database, real classifier, real tokenizer
    val db = createTestDatabase()
    val classifier = IntentClassifier.getInstance(context)
    classifier.initialize(modelPath)

    // When: User sends message
    composeTestRule.onNodeWithTag("input_field").performTextInput("Turn on lights")
    composeTestRule.onNodeWithTag("send_button").performClick()

    // Then: User message appears
    composeTestRule.onNodeWithText("Turn on lights").assertExists()

    // And: AVA message appears (wait for classification)
    composeTestRule.waitUntil(5000) {
        composeTestRule.onAllNodesWithTag("ava_message").fetchSemanticsNodes().isNotEmpty()
    }

    // And: Confidence badge visible
    composeTestRule.onNodeWithTag("confidence_badge").assertExists()

    // And: Message saved to database
    val messages = db.messageDao().getMessagesForConversation("test_conv_id").first()
    assertEquals(2, messages.size)
    assertEquals("user", messages[0].role)
    assertEquals("assistant", messages[1].role)
}
```

### UI Tests (Compose Testing)

```kotlin
@Test
fun `long_press_message_shows_teach_ava_menu`() {
    composeTestRule.setContent {
        MessageBubble(
            message = Message("id", "convId", "assistant", "I'll control lights", 0.85f),
            onTeach = { /* handled */ }
        )
    }

    // When: Long-press message
    composeTestRule.onNodeWithTag("message_bubble").performTouchInput {
        longClick()
    }

    // Then: Context menu appears
    composeTestRule.onNodeWithText("Teach AVA this").assertExists()
    composeTestRule.onNodeWithText("Copy text").assertExists()
}
```

---

## Success Metrics

**Week 6 Completion**:
- ✅ 10 user stories completed
- ✅ 80%+ test coverage (enforced)
- ✅ End-to-end <500ms (measured)
- ✅ 0 P0 bugs (blocking issues resolved)
- ✅ User can complete: Send → Respond → Teach → History flow

**Quality Gates** (from android-app profile):
- ✅ Privacy gate: 100% local processing (verified)
- ✅ Performance gate: NLU <100ms, end-to-end <500ms
- ✅ Testing gate: 80%+ coverage, all tests pass
- ✅ Teach-AVA gate: Training flow validated

---

## Terminology Glossary

To maintain consistency across specification, plan, and implementation:

| Term | Context | Definition | Example |
|------|---------|------------|---------|
| **Utterance** | NLU/Classification | Raw user input text sent to NLU classifier | "Turn on the lights" |
| **Message** | UI/Database | Stored conversation entry with metadata | Message(id, conversationId, role, content, confidence) |
| **Confidence Badge** | UI Component | Visual indicator showing NLU confidence | "🟢 85%" (not "confidence indicator") |
| **Intent** | NLU Result | Classified user intention from NLU | "control_lights", "check_weather" |
| **Template** | Response Generation | Pre-defined response text for intent | "I'll control the lights." |
| **Conversation** | Data Model | Collection of related messages | Conversation(id, title, createdAt, updatedAt) |
| **Training Example** | Teach-AVA | User-taught intent mapping | TrainExample(utterance, intent, locale, hash) |

**Usage Guidelines**:
- Use **"utterance"** when referring to NLU input/classification context
- Use **"message"** when referring to UI display or database storage
- Use **"confidence badge"** consistently (never "confidence indicator")
- Use **"intent"** for NLU results, **"template"** for response text

---

## References

- **Constitution**: `.ideacode/memory/principles.md` (v1.3.2)
- **User Decisions**: `.ideacode/USER_DECISIONS_WEEK6.md`
- **AI UI Best Practices**: `.ideacode/AI_UI_BEST_PRACTICES.md`
- **Database Layer**: Week 3-4 implementation (ConversationRepository, MessageRepository)
- **NLU Layer**: Week 5 implementation (IntentClassifier, BertTokenizer, ModelManager)
- **Teach-AVA UI**: Week 5 implementation (TeachAvaScreen, dialogs, ViewModel)
- **Phase Status**: `.ideacode/PROJECT_PHASES_STATUS_UPDATED.md`

---

**Status**: ✅ Specification Complete
**Next Step**: `/idea2.plan` to create phased implementation plan
**Estimated Effort**: 4-6 days (Tier 3 complexity)
**Team**: Multi-agent deployment (ui-expert, database-expert, nlu-expert, test-specialist)

---

**Approved By**: Manoj Jhawar (AVA AI Project Lead)
**Date**: 2025-01-28
