---
description: Universal analysis .code .ui .workflow .docs .review .pr .swarm | /i.analyze .code src/
---

# /i.analyze - Universal Analysis Command

---

## IDEACODE API Integration

This command uses the IDEACODE API for token efficiency (97% savings).

API Endpoint: `http://localhost:3850/i.analyze`
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
| Output | `.quiet` `.verbose` `.save` |
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

### Output Modifiers

| Modifier | Effect |
|----------|--------|
| `.quiet` | Minimal output (summary only) |
| `.verbose` | Detailed output with all findings |
| `.save` | Force save to file (default: auto-save) |

## Auto-Detection

| Input | Type |
|-------|------|
| `.kt`, `.java`, `.ts`, `.py`, `.go`, `.rs`, `.swift` | CODE |
| `.png`, `.jpg`, `.jpeg`, `.webp`, `.gif` | UI |
| `.md` with spec/plan/tasks | WORKFLOW |
| `.md` other | DOCS |
| Directory | CODE (scan all) |

---

## MANDATORY: Echo Findings + Save Report

### Console Output (Always)

After analysis, ALWAYS display findings on screen:

```
╔══════════════════════════════════════════════════════════════╗
║  📊 ANALYSIS COMPLETE: {target}                              ║
╚══════════════════════════════════════════════════════════════╝

SUMMARY:
  Type: {CODE|UI|DOCS|WORKFLOW}
  Files: {count} analyzed
  Score: {score}/100

FINDINGS:
  ✗ CRITICAL (P0): {count}
    - {issue_1}
    - {issue_2}

  ⚠ HIGH (P1): {count}
    - {issue_1}

  ○ MEDIUM (P2): {count}
    - {issue_1}

METRICS:
  Complexity: {avg} | Max: {max}
  Coverage: {percent}%
  Duplication: {percent}%

RECOMMENDATIONS:
  1. {action_1}
  2. {action_2}

Report saved: {path_to_report}
Run: /i.fix to address issues
Run: /i.review .swarm for expert analysis
```

### Auto-Save Report (Always)

| Input Path | Report Location |
|------------|-----------------|
| `Modules/VoiceOS/...` | `Docs/VoiceOS/Analysis/Analysis-{component}-YYMMDD-V1.md` |
| `Modules/AVA/...` | `Docs/AVA/Analysis/Analysis-{component}-YYMMDD-V1.md` |
| `Modules/WebAvanue/...` | `Docs/WebAvanue/Analysis/Analysis-{component}-YYMMDD-V1.md` |
| `Modules/Cockpit/...` | `Docs/Cockpit/Analysis/Analysis-{component}-YYMMDD-V1.md` |
| `src/...` | `Docs/Project/Analysis/Analysis-{component}-YYMMDD-V1.md` |
| `android/...` | `Docs/Android/Analysis/Analysis-{component}-YYMMDD-V1.md` |
| `ios/...` | `Docs/iOS/Analysis/Analysis-{component}-YYMMDD-V1.md` |

### Report Template

```markdown
# Analysis Report: {component}

**Date:** {YYYY-MM-DD HH:MM}
**Analyzer:** Claude (AI)
**Target:** {file_path}
**Type:** {CODE|UI|DOCS|WORKFLOW}
**Score:** {score}/100

---

## Executive Summary

{one_paragraph_summary}

---

## Analysis Results

### Layer 1: Functional Correctness
- Status: {PASS|FAIL}
- Issues: {list}

### Layer 2: Static Analysis
- Status: {PASS|FAIL}
- Syntax Errors: {count}
- Type Errors: {count}
- Unused Code: {count}

### Layer 3: Runtime Analysis
- Status: {PASS|WARN|FAIL}
- Memory Issues: {list}
- Null Safety: {status}
- Bounds Checks: {status}

### Layer 4: Dependencies
- Status: {PASS|WARN|FAIL}
- Circular Deps: {count}
- Missing Deps: {count}
- Outdated Deps: {count}

### Layer 5: Error Handling
- Status: {PASS|WARN|FAIL}
- Coverage: {percent}%
- Swallowed Exceptions: {count}
- Unhandled Cases: {list}

### Layer 6: Architecture
- Score: {score}/10
- SOLID Compliance: {percent}%
- Coupling: {low|medium|high}
- Violations: {list}

### Layer 7: Performance
- Score: {score}/10
- Avg Complexity: {value}
- Bottlenecks: {list}
- Recommendations: {list}

---

## Findings Summary

| Priority | Count | Description |
|----------|-------|-------------|
| P0 Critical | {count} | {summary} |
| P1 High | {count} | {summary} |
| P2 Medium | {count} | {summary} |

---

## Action Items

### Immediate (P0)
{critical_actions}

### Soon (P1)
{high_priority_actions}

### Later (P2)
{medium_priority_actions}

---

## Metrics

| Metric | Value | Target | Status |
|--------|-------|--------|--------|
| Lines of Code | {value} | - | - |
| Cyclomatic Complexity | {value} | <10 | {status} |
| Test Coverage | {value}% | >80% | {status} |
| Code Duplication | {value}% | <5% | {status} |
| Function Length | {max} | <30 | {status} |

---

## Related

- `/i.fix` - Fix identified issues
- `/i.review .swarm` - Expert analysis
- `/i.refactor` - Improve code quality
```

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

