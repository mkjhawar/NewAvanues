# Chat UI with NLU Integration - Task Breakdown

**Feature ID:** 006-chat-ui
**Created:** 2025-01-28
**Profile:** android-app
**Total Tasks:** 28
**Estimated Effort:** 40 hours (sequential) | 28 hours (parallel)
**Time Savings:** ~30% with parallel execution

---

## Task Summary

| Phase | Tasks | Hours | Agents | Can Parallelize |
|-------|-------|-------|--------|-----------------|
| Phase 1: Foundation | 7 | 16h | 4 | 4 tasks |
| Phase 2: NLU Integration | 8 | 16h | 4 | 3 tasks |
| Phase 3: Teach-AVA | 5 | 8h | 3 | 2 tasks |
| Phase 4: History Overlay | 5 | 8h | 3 | 2 tasks |
| Phase 5: Voice + Polish | 6 | 8h | 3 | 2 tasks |
| Testing & Docs | 4 | 4h | 2 | 1 task |
| **Total** | **35** | **60h** | **6** | **14** |

---

## Phase 1: Foundation (Day 1-2)

### Task P1T01: Create ChatScreen Scaffold
**Description:** Create main ChatScreen composable with Scaffold, TopAppBar, and navigation integration
**Agent:** @ui-expert
**Estimated Time:** 2 hours
**Complexity:** Tier 1
**Dependencies:** None
**Blocks:** P1T02, P1T03

**Quality Gates:**
- [ ] Screen renders without errors
- [ ] TopAppBar shows "AVA AI" title
- [ ] Navigation integrated (accessible from MainActivity)
- [ ] No lint errors

**Files to Create:**
- `features/chat/ui/ChatScreen.kt`

**Testing Requirements:**
- Compose preview function works
- Screen renders in UI test

**Documentation Requirements:**
- KDoc for ChatScreen composable
- Parameter descriptions

---

### Task P1T02: Create MessageBubble Component
**Description:** Create MessageBubble composable with user/AVA variants and timestamp display
**Agent:** @ui-expert
**Estimated Time:** 3 hours
**Complexity:** Tier 1
**Dependencies:** None
**Blocks:** P1T03

**Quality Gates:**
- [ ] User message: right-aligned, blue background, white text
- [ ] AVA message: left-aligned, grey background, black text
- [ ] Timestamp shows relative format ("2m ago", "Yesterday 3:15 PM")
- [ ] Accessibility: contentDescription set correctly
- [ ] 48dp minimum touch target

**Files to Create:**
- `features/chat/ui/components/MessageBubble.kt`
- `features/chat/ui/components/MessageBubblePreview.kt`

**Testing Requirements:**
- Compose preview for both variants
- UI test: MessageBubble renders correctly
- Accessibility test: TalkBack compatibility

**Documentation Requirements:**
- KDoc for MessageBubble
- Document variant parameter

---

### Task P1T03: Implement LazyColumn Message List
**Description:** Add LazyColumn with reverse layout for message display in ChatScreen
**Agent:** @ui-expert
**Estimated Time:** 2 hours
**Complexity:** Tier 1
**Dependencies:** P1T01, P1T02
**Blocks:** P1T05

**Quality Gates:**
- [ ] LazyColumn reverse layout (latest at bottom)
- [ ] Auto-scroll to bottom on new message
- [ ] key = message.id for stable item identity
- [ ] contentType for user/ava variants
- [ ] Scroll performance: 60 FPS with 100+ messages

**Files to Modify:**
- `features/chat/ui/ChatScreen.kt`

**Testing Requirements:**
- UI test: Scroll performance with 100+ messages
- Test: Auto-scroll on new message

**Documentation Requirements:**
- Comment explaining reverse layout choice

---

### Task P1T04: Create ChatViewModel Skeleton
**Description:** Create ChatViewModel with basic state management (messages StateFlow, conversationId)
**Agent:** @kotlin-expert
**Estimated Time:** 2 hours
**Complexity:** Tier 1
**Dependencies:** None
**Blocks:** P1T05, P1T06

**Quality Gates:**
- [ ] StateFlow<List<Message>> for messages
- [ ] StateFlow<String?> for active conversationId
- [ ] viewModelScope for coroutines
- [ ] Proper lifecycle handling
- [ ] No memory leaks

**Files to Create:**
- `features/chat/ui/ChatViewModel.kt`
- `features/chat/ui/ChatViewModelTest.kt`

**Testing Requirements:**
- Unit test: ViewModel initialization
- Unit test: State updates correctly
- Coverage ≥ 80%

**Documentation Requirements:**
- KDoc for ChatViewModel class
- Document all public methods

---

### Task P1T05: Integrate Database Repositories
**Description:** Connect ConversationRepository and MessageRepository to ChatViewModel, implement observeMessages
**Agent:** @database-expert
**Estimated Time:** 3 hours
**Complexity:** Tier 2
**Dependencies:** P1T03, P1T04
**Blocks:** P1T06

**Quality Gates:**
- [ ] observeMessages returns Flow<List<Message>>
- [ ] Load active conversation on init
- [ ] Handle empty state (no conversations)
- [ ] Handle edge cases (conversation not found)
- [ ] Database queries <40ms (validated Week 3-4)

**Files to Modify:**
- `features/chat/ui/ChatViewModel.kt`

**Testing Requirements:**
- Integration test: Messages from DB appear in UI
- Unit test: observeMessages flow emits correctly
- Test: Handle empty database
- Coverage ≥ 80%

**Documentation Requirements:**
- Document observeMessages flow
- Comment on active conversation logic

---

### Task P1T06: Create Input Field and Send Button
**Description:** Add OutlinedTextField and Send button at bottom of ChatScreen
**Agent:** @ui-expert
**Estimated Time:** 2 hours
**Complexity:** Tier 1
**Dependencies:** P1T04
**Blocks:** P1T07

**Quality Gates:**
- [ ] OutlinedTextField with hint "Type a message..."
- [ ] Send button enabled only when text non-empty
- [ ] Input field clears after send
- [ ] Keyboard shows automatically (optional)
- [ ] Accessibility: contentDescription for button

