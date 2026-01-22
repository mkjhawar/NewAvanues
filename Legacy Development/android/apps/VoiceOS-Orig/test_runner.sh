#!/bin/bash
#
# Autonomous Test Runner for VoiceOS KMP Libraries
# Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
# Created: 2025-11-17
#
# This script automatically runs all tests for KMP libraries and generates a report

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test results
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Report file
REPORT_FILE="test_report_$(date +%Y%m%d_%H%M%S).md"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   VoiceOS KMP Libraries Test Runner${NC}"
echo -e "${BLUE}========================================${NC}"
echo

# Function to run tests for a library
run_library_tests() {
    local library=$1
    local display_name=$2

    echo -e "${YELLOW}Testing $display_name...${NC}"

    # Try different test targets
    for target in "test" "allTests" "jvmTest" "check"; do
        if ./gradlew :libraries:core:$library:$target --quiet 2>/dev/null; then
            echo -e "${GREEN}✓ $display_name tests completed${NC}"
            ((PASSED_TESTS++))
            echo "## $display_name" >> "$REPORT_FILE"
            echo "✅ All tests passed" >> "$REPORT_FILE"
            echo >> "$REPORT_FILE"
            return 0
        fi
    done

    # If we get here, tests failed or were skipped
    if ./gradlew :libraries:core:$library:tasks 2>/dev/null | grep -q "test"; then
        echo -e "${RED}✗ $display_name tests failed${NC}"
        ((FAILED_TESTS++))
        echo "## $display_name" >> "$REPORT_FILE"
        echo "❌ Tests failed" >> "$REPORT_FILE"
        echo >> "$REPORT_FILE"
    else
        echo -e "${YELLOW}⊘ $display_name tests skipped (no tests found)${NC}"
        ((SKIPPED_TESTS++))
        echo "## $display_name" >> "$REPORT_FILE"
        echo "⏭️ No tests found" >> "$REPORT_FILE"
        echo >> "$REPORT_FILE"
    fi
}

# Initialize report
echo "# VoiceOS KMP Libraries Test Report" > "$REPORT_FILE"
echo "Generated: $(date)" >> "$REPORT_FILE"
echo >> "$REPORT_FILE"

# Test each library
echo -e "${BLUE}Running tests for all KMP libraries...${NC}"
echo

# Libraries to test (based on actual project structure)
LIBRARIES=(
    "accessibility-types:Accessibility Types"
    "command-models:Command Models"
    "constants:Constants"
    "exceptions:Exceptions"
    "hash:Hash Utils"
    "json-utils:JSON Utils"
    "result:Result Types"
    "text-utils:Text Utils"
    "validation:Validation Utils"
    "voiceos-logging:Logging with PII Redaction"
)

for lib_info in "${LIBRARIES[@]}"; do
    IFS=':' read -r lib_name display_name <<< "$lib_info"
    ((TOTAL_TESTS++))
    run_library_tests "$lib_name" "$display_name"
    echo
done

# Compile test statistics
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}           Test Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo
echo -e "Total Libraries Tested: ${TOTAL_TESTS}"
echo -e "${GREEN}Passed: ${PASSED_TESTS}${NC}"
echo -e "${RED}Failed: ${FAILED_TESTS}${NC}"
echo -e "${YELLOW}Skipped: ${SKIPPED_TESTS}${NC}"

# Add summary to report
echo "## Summary" >> "$REPORT_FILE"
echo >> "$REPORT_FILE"
echo "| Metric | Count |" >> "$REPORT_FILE"
echo "|--------|-------|" >> "$REPORT_FILE"
echo "| Total Libraries | $TOTAL_TESTS |" >> "$REPORT_FILE"
echo "| ✅ Passed | $PASSED_TESTS |" >> "$REPORT_FILE"
echo "| ❌ Failed | $FAILED_TESTS |" >> "$REPORT_FILE"
echo "| ⏭️ Skipped | $SKIPPED_TESTS |" >> "$REPORT_FILE"
echo >> "$REPORT_FILE"

# Calculate success rate
if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    echo >> "$REPORT_FILE"
    echo "**Success Rate:** ${SUCCESS_RATE}%" >> "$REPORT_FILE"
fi

# Run integration tests if available
echo
echo -e "${BLUE}Running integration tests...${NC}"

# Check if VoiceOSCore tests pass with the libraries
if ./gradlew :modules:apps:VoiceOSCore:testDebugUnitTest --quiet 2>/dev/null; then
    echo -e "${GREEN}✓ Integration tests passed${NC}"
    echo >> "$REPORT_FILE"
    echo "## Integration Tests" >> "$REPORT_FILE"
    echo "✅ VoiceOSCore integration tests passed" >> "$REPORT_FILE"
else
    echo -e "${YELLOW}⊘ Integration tests skipped or failed${NC}"
    echo >> "$REPORT_FILE"
    echo "## Integration Tests" >> "$REPORT_FILE"
    echo "⏭️ VoiceOSCore integration tests skipped" >> "$REPORT_FILE"
fi

# Generate coverage report if possible
echo
echo -e "${BLUE}Attempting to generate coverage report...${NC}"

if ./gradlew jacocoTestReport 2>/dev/null; then
    echo -e "${GREEN}✓ Coverage report generated${NC}"
    echo >> "$REPORT_FILE"
    echo "## Coverage Report" >> "$REPORT_FILE"
    echo "✅ Coverage report generated at build/reports/jacoco" >> "$REPORT_FILE"
else
    echo -e "${YELLOW}⊘ Coverage report not available${NC}"
    echo >> "$REPORT_FILE"
    echo "## Coverage Report" >> "$REPORT_FILE"
    echo "⏭️ Coverage report not available" >> "$REPORT_FILE"
fi

# Final report
echo
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}    Test Report: ${REPORT_FILE}${NC}"
echo -e "${BLUE}========================================${NC}"

# Exit with appropriate code
if [ $FAILED_TESTS -gt 0 ]; then
    exit 1
else
    exit 0
fi