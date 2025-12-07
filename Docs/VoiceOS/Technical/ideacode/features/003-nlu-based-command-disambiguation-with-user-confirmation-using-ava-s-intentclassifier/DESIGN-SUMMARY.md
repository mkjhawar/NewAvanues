# NLU Command Disambiguation - Design Summary

**Feature ID:** 003
**Status:** Design Complete - Awaiting AVA NLU Validation
**Created:** 2025-11-12

---

## Overview

This feature integrates AVA's ONNX-based NLU (IntentClassifier with MobileBERT) into VoiceOS command processing to provide intelligent command disambiguation with user-configurable confidence thresholds.

---

## The Problem We're Solving

### Current Issues:
1. ✅ **FIXED:** Fuzzy matching caused wrong commands ("Clear history" → "Clear")
2. ⚠️ **REMAINING:** Real-time search lacks confidence scoring
3. ⚠️ **REMAINING:** No way to ask user "Did you mean...?"

### Why Not Just Real-Time Search?
Real-time element search works but:
- No confidence scoring (can't tell if match is good)
- No ranking of multiple matches
- No user confirmation when uncertain
- Doesn't learn from user choices

---

## Proposed Solution

### User Experience Flow

**Scenario 1: High Confidence (≥ threshold) - Auto-Execute**
```
User: "Clear history"
System: [NLU scores: "Clear history" 92%, "Clear" 45%, "Clear cache" 31%]
System: [92% ≥ 70% threshold]
System: ✓ Executes "Clear history" immediately
System: [Toast] "Executed 'Clear history' (92% confidence)"
```

**Scenario 2: Low Confidence (< threshold) - Ask User**
```
User: "Open settings"
System: [NLU scores: "Settings" 65%, "Open" 48%, "Open menu" 42%]
System: [65% < 70% threshold]
System: [Dialog] "Did you mean:"
         • Settings (65%)
         • Open (48%)
         • Open menu (42%)
User: [Voice] "option 1" OR [Tap] Settings
System: ✓ Executes "Settings"
System: [Stores learning example for future improvement]
```

---

## Key Features

### 1. Smart Command Ranking
- Uses ML (MobileBERT) instead of string matching
- Semantic similarity: "Delete history" matches "Clear history"
- Confidence scores: 0-100% for every candidate

### 2. User-Configurable Threshold
- Default: 70%
- Range: 50% - 95% (step 5%)
- Adjustable via slider OR text box
- Real-time preview of behavior

**Settings UI:**
```
┌─────────────────────────────────────────────┐
│ NLU Confidence Threshold                     │
│                                               │
│ Commands with ≥70% confidence will execute   │
│ immediately without asking for confirmation. │
│                                               │
│ [50%]──────●────────────────────[95%]        │
│         [   70   ] %                         │
│                                               │
│ Higher threshold = More confirmations        │
│ Lower threshold = More automation            │
└─────────────────────────────────────────────┘
```

### 3. Confirmation Dialog
- Shows top 3 suggestions with percentages
- Voice input: "yes", "no", "option 1", "option 2", "option 3"
- UI tap targets for each option
- 10 second timeout → Cancel (NOT auto-execute)

### 4. Learning System
- Stores user confirmations in database
- Training examples: (utterance, confirmed_command, confidence, timestamp)
- Future: Export for model fine-tuning

---

## Technical Architecture

### New Components

**1. CommandDisambiguator**
```kotlin
class CommandDisambiguator(
    context: Context,
    database: VoiceOSAppDatabase
) {
    suspend fun disambiguateCommand(
        utterance: String,
        packageName: String
    ): DisambiguationResult
}

sealed class DisambiguationResult {
    data class HighConfidence(suggestion: SuggestedCommand)
    data class MultipleOptions(suggestions: List<SuggestedCommand>)
    object NoCandidates
    data class Error(message: String)
}
```

**2. ConfirmationManager**
```kotlin
class ConfirmationManager(context: Context) {
    suspend fun showConfirmationDialog(
        suggestions: List<SuggestedCommand>
    ): ConfirmationResult
}
```

**3. CommandLearningRepository**
```kotlin
class CommandLearningRepository(database: VoiceOSAppDatabase) {
    suspend fun storeTrainingExample(
        utterance: String,
        confirmedCommand: String,
        originalConfidence: Float
    )
}
```

### Updated Command Flow

**Before (v4.2.0 - Current):**
```
1. Exact match → Execute
2. Real-time search → Execute
3. Database commands → Execute
4. Static commands → Execute
```

**After (v4.3.0 - Proposed):**
```
1. Exact match → Execute immediately
2. NLU disambiguation:
   a. If confidence ≥ threshold → Execute immediately + toast
   b. If confidence < threshold → Ask user + show percentages
   c. User confirms → Execute + store learning example
   d. User rejects → Fall back to real-time search
3. Real-time search → Execute
4. Database commands → Execute
5. Static commands → Execute
```

---

## Performance Requirements

| Metric | Budget | Expected |
|--------|--------|----------|
| Model loading (cold) | <500ms | ~300ms |
| Inference (warm) | <100ms | ~50ms |
| Confirmation display | <100ms | ~50ms |
| Total processing | <200ms | ~150ms |
| Memory footprint | <100MB | ~77MB |
| Battery impact | <5%/hour | ~2%/hour |

---

## Dependencies

### From AVA:
- `AVA/Features/NLU` module (KMP)
- MobileBERT INT8 ONNX model (25.5 MB)
- ONNX Runtime Mobile 1.17.0

### Integration:
```gradle
// VoiceOSCore/build.gradle.kts
dependencies {
    implementation(project(":AVA:Features:NLU"))
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")
}
```

---

## Privacy & Security

✅ **100% On-Device:**
- All NLU processing local (no cloud API calls)
- User utterances never leave device
- Training examples stored in encrypted Room database

✅ **No New Permissions:**
- Uses existing VoiceOS permissions
- No additional privacy concerns

---

## Testing Strategy

### Unit Tests (90%+ coverage)
- CommandDisambiguator ranking logic
- Threshold comparison logic
- Settings validation
- Learning repository

### Integration Tests (80%+ coverage)
- End-to-end command flow
- NLU → Confirmation → Execution
- User rejection → Real-time search fallback
- Learning example storage

### Performance Tests
- 100 consecutive inferences (<100ms each)
- 1000 inferences (no memory leaks)
- Battery drain measurement

---

## Implementation Phases

### Phase 0: Validation (Current)
- [ ] Validate AVA NLU module compatibility
- [ ] Run performance benchmarks
- [ ] Make Go/No-Go decision
- **Blocker:** Implementation cannot begin until validation passes

### Phase 1: Core NLU Integration (If validation passes)
- [ ] Add AVA NLU dependency
- [ ] Implement CommandDisambiguator
- [ ] Update VoiceCommandProcessor flow
- [ ] Add threshold to SharedPreferences

### Phase 2: UI Components
- [ ] Implement confirmation dialog
- [ ] Add settings screen with slider + text box
- [ ] Show confidence percentages

### Phase 3: Learning System
- [ ] Implement CommandLearningRepository
- [ ] Store training examples
- [ ] Add export functionality

### Phase 4: Testing & Polish
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Performance testing
- [ ] User acceptance testing

---

## Risks & Mitigation

### Risk 1: AVA NLU Not Ready
**Impact:** High (blocks entire feature)
**Probability:** Medium
**Mitigation:** Validation testing before implementation
**Contingency:** Alternative solutions documented (see AVA-NLU-VALIDATION.md)

### Risk 2: Performance Issues
**Impact:** Medium (degrades UX)
**Probability:** Low (AVA already tested)
**Mitigation:** Performance benchmarks in validation
**Contingency:** Adjust threshold ranges, optimize model

### Risk 3: Model Size Too Large
**Impact:** Low (app size increase)
**Probability:** Low (25.5 MB acceptable)
**Mitigation:** Model already quantized (INT8)
**Contingency:** Consider smaller model (DistilBERT)

---

## Success Metrics

### Functional
- [ ] NLU inference <100ms
- [ ] Confidence scores accurate (semantic similarity)
- [ ] Threshold settings work correctly
- [ ] User confirmations stored
- [ ] 0 crashes related to NLU

### User Experience
- [ ] Fewer wrong command executions (vs fuzzy matching)
- [ ] User satisfaction with confirmation prompts
- [ ] Threshold customization used by power users
- [ ] Faster command execution than pure confirmation approach

### Technical
- [ ] 90%+ unit test coverage
- [ ] 80%+ integration test coverage
- [ ] No memory leaks
- [ ] <5% battery impact

---

## Documentation

### Required Updates:
- [ ] Chapter 3: VoiceOSCore Module (add NLU section)
- [ ] Developer Manual: CommandDisambiguator API
- [ ] User Guide: Confidence threshold settings
- [ ] Architecture Docs: ADR-007 NLU Integration

### New Documents:
- [x] spec.md - Delta specification
- [x] proposal.md - Feature proposal
- [x] AVA-NLU-VALIDATION.md - Validation checklist
- [x] DESIGN-SUMMARY.md - This document

---

## Decision Required

**Before proceeding with implementation, we need:**

1. ✅ Review this design summary
2. ⏳ Run AVA NLU validation tests
3. ⏳ Verify all 6 critical requirements pass
4. ⏳ Make Go/No-Go decision
5. ⏳ If GO: Proceed with Phase 1 implementation
6. ⏳ If NO-GO: Choose alternative solution

**Timeline:**
- Design: Complete ✅
- Validation: 1-2 days ⏳
- Decision: After validation ⏳
- Implementation: 1-2 weeks (if GO)

---

## Files in This Feature

```
.ideacode-v2/features/003-nlu-based-command-disambiguation.../
├── proposal.md                  # Business justification
├── spec.md                      # Technical specification
├── AVA-NLU-VALIDATION.md       # Validation checklist
└── DESIGN-SUMMARY.md           # This document
```

---

**Status:** ✅ Design Complete, ⏳ Awaiting Validation
**Next Step:** Run AVA NLU validation tests
**Blocker:** Cannot implement until validation passes

---

**Created:** 2025-11-12
**Author:** Manoj Jhawar
**Last Updated:** 2025-11-12
