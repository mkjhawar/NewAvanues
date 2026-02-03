#!/bin/bash

# AvaElements Web Renderer - Test Script
# Runs all tests with coverage analysis

set -e

echo "================================================"
echo "AvaElements Web Renderer - Test Script"
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo ""
echo "Step 1: Running unit tests..."
npm run test -- --coverage --maxWorkers=50%
echo -e "${GREEN}✓ Unit tests passed${NC}"

echo ""
echo "Step 2: Running accessibility tests..."
npm run test:a11y
echo -e "${GREEN}✓ Accessibility tests passed${NC}"

echo ""
echo "Step 3: Analyzing coverage..."
if [ -f "coverage/lcov.info" ]; then
  # Extract coverage percentages
  STATEMENTS=$(grep -A 1 "Statements" coverage/lcov-report/index.html | grep -oP '\d+\.\d+' | head -1 || echo "N/A")
  BRANCHES=$(grep -A 1 "Branches" coverage/lcov-report/index.html | grep -oP '\d+\.\d+' | head -1 || echo "N/A")
  FUNCTIONS=$(grep -A 1 "Functions" coverage/lcov-report/index.html | grep -oP '\d+\.\d+' | head -1 || echo "N/A")
  LINES=$(grep -A 1 "Lines" coverage/lcov-report/index.html | grep -oP '\d+\.\d+' | head -1 || echo "N/A")

  echo ""
  echo "Coverage Summary:"
  echo "  Statements: ${STATEMENTS}%"
  echo "  Branches:   ${BRANCHES}%"
  echo "  Functions:  ${FUNCTIONS}%"
  echo "  Lines:      ${LINES}%"

  # Check if coverage meets threshold (90%)
  if [ "$STATEMENTS" != "N/A" ]; then
    COVERAGE_THRESHOLD=90
    STATEMENTS_INT=${STATEMENTS%.*}

    if [ "$STATEMENTS_INT" -lt "$COVERAGE_THRESHOLD" ]; then
      echo -e "${YELLOW}⚠ Warning: Coverage below ${COVERAGE_THRESHOLD}% threshold${NC}"
    else
      echo -e "${GREEN}✓ Coverage meets ${COVERAGE_THRESHOLD}% threshold${NC}"
    fi
  fi
else
  echo -e "${YELLOW}⚠ Coverage report not found${NC}"
fi

echo ""
echo "================================================"
echo -e "${GREEN}All tests completed successfully!${NC}"
echo "================================================"
echo ""
echo "Coverage report: coverage/lcov-report/index.html"
echo ""
