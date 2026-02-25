#!/bin/bash
# MagicUI Language Server - Launch Script
# Starts the Language Server with optional port configuration

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"

JAR_FILE="$PROJECT_ROOT/Modules/AvanueUI/AvanueLanguageServer/build/libs/LanguageServer-1.0.0.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR file not found. Run ./scripts/package.sh first."
    exit 1
fi

# Default to stdio mode, or socket mode if port provided
MODE="${1:-stdio}"
PORT="${2:-9999}"

echo "======================================"
echo "MagicUI Language Server"
echo "======================================"
echo "Mode: $MODE"

if [ "$MODE" = "socket" ]; then
    echo "Port: $PORT"
    echo ""
    echo "Starting server on port $PORT..."
    java -jar "$JAR_FILE" --socket "$PORT"
else
    echo "Transport: stdio"
    echo ""
    echo "Starting server with stdio..."
    java -jar "$JAR_FILE"
fi
