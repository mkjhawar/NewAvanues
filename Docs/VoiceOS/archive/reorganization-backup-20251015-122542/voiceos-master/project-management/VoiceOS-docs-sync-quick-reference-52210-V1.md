# Documentation Sync - Quick Reference

**Location:** `/Volumes/M Drive/Coding/Warp/vos4`  
**Script:** `sync-docs.sh`  
**Strategy:** `docs/DOCUMENTATION-MERGE-STRATEGY.md`

## Quick Commands

### Automated Sync (Recommended)
```bash
cd "/Volumes/M Drive/Coding/Warp/vos4"
./sync-docs.sh
```

### Manual Sync Process
```bash
# Current branch: vos4-legacyintegration (example)
git checkout VOS4
git checkout vos4-legacyintegration -- docs/
git add docs/ && git commit -m "docs: Sync from vos4-legacyintegration"

git checkout main  
git checkout vos4-legacyintegration -- docs/
git add docs/ && git commit -m "docs: Sync from vos4-legacyintegration"

git checkout vos4-legacykeyboard
git checkout vos4-legacyintegration -- docs/  
git add docs/ && git commit -m "docs: Sync from vos4-legacykeyboard"

git checkout vos4-legacyintegration
```

## Pre-Sync Checklist
- [ ] All documentation changes committed
- [ ] Working directory clean (`git status`)
- [ ] In correct directory: `/Volumes/M Drive/Coding/Warp/vos4`

## What Gets Synced
✅ **YES - Sync These:**
- `/docs/modules/` - Module documentation
- `/docs/voiceos-master/` - System documentation  
- `/docs/documentation-control/` - Standards and control
- `/docs/templates/` - Documentation templates
- `/docs/archive/` - Historical documentation
- Root documentation files in `/docs/`

❌ **NO - Don't Sync:**
- `/docs/coding/` - Branch-specific development notes
- Code files (`/modules/`, `/apps/`)
- Build configurations
- Git files

## Branch Structure
```
main ← sync ← vos4-legacyintegration (current)
VOS4 ← sync ← vos4-legacyintegration (current)  
vos4-legacykeyboard ← sync ← vos4-legacyintegration (current)
```

## Troubleshooting

### "Uncommitted changes" Error
```bash
git status                    # See what's uncommitted
git add docs/                 # Stage documentation changes
git commit -m "docs: [description]"  # Commit changes
./sync-docs.sh               # Re-run sync
```

### Branch Not Found
```bash
git branch -a               # List all branches
git checkout [missing-branch]  # Switch to create local branch
git checkout vos4-legacyintegration  # Return to source
./sync-docs.sh             # Re-run sync
```

### Sync Failed
```bash
git checkout vos4-legacyintegration  # Return to source branch
git status                          # Check current state
# Fix issues manually, then re-run sync
```

## Verification
After sync, check:
```bash
# Compare docs structure across branches
git checkout VOS4 && ls docs/
git checkout main && ls docs/  
git checkout vos4-legacykeyboard && ls docs/
git checkout vos4-legacyintegration  # Return to working branch
```

---
**Created:** 2025-09-09  
**Usage:** Run before any major documentation changes