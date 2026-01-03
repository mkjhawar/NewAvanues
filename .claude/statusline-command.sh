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

    # Check available LLM providers from health response
    llm_status="No LLM"
    if echo "$health" | grep -q '"ollama":true'; then
        # Get default model from routing config
        routing=$(curl -s --connect-timeout 0.1 --max-time 0.2 "http://localhost:3850/v1/llm/routing" 2>/dev/null)
        model=$(echo "$routing" | sed 's/.*"simple_model":"\([^"]*\)".*/\1/' | cut -d: -f1)
        [ -n "$model" ] && llm_status="Ollama:$model" || llm_status="Ollama"
    elif echo "$health" | grep -q '"anthropic":true'; then
        llm_status="Claude"
    elif echo "$health" | grep -q '"openai":true'; then
        llm_status="OpenAI"
    elif echo "$health" | grep -q '"openrouter":true'; then
        llm_status="OpenRouter"
    elif echo "$health" | grep -q '"groq":true'; then
        llm_status="Groq"
    fi

    # Build status: API-R87:3-Cloud:Claude or API-R87:3-No LLM
    api_status="API${rag:+-$rag}-${llm_status}"
else
    # API not running - using .md files only
    api_status="IDC"
fi

# Build output
output="IDC v$VERSION | $api_status | $repo:$branch"

printf "%s" "$output"
