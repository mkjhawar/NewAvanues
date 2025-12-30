#!/bin/bash

# Claude Code Enhanced Status Line (v11.4.0)
# Format: IDEACODE v# | API-R:provider | repo | branch | git-stats | CXT: %% (K/K) | context-indicator | model | CLI version | RESTART CLIENT
# Features: Auto-save at 80%, context warnings, git status, version tracking, API status colors, RAG detection
# Indicators: API (basic), API-R:O (RAG+Ollama), API-R:AI (RAG+OpenAI), >>API-R:O<< (needs restart)
# RAG Providers: :O (Ollama - free, local), :AI (OpenAI - cloud, paid)
# Colors: Green API (running), Red flashing API (needs restart), Red RESTART CLIENT (version/instruction changes)

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

# Get context window limit for model
get_context_limit() {
    local model="$1"
    case "$model" in
        *"Opus 4.5"*) echo "200000" ;;
        *"Sonnet 4.5"*) echo "200000" ;;
        *"Haiku 4.5"*) echo "200000" ;;
        *"Opus 3"*) echo "200000" ;;
        *"Sonnet 3.5"*) echo "200000" ;;
        *"Haiku 3"*) echo "200000" ;;
        *) echo "200000" ;;  # Default fallback
    esac
}

context_limit=$(get_context_limit "$model_name")

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

# Get IDEACODE framework version from central version-info.json
ideacode_version="?"
central_version_file="/Volumes/M-Drive/Coding/ideacode/version-info.json"
if [[ -f "$central_version_file" ]]; then
    ideacode_version=$(jq -r '.framework_version // "?"' "$central_version_file" 2>/dev/null)
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

# Context status - enhanced with token usage display
transcript_path=$(echo "$input" | jq -r '.transcript_path // ""')
context_indicator=""
context_saved=""
context_display=""

if [[ -f "$transcript_path" ]]; then
    transcript_size=$(wc -c < "$transcript_path" 2>/dev/null || echo "0")

    # Status-based context indicator: OK, COMPACT, or CLEAR-{filename}
    # Thresholds: <1MB = OK, 1MB-2MB = COMPACT, >2MB = CLEAR (create handover)
    if [[ $transcript_size -gt 2000000 ]]; then
        # CLEAR state - create handover document
        save_dir="$cwd/contextsave"
        mkdir -p "$save_dir" 2>/dev/null
        timestamp=$(date +%Y%m%d-%H%M)
        handover_file="handover-$timestamp.md"
        handover_path="$save_dir/$handover_file"

        # Create handover document if not already exists
        if [[ ! -f "$handover_path" ]]; then
            {
                echo "# Handover Document - $timestamp"
                echo ""
                echo "**Repository:** $repo"
                echo "**Branch:** $branch"
                echo "**Created:** $(date)"
                echo ""
                echo "## Context Summary"
                echo "This conversation has exceeded the context window limit (>2MB)."
                echo "Use this handover document to resume work after running /clear."
                echo ""
                echo "## Next Steps"
                echo "1. Review the conversation transcript below"
                echo "2. Run /clear to start fresh"
                echo "3. Reference this document to resume where you left off"
                echo ""
                echo "---"
                echo ""
                echo "## Full Conversation Transcript"
                echo ""
                cat "$transcript_path"
            } > "$handover_path" 2>/dev/null

            # Cleanup old handovers (keep last 3)
            cd "$save_dir" 2>/dev/null && ls -t handover-*.md 2>/dev/null | tail -n +4 | xargs rm -f 2>/dev/null
        fi

        context_display="CXT: CLEAR-$handover_file"
    elif [[ $transcript_size -gt 1000000 ]]; then
        # COMPACT state - getting full, should compact soon
        context_display="CXT: COMPACT"
    else
        # OK state - normal usage
        context_display="CXT: OK"
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

# Track IDEACODE version changes and instruction file changes
restart_needed=false

# Get repo name for repo-specific tracking
repo_name=$(basename "$(git -C "$cwd" rev-parse --show-toplevel 2>/dev/null)" 2>/dev/null || basename "$cwd")

# Global version tracking (IDEACODE framework version applies to all repos)
version_last_file="$HOME/.claude/.ideacode_version_last"

# Per-repo checksum tracking (prevents false warnings when switching repos)
checksum_file="$HOME/.claude/.instruction_checksums_${repo_name}"

# Check IDEACODE version change
if [[ -f "$version_last_file" ]]; then
    last_version=$(cat "$version_last_file" 2>/dev/null)
    if [[ "$last_version" != "$ideacode_version" ]]; then
        restart_needed=true
    fi
else
    echo "$ideacode_version" > "$version_last_file" 2>/dev/null
fi

# Check instruction file changes (CLAUDE.md and config files)
# Get git repo root (consistent regardless of subfolder)
git_root=$(git -C "$cwd" rev-parse --show-toplevel 2>/dev/null || echo "$cwd")

instruction_files=(
    "/Volumes/M-Drive/Coding/.claude/CLAUDE.md"
    "$git_root/.claude/CLAUDE.md"
    "$git_root/.ideacode/config.ideacode"
)

