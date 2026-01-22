#!/bin/bash
# gradle-run.sh - Universal Gradle wrapper with reliable output capture
#
# Problem: Direct piping from Gradle (e.g., ./gradlew task | tail -40) produces
# empty output in non-TTY environments like Claude Code's Bash tool.
#
# Solution: This script captures output to a temp file, then reads it back.
#
# Usage:
#   ./scripts/gradle-run.sh [gradle-task] [gradle-args...]
#   ./scripts/gradle-run.sh :Modules:AvaMagic:AVACode:build
#   ./scripts/gradle-run.sh :Modules:VoiceOSCoreNG:compileDebugKotlinAndroid --info
#
# Options (must come before gradle task):
#   --tail N      Show only last N lines (default: all)
#   --head N      Show only first N lines (default: all)
#   --quiet       Suppress info messages, only show build output
#   --no-color    Force no ANSI colors (already default via --console=plain)
#
# Examples:
#   ./scripts/gradle-run.sh --tail 50 :Modules:VoiceOSCoreNG:build
#   ./scripts/gradle-run.sh --quiet :Modules:AvaMagic:AVACode:assembleDebug

set -e

# Parse script options
TAIL_LINES=""
HEAD_LINES=""
QUIET=false
GRADLE_ARGS=()

while [[ $# -gt 0 ]]; do
    case "$1" in
        --tail)
            TAIL_LINES="$2"
            shift 2
            ;;
        --head)
            HEAD_LINES="$2"
            shift 2
            ;;
        --quiet)
            QUIET=true
            shift
            ;;
        --no-color)
            # Already handled by --console=plain
            shift
            ;;
        *)
            GRADLE_ARGS+=("$1")
            shift
            ;;
    esac
done

if [[ ${#GRADLE_ARGS[@]} -eq 0 ]]; then
    echo "Usage: $0 [options] <gradle-task> [gradle-args...]"
    echo ""
    echo "Options:"
    echo "  --tail N    Show only last N lines"
    echo "  --head N    Show only first N lines"
    echo "  --quiet     Suppress info messages"
    echo ""
    echo "Examples:"
    echo "  $0 :Modules:AvaMagic:AVACode:build"
    echo "  $0 --tail 50 :Modules:VoiceOSCoreNG:compileDebugKotlinAndroid"
    exit 1
fi

# Find project root (where gradlew is)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ ! -f "$PROJECT_ROOT/gradlew" ]]; then
    echo "Error: gradlew not found at $PROJECT_ROOT"
    exit 1
fi

# Set JAVA_HOME if needed (JDK 17 for Kotlin)
if command -v /usr/libexec/java_home &> /dev/null; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
fi

# Create temp file for output capture
TEMP_OUTPUT=$(mktemp -t gradle_output.XXXXXX)
trap "rm -f $TEMP_OUTPUT" EXIT

# Run Gradle with plain console, capturing all output
[[ "$QUIET" != "true" ]] && echo "Running: ./gradlew ${GRADLE_ARGS[*]}"
[[ "$QUIET" != "true" ]] && echo "---"

cd "$PROJECT_ROOT"
./gradlew --console=plain "${GRADLE_ARGS[@]}" > "$TEMP_OUTPUT" 2>&1
EXIT_CODE=$?

# Output based on options
if [[ -n "$TAIL_LINES" ]]; then
    tail -n "$TAIL_LINES" "$TEMP_OUTPUT"
elif [[ -n "$HEAD_LINES" ]]; then
    head -n "$HEAD_LINES" "$TEMP_OUTPUT"
else
    cat "$TEMP_OUTPUT"
fi

exit $EXIT_CODE
