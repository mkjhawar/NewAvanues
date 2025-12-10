#!/bin/bash
# LearnApp VUID Creation Fix - Phase 5 Test Automation
# Version: 1.0
# Date: 2025-12-08

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="/Volumes/M-Drive/Coding/NewAvanues"
TEST_DATE=$(date "+%Y%m%d-%H%M%S")
RESULTS_DIR="${PROJECT_ROOT}/Docs/VoiceOS/Testing/VUID-Phase5-${TEST_DATE}"
VOICEOS_PACKAGE="com.augmentalis.voiceos"
EXPLORATION_TIMEOUT=1080000  # 18 minutes in milliseconds

# Test apps configuration
declare -A TEST_APPS=(
    ["deviceinfo"]="com.ytheekshana.deviceinfo"
    ["teams"]="com.microsoft.teams"
    ["googlenews"]="com.google.android.apps.magazines"
    ["amazon"]="com.amazon.mShop.android.shopping"
    ["settings"]="com.android.settings"
    ["facebook"]="com.facebook.katana"
    ["testapp"]="com.augmentalis.testapp"
)

# Voice commands per app
declare -A VOICE_COMMANDS_DEVICEINFO=(
    [1]="Select CPU tab"
    [2]="Open tests card"
    [3]="Rate this app"
)

declare -A VOICE_COMMANDS_TEAMS=(
    [1]="Open chat"
    [2]="Select new message"
)

declare -A VOICE_COMMANDS_GOOGLENEWS=(
    [1]="Select Following tab"
    [2]="Open first news card"
)

declare -A VOICE_COMMANDS_AMAZON=(
    [1]="Select first product"
    [2]="Add to cart"
)

declare -A VOICE_COMMANDS_SETTINGS=(
    [1]="Open Wi-Fi settings"
    [2]="Toggle Bluetooth"
)

declare -A VOICE_COMMANDS_FACEBOOK=(
    [1]="Select notifications tab"
    [2]="Like first post"
)

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_device() {
    log_info "Checking for connected device..."
    DEVICE_SERIAL=$(adb devices | awk 'NR==2 {print $1}')

    if [ -z "$DEVICE_SERIAL" ]; then
        log_error "No device connected. Please connect device or start emulator."
        exit 1
    fi

    log_info "Device connected: $DEVICE_SERIAL"
}

