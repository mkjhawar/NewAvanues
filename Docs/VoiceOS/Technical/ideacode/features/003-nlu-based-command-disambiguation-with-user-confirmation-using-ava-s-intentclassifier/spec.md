# Delta for NLU Command Disambiguation Specification

**Feature:** NLU-based command disambiguation with user confirmation using AVA's IntentClassifier
**Feature ID:** 003
**Affected Spec:** `specs/voiceoscore/command-processing.md`
**Created:** 2025-11-12
**Author:** Manoj Jhawar

---

## Summary

This delta integrates AVA's ONNX-based NLU system (IntentClassifier with MobileBERT) into VoiceOS command processing to provide intelligent command disambiguation when exact matches are not found. The system will use machine learning to suggest the most likely matching commands and ask users to confirm before execution.

**Current Problem:**
- Fuzzy string matching causes wrong command execution ("Clear history" → "Clear")
- Real-time element search works but has no confidence scoring
- No way to ask user for confirmation when uncertain

**Solution:**
- Use AVA's IntentClassifier to rank possible command matches
- Show top 3 suggestions with confidence scores
- Ask user to confirm before executing uncertain commands
- Learn from user confirmations to improve future predictions

---

## ADDED Requirements

### Requirement: AVA NLU Integration

The system SHALL integrate AVA's NLU Features module into VoiceOSCore for intelligent command classification.

**Rationale:** AVA's ONNX Runtime + MobileBERT provides <100ms on-device NLU with 99.2% accuracy, enabling intelligent command disambiguation without cloud API calls.

**Priority:** High

**Acceptance Criteria:**
- [ ] AVA NLU Features module added as dependency to VoiceOSCore
- [ ] IntentClassifier initializes successfully with MobileBERT model
- [ ] Model loading completes in <500ms (cold start)
- [ ] Inference completes in <100ms (warm)
- [ ] Memory footprint <100MB (27.5MB model + 50MB inference RAM)

#### Scenario: NLU classifier initialization

**GIVEN** VoiceOSCore module is starting
**WHEN** CommandDisambiguator initializes
**THEN** AVA's IntentClassifier loads MobileBERT model from assets
**AND** initialization completes within 500ms
**AND** classifier is ready for inference

**Test Data:**
- Model path: `assets/models/mobilebert_int8.onnx`
- Model size: 25.5 MB

**Expected Result:**
- Model loads successfully
- No OutOfMemoryError
- Classifier returns ready state

---

### Requirement: Command Disambiguation with NLU

The system SHALL use NLU to rank candidate commands when no exact match is found in the database.

**Rationale:** Machine learning provides better command matching than fuzzy string matching, with confidence scores to enable intelligent fallback strategies.

**Priority:** High

**Acceptance Criteria:**
- [ ] When exact command match fails, system collects candidate commands from database
- [ ] IntentClassifier ranks candidates by semantic similarity to user's utterance
- [ ] System returns top 3 suggestions with confidence scores
- [ ] Confidence >= user threshold (default 70%): Execute immediately WITHOUT confirmation
- [ ] Confidence < user threshold (default 70%): Ask for confirmation with multi-option selection
- [ ] Fallback to real-time element search if no candidates exist
- [ ] Display actual confidence percentage to user in confirmation dialog

#### Scenario: High confidence auto-execution

**GIVEN** user says "Clear history"
**AND** database has commands: ["Clear", "Clear history", "Clear cache"]
**AND** no exact match exists for "Clear history"
**AND** user's confidence threshold is set to 70%
**WHEN** NLU classifier ranks candidates
**THEN** top suggestion is "Clear history" with confidence 92%
**AND** 92% >= 70% threshold
**AND** system executes "Clear history" immediately WITHOUT asking
**AND** shows brief toast: "Executed 'Clear history' (92% confidence)"

**Test Data:**
- User utterance: "Clear history"
- Candidate commands: ["Clear", "Clear history", "Clear cache"]
- User threshold: 70%

