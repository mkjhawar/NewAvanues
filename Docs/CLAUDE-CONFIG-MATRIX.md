# Claude Code Configuration Matrix

**Updated:** 2026-01-23 | **Version:** 1.0

## Overview

This document shows the Claude Code configuration for each repository in `/Volumes/M-Drive/Coding/`.

## Configuration Matrix

| Repository | CLAUDE.md | settings.json | API | Coordination |
|------------|-----------|---------------|-----|--------------|
| `/Volumes/M-Drive/Coding/` (root) | symlink → CodeAvanue/LLMI | session-start.sh | :3850 | ask user |
| `NewAvanues/` | symlink → root | session-start.sh | :3850 | ask user |
| `CodeAvanue/` | symlink → LLMI | standard template | :3850 | ask user |
| `ideacode/` | local (v17.6) | local | :3850 | ask user |
| `TerminalAvenue/` | inherits root | inherits root | :3850 | ask user |
| `AvaConnect/` | symlink → root | inherits root | :3850 | ask user |

## Symlink Chain

```
repo/.claude/CLAUDE.md
    ↓
/Volumes/M-Drive/Coding/.claude/CLAUDE.md
    ↓
/Volumes/M-Drive/Coding/CodeAvanue/LLMI/CLAUDE.md  (SOURCE OF TRUTH)
```

## Session Start Behavior

### New Flow (v22.0.0+)

```
1. Hook runs session-start.sh (report only)
2. Claude Code checks API status
3. Claude Code asks user:
   - "Use API?" (if running) OR "Start API?" (if offline)
   - "Join coordination or work in isolation?"
4. Claude Code executes user choice
5. Session begins
```

### User Choices

| API Choice | Effect |
|------------|--------|
| Yes - use API | Enable API calls for memory, RAG, coordination |
| No - work offline | Disable API calls, use local tools only |
| Start API now | Start server, then enable API calls |
| Skip | Don't ask again this session |

| Coordination Choice | Effect |
|---------------------|--------|
| Coordinate | Register terminal, see other terminals, share ACTIVE-WORK.md |
| Isolate | No registration, work independently, no coordination warnings |

## Key Files

| File | Location | Purpose |
|------|----------|---------|
| CLAUDE.md | CodeAvanue/LLMI/ | Source of truth for LLM instructions |
| settings.json | .claude/ | Hook configuration per repo |
| settings.json.template | CodeAvanue/.claude/ | Standard template for new repos |
| session-preferences.json | .codeavenue/ | User default preferences |
| session-start.sh | .claude/hooks/ | Report-only startup hook |
| startup-banner.sh | .claude/ | Banner display (no auto-start) |

## Preferences File

Location: `/Volumes/M-Drive/Coding/.codeavenue/session-preferences.json`

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

## Troubleshooting

### Slow Loading

**Cause:** Old startup-banner.sh with auto-start + sleep delay
**Fix:** Ensure settings.json uses session-start.sh, not startup-banner.sh

### API Starts Automatically

**Cause:** Old configuration or startup-banner.sh
**Fix:**
1. Update to session-start.sh hook
2. Set `api.autoStart: false` in session-preferences.json

### Wrong CLAUDE.md Loading

**Cause:** Broken symlink or regular file instead of symlink
**Fix:**
```bash
cd repo/.claude
rm CLAUDE.md
ln -s /Volumes/M-Drive/Coding/.claude/CLAUDE.md CLAUDE.md
```

### Terminal Not Coordinating

**Cause:** User chose "isolate" or API offline
**Fix:** Start API and choose "coordinate" when prompted

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-23 | Initial matrix with interactive prompts |
