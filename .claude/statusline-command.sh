#!/bin/sh

# Claude Code Status Line v3 (Consolidated)
# Single curl to /statusline replaces 6 separate API calls.
# Output: ic | API | model | Ctx:XX% | R:count | repo:branch | CC:ver [alert]

# Read JSON input from Claude Code
input=$(cat)

# Extract context percentage from Claude Code JSON
context_pct=""
if [ -n "$input" ]; then
    pct=$(echo "$input" | sed -n 's/.*"used_percentage"[[:space:]]*:[[:space:]]*\([0-9.]*\).*/\1/p' | head -1)
    if [ -n "$pct" ] && [ "$pct" != "null" ]; then
        pct=$(printf "%.0f" "$pct" 2>/dev/null || echo "")
        [ -n "$pct" ] && context_pct="${pct}%"
    fi
    if [ -z "$context_pct" ]; then
        rem=$(echo "$input" | sed -n 's/.*"remaining_percentage"[[:space:]]*:[[:space:]]*\([0-9.]*\).*/\1/p' | head -1)
        if [ -n "$rem" ] && [ "$rem" != "null" ]; then
            pct=$(printf "%.0f" "$rem" 2>/dev/null || echo "")
            [ -n "$pct" ] && context_pct="${pct}%R"
        fi
    fi
fi

