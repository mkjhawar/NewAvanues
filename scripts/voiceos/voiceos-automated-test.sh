#!/bin/bash
# VoiceOS Automated Accessibility Testing Script
# Tests: VoiceOSCore, JIT Learning, LearnApp
# Date: 2025-12-29

set -e

ADB=~/Library/Android/sdk/platform-tools/adb
DEVICE="emulator-5554"
VOICEOS_PKG="com.augmentalis.voiceos"
VOICEOS_SERVICE="com.augmentalis.voiceos/com.augmentalis.voiceoscore.accessibility.VoiceOSService"
LOG_DIR="/tmp/voiceos-test-logs"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Create log directory
mkdir -p "$LOG_DIR"

echo "=============================================="
echo "  VoiceOS Automated Accessibility Testing"
echo "  Timestamp: $TIMESTAMP"
echo "=============================================="

# Test apps to cycle through
TEST_APPS=(
    "com.android.settings"
    "com.android.contacts"
    "com.android.dialer"
    "com.android.calendar"
    "com.android.deskclock"
    "com.android.camera2"
    "com.android.launcher3"
)

# Function: Check device connection
check_device() {
    log_info "Checking device connection..."
    if ! $ADB -s $DEVICE get-state &>/dev/null; then
        log_error "Device $DEVICE not found!"
        exit 1
    fi
    log_success "Device connected: $DEVICE"
}

# Function: Clear logcat and start fresh logging
start_logging() {
    log_info "Starting logcat capture..."
    $ADB -s $DEVICE logcat -c
    $ADB -s $DEVICE logcat -v time > "$LOG_DIR/logcat_${TIMESTAMP}.log" &
    LOGCAT_PID=$!
    echo $LOGCAT_PID > /tmp/voiceos_logcat_pid
    log_success "Logcat capture started (PID: $LOGCAT_PID)"
}

# Function: Stop logging
stop_logging() {
    if [ -f /tmp/voiceos_logcat_pid ]; then
        LOGCAT_PID=$(cat /tmp/voiceos_logcat_pid)
        kill $LOGCAT_PID 2>/dev/null || true
        rm /tmp/voiceos_logcat_pid
        log_info "Logcat capture stopped"
    fi
}

# Function: Enable accessibility service
enable_accessibility() {
    log_info "Enabling VoiceOS Accessibility Service..."

    # Get current services
    CURRENT=$($ADB -s $DEVICE shell settings get secure enabled_accessibility_services 2>/dev/null || echo "")

    if [[ "$CURRENT" == *"$VOICEOS_SERVICE"* ]]; then
        log_info "VoiceOS service already enabled"
    else
        if [ -z "$CURRENT" ] || [ "$CURRENT" == "null" ]; then
            $ADB -s $DEVICE shell settings put secure enabled_accessibility_services "$VOICEOS_SERVICE"
        else
            $ADB -s $DEVICE shell settings put secure enabled_accessibility_services "$CURRENT:$VOICEOS_SERVICE"
        fi
    fi

    # Enable accessibility
    $ADB -s $DEVICE shell settings put secure accessibility_enabled 1

    sleep 2
    log_success "Accessibility service enabled"
}

# Function: Launch VoiceOS app
launch_voiceos() {
    log_info "Launching VoiceOS..."
    $ADB -s $DEVICE shell am start -n "$VOICEOS_PKG/.MainActivity" 2>/dev/null || \
    $ADB -s $DEVICE shell am start -n "$VOICEOS_PKG/.ui.activities.MainActivity" 2>/dev/null || \
    $ADB -s $DEVICE shell monkey -p "$VOICEOS_PKG" 1 2>/dev/null
    sleep 3
    log_success "VoiceOS launched"
}

