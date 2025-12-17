# Folder Registry - VoiceOS Module v12.0.0

## Module Structure

```
Modules/VoiceOS/
├── .claude/                    # Claude configuration
├── .ideacode/                  # IDEACODE configuration
│   ├── config.idc             # Module config
│   └── registries/            # This directory
├── apps/                       # Android apps
│   └── VoiceOSCore/           # Main app
├── core/                       # Core libraries
│   ├── database/              # Database layer
│   └── accessibility/         # Accessibility services
└── libraries/                  # Module libraries
```

## Folder Conventions

| Folder | Case | Purpose |
|--------|------|---------|
| `apps/` | lowercase | Android applications |
| `core/` | lowercase | Core functionality |
| `libraries/` | lowercase | Shared libraries |
| `.claude/` | lowercase (hidden) | Claude configuration |
| `.ideacode/` | lowercase (hidden) | IDEACODE configuration |

## Documentation Locations

- **Living Docs:** `/Volumes/M-Drive/Coding/NewAvanues/Docs/VoiceOS/LivingDocs/`
- **Module Docs:** `Modules/VoiceOS/docs/` (if exists)

---

Updated: 2025-12-15 | Version: 12.0.0
