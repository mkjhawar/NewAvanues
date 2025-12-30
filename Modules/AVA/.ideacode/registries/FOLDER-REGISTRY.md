# Folder Registry - AVA Module v12.0.0

## Module Structure

```
Modules/AVA/
├── .claude/                    # Claude configuration
├── .ideacode/                  # IDEACODE configuration
│   ├── config.idc             # Module config
│   └── registries/            # This directory
├── core/                       # Core AI functionality
├── platforms/                  # Platform-specific implementations
│   ├── android/               # Android implementation
│   ├── ios/                   # iOS implementation
│   └── web/                   # Web implementation
└── shared/                     # Shared code (KMP)
```

## Folder Conventions

| Folder | Case | Purpose |
|--------|------|---------|
| `platforms/` | lowercase | Platform implementations |
| `shared/` | lowercase | Kotlin Multiplatform shared code |
| `core/` | lowercase | Core AI logic |
| `.claude/` | lowercase (hidden) | Claude configuration |
| `.ideacode/` | lowercase (hidden) | IDEACODE configuration |

## Documentation Locations

- **Living Docs:** `/Volumes/M-Drive/Coding/NewAvanues/Docs/AVA/LivingDocs/`
- **Module Docs:** `Modules/AVA/docs/` (if exists)

---

Updated: 2025-12-15 | Version: 12.0.0
