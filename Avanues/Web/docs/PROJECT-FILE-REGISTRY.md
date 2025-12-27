# Project File Registry - MainAvanues Monorepo

**Purpose:** Define file types and their correct locations across repositories
**Version:** 1.0
**Date:** 2025-11-25
**Framework:** IDEACODE v8.5

---

## 📋 File Type Categories

### 1. Universal Project Documentation
**Location:** `/Volumes/M-Drive/Coding/MainAvanues/docs/project/`
**Description:** Documentation that applies to the entire MainAvanues project

| File Type | Pattern | Location | Example |
|-----------|---------|----------|---------|
| Project-wide best practices | `*-best-practices.md` | `docs/project/` | `monorepo-best-practices.md` |
| Project structure | `*-structure.md` | `docs/project/` | `monorepo-structure.md` |
| Git guides (project-wide) | `git-*.md` | `docs/project/` | `git-access-guide.md` |
| Project standards | `*-standards.md` | `docs/project/` | `coding-standards.md` |
| Contributing guides | `CONTRIBUTING.md` | `docs/project/` | `CONTRIBUTING.md` |
| Project architecture | `ARCHITECTURE.md` | `docs/project/` | `ARCHITECTURE.md` |

### 2. Module-Specific Documentation
**Location:** `/Volumes/M-Drive/Coding/MainAvanues/docs/{module}/`
**Description:** Documentation specific to individual modules

| File Type | Pattern | Location | Example |
|-----------|---------|----------|---------|
| Module README | `README.md` | `docs/{module}/` | `docs/webavanue/README.md` |
| Module API docs | `API.md` | `docs/{module}/` | `docs/webavanue/API.md` |
| Module migration | `MIGRATION.md` | `docs/{module}/` | `docs/webavanue/MIGRATION.md` |
| Feature specs | `spec-*.md` | `docs/{module}/specs/` | `docs/webavanue/specs/spec-tab-groups.md` |
| Implementation plans | `plan-*.md` | `docs/{module}/plans/` | `docs/webavanue/plans/plan-tab-groups.md` |
| Module changelog | `CHANGELOG.md` | `docs/{module}/` | `docs/webavanue/CHANGELOG.md` |

### 3. Development Process Documentation
**Location:** `/Volumes/M-Drive/Coding/MainAvanues/docs/development/`
**Description:** How to develop, build, test

| File Type | Pattern | Location | Example |
|-----------|---------|----------|---------|
| Setup guides | `setup-*.md` | `docs/development/` | `setup-environment.md` |
| Build guides | `build-*.md` | `docs/development/` | `build-instructions.md` |
| Testing guides | `testing-*.md` | `docs/development/` | `testing-strategy.md` |
| Debugging guides | `debug-*.md` | `docs/development/` | `debug-guide.md` |

### 4. Development Session Artifacts
**Location:** `/Volumes/M-Drive/Coding/MainAvanues/docs/develop/{module}/`
**Description:** Build results, test results, session logs (temporary/historical)

| File Type | Pattern | Location | Example |
|-----------|---------|----------|---------|
| Build results | `*-build-results-*.md` | `docs/develop/{module}/` | `webavanue-build-results-202511250300.md` |
| Test results | `*-test-results-*.md` | `docs/develop/{module}/` | `webavanue-test-results-202511250315.md` |
| Git verification | `*-git-*.md` | `docs/develop/{module}/` | `webavanue-git-history-verification-*.md` |
| Migration logs | `*-migration-*.md` | `docs/develop/{module}/` | `webavanue-migration-complete-summary.md` |
| Session summaries | `*-session-*.md` | `docs/develop/{module}/` | `webavanue-session-summary-*.md` |

### 5. Architecture & Design
**Location:** `/Volumes/M-Drive/Coding/MainAvanues/docs/architecture/`
**Description:** Architectural decisions, design patterns

| File Type | Pattern | Location | Example |
|-----------|---------|----------|---------|
| Architecture docs | `architecture-*.md` | `docs/architecture/` | `architecture-overview.md` |
| Design decisions | `adr-*.md` | `docs/architecture/decisions/` | `adr-001-use-kmp.md` |
| Design patterns | `pattern-*.md` | `docs/architecture/patterns/` | `pattern-repository.md` |
| Module dependencies | `dependencies-*.md` | `docs/architecture/` | `dependencies-graph.md` |

### 6. Operations & Deployment
**Location:** `/Volumes/M-Drive/Coding/MainAvanues/docs/operations/`
**Description:** How to deploy, monitor, troubleshoot

