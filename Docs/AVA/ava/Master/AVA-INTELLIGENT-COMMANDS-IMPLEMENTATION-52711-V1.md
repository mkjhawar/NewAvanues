# Intelligent Commands Implementation Summary

**Version:** 9.0
**Date:** 2025-11-27
**Status:** ✅ Implemented and Configured

---

## Overview

All IDEACODE v9.0 commands now feature comprehensive intelligence layers that:
- Auto-detect user intent and context
- Suggest optimal workflows
- Auto-chain to related commands
- Provide configurable thresholds
- Respect .yolo modifier for auto-execution

---

## Implemented Intelligence

### ✅ 1. /fix - Bug Fixing Intelligence

**Intelligence Layer:** Stage -1 (Pre-Execution)

**Features:**
- **Feature vs Bug Detection:** Analyzes user input to detect if request is actually a feature (not a bug)
  - If feature detected with 85%+ confidence → Auto-redirect to `/develop`
  - Keywords: "add", "implement", "create" vs "broken", "crash", "error"

- **Multiple Issues Detection:** Identifies when user wants to fix multiple issues
  - If detected → Suggests `/plan` → `/tasks` → `/implement` workflow

- **Single Shot vs Planned Fix:** Determines complexity
  - Simple one-line fix → Execute directly
  - Complex multi-file fix → Create plan first

- **Post-Fix Intelligent Chaining:**
  - Technical Debt > threshold → Auto-chain to `/refactor`
  - Related bugs detected → Suggest fixing together
  - Documentation needed → Chain to `/document`

**Thresholds (Configurable):**
- Feature redirect confidence: 90%
- Multiple issues threshold: 3+
- Technical debt trigger: 70%

**Example:**
```bash
/fix "add OAuth to fix social login"
→ Intelligence: Detected FEATURE_REQUEST (confidence: 95%)
→ Auto-redirect: /develop "add OAuth to social login"
```

---

### ✅ 2. /develop - Feature Development Intelligence

**Intelligence Layer:** Stage -1 (Pre-Execution)

**Features:**
- **Complexity Analysis:** Scores development complexity 0-100
  - <30: Too simple, redirect to `/fix`
  - 30-80: Standard feature workflow
  - 80+: Architectural change, needs design phase

- **Bug Disguised as Feature:** Detects bug fixes mistakenly called "features"
  - If bug detected → Redirect to `/fix`

- **Existing Spec Detection:** Checks if specification already exists
  - If found → Ask to reuse or create new

- **Urgency Detection:** Identifies urgent/hotfix requests
  - If urgent → Streamlined workflow, skip planning

- **Post-Development Chaining:**
  - Work complete → Chain to `/review .swarm`
  - Tests missing → Chain to `/develop .yolo "add tests"`

**Thresholds (Configurable):**
- Trivial threshold: 20
- Architectural threshold: 80
- Test coverage requirement: 90%

**Example:**
```bash
/develop "fix typo in button text"
→ Intelligence: Detected TRIVIAL_CHANGE (score: 15)
→ Redirect: /fix "fix typo in button text" (more efficient)
```

---

### ✅ 3. /analyze - Code Analysis Intelligence

**Intelligence Layer:** Step 0 (Pre-Analysis)

**Features:**
- **Purpose Detection:** Identifies why user is analyzing
  - FIND_BUGS → Chain to `/fix`
  - CODE_REVIEW → Chain to `/review`
  - REFACTORING → Chain to `/refactor`
  - UNDERSTANDING → Chain to `/document`
  - GENERAL → Run comprehensive analysis

- **Context-Aware Analysis:** Uses project context
  - Failing tests detected → Focus on debugging
  - High technical debt → Focus on refactoring opportunities

- **Post-Analysis Actions:**
  - Critical bugs found → Auto-chain to `/fix .yolo`
  - Technical debt > 70% → Suggest `/refactor`
  - Documentation gaps → Suggest `/document`
  - Test coverage < 90% → Suggest adding tests

**Thresholds (Configurable):**
- Critical bug threshold: 1+
- Technical debt trigger: 70%
- Documentation gap trigger: 5+ missing docs
- Test coverage target: 90%

**Example:**
```bash
/analyze "find bugs in auth module"
→ Intelligence: Purpose = FIND_BUGS
→ Analysis: 3 critical bugs found
→ Auto-chain: /fix .yolo "critical auth bugs" (with .yolo)
```

---

