#!/bin/bash
# MagicUI Language Server - Packaging Script
# Creates executable JAR with all dependencies

set -e

echo "======================================"
echo "MagicUI Language Server - Packaging"
echo "======================================"

# Navigate to project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"

cd "$PROJECT_ROOT"

echo "Building Language Server..."
./gradlew :Modules:AvanueUI:AvanueLanguageServer:clean
./gradlew :Modules:AvanueUI:AvanueLanguageServer:jar

JAR_FILE="Modules/AvanueUI/AvanueLanguageServer/build/libs/LanguageServer-1.0.0.jar"

if [ -f "$JAR_FILE" ]; then
    echo ""
    echo "✅ Package created successfully!"
    echo "Location: $JAR_FILE"
    echo ""
    echo "File size: $(du -h "$JAR_FILE" | cut -f1)"
    echo ""
    echo "To run:"
    echo "  java -jar $JAR_FILE"
    echo ""
    echo "Or use the launch script:"
    echo "  ./scripts/launch.sh"
else
    echo "❌ Build failed - JAR not found"
    exit 1
fi
