# NewAvanues - Project Instructions

Repository: NewAvanues - Main Monorepo (AVA, VoiceOS, WebAvanue, Cockpit, NLU)

---

## PROJECT STRUCTURE

| Path | Purpose |
|------|---------|
| Modules/AVA/ | AI assistant platform |
| Modules/VoiceOS/ | Voice-first accessibility service |
| Modules/WebAvanue/ | Web platform (Tauri + React) |
| Modules/Cockpit/ | Management dashboard |
| Modules/NLU/ | Natural language understanding |
| Common/ | Shared KMP libraries |

---

## PROJECT-SPECIFIC TECH

| Component | Technology |
|-----------|------------|
| Build | Gradle composite builds + version catalogs |
| Shared logic | KMP (Kotlin Multiplatform) |
| Database | SQLDelight (NOT Room) |
| DI | Koin |
| Web | Tauri + React + TypeScript |
| Language Server | Kotlin LSP (for DSL/tooling) |
| Web Components | React + TypeScript (embeddable UI) |
| IDE Plugins | VS Code (TypeScript) + IntelliJ (Kotlin + JCEF) |

---

## LSP ARCHITECTURE (AVAMagic Tooling)

**Layer 1: Core Language Server (Kotlin)**
- DSL parsing, validation, code generation
- Implements: Language Server Protocol (LSP)
- Size: ~25MB (shared across all tools)
- Reuses: ThemeCompiler.kt, VUIDCreator, existing MagicUI logic

**Layer 2: Web UI Components (React + TypeScript)**
- Theme color picker, property inspector, live preview
- Monaco Editor integration (VS Code's editor)
- Size: ~5MB (cached, embeddable everywhere)

**Layer 3: Platform Integrations**
- VS Code Extension: TypeScript + LSP client (~2MB)
- IntelliJ Plugin: Kotlin + JCEF browser (~3MB)
- Standalone Apps: Compose Desktop + CEF OR Tauri v2

**Total Ecosystem:** ~45MB (90% code reuse across all tools)

---

## DEVELOPMENT BRANCHES

| Branch | Purpose |
|--------|---------|
| Avanues-Main | Main integration |
| AVA-Development | AVA features |
| VoiceOS-Development | VoiceOS features |
| WebAvanue-Development | WebAvanue features |
| Cockpit-Development | Cockpit features |
| NLU-Development | NLU features |

---

## WORKTREES

| Worktree | Path | Branch |
|----------|------|--------|
| NewAvanues-AVA | /Volumes/M-Drive/Coding/NewAvanues-AVA | AVA-Development |
| NewAvanues-VoiceOS | /Volumes/M-Drive/Coding/NewAvanues-VoiceOS | VoiceOS-Development |
| NewAvanues-WebAvanue | /Volumes/M-Drive/Coding/NewAvanues-WebAvanue | WebAvanue-Development |
| NewAvanues-Cockpit | /Volumes/M-Drive/Coding/NewAvanues-Cockpit | Cockpit-Development |
| NewAvanues-NLU | /Volumes/M-Drive/Coding/NewAvanues-NLU | NLU-Development |

---

## SPECIAL RULES

| Rule | Requirement |
|------|-------------|
| Database | SQLDelight ONLY (never Room) |
| Module work | Check module-level .claude/CLAUDE.md first |
| Cross-module | Check CROSS-MODULE-DEPENDENCIES.md |
| Worktrees | NEVER switch worktrees without approval |

---

## DOCUMENTATION

| Type | Location |
|------|----------|
| Living Docs | `Docs/{Module}/LivingDocs/LD-*.md` |
| Registries | `.ideacode/registries/` |
| Project Info | `docs/project-info/` |

---

Updated: 2025-12-24 | Version: 12.2.0
