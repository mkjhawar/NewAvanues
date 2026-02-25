# Wave 1 — A4 Master Analysis
## Modules: AVID | AVU | AVACode
**Date:** 260222 | **Branch:** VoiceOS-1M-SpeechEngine

---

## Module: AVID
**Path:** `Modules/AVID/`

### PURPOSE
Generates stable, unique voice identifiers (AVIDs) for UI elements, screens, apps, and data entities across all Avanue platform targets. Two ID families co-exist: (1) compact deterministic fingerprints (`{reversedPkg}:{version}:{type}:{hash8}`) for UI element voice targeting via `Fingerprint` + `AvidGenerator`, and (2) sequential global/local IDs (`AVID-A-000001` / `AVIDL-A-000047`) for data entities synced across devices via `AvidGlobalID` / `AvidLocalID`.

### WHY
Voice commands must target the same UI element consistently across app launches and devices. A deterministic fingerprint (based on packageName + elementType + resourceId + name + contentDesc) ensures `"click Save"` always resolves to the same AVID regardless of which device generated it. Sequential IDs support offline creation (AVIDL) with promotion to server-assigned global IDs (AVID) on sync.

### DEPS
- `kotlinx-atomicfu` — thread-safe sequence counters in `AvidGlobalID` / `AvidLocalID`
- `platform.Foundation.NSDate` — iOS implementation of `currentTimeMillis()` (iosMain only)
- No other module dependencies — intentionally leaf-level

### CONSUMERS
- `Modules/VoiceOSCore` — `ElementFingerprint`, `OverlayItemGenerator`, all AVID assignment at scraping time
- `Modules/Cockpit` — frame/window IDs
- `Modules/AVU` — AVID strings appear in AVU wire messages as element identifiers
- `Modules/AVACode` — future form/workflow entity identification
- `Apps/Android/VoiceOS` — `VoiceOSAccessibilityService` calls `Fingerprint.forElement()`

### KMP
- **commonMain**: `Fingerprint`, `AvidGlobalID`, `AvidLocalID`, `TypeCode`, `Platform`, `AvidGenerator`
- **androidMain**: `AvidGeneratorAndroid.kt` (actual `currentTimeMillis()` → `System.currentTimeMillis()`)
- **iosMain**: `AvidGeneratorIos.kt` (actual `currentTimeMillis()` → `NSDate().timeIntervalSince1970`)
- **desktopMain**: `AvidGeneratorDesktop.kt` (actual `currentTimeMillis()` → `System.currentTimeMillis()`)
- **KMP Score: 89%** — only `currentTimeMillis()` is platform-specific

### KEY_CLASSES
| Class | Role |
|-------|------|
| `Fingerprint` | Deterministic polynomial hash generator (8-char for elements, 12-char for apps) |
| `AvidGenerator` | Compact format generation + validation + parsing + legacy migration |
| `AvidGlobalID` | Sequential synced IDs (AVID-A-000001) with atomic counter |
| `AvidLocalID` | Offline-creation pending-sync IDs (AVIDL-A-000047) with promotion API |
| `TypeCode` | Canonical 3-char type constants (BTN, INP, SCR, …) |
| `Platform` | Platform enum with single-char wire codes (A/I/W/M/X/L) |

### HEALTH
**YELLOW** | Score: 72/100

**Open bugs:**
- P1: `generateCompact()` uses random hash (not deterministic) when no `elementHash` supplied — destroys fingerprint stability for VOS command storage
- P1: `AvidGlobalID`/`AvidLocalID` default to `Platform.ANDROID` with no enforcement — iOS/desktop IDs silently tagged wrong if `setPlatform()` not called
- P1: `generateFromContent()` uses JVM `String.hashCode()` — not deterministic on K/N

**Review doc:** `docs/reviews/AVID-AVU-AVACode-Review-QualityAnalysis-260222-V1.md`

---

## Module: AVU
**Path:** `Modules/AVU/`

### PURPOSE
Avanues Universal (AVU) is the wire protocol, DSL, and runtime execution layer for voice command automation. It operates at three levels: (1) **Wire codec** — pipe-delimited `CODE:field1:field2` messages with percent-encoded escaping (`AVUEncoder`/`AVUDecoder`); (2) **DSL** — declarative `.avp`/`.vos` text files with a Python-style indented syntax parsed by `AvuDslLexer` + `AvuDslParser` into an AST; (3) **Interpreter** — `AvuInterpreter` tree-walks the AST with sandbox enforcement and dispatches code invocations through `IAvuDispatcher`. Plugin files (`.avp`) extend the runtime through a permission-gated loading pipeline in `PluginLoader`.

