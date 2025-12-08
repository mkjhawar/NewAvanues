#!/bin/bash

# AvaElements Web Renderer - Build Script
# Performs clean build with type checking and linting

set -e

echo "================================================"
echo "AvaElements Web Renderer - Build Script"
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
echo "Step 1: Clean previous builds..."
rm -rf dist coverage .tsbuildinfo
echo -e "${GREEN}✓ Cleaned${NC}"

echo ""
echo "Step 2: Type checking..."
npm run type-check
echo -e "${GREEN}✓ Type check passed${NC}"

echo ""
echo "Step 3: Linting..."
npm run lint
echo -e "${GREEN}✓ Lint check passed${NC}"

echo ""
echo "Step 4: Building library..."
npm run build
echo -e "${GREEN}✓ Build completed${NC}"

echo ""
echo "Step 5: Generating type declarations..."
tsc --emitDeclarationOnly --outDir dist
echo -e "${GREEN}✓ Type declarations generated${NC}"

echo ""
echo "Step 6: Analyzing bundle size..."
DIST_SIZE=$(du -sh dist | cut -f1)
echo "Total bundle size: ${DIST_SIZE}"

# Check if bundle is too large (warning threshold)
DIST_SIZE_KB=$(du -sk dist | cut -f1)
if [ $DIST_SIZE_KB -gt 200 ]; then
  echo -e "${YELLOW}⚠ Warning: Bundle size exceeds 200KB recommendation${NC}"
else
  echo -e "${GREEN}✓ Bundle size within limits${NC}"
fi

echo ""
echo "================================================"
echo -e "${GREEN}Build completed successfully!${NC}"
echo "================================================"
echo ""
echo "Outputs:"
echo "  - ESM: dist/index.js"
echo "  - CJS: dist/index.cjs"
echo "  - Types: dist/index.d.ts"
echo ""