# Function: Launch an app and wait for JIT capture
test_app() {
    local pkg=$1
    local app_name=$(echo $pkg | sed 's/com.android.//')

    log_info "Testing app: $app_name ($pkg)"

    # Launch the app
    $ADB -s $DEVICE shell am start -n "$pkg/.MainActivity" 2>/dev/null || \
    $ADB -s $DEVICE shell monkey -p "$pkg" 1 2>/dev/null

    sleep 2  # Wait for JIT to capture screen

    # Get UI hierarchy for clicking
    log_info "  Capturing UI hierarchy..."
    $ADB -s $DEVICE shell uiautomator dump /sdcard/ui_dump.xml 2>/dev/null || true

    # Click on clickable elements (simulate user interaction)
    log_info "  Performing UI interactions..."

    # Get screen dimensions
    SCREEN_SIZE=$($ADB -s $DEVICE shell wm size | grep -oE '[0-9]+x[0-9]+')
    WIDTH=$(echo $SCREEN_SIZE | cut -d'x' -f1)
    HEIGHT=$(echo $SCREEN_SIZE | cut -d'x' -f2)

    # Click at various positions to trigger element detection
    # Center of screen
    $ADB -s $DEVICE shell input tap $((WIDTH/2)) $((HEIGHT/2))
    sleep 1

    # Upper area (typically has buttons/menus)
    $ADB -s $DEVICE shell input tap $((WIDTH/2)) $((HEIGHT/4))
    sleep 1

    # Scroll down to trigger more element captures
    $ADB -s $DEVICE shell input swipe $((WIDTH/2)) $((HEIGHT*3/4)) $((WIDTH/2)) $((HEIGHT/4)) 500
    sleep 2

    # Scroll back up
    $ADB -s $DEVICE shell input swipe $((WIDTH/2)) $((HEIGHT/4)) $((WIDTH/2)) $((HEIGHT*3/4)) 500
    sleep 2

    log_success "  App $app_name tested"
}

# Function: Press back button
go_back() {
    $ADB -s $DEVICE shell input keyevent KEYCODE_BACK
    sleep 1
}

# Function: Go to home screen
go_home() {
    $ADB -s $DEVICE shell input keyevent KEYCODE_HOME
    sleep 1
}

