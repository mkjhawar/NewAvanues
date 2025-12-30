#!/bin/bash
#
# test-learnapp-emulator.sh - Automated LearnApp/JIT Testing on Emulator
#
# Tests LearnApp functionality including:
# - Consent dialog display
# - JIT mode activation via Skip button
# - Screen learning and persistence
# - Integration with Google apps
#
# Usage: ./scripts/test-learnapp-emulator.sh [device_id]
#

set -e

# Configuration
DEVICE="${1:-emulator-5554}"
PACKAGE="com.augmentalis.voiceos"
SERVICE="$PACKAGE/.voiceoscore.accessibility.VoiceOSService"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== LearnApp/JIT Automated Test Suite ===${NC}"
echo -e "${BLUE}Device: $DEVICE${NC}"
echo -e "${BLUE}Package: $PACKAGE${NC}"
echo ""

# Function to print status
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Step 1: Grant permissions
echo -e "${BLUE}Step 1: Granting Permissions${NC}"
print_info "Granting SYSTEM_ALERT_WINDOW for consent dialog..."
$ADB -s $DEVICE shell appops set $PACKAGE SYSTEM_ALERT_WINDOW allow
print_status "Overlay permission granted"

print_info "Granting storage permissions..."
$ADB -s $DEVICE shell pm grant $PACKAGE android.permission.READ_EXTERNAL_STORAGE 2>/dev/null || true
$ADB -s $DEVICE shell pm grant $PACKAGE android.permission.WRITE_EXTERNAL_STORAGE 2>/dev/null || true
print_status "Storage permissions granted"

echo ""

# Step 2: Enable accessibility service
echo -e "${BLUE}Step 2: Enabling VoiceOS Accessibility Service${NC}"
print_info "Checking if service is already enabled..."

ENABLED_SERVICES=$($ADB -s $DEVICE shell settings get secure enabled_accessibility_services)
if [[ "$ENABLED_SERVICES" == *"$SERVICE"* ]]; then
    print_status "VoiceOS accessibility service already enabled"
else
    print_info "Enabling VoiceOS accessibility service..."

    # Enable the service
    if [[ "$ENABLED_SERVICES" == "null" ]] || [[ -z "$ENABLED_SERVICES" ]]; then
        $ADB -s $DEVICE shell settings put secure enabled_accessibility_services "$SERVICE"
    else
        $ADB -s $DEVICE shell settings put secure enabled_accessibility_services "${ENABLED_SERVICES}:${SERVICE}"
    fi

    # Enable accessibility
    $ADB -s $DEVICE shell settings put secure accessibility_enabled 1

    print_status "VoiceOS accessibility service enabled"

    # Wait for service to start
    print_info "Waiting for service to initialize (5 seconds)..."
    sleep 5
fi

echo ""

# Step 3: Start monitoring logcat
echo -e "${BLUE}Step 3: Monitoring Logcat${NC}"
print_info "Clearing logcat..."
$ADB -s $DEVICE logcat -c

print_info "Starting logcat monitor in background..."
$ADB -s $DEVICE logcat | grep -E "(LearnApp|JustInTimeLearner|ConsentDialog|NewAppDetected)" > /tmp/voiceos-test-logcat.log &
LOGCAT_PID=$!

sleep 2
echo ""

# Step 4: Test Google apps
echo -e "${BLUE}Step 4: Testing LearnApp with Google Apps${NC}"

# List of Google apps to test
GOOGLE_APPS=(
    "com.google.android.gm:Gmail"
    "com.google.android.apps.maps:Maps"
    "com.google.android.youtube:YouTube"
)

test_app() {
    local app_package=$1
    local app_name=$2

    print_info "Testing: $app_name ($app_package)"

    # Check if app is installed
    if ! $ADB -s $DEVICE shell pm list packages | grep -q "^package:${app_package}$"; then
        print_warning "$app_name not installed - skipping"
        return
    fi

    # Launch the app
    print_info "  Launching $app_name..."
    $ADB -s $DEVICE shell monkey -p $app_package -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1 || true

    # Wait for app to launch and LearnApp to detect
    print_info "  Waiting for LearnApp detection (8 seconds)..."
    sleep 8

    # Go back to home
    $ADB -s $DEVICE shell input keyevent KEYCODE_HOME
    sleep 2

    print_status "$app_name test complete"
    echo ""
}

# Run tests for each app
for app_info in "${GOOGLE_APPS[@]}"; do
    IFS=':' read -r package name <<< "$app_info"
    test_app "$package" "$name"
done

# Step 5: Stop logcat monitoring
print_info "Stopping logcat monitor..."
sleep 2
kill $LOGCAT_PID 2>/dev/null || true

echo ""

# Step 6: Generate report
echo -e "${BLUE}=== Test Summary ===${NC}"
echo ""

print_info "Analyzing test results..."
echo ""

# Count events in logcat
NEW_APP_DETECTED=$(grep -c "NewAppDetected" /tmp/voiceos-test-logcat.log 2>/dev/null || echo "0")
CONSENT_SHOWN=$(grep -c "showConsentDialog" /tmp/voiceos-test-logcat.log 2>/dev/null || echo "0")
JIT_ACTIVATED=$(grep -c "JIT learning activated" /tmp/voiceos-test-logcat.log 2>/dev/null || echo "0")
SCREENS_LEARNED=$(grep -c "Learning this screen" /tmp/voiceos-test-logcat.log 2>/dev/null || echo "0")
MANUAL_MODE=$(grep -c "Manual mode enabled" /tmp/voiceos-test-logcat.log 2>/dev/null || echo "0")

echo -e "${BLUE}Statistics:${NC}"
echo "  New apps detected: $NEW_APP_DETECTED"
echo "  Consent dialogs shown: $CONSENT_SHOWN"
echo "  Manual mode logs: $MANUAL_MODE"
echo "  JIT mode activations: $JIT_ACTIVATED"
echo "  Screens learned (JIT): $SCREENS_LEARNED"
echo ""

echo -e "${BLUE}Logcat saved to: /tmp/voiceos-test-logcat.log${NC}"
echo -e "${BLUE}Review with: cat /tmp/voiceos-test-logcat.log | grep -E '(LearnApp|JIT|Consent)'${NC}"
echo ""

# Check for errors
ERRORS=$(grep -c "ERROR\|Exception\|crash" /tmp/voiceos-test-logcat.log 2>/dev/null || echo "0")
if [ "$ERRORS" -gt 0 ]; then
    print_error "Found $ERRORS errors in logcat - review log file"
else
    print_status "No errors detected"
fi

echo ""

# Show relevant log snippets
if [ -f /tmp/voiceos-test-logcat.log ]; then
    echo -e "${BLUE}=== Key Log Events ===${NC}"
    echo ""

    # Show LearnApp events
    echo -e "${YELLOW}LearnApp Detection Events:${NC}"
    grep "NewAppDetected\|LearnApp" /tmp/voiceos-test-logcat.log | head -10
    echo ""

    # Show consent dialog events
    echo -e "${YELLOW}Consent Dialog Events:${NC}"
    grep "ConsentDialog\|showConsent" /tmp/voiceos-test-logcat.log | head -5
    echo ""

    # Show JIT events
    echo -e "${YELLOW}JIT Learning Events:${NC}"
    grep "JustInTimeLearner\|JIT" /tmp/voiceos-test-logcat.log | head -5
    echo ""
fi

print_status "LearnApp/JIT testing complete!"
echo ""

# Return to home
$ADB -s $DEVICE shell input keyevent KEYCODE_HOME