create_results_dir() {
    log_info "Creating results directory: $RESULTS_DIR"
    mkdir -p "$RESULTS_DIR"
    mkdir -p "$RESULTS_DIR/screenshots"
    mkdir -p "$RESULTS_DIR/logs"
    mkdir -p "$RESULTS_DIR/databases"

    # Create summary file
    cat > "$RESULTS_DIR/test-summary.txt" << EOF
LearnApp VUID Creation Fix - Phase 5 Test Results
==================================================
Test Date: $(date "+%Y-%m-%d %H:%M:%S")
Device: $DEVICE_SERIAL
VoiceOS Package: $VOICEOS_PACKAGE

Apps to Test: ${#TEST_APPS[@]}
EOF
}

check_voiceos_installed() {
    log_info "Checking VoiceOS installation..."

    if ! adb shell pm list packages | grep -q "$VOICEOS_PACKAGE"; then
        log_error "VoiceOS not installed on device"
        exit 1
    fi

    log_info "VoiceOS installed ✓"
}

clear_app_data() {
    local package=$1
    log_info "Clearing data for $package"
    adb shell pm clear "$package" 2>/dev/null || log_warn "Could not clear $package (may not be installed)"
}

delete_app_vuids() {
    local package=$1
    log_info "Deleting VUIDs for $package"
    adb shell am broadcast -a com.augmentalis.voiceos.DELETE_VUIDS --es package "$package" 2>/dev/null || true
}

start_exploration() {
    local package=$1
    local app_name=$2

    log_info "Starting exploration for $app_name ($package)"

    # Start exploration via LearnApp
    adb shell am start -n com.augmentalis.voiceos/.learnapp.LearnAppActivity \
        --es target_package "$package" \
        --ei timeout "$EXPLORATION_TIMEOUT"

    log_info "Exploration started. Monitoring progress..."
}

monitor_exploration() {
    local package=$1
    local app_name=$2
    local log_file="$RESULTS_DIR/logs/${app_name}-logcat.txt"

    log_info "Monitoring exploration (this may take up to 18 minutes)..."

    # Start logcat in background
    adb logcat -s LearnApp:* ExplorationEngine:* UUIDCreator:* ClickabilityDetector:* > "$log_file" &
    LOGCAT_PID=$!

    # Wait for exploration to complete
    local timeout=1200  # 20 minutes (18 min exploration + 2 min buffer)
    local elapsed=0

    while [ $elapsed -lt $timeout ]; do
        # Check if exploration is complete
        if adb logcat -d | grep -q "Exploration completed for $package"; then
            log_info "Exploration completed!"
            kill $LOGCAT_PID 2>/dev/null || true
            return 0
        fi

        # Check if exploration failed
        if adb logcat -d | grep -q "Exploration failed for $package"; then
            log_error "Exploration failed!"
            kill $LOGCAT_PID 2>/dev/null || true
            return 1
        fi

        sleep 10
        elapsed=$((elapsed + 10))

        # Show progress every minute
        if [ $((elapsed % 60)) -eq 0 ]; then
            log_info "  Elapsed: $((elapsed / 60)) minutes..."
        fi
    done

    log_warn "Exploration timeout reached"
    kill $LOGCAT_PID 2>/dev/null || true
    return 1
}

collect_metrics() {
    local package=$1
    local app_name=$2
    local metrics_file="$RESULTS_DIR/${app_name}-metrics.txt"

    log_info "Collecting metrics for $app_name..."

    # Get elements detected
    local elements_detected=$(adb shell "run-as $VOICEOS_PACKAGE sqlite3 databases/learnapp.db \
        \"SELECT COUNT(*) FROM elements WHERE package='$package'\"" 2>/dev/null || echo "0")

    # Get VUIDs created
    local vuids_created=$(adb shell "run-as $VOICEOS_PACKAGE sqlite3 databases/voiceos.db \
        \"SELECT COUNT(*) FROM vuids WHERE packageName='$package'\"" 2>/dev/null || echo "0")

    # Calculate creation rate
    local creation_rate=0
    if [ "$elements_detected" -gt 0 ]; then
        creation_rate=$(echo "scale=2; $vuids_created / $elements_detected * 100" | bc)
    fi

    # Get breakdown by type
    adb shell "run-as $VOICEOS_PACKAGE sqlite3 databases/learnapp.db \
        \"SELECT className, COUNT(*) as count \
         FROM elements \
         WHERE package='$package' \
         GROUP BY className\"" > "$RESULTS_DIR/${app_name}-element-types.txt" 2>/dev/null || true

    # Write metrics
    cat > "$metrics_file" << EOF
App: $app_name
Package: $package
Test Date: $(date "+%Y-%m-%d %H:%M:%S")

VUID Creation Metrics:
- Elements Detected: $elements_detected
- VUIDs Created: $vuids_created
- Creation Rate: $creation_rate%

Status: $([ $(echo "$creation_rate >= 95" | bc) -eq 1 ] && echo "PASS ✓" || echo "FAIL ✗")
EOF

    log_info "Metrics collected: $elements_detected detected, $vuids_created VUIDs created ($creation_rate%)"

    # Return pass/fail
    [ $(echo "$creation_rate >= 95" | bc) -eq 1 ]
}

test_voice_command() {
    local command=$1
    local app_name=$2
    local command_num=$3

    log_info "Testing voice command: \"$command\""

    # Send voice command
    adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
        --es command "$command" > /dev/null 2>&1

    # Wait for command to execute
    sleep 2

    # Capture screenshot
    adb exec-out screencap -p > "$RESULTS_DIR/screenshots/${app_name}-command-${command_num}.png"

    log_info "  Screenshot saved"
}

test_app() {
    local app_key=$1
    local package=${TEST_APPS[$app_key]}
    local app_name=$app_key

    log_info "========================================="
    log_info "Testing: $app_name"
    log_info "Package: $package"
    log_info "========================================="

    # Pre-test setup
    clear_app_data "$package"
    delete_app_vuids "$package"
    sleep 2

    # Capture before screenshot
    adb exec-out screencap -p > "$RESULTS_DIR/screenshots/${app_name}-before.png"

    # Start exploration
    start_exploration "$package" "$app_name"

    # Monitor exploration
    if ! monitor_exploration "$package" "$app_name"; then
        log_error "Exploration failed for $app_name"
        return 1
    fi

    # Collect metrics
    if ! collect_metrics "$package" "$app_name"; then
        log_warn "$app_name did not achieve 95%+ creation rate"
    fi

    # Capture after screenshot
    adb exec-out screencap -p > "$RESULTS_DIR/screenshots/${app_name}-after.png"

    # Test voice commands
    log_info "Testing voice commands for $app_name..."

    case $app_name in
        deviceinfo)
            for cmd_num in "${!VOICE_COMMANDS_DEVICEINFO[@]}"; do
                test_voice_command "${VOICE_COMMANDS_DEVICEINFO[$cmd_num]}" "$app_name" "$cmd_num"
            done
            ;;
        teams)
            for cmd_num in "${!VOICE_COMMANDS_TEAMS[@]}"; do
                test_voice_command "${VOICE_COMMANDS_TEAMS[$cmd_num]}" "$app_name" "$cmd_num"
            done
            ;;
        googlenews)
            for cmd_num in "${!VOICE_COMMANDS_GOOGLENEWS[@]}"; do
                test_voice_command "${VOICE_COMMANDS_GOOGLENEWS[$cmd_num]}" "$app_name" "$cmd_num"
            done
            ;;
        amazon)
            for cmd_num in "${!VOICE_COMMANDS_AMAZON[@]}"; do
                test_voice_command "${VOICE_COMMANDS_AMAZON[$cmd_num]}" "$app_name" "$cmd_num"
            done
            ;;
        settings)
            for cmd_num in "${!VOICE_COMMANDS_SETTINGS[@]}"; do
                test_voice_command "${VOICE_COMMANDS_SETTINGS[$cmd_num]}" "$app_name" "$cmd_num"
            done
            ;;
        facebook)
            for cmd_num in "${!VOICE_COMMANDS_FACEBOOK[@]}"; do
                test_voice_command "${VOICE_COMMANDS_FACEBOOK[$cmd_num]}" "$app_name" "$cmd_num"
            done
            ;;
    esac

    log_info "$app_name testing complete"
    echo ""
}

