#!/usr/bin/env bash
#
# voiceos-autonomous-test.sh - Enhanced VoiceOS Autonomous Testing with JSON Metrics
#
# Tests JIT Learning, LearnAppLite, Database Updates, Command Generation
# Outputs detailed metrics in JSON format for programmatic analysis
#
# Requirements: bash 4.0+ (for associative arrays)
#   macOS: brew install bash && use /opt/homebrew/bin/bash or /usr/local/bin/bash
#
# Usage:
#   ./voiceos-autonomous-test.sh                    # Default apps
#   ./voiceos-autonomous-test.sh --output /path/    # Custom output directory
#   ./voiceos-autonomous-test.sh --device emulator-5556
#   ./voiceos-autonomous-test.sh --apps "com.android.settings,com.android.contacts"
#
# Author: VoiceOS Development Team
# Version: 2.0.0
#

# Check bash version (need 4.0+ for associative arrays)
if [[ "${BASH_VERSINFO[0]}" -lt 4 ]]; then
    echo "Error: This script requires bash 4.0 or higher for associative arrays."
    echo "Current bash version: $BASH_VERSION"
    echo ""
    echo "On macOS, install newer bash with: brew install bash"
    echo "Then run with: /opt/homebrew/bin/bash $0 $*"
    echo "Or:           /usr/local/bin/bash $0 $*"
    exit 1
fi

set -euo pipefail

# ==============================================================================
# CONFIGURATION
# ==============================================================================

ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
DEVICE="${DEVICE:-emulator-5554}"
OUTPUT_DIR="${OUTPUT_DIR:-/tmp}"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
LOG_FILE="${OUTPUT_DIR}/voiceos-test-${TIMESTAMP}.log"
JSON_FILE="${OUTPUT_DIR}/voiceos-metrics-${TIMESTAMP}.json"
VERBOSE=${VERBOSE:-0}

# Colors for terminal output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Default test apps: package|action|name
DEFAULT_APPS=(
    "com.android.settings|android.settings.SETTINGS|Settings"
    "com.google.android.apps.messaging|android.intent.action.MAIN|Messages"
    "com.android.contacts|android.intent.action.MAIN|Contacts"
    "com.google.android.calendar|android.intent.action.MAIN|Calendar"
    "com.android.vending|android.intent.action.MAIN|Play Store"
    "com.google.android.gm|android.intent.action.MAIN|Gmail"
    "com.google.android.apps.maps|android.intent.action.MAIN|Maps"
)

# Apps to test (can be overridden via --apps)
APPS=()

# ==============================================================================
# METRICS STORAGE
# ==============================================================================

# Global metrics (associative array - requires bash 4+)
declare -A OVERALL_METRICS=(
    [total_apps]=0
    [total_screens]=0
    [total_elements]=0
    [total_vuids]=0
    [total_commands]=0
    [total_errors]=0
    [start_time]=$(date +%s%3N)
    [end_time]=0
)

# Per-app metrics stored as JSON array
APP_METRICS_JSON=()

# ==============================================================================
# UTILITY FUNCTIONS
# ==============================================================================

log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    echo "[$timestamp] [$level] $message" >> "$LOG_FILE"

    case $level in
        INFO)  echo -e "${BLUE}[INFO]${NC} $message" ;;
        OK)    echo -e "${GREEN}[OK]${NC} $message" ;;
        WARN)  echo -e "${YELLOW}[WARN]${NC} $message" ;;
        ERROR) echo -e "${RED}[ERROR]${NC} $message" ;;
        DEBUG) [[ $VERBOSE -eq 1 ]] && echo -e "${CYAN}[DEBUG]${NC} $message" ;;
        TEST)  echo -e "${MAGENTA}[TEST]${NC} $message" ;;
        *)     echo -e "$message" ;;
    esac
}

