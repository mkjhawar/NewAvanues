# VoiceOSCore System Testing Guide

**Version:** 1.0
**Date:** 2026-01-19
**Author:** Claude (AI)
**Module:** VoiceOSCore + voiceoscoreng

---

## Table of Contents

1. [Overview](#1-overview)
2. [Test Environment Setup](#2-test-environment-setup)
3. [Chain of Thought: System Flow](#3-chain-of-thought-system-flow)
4. [Tree of Thought: Test Categories](#4-tree-of-thought-test-categories)
5. [Test Procedures](#5-test-procedures)
   - [5.1 Accessibility Service Tests](#51-accessibility-service-tests)
   - [5.2 Element Extraction & Scraping Tests](#52-element-extraction--scraping-tests)
   - [5.3 AVID/Fingerprint Generation Tests](#53-avidfingerprint-generation-tests)
   - [5.4 Deduplication & Hashing Tests](#54-deduplication--hashing-tests)
   - [5.5 Database Persistence Tests](#55-database-persistence-tests)
   - [5.6 Command System Tests](#56-command-system-tests)
   - [5.7 Speech Recognition Tests](#57-speech-recognition-tests)
   - [5.8 Handler Execution Tests](#58-handler-execution-tests)
   - [5.9 Screen Caching Tests](#59-screen-caching-tests)
   - [5.10 NLU/LLM Integration Tests](#510-nlullm-integration-tests)
6. [Log Monitoring Guide](#6-log-monitoring-guide)
7. [Issue Reporting Template](#7-issue-reporting-template)
8. [Performance Benchmarks](#8-performance-benchmarks)

---

## 1. Overview

This document provides comprehensive testing procedures for the VoiceOSCore voice accessibility system. The system enables hands-free device control through:

- **Screen scraping** via Android Accessibility Service
- **Element fingerprinting** (AVID) for stable element identification
- **Command generation** from UI elements
- **Voice recognition** integration (Vivoka, Android STT, etc.)
- **Database persistence** for learned commands
- **Deduplication** to avoid redundant scanning

### Critical Success Metrics

| Metric | Target | Current |
|--------|--------|---------|
| Screen scan time | < 500ms | TBD |
| Cache hit rate | > 80% | TBD |
| Command match accuracy | > 95% | TBD |
| False positive rate | < 5% | TBD |
| AVID stability | 100% deterministic | TBD |

---

## 2. Test Environment Setup

### Prerequisites

```bash
# ADB access
adb devices  # Should show connected device

# Logcat filter setup
adb logcat -c  # Clear existing logs
adb logcat -v time | grep -E "VoiceOS|ActionCoord|CommandReg|ElementExt|ScreenCache|GestureHandler|DynamicCommand"
```

### Recommended Logcat Tags

```bash
# Primary debugging tags
TAG_FILTER="VoiceOS|ActionCoordinator|CommandRegistry|ElementExtractor|ScreenCacheManager|DynamicCommandGen|AndroidGestureHandler|HashUtils|ElementFingerprint"

# Full monitoring command
adb logcat -v threadtime *:S VoiceOS:D ActionCoordinator:D CommandRegistry:D ElementExtractor:D ScreenCacheManager:D DynamicCommandGen:D
```

### Test Apps (in order of complexity)

1. **Settings** - Simple, static UI, good baseline
2. **Calculator** - Numeric buttons, grid layout
3. **Gmail** - Dynamic lists, complex hierarchy
4. **Chrome** - WebViews, dynamic content
5. **Maps** - Heavy custom views, gestures

---

## 3. Chain of Thought: System Flow

Understanding the complete data flow is essential for debugging:

```
┌──────────────────────────────────────────────────────────────────────┐
│                        VOICE COMMAND FLOW                            │
└──────────────────────────────────────────────────────────────────────┘

1. SCREEN SCRAPING PHASE
   ┌─────────────────┐
   │ Accessibility   │ onAccessibilityEvent()
   │ Service         │──────────────────────────┐
   └─────────────────┘                          │
                                                ▼
   ┌─────────────────┐    ┌─────────────────┐   │
   │ Screen Cache    │◄───│ Check Screen    │◄──┘
   │ Manager         │    │ Hash            │
   └────────┬────────┘    └─────────────────┘
            │
            │ Cache Miss
            ▼
   ┌─────────────────┐    ┌─────────────────┐
   │ Element         │───►│ Deduplication   │
   │ Extractor       │    │ (Hash)          │
   └────────┬────────┘    └─────────────────┘
            │
            ▼
   ┌─────────────────┐    ┌─────────────────┐
   │ Element         │───►│ AVID            │
   │ Fingerprint     │    │ Generation      │
   └────────┬────────┘    └─────────────────┘
            │
            ▼
   ┌─────────────────┐    ┌─────────────────┐
   │ Command         │───►│ CommandRegistry │
   │ Generator       │    │ (In-Memory)     │
   └────────┬────────┘    └─────────────────┘
            │
            ▼
   ┌─────────────────┐    ┌─────────────────┐
   │ Dynamic Command │───►│ Database        │
   │ Generator       │    │ (SQLDelight)    │
   └─────────────────┘    └─────────────────┘

2. VOICE RECOGNITION PHASE
   ┌─────────────────┐
   │ Speech Engine   │ startListening()
   │ (Vivoka/STT)    │──────────────────────────┐
   └─────────────────┘                          │
                                                ▼
   ┌─────────────────┐    ┌─────────────────┐   │
   │ Speech Result   │───►│ Action          │◄──┘
   │                 │    │ Coordinator     │
   └─────────────────┘    └────────┬────────┘
                                   │
                                   ▼
3. COMMAND MATCHING PHASE          │
   ┌─────────────────┐             │
   │ Extract Verb    │◄────────────┘
   │ + Target        │
   └────────┬────────┘
            │
            ▼
   ┌─────────────────┐    ┌─────────────────┐
   │ CommandRegistry │───►│ Fuzzy Match?    │
   │ .findByPhrase() │    │ (CommandMatcher)│
   └────────┬────────┘    └─────────────────┘
            │
            ▼
   ┌─────────────────┐    ┌─────────────────┐
   │ Handler         │───►│ Execute         │
   │ Registry        │    │ Gesture/Action  │
   └─────────────────┘    └─────────────────┘
```

### Key Decision Points

| Step | Decision | Log to Watch |
|------|----------|--------------|
| 1 | Cache hit/miss | `ScreenCacheManager: hasScreen()` |
| 2 | Element actionable? | `CommandGenerator: isActionable` |
| 3 | Has voice content? | `CommandGenerator: hasVoiceContent` |
| 4 | AVID generated? | `ElementFingerprint: generate()` |
| 5 | Command registered? | `CommandRegistry: updateSync()` |
| 6 | Phrase matched? | `CommandRegistry: findByPhrase()` |
| 7 | Handler found? | `ActionCoordinator: findHandler()` |
| 8 | Bounds available? | `AndroidGestureHandler: bounds=` |

---

## 4. Tree of Thought: Test Categories

```
VoiceOSCore Testing
├── 🔵 UNIT TESTS (Isolated Components)
│   ├── ElementFingerprint
│   │   ├── Determinism (same input → same output)
│   │   ├── Format validation (TYPE:hash8)
│   │   └── Type code mapping
│   ├── HashUtils
│   │   ├── SHA-256 correctness
│   │   ├── App hash generation
│   │   └── Collision resistance
│   ├── CommandMatcher
│   │   ├── Exact matching
│   │   ├── Fuzzy threshold (0.7)
│   │   └── Synonym expansion
│   └── Bounds
│       ├── Center calculation
│       ├── Parse from string
│       └── Dimension accessors
│
├── 🟢 INTEGRATION TESTS (Component Chains)
│   ├── Scraping → Generation
│   │   ├── Element extraction accuracy
│   │   ├── Label derivation priority
│   │   └── Hierarchy tracking
│   ├── Generation → Registry
│   │   ├── Command registration
│   │   ├── AVID preservation
│   │   └── Metadata (bounds, className)
│   ├── Registry → Execution
│   │   ├── Phrase lookup
│   │   ├── Handler routing
│   │   └── Gesture dispatch
│   └── Database → Cache
│       ├── Persistence correctness
│       ├── Cache invalidation
│       └── Version tracking
│
├── 🟡 END-TO-END TESTS (Full Flow)
│   ├── Static Screen (Settings)
│   │   ├── Full scan
│   │   ├── Voice command
│   │   └── Action execution
│   ├── Dynamic Screen (Gmail)
│   │   ├── List item detection
│   │   ├── Index commands ("first", "second")
│   │   └── Label commands ("Sender Name")
│   └── Repeated Navigation
│       ├── Cache hit verification
│       ├── No rescan on return
│       └── Version invalidation
│
└── 🔴 STRESS TESTS (Edge Cases)
    ├── Large element count (500+)
    ├── Deep nesting (10+ levels)
    ├── Rapid screen changes
    ├── Concurrent voice commands
    └── Memory pressure
```

---

## 5. Test Procedures

### 5.1 Accessibility Service Tests

#### Test A11Y-001: Service Lifecycle

**Objective:** Verify service connects and initializes correctly

**Steps:**
1. Disable VoiceOS accessibility service in Settings
2. Clear app data: `adb shell pm clear com.augmentalis.voiceoscoreng`
3. Enable VoiceOS accessibility service
4. Monitor logs

**Expected Logs:**
```
D/VoiceOS: onServiceConnected - Service started
D/VoiceOS: Initializing VoiceOSCore...
D/VoiceOS: Speech engine initialized: VIVOKA
D/VoiceOS: Handlers registered: AndroidGestureHandler, SystemHandler
D/VoiceOS: Service ready
```

**What to Report:**
- [ ] Service connected successfully
- [ ] VoiceOSCore initialized
- [ ] Speech engine name
- [ ] Handler count
- [ ] Any exceptions

---

#### Test A11Y-002: Event Routing

**Objective:** Verify accessibility events are properly routed

**Steps:**
1. Open Settings app
2. Navigate to different screens
3. Monitor `onAccessibilityEvent` logs

**Expected Logs:**
```
D/VoiceOS: onAccessibilityEvent: TYPE_WINDOW_STATE_CHANGED package=com.android.settings
D/VoiceOS: onAccessibilityEvent: TYPE_WINDOW_CONTENT_CHANGED package=com.android.settings
D/VoiceOS: Debouncing screen change (300ms)
D/VoiceOS: Performing exploration...
```

**What to Report:**
- [ ] Event types received (WINDOW_STATE_CHANGED, WINDOW_CONTENT_CHANGED)
- [ ] Package name correct
- [ ] Debouncing triggered
- [ ] Exploration started

---

### 5.2 Element Extraction & Scraping Tests

#### Test EXT-001: Basic Extraction

**Objective:** Verify elements are extracted from accessibility tree

**Steps:**
1. Open Settings app
2. Wait for scan to complete
3. Check extraction logs

**Expected Logs:**
```
D/ElementExtractor: extractElements: starting at depth 0
D/ElementExtractor: Extracted 127 elements, 45 actionable
D/ElementExtractor: Hierarchy: 127 nodes, max depth 8
D/ElementExtractor: Duplicates found: 3
```

**What to Report:**
- [ ] Total element count
- [ ] Actionable element count
- [ ] Max hierarchy depth
- [ ] Duplicate count
- [ ] Extraction time (if logged)

---

#### Test EXT-002: List Item Detection (Gmail)

**Objective:** Verify dynamic list items are properly identified

**Steps:**
1. Open Gmail app
2. Navigate to inbox with multiple emails
3. Check list item logs

**Expected Logs:**
```
D/ElementExtractor: isDynamicContainer: RecyclerView detected
D/ElementExtractor: findTopLevelListItems: 15 items with listIndex >= 0
D/ElementExtractor: EMAIL ROW: 'Sender Name' bounds=(0,200,1080,320) h=120
D/ElementExtractor: findTopLevelListItems: found 10 email rows
D/ElementExtractor: findTopLevelListItems: 10 unique rows after dedup
```

**What to Report:**
- [ ] Dynamic container detected (RecyclerView/ListView)
- [ ] List items found count
- [ ] Email rows identified
- [ ] Unique rows after dedup
- [ ] Any misidentified elements

---

#### Test EXT-003: Label Derivation

**Objective:** Verify labels are derived correctly from element properties

**Steps:**
1. Open Calculator app
2. Check label derivation for buttons

**Expected Logs:**
```
D/ElementExtractor: deriveElementLabels: Button text='7' → label='7'
D/ElementExtractor: deriveElementLabels: Button resourceId='btn_equals' → label='equals'
D/ElementExtractor: deriveElementLabels: ImageView contentDesc='Delete' → label='Delete'
```

**Priority Order (verify):**
1. `text` (if not blank)
2. `contentDescription` (if not blank)
3. `resourceId` (after last `/`, replace `_` with space)
4. `className` (last segment, fallback)

**What to Report:**
- [ ] Label source for each element type
- [ ] Any missing labels
- [ ] Incorrect label derivation

---

### 5.3 AVID/Fingerprint Generation Tests

#### Test AVID-001: Determinism

**Objective:** Verify same element produces same AVID across scans

**Steps:**
1. Open Settings app
2. Note the AVID for "Wi-Fi" element
3. Navigate away and return
4. Compare AVID

**Expected Logs:**
```
D/ElementFingerprint: generate() className=TextView, resourceId=wifi_title, text=Wi-Fi
D/ElementFingerprint: Generated AVID: TXT:a3f2e1c9
```

**What to Report:**
- [ ] AVID format correct (TYPE:hash8)
- [ ] Same AVID on second scan
- [ ] Type code matches element type

---

#### Test AVID-002: Type Code Mapping

**Objective:** Verify correct type codes for element classes

**Expected Mappings:**

| Element Class | Type Code |
|--------------|-----------|
| Button | BTN |
| TextView | TXT |
| EditText | EDT |
| ImageView | IMG |
| ImageButton | IMB |
| CheckBox | CHK |
| Switch | SWT |
| SeekBar | SKB |
| RadioButton | RAD |
| RecyclerView | RCV |
| ScrollView | SCV |
| ViewGroup | VGP |
| (unknown) | UNK |

**What to Report:**
- [ ] Any incorrect type codes
- [ ] Missing type code mappings
- [ ] Custom view handling

---

#### Test AVID-003: Collision Testing

**Objective:** Verify no hash collisions for different elements

**Steps:**
1. Scan a screen with many elements (100+)
2. Check for duplicate AVIDs

**Expected Logs:**
```
D/CommandRegistry: updateSync: received 50 commands, 50 have valid AVIDs
```

**What to Report:**
- [ ] Total commands vs unique AVIDs
- [ ] Any duplicate AVIDs for different elements
- [ ] Collision details (if any)

---

### 5.4 Deduplication & Hashing Tests

#### Test DEDUP-001: Element Deduplication

**Objective:** Verify duplicate elements are detected and skipped

**Steps:**
1. Open a screen with list items (Gmail)
2. Check deduplication logs

**Expected Logs:**
```
D/ElementExtractor: DUPLICATE FOUND: hash=a1b2c3d4e5f6g7h8 class=TextView text='Unread'
D/DynamicCommandGen: Commands: 45 total (30 static, 15 dynamic)
```

**Hash Formula:**
```
hash = SHA-256(className|resourceId|text).take(16)
```

**What to Report:**
- [ ] Duplicate count
- [ ] Hash format (16 chars hex)
- [ ] Correctly identified duplicates

---

#### Test DEDUP-002: App Hash Versioning

**Objective:** Verify app hash changes when app version changes

**Steps:**
1. Note current app hash for Settings
2. Update Settings app (if possible) or simulate version change
3. Check hash regeneration

**Expected Logs:**
```
D/HashUtils: calculateAppHash: com.android.settings:12345 → hash=abc123...
D/ScreenCacheManager: App version changed, invalidating cache for com.android.settings
```

**Hash Formula:**
```
appHash = SHA-256(packageName:versionCode).take(8)
```

**What to Report:**
- [ ] App hash format (8 chars hex)
- [ ] Hash changes with version
- [ ] Cache invalidation triggered

---

### 5.5 Database Persistence Tests

#### Test DB-001: Command Insertion

**Objective:** Verify commands are persisted to SQLDelight database

**Steps:**
1. Scan a new screen (not previously seen)
2. Check database insertion logs

**Expected Logs:**
```
D/DynamicCommandGen: Step 1/3: Inserted scraped_app for com.android.settings
D/DynamicCommandGen: Step 2/3: Inserted 30 scraped_elements for 30 unique hashes
D/DynamicCommandGen: Step 3/3: Persisted 25 STATIC commands to voiceos.db (skipped 5 dynamic)
```

**What to Report:**
- [ ] scraped_app inserted
- [ ] scraped_element count
- [ ] Generated command count
- [ ] Static vs dynamic separation
- [ ] Any FK constraint errors

---

#### Test DB-002: Deduplication on Re-scan

**Objective:** Verify no duplicate insertions on repeated scans

**Steps:**
1. Scan Settings screen
2. Navigate away
3. Return to Settings screen
4. Check for "already exists" logs

**Expected Logs:**
```
D/DynamicCommandGen: Element hash a1b2c3d4... already exists in DB
D/DynamicCommandGen: Step 2/3: Inserted 0 scraped_elements (30 pre-existed)
```

**What to Report:**
- [ ] Pre-existing element count
- [ ] New insertions (should be 0)
- [ ] Any duplicate key errors

---

#### Test DB-003: Foreign Key Integrity

**Objective:** Verify FK constraints are enforced

**Steps:**
1. Check for FK violation errors in logs
2. Verify cascade deletes work

**Expected (NO errors):**
```
# Should NOT see:
E/DynamicCommandGen: FOREIGN KEY constraint failed (code 787)
```

**What to Report:**
- [ ] Any FK constraint errors
- [ ] Error code and context
- [ ] Element hash that failed

---

### 5.6 Command System Tests

#### Test CMD-001: Command Registration

**Objective:** Verify commands are registered in CommandRegistry

**Steps:**
1. Scan Settings screen
2. Check registry update logs

**Expected Logs:**
```
D/CommandRegistry: updateSync: received 45 commands, 45 have valid AVIDs
D/CommandRegistry: updateSync: first 3 commands: ['Wi-Fi' (bounds=0,200,1080,280), 'Bluetooth' (bounds=0,280,1080,360), 'Display' (bounds=0,360,1080,440)]
```

**What to Report:**
- [ ] Command count
- [ ] Valid AVID count (should match)
- [ ] Sample commands with bounds
- [ ] Any commands filtered out

---

#### Test CMD-002: Command Matching (Exact)

**Objective:** Verify exact phrase matching works

**Steps:**
1. Scan Settings screen
2. Speak "Wi-Fi" or simulate command
3. Check matching logs

**Expected Logs:**
```
D/ActionCoordinator: processVoiceCommand: 'wi-fi' (conf: 0.95)
D/ActionCoordinator: Dynamic command registry size: 45
D/ActionCoordinator: Extracted verb='null', target='wi-fi'
D/CommandRegistry: findByPhrase('wi-fi'): searching 45 commands
D/CommandRegistry: findByPhrase: exact match found - 'Wi-Fi'
D/ActionCoordinator: Dynamic command match! phrase='tap wi-fi', actionType=CLICK, bounds=0,200,1080,280
```

**What to Report:**
- [ ] Phrase normalized correctly
- [ ] Registry size correct
- [ ] Exact match found
- [ ] Action phrase generated
- [ ] Bounds present

---

#### Test CMD-003: Command Matching (Fuzzy)

**Objective:** Verify fuzzy matching handles voice variations

**Steps:**
1. Scan Settings screen
2. Speak slightly incorrect phrase (e.g., "WiFi" instead of "Wi-Fi")
3. Check fuzzy matching logs

**Expected Logs:**
```
D/CommandRegistry: findByPhrase: no exact match for 'wifi'
D/CommandMatcher: Fuzzy match: 'wifi' → 'Wi-Fi' (confidence: 0.85)
D/ActionCoordinator: Fuzzy command match! phrase='tap wifi', confidence=0.85
```

**Fuzzy Threshold:** 0.7 (configurable)

**What to Report:**
- [ ] No exact match logged
- [ ] Fuzzy match attempted
- [ ] Confidence score
- [ ] Above/below threshold

---

#### Test CMD-004: Index Commands (List Items)

**Objective:** Verify numbered commands work for lists

**Steps:**
1. Open Gmail inbox
2. Speak "first" or "second"
3. Check index command logs

**Expected Logs:**
```
D/CommandGenerator: generateListIndexCommands: 10 list items
D/CommandGenerator: Generated index commands: ['first', 'second', 'third', ...]
D/CommandRegistry: addAll: added 10 index commands
D/ActionCoordinator: Dynamic command match! phrase='tap first', actionType=CLICK
```

**What to Report:**
- [ ] Index command count
- [ ] Commands generated (first, second, third, etc.)
- [ ] Correct item targeted

---

### 5.7 Speech Recognition Tests

#### Test SPK-001: Speech Engine Initialization

**Objective:** Verify speech engine initializes correctly

**Steps:**
1. Start VoiceOS service
2. Check speech engine logs

**Expected Logs:**
```
D/VoiceOS: Initializing speech engine: VIVOKA
D/SpeechEngine: Vivoka SDK initialized
D/SpeechEngine: Grammar loaded with 150 phrases
```

**What to Report:**
- [ ] Engine type (VIVOKA, ANDROID_STT, etc.)
- [ ] Initialization success
- [ ] Phrase count in grammar
- [ ] Any initialization errors

---

#### Test SPK-002: Grammar Update

**Objective:** Verify dynamic phrases are added to speech grammar

**Steps:**
1. Scan a new screen
2. Check grammar update logs

**Expected Logs:**
```
D/DynamicCommandGen: Updated speech engine with 75 command phrases (25 static, 45 elements, 5 index)
```

**What to Report:**
- [ ] Total phrase count
- [ ] Static phrase count
- [ ] Element phrase count
- [ ] Index phrase count
- [ ] Any duplicates removed

---

#### Test SPK-003: Speech Result Flow

**Objective:** Verify speech results are processed correctly

**Steps:**
1. Speak a command
2. Check speech result logs

**Expected Logs:**
```
D/VoiceOS: Speech result: 'scroll down' (confidence: 0.92)
D/VoiceOS: isFinal: true
D/ActionCoordinator: processVoiceCommand: 'scroll down' (conf: 0.92)
```

**What to Report:**
- [ ] Text recognized
- [ ] Confidence score
- [ ] isFinal flag
- [ ] Passed to ActionCoordinator

---

### 5.8 Handler Execution Tests

#### Test HDL-001: Handler Selection

**Objective:** Verify correct handler is selected for command

**Steps:**
1. Speak "scroll down"
2. Check handler selection logs

**Expected Logs:**
```
D/ActionCoordinator: processCommand: phrase='scroll down', actionType=EXECUTE
D/ActionCoordinator: findHandler result: AndroidGestureHandler
D/AndroidGestureHandler: execute: phrase='scroll down', actionType=EXECUTE
```

**What to Report:**
- [ ] Handler type selected
- [ ] Correct for action type
- [ ] Execute called

---

#### Test HDL-002: Gesture Dispatch (Tap)

**Objective:** Verify tap gestures are dispatched correctly

**Steps:**
1. Speak "click Wi-Fi" (or element name)
2. Check gesture dispatch logs

**Expected Logs:**
```
D/AndroidGestureHandler: Executing TAP/CLICK for 'tap wi-fi', metadata: {bounds=0,200,1080,280, ...}
D/AndroidGestureHandler: Parsed bounds from metadata: Bounds(0, 200, 1080, 280)
D/AndroidGestureHandler: Clicking with bounds: 0,200,1080,280
D/AndroidGestureDispatcher: tap(540.0, 240.0) - center of bounds
D/AndroidGestureHandler: Click succeeded for 'tap wi-fi'
```

**What to Report:**
- [ ] Bounds parsed correctly
- [ ] Center calculated (centerX, centerY)
- [ ] Tap dispatched
- [ ] Success/failure result

---

#### Test HDL-003: Gesture Dispatch (Scroll)

**Objective:** Verify scroll gestures work

**Steps:**
1. Speak "scroll down"
2. Check scroll logs

**Expected Logs:**
```
D/AndroidGestureHandler: phrase='scroll down' matches scroll command
D/AndroidGestureDispatcher: scroll(direction=down)
D/AndroidGestureHandler: Scrolled down - success
```

**What to Report:**
- [ ] Direction detected
- [ ] Scroll dispatched
- [ ] Success/failure

---

#### Test HDL-004: System Commands

**Objective:** Verify system commands (back, home, etc.)

**Steps:**
1. Speak "go back" or "go home"
2. Check system handler logs

**Expected Logs:**
```
D/SystemHandler: Executing goBack
D/AndroidSystemExecutor: performGlobalAction(GLOBAL_ACTION_BACK)
D/SystemHandler: goBack succeeded
```

**What to Report:**
- [ ] Command recognized
- [ ] Global action executed
- [ ] Success/failure

---

### 5.9 Screen Caching Tests

#### Test CACHE-001: Cache Miss (First Visit)

**Objective:** Verify full scan on first screen visit

**Steps:**
1. Clear app data
2. Open Settings
3. Check cache logs

**Expected Logs:**
```
D/ScreenCacheManager: generateScreenHash: hash=abc123def456...
D/ScreenCacheManager: hasScreen(abc123def456) = false (cache miss)
D/VoiceOS: Cache miss - performing full exploration
D/ScreenCacheManager: saveScreen(abc123def456, ScreenInfo...)
D/ScreenCacheManager: saveCommandsForScreen(abc123def456, 45 commands)
```

**What to Report:**
- [ ] Screen hash generated
- [ ] Cache miss logged
- [ ] Full exploration performed
- [ ] Screen saved to cache
- [ ] Commands cached

---

#### Test CACHE-002: Cache Hit (Return Visit)

**Objective:** Verify cached data is used on return

**Steps:**
1. Navigate to another app
2. Return to Settings
3. Check cache logs

**Expected Logs:**
```
D/ScreenCacheManager: generateScreenHash: hash=abc123def456...
D/ScreenCacheManager: hasScreen(abc123def456) = true (cache hit)
D/VoiceOS: Cache hit - using cached commands
D/ScreenCacheManager: getCommandsForScreen(abc123def456) returned 45 commands
D/CommandRegistry: updateSync: received 45 commands (from cache)
```

**What to Report:**
- [ ] Same hash generated
- [ ] Cache hit logged
- [ ] No full exploration
- [ ] Commands loaded from cache
- [ ] Time saved (no extraction)

---

#### Test CACHE-003: Cache Invalidation

**Objective:** Verify cache is invalidated on app update

**Steps:**
1. Note cached screens for an app
2. Simulate app version change (or actually update)
3. Return to app
4. Check invalidation logs

**Expected Logs:**
```
D/ScreenCacheManager: App version changed for com.android.settings (123 → 124)
D/ScreenCacheManager: clearScreensForPackage(com.android.settings)
D/ScreenCacheManager: Cleared 5 cached screens
D/VoiceOS: Cache invalidated - performing full exploration
```

**What to Report:**
- [ ] Version change detected
- [ ] Cache cleared for package
- [ ] Count of cleared screens
- [ ] Full exploration triggered

---

### 5.10 NLU/LLM Integration Tests

> **Note:** VoiceOSCore uses keyword-based matching, not deep NLU. LLM integration is external (AVA module).

#### Test NLU-001: Synonym Expansion

**Objective:** Verify synonym expansion in matching

**Steps:**
1. Speak "press Wi-Fi" (instead of "click" or "tap")
2. Check synonym expansion logs

**Expected Logs:**
```
D/CommandMatcher: matchWithSynonyms: input='press wi-fi'
D/SynonymProvider: expand('press') = ['press', 'click', 'tap', 'select']
D/CommandMatcher: Trying synonym: 'click wi-fi'
D/CommandMatcher: Exact match with synonym expansion
```

**What to Report:**
- [ ] Synonym expansion triggered
- [ ] Alternatives tried
- [ ] Match found via synonym

---

#### Test NLU-002: LLM Synonym Enhancement (Database)

**Objective:** Verify LLM-generated synonyms are stored

**Steps:**
1. Check database for commands with synonyms
2. Verify LLM-enhanced commands

**Expected Database State:**
```sql
SELECT command_text, synonyms, confidence
FROM generated_command
WHERE synonyms IS NOT NULL;

-- Expected:
-- 'submit' | '["sign in","log in","authenticate"]' | 0.95
```

**What to Report:**
- [ ] Synonym field populated
- [ ] JSON format valid
- [ ] Confidence updated
- [ ] Sync flag set

---

## 6. Log Monitoring Guide

### Quick Reference: Log Tags

| Tag | Component | What to Watch |
|-----|-----------|---------------|
| `VoiceOS` | Main Service | Lifecycle, events |
| `ActionCoordinator` | Command Routing | Match flow, handler selection |
| `CommandRegistry` | In-Memory Store | Registration, lookup |
| `ElementExtractor` | Scraping | Element counts, duplicates |
| `ScreenCacheManager` | Caching | Hits, misses, invalidation |
| `DynamicCommandGen` | Command Creation | Static/dynamic split, DB persist |
| `AndroidGestureHandler` | Gesture Dispatch | Bounds, tap/scroll |
| `ElementFingerprint` | AVID | Hash generation |
| `HashUtils` | Hashing | App hash, element hash |
| `SpeechEngine` | Voice | Recognition results |

### Logcat Filter Commands

```bash
# Full debug (verbose)
adb logcat -v threadtime | grep -E "VoiceOS|ActionCoord|CommandReg|ElementExt|ScreenCache|DynamicCommand|GestureHandler|Fingerprint|HashUtils"

# Command flow only
adb logcat -v time | grep -E "ActionCoordinator|CommandRegistry"

# Scraping only
adb logcat -v time | grep -E "ElementExtractor|DynamicCommandGen"

# Caching only
adb logcat -v time | grep -E "ScreenCacheManager"

# Gesture execution only
adb logcat -v time | grep -E "AndroidGestureHandler|GestureDispatcher"

# Errors only
adb logcat *:E | grep -E "VoiceOS|ActionCoord|CommandReg"
```

### Key Patterns to Watch

**Success Patterns:**
```
✓ findByPhrase: exact match found
✓ Dynamic command match!
✓ findHandler result: AndroidGestureHandler
✓ Click succeeded
✓ Cache hit
✓ updateSync: received N commands, N have valid AVIDs
```

**Failure Patterns:**
```
✗ findByPhrase: no match for 'X'. Available: [...]
✗ No handler found for 'X'
✗ No bounds in metadata for 'X', returning notHandled
✗ Dynamic command registry size: 0
✗ FOREIGN KEY constraint failed
✗ Click failed
```

---

## 7. Issue Reporting Template

When reporting issues, use this template:

```markdown
## Issue Report: [Brief Title]

### Test ID
[e.g., CMD-002]

### Environment
- Device: [e.g., Pixel 6]
- Android Version: [e.g., 14]
- VoiceOSCore Version: [e.g., 1.0.0]
- App Under Test: [e.g., Gmail]

### Steps to Reproduce
1. [Step 1]
2. [Step 2]
3. [Step 3]

### Expected Behavior
[What should happen]

### Actual Behavior
[What actually happened]

### Relevant Logs
```
[Paste filtered logcat output here]
```

### Screenshots/Videos
[If applicable]

### Analysis
[Your interpretation of the issue]

### Suggested Fix
[If you have ideas]
```

---

## 8. Performance Benchmarks

### Target Metrics

| Operation | Target | Method |
|-----------|--------|--------|
| Screen scan | < 500ms | Time from event to commands registered |
| Cache lookup | < 10ms | Time for hasScreen() |
| Command match | < 50ms | Time from voice result to handler |
| Gesture dispatch | < 100ms | Time from handler to gesture complete |
| Full voice-to-action | < 1000ms | End-to-end latency |

### Measuring Performance

Add timing logs or use:
```bash
# Measure time between log entries
adb logcat -v threadtime | grep -E "processVoiceCommand|Click succeeded"
```

### Memory Monitoring

```bash
# Memory usage
adb shell dumpsys meminfo com.augmentalis.voiceoscoreng

# Watch for:
# - Heap size growth
# - Object count increases
# - GC frequency
```

---

## Appendix A: Test Checklist

### Pre-Test Setup
- [ ] Device connected via ADB
- [ ] VoiceOS accessibility enabled
- [ ] Logcat filter running
- [ ] Test apps installed

### Core Functionality
- [ ] A11Y-001: Service Lifecycle
- [ ] A11Y-002: Event Routing
- [ ] EXT-001: Basic Extraction
- [ ] EXT-002: List Item Detection
- [ ] EXT-003: Label Derivation
- [ ] AVID-001: Determinism
- [ ] AVID-002: Type Code Mapping
- [ ] AVID-003: Collision Testing
- [ ] DEDUP-001: Element Deduplication
- [ ] DEDUP-002: App Hash Versioning
- [ ] DB-001: Command Insertion
- [ ] DB-002: Deduplication on Re-scan
- [ ] DB-003: Foreign Key Integrity
- [ ] CMD-001: Command Registration
- [ ] CMD-002: Command Matching (Exact)
- [ ] CMD-003: Command Matching (Fuzzy)
- [ ] CMD-004: Index Commands
- [ ] SPK-001: Speech Engine Init
- [ ] SPK-002: Grammar Update
- [ ] SPK-003: Speech Result Flow
- [ ] HDL-001: Handler Selection
- [ ] HDL-002: Gesture Dispatch (Tap)
- [ ] HDL-003: Gesture Dispatch (Scroll)
- [ ] HDL-004: System Commands
- [ ] CACHE-001: Cache Miss
- [ ] CACHE-002: Cache Hit
- [ ] CACHE-003: Cache Invalidation
- [ ] NLU-001: Synonym Expansion
- [ ] NLU-002: LLM Synonym Enhancement

### Stress Tests
- [ ] Large element count (500+)
- [ ] Deep hierarchy (10+ levels)
- [ ] Rapid screen changes
- [ ] Extended operation (1hr+)

---

## Appendix B: Quick Troubleshooting

| Symptom | Check | Likely Cause |
|---------|-------|--------------|
| No commands registered | `CommandRegistry: updateSync` | Extraction failed or all filtered |
| Command not found | `findByPhrase: no match` | Phrase mismatch or registry empty |
| No handler | `findHandler result: null` | Phrase not matching any handler |
| Tap doesn't work | `bounds=null` | Bounds missing from metadata |
| Wrong element clicked | AVID logs | Fingerprint collision or stale cache |
| Slow performance | Timing logs | Cache misses or large element count |
| FK errors | DB logs | Element not inserted before command |

---

**Document End**

*For questions or updates, contact the VoiceOSCore development team.*
