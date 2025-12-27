# Folder Registry (Living Document)

## Purpose
Tracks all folders with structure and purpose.
**Prevents:** Duplicate folders, incorrect nesting, orphaned directories.
**MANDATORY:** Claude MUST read this registry before creating/saving files.

---

## CRITICAL FILE LOCATIONS (READ FIRST)

| File Type | Location | Pattern |
|-----------|----------|---------|
| Global CLAUDE.md | `/Volumes/M-Drive/Coding/.claude/CLAUDE.md` | Applies to ALL projects |
| Project CLAUDE.md | `{project}/.claude/CLAUDE.md` | Project-specific rules |
| Context Saves | `{project}/contextsave/` | `con-{app}-{module}-{desc}-{YYYYMMDD}.md` |
| Living Docs | `{project}/.ideacode/living-docs/` | `LD-App-Module-Description-V#.md` |
| Documents | `{project}/Docs/` | `App-Module-Description-YDDMMHH-V#.md` |
| Specifications | `{project}/Docs/{App}/MasterSpecs/` | `App-Spec-Feature-YDDMM-V#.md` |
| Plans | `{project}/Docs/{App}/MasterSpecs/` | `App-Plan-Feature-YDDMM-V#.md` |
| Commands | `/Volumes/M-Drive/Coding/.claude/commands/` | GLOBAL ONLY |

---

## CLAUDE.md REGISTRY (All Repos)

| Repo | Path | Type |
|------|------|------|
| Global | `/Volumes/M-Drive/Coding/.claude/CLAUDE.md` | Global rules |
| ideacode | `/Volumes/M-Drive/Coding/ideacode/.claude/CLAUDE.md` | Framework |
| AVA | `/Volumes/M-Drive/Coding/AVA/.claude/CLAUDE.md` | Android App |
| AvaConnect | `/Volumes/M-Drive/Coding/AvaConnect/.claude/CLAUDE.md` | Android App |
| Avanues | `/Volumes/M-Drive/Coding/Avanues/.claude/CLAUDE.md` | Android Platform |
| VoiceOS | `/Volumes/M-Drive/Coding/VoiceOS/.claude/CLAUDE.md` | Voice OS |
| NewAvanues | `/Volumes/M-Drive/Coding/NewAvanues/.claude/CLAUDE.md` | Monorepo |
| MainAvanues | `/Volumes/M-Drive/Coding/MainAvanues/.claude/CLAUDE.md` | Monorepo |

**FORBIDDEN LOCATIONS:**
- `.claude-context-saves/` - DO NOT USE
- `.ideacode/continuity/` - DEPRECATED, use `contextsave/`

---

## Monorepo Root Structure

| Path | Purpose |
|------|---------|
| android/apps/ | Android applications (lowercase - Gradle) |
| ios/apps/ | iOS applications (lowercase - Gradle) |
| desktop/apps/ | Desktop applications (lowercase - Gradle) |
| web/apps/ | Web applications (lowercase - Gradle) |
| backend/services/ | Backend services (lowercase - Gradle) |
| Common/Libraries/ | KMP shared libraries (PascalCase) |
| Shared/Assets/ | Shared assets (PascalCase) |
| Modules/ | Feature modules (PascalCase) |
| Docs/Project/ | Project-wide docs (PascalCase) |
| Docs/VoiceOS/ | VoiceOS documentation |
| Docs/AVA/ | AVA documentation |
| Docs/Avanues/ | Avanues documentation |
| Docs/AvaConnect/ | AvaConnect documentation |
| Docs/Common/ | Shared library docs |
| contextsave/ | Context saves |
| .claude/ | Claude Code config |
| .ideacode/ | IDEACODE config |
| .ideacode/living-docs/ | Living documentation |
| .ideacode/registries/ | File/folder tracking |

---

## Folder Purposes

| Folder | Purpose | Contains |
|--------|---------|----------|
| `.claude/commands/` | Slash command definitions | *.md command files |
| `.claude/lib/` | Shared algorithms/utilities | Reusable logic |
| `.ideacode/continuity/` | Session state files | con-*.md |
| `.ideacode/living-docs/` | Evolving project docs | LD-*.md |
| `.ideacode/registries/` | File/folder tracking | *-REGISTRY.md |
| `.ideacode/archive/` | Completed features | Archived work |
| `ideacode-mcp/src/` | MCP server source | *.ts files |
| `ideacode-mcp/src/tools/` | Tool implementations | ideacode_*.ts |
| `ideacode-plugin/` | Claude Code plugin | Distribution bundle |
| `ideacode-plugin/.claude-plugin/` | Plugin manifest | plugin.json |
| `ideacode-plugin/commands/` | Bundled slash commands | *.md |
| `ideacode-plugin/hooks/` | Plugin hooks | hooks.json |
| `ideacode-plugin/servers/mcp/` | Bundled MCP server | Compiled dist/ |
| `ideacode-plugin/scripts/` | Plugin scripts | *.sh |
| `programming-standards/` | Coding standards | {language}/*.md |
| `protocols/` | Workflow protocols | Protocol-*.md |
| `docs/` | User documentation | *.md |

---

## Naming Rules

| Context | Convention | Example |
|---------|------------|---------|
| Gradle paths | lowercase | `android/`, `ios/`, `desktop/` |
| All other folders | PascalCase | `Docs/`, `Common/`, `Shared/` |
| App/Module folders | PascalCase | `VoiceOS/`, `AVA/`, `WebAvanue/` |
| Config folders | kebab-case | `.claude/`, `.ideacode/`, `living-docs/` |
| Documents | App-prefixed | `App-Module-Description-YDDMMHH-V#.md` |

### Monorepo Structure (PascalCase)

| Folder | Purpose |
|--------|---------|
| `Docs/` | All documentation |
| `Docs/{App}/MasterSpecs/` | Universal specs |
| `Docs/{App}/Platform/{Platform}/` | Platform-specific docs |
| `Common/` | Shared KMP libraries |
| `Modules/` | Feature modules |
| `Shared/` | Assets, configs |

---

## Before Creating Folders

| Check | Action |
|-------|--------|
| Similar exists? | Search this registry |
| Correct parent? | Verify nesting level |
| Purpose documented? | Add to this registry |
| Naming correct? | Follow conventions |

---

## Forbidden Actions

| Action | Why | Instead |
|--------|-----|---------|
| Create duplicate | Causes confusion | Use existing |
| Nest too deep | Hard to navigate | Flatten structure |
| Abbreviate | Unclear purpose | Use full words |
| Mix case | Inconsistent | Use lowercase |

---

## Adding New Folders

1. Check this registry for similar folders
2. Determine correct parent directory
3. Follow naming: `lowercase-kebab-case/`
4. Add entry with purpose to this registry
5. Create README.md inside if non-obvious

---

## Enforcement Rules

| Rule | Requirement |
|------|-------------|
| Registry first | ALWAYS check registry before creating folders |
| PascalCase | Use for Docs, Apps, Modules, Common, Shared |
| lowercase | Use ONLY for Gradle paths (android/, ios/, desktop/) |
| No duplicates | Search registry before creating |
| Uppercase V | Version indicator in filenames is uppercase V |

---
*Updated: 2025-12-03 | IDEACODE v10.2*
