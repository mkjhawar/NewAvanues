#!/bin/bash

# Claude Code Enhanced Status Line (v10.0)
# Format: IDEACODE v# | repo | branch | git-stats | context-indicator | model | CLI version
# Features: Auto-save at 80%, context warnings, git status

# Read JSON input from stdin
input=$(cat)

# Extract values
model_name=$(echo "$input" | jq -r '.model.display_name // "Claude"')
cwd=$(echo "$input" | jq -r '.workspace.current_dir // .cwd // "~"')

# Get the RUNNING Claude Code version from JSON input
running_version=$(echo "$input" | jq -r '.version // "unknown"')
version="$running_version"

# Get short model name (e.g., "Sonnet 4.5")
model_short=$(echo "$model_name" | sed 's/Claude //' | sed 's/3.5//' | xargs)

# Extract cost and session info from Claude Code JSON
session_id=$(echo "$input" | jq -r '.session_id // ""')
total_cost=$(echo "$input" | jq -r '.cost.total_cost_usd // 0')
duration_ms=$(echo "$input" | jq -r '.cost.total_duration_ms // 0')
lines_added=$(echo "$input" | jq -r '.cost.total_lines_added // 0')
lines_removed=$(echo "$input" | jq -r '.cost.total_lines_removed // 0')

# Format cost (if > 0)
cost_display=""
if [[ "$total_cost" != "0" ]] && [[ "$total_cost" != "null" ]]; then
    cost_display=$(printf "\$%.2f" "$total_cost" 2>/dev/null || echo "")
fi

# Format duration (ms to minutes)
duration_display=""
if [[ "$duration_ms" != "0" ]] && [[ "$duration_ms" != "null" ]]; then
    duration_min=$((duration_ms / 60000))
    if [[ $duration_min -gt 0 ]]; then
        duration_display="${duration_min}m"
    fi
fi

# Short session ID (first 8 chars)
session_short=""
if [[ -n "$session_id" ]] && [[ "$session_id" != "null" ]]; then
    session_short="${session_id:0:8}"
fi

# Get IDEACODE framework version from config
ideacode_version="?"
if [[ -f "$cwd/.ideacode/config.ideacode" ]]; then
    ideacode_version=$(jq -r '.version // "?"' "$cwd/.ideacode/config.ideacode" 2>/dev/null)
elif [[ -f "$cwd/.ideacode/config.yml" ]]; then
    ideacode_version=$(grep '^version:' "$cwd/.ideacode/config.yml" 2>/dev/null | sed 's/version: *"//' | sed 's/"//' | xargs)
fi

# Git info with detailed status
repo=""
branch=""
git_stats=""
git_dirty=""
if git -C "$cwd" rev-parse --git-dir > /dev/null 2>&1; then
    branch=$(git -C "$cwd" branch --show-current 2>/dev/null || echo "detached")
    repo=$(basename "$(git -C "$cwd" rev-parse --show-toplevel 2>/dev/null)" 2>/dev/null || echo "unknown")

    # Get detailed git status (+added, ~modified, -deleted)
    status_output=$(git -C "$cwd" status --porcelain 2>/dev/null)
    if [[ -n "$status_output" ]]; then
        git_dirty="*"  # Mark branch as dirty
        added=$(echo "$status_output" | grep -c "^??" 2>/dev/null) || added=0
        modified=$(echo "$status_output" | grep -cE "^ M|^M |^MM" 2>/dev/null) || modified=0
        deleted=$(echo "$status_output" | grep -cE "^ D|^D " 2>/dev/null) || deleted=0
        staged=$(echo "$status_output" | grep -cE "^[MADRC]" 2>/dev/null) || staged=0

        parts=""
        [[ "$added" -gt 0 ]] && parts="+$added"
        [[ "$modified" -gt 0 ]] && parts="$parts ~$modified"
        [[ "$deleted" -gt 0 ]] && parts="$parts -$deleted"
        [[ "$staged" -gt 0 ]] && parts="$parts S:$staged"
        git_stats=$(echo "$parts" | xargs)
    fi
fi

# Context status - simple indicator based on transcript size
transcript_path=$(echo "$input" | jq -r '.transcript_path // ""')
context_indicator=""
context_saved=""