profile_performance() {
    log_info "========================================="
    log_info "Performance Profiling"
    log_info "========================================="

    # Extract timing data from logs
    log_info "Analyzing VUID creation timing..."

    for app_key in "${!TEST_APPS[@]}"; do
        local log_file="$RESULTS_DIR/logs/${app_key}-logcat.txt"

        if [ -f "$log_file" ]; then
            grep -E "VUID creation time|Clickability scoring time" "$log_file" > \
                "$RESULTS_DIR/${app_key}-timing.txt" 2>/dev/null || true
        fi
    done

    # Memory profiling
    log_info "Capturing memory profile..."
    adb shell dumpsys meminfo $VOICEOS_PACKAGE > "$RESULTS_DIR/memory-profile.txt"

    log_info "Performance profiling complete"
}

run_regression_tests() {
    log_info "========================================="
    log_info "Regression Testing"
    log_info "========================================="

    cd "$PROJECT_ROOT"

    # Unit tests
    log_info "Running unit tests..."
    ./gradlew :VoiceOS:apps:VoiceOSCore:testDebugUnitTest --tests "*.learnapp.*" > \
        "$RESULTS_DIR/unit-test-results.txt" 2>&1 || log_warn "Some unit tests failed"

    # Integration tests
    log_info "Running integration tests..."
    ./gradlew :VoiceOS:apps:VoiceOSCore:connectedDebugAndroidTest --tests "*.learnapp.integration.*" > \
        "$RESULTS_DIR/integration-test-results.txt" 2>&1 || log_warn "Some integration tests failed"

    log_info "Regression testing complete"
}

generate_summary() {
    log_info "========================================="
    log_info "Generating Test Summary"
    log_info "========================================="

    local summary_file="$RESULTS_DIR/validation-summary.txt"
    local passed_count=0
    local total_count=${#TEST_APPS[@]}

    # Count passed apps
    for app_key in "${!TEST_APPS[@]}"; do
        if [ -f "$RESULTS_DIR/${app_key}-metrics.txt" ]; then
            if grep -q "Status: PASS" "$RESULTS_DIR/${app_key}-metrics.txt"; then
                passed_count=$((passed_count + 1))
            fi
        fi
    done

    # Generate summary
    cat > "$summary_file" << EOF
LearnApp VUID Creation Fix - Phase 5 Validation Summary
========================================================

Test Date: $(date "+%Y-%m-%d %H:%M:%S")
Duration: TBD
Apps Tested: $total_count

Overall Results:
- Apps Passing (95%+ creation rate): $passed_count/$total_count
- Average Creation Rate: TBD (calculated from metrics)

Performance:
- Average Overhead: TBD
- Average Exploration Time Increase: TBD
- Memory Impact: TBD

Regression Tests:
- Unit Tests: See unit-test-results.txt
- Integration Tests: See integration-test-results.txt

Status: $([ $passed_count -eq $total_count ] && echo "PASS ✓" || echo "FAIL ✗")

Detailed Results:
$(for app_key in "${!TEST_APPS[@]}"; do
    if [ -f "$RESULTS_DIR/${app_key}-metrics.txt" ]; then
        grep -A 5 "VUID Creation Metrics:" "$RESULTS_DIR/${app_key}-metrics.txt"
        echo ""
    fi
done)
EOF

    log_info "Summary generated: $summary_file"

    # Display summary
    cat "$summary_file"
}

# Main execution
main() {
    log_info "LearnApp VUID Creation Fix - Phase 5 Test Automation"
    log_info "======================================================"
    echo ""

    # Setup
    check_device
    create_results_dir
    check_voiceos_installed

    # Test each app
    for app_key in "${!TEST_APPS[@]}"; do
        test_app "$app_key" || log_warn "Test failed for $app_key"
        sleep 5  # Brief pause between apps
    done

    # Performance profiling
    profile_performance

    # Regression tests
    run_regression_tests

    # Generate summary
    generate_summary

    log_info ""
    log_info "======================================================"
    log_info "All tests complete!"
    log_info "Results saved to: $RESULTS_DIR"
    log_info "======================================================"
}

# Run main if executed directly
if [ "${BASH_SOURCE[0]}" == "${0}" ]; then
    main "$@"
fi
