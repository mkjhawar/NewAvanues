# Plan: Hybrid Persistence + Import/Export
Feature: ScrollView Commands | Module: VoiceOSCore + voiceoscoreng | V2 | 260122

## Meta
platforms: KMP(commonMain), Android
tasks: 28 (18 hybrid + 10 import/export)
risk: medium
deps: P5 requires P4 complete

## Phase Order (CoT)
P1:Core → P2:Android → P3:Wire → P4:Test → P5:Export
- P1 first: enums/classes foundation
- P2 second: PackageManager Android-specific
- P3 third: connect to existing flow
- P4 fourth: validate before export
- P5 last: needs final DB schema

## Approach (ToT)
❌ A:BigBang - high risk, no incremental value
✅ B:Phased - incremental, testable, easy rollback
🔄 C:QuickFix - backup if time-critical

## P1: Core Infrastructure
loc: VoiceOSCore/commonMain/.../voiceoscore/

| # | task | file | what |
|---|------|------|------|
| 1.1 | AppCategory enum | AppCategoryClassifier.kt | EMAIL,MESSAGING,SETTINGS+DynamicBehavior |
| 1.2 | package matcher | ^ | pattern-based (gmail,whatsapp,settings) |
| 1.3 | ContainerBehavior | ContainerClassifier.kt | ALWAYS_DYNAMIC,CONDITIONALLY,STATIC |
| 1.4 | container logic | ^ | RecyclerView vs ScrollView split |
| 1.5 | ContentSignal | ContentAnalyzer.kt | TextLength,hasResourceId,dynamicPatterns,stabilityScore |
| 1.6 | content analysis | ^ | dynamic/static patterns, scoring |
| 1.7 | ScreenType | ScreenClassifier.kt | SETTINGS,LIST,FORM,etc |
| 1.8 | screen logic | ^ | element stats → screen type |
| 1.9 | DecisionEngine | PersistenceDecisionEngine.kt | 6-rule matrix |

## P2: Android Integration
loc: voiceoscoreng/.../service/

| # | task | file | what |
|---|------|------|------|
| 2.1 | IAppCategoryProvider | IAppCategoryProvider.kt(KMP) | platform interface |
| 2.2 | AndroidProvider | AndroidAppCategoryProvider.kt | PackageManager.getAppInfo().category |
| 2.3 | inject provider | DynamicCommandGenerator.kt | constructor param |

## P3: Wiring

| # | task | file | what |
|---|------|------|------|
| 3.1 | pass category | ElementExtractor.kt | category→extractElements() |
| 3.2 | replace isDynamic | ElementInfo.kt | deprecate getter, new decision method |
| 3.3 | use engine | CommandGenerator.kt | DecisionEngine.shouldPersist() |
| 3.4 | pass context | CommandOrchestrator.kt | screen context→engine |
| 3.5 | wire service | VoiceOSAccessibilityService.kt | create provider, pass to generator |

## P4: Testing

| # | task | file | what |
|---|------|------|------|
| 4.1-4.4 | unit tests | *Test.kt | AppCategory,Container,Content,DecisionEngine |
| 4.5-4.7 | manual | - | RealWear Settings(✓persist), Gmail inbox(✗), Gmail menu(✓) |

## P5: Import/Export
prereq: P4 complete (need final DB schema)

**KMP Core** (loc: VoiceOSCore/commonMain)
| # | file | what |
|---|------|------|
| 5.1 | - | analyze DB: scraped_app,scraped_element,scraped_command,app_category_cache |
| 5.2 | CommandExportModels.kt | ExportManifest,AppExportData,CommandExportData |
| 5.3 | ICommandExporter.kt | export contract |
| 5.4 | ICommandImporter.kt | import contract |
| 5.5 | CommandExporter.kt | all/selective→JSON |
| 5.6 | CommandImporter.kt | merge/replace strategies |
| 5.7 | ExportSerializer.kt | JSON+versioning |

**Android UI** (loc: voiceoscoreng)
| # | file | what |
|---|------|------|
| 5.8 | AndroidExportFileProvider.kt | SAF file access |
| 5.9 | ExportSettingsActivity.kt | app selection, export options |
| 5.10 | ImportSettingsActivity.kt | file picker, preview, import |

export: all|single|multi apps, JSON+meta
import: merge|replace|preview

## Files Summary
**New (19):** P1-4: 9 files (5 classifiers + interface + provider + 2 tests), P5: 10 files (7 KMP + 3 Android)
**Modified (5):** ElementInfo.kt, CommandGenerator.kt, CommandOrchestrator.kt, ElementExtractor.kt, DynamicCommandGenerator.kt

## Decision Rules
```kotlin
// R1: ALWAYS_DYNAMIC → never persist
// R2: SETTINGS|SYSTEM apps → persist unless dynamicPatterns
// R3: SETTINGS_SCREEN → persist unless dynamicPatterns
// R4: FORM_SCREEN → persist if textLength!=LONG
// R5: EMAIL|MESSAGING|SOCIAL → (SHORT+resourceId) || stability>70
// R6: unknown → stability>60 && !dynamicPatterns
```

## Risks
| risk | L | I | mitigation |
|------|---|---|------------|
| wrong classification | M | M | test coverage |
| perf regression | L | M | early-exit RecyclerView |
| breaking existing | M | H | deprecate, gradual migrate |
| PackageManager API | L | L | fallback to patterns |

## Success Criteria
| scenario | expect | verify |
|----------|--------|--------|
| RealWear Settings | persist | COUNT>0 |
| Gmail Inbox | skip | COUNT=0 emails |
| Gmail Menu | persist | COUNT>0 menu |
| Unknown+ScrollView | static→persist | stability>60 |
| RecyclerView any | skip | R1 early exit |

## Deps Flow
P1+P2.1 parallel → P2.2-3 → P3 → P4 → P5
P5 blocked: needs final schema from P4

## Rollback
1. revert ElementInfo.isDynamicContent
2. remove engine calls
3. keep classifier files (harmless unused)
