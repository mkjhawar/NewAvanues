# Living Document: MasterDocs Integration Guide

**Version:** 1.0
**Updated:** 2026-01-11
**Status:** Active
**Purpose:** How to add the MasterDocs system to any project or framework

---

## What is MasterDocs?

MasterDocs is a **dual-format documentation system** that maintains synchronized documentation for both humans and AI/LLMs:

| Format | Purpose | Location |
|--------|---------|----------|
| **Human-readable** | Developer documentation, diagrams | `{Module}/README.md`, `{Module}/html/` |
| **AI-readable** | Structured data for LLM context | `AI/*.ai.md` (YAML blocks) |
| **Living Docs** | Meta-documentation, protocols | `LD/LD-*.md` |

**Why?** When Claude or another LLM reads your codebase, it benefits from structured, up-to-date documentation that describes classes, APIs, and code health status.

---

## Directory Structure

```
{project}/Docs/MasterDocs/
├── AI/                              # AI-readable documents
│   ├── PLATFORM-INDEX.ai.md         # Module registry, dependencies, APIs
│   ├── CLASS-INDEX.ai.md            # All classes with methods/fields
│   ├── REFACTORING-GUIDE.ai.md      # Code health, SOLID status, resolved issues
│   ├── API-REFERENCE.ai.md          # API signatures (optional)
│   └── DEPENDENCY-MAP.ai.md         # Module dependencies (optional)
│
├── LD/                              # Living Documents (meta)
│   ├── LD-MasterDocs-Protocol-V1.md
│   ├── LD-MasterDocs-Integration-Guide-V1.md (this file)
│   ├── LD-Platform-Overview-V1.md
│   └── LD-API-Reference-V1.md
│
├── {Module}/                        # Per-module human docs
│   ├── README.md                    # Overview, architecture, usage
│   └── html/                        # Visual diagrams (optional)
│       ├── index.html
│       ├── architecture.html
│       ├── flowcharts.html
│       └── class-diagrams.html
│
└── Common/                          # Shared libraries docs
    └── README.md
```

---

## Components Added

### 1. AI-Readable Documents (`AI/*.ai.md`)

These files use YAML code blocks for structured data that LLMs can parse:

#### PLATFORM-INDEX.ai.md
```yaml
## MODULE_REGISTRY

### MODULE_NAME
\`\`\`yaml
id: module-id
path: /Modules/ModuleName
type: kmp-library|android-app|etc
status: production|development
version: x.y.z
purpose: One-line description
key_classes:
  - ClassName: Short description
dependencies:
  - OtherModule
capabilities:
  - feature_one
  - feature_two
\`\`\`
```

#### CLASS-INDEX.ai.md
```yaml
### ClassName
\`\`\`yaml
package: com.company.module
file: ClassName.kt
type: class|interface|object|sealed_class
lines: 150 (optional)
commit: abc123 (if recently added)
purpose: What this class does
extends: ParentClass (optional)
implements: InterfaceName (optional)
pattern: singleton|factory|builder (optional)
methods:
  - methodName(param: Type): ReturnType
  - anotherMethod(): Unit
\`\`\`
```

#### REFACTORING-GUIDE.ai.md
```yaml
## CURRENT_STATUS
\`\`\`yaml
solid_compliance: 9.5/10
critical_issues: 0
high_issues: 0
status: HEALTHY|NEEDS_ATTENTION|CRITICAL
\`\`\`

## RESOLVED_ISSUES

### ISSUE_ID [RESOLVED]
\`\`\`yaml
status: RESOLVED
resolved_in: commit_hash
evidence:
  - What was done
verification: How to confirm
\`\`\`
```

### 2. API Endpoints (ideacode-api)

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/masterdocs/init` | POST | Initialize MasterDocs for a new repo |
| `/masterdocs/update` | POST | Update AI docs (class/resolved/module) |
| `/masterdocs/status` | GET | Check MasterDocs completeness |

#### Initialize Example
```bash
curl -X POST http://localhost:3850/masterdocs/init \
  -H "Content-Type: application/json" \
  -d '{
    "repo": "MyProject",
    "projectName": "My Project",
    "projectType": "kmp-library",
    "modules": ["Core", "UI", "Data"]
  }'
```

#### Update Example (add class)
```bash
curl -X POST http://localhost:3850/masterdocs/update \
  -H "Content-Type: application/json" \
  -d '{
    "repo": "MyProject",
    "updateType": "class",
    "data": {
      "className": "UserService",
      "package": "com.example.service",
      "file": "UserService.kt",
      "type": "class",
      "purpose": "Manages user authentication and profile",
      "methods": ["login(email, password): Result", "logout(): Unit"]
    }
  }'
```

### 3. CLAUDE.md Integration

Added to global CLAUDE.md:

```markdown
## SESSION START (MANDATORY)
...
4. **MasterDocs (if exists):** Read these AI files for codebase context:
   - Docs/MasterDocs/AI/PLATFORM-INDEX.ai.md - Module overview
   - Docs/MasterDocs/AI/CLASS-INDEX.ai.md - Class reference
   - Docs/MasterDocs/AI/REFACTORING-GUIDE.ai.md - Code health status
...

## MASTERDOCS SYSTEM

### Mandatory Update Rules

**YOU MUST update MasterDocs when:**

