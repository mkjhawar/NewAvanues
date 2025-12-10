# VOS4 CommandManager - Master TODO List (Priority-Based)

**Created:** 2025-10-09 21:30:00 PDT
**Status:** ACTIVE - Option A Execution (Foundation First)
**Branch:** vos4-legacyintegration
**Total Estimated Time:** 70.5 hours

---

## 🚨 PRIORITY ORDER: Foundation → Requirements → Integration

**Philosophy:** Fix what's broken, build the foundation, then integrate properly.

---

## ⚠️ TIER 0: CRITICAL FIXES (MUST DO FIRST) - 0.5 hours

### **Fix 0.1: Phase 2.4c Build Errors** ✋ **BLOCKING**
**Priority:** CRITICAL
**Time:** 30 minutes
**Status:** ⚠️ 23 build errors
**Blocking:** All Phase 2 completion

**Errors:**
1. `CommandFileWatcher.kt` - 6 errors (missing FileObserver constants)
2. `CommandManagerSettingsFragment.kt` - 17 errors (missing Fragment dependencies)

**Files to Fix:**
- `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/loader/CommandFileWatcher.kt`
- `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/ui/CommandManagerSettingsFragment.kt`

**Fix Required:**
```kotlin
// CommandFileWatcher.kt - Add imports
import android.os.FileObserver.MODIFY
import android.os.FileObserver.CLOSE_WRITE
import android.os.FileObserver.CREATE
import android.os.FileObserver.DELETE
import android.os.FileObserver.MOVED_FROM
import android.os.FileObserver.MOVED_TO

// CommandManagerSettingsFragment.kt - Add Fragment dependency
import androidx.fragment.app.Fragment
```

**Success Criteria:**
- [ ] Zero build errors in CommandManager module
- [ ] All files compile successfully

**Agent:** general-purpose (for code fixes)

---

## 🏗️ TIER 1: FOUNDATION (PHASE 1 CORE) - 40 hours

### **1.1: Base Action System** (8 hours)
**Priority:** HIGH (Foundation Layer)
**Status:** ⚠️ Partial (files exist, not integrated)
**Dependencies:** None

#### **Task 1.1a: Verify & Complete BaseAction** (2 hours)
- [x] BaseAction.kt - Fixed today (0 errors)
- [ ] Review all 12 action classes for completeness
- [ ] Add missing action implementations
- [ ] Create unit tests for each action
- [ ] Document action execution flow

**Files:**
```
/CommandManager/actions/
├── BaseAction.kt              ✅ Fixed
├── NavigationActions.kt       ⚠️ Review needed
├── AppActions.kt              ⚠️ Review needed
├── SystemActions.kt           ⚠️ Review needed
├── VolumeActions.kt           ⚠️ Review needed
├── TextActions.kt             ⚠️ Review needed
├── GestureActions.kt          ⚠️ Review needed
├── CursorActions.kt           ⚠️ Review needed
├── DragActions.kt             ⚠️ Review needed
├── ScrollActions.kt           ⚠️ Review needed
├── DictationActions.kt        ⚠️ Review needed
├── OverlayActions.kt          ⚠️ Review needed
```

**Success Criteria:**
- [ ] All action classes compile
- [ ] All actions have execute() method
- [ ] Unit tests for each action (80% coverage)
- [ ] API documentation complete

**Agent:** general-purpose

---

#### **Task 1.1b: Composite Actions** (3 hours)
**Priority:** MEDIUM
**Status:** ⚠️ Files exist, untested

**Implementation:**
1. Review CompositeAction pattern
2. Implement action chaining
3. Add rollback on failure
4. Create macro system integration
5. Test composite execution

**Files:**
```
/CommandManager/macros/
├── CommandMacro.kt
├── MacroExecutor.kt
├── MacroStep.kt
├── MacroDSL.kt
└── MacroContext.kt
```

**Success Criteria:**
- [ ] Composite actions execute sequentially
- [ ] Rollback works on failure
- [ ] Macros can be defined in JSON
- [ ] Integration tests pass

**Agent:** general-purpose

---

#### **Task 1.1c: Dynamic Command Registry** (3 hours)
**Priority:** HIGH
**Status:** ⚠️ Partial implementation

**Implementation:**
1. Complete DynamicCommandRegistry
2. Add namespace management
3. Implement conflict detection
4. Add priority-based resolution
5. Create registration API