### ✅ 4. /review - Code Review Intelligence

**Intelligence Layer:** Step 0 (Pre-Review)

**Features:**
- **Pre-Review Blocking Checks:**
  - ❌ Failing tests → Block review (fix tests first)
  - ❌ Compilation errors → Block review
  - ❌ Merge conflicts → Block review
  - ⚠️ Code style violations → Auto-fix with .yolo

- **Auto-Fix Detection (Pre-Review):**
  - High confidence (99%): Code style, imports → Auto-fix with .yolo
  - Medium confidence (85%): Documentation gaps → Suggest
  - Low confidence (<80%): Test improvements → Recommend only

- **Post-Review Intelligent Chaining:**
  - Critical bugs found → `/fix .yolo "{bug}"`
  - High technical debt → `/refactor .cot`
  - APP review complete → `/plan specs/enhancement-proposals.md`
  - PR with blocking issues → `gh pr review --request-changes`
  - Work 100% complete → `/review .swarm`

**Thresholds (Configurable):**
- Auto-fix confidence threshold: 95%
- Blocking test failure count: 1+
- Technical debt trigger: 70%

**Example:**
```bash
/review .yolo
→ Intelligence: Pre-review checks...
→ Found: 2 failing tests (BLOCKING)
→ Auto-fix: /fix .yolo "failing tests"
→ Re-check: All tests passing ✅
→ Proceeding with review...
```

---

### ✅ 5. /scan - Project Scanning Intelligence

**Intelligence Layer:** Step 0 (Pre-Scan)

**Features:**
- **Smart Type Selection (Context-Aware):**
  - Active work + specs detected → Suggest PROGRESS scan
  - Security vulnerabilities detected → Suggest DEPENDENCIES scan
  - Many TODOs (>50) → Suggest TODOS scan
  - New project or missing docs → Suggest STRUCTURE scan

- **Contextual Recommendations:**
  - STRUCTURE: Missing README, needs modularization, long files
  - DEPENDENCIES: Security vulns, outdated deps, version conflicts
  - TODOS: Critical TODOs (HACK/FIXME), stale TODOs
  - PROGRESS: Ready for review, blockers detected, low test coverage

- **Post-Scan Intelligent Actions:**
  - Security vulnerabilities → Auto-chain to `/fix .yolo "update vulnerable deps"`
  - Critical TODOs (HACK/FIXME) → Auto-chain to `/fix .yolo "critical TODOs"`
  - Work 100% complete → Suggest `/review .swarm`
  - Blockers detected → Auto-fix with `/fix .yolo "{blocker}"`
  - Poor organization → Suggest `/refactor`

**Thresholds (Configurable):**
- TODO count threshold: 50+
- Security vuln auto-fix: Critical/High CVEs
- Progress complete threshold: 100%

**Example:**
```bash
/scan todos
→ Intelligence: Found 5 HACK comments, 35 old TODOs
→ Recommendations:
   🔴 HIGH: Fix 5 HACK comments immediately
   🟡 MEDIUM: Convert 35 old TODOs to issues
→ Auto-action with .yolo: /fix .yolo "critical TODOs"
```

---

### ✅ 6. /project - Project Operations Intelligence

**Intelligence Layer:** Step 0 (Pre-Operation)

**Features:**
- **Project Health Check (Always Run):**
  - Detects IDEACODE presence and version
  - Checks for missing critical files (.claude/CLAUDE.md, .ideacode/config.yml, README.md)
  - Validates configuration (project name, type, etc.)
  - Calculates health score (0-100)

- **Smart Operation Selection:**
  - No IDEACODE detected → Suggest INIT
  - Outdated version detected → Suggest UPDATE
  - Compliance issues detected → Suggest VALIDATE
  - Otherwise → Show INSTRUCTIONS

- **Intelligent Recommendations:**
  - Missing critical files → Auto-create with .yolo
  - Configuration errors → Auto-fix
  - Health score < 70 → Run comprehensive validation
  - Outdated version → Auto-update to v9.0

**Thresholds (Configurable):**
- Health score target: 80+
- Outdated version detection: < v9.0
- Critical files: .claude/CLAUDE.md, .ideacode/config.yml, README.md

**Example:**
```bash
/project
→ Intelligence: Health check...
→ Health Score: 60/100
→ Issues: Missing README.md, IDEACODE v8.5 (outdated)
→ Recommendations:
   🔴 Update to v9.0: /project update .yolo
   🟡 Create README.md
```

