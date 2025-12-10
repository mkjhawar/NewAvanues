# Documentation Merge Strategy

**Created:** 2025-09-09 11:15:00 PDT  
**Purpose:** Keep documentation synchronized across all VOS4 branches

## Branch Structure
```
main (stable)
├── VOS4 (primary development)
├── vos4-legacyintegration (current - legacy integration work)
└── vos4-legacykeyboard (keyboard-specific legacy work)
```

## Merge Strategy

### What to Merge
Only merge documentation changes (`/docs/` folder):
- Module documentation (`/docs/modules/`)
- System documentation (`/docs/voiceos-master/`)
- Documentation control files (`/docs/documentation-control/`)
- Templates (`/docs/templates/`)
- Archive documentation (`/docs/archive/`)
- Strategy and control documents

### What NOT to Merge
- Code changes (`/modules/`, `/apps/`)
- Active development tracking (`/docs/coding/`) - branch-specific
- Build files (`build.gradle`, etc.)
- Configuration changes (`.properties`, etc.)
- Git-specific files (`.gitignore`, etc.)

### Current Documentation Structure
```
/docs/
├── archive/                    # Historical documentation
├── coding/                     # Branch-specific development notes (DO NOT MERGE)
├── documentation-control/      # Documentation standards and control
├── modules/                    # Module-specific documentation
├── templates/                  # Documentation templates
├── voiceos-master/            # System-wide documentation
└── DOCUMENTATION-MERGE-STRATEGY.md # This file
```

## Merge Process

### Manual Process

1. **Complete documentation updates on current branch**
   ```bash
   git add docs/
   git commit -m "docs: Update documentation structure and content"
   ```

2. **Merge to VOS4 branch**
   ```bash
   git checkout VOS4
   git checkout vos4-legacyintegration -- docs/
   git add docs/
   git commit -m "docs: Sync documentation from vos4-legacyintegration"
   ```

3. **Merge to main branch**
   ```bash
   git checkout main
   git checkout vos4-legacyintegration -- docs/
   git add docs/
   git commit -m "docs: Sync documentation from vos4-legacyintegration"
   ```

4. **Merge to vos4-legacykeyboard branch**
   ```bash
   git checkout vos4-legacykeyboard
   git checkout vos4-legacyintegration -- docs/
   git add docs/
   git commit -m "docs: Sync documentation from vos4-legacyintegration"
   ```

5. **Return to working branch**
   ```bash
   git checkout vos4-legacyintegration
   ```

### Automated Process
Use the `sync-docs.sh` script (see below) for automated synchronization.

## Automation Script

The `sync-docs.sh` script automates the documentation sync process:

```bash
#!/bin/bash
# Documentation sync script for VOS4 branches

CURRENT_BRANCH=$(git branch --show-current)
BRANCHES="VOS4 main vos4-legacykeyboard"
TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S %Z")

echo "=== Documentation Sync Script ==="
echo "Time: $TIMESTAMP"
echo "Current branch: $CURRENT_BRANCH"
echo ""

# Check for uncommitted changes
if ! git diff-index --quiet HEAD --; then
  echo "❌ Error: You have uncommitted changes. Please commit or stash them first."
  exit 1
fi

echo "Syncing docs from $CURRENT_BRANCH to: $BRANCHES"
echo ""

for branch in $BRANCHES; do
  if [ "$branch" != "$CURRENT_BRANCH" ]; then
    echo "→ Syncing to $branch..."
    git checkout $branch
    git checkout $CURRENT_BRANCH -- docs/
    git add docs/
    git commit -m "docs: Sync documentation from $CURRENT_BRANCH

- Updated documentation structure
- Synced at $TIMESTAMP"
    echo "✅ Synced to $branch"
    echo ""
  fi
done

git checkout $CURRENT_BRANCH
echo "✅ Documentation sync complete!"
echo "✅ Returned to $CURRENT_BRANCH"
```

## Usage Instructions

### Running the Sync Script
```bash
# Make sure you're in the VOS4 directory
cd "/Volumes/M Drive/Coding/vos4"

# Run the sync script
./sync-docs.sh
```

### Prerequisites
1. All documentation changes must be committed before running sync
2. Must have access to all target branches
3. Working directory must be clean (no uncommitted changes)

## Verification Steps
After merging, verify:
1. **Structure consistency**: All branches have identical `/docs/` structure
2. **Content accuracy**: Module documentation is properly organized
3. **No conflicts**: No merge conflicts in documentation
4. **Branch isolation**: Branch-specific `/docs/coding/` folders remain independent
5. **Commit history**: Clean commit messages for documentation syncs

## Conflict Resolution
If conflicts occur during sync:
1. **Identify conflict source**: Usually in shared documentation files
2. **Resolve manually**: Edit conflicted files to keep most recent content
3. **Test locally**: Verify documentation builds/renders correctly
4. **Commit resolution**: Clear commit message explaining resolution

## Schedule and Triggers

### Immediate Sync Required
- Major documentation reorganization
- New module documentation added
- Documentation standards updated
- Template changes

### Regular Schedule
- **Weekly sync**: Every Friday after documentation updates
- **Release sync**: Before any release or major merge to main
- **Monthly audit**: Verify all branches have consistent documentation

### As-Needed Basis
- After significant feature documentation
- When documentation standards change
- Before important demos or presentations

## Branch-Specific Notes

### vos4-legacyintegration (current)
- Primary documentation development branch
- All major documentation changes originate here
- Source for syncing to other branches

### VOS4
- Primary development branch
- Should receive documentation syncs regularly
- May have development-specific docs in `/docs/coding/`

### main
- Stable branch for releases
- Documentation should be production-ready
- Only receives thoroughly reviewed documentation

### vos4-legacykeyboard
- Feature-specific branch
- Receives general documentation syncs
- May have keyboard-specific documentation

## Best Practices

1. **Single source of truth**: Make documentation changes on one branch first
2. **Commit before sync**: Always commit documentation changes before syncing
3. **Review before merge**: Review documentation changes before syncing
4. **Test documentation**: Verify links, formatting, and content accuracy
5. **Clear commit messages**: Use descriptive messages for sync commits
6. **Regular syncs**: Don't let documentation drift between branches

## Troubleshooting

### Common Issues
- **Uncommitted changes**: Commit or stash changes before running sync
- **Branch not found**: Ensure all target branches exist locally
- **Merge conflicts**: Resolve conflicts in shared documentation files
- **Permission issues**: Ensure write access to all branches

### Recovery Steps
If sync fails:
1. Return to original branch: `git checkout [original-branch]`
2. Check git status: `git status`
3. Resolve any issues manually
4. Re-run sync script or perform manual sync

---
**Last Updated:** 2025-09-09 - Created comprehensive documentation merge strategy
**Author:** VOS4 Documentation Team
**Status:** Active - Use for all documentation synchronization