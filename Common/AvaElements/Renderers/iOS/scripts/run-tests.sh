#!/bin/bash
# iOS Test Runner Script
# Runs all tests with coverage and generates reports

set -e

echo "========================================="
echo "iOS SwiftUI Renderer - Test Runner"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$PROJECT_DIR/.build"
COVERAGE_DIR="$BUILD_DIR/coverage"
REPORTS_DIR="$PROJECT_DIR/TestReports"

echo "Project directory: $PROJECT_DIR"
echo ""

# Clean previous build
echo -e "${YELLOW}Cleaning previous build...${NC}"
rm -rf "$BUILD_DIR"
rm -rf "$REPORTS_DIR"
mkdir -p "$REPORTS_DIR"

# Resolve dependencies
echo -e "${YELLOW}Resolving dependencies...${NC}"
swift package resolve
echo ""

# Build tests
echo -e "${YELLOW}Building tests...${NC}"
swift build --build-tests
echo ""

# Run unit tests
echo -e "${YELLOW}Running unit tests...${NC}"
swift test \
  --parallel \
  --enable-code-coverage \
  --filter SwiftUIRendererTests \
  | tee "$REPORTS_DIR/unit-tests.log"

UNIT_TEST_EXIT_CODE=$?
echo ""

# Run snapshot tests
echo -e "${YELLOW}Running snapshot tests...${NC}"
swift test \
  --parallel \
  --enable-code-coverage \
  --filter SnapshotTests \
  | tee "$REPORTS_DIR/snapshot-tests.log"

SNAPSHOT_TEST_EXIT_CODE=$?
echo ""

# Run performance tests
echo -e "${YELLOW}Running performance tests...${NC}"
swift test \
  --enable-code-coverage \
  --filter PerformanceTests \
  | tee "$REPORTS_DIR/performance-tests.log"

PERF_TEST_EXIT_CODE=$?
echo ""

# Generate coverage report
echo -e "${YELLOW}Generating coverage report...${NC}"
mkdir -p "$COVERAGE_DIR"

xcrun llvm-cov export \
  -format="lcov" \
  "$BUILD_DIR/debug/AvaElementsRendererPackageTests.xctest/Contents/MacOS/AvaElementsRendererPackageTests" \
  -instr-profile "$BUILD_DIR/debug/codecov/default.profdata" \
  > "$COVERAGE_DIR/coverage.lcov"

xcrun llvm-cov report \
  "$BUILD_DIR/debug/AvaElementsRendererPackageTests.xctest/Contents/MacOS/AvaElementsRendererPackageTests" \
  -instr-profile "$BUILD_DIR/debug/codecov/default.profdata" \
  > "$REPORTS_DIR/coverage-report.txt"

# Extract coverage percentage
COVERAGE=$(xcrun llvm-cov report \
  "$BUILD_DIR/debug/AvaElementsRendererPackageTests.xctest/Contents/MacOS/AvaElementsRendererPackageTests" \
  -instr-profile "$BUILD_DIR/debug/codecov/default.profdata" \
  | grep TOTAL | awk '{print $NF}' | sed 's/%//')

echo ""
echo -e "${GREEN}Coverage: ${COVERAGE}%${NC}"
echo ""

# Count tests
TOTAL_TESTS=$(cat "$REPORTS_DIR"/*.log | grep -c "Test Case.*passed\|failed" || echo "0")
PASSED_TESTS=$(cat "$REPORTS_DIR"/*.log | grep -c "Test Case.*passed" || echo "0")
FAILED_TESTS=$(cat "$REPORTS_DIR"/*.log | grep -c "Test Case.*failed" || echo "0")

# Generate summary report
echo "=========================================" > "$REPORTS_DIR/SUMMARY.md"
echo "iOS Test Summary Report" >> "$REPORTS_DIR/SUMMARY.md"
echo "=========================================" >> "$REPORTS_DIR/SUMMARY.md"
echo "" >> "$REPORTS_DIR/SUMMARY.md"
echo "## Test Results" >> "$REPORTS_DIR/SUMMARY.md"
echo "" >> "$REPORTS_DIR/SUMMARY.md"
echo "- **Total Tests**: $TOTAL_TESTS" >> "$REPORTS_DIR/SUMMARY.md"
echo "- **Passed**: $PASSED_TESTS" >> "$REPORTS_DIR/SUMMARY.md"
echo "- **Failed**: $FAILED_TESTS" >> "$REPORTS_DIR/SUMMARY.md"
echo "- **Coverage**: ${COVERAGE}%" >> "$REPORTS_DIR/SUMMARY.md"
echo "" >> "$REPORTS_DIR/SUMMARY.md"
echo "## Exit Codes" >> "$REPORTS_DIR/SUMMARY.md"
echo "" >> "$REPORTS_DIR/SUMMARY.md"
echo "- Unit Tests: $UNIT_TEST_EXIT_CODE" >> "$REPORTS_DIR/SUMMARY.md"
echo "- Snapshot Tests: $SNAPSHOT_TEST_EXIT_CODE" >> "$REPORTS_DIR/SUMMARY.md"
echo "- Performance Tests: $PERF_TEST_EXIT_CODE" >> "$REPORTS_DIR/SUMMARY.md"
echo "" >> "$REPORTS_DIR/SUMMARY.md"

# Quality gates
echo "========================================="
echo "Quality Gates"
echo "========================================="
echo ""

QUALITY_PASS=true

# Check coverage threshold (90%)
if (( $(echo "$COVERAGE < 90" | bc -l) )); then
  echo -e "${RED}❌ Coverage ${COVERAGE}% is below 90% threshold${NC}"
  QUALITY_PASS=false
else
  echo -e "${GREEN}✅ Coverage ${COVERAGE}% meets 90% threshold${NC}"
fi

# Check test count (400+ expected)
if [ $TOTAL_TESTS -lt 400 ]; then
  echo -e "${RED}❌ Test count $TOTAL_TESTS is below 400 minimum${NC}"
  QUALITY_PASS=false
else
  echo -e "${GREEN}✅ Test count $TOTAL_TESTS meets 400 minimum${NC}"
fi

# Check for test failures
if [ $FAILED_TESTS -gt 0 ]; then
  echo -e "${RED}❌ $FAILED_TESTS test(s) failed${NC}"
  QUALITY_PASS=false
else
  echo -e "${GREEN}✅ All tests passed${NC}"
fi

echo ""

# Final result
if [ "$QUALITY_PASS" = true ]; then
  echo -e "${GREEN}=========================================${NC}"
  echo -e "${GREEN}✅ All quality gates passed!${NC}"
  echo -e "${GREEN}=========================================${NC}"
  echo ""
  echo "Reports saved to: $REPORTS_DIR"
  exit 0
else
  echo -e "${RED}=========================================${NC}"
  echo -e "${RED}❌ Quality gates failed${NC}"
  echo -e "${RED}=========================================${NC}"
  echo ""
  echo "Reports saved to: $REPORTS_DIR"
  exit 1
fi
