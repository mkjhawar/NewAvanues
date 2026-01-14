# IDEACODE v9.0 - Intelligent Commands Framework

**Version:** 9.0
**Date:** 2025-11-27
**Purpose:** Universal intelligence layer for context-aware command execution

---

## 🧠 Core Concept

**Every IDEACODE command should be intelligent enough to:**
1. Understand the user's actual intent (not just literal request)
2. Detect the optimal workflow for the task
3. Auto-chain to related commands when beneficial
4. Adapt complexity based on context
5. Prevent common mistakes and anti-patterns

---

## ⚙️ Configurable Intelligence Thresholds

**All intelligence thresholds are configurable via project constitution or session rules.**

### Default Thresholds

```typescript
const INTELLIGENCE_THRESHOLDS = {
  // Complexity scoring (0-100)
  complexity: {
    trivial: 20,        // <20: Trivial change (one line)
    simple: 40,         // 20-40: Simple (single file)
    moderate: 60,       // 40-60: Moderate (multiple files)
    complex: 80,        // 60-80: Complex (multiple layers)
    architectural: 80   // 80+: Architectural (system-wide)
  },

  // Quality metrics (0-100)
  quality: {
    technicalDebt: 70,       // 70+: Trigger refactor recommendation
    testCoverage: 90,        // <90%: Suggest adding tests
    documentationCoverage: 70 // <70%: Suggest documentation
  },

  // Intent detection confidence (0-1.0)
  intent: {
    autoRedirect: 0.90,   // 90%+ confidence: Auto-redirect
    suggest: 0.70,        // 70%+ confidence: Suggest alternative
    warn: 0.50,           // 50%+ confidence: Warn user
    silent: 0.00          // <50% confidence: Execute as requested
  },

  // Issue severity thresholds
  issues: {
    criticalCount: 1,     // 1+ critical: Urgent action
    warningCount: 5,      // 5+ warnings: Suggest fixes
    documentationGaps: 5  // 5+ gaps: Suggest documentation
  }
};
```

### Configuration Methods

**1. Global Configuration (applies to all projects):**
```bash
# Via constitution rules
ideacode_constitution_set \
  --session_id global \
  --project "*" \
  --rules '[{
    "id": "intelligence.technicalDebt",
    "description": "Technical debt threshold for refactoring",
    "priority": 5,
    "enforced": false,
    "category": "quality",
    "value": 70
  }]'
```

**2. Project-Specific Configuration:**
```yaml
# .ideacode/config.yml
intelligence:
  thresholds:
    technicalDebt: 60        # Stricter for this project
    testCoverage: 95         # Higher standard
    documentationCoverage: 80
```

**3. Session-Specific Override:**
```bash
# For current session only
ideacode_constitution_add_rule \
  --session_id {current} \
  --project "MyProject" \
  --rule_id "intelligence.technicalDebt" \
  --description "Temporary: Lower debt threshold for legacy cleanup" \
  --priority 10 \
  --category "quality" \
  --value 50
```

### Threshold Rationale

| Threshold | Default | Rationale | Research Basis |
|-----------|---------|-----------|----------------|
| Technical Debt | 70% | Industry tipping point where costs escalate | SonarQube, CodeClimate standards |
| Test Coverage | 90% | Google/Microsoft standard for critical paths | Industry best practices |
| Documentation | 70% | Balance between completeness and effort | Microsoft documentation standards |
| Auto-Redirect | 90% | High confidence needed to override user | UX research on automation trust |

### Override Examples

**Strict Quality Team:**
```yaml
intelligence:
  thresholds:
    technicalDebt: 50      # More aggressive
    testCoverage: 95       # Higher bar
    documentationCoverage: 80
```

**Legacy Codebase:**
```yaml
intelligence:
  thresholds:
    technicalDebt: 85      # Accept more debt initially
    testCoverage: 70       # Lower bar during migration
    documentationCoverage: 50
```

**Prototype/Experiment:**
```yaml
intelligence:
  thresholds:
    technicalDebt: 90      # Very permissive
    testCoverage: 50       # Minimal testing
    documentationCoverage: 30
```

---

## 📋 Universal Intelligence Layer

### 1. Context Detection

