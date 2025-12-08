# VOS4 Naming Conventions

**Document Type:** Project Standards
**Last Updated:** 2025-10-10 09:12:42 PDT
**Status:** Official Standard
**Compliance:** Mandatory for all VOS4 development

---

## Executive Summary

This document defines the official naming conventions for all VOS4 project artifacts including source code, documentation, folders, packages, variables, and files. Adherence to these standards is **MANDATORY** for all contributions.

**Key Principles:**
- **Consistency:** Same naming pattern for same artifact type
- **Clarity:** Names should be self-documenting
- **Standards:** Follow industry best practices (Kotlin, Android, Java)
- **NO REDUNDANCY:** Avoid redundant prefixes/suffixes in names

---

## Table of Contents

1. [Source Code Files](#source-code-files)
2. [Documentation Files](#documentation-files)
3. [Folder Names](#folder-names)
4. [Package Names](#package-names)
5. [Class and Interface Names](#class-and-interface-names)
6. [Method and Function Names](#method-and-function-names)
7. [Variable and Property Names](#variable-and-property-names)
8. [Constant Names](#constant-names)
9. [Resource Files](#resource-files)
10. [Database Entity Names](#database-entity-names)
11. [Quick Reference](#quick-reference)

---

## Source Code Files

### Kotlin/Java Class Files

**Convention:** `PascalCase.kt` or `PascalCase.java`

**Rules:**
- File name MUST match the primary class name
- Use PascalCase (first letter of each word capitalized)
- No underscores, no hyphens
- Descriptive, not abbreviated

**Examples:**
```
✅ CORRECT:
AccessibilityScrapingIntegration.kt
VoiceCommandProcessor.kt
GeneratedCommandEntity.kt
AppScrapingDatabase.kt
UUIDCreator.kt

❌ INCORRECT:
accessibility_scraping_integration.kt  (snake_case)
voice-command-processor.kt             (kebab-case)
generatedCommandEntity.kt              (camelCase)
AppScraping.kt                         (too abbreviated)
ASIntegration.kt                       (cryptic abbreviation)
```

**Naming Patterns:**

| File Type | Pattern | Example |
|-----------|---------|---------|
| Entity | `[Name]Entity.kt` | `ScrapedElementEntity.kt` |
| DAO | `[Name]Dao.kt` | `ScrapedElementDao.kt` |
| Database | `[Name]Database.kt` | `AppScrapingDatabase.kt` |
| Manager | `[Name]Manager.kt` | `CommandManager.kt` |
| Service | `[Name]Service.kt` | `VoiceAccessibilityService.kt` |
| Processor | `[Name]Processor.kt` | `VoiceCommandProcessor.kt` |
| Adapter | `[Name]Adapter.kt` | `CommandListAdapter.kt` |
| Fragment | `[Name]Fragment.kt` | `SettingsFragment.kt` |
| Activity | `[Name]Activity.kt` | `MainActivity.kt` |
| ViewModel | `[Name]ViewModel.kt` | `CommandViewModel.kt` |

---

## Documentation Files

### Markdown Documentation Files

**Convention:** `PascalCase-With-Hyphens-YYMMDD-HHMM.md`

**Rules:**
- Use PascalCase for each word
- Separate words with hyphens (-)
- MUST include timestamp suffix: `YYMMDD-HHMM`
- Descriptive, indicates content type
- No spaces, no underscores

**Timestamp Format:**
- `YYMMDD`: Year-Month-Day (e.g., 251010 for Oct 10, 2025)
- `HHMM`: Hour-Minute in 24-hour format (e.g., 0912 for 9:12 AM, 1430 for 2:30 PM)
- Get timestamp: `date "+%y%m%d-%H%M"`

**Examples:**
```
✅ CORRECT:
UUID-Hash-Persistence-Architecture-251010-0157.md
Architecture-Refactor-Roadmap-251010-0157.md
Module-Documentation-Structure-Audit-251010-0907.md
VOS4-TODO-Master-251009-0230.md
SpeechRecognition-Status-251009-1430.md

❌ INCORRECT:
uuid_hash_persistence.md                          (no timestamp, snake_case)
architecture refactor roadmap.md                  (spaces, no timestamp)
module-documentation-structure-audit.md           (kebab-case, no timestamp)
VOS4-TODO-Master.md                               (no timestamp)
SpeechRecognition-Status-251009-02:30.md         (colon in timestamp - INVALID)
```

**Document Type Prefixes:**

| Document Type | Prefix/Pattern | Example |
|--------------|----------------|---------|
| TODO Lists | `[Module]-TODO-[Date].md` | `VOS4-TODO-Master-251010-0912.md` |
| Status Reports | `[Module]-Status-[Date].md` | `CommandManager-Status-251010-0912.md` |
| Architecture | `[Topic]-Architecture-[Date].md` | `UUID-Persistence-Architecture-251010-0912.md` |
| Roadmaps | `[Topic]-Roadmap-[Date].md` | `Architecture-Refactor-Roadmap-251010-0912.md` |
| Audits | `[Topic]-Audit-[Date].md` | `Module-Structure-Audit-251010-0912.md` |
| Analysis | `[Topic]-Analysis-[Date].md` | `Hash-System-Analysis-251010-0912.md` |
| API Docs | `[ClassName]-API-[Date].md` | `UUIDCreator-API-251009-1123.md` |
| Fix Plans | `[Module]-[Issue]-Fix-Plan-[Date].md` | `VoiceAccessibility-ForeignKey-Fix-Plan-251010-0021.md` |

**Special Documentation Files (No Timestamp Required):**
```
README.md         (project root, module root)
CLAUDE.md         (agent instructions)
CHANGELOG.md      (ongoing changelog)
LICENSE.md        (license file)
.gitignore        (git configuration)
```

---

## Folder Names

### Code Module Folders

**Convention:** `PascalCase`

**Rules:**
- Must match the module name exactly
- No spaces, no hyphens, no underscores
- Located in `/modules/{apps|libraries|managers}/`

**Examples:**
```
✅ CORRECT:
modules/apps/VoiceAccessibility/
modules/apps/LearnApp/
modules/libraries/UUIDCreator/
modules/managers/CommandManager/

❌ INCORRECT:
modules/apps/voice-accessibility/     (kebab-case)
modules/apps/Voice_Accessibility/     (snake_case)
modules/apps/voiceaccessibility/      (lowercase)
```

### Documentation Module Folders

**Convention:** `PascalCase` (MUST MATCH CODE MODULE NAME EXACTLY)

**Rules:**
- MUST match the corresponding code module name exactly
- Use PascalCase (same as code modules)
- No spaces, no hyphens, no underscores
- Located in `/docs/modules/{ModuleName}/`

**Examples:**
```
✅ CORRECT (matches code):
docs/modules/VoiceAccessibility/    ← matches modules/apps/VoiceAccessibility/
docs/modules/CommandManager/        ← matches modules/managers/CommandManager/
docs/modules/UUIDCreator/           ← matches modules/libraries/UUIDCreator/
docs/modules/DeviceManager/         ← matches modules/libraries/DeviceManager/

❌ INCORRECT (doesn't match code):
docs/modules/voice-accessibility/   (kebab-case - WRONG)
docs/modules/CommandManager/       (kebab-case - WRONG)
docs/modules/uuid-creator/          (kebab-case - WRONG)
docs/modules/DeviceManager/        (kebab-case - WRONG)
```

**Code-to-Documentation Mapping:**
```
Code Module                          → Documentation Folder
modules/apps/VoiceCursor/           → docs/modules/VoiceCursor/
modules/apps/VoiceOSCore/           → docs/modules/VoiceOSCore/
modules/libraries/DeviceManager/    → docs/modules/DeviceManager/
modules/libraries/SpeechRecognition/→ docs/modules/SpeechRecognition/
modules/managers/CommandManager/    → docs/modules/CommandManager/
modules/managers/HUDManager/        → docs/modules/HUDManager/
```

### System Documentation Folders

**Convention:** `kebab-case`

**Rules:**
- All lowercase
- Separate words with hyphens (-)
- Used for non-module documentation folders
- Examples: `voiceos-master/`, `documentation-control/`

**Examples:**
```
✅ CORRECT:
docs/voiceos-master/          (system-level docs)
docs/documentation-control/   (doc management)
docs/scripts/agent-tools/     (automation scripts)
Active/                       (current work - PascalCase OK for special)
ProjectInstructions/          (PascalCase OK for special)

❌ INCORRECT:
docs/VoiceOSMaster/           (PascalCase - use kebab-case)
docs/Documentation_Control/   (snake_case)
docs/voiceosmaster/           (no hyphens - hard to read)
```

### Special Folders

**Convention:** Lowercase or UPPERCASE depending on purpose

**Lowercase folders** (standard directories):
```
docs/
modules/
coding/
tests/
agent-tools/
```

**UPPERCASE folders** (special categories):
```
coding/TODO/
coding/STATUS/
coding/ISSUES/
coding/DECISIONS/
```

---

## Package Names

### Kotlin/Java Packages

**Convention:** `lowercase.dot.separated`

**Rules:**
- All lowercase, no caps
- Separate package levels with dots (.)
- Follow reverse domain convention
- Module name should match package tail

**Standard VOS4 Namespace:**
```
com.augmentalis.[module].[subpackage]
```

**Examples:**
```
✅ CORRECT:
com.augmentalis.voiceaccessibility
com.augmentalis.voiceaccessibility.scraping
com.augmentalis.voiceaccessibility.scraping.database
com.augmentalis.commandmanager
com.augmentalis.uuidcreator.database

❌ INCORRECT:
com.augmentalis.VoiceAccessibility        (PascalCase - WRONG)
com.augmentalis.voice_accessibility       (underscores - WRONG)
com.ai.voiceaccessibility                 (old namespace - DEPRECATED)
```

**Package Structure Pattern:**
```
com.augmentalis.[module]/
├── [feature]/                # Feature-specific code
│   ├── database/            # Database layer
│   ├── entities/            # Data entities
│   ├── dao/                 # Data access objects
│   ├── ui/                  # UI components
│   └── utils/               # Utilities
└── [FeatureManager].kt      # Main manager/coordinator
```

---

## Class and Interface Names

### Class Names

**Convention:** `PascalCase`

**Rules:**
- Noun or noun phrase
- First letter of each word capitalized
- Descriptive of purpose
- Avoid redundant prefixes

**Examples:**
```
✅ CORRECT:
class AccessibilityScrapingIntegration
class VoiceCommandProcessor
class ScrapedElementEntity
data class LearnAppResult

❌ INCORRECT:
class accessibilityScrapingIntegration   (camelCase)
class Accessibility_Scraping             (underscores)
class ASI                                (cryptic abbreviation)
class VoiceAccessibilityScrapingIntegrationManager  (redundant, too long)
```

### Interface Names

**Convention:** `PascalCase` (same as classes)

**Rules:**
- Use adjectives ending in "-able" when appropriate
- Use nouns for capability interfaces
- No "I" prefix (not C# style)

**Examples:**
```
✅ CORRECT:
interface Clickable
interface Scrollable
interface CommandExecutor
interface DatabaseDao

❌ INCORRECT:
interface IClickable         (C# style prefix - avoid)
interface clickable          (lowercase)
interface Click_able         (underscores)
```

### Data Class Names

**Convention:** Same as regular classes - `PascalCase`

**Examples:**
```
✅ CORRECT:
data class ScrapedElementEntity
data class LearnAppResult
data class CommandResult
data class HierarchyBuildInfo

❌ INCORRECT:
data class scrapedElementEntity   (camelCase)
data class Scraped_Element        (underscores)
```

---

## Method and Function Names

### Kotlin Functions and Methods

**Convention:** `camelCase`

**Rules:**
- Start with lowercase letter
- Verb or verb phrase
- Descriptive of action performed
- Boolean-returning methods can start with "is", "has", "can"

**Examples:**
```
✅ CORRECT:
fun scrapeCurrentWindow()
fun processVoiceCommand(input: String)
suspend fun getElementByHash(hash: String)
fun isActionable(node: AccessibilityNodeInfo): Boolean
fun calculateNodePath(node: AccessibilityNodeInfo): String

❌ INCORRECT:
fun ScrapeCurrentWindow()        (PascalCase - wrong)
fun scrape_current_window()      (snake_case - wrong)
fun scw()                        (cryptic abbreviation)
fun DoProcessing()               (PascalCase - wrong)
```

### Suspend Functions

**Convention:** Same as regular functions - `camelCase`

**Note:** Kotlin suspend functions use same naming as regular functions

**Examples:**
```
✅ CORRECT:
suspend fun insertElement(element: ScrapedElementEntity)
suspend fun processCommand(voiceInput: String): CommandResult
suspend fun upsertElement(element: ScrapedElementEntity): String

❌ INCORRECT:
suspend fun InsertElement()      (PascalCase)
suspend fun insert_element()     (snake_case)
```

### Boolean Methods

**Convention:** `isXxx`, `hasXxx`, `canXxx`, `shouldXxx`

**Examples:**
```
✅ CORRECT:
fun isActionable(node: AccessibilityNodeInfo): Boolean
fun hasValidHash(): Boolean
fun canExecuteCommand(): Boolean
fun shouldFilterElement(): Boolean

❌ INCORRECT:
fun actionable(): Boolean        (missing prefix)
fun checkValid(): Boolean        (use isValid instead)
fun Actionable(): Boolean        (PascalCase)
```

---

## Variable and Property Names

### Local Variables

**Convention:** `camelCase`

**Rules:**
- Descriptive, not abbreviated
- Noun or noun phrase
- Start with lowercase

**Examples:**
```
✅ CORRECT:
val elementHash = fingerprint.generateHash()
val currentIndex = elements.size
val packageName = node.packageName?.toString()
var scrapedElements = mutableListOf<ScrapedElementEntity>()

❌ INCORRECT:
val ElementHash                  (PascalCase)
val element_hash                 (snake_case)
val eh                          (cryptic abbreviation)
val eHash                       (unclear abbreviation)
```

### Class Properties

**Convention:** `camelCase` (same as local variables)

**Examples:**
```
✅ CORRECT:
class AccessibilityScrapingIntegration(
    private val context: Context,
    private val accessibilityService: AccessibilityService
) {
    private val database: AppScrapingDatabase
    private val commandGenerator: CommandGenerator
    private var lastScrapedAppHash: String? = null
}

❌ INCORRECT:
private val Context: Context                    (PascalCase)
private val accessibility_service               (snake_case)
private val db                                  (cryptic abbreviation - use database)
```

### Backing Fields

**Convention:** Prefix with underscore `_camelCase`

**Note:** Only use when necessary for custom getters/setters

**Examples:**
```
✅ CORRECT:
private var _isInitialized = false
val isInitialized: Boolean
    get() = _isInitialized

private var _elementCount = 0
val elementCount: Int
    get() = _elementCount
```

---

## Constant Names

### Compile-Time Constants

**Convention:** `SCREAMING_SNAKE_CASE`

**Rules:**
- All uppercase
- Separate words with underscores
- Defined at top level or in companion object
- Use `const val` for primitive types

**Examples:**
```
✅ CORRECT:
companion object {
    private const val TAG = "AccessibilityScrapingIntegration"
    private const val MAX_DEPTH = 50
    private const val MIN_MATCH_SCORE = 0.5f
    private const val DATABASE_NAME = "app_scraping.db"
}

private val EXCLUDED_PACKAGES = setOf(
    "com.android.systemui",
    "com.android.launcher"
)

❌ INCORRECT:
companion object {
    private const val tag = "..."               (lowercase)
    private const val maxDepth = 50             (camelCase)
    private const val Max_Depth = 50            (mixed case)
}
```

### Runtime Constants

**Convention:** `camelCase` (regular property naming)

**Note:** Use when value is computed at runtime

**Examples:**
```
✅ CORRECT:
companion object {
    private val standardFolders = listOf(
        "architecture",
        "changelog",
        "developer-manual"
    )
}
```

---

## Resource Files

### Android XML Layout Files

**Convention:** `lowercase_snake_case.xml`

**Rules:**
- All lowercase
- Separate words with underscores
- Prefix with layout type

**Examples:**
```
✅ CORRECT:
activity_main.xml
fragment_settings.xml
item_command_list.xml
dialog_confirm_action.xml

❌ INCORRECT:
ActivityMain.xml                 (PascalCase)
activity-main.xml                (kebab-case)
main.xml                         (missing prefix)
```

### String Resources

**Convention:** `lowercase_snake_case`

**Examples:**
```xml
✅ CORRECT:
<string name="app_name">VoiceOS</string>
<string name="command_not_found">Command not recognized</string>
<string name="error_no_accessibility">Accessibility service not enabled</string>

❌ INCORRECT:
<string name="AppName">                      (PascalCase)
<string name="command-not-found">            (kebab-case)
<string name="err">                          (cryptic abbreviation)
```

### Drawable Resources

**Convention:** `lowercase_snake_case.xml` or `lowercase_snake_case.png`

**Prefixes:**
- `ic_` - Icons
- `bg_` - Backgrounds
- `img_` - Images

**Examples:**
```
✅ CORRECT:
ic_microphone.xml
ic_settings_24dp.xml
bg_rounded_button.xml
img_logo.png

❌ INCORRECT:
microphone.xml                   (missing prefix)
icMicrophone.xml                 (camelCase)
ic-microphone.xml                (kebab-case)
```

---

## Database Entity Names

### Entity Class Names

**Convention:** `[Name]Entity`

**Examples:**
```
✅ CORRECT:
@Entity(tableName = "scraped_elements")
data class ScrapedElementEntity

@Entity(tableName = "generated_commands")
data class GeneratedCommandEntity

❌ INCORRECT:
data class ScrapedElement        (missing Entity suffix)
data class ElementEntity         (ambiguous - what kind?)
```

### Table Names

**Convention:** `lowercase_snake_case`

**Rules:**
- Use plural nouns
- Separate words with underscores
- Descriptive of content

**Examples:**
```
✅ CORRECT:
@Entity(tableName = "scraped_elements")
@Entity(tableName = "generated_commands")
@Entity(tableName = "scraped_hierarchy")

❌ INCORRECT:
@Entity(tableName = "ScrapedElements")       (PascalCase)
@Entity(tableName = "scraped-elements")      (kebab-case)
@Entity(tableName = "element")               (singular)
```

### Column Names

**Convention:** `lowercase_snake_case`

**Examples:**
```
✅ CORRECT:
@ColumnInfo(name = "element_hash")
@ColumnInfo(name = "app_id")
@ColumnInfo(name = "created_at")
@ColumnInfo(name = "is_clickable")

❌ INCORRECT:
@ColumnInfo(name = "elementHash")            (camelCase)
@ColumnInfo(name = "element-hash")           (kebab-case)
@ColumnInfo(name = "ElementHash")            (PascalCase)
```

---

## Quick Reference

### At-a-Glance Naming Table

| Artifact Type | Convention | Example |
|--------------|------------|---------|
| **Kotlin/Java class files** | `PascalCase.kt` | `AccessibilityScrapingIntegration.kt` |
| **Documentation files** | `PascalCase-With-Hyphens-YYMMDD-HHMM.md` | `Architecture-Refactor-Roadmap-251010-0157.md` |
| **Code module folders** | `PascalCase/` | `VoiceAccessibility/` |
| **Doc module folders** | `PascalCase/` (same as code) | `VoiceAccessibility/` |
| **System doc folders** | `kebab-case/` | `voiceos-master/` |
| **Package names** | `lowercase.dot.separated` | `com.augmentalis.voiceaccessibility` |
| **Class names** | `PascalCase` | `VoiceCommandProcessor` |
| **Interface names** | `PascalCase` | `CommandExecutor` |
| **Methods/functions** | `camelCase` | `processVoiceCommand()` |
| **Variables/properties** | `camelCase` | `elementHash` |
| **Constants** | `SCREAMING_SNAKE_CASE` | `MAX_DEPTH` |
| **XML layouts** | `lowercase_snake_case.xml` | `activity_main.xml` |
| **Database tables** | `lowercase_snake_case` | `scraped_elements` |
| **Database columns** | `lowercase_snake_case` | `element_hash` |

---

## Common Violations and Fixes

### Violation 1: Wrong Case for Documentation Files

```
❌ WRONG:
uuid-hash-persistence-architecture.md
architecture_refactor_roadmap.md

✅ CORRECT:
UUID-Hash-Persistence-Architecture-251010-0912.md
Architecture-Refactor-Roadmap-251010-0912.md
```

### Violation 2: Missing Timestamps

```
❌ WRONG:
VOS4-TODO-Master.md
CommandManager-Status.md

✅ CORRECT:
VOS4-TODO-Master-251010-0912.md
CommandManager-Status-251010-0912.md
```

### Violation 3: Wrong Folder Case

```
❌ WRONG:
docs/modules/voice-accessibility/     (kebab-case - WRONG, must match code)
docs/modules/CommandManager/         (kebab-case - WRONG, must match code)
modules/apps/voice-accessibility/     (kebab-case - WRONG for code)

✅ CORRECT:
docs/modules/VoiceAccessibility/      (PascalCase - matches code module)
docs/modules/CommandManager/          (PascalCase - matches code module)
modules/apps/VoiceAccessibility/      (PascalCase - correct for code)
```

**Key Rule:** Documentation module folders MUST match code module names EXACTLY (PascalCase).

### Violation 4: Redundant Naming

```
❌ WRONG (Redundant):
VoiceAccessibilityScrapingIntegrationManager    (too long, redundant)
VoiceAccessibilityService.kt in voiceaccessibility package  (redundant prefix)

✅ CORRECT (Concise):
ScrapingIntegration                             (context clear from package)
VoiceAccessibilityService.kt                    (service name is appropriate)
```

### Violation 5: Cryptic Abbreviations

```
❌ WRONG:
val eh = element.hash
fun pvc(input: String)
class ASI

✅ CORRECT:
val elementHash = element.hash
fun processVoiceCommand(input: String)
class AccessibilityScrapingIntegration
```

---

## Enforcement and Compliance

### Pre-Commit Checks

Before committing code, verify:
1. ✅ All Kotlin/Java files use PascalCase
2. ✅ All documentation files have timestamps
3. ✅ All folders use correct case (code vs documentation)
4. ✅ No redundant naming
5. ✅ No cryptic abbreviations

### Code Review Checklist

Reviewers should check:
- [ ] File names match conventions
- [ ] Class/interface names are clear and follow PascalCase
- [ ] Methods use camelCase and are verb phrases
- [ ] Variables are descriptive, not abbreviated
- [ ] Constants use SCREAMING_SNAKE_CASE
- [ ] Database entities follow naming patterns
- [ ] No naming inconsistencies across related files

### Tools and Scripts

Naming compliance can be verified with:
```bash
# Check for incorrectly named Kotlin files
find modules -name "*.kt" | grep -v '^[A-Z]'

# Check for documentation files without timestamps
find docs -name "*.md" | grep -v '[0-9]\{6\}-[0-9]\{4\}\.md$'

# Check for wrong-case documentation folders
find docs/modules -type d -name '[A-Z]*'
```

---

## References

### Industry Standards

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- [Java Naming Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-namingconventions.html)

### VOS4 Internal Documents

- `/CLAUDE.md` - Agent instructions and project structure
- `/Volumes/M Drive/Coding/Warp/Agent-Instructions/VOS4-DOCUMENTATION-PROTOCOL.md` - Documentation standards
- `/Volumes/M Drive/Coding/Warp/Agent-Instructions/VOS4-CODING-PROTOCOL.md` - Coding standards

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-10-10 | Initial comprehensive naming conventions document |

---

**Last Updated:** 2025-10-10 09:12:42 PDT
**Status:** Official Standard - Mandatory Compliance
**Reviewed By:** VOS4 Development Team
**Next Review:** 2025-11-10