**Files to Modify:**
- `features/chat/ui/ChatScreen.kt`

**Testing Requirements:**
- UI test: Input field accepts text
- UI test: Send button enabled/disabled correctly
- UI test: Field clears after send

**Documentation Requirements:**
- Comment on enabled state logic

---

### Task P1T07: Implement Basic Send Message Flow
**Description:** Connect send button to ChatViewModel.sendMessage(), save user message to DB, display placeholder AVA response
**Agent:** @kotlin-expert
**Estimated Time:** 2 hours
**Complexity:** Tier 2
**Dependencies:** P1T05, P1T06
**Blocks:** P2T01

**Quality Gates:**
- [ ] User message saved to database (role: "user")
- [ ] Placeholder AVA message created (role: "assistant", content: "Processing...")
- [ ] Messages appear in UI immediately
- [ ] No crashes on send
- [ ] Handles rapid sends (debounce if needed)

**Files to Modify:**
- `features/chat/ui/ChatViewModel.kt`

**Testing Requirements:**
- Integration test: Send message → appears in DB → appears in UI
- Unit test: sendMessage saves to repository
- Test: Rapid send handling
- Coverage ≥ 80%

**Documentation Requirements:**
- Document sendMessage flow
- Comment on placeholder response (Phase 2 will replace)

---

## Phase 2: NLU Integration (Day 3-4)

### Task P2T01: Initialize IntentClassifier in ViewModel
**Description:** Add IntentClassifier initialization in ChatViewModel init, load ONNX model via ModelManager
**Agent:** @nlu-expert
**Estimated Time:** 2 hours
**Complexity:** Tier 2
**Dependencies:** P1T07
**Blocks:** P2T02

**Quality Gates:**
- [ ] IntentClassifier.getInstance() called in init
- [ ] Model loaded successfully (or error handled)
- [ ] ModelManager.isModelAvailable() checked
- [ ] Handle model loading failure gracefully
- [ ] Initialization <500ms

**Files to Modify:**
- `features/chat/ui/ChatViewModel.kt`

**Testing Requirements:**
- Unit test: Classifier initialized correctly
- Unit test: Handle model not found
- Mock classifier for tests
- Coverage ≥ 80%

**Documentation Requirements:**
- Document classifier initialization
- Comment on error handling

---

### Task P2T02: Load Candidate Intents
**Description:** Implement candidate intent loading from built-in list + user-taught intents (TrainExample)
**Agent:** @nlu-expert
**Estimated Time:** 2 hours
**Complexity:** Tier 2
**Dependencies:** P2T01
**Blocks:** P2T03

**Quality Gates:**
- [ ] Built-in intents: hardcoded list (control_lights, check_weather, etc.)
- [ ] User-taught intents: TrainExampleRepository.getAllIntents()
- [ ] Combine + deduplicate intents
- [ ] Handle empty TrainExample table
- [ ] Candidate list updated when new intents taught

**Files to Modify:**
- `features/chat/ui/ChatViewModel.kt`
- `features/chat/data/BuiltInIntents.kt` (create)

**Testing Requirements:**
- Unit test: Built-in intents loaded
- Unit test: User intents from DB loaded
- Unit test: Deduplication works
- Coverage ≥ 80%

**Documentation Requirements:**
- Document candidate intent strategy
- List built-in intents in code comment

---

### Task P2T03: Integrate NLU Classification Pipeline
**Description:** Replace placeholder AVA response with real NLU classification (tokenize → classify → template response)
**Agent:** @nlu-expert
**Estimated Time:** 3 hours
**Complexity:** Tier 2
**Dependencies:** P2T02
**Blocks:** P2T04, P2T05

**Quality Gates:**
- [ ] On sendMessage: tokenize utterance with BertTokenizer
- [ ] Classify with IntentClassifier (candidate intents)
- [ ] Result<IntentClassification> handled (success + error)
- [ ] AVA message created with intent + confidence
- [ ] Classification completes <100ms (target <50ms)
- [ ] Log timing metrics (tokenization, classification, total)

**Files to Modify:**
- `features/chat/ui/ChatViewModel.kt`

**Testing Requirements:**
- Unit test: Tokenization → classification → response
- Unit test: Handle classification error
- Integration test: End-to-end send → classify → display
- Performance test: <100ms classification
- Coverage ≥ 80%

**Documentation Requirements:**
- Document NLU pipeline flow
- Comment on error handling

---

### Task P2T04: Create Intent Template System
**Description:** Create IntentTemplates object with intent → template response mapping
**Agent:** @kotlin-expert
**Estimated Time:** 1 hour
**Complexity:** Tier 1
**Dependencies:** P2T03
**Blocks:** P2T05

**Quality Gates:**
- [ ] IntentTemplates.getResponse(intent) returns template
- [ ] All built-in intents have templates
- [ ] "unknown" template for low confidence
- [ ] Templates are concise (1-2 sentences)
- [ ] Easy to add new templates

**Files to Create:**
- `features/chat/data/IntentTemplates.kt`
- `features/chat/data/IntentTemplatesTest.kt`

**Testing Requirements:**
- Unit test: getResponse returns correct template
- Unit test: Unknown intent returns default
- Coverage ≥ 80%

**Documentation Requirements:**
- KDoc for IntentTemplates
- Comment on template design philosophy

---

### Task P2T05: Implement Confidence Badge UI
**Description:** Update MessageBubble to show confidence badges (🟢🟡🔴) based on threshold
**Agent:** @ui-expert
**Estimated Time:** 3 hours
**Complexity:** Tier 2
**Dependencies:** P2T03, P2T04
**Blocks:** P2T06

**Quality Gates:**
- [ ] Green badge (>70%): Small percentage, no button
- [ ] Yellow badge (50-70%): Percentage + "Confirm?" button
- [ ] Red badge (<50%): Percentage + "Teach AVA" button
- [ ] Badge animates in (fade in 200ms)
- [ ] Accessibility: Badge color + text (not color alone)
- [ ] 48dp touch target for buttons