### 7-Layer Framework

| Layer | Focus | Priority |
|-------|-------|----------|
| 1 | Functional Correctness | P0 |
| 2 | Static Analysis (syntax, types, unused) | P0 |
| 3 | Runtime Analysis (memory, null, bounds) | P0 |
| 4 | Dependency Analysis (circular, missing) | P1 |
| 5 | Error Handling (try/catch, propagation) | P1 |
| 6 | Architecture (SOLID, patterns, coupling) | P2 |
| 7 | Performance (complexity, bottlenecks) | P2 |

### Output Format

```
╔══════════════════════════════════════════════════════════════╗
║  📊 CODE ANALYSIS: {target}                                  ║
╚══════════════════════════════════════════════════════════════╝

Layer 1 - Functional:    [✓ PASS]
Layer 2 - Static:        [✓ PASS] 0 errors
Layer 3 - Runtime:       [⚠ WARN] 2 null safety issues
Layer 4 - Dependencies:  [✓ PASS] No circular deps
Layer 5 - Error Handling:[✗ FAIL] 3 swallowed exceptions
Layer 6 - Architecture:  [8/10] SOLID: 85%
Layer 7 - Performance:   [9/10] O(n), no bottlenecks

══════════════════════════════════════════════════════════════

CRITICAL: 0 | HIGH: 3 | MEDIUM: 2

Report saved: Docs/VoiceOS/Analysis/Analysis-AuthManager-260104-V1.md
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
╔══════════════════════════════════════════════════════════════╗
║  🎨 UI ANALYSIS: {screenshot}                                ║
╚══════════════════════════════════════════════════════════════╝

Layout:       {grid/flex}, {hierarchy depth} levels
Components:   {count} identified
  - Buttons: {count}
  - Inputs: {count}
  - Cards: {count}
Colors:       {palette extracted}
Accessibility: [✓ PASS | ⚠ WARN | ✗ FAIL]
  - Contrast: {ratio}
  - Touch targets: {size}

Suggestions:
  1. {improvement_1}
  2. {improvement_2}

Report saved: Docs/{App}/Analysis/UI-Analysis-{screen}-260104-V1.md
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
| Critical bugs | >0 found | `/i.fix .yolo` |
| High debt | >70 score | `/i.refactor` |
| Low docs | <50% coverage | `/i.document` |
| Low tests | <90% coverage | `/i.implement .test` |

---

## Auto-Trigger /i.review

After analysis completes, automatically suggest or run review:

```
╔══════════════════════════════════════════════════════════════╗
║  🔍 ANALYSIS COMPLETE - Review Recommended                   ║
╚══════════════════════════════════════════════════════════════╝

Found {count} issues. Run comprehensive review?

  → /i.review .quick    (critical checks only)
  → /i.review .full     (all 6 categories)
  → /i.review .swarm    (multi-agent expert analysis)

Press Enter to run /i.review .quick or type alternative:
```

---

## Examples

| Command | Behavior |
|---------|----------|
| `/i.analyze src/Auth.kt` | Auto-detect CODE, 7-layer analysis, save report |
| `/i.analyze .ui screenshot.png` | Vision analysis, component extraction, save report |
| `/i.analyze .code src/` | Focus on code quality, 7-layer framework |
| `/i.analyze .ui .app` | UI analysis for current app |
| `/i.analyze .docs` | Analyze documentation coverage and accuracy |
| `/i.analyze .debug .swarm` | Multi-agent with verbose debug logging |
| `/i.analyze .code .cot src/` | Code analysis with CoT reasoning |
| `/i.analyze .workflow spec.md` | Spec completeness check |
| `/i.analyze .swarm src/` | Multi-agent analysis |
| `/i.analyze .quiet src/` | Summary only, minimal output |
| `/i.analyze .verbose src/` | All findings with full details |

---

## Quality Gates

| Metric | Target |
|--------|--------|
| P0 issues | 0 |
| P1 issues | ≤3 |
| Architecture score | ≥7/10 |
| Test coverage | ≥90% |

---

## File Naming Convention

Report files follow the pattern:
```
{Type}-{Component}-{YYMMDD}-V{version}.md
```

Examples:
- `Analysis-AuthManager-260104-V1.md`
- `UI-Analysis-LoginScreen-260104-V1.md`
- `Workflow-Analysis-SpecReview-260104-V1.md`

---

## Related

`/i.fix`, `/i.refactor`, `/i.review`, `/i.document`
