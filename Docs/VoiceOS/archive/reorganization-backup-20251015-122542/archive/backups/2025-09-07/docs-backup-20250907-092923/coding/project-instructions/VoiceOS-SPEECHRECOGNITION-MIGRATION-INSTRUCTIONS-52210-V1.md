# 🎯 SpeechRecognition Migration Implementation Instructions

**Created:** 2025-09-03  
**Last Updated:** 2025-09-03  
**Purpose:** Comprehensive instructions for migrating LegacyAvenue VoiceOS to VOS4 SpeechRecognition module  
**CRITICAL:** These instructions are MANDATORY and override general instructions where specified

---

## 🛑 SECTION 0: ABSOLUTE MANDATORY RULE - NO WORK WITHOUT APPROVAL

### THIS IS THE MOST IMPORTANT RULE IN THIS DOCUMENT

**STOP AND READ:** 
- ❌ **DO NOT START ANY WORK** without explicit user approval
- ❌ **DO NOT BEGIN PHASE 1** without user saying "start", "begin", "proceed with Phase 1" or similar
- ❌ **DO NOT ANALYZE CODE** for implementation without permission
- ✅ **ONLY ALLOWED ACTION:** Present the implementation plan and WAIT

### WORKFLOW MUST BE:
1. **YOU:** Present complete implementation plan
2. **YOU:** WAIT for user response
3. **USER:** Reviews plan and either:
   - Approves: "OK", "Proceed", "Start", "Begin", "Go ahead"
   - Modifies: Provides changes to the plan
   - Rejects: Asks for different approach
4. **YOU:** ONLY THEN begin approved work

### IF USER ASKS QUESTIONS:
- Answer the question ONLY
- Do NOT start implementation
- Do NOT prepare code
- WAIT for explicit approval to begin work

---

## 🔴 SECTION 1: MANDATORY PERMISSION PROTOCOL

### PERMISSION REQUIRED AT EVERY STAGE

**Before ANYTHING:**
1. Present plan → WAIT for approval
2. User approves → Begin ONLY what was approved
3. Complete approved work → STOP
4. Present next stage → WAIT for approval
5. NEVER assume approval for next steps

**Approval Keywords to Watch For:**
- "Proceed" / "Go ahead" / "Start" / "Begin"
- "Yes, implement" / "OK, do it" / "Make the changes"
- "Continue" / "Next phase" / "Move forward"

**NON-Approval Responses (DO NOT START):**
- "Show me" / "What would that look like?"
- "Explain" / "Tell me more"
- Questions of any kind
- Comments without explicit approval

---

## ⚠️ SECTION 2: CRITICAL NAMING CORRECTION

### CORRECT Project Name Spelling:
- **CORRECT:** LegacyAvenue (check actual folder name to confirm)
- **WRONG:** LegacyAvenue (with 'e' at end)
- **ALWAYS:** Verify exact spelling from folder structure
- **NEVER:** Assume spelling - check the actual directory name

### Naming Standards:
- **LegacyAvenue context:** VoiceOS module
- **VOS4 context:** SpeechRecognition module  
- **Package names:** com.augmentalis.* (NEVER com.ai.*)

---

## 📋 SECTION 3: IMPLEMENTATION PHASE STRUCTURE

### CRITICAL: Each Phase Requires Separate Approval

```
APPROVAL GATE 1: Present Phase Plan → WAIT FOR USER APPROVAL
    ↓ (only after approval)
Phase X: [Name]
├── 1. PLANNING STAGE
│   ├── Analyze LegacyAvenue implementation
│   ├── Present detailed migration plan
│   ├── List all files to be created/modified
│   ├── Show proposed code structure
│   └── ⏸️ STOP - WAIT FOR USER APPROVAL
│
├── APPROVAL GATE 2: User must approve plan
│   └── ⏸️ DO NOT PROCEED WITHOUT EXPLICIT APPROVAL
│
├── 2. IMPLEMENTATION STAGE
│   ├── Use specialized agents for coding
│   ├── Implement ONLY approved changes
│   ├── Create comprehensive unit tests
│   └── No scope creep allowed
│
├── 3. VERIFICATION STAGE
│   ├── Run COT+ROT analysis
│   ├── Execute all unit tests
│   ├── Compare with LegacyAvenue
│   └── Report verification results
│
├── 4. DOCUMENTATION STAGE
│   ├── Update ALL affected documentation
│   ├── Create/update architecture diagrams
│   ├── Update module-specific docs
│   └── Update roadmaps and plans
│
├── 5. COMMIT STAGE
│   ├── Stage ONLY files YOU modified
│   ├── Commit with descriptive message
│   ├── Push to repository
│   └── Update status documents
│
└── APPROVAL GATE 3: Present completion → WAIT before next phase
    └── ⏸️ STOP - DO NOT START NEXT PHASE WITHOUT APPROVAL
```