print_header() {
    echo ""
    echo -e "${CYAN}=============================================="
    echo -e "VoiceOS Autonomous Testing Suite v2.0"
    echo -e "=============================================="
    echo -e "Timestamp: $TIMESTAMP"
    echo -e "Device:    $DEVICE"
    echo -e "Output:    $JSON_FILE"
    echo -e "Log:       $LOG_FILE"
    echo -e "==============================================${NC}"
    echo ""
}

# Escape JSON strings
json_escape() {
    local str="$1"
    str="${str//\\/\\\\}"
    str="${str//\"/\\\"}"
    str="${str//$'\n'/\\n}"
    str="${str//$'\r'/\\r}"
    str="${str//$'\t'/\\t}"
    echo "$str"
}

# ==============================================================================
# ADB WRAPPER FUNCTIONS
# ==============================================================================

adb_cmd() {
    $ADB -s "$DEVICE" "$@" 2>/dev/null
}

clear_logs() {
    log DEBUG "Clearing logcat buffer..."
    adb_cmd logcat -c
}

get_app_version() {
    local pkg=$1
    local version=$(adb_cmd shell dumpsys package "$pkg" 2>/dev/null | grep -m1 "versionName=" | sed 's/.*versionName=//;s/ .*//' || echo "unknown")
    echo "$version"
}

is_app_installed() {
    local pkg=$1
    adb_cmd shell pm list packages 2>/dev/null | grep -q "^package:${pkg}$"
}

# ==============================================================================
# METRICS COLLECTION FUNCTIONS
# ==============================================================================

# Extract metrics from logcat for a specific app test session
collect_app_metrics() {
    local pkg=$1
    local name=$2
    local start_ms=$3
    local end_ms=$4

    local version=$(get_app_version "$pkg")

    # Screen metrics
    local screens_seen=$(adb_cmd logcat -d | grep -c "onAccessibilityEvent.*TYPE_WINDOW_STATE_CHANGED" 2>/dev/null || echo "0")
    local screens_hashed=$(adb_cmd logcat -d | grep -c "Screen hash\|ScreenHash\|hash.*screen" 2>/dev/null || echo "0")
    local screens_cached=$(adb_cmd logcat -d | grep -c "cached.*screen\|screen.*cached\|Cache.*hit" 2>/dev/null || echo "0")
    local screens_learned=$(adb_cmd logcat -d | grep -c "Learning.*screen\|Screen.*learned\|learned.*screen" 2>/dev/null || echo "0")

    # Element/VUID metrics
    local elements_extracted=$(adb_cmd logcat -d | grep -oP "Extracted \K\d+" 2>/dev/null | awk '{sum+=$1} END {print sum+0}' || echo "0")
    local vuids_created=$(adb_cmd logcat -d | grep -c "VUID.*created\|Created.*VUID\|VuidCreator\|GeneratedVUID" 2>/dev/null || echo "0")

    # Command metrics
    local commands_seen=$(adb_cmd logcat -d | grep -c "command.*detected\|detected.*command" 2>/dev/null || echo "0")
    local commands_generated=$(adb_cmd logcat -d | grep -c "Generated.*command\|command.*generated\|CommandGen" 2>/dev/null || echo "0")

    # JIT metrics
    local jit_captures=$(adb_cmd logcat -d | grep -c "JIT.*captured\|JitElementCapture\|JustInTime.*capture" 2>/dev/null || echo "0")

    # Database metrics
    local db_inserts=$(adb_cmd logcat -d | grep -c "INSERT.*command\|command.*INSERT\|persisted.*command" 2>/dev/null || echo "0")

    # Error count for this app
    local errors=$(adb_cmd logcat -d | grep -iE "voiceos|augmentalis" | grep -iE "Exception|Error|FATAL" | grep -v "AiAi\|ImeTracker\|ResourceMonitor" | wc -l | tr -d ' ')

    # Calculate duration
    local duration_ms=$((end_ms - start_ms))

    # Build JSON object for this app
    local app_json=$(cat <<EOF
{
    "package": "$(json_escape "$pkg")",
    "name": "$(json_escape "$name")",
    "version": "$(json_escape "$version")",
    "timestamp_start": $start_ms,
    "timestamp_end": $end_ms,
    "duration_ms": $duration_ms,
    "metrics": {
        "screens": {
            "seen": $screens_seen,
            "hashed": $screens_hashed,
            "cached": $screens_cached,
            "learned": $screens_learned
        },
        "elements": {
            "extracted": ${elements_extracted:-0}
        },
        "vuids": {
            "created": ${vuids_created:-0}
        },
        "commands": {
            "seen": $commands_seen,
            "generated": $commands_generated,
            "db_inserts": $db_inserts
        },
        "jit": {
            "captures": $jit_captures
        },
        "errors": $errors
    }
}
EOF
)

    # Add to app metrics array
    APP_METRICS_JSON+=("$app_json")

    # Update overall metrics
    OVERALL_METRICS[total_screens]=$((${OVERALL_METRICS[total_screens]} + screens_seen))
    OVERALL_METRICS[total_elements]=$((${OVERALL_METRICS[total_elements]} + ${elements_extracted:-0}))
    OVERALL_METRICS[total_vuids]=$((${OVERALL_METRICS[total_vuids]} + vuids_created))
    OVERALL_METRICS[total_commands]=$((${OVERALL_METRICS[total_commands]} + commands_generated))
    OVERALL_METRICS[total_errors]=$((${OVERALL_METRICS[total_errors]} + errors))

    # Log summary for this app
    log OK "App: $name | Screens: $screens_seen seen/$screens_hashed hashed | Elements: ${elements_extracted:-0} | VUIDs: $vuids_created | Commands: $commands_generated | Errors: $errors"
}

