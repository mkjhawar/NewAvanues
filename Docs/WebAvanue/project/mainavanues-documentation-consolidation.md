# Documentation Consolidation Strategy

**Version:** 1.1
**Date:** 2025-11-24
**Updated:** 2025-11-24 (Added lessons from WebAvanue migration)
**Purpose:** Consolidate scattered documentation from 5 repos into MainAvanues monorepo

**See Also:**
- [MIGRATION-LESSONS-LEARNED.md](./migration-analysis/MIGRATION-LESSONS-LEARNED.md) - Key principles and anti-patterns
- [/Volumes/M-Drive/Coding/ideacode/updateideas/foldernaming.md](file:///Volumes/M-Drive/Coding/ideacode/updateideas/foldernaming.md) - IDEACODE folder naming guidelines

---

## 📊 Current State Analysis

### Scattered Across 5 Repositories

| Repo | Location | Estimated Files | Issues |
|------|----------|----------------|--------|
| AVA AI | `/docs`, `.ideacode` | 30-50 | Deep nesting, duplicate folders |
| VoiceOS | `/docs`, `.ideacode` | 25-40 | Inconsistent naming |
| AVA Connect | `/docs`, `.ideacode` | 15-25 | Mixed structure |
| Avanues | `/docs`, `.ideacode` | 20-35 | Multiple doc locations |
| Web Avanue | `/docs` | 10-20 | Minimal structure |

**Total:** ~100-170 documentation files scattered across 5 repos

**Problems:**
- Duplicate files (README.md, ARCHITECTURE.md in each repo)
- Inconsistent naming (no standard convention)
- No central registry (AI creates duplicate files)
- Deep folder nesting (hard to find docs)
- Split IDEACODE folders (specs/features scattered)

---

## 🎯 Target Structure: Hybrid Centralized

### Design Principle
**"All documentation lives in `/docs/`, organized to mirror the code structure"**

```
MainAvanues/
└── docs/                                    # Single source of truth
    ├── README.md                            # Monorepo overview
    ├── ARCHITECTURE.md                      # Architecture decisions
    ├── CONTRIBUTING.md                      # Contribution guidelines
    ├── SETUP.md                             # Development setup
    ├── DEPLOYMENT.md                        # Deployment guide
    ├── TESTING.md                           # Testing strategy
    │
    ├── ideacode/                            # Root IDEACODE (monorepo-wide)
    │   ├── specs/                           # Cross-app specifications
    │   ├── features/                        # Monorepo-wide features
    │   ├── archive/                         # Monorepo archive
    │   └── registries/
    │       └── DOCUMENTATION-REGISTRY.json  # Master registry
    │
    ├── android/                             # Mirrors android/ code
    │   └── apps/
    │       ├── ava/
    │       │   ├── dev-overview.md          # App overview
    │       │   ├── dev-architecture.md      # App architecture
    │       │   ├── api-voice-commands.md    # API docs
    │       │   └── ideacode/                # AVA-specific IDEACODE
    │       │       ├── specs/
    │       │       ├── features/
    │       │       └── archive/
    │       ├── voiceos/
    │       │   ├── dev-overview.md
    │       │   └── ideacode/
    │       ├── avaconnect/
    │       │   ├── dev-overview.md
    │       │   └── ideacode/
    │       └── avanues/
    │           ├── dev-overview.md
    │           └── ideacode/
    │
    ├── common/                              # Mirrors common/ code
    │   └── libs/
    │       ├── voice/
    │       │   ├── feature-recognition/
    │       │   │   └── api-overview.md      # Library API docs
    │       │   └── feature-dsl/
    │       │       └── api-overview.md
    │       ├── accessibility/
    │       │   └── feature-voice-cursor/
    │       │       └── api-overview.md
    │       └── shared/
    │           └── ui-design-system/
    │               └── api-overview.md
    │
    ├── web/                                 # Mirrors web/ code
    │   └── apps/
    │       └── webavanue/
    │           ├── dev-overview.md
    │           └── ideacode/
    │
    └── archive/                             # Historical documentation
        └── 2024/
            ├── AVA/
            ├── VoiceOS/
            ├── AVAConnect/
            ├── Avanues/
            └── WebAvanue/
```

---

## 📝 File Naming Convention

### Pattern: `{type}-{context}-{topic}.md`

**Types:**
| Type | Purpose | Examples |
|------|---------|----------|
| `dev` | Developer guides | `dev-overview.md`, `dev-architecture.md`, `dev-setup.md` |
| `api` | API documentation | `api-voice-commands.md`, `api-rest-endpoints.md` |
| `user` | User documentation | `user-quick-start.md`, `user-faq.md` |
| `spec` | Feature specifications | `spec-voice-dsl.md`, `spec-accessibility.md` |
| `adr` | Architecture Decision Records | `adr-001-kmp-migration.md`, `adr-002-monorepo.md` |
| `rfc` | Request for Comments | `rfc-voice-protocol-v2.md` |
| `test` | Test documentation | `test-integration-strategy.md` |
| `fix` | Bug fix documentation | `fix-auth-crash.md` |

**Examples:**
- `dev-android-setup.md` → Android development setup guide
- `api-voice-recognition.md` → Voice recognition API documentation
- `user-accessibility-features.md` → User guide for accessibility
- `spec-voice-dsl.md` → Voice DSL specification
- `adr-001-kmp-migration.md` → ADR for KMP migration

**Benefits:**
- Self-documenting (type tells you what it is)
- Sortable (files group by type)
- Searchable (grep for "api-*" for all API docs)
- AI-friendly (clear naming helps AI find relevant docs)

---

## 🗂️ Documentation Registry

### Purpose
**Prevent AI from creating duplicate files**

### Location
`docs/ideacode/registries/DOCUMENTATION-REGISTRY.json`

### Structure
```json
{
  "version": "1.0",
  "last_updated": "2025-11-24",
  "documents": {
    "docs/README.md": {
      "type": "overview",
      "created": "2025-11-24",
      "last_modified": "2025-11-24",
      "description": "Monorepo overview and quick start",
      "tags": ["overview", "setup"]
    },
    "docs/android/apps/ava/dev-overview.md": {
      "type": "dev",
      "app": "ava",
      "platform": "android",
      "created": "2025-11-24",
      "last_modified": "2025-11-24",
      "description": "AVA app overview and architecture",
      "tags": ["ava", "overview", "architecture"]
    }
  },
  "naming_convention": {
    "pattern": "{type}-{context}-{topic}.md",
    "types": ["dev", "api", "user", "spec", "adr", "rfc", "test", "fix"]
  }
}
```

### AI Protocol
1. **Before creating a new doc:** Check registry for existing files
2. **After creating a doc:** Add entry to registry
3. **When updating a doc:** Update `last_modified` timestamp
4. **When deleting a doc:** Remove from registry

---

## 🔄 Multiple IDEACODE Folders

### Decision Tree: Where to Put Features?

```
Is this feature monorepo-wide? (affects multiple apps/libs)
├─ YES → Use /docs/ideacode/
│   Examples:
│   - Authentication system (used by all apps)
│   - Design system overhaul (affects all UIs)
│   - Build system changes (affects all modules)
│
└─ NO → Is it app-specific or library-specific?
    ├─ App-specific → Use /docs/{platform}/apps/{app}/ideacode/
    │   Examples:
    │   - AVA voice command feature → /docs/android/apps/ava/ideacode/specs/
    │   - VoiceOS accessibility feature → /docs/android/apps/voiceos/ideacode/specs/
    │
    └─ Library-specific → Document in /docs/common/libs/{scope}/{lib}/
        Examples:
        - Voice recognition API → /docs/common/libs/voice/feature-recognition/
        - UI design system → /docs/common/libs/shared/ui-design-system/
```

### Examples

**Monorepo-Wide Feature:**
```
Feature: Add OAuth2 authentication across all apps
Location: /docs/ideacode/specs/oauth2-authentication.md
Reason: Affects AVA, VoiceOS, AVA Connect, Avanues, Web Avanue
```

**App-Specific Feature:**
```
Feature: Add voice gesture controls to AVA
Location: /docs/android/apps/ava/ideacode/specs/voice-gestures.md
Reason: Only affects AVA app
```

**Library Feature:**
```
Feature: Improve voice recognition accuracy
Location: /docs/common/libs/voice/feature-recognition/api-improvements.md
Reason: Changes to voice recognition library API
```

---

## 🚀 Migration Process

### Step 1: Run Documentation Analysis
```bash
./scripts/migrate-docs.sh --dry-run
```

**Output:**
- Count of markdown files per repo
- Detection of docs/ folders
- Check for IDEACODE folders
- List of key documents
- Duplicate filename warnings

### Step 2: Review Migration Plan
Review the dry-run output to see:
- Where each file will be moved
- Naming convention suggestions
- Duplicate conflicts
- Archive strategy

### Step 3: Execute Migration
```bash
./scripts/migrate-docs.sh
```

**Actions:**
- Creates backups in `.migration-backups/docs-*`
- Migrates docs to hybrid centralized structure
- Renames files to follow naming convention
- Consolidates IDEACODE folders
- Moves archive to `/docs/archive/2024/{repo}/`
- Creates migration log in `.doc-migration.log`

### Step 4: Post-Migration Tasks
1. **Review migrated docs** in `/docs/`
2. **Update registry** with new document entries
3. **Fix cross-references** (update links between docs)
4. **Remove duplicates** (keep best version)
5. **Validate naming** (ensure all files follow convention)
6. **Test AI integration** (verify registry prevents duplicates)

---

## 📋 Migration Checklist

### Pre-Migration
- [ ] Commit all changes in source repos
- [ ] Run `migrate-docs.sh --dry-run` to review plan
- [ ] Check for critical docs that need special handling
- [ ] Backup important documentation separately

### During Migration
- [ ] Run `migrate-docs.sh`
- [ ] Monitor console output for errors
- [ ] Review `.doc-migration.log` for details

### Post-Migration
- [ ] Verify all docs copied successfully
- [ ] Check `.migration-backups/` for backup files
- [ ] Update `DOCUMENTATION-REGISTRY.json`
- [ ] Fix broken cross-references
- [ ] Remove duplicate files
- [ ] Validate filename conventions
- [ ] Test IDEACODE folder structure
- [ ] Update `.claude/CLAUDE.md` with new paths

### Validation
- [ ] All app docs in `/docs/{platform}/apps/{app}/`
- [ ] All library docs in `/docs/common/libs/{scope}/{lib}/`
- [ ] Monorepo docs in `/docs/`
- [ ] IDEACODE specs properly organized
- [ ] Archive in `/docs/archive/2024/`
- [ ] Registry up to date
- [ ] No orphaned docs in old locations

---

## 🛠️ Maintenance

### Adding New Documentation
1. **Check registry first** - Use IDEACODE MCP tool to check registry
2. **Follow naming convention** - Use `{type}-{context}-{topic}.md`
3. **Choose correct location:**
   - Monorepo-wide → `/docs/`
   - App-specific → `/docs/{platform}/apps/{app}/`
   - Library → `/docs/common/libs/{scope}/{lib}/`
4. **Update registry** - Add entry to `DOCUMENTATION-REGISTRY.json`

### Updating Existing Documentation
1. Update the file
2. Update `last_modified` in registry
3. Update cross-references if needed

### Archiving Documentation
1. Move to `/docs/archive/{year}/{source}/`
2. Update registry with archive location
3. Add note in original location (if applicable)

---

## 🔍 Finding Documentation

### By App
```bash
# All AVA documentation
ls docs/android/apps/ava/

# AVA IDEACODE specs
ls docs/android/apps/ava/ideacode/specs/
```

### By Library
```bash
# Voice recognition library
ls docs/common/libs/voice/feature-recognition/

# Design system
ls docs/common/libs/shared/ui-design-system/
```

### By Type
```bash
# All API docs
find docs -name "api-*.md"

# All developer guides
find docs -name "dev-*.md"

# All specs
find docs -name "spec-*.md"
```

### By Topic
```bash
# Search for "authentication"
grep -r "authentication" docs/

# Search registry
jq '.documents | to_entries[] | select(.value.description | contains("auth"))' \
   docs/ideacode/registries/DOCUMENTATION-REGISTRY.json
```

---

## 📊 Expected Results

### Before Migration
```
AVA AI/docs/          → 30-50 files (scattered, duplicates)
VoiceOS/docs/         → 25-40 files (inconsistent naming)
AVAConnect/docs/      → 15-25 files (mixed structure)
Avanues/docs/         → 20-35 files (multiple locations)
WebAvanue/docs/       → 10-20 files (minimal structure)
──────────────────────────────────────────────────
Total: ~100-170 files, no standard, duplicates
```

### After Migration
```
MainAvanues/docs/
├── 6 root-level docs (FLAT)
├── ideacode/ (monorepo-wide)
├── android/apps/{4 apps}/ (organized)
├── common/libs/{25+ libs}/ (organized)
├── web/apps/webavanue/ (organized)
└── archive/2024/ (historical)
──────────────────────────────────────────────────
Total: Same files, organized, no duplicates, registry-tracked
```

**Benefits:**
- ✅ Single source of truth
- ✅ Consistent naming
- ✅ Registry prevents duplicates
- ✅ Easy to find docs
- ✅ AI-friendly structure
- ✅ Mirrors code organization

---

## 📚 Related Documents

- [MONOREPO-STRUCTURE.md](./MONOREPO-STRUCTURE.md) - Complete monorepo structure
- [DOCUMENTATION-STRUCTURE-FINAL.md](./migration-analysis/DOCUMENTATION-STRUCTURE-FINAL.md) - Detailed design
- [MONOREPO-RESEARCH-FINDINGS.md](./migration-analysis/MONOREPO-RESEARCH-FINDINGS.md) - Industry research
- [README.md](./README.md) - Monorepo overview

---

## 🔧 Scripts

- `scripts/migrate-docs.sh` - Documentation migration script
- `scripts/migrate-docs.sh --dry-run` - Preview migration without changes
- `scripts/validate-docs.sh` - Validate documentation structure (TODO)
- `scripts/update-registry.sh` - Update registry from filesystem (TODO)

---

**Last Updated:** 2025-11-24
**Author:** IDEACODE Framework
**Maintainer:** Manoj Jhawar (manoj@ideahq.net)