**Files to Modify:**
- `features/chat/ui/components/MessageBubble.kt`
- `features/chat/data/Message.kt` (add confidence field)

**Testing Requirements:**
- UI test: Green badge renders correctly
- UI test: Yellow badge with button
- UI test: Red badge with button
- Accessibility test: Color not sole indicator
- Coverage ≥ 80%

**Documentation Requirements:**
- Document confidence threshold logic
- Comment on badge color choices

---

### Task P2T06: Implement Auto-Prompt on Low Confidence
**Description:** When confidence <0.5, use "unknown" template and show "Teach AVA" button
**Agent:** @kotlin-expert
**Estimated Time:** 1 hour
**Complexity:** Tier 1
**Dependencies:** P2T05
**Blocks:** P3T01

**Quality Gates:**
- [ ] If confidence <0.5: template = "unknown"
- [ ] showTeachButton flag set to true
- [ ] Button opens Teach-AVA bottom sheet (Phase 3)
- [ ] Threshold configurable (default 0.5)
- [ ] Handles edge case (confidence = exactly 0.5)

**Files to Modify:**
- `features/chat/ui/ChatViewModel.kt`

**Testing Requirements:**
- Unit test: Confidence <0.5 → unknown template
- Unit test: showTeachButton = true
- Unit test: Confidence thresholds (0.49, 0.5, 0.51)
- Coverage ≥ 80%

**Documentation Requirements:**
- Document threshold logic
- Comment on configurable threshold (future setting)

---

### Task P2T07: Performance Validation
**Description:** Measure NLU pipeline performance on physical device, validate <100ms target
**Agent:** @nlu-expert
**Estimated Time:** 2 hours
**Complexity:** Tier 2
**Dependencies:** P2T03
**Blocks:** None

**Quality Gates:**
- [ ] Log timing metrics: tokenization, classification, total
- [ ] Test on physical device (if available)
- [ ] Target: <100ms total (tokenization + classification)
- [ ] If >100ms: Investigate (NNAPI enabled? Model loaded?)
- [ ] Document findings in notes.md

**Files to Modify:**
- `features/chat/ui/ChatViewModel.kt` (add logging)

**Testing Requirements:**
- Performance test: Log 100 classifications
- Calculate average, p50, p95, p99
- Test on low-end device (2GB RAM) if available

**Documentation Requirements:**
- Update `docs/ProjectInstructions/notes.md` with performance findings
- Document optimization opportunities

---

### Task P2T08: NLU Integration Testing
**Description:** Create comprehensive unit tests for NLU integration in ChatViewModel
**Agent:** @test-specialist
**Estimated Time:** 2 hours
**Complexity:** Tier 2
**Dependencies:** P2T01, P2T02, P2T03, P2T04, P2T05, P2T06
**Blocks:** P3T01

**Quality Gates:**
- [ ] Test: High confidence (>70%) → Green badge, correct template
- [ ] Test: Medium confidence (50-70%) → Yellow badge, confirm button
- [ ] Test: Low confidence (<50%) → Red badge, Teach AVA button, "unknown" template
- [ ] Test: Error handling (classifier fails)
- [ ] Coverage ≥ 80% for NLU-related code

**Files to Create:**
- `features/chat/ui/ChatViewModelNluTest.kt`

**Testing Requirements:**
- All confidence ranges tested
- Edge cases covered (0.0, 0.49, 0.5, 0.7, 1.0)
- Mock classifier for predictable tests
- Coverage report shows ≥80%

**Documentation Requirements:**
- Document test scenarios in comments

---

## Phase 3: Teach-AVA Integration (Day 5)

### Task P3T01: Create TeachAvaBottomSheet Component
**Description:** Create TeachAvaBottomSheet composable with ModalBottomSheet, intent selection UI
**Agent:** @ui-expert
**Estimated Time:** 4 hours
**Complexity:** Tier 2
**Dependencies:** P2T06, P2T08
**Blocks:** P3T02

**Quality Gates:**
- [ ] ModalBottomSheet (~1/3 screen height)
- [ ] Header: "🎓 Teach AVA"
- [ ] Section 1: "You said: [utterance]"
- [ ] Section 2: "I understood: [intent] ([confidence]%)"
- [ ] Section 3: LazyColumn of suggested intents
- [ ] Text field: "Or type a new intent..."
- [ ] Actions: [Cancel] [Save]
- [ ] Chat visible above bottom sheet
- [ ] Accessibility: All elements labeled

**Files to Create:**
- `features/chat/ui/components/TeachAvaBottomSheet.kt`
- `features/chat/ui/components/TeachAvaBottomSheetPreview.kt`

**Testing Requirements:**
- Compose preview works
- UI test: Bottom sheet renders
- UI test: Cancel dismisses sheet
- Accessibility test: TalkBack compatibility

**Documentation Requirements:**
- KDoc for TeachAvaBottomSheet
- Document all parameters

---

### Task P3T02: Add Long-Press Context Menu to MessageBubble
**Description:** Add long-press gesture detector to MessageBubble, show context menu ("Teach AVA", "Copy", "Delete")
**Agent:** @ui-expert
**Estimated Time:** 2 hours
**Complexity:** Tier 2
**Dependencies:** P3T01
**Blocks:** P3T03

**Quality Gates:**
- [ ] Long-press shows context menu (DropdownMenu)
- [ ] Menu options: "Teach AVA this", "Copy text", "Delete message" (user only)
- [ ] Tapping "Teach AVA" opens bottom sheet
- [ ] Context menu dismisses on selection
- [ ] Accessibility: Long-press announced by TalkBack

**Files to Modify:**
- `features/chat/ui/components/MessageBubble.kt`

**Testing Requirements:**
- UI test: Long-press shows menu
- UI test: Menu options correct for user/AVA
- UI test: "Teach AVA" callback invoked

**Documentation Requirements:**
- Comment on long-press gesture threshold

---

### Task P3T03: Integrate TrainExampleRepository
**Description:** Connect "Save" button to TrainExampleRepository.insert(), save training example
**Agent:** @database-expert
**Estimated Time:** 1 hour
**Complexity:** Tier 1
**Dependencies:** P3T02
**Blocks:** P3T04