### WHY
Separating the voice automation language from the execution platform allows workflows to be written once as text files and run on Android, iOS, or desktop without recompilation. The wire codec provides compact binary-efficient IPC between the VoiceOS service and client apps. The self-documenting header system (`AvuHeader`, `AvuCodeRegistry`) makes `.hov` handover files and `.acd` app category databases machine-readable by any parser without external schema.

### DEPS
- `kotlinx-coroutines-core` — `delay()` in `AvuInterpreter`, `suspend fun dispatch()` in `IAvuDispatcher`
- No database or UI dependencies — pure logic layer
- `AvuDslLexer` → `AvuDslParser` → `AvuInterpreter` → `IAvuDispatcher` (platform-specific)

### CONSUMERS
- `Modules/VoiceOSCore` — `VoiceOSRpcServer` encodes/decodes `VCM`/`ACC`/`DEC` IPC messages via `AVUEncoder`/`AVUDecoder`; `PluginManager` loads `.avp` files via `PluginLoader`; `MacroDslMigrator` converts old MacroDSL to AVU DSL
- `Modules/AVACode` — `WorkflowDefinition` uses `MigrationStep` types that mirror `AvuAstNode.Statement` patterns
- `Apps/Android/VoiceOS` — `VoiceOSIpcService` sends `VCM:...` messages that AVU encodes
- `Apps/Android/WebAvanue` — `DOMScraperBridge` receives `NAV:`/`URL:` messages decoded by `AVUDecoder`

### KMP
- **commonMain**: All codec, DSL lexer/parser/AST/interpreter/tooling/plugin/migration/registry
- **androidMain**: `Platform.android.kt` (empty placeholder)
- **iosMain**: `Platform.ios.kt` (empty placeholder)
- **desktopMain**: `Platform.desktop.kt` + `DesktopAvuDispatcher`
- **iosMain**: `IosAvuDispatcher`
- **commonTest**: Full interpreter + tooling + plugin + migration test suite (13 test files)
- **KMP Score: 95%** — near-100% shared; platform files are thin dispatcher stubs

### KEY_CLASSES
| Class | Role |
|-------|------|
| `AVUEncoder` | Encodes all message types to `CODE:field:field` wire format; plugin manifests; ACD databases; handover files |
| `AVUDecoder` | Parses all message types from wire format; full plugin manifest + ACD + handover parsing |
| `AvuEscape` | Single source of truth for percent-encoding (`%`, `:`, `\n`, `\r`) — correct order enforced |
| `AvuHeader` | Self-documenting file header parser/generator for `.avu`/`.vos`/`.hov` files |
| `AvuCodeRegistry` | Central registry of all 3-letter codes with metadata for legend generation + validation |
| `AvuDslLexer` | Tokenizer with Python-style INDENT/DEDENT and header section passthrough |
| `AvuDslParser` | Recursive descent parser producing `AvuDslFile` AST with error recovery |
| `AvuInterpreter` | Tree-walking interpreter with sandbox enforcement (steps/time/nesting/loops/vars) |
| `ExpressionEvaluator` | Expression evaluator with JS-style type coercion; built-in dispatch via QRY code |
| `ExecutionContext` | Per-run state: scope stack, function registry, event bus, sandbox counters |
| `SandboxConfig` | Resource limit configuration (DEFAULT / STRICT / SYSTEM / TESTING) |
| `IAvuDispatcher` | Platform abstraction for code invocation; `CompositeDispatcher` for chain-of-responsibility |
| `PluginLoader` | 6-step `.avp` validation pipeline: parse → type check → manifest → permission check → sandbox |
| `MacroDslMigrator` | Converts old compiled MacroDSL to declarative AVU DSL `.vos` text |
| `AvuV1Compat` | Parses legacy v1 wire messages for backward compatibility and migration preview |

### HEALTH
**YELLOW** | Score: 74/100

**Open bugs:**
- P0/Critical: `CODE_ACCEPT_DATA = "ACD"` collides with `CODE_APP_CATEGORY_DB = "ACD"` — wire protocol ambiguity (known since 260220, unresolved)
- P0/High: `AVUDecoder.ParsedMessage.param()` double-unescapes already-decoded values — fields containing literal `%` are corrupted
- P1/High: Lexer `canMatchAt(current, '-')` bug — 2-dash `--` triggers header separator mode (should be `current + 1`)
- P1/High: `and`/`or` evaluator comment says "non-short-circuit" but uses Kotlin `&&`/`||` which are short-circuit — contract mismatch
- P1/High: `@Synchronized` on `AvuCodeRegistry` is no-op on K/N — not thread-safe on iOS/desktop

