# /i.version - Automatic Version Management

**Usage:** `/i.version [command]`

## Commands

| Command | Action | Example |
|---------|--------|---------|
| (none) | Detect current version | `/i.version` |
| `.detect` | Show version info | `/i.version .detect` |
| `.patch` | Increment patch (11.0.0 → 11.0.1) | `/i.version .patch` |
| `.minor` | Increment minor (11.0.0 → 11.1.0) | `/i.version .minor` |
| `.major` | Increment major (11.0.0 → 12.0.0) | `/i.version .major` |
| `.sync` | Sync current version to all files | `/i.version .sync` |
| `.check` | Check version consistency | `/i.version .check` |

## What It Does

**Automatically manages version across:**
- `VERSION` file (source of truth)
- `version-info.json`
- `.ideacode/config.ideacode`
- `.ideacode/config.yml`
- `.ideacode/config.idc`
- `ideacode-mcp/package.json`
- `CLAUDE.md` files

## Examples

```bash
# Check current version
/i.version

# Bump patch version (bug fixes)
/i.version .patch

# Bump minor version (new features)
/i.version .minor

# Bump major version (breaking changes)
/i.version .major

# Just sync without bumping
/i.version .sync
```

## Workflow

**When you make changes:**

1. **Bug fix** → `/i.version .patch` (11.0.0 → 11.0.1)
2. **New feature** → `/i.version .minor` (11.0.0 → 11.1.0)
3. **Breaking change** → `/i.version .major` (11.0.0 → 12.0.0)

The system automatically:
- ✅ Updates all config files
- ✅ Updates package.json files
- ✅ Updates documentation
- ✅ Checks consistency
- ✅ Reminds you to commit

## Implementation

Runs: `/Volumes/M-Drive/Coding/ideacode/scripts/auto-version.sh`

---

**Version:** 11.0
**Auto-invoked:** No (manual command)