current_checksums=""
for file in "${instruction_files[@]}"; do
    if [[ -f "$file" ]]; then
        checksum=$(md5 -q "$file" 2>/dev/null || echo "")
        if [[ -n "$current_checksums" ]]; then
            current_checksums="${current_checksums}"$'\n'"${file}:${checksum}"
        else
            current_checksums="${file}:${checksum}"
        fi
    fi
done

if [[ -f "$checksum_file" ]]; then
    last_checksums=$(cat "$checksum_file" 2>/dev/null)
    if [[ "$last_checksums" != "$current_checksums" ]]; then
        restart_needed=true
    fi
else
    echo "$current_checksums" > "$checksum_file" 2>/dev/null
fi

# Always update tracking files to current state (allows cross-session eventual consistency)
# This prevents lock-in where restart_needed stays true indefinitely
echo "$ideacode_version" > "$version_last_file" 2>/dev/null
echo "$current_checksums" > "$checksum_file" 2>/dev/null

# Build statusline
status_parts=()

# IDEACODE version (always show, use "?" if not found)
status_parts+=("IDEACODE v${ideacode_version}")

# Check if IDEACODE API is running, auto-start if not
api_marker="$HOME/.claude/.ideacode_api_started"
api_status=""

# Check if RAG is available (database exists and has data)
rag_available=false
rag_provider=""
rag_db="/Volumes/M-Drive/Coding/ideacode/ideacode-api/data/rag.db"
if [[ -f "$rag_db" ]]; then
    # Check if database has indexed data (commands table has rows)
    rag_count=$(sqlite3 "$rag_db" "SELECT COUNT(*) FROM command_library;" 2>/dev/null || echo "0")
    if [[ "$rag_count" -gt 0 ]]; then
        rag_available=true
        # Get the active provider with most embeddings (the one actually used)
        rag_provider=$(sqlite3 "$rag_db" "SELECT provider FROM embeddings_metadata WHERE is_active=1 ORDER BY total_embeddings DESC LIMIT 1;" 2>/dev/null || echo "")
        # Short provider name: ollama -> O, openai -> AI
        case "$rag_provider" in
            "ollama") rag_provider=":O" ;;
            "openai") rag_provider=":AI" ;;
            *) rag_provider="" ;;
        esac
    fi
fi

if curl -s --connect-timeout 0.5 http://localhost:3847/health > /dev/null 2>&1; then
    # API is running - show API-R:provider if RAG available, API if not
    if [[ "$restart_needed" == true ]]; then
        if [[ "$rag_available" == true ]]; then
            api_status=">>API-R${rag_provider}<<"
        else
            api_status=">>API<<"
        fi
    else
        if [[ "$rag_available" == true ]]; then
            api_status="API-R${rag_provider}"
        else
            api_status="API"
        fi
    fi
    touch "$api_marker" 2>/dev/null
else
    # Auto-start API server if not already attempted this session
    if [[ ! -f "$api_marker" ]]; then
        api_dir="/Volumes/M-Drive/Coding/ideacode/ideacode-api"
        if [[ -d "$api_dir" ]]; then
            # Start API server in background
            (cd "$api_dir" && npm start > /dev/null 2>&1 &) &
            touch "$api_marker" 2>/dev/null
            # Give it a moment to start
            sleep 0.5
            if curl -s --connect-timeout 0.5 http://localhost:3847/health > /dev/null 2>&1; then
                if [[ "$restart_needed" == true ]]; then
                    if [[ "$rag_available" == true ]]; then
                        api_status=">>API-R${rag_provider}<<"
                    else
                        api_status=">>API<<"
                    fi
                else
                    if [[ "$rag_available" == true ]]; then
                        api_status="API-R${rag_provider}"
                    else
                        api_status="API"
                    fi
                fi
            fi
        fi
    fi
fi

# Add API status if set
[[ -n "$api_status" ]] && status_parts+=("$api_status")

# Repository (always show if in git repo)
if [[ -n "$repo" ]]; then
    status_parts+=("$repo")
else
    # Fallback: use directory name
    repo_fallback=$(basename "$cwd" 2>/dev/null || echo "unknown")
    [[ "$repo_fallback" != "~" ]] && status_parts+=("$repo_fallback")
fi

# Branch (with * if dirty)
if [[ -n "$branch" ]]; then
    status_parts+=("${git_dirty}${branch}")
fi

# Git stats (if any changes)
[[ -n "$git_stats" ]] && status_parts+=("$git_stats")

# Context display (token usage)
[[ -n "$context_display" ]] && status_parts+=("$context_display")

# Model
status_parts+=("$model_short")

# CLI version - only show both if update available
if [[ "$version" != "$newest_version" ]]; then
    status_parts+=("v${version} -> v${newest_version}")
else
    status_parts+=("v${version}")
fi

# Add restart message if needed (version or instruction changes)
if [[ "$restart_needed" == true ]]; then
    status_parts+=("restart")
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