**Quality Gates:**
- [ ] On "Save": Create TrainExample (utterance, intent, locale, hash)
- [ ] Call TrainExampleRepository.insert()
- [ ] Handle duplicate (hash collision) gracefully
- [ ] Success: Show toast "AVA learned: [utterance] → [intent]"
- [ ] Error: Show error toast

**Files to Modify:**
- `features/chat/ui/ChatViewModel.kt`

**Testing Requirements:**
- Integration test: Save → appears in TrainExample table
- Unit test: Duplicate handling
- Test: Success toast appears
- Coverage ≥ 80%

**Documentation Requirements:**
- Document save training example flow
- Comment on hash deduplication

---

### Task P3T04: Implement Bottom Sheet State Management
**Description:** Add showTeachAvaSheet state to ChatViewModel, handle show/dismiss
**Agent:** @kotlin-expert
**Estimated Time:** 1 hour
**Complexity:** Tier 1
**Dependencies:** P3T03
**Blocks:** P3T05

**Quality Gates:**
- [ ] StateFlow<Boolean> for showTeachAvaSheet
- [ ] openTeachAvaSheet(messageId) method
- [ ] dismissTeachAvaSheet() method
- [ ] Pass message data to bottom sheet (utterance, intent, confidence)
- [ ] State persists across recomposition

**Files to Modify:**
- `features/chat/ui/ChatViewModel.kt`

**Testing Requirements:**
- Unit test: openTeachAvaSheet sets state
- Unit test: dismissTeachAvaSheet clears state
- Test: Message data passed correctly
- Coverage ≥ 80%

**Documentation Requirements:**
- Document state management for bottom sheet

---

### Task P3T05: Teach-AVA Integration Testing
**Description:** Create UI and integration tests for Teach-AVA flow
**Agent:** @test-specialist
**Estimated Time:** 2 hours
**Complexity:** Tier 2
**Dependencies:** P3T01, P3T02, P3T03, P3T04
**Blocks:** P4T01

**Quality Gates:**
- [ ] UI test: Long-press message → menu appears
- [ ] UI test: Tap "Teach AVA" → bottom sheet opens
- [ ] Integration test: Save → TrainExample in DB
- [ ] Integration test: Success toast appears
- [ ] Coverage ≥ 80%

**Files to Create:**
- `features/chat/ui/TeachAvaIntegrationTest.kt`

**Testing Requirements:**
- End-to-end test: Low confidence → prompt → teach → save
- Test: User-taught intent appears in candidate list next time
- Coverage report shows ≥80%

**Documentation Requirements:**
- Document Teach-AVA test scenarios

---

## Phase 4: History Overlay (Day 6)

### Task P4T01: Create HistoryOverlay Component
**Description:** Create HistoryOverlay composable with side panel, conversation list
**Agent:** @ui-expert
**Estimated Time:** 4 hours
**Complexity:** Tier 2
**Dependencies:** P3T05
**Blocks:** P4T02

**Quality Gates:**
- [ ] AnimatedVisibility slide-in from right
- [ ] Width: 25% of screen
- [ ] Header: "📜 History"
- [ ] LazyColumn of conversations (sorted by updatedAt desc)
- [ ] Conversation item: title, preview, timestamp, active indicator
- [ ] Swipe right to dismiss (DismissibleNavigationDrawer)
- [ ] Accessibility: All elements labeled

**Files to Create:**
- `features/chat/ui/components/HistoryOverlay.kt`
- `features/chat/ui/components/HistoryOverlayPreview.kt`

**Testing Requirements:**
- Compose preview works
- UI test: Overlay slides in/out
- UI test: Conversation list renders
- Accessibility test: TalkBack compatibility

**Documentation Requirements:**
- KDoc for HistoryOverlay
- Document animation choices

---

### Task P4T02: Add Voice Trigger for History Overlay
**Description:** Handle "show_history" intent to open history overlay
**Agent:** @kotlin-expert
**Estimated Time:** 1 hour
**Complexity:** Tier 1
**Dependencies:** P4T01
**Blocks:** P4T03

**Quality Gates:**
- [ ] When user sends "show history" or "show transcript"
- [ ] Classify as "show_history" intent
- [ ] Set showHistory StateFlow to true
- [ ] Overlay appears
- [ ] Voice command "close history" dismisses

**Files to Modify:**
- `features/chat/ui/ChatViewModel.kt`
- `features/chat/data/IntentTemplates.kt` (add show_history)
- `features/chat/data/BuiltInIntents.kt` (add show_history)

**Testing Requirements:**
- Unit test: "show history" → showHistory = true
- Unit test: "close history" → showHistory = false
- Coverage ≥ 80%

**Documentation Requirements:**
- Document voice trigger logic

---

### Task P4T03: Implement Conversation Switching
**Description:** Add switchConversation(id) method to ChatViewModel, load messages for new conversation
**Agent:** @kotlin-expert
**Estimated Time:** 2 hours
**Complexity:** Tier 2
**Dependencies:** P4T02
**Blocks:** P4T04

**Quality Gates:**
- [ ] switchConversation(id) updates active conversationId
- [ ] Load messages for new conversation
- [ ] Save active conversation ID to SharedPreferences
- [ ] Handle conversation not found error
- [ ] Overlay remains open after switch (can switch again)

**Files to Modify:**
- `features/chat/ui/ChatViewModel.kt`

**Testing Requirements:**
- Integration test: Switch conversation → messages load
- Unit test: Active conversation ID saved
- Test: Handle invalid conversation ID
- Coverage ≥ 80%

**Documentation Requirements:**
- Document conversation switching flow
- Comment on preference persistence

---

### Task P4T04: Add Voice Navigation (Optional)
**Description:** Implement "previous conversation" and "next conversation" voice commands (optional, depends on VOS4)
**Agent:** @kotlin-expert
**Estimated Time:** 1 hour
**Complexity:** Tier 2
**Dependencies:** P4T03
**Blocks:** P4T05