**Expected Result:**
- Confidence scores: {"Clear history": 92%, "Clear": 45%, "Clear cache": 31%}
- Command executes immediately (no confirmation needed)
- Toast notification shows command + confidence %
- Execution within 200ms

#### Scenario: Low confidence requires confirmation

**GIVEN** user says "Open settings"
**AND** database has commands: ["Open", "Settings", "Open menu"]
**AND** user's confidence threshold is set to 70%
**WHEN** NLU classifier ranks candidates
**THEN** top suggestion is "Settings" with confidence 65%
**AND** 65% < 70% threshold
**AND** system shows "Did you mean:"
  - "Settings (65%)"
  - "Open (48%)"
  - "Open menu (42%)"
**AND** user can select via voice ("option 1") or UI tap

**Test Data:**
- User utterance: "Open settings"
- Candidate commands: ["Open", "Settings", "Open menu", "Configure"]
- User threshold: 70%

**Expected Result:**
- Top confidence 65% < 70% threshold
- Confirmation dialog displayed with 3 options
- Each option shows confidence percentage
- User can select, cancel, or wait for timeout

#### Scenario: User adjusts confidence threshold

**GIVEN** user is in VoiceOS settings
**AND** navigates to "Voice Commands" → "NLU Confidence Threshold"
**WHEN** user adjusts slider from 70% to 85%
**THEN** threshold is saved to SharedPreferences
**AND** future commands use 85% threshold
**AND** more commands will require confirmation (higher bar)

**Test Data:**
- Original threshold: 70%
- New threshold: 85%

**Expected Result:**
- Threshold updates immediately
- Commands with 70-84% confidence now require confirmation
- Commands with ≥85% confidence execute immediately

---

### Requirement: User Confirmation Interface

The system SHALL provide voice and UI interfaces for users to confirm command disambiguation suggestions with confidence percentages displayed.

**Rationale:** Users must be able to quickly confirm or reject suggestions through their preferred input method (voice or touch). Showing confidence percentages helps users make informed decisions.

**Priority:** High

**Acceptance Criteria:**
- [ ] Confirmation overlay displays suggested command(s) with confidence percentages
- [ ] Each option shows confidence in format: "Command Name (XX%)"
- [ ] Voice confirmation supports: "yes", "no", "option 1", "option 2", "option 3", "cancel"
- [ ] UI confirmation provides tap targets for each option
- [ ] Confirmation times out after 10 seconds (cancels, does NOT auto-execute)
- [ ] Rejection triggers fallback to real-time element search

#### Scenario: Confirmation dialog with percentages (below threshold)

**GIVEN** user says "Open settings"
**AND** top suggestion is "Settings" with confidence 65%
**AND** user threshold is 70%
**AND** 65% < 70% (below threshold, requires confirmation)
**WHEN** system displays confirmation dialog
**THEN** dialog shows:
  - "Did you mean:"
  - Option 1: "Settings (65%)" ← Highest confidence
  - Option 2: "Open (48%)"
  - Option 3: "Open menu (42%)"
**AND** user can select via voice or tap

**Test Data:**
- Threshold: 70%
- Suggestions: [("Settings", 65%), ("Open", 48%), ("Open menu", 42%)]

**Expected Result:**
- Dialog displays all 3 options with percentages
- Options sorted by confidence (highest first)
- User can select any option or cancel

#### Scenario: Voice confirmation

**GIVEN** system shows "Did you mean 'Clear history' (65%)?"
**WHEN** user says "yes"
**THEN** system executes "Clear history" command
**AND** stores user confirmation for learning
**AND** confirmation overlay dismisses

**Test Data:**
- Suggested command: "Clear history"
- User voice input: "yes"

**Expected Result:**
- Command executes immediately
- Confirmation stored in training database
- Overlay dismissed within 100ms

#### Scenario: UI tap confirmation

**GIVEN** system shows multi-option prompt with 3 choices
**WHEN** user taps "option 2"
**THEN** system executes selected command
**AND** stores user choice for learning

---

### Requirement: Command Learning from Confirmations