---

## 🚀 SECTION 4: DETAILED MIGRATION PHASES

### IMPORTANT: Present this plan first, then WAIT for approval before starting ANY phase

### Phase 1: Core Manager Implementation
**Objective:** Create SpeechRecognitionManager as the central orchestrator

**Components to Migrate:**
- VoiceOSCore (from LegacyAvenue) → SpeechRecognitionManager
- Core initialization logic
- Engine management system
- State management
- Event handling system

**Required Deliverables:**
- [ ] SpeechRecognitionManager.kt implementation
- [ ] Unit tests for all manager functions
- [ ] Architecture diagram showing manager relationships
- [ ] API documentation for public methods
- [ ] Updated VOS4 architecture documentation

**WAIT FOR APPROVAL BEFORE STARTING**

### Phase 2: Service Architecture
**Objective:** Implement Android Service for background operation

**Components to Migrate:**
- VoiceOSService (from LegacyAvenue) → SpeechRecognitionService
- Background operation handling
- Notification management
- Service lifecycle
- Permission handling

**Required Deliverables:**
- [ ] SpeechRecognitionService.kt implementation
- [ ] AndroidManifest.xml updates
- [ ] Service unit tests
- [ ] Service lifecycle documentation
- [ ] Permission handling guide

**WAIT FOR APPROVAL BEFORE STARTING**

### Phase 3: Engine Integration
**Objective:** Connect existing engines to manager

**Components to Integrate:**
- VoskEngine complete integration
- VivokaEngine complete integration
- GoogleEngine complete integration
- Engine factory pattern
- Dynamic engine switching

**Required Deliverables:**
- [ ] EngineFactory implementation
- [ ] Integration tests for each engine
- [ ] Engine switching unit tests
- [ ] Engine configuration documentation
- [ ] Performance benchmarks

**WAIT FOR APPROVAL BEFORE STARTING**

### Phase 4: Application Integration
**Objective:** Integrate with main VOS4 application

**Components to Implement:**
- Module initialization in VoiceOS.kt
- Application-level hooks
- Dependency injection setup
- Module registration system

**Required Deliverables:**
- [ ] Updated VoiceOS.kt with module registration
- [ ] Integration tests
- [ ] Application integration guide
- [ ] Dependency graph documentation
- [ ] Sample usage code

**WAIT FOR APPROVAL BEFORE STARTING**

### Phase 5: Command Processing & Completion
**Objective:** Migrate command processing and finalize system

**Components to Migrate:**
- CommandProcessor (from LegacyAvenue)
- Command routing
- Result handling
- Error management
- Final system integration

**Required Deliverables:**
- [ ] Complete command processing system
- [ ] End-to-end integration tests
- [ ] Error handling documentation
- [ ] Complete API reference
- [ ] Migration completion report

**WAIT FOR APPROVAL BEFORE STARTING**

---

## 🔍 SECTION 5: VERIFICATION PROTOCOLS

### 5.1 COT+ROT Analysis (After EACH Phase)

**COT (Chain of Thought) Analysis:**
```
1. List every class/function in LegacyAvenue component
2. Verify each exists in VOS4 implementation
3. Compare method signatures for equivalence
4. Check initialization sequences match
5. Verify all features are present
6. Confirm all parameters are handled
7. Check return types match
```

**ROT (Reflection on Thought) Analysis:**
```
1. Does VOS4 implementation maintain 100% functional equivalence?
2. Are there any behavioral differences?
3. Have all edge cases been handled?
4. Is error handling equivalent or better?
5. Are there any missing integrations?
6. Do unit tests cover all scenarios?
7. Is performance equivalent or better?
```

### 5.2 Quality Assurance Checklist
- [ ] No truncated code blocks
- [ ] No missing functions or features
- [ ] No omitted error handling
- [ ] Consistent naming (LegacyAvenue - verify exact spelling)
- [ ] No grammatical inconsistencies
- [ ] All TODO items addressed
- [ ] All unit tests passing
- [ ] Documentation complete and detailed