**Quality Gates:**
- [ ] "Previous conversation" → load older conversation
- [ ] "Next conversation" → load newer conversation
- [ ] Handle edge cases (no previous/next)
- [ ] AVA responds: "Loaded conversation from [timestamp]"
- [ ] Works with voice input (if VOS4 ready)

**Files to Modify:**
- `features/chat/ui/ChatViewModel.kt`
- `features/chat/data/IntentTemplates.kt` (add navigation intents)

**Testing Requirements:**
- Unit test: Previous/next navigation
- Test: Edge cases (first/last conversation)
- Coverage ≥ 80%

**Documentation Requirements:**
- Document navigation logic
- Note: Optional, depends on VOS4

---

### Task P4T05: History Overlay Testing
**Description:** Create UI and integration tests for history overlay
**Agent:** @test-specialist
**Estimated Time:** 2 hours
**Complexity:** Tier 2
**Dependencies:** P4T01, P4T02, P4T03, P4T04
**Blocks:** P5T01

**Quality Gates:**
- [ ] UI test: "show history" → overlay appears
- [ ] UI test: Tap conversation → messages load
- [ ] UI test: Swipe right → overlay dismisses
- [ ] Integration test: Voice navigation works
- [ ] Coverage ≥ 80%

**Files to Create:**
- `features/chat/ui/HistoryOverlayTest.kt`

**Testing Requirements:**
- End-to-end test: Show history → switch conversation → close
- Test: Voice navigation (if VOS4 ready)
- Coverage report shows ≥80%

**Documentation Requirements:**
- Document history overlay test scenarios

---

## Phase 5: Voice Input & Polish (Day 7)

### Task P5T01: Create VoiceInputModal Component (Optional)
**Description:** Create VoiceInputModal with microphone animation, live transcription (if VOS4 ready)
**Agent:** @ui-expert
**Estimated Time:** 3 hours
**Complexity:** Tier 2
**Dependencies:** P4T05
**Blocks:** P5T02

**Quality Gates:**
- [ ] Pulsing microphone animation (animated scale + color)
- [ ] Waveform visualization (placeholder bars)
- [ ] Live transcription display (updates as user speaks)
- [ ] Real-time confidence indicator (green/yellow/red)
- [ ] Stop button to finalize
- [ ] Accessibility: Announces "Listening"

**Files to Create:**
- `features/chat/ui/components/VoiceInputModal.kt`
- `features/chat/ui/components/VoiceInputModalPreview.kt`

**Testing Requirements:**
- Compose preview works
- UI test: Modal renders
- UI test: Stop button works
- Accessibility test: TalkBack compatibility

**Documentation Requirements:**
- KDoc for VoiceInputModal
- Document VOS4 dependency

**Note:** This task is **optional (P1)**. If VOS4 not ready by Day 7, defer to Week 8 and use time for P5T04 performance optimization instead.

---

### Task P5T02: Integrate VOS4 SpeechRecognitionManager (Optional)
**Description:** Connect VoiceInputModal to VOS4 SpeechRecognitionManager, implement live transcription
**Agent:** @kotlin-expert
**Estimated Time:** 2 hours
**Complexity:** Tier 2
**Dependencies:** P5T01
**Blocks:** None

**Quality Gates:**
- [ ] Initialize VOS4 SpeechRecognitionManager
- [ ] Start Vosk recognition on modal open
- [ ] Listen for partial results (live transcription)
- [ ] On final result: Send as message (same flow as text)
- [ ] Handle errors: No speech, recognition failed
- [ ] Fallback to text input on error

**Files to Modify:**
- `features/chat/ui/ChatViewModel.kt`

**Testing Requirements:**
- Integration test: Voice input → text message (mock VOS4)
- Unit test: Handle no speech detected
- Test: Fallback to text input

**Documentation Requirements:**
- Document VOS4 integration
- Comment on error handling

**Note:** This task is **optional (P1)**. If VOS4 not ready, document decision to defer to Week 8.

---

### Task P5T03: Create Conversation Mode Setting
**Description:** Add "Conversation Mode" setting screen with three options (append, new, ask)
**Agent:** @ui-expert
**Estimated Time:** 2 hours
**Complexity:** Tier 1
**Dependencies:** None (can run in parallel)
**Blocks:** None

**Quality Gates:**
- [ ] Settings screen with "Conversation Mode" option
- [ ] Three radio buttons: "Append to recent", "New each session", "Ask me"
- [ ] Save to SharedPreferences
- [ ] Implement in MainActivity on app launch
- [ ] Default: "Append to recent"

**Files to Create:**
- `features/settings/ui/SettingsScreen.kt`
- `features/settings/data/ConversationMode.kt` (enum)

**Testing Requirements:**
- UI test: Settings screen renders
- Unit test: Save/load preference
- Integration test: App launch behavior for each mode

**Documentation Requirements:**
- KDoc for SettingsScreen
- Document conversation mode options

---

### Task P5T04: Performance Optimization
**Description:** Profile with Android Profiler, optimize LazyColumn, reduce recompositions
**Agent:** @kotlin-expert
**Estimated Time:** 2 hours
**Complexity:** Tier 2
**Dependencies:** P2T07 (performance validation)
**Blocks:** None

**Quality Gates:**
- [ ] Profile with Android Profiler (CPU, memory)
- [ ] Optimize LazyColumn: key, contentType, remember
- [ ] Reduce recompositions: derivedStateOf where appropriate
- [ ] Test scroll performance with 1000+ messages
- [ ] Target: 60 FPS scroll, <500ms end-to-end
- [ ] Document findings in notes.md

**Files to Modify:**
- `features/chat/ui/ChatScreen.kt`
- `features/chat/ui/ChatViewModel.kt`

**Testing Requirements:**
- Performance test: Scroll 1000+ messages at 60 FPS
- Test: End-to-end <500ms (user send → AVA response displayed)
- Profile memory usage (<512MB peak)

**Documentation Requirements:**
- Update `docs/ProjectInstructions/notes.md` with optimization findings
- Document recomposition hotspots

---

