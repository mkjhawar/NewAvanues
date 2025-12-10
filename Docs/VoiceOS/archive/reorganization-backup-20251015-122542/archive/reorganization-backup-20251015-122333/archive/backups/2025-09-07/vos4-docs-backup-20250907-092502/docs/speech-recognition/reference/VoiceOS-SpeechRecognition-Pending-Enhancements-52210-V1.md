/**
 * SpeechRecognition Pending Enhancements Documentation
 * Path: /docs/modules/speechrecognition/SpeechRecognition-Pending-Enhancements.md
 * 
 * Created: 2025-01-27
 * Author: Manoj Jhawar
 * Status: PENDING IMPLEMENTATION
 * Module: SpeechRecognition
 * 
 * Purpose: Document all pending enhancements for VoskService and SpeechRecognition module
 * based on analysis of missing features from updated VoskSpeechRecognitionService
 * 
 * IMPORTANT: These are PLANNED enhancements, NOT implemented features
 */

# SpeechRecognition Module - Pending Enhancements

## Status: PENDING IMPLEMENTATION
**Analysis Date:** 2025-01-27  
**Estimated Implementation Time:** 44 hours total  
**Priority:** HIGH - Critical functionality gaps

## Executive Summary

After comparing the current VOS4 VoskService implementation with the updated VoskSpeechRecognitionService, we've identified 5 critical feature gaps that need implementation. These enhancements will provide:
- **95% command accuracy** (up from 70%)
- **70% battery savings** in sleep mode
- **Zero audio gaps** during mode switching
- **Full dictation control** with session management
- **Dynamic command updates** without service restart

## 🔴 Critical Pending Enhancements

### 1. A&C Hybrid Dual Recognizer System [12-16 hours]

**Status:** NOT IMPLEMENTED  
**Priority:** CRITICAL  
**Impact:** 30-40% accuracy improvement, instant mode switching

#### What's Missing
- Current VOS4 uses single recognizer for all modes
- No grammar optimization for commands vs dictation
- Cannot run parallel recognition tasks

#### Pending Implementation
```kotlin
class VoskService {
    // PENDING: Dual recognizer instances
    private var commandRecognizer: Recognizer? = null    // Grammar-constrained
    private var dictationRecognizer: Recognizer? = null  // Full vocabulary
    private var activeRecognizer: Recognizer? = null     // Currently active
    
    // PENDING: Resource management
    private val recognizerLock = Mutex()
    private var dictationLastUsed = 0L
    private var dictationReleaseJob: Job? = null
}
```

#### Key Features to Implement
- [ ] Dual recognizer initialization (command + dictation)
- [ ] Lazy loading for dictation recognizer
- [ ] Memory pressure handling (ComponentCallbacks2)
- [ ] Seamless recognizer switching without audio gaps
- [ ] Resource monitor for automatic cleanup
- [ ] Fallback mechanisms for error recovery

#### Expected Benefits
- Command accuracy: 70% → 95%
- Mode switch time: 200ms → 0ms (after first use)
- Memory usage: Adaptive 65-95MB based on usage

---

### 2. Grammar Constraints with JSON Compilation [8-10 hours]

**Status:** NOT IMPLEMENTED  
**Priority:** CRITICAL  
**Impact:** 95% command recognition accuracy

#### What's Missing
- No grammar constraints in current implementation
- Commands compete with full vocabulary
- No smart rebuild logic

#### Pending Implementation
```kotlin
class GrammarManager {
    // PENDING: Grammar components
    private val coreCommands = setOf<String>()      // System commands
    private val dynamicCommands = mutableSetOf<String>() // UI-scraped
    private var cachedGrammarJson: String? = null
    private var grammarDirty = false
    
    // PENDING: Smart rebuild logic
    fun shouldRebuild(oldCommands: Set<String>, newCommands: Set<String>): Boolean {
        val changeRatio = (added + removed).size / oldCommands.size
        return changeRatio > 0.2 // 20% threshold
    }
}
```

#### Key Features to Implement
- [ ] Core + dynamic command separation
- [ ] Grammar JSON generation with Gson
- [ ] Smart rebuild logic (20% change threshold)
- [ ] Grammar caching with 5-minute TTL
- [ ] Frequency tracking for optimization
- [ ] Context-aware command selection
- [ ] Multi-language grammar support

#### Expected Benefits
- Command accuracy: 70% → 95%
- Recognition latency: 200ms → 50ms
- Reduced false positives

