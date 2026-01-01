#!/bin/sh

# Claude Code Enhanced Status Line
# Uses /bin/sh and fast git commands only
# Version sourced from .ideacode/VERSION

# Read version from central file
VERSION_FILE="/Volumes/M-Drive/Coding/.ideacode/VERSION"
VERSION=$(cat "$VERSION_FILE" 2>/dev/null || echo "?")

# Git info from current directory (fast commands only)
repo="unknown"
branch="unknown"

if git rev-parse --git-dir >/dev/null 2>&1; then
    branch=$(git branch --show-current 2>/dev/null || echo "detached")
    repo=$(basename "$(git rev-parse --show-toplevel 2>/dev/null)" 2>/dev/null || echo "unknown")
fi

# API status (quick check with strict timeout)
api_status=""
health=$(curl -s --connect-timeout 0.2 --max-time 0.3 "http://localhost:3850/health" 2>/dev/null)
if [ -n "$health" ]; then
    rag_suffix=""
    [ -f "/Volumes/M-Drive/Coding/ideacode/data/rag.db" ] && rag_suffix="-R"

    if echo "$health" | grep -q '"ollama":true'; then
        api_status="API${rag_suffix}:Ollama"
    elif echo "$health" | grep -q '"anthropic":true'; then
        api_status="API${rag_suffix}:Anthropic"
    else
        api_status="API${rag_suffix}"
    fi
fi

# Build output
output="IDEACODE v$VERSION"
[ -n "$api_status" ] && output="$output | $api_status"
output="$output | $repo | $branch | MEM: OK"

printf "%s" "$output"
