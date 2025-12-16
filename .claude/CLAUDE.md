# NewAvanues - Project Instructions

Repository: NewAvanues monorepo (AVA, VoiceOS, WebAvanue, Cockpit, NLU)

---

## TERMINAL ISOLATION (CRITICAL)

**FIRST ACTION ON SESSION START:**
```bash
pwd  # Detect current repo/worktree
```

**Show:** Working in: {detected-path}

**STAY IN DETECTED REPO/WORKTREE:**
- NewAvanues has multiple development branches and worktrees
- NEVER switch repos/worktrees without explicit user permission
- NEVER work across multiple branches in single command
- ASK before any cross-repo or cross-worktree operation

---

## PROJECT STRUCTURE

Monorepo containing:
- **AVA:** AI assistant platform
- **VoiceOS:** Voice-first Android accessibility service
- **WebAvanue:** Web platform
- **Cockpit:** Management dashboard
- **NLU:** Natural language understanding
- **Common:** Shared libraries and core components

---

## DEVELOPMENT BRANCHES

| Branch | Purpose |
|--------|---------|
| Avanues-Main | Main integration branch |
| AVA-Development | AVA feature development |
| VoiceOS-Development | VoiceOS feature development |
| WebAvanue-Development | WebAvanue feature development |
| Cockpit-Development | Cockpit feature development |
| NLU-Development | Natural Language Understanding |

**RULE:** NEVER commit to main. Always use feature/ or bugfix/ branches.

---

## MODULE-SPECIFIC WORK

When working in a module:
1. Check for module-level `.claude/CLAUDE.md` (if exists)
2. Read module-level instructions BEFORE making changes
3. Stay within module scope unless explicitly asked

---

## DOCUMENTATION LOCATIONS

**Living Docs:** `/Volumes/M-Drive/Coding/NewAvanues/Docs/{Module}/LivingDocs/LD-*.md`
- Root docs: `Docs/NewAvanues/LivingDocs/`
- Module docs: `Docs/{VoiceOS|AVA|WebAvanue|Cockpit|NLU}/LivingDocs/`

**Registries:** `.ideacode/registries/` (root) and `Modules/{Module}/.ideacode/registries/`
- Root: MODULE-REGISTRY.md, CROSS-MODULE-DEPENDENCIES.md
- Per module: FOLDER-REGISTRY.md, FILE-REGISTRY.md, COMPONENT-REGISTRY.md

**Project Info:** `docs/project-info/` (central reference docs)

---

## INHERITED RULES

All rules from `/Volumes/M-Drive/Coding/.claude/CLAUDE.md` apply:
- ZERO TOLERANCE RULES (mandatory naming, no hardwiring, etc.)
- File naming: App-Module-Desc-YDDMMHH-V#.md
- No delete without approval
- Test coverage 90%+
- Database: SQLDelight ONLY (not Room)

---

Updated: 2025-12-15 | Version: 12.0.0