```typescript
interface CommandContext {
  // What user explicitly requested
  explicit: {
    command: string;
    description: string;
    modifiers: string[];
    target?: string;
  };

  // What user actually needs (inferred)
  inferred: {
    realIntent: 'fix' | 'feature' | 'refactor' | 'document' | 'analyze';
    complexity: 'trivial' | 'simple' | 'moderate' | 'complex' | 'architectural';
    scope: 'file' | 'folder' | 'module' | 'project';
    urgency: 'critical' | 'high' | 'medium' | 'low';
  };

  // Current project state
  project: {
    hasUncommittedChanges: boolean;
    hasFailingTests: boolean;
    technicalDebt: number; // 0-100
    documentationCoverage: number; // 0-100
    testCoverage: number; // 0-100
    recentActivity: 'active' | 'moderate' | 'stale';
  };

  // Related context
  related: {
    specs: string[];
    plans: string[];
    tasks: string[];
    todos: string[];
    issues: string[];
    recentCommits: string[];
  };
}
```

---

### 2. Intent Detection

```typescript
function detectIntent(userInput: string, context: CommandContext): Intent {
  // Analyze language patterns
  const patterns = {
    fix: ['fix', 'broken', 'crash', 'error', 'bug', 'not working', 'fails'],
    feature: ['add', 'implement', 'create', 'build', 'new', 'support for'],
    refactor: ['clean', 'reorganize', 'improve structure', 'technical debt'],
    document: ['document', 'explain', 'write docs', 'update docs'],
    analyze: ['analyze', 'review', 'check', 'investigate', 'understand']
  };

  // Check for mismatch between command and intent
  if (command === '/fix' && containsKeywords(userInput, patterns.feature)) {
    return {
      type: 'MISMATCH',
      detectedIntent: 'feature',
      suggestion: '/develop',
      confidence: 0.9
    };
  }

  // Detect disguised requests
  if (command === '/analyze' && containsKeywords(userInput, patterns.fix)) {
    return {
      type: 'IMPLICIT_FIX',
      detectedIntent: 'fix',
      suggestion: 'Auto-chain to /fix after analysis',
      confidence: 0.8
    };
  }

  return {
    type: 'ALIGNED',
    detectedIntent: inferredFromCommand(command),
    confidence: 1.0
  };
}
```

---

### 3. Complexity Detection

```typescript
function detectComplexity(task: string, context: CommandContext): Complexity {
  let score = 0;

  // Analyze task description
  if (task.includes('multiple') || task.includes('several')) score += 20;
  if (task.includes('system') || task.includes('architecture')) score += 30;
  if (task.includes('across') || task.includes('throughout')) score += 25;

  // Analyze codebase impact
  const affectedFiles = estimateAffectedFiles(task, context);
  if (affectedFiles > 10) score += 30;
  else if (affectedFiles > 5) score += 20;
  else if (affectedFiles > 2) score += 10;

  // Analyze layers
  const layers = estimateAffectedLayers(task, context);
  score += layers * 10;

  // Analyze dependencies
  const dependencies = estimateDependencies(task, context);
  score += dependencies * 5;

  // Map to complexity level
  if (score < 20) return 'trivial';   // One line change
  if (score < 40) return 'simple';    // Single file, clear solution
  if (score < 60) return 'moderate';  // Multiple files, straightforward
  if (score < 80) return 'complex';   // Multiple layers, needs planning
  return 'architectural';             // System-wide, needs design

}
```

---

### 4. Workflow Selection