---

## 📝 SECTION 5.5: FILE NAMING CONVENTIONS

### MANDATORY File Naming Format
**Format:** `MODULENAME/APPNAME-WhatItIs-YYMMDD-HHMM`

**Components:**
- **MODULENAME/APPNAME**: Module or application name (e.g., SPEECHRECOGNITION, VOS4, LEGACYAVENUE)
- **WhatItIs**: Brief description of what the file contains (e.g., MIGRATION-STATUS, BUILD-STATUS, INVENTORY)
- **YYMMDD**: Date in 6-digit format (year-month-day)
- **HHMM**: Time in 24-hour format (not 12-hour)

**Correct Examples:**
```
SPEECHRECOGNITION-MIGRATION-STATUS-250903-1430
VOS4-BUILD-STATUS-250903-0430
LEGACYAVENUE-INVENTORY-250903-0425
SPEECHRECOGNITION-IMPLEMENTATION-GUIDE-250903-1615
VOS4-ARCHITECTURE-DIAGRAM-250903-0930
```

**Incorrect Examples:**
```
❌ SPEECHRECOGNITION-MIGRATION-STATUS-250903-230PM  (12-hour format)
❌ migration-status-250903-1430  (no module prefix)
❌ SPEECHRECOGNITION-250903-1430  (missing description)
❌ SPEECHRECOGNITION-MIGRATION-STATUS-20250903-1430  (8-digit date)
```

**Apply This Convention To:**
- Status reports and tracking documents
- Migration documentation
- Implementation guides
- Architecture diagrams
- Analysis reports
- Any temporary or session-specific files

---

## 📚 SECTION 6: DOCUMENTATION REQUIREMENTS

### 6.1 Documentation Updates Per Phase (MANDATORY)

**Module Documentation:**
- `/docs/modules/SpeechRecognition/README.md` - Complete module overview
- `/docs/modules/SpeechRecognition/Architecture.md` - Detailed architecture
- `/docs/modules/SpeechRecognition/API-Reference.md` - Complete API documentation
- `/docs/modules/SpeechRecognition/Changelog.md` - Detailed change log
- `/docs/modules/SpeechRecognition/Testing-Guide.md` - How to test the module

**System-Level Documentation:**
- `/docs/Planning/Architecture/VOS4-Architecture-Master.md` - Updated system architecture
- `/docs/Planning/Architecture/VOS4-Roadmap-Master.md` - Updated roadmap
- `/docs/Planning/Architecture/VOS4-Implementation-Master.md` - Implementation details
- `/docs/Status/Current/VOS4-Status-Comprehensive.md` - Current status
- `/docs/TODO/VOS4-TODO-Master.md` - Updated task list

**Visual Documentation (MUST include):**
- Architecture diagrams (Mermaid or similar)
- Sequence diagrams for key flows
- Class diagrams for major components
- State diagrams for state machines
- Data flow diagrams

### 6.2 Documentation Standards
- **DETAILED, not summary** - Include all technical details
- **Code examples** - Show actual usage
- **Edge cases** - Document all special cases
- **Error scenarios** - Document all error conditions
- **Performance notes** - Include benchmarks and optimizations

---

## 🧪 SECTION 7: TESTING REQUIREMENTS

### 7.1 Unit Test Requirements
**For EACH component created:**
- Minimum 80% code coverage
- Test all public methods
- Test error conditions
- Test edge cases
- Test state transitions
- Mock external dependencies

### 7.2 Test Organization
```
/tests/
├── unit/
│   ├── SpeechRecognitionManagerTest.kt
│   ├── SpeechRecognitionServiceTest.kt
│   ├── EngineFactoryTest.kt
│   └── [Component]Test.kt
├── integration/
│   ├── EngineIntegrationTest.kt
│   ├── ServiceIntegrationTest.kt
│   └── End2EndTest.kt
└── fixtures/
    └── TestData.kt
```

---

## 💾 SECTION 8: VERSION CONTROL PROTOCOL

### 8.1 Commit Rules (MANDATORY)
**After EACH Phase completion:**
1. Stage ONLY files YOU personally modified
   ```bash
   git add <specific-file-path>  # NEVER use git add . or git add -A
   ```
