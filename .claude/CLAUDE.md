# NewAvanues - Project Instructions

NewAvanues - Platform-centric KMP monorepo.

---

## IDEACODE API (PREFERRED - 97% TOKEN SAVINGS)

API URL: `http://localhost:3847`
Check: `curl -s http://localhost:3847/health`

| When API Running | Use |
|------------------|-----|
| /i.think, /i.analyze, /i.research | API (read-only, cacheable) |
| /i.specify, /i.plan, /i.review | API (planning phases) |
| /i.develop, /i.fix, /i.implement | MCP (needs file access) |

Start: `cd /Volumes/M-Drive/Coding/ideacode/ideacode-api && npm start`

---

## MANDATORY: Before Any File Operation

1. **READ REGISTRIES FIRST:**
   - `.ideacode/Registries/FOLDER-REGISTRY.md` - Folder structure
   - `.ideacode/Registries/Modules.registry.json` - Module paths
   - `.ideacode/Registries/Docs.registry.json` - Doc paths

2. **Use correct case:**
   - Gradle paths: `lowercase` (android/, ios/, desktop/)
   - All other folders: `PascalCase` (Common/, Docs/, Shared/)

---

## Monorepo Structure

| Path | Purpose | Case |
|------|---------|------|
| `android/` | Android apps | lowercase (Gradle) |
| `ios/` | iOS apps | lowercase (Gradle) |
| `desktop/` | Desktop apps | lowercase (Gradle) |
| `Common/` | KMP shared libraries | PascalCase |
| `Modules/` | Feature modules | PascalCase |
| `Shared/` | Assets, configs | PascalCase |
| `Docs/` | Documentation | PascalCase |

---

## Apps

| App | Platforms | Android Path | Docs Path |
|-----|-----------|--------------|-----------|
| VoiceOS | android, ios, desktop | `android/voiceos/` | `Docs/VoiceOS/` |
| AVA | android, ios, macos, win, linux, web | `android/ava/` | `Docs/AVA/` |
| Avanues | android | `android/avanues/` | `Docs/Avanues/` |
| AvaConnect | android, web | `android/avaconnect/` | `Docs/AvaConnect/` |

---

## Common Libraries

| Library | Path | Description |
|---------|------|-------------|
| Database | `Common/Database/` | SQLDelight |
| Voice | `Common/Voice/` | Voice recognition |
| NLU | `Common/NLU/` | NLU engine |
| UI | `Common/UI/` | MagicUI |
| Utils | `Common/Utils/` | Utilities |

---

## Documentation Structure

| Path | Purpose |
|------|---------|
| `Docs/Project/` | Living docs (LD-*.md) |
| `Docs/{App}/MasterSpecs/` | Universal specs |
| `Docs/{App}/Platform/{Platform}/` | Platform-specific |
| `Docs/Common/{Domain}/` | Shared lib docs |

---

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Documents | App-Module-Description-YDDMMHH-V#.md | `VoiceOS-NLU-Integration-5031215-V1.md` |
| Living Docs | LD-App-Module-Description-V#.md | `LD-VoiceOS-Architecture-V1.md` |
| Specs | App-Spec-Feature-YDDMM-V#.md | `AVA-Spec-VoiceCommands-50312-V1.md` |
| Plans | App-Plan-Feature-YDDMM-V#.md | `AVA-Plan-Migration-50312-V1.md` |

### Date Formats

| Format | Use | Parts |
|--------|-----|-------|
| YDDMMHH | Documents | Y + DD + MM + HH |
| YDDMM | Specs, Plans | Y + DD + MM |

Y = last digit of year (5 for 2025), DD = day, MM = month, HH = hour

---

## Forbidden Actions

| Action | Why |
|--------|-----|
| Create folder not in registry | Causes sprawl |
| Use lowercase for non-Gradle paths | Inconsistent |
| Use PascalCase for Gradle paths | Gradle breaks |
| Put docs in wrong App/Platform folder | Hard to find |
| Skip MasterSpecs | Missing universal docs |
| Create file without checking registry | May duplicate |

---

## Key Rules

| Rule | Requirement |
|------|-------------|
| Registry First | ALWAYS check registries before creating files |
| Clean Architecture | SOLID principles, no legacy code |
| Cross-Platform | Design for all platforms from start |
| No Delete | Never delete without approval + pros/cons |

---

## Inherited Rules

All rules from `/Volumes/M-Drive/Coding/.claude/CLAUDE.md` apply.

---

**Updated:** 2025-12-06 | **Version:** 10.3