**Files:**
```
/CommandManager/dynamic/
├── DynamicCommandRegistry.kt  ⚠️ Incomplete
├── NamespaceManager.kt
├── ConflictDetector.kt
├── CommandPriority.kt
├── RegistrationListener.kt
└── VoiceCommand.kt
```

**Success Criteria:**
- [ ] Commands can register dynamically
- [ ] Namespace conflicts detected
- [ ] Priority system works
- [ ] Events fire on registration

**Agent:** general-purpose

---

### **1.2: JSON Command Definitions** (6 hours)

#### **Task 1.2a: Define JSON Schema** (2 hours)
**Priority:** HIGH
**Status:** ⚠️ Array format exists, needs command schema

**Implementation:**
1. Extend array JSON to include actions
2. Define command definition schema
3. Add validation rules
4. Document JSON structure

**Example Schema:**
```json
{
  "commands": [
    ["navigate_forward", "forward", ["next"], "description", {
      "action": "navigation.forward",
      "parameters": {},
      "context": ["screen"],
      "priority": 50
    }]
  ]
}
```

**Success Criteria:**
- [ ] Schema defined and documented
- [ ] Validation rules implemented
- [ ] Example JSON files created
- [ ] Parser supports full schema

**Agent:** general-purpose

---

#### **Task 1.2b: Command Loader Integration** (4 hours)
**Priority:** HIGH
**Status:** ⚠️ Loader exists, needs action mapping

**Implementation:**
1. Extend CommandLoader to load action definitions
2. Map JSON actions to action classes
3. Add validation during load
4. Create action factory pattern
5. Test end-to-end loading

**Success Criteria:**
- [ ] JSON loads with action definitions
- [ ] Actions instantiate correctly
- [ ] Validation catches errors
- [ ] Fallback to defaults works

**Agent:** general-purpose

---

### **1.3: Context System** (12 hours)

#### **Task 1.3a: Context Detection** (6 hours)
**Priority:** HIGH
**Status:** ⚠️ ContextManager fixed, needs integration

**Implementation:**
1. Complete ContextManager integration
2. Implement ContextDetector
3. Add context providers
4. Create context caching
5. Test detection accuracy

**Files:**
```
/CommandManager/context/
├── ContextManager.kt       ✅ Fixed (0 errors)
├── ContextDetector.kt      ⚠️ Needs completion
├── CommandContext.kt       ✅ Sealed class exists
├── ContextMatcher.kt       ⚠️ Needs work
├── ContextRule.kt          ⚠️ Needs work
└── ContextSuggester.kt     ⚠️ Needs work
```

**Success Criteria:**
- [ ] Context detection works in real-time
- [ ] App context detected accurately
- [ ] Screen context captured
- [ ] Performance < 50ms overhead

**Agent:** general-purpose

---

#### **Task 1.3b: Context Rules Engine** (6 hours)
**Priority:** MEDIUM
**Status:** ⚠️ Basic structure exists

**Implementation:**
1. Define rule syntax
2. Implement rule evaluation
3. Add rule composition (AND/OR/NOT)
4. Create predefined rules
5. Test rule matching

**Success Criteria:**
- [ ] Rules evaluate correctly
- [ ] Complex rules supported
- [ ] Rule performance acceptable
- [ ] Rule syntax documented

**Agent:** general-purpose

---

### **1.4: Learning & Suggestions** (14 hours)

#### **Task 1.4a: Preference Learning** (8 hours)
**Priority:** MEDIUM
**Status:** ⚠️ Database exists, logic missing

**Implementation:**
1. Use CommandUsageEntity for learning
2. Implement usage pattern detection
3. Add frequency-based ranking
4. Create context-aware suggestions
5. Test learning accuracy

**Files:**
```
/CommandManager/context/
├── PreferenceLearner.kt    ⚠️ Needs implementation
└── LearningDatabase.kt     ⚠️ Needs implementation
```

**Success Criteria:**
- [ ] Usage patterns detected
- [ ] Suggestions improve over time
- [ ] Context influences suggestions
- [ ] Privacy controls work

**Agent:** general-purpose

---

#### **Task 1.4b: Command Suggestions** (6 hours)
**Priority:** LOW
**Status:** ⚠️ UI exists, logic missing

**Implementation:**
1. Create suggestion API
2. Implement ranking algorithm
3. Add suggestion UI integration
4. Test suggestion accuracy

**Success Criteria:**
- [ ] Suggestions are relevant
- [ ] Performance < 100ms
- [ ] UI displays suggestions
- [ ] Users can accept/reject

