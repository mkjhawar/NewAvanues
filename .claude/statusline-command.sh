#!/bin/sh

# Claude Code Status Line v2 (Redesigned)
# Line 1: LLMIM:ON/OFF LocalLLMName | ICA version | CLI model | Context %
# Line 2: API:ON/OFF | RAG:count (repo) Tot:total | Location:Branch | CC version

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
location="unknown"

if git rev-parse --git-dir >/dev/null 2>&1; then
    branch=$(git branch --show-current 2>/dev/null || echo "detached")
    repo_root=$(git rev-parse --show-toplevel 2>/dev/null)
    repo=$(basename "$repo_root" 2>/dev/null || echo "unknown")
    cwd=$(pwd)
    if [ "$cwd" = "$repo_root" ]; then
        location="$repo"
    else
        rel_path="${cwd#$repo_root/}"
        location="$repo/$rel_path"
    fi
fi

# LLMIM status (prompt enrichment)
ic_state_file="$HOME/.claude/.ic-enabled"
if [ -f "$ic_state_file" ] && [ "$(cat "$ic_state_file" 2>/dev/null)" = "0" ]; then
    llmim_on=false
else
    llmim_on=true
fi

# Initialize variables
VERSION="?"
api_on=false
cli_part=""
local_llm_name=""
rag_repo_count=""
rag_total=""
llm_active=""
mcp=""
# Dual version: show both if session differs from installed binary
if [ -n "$CC_SESSION_VER" ] && [ "$CC_SESSION_VER" != "$CC_VER" ] && [ "$CC_SESSION_VER" != "?" ]; then
    cc_version_display="v${CC_VER}(bin) v${CC_SESSION_VER}(sess)"
else
    cc_version_display="v$CC_VER"
fi

# API health check
health=$(curl -s --connect-timeout 0.2 --max-time 0.3 "http://localhost:3850/health" 2>/dev/null)

if [ -n "$health" ]; then
    api_on=true
    VERSION=$(echo "$health" | sed 's/.*"version":"\([^"]*\)".*/\1/')

    # LLM config status (model names, mode)
    llm_status=$(curl -s --connect-timeout 0.2 --max-time 0.3 "http://localhost:3850/llm-config/status" 2>/dev/null)
    if [ -n "$llm_status" ]; then
        mode=$(echo "$llm_status" | sed 's/.*"mode":"\([^"]*\)".*/\1/' | tr '[:lower:]' '[:upper:]')
        primary=$(echo "$llm_status" | sed 's/.*"primary":"\([^"]*\)".*/\1/')
        ollama=$(echo "$llm_status" | sed 's/.*"ollamaStatus":"\([^"]*\)".*/\1/')
        local_llm=$(echo "$llm_status" | sed 's/.*"local":"\([^"]*\)".*/\1/')

        cli_part="${mode}:${primary}"

        if [ "$ollama" = "running" ] && [ "$local_llm" != "Offline" ]; then
            local_llm_name="$local_llm"
        fi
    fi

    # RAG stats - global total
    rag_stats=$(curl -s --connect-timeout 0.2 --max-time 0.5 "http://localhost:3850/rag/stats" 2>/dev/null)
    if [ -n "$rag_stats" ]; then
        rag_total=$(echo "$rag_stats" | sed 's/.*"totalDocuments":\([0-9]*\).*/\1/')
        [ "$rag_total" = "$rag_stats" ] && rag_total=""
    fi

    # RAG stats - per current repo
    if [ "$repo" != "unknown" ] && [ -n "$rag_total" ]; then
        rag_repo=$(curl -s --connect-timeout 0.2 --max-time 0.5 "http://localhost:3850/rag/stats?repo=$repo" 2>/dev/null)
        if [ -n "$rag_repo" ]; then
            rag_repo_count=$(echo "$rag_repo" | sed 's/.*"docCount":\([0-9]*\).*/\1/')
            [ "$rag_repo_count" = "$rag_repo" ] && rag_repo_count=""
        fi
    fi

    # Active LLM operations indicator
    active_ops=$(curl -s --connect-timeout 0.1 --max-time 0.2 "http://localhost:3850/llm/active-operations" 2>/dev/null)
    if [ -n "$active_ops" ]; then
        op_count=$(echo "$active_ops" | sed 's/.*"count":\([0-9]*\).*/\1/')
        if [ -n "$op_count" ] && [ "$op_count" -gt 0 ] 2>/dev/null; then
            llm_active=" [LLM...]"
        fi
    fi

    # CC version check (cached) - only override display if npm update available
    version_info=$(curl -s --connect-timeout 0.2 --max-time 0.3 "http://localhost:3850/version-check" 2>/dev/null)
    if [ -n "$version_info" ]; then
        has_update=$(echo "$version_info" | sed 's/.*"hasUpdate":\([a-z]*\).*/\1/')
        if [ "$has_update" = "true" ]; then
            display=$(echo "$version_info" | sed 's/.*"display":"\([^"]*\)".*/\1/')
            [ -n "$display" ] && [ "$display" != "$version_info" ] && cc_version_display="$display"
        fi
    fi

    # MCP status
    mcp_status_file="/Volumes/M-Drive/Coding/.codeavenue/mcp-status.txt"
    if [ -f "$mcp_status_file" ]; then
        file_age=$(($(date +%s) - $(stat -f %m "$mcp_status_file" 2>/dev/null || echo 0)))
        if [ "$file_age" -lt 300 ]; then
            mcp_content=$(cat "$mcp_status_file" 2>/dev/null)
            if echo "$mcp_content" | grep -q "MCP:ON"; then
                mcp="+MCP"
            fi
        fi
    fi
