---
description: Universal analysis .code .ui .workflow .docs .review .pr .swarm | /i.analyze .code src/
---

# /i.analyze - Universal Analysis Command

---

## IDEACODE API Integration

This command uses the IDEACODE API for token efficiency (97% savings).

API Endpoint: `http://localhost:3847/i.analyze`
Auto-start: API server starts automatically if not running

---


## Usage
`/analyze [target] [modifiers]`

## Arguments
| Arg | Required | Description |
|-----|----------|-------------|
| target | No | File, folder, module, or screenshot path (auto-detected) |

## Modifiers
| Type | Options |
|------|---------|
| Analysis | `.code` `.ui` `.workflow` `.docs` `.review` `.pr` `.app` |
| Reasoning | `.cot` `.tot` |
| Behavior | `.swarm` `.yolo` `.stop` |
| Debug | `.debug` |

### Scope Modifiers

| Modifier | Focus | Use Case |
|----------|-------|----------|
| `.code` | Code quality analysis | 7-layer framework, SOLID, performance |
| `.ui` | UI/UX analysis | Layout, components, accessibility via Vision |
| `.docs` | Documentation analysis | Coverage, accuracy, examples, freshness |
| `.review` | Code review mode | Quality, patterns, score, recommendations |
| `.pr` | Pull request review | Changes, approval/rejection recommendation |
| `.app` | Full application review | Spec, enhancements, architecture |
| `.debug` | Detailed debug logging | Verbose output, intermediate steps, reasoning traces |

## Auto-Detection
| Input | Type |
|-------|------|
| `.kt`, `.java`, `.ts`, `.py`, `.go`, `.rs`, `.swift` | CODE |
| `.png`, `.jpg`, `.jpeg`, `.webp`, `.gif` | UI |
| `.md` with spec/plan/tasks | WORKFLOW |
| `.md` other | DOCS |
| Directory | CODE (scan all) |

---

## Domain Specialist (MANDATORY)

**Spawn specialist before analysis.**

| Domain | Specialist | Level |
|--------|------------|-------|
| Memory, performance | Systems | OS-Level |
| Threading, concurrency | Concurrency | OS-Level |
| Platform internals | Platform | OS-Level |
| Security, auth, crypto | Security | PhD-Level |
| Algorithm complexity | Algorithm | PhD-Level |
| API, network | Network | Senior |
| Database, queries | Data | Senior |
| UI components | UI | Senior |

---

## CoT/ToT Analysis (MANDATORY)

| Method | When | Approach |
|--------|------|----------|
| **CoT** | Linear flow | Step-by-step reasoning |
| **ToT** | Complex/branching | Multi-hypothesis exploration |

**Required Output:**
```
ANALYSIS:
- Files read: [list]
- Call flow: A → B → C
- Impact scope: [components]
- Findings: [issues]
- Method: CoT | ToT
```

---

## .code - Code Analysis

### Pre-Analysis: MASTER-INDEX Check
**MANDATORY:** Read `/Volumes/M-Drive/Coding/.ideacode/MASTER-INDEX.md` before analysis.

### 8-Layer Framework
| Layer | Focus | Priority |
|-------|-------|----------|
| 1 | Functional Correctness | P0 |
| 2 | Static Analysis (syntax, types, unused) | P0 |
| 3 | Runtime Analysis (memory, null, bounds) | P0 |
| 4 | Dependency Analysis (circular, missing) | P1 |
| 5 | Error Handling (try/catch, propagation) | P1 |
| 6 | Architecture (SOLID, patterns, coupling) | P2 |
| 7 | Performance (complexity, bottlenecks) | P2 |
| 8 | Folder/Naming (KMP conventions) | P0 |

### Layer 8: Folder Structure Validation
| Check | Rule | Action |
|-------|------|--------|
| KMP source sets | Use exact Gradle names | REJECT |
| Package depth | Max 4 levels | WARN |
| Redundant folders | No `classes/`, `helpers/` | REJECT |

**Required KMP Names:** commonMain, commonTest, androidMain, androidUnitTest, androidInstrumentedTest, iosMain, iosTest, desktopMain, desktopTest

### Output Format
```
CODE ANALYSIS: {target}
 Functional: [PASS/FAIL] - {issues}
 Static: [PASS/FAIL] - {issues}
 Runtime: [PASS/FAIL] - {issues}
 Dependencies: [PASS/FAIL] - {circular deps}
 Error Handling: [PASS/FAIL] - {gaps}
 Architecture: [score/10] - {violations}
 Performance: [score/10] - {bottlenecks}

CRITICAL: {count} | HIGH: {count} | MEDIUM: {count}
```

---

## .ui - UI Analysis (Vision)

### Steps
| Step | Action |
|------|--------|
| 1 | Load screenshot via Claude Vision |
| 2 | Extract layout structure (hierarchy, grid) |
| 3 | Identify components (buttons, inputs, cards) |
| 4 | Analyze styling (colors, spacing, typography) |
| 5 | Check accessibility (contrast, touch targets) |
| 6 | Suggest improvements |
| 7 | Generate code (optional) |

### Analysis Types
| Type | Focus |
|------|-------|
| `layout` | Structure, hierarchy, spacing |
| `components` | Component identification, patterns |
| `accessibility` | WCAG compliance, contrast, targets |
| `full` | All of the above |

### Output Format
```
UI ANALYSIS: {screenshot}
 Layout: {grid/flex}, {hierarchy depth}
 Components: {count} identified
 Colors: {palette extracted}
 Accessibility: [PASS/WARN/FAIL]
 Suggestions: {improvements}
```

---

## .workflow - Workflow Analysis

Analyzes spec/plan/tasks for:
| Check | Description |
|-------|-------------|
| Completeness | All phases defined? |
| Consistency | No contradictions? |
| Dependencies | Correct ordering? |
| Gaps | Missing requirements? |

---

## .docs - Documentation Analysis

| Check | Description |
|-------|-------------|
| Coverage | All public APIs documented? |
| Accuracy | Docs match implementation? |
| Examples | Working code examples? |
| Freshness | Last updated date? |

---

## Intelligence Chaining

Post-analysis auto-detection:

| Finding | Trigger | Suggestion |
|---------|---------|------------|
| Critical bugs | >0 found | `/fix .yolo` |
| High debt | >70 score | `/refactor` |
| Low docs | <50% coverage | `/document` |
| Low tests | <90% coverage | `/implement .test` |

---

## Examples
| Command | Behavior |
|---------|----------|
| `/analyze src/Auth.kt` | Auto-detect CODE, 7-layer analysis |
| `/analyze .ui screenshot.png` | Vision analysis, component extraction |
| `/analyze .code src/` | Focus on code quality, 7-layer framework |
| `/analyze .ui .app` | UI analysis for current app |
| `/analyze .docs` | Analyze documentation coverage and accuracy |
| `/analyze .debug .swarm` | Multi-agent with verbose debug logging |
| `/analyze .code .cot src/` | Code analysis with CoT reasoning |
| `/analyze .workflow spec.md` | Spec completeness check |
| `/analyze .swarm src/` | Multi-agent analysis |

---

## Quality Gates
| Metric | Target |
|--------|--------|
| P0 issues | 0 |
| P1 issues | ≤3 |
| Architecture score | ≥7/10 |
| Test coverage | ≥90% |

## Related
`/ifix`, `/refactor`, `/review`, `/document`
