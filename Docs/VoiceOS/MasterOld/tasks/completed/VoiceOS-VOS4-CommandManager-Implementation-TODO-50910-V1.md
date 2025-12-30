# VOS4 CommandManager Implementation TODO

**Created:** 2025-10-09 19:02:22 PDT
**Status:** Week 4 Ready to Start + JSON/Scraping Integration
**Priority:** HIGH
**Scope:** CommandManager dynamic features + JSON architecture + Scraping database integration

---

## 📊 Overall Status

**Completed Work:**
- ✅ Week 1: HILT Foundation (42 hours)
- ✅ Week 2: Remote Logging, VOSK, Overlays (29 hours)
- ✅ Week 3: Cursor, LearnApp, DeviceManager (40 hours)

**Current Phase:**
- 🟠 Week 4: CommandManager Dynamic Features (38 hours)
- 🟠 JSON Architecture Implementation (12 hours)
- 🟠 Accessibility Scraping Integration (16 hours)

**Total Remaining:** 66 hours

---

## 🎯 PHASE 1: CommandManager Dynamic Features (38 hours)

### 1.1 Dynamic Command Registration (8 hours)

**Status:** ⏸️ NOT STARTED

**Files to Create:**
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/dynamic/DynamicCommandRegistry.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/dynamic/CommandPriority.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/dynamic/ConflictDetector.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/dynamic/NamespaceManager.kt`

**Features:**
- [ ] Runtime command registration/unregistration via public API
- [ ] Priority-based command resolution (1-100, higher = higher priority)
- [ ] Command conflict detection (multiple commands matching same phrase)
- [ ] Namespace management for module isolation
- [ ] Command registration callbacks
- [ ] Unit tests (20+ tests)

---

### 1.2 Custom Command Editor UI (10 hours)

**Status:** ⏸️ NOT STARTED

**Files to Create:**
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/ui/editor/CommandEditorScreen.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/ui/editor/CommandCreationWizard.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/ui/editor/CommandTestingPanel.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/ui/editor/CommandLibraryBrowser.kt`

**Features:**
- [ ] Jetpack Compose UI with Material 3 design
- [ ] Command creation wizard
- [ ] Real-time testing interface
- [ ] Import/export JSON (array-based)
- [ ] 15+ command templates

---

### 1.3 Command Macros (8 hours)

**Status:** ⏸️ NOT STARTED

**Files to Create:**
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/macros/CommandMacro.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/macros/MacroExecutor.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/macros/MacroDSL.kt`

**Features:**
- [ ] Multi-step sequences (up to 20 steps)
- [ ] Conditional execution (if/else)
- [ ] Variable support
- [ ] Loop/branching support

---

### 1.4 Context-Aware Commands (12 hours)

**Status:** 🟡 PARTIALLY STARTED (ContextManager.kt exists)

**Files to Create:**
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/context/ContextDetector.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/context/ContextMatcher.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/context/PreferenceLearner.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/context/ContextSuggester.kt`

**Features:**
- [ ] App-specific activation
- [ ] Screen-state-based commands
- [ ] Time/location-based commands
- [ ] User preference learning

---

## 🎯 PHASE 2: JSON Architecture (12 hours)

### 2.1 Array-Based JSON (4 hours)

**Status:** ⏸️ NOT STARTED

**Files to Create:**
- `modules/managers/CommandManager/src/main/assets/localization/commands/en-US.json`
- `modules/managers/CommandManager/src/main/assets/localization/commands/es-ES.json`
- `modules/managers/CommandManager/src/main/assets/localization/commands/fr-FR.json`

**Format:**
```json
{
  "version": "1.0",
  "locale": "en-US",
  "fallback": "en-US",
  "commands": [
    ["navigate_forward", "forward", ["next", "advance"], "Move forward"],
    ["action_click", "click", ["tap", "select"], "Activate element"]
  ]
}
```

**Features:**
- [ ] 1 line per command (73% size reduction)
- [ ] Array structure: [id, primary, [synonyms], description]
- [ ] All locales in array format

---

### 2.2 English Fallback Database (3 hours)

**Status:** ⏸️ NOT STARTED

