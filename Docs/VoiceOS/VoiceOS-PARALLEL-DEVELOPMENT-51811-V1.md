# VoiceOS Parallel Development Guide

**Version:** 1.0 | **Created:** 2025-11-18

---

## Overview

VoiceOS uses git worktrees for parallel development of two versions:

| Version | Branch | Directory | Database | Purpose |
|---------|--------|-----------|----------|---------|
| **Room Stable** | `room/stable` | `/Volumes/M-Drive/Coding/VoiceOS-Room` | Room (Android-only) | Production-ready, stable |
| **KMP Development** | `kmp/main` | `/Volumes/M-Drive/Coding/VoiceOS` | SQLDelight (Multiplatform) | New development |

---

## Directory Structure

```
/Volumes/M-Drive/Coding/
├── VoiceOS/                    # KMP Development (kmp/main)
│   ├── libraries/core/         # KMP libraries
│   │   ├── database/           # SQLDelight database
│   │   ├── accessibility-types/
│   │   ├── command-models/
│   │   └── ... (10 more)
│   └── modules/                # App modules
│
└── VoiceOS-Room/               # Room Stable (room/stable)
    └── modules/                # Original Room-based modules
        └── managers/VoiceDataManager/  # Room database
```

---

## Worktree Commands

### Check Status
```bash
git worktree list
```

### Build Each Version

**KMP Version:**
```bash
cd /Volumes/M-Drive/Coding/VoiceOS
./gradlew assembleDebug
```

**Room Version:**
```bash
cd /Volumes/M-Drive/Coding/VoiceOS-Room
./gradlew assembleDebug
```

### Switch Between Worktrees
```bash
# Work on KMP
cd /Volumes/M-Drive/Coding/VoiceOS

# Work on Room
cd /Volumes/M-Drive/Coding/VoiceOS-Room
```

---

## Branch Workflow

### KMP Development (kmp/main)

```bash
# Create feature branch
git checkout -b kmp/feature/new-repository

# Work on feature
# ... make changes ...

# Commit
git add -A && git commit -m "feat(kmp): Add repository abstraction"

# Merge to kmp/main
git checkout kmp/main
git merge kmp/feature/new-repository
```

### Room Stable (room/stable)

```bash
cd /Volumes/M-Drive/Coding/VoiceOS-Room

# Create hotfix branch
git checkout -b room/hotfix/critical-bug

# Fix bug
# ... make changes ...

# Commit
git add -A && git commit -m "fix: Critical database bug"

# Merge to room/stable
git checkout room/stable
git merge room/hotfix/critical-bug
```

### Sync Changes Between Versions

**Cherry-pick specific commits:**
```bash
# In KMP worktree, get a fix from room/stable
git cherry-pick <commit-hash>

# In Room worktree, get a feature from kmp/main
git cherry-pick <commit-hash>
```

**Merge entire branch (when ready):**
```bash
# Merge KMP work into main (when migration complete)
git checkout main
git merge kmp/main
```

---

## Key Differences

### KMP Version (kmp/main)

**Database:**
- SQLDelight 2.0.1 (Kotlin Multiplatform)
- Location: `libraries/core/database/`
- 18 tables with cross-platform support
- Targets: Android, iOS, JVM

**Structure:**
```
libraries/core/database/
├── src/
│   ├── commonMain/           # Shared code
│   │   ├── sqldelight/       # Schema files (.sq)
│   │   └── kotlin/           # Repositories
│   ├── androidMain/          # Android driver
│   ├── iosMain/              # iOS driver
│   └── jvmMain/              # JVM driver (tests)
```

**Usage:**
```kotlin
// Create database manager
val driverFactory = DatabaseDriverFactory(context)
val dbManager = VoiceOSDatabaseManager(driverFactory)

// Use repository
val history = dbManager.commandHistory.getAll()
```

### Room Version (room/stable)

**Database:**
- Room 2.6.1 (Android-only)
- Location: `modules/managers/VoiceDataManager/`
- 14 tables (Android only)

**Usage:**
```kotlin
// Get database instance
val db = VoiceOSDatabase.getInstance(context)

// Use DAO directly
val commands = db.customCommandDao().getAll()
```

---

## Testing

### Run Tests - KMP Version
```bash
cd /Volumes/M-Drive/Coding/VoiceOS

# Database tests (JVM)
./gradlew :libraries:core:database:jvmTest

# All library tests
./gradlew testDebugUnitTest
```

### Run Tests - Room Version
```bash
cd /Volumes/M-Drive/Coding/VoiceOS-Room

# VoiceDataManager tests
./gradlew :modules:managers:VoiceDataManager:testDebugUnitTest
```

---

## IDE Setup

### Android Studio / IntelliJ

You can open both worktrees as separate projects:

1. **KMP Project:** Open `/Volumes/M-Drive/Coding/VoiceOS`
2. **Room Project:** Open `/Volumes/M-Drive/Coding/VoiceOS-Room`

Each will have independent:
- Gradle sync
- Build configurations
- Run configurations

### Visual Studio Code

```bash
# Open both in split view
code /Volumes/M-Drive/Coding/VoiceOS /Volumes/M-Drive/Coding/VoiceOS-Room
```

---

## Release Process

### Room Version (Current Production)
1. Test on `room/stable`
2. Tag release: `git tag -a v4.x.x -m "Release 4.x.x (Room)"`
3. Build APK from VoiceOS-Room directory

### KMP Version (Future Production)
1. Complete migration phases
2. Test all platforms (Android, iOS, JVM)
3. Merge `kmp/main` to `main`
4. Tag release: `git tag -a v5.0.0 -m "Release 5.0.0 (KMP)"`

---

## Troubleshooting

### "Branch already checked out"
```bash
# Can't checkout same branch in multiple worktrees
# Use the worktree where it's already checked out
git worktree list
```

### Gradle Cache Conflicts
```bash
# Clear Gradle cache if builds interfere
./gradlew clean
rm -rf ~/.gradle/caches/
```

### Worktree Corruption
```bash
# Repair worktree
git worktree repair

# Remove and recreate
git worktree remove /Volumes/M-Drive/Coding/VoiceOS-Room
git worktree add /Volumes/M-Drive/Coding/VoiceOS-Room room/stable
```

---

## Best Practices

1. **Keep worktrees in sync** - Pull regularly in both
2. **Use separate terminals** - One for each worktree
3. **Name branches clearly** - `kmp/feature/xxx` vs `room/hotfix/xxx`
4. **Test in both** - Ensure changes don't break either version
5. **Document differences** - Update this guide as structure evolves

---

## Migration Timeline

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | SQLDelight schemas created | Complete |
| 2 | Unit tests and basic integration | Complete |
| 3 | Repository abstraction layer | Pending |
| 4 | VoiceDataManager migration | Pending |
| 5 | VoiceOSCore migration | Pending |
| 6 | iOS/Desktop drivers | Pending |
| 7 | Merge to main | Pending |

See `docs/KMP-MIGRATION-PLAN.md` for detailed implementation plan.

---

**Author:** VoiceOS Team
**Last Updated:** 2025-11-18