```typescript
interface WorkflowDecision {
  primary: string;        // Main command to execute
  chain?: string[];       // Auto-chain these commands
  skip?: string[];        // Skip these steps (optimization)
  reasoning: string;      // Why this workflow
}

function selectWorkflow(
  command: string,
  intent: Intent,
  complexity: Complexity,
  context: CommandContext
): WorkflowDecision {

  // Example: /fix command
  if (command === '/fix') {
    // Detected as feature, not bug
    if (intent.type === 'MISMATCH' && intent.detectedIntent === 'feature') {
      return {
        primary: '/develop',
        reasoning: 'Detected feature request disguised as fix',
        chain: ['/document .dev']
      };
    }

    // Simple one-shot fix
    if (complexity === 'trivial' || complexity === 'simple') {
      return {
        primary: '/fix',
        skip: ['/plan', '/tasks'],
        reasoning: 'Simple fix, no planning needed',
        chain: context.project.documentationCoverage < 70 ? ['/document .api'] : []
      };
    }

    // Complex fix needs planning
    if (complexity === 'complex' || complexity === 'architectural') {
      return {
        primary: '/fix',
        chain: ['/plan', '/tasks', '/implement', '/document .dev'],
        reasoning: 'Complex fix requires planning and documentation'
      };
    }

    // Moderate - multiple small fixes
    return {
      primary: '/fix',
      chain: ['/plan', '/implement'],
      skip: ['/specify'],
      reasoning: 'Multiple fixes need plan but not full spec'
    };
  }

  // Example: /develop command
  if (command === '/develop') {
    // Actually just a simple change
    if (complexity === 'trivial') {
      return {
        primary: '/fix',
        reasoning: 'Change too simple for full development workflow',
        skip: ['/specify', '/plan']
      };
    }

    // Full feature development
    if (complexity === 'complex' || complexity === 'architectural') {
      return {
        primary: '/develop',
        chain: ['/specify', '/plan', '/implement', '/review', '/document .all'],
        reasoning: 'Full feature needs complete workflow'
      };
    }

    // Moderate feature
    return {
      primary: '/develop',
      chain: ['/plan', '/implement', '/document .dev'],
      skip: ['/specify'],
      reasoning: 'Feature needs implementation but spec can be lightweight'
    };
  }

  // Default: execute as requested
  return {
    primary: command,
    reasoning: 'No intelligence override needed'
  };
}
```

---

## 🎯 Command-Specific Intelligence

### /develop - Development Workflow

**Intelligence:**
```typescript
class DevelopIntelligence {
  analyze(input: string, context: CommandContext) {
    // Too simple? Just fix it
    if (isTooSimpleForFullWorkflow(input, context)) {
      return {
        redirect: '/fix',
        reasoning: 'This is a simple change, not a full feature'
      };
    }

    // Already has spec? Skip to implementation
    if (hasExistingSpec(input, context)) {
      return {
        skip: ['/specify'],
        startAt: '/plan',
        reasoning: 'Spec already exists'
      };
    }

    // High urgency? Skip heavy planning
    if (context.inferred.urgency === 'critical') {
      return {
        skip: ['/clarify'],
        lightweight: true,
        reasoning: 'Critical urgency, streamline workflow'
      };
    }

    // Prototype mode? Skip tests initially
    if (hasModifier(context, 'prototype')) {
      return {
        skip: ['/test'],
        defer: ['/test'],
        reasoning: 'Prototype mode, defer testing'
      };
    }

    return { executeNormal: true };
  }
}
```

**Examples:**
```bash
# Input: /develop "fix typo in button text"
# Intelligence: Too simple for full workflow
# → Redirect to: /fix .yolo "typo in button text"

# Input: /develop "add OAuth integration"
# Intelligence: Complex feature, full workflow needed
# → Execute: /specify → /plan → /implement → /document

# Input: /develop "implement login screen"
# Intelligence: Existing spec found (specs/login-screen.md)
# → Skip /specify, start at /plan
```

---

### /fix - Bug Fixing

**Intelligence:**
```typescript
class FixIntelligence {
  analyze(input: string, context: CommandContext) {
    // Feature detection
    if (isActuallyFeature(input)) {
      return {
        redirect: '/develop',
        reasoning: 'Adding new functionality, not fixing existing'
      };
    }

    // Simple one-shot?
    if (isSingleIssue(input, context)) {
      return {
        workflow: 'direct',
        skip: ['/plan', '/tasks'],
        reasoning: 'Single issue, fix directly'
      };
    }

    // Multiple related issues?
    const relatedIssues = findRelatedIssues(input, context);
    if (relatedIssues.length > 2) {
      return {
        workflow: 'planned',
        chain: ['/plan', '/tasks', '/implement'],
        reasoning: `${relatedIssues.length} related issues found`
      };
    }

    // Root cause unclear?
    if (requiresInvestigation(input, context)) {
      return {
        workflow: 'investigative',
        chain: ['/debug', '/analyze'],
        reasoning: 'Root cause unclear, investigation needed'
      };
    }

    return { workflow: 'standard' };
  }
}
```

