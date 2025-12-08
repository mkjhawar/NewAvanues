# LearnApp Developer Manual

**Module**: LearnApp
**Package**: `com.augmentalis.voiceoscore.learnapp`
**Type**: Application Module
**Last Updated**: 2025-12-08
**Status**: Production

---

## Chapters

| Chapter | Title | Description |
|---------|-------|-------------|
| [01](./01-Overview-Architecture.md) | Overview & Architecture | Purpose, features, technology stack, high-level architecture, data flow |
| [02](./02-Exploration-Engine.md) | Exploration Engine | Hybrid C-Lite, Aggressive Mode, Cumulative VUID Tracking, DFS algorithm |
| [03](./03-Developer-Settings.md) | Developer Settings | 51 configurable parameters, UI, categories |
| [04](./04-Database-Persistence.md) | Database & Persistence | SQLDelight schema, Voice Command persistence, Contact Learning |
| [05](./05-Critical-Fixes.md) | Critical Fixes | Bug fixes, recovery mechanisms, safety enhancements |
| [06](./06-Troubleshooting.md) | Troubleshooting | Common issues, debugging, performance, patterns |
| [07](./07-Version-History.md) | Version History | Changelog and version details |

---

## Quick Links

### Most Referenced

- [Cumulative VUID Tracking](./02-Exploration-Engine.md#cumulative-vuid-tracking-2025-12-08-critical-fix) - Fix for 10% vs 50-75% completion discrepancy
- [Hybrid C-Lite Strategy](./02-Exploration-Engine.md#hybrid-c-lite-exploration-strategy-2025-12-05) - 98% click success rate
- [Developer Settings](./03-Developer-Settings.md) - 51 configurable parameters
- [Database Schema](./04-Database-Persistence.md#database-schema) - SQLDelight tables

### By Task

| Task | Chapter |
|------|---------|
| Understanding architecture | [01 - Overview](./01-Overview-Architecture.md) |
| Debugging exploration issues | [02 - Exploration](./02-Exploration-Engine.md), [06 - Troubleshooting](./06-Troubleshooting.md) |
| Tuning exploration parameters | [03 - Settings](./03-Developer-Settings.md) |
| Database queries | [04 - Database](./04-Database-Persistence.md) |
| Reviewing recent fixes | [05 - Fixes](./05-Critical-Fixes.md), [07 - History](./07-Version-History.md) |

---

## Related Documentation

- [User Manual](../VoiceOS-user-manual-50512-V1.md)
- [Exploration Architecture](../VoiceOS-developer-manual-exploration-architecture-50412-V1.md)
- [Integration Guide](./VoiceOS-Integration-Quick-Start-51310-V1.md)
- [AppStateDetector Migration](./VoiceOS-AppStateDetector-Migration-Guide-51310-V1.md)

---

## File Structure

```
developer-manual/
├── 00-Index.md                 # This file
├── 01-Overview-Architecture.md # Architecture and overview
├── 02-Exploration-Engine.md    # Exploration algorithms
├── 03-Developer-Settings.md    # Configurable parameters
├── 04-Database-Persistence.md  # Database and persistence
├── 05-Critical-Fixes.md        # Bug fixes
├── 06-Troubleshooting.md       # Debugging guide
└── 07-Version-History.md       # Changelog
```

---

**Version**: 2.0 (Chapter Split)
**Previous**: VoiceOS-developer-manual-50612-V1.md (monolithic)
