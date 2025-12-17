# Module Registry - NewAvanues v12.0.0

## Active Modules

| Module | Path | Type | Status | Description |
|--------|------|------|--------|-------------|
| **VoiceOS** | `Modules/VoiceOS/` | Android App | Active | Voice-first Android accessibility service |
| **AVA** | `Modules/AVA/` | Cross-platform | Active | AI assistant platform |
| **WebAvanue** | `Modules/WebAvanue/` | Web App | Active | Web platform |
| **Cockpit** | `Modules/Cockpit/` | Management | Active | Management dashboard |
| **NLU** | `Modules/NLU/` | ML Service | Active | Natural language understanding |
| **Common** | `Common/` | Shared Libraries | Active | Shared libraries and core components |

## Module Structure

Each module has:
- `.claude/CLAUDE.md` - Module-specific instructions
- `.ideacode/config.idc` - Module configuration
- `.ideacode/registries/` - Module-specific registries
  - `FOLDER-REGISTRY.md` - Folder structure
  - `FILE-REGISTRY.md` - File naming conventions
  - `COMPONENT-REGISTRY.md` - Components/features

## Documentation Locations

| Module | Living Docs | Registries |
|--------|-------------|------------|
| VoiceOS | `Docs/VoiceOS/LivingDocs/` | `Modules/VoiceOS/.ideacode/registries/` |
| AVA | `Docs/AVA/LivingDocs/` | `Modules/AVA/.ideacode/registries/` |
| WebAvanue | `Docs/WebAvanue/LivingDocs/` | `Modules/WebAvanue/.ideacode/registries/` |
| Cockpit | `Docs/Cockpit/LivingDocs/` | `Modules/Cockpit/.ideacode/registries/` |
| NLU | `Docs/NLU/LivingDocs/` | `Modules/NLU/.ideacode/registries/` |
| Common | `Docs/Common/` | `Common/.ideacode/registries/` |

## Development Branches

| Module | Branch | Purpose |
|--------|--------|---------|
| VoiceOS | `VoiceOS-Development` | VoiceOS feature development |
| AVA | `AVA-Development` | AVA feature development |
| WebAvanue | `WebAvanue-Development` | WebAvanue feature development |
| Cockpit | `Cockpit-Development` | Cockpit feature development |
| NLU | `NLU-Development` | Natural Language Understanding |

## Cross-Module Work

Before working across modules:
1. Check `CROSS-MODULE-DEPENDENCIES.md` for dependency map
2. Ask user for approval
3. Document changes in both modules' living docs

---

Updated: 2025-12-15 | Version: 12.0.0