if [[ -f "$transcript_path" ]]; then
    transcript_size=$(wc -c < "$transcript_path" 2>/dev/null || echo "0")

    # Check if context was saved
    save_marker="$HOME/.claude/.context_saved_$(basename "$transcript_path")"
    if [[ -f "$save_marker" ]]; then
        context_saved=" SAVED"
    fi

    # Simple size-based indicator (transcript bytes, not token estimation)
    # Small: <500KB, Medium: 500KB-1MB, Large: 1MB-2MB, Critical: >2MB
    if [[ $transcript_size -gt 2000000 ]]; then
        context_indicator="[!] /compact now$context_saved"
        # Auto-save if not already saved
        if [[ ! -f "$save_marker" ]]; then
            save_dir="$cwd/contextsave"
            mkdir -p "$save_dir" 2>/dev/null
            timestamp=$(date +%Y%m%d-%H%M%S)
            save_file="$save_dir/context-$timestamp.md"
            {
                echo "# Context Save - $timestamp"
                echo "**Repository:** $repo:$branch"
                echo "**Saved:** $(date)"
                echo "---"
                cat "$transcript_path"
            } > "$save_file" 2>/dev/null && touch "$save_marker"
            context_saved=" SAVED"
            context_indicator="[!] /compact now$context_saved"
            # Cleanup old saves
            cd "$save_dir" 2>/dev/null && ls -t context-*.md 2>/dev/null | tail -n +4 | xargs rm -f 2>/dev/null
        fi
    elif [[ $transcript_size -gt 1000000 ]]; then
        context_indicator="[*] consider /compact$context_saved"
    elif [[ $transcript_size -gt 500000 ]]; then
        context_indicator="[~] growing$context_saved"
    else
        context_indicator="ok$context_saved"
    fi
fi

# Check for newer Claude Code version (cached hourly)
version_cache="$HOME/.claude/.version_check"
newest_version="$version"
cache_stale=true
if [[ -f "$version_cache" ]]; then
    cache_age=$(($(date +%s) - $(stat -f %m "$version_cache" 2>/dev/null || echo 0)))
    if [[ $cache_age -lt 3600 ]]; then
        cache_stale=false
        newest_version=$(cat "$version_cache" 2>/dev/null || echo "$version")
    fi
fi
if [[ "$cache_stale" == true ]]; then
    latest=$(curl -sf --max-time 2 https://registry.npmjs.org/@anthropic-ai/claude-code/latest 2>/dev/null | jq -r '.version // ""')
    if [[ -n "$latest" ]] && [[ "$latest" != "null" ]] && [[ "$latest" != "" ]]; then
        echo "$latest" > "$version_cache"
        newest_version="$latest"
    elif [[ -f "$version_cache" ]]; then
        newest_version=$(cat "$version_cache" 2>/dev/null || echo "$version")
    fi
fi

# Build statusline
status_parts=()

# IDEACODE version
[[ "$ideacode_version" != "?" ]] && status_parts+=("IDEACODE v${ideacode_version}")

# Repository
[[ -n "$repo" ]] && status_parts+=("$repo")

# Branch (with * if dirty)
[[ -n "$branch" ]] && status_parts+=("${git_dirty}${branch}")

# Git stats (if any changes)
[[ -n "$git_stats" ]] && status_parts+=("$git_stats")

# Context indicator
[[ -n "$context_indicator" ]] && status_parts+=("$context_indicator")

# Model
status_parts+=("$model_short")

# Detect API vs Subscription mode
is_api_mode=false
if [[ -n "$ANTHROPIC_API_KEY" ]]; then
    is_api_mode=true
fi

# Session cost (API mode only) or duration (subscription mode)
session_info=""
if [[ "$is_api_mode" == true ]] && [[ -n "$cost_display" ]]; then
    session_info="$cost_display"
    [[ -n "$duration_display" ]] && session_info="$session_info $duration_display"
else
    # Subscription mode - just show duration
    [[ -n "$duration_display" ]] && session_info="$duration_display"
fi
[[ -n "$session_info" ]] && status_parts+=("$(echo $session_info | xargs)")

# Lines changed
if [[ "$lines_added" != "0" ]] || [[ "$lines_removed" != "0" ]]; then
    lines_display="+${lines_added}/-${lines_removed}"
    status_parts+=("$lines_display")
fi

# Active MCP server (read from Claude Code config)
mcp_config="$HOME/Library/Application Support/Claude Code/mcp.json"
if [[ -f "$mcp_config" ]]; then
    mcp_name=$(jq -r '.mcpServers | keys[0] // empty' "$mcp_config" 2>/dev/null)
    [[ -n "$mcp_name" ]] && status_parts+=("$mcp_name")
fi

# CLI version - only show both if update available
if [[ "$version" != "$newest_version" ]]; then
    status_parts+=("v${version} -> v${newest_version}")
else
    status_parts+=("v${version}")
fi

# Manually join with " | " separator
status=""
for i in "${!status_parts[@]}"; do
    if [[ $i -eq 0 ]]; then
        status="${status_parts[$i]}"
    else
        status="${status} | ${status_parts[$i]}"
    fi
done

# Output
printf "%s" "$status"