# ==============================================================================
# TEST FUNCTIONS
# ==============================================================================

ensure_voiceos_running() {
    log INFO "Ensuring VoiceOS is running..."

    # Check if VoiceOS accessibility service is enabled
    local enabled_services=$(adb_cmd shell settings get secure enabled_accessibility_services)
    local voiceos_service="com.augmentalis.voiceos/.voiceoscore.accessibility.VoiceOSService"

    if [[ "$enabled_services" != *"$voiceos_service"* ]]; then
        log WARN "VoiceOS accessibility service not enabled. Enabling..."

        if [[ "$enabled_services" == "null" ]] || [[ -z "$enabled_services" ]]; then
            adb_cmd shell settings put secure enabled_accessibility_services "$voiceos_service"
        else
            adb_cmd shell settings put secure enabled_accessibility_services "${enabled_services}:${voiceos_service}"
        fi

        adb_cmd shell settings put secure accessibility_enabled 1
        sleep 3
    fi

    # Launch VoiceOS app to ensure everything is initialized
    adb_cmd shell am start -n com.augmentalis.voiceos/.MainActivity >/dev/null 2>&1 || true
    sleep 2

    # Go back to home
    adb_cmd shell input keyevent KEYCODE_HOME
    sleep 1

    log OK "VoiceOS is ready"
}