### Task P5T05: Final End-to-End Testing
**Description:** Execute comprehensive end-to-end test, verify all acceptance criteria met
**Agent:** @test-specialist
**Estimated Time:** 2 hours
**Complexity:** Tier 2
**Dependencies:** All Phase 1-5 tasks
**Blocks:** P5T06

**Quality Gates:**
- [ ] End-to-end test: Send → AVA responds → Teach → History
- [ ] Verify all 7 functional requirements met
- [ ] Verify all 4 non-functional requirements met
- [ ] Performance: <500ms end-to-end (measured)
- [ ] Coverage: ≥80% (verified)
- [ ] All P0 bugs resolved

**Files to Create:**
- `features/chat/ChatEndToEndTest.kt`

**Testing Requirements:**
- Full user journey test (all features)
- Performance measurements logged
- Coverage report generated
- All tests passing

**Documentation Requirements:**
- Document test results
- Update `docs/ProjectInstructions/progress.md` with Week 6 status

---

### Task P5T06: Bug Fixes and Polish
**Description:** Fix any blocking bugs discovered during testing, polish UI/UX
**Agent:** Multiple (as needed)
**Estimated Time:** 2 hours
**Complexity:** Tier 2
**Dependencies:** P5T05
**Blocks:** None

**Quality Gates:**
- [ ] All P0 bugs fixed
- [ ] No crashes in normal flow
- [ ] UI animations smooth
- [ ] Error messages clear and helpful
- [ ] Accessibility issues resolved

**Files to Modify:**
- Various (based on bugs found)

**Testing Requirements:**
- Regression tests for bug fixes
- Verify all tests still pass

**Documentation Requirements:**
- Update `docs/ProjectInstructions/bugs.md` with resolved issues
- Document any deferred issues (P1, P2)

---

## Testing & Documentation (Final)

### Task TDT01: Comprehensive Test Coverage Validation
**Description:** Run full test suite, verify ≥80% coverage, generate coverage report
**Agent:** @test-specialist
**Estimated Time:** 1 hour
**Complexity:** Tier 1
**Dependencies:** P5T06
**Blocks:** TDT02

**Quality Gates:**
- [ ] `./gradlew testDebugUnitTestCoverage` shows ≥80%
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] All UI tests pass (Compose testing)
- [ ] Coverage report generated (HTML)

**Commands:**
```bash
./gradlew test
./gradlew connectedAndroidTest
./gradlew testDebugUnitTestCoverage
```

**Testing Requirements:**
- All tests passing
- Coverage ≥80% for all new code
- No flaky tests

**Documentation Requirements:**
- Save coverage report to `build/reports/coverage/`
- Document coverage in progress.md

---

### Task TDT02: Update Living Documentation
**Description:** Update notes.md, decisions.md, progress.md with Week 6 insights and status
**Agent:** @documentation-specialist
**Estimated Time:** 2 hours
**Complexity:** Tier 1
**Dependencies:** TDT01
**Blocks:** TDT03

**Quality Gates:**
- [ ] notes.md: Implementation insights (Compose patterns, NLU integration)
- [ ] decisions.md: ADRs for technical decisions (template responses, voice input)
- [ ] progress.md: Week 6 summary (start/end dates, deliverables, metrics)
- [ ] bugs.md: Updated with resolved issues

**Files to Modify:**
- `docs/ProjectInstructions/notes.md`
- `docs/ProjectInstructions/decisions.md`
- `docs/ProjectInstructions/progress.md`
- `docs/ProjectInstructions/bugs.md`

**Documentation Requirements:**
- Add 4 ADRs to decisions.md (template responses, voice input, history UI, thresholds)
- Update progress.md with Week 6 metrics (files created, test coverage, velocity)
- Document learnings in notes.md (Compose best practices, NLU performance)

---

### Task TDT03: Final Code Review and Commit
**Description:** Review all code for quality, create final commit with conventional format
**Agent:** Multiple (peer review)
**Estimated Time:** 1 hour
**Complexity:** Tier 1
**Dependencies:** TDT02
**Blocks:** None

**Quality Gates:**
- [ ] All code follows Kotlin coding standards
- [ ] No TODOs or FIXMEs left in code
- [ ] All public APIs documented (KDoc)
- [ ] No lint errors
- [ ] Commit message follows conventional format

