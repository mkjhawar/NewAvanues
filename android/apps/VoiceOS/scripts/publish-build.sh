#!/bin/bash
# publish-build.sh - Copy APK to central builds directory with versioned naming
#
# Usage: ./scripts/publish-build.sh [debug|release]
# Default: debug
#
# Output: /Volumes/M-Drive/Coding/builds/{ProjectName}/{debug|release}/{app}-{type}-v{version}-{YYYYMMDD-HHMM}.apk
#
# Retention:
#   - Debug: Keep last 5 builds on disk, git tracks last 10
#   - Release: Keep all builds on disk, git tracks last 5

set -e

# Configuration
BUILDS_ROOT="/Volumes/M-Drive/Coding/builds"
PROJECT_NAME="VoiceOS"
APP_NAME="voiceos"
BUILD_TYPE="${1:-debug}"
DEBUG_KEEP_COUNT=5
DEBUG_GIT_TRACK_COUNT=10
RELEASE_GIT_TRACK_COUNT=5

# Paths
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SOURCE_APK="$PROJECT_ROOT/app/build/outputs/apk/$BUILD_TYPE/app-$BUILD_TYPE.apk"
DEST_DIR="$BUILDS_ROOT/$PROJECT_NAME/$BUILD_TYPE"

# Extract version from build.gradle.kts
VERSION_FILE="$PROJECT_ROOT/app/build.gradle.kts"
if [[ -f "$VERSION_FILE" ]]; then
    VERSION_NAME=$(grep -E "versionName\s*=" "$VERSION_FILE" | head -1 | sed 's/.*"\(.*\)".*/\1/' | tr -d ' ')
    VERSION_CODE=$(grep -E "versionCode\s*=" "$VERSION_FILE" | head -1 | sed 's/.*=\s*\([0-9]*\).*/\1/' | tr -d ' ')
else
    # Fallback for build.gradle (Groovy)
    VERSION_FILE="$PROJECT_ROOT/app/build.gradle"
    VERSION_NAME=$(grep -E "versionName\s+" "$VERSION_FILE" | head -1 | sed 's/.*"\(.*\)".*/\1/' | tr -d ' ')
    VERSION_CODE=$(grep -E "versionCode\s+" "$VERSION_FILE" | head -1 | sed 's/.*[^0-9]\([0-9]*\).*/\1/' | tr -d ' ')
fi

# Fallback version if extraction fails
VERSION_NAME="${VERSION_NAME:-0.0.0}"
VERSION_CODE="${VERSION_CODE:-0}"

# Timestamp
TIMESTAMP=$(date "+%Y%m%d-%H%M")

# Destination filename
DEST_FILENAME="${APP_NAME}-${BUILD_TYPE}-v${VERSION_NAME}-${TIMESTAMP}.apk"
DEST_PATH="$DEST_DIR/$DEST_FILENAME"

# Validate source APK exists
if [[ ! -f "$SOURCE_APK" ]]; then
    echo "ERROR: Source APK not found: $SOURCE_APK"
    echo "Run './gradlew assemble${BUILD_TYPE^}' first"
    exit 1
fi

# Create destination directory
mkdir -p "$DEST_DIR"

# Copy APK
echo "Publishing build..."
echo "  Source: $SOURCE_APK"
echo "  Dest:   $DEST_PATH"
cp "$SOURCE_APK" "$DEST_PATH"

# Get file size
SIZE=$(ls -lh "$DEST_PATH" | awk '{print $5}')
echo "  Size:   $SIZE"
echo "  Version: v${VERSION_NAME} (${VERSION_CODE})"

# Cleanup old debug builds (keep last N)
if [[ "$BUILD_TYPE" == "debug" ]]; then
    echo ""
    echo "Cleaning up old debug builds (keeping last $DEBUG_KEEP_COUNT)..."
    cd "$DEST_DIR"
    # List files sorted by time (oldest first), skip the last N, and remove
    ls -t *.apk 2>/dev/null | tail -n +$((DEBUG_KEEP_COUNT + 1)) | while read -r old_file; do
        echo "  Removing: $old_file"
        rm -f "$old_file"
    done
fi

# Update .gitignore for retention policy
GITIGNORE_PATH="$DEST_DIR/.gitignore"
echo ""
echo "Updating .gitignore for git tracking policy..."

if [[ "$BUILD_TYPE" == "debug" ]]; then
    # Git track last N debug builds
    TRACK_COUNT=$DEBUG_GIT_TRACK_COUNT
else
    # Git track last N release builds
    TRACK_COUNT=$RELEASE_GIT_TRACK_COUNT
fi

# Create .gitignore that ignores all except last N
cd "$DEST_DIR"
{
    echo "# Auto-generated - tracks last $TRACK_COUNT builds"
    echo "*.apk"
    echo ""
    echo "# Tracked builds (last $TRACK_COUNT):"
    ls -t *.apk 2>/dev/null | head -n "$TRACK_COUNT" | while read -r tracked_file; do
        echo "!$tracked_file"
    done
} > "$GITIGNORE_PATH"

echo ""
echo "BUILD PUBLISHED SUCCESSFULLY"
echo "============================================"
echo "  $DEST_PATH"
echo "============================================"