**Examples:**
```bash
# Input: /fix "add OAuth to fix login"
# Intelligence: "add OAuth" = feature, not fix
# → Redirect to: /develop "OAuth integration"

# Input: /fix "button not working"
# Intelligence: Single issue, clear fix
# → Execute: Direct fix (no planning)

# Input: /fix "authentication system issues"
# Intelligence: Multiple related issues detected
# → Execute: /plan → /tasks → /implement

# Input: /fix "random crashes"
# Intelligence: Root cause unclear
# → Execute: /debug → /analyze → /fix
```

---

### /analyze - Analysis

**Intelligence:**
```typescript
class AnalyzeIntelligence {
  analyze(input: string, context: CommandContext) {
    // Analyze for what purpose?
    const purpose = detectAnalysisPurpose(input, context);

    if (purpose === 'find-bugs') {
      return {
        chain: ['/fix'],
        reasoning: 'Analysis to find bugs, auto-chain to fix'
      };
    }

    if (purpose === 'code-review') {
      return {
        chain: ['/review'],
        reasoning: 'Analysis for quality, chain to review'
      };
    }

    if (purpose === 'refactoring') {
      return {
        chain: ['/refactor'],
        reasoning: 'Analysis to improve code, chain to refactor'
      };
    }

    if (purpose === 'understanding') {
      return {
        chain: ['/document .dev'],
        reasoning: 'Analysis for understanding, document findings'
      };
    }

    // Detected issues during analysis?
    const onComplete = (results) => {
      if (results.criticalIssues > 0) {
        return { autoChain: '/fix .yolo' };
      }
      if (results.technicalDebt > 70) {
        return { autoChain: '/refactor' };
      }
      if (results.documentationGaps.length > 5) {
        return { autoChain: '/document .dev' };
      }
    };

    return { executeNormal: true, onComplete };
  }
}
```

**Examples:**
```bash
# Input: /analyze . "looking for bugs"
# Intelligence: Purpose = find bugs
# → Execute: /analyze → Auto-chain to /fix for each bug found

# Input: /analyze module webavanue
# Intelligence: General analysis
# → Execute: /analyze → If 10 critical issues found → /fix .yolo

# Input: /analyze "to understand auth flow"
# Intelligence: Purpose = understanding
# → Execute: /analyze → /document .dev (document findings)
```

---

### /review - Code Review

**Intelligence:**
```typescript
class ReviewIntelligence {
  analyze(input: string, context: CommandContext) {
    // What's being reviewed?
    const target = detectReviewTarget(input, context);

    // Pre-review checks
    if (context.project.hasFailingTests) {
      return {
        block: true,
        fix: '/fix .yolo "failing tests"',
        reasoning: 'Fix failing tests before review'
      };
    }

    // Auto-fix simple issues?
    const onComplete = (results) => {
      const autoFixable = results.issues.filter(i => i.autoFixable);

      if (autoFixable.length > 0 && hasModifier(context, 'yolo')) {
        return {
          autoFix: autoFixable,
          chain: ['/fix .yolo']
        };
      }

      if (results.documentationNeeded) {
        return { chain: ['/document .api'] };
      }

      if (results.technicalDebt > 70) {
        return { chain: ['/refactor'] };
      }
    };

    return { executeNormal: true, onComplete };
  }
}
```

**Examples:**
```bash
# Input: /review .
# Intelligence: Failing tests detected
# → Block review, first run: /fix .yolo "failing tests"

# Input: /review . .yolo
# Intelligence: Review complete, 15 auto-fixable issues
# → Auto-chain: /fix .yolo for each issue

# Input: /review app module webavanue
# Intelligence: Major documentation gaps found
# → Auto-chain: /document .all
```

---

### /scan - Project Scanning

**Intelligence:**
```typescript
class ScanIntelligence {
  analyze(input: string, context: CommandContext) {
    // What to scan for?
    const scanType = detectScanType(input);

    const onComplete = (results) => {
      if (scanType === 'todos') {
        const criticalTodos = results.todos.filter(t => t.priority === 'critical');
        if (criticalTodos.length > 0) {
          return {
            warn: `${criticalTodos.length} critical TODOs found`,
            suggest: '/fix .yolo'
          };
        }
      }

      if (scanType === 'dependencies') {
        const cves = results.vulnerabilities.filter(v => v.severity === 'critical');
        if (cves.length > 0) {
          return {
            critical: true,
            autoFix: '/project update .yolo',
            reasoning: `${cves.length} critical security vulnerabilities`
          };
        }
      }

      if (scanType === 'progress') {
        if (results.blockedTasks.length > 0) {
          return {
            suggest: `/fix "${results.blockedTasks[0].blocker}"`
          };
        }
      }
    };

    return { executeNormal: true, onComplete };
  }
}
```