2. Verify staged files
   ```bash
   git status  # Check only your files are staged
   ```
3. Commit with descriptive message
   ```bash
   git commit -m "feat(SpeechRecognition): [Phase X] - Description"
   ```
4. Push to repository
   ```bash
   git push
   ```

### 8.2 Commit Triggers
**MUST commit when:**
- Phase completed successfully
- Context window approaching 10%
- Major milestone reached
- Before requesting compaction

---

## 🔄 SECTION 9: CONTEXT MANAGEMENT PROTOCOL

### 9.1 When Context Reaches 10%:

1. **Update Status Documents:**
   - `/docs/Status/Current/SpeechRecognition-Migration-Status.md`
   - `/docs/TODO/SpeechRecognition-Migration-TODO.md`
   - `/Agent-Instructions/CURRENT-TASK-PRIORITY.md`

2. **Create Precompaction Report:**
   ```markdown
   /docs/Status/Precompaction/SpeechRecognition-Migration-[DATE].md
   
   Include:
   - Current phase status
   - Completed components list
   - Pending tasks with details
   - Current file being worked on
   - Exact line/function if mid-implementation
   - Any decisions made
   - Next immediate steps
   ```

3. **Stage and Commit:**
   - Commit all current work
   - Push to repository

4. **Request Compaction:**
   - Inform user: "Context at 10%. Precompaction report created at [path]. Please type '/compact read precompaction report and continue'"

---

## 🛠️ SECTION 10: SPECIALIZED AGENTS TO USE

| Task Type | Agent to Use | Purpose |
|-----------|--------------|---------|
| Migration Analysis | general-purpose | Analyze and plan migration |
| Implementation | Task (coding) | Write migration code |
| Testing | Task (testing) | Create and run tests |
| Debugging | Task (debugging) | Fix issues and errors |
| Documentation | Task (documentation) | Update all docs |
| UI Creation | Task (UI) | Create any UI components |

---

## 📊 SECTION 11: PROGRESS TRACKING

### Status Updates Required
**After EACH work session update:**
- `/docs/Status/Current/Migration-Progress.md`
- `/docs/TODO/Outstanding-Tasks.md`
- `/Agent-Instructions/SESSION-STATUS-[ID].md`

**Include in updates:**
- Percentage complete per phase
- Components migrated
- Tests written/passing
- Documentation updated
- Blockers encountered
- Next steps

---

## 🚫 SECTION 12: NEVER DO WITHOUT EXPLICIT PERMISSION

1. **START ANY WORK OR IMPLEMENTATION**
2. **BEGIN ANY PHASE**
3. **ANALYZE CODE FOR IMPLEMENTATION**
4. Delete any existing code or files
5. Rename existing classes or methods  
6. Change public APIs
7. Modify architectural patterns
8. Skip verification steps
9. Combine phases
10. Add features not in LegacyAvenue
11. Make "improvements" beyond functional equivalence
12. Use `git add .` or `git add -A`
13. Proceed when context below 10% without compaction
14. Assume approval from questions or comments
15. Start next phase without explicit approval

---

## ✅ SECTION 13: ALWAYS DO

1. **WAIT for explicit permission before ANY work**
2. **PRESENT plan first, implement second**
3. Ask for permission before EVERY change
4. Show complete code blocks (never truncate)
5. Create unit tests for all new code
6. Update ALL affected documentation with detail
7. Verify functional equivalence after each phase
8. Report any discrepancies found
9. Maintain VOS4 coding standards
10. Stage only your modified files
11. Monitor context window percentage
12. Create precompaction reports at 10% context
13. Verify LegacyAvenue spelling from actual folder

---

## 📈 SECTION 14: SUCCESS CRITERIA

### Migration is complete when:
- [ ] 100% functional equivalence achieved
- [ ] All unit tests passing (>80% coverage)
- [ ] All integration tests passing
- [ ] All documentation updated and detailed
- [ ] Performance metrics meet or exceed LegacyAvenue
- [ ] No regression in functionality
- [ ] Code review completed
- [ ] All phases verified with COT+ROT

---

## 🎯 SECTION 15: LIVING IMPLEMENTATION PLAN (UPDATED REAL-TIME)

**Last Updated:** 2025-09-03 12:35  
**Status:** ACTIVE - Phase 1 In Progress  
**Document Type:** Living Document - Updates as requirements change