| File Type | Pattern | Location | Example |
|-----------|---------|----------|---------|
| Deployment guides | `deploy-*.md` | `docs/operations/` | `deploy-production.md` |
| Monitoring guides | `monitor-*.md` | `docs/operations/` | `monitor-performance.md` |
| Troubleshooting | `troubleshoot-*.md` | `docs/operations/` | `troubleshoot-crashes.md` |
| Runbooks | `runbook-*.md` | `docs/operations/runbooks/` | `runbook-incident-response.md` |

### 7. Migration Analysis (Temporary)
**Location:** `/Volumes/M-Drive/Coding/MainAvanues/docs/migration-analysis/`
**Description:** Migration planning and analysis (archive after completion)

| File Type | Pattern | Location | Example |
|-----------|---------|----------|---------|
| Migration guides | `*-migration-guide.md` | `docs/migration-analysis/` | `complete-migration-guide.md` |
| Migration checklists | `*-checklist.md` | `docs/migration-analysis/` | `migration-checklist.md` |
| Migration lessons | `*-lessons-learned.md` | `docs/migration-analysis/` | `migration-lessons-learned.md` |
| Research findings | `*-research-*.md` | `docs/migration-analysis/` | `monorepo-research-findings.md` |

### 8. IDEACODE Framework Documentation
**Location:** `/Volumes/M-Drive/Coding/ideacode/`
**Description:** IDEACODE framework itself (separate repo)

| File Type | Pattern | Location | Example |
|-----------|---------|----------|---------|
| Framework protocols | `Protocol-*.md` | `ideacode/protocols/` | `Protocol-Code-Review-v1.0.md` |
| Programming standards | `*.md` | `ideacode/programming-standards/` | `ideacode-core.md` |
| Slash commands | `ideacode.*.md` | `ideacode/.claude/commands/` | `ideacode.specify.md` |
| Framework config | `config.yml` | `ideacode/.ideacode/` | `config.yml` |
| Update ideas | `*.md` | `ideacode/updateideas/` | `foldernaming.md` |

---

## 📂 Actual Directory Structure (After Cleanup)

