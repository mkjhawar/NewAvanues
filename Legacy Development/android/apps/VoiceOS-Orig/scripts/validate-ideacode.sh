#!/bin/bash
# IDEACODE v9.0 - Command & Modifier Validator Hook
# Enforces new naming conventions and dot modifiers

# Read stdin for tool call info
read -r TOOL_INFO

# Extract command if it's a slash command
COMMAND=$(echo "$TOOL_INFO" | grep -oE '/[a-z]+' | head -1)

# Check for old /ideacode.* pattern
if echo "$TOOL_INFO" | grep -qE '/ideacode\.[a-z]+'; then
    echo "ERROR: Old command format detected!"
    echo "  Use: /fix, /develop, /wiz (not /ideacode.fix)"
    echo "  See: /modifiers for full list"
    exit 1
fi

# Check for old # modifiers (but allow # in comments/strings)
if echo "$TOOL_INFO" | grep -qE '\s#(yolo|swarm|cot|tot|tcr|stop|advanced|tutor|global|project|test|security|backlog|docs)\b'; then
    echo "ERROR: Old modifier syntax detected!"
    echo "  Use: .yolo .swarm .cot (not #yolo #swarm #cot)"
    echo "  Modifiers now use dot prefix: .modifier"
    exit 1
fi

# Valid - pass through
exit 0