| Event | Action Required |
|-------|-----------------|
| New class/interface created | **YOU MUST** add to CLASS-INDEX.ai.md |
| New module added | **YOU MUST** update PLATFORM-INDEX.ai.md + create README.md |
| Public API changed | **YOU MUST** update PLATFORM-INDEX.ai.md |
| Bug/issue fixed | **YOU MUST** mark resolved in REFACTORING-GUIDE.ai.md |
| SOLID refactoring | **YOU MUST** update both guides |
```

---

## How to Add MasterDocs to Another Framework

### Step 1: Create Directory Structure

```bash
mkdir -p Docs/MasterDocs/{AI,LD}
```

### Step 2: Initialize via API (Recommended)

```bash
# Start the ideacode API server
cd ideacode/api && npm start

# Initialize MasterDocs
curl -X POST http://localhost:3850/masterdocs/init \
  -H "Content-Type: application/json" \
  -d '{
    "repo": "YourRepoName",
    "projectName": "Your Project",
    "modules": ["Module1", "Module2"]
  }'
```

### Step 3: Or Create Manually

Create these files with the templates from this guide:

1. `AI/PLATFORM-INDEX.ai.md` - Module registry
2. `AI/CLASS-INDEX.ai.md` - Class reference
3. `AI/REFACTORING-GUIDE.ai.md` - Code health
4. `LD/LD-MasterDocs-Protocol-V1.md` - Protocol reference
5. `{Module}/README.md` - For each module

### Step 4: Update Project CLAUDE.md

Add to your project's `.claude/CLAUDE.md`:

```markdown
## MASTERDOCS

Location: `/Docs/MasterDocs/`

### Session Start
Read AI docs for codebase context:
- AI/PLATFORM-INDEX.ai.md
- AI/CLASS-INDEX.ai.md
- AI/REFACTORING-GUIDE.ai.md

### Update Rules
- New class → Update CLASS-INDEX.ai.md
- New module → Update PLATFORM-INDEX.ai.md + create README.md
- Issue fixed → Mark resolved in REFACTORING-GUIDE.ai.md with commit
```

### Step 5: Symlink Global CLAUDE.md (Optional)

If using IDEACODE framework:

```bash
ln -sf /path/to/ideacode/claude/CLAUDE-MASTER.md .claude/CLAUDE.md
```

---

## Maintenance Workflow

### On Every Code Change

```
IF new class created:
  → Add entry to AI/CLASS-INDEX.ai.md

IF new module added:
  → Add entry to AI/PLATFORM-INDEX.ai.md
  → Create {Module}/README.md

IF bug/issue fixed:
  → Mark resolved in AI/REFACTORING-GUIDE.ai.md
  → Include commit hash as evidence

IF API changed:
  → Update API section in AI/PLATFORM-INDEX.ai.md
```

### On Feature Completion

1. Review all AI docs for accuracy
2. Update module README.md files
3. Regenerate HTML diagrams if architecture changed

### Periodic Audit

```bash
# Check MasterDocs completeness
curl "http://localhost:3850/masterdocs/status?repo=YourRepo"
```

---

## File Templates

### Minimal PLATFORM-INDEX.ai.md

```markdown
# PLATFORM-INDEX
# AI-Readable Platform Documentation Index
# Version: 1.0 | Updated: YYYY-MM-DD

---

## PLATFORM_METADATA
\`\`\`yaml
name: Project Name
type: project-type
status: development
\`\`\`

---

## MODULE_REGISTRY

### MAIN
\`\`\`yaml
id: main
path: /
type: library
status: development
purpose: Main module
key_classes: []
dependencies: []
capabilities: []
\`\`\`

---

# END PLATFORM-INDEX
```

### Minimal CLASS-INDEX.ai.md

```markdown
# CLASS-INDEX
# AI-Readable Class Reference
# Version: 1.0 | Updated: YYYY-MM-DD

---

## MAIN_CLASSES

(Add classes as development progresses)

---

# END CLASS-INDEX
```

### Minimal REFACTORING-GUIDE.ai.md

```markdown
# REFACTORING-GUIDE
# AI-Readable Refactoring Status
# Version: 1.0 | Updated: YYYY-MM-DD
# Status: NEW PROJECT

---

## CURRENT_STATUS

\`\`\`yaml
solid_compliance: N/A
critical_issues: 0
high_issues: 0
status: NEW
\`\`\`

---

## RESOLVED_ISSUES

(Track resolved issues here)

---

# END REFACTORING-GUIDE
```

---

## Benefits

1. **LLM Context** - Claude reads AI docs at session start, understands codebase immediately
2. **Code Health Tracking** - REFACTORING-GUIDE tracks issues and resolutions with commit evidence
3. **Dual Format** - Humans get readable READMEs, AI gets structured YAML
4. **API Integration** - Programmatic updates via REST API
5. **Living Documentation** - Mandatory update rules keep docs synchronized with code

---

## Commits That Added This System

| Commit | Description |
|--------|-------------|
| `082c121` | CLAUDE.md mandatory rules and session start |
| `d51d9f8` | MasterDocs API endpoints v19 |
| `f0149b8b` | MasterDocs protocol and instructions |
| `dd7dbed0` | AI documentation with SOLID resolved issues |

---

*This is a Living Document. Update as the MasterDocs system evolves.*
