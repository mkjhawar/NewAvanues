# Modular Refactor Plan: .ava Integration

**Date:** 2025-11-17
**Strategy:** Hybrid Option 1 + 3 (Incremental + Modular)
**Goal:** Restore .ava integration while isolating Kotlin compiler bug

---

## Current State (Problems)

**Three Monolithic Files:**
1. AvaFileLoader.kt (270 lines) - Compiler error at EOF+1 (intermittent)
2. IntentExamplesMigration.kt (214 lines) - Compiler error at EOF+1 (persistent)
3. VoiceOSIntegration.kt (309 lines) - Cascade errors

**Total:** 793 lines across 3 files with unknown compiler bug trigger

---

## Target State (Solution)

**10 Focused Modules:**
- Each module: 20-60 lines
- Single responsibility
- Clear dependencies
- Incremental compilation testing

**Total:** ~430 lines across 10 files (plus reduced complexity)

---

## Module Breakdown

### Layer 1: Data Models (SAFEST - Pure Data)

#### Module 1: AvaIntent.kt
**Package:** `com.augmentalis.ava.features.nlu.ava.model`
**Lines:** ~35
**Risk:** ⚪ VERY LOW (pure data classes)
**Dependencies:** None

```kotlin
// Data classes only - no logic
data class AvaIntent(...)
data class AvaFileMetadata(...)
data class AvaFile(...)
```

**Functionality:**
- Define .ava format data structures
- No I/O, no parsing, no business logic
- Just structure definitions

---

#### Module 2: VoiceOSCommand.kt
**Package:** `com.augmentalis.ava.features.nlu.voiceos.model`
**Lines:** ~25
**Risk:** ⚪ VERY LOW (pure data classes)
**Dependencies:** None

```kotlin
// Data classes only
data class VoiceOSCommand(...)
data class VoiceOSFileInfo(...)
data class VoiceOSFile(...)
```

**Functionality:**
- Define .vos format data structures
- No I/O, no parsing, no business logic

---

### Layer 2: Parsing (LOW RISK - Pure Functions)

#### Module 3: AvaFileParser.kt
**Package:** `com.augmentalis.ava.features.nlu.ava.parser`
**Lines:** ~50
**Risk:** 🟡 LOW (JSON parsing, no I/O)
**Dependencies:** Module 1 (AvaIntent)

```kotlin
object AvaFileParser {
    fun parse(jsonString: String): AvaFile
    fun parseIntent(intentJson: JSONObject, locale: String): AvaIntent
    fun parseMetadata(metaJson: JSONObject): AvaFileMetadata
    fun parseGlobalSynonyms(synJson: JSONObject): Map<String, List<String>>
}
```

**Functionality:**
- Parse .ava JSON string → AvaFile object
- Pure functions, no side effects
- No file I/O (receives string, returns object)

---

#### Module 4: VoiceOSParser.kt
**Package:** `com.augmentalis.ava.features.nlu.voiceos.parser`
**Lines:** ~45
**Risk:** 🟡 LOW (JSON parsing, no I/O)
**Dependencies:** Module 2 (VoiceOSCommand)

```kotlin
object VoiceOSParser {
    fun parse(jsonString: String): VoiceOSFile
    fun parseCommand(cmdJson: JSONObject): VoiceOSCommand
    fun parseFileInfo(infoJson: JSONObject): VoiceOSFileInfo
}
```

**Functionality:**
- Parse .vos JSON string → VoiceOSFile object
- Pure functions, no side effects

---

### Layer 3: File I/O (MEDIUM RISK - File System Operations)

#### Module 5: AvaFileReader.kt
**Package:** `com.augmentalis.ava.features.nlu.ava.io`
**Lines:** ~55
**Risk:** 🟠 MEDIUM (file I/O, directory traversal)
**Dependencies:** Module 1 (AvaIntent), Module 3 (AvaFileParser)

```kotlin
class AvaFileReader {
    fun loadAvaFile(filePath: String): AvaFile
    fun loadIntentsFromDirectory(directoryPath: String, source: String): List<AvaIntent>
    fun getAvailableLocales(baseDir: String): List<String>
    private fun findAvaFiles(directory: File): Array<File>
}
```

**Functionality:**
- Read .ava files from file system
- List files in directories
- Locale detection and fallback
- Uses AvaFileParser for parsing

---

#### Module 6: VoiceOSDetector.kt
**Package:** `com.augmentalis.ava.features.nlu.voiceos.detection`
**Lines:** ~30
**Risk:** 🟡 LOW (package manager queries only)
**Dependencies:** None