**Examples:**
```bash
# Input: /scan todos
# Intelligence: 3 critical TODOs found
# → Suggest: /fix .yolo "critical TODO 1"

# Input: /scan deps
# Intelligence: Critical CVE detected
# → Auto-execute: /project update .yolo

# Input: /scan progress
# Intelligence: Task blocked by "API not ready"
# → Suggest: /fix "implement API"
```

---

### /project - Project Operations

**Intelligence:**
```typescript
class ProjectIntelligence {
  analyze(input: string, context: CommandContext) {
    // Auto-update detection
    if (operation === 'instructions') {
      // Check if outdated
      const currentVersion = context.project.ideacodeVersion;
      const latestVersion = '9.0';

      if (currentVersion < latestVersion) {
        return {
          warn: `IDEACODE outdated (${currentVersion} → ${latestVersion})`,
          suggest: '/project update .yolo'
        };
      }
    }

    if (operation === 'validate') {
      const onComplete = (results) => {
        if (results.score < 70) {
          return {
            autoFix: '/project update .yolo',
            reasoning: `Compliance score too low (${results.score}/100)`
          };
        }
      };
    }

    return { executeNormal: true };
  }
}
```

---

### /debug - Debugging

**Intelligence:**
```typescript
class DebugIntelligence {
  analyze(input: string, context: CommandContext) {
    // Auto-detect debug type
    const debugType = detectDebugType(input);

    const onComplete = (results) => {
      if (results.rootCauseFound) {
        if (hasModifier(context, 'yolo')) {
          return {
            autoFix: true,
            chain: ['/fix .yolo', '/document .dev']
          };
        } else {
          return {
            suggest: `/fix "${results.rootCause}"`
          };
        }
      }

      if (results.relatedBugs.length > 0) {
        return {
          chain: results.relatedBugs.map(bug => `/fix "${bug}"`)
        };
      }
    };

    return { executeNormal: true, onComplete };
  }
}
```

---

### /mockup - Code Generation

**Intelligence:**
```typescript
class MockupIntelligence {
  analyze(input: string, context: CommandContext) {
    // Auto-detect profile
    const profile = autoDetectProfile(context);

    const onComplete = (results) => {
      // Generated code has TODOs?
      if (results.generatedTodos.length > 0) {
        return {
          warn: `${results.generatedTodos.length} TODOs in generated code`,
          chain: results.generatedTodos.map(todo => `/implement "${todo}"`)
        };
      }

      // Missing tests?
      if (!results.testsGenerated) {
        return {
          chain: ['/implement .test "generated components"']
        };
      }

      // Documentation needed?
      if (results.publicApi) {
        return {
          chain: ['/document .api']
        };
      }
    };

    return { executeNormal: true, onComplete };
  }
}
```

---

## 🔗 Auto-Chaining Matrix

| Command | Detects | Auto-Chains To | Condition |
|---------|---------|----------------|-----------|
| `/develop` | Too simple | `/fix` | Complexity = trivial |
| `/fix` | Feature request | `/develop` | "add/implement" keywords |
| `/fix` | Multiple issues | `/plan` → `/implement` | >3 related issues |
| `/analyze` | Critical bugs | `/fix .yolo` | Critical issues found |
| `/analyze` | High debt | `/refactor` | Technical debt >70 |
| `/review` | Fixable issues | `/fix .yolo` | Auto-fixable + .yolo |
| `/review` | Doc gaps | `/document .api` | API changed |
| `/scan todos` | Critical TODOs | `/fix` | Priority = critical |
| `/scan deps` | CVEs | `/project update` | Security vulnerability |
| `/debug` | Root cause found | `/fix` | Root cause identified |
| `/mockup` | Incomplete | `/implement` | Generated TODOs |

---

## ⚡ Intelligence in Action

### Scenario 1: Misidentified Request

**User input:**
```bash
/fix "add dark mode support"
```

**Intelligence detection:**
```
Intent Analysis:
✗ Command: /fix (implies bug)
✓ Intent: "add dark mode" (new feature)
✓ Pattern: "add X" = feature request
✓ Confidence: 95%

Decision: REDIRECT to /develop
```