---

### 3. Sleep/Wake State Management with Configuration [6-8 hours]

**Status:** NOT IMPLEMENTED  
**Priority:** HIGH  
**Impact:** 70% battery savings in sleep mode

#### What's Missing
- No sleep/wake functionality
- No mute/unmute commands
- No wake word detection
- No power state management

#### Pending Implementation
```kotlin
class SleepWakeManager {
    // PENDING: Power states
    private enum class PowerState {
        ACTIVE,    // Full functionality
        STANDBY,   // Commands only (no dictation)
        SLEEPING   // Wake word only
    }
    
    // PENDING: Configurable settings (ObjectBox)
    @Entity
    data class SleepWakeSettings(
        var toStandbyMs: Long = 60_000L,
        var toSleepMs: Long = 300_000L,
        var wakeWords: String = "", // JSON array
        var audioFeedbackEnabled: Boolean = true,
        var statusBarIconEnabled: Boolean = true
    )
}
```

#### Key Features to Implement
- [ ] 3-state power management system
- [ ] Wake word recognizer (minimal 5MB)
- [ ] Configurable timeouts via settings UI
- [ ] Localized wake words ("Hey Ava", "Hello Ava")
- [ ] Audio feedback (chimes) with settings
- [ ] Status bar icon with 3 styles
- [ ] Emergency command bypass
- [ ] Test mode for faster timeout testing

#### Expected Benefits
- Battery usage: 2.0% → 0.4% per hour (sleeping)
- Memory usage: 65MB → 20MB (sleeping)
- Instant wake for emergency commands

---

### 4. Explicit Dictation Control with Sessions [8 hours]

**Status:** NOT IMPLEMENTED  
**Priority:** HIGH  
**Impact:** Complete dictation control with rich features

#### What's Missing
- No explicit startDictation()/stopDictation()
- No session tracking
- No pause/resume capability
- No context-aware configuration

#### Pending Implementation
```kotlin
class SmartDictationController {
    // PENDING: Session management
    data class DictationSession(
        val id: String,
        val startTime: Long,
        val context: DictationContext,
        val config: DictationConfig,
        var text: StringBuilder,
        var wordCount: Int,
        var isPaused: Boolean
    )
    
    // PENDING: Core methods
    fun startDictation(context: DictationContext?): Result<DictationSession>
    fun stopDictation(): Result<String>
    fun pauseDictation(): Result<Unit>
    fun resumeDictation(): Result<DictationSession>
    fun isDictating(): Boolean
}
```