```kotlin
class VoiceOSDetector(private val context: Context) {
    fun isVoiceOSInstalled(): Boolean
    fun getInstalledVoiceOSPackages(): List<String>

    companion object {
        private const val VOICEOS_PACKAGE = "com.avanues.voiceos"
        private const val VOICEOS_LAUNCHER_PACKAGE = "com.avanues.launcher"
        private const val VOICEOS_FRAMEWORK_PACKAGE = "com.ideahq.voiceos"
    }
}
```

**Functionality:**
- Check if VoiceOS installed
- Package detection via PackageManager
- No ContentProvider queries (those go in Module 7)

---

### Layer 4: Integration (HIGHER RISK - External Communication)

#### Module 7: VoiceOSQueryProvider.kt
**Package:** `com.augmentalis.ava.features.nlu.voiceos.provider`
**Lines:** ~65
**Risk:** 🟠 MEDIUM (ContentProvider IPC)
**Dependencies:** Module 6 (VoiceOSDetector)

```kotlin
class VoiceOSQueryProvider(private val context: Context) {
    fun queryAppContext(): String?
    fun queryClickableElements(): List<Map<String, String>>
    fun queryCommandHierarchy(): String?

    private fun executeQuery(uri: Uri, projection: Array<String>?): Cursor?

    companion object {
        private const val VOICEOS_AUTHORITY = "com.avanues.voiceos.provider"
        private val VOICEOS_APP_CONTEXT_URI = Uri.parse("content://$VOICEOS_AUTHORITY/app_context")
        private val VOICEOS_CLICKABLE_ELEMENTS_URI = Uri.parse("content://$VOICEOS_AUTHORITY/clickable_elements")
    }
}
```

**Functionality:**
- Query VoiceOS ContentProvider
- App context retrieval
- Clickable elements retrieval
- Graceful error handling

---

#### Module 8: AvaToEntityConverter.kt
**Package:** `com.augmentalis.ava.features.nlu.ava.converter`
**Lines:** ~40
**Risk:** 🟡 LOW (data transformation only)
**Dependencies:** Module 1 (AvaIntent), DatabaseProvider

```kotlin
object AvaToEntityConverter {
    fun convertToEntities(intents: List<AvaIntent>): List<IntentExampleEntity>
    fun convertToEntity(intent: AvaIntent, isPrimary: Boolean): IntentExampleEntity
    fun generateHash(intentId: String, exampleText: String): String
}
```

**Functionality:**
- Convert AvaIntent → IntentExampleEntity
- Hash generation for deduplication
- Timestamp management

---

#### Module 9: VoiceOSToAvaConverter.kt
**Package:** `com.augmentalis.ava.features.nlu.voiceos.converter`
**Lines:** ~60
**Risk:** 🟡 LOW (data transformation only)
**Dependencies:** Module 1 (AvaIntent), Module 2 (VoiceOSCommand)

```kotlin
object VoiceOSToAvaConverter {
    fun convertVosToAva(vosFile: VoiceOSFile): AvaFile
    fun convertCommand(command: VoiceOSCommand, locale: String): AvaIntent

    private fun detectCategory(action: String): String
    private fun generateTags(cmdText: String): List<String>
}
```

**Functionality:**
- Convert VoiceOSFile → AvaFile
- Command mapping (action → intent)
- Category detection
- Tag generation

---

### Layer 5: Orchestration (MEDIUM RISK - Coordination Logic)

#### Module 10: IntentSourceCoordinator.kt
**Package:** `com.augmentalis.ava.features.nlu.migration`
**Lines:** ~75
**Risk:** 🟠 MEDIUM (complex orchestration)
**Dependencies:** Modules 5, 7, 8, 9, DatabaseProvider, LanguagePackManager

```kotlin
class IntentSourceCoordinator(private val context: Context) {
    suspend fun migrateIfNeeded(): Boolean
    suspend fun forceMigration(): Int
    suspend fun clearDatabase()
    suspend fun getMigrationStatus(): Map<String, Any>

    private suspend fun migrate(): Int
    private suspend fun loadFromAvaSources(): List<IntentExampleEntity>
    private suspend fun loadFromVoiceOSSources(): List<IntentExampleEntity>
    private suspend fun loadFromJsonSource(): List<IntentExampleEntity>
}
```

**Functionality:**
- Orchestrates all sources (.ava, .vos, JSON)
- Priority-based loading (try .ava first, fallback to JSON)
- Database population
- Migration status reporting
- **Replaces:** IntentExamplesMigration.kt

---

## Implementation Order & Testing Strategy

### Phase 0: Baseline Preparation
```bash
# Revert to working state (commit be6f94e)
git checkout be6f94e -- Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/IntentExamplesMigration.kt

# Delete problematic files
rm Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/AvaFileLoader.kt
rm Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/VoiceOSIntegration.kt

# Verify compilation
./gradlew :Universal:AVA:Features:NLU:compileDebugKotlinAndroid
# Expected: BUILD SUCCESSFUL ✅
```