**Agent:** general-purpose

---

## 🔗 TIER 2: INTEGRATION (CONNECT SYSTEMS) - 20 hours

### **2.1: VoiceAccessibility Integration** (12 hours) ✋ **CRITICAL**
**Priority:** CRITICAL
**Status:** ❌ Zero integration exists
**Blocking:** All functionality

#### **Task 2.1a: CommandManager Initialization** (3 hours)
**File:** `VoiceAccessibilityService.kt`

**Implementation:**
1. Add CommandManager dependency
2. Initialize in onCreate()
3. Load commands on startup
4. Handle initialization errors

```kotlin
class VoiceAccessibilityService : AccessibilityService() {
    private lateinit var commandManager: CommandManager
    private lateinit var commandResolver: CommandResolver
    private lateinit var commandLoader: CommandLoader

    override fun onCreate() {
        super.onCreate()

        // Initialize CommandManager
        val database = CommandDatabase.getInstance(this)
        commandManager = CommandManager.getInstance(this)
        commandResolver = CommandResolver(
            database.voiceCommandDao(),
            database.commandUsageDao()
        )
        commandLoader = CommandLoader(
            context = this,
            commandDao = database.voiceCommandDao(),
            versionDao = database.databaseVersionDao()
        )

        // Load commands
        lifecycleScope.launch {
            val result = commandLoader.initializeCommands()
            Log.i(TAG, "Commands loaded: $result")
        }
    }
}
```

**Success Criteria:**
- [ ] CommandManager initializes
- [ ] Database loads on startup
- [ ] No crashes or errors
- [ ] Logs show successful init

**Agent:** general-purpose

---

#### **Task 2.1b: Voice Command Pipeline** (5 hours)
**Files:** `VoiceRecognitionManager.kt`, `ActionCoordinator.kt`

**Implementation:**
1. Route voice input to CommandResolver
2. Execute resolved commands via CommandManager
3. Track usage statistics
4. Handle execution errors

```kotlin
// VoiceRecognitionManager.kt
fun onRecognitionResult(text: String) {
    lifecycleScope.launch {
        // Resolve command
        val result = commandResolver.resolveCommand(
            userInput = text,
            userLocale = getCurrentLocale(),
            contextApp = getCurrentApp()
        )

        when (result) {
            is ResolveResult.Match -> {
                // Execute via CommandManager
                val command = Command(
                    id = result.entity.id,
                    text = text,
                    source = CommandSource.VOICE,
                    confidence = 1.0f
                )
                commandManager.executeCommand(command)
            }
            is ResolveResult.NoMatch -> {
                // Handle no match
                showNoMatchUI(text)
            }
        }
    }
}
```

**Success Criteria:**
- [ ] Voice input reaches CommandResolver
- [ ] Commands execute correctly
- [ ] Usage tracked in database
- [ ] Errors handled gracefully

**Agent:** general-purpose

---

#### **Task 2.1c: ActionCoordinator Migration** (4 hours)
**File:** `ActionCoordinator.kt`

**Decision Required:** Choose migration strategy:
- **Option A:** Replace ActionCoordinator with CommandManager
- **Option B:** Have ActionCoordinator delegate to CommandManager
- **Option C:** Keep both, route based on command type

**Recommended:** Option B (delegate pattern)

**Implementation:**
1. Add CommandManager reference
2. Delegate command execution to CM
3. Keep handler system for fallback
4. Gradual migration path

**Success Criteria:**
- [ ] Existing commands still work
- [ ] New commands use CommandManager
- [ ] No breaking changes
- [ ] Migration documented

**Agent:** general-purpose

---

### **2.2: Database Integration Verification** (4 hours)
**Priority:** HIGH
**Status:** ⚠️ Database works standalone, needs verification

**Implementation:**
1. Verify initializeCommands() called on startup
2. Test English fallback logic
3. Verify persistence check works
4. Test usage tracking
5. Validate fuzzy matching

**Success Criteria:**
- [ ] Database populated on first run
- [ ] Subsequent runs skip reload
- [ ] English fallback works
- [ ] Usage stats accumulate

**Agent:** general-purpose

---

### **2.3: Context Integration** (4 hours)
**Priority:** MEDIUM
**Status:** ⚠️ Context detection not wired up

**Implementation:**
1. Wire ContextManager to VoiceAccessibilityService
2. Update context on window changes
3. Pass context to CommandResolver
4. Test context-aware commands

