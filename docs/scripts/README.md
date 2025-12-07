# VOS4 Scripts Collection

**Last Updated:** 2025-10-10 09:58:34 PDT
**Location:** `/docs/scripts/`
**Purpose:** Consolidated repository for all VOS4 automation, development, and maintenance scripts

---

## Overview

This directory contains all scripts used for VOS4 development, maintenance, and automation. Scripts are organized by purpose into subdirectories for easy discovery and maintenance.

**Why Consolidated:**
- ✅ Single source of truth for all scripts
- ✅ Easy to find and maintain
- ✅ Prevents duplicate scripts
- ✅ Clear categorization by purpose
- ✅ Part of documentation (scripts document workflows)

---

## Directory Structure

```
docs/scripts/
├── agent-tools/        # Scripts for AI agent automation
├── audit/             # Audit and compliance scripts
├── build/             # Build and test automation
├── development/       # Development tools and utilities
└── README.md          # This file
```

---

## Agent Tools (`/agent-tools/`)

**Purpose:** Scripts designed to be used by AI agents (Claude Code) for automated code maintenance

| Script | Purpose | Usage |
|--------|---------|-------|
| `analyze_errors.py` | Analyze build errors and suggest fixes | `python3 analyze_errors.py <error_log>` |
| `analyze_imports.sh` | Analyze import statements in Kotlin files | `./analyze_imports.sh` |
| `enable-accessibility-adb.sh` | Enable accessibility service via ADB | `./enable-accessibility-adb.sh` |
| `fix-all-voicecursor-redundancy.sh` | Fix redundant package names in VoiceCursor | `./fix-all-voicecursor-redundancy.sh` |
| `fix-voicecursor-redundancy.sh` | Fix specific VoiceCursor redundancy | `./fix-voicecursor-redundancy.sh` |
| `fix_warnings.sh` | Auto-fix common compiler warnings | `./fix_warnings.sh` |
| `organize_imports.sh` | Organize and cleanup Kotlin imports | `./organize_imports.sh` |
| `safe_import_cleanup.sh` | Safely remove unused imports | `./safe_import_cleanup.sh` |
| `setup-modules.sh` | Setup module structure | `./setup-modules.sh` |
| `targeted_import_cleanup.sh` | Targeted import cleanup | `./targeted_import_cleanup.sh` |

**When to Use:**
- During code refactoring
- After major package reorganizations
- When fixing import issues
- For automated code cleanup

---

## Audit Scripts (`/audit/`)

**Purpose:** Scripts for auditing project structure, documentation, and compliance

| Script | Purpose | Usage |
|--------|---------|-------|
| `audit_docs_structure.sh` | Audit module documentation structure | `./audit_docs_structure.sh` |

**When to Use:**
- Before major releases
- After adding/removing modules
- Monthly documentation review
- When verifying compliance with standards

**Example Output:**
```
=== MODULE DOCUMENTATION STRUCTURE AUDIT ===

=== MISSING MODULE DOCUMENTATION FOLDERS ===
(none)

=== EXTRA/DUPLICATE DOCUMENTATION FOLDERS ===
(none)

=== MODULE FOLDER STRUCTURE COMPLIANCE ===
📁 voice-accessibility
   ✅ COMPLETE - All standard folders present
```

---

## Build Scripts (`/build/`)

**Purpose:** Build automation, testing, and Git hooks

| Script | Purpose | Usage |
|--------|---------|-------|
| `coverage-guard.sh` | Enforce test coverage thresholds | `./coverage-guard.sh` |
| `fix-path-redundancy.sh` | Fix path redundancies in code | `./fix-path-redundancy.sh` |
| `generate-test.sh` | Generate test boilerplate | `./generate-test.sh <ClassName>` |
| `select-test-template.sh` | Select test template | `./select-test-template.sh` |
| `setup-hooks.sh` | Setup Git hooks | `./setup-hooks.sh` |

**When to Use:**
- Setting up development environment
- CI/CD pipelines
- Pre-commit checks
- Generating test scaffolding

---

## Development Tools (`/development/`)

**Purpose:** General development utilities and converters

| Script/Tool | Purpose | Language | Usage |
|-------------|---------|----------|-------|
| `code-indexer.py` | Index codebase for quick search | Python | `python3 code-indexer.py` |
| `sync-docs.sh` | Sync documentation between repos | Bash | `./sync-docs.sh` |
| `renameUuidManagerToUuidCreator.sh` | Rename UUIDManager to UUIDCreator | Bash | `./renameUuidManagerToUuidCreator.sh` |
| `test-dashboard.kt` | Test dashboard Kotlin code | Kotlin | (Code file, not executable) |
| `VoiceUIConverter.kt` | Convert VoiceUI components | Kotlin | (Code file, not executable) |
| `vos3-decoder/` | VOS3 decoder utilities | Various | See subfolder README |

**When to Use:**
- Code migration between versions
- Documentation synchronization
- Development utilities
- Code generation

---

## Module-Specific Scripts

Some scripts remain in module folders because they're tightly coupled to specific modules:

**Location:** `/modules/managers/CommandManager/`
- `fix_definitions.sh` - Fix command definitions
- `fix_all_categories.sh` - Fix all command categories