The system MUST store user confirmations to improve future NLU predictions.

**Rationale:** User confirmations are valuable training data that can be used to fine-tune the NLU model or adjust confidence thresholds for specific command patterns.

**Priority:** Medium

**Acceptance Criteria:**
- [ ] Each confirmation creates a training example (utterance → confirmed command)
- [ ] Training examples stored in Room database
- [ ] Examples include: utterance, confirmed command, timestamp, confidence score
- [ ] Future: Examples can be exported for model fine-tuning

#### Scenario: Store confirmation for learning

**GIVEN** user confirms "Clear history" for utterance "Clear history"
**WHEN** confirmation is accepted
**THEN** system stores training example:
  - utterance: "Clear history"
  - intent: "Clear history"
  - timestamp: current time
  - original_confidence: 0.92
  - user_confirmed: true
**AND** example can be retrieved for future fine-tuning

---

### Requirement: User-Configurable Confidence Threshold

The system SHALL provide a settings interface for users to adjust the NLU confidence threshold.

**Rationale:** Different users have different preferences for automation vs. confirmation. Power users may want 90%+ threshold for maximum accuracy, while casual users may prefer 60% for more automation.

**Priority:** High

**Acceptance Criteria:**
- [ ] Settings screen has "Voice Commands" section with "NLU Confidence Threshold" setting
- [ ] Threshold adjustable via slider (range: 50% - 95%, step: 5%)
- [ ] Threshold also adjustable via editable text box (accepts 50-95 integer)
- [ ] Slider and text box are synchronized (changing one updates the other)
- [ ] Current threshold displayed as percentage (e.g., "70%")
- [ ] Default threshold is 70%
- [ ] Threshold saved to SharedPreferences
- [ ] Real-time preview: "Commands with ≥70% confidence will execute immediately"

#### Scenario: Adjust threshold via slider

**GIVEN** user is in VoiceOS Settings → Voice Commands
**AND** current threshold is 70%
**WHEN** user drags slider to 85%
**THEN** text box updates to show "85"
**AND** preview text updates to "Commands with ≥85% confidence will execute immediately"
**AND** threshold is saved immediately

**Test Data:**
- Initial threshold: 70%
- Slider position: 85%

**Expected Result:**
- Text box shows "85"
- Preview text shows "≥85%"
- SharedPreferences updated
- No need to tap "Save" button (auto-save)

#### Scenario: Adjust threshold via text box

**GIVEN** user is in confidence threshold settings
**AND** current threshold is 70%
**WHEN** user taps text box and types "55"
**THEN** slider position updates to 55%
**AND** preview text updates to "Commands with ≥55% confidence will execute immediately"
**AND** threshold is saved immediately

**Test Data:**
- Initial threshold: 70%
- Text input: "55"

**Expected Result:**
- Slider moves to 55% position
- Preview text shows "≥55%"
- SharedPreferences updated

#### Scenario: Invalid text input validation

**GIVEN** user is in confidence threshold settings
**WHEN** user types "120" (out of range)
**THEN** system shows error "Threshold must be between 50% and 95%"
**AND** text box reverts to last valid value
**AND** slider position unchanged

**Test Data:**
- Invalid inputs: "120", "30", "abc", "-10"

**Expected Result:**
- Error message displayed
- Value reverts to previous valid setting
- No change to SharedPreferences

#### Scenario: Settings UI layout

**GIVEN** user opens VoiceOS Settings → Voice Commands
**THEN** NLU Confidence Threshold section displays:

```
┌─────────────────────────────────────────────┐
│ Voice Commands                               │
├─────────────────────────────────────────────┤
│ NLU Confidence Threshold                     │
│                                               │
│ Commands with ≥70% confidence will execute   │
│ immediately without asking for confirmation. │
│                                               │
│ [50%]──────●────────────────────[95%]        │
│              70                               │
│         [   70   ] %                         │
│         ↑ Editable text box                  │
│                                               │
│ Higher threshold = More confirmations        │
│ Lower threshold = More automation            │
└─────────────────────────────────────────────┘
```

