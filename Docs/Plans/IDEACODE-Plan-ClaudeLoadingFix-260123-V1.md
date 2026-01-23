# Implementation Plan: Fix Claude Code Loading Issues

## Overview
| Item | Value |
|------|-------|
| Platforms | All repos in /Volumes/M-Drive/Coding/ |
| Swarm Recommended | Yes (7 parallel cleanup tasks) |
| Estimated Tasks | 12 |
| Risk Level | Medium (config changes, no code deletion) |

## Problem Summary
Claude Code loads slowly or fails in various repos due to:
1. Git merge conflict file in `.claude/`
2. Competing frameworks: `ideacode/` (v12.1, port 3847) vs `CodeAvanue/` (v22.0.0, port 3850)
3. Inconsistent `settings.json` hooks across repos
4. API auto-start with 2-second blocking delay
5. Broken symlink chain in NewAvanues

## Constraints
- **KEEP** both terminal apps: `ideacode/ideaTerm/` and `TerminalAvenue/`
- **KEEP** `ideacode/` folder (contains active ideaTerm development)
- **KEEP** `CodeAvanue/` as source of truth for LLMI instructions

---

## Phase 1: Clean Conflict Files (P0 - Critical)

### Task 1.1: Remove Git Conflict Artifacts
**Location:** `/Volumes/M-Drive/Coding/.claude/`
**Action:** Delete conflict and backup files
```bash
rm "/Volumes/M-Drive/Coding/.claude/CLAUDE [conflicted].md"
rm "/Volumes/M-Drive/Coding/.claude/CLAUDE.md~origin_bugfix_webavanues-privacy-settings-issues"
```
**Risk:** None - these are stale conflict artifacts

### Task 1.2: Clean NewAvanues Backup Files
**Location:** `/Volumes/M-Drive/Coding/NewAvanues/.claude/`
**Action:** Remove old backup files cluttering the folder
```bash
rm /Volumes/M-Drive/Coding/NewAvanues/.claude/CLAUDE.md~HEAD
rm /Volumes/M-Drive/Coding/NewAvanues/.claude/settings.json.backup
rm /Volumes/M-Drive/Coding/NewAvanues/.claude/settings.json.bak.*
rm /Volumes/M-Drive/Coding/NewAvanues/.claude/settings.json.v85backup
rm /Volumes/M-Drive/Coding/NewAvanues/.claude/statusline-command.sh.bak.*
rm /Volumes/M-Drive/Coding/NewAvanues/.claude/statusline-command.sh.old.*
```

---

## Phase 2: Interactive API & Coordination Prompts (P0 - Critical)

### Task 2.1: Remove Auto-Start from Shell Hooks
**Location:** `/Volumes/M-Drive/Coding/.claude/startup-banner.sh`
**Action:** Remove API auto-start logic entirely - let Claude Code handle it interactively

**Before (lines 13-29):** Auto-starts API with blocking sleep
**After:** Only check status, report to Claude Code, no auto-start

```bash
# ============ API Status Check (NO AUTO-START) ============
API_PORT=3850
api_status="offline"
if curl -s --connect-timeout 1 --max-time 2 "http://localhost:${API_PORT}/health" >/dev/null 2>&1; then
    api_status="running"
fi
# Export for Claude Code to read
echo "IDEACODE_API_STATUS=$api_status"
```

### Task 2.2: Add Interactive Prompts to CLAUDE.md
**Location:** `/Volumes/M-Drive/Coding/CodeAvanue/LLMI/CLAUDE.md`
**Action:** Add session start instructions for Claude Code to ask user

**Add new section after "## Load Rules":**

```markdown
## Session Start Protocol

At the START of every new session, Claude Code MUST:

### Step 1: Check API Status
Run: `curl -s --connect-timeout 1 http://localhost:3850/health`

### Step 2: Ask User (use AskUserQuestion tool)

**If API is RUNNING:**
| Question | Options |
|----------|---------|
| "API is running on :3850. Use it for this session?" | Yes (Recommended), No - work offline |
| "Join terminal coordination?" | Yes - coordinate with other terminals (Recommended), No - work in isolation |

**If API is OFFLINE:**
| Question | Options |
|----------|---------|
| "API is offline. What would you like to do?" | Start API now (Recommended), Work offline, Skip for this session |
| "Join terminal coordination?" | Yes (Recommended), No - work in isolation |

### Step 3: Execute User Choice
| Choice | Action |
|--------|--------|
| Start API | Run: `cd /Volumes/M-Drive/Coding/CodeAvanue/api && nohup node dist/index.js > /tmp/codeavenue-api.log 2>&1 &` |
| Work offline | Set internal flag, skip API calls |
| Coordinate | Register terminal via POST /coordination/register |
| Isolation | Skip coordination, work independently |

