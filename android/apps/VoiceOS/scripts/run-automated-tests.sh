#!/bin/bash
# VoiceOS Automated Test Runner
# Usage: ./scripts/run-automated-tests.sh [options]
#
# Options:
#   --unit          Run unit tests only (default)
#   --instrumented  Run instrumented tests on connected device/emulator
#   --all           Run all tests
#   --coverage      Generate coverage report
#   --help          Show this help message

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
RUN_UNIT=true
RUN_INSTRUMENTED=false
RUN_COVERAGE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --unit)
            RUN_UNIT=true
            RUN_INSTRUMENTED=false
            shift
            ;;
        --instrumented)
            RUN_UNIT=false
            RUN_INSTRUMENTED=true
            shift
            ;;
        --all)
            RUN_UNIT=true
            RUN_INSTRUMENTED=true
            shift
            ;;
        --coverage)
            RUN_COVERAGE=true
            shift
            ;;
        --help)
            cat << EOF
VoiceOS Automated Test Runner

Usage: ./scripts/run-automated-tests.sh [options]

Options:
  --unit          Run unit tests only (default)
  --instrumented  Run instrumented tests on connected device/emulator
  --all           Run all tests (unit + instrumented)
  --coverage      Generate coverage report
  --help          Show this help message

Examples:
  ./scripts/run-automated-tests.sh                  # Run unit tests
  ./scripts/run-automated-tests.sh --instrumented   # Run on emulator
  ./scripts/run-automated-tests.sh --all            # Run everything
  ./scripts/run-automated-tests.sh --coverage       # Run with coverage

EOF
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  VoiceOS Automated Test Suite${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run test and track results
run_test() {
    local test_name=$1
    local test_command=$2

    echo -e "${YELLOW}Running: $test_name${NC}"

    if eval $test_command > /tmp/voiceos_test_output.log 2>&1; then
        echo -e "${GREEN}✓ $test_name PASSED${NC}"
        ((PASSED_TESTS++))

        # Show test summary
        if grep -q "BUILD SUCCESSFUL" /tmp/voiceos_test_output.log; then
            TEST_COUNT=$(grep -oE "[0-9]+ tests completed" /tmp/voiceos_test_output.log | grep -oE "[0-9]+" | head -1)
            if [ -n "$TEST_COUNT" ]; then
                echo -e "  ${BLUE}→ $TEST_COUNT tests executed${NC}"
            fi
        fi
    else
        echo -e "${RED}✗ $test_name FAILED${NC}"
        ((FAILED_TESTS++))

        # Show error summary
        echo -e "${RED}Error output:${NC}"
        grep -E "(FAILED|error:|e: )" /tmp/voiceos_test_output.log | head -20
    fi

    ((TOTAL_TESTS++))
    echo ""
}

# 1. Unit Tests
if [ "$RUN_UNIT" = true ]; then
    echo -e "${BLUE}=== Unit Tests ===${NC}"
    echo ""

    run_test "Automated Tests (Database + Voice Commands)" \
        "./gradlew :tests:automated-tests:test"
fi

# 2. Instrumented Tests (if requested)
if [ "$RUN_INSTRUMENTED" = true ]; then
    echo -e "${BLUE}=== Instrumented Tests (Emulator/Device) ===${NC}"
    echo ""

    # Check if device/emulator is connected
    if ! adb devices | grep -q "device$"; then
        echo -e "${RED}✗ No device/emulator connected${NC}"
        echo -e "${YELLOW}  Please start an emulator or connect a device${NC}"
        echo -e "${YELLOW}  Use: emulator -avd <avd_name>${NC}"
        exit 1
    fi

    echo -e "${GREEN}✓ Device/Emulator connected${NC}"
    echo ""

    run_test "Instrumented Database Tests" \
        "./gradlew :tests:automated-tests:connectedAndroidTest"
fi

# 3. Coverage Report (if requested)
if [ "$RUN_COVERAGE" = true ]; then
    echo -e "${BLUE}=== Generating Coverage Report ===${NC}"
    echo ""

    run_test "Coverage Report Generation" \
        "./gradlew :tests:automated-tests:jacocoTestReport"

    COVERAGE_REPORT="tests/automated-tests/build/reports/jacoco/test/html/index.html"
    if [ -f "$COVERAGE_REPORT" ]; then
        echo -e "${GREEN}✓ Coverage report generated${NC}"
        echo -e "  ${BLUE}→ Open: $COVERAGE_REPORT${NC}"
    fi
fi

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Test Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "Total Test Suites: $TOTAL_TESTS"
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
echo -e "${RED}Failed: $FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 All tests passed!${NC}"
    echo ""
    echo -e "${BLUE}Test Details:${NC}"
    echo -e "  - Database tests: 18 test cases"
    echo -e "  - Voice command tests: 29 test cases"
    echo -e "  - Total: 47 automated test cases"
    exit 0
else
    echo -e "${RED}❌ Some tests failed${NC}"
    echo -e "${YELLOW}Review the error output above for details${NC}"
    exit 1
fi
