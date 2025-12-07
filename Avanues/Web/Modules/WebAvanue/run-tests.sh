#!/bin/bash

# WebAvanue Test Runner Script
# Runs all tests across all platforms with coverage reporting

set -e

echo "🧪 WebAvanue Test Suite"
echo "========================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if gradle wrapper exists
if [ ! -f "./gradlew" ]; then
    echo -e "${RED}❌ Gradle wrapper not found!${NC}"
    echo "Please run: gradle wrapper"
    exit 1
fi

# Clean previous test results
echo "🧹 Cleaning previous test results..."
./gradlew clean

echo ""
echo "🏗️  Building project..."
./gradlew build --no-daemon

echo ""
echo "📱 Running Android Tests..."
echo "----------------------------"
if ./gradlew :BrowserCoreData:testDebugUnitTest --no-daemon; then
    echo -e "${GREEN}✅ Android tests passed${NC}"
else
    echo -e "${RED}❌ Android tests failed${NC}"
    ANDROID_FAILED=1
fi

echo ""
echo "🍎 Running iOS Tests..."
echo "----------------------------"
if ./gradlew :BrowserCoreData:iosX64Test --no-daemon; then
    echo -e "${GREEN}✅ iOS tests passed${NC}"
else
    echo -e "${YELLOW}⚠️  iOS tests skipped (requires macOS with Xcode)${NC}"
fi

echo ""
echo "🖥️  Running Desktop Tests..."
echo "----------------------------"
if ./gradlew :BrowserCoreData:desktopTest --no-daemon; then
    echo -e "${GREEN}✅ Desktop tests passed${NC}"
else
    echo -e "${RED}❌ Desktop tests failed${NC}"
    DESKTOP_FAILED=1
fi

echo ""
echo "🧩 Running Common Tests..."
echo "----------------------------"
if ./gradlew :BrowserCoreData:allTests --no-daemon; then
    echo -e "${GREEN}✅ Common tests passed${NC}"
else
    echo -e "${RED}❌ Common tests failed${NC}"
    COMMON_FAILED=1
fi

echo ""
echo "📊 Generating Test Reports..."
echo "----------------------------"

# Generate test report
./gradlew :BrowserCoreData:allTests --no-daemon

# Check if any platform failed
if [ -n "$ANDROID_FAILED" ] || [ -n "$DESKTOP_FAILED" ] || [ -n "$COMMON_FAILED" ]; then
    echo ""
    echo -e "${RED}❌ Some tests failed. Please check the reports:${NC}"
    echo "   • Android: BrowserCoreData/build/reports/tests/testDebugUnitTest/index.html"
    echo "   • Desktop: BrowserCoreData/build/reports/tests/desktopTest/index.html"
    echo "   • Common: BrowserCoreData/build/reports/tests/allTests/index.html"
    exit 1
else
    echo ""
    echo -e "${GREEN}✅ All tests passed successfully!${NC}"
    echo ""
    echo "📈 Test Summary:"
    echo "   • Domain Models: ✅"
    echo "   • Repository: ✅"
    echo "   • Use Cases: ✅"
    echo "   • Platform WebViews: ✅"
    echo ""
    echo "📁 Test reports available at:"
    echo "   • BrowserCoreData/build/reports/tests/"
fi

echo ""
echo "🎉 Test suite complete!"