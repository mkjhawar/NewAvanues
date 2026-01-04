#!/bin/bash
# worktree-cleanup.sh - Automatically remove worktrees for merged/deleted branches
# Usage: ./scripts/worktree-cleanup.sh [--dry-run] [--force]
#
# Removes worktrees when:
#   1. Branch was merged to main/Avanues-Main
#   2. Branch no longer exists on remote
#   3. Branch was deleted locally
#
# Safety: Always prompts before removal unless --force is used

set -e

DRY_RUN=false
FORCE=false
MAIN_BRANCHES=("main" "Avanues-Main" "master")

# Parse arguments
for arg in "$@"; do
    case $arg in
        --dry-run) DRY_RUN=true ;;
        --force) FORCE=true ;;
        --help|-h)
            echo "Usage: $0 [--dry-run] [--force]"
            echo ""
            echo "Options:"
            echo "  --dry-run  Show what would be removed without removing"
            echo "  --force    Remove without prompting"
            exit 0
            ;;
    esac
done

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

echo "Fetching latest from remote..."
git fetch --prune origin 2>/dev/null || true

echo ""
echo "Analyzing worktrees..."
echo "======================"

STALE_WORKTREES=()
REASONS=()

# Get all worktrees except the main one
while IFS= read -r line; do
    case "$line" in
        worktree*)
            WORKTREE_PATH="${line#worktree }"
            ;;
        branch*)
            BRANCH="${line#branch refs/heads/}"

            # Skip the main worktree
            if [ "$WORKTREE_PATH" = "$REPO_ROOT" ]; then
                continue
            fi

            REASON=""

            # Check 1: Branch merged to main?
            for main in "${MAIN_BRANCHES[@]}"; do
                if git branch --merged "$main" 2>/dev/null | grep -q "^\s*$BRANCH$"; then
                    REASON="merged to $main"
                    break
                fi
            done

            # Check 2: Branch deleted on remote?
            if [ -z "$REASON" ]; then
                if ! git show-ref --verify --quiet "refs/remotes/origin/$BRANCH" 2>/dev/null; then
                    # Check if it ever existed on remote
                    if git config "branch.$BRANCH.remote" >/dev/null 2>&1; then
                        REASON="deleted on remote"
                    fi
                fi
            fi

            # Check 3: Branch deleted locally but worktree remains?
            if [ -z "$REASON" ]; then
                if ! git show-ref --verify --quiet "refs/heads/$BRANCH" 2>/dev/null; then
                    REASON="branch no longer exists"
                fi
            fi

            if [ -n "$REASON" ]; then
                STALE_WORKTREES+=("$WORKTREE_PATH")
                REASONS+=("$BRANCH ($REASON)")
            fi
            ;;
    esac
done < <(git worktree list --porcelain)

# Also check for worktrees with missing directories
while IFS= read -r line; do
    if [[ "$line" == *"prunable"* ]]; then
        echo "Found prunable worktree metadata (will be cleaned)"
    fi
done < <(git worktree list --porcelain 2>&1)

if [ ${#STALE_WORKTREES[@]} -eq 0 ]; then
    echo "No stale worktrees found. All worktrees are active."
    echo ""
    echo "Current worktrees:"
    git worktree list
    exit 0
fi

echo ""
echo "Found ${#STALE_WORKTREES[@]} stale worktree(s):"
echo ""
for i in "${!STALE_WORKTREES[@]}"; do
    echo "  [$((i+1))] ${REASONS[$i]}"
    echo "      Path: ${STALE_WORKTREES[$i]}"
    echo ""
done

if [ "$DRY_RUN" = true ]; then
    echo "[DRY RUN] Would remove the above worktrees."
    exit 0
fi

if [ "$FORCE" = false ]; then
    echo "Remove these worktrees? (y/N/all) "
    read -r REPLY
    case "$REPLY" in
        y|Y)
            # Remove one by one with confirmation
            for i in "${!STALE_WORKTREES[@]}"; do
                echo "Remove ${REASONS[$i]}? (y/N) "
                read -r CONFIRM
                if [[ "$CONFIRM" =~ ^[Yy]$ ]]; then
                    echo "Removing: ${STALE_WORKTREES[$i]}"
                    git worktree remove "${STALE_WORKTREES[$i]}" --force 2>/dev/null || rm -rf "${STALE_WORKTREES[$i]}"
                fi
            done
            ;;
        all|ALL)
            for path in "${STALE_WORKTREES[@]}"; do
                echo "Removing: $path"
                git worktree remove "$path" --force 2>/dev/null || rm -rf "$path"
            done
            ;;
        *)
            echo "Aborted."
            exit 0
            ;;
    esac
else
    for path in "${STALE_WORKTREES[@]}"; do
        echo "Removing: $path"
        git worktree remove "$path" --force 2>/dev/null || rm -rf "$path"
    done
fi

echo ""
echo "Pruning stale worktree metadata..."
git worktree prune

echo ""
echo "Cleanup complete!"
echo ""
echo "Remaining worktrees:"
git worktree list