---

## Universal Intelligence Framework

All commands share these common patterns:

### 1. **Intelligence Layer Phases**

```typescript
// Phase 1: Context Detection
- Analyze user input and intent
- Check project context and state
- Detect mismatches between intent and command

// Phase 2: Intent Analysis
- Calculate confidence scores (0-1.0)
- Identify optimal workflow
- Detect auto-fixable issues

// Phase 3: Workflow Selection
- Select appropriate workflow or redirect
- Prepare auto-chaining recommendations
- Calculate expected actions
```

### 2. **Confidence-Based Decisions**

| Confidence | Action | Example |
|-----------|--------|---------|
| 95%+ | Auto-redirect/execute | Feature detected in /fix → redirect to /develop |
| 80-95% | Strong suggestion | Multiple issues → suggest /plan workflow |
| 60-80% | Recommendation | Technical debt high → recommend /refactor |
| <60% | Silent | Execute as requested |

### 3. **Configurable Thresholds**

**Location:** `/Volumes/M-Drive/Coding/ideacode/docs/INTELLIGENT-COMMANDS-FRAMEWORK.md`

**Default Thresholds:**
```typescript
const INTELLIGENCE_THRESHOLDS = {
  // Complexity scoring (0-100)
  complexity: {
    trivial: 20,
    simple: 40,
    moderate: 60,
    complex: 80,
    architectural: 80
  },

  // Quality metrics (0-100)
  quality: {
    technicalDebt: 70,        // 70+: Trigger refactor
    testCoverage: 90,         // <90%: Suggest tests
    documentationCoverage: 70 // <70%: Suggest docs
  },

  // Intent detection confidence (0-1.0)
  intent: {
    autoRedirect: 0.90,   // 90%+ confidence: Auto-redirect
    suggest: 0.70,        // 70%+ confidence: Suggest
    warn: 0.50,           // 50%+ confidence: Warn
    silent: 0.00          // <50% confidence: Silent
  },

  // Issue severity
  issues: {
    criticalCount: 1,     // 1+ critical: Urgent
    warningCount: 5,      // 5+ warnings: Suggest fixes
    documentationGaps: 5, // 5+ gaps: Suggest docs
    todoCount: 50         // 50+ TODOs: Suggest cleanup
  }
};
```

**Configuration:**
```yaml
# .ideacode/config.yml
intelligence:
  thresholds:
    technicalDebt: 60        # Stricter for this project
    testCoverage: 95         # Higher standard
    documentationCoverage: 80
    autoRedirect: 0.95       # More conservative auto-redirects
```

---

## Auto-Chaining Examples

### Example 1: Feature Request in /fix

```bash
User: /fix "add dark mode toggle"

Intelligence:
✓ Detected: FEATURE_REQUEST (confidence: 95%)
✓ Keywords: "add" (feature) vs no bug keywords
✓ Decision: Auto-redirect to /develop

Output:
⚡ Auto-redirect: /develop "add dark mode toggle"
Reason: Feature request detected (95% confidence)
```

### Example 2: Complex Bug Requiring Planning

```bash
User: /fix "multiple authentication issues across the app"

Intelligence:
✓ Detected: MULTIPLE_ISSUES (confidence: 90%)
✓ Keywords: "multiple", "across the app"
✓ Decision: Suggest planned workflow

Output:
💡 Suggestion: Multiple issues detected
Recommended workflow:
  1. /plan "fix authentication issues"
  2. /tasks (break down fixes)
  3. /implement .yolo tasks.md
Use .yolo to auto-execute this workflow
```

### Example 3: Analysis Leading to Fix

```bash
User: /analyze "find bugs in payment module"

Intelligence:
✓ Purpose: FIND_BUGS
✓ Expected action: Chain to /fix after analysis

Analysis Results:
- 3 critical bugs found
- 5 warnings
- Technical debt: 45% (below threshold)

Post-Analysis:
⚡ Auto-chain: /fix .yolo "3 critical payment bugs"
Bugs:
1. Null pointer in checkout
2. Race condition in payment processing
3. SQL injection in order history
```

### Example 4: Review with Pre-Flight Checks

```bash
User: /review .yolo

Intelligence - Pre-Review Checks:
✗ BLOCKING: 2 tests failing
✗ BLOCKING: 1 compilation error
⚠️ WARNING: 45 code style violations

With .yolo modifier:
⚡ Auto-fixing blocking issues...
   ✓ Fixed failing tests
   ✓ Fixed compilation error
   ✓ Auto-formatted code (45 violations)

✅ All checks passed. Proceeding with review...
```

