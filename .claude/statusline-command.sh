#!/bin/sh

# Claude Code Enhanced Status Line
# Uses /bin/sh and fast commands only
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
    # RAG suffix - docs:collections format (R87:3)
    rag=""
    rag_stats=$(curl -s --connect-timeout 0.1 --max-time 0.2 "http://localhost:3850/v1/search/stats" 2>/dev/null)
    if [ -n "$rag_stats" ]; then
        doc_count=$(echo "$rag_stats" | sed 's/.*"total_documents":\([0-9]*\).*/\1/')
        col_count=$(echo "$rag_stats" | sed 's/.*"collections":\[\([^]]*\)\].*/\1/' | tr ',' '\n' | grep -c '"')
        [ -n "$doc_count" ] && [ "$doc_count" -gt 0 ] 2>/dev/null && rag="R${doc_count}:${col_count}"
    fi

    # Build status: API-R87:3 (LLM routing disabled, using RAG only)
    api_status="API${rag:+-$rag}"
else
    # API not running - using .md files only
    api_status="IDC"
fi

# Build output
output="IDC v$VERSION | $api_status | $repo:$branch"

printf "%s" "$output"
