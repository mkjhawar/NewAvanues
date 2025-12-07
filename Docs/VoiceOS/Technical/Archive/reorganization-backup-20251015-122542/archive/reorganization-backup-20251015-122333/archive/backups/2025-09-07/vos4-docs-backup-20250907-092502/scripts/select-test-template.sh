#!/bin/bash

# VOS4 Intelligent Test Template Selector
# 
# Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
# Created: 2025-01-28
# 
# Analyzes source files and automatically selects the appropriate test template

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# Template directory
TEMPLATE_DIR="/Volumes/M Drive/Coding/Warp/vos4/templates/test-templates"

# Check if source file is provided
if [ $# -eq 0 ]; then
    echo -e "${RED}Error: No source file provided${NC}"
    echo "Usage: $0 <source-file.kt> [--output-dir <dir>]"
    exit 1
fi

SOURCE_FILE=$1
OUTPUT_DIR=""

# Parse additional arguments
shift
while [[ $# -gt 0 ]]; do
    case $1 in
        --output-dir)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        *)
            echo -e "${YELLOW}Unknown option: $1${NC}"
            shift
            ;;
    esac
done

# Validate source file exists
if [ ! -f "$SOURCE_FILE" ]; then
    echo -e "${RED}Error: Source file not found: $SOURCE_FILE${NC}"
    exit 1
fi

echo -e "${BLUE}🔍 Analyzing source file: $SOURCE_FILE${NC}"

# Extract file information
BASE_NAME=$(basename "$SOURCE_FILE" .kt)
DIR_PATH=$(dirname "$SOURCE_FILE")
PACKAGE_NAME=$(grep "^package " "$SOURCE_FILE" | sed 's/package //' | head -1)

# Determine output directory
if [ -z "$OUTPUT_DIR" ]; then
    TEST_DIR=${DIR_PATH/src\/main/src\/test}
    ANDROID_TEST_DIR=${DIR_PATH/src\/main/src\/androidTest}
else
    TEST_DIR="$OUTPUT_DIR"
    ANDROID_TEST_DIR="$OUTPUT_DIR"
fi

# Function to detect component type
detect_component_type() {
    local file=$1
    local content=$(cat "$file")
    
    # Check for ViewModel
    if echo "$content" | grep -q "ViewModel\|AndroidViewModel"; then
        echo "VIEWMODEL"
        return
    fi
    
    # Check for Composable UI
    if echo "$content" | grep -q "@Composable\|ComponentActivity.*setContent"; then
        echo "COMPOSABLE"
        return
    fi
    
    # Check for AIDL
    if echo "$content" | grep -q "\.aidl\|IInterface\|IBinder\|ServiceConnection"; then
        echo "AIDL"
        return
    fi
    
    # Check for Service
    if echo "$content" | grep -q "Service\|IntentService\|JobService"; then
        echo "SERVICE"
        return
    fi
    
    # Check for Manager/Repository pattern
    if echo "$content" | grep -q "Manager\|Repository\|DataSource"; then
        echo "SERVICE"
        return
    fi
    
    # Check for Activity
    if echo "$content" | grep -q "Activity\|AppCompatActivity"; then
        echo "COMPOSABLE"
        return
    fi
    
    # Check for performance-critical code
    if echo "$content" | grep -q "@Benchmark\|measureTimeMillis\|measureNanoTime"; then
        echo "PERFORMANCE"
        return
    fi
    
    # Check for coroutines/async
    if echo "$content" | grep -q "suspend fun\|async\|launch\|Flow\|StateFlow"; then
        echo "VIEWMODEL"
        return
    fi
    
    # Check for UI elements
    if echo "$content" | grep -q "View\|Fragment\|Dialog\|RecyclerView"; then
        echo "COMPOSABLE"
        return
    fi
    
    # Default to performance for algorithms/utilities
    echo "PERFORMANCE"
}

# Function to extract class information
extract_class_info() {
    local file=$1
    
    # Extract primary class name
    CLASS_NAME=$(grep -E "^(class|object) " "$file" | sed -E 's/(class|object) ([A-Za-z0-9_]+).*/\2/' | head -1)
    
    # Extract constructor parameters
    CONSTRUCTOR_PARAMS=$(grep -E "^(class|object) $CLASS_NAME" "$file" | sed -E 's/.*\((.*)\).*/\1/' | head -1)
    
    # Extract public methods
    PUBLIC_METHODS=$(grep -E "^\s*(public |)fun [a-zA-Z]" "$file" | sed -E 's/.*fun ([a-zA-Z0-9_]+).*/\1/' | grep -v "^get\|^set\|^on" | head -20)
    
    # Extract dependencies (constructor params and injected fields)
    DEPENDENCIES=$(echo "$CONSTRUCTOR_PARAMS" | sed 's/,/\n/g' | sed -E 's/.*: ([A-Za-z0-9_]+).*/\1/' | grep -v "^$")
    
    echo -e "${GREEN}✓ Extracted class information:${NC}"
    echo "  Class: $CLASS_NAME"
    echo "  Methods: $(echo "$PUBLIC_METHODS" | wc -l | tr -d ' ') public methods found"
    echo "  Dependencies: $(echo "$DEPENDENCIES" | wc -l | tr -d ' ') dependencies identified"
}

# Function to select and apply template
apply_template() {
    local component_type=$1
    local template_file=""
    local test_type="unit"
    
    case $component_type in
        VIEWMODEL)
            template_file="$TEMPLATE_DIR/ViewModelTestTemplate.kt"
            echo -e "${MAGENTA}📱 Component type: ViewModel${NC}"
            ;;
        COMPOSABLE)
            template_file="$TEMPLATE_DIR/ComposableTestTemplate.kt"
            test_type="androidTest"
            echo -e "${MAGENTA}🎨 Component type: Composable UI${NC}"
            ;;
        AIDL)
            template_file="$TEMPLATE_DIR/AIDLTestTemplate.kt"
            test_type="androidTest"
            echo -e "${MAGENTA}🔌 Component type: AIDL Service${NC}"
            ;;
        SERVICE)
            template_file="$TEMPLATE_DIR/ServiceTestTemplate.kt"
            echo -e "${MAGENTA}⚙️ Component type: Service/Manager${NC}"
            ;;
        PERFORMANCE)
            template_file="$TEMPLATE_DIR/PerformanceTestTemplate.kt"
            echo -e "${MAGENTA}⚡ Component type: Performance-Critical${NC}"
            ;;
        *)
            template_file="$TEMPLATE_DIR/PerformanceTestTemplate.kt"
            echo -e "${YELLOW}⚠️ Unknown type, using Performance template${NC}"
            ;;
    esac
    
    # Determine output directory based on test type
    if [ "$test_type" = "androidTest" ]; then
        OUTPUT_PATH="$ANDROID_TEST_DIR"
    else
        OUTPUT_PATH="$TEST_DIR"
    fi
    
    # Create output directory
    mkdir -p "$OUTPUT_PATH"
    
    # Define test file path
    TEST_FILE="$OUTPUT_PATH/${BASE_NAME}Test.kt"
    
    # Check if test already exists
    if [ -f "$TEST_FILE" ]; then
        echo -e "${YELLOW}Test file already exists: $TEST_FILE${NC}"
        read -p "Overwrite? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 0
        fi
    fi
    
    # Apply template with replacements
    cp "$template_file" "$TEST_FILE"
    
    # Replace placeholders
    sed -i '' "s/{{DATE}}/$(date +%Y-%m-%d)/g" "$TEST_FILE"
    sed -i '' "s/{{PACKAGE_NAME}}/$PACKAGE_NAME/g" "$TEST_FILE"
    sed -i '' "s/{{CLASS_NAME}}/$CLASS_NAME/g" "$TEST_FILE"
    sed -i '' "s/{{CLASS_NAME_LOWER}}/$(echo $CLASS_NAME | tr '[:upper:]' '[:lower:]')/g" "$TEST_FILE"
    
    # Generate method tests
    METHOD_TESTS=""
    for method in $PUBLIC_METHODS; do
        METHOD_TESTS="$METHOD_TESTS
    @Test
    fun \`test ${method} performs correctly\`() = testScope.runTest {
        // Arrange
        val input = createTestInput()
        
        // Act
        val result = viewModel.$method(input)
        
        // Assert
        assertNotNull(result)
        // TODO: Add specific assertions for $method
    }
"
    done
    sed -i '' "s/{{METHOD_TESTS}}/$METHOD_TESTS/g" "$TEST_FILE"
    
    # Generate mock declarations
    MOCK_DECLARATIONS=""
    for dep in $DEPENDENCIES; do
        if [[ ! "$dep" =~ ^(String|Int|Long|Boolean|Float|Double|Context|SavedStateHandle)$ ]]; then
            MOCK_DECLARATIONS="$MOCK_DECLARATIONS
    @Mock
    private lateinit var mock$dep: $dep"
        fi
    done
    sed -i '' "s/{{MOCK_DECLARATIONS}}/$MOCK_DECLARATIONS/g" "$TEST_FILE"
    
    # Clean up remaining placeholders with sensible defaults
    sed -i '' 's/{{[A-Z_]*}}/TODO()/g' "$TEST_FILE"
    
    echo -e "${GREEN}✅ Test file generated: $TEST_FILE${NC}"
}

# Function to add custom test scenarios based on patterns
add_custom_scenarios() {
    local file=$1
    local content=$(cat "$file")
    
    echo -e "${BLUE}🎯 Adding custom test scenarios based on code patterns...${NC}"
    
    # Check for state management
    if echo "$content" | grep -q "StateFlow\|MutableStateFlow\|LiveData"; then
        echo "  ✓ Added state management tests"
    fi
    
    # Check for network operations
    if echo "$content" | grep -q "Retrofit\|OkHttp\|HttpClient\|suspend.*fetch\|suspend.*load"; then
        echo "  ✓ Added network operation tests with error scenarios"
    fi
    
    # Check for database operations
    if echo "$content" | grep -q "Room\|ObjectBox\|Dao\|Entity\|Database"; then
        echo "  ✓ Added database operation tests"
    fi
    
    # Check for permissions
    if echo "$content" | grep -q "checkSelfPermission\|requestPermissions\|PERMISSION"; then
        echo "  ✓ Added permission handling tests"
    fi
    
    # Check for lifecycle methods
    if echo "$content" | grep -q "onCreate\|onStart\|onResume\|onPause\|onStop\|onDestroy"; then
        echo "  ✓ Added lifecycle tests"
    fi
    
    # Check for voice/audio operations (VOS4 specific)
    if echo "$content" | grep -q "Voice\|Speech\|Recognition\|Audio\|Microphone"; then
        echo "  ✓ Added voice operation tests"
    fi
    
    # Check for AIDL/IPC (VOS4 specific)
    if echo "$content" | grep -q "IVoice\|IBinder\|ServiceConnection\|AIDL"; then
        echo "  ✓ Added AIDL communication tests"
    fi
}

# Main execution
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo -e "${BLUE}    VOS4 Intelligent Test Generator${NC}"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo

# Detect component type
COMPONENT_TYPE=$(detect_component_type "$SOURCE_FILE")

# Extract class information
extract_class_info "$SOURCE_FILE"

# Apply appropriate template
apply_template "$COMPONENT_TYPE"

# Add custom scenarios
add_custom_scenarios "$SOURCE_FILE"

# Final summary
echo
echo -e "${GREEN}═══════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ Test generation complete!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════${NC}"
echo
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Review the generated test file"
echo "2. Replace TODO placeholders with actual test logic"
echo "3. Add test data and mock configurations"
echo "4. Run: ./gradlew test"
echo
echo -e "${BLUE}Tips:${NC}"
echo "• Use @ParameterizedTest for testing multiple scenarios"
echo "• Add @DisplayName annotations for better test reporting"
echo "• Consider adding performance benchmarks for critical paths"
echo "• Ensure test isolation - each test should be independent"

# Make script executable if needed
chmod +x "$0" 2>/dev/null || true