---

### Phase 1: Data Models (Safe Foundation)

**Step 1.1: Module 1 (AvaIntent.kt)**
```bash
# Create data classes
Write: Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/ava/model/AvaIntent.kt

# Test compilation
./gradlew :Universal:AVA:Features:NLU:compileDebugKotlinAndroid
# Expected: BUILD SUCCESSFUL ✅ (very low risk)
```

**Step 1.2: Module 2 (VoiceOSCommand.kt)**
```bash
# Create data classes
Write: Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/voiceos/model/VoiceOSCommand.kt

# Test compilation
./gradlew :Universal:AVA:Features:NLU:compileDebugKotlinAndroid
# Expected: BUILD SUCCESSFUL ✅ (very low risk)
```

**Decision Point 1:**
- ✅ If both succeed → Proceed to Phase 2
- ❌ If either fails → CRITICAL: Even pure data classes trigger bug, need deeper investigation

---

### Phase 2: Parsing Layer (Pure Functions)

**Step 2.1: Module 3 (AvaFileParser.kt)**
```bash
# Create parser
Write: Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/ava/parser/AvaFileParser.kt

# Test compilation
./gradlew :Universal:AVA:Features:NLU:compileDebugKotlinAndroid
# Expected: BUILD SUCCESSFUL ✅ (low risk)
```

**Decision Point 2A:**
- ✅ If succeeds → Proceed to Step 2.2
- ❌ If fails → JSON parsing triggers bug, try simplifying (remove inline functions, reduce nesting)

**Step 2.2: Module 4 (VoiceOSParser.kt)**
```bash
# Create parser
Write: Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/voiceos/parser/VoiceOSParser.kt

# Test compilation
./gradlew :Universal:AVA:Features:NLU:compileDebugKotlinAndroid
# Expected: BUILD SUCCESSFUL ✅ (low risk)
```

**Decision Point 2B:**
- ✅ If succeeds → Proceed to Phase 3
- ❌ If fails → Identify problematic parsing pattern, simplify

---

### Phase 3: File I/O Layer

**Step 3.1: Module 5 (AvaFileReader.kt)**
```bash
# Create file reader
Write: Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/ava/io/AvaFileReader.kt

# Test compilation
./gradlew :Universal:AVA:Features:NLU:compileDebugKotlinAndroid
# Expected: BUILD SUCCESSFUL ✅ or ❌ COMPILER BUG
```

**Decision Point 3A:**
- ✅ If succeeds → Proceed to Step 3.2
- ❌ If fails → **HIGH CONFIDENCE: File I/O pattern triggers bug**
  - Try workarounds:
    - Remove `use {}` blocks (use manual try/finally)
    - Remove `listFiles { filter }` (use listFiles() then manual filter)
    - Simplify directory traversal
    - Remove inline functions

**Step 3.2: Module 6 (VoiceOSDetector.kt)**
```bash
# Create detector
Write: Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/voiceos/detection/VoiceOSDetector.kt

# Test compilation
./gradlew :Universal:AVA:Features:NLU:compileDebugKotlinAndroid
# Expected: BUILD SUCCESSFUL ✅ (low risk - just PackageManager calls)
```

---

### Phase 4: Integration Layer

**Step 4.1: Module 7 (VoiceOSQueryProvider.kt)**
```bash
# Create query provider
Write: Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/voiceos/provider/VoiceOSQueryProvider.kt

# Test compilation
./gradlew :Universal:AVA:Features:NLU:compileDebugKotlinAndroid
# Expected: BUILD SUCCESSFUL ✅ or ❌ (ContentProvider queries)
```

**Step 4.2: Module 8 (AvaToEntityConverter.kt)**
```bash
# Create converter
Write: Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/ava/converter/AvaToEntityConverter.kt

# Test compilation
./gradlew :Universal:AVA:Features:NLU:compileDebugKotlinAndroid
# Expected: BUILD SUCCESSFUL ✅ (low risk - data transformation)
```

**Step 4.3: Module 9 (VoiceOSToAvaConverter.kt)**
```bash
# Create converter
Write: Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/voiceos/converter/VoiceOSToAvaConverter.kt

# Test compilation
./gradlew :Universal:AVA:Features:NLU:compileDebugKotlinAndroid
# Expected: BUILD SUCCESSFUL ✅ (low risk - data transformation)
```

---

### Phase 5: Orchestration Layer

**Step 5.1: Module 10 (IntentSourceCoordinator.kt)**
```bash
# Create coordinator
Write: Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/migration/IntentSourceCoordinator.kt

# Test compilation
./gradlew :Universal:AVA:Features:NLU:compileDebugKotlinAndroid
# Expected: BUILD SUCCESSFUL ✅ or ❌ (complex orchestration)
```