### Example 5: Scan Detecting Security Issues

```bash
User: /scan deps .yolo

Intelligence:
✓ Type: DEPENDENCIES
✓ Context: Security scan

Results:
🔴 CRITICAL: 3 security vulnerabilities (CVEs)
   - okhttp:4.11.0 → CVE-2023-XXXX (RCE)
   - json:20210307 → CVE-2022-XXXX (DoS)
   - commons:3.1 → CVE-2021-XXXX (XSS)

Post-Scan Action (Auto-execute with .yolo):
⚡ /fix .yolo "update vulnerable dependencies"
   ✓ Updated okhttp to 4.11.2 (fixes CVE)
   ✓ Updated json to 20230227 (fixes CVE)
   ✓ Updated commons to 3.2.1 (fixes CVE)
✅ All security vulnerabilities patched
```

---

## Benefits

### 1. **Reduced Cognitive Load**
- Users don't need to remember which command to use
- System detects intent and suggests optimal workflow
- Auto-chaining eliminates manual workflow orchestration

### 2. **Faster Workflows**
- .yolo modifier enables full automation
- Auto-fixes for common issues (code style, security patches)
- Intelligent redirects prevent wrong workflow selection

### 3. **Quality Assurance**
- Pre-flight checks prevent bad reviews
- Auto-detects when work is ready for next stage
- Enforces quality gates (tests, coverage, style)

### 4. **Learning and Discovery**
- System suggests workflows you might not know about
- Shows reasoning (.cot modifier) for educational value
- Recommends best practices automatically

### 5. **Consistency**
- Same intelligence patterns across all commands
- Predictable behavior with configurable thresholds
- Universal .yolo/.swarm/.cot modifiers

---

## Testing

### Manual Testing Scenarios

1. **Feature vs Bug Detection:**
   - `/fix "add OAuth"` → Should redirect to `/develop`
   - `/fix "broken OAuth login"` → Should stay in `/fix`

2. **Complexity Detection:**
   - `/develop "fix typo"` → Should redirect to `/fix`
   - `/develop "add user authentication"` → Should use full workflow

3. **Pre-Review Checks:**
   - `/review` with failing tests → Should block
   - `/review .yolo` with failing tests → Should auto-fix then review

4. **Post-Scan Actions:**
   - `/scan deps` with CVEs → Should suggest `/fix`
   - `/scan todos` with HACKs → Should suggest `/fix .yolo`

5. **Project Health:**
   - `/project` in new repo → Should suggest `/project init`
   - `/project` with v8.5 → Should suggest `/project update`

### Integration Testing

Test full workflow chains:
```bash
# Test 1: Feature development workflow
/fix "add dark mode"
→ Redirects to /develop
→ Creates spec, plan, tasks
→ Implements with .yolo
→ Chains to /review
→ Chains to /document

# Test 2: Bug fix workflow
/analyze "find auth bugs"
→ Detects 3 bugs
→ Chains to /fix .yolo
→ Fixes all 3 bugs
→ Updates tests
→ Chains to /review
```

---

## Next Steps

### 1. **Remaining Commands (Lower Priority)**
- `/debug` - Debug session intelligence (lighter enhancement)
- `/mockup` - Mockup-to-code intelligence (lighter enhancement)

### 2. **Testing Phase**
- Create comprehensive test suite
- Manual testing of all auto-chain scenarios
- Validate threshold configurations
- Test .yolo behavior across all commands

### 3. **Distribution**
- Sync to all 7 IDEACODE-integrated repositories:
  1. **ideacode** - Core framework (primary)
  2. **AVA** - Android voice assistant app
  3. **AvaConnect** - Android connectivity management
  4. **Avanues** - Voice-first Android platform
  5. **VoiceOS** - Core voice OS (contains LearnApp)
  6. **NewAvanues** - Next-gen monorepo (AVA + VoiceOS + AvaMagic)
  7. **MainAvanues** - Main development monorepo (contains WebAvanue)

### 4. **Documentation**
- Update user guides with intelligence examples
- Create video demonstrations
- Write migration guide for users on v8.x

---

**Implementation Status:** ✅ COMPLETE for major commands
**Remaining Work:** Testing and distribution
**Author:** Manoj Jhawar + Claude (Sonnet 4.5)
**Date:** 2025-11-27
