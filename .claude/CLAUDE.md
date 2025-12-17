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

Updated: 2025-12-17 | Version: 12.1.0
