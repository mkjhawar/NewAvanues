#!/bin/bash

# IDEACODE Startup Banner
# Displays when Claude Code starts a new session
# Shows IDEACODE branding alongside Claude Code

# Read JSON input from stdin to get current directory
input=$(cat)
cwd=$(echo "$input" | jq -r '.workspace.current_dir // .cwd // "~"')

# Get IDEACODE version from config (check JSON first, then YAML)
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