```
/Volumes/M-Drive/Coding/MainAvanues/docs/
├── project/                                # Universal project documentation
│   ├── LD-mainavanues-architecture.md
│   ├── mainavanues-monorepo-best-practices.md
│   ├── mainavanues-monorepo-structure.md
│   ├── mainavanues-git-access-guide.md
│   ├── mainavanues-setup.md
│   ├── mainavanues-testing.md
│   ├── mainavanues-documentation-consolidation.md
│   └── mainavanues-readme.md
│
├── architecture/                           # Architecture & design
│   ├── decisions/                          # ADRs (Architecture Decision Records)
│   └── patterns/                           # Design patterns
│
├── operations/                             # Operations & deployment
│   └── runbooks/                           # Operational runbooks
│
├── webavanue/                              # WebAvanue module docs
│   ├── LD-webavanue-readme.md             # Living document
│   ├── webavanue-dev-overview.md
│   ├── webavanue-webxr-design.md
│   ├── webavanue-webxr-backlog.md
│   ├── webavanue-scrolling-implementation-guidance.md
│   ├── webavanue-ready-to-implement.md
│   ├── webavanue-user-manual.md
│   ├── specs/                              # Feature specifications (12 files)
│   │   ├── webavanue-spec-scrolling-controls.md
│   │   ├── webavanue-spec-zoom-controls.md
│   │   ├── webavanue-spec-desktop-mode.md
│   │   ├── webavanue-spec-favorites-bar.md
│   │   ├── webavanue-spec-clear-cookies.md
│   │   ├── webavanue-spec-frame-navigation.md
│   │   ├── webavanue-spec-touch-controls.md
│   │   ├── webavanue-spec-cursor-controls.md
│   │   ├── webavanue-spec-http-auth.md
│   │   ├── webavanue-spec-qr-scanner.md
│   │   └── webavanue-spec-webxr-support.md
│   ├── plans/                              # Implementation plans (7 files)
│   │   ├── webavanue-plan-scrolling-controls.md
│   │   ├── webavanue-plan-zoom-controls.md
│   │   ├── webavanue-plan-desktop-mode.md
│   │   ├── webavanue-plan-favorites-bar.md
│   │   ├── webavanue-plan-clear-cookies.md
│   │   └── webavanue-plan-webxr-support.md
│   ├── proposals/                          # Feature proposals (12 files)
│   │   ├── webavanue-proposal-scrolling-controls.md
│   │   ├── webavanue-proposal-zoom-controls.md
│   │   ├── (... 10 more proposals)
│   │   └── webavanue-proposal-webxr-support.md
│   └── archive/                            # Completed work with timestamps (10 files)
│       ├── webavanue-webxr-implementation-status-202511231800.md
│       ├── webavanue-webxr-phase2-implementation-202511231650.md
│       ├── webavanue-webxr-emulator-test-results-202511231700.md
│       ├── webavanue-webxr-phase1-implementation-202511231400.md
│       ├── webavanue-session-summary-202511220000.md
│       ├── webavanue-bug-state-serialization-crash-202511241800.md
│       ├── webavanue-fix-summary-browser-bugs-202511241900.md
│       ├── webavanue-gestures-implementation-202511242000.md
│       └── webavanue-legacy-migration-summary-202511242100.md
│
├── voiceos/                                # VoiceOS module docs
│   ├── voiceos-dev-overview.md
│   ├── specs/
│   ├── plans/
│   ├── proposals/
│   └── archive/
│
├── avaconnect/                             # AvaConnect module docs
│   ├── avaconnect-dev-overview.md
│   ├── specs/
│   ├── plans/
│   ├── proposals/
│   └── archive/
│
├── ava/                                    # AVA module docs
│   ├── ava-dev-overview.md
│   ├── specs/
│   ├── plans/
│   ├── proposals/
│   └── archive/
│
├── avanues/                                # Avanues platform docs
│   ├── avanues-dev-overview.md
│   └── (module structure as needed)
│
├── shared-libs/                            # Shared libraries
│   ├── accessibility/
│   ├── ui/
│   └── voice/
│
├── develop/                                # Development artifacts (temporary)
│   ├── webavanue/                          # 4 timestamped files
│   ├── voiceos/
│   ├── avaconnect/
│   └── ava/
│
├── ideacode/                               # IDEACODE framework usage (project-specific)
│   ├── specs/
│   ├── features/
│   ├── protocols/
│   ├── design-standards/
│   ├── registries/
│   └── (other IDEACODE artifacts)
│
├── migration-analysis/                     # Migration docs (archive after completion)
│
├── manuals/                                # User & developer manuals
│   ├── developer/
│   ├── user/
│   └── design/
│
└── archive/                                # Archived docs
    └── 2024/

/Volumes/M-Drive/Coding/ideacode/           # IDEACODE framework (separate repo)
├── .claude/commands/                       # Slash commands
├── .ideacode/config.yml                    # Framework config
├── protocols/                              # Master protocols
├── programming-standards/                  # Standards registry
└── libraries/                              # Shared libraries
```

---

## 🔀 File Movement Rules

### When to Move Files

| Current Location | Should Be | Reason |
|------------------|-----------|--------|
| `docs/*.md` (project-wide) | `docs/project/` | Universal project docs |
| `docs/develop/*.md` (not module-specific) | `docs/project/` or `docs/development/` | Permanent docs shouldn't be in develop/ |
| `docs/{module}/*-spec.md` | `docs/{module}/specs/` | Feature specs go in specs/ |
| `docs/{module}/*-plan.md` | `docs/{module}/plans/` | Implementation plans go in plans/ |
| Root `README.md`, `CONTRIBUTING.md` | `docs/project/` | Project-level docs |

### Files to Move Now

```bash
# Current incorrect locations → Correct locations

docs/MONOREPO-BEST-PRACTICES.md → docs/project/monorepo-best-practices.md
docs/MONOREPO-STRUCTURE.md → docs/project/monorepo-structure.md
docs/develop/GIT-ACCESS-GUIDE.md → docs/project/git-access-guide.md
```

---

## 📝 Naming Conventions (IDEACODE v8.5)

### General Rules
1. **All lowercase** with hyphens (kebab-case)
2. **No type prefixes** (no `feature-`, `data-`, `ui-`)
3. **Descriptive and concise**
4. **Consistent patterns** across similar file types

### Documentation Files

#### Repository-Level Files
**Pattern:** `{reponame}-{description}.md`
```
mainavanues-monorepo-best-practices.md      ✅
mainavanues-git-access-guide.md             ✅
mainavanues-setup.md                        ✅
```

#### Living Documents (Continuously Updated)
**Pattern:** `LD-{repo/module}-{description}.md`
```
LD-mainavanues-architecture.md              ✅
LD-webavanue-readme.md                      ✅
LD-voiceos-api.md                           ✅
```

#### Module-Specific Files
**Pattern:** `{modulename}-{feature}-{description}.md`
```
webavanue-spec-scrolling-controls.md        ✅
webavanue-plan-zoom-controls.md             ✅
webavanue-proposal-touch-controls.md        ✅
webavanue-webxr-design.md                   ✅
```

