# LD-NAV-Development-Guide-V1

**Living Document** | NewAvanues Development Guide
**Version:** 1.0 | **Created:** 2025-12-15 | **Status:** Active

---

## Getting Started

### Prerequisites

- **Java:** JDK 17+ (use `/usr/libexec/java_home` to check)
- **Android SDK:** Latest (for VoiceOS development)
- **Node.js:** 18+ (for WebAvanue)
- **Python:** 3.9+ (for NLU)
- **Gradle:** 8.0+ (included in wrapper)

### Repository Setup

```bash
# Clone repository
cd /Volumes/M-Drive/Coding/NewAvanues

# Check current branch
git branch --show-current

# Verify structure
ls -la .claude .ideacode Modules/
```

---

## Terminal Isolation

**CRITICAL:** Always verify you're in the correct repo/branch before starting work.

```bash
pwd  # Should show /Volumes/M-Drive/Coding/NewAvanues
```

**Never work across multiple repos/branches without explicit user approval.**

---

## Development Workflow

### 1. Choose Your Module

| Module | Use When |
|--------|----------|
| VoiceOS | Android accessibility, voice commands |
| AVA | AI assistant logic, cross-platform |
| WebAvanue | Web dashboard, management UI |
| Cockpit | Module management, configuration |
| NLU | Intent recognition, entity extraction |

### 2. Check Registries First

Before creating files:
```bash
# Root-level registry
cat .ideacode/registries/MODULE-REGISTRY.md

# Module-specific registry
cat Modules/{Module}/.ideacode/registries/FOLDER-REGISTRY.md
cat Modules/{Module}/.ideacode/registries/FILE-REGISTRY.md
```

### 3. Read Module Instructions

```bash
cat Modules/{Module}/.claude/CLAUDE.md
```

### 4. Follow File Naming

| Type | Pattern | Example |
|------|---------|---------|
| Living Docs | `LD-{MOD}-{Desc}-V#.md` | `LD-VOS-Feature-V1.md` |
| Specs | `{MOD}-Spec-{Feature}-YDDMM-V#.md` | `VOS-Spec-Voice-51215-V1.md` |
| Plans | `{MOD}-Plan-{Feature}-YDDMM-V#.md` | `VOS-Plan-Voice-51215-V1.md` |

**Module Codes:**
- VOS = VoiceOS
- AVA = AVA
- WEB = WebAvanue
- CPT = Cockpit
- NLU = NLU
- NAV = NewAvanues (root)

---

## Building

### Android (VoiceOS)

```bash
cd /Volumes/M-Drive/Coding/NewAvanues
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:assembleDebug
```

### Web (WebAvanue)

```bash
cd /Volumes/M-Drive/Coding/NewAvanues/Modules/WebAvanue
npm install
npm run build
```

### NLU

```bash
cd /Volumes/M-Drive/Coding/NewAvanues/Modules/NLU
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

---

## Testing

### Unit Tests

```bash
# VoiceOS
./gradlew :Modules:VoiceOS:core:database:test

# WebAvanue
cd Modules/WebAvanue && npm test
```

### Integration Tests

See module-specific living docs for integration test procedures.

---

## Configuration Files

All configuration uses **IDC format** (v12.0.0):

| File | Location | Purpose |
|------|----------|---------|
| Root Config | `.ideacode/config.idc` | Monorepo configuration |
| Settings | `.claude/settings.idc` | Claude settings |
| MCP | `.claude/mcp.idc` | MCP server config |
| Module Config | `Modules/{Module}/.ideacode/config.idc` | Module-specific config |

**Never use JSON for IDEACODE configs** - always use IDC format.

---

## Documentation Locations

| Type | Location | Example |
|------|----------|---------|
| Living Docs | `Docs/{Module}/LivingDocs/` | `Docs/VoiceOS/LivingDocs/LD-VOS-*-V1.md` |
| Registries | `Modules/{Module}/.ideacode/registries/` | `FOLDER-REGISTRY.md` |
| Project Info | `docs/project-info/` | `API-CONTRACTS.md` |
| Context Saves | `contextsave/` | `con-nav-voiceos-feature-20251215.md` |

---

## Code Standards

### Kotlin
- Use `PascalCase` for classes
- Use `camelCase` for functions/variables
- Follow SOLID principles
- 90%+ test coverage for business logic

### TypeScript/React
- Use `PascalCase` for components
- Use `camelCase` for functions/variables
- Use hooks for state management
- PropTypes or TypeScript types required

### Python
- Follow PEP 8
- Use type hints
- Document all public functions
- 90%+ test coverage for models

---

## Cross-Module Work

**Before making changes that affect multiple modules:**

1. Check `CROSS-MODULE-DEPENDENCIES.md`
2. Ask user for approval
3. Test in all affected modules
4. Update API contracts
5. Document in all relevant living docs

---

## Common Commands

### Git

```bash
git status                    # Check status
git add .                     # Stage changes
git commit -m "message"       # Commit
git push                      # Push to remote
```

### Gradle

```bash
./gradlew tasks               # List tasks
./gradlew clean               # Clean build
./gradlew build               # Full build
```

### ADB (Android)

```bash
adb devices                   # List devices
adb install app.apk           # Install APK
adb logcat                    # View logs
```

---

## Troubleshooting

### Build Fails

1. Clean build: `./gradlew clean`
2. Check Java version: `java -version` (should be 17+)
3. Invalidate caches in IDE

### Tests Fail

1. Check test coverage threshold (90%+)
2. Review test logs
3. Run specific test: `./gradlew :module:test --tests TestClass`

### IDC Config Errors

1. Validate format (check for missing colons)
2. Check schema version: `schema: idc-1.0`
3. See: `docs/specifications/IDC-FORMAT-SPEC-V1.md`

---

## Best Practices

✅ **Do:**
- Read registries before creating files
- Follow file naming conventions
- Check CLAUDE.md for module-specific rules
- Update living docs when making significant changes
- Test thoroughly (90%+ coverage)
- Use IDC format for configs

❌ **Don't:**
- Work across repos without permission
- Use JSON for IDEACODE configs
- Hardcode configuration
- Delete code without approval
- Skip tests
- Commit to `main` directly

---

## Related Documentation

- [Core Architecture](LD-NAV-Core-Architecture-V1.md)
- [Module Registry](../../.ideacode/registries/MODULE-REGISTRY.md)
- [Cross-Module Dependencies](../../.ideacode/registries/CROSS-MODULE-DEPENDENCIES.md)

---

**Last Updated:** 2025-12-15
**Maintained By:** NewAvanues Team
**Version:** 12.0.0