**Files to Create:**
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/loader/CommandLoader.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/loader/ArrayJsonParser.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/loader/CommandResolver.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/database/CommandDatabase.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/database/VoiceCommandEntity.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/database/VoiceCommandDao.kt`

**Features:**
- [ ] ALWAYS load English first (fallback)
- [ ] Load user locale if different
- [ ] Fallback resolution: user locale → English → null
- [ ] Array-based JSON parsing

---

### 2.3 Number Overlay Aesthetics (5 hours)

**Status:** ⏸️ NOT STARTED

**Files to Create:**
- `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/overlays/NumberOverlayRenderer.kt`
- `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/overlays/NumberOverlayStyle.kt`

**Design:**
- [ ] Circular badge (32dp diameter)
- [ ] Top-right/top-left anchor (configurable)
- [ ] 4px offset from element edge
- [ ] Material 3 colors: Green (#4CAF50), Orange (#FF9800), Grey (#9E9E9E)
- [ ] White number text (14sp bold)
- [ ] Drop shadow (4px blur)

---

## 🎯 PHASE 3: Scraping Integration (16 hours)

### 3.1 App Scraping Database (6 hours)

**Status:** ⏸️ NOT STARTED

**Files to Create:**
- `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/scraping/AppScrapingDatabase.kt`
- `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/scraping/ScrapedAppEntity.kt`
- `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/scraping/ScrapedElementEntity.kt`
- `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/scraping/ScrapedHierarchyEntity.kt`
- `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/scraping/GeneratedCommandEntity.kt`
- `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/scraping/*Dao.kt` (4 DAOs)

**Features:**
- [ ] Store scraped apps with hash
- [ ] Store elements with accessibility properties
- [ ] Hierarchical parent-child relationships
- [ ] Generated commands per element
- [ ] Usage statistics tracking

---

### 3.2 Scraping Integration (6 hours)

**Status:** ⏸️ NOT STARTED

**Files to Create:**
- `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/scraping/AccessibilityTreeScraper.kt`
- `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/scraping/ElementHasher.kt`
- `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/scraping/ScrapingCoordinator.kt`

**Features:**
- [ ] Hook into window state changes
- [ ] Scrape accessibility tree
- [ ] Calculate element hashes
- [ ] Build hierarchy relationships
- [ ] Generate commands from scraped data

---

### 3.3 Voice Recognition Integration (4 hours)

**Status:** ⏸️ NOT STARTED

**Files to Create:**
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/processor/VoiceCommandProcessor.kt`
- `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/processor/NodeFinder.kt`

**Features:**
- [ ] Query scraped database for commands
- [ ] Find UI nodes by hash
- [ ] Execute actions on elements
- [ ] Update usage statistics

---

## 🚀 Deployment Strategy: 5 Specialized Agents

### Agent 1: Android OS Expert (Dynamic Commands + Context)
**Expertise:** Android OS internals, Runtime APIs, System services
**Duration:** 20 hours
**Tasks:**
- Dynamic command registration (8h)
- Context-aware commands (12h)
**Files:** 8-10 files

### Agent 2: UI/UX Expert (Command Editor + Overlays)
**Expertise:** Jetpack Compose, Material 3, UI/UX design
**Duration:** 15 hours
**Tasks:**
- Custom command editor UI (10h)
- Number overlay aesthetics (5h)
**Files:** 6-8 files

### Agent 3: Database Expert (JSON + Scraping DB)
**Expertise:** Room Database, Data modeling, SQL optimization
**Duration:** 13 hours
**Tasks:**
- English fallback database (3h)
- App scraping database (6h)
- Voice recognition integration (4h)
**Files:** 12-15 files

### Agent 4: Accessibility Expert (Scraping + Macros)
**Expertise:** AccessibilityService, Node traversal, UI automation
**Duration:** 14 hours
**Tasks:**
- Command macros (8h)
- Scraping integration (6h)
**Files:** 6-8 files

### Agent 5: Documentation Expert (JSON + Tests)
**Expertise:** Technical writing, API documentation, Test coverage
**Duration:** 12 hours
**Tasks:**
- Array-based JSON creation (4h)
- Unit tests (4h)
- Integration tests (4h)
**Files:** 15+ files (JSON + tests)

---

## 📊 Timeline

**Parallel Execution:**
- Agents 1-5 run simultaneously
- Total time: **~20 hours** (longest agent)
- Sequential would be: 74 hours
- **Savings: 54 hours (73% faster)**

---

## ✅ Success Criteria

- [ ] All 38h CommandManager features complete
- [ ] Array-based JSON for all commands
- [ ] English fallback always loaded
- [ ] Number overlays aesthetically pleasing
- [ ] Apps scraped and stored in database
- [ ] Voice commands work from scraped data
- [ ] 85+ tests passing
- [ ] 0 build errors

---

**Last Updated:** 2025-10-09 19:02:22 PDT
**Ready to Deploy:** 5 specialized PhD-level agents
**Estimated Completion:** ~20 hours with parallel execution