#### Timestamped Files (One-Time/Completed Work)
**Pattern:** `{modulename}-{feature}-{description}-YYYYMMDDHHMM.md`
```
webavanue-build-results-202511250300.md     ✅
webavanue-webxr-implementation-status-202511231800.md  ✅
voiceos-migration-summary-202511201400.md   ✅
```

#### ❌ INCORRECT Examples
```
MONOREPO-BEST-PRACTICES.md      # ❌ All caps (legacy style)
GIT-ACCESS-GUIDE.md             # ❌ All caps
WebAvanue-Build-Results.md      # ❌ Mixed case
Feature-Tab-Groups-Spec.md      # ❌ Type prefix + mixed case
TAB-GROUPS-PLAN.md              # ❌ All caps
spec-tab-groups.md              # ❌ Missing module prefix
build-results-202511250300.md   # ❌ Missing module prefix
```

### Date Format in Filenames
When including dates: `YYYYMMDDHHMM` (no separators)

**Examples:**
- `webavanue-test-results-202511250315.md` ✅
- `webavanue-test-results-2025-11-25-03-15.md` ❌

### Version Format in Filenames
When including versions: `-vX.Y.md` or `-vX.Y.Z.md`

**Examples:**
- `protocol-code-review-v1.0.md` ✅
- `Protocol-Code-Review-v1.0.md` ❌ (caps)
- `code-review-protocol-version-1.0.md` ❌ (verbose)

---

## 🤖 Ensuring IDEACODE-MCP Usage

### Problem
AI not consistently using IDEACODE-MCP tools and conventions

### Solutions

#### 1. **Update .claude/CLAUDE.md** (Project Instructions)

Add explicit enforcement section:

```markdown
## MANDATORY RULES (ENFORCE ALWAYS)

### 1. ALWAYS Use IDEACODE-MCP Tools First
**Before doing ANY IDEACODE operation, check if MCP tool exists.**

Default tool mappings (USE THESE):
- Create specification → `mcp__ideacode-mcp__ideacode_specify`
- Create plan → `mcp__ideacode-mcp__ideacode_plan`
- Implement → `mcp__ideacode-mcp__ideacode_implement`
- Run tests → `mcp__ideacode-mcp__ideacode_test`
- Git commit → `mcp__ideacode-mcp__ideacode_commit`
- Archive feature → `mcp__ideacode-mcp__ideacode_archive`

**If MCP tool is not available, inform user and ask to check MCP server.**

### 2. ALWAYS Use IDEACODE Naming Conventions
- All filenames: lowercase-with-hyphens.md
- No type prefixes (no feature-, data-, ui-)
- Dates: YYYYMMDDHHMM format
- Versions: -vX.Y.md format

### 3. ALWAYS Use Correct File Locations
Reference: /docs/PROJECT-FILE-REGISTRY.md

**Before creating any file:**
1. Check PROJECT-FILE-REGISTRY.md for correct location
2. Use lowercase-kebab-case filename
3. Place in correct directory per registry

**Common locations:**
- Project-wide docs → docs/project/
- Module docs → docs/{module}/
- Feature specs → docs/{module}/specs/
- Build/test results → docs/develop/{module}/
```

#### 2. **Create .claude/settings.json** (Claude Code Settings)

```json
{
  "rules": [
    {
      "pattern": ".*",
      "beforeAction": "always-check-ideacode-mcp",
      "enforceMCP": true
    }
  ],
  "ideacode": {
    "enforceNaming": true,
    "enforceFileRegistry": true,
    "mcpRequired": true,
    "registryPath": "docs/PROJECT-FILE-REGISTRY.md"
  },
  "reminders": {
    "beforeFileCreate": "Check PROJECT-FILE-REGISTRY.md for location and naming",
    "beforeIDEACODEOperation": "Use mcp__ideacode-mcp__* tools, not manual operations"
  }
}
```

#### 3. **Add to System Prompt** (via MCP Server Config)

Update `ideacode-mcp/src/index.ts` to inject rules:

```typescript
// Add to MCP server response
const ENFORCEMENT_RULES = `
CRITICAL RULES - CHECK ON EVERY OPERATION:

1. FILE CREATION:
   - Check /docs/PROJECT-FILE-REGISTRY.md FIRST
   - Use lowercase-kebab-case.md naming
   - Place in correct directory per registry

2. IDEACODE OPERATIONS:
   - ALWAYS use mcp__ideacode-mcp__* tools
   - NEVER manually create specs/plans/implementations
   - If MCP tool fails, notify user immediately

