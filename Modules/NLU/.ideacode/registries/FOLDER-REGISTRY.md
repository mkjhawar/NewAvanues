# Folder Registry - NLU Module v12.0.0

## Module Structure

```
Modules/NLU/
├── .claude/                    # Claude configuration
├── .ideacode/                  # IDEACODE configuration
│   ├── config.idc             # Module config
│   └── registries/            # This directory
├── core/                       # Core NLU engine
│   ├── intent/                # Intent recognition
│   ├── entity/                # Entity extraction
│   └── context/               # Context management
├── models/                     # ML models
└── training/                   # Training data and scripts
```

## Folder Conventions

| Folder | Case | Purpose |
|--------|------|---------|
| `core/` | lowercase | Core NLU functionality |
| `models/` | lowercase | Machine learning models |
| `training/` | lowercase | Training resources |
| `.claude/` | lowercase (hidden) | Claude configuration |
| `.ideacode/` | lowercase (hidden) | IDEACODE configuration |

## Documentation Locations

- **Living Docs:** `/Volumes/M-Drive/Coding/NewAvanues/Docs/NLU/LivingDocs/`
- **Module Docs:** `Modules/NLU/docs/` (if exists)

---

Updated: 2025-12-15 | Version: 12.0.0