**Why Separate:** These scripts modify CommandManager-specific data structures and should be version-controlled with the module.

---

## Usage Guidelines

### Running Scripts

**From Project Root:**
```bash
cd /Volumes/M\ Drive/Coding/vos4
./docs/scripts/agent-tools/analyze_imports.sh
```

**From Scripts Directory:**
```bash
cd docs/scripts
./agent-tools/analyze_imports.sh
```

### Best Practices

1. **Always Check Help:**
   ```bash
   ./script-name.sh --help  # If available
   ```

2. **Review Before Running:**
   - Read script comments
   - Understand what it modifies
   - Check if it requires specific environment

3. **Backup Before Automation:**
   ```bash
   git status  # Ensure clean working tree
   git stash   # Or commit current work
   ```

4. **Test on Small Scope First:**
   - If possible, test on single file/module
   - Verify results before full run

---

## Creating New Scripts

### Naming Conventions

**Bash Scripts:**
- Use `kebab-case.sh` for multi-word names
- Use `snake_case.sh` for legacy compatibility
- Example: `analyze-imports.sh`, `fix_warnings.sh`

**Python Scripts:**
- Use `kebab-case.py` or `snake_case.py`
- Example: `code-indexer.py`, `analyze_errors.py`

### Script Template

```bash
#!/bin/bash
#
# Script Name: script-name.sh
# Purpose: Brief description of what this script does
# Usage: ./script-name.sh [arguments]
# Author: VOS4 Development Team
# Created: YYYY-MM-DD
#

set -e  # Exit on error
set -u  # Exit on undefined variable

# Constants
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Functions
function main() {
    echo "Script executing..."
    # Main logic here
}

# Execute
main "$@"
```

### Where to Put New Scripts

| Script Type | Location |
|------------|----------|
| AI agent automation | `/docs/scripts/agent-tools/` |
| Audit/compliance | `/docs/scripts/audit/` |
| Build/test automation | `/docs/scripts/build/` |
| General development | `/docs/scripts/development/` |
| Module-specific | Keep in module folder |

---

## Maintenance

### Regular Tasks

**Monthly:**
- Run `audit_docs_structure.sh` to verify documentation compliance
- Review and update script documentation
- Remove obsolete scripts

**After Major Changes:**
- Update scripts that depend on project structure
- Test all affected scripts
- Update this README if new scripts added

**Before Release:**
- Verify all scripts in `/build/` work correctly
- Test audit scripts for compliance
- Document any new scripts

---

## Troubleshooting

### Common Issues

**Permission Denied:**
```bash
chmod +x docs/scripts/path/to/script.sh
```

**Script Not Found:**
```bash
# Ensure you're in project root
pwd  # Should show: /Volumes/M Drive/Coding/vos4

# Or use absolute path
/Volumes/M\ Drive/Coding/vos4/docs/scripts/...
```

**Environment Issues:**
```bash
# Some scripts expect to be run from project root
cd /Volumes/M\ Drive/Coding/vos4
./docs/scripts/...
```

---

## Script Dependencies

### Required Tools

Most scripts require:
- **Bash:** 4.0+ (macOS default is 3.2, consider upgrading)
- **Python:** 3.8+ (for .py scripts)
- **Git:** 2.x
- **Android SDK:** For ADB scripts
- **Kotlin Compiler:** For .kt files

### Installing Dependencies

```bash
# Bash 4+ (optional upgrade)
brew install bash

# Python 3
brew install python3

# Android SDK (for ADB)
brew install --cask android-platform-tools
```

---

## Migration Notes

**Previous Locations:** Scripts were previously scattered across:
- `/agent-tools/` → Now in `/docs/scripts/agent-tools/`
- `/tools/` → Now in `/docs/scripts/development/`
- `/scripts/` → Now in `/docs/scripts/build/`
- `/` (root) → Moved to appropriate subdirectories

**Consolidation Date:** 2025-10-10
**Reason:** Centralize all automation scripts for easier maintenance and discovery

**Breaking Changes:**
- Update any external references to old script paths
- Git hooks may need path updates if they referenced old locations
- CI/CD pipelines should be updated to new paths

---

## Related Documentation

- `/CLAUDE.md` - Project structure and agent instructions
- `/docs/voiceos-master/standards/NAMING-CONVENTIONS.md` - Naming standards
- `/Volumes/M Drive/Coding/Warp/Agent-Instructions/VOS4-DOCUMENTATION-PROTOCOL.md` - Documentation standards

---

## Contributing

### Adding a New Script

1. Create script in appropriate subdirectory
2. Add header comment with purpose and usage
3. Make executable: `chmod +x script-name.sh`
4. Test thoroughly
5. Update this README with entry in relevant table
6. Commit with descriptive message

### Modifying Existing Scripts

1. Document reason for change in commit message
2. Update script header comments if usage changes
3. Test on clean environment
4. Update README if public interface changes

---

**Last Updated:** 2025-10-10 09:58:34 PDT
**Maintained By:** VOS4 Development Team
**Questions:** Refer to `/CLAUDE.md` or project documentation