# Extract CLI model from Claude Code JSON input (actual model, not API config)
cli_model=""
if [ -n "$input" ]; then
    cli_model=$(echo "$input" | sed -n 's/.*"model"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
fi

# Claude Code version (installed binary)
CC_VER=$(claude --version 2>/dev/null | sed 's/ .*//' || echo "?")

# Session version (captured at session start by startup-banner.sh)
CC_SESSION_VER=""
if [ -f "$HOME/.claude/.session-version" ]; then
    CC_SESSION_VER=$(cat "$HOME/.claude/.session-version" 2>/dev/null)
fi

# Git info
repo="unknown"
branch="unknown"

if git rev-parse --git-dir >/dev/null 2>&1; then
    branch=$(git branch --show-current 2>/dev/null || echo "detached")
    repo_root=$(git rev-parse --show-toplevel 2>/dev/null)
    repo=$(basename "$repo_root" 2>/dev/null || echo "unknown")
fi

# LLMIM status (prompt enrichment)
ic_state_file="$HOME/.claude/.ic-enabled"
if [ -f "$ic_state_file" ] && [ "$(cat "$ic_state_file" 2>/dev/null)" = "0" ]; then
    ic_flag="ic:OFF"
else
    ic_flag="ic"
fi

# Dual version: show both if session differs from installed binary
if [ -n "$CC_SESSION_VER" ] && [ "$CC_SESSION_VER" != "$CC_VER" ] && [ "$CC_SESSION_VER" != "?" ]; then
    cc_version_display="v${CC_VER}(bin) v${CC_SESSION_VER}(sess)"
else
    cc_version_display="v$CC_VER"
fi

# Initialize API-sourced variables
api_flag="API:OFF"
cli_part=""
rag_flag=""
llm_active=""
mcp=""

# ============================================================================
# SINGLE API CALL - replaces 6 separate curls
# ============================================================================
sl_url="http://localhost:3850/statusline"
[ "$repo" != "unknown" ] && sl_url="${sl_url}?repo=${repo}"

sl_data=$(curl -s --connect-timeout 0.3 --max-time 0.5 "$sl_url" 2>/dev/null)

if [ -n "$sl_data" ] && echo "$sl_data" | grep -q '"api":true'; then
    # API is online
    # MCP status (local file check, not from API)
    mcp_status_file="${CODING_ROOT:-/tmp}/.codeavenue/mcp-status.txt"
    if [ -f "$mcp_status_file" ]; then
        file_age=$(($(date +%s) - $(stat -f %m "$mcp_status_file" 2>/dev/null || echo 0)))
        if [ "$file_age" -lt 300 ]; then
            mcp_content=$(cat "$mcp_status_file" 2>/dev/null)
            if echo "$mcp_content" | grep -q "MCP:ON"; then
                mcp="+MCP"
            fi
        fi
    fi
    api_flag="API${mcp}"

    # LLM mode:primary from response
    mode=$(echo "$sl_data" | sed -n 's/.*"mode"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
    primary=$(echo "$sl_data" | sed -n 's/.*"primary"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
    if [ -n "$mode" ] && [ -n "$primary" ]; then
        cli_part=$(echo "$mode" | tr '[:lower:]' '[:upper:]')":${primary}"
    fi

    # RAG counts - prefer repo count, fallback to total
    rag_repo_val=$(echo "$sl_data" | sed -n 's/.*"repo"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p' | head -1)
    rag_total_val=$(echo "$sl_data" | sed -n 's/.*"total"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p' | head -1)
    if [ -n "$rag_repo_val" ] && [ "$rag_repo_val" -gt 0 ] 2>/dev/null; then
        rag_flag="R:${rag_repo_val}"
    elif [ -n "$rag_total_val" ] && [ "$rag_total_val" -gt 0 ] 2>/dev/null; then
        rag_flag="R:${rag_total_val}"
    fi

    # Active LLM operations
    active_ops_val=$(echo "$sl_data" | sed -n 's/.*"activeOps"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p' | head -1)
    if [ -n "$active_ops_val" ] && [ "$active_ops_val" -gt 0 ] 2>/dev/null; then
        llm_active=" [LLM...]"
    fi

    # Version check - override CC display if update available
    has_update=$(echo "$sl_data" | sed -n 's/.*"hasUpdate"[[:space:]]*:[[:space:]]*\(true\).*/\1/p' | head -1)
    if [ "$has_update" = "true" ]; then
        vc_display=$(echo "$sl_data" | sed -n 's/.*"display"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
        [ -n "$vc_display" ] && cc_version_display="$vc_display"
    fi
fi

# Handover alert (flashing - alternates between two states on each refresh)
handover_alert=""
flash_tick=$(( $(date +%s) % 2 ))
if [ -d ".claude/handovers" ]; then
    handover_count=$(ls -1 .claude/handovers/*.md 2>/dev/null | wc -l | tr -d ' ')
    if [ "$handover_count" -gt 0 ] 2>/dev/null; then
        if [ "$flash_tick" -eq 0 ]; then
            handover_alert=">>> HANDOVER READY - /clear <<<"
        else
            handover_alert="                              "
        fi
    fi
fi
if [ -d ".ava" ] && [ -f ".ava/session-state.json" ]; then
    ava_state=$(cat .ava/session-state.json 2>/dev/null)
    if echo "$ava_state" | grep -q '"completed":true\|"blocked":true'; then
        if [ "$flash_tick" -eq 0 ]; then
            handover_alert=">>> AVA COMPLETE - /clear <<<"
        else
            handover_alert="                             "
        fi
    fi
fi

# =============================================================================
# BUILD OUTPUT (single line)
# =============================================================================

# CLI model: prefer Claude Code's own model, fallback to API config
cli_display=""
if [ -n "$cli_model" ]; then
    cli_display=$(echo "$cli_model" | sed 's/^claude-//' | sed 's/-[0-9]\{8,\}$//')
elif [ -n "$cli_part" ]; then
    cli_display="$cli_part"
fi

# Build single line: ic | API | model | Ctx:XX% | R:count | repo:branch | CC:ver [alert]
line="$ic_flag | $api_flag"
[ -n "$cli_display" ] && line="$line | $cli_display"
[ -n "$context_pct" ] && line="$line | Ctx:$context_pct"
[ -n "$rag_flag" ] && line="$line | $rag_flag"
line="$line | $repo:$branch"
line="$line | $cc_version_display"
[ -n "$llm_active" ] && line="$line$llm_active"
[ -n "$handover_alert" ] && line="$line | $handover_alert"

printf "%s" "$line"