### Step 4: Persist Choice
Store in CLAUDE_ENV_FILE:
- `IDEACODE_USE_API=1|0`
- `IDEACODE_COORDINATE=1|0`
```

### Task 2.3: Create Session Config File
**Location:** `/Volumes/M-Drive/Coding/.codeavenue/session-preferences.json`
**Action:** Store user's default preferences (can be overridden per session)

```json
{
  "api": {
    "autoStart": false,
    "askOnOffline": true,
    "defaultChoice": "ask"
  },
  "coordination": {
    "enabled": true,
    "askOnStart": true,
    "defaultMode": "ask"
  }
}
```

### Task 2.4: Update session-start.sh Hook
**Location:** `/Volumes/M-Drive/Coding/.claude/hooks/session-start.sh`
**Action:** Make it report status only, not auto-register

**New behavior:**
1. Check API status → output to stdout for Claude to read
2. Check coordination status → output active terminal count
3. Do NOT auto-register (Claude will do it after asking user)
4. Still check for handover files (non-blocking)

---

## Phase 3: Standardize Settings.json (P1 - High)

### Task 3.1: Create Standard Settings Template
**Location:** `/Volumes/M-Drive/Coding/CodeAvanue/.claude/settings.json.template`
**Action:** Create canonical settings template that all repos should use

```json
{
  "statusLine": {
    "type": "command",
    "command": "sh /Volumes/M-Drive/Coding/.claude/statusline-command.sh"
  },
  "hooks": {
    "SessionStart": [
      {
        "matcher": "startup",
        "hooks": [
          {
            "type": "command",
            "command": "/Volumes/M-Drive/Coding/.claude/hooks/session-start.sh"
          }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Write|Edit",
        "hooks": [
          {
            "type": "command",
            "command": "/Volumes/M-Drive/Coding/.claude/hooks/post-write.sh"
          }
        ]
      }
    ]
  }
}
```

### Task 3.2: Update NewAvanues Settings
**Location:** `/Volumes/M-Drive/Coding/NewAvanues/.claude/settings.json`
**Action:** Replace custom startup-banner.sh with standard session-start.sh
**Reason:** startup-banner.sh has the 2-second blocking delay

---

## Phase 4: Fix Symlink Chain (P1 - High)

### Task 4.1: Convert NewAvanues CLAUDE.md to Symlink
**Location:** `/Volumes/M-Drive/Coding/NewAvanues/.claude/CLAUDE.md`
**Current:** Regular file (752 bytes, "Lite" version)
**Action:** Replace with symlink to global CLAUDE.md

```bash
cd /Volumes/M-Drive/Coding/NewAvanues/.claude
rm CLAUDE.md
ln -s /Volumes/M-Drive/Coding/.claude/CLAUDE.md CLAUDE.md
```

**Alternative:** Keep lite version if NewAvanues needs different instructions
- Create `/Volumes/M-Drive/Coding/CodeAvanue/LLMI/CLAUDE-lite.md`
- Symlink NewAvanues to the lite version

---

## Phase 5: Isolate ideacode Framework (P2 - Medium)

### Task 5.1: Disable ideacode API References
**Location:** `/Volumes/M-Drive/Coding/ideacode/claude/CLAUDE.md`
**Action:** Update to reference CodeAvanue API (port 3850) instead of old port 3847

**Note:** The conflicted file referenced port 3847. The current live CLAUDE.md uses port 3850. No action needed if conflict file is removed.

### Task 5.2: Update ideacode .claude/settings.local.json
**Location:** `/Volumes/M-Drive/Coding/ideacode/.claude/settings.local.json`
**Action:** Ensure it points to CodeAvanue infrastructure, not local ideacode API

---

## Phase 6: Verify and Test (P2 - Medium)

### Task 6.1: Test Loading in Each Repo
**Repos to test:**
1. `/Volumes/M-Drive/Coding/` (root)
2. `/Volumes/M-Drive/Coding/NewAvanues/`
3. `/Volumes/M-Drive/Coding/CodeAvanue/`
4. `/Volumes/M-Drive/Coding/ideacode/`
5. `/Volumes/M-Drive/Coding/TerminalAvenue/`

**Test criteria:**
- Loads in < 3 seconds
- No API auto-start unless needed
- Correct CLAUDE.md loaded (check version in banner)

### Task 6.2: Document Final Configuration
**Location:** `/Volumes/M-Drive/Coding/docs/CLAUDE-CONFIG-MATRIX.md`
**Action:** Create matrix showing each repo's configuration

---

## Time Estimates

| Execution | Time |
|-----------|------|
| Sequential | ~70 min |
| Parallel (Swarm) | ~25 min |
| Savings | 45 min (64%) |

---

## Rollback Plan

If issues occur:
1. Restore conflict files from git: `git checkout -- ".claude/"`
2. Restore NewAvanues settings: `git checkout -- "NewAvanues/.claude/"`
3. Revert startup-banner.sh changes

---

## Task Summary

| # | Task | Priority | Est. Time |
|---|------|----------|-----------|
| 1.1 | Remove conflict files | P0 | 1 min |
| 1.2 | Clean backup files | P0 | 1 min |
| 2.1 | Remove auto-start from startup-banner.sh | P0 | 5 min |
| 2.2 | Add interactive prompts to CLAUDE.md | P0 | 10 min |
| 2.3 | Create session-preferences.json | P0 | 3 min |
| 2.4 | Update session-start.sh to report-only | P0 | 8 min |
| 3.1 | Create settings template | P1 | 5 min |
| 3.2 | Update NewAvanues settings | P1 | 3 min |
| 4.1 | Fix NewAvanues symlink | P1 | 2 min |
| 5.1 | Verify ideacode CLAUDE.md | P2 | 3 min |
| 5.2 | Update ideacode settings | P2 | 3 min |
| 6.1 | Test all repos | P2 | 15 min |
| 6.2 | Document configuration | P2 | 10 min |

---

## Swarm Assignment (if approved)

| Agent | Tasks |
|-------|-------|
| Agent 1 | 1.1, 1.2 (cleanup conflict/backup files) |
| Agent 2 | 2.1, 2.4 (shell script updates) |
| Agent 3 | 2.2 (CLAUDE.md interactive prompts) |
| Agent 4 | 2.3, 3.1 (config files) |
| Agent 5 | 3.2, 4.1 (NewAvanues fixes) |
| Agent 6 | 5.1, 5.2 (ideacode isolation) |
| Agent 7 | 6.1, 6.2 (testing & docs) |

---

**Author:** Claude | **Version:** 1.0 | **Date:** 2026-01-23
