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

# Get repo root and name
repo_root=$(cd "$cwd" && git rev-parse --show-toplevel 2>/dev/null)
repo_name=$(basename "$repo_root")
git_dir="$repo_root/.git"

# Detect module/feature from path (if in subdirectory)
# e.g., /NewAvanues/Modules/VoiceOS -> VoiceOS
# e.g., /NewAvanues/Modules/AVAMagic/AVAUI -> AVAMagic
module_name=""
if [[ "$cwd" != "$repo_root" ]]; then
    relative_path="${cwd#$repo_root/}"
    # Extract first meaningful directory (skip Modules/, apps/, etc.)
    if [[ "$relative_path" == Modules/* ]]; then
        module_name=$(echo "$relative_path" | cut -d'/' -f2)
    elif [[ "$relative_path" == apps/* ]]; then
        module_name=$(echo "$relative_path" | cut -d'/' -f2)
    else
        module_name=$(echo "$relative_path" | cut -d'/' -f1)
    fi
fi

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
            # Generate worktree name with module and PID for readability
            if [[ -n "$module_name" ]]; then
                wt_name="${repo_name}__${module_name}__t$$"
            else
                wt_name="${repo_name}__main__t$$"
            fi
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

                # Add .gitignore to Worktrees if not exists
                if [[ ! -f "/Volumes/M-Drive/Worktrees/.gitignore" ]]; then
                    echo -e ".DS_Store\n*.swp\n*~" > /Volumes/M-Drive/Worktrees/.gitignore
                fi

                branch=$(cd "$repo_root" && git branch --show-current 2>/dev/null)

                if [[ -n "$branch" ]]; then
                    # Create worktree on same branch
                    (cd "$repo_root" && git worktree add "$wt_path" "$branch" 2>/dev/null) || \
                    (cd "$repo_root" && git worktree add "$wt_path" --detach 2>/dev/null)
                else
                    (cd "$repo_root" && git worktree add "$wt_path" --detach 2>/dev/null)
                fi

                if [[ -d "$wt_path" ]]; then
                    # Copy .claude settings to worktree
                    if [[ -d "$repo_root/.claude" ]]; then
                        cp -r "$repo_root/.claude" "$wt_path/.claude" 2>/dev/null
                    fi
                    # Copy .ideacode config
                    if [[ -d "$repo_root/.ideacode" ]]; then
                        cp -r "$repo_root/.ideacode" "$wt_path/.ideacode" 2>/dev/null
                    fi
                    # Remove .DS_Store files from copied directories
                    find "$wt_path/.claude" "$wt_path/.ideacode" -name ".DS_Store" -delete 2>/dev/null

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
