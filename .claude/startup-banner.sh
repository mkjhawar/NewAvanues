#!/bin/bash

# IDEACODE Startup Banner with Shared Memory Integration
# Displays when Claude Code starts a new session
# Registers session and loads module context

# Colors
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

# Read JSON input from stdin to get current directory
input=$(cat)
cwd=$(echo "$input" | jq -r '.workspace.current_dir // .cwd // "~"')

# ============ API Status Check (NO AUTO-START) ============
# Claude Code will ask user interactively whether to start API
API_PORT=3850

api_status="offline"
if curl -s --connect-timeout 1 --max-time 2 "http://localhost:${API_PORT}/health" >/dev/null 2>&1; then
    api_status="running"
fi
# Note: Auto-start removed. Claude Code handles this interactively via AskUserQuestion
cwd=$(echo "$input" | jq -r '.workspace.current_dir // .cwd // "."')

# Get IDEACODE version
ideacode_version="?"
if [[ -f "$cwd/.ideacode/config.ideacode" ]]; then
    ideacode_version=$(jq -r '.version // "?"' "$cwd/.ideacode/config.ideacode" 2>/dev/null)
elif [[ -f "$cwd/.ideacode/config.yml" ]]; then
    ideacode_version=$(grep '^version:' "$cwd/.ideacode/config.yml" 2>/dev/null | sed 's/version: *"//' | sed 's/"//' | xargs)
elif [[ -f "$cwd/.ideacode/config.yaml" ]]; then
    ideacode_version=$(grep '^version:' "$cwd/.ideacode/config.yaml" 2>/dev/null | sed 's/version: *"//' | sed 's/"//' | xargs)
fi

# Fallback to IDEACODE framework version-info.json
if [[ "$ideacode_version" == "?" ]] && [[ -f "/Volumes/M-Drive/Coding/ideacode/version-info.json" ]]; then
    ideacode_version=$(jq -r '.framework_version // "?"' /Volumes/M-Drive/Coding/ideacode/version-info.json 2>/dev/null)
fi

# Get repo info
repo_name="unknown"
branch="unknown"
if git -C "$cwd" rev-parse --git-dir > /dev/null 2>&1; then
    repo_name=$(basename "$(git -C "$cwd" rev-parse --show-toplevel 2>/dev/null)")
    branch=$(git -C "$cwd" branch --show-current 2>/dev/null || echo "detached")
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
║         Iterative Design Enhanced Architecture v${ideacode_version}                 ║
║                                                                          ║
║                   Powered by IDEACODE Framework                         ║
║                                                                          ║
╚══════════════════════════════════════════════════════════════════════════╝

EOF

# Register session and load context if shared memory manager exists
SHARED_MEMORY_MANAGER="$cwd/.claude/hooks/shared-memory-manager.sh"
if [[ ! -f "$SHARED_MEMORY_MANAGER" ]]; then
    # Try IDEACODE framework path
    SHARED_MEMORY_MANAGER="/Volumes/M-Drive/Coding/ideacode/.claude/hooks/shared-memory-manager.sh"
fi

if [[ -x "$SHARED_MEMORY_MANAGER" ]]; then
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BOLD}Working in:${NC} $repo_name"
    echo -e "${BOLD}Branch:${NC} $branch"
    echo ""

    # Register this session
    "$SHARED_MEMORY_MANAGER" register 2>/dev/null || true

    # Check for other active sessions in same repo
    active_sessions=$(jq -r '.sessions | length' "$cwd/.ideacode/shared-memory/sessions.json" 2>/dev/null || echo "0")
    if [[ "$active_sessions" -gt 1 ]]; then
        echo ""
        echo -e "${YELLOW}NOTE: ${active_sessions} active sessions in this repo${NC}"
        echo -e "${YELLOW}Use '/i.memory status' to see what others are working on${NC}"
    fi

    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
else
    # Fallback: just show repo info
    echo -e "${BOLD}Working in:${NC} $repo_name"
    echo -e "${BOLD}Branch:${NC} $branch"
    echo ""
fi