**Success Criteria:**
- [ ] Context updates automatically
- [ ] Commands filtered by context
- [ ] Performance acceptable
- [ ] Context suggestions work

**Agent:** general-purpose

---

## 📚 TIER 3: DOCUMENTATION (CRITICAL GAPS) - 8 hours

### **3.1: Integration Documentation** (4 hours) ✋ **CRITICAL**
**Priority:** CRITICAL
**Status:** ❌ Missing

**Files to Create:**
```
/docs/modules/command-manager/
├── architecture/
│   └── Integration-Architecture.md          (NEW)
├── developer-manual/
│   ├── Integration-Guide.md                 (NEW)
│   └── Migration-From-ActionCoordinator.md  (NEW)
└── diagrams/
    └── Integration-Flow.png                 (NEW)
```

**Content Required:**
1. How CommandManager integrates with VA
2. Data flow diagrams
3. API usage examples
4. Migration guide for existing code

**Agent:** general-purpose (documentation specialist)

---

### **3.2: API Documentation** (2 hours)
**Priority:** HIGH
**Status:** ❌ Missing

**Files to Create:**
```
/docs/modules/command-manager/reference/api/
├── CommandManager-API.md
├── CommandResolver-API.md
├── CommandLoader-API.md
└── Action-System-API.md
```

**Agent:** general-purpose (documentation specialist)

---

### **3.3: Architecture Documentation** (2 hours)
**Priority:** HIGH
**Status:** ❌ Missing

**Files to Create:**
```
/docs/modules/command-manager/architecture/
├── Database-Schema.md
├── Action-System-Design.md
└── Context-Detection-System.md
```

**Agent:** general-purpose (documentation specialist)

---

## ✅ TIER 4: REMAINING FEATURES - 2 hours

### **4.1: Phase 2.3 - Number Overlays** (Optional)
**Priority:** LOW
**Time:** 5 hours
**Status:** Not started
**Note:** Can be done after integration

### **4.2: Phase 3 - Scraping Integration** (Optional)
**Priority:** LOW
**Time:** 16 hours
**Status:** Not started
**Note:** Can be done after integration

---

## 📊 EXECUTION SUMMARY

### **Total Time by Tier:**
```
Tier 0 (Critical Fixes):     0.5 hours  ✋ DO FIRST
Tier 1 (Foundation):        40.0 hours  🏗️ BUILD FOUNDATION
Tier 2 (Integration):       20.0 hours  🔗 CONNECT SYSTEMS
Tier 3 (Documentation):      8.0 hours  📚 DOCUMENT EVERYTHING
Tier 4 (Optional Features):  2.0 hours  ✨ ENHANCEMENTS
────────────────────────────────────────
TOTAL:                      70.5 hours
```

### **Execution Order:**
1. **Week 1 (40h):** Tier 0 + Tier 1 (Foundation complete)
2. **Week 2 (20h):** Tier 2 (Integration working)
3. **Week 3 (8h):** Tier 3 (Documentation complete)
4. **Week 4 (2h):** Tier 4 (Optional features)

---

## 🎯 SUCCESS CRITERIA (MUST ACHIEVE ALL)

### **Foundation (Tier 1):**
- [ ] All action classes compile and work
- [ ] Dynamic registry functional
- [ ] Context detection accurate
- [ ] JSON schema complete
- [ ] 80% test coverage

### **Integration (Tier 2):**
- [ ] Voice input → CommandResolver → CommandManager → Action execution
- [ ] Database loads on startup
- [ ] Usage tracking works
- [ ] Context filtering works
- [ ] Zero crashes

### **Documentation (Tier 3):**
- [ ] Integration guide complete
- [ ] API docs complete
- [ ] Architecture documented
- [ ] Migration guide written

---

## 🚀 NEXT IMMEDIATE ACTIONS

**Right Now (Next 30 Minutes):**
1. ✅ Status document created
2. ✅ Master TODO created
3. ⏳ **Deploy agents to fix Tier 0 errors**

**Next Agent Deployment:**
```bash
# Agent 1: Fix CommandFileWatcher.kt (15 min)
# Agent 2: Fix CommandManagerSettingsFragment.kt (15 min)
# Agent 3: Verify Phase 2 compilation (5 min)
```

---

**Created:** 2025-10-09 21:30:00 PDT
**Last Updated:** 2025-10-09 21:30:00 PDT
**Status:** ACTIVE
**Next Review:** After Tier 0 completion