launch_and_learn() {
    local pkg=$1
    local action=$2
    local name=$3

    log TEST "Testing: $name ($pkg)"

    # Check if app is installed
    if ! is_app_installed "$pkg"; then
        log WARN "App $name ($pkg) not installed - skipping"
        return 1
    fi

    # Record start time
    local start_ms=$(date +%s%3N)

    # Clear logs before testing this app
    clear_logs

    # Launch the app
    log DEBUG "Launching $name..."
    adb_cmd shell am start -a "$action" -c android.intent.category.LAUNCHER 2>/dev/null || \
    adb_cmd shell am start -a "$action" 2>/dev/null || \
    adb_cmd shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 2>/dev/null || true

    # Wait for JIT learning to capture initial screen
    sleep 3

    # Perform interactions to trigger more captures
    log DEBUG "Performing scroll interactions..."
    adb_cmd shell input swipe 500 1500 500 500 500   # Scroll down
    sleep 2
    adb_cmd shell input swipe 500 500 500 1500 500   # Scroll up
    sleep 2

    # Try tapping on some elements (may trigger navigation)
    adb_cmd shell input tap 500 300 2>/dev/null || true
    sleep 2
    adb_cmd shell input keyevent KEYCODE_BACK 2>/dev/null || true
    sleep 1

    # Record end time
    local end_ms=$(date +%s%3N)

    # Collect metrics for this app
    collect_app_metrics "$pkg" "$name" "$start_ms" "$end_ms"

    # Return to home
    adb_cmd shell input keyevent KEYCODE_HOME
    sleep 1

    return 0
}

# ==============================================================================
# REPORT GENERATION
# ==============================================================================

generate_json_report() {
    log INFO "Generating JSON metrics report..."

    OVERALL_METRICS[end_time]=$(date +%s%3N)
    local total_duration=$((${OVERALL_METRICS[end_time]} - ${OVERALL_METRICS[start_time]}))

    # Build apps JSON array
    local apps_json=""
    local first=1
    for app_json in "${APP_METRICS_JSON[@]}"; do
        if [[ $first -eq 1 ]]; then
            apps_json="$app_json"
            first=0
        else
            apps_json="$apps_json,$app_json"
        fi
    done

    # Generate complete JSON report
    cat > "$JSON_FILE" <<EOF
{
    "test_run": {
        "id": "$(uuidgen 2>/dev/null || echo "test-${TIMESTAMP}")",
        "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
        "timestamp_start_ms": ${OVERALL_METRICS[start_time]},
        "timestamp_end_ms": ${OVERALL_METRICS[end_time]},
        "duration_ms": $total_duration,
        "device": "$(json_escape "$DEVICE")",
        "voiceos_version": "$(get_app_version "com.augmentalis.voiceos")"
    },
    "summary": {
        "total_apps_tested": ${OVERALL_METRICS[total_apps]},
        "total_screens_seen": ${OVERALL_METRICS[total_screens]},
        "total_elements_extracted": ${OVERALL_METRICS[total_elements]},
        "total_vuids_created": ${OVERALL_METRICS[total_vuids]},
        "total_commands_generated": ${OVERALL_METRICS[total_commands]},
        "total_errors": ${OVERALL_METRICS[total_errors]},
        "success_rate": $(awk "BEGIN {printf \"%.2f\", (${OVERALL_METRICS[total_apps]} > 0 && ${OVERALL_METRICS[total_errors]} == 0) ? 100 : ((${OVERALL_METRICS[total_apps]} - ${OVERALL_METRICS[total_errors]}) / ${OVERALL_METRICS[total_apps]:-1} * 100)}")
    },
    "apps": [
        $apps_json
    ],
    "environment": {
        "adb_path": "$(json_escape "$ADB")",
        "output_dir": "$(json_escape "$OUTPUT_DIR")",
        "log_file": "$(json_escape "$LOG_FILE")",
        "hostname": "$(hostname)"
    }
}
EOF

    log OK "JSON report saved to: $JSON_FILE"
}

