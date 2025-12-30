#!/bin/bash

# Claude Code Enhanced Status Line (v13.0)
# Format: IDEACODE v# | Repo | Branch | MEM: OK/COMPACT/CLEAR | HANDOVER: file | v#>v# | #Tokens
# Features: Memory status, handover tracking, token count, version comparison

# Read JSON input from stdin
input=$(cat)

# Extract values from Claude Code JSON
model_name=$(echo "$input" | jq -r '.model.display_name // "Claude"')
cwd=$(echo "$input" | jq -r '.workspace.current_dir // .cwd // "~"')
running_version=$(echo "$input" | jq -r '.version // "unknown"')
transcript_path=$(echo "$input" | jq -r '.transcript_path // ""')

# Get IDEACODE framework version
ideacode_version="?"
central_version_file="/Volumes/M-Drive/Coding/ideacode/version-info.json"
if [[ -f "$central_version_file" ]]; then
    ideacode_version=$(jq -r '.framework_version // "?"' "$central_version_file" 2>/dev/null)
fi

# Git info
repo=""
branch=""
if git -C "$cwd" rev-parse --git-dir > /dev/null 2>&1; then
    branch=$(git -C "$cwd" branch --show-current 2>/dev/null || echo "detached")
    repo=$(basename "$(git -C "$cwd" rev-parse --show-toplevel 2>/dev/null)" 2>/dev/null || echo "unknown")

    # Add dirty indicator
    if [[ -n "$(git -C "$cwd" status --porcelain 2>/dev/null)" ]]; then
        branch="*${branch}"
    fi
else
    repo=$(basename "$cwd" 2>/dev/null || echo "unknown")
fi

# Memory/Context status and token count
mem_status="OK"
handover_display=""
token_count=0

if [[ -f "$transcript_path" ]]; then
    transcript_size=$(wc -c < "$transcript_path" 2>/dev/null || echo "0")
    # Estimate tokens (~4 chars per token)
    token_count=$((transcript_size / 4))

    # Format token count (K for thousands)
    if [[ $token_count -gt 1000 ]]; then
        token_display="$((token_count / 1000))K"
    else
        token_display="$token_count"
    fi

    # Memory thresholds: <1MB = OK, 1-2MB = COMPACT, >2MB = CLEAR
    if [[ $transcript_size -gt 2000000 ]]; then
        mem_status="CLEAR"

        # Create handover document
        save_dir="$cwd/.claude"
        mkdir -p "$save_dir" 2>/dev/null
        timestamp=$(date +%Y%m%d-%H%M)
        handover_file=".last-handover-${timestamp}"
        handover_path="$save_dir/$handover_file"

        if [[ ! -f "$handover_path" ]]; then
            {
                echo "# Handover - $timestamp"
                echo "**Repo:** $repo | **Branch:** $branch"
                echo "**Created:** $(date)"
                echo ""
                echo "## Summary"
                echo "Context exceeded 2MB. Run /clear and resume with this file."
                echo ""
                echo "## Transcript"
                cat "$transcript_path"
            } > "$handover_path" 2>/dev/null

            # Cleanup old handovers (keep last 3)
            ls -t "$save_dir"/.last-handover-* 2>/dev/null | tail -n +4 | xargs rm -f 2>/dev/null
        fi

        handover_display="HANDOVER: $handover_file"
    elif [[ $transcript_size -gt 1000000 ]]; then
        mem_status="COMPACT"
    fi
else
    token_display="0"
fi

# Check for newer Claude Code version (cached hourly)
version_cache="$HOME/.claude/.version_check"
newest_version="$running_version"
cache_stale=true

if [[ -f "$version_cache" ]]; then
    cache_age=$(($(date +%s) - $(stat -f %m "$version_cache" 2>/dev/null || echo 0)))
    if [[ $cache_age -lt 3600 ]]; then
        cache_stale=false
        newest_version=$(cat "$version_cache" 2>/dev/null || echo "$running_version")
    fi
fi

if [[ "$cache_stale" == true ]]; then
    latest=$(curl -sf --max-time 2 https://registry.npmjs.org/@anthropic-ai/claude-code/latest 2>/dev/null | jq -r '.version // ""')
    if [[ -n "$latest" ]] && [[ "$latest" != "null" ]]; then
        echo "$latest" > "$version_cache"
        newest_version="$latest"
    fi
fi

# Version display
if [[ "$running_version" != "$newest_version" ]]; then
    version_display="v${running_version}>v${newest_version}"
else
    version_display="v${running_version}"
fi

# API auto-start (persistent - survives Claude Code exit)
api_status=""
API_URL="http://localhost:3850"
API_DIR="/Volumes/M-Drive/Coding/ideacode"

if curl -s --connect-timeout 0.5 "$API_URL/health" > /dev/null 2>&1; then
    api_status="API"
else
    # Start API with nohup so it persists after Claude Code exits
    if [[ -d "$API_DIR" ]]; then
        (cd "$API_DIR" && nohup npm run start > /dev/null 2>&1 &)
        sleep 1
        if curl -s --connect-timeout 0.5 "$API_URL/health" > /dev/null 2>&1; then
            api_status="API"
        fi
    fi
fi

# Build statusline
status_parts=()

# IDEACode version
status_parts+=("IDEACODE v${ideacode_version}")

# API status (only show if running)
[[ -n "$api_status" ]] && status_parts+=("$api_status")

# Repository
status_parts+=("$repo")

# Branch
status_parts+=("$branch")

# Memory status
status_parts+=("MEM: $mem_status")

# Handover (only if created)
[[ -n "$handover_display" ]] && status_parts+=("$handover_display")

# Claude Code version
status_parts+=("$version_display")

# Token count
status_parts+=("#${token_display}")

# Join with " | "
status=""
for i in "${!status_parts[@]}"; do
    if [[ $i -eq 0 ]]; then
        status="${status_parts[$i]}"
    else
        status="${status} | ${status_parts[$i]}"
    fi
done

printf "%s" "$status"
