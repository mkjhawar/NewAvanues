# Cross-Module Dependencies - NewAvanues v12.0.0

## Dependency Map

```
Common (Core Libraries)
├── VoiceOS (depends on Common/Core, Common/Libraries)
├── AVA (depends on Common/Core, Common/Libraries)
├── WebAvanue (depends on Common/Core)
└── Cockpit (depends on Common/Core, manages all modules)
```

## Module Dependencies

### VoiceOS
**Depends On:**
- `Common/Core/` - Core utilities and base classes
- `Common/Libraries/` - Shared Android libraries
- `Common/ThirdParty/` - Third-party dependencies

**Depended By:**
- `Cockpit` - Manages VoiceOS configuration

### AVA
**Depends On:**
- `Common/Core/` - Core utilities
- `Common/Libraries/` - Shared libraries

**Depended By:**
- `Cockpit` - Manages AVA configuration
- `VoiceOS` - Integration with AVA assistant

### WebAvanue
**Depends On:**
- `Common/Core/` - Core utilities

**Depended By:**
- `Cockpit` - Web interface integration

### Cockpit
**Depends On:**
- `Common/Core/` - Core utilities
- All modules (for management)

**Depended By:**
- None (top-level management dashboard)

### Common
**Depends On:**
- External dependencies only

**Depended By:**
- All modules

## IPC Contracts

| From → To | Method | Contract Location |
|-----------|--------|-------------------|
| VoiceOS → AVA | Voice commands | `docs/project-info/IPC-METHODS.md` |
| AVA → VoiceOS | Assistant responses | `docs/project-info/IPC-METHODS.md` |
| Cockpit → All | Configuration updates | `docs/project-info/API-CONTRACTS.md` |
| All → Cockpit | Status reports | `docs/project-info/API-CONTRACTS.md` |

## API Contracts

See: `docs/project-info/API-CONTRACTS.md`

## Intent Registry

Android intents between modules: `docs/project-info/INTENT-REGISTRY.md`

## Modification Guidelines

**When modifying Common:**
1. Test changes in all dependent modules
2. Update `Common/CHANGELOG.md`
3. Notify all module maintainers
4. Run full integration tests

**When adding cross-module features:**
1. Document in both modules' living docs
2. Update this dependency map
3. Update API contracts
4. Get approval for multi-module changes

---

Updated: 2025-12-15 | Version: 12.0.0