**Commit Message Template:**
```
feat(chat-ui): Add chat UI with NLU integration and Teach-AVA flow

- Message bubbles with confidence indicators (🟢🟡🔴)
- Text input with ONNX NLU classification
- Auto-prompt on low confidence (<0.5)
- Teach-AVA bottom sheet integration
- Conversation history overlay (voice-triggered)
- Voice input modal (optional, VOS4 dependency)
- Conversation mode setting

Closes #6

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

**Documentation Requirements:**
- Final review checklist
- Commit pushed to main branch

---

## Execution Plan

### Parallel Execution Opportunities

**Batch 1 (Day 1, No Dependencies):**
- **P1T01** - ChatScreen Scaffold (@ui-expert)
- **P1T02** - MessageBubble Component (@ui-expert)
- **P1T04** - ChatViewModel Skeleton (@kotlin-expert)

*Run in parallel: 3 agents, ~2 hours*

**Batch 2 (Day 1-2, Depends on Batch 1):**
- **P1T03** - LazyColumn Message List (@ui-expert) - depends: P1T01, P1T02
- **P1T05** - Database Integration (@database-expert) - depends: P1T04
- **P1T06** - Input Field (@ui-expert) - depends: P1T04

*Run in parallel: 3 agents, ~3 hours*

**Batch 3 (Day 2, Depends on Batch 2):**
- **P1T07** - Send Message Flow (@kotlin-expert) - depends: P1T05, P1T06

*Sequential: 1 agent, ~2 hours*

**Batch 4 (Day 3, No Dependencies from each other):**
- **P2T01** - Initialize IntentClassifier (@nlu-expert) - depends: P1T07
- **P2T02** - Load Candidate Intents (@nlu-expert) - depends: P2T01

*Sequential within track, ~4 hours*

**Batch 5 (Day 3-4, NLU Pipeline):**
- **P2T03** - NLU Classification Pipeline (@nlu-expert) - depends: P2T02
- **P2T04** - Intent Templates (@kotlin-expert) - depends: P2T03
- **P2T05** - Confidence Badge UI (@ui-expert) - depends: P2T03, P2T04

*Mixed: 3 agents (P2T04 and P2T05 can start when P2T03 is ready), ~6 hours*

**Batch 6 (Day 4, Testing):**
- **P2T06** - Auto-Prompt (@kotlin-expert) - depends: P2T05
- **P2T07** - Performance Validation (@nlu-expert) - depends: P2T03 (can run in parallel with P2T06)
- **P2T08** - NLU Testing (@test-specialist) - depends: all P2 tasks

*Mixed: 2-3 agents, ~4 hours*

**Batch 7 (Day 5, Teach-AVA):**
- **P3T01** - TeachAvaBottomSheet (@ui-expert) - depends: P2T06, P2T08
- **P3T02** - Long-Press Menu (@ui-expert) - depends: P3T01
- **P3T03** - TrainExample Integration (@database-expert) - depends: P3T02 (can start earlier)
- **P3T04** - Bottom Sheet State (@kotlin-expert) - depends: P3T03

*Mixed: 3 agents, ~8 hours*

**Batch 8 (Day 6, History Overlay):**
- **P4T01** - HistoryOverlay Component (@ui-expert) - depends: P3T05
- **P4T02** - Voice Trigger (@kotlin-expert) - depends: P4T01
- **P4T03** - Conversation Switching (@kotlin-expert) - depends: P4T02
- **P4T04** - Voice Navigation (@kotlin-expert) - depends: P4T03

*Sequential: 2 agents, ~8 hours*

**Batch 9 (Day 7, Voice + Polish):**
- **P5T03** - Conversation Mode Setting (@ui-expert) - no dependencies (can run any time)
- **P5T04** - Performance Optimization (@kotlin-expert) - depends: P2T07

*Parallel: 2 agents, ~4 hours*

**Optional (if VOS4 ready):**
- **P5T01** - VoiceInputModal (@ui-expert)
- **P5T02** - VOS4 Integration (@kotlin-expert)

*If VOS4 not ready: Skip P5T01, P5T02, spend time on P5T04 instead*

**Final (Day 7):**
- **P5T05** - End-to-End Testing (@test-specialist) - depends: all Phase 1-5
- **P5T06** - Bug Fixes (@multiple) - depends: P5T05
- **TDT01** - Coverage Validation (@test-specialist) - depends: P5T06
- **TDT02** - Living Docs (@documentation-specialist) - depends: TDT01
- **TDT03** - Final Review (@multiple) - depends: TDT02

*Sequential: ~6 hours*

### Critical Path

**Critical Path** (minimum time to complete):
```
P1T01 → P1T03 → P1T05 → P1T07 → P2T01 → P2T02 → P2T03 → P2T05 → P2T06 → P2T08 →
P3T01 → P3T02 → P3T03 → P3T04 → P3T05 → P4T01 → P4T02 → P4T03 → P4T05 →
P5T05 → P5T06 → TDT01 → TDT02 → TDT03
```

**Critical Path Time:** ~28 hours (with parallel execution)
**Sequential Time:** ~60 hours
**Time Savings:** ~53% with parallel execution

### Optimization Opportunities

- **14 tasks can run in parallel** at various points
- **Expected time savings: ~30%** with multi-agent deployment
- **Agents can work on independent modules** (UI, ViewModel, Database, NLU) simultaneously
- **Testing specialist validates after each phase** (IDE Loop Defend phase)
- **Documentation specialist updates living docs continuously** (IDE Loop Evaluate phase)

---

## Risk Register

### Risk 1: NLU Performance on Low-End Devices
**Affected Tasks:** P2T03, P2T07, P5T04
**Impact:** High (affects UX, success criteria)
**Probability:** Medium (40%)
**Mitigation:**
- Task P2T07 validates performance early (Day 3)
- Enable NNAPI hardware acceleration
- Test on physical device if available
- Reduce sequence length if needed (128 → 64 tokens)
**Contingency:**
- Show "Processing..." indicator for slow devices
- Move classification to background thread
- Defer optimization to Week 7 if needed

---

### Risk 2: VOS4 Not Ready for Voice Input
**Affected Tasks:** P5T01, P5T02
**Impact:** Low (voice input is P1, not P0)
**Probability:** High (60%)
**Mitigation:**
- Check VOS4 readiness before Day 7 (P5T01)
- Mark tasks as optional (P1)
- Have fallback plan: Performance optimization (P5T04)
**Contingency:**
- Document decision to defer voice input to Week 8
- Update spec.md status (FR-006-005: Deferred to Week 8)
- Use time for performance optimization, bug fixes, polish

---

### Risk 3: Teach-AVA UX Confusion
**Affected Tasks:** P3T01, P3T02, P3T05
**Impact:** Medium (affects training flow, core feature)
**Probability:** Low (20%)
**Mitigation:**
- Follow established patterns from AI_UI_BEST_PRACTICES.md
- Clear labels ("You said", "I understood", "What did you mean?")
- Voice command alternative ("Teach AVA that...")
**Contingency:**
- Iterate on feedback in Week 7
- Add tooltips or onboarding flow
- User testing with 3-5 external testers

---

### Risk 4: LazyColumn Scroll Performance
**Affected Tasks:** P1T03, P5T04
**Impact:** Medium (affects UX for long conversations)
**Probability:** Low (30%)
**Mitigation:**
- Use `key = message.id` for stable item identity (P1T03)
- Use `contentType` for different message types (P1T03)
- Test with 1000+ messages early (P1T03)
- Performance optimization task (P5T04)
**Contingency:**
- Implement pagination (load 100 messages at a time)
- Use `rememberLazyListState` for scroll position preservation

---

## Quality Gate Summary (from profile: android-app)

**Overall Project Gates:**
- [ ] Test coverage ≥ 80% (enforced by TDT01)
- [ ] Build time ≤ 120 seconds (profile default)
- [ ] All public APIs documented (KDoc required)
- [ ] All tests passing (TDT01 verification)
- [ ] No security vulnerabilities (privacy gate maintained)
- [ ] Performance requirements met (NFR-006-001: <500ms end-to-end)

**Profile-Specific Gates (android-app):**
- [ ] Privacy gate: 100% local processing (maintained from Week 1-5)
- [ ] Accessibility: Screen reader compatible, 48dp touch targets, WCAG AA
- [ ] Performance: NLU <100ms, end-to-end <500ms, 60 FPS scroll
- [ ] Teach-AVA gate: Training flow validated (P3T05)
- [ ] Testing gate: 80%+ coverage (TDT01)

**Phase-Specific Gates:**

**Phase 1 (Foundation):**
- [ ] ChatScreen renders without errors
- [ ] Messages display correctly (user/AVA variants)
- [ ] Input field and send button work
- [ ] Messages saved to database
- [ ] UI tests pass

**Phase 2 (NLU Integration):**
- [ ] IntentClassifier initialized successfully
- [ ] Candidate intents loaded (built-in + user-taught)
- [ ] Classification completes <100ms
- [ ] Confidence badges render correctly (🟢🟡🔴)
- [ ] Auto-prompt on low confidence (<0.5)
- [ ] Unit tests pass (≥80% coverage)

**Phase 3 (Teach-AVA):**
- [ ] Bottom sheet renders (~1/3 screen)
- [ ] Long-press shows context menu
- [ ] Training example saves to database
- [ ] Success toast appears
- [ ] Integration tests pass

**Phase 4 (History Overlay):**
- [ ] Overlay slides in from right (25% width)
- [ ] Conversation list displays correctly
- [ ] Voice trigger "show history" works
- [ ] Conversation switching works
- [ ] UI tests pass

**Phase 5 (Voice + Polish):**
- [ ] Voice input works (if VOS4 ready) OR decision to defer documented
- [ ] Conversation mode setting functional
- [ ] End-to-end <500ms (measured)
- [ ] 80%+ test coverage maintained
- [ ] All P0 bugs fixed

---

## IDE Loop Integration

Each task follows **IDEACODE IDE Loop**:

### 1. Implement Phase
**Specialist builds feature**
- @ui-expert, @kotlin-expert, @database-expert, @nlu-expert
- Write code following Kotlin coding standards
- Add inline comments for complex logic
- Create or update files as specified in task

### 2. Defend Phase (MANDATORY)
**@test-specialist creates tests**
- Auto-invoked after each implementation task
- Unit tests for logic
- Integration tests for database/NLU
- UI tests for Compose components
- Verify ≥80% coverage
- **Blocker:** Cannot proceed if tests fail or coverage <80%

### 3. Evaluate Phase
**Verify requirements, user approval**
- Check quality gates for task
- Review against acceptance criteria
- Update living docs (notes.md, decisions.md)
- Get user approval if needed (for major decisions)
- **Blocker:** Cannot proceed if requirements not met

### 4. Commit Phase
**Lock in progress**
- Atomic commit (conventional format)
- Push to main branch
- Update progress.md
- Mark task as complete

**Commit Message Format:**
```
<type>(<scope>): <description>

