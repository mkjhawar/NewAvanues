#!/bin/bash
#
# Agent 5: Lifecycle Test Runner
# Run this script after Agent 1 fixes the build system
#

set -e

echo "================================================================================"
echo "AGENT 5: LIFECYCLE TEST RUNNER"
echo "================================================================================"
echo ""
echo "Prerequisites:"
echo "  ✅ Agent 1 must have fixed KSP/build issues"
echo "  ✅ Build system must be working"
echo ""
echo "Press Enter to continue or Ctrl+C to abort..."
read

echo ""
echo "Step 1: Verify build system is working..."
echo "Running: ./gradlew :app:assembleDebug --dry-run"
./gradlew :app:assembleDebug --dry-run 2>&1 | tail -5
echo ""

echo "Step 2: Clean build..."
./gradlew clean
echo ""

echo "Step 3: Compile main sources..."
./gradlew :modules:apps:VoiceOSCore:compileDebugKotlin
echo ""

echo "Step 4: Compile test sources..."
./gradlew :modules:apps:VoiceOSCore:compileDebugUnitTestKotlin
echo ""

echo "Step 5: Run lifecycle tests..."
echo "Running: ./gradlew :modules:apps:VoiceOSCore:testDebugUnitTest --tests \"*.lifecycle.*\""
./gradlew :modules:apps:VoiceOSCore:testDebugUnitTest --tests "*.lifecycle.*" 2>&1 | tee /tmp/agent5-lifecycle-test-results.txt
echo ""

echo "================================================================================"
echo "TEST RESULTS"
echo "================================================================================"
echo ""
grep -E "tests completed|tests passed|tests failed" /tmp/agent5-lifecycle-test-results.txt || echo "Check /tmp/agent5-lifecycle-test-results.txt for full results"
echo ""
echo "Expected: 51 tests passed (15 + 10 + 11 + 15)"
echo ""
echo "================================================================================"
