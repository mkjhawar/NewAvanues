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
  echo "Run 'git status' to see uncommitted changes."
  exit 1
fi

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
  echo "❌ Error: Not in a git repository."
  exit 1
fi

echo "Syncing docs from $CURRENT_BRANCH to: $BRANCHES"
echo ""

# Track success/failure
SYNC_SUCCESS=true
SYNCED_BRANCHES=()
FAILED_BRANCHES=()

for branch in $BRANCHES; do
  if [ "$branch" != "$CURRENT_BRANCH" ]; then
    echo "→ Syncing to $branch..."
    
    # Check if branch exists
    if git show-ref --verify --quiet refs/heads/$branch; then
      # Checkout target branch
      if git checkout $branch; then
        # Copy docs from source branch
        if git checkout $CURRENT_BRANCH -- docs/; then
          # Add and commit changes
          git add docs/
          if git commit -m "docs: Sync documentation from $CURRENT_BRANCH

- Updated documentation structure and content
- Synced at $TIMESTAMP
- Source branch: $CURRENT_BRANCH"; then
            echo "✅ Successfully synced to $branch"
            SYNCED_BRANCHES+=($branch)
          else
            echo "⚠️  No changes to sync to $branch (already up to date)"
            SYNCED_BRANCHES+=($branch)
          fi
        else
          echo "❌ Failed to copy docs to $branch"
          FAILED_BRANCHES+=($branch)
          SYNC_SUCCESS=false
        fi
      else
        echo "❌ Failed to checkout $branch"
        FAILED_BRANCHES+=($branch)
        SYNC_SUCCESS=false
      fi
    else
      echo "❌ Branch $branch does not exist"
      FAILED_BRANCHES+=($branch)
      SYNC_SUCCESS=false
    fi
    echo ""
  fi
done

# Return to original branch
echo "→ Returning to $CURRENT_BRANCH..."
if git checkout $CURRENT_BRANCH; then
  echo "✅ Returned to $CURRENT_BRANCH"
else
  echo "❌ Failed to return to $CURRENT_BRANCH"
  SYNC_SUCCESS=false
fi

echo ""
echo "=== Sync Summary ==="
echo "Source branch: $CURRENT_BRANCH"
echo "Timestamp: $TIMESTAMP"

if [ ${#SYNCED_BRANCHES[@]} -gt 0 ]; then
  echo "✅ Successfully synced branches: ${SYNCED_BRANCHES[*]}"
fi

if [ ${#FAILED_BRANCHES[@]} -gt 0 ]; then
  echo "❌ Failed to sync branches: ${FAILED_BRANCHES[*]}"
fi

if $SYNC_SUCCESS; then
  echo ""
  echo "🎉 Documentation sync completed successfully!"
  echo ""
  echo "Next steps:"
  echo "1. Review the synced documentation in each branch"
  echo "2. Push changes to remote if needed: git push origin [branch-name]"
  echo "3. Verify documentation consistency across branches"
else
  echo ""
  echo "⚠️  Documentation sync completed with errors."
  echo "Please review the failed branches and resolve issues manually."
  exit 1
fi