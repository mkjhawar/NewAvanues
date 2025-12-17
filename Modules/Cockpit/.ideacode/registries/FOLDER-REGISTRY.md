# Folder Registry - Cockpit Module v12.0.0

## Module Structure

```
Modules/Cockpit/
├── .claude/                    # Claude configuration
├── .ideacode/                  # IDEACODE configuration
│   ├── config.idc             # Module config
│   └── registries/            # This directory
├── src/                        # Source code
│   ├── management/            # Module management
│   ├── monitoring/            # System monitoring
│   ├── configuration/         # Configuration management
│   └── dashboard/             # Dashboard UI
└── plugins/                    # Module plugins
```

## Folder Conventions

| Folder | Case | Purpose |
|--------|------|---------|
| `src/` | lowercase | Source code |
| `management/` | lowercase | Module management |
| `monitoring/` | lowercase | System monitoring |
| `plugins/` | lowercase | Extensible plugins |
| `.claude/` | lowercase (hidden) | Claude configuration |
| `.ideacode/` | lowercase (hidden) | IDEACODE configuration |

## Documentation Locations

- **Living Docs:** `/Volumes/M-Drive/Coding/NewAvanues/Docs/Cockpit/LivingDocs/`
- **Module Docs:** `Modules/Cockpit/docs/` (if exists)

---

Updated: 2025-12-15 | Version: 12.0.0
