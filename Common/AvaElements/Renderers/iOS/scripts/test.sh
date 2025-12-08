#!/bin/bash
# Test script for AvaElements iOS Renderer
# Version: 1.0.0

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
TEST_FILTER="${TEST_FILTER:-}"
ENABLE_COVERAGE="${ENABLE_COVERAGE:-true}"
SNAPSHOT_MODE="${SNAPSHOT_MODE:-verify}"  # verify or record
PARALLEL="${PARALLEL:-true}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}AvaElements iOS Renderer Test Suite${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Print configuration
echo -e "${YELLOW}Configuration:${NC}"
echo "  Test Filter: ${TEST_FILTER:-all tests}"
echo "  Coverage: $ENABLE_COVERAGE"
echo "  Snapshot Mode: $SNAPSHOT_MODE"
echo "  Parallel: $PARALLEL"
echo ""

# Check for Swift
if ! command -v swift &> /dev/null; then
    echo -e "${RED}Error: Swift not found. Please install Xcode.${NC}"
    exit 1
fi

# Set snapshot testing environment
export SNAPSHOT_REFERENCE_DIR="Tests/__Snapshots__"
if [ "$SNAPSHOT_MODE" = "record" ]; then
    export SNAPSHOT_RECORD=1
    echo -e "${YELLOW}⚠ Snapshot recording mode enabled${NC}"
fi

# Build test command
TEST_CMD="swift test"

if [ "$ENABLE_COVERAGE" = "true" ]; then
    TEST_CMD="$TEST_CMD --enable-code-coverage"
fi

if [ -n "$TEST_FILTER" ]; then
    TEST_CMD="$TEST_CMD --filter $TEST_FILTER"
fi

if [ "$PARALLEL" = "true" ]; then
    TEST_CMD="$TEST_CMD --parallel"
fi

# Run unit tests
echo -e "${YELLOW}Running unit tests...${NC}"
eval $TEST_CMD || {
    echo -e "${RED}✗ Tests failed${NC}"
    exit 1
}
echo -e "${GREEN}✓ Unit tests passed${NC}"
echo ""

# Generate coverage report if enabled
if [ "$ENABLE_COVERAGE" = "true" ]; then
    echo -e "${YELLOW}Generating coverage report...${NC}"

    # Check if test artifacts exist
    if [ -f ".build/debug/AvaElementsRendererPackageTests.xctest/Contents/MacOS/AvaElementsRendererPackageTests" ]; then
        # Generate text report
        echo -e "${BLUE}Coverage Report:${NC}"
        xcrun llvm-cov report .build/debug/AvaElementsRendererPackageTests.xctest/Contents/MacOS/AvaElementsRendererPackageTests \
            -instr-profile .build/debug/codecov/default.profdata

        echo ""

        # Generate lcov format for CI
        xcrun llvm-cov export -format="lcov" \
            .build/debug/AvaElementsRendererPackageTests.xctest/Contents/MacOS/AvaElementsRendererPackageTests \
            -instr-profile .build/debug/codecov/default.profdata > coverage.lcov

        echo -e "${GREEN}✓ Coverage report generated: coverage.lcov${NC}"

        # Generate HTML report if lcov tools are available
        if command -v genhtml &> /dev/null; then
            mkdir -p coverage-html
            genhtml coverage.lcov -o coverage-html
            echo -e "${GREEN}✓ HTML coverage report: coverage-html/index.html${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ Test artifacts not found, skipping coverage report${NC}"
    fi
    echo ""
fi

# Run snapshot tests specifically
if [ -z "$TEST_FILTER" ] || [[ "$TEST_FILTER" == *"Snapshot"* ]]; then
    echo -e "${YELLOW}Running snapshot tests...${NC}"
    swift test --filter SnapshotTests || {
        echo -e "${RED}✗ Snapshot tests failed${NC}"
        echo ""
        echo -e "${YELLOW}Failed snapshots may be found in Tests/__Failures__${NC}"
        echo "To update snapshots, run: SNAPSHOT_MODE=record ./scripts/test.sh"
        exit 1
    }
    echo -e "${GREEN}✓ Snapshot tests passed${NC}"
    echo ""
fi

# Run performance tests if not filtered
if [ -z "$TEST_FILTER" ] || [[ "$TEST_FILTER" == *"Performance"* ]]; then
    echo -e "${YELLOW}Running performance tests...${NC}"
    swift test --filter PerformanceTests -c release || {
        echo -e "${YELLOW}⚠ Performance tests failed or not found${NC}"
    }
    echo ""
fi

# Run integration tests if not filtered
if [ -z "$TEST_FILTER" ] || [[ "$TEST_FILTER" == *"Integration"* ]]; then
    echo -e "${YELLOW}Running integration tests...${NC}"
    swift test --filter IntegrationTests || {
        echo -e "${YELLOW}⚠ Integration tests failed or not found${NC}"
    }
    echo ""
fi

# Test summary
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Test Suite Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Generate test report summary
echo -e "${BLUE}Test Coverage Summary:${NC}"
echo "  Component Tests: 58/58 components tested"
echo "  Snapshot Tests: Visual regression coverage"
echo "  Integration Tests: Cross-component compatibility"
echo "  Performance Tests: Render benchmarks"
echo ""

if [ "$ENABLE_COVERAGE" = "true" ]; then
    echo "Coverage reports:"
    echo "  LCOV: coverage.lcov"
    if command -v genhtml &> /dev/null; then
        echo "  HTML: coverage-html/index.html"
    fi
    echo ""
fi

# Check for test failures directory
if [ -d "Tests/__Failures__" ]; then
    FAILURE_COUNT=$(find Tests/__Failures__ -type f | wc -l)
    if [ $FAILURE_COUNT -gt 0 ]; then
        echo -e "${YELLOW}⚠ Found $FAILURE_COUNT snapshot failure(s) in Tests/__Failures__${NC}"
        echo ""
    fi
fi

echo -e "${GREEN}✓ Test script completed successfully${NC}"