3. NAMING CONVENTIONS:
   - Filenames: lowercase-with-hyphens.md
   - No type prefixes (feature-, data-, ui-)
   - Dates: YYYYMMDDHHMM
   - Versions: -vX.Y.md
`;
```

#### 4. **Pre-Flight Checklist Tool**

Create a tool that Claude MUST call before creating files:

```typescript
// ideacode-mcp/src/tools/pre-flight-check.ts
export async function preFlightCheck(args: {
  operation: string;
  filename?: string;
  location?: string;
}): Promise<string> {
  const registry = await readFileRegistry();

  // Check naming convention
  if (args.filename && !isValidNaming(args.filename)) {
    return `❌ Invalid naming: ${args.filename}
Should be: ${toKebabCase(args.filename)}
Rule: lowercase-with-hyphens.md`;
  }

  // Check location
  if (args.location && !isValidLocation(registry, args.filename, args.location)) {
    return `❌ Invalid location: ${args.location}
Should be: ${getCorrectLocation(registry, args.filename)}
Check: docs/PROJECT-FILE-REGISTRY.md`;
  }

  // Check MCP tool usage
  if (isIDEACODEOperation(args.operation) && !usingMCPTool()) {
    return `❌ Must use MCP tool for IDEACODE operations
Use: mcp__ideacode-mcp__ideacode_${args.operation}`;
  }

  return `✅ Pre-flight check passed`;
}
```

#### 5. **User Reminder System**

Add to `.claude/CLAUDE.md`:

```markdown
## AI Self-Check Protocol

Before EVERY file operation, mentally check:

1. ❓ Is this file location correct per PROJECT-FILE-REGISTRY.md?
2. ❓ Is this filename using kebab-case (lowercase-with-hyphens)?
3. ❓ Am I using MCP tools for IDEACODE operations?
4. ❓ Did I check for existing files that should be moved?

If ANY answer is NO or UNSURE:
- STOP
- Check PROJECT-FILE-REGISTRY.md
- Use correct location and naming
- Use MCP tools if applicable
```

---

## 🔧 Immediate Actions Required

### 1. Move Files to Correct Locations

```bash
# Move project-wide docs to docs/project/
mkdir -p docs/project
mv docs/MONOREPO-BEST-PRACTICES.md docs/project/monorepo-best-practices.md
mv docs/MONOREPO-STRUCTURE.md docs/project/monorepo-structure.md
mv docs/develop/GIT-ACCESS-GUIDE.md docs/project/git-access-guide.md

# Create missing directories
mkdir -p docs/webavanue/{specs,plans}
mkdir -p docs/voiceos/{specs,plans}
mkdir -p docs/architecture/{decisions,patterns}
mkdir -p docs/operations/runbooks
```

### 2. Update .claude/CLAUDE.md

Add the "MANDATORY RULES" section with explicit MCP enforcement.

### 3. Create .claude/settings.json

Add the settings file with enforcement rules.

### 4. Update IDEACODE-MCP

Add pre-flight check tool to MCP server.

### 5. Test Enforcement

```bash
# Test by asking Claude to create a file
# Should automatically:
# 1. Check PROJECT-FILE-REGISTRY.md
# 2. Use correct naming (lowercase-kebab-case)
# 3. Use MCP tools for IDEACODE operations
```

---

## 📋 File Registry Checklist

When creating ANY file, check:

- [ ] Consulted PROJECT-FILE-REGISTRY.md for location
- [ ] Used lowercase-kebab-case naming
- [ ] No type prefixes (feature-, data-, ui-)
- [ ] Placed in correct directory
- [ ] Used MCP tool if IDEACODE operation
- [ ] Updated relevant documentation references

---

## 🔍 Quick Location Lookup

**"Where does this file go?"**

| File Purpose | Location |
|--------------|----------|
| Monorepo practices, git guides, project standards | `docs/project/` |
| Module README, API, changelog | `docs/{module}/` |
| Feature specifications | `docs/{module}/specs/` |
| Implementation plans | `docs/{module}/plans/` |
| Build/test results (temp) | `docs/develop/{module}/` |
| Architecture decisions (ADRs) | `docs/architecture/decisions/` |
| Setup, build, test guides | `docs/development/` |
| Deployment, monitoring | `docs/operations/` |
| Migration analysis (temp) | `docs/migration-analysis/` |

---

**Last Updated:** 2025-11-25
**Maintainer:** @manoj_mbpm14
**Version:** 1.0
**Framework:** IDEACODE v8.5