**Components:**
- Title: "NLU Confidence Threshold"
- Explanation text (dynamic based on threshold)
- Slider: 50% to 95%, step 5%
- Text box: Editable, shows numeric value
- Suffix label: "%"
- Help text: Explains higher/lower trade-offs

---

### Requirement: Command Processing Flow Update

The system SHALL update VoiceCommandProcessor to integrate NLU disambiguation before real-time search.

**Rationale:** NLU provides better accuracy than both fuzzy matching and blind real-time search, so it should be attempted first.

**Priority:** High

**Acceptance Criteria:**
- [ ] Fuzzy matching completely removed from findMatchingCommand()
- [ ] New command priority: 1) Exact match, 2) NLU disambiguation, 3) Real-time search, 4) Database commands, 5) Static commands
- [ ] NLU disambiguation only called when exact match fails
- [ ] Real-time search only called when NLU fails or user rejects suggestions
- [ ] Processing flow completes within 200ms (excluding user confirmation wait)

#### Scenario: Updated command processing flow

**GIVEN** user says "Clear history"
**AND** no exact match in database
**WHEN** VoiceCommandProcessor processes command
**THEN** flow executes in order:
  1. findMatchingCommand() returns null (no exact match)
  2. tryNLUDisambiguation() returns suggestion with 0.92 confidence
  3. User confirmation requested
  4. User confirms
  5. Command executes
**AND** real-time search is skipped
**AND** total processing time <200ms (excluding confirmation wait)

---

## MODIFIED Requirements

### Requirement: VoiceCommandProcessor.findMatchingCommand()

**REMOVED:**
- Fuzzy matching logic using substring `contains()`
- Synonyms fuzzy matching

**REASONING:** Fuzzy matching caused incorrect command execution. NLU provides semantic matching with confidence scores, eliminating need for string-based fuzzy matching.

---

## REMOVED Requirements

> No requirements completely removed, only fuzzy matching logic within existing requirement

---

## Architecture Design

### Component Structure

```
VoiceOSCore/
├── nlu/
│   ├── CommandDisambiguator.kt           # Wraps AVA's IntentClassifier
│   ├── ConfirmationManager.kt            # Handles user confirmation flow
│   ├── CommandLearningRepository.kt      # Stores training examples
│   └── entities/
│       └── CommandTrainingExample.kt     # Room entity for learning
└── scraping/
    └── VoiceCommandProcessor.kt          # Updated flow: exact → NLU → realtime → db → static
```

### Dependencies

**New Gradle Dependency:**
```kotlin
// VoiceOSCore/build.gradle.kts
dependencies {
    implementation(project(":AVA:Features:NLU"))  // AVA NLU module
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")
}
```

### Class Design

```kotlin
// CommandDisambiguator.kt
class CommandDisambiguator(
    context: Context,
    private val database: VoiceOSAppDatabase
) {
    private val intentClassifier = IntentClassifier.getInstance(context)

    suspend fun disambiguateCommand(
        utterance: String,
        packageName: String
    ): DisambiguationResult {
        // 1. Get candidate commands from database
        val candidates = database.generatedCommandDao()
            .getCommandsForApp(packageName)
            .map { it.commandText }

        if (candidates.isEmpty()) {
            return DisambiguationResult.NoCandidates
        }

        // 2. Run NLU classification
        val result = intentClassifier.classifyIntent(
            utterance = utterance,
            candidateIntents = candidates
        )

        // 3. Return top 3 with confidence scores
        when (result) {
            is Result.Success -> {
                val top3 = result.data.allScores
                    .entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .map { (intent, score) ->
                        SuggestedCommand(intent, score)
                    }

                return if (top3.first().confidence >= 0.7) {
                    DisambiguationResult.HighConfidence(top3.first())
                } else {
                    DisambiguationResult.MultipleOptions(top3)
                }
            }
            is Result.Error -> {
                return DisambiguationResult.Error(result.message)
            }
        }
    }
}

sealed class DisambiguationResult {
    data class HighConfidence(val suggestion: SuggestedCommand) : DisambiguationResult()
    data class MultipleOptions(val suggestions: List<SuggestedCommand>) : DisambiguationResult()
    object NoCandidates : DisambiguationResult()
    data class Error(val message: String) : DisambiguationResult()
}

data class SuggestedCommand(
    val commandText: String,
    val confidence: Float
)
```

