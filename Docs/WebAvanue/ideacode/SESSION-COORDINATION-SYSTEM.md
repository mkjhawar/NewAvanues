# MainAvanues Session Coordination System

**Version:** 1.0.0
**Date:** 2025-11-21
**Status:** Design Specification
**Purpose:** Enable seamless parallel AI development sessions with zero conflicts
**Integration:** MCP Server Instructions + IDEACODE Framework

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Session Lifecycle](#session-lifecycle)
4. [Module Locking System](#module-locking-system)
5. [Automated Sprint Commits](#automated-sprint-commits)
6. [Documentation Update Protocol](#documentation-update-protocol)
7. [Session Manager CLI](#session-manager-cli)
8. [MCP Integration](#mcp-integration)
9. [Conflict Resolution](#conflict-resolution)
10. [Best Practices](#best-practices)

---

## Overview

### The Problem

In a monorepo with multiple modules, parallel development by multiple AI sessions (or developers) can cause:
- ❌ Git merge conflicts
- ❌ Overlapping file modifications
- ❌ Branch collision
- ❌ Lost work from competing commits
- ❌ Incomplete documentation updates

### The Solution

**Coordinated Modular Development System** that provides:
- ✅ Automatic session registration and conflict detection
- ✅ Module-level locking to prevent overlaps
- ✅ Git worktree isolation for true parallel work
- ✅ Automated sprint-based commits with scoped staging
- ✅ Mandatory documentation updates before commits
- ✅ Session state tracking and recovery

---

## Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────────┐
│                  SESSION COORDINATION SYSTEM                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────┐  ┌──────────────┐  ┌──────────────────┐    │
│  │   Session     │  │   Module     │  │   Sprint Commit  │    │
│  │   Registry    │  │   Locking    │  │   Automation     │    │
│  └───────┬───────┘  └──────┬───────┘  └────────┬─────────┘    │
│          │                 │                    │              │
│  ┌───────▼─────────────────▼────────────────────▼─────────┐    │
│  │              Session Manager (CLI + MCP)              │    │
│  └───────────────────────────────────────────────────────┘    │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │          Documentation Update Enforcer              │   │
│  │  - Update developer manuals                              │   │
│  │  - Update user manuals                                   │   │
│  │  - Update implementation plans                           │   │
│  │  - Update TODO lists                                     │   │
│  │  - Update roadmaps                                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │          Git Worktree Manager                           │   │
│  │  - Auto-create isolated workspaces                      │   │
│  │  - Scoped commits per module                            │   │
│  │  - Auto-cleanup on session end                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Directory Structure

```
MainAvanues/
├── .ideacode/
│   ├── sessions/                      # Session coordination
│   │   ├── registry.json              # Active sessions registry
│   │   ├── active/                    # Active session markers
│   │   │   └── {session-id}           # Session metadata
│   │   ├── locks/                     # Module locks
│   │   │   └── {module-path}.lock     # Lock metadata
│   │   └── history/                   # Completed sessions
│   │       └── {date}/{session-id}.json
│   │
│   └── sprint-state/                  # Sprint tracking
│       ├── {session-id}/
│       │   ├── current-sprint.json    # Current sprint state
│       │   ├── completed-phases.json  # Completed work
│       │   └── pending-docs.json      # Documentation to update
│       └── templates/
│           └── sprint-commit-template.md
│
├── .claude/
│   └── commands/
│       ├── session.start.md           # Start coordinated session
│       ├── session.status.md          # Check session status
│       ├── session.end.md             # End session
│       ├── session.commit.md          # Sprint commit
│       └── session.sync.md            # Sync with other sessions
│
├── scripts/
│   ├── session-manager.sh             # Session CLI
│   ├── sprint-commit.sh               # Automated commit workflow
│   └── doc-update-enforcer.sh         # Documentation enforcement
│
├── docs/
│   ├── developer-manual/              # Developer documentation
│   │   ├── chapters/
│   │   │   ├── 01-getting-started.md
│   │   │   ├── 02-architecture.md
│   │   │   └── ...
│   │   └── README.md                  # Manual index
│   │
│   ├── user-manual/                   # User documentation
│   │   ├── chapters/
│   │   └── README.md
│   │
│   └── implementation-plans/          # Implementation tracking
│       ├── {module}/
│       │   ├── roadmap.md
│       │   ├── tasks.md
│       │   └── status.md
│       └── global-roadmap.md
│
└── /Volumes/M-Drive/Coding/MainAvanues-sessions/  # Worktrees
    ├── {session-id-1}/                # Isolated workspace 1
    ├── {session-id-2}/                # Isolated workspace 2
    └── ...
```

---

## Session Lifecycle

### Phase 1: Session Initialization

```
┌─────────────────────────────────────────────────────────────────┐
│  1. AI Request: "I want to work on WebAvanue dark mode"        │
└─────────────────┬───────────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. Session Manager: Check if WebAvanue is available           │
│     - Query registry.json                                       │
│     - Check locks/apps/webavanue.lock                          │
└─────────────────┬───────────────────────────────────────────────┘
                  │
                  ├─── Locked ──────────────────────────────┐
                  │                                          │
                  ▼                                          ▼
┌─────────────────────────────────────┐   ┌─────────────────────────────────┐
│  Module Available                   │   │  Module Locked                  │
│  ✅ Proceed                         │   │  ❌ Show conflict               │
└─────────────────┬───────────────────┘   │  - Locked by: session-xyz       │
                  │                       │  - Since: 2025-11-21 14:30     │
                  ▼                       │  - Options:                     │
┌─────────────────────────────────────┐   │    1. Wait                      │
│  3. Create Session                  │   │    2. Choose different module   │
│     - Generate session ID           │   │    3. Coordinate with owner     │
│     - Create feature branch         │   └─────────────────────────────────┘
│     - Setup git worktree            │
│     - Lock module                   │
│     - Register in registry.json     │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. Session Ready                                               │
│     - Worktree: /Volumes/.../MainAvanues-sessions/web-dark-mode │
│     - Branch: feature/webavanue/dark-mode                       │
│     - Scope: apps/webavanue/**                                  │
│     - Status: ACTIVE                                            │
└─────────────────────────────────────────────────────────────────┘
```

### Phase 2: Development Work (Sprint-Based)

```
Session Active → Sprint 1 → Sprint 2 → ... → Session End
                   │          │
                   │          ├── Sub-Phase 1
                   │          ├── Sub-Phase 2
                   │          └── Sub-Phase 3
                   │
                   └── Commit Checkpoint:
                       1. Stage module files
                       2. Update documentation
                       3. Update status files
                       4. Commit with sprint summary
                       5. Push to remote
```

### Phase 3: Sprint Commit (Automated)

**Triggered when:**
- ✅ AI completes a logical unit of work (sprint/phase/sub-phase)
- ✅ Feature implementation reaches checkpoint
- ✅ Tests pass for current work
- ✅ AI explicitly marks sprint complete

**Automated workflow:**

```
1. Detect Sprint Completion
   ↓
2. Identify Changed Files
   - Run: git status
   - Filter: Only files in module scope
   ↓
3. Stage Module Files
   - git add apps/webavanue/**/*.kt
   - git add apps/webavanue/**/*.xml
   - Exclude: Generated files, build artifacts
   ↓
4. Documentation Update (MANDATORY)
   ├── Developer Manual
   │   - Update affected chapters
   │   - Add new chapters if needed
   │   - Update API references
   ├── User Manual
   │   - Update user-facing features
   │   - Update screenshots if UI changed
   ├── Implementation Plan
   │   - Mark completed tasks
   │   - Update status.md
   │   - Update roadmap.md if scope changed
   └── TODO Lists
       - Mark completed todos
       - Add new todos discovered during work
   ↓
5. Stage Documentation
   - git add docs/developer-manual/chapters/{affected}.md
   - git add docs/user-manual/chapters/{affected}.md
   - git add docs/implementation-plans/{module}/status.md
   ↓
6. Generate Commit Message
   - Format: {type}({scope}): {summary}
   - Include: Sprint number, phase, work summary
   - Add: Co-authored-by if applicable
   ↓
7. Commit
   - git commit -m "{message}"
   - Create commit hash
   - Log in sprint-state/
   ↓
8. Push to Remote
   - git push origin {branch}
   - Verify push success
   ↓
9. Update Session State
   - Record sprint completion
   - Update last_commit timestamp
   - Update sprint counter
```

### Phase 4: Session End

```
1. Final Sprint Commit (if pending changes)
   ↓
2. Verify All Documentation Updated
   - Check for uncommitted doc changes
   - Warn if docs incomplete
   ↓
3. Create Session Summary
   - Total commits made
   - Modules modified
   - Documentation updated
   - Tests status
   ↓
4. Archive Session
   - Move to sessions/history/
   - Preserve metadata
   ↓
5. Cleanup
   - Remove git worktree
   - Remove module lock
   - Update registry.json
   ↓
6. Suggest Next Steps
   - Create PR
   - Continue in new session
   - Merge branch
```

---

## Module Locking System

### Lock Types

#### 1. Exclusive Lock
**Usage:** Full module ownership
**Scope:** Entire module directory
**Allows:** Only owning session can modify

```json
{
  "lock_type": "exclusive",
  "locked_by": "session-webavanue-20251121-143022",
  "module_path": "apps/webavanue",
  "lock_scope": ["apps/webavanue/**"],
  "locked_at": "2025-11-21T14:30:22Z"
}
```

#### 2. Partial Lock
**Usage:** Specific files within module
**Scope:** Individual files or subdirectories
**Allows:** Other sessions can modify unlocked files

```json
{
  "lock_type": "partial",
  "locked_by": "session-theme-20251121-150000",
  "module_path": "packages/theme-system",
  "lock_scope": [
    "packages/theme-system/DarkMode.kt",
    "packages/theme-system/ThemeManager.kt"
  ],
  "locked_at": "2025-11-21T15:00:00Z"
}
```

#### 3. Shared Lock (Read-Only)
**Usage:** Multiple sessions reading, none writing
**Scope:** Entire module (read-only)
**Allows:** Multiple readers, no writers

```json
{
  "lock_type": "shared",
  "locked_by": ["session-a", "session-b"],
  "module_path": "packages/core-utils",
  "lock_scope": ["packages/core-utils/**"],
  "access": "read-only",
  "locked_at": "2025-11-21T15:30:00Z"
}
```

### Lock Acquisition Protocol

```
Request Module Lock
  ↓
Check Current Locks
  ↓
  ├─ No Lock → Grant Exclusive Lock
  ├─ Exclusive Lock → DENY (conflict)
  ├─ Partial Lock → Check file overlap
  │   ├─ No overlap → Grant Partial Lock
  │   └─ Overlap → DENY (conflict)
  └─ Shared Lock → Upgrade to Exclusive? (requires coordination)
```

### Lock Metadata Schema

```json
{
  "module_path": "apps/webavanue",
  "lock_type": "exclusive",
  "locked_by": "session-webavanue-20251121-143022",
  "lock_scope": ["apps/webavanue/**"],
  "locked_at": "2025-11-21T14:30:22Z",
  "expires_at": null,
  "auto_release": false,
  "reason": "Implementing dark mode feature",
  "session_metadata": {
    "user": "manoj",
    "ai_agent": "claude-sonnet-4",
    "branch": "feature/webavanue/dark-mode",
    "worktree": "/Volumes/.../MainAvanues-sessions/web-dark-mode"
  }
}
```

---

## Automated Sprint Commits

### Sprint Definition

A **sprint** is a logical unit of work within a session:
- Implements a feature phase
- Completes a set of related tasks
- Reaches a stable checkpoint
- Typically 30-120 minutes of work

### Sprint Structure

```
Sprint
├── Phase 1: Foundation
│   ├── Sub-phase 1.1: Data models
│   ├── Sub-phase 1.2: API layer
│   └── Sub-phase 1.3: Tests
│       → COMMIT CHECKPOINT
│
├── Phase 2: UI Implementation
│   ├── Sub-phase 2.1: Components
│   ├── Sub-phase 2.2: Screens
│   └── Sub-phase 2.3: Integration
│       → COMMIT CHECKPOINT
│
└── Phase 3: Polish
    ├── Sub-phase 3.1: Error handling
    ├── Sub-phase 3.2: Performance
    └── Sub-phase 3.3: Documentation
        → COMMIT CHECKPOINT
```

### Commit Workflow

#### 1. Sprint Completion Detection

**AI signals completion via:**
```markdown
✅ Sprint 1, Phase 2, Sub-phase 2.2 complete
- Implemented BrowserScreen component
- Added navigation integration
- Tests passing (15/15)

📋 Ready for commit checkpoint
```

**Automated trigger:**
```bash
# AI calls MCP function
ideacode_sprint_commit({
  "session_id": "webavanue-20251121-143022",
  "sprint": 1,
  "phase": 2,
  "sub_phase": 2,
  "summary": "Implement BrowserScreen component with navigation"
})
```

#### 2. File Staging (Module-Scoped)

**Algorithm:**

```python
def stage_sprint_files(session_id, module_path):
    """
    Stage only files within module scope for commit
    """
    # Get changed files
    changed_files = git.status(['--porcelain'])

    # Filter by module scope
    module_files = [
        f for f in changed_files
        if f.startswith(module_path)
        and not is_excluded(f)
    ]

    # Exclude patterns
    excluded = [
        '**/*.class',
        '**/build/',
        '**/.gradle/',
        '**/bin/',
        '**/*.iml',
        '**/.DS_Store'
    ]

    # Stage files
    for file in module_files:
        git.add(file)

    return module_files
```

**Example:**

```bash
# Changed files in session
apps/webavanue/src/ui/BrowserScreen.kt
apps/webavanue/src/navigation/Navigator.kt
apps/webavanue/build/tmp/compileKotlin/cacheable/last-build.bin  # ❌ Excluded
packages/theme-system/ThemeManager.kt  # ❌ Outside module scope

# Staged for commit
git add apps/webavanue/src/ui/BrowserScreen.kt
git add apps/webavanue/src/navigation/Navigator.kt
```

#### 3. Documentation Update Enforcement

**MANDATORY before commit:**

```
┌─────────────────────────────────────────────────────────────────┐
│  Documentation Update Checklist                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Developer Manual:                                              │
│  [ ] Update affected chapters                                   │
│  [ ] Add new sections if needed                                 │
│  [ ] Update code examples                                       │
│  [ ] Update API references                                      │
│                                                                 │
│  User Manual:                                                   │
│  [ ] Update user-facing features                                │
│  [ ] Add screenshots if UI changed                              │
│  [ ] Update tutorials/guides                                    │
│                                                                 │
│  Implementation Plans:                                          │
│  [ ] Mark completed tasks in tasks.md                           │
│  [ ] Update status.md with progress                             │
│  [ ] Update roadmap.md if scope changed                         │
│  [ ] Add new discovered tasks                                   │
│                                                                 │
│  Status Files:                                                  │
│  [ ] Update TODO list                                           │
│  [ ] Update CHANGELOG                                           │
│  [ ] Update version numbers if applicable                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Enforcement workflow:**

```bash
# 1. Detect what changed
changed_components=$(analyze_changed_files "$module_path")

# 2. Identify affected documentation
affected_docs=$(map_code_to_docs "$changed_components")

# 3. Check if docs exist
for doc in $affected_docs; do
    if [ ! -f "$doc" ]; then
        create_doc_from_template "$doc"
    fi
done

# 4. AI updates documentation
# (AI is prompted to update each doc file)

# 5. Verify documentation updated
for doc in $affected_docs; do
    if ! git diff --cached --quiet "$doc"; then
        echo "✅ $doc updated"
    else
        echo "⚠️  $doc NOT updated - blocking commit"
        exit 1
    fi
done

# 6. Stage documentation
git add docs/**
```

**Documentation mapping:**

```yaml
# .ideacode/sessions/doc-mapping.yml
mappings:
  - code_pattern: "apps/webavanue/src/ui/**"
    docs:
      - "docs/developer-manual/chapters/05-ui-components.md"
      - "docs/user-manual/chapters/03-interface.md"

  - code_pattern: "apps/webavanue/src/navigation/**"
    docs:
      - "docs/developer-manual/chapters/06-navigation.md"
      - "docs/implementation-plans/webavanue/tasks.md"

  - code_pattern: "packages/avamagic-ui/**"
    docs:
      - "docs/developer-manual/chapters/10-avamagic-framework.md"
      - "docs/api-reference/avamagic-ui.md"
```

#### 4. Commit Message Generation

**Template:**

```
{type}({scope}): {summary}

Sprint: {sprint_number}
Phase: {phase_number}.{sub_phase_number}
Module: {module_path}

Changes:
- {change_1}
- {change_2}
- {change_3}

Documentation Updated:
- {doc_1}
- {doc_2}

Tests: {tests_status}
Coverage: {coverage_percentage}%

Session: {session_id}
```

**Example:**

```
feat(webavanue): implement BrowserScreen with navigation

Sprint: 1
Phase: 2.2
Module: apps/webavanue

Changes:
- Add BrowserScreen composable with WebView integration
- Implement navigation between tabs
- Add address bar state management
- Handle back/forward navigation

Documentation Updated:
- docs/developer-manual/chapters/05-ui-components.md
- docs/user-manual/chapters/03-browser-interface.md
- docs/implementation-plans/webavanue/tasks.md
- docs/implementation-plans/webavanue/status.md

Tests: 15/15 passing
Coverage: 92%

Session: webavanue-20251121-143022
```

#### 5. Commit Execution

```bash
# Generate commit message
commit_msg=$(generate_sprint_commit_message \
    --session "$session_id" \
    --sprint "$sprint_num" \
    --phase "$phase_num" \
    --module "$module_path")

# Create commit
git commit -F - <<EOF
$commit_msg
EOF

# Verify commit created
commit_hash=$(git rev-parse HEAD)

# Log commit in session state
echo "{
  \"commit_hash\": \"$commit_hash\",
  \"sprint\": $sprint_num,
  \"phase\": $phase_num,
  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
  \"files_changed\": $(git diff-tree --no-commit-id --name-only -r HEAD | wc -l),
  \"docs_updated\": [$(git diff-tree --no-commit-id --name-only -r HEAD | grep '^docs/' | jq -R -s -c 'split("\n")[:-1]')]
}" >> .ideacode/sprint-state/$session_id/completed-phases.json

echo "✅ Sprint commit: $commit_hash"
```

#### 6. Push to Remote

```bash
# Push to origin
git push origin "$branch_name"

# Verify push
if [ $? -eq 0 ]; then
    echo "✅ Pushed to origin/$branch_name"

    # Update session state
    update_session_state "$session_id" "last_push" "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
else
    echo "❌ Push failed - manual intervention required"
    exit 1
fi
```

---

## Documentation Update Protocol

### Mandatory Documentation Files

Every sprint commit MUST update relevant documentation:

#### 1. Developer Manual

**Location:** `docs/developer-manual/chapters/`

**When to update:**
- ✅ New API added
- ✅ Component architecture changed
- ✅ New module created
- ✅ Build process modified
- ✅ Dependencies added/changed

**Update checklist:**
```markdown
- [ ] Update chapter introduction if scope changed
- [ ] Add new sections for new features
- [ ] Update code examples to reflect changes
- [ ] Update diagrams if architecture changed
- [ ] Add troubleshooting notes for common issues
- [ ] Update API reference tables
```

**Example update:**

```markdown
# Chapter 5: UI Components

## 5.3 BrowserScreen Component

**Added:** 2025-11-21 (Sprint 1, Phase 2.2)

The `BrowserScreen` component provides the main browser interface with
integrated navigation and WebView rendering.

### Usage

\`\`\`kotlin
@Composable
fun BrowserScreen(
    tabs: List<Tab>,
    currentTabId: String,
    onTabChange: (String) -> Unit
) {
    // Implementation details...
}
\`\`\`

### Architecture

[Diagram of BrowserScreen component tree]

### Related Components
- AddressBar (§5.1)
- TabBar (§5.2)
- CommandBar (§5.4)
```

#### 2. User Manual

**Location:** `docs/user-manual/chapters/`

**When to update:**
- ✅ User-visible feature added
- ✅ UI changed
- ✅ User workflow modified
- ✅ Settings added/changed

**Update checklist:**
```markdown
- [ ] Add feature description in user-friendly language
- [ ] Include screenshots of new UI
- [ ] Update step-by-step tutorials
- [ ] Add tips and tricks section
- [ ] Update FAQ if needed
```

#### 3. Implementation Plans

**Location:** `docs/implementation-plans/{module}/`

**Files to update:**
- `tasks.md` - Mark completed, add new tasks
- `status.md` - Update progress metrics
- `roadmap.md` - Adjust timeline if scope changed

**Update checklist:**
```markdown
- [ ] Mark completed tasks with ✅
- [ ] Update percentage complete
- [ ] Add newly discovered tasks
- [ ] Update blockers/risks if any
- [ ] Adjust timeline if behind/ahead schedule
```

**Example update:**

```markdown
# WebAvanue Implementation Status

**Last Updated:** 2025-11-21 15:45:22 (Sprint 1, Phase 2.2)

## Progress

- Overall: 45% complete (was 30%)
- Sprint 1: 90% complete
  - Phase 1: ✅ Complete
  - Phase 2: 🟡 90% (Sub-phase 2.3 remaining)
  - Phase 3: 📋 Not started

## Completed This Sprint

- ✅ BrowserScreen component implementation
- ✅ Navigation integration
- ✅ Tab management UI

## Pending Tasks

- 📋 Command bar voice integration
- 📋 History tracking
- 📋 Bookmark management

## Discovered Tasks (Added This Sprint)

- 📋 Implement WebView lifecycle management
- 📋 Add error page for failed loads
- 📋 Optimize tab switching animation
```

#### 4. TODO Lists

**Location:** Module root or `.ideacode/sprint-state/{session-id}/`

**Update checklist:**
```markdown
- [ ] Mark completed TODOs
- [ ] Remove obsolete TODOs
- [ ] Add new TODOs discovered during implementation
- [ ] Update priority if needed
```

#### 5. Changelog

**Location:** `CHANGELOG.md` (module or repo root)

**Format:** Keep-a-Changelog standard

```markdown
# Changelog

## [Unreleased]

### Added (Sprint 1, Phase 2.2)
- BrowserScreen component with WebView integration
- Navigation system for tab switching
- Address bar state management

### Changed
- Refactored TabBar to support dynamic tab creation

### Fixed
- WebView layout overflow issue (#123)
```

---

## Session Manager CLI

### Commands

#### 1. Start Session

```bash
./scripts/session-manager.sh start <module> <description>
```

**Example:**
```bash
./scripts/session-manager.sh start webavanue "dark mode implementation"
```

**Output:**
```
🚀 Starting new session...
   Session ID: webavanue-20251121-143022
   Module: apps/webavanue
   Branch: feature/webavanue/dark-mode
   Worktree: /Volumes/M-Drive/Coding/MainAvanues-sessions/webavanue-20251121-143022

✅ Session started successfully!

📋 Next Steps:
   1. Open new terminal or Claude Code instance
   2. Run: cd /Volumes/M-Drive/Coding/MainAvanues-sessions/webavanue-20251121-143022
   3. Run: source .session-env
   4. Start development on apps/webavanue

💡 Tip: All changes will auto-scope to apps/webavanue
💡 Other sessions cannot modify this module until you end the session
```

#### 2. Session Status

```bash
./scripts/session-manager.sh status
```

**Output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Active Sessions (2)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Session: webavanue-20251121-143022
  Module: apps/webavanue
  Branch: feature/webavanue/dark-mode
  Worktree: /Volumes/.../webavanue-20251121-143022
  Description: dark mode implementation
  Started: 2025-11-21T14:30:22Z
  Commits: 3
  Last Activity: 2025-11-21T15:45:10Z

Session: avamagic-forms-20251121-143530
  Module: packages/avamagic-ui
  Branch: feature/avamagic/forms-validation
  Worktree: /Volumes/.../avamagic-forms-20251121-143530
  Description: form validation system
  Started: 2025-11-21T14:35:30Z
  Commits: 2
  Last Activity: 2025-11-21T15:44:55Z

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 Locked Modules:
  apps/webavanue → locked by webavanue-20251121-143022
  packages/avamagic-ui → locked by avamagic-forms-20251121-143530
```

#### 3. Sprint Commit

```bash
./scripts/session-manager.sh commit <session-id> [sprint] [phase]
```

**Example:**
```bash
./scripts/session-manager.sh commit webavanue-20251121-143022 1 2.2
```

**Workflow:**
```
1. Analyzing changed files...
   Found 12 files in apps/webavanue

2. Staging module-scoped files...
   ✅ apps/webavanue/src/ui/BrowserScreen.kt
   ✅ apps/webavanue/src/navigation/Navigator.kt
   ✅ apps/webavanue/src/state/NavigationState.kt
   ...

3. Checking documentation updates...
   📝 Required updates:
      - docs/developer-manual/chapters/05-ui-components.md
      - docs/user-manual/chapters/03-interface.md
      - docs/implementation-plans/webavanue/tasks.md
      - docs/implementation-plans/webavanue/status.md

4. Waiting for AI to update documentation...
   [AI prompted to update each doc file]

5. Verifying documentation updates...
   ✅ docs/developer-manual/chapters/05-ui-components.md
   ✅ docs/user-manual/chapters/03-interface.md
   ✅ docs/implementation-plans/webavanue/tasks.md
   ✅ docs/implementation-plans/webavanue/status.md

6. Staging documentation...
   ✅ 4 documentation files staged

7. Generating commit message...
   ✅ Message generated (Sprint 1, Phase 2.2)

8. Creating commit...
   ✅ Commit: a3f7d9e2

9. Pushing to origin/feature/webavanue/dark-mode...
   ✅ Pushed successfully

✅ Sprint commit complete!
   Commit: a3f7d9e2
   Files: 16 changed (12 code, 4 docs)
   Branch: feature/webavanue/dark-mode
```

#### 4. End Session

```bash
./scripts/session-manager.sh end <session-id>
```

**Example:**
```bash
./scripts/session-manager.sh end webavanue-20251121-143022
```

**Output:**
```
🛑 Ending session: webavanue-20251121-143022
   Module: apps/webavanue
   Branch: feature/webavanue/dark-mode

⚠️  Uncommitted changes detected
Commit message (or press Enter to skip): Final polish and cleanup

✅ Changes committed and pushed

📊 Session Summary:
   - Duration: 3h 15m
   - Commits: 4
   - Files Modified: 28
   - Documentation Updated: 6 files
   - Tests: 42/42 passing

✅ Session ended

📋 Next Steps:
   1. Merge your branch: gh pr create --base main --head feature/webavanue/dark-mode
   2. Or continue work: ./scripts/session-manager.sh start webavanue "additional features"
```

#### 5. Check Conflicts

```bash
./scripts/session-manager.sh check <module>
```

**Example:**
```bash
./scripts/session-manager.sh check webavanue
```

**Output if available:**
```
✅ No conflicts - module apps/webavanue is available
```

**Output if locked:**
```
❌ Conflict detected!
   Module 'apps/webavanue' is locked by: webavanue-20251121-143022

   Session: webavanue-20251121-143022
   Started: 2025-11-21T14:30:22Z
   Description: dark mode implementation
   Worktree: /Volumes/.../webavanue-20251121-143022

💡 Options:
   1. Work on a different module
   2. Wait for session to end
   3. Coordinate with session owner
   4. Force end session: ./scripts/session-manager.sh end webavanue-20251121-143022
```

---

## MCP Integration

### MCP Functions

Add these functions to the IDEACODE MCP server:

#### 1. `ideacode_session_start`

```typescript
{
  name: "ideacode_session_start",
  description: "Start a coordinated development session with module locking",
  parameters: {
    module: {
      type: "string",
      description: "Module to work on (e.g., 'webavanue', 'avamagic-ui')",
      required: true
    },
    description: {
      type: "string",
      description: "Brief description of work (e.g., 'dark mode implementation')",
      required: true
    },
    project_path: {
      type: "string",
      description: "Path to project (optional, auto-detect)",
      required: false
    }
  }
}
```

**Returns:**
```json
{
  "session_id": "webavanue-20251121-143022",
  "module_path": "apps/webavanue",
  "branch": "feature/webavanue/dark-mode",
  "worktree": "/Volumes/.../MainAvanues-sessions/webavanue-20251121-143022",
  "locked": true,
  "status": "active"
}
```

#### 2. `ideacode_sprint_commit`

```typescript
{
  name: "ideacode_sprint_commit",
  description: "Automated sprint commit with documentation enforcement",
  parameters: {
    session_id: {
      type: "string",
      description: "Active session ID",
      required: true
    },
    sprint: {
      type: "number",
      description: "Sprint number",
      required: true
    },
    phase: {
      type: "string",
      description: "Phase number (e.g., '2.2' for Phase 2, Sub-phase 2)",
      required: true
    },
    summary: {
      type: "string",
      description: "Brief summary of work completed",
      required: true
    },
    force_docs: {
      type: "boolean",
      description: "Skip documentation update check (NOT RECOMMENDED)",
      required: false,
      default: false
    }
  }
}
```

**Workflow:**
1. Stage module files
2. Check documentation requirements
3. **Pause and prompt AI** to update docs
4. Verify docs updated
5. Stage documentation
6. Generate commit message
7. Create commit
8. Push to remote
9. Return commit hash

**Returns:**
```json
{
  "success": true,
  "commit_hash": "a3f7d9e2",
  "files_changed": 16,
  "docs_updated": [
    "docs/developer-manual/chapters/05-ui-components.md",
    "docs/user-manual/chapters/03-interface.md",
    "docs/implementation-plans/webavanue/tasks.md",
    "docs/implementation-plans/webavanue/status.md"
  ],
  "pushed": true
}
```

#### 3. `ideacode_session_status`

```typescript
{
  name: "ideacode_session_status",
  description: "Get status of active sessions",
  parameters: {
    project_path: {
      type: "string",
      description: "Path to project (optional)",
      required: false
    }
  }
}
```

**Returns:**
```json
{
  "active_sessions": [
    {
      "session_id": "webavanue-20251121-143022",
      "module": "apps/webavanue",
      "branch": "feature/webavanue/dark-mode",
      "started": "2025-11-21T14:30:22Z",
      "commits": 3,
      "last_activity": "2025-11-21T15:45:10Z"
    }
  ],
  "locked_modules": {
    "apps/webavanue": "webavanue-20251121-143022",
    "packages/avamagic-ui": "avamagic-forms-20251121-143530"
  }
}
```

#### 4. `ideacode_session_end`

```typescript
{
  name: "ideacode_session_end",
  description: "End a development session",
  parameters: {
    session_id: {
      type: "string",
      description: "Session ID to end",
      required: true
    },
    final_commit_message: {
      type: "string",
      description: "Optional final commit message",
      required: false
    }
  }
}
```

#### 5. `ideacode_check_module_lock`

```typescript
{
  name: "ideacode_check_module_lock",
  description: "Check if module is available for work",
  parameters: {
    module: {
      type: "string",
      description: "Module to check (e.g., 'webavanue')",
      required: true
    }
  }
}
```

**Returns:**
```json
{
  "available": false,
  "locked_by": "webavanue-20251121-143022",
  "lock_type": "exclusive",
  "locked_since": "2025-11-21T14:30:22Z",
  "session_details": {
    "description": "dark mode implementation",
    "user": "manoj"
  }
}
```

---

## Conflict Resolution

### Conflict Scenarios

#### Scenario 1: Module Already Locked

**Situation:**
- Session A: Working on `apps/webavanue`
- Session B: Tries to start work on `apps/webavanue`

**Resolution:**
```
Session B receives:
  ❌ Module 'apps/webavanue' is locked by session-A

  Options:
  1. Wait for Session A to complete
  2. Work on different module
  3. Request coordination with Session A
  4. Force-end Session A (requires approval)
```

#### Scenario 2: Shared File Modification

**Situation:**
- Session A: Working on `packages/theme-system` (partial lock on `DarkMode.kt`)
- Session B: Wants to modify `ThemeManager.kt` in same package

**Resolution:**
```
✅ Allowed - files don't overlap

Session B gets partial lock:
  - Lock: packages/theme-system/ThemeManager.kt
  - Session A retains: packages/theme-system/DarkMode.kt
```

#### Scenario 3: Documentation Conflict

**Situation:**
- Both sessions modify same developer manual chapter

**Resolution:**
```
Last commit wins, but warning issued:

⚠️  Documentation conflict detected
    File: docs/developer-manual/chapters/05-ui-components.md
    Modified by: session-A (2 minutes ago)

    Recommendation:
    1. Pull latest changes
    2. Merge manually
    3. Re-commit
```

#### Scenario 4: Stale Session

**Situation:**
- Session has no activity for >4 hours

**Resolution:**
```
Auto-cleanup after 4 hours idle:
  1. Warn session owner (if detectable)
  2. Wait 15 minutes
  3. Auto-commit pending changes with note
  4. Archive session
  5. Release locks
```

---

## Best Practices

### For AI Agents

1. **Always start sessions explicitly**
   ```
   Before work: ideacode_session_start("webavanue", "dark mode")
   ```

2. **Commit at logical checkpoints**
   - After completing a feature phase
   - Before switching contexts
   - Every 30-60 minutes of work
   - When tests all pass

3. **Update documentation BEFORE committing**
   - Developer manual for API changes
   - User manual for UI changes
   - Status files for progress
   - Never skip documentation

4. **Scope commits to module**
   - Only stage files in your module
   - Don't include generated files
   - Don't include build artifacts

5. **End sessions when done**
   - Don't leave sessions hanging
   - Always end explicitly
   - Create PR or merge branch

### For Multi-Session Coordination

1. **Check status before starting**
   ```bash
   ./scripts/session-manager.sh status
   ```

2. **Communicate via session descriptions**
   - Use clear, descriptive names
   - Include ticket/issue numbers
   - Mention dependencies

3. **Plan module boundaries**
   - Design features to minimize overlap
   - Extract shared code to packages
   - Keep modules loosely coupled

4. **Coordinate shared package changes**
   - If must modify shared package:
     - Create separate session for package
     - Merge package changes first
     - Rebase app sessions

---

## Implementation Checklist

### Phase 1: Core Infrastructure
- [ ] Create session registry schema
- [ ] Implement session-manager.sh CLI
- [ ] Create git worktree automation
- [ ] Implement module locking system
- [ ] Add session status tracking

### Phase 2: Sprint Commit Automation
- [ ] Create sprint-commit.sh script
- [ ] Implement module-scoped file staging
- [ ] Create documentation mapping system
- [ ] Implement doc-update-enforcer.sh
- [ ] Add commit message generation
- [ ] Add auto-push functionality

### Phase 3: MCP Integration
- [ ] Add ideacode_session_start MCP function
- [ ] Add ideacode_sprint_commit MCP function
- [ ] Add ideacode_session_status MCP function
- [ ] Add ideacode_session_end MCP function
- [ ] Add ideacode_check_module_lock MCP function
- [ ] Update MCP server documentation

### Phase 4: Documentation Templates
- [ ] Create developer manual chapter template
- [ ] Create user manual chapter template
- [ ] Create implementation plan templates
- [ ] Create status file templates
- [ ] Create commit message templates

### Phase 5: Testing & Validation
- [ ] Test single session workflow
- [ ] Test parallel sessions (no overlap)
- [ ] Test conflict detection
- [ ] Test documentation enforcement
- [ ] Test sprint commit automation
- [ ] Test session recovery from failure

### Phase 6: Documentation
- [ ] Write user guide for session system
- [ ] Write developer guide for MCP integration
- [ ] Create troubleshooting guide
- [ ] Add examples and tutorials

---

## Example Workflows

### Workflow 1: Single Developer, Multiple Features

```bash
# Morning: Start WebAvanue work
./scripts/session-manager.sh start webavanue "dark mode"
cd /Volumes/.../MainAvanues-sessions/webavanue-*/
source .session-env

# Work on dark mode...
# Sprint commit at logical checkpoint
ideacode_sprint_commit(...) # Via AI

# Afternoon: Switch to AVAMagic work
./scripts/session-manager.sh end webavanue-*
./scripts/session-manager.sh start avamagic-ui "forms"
cd /Volumes/.../MainAvanues-sessions/avamagic-*/
source .session-env

# Work on forms...
ideacode_sprint_commit(...) # Via AI

# End of day
./scripts/session-manager.sh end avamagic-*
```

### Workflow 2: Two AI Terminals in Parallel

```bash
# Terminal 1: WebAvanue
./scripts/session-manager.sh start webavanue "dark mode"
# AI works on apps/webavanue/**

# Terminal 2: AVAMagic (simultaneous)
./scripts/session-manager.sh start avamagic-ui "forms"
# AI works on packages/avamagic-ui/**

# No conflicts! Different modules.
# Both can commit and push independently.
```

### Workflow 3: Coordinated Shared Package Update

```bash
# Terminal 1: Need to update theme-system package
./scripts/session-manager.sh start theme-system "api-update"
# Work on packages/theme-system/**
# Complete and merge first

# Terminal 2: WebAvanue depends on theme-system
# Wait for theme-system merge
git pull origin main  # Get theme updates
./scripts/session-manager.sh start webavanue "dark-mode"
# Now work with updated theme-system
```

---

## Monitoring & Observability

### Session Metrics

Track in `.ideacode/sessions/metrics.json`:

```json
{
  "total_sessions": 42,
  "active_sessions": 2,
  "avg_session_duration_minutes": 145,
  "total_commits": 187,
  "avg_commits_per_session": 4.5,
  "conflicts_detected": 3,
  "conflicts_resolved": 3,
  "documentation_compliance": "98%",
  "modules_with_sessions": [
    "apps/webavanue",
    "packages/avamagic-ui"
  ]
}
```

### Health Checks

```bash
# Check for stale sessions
./scripts/session-manager.sh health

Output:
✅ 2 active sessions (healthy)
⚠️  1 idle session >2 hours (webavanue-20251121-080000)
✅ All locks valid
✅ All worktrees accessible
```

---

## Future Enhancements

### v1.1: AI-to-AI Communication
- Sessions can send messages to each other
- Request permission to modify shared files
- Notify about completed work

### v1.2: Automatic Conflict Resolution
- AI suggests merge strategies
- Auto-rebase dependent branches
- Smart documentation merging

### v1.3: Session Templates
- Pre-configured sessions for common tasks
- Template-based documentation updates
- Workflow automation

### v1.4: Cross-Project Sessions
- Work across multiple monorepos
- Coordinate between MainAvanues and external libs
- Unified session registry

---

## Conclusion

This Session Coordination System enables **true parallel development** in MainAvanues monorepo while maintaining:

- ✅ **Zero conflicts** via module locking
- ✅ **Clean git history** via scoped commits
- ✅ **Complete documentation** via mandatory updates
- ✅ **Automated workflows** via sprint commits
- ✅ **Seamless experience** via MCP integration

**Ready for implementation.**

---

**Author:** IDEACODE Framework
**Version:** 1.0.0
**Last Updated:** 2025-11-21
**License:** Proprietary (MainAvanues Project)