**Decision Point 5:**
- ✅ If succeeds → Proceed to Phase 6
- ❌ If fails → Simplify orchestration logic, remove nested coroutines/suspends

---

### Phase 6: Integration with IntentClassifier

**Step 6.1: Update IntentClassifier.kt**
```kotlin
// Replace:
// val migration = IntentExamplesMigration(context)

// With:
val migration = IntentSourceCoordinator(context)
// API is identical, drop-in replacement
```

**Step 6.2: Final Compilation Test**
```bash
./gradlew :Universal:AVA:Features:NLU:compileDebugKotlinAndroid
# Expected: BUILD SUCCESSFUL ✅
```

---

## Benefits of This Approach

### 1. Bug Isolation
- **Pinpoint Trigger:** If Module 5 fails but 1-4 succeed, we know file I/O pattern triggers the bug
- **Targeted Fixes:** Can rewrite only the problematic module
- **Workaround Options:** Can simplify just the failing module

### 2. Better Architecture
- **Single Responsibility:** Each module does ONE thing
- **Testability:** Each module can be unit tested independently
- **Maintainability:** 40-line files are easier to understand than 300-line files
- **Reusability:** VoiceOSParser can be used by other modules later

### 3. Reduced Complexity
- **793 lines → ~430 lines** (removing duplication, simplifying logic)
- **3 files → 10 files** (but each much simpler)
- **Clear Dependencies:** Explicit imports show module relationships

### 4. Incremental Progress
- **Partial Success:** If we get to Module 8 before hitting bug, we have 80% functionality working
- **Staged Commits:** Can commit each successful phase
- **Rollback Safety:** Can revert individual modules without losing all work

---

## Risk Assessment

**If the bug appears at:**

- **Module 1-2 (Data):** ⚠️ CRITICAL - Even pure data classes fail, very deep compiler issue
- **Module 3-4 (Parsing):** 🟡 MODERATE - JSON parsing patterns problematic, use simpler parsing
- **Module 5 (File I/O):** 🟠 LIKELY - File operations are complex, use workarounds (no inline, no lambdas)
- **Module 7 (ContentProvider):** 🟠 LIKELY - IPC might trigger bug, simplify Cursor handling
- **Module 10 (Orchestration):** 🟠 POSSIBLE - Complex async logic, simplify coroutines

**Most Likely Culprits (Based on Pattern):**
1. **Module 5 (AvaFileReader)** - File I/O with closures (`use {}`, `listFiles { }`)
2. **Module 7 (VoiceOSQueryProvider)** - ContentProvider IPC with Cursor handling
3. **Module 10 (IntentSourceCoordinator)** - Complex coroutine orchestration

---

## Fallback Strategies

### If Module 5 Fails (File I/O)
```kotlin
// Original (might trigger bug):
val files = directory.listFiles { file -> file.extension == "ava" }
file.bufferedReader().use { it.readText() }

// Workaround (simpler):
val files = directory.listFiles() ?: emptyArray()
val avaFiles = files.filter { it.extension == "ava" }
val text = file.readText()  // No use {} block
```

### If Module 7 Fails (ContentProvider)
```kotlin
// Original (might trigger bug):
cursor?.use {
    if (it.moveToFirst()) { ... }
}

// Workaround (manual close):
val cursor = query(...)
try {
    if (cursor?.moveToFirst() == true) { ... }
} finally {
    cursor?.close()
}
```

### If Module 10 Fails (Orchestration)
- Split into IntentSourceCoordinator + AvaSourceLoader + JsonSourceLoader
- Further decompose async logic
- Use simpler synchronous patterns where possible

---

## Success Criteria

**Minimum Success:**
- Modules 1-9 compile successfully
- Module 10 works OR can be split further
- IntentClassifier integrates successfully
- Full test suite passes

**Full Success:**
- All 10 modules compile on first try
- IntentClassifier integration works
- Tests pass
- No compiler bugs encountered

**Partial Success:**
- Modules 1-7 work
- Module 8-10 trigger bug → document exact patterns
- Fallback to simplified version of problematic modules

---

## Next Steps

**Awaiting User Approval:**
1. Review this breakdown
2. Approve module structure
3. Approve implementation order
4. Start Phase 0 (Baseline Preparation)

**Estimated Time:**
- Phase 0: 5 minutes
- Phase 1: 15 minutes (Modules 1-2)
- Phase 2: 20 minutes (Modules 3-4)
- Phase 3: 25 minutes (Modules 5-6)
- Phase 4: 30 minutes (Modules 7-9)
- Phase 5: 20 minutes (Module 10)
- Phase 6: 10 minutes (Integration)
- **Total:** ~2 hours (with testing and potential bug workarounds)

---

**Ready to proceed?**