---

## Impact Analysis

### Breaking Changes

None. This is an enhancement to existing command processing flow.

### Non-Breaking Changes

- Addition of AVA NLU dependency
- New CommandDisambiguator component
- New ConfirmationManager component
- New Room entities for training examples
- Updated VoiceCommandProcessor flow
- New confirmation overlay UI

### Migration Required

- [ ] No database migration required (new tables only)
- [ ] AVA NLU model must be bundled in VoiceOSCore assets
- [ ] Existing commands continue to work via exact match

---

## Testing Requirements

### Unit Tests

```kotlin
// CommandDisambiguatorTest.kt
@Test fun `high confidence returns single suggestion`()
@Test fun `low confidence returns top 3 options`()
@Test fun `no candidates returns NoCandidates`()
@Test fun `inference completes under 100ms`()

// ConfirmationManagerTest.kt
@Test fun `voice yes confirms top suggestion`()
@Test fun `voice option 2 selects second option`()
@Test fun `timeout with high confidence executes command`()
@Test fun `timeout with low confidence cancels`()

// CommandLearningRepositoryTest.kt
@Test fun `confirmation stores training example`()
@Test fun `examples can be retrieved for export`()
```

### Integration Tests

```kotlin
// NLUCommandFlowTest.kt
@Test fun `exact match skips NLU`()
@Test fun `no exact match triggers NLU`()
@Test fun `NLU failure falls back to realtime search`()
@Test fun `user rejection falls back to realtime search`()
@Test fun `confirmed commands execute correctly`()
```

### Coverage Goals

- Unit test coverage: ≥90% for NLU components
- Integration test coverage: ≥80% for command flow
- E2E test coverage: All disambiguation scenarios

---

## Performance Impact

**Expected Performance:**
- Model initialization: <500ms (one-time at app start)
- NLU inference: <100ms per command
- Confirmation display: <100ms
- Total command processing: <200ms (excluding user confirmation wait)

**Benchmarking Required:**
- [ ] Measure cold start NLU initialization time
- [ ] Measure warm NLU inference time across 100 commands
- [ ] Verify no memory leaks after 1000 classifications
- [ ] Verify battery impact <5% per hour of continuous use

---

## Security Impact

**Privacy Considerations:**
- ✅ All NLU processing is 100% on-device (no cloud API calls)
- ✅ User utterances never leave device
- ✅ Training examples stored locally in encrypted Room database

**Security Review Required:** No (uses existing AVA security model)

---

## Documentation Updates Required

- [ ] Update Chapter 3 (VoiceOSCore Module) with NLU integration section
- [ ] Update developer manual with CommandDisambiguator API
- [ ] Add NLU performance benchmarks to Appendix
- [ ] Update voice command testing procedures

---

## Merge Instructions

**When this feature is archived:**

1. Merge this delta into `specs/voiceoscore/command-processing.md`
2. Update VoiceOS Developer Manual Chapter 3
3. Create ADR-007: NLU Integration for Command Disambiguation

---

## Dependencies on AVA

**Required from AVA:**
- `AVA/Features/NLU` module (KMP library)
- MobileBERT INT8 ONNX model (25.5 MB)
- ONNX Runtime Mobile 1.17.0

**Integration Path:**
```bash
# Option 1: Git submodule
git submodule add https://github.com/augmentalis/ava.git AVA
# Gradle: implementation(project(":AVA:Features:NLU"))

# Option 2: Maven artifact (future)
# implementation("com.augmentalis.ava:nlu:1.0.0")
```

---

**Template Version:** 6.0.0 (customized for NLU integration)
**Last Updated:** 2025-11-12