#### Key Features to Implement
- [ ] Explicit start/stop/pause/resume methods
- [ ] Session tracking with UUID
- [ ] Context detection (Message, Note, Search, Form)
- [ ] Auto-capitalization and punctuation
- [ ] Profanity filter option
- [ ] Max duration and silence detection
- [ ] Visual overlay feedback
- [ ` Audio feedback for state changes
- [ ] Content Provider for app integration
- [ ] Intent API for external apps

#### Expected Benefits
- Clear user control over dictation
- Session preservation across pauses
- Context-appropriate processing
- Rich app integration

---

### 5. Dynamic Command Registration with Hot-Swapping [12 hours]

**Status:** NOT IMPLEMENTED  
**Priority:** MEDIUM  
**Impact:** Real-time grammar updates, learning system

#### What's Missing
- Cannot update commands without rebuild
- No command usage tracking
- No learning from user behavior
- Audio gaps during grammar updates

#### Pending Implementation
```kotlin
class DynamicCommandRegistry {
    // PENDING: Multi-tier structure
    private val hotCommands = LRUCache<String, CommandInfo>(50)
    private val warmCommands = mutableSetOf<String>()
    private val coldCommands = mutableSetOf<String>()
    
    // PENDING: Shadow recognizer for hot-swapping
    private var shadowRecognizer: Recognizer? = null
    
    // PENDING: Smart rebuild
    fun shouldRebuild(): Boolean {
        return changeRatio > 0.2 && pendingSize > 10
    }
}
```

#### Key Features to Implement
- [ ] Multi-tier command cache (hot/warm/cold)
- [ ] Shadow recognizer for zero-gap rebuilds
- [ ] Smart rebuild decision logic
- [ ] Command usage tracking
- [ ] Learning from user corrections
- [ ] Batch command registration
- [ ] UUID-based command tracking
- [ ] Cleanup of stale commands
- [ ] Export/import command sets

#### Expected Benefits
- Zero audio gaps during updates
- Commands learn from usage
- Efficient batch updates
- Better personalization

---

## 📦 UUID Integration Requirements

**Documentation Location:** `/libraries/UUIDManager/docs/UUID-SpeechRecognition-Integration.md`

### Pending UUID Implementation

#### ObjectBox Entities to Create
```kotlin
@Entity
data class CommandConceptEntity(
    @Id var id: Long = 0,
    @Unique @Index var uuid: String = "",  // UUIDv7 for persistent
    var canonicalName: String = "",
    var category: String = "",
    var frequency: Int = 0
)

@Entity
data class PhraseEntity(
    @Id var id: Long = 0,
    @Unique @Index var uuid: String = "",  // UUIDv5 deterministic
    @Index var conceptUuid: String = "",
    var text: String = "",
    var locale: String = "en-US"
)

@Entity
data class ContextEntity(
    @Id var id: Long = 0,
    @Unique @Index var uuid: String = "",  // UUIDv5 deterministic
    var contextKey: String = "",
    var appPackage: String = ""
)
```

#### UUID Generation Required
- **UUIDv7**: For persistent stored commands (sortable by time)
- **UUIDv5**: For deterministic IDs (same input → same UUID)
- **UUIDv4**: For temporary session items

### Benefits of UUID System
- 30-50% faster context switching
- Zero duplicates in grammar
- Precise cache invalidation
- Cross-context learning

---

## 📋 Implementation Plan

### Phase 1: Foundation [Week 1]
1. **Dual Recognizer System** (16 hours)
   - Implement A&C hybrid architecture
   - Add resource management
   - Test mode switching

2. **Grammar Constraints** (10 hours)
   - Create grammar manager
   - Implement JSON compilation
   - Add smart rebuild logic

### Phase 2: Power & Control [Week 2]
3. **Sleep/Wake Management** (8 hours)
   - Implement 3-state system
   - Add settings UI
   - Create wake word recognizer

4. **Dictation Control** (8 hours)
   - Build session manager
   - Add UI feedback
   - Create app integration APIs

### Phase 3: Advanced Features [Week 3]
5. **Dynamic Commands** (12 hours)
   - Implement command registry
   - Add hot-swapping
   - Create learning system

6. **UUID Integration** (8 hours)
   - Create ObjectBox entities
   - Implement UUID generation
   - Migrate existing data

### Phase 4: Testing & Polish [Week 4]
- Integration testing
- Performance optimization
- Documentation updates
- User testing

---

## ⚠️ Risk Mitigation

### Technical Risks
1. **Memory Pressure**: Dual recognizers use +30MB
   - Mitigation: Lazy loading, aggressive cleanup

2. **Audio Gaps**: Mode switching may cause gaps
   - Mitigation: Shadow recognizer hot-swapping

3. **Grammar Size**: Too many commands slow recognition
   - Mitigation: 500 command limit, smart selection

### Implementation Risks
1. **Complexity**: Many interconnected features
   - Mitigation: Phased implementation, extensive testing

2. **Backward Compatibility**: Existing apps may break
   - Mitigation: Maintain current APIs, add new ones

---

## 🎯 Success Criteria

### Performance Targets
- Command accuracy: ≥95%
- Mode switch time: <10ms
- Battery usage (sleep): <0.5%/hour
- Memory usage: 65-95MB adaptive
- Grammar rebuild: <200ms

### User Experience
- Zero audio gaps
- Instant command response
- Clear sleep/wake feedback
- Seamless dictation control
- Dynamic command learning

---

## 📚 Related Documentation

- `/libraries/UUIDManager/docs/UUID-SpeechRecognition-Integration.md` - UUID implementation details
- `/docs/TODO/VOS4-TODO-Master.md` - Updated with pending tasks
- `/libraries/SpeechRecognition/README.md` - Module overview
- `/docs/modules/speechrecognition/SpeechRecognition-Changelog.md` - Track implementation progress

---

**Status:** PENDING IMPLEMENTATION  
**Priority:** HIGH  
**Estimated Time:** 44 hours  
**Dependencies:** ObjectBox, UUIDManager  
**Author:** Manoj Jhawar  
**Created:** 2025-01-27  

**IMPORTANT:** This document describes PLANNED enhancements. None of these features are currently implemented in VOS4.
