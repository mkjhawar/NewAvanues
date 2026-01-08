#!/bin/bash
# worktree-add.sh - Create a new git worktree for parallel branch development
# Usage: ./scripts/worktree-add.sh <branch> [base-branch]
#
# Examples:
#   ./scripts/worktree-add.sh feature/new-ui           # Creates from current branch
#   ./scripts/worktree-add.sh feature/nlu-speed main   # Creates from main
#   ./scripts/worktree-add.sh VoiceOS-Development      # Uses existing remote branch

set -e

BRANCH="${1}"
BASE="${2:-HEAD}"
REPO_ROOT="$(git rev-parse --show-toplevel)"
REPO_NAME="$(basename "$REPO_ROOT")"
PARENT_DIR="$(dirname "$REPO_ROOT")"

# Sanitize branch name for folder (replace / with __)
FOLDER_NAME="${REPO_NAME}__${BRANCH//\//__}"
WORKTREE_PATH="${PARENT_DIR}/${FOLDER_NAME}"

if [ -z "$BRANCH" ]; then
    echo "Usage: $0 <branch> [base-branch]"
    echo ""
    echo "Examples:"
    echo "  $0 feature/new-ui           # Create from current branch"
    echo "  $0 feature/nlu-speed main   # Create from main"
    echo "  $0 VoiceOS-Development      # Use existing remote branch"
    exit 1
fi

# Check if worktree already exists
if [ -d "$WORKTREE_PATH" ]; then
    echo "Error: Worktree already exists at: $WORKTREE_PATH"
    echo "Use: ./scripts/worktree-remove.sh $WORKTREE_PATH"
    exit 1
fi

# Check if branch exists locally or remotely
if git show-ref --verify --quiet "refs/heads/$BRANCH"; then
    echo "Using existing local branch: $BRANCH"
    git worktree add "$WORKTREE_PATH" "$BRANCH"
elif git show-ref --verify --quiet "refs/remotes/origin/$BRANCH"; then
    echo "Tracking remote branch: origin/$BRANCH"
    git worktree add --track -b "$BRANCH" "$WORKTREE_PATH" "origin/$BRANCH"
else
    echo "Creating new branch '$BRANCH' from '$BASE'"
    git worktree add -b "$BRANCH" "$WORKTREE_PATH" "$BASE"
fi

echo ""
echo "Worktree created successfully!"
echo "  Path:   $WORKTREE_PATH"
echo "  Branch: $BRANCH"
echo ""
echo "To use: cd $WORKTREE_PATH"