**Execution:**
```markdown
## Intent Mismatch Detected 🎯

You requested: `/fix "add dark mode support"`

**Analysis:**
- Command: /fix (bug fixing)
- Actual intent: Add new feature (dark mode)
- Pattern: "add X" indicates feature, not bug fix

**This is a feature request, not a bug.**

### Auto-Redirecting to Development Workflow

```bash
/develop .yolo "dark mode support"
```

Starting feature development workflow...
{Execute /develop}
```

---

### Scenario 2: Complexity-Driven Workflow

**User input:**
```bash
/fix "authentication has multiple issues"
```

**Intelligence detection:**
```
Complexity Analysis:
✓ Multiple issues mentioned
✓ Scan found 5 related bugs
✓ 3 layers affected (UI, logic, data)
✓ Estimated 8 tasks

Decision: PLANNED workflow (not one-shot)
```

**Execution:**
```markdown
## Complex Fix Detected - Planning Required

**Analysis:**
- Multiple related issues detected (5 bugs)
- Affects 3 layers (UI, domain, data)
- Estimated effort: 8 tasks

**This requires planning, not a quick fix.**

### Auto-Creating Fix Plan

{Execute /plan}

**Generated:** tasks/fix-auth-issues.md

### Executing Planned Fixes

{IF .yolo}
  {Execute /implement .yolo tasks/fix-auth-issues.md}
{ELSE}
  Next: /implement tasks/fix-auth-issues.md
{END IF}
```

---

### Scenario 3: Context-Aware Chaining

**User input:**
```bash
/analyze module webavanue
```

**Intelligence detection:**
```
Analysis complete.

Results:
- 3 critical bugs found
- Technical debt: 45/100 (acceptable)
- Documentation coverage: 35% (low)

Post-Analysis Intelligence:
✓ Critical bugs → Auto-suggest fixes
✓ Low docs → Chain to /document
```

**Execution:**
```markdown
## Analysis Complete + Intelligent Recommendations

### Findings
- ✅ Code quality: 78/100 (good)
- ⚠️ 3 critical bugs found
- ⚠️ Documentation: 35% coverage (target: 70%)

### Auto-Generated Actions

{IF .yolo}
  ✅ Auto-fixing 3 critical bugs...
  ✅ Bug 1 fixed (2 min)
  ✅ Bug 2 fixed (3 min)
  ✅ Bug 3 fixed (5 min)

  ✅ Updating documentation...
  {Execute /document .dev module webavanue}
{ELSE}
  **Recommended next steps:**
  1. /fix .yolo "critical bugs in auth module"
  2. /document .dev module webavanue
{END IF}
```

---

## 🛡️ Safety and Overrides

### User Override

Users can always override intelligence:

```bash
# Force execute despite intelligence warnings
/fix --force "add OAuth"  # Won't redirect to /develop

# Disable auto-chaining
/analyze .stop            # Won't auto-chain to /fix

# Explicit workflow
/develop --no-intelligence "simple change"  # Full workflow even if simple
```

### Confidence Thresholds

```typescript
const CONFIDENCE_THRESHOLDS = {
  AUTO_REDIRECT: 0.9,    // 90%+ confidence → auto-redirect
  SUGGEST: 0.7,          // 70%+ confidence → suggest alternative
  WARN: 0.5,             // 50%+ confidence → warn user
  SILENT: 0.0            // <50% confidence → execute as requested
};
```

---

## 📊 Intelligence Metrics

Track intelligence effectiveness:

```typescript
interface IntelligenceMetrics {
  redirects: {
    total: number;
    accepted: number;  // User followed redirect
    rejected: number;  // User overrode
    accuracy: number;  // % that were correct
  };

  chains: {
    total: number;
    successful: number;
    failed: number;
  };

  timeS saved: number;  // Minutes saved by smart decisions
}
```

---

## ✅ Implementation Checklist

**For each command:**
- [ ] Add Intelligence class
- [ ] Implement intent detection
- [ ] Implement complexity detection
- [ ] Define auto-chain rules
- [ ] Add override mechanisms
- [ ] Update documentation
- [ ] Add tests for intelligence layer

---

**Framework Version:** 1.0
**Status:** Ready for implementation
**Next:** Apply to all commands + create /document
