#!/bin/bash

# IDEACODE Startup Banner + Auto-Worktree
# Displays banner and auto-creates worktree if in main repo

# Read JSON input from stdin
input=$(cat)
cwd=$(echo "$input" | jq -r '.workspace.current_dir // .cwd // "."')

# Get IDEACODE version
ideacode_version="?"
if [[ -f "/Volumes/M-Drive/Coding/.ideacode/VERSION" ]]; then
    ideacode_version=$(cat /Volumes/M-Drive/Coding/.ideacode/VERSION | tr -d '[:space:]')
fi

# Get repo name
repo_name=$(basename "$cwd")
git_dir="$cwd/.git"

# Check if we're in a worktree (already isolated)
in_worktree=false
if [[ -f "$git_dir" ]]; then
    in_worktree=true
fi

# Large monorepos that need worktree isolation
MONOREPOS="NewAvanues VoiceOS Avanues"

# Worktree logic
worktree_msg=""
if [[ "$in_worktree" == "false" ]] && [[ -d "$git_dir" ]]; then
    # Check if this is a monorepo that needs isolation
    for mono in $MONOREPOS; do
        if [[ "$repo_name" == "$mono" ]]; then
            # Generate worktree name with parent shell PID
            wt_name="${repo_name}__t$$"
            wt_path="/Volumes/M-Drive/Worktrees/${wt_name}"

            # Check if worktree already exists
            if [[ -d "$wt_path" ]]; then
                worktree_msg="
║  WORKTREE EXISTS: ${wt_name}
║
║  Run:  cd ${wt_path} && claude
"
            else
                # Create the worktree
                mkdir -p /Volumes/M-Drive/Worktrees
                branch=$(cd "$cwd" && git branch --show-current 2>/dev/null)

                if [[ -n "$branch" ]]; then
                    # Create worktree on same branch
                    (cd "$cwd" && git worktree add "$wt_path" "$branch" 2>/dev/null) || \
                    (cd "$cwd" && git worktree add "$wt_path" --detach 2>/dev/null)
                else
                    (cd "$cwd" && git worktree add "$wt_path" --detach 2>/dev/null)
                fi

                if [[ -d "$wt_path" ]]; then
                    # Copy .claude settings to worktree
                    if [[ -d "$cwd/.claude" ]]; then
                        cp -r "$cwd/.claude" "$wt_path/.claude" 2>/dev/null
                    fi
                    # Copy .ideacode config
                    if [[ -d "$cwd/.ideacode" ]]; then
                        cp -r "$cwd/.ideacode" "$wt_path/.ideacode" 2>/dev/null
                    fi

                    worktree_msg="
║  WORKTREE CREATED: ${wt_name}
║  Branch: ${branch:-detached}
║
║  Run:  cd ${wt_path} && claude
"
                else
                    worktree_msg="
║  WARNING: Failed to create worktree
║  Working in main repo (may cause conflicts)
"
                fi
            fi
            break
        fi
    done
fi

# Already in worktree
if [[ "$in_worktree" == "true" ]]; then
    wt_info=$(cd "$cwd" && git worktree list --porcelain 2>/dev/null | head -1 | sed 's/worktree //')
    worktree_msg="
║  WORKTREE: $(basename "$cwd")
║  Isolated workspace - safe for multi-terminal
"
fi

cat << EOF

╔══════════════════════════════════════════════════════════════════════════╗
║                                                                          ║
║        ██╗██████╗ ███████╗ █████╗  ██████╗ ██████╗ ██████╗ ███████╗    ║
║        ██║██╔══██╗██╔════╝██╔══██╗██╔════╝██╔═══██╗██╔══██╗██╔════╝    ║
║        ██║██║  ██║█████╗  ███████║██║     ██║   ██║██║  ██║█████╗      ║
║        ██║██║  ██║██╔══╝  ██╔══██║██║     ██║   ██║██║  ██║██╔══╝      ║
║        ██║██████╔╝███████╗██║  ██║╚██████╗╚██████╔╝██████╔╝███████╗    ║
║        ╚═╝╚═════╝ ╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚═════╝ ╚═════╝ ╚══════╝    ║
║                                                                          ║
║         Intelligent Devices Enhanced Application v${ideacode_version}                ║
║                                                                          ║${worktree_msg}
╚══════════════════════════════════════════════════════════════════════════╝

EOF