### 📊 Current Implementation Status

```markdown
## 📋 SPEECHRECOGNITION MIGRATION - LIVING IMPLEMENTATION PLAN

### Overview:
Migration of LegacyAvenue VoiceOS to VOS4 with 100% functional equivalence.
**REVISED APPROACH:** Integration-focused using existing 80-98% complete code.

### ✅ Phase 0: Foundation & Analysis - COMPLETED (45 min)
- [x] LegacyAvenue component mapping
- [x] VOS4 build verification  
- [x] Reusable code discovery (MAJOR WIN: 80-98% complete)
- [x] Success metrics definition
- [x] Test infrastructure setup

### 🚀 Phase 1: Provider Integration (MODIFIED - IN PROGRESS)
**Timeline:** 1-2 weeks (reduced from 4 weeks)
**Approach:** Integrate existing code, not build from scratch

#### Phase 1.1: Vivoka to 100% (Priority 1) - IN PROGRESS
##### Subphase 1.1a: Analysis - COMPLETED ✅
- [x] Located 997-line implementation (98% complete)
- [x] Identified missing 2%: testing, assets, error recovery
- [x] Found critical continuous recognition fix already implemented

##### Subphase 1.1b: Complete Missing 2% (15 min) - PENDING
- [ ] Create integration test file (5 min)
- [ ] Add asset validation enhancement (4 min)
- [ ] Implement error recovery mechanisms (4 min)
- [ ] Add performance monitoring (2 min)

##### Subphase 1.1c: Integration Testing - PENDING
- [ ] Test with actual VSDK
- [ ] Validate continuous recognition
- [ ] Memory/performance benchmarks
- [ ] COT+TOT validation

#### Phase 1.2: AndroidSTT to 100% (Priority 2) - PENDING
##### Subphase 1.2a: Analysis (15 min)
- [ ] Locate implementation (90% complete per analysis)
- [ ] Identify missing 10%
- [ ] Create completion checklist

##### Subphase 1.2b: Complete Missing Components (30 min)
- [ ] Implement missing features
- [ ] Add error handling
- [ ] Verify 19 language support

##### Subphase 1.2c: Testing (15 min)
- [ ] Integration tests
- [ ] Performance validation
- [ ] COT+TOT analysis

#### Phase 1.3: Remaining Providers - PENDING
- [ ] Vosk to 100% (95% complete)
- [ ] Google Cloud integration (80% complete)
- [ ] Provider switching mechanism
- [ ] Fallback system

### 📈 Phase 2-7: REVISED TIMELINE (Subject to Change)

**Phase 2:** Service Architecture (1 week - reduced from 4)
**Phase 3:** Command Processing (1 week - reduced from 4)
**Phase 4:** UI/UX Integration (1 week - reduced from 4)
**Phase 5:** Testing & Validation (1 week - reduced from 3)
**Phase 6:** Optimization (3 days - reduced from 2 weeks)
**Phase 7:** Polish & Deploy (3 days - reduced from 2 weeks)

### 🎯 Key Adjustments Made:
1. **Smaller subphases** - 15-30 minute chunks for faster COT+TOT
2. **Priority reordering** - Vivoka → AndroidSTT → Others
3. **Integration over creation** - Using existing 80-98% code
4. **Timeline reduction** - 7-11 weeks vs original 19-25 weeks

### ⚠️ Current Blockers:
None - Proceeding with Phase 1.1b

### 📊 Metrics:
- **Overall Progress:** 15% → 20% (Phase 0 complete)
- **Time Saved:** 12-14 weeks
- **Code Reuse:** 80-98% for all providers

### 🔄 Next Immediate Actions:
1. Complete Vivoka missing 2% (15 min)
2. Test Vivoka integration
3. Move to AndroidSTT analysis

**This is a LIVING DOCUMENT - Updates with each subphase completion**
```

---

**CRITICAL REMINDERS:** 
- **DO NOT START WITHOUT EXPLICIT APPROVAL**
- The goal is 100% functional equivalence
- Every feature must be preserved
- Documentation must be DETAILED not summarized
- Always WAIT for permission
- Monitor context window constantly
- Verify LegacyAvenue spelling from folder

**FILE LOCATION:** `/Volumes/M Drive/Coding/Warp/vos4/docs/project-instructions/SPEECHRECOGNITION-MIGRATION-INSTRUCTIONS.md`