<body>

Closes #<task-id>

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

**Example:**
```
feat(chat-ui): Add message bubbles with confidence badges

- User messages: right-aligned, blue background
- AVA messages: left-aligned, grey background
- Confidence badges: 🟢🟡🔴 based on threshold
- Accessibility: contentDescription for badges

Closes #P2T05

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

---

## Next Steps

1. **Review Task Breakdown for Completeness**
   - Verify all 35 tasks cover full specification
   - Confirm dependencies correct
   - Approve parallel execution plan

2. **Approve Execution Plan**
   - Confirm multi-agent deployment strategy
   - Agree on critical path (28 hours parallel vs 60 hours sequential)
   - Identify any missing tasks

3. **Run `/idea2.implement`** to Execute with IDE Loop
   - Deploy agents in parallel batches
   - Enforce quality gates at each phase
   - Update living docs continuously
   - Execute atomic commits after each task

---

**Last Updated:** 2025-01-28
**Status:** ✅ Task Breakdown Complete, Ready for Execution
**Approved By:** Pending user review

---

## Appendix: Task Dependencies Visualization

```
Phase 1 (Foundation):
P1T01 ─────┬─────> P1T03 ────> P1T05 ────> P1T07 ────> Phase 2
           │                      ↑            ↑
P1T02 ─────┘                      │            │
                                  │            │
P1T04 ────────────────────────────┴────> P1T06

Phase 2 (NLU Integration):
P1T07 ────> P2T01 ────> P2T02 ────> P2T03 ─────┬────> P2T05 ────> P2T06 ────> Phase 3
                                                │        ↑
                                                └──> P2T04

                        P2T03 ────> P2T07 (parallel)

                        All P2 ────> P2T08 (testing)

Phase 3 (Teach-AVA):
P2T06 ────> P3T01 ────> P3T02 ────> P3T03 ────> P3T04 ────┬────> Phase 4
P2T08 ────────────────────────────────────────────────────┘

                        All P3 ────> P3T05 (testing)

Phase 4 (History Overlay):
P3T05 ────> P4T01 ────> P4T02 ────> P4T03 ────> P4T04 ────┬────> Phase 5
                                                            │
                        All P4 ────> P4T05 (testing) ──────┘

Phase 5 (Voice + Polish):
                        P5T03 (parallel, no dependencies)

P4T05 ────> P5T01 ────> P5T02 (optional, VOS4)

P2T07 ────> P5T04 (performance optimization)

All Phase 1-5 ────> P5T05 ────> P5T06 ────> Testing & Docs

Testing & Docs:
P5T06 ────> TDT01 ────> TDT02 ────> TDT03 (final commit)
```

---

**Total Estimated Effort:** 60 hours sequential | 28 hours parallel
**Recommended Approach:** Parallel multi-agent deployment (30% time savings)
**Critical Path:** 28 hours
**Agents Required:** 6 specialists (ui-expert, kotlin-expert, database-expert, nlu-expert, test-specialist, documentation-specialist)