**Review doc:** `docs/reviews/AVID-AVU-AVACode-Review-QualityAnalysis-260222-V1.md`

---

## Module: AVACode
**Path:** `Modules/AVACode/`

### PURPOSE
Provides a Kotlin DSL for generating structured forms with validation, database schema, two-way data binding, and multi-step workflow orchestration. Forms (`FormDefinition`) are described with typed fields (`TextFieldBuilder`, `EmailFieldBuilder`, etc.) and validation rules (`ValidationRule`); these auto-generate SQL DDL via `DatabaseSchema`. Workflows (`WorkflowDefinition`) sequence forms into multi-step processes with navigation, skip conditions, and lifecycle callbacks.

### WHY
Enables voice-driven data entry ("fill in the registration form", "next step") by providing a strongly-typed, self-describing form model that the UI layer can render and voice can navigate. The database schema generation ensures form fields and database columns stay in sync without manual DDL. Workflow orchestration supports complex onboarding/data-collection flows with conditional step display.

### DEPS
- No external library dependencies — pure Kotlin stdlib
- `java.io.File` in `FileIO.jvm.kt` (jvmMain only)
- `System.currentTimeMillis()` in `WorkflowInstance` + `WorkflowPersistence` — **JVM only, currently in commonMain** (bug)

### CONSUMERS
- **Planned consumers** (not yet active): `Modules/Cockpit` (form frames), `Apps/Android/VoiceOS` (multi-step voice onboarding)
- `jvmMain` `FileIO` is consumed by the AVA CLI toolchain for reading/writing form definition files

### KMP
- **commonMain**: All forms and workflow logic (10 files)
- **jvmMain**: `FileIO.jvm.kt` (actual for CLI file I/O)
- **KMP Score: 90%** — commonMain is dominant; however `System.currentTimeMillis()` usage in commonMain is a latent KMP violation that will block iOS/desktop compilation
- **Note**: The `expect object FileIO` counterpart in commonMain was not found in the file listing — verify it exists or the `actual` keyword in `FileIO.jvm.kt` is erroneous

### KEY_CLASSES
| Class | Role |
|-------|------|
| `FormDefinition` | Immutable form descriptor with field list, metadata, validation, completion tracking |
| `FieldDefinition` | Single field with type, validation rules, and database column config |
| `ValidationRule` | Sealed class hierarchy: Required, MinLength, MaxLength, Pattern, Email, Min, Max, Range, InList, MinDate, MaxDate, password rules, Custom |
| `DatabaseSchema` | Generates CREATE TABLE + CREATE INDEX SQL DDL from `FormDefinition` |
| `FormBinding` | Mutable two-way binding: get/set fields, change listeners, validation listeners, commit/reset |
| `WorkflowDefinition` | Immutable workflow with ordered step list; creates `WorkflowInstance` |
| `StepDefinition` | Single workflow step: optional form, show/skip conditions, lifecycle callbacks |
| `WorkflowInstance` | Mutable workflow runtime state: navigation (next/back/skip/jumpTo), progress, history |
| `WorkflowPersistence` | Serialize/deserialize `WorkflowInstance` to/from key-value map; checkpoint/restore |
| `InMemoryWorkflowStorage` | In-memory `WorkflowStorage` implementation for testing |
| `SQLDialect` | Enum for SQLite/MySQL/PostgreSQL/H2 DDL dialect switching |

### HEALTH
**GREEN** | Score: 80/100

**Open bugs:**
- P1/High: `WorkflowInstance` is `data class` with `MutableMap`/`MutableList` — `copy()` shares mutable references; `next()`/`back()` mutate before returning copy — consumers observing old instance see silent mutations
- P1/High: `System.currentTimeMillis()` in commonMain blocks KMP iOS/desktop targets
- P2/Medium: `FormBinding.commit()` relies on `Map.toMap()` returning a mutable `LinkedHashMap` — impl-detail cast `(initialData as? MutableMap)?.clear()` will silently become no-op if stdlib changes

**Review doc:** `docs/reviews/AVID-AVU-AVACode-Review-QualityAnalysis-260222-V1.md`