# Function: Capture performance metrics from logcat
analyze_logs() {
    log_info "Analyzing captured logs..."

    local logfile="$LOG_DIR/logcat_${TIMESTAMP}.log"
    local report="$LOG_DIR/test_report_${TIMESTAMP}.md"

    echo "# VoiceOS Automated Test Report" > "$report"
    echo "**Timestamp:** $TIMESTAMP" >> "$report"
    echo "" >> "$report"

    # JIT Learning Analysis
    echo "## JIT Learning Metrics" >> "$report"
    echo "\`\`\`" >> "$report"

    # Count JIT captures
    JIT_CAPTURES=$(grep -c "JIT captured" "$logfile" 2>/dev/null || echo "0")
    echo "Total JIT Captures: $JIT_CAPTURES" >> "$report"

    # Count screens saved
    SCREENS_SAVED=$(grep -c "Screen saved" "$logfile" 2>/dev/null || echo "0")
    echo "Screens Saved: $SCREENS_SAVED" >> "$report"

    # Count elements persisted
    ELEMENTS=$(grep -oE "Persisted [0-9]+ new elements" "$logfile" 2>/dev/null | awk '{sum+=$2} END {print sum}' || echo "0")
    echo "Total Elements Persisted: ${ELEMENTS:-0}" >> "$report"

    # Count voice commands generated
    COMMANDS=$(grep -oE "Generated [0-9]+ voice commands" "$logfile" 2>/dev/null | awk '{sum+=$2} END {print sum}' || echo "0")
    echo "Voice Commands Generated: ${COMMANDS:-0}" >> "$report"

    # Count skipped screens (deduplication working)
    SKIPPED=$(grep -c "Screen already captured, skipping" "$logfile" 2>/dev/null || echo "0")
    echo "Screens Skipped (deduplicated): $SKIPPED" >> "$report"

    echo "\`\`\`" >> "$report"
    echo "" >> "$report"

    # Performance Metrics
    echo "## Performance Metrics" >> "$report"
    echo "\`\`\`" >> "$report"

    # JIT timing
    grep -oE "JIT learned screen in [0-9]+ms" "$logfile" 2>/dev/null | \
        awk -F'in |ms' '{sum+=$2; count++} END {if(count>0) printf "Avg JIT Time: %.0fms (n=%d)\n", sum/count, count}' >> "$report" || echo "Avg JIT Time: N/A" >> "$report"

    # Element capture timing
    grep -oE "Captured [0-9]+ elements in [0-9]+ms" "$logfile" 2>/dev/null | \
        awk -F'in |ms' '{sum+=$2; count++} END {if(count>0) printf "Avg Capture Time: %.0fms (n=%d)\n", sum/count, count}' >> "$report" || echo "Avg Capture Time: N/A" >> "$report"

    echo "\`\`\`" >> "$report"
    echo "" >> "$report"

    # Warnings and Errors
    echo "## Warnings & Issues" >> "$report"
    echo "\`\`\`" >> "$report"

    TIMEOUTS=$(grep -c "Element capture timed out" "$logfile" 2>/dev/null || echo "0")
    echo "Element Capture Timeouts: $TIMEOUTS" >> "$report"

    NO_ROOT=$(grep -c "No root node available" "$logfile" 2>/dev/null || echo "0")
    echo "No Root Node Errors: $NO_ROOT" >> "$report"

    FALLBACK=$(grep -c "using fallback hash" "$logfile" 2>/dev/null || echo "0")
    echo "Fallback Hash Used: $FALLBACK" >> "$report"

    echo "\`\`\`" >> "$report"
    echo "" >> "$report"

    # Apps tested
    echo "## Apps Tested" >> "$report"
    for app in "${TEST_APPS[@]}"; do
        app_captures=$(grep -c "$app" "$logfile" 2>/dev/null || echo "0")
        echo "- $app: $app_captures log entries" >> "$report"
    done

    echo "" >> "$report"
    echo "## Raw Log Sample (Last 50 JIT entries)" >> "$report"
    echo "\`\`\`" >> "$report"
    grep -E "JIT|JitElement|Screen saved|voice commands" "$logfile" 2>/dev/null | tail -50 >> "$report" || echo "No JIT entries found" >> "$report"
    echo "\`\`\`" >> "$report"

    log_success "Report generated: $report"
    echo ""
    echo "=============================================="
    echo "  TEST SUMMARY"
    echo "=============================================="
    echo "JIT Captures: $JIT_CAPTURES"
    echo "Screens Saved: $SCREENS_SAVED"
    echo "Elements Persisted: ${ELEMENTS:-0}"
    echo "Voice Commands: ${COMMANDS:-0}"
    echo "Deduplicated: $SKIPPED"
    echo "Timeouts: $TIMEOUTS"
    echo "=============================================="
    echo "Full report: $report"
    echo "Full logcat: $logfile"
}

# Function: Run comprehensive test
run_test() {
    check_device
    start_logging

    # Give logcat time to start
    sleep 2

    enable_accessibility
    launch_voiceos

    # Wait for service to initialize
    sleep 5

    log_info "Starting app cycling test..."

    for app in "${TEST_APPS[@]}"; do
        test_app "$app"
        go_home
        sleep 1
    done

    # Return to VoiceOS to check state
    launch_voiceos
    sleep 3

    # Additional interactions in VoiceOS itself
    log_info "Testing VoiceOS internal screens..."

    # Navigate through VoiceOS screens
    $ADB -s $DEVICE shell input tap 200 400  # Tap on menu items
    sleep 2
    go_back

    $ADB -s $DEVICE shell input tap 200 600
    sleep 2
    go_back

    # Final wait for any pending captures
    log_info "Waiting for final captures..."
    sleep 5

    stop_logging
    analyze_logs
}

# Cleanup on exit
trap stop_logging EXIT

# Main execution
case "${1:-run}" in
    run)
        run_test
        ;;
    enable)
        check_device
        enable_accessibility
        ;;
    analyze)
        analyze_logs
        ;;
    *)
        echo "Usage: $0 [run|enable|analyze]"
        exit 1
        ;;
esac