print_summary() {
    local total_duration=$((${OVERALL_METRICS[end_time]} - ${OVERALL_METRICS[start_time]}))
    local duration_sec=$((total_duration / 1000))

    echo ""
    echo -e "${CYAN}=============================================="
    echo -e "TEST SUMMARY"
    echo -e "==============================================${NC}"
    echo ""
    echo -e "Apps Tested:       ${GREEN}${OVERALL_METRICS[total_apps]}${NC}"
    echo -e "Total Screens:     ${GREEN}${OVERALL_METRICS[total_screens]}${NC}"
    echo -e "Elements Found:    ${GREEN}${OVERALL_METRICS[total_elements]}${NC}"
    echo -e "VUIDs Created:     ${GREEN}${OVERALL_METRICS[total_vuids]}${NC}"
    echo -e "Commands Gen:      ${GREEN}${OVERALL_METRICS[total_commands]}${NC}"

    if [[ ${OVERALL_METRICS[total_errors]} -gt 0 ]]; then
        echo -e "Errors:            ${RED}${OVERALL_METRICS[total_errors]}${NC}"
    else
        echo -e "Errors:            ${GREEN}0${NC}"
    fi

    echo -e "Total Duration:    ${BLUE}${duration_sec}s${NC}"
    echo ""
    echo -e "${CYAN}Output Files:${NC}"
    echo -e "  JSON:  $JSON_FILE"
    echo -e "  Log:   $LOG_FILE"
    echo ""

    # Quick JSON preview
    if command -v jq &> /dev/null; then
        echo -e "${CYAN}JSON Preview (summary):${NC}"
        jq '.summary' "$JSON_FILE" 2>/dev/null || true
    fi
}

# ==============================================================================
# ARGUMENT PARSING
# ==============================================================================

parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --device|-d)
                DEVICE="$2"
                shift 2
                ;;
            --output|-o)
                OUTPUT_DIR="$2"
                shift 2
                ;;
            --apps|-a)
                IFS=',' read -ra custom_apps <<< "$2"
                for app in "${custom_apps[@]}"; do
                    APPS+=("$app|android.intent.action.MAIN|${app##*.}")
                done
                shift 2
                ;;
            --verbose|-v)
                VERBOSE=1
                shift
                ;;
            --help|-h)
                echo "Usage: $0 [OPTIONS]"
                echo ""
                echo "Options:"
                echo "  -d, --device DEVICE    ADB device ID (default: emulator-5554)"
                echo "  -o, --output DIR       Output directory for JSON/logs (default: /tmp)"
                echo "  -a, --apps APPS        Comma-separated list of package names"
                echo "  -v, --verbose          Enable verbose logging"
                echo "  -h, --help             Show this help message"
                echo ""
                echo "Example:"
                echo "  $0 --device emulator-5554 --output /tmp --apps com.android.settings,com.android.contacts"
                exit 0
                ;;
            *)
                log ERROR "Unknown option: $1"
                exit 1
                ;;
        esac
    done

    # Use default apps if none specified
    if [[ ${#APPS[@]} -eq 0 ]]; then
        APPS=("${DEFAULT_APPS[@]}")
    fi

    # Update file paths with new output dir
    LOG_FILE="${OUTPUT_DIR}/voiceos-test-${TIMESTAMP}.log"
    JSON_FILE="${OUTPUT_DIR}/voiceos-metrics-${TIMESTAMP}.json"
}

# ==============================================================================
# MAIN EXECUTION
# ==============================================================================

main() {
    parse_args "$@"

    # Create output directory if needed
    mkdir -p "$OUTPUT_DIR"

    # Initialize log file
    echo "VoiceOS Autonomous Test - Started at $(date)" > "$LOG_FILE"
    echo "Device: $DEVICE" >> "$LOG_FILE"
    echo "Apps to test: ${#APPS[@]}" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"

    print_header

    # Ensure VoiceOS is running
    ensure_voiceos_running

    # Clear logs before starting
    clear_logs
    sleep 1

    # Test each app
    local tested_count=0
    for app_info in "${APPS[@]}"; do
        IFS='|' read -r pkg action name <<< "$app_info"

        if launch_and_learn "$pkg" "$action" "$name"; then
            ((tested_count++))
        fi

        # Small delay between apps
        sleep 1
    done

    OVERALL_METRICS[total_apps]=$tested_count

    # Generate reports
    generate_json_report
    print_summary

    log OK "Testing complete!"
}

# Run if called directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