else
    # API offline - read version from package.json
    PKG_JSON="/Volumes/M-Drive/Coding/CodeAvanue/api/package.json"
    VERSION=$(sed -n 's/.*"version": *"\([^"]*\)".*/\1/p' "$PKG_JSON" 2>/dev/null || echo "?")
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
# BUILD OUTPUT
# =============================================================================

# Line 1: LLMIM:ON/OFF [LocalLLMName] | ICA vX.X.X | MODE:Model | Ctx:XX%
if [ "$llmim_on" = true ]; then
    if [ -n "$local_llm_name" ]; then
        llmim_part="LLMIM:ON ${local_llm_name}${llm_active}"
    else
        llmim_part="LLMIM:ON (Offline)${llm_active}"
    fi
else
    llmim_part="LLMIM:OFF"
fi

line1="$llmim_part | ICA v$VERSION"
# CLI model: prefer Claude Code's own model (accurate), fallback to API config
if [ -n "$cli_model" ]; then
    cli_model_display=$(echo "$cli_model" | sed 's/-[0-9]\{8,\}$//' | sed 's/-\([0-9]*\)-\([0-9]*\)$/ \1.\2/' | tr '-' ' ' | awk '{for(i=1;i<=NF;i++){$i=toupper(substr($i,1,1))substr($i,2)}}1')
    line1="$line1 | CLI:$cli_model_display"
elif [ -n "$cli_part" ]; then
    line1="$line1 | $cli_part"
fi
[ -n "$context_pct" ] && line1="$line1 | Ctx:$context_pct"

# Line 2: API:ON/OFF | RAG:count (repo) Tot:total | Location:Branch | CC:version
if [ "$api_on" = true ]; then
    api_display="API:ON${mcp}"
else
    api_display="API:OFF"
fi

rag_display=""
if [ -n "$rag_repo_count" ] && [ -n "$rag_total" ]; then
    rag_display="RAG:${rag_repo_count} (${repo}) Tot:${rag_total}"
elif [ -n "$rag_total" ]; then
    rag_display="RAG:Tot:${rag_total}"
fi

line2="$api_display"
[ -n "$rag_display" ] && line2="$line2 | $rag_display"
line2="$line2 | $location:$branch | CC:$cc_version_display"
[ -n "$handover_alert" ] && line2="$line2 | $handover_alert"

printf "%s\n%s" "$line1" "$line2"
