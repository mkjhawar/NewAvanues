#!/bin/bash

# VOS4 Automatic Test Generator
# 
# Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
# Created: 2025-01-28
# 
# Automatically generates test files for new or modified source files

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Check if source file is provided
if [ $# -eq 0 ]; then
    echo -e "${RED}Error: No source file provided${NC}"
    echo "Usage: $0 <source-file.kt> [--force]"
    exit 1
fi

SOURCE_FILE=$1
FORCE_OVERWRITE=${2:-""}

# Validate source file exists
if [ ! -f "$SOURCE_FILE" ]; then
    echo -e "${RED}Error: Source file not found: $SOURCE_FILE${NC}"
    exit 1
fi

# Extract file information
BASE_NAME=$(basename "$SOURCE_FILE" .kt)
DIR_PATH=$(dirname "$SOURCE_FILE")
PACKAGE_NAME=$(grep "^package " "$SOURCE_FILE" | sed 's/package //')

# Convert source path to test path
TEST_DIR=${DIR_PATH/src\/main/src\/test}
ANDROID_TEST_DIR=${DIR_PATH/src\/main/src\/androidTest}

# Determine test type based on content
IS_UI_COMPONENT=false
IS_VIEWMODEL=false
IS_SERVICE=false
IS_MANAGER=false
IS_ACTIVITY=false

if grep -q "ComponentActivity\|@Composable" "$SOURCE_FILE"; then
    IS_UI_COMPONENT=true
    TEST_DIR=$ANDROID_TEST_DIR
elif grep -q "ViewModel" "$SOURCE_FILE"; then
    IS_VIEWMODEL=true
elif grep -q "Service\|Manager" "$SOURCE_FILE"; then
    IS_SERVICE=true
elif grep -q "Activity" "$SOURCE_FILE"; then
    IS_ACTIVITY=true
    TEST_DIR=$ANDROID_TEST_DIR
elif grep -q "Manager" "$SOURCE_FILE"; then
    IS_MANAGER=true
fi

# Create test directory if it doesn't exist
mkdir -p "$TEST_DIR"

# Define test file path
TEST_FILE="${TEST_DIR}/${BASE_NAME}Test.kt"

# Check if test file already exists
if [ -f "$TEST_FILE" ] && [ "$FORCE_OVERWRITE" != "--force" ]; then
    echo -e "${YELLOW}Test file already exists: $TEST_FILE${NC}"
    echo "Use --force to overwrite"
    exit 0
fi

echo -e "${BLUE}Generating test for: $SOURCE_FILE${NC}"
echo "Test file: $TEST_FILE"

# Extract class/object names and methods from source
CLASSES=$(grep -E "^(class|object) " "$SOURCE_FILE" | sed -E 's/(class|object) ([A-Za-z0-9_]+).*/\2/' | head -1)
METHODS=$(grep -E "fun [a-zA-Z]" "$SOURCE_FILE" | sed -E 's/.*fun ([a-zA-Z0-9_]+).*/\1/' | grep -v "^get\|^set\|^on" | head -10)

# Generate appropriate test template based on type
if [ "$IS_UI_COMPONENT" = true ]; then
    cat > "$TEST_FILE" << EOF
/**
 * ${BASE_NAME}Test.kt - Automated UI tests for $BASE_NAME
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: $(date +%Y-%m-%d)
 * 
 * AUTO-GENERATED TEST - Please complete implementation
 */
package $PACKAGE_NAME

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class ${BASE_NAME}Test {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }
    
    @Test
    fun test_${BASE_NAME}_renders_correctly() {
        composeTestRule.setContent {
            // TODO: Set up your composable
            // $CLASSES()
        }
        
        // TODO: Add assertions
        composeTestRule.onRoot().assertExists()
    }
    
    @Test
    fun test_${BASE_NAME}_handles_click_events() {
        var clicked = false
        
        composeTestRule.setContent {
            // TODO: Set up composable with click handler
        }
        
        // TODO: Perform click and verify
        // composeTestRule.onNodeWithTag("tag").performClick()
        // assertTrue(clicked)
    }
    
    @Test
    fun test_${BASE_NAME}_displays_correct_content() {
        composeTestRule.setContent {
            // TODO: Set up composable with test data
        }
        
        // TODO: Verify content is displayed
        // composeTestRule.onNodeWithText("Expected Text").assertIsDisplayed()
    }
EOF

elif [ "$IS_VIEWMODEL" = true ]; then
    cat > "$TEST_FILE" << EOF
/**
 * ${BASE_NAME}Test.kt - Automated tests for $BASE_NAME
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: $(date +%Y-%m-%d)
 * 
 * AUTO-GENERATED TEST - Please complete implementation
 */
package $PACKAGE_NAME

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.*

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ${BASE_NAME}Test {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Mock
    private lateinit var mockObserver: Observer<Any> // TODO: Replace with actual type
    
    private lateinit var viewModel: $CLASSES
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        viewModel = $CLASSES()
        // TODO: Set up observers
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun test_initial_state() {
        // TODO: Verify initial state
        assertNotNull(viewModel)
    }
EOF

    # Add test methods for each public method found
    for method in $METHODS; do
        cat >> "$TEST_FILE" << EOF
    
    @Test
    fun test_${method}() = runTest {
        // Arrange
        // TODO: Set up test data
        
        // Act
        // viewModel.$method()
        
        // Assert
        // TODO: Verify expected behavior
        verify(mockObserver).onChanged(any())
    }
EOF
    done

    cat >> "$TEST_FILE" << EOF
}
EOF

else
    # Generic test template
    cat > "$TEST_FILE" << EOF
/**
 * ${BASE_NAME}Test.kt - Automated tests for $BASE_NAME
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: $(date +%Y-%m-%d)
 * 
 * AUTO-GENERATED TEST - Please complete implementation
 */
package $PACKAGE_NAME

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.*

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ${BASE_NAME}Test {
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var subject: $CLASSES
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        subject = $CLASSES()
    }
    
    @Test
    fun test_initialization() {
        assertNotNull(subject)
    }
EOF

    # Add test methods for each public method
    for method in $METHODS; do
        cat >> "$TEST_FILE" << EOF
    
    @Test
    fun test_${method}() = runTest {
        // Arrange
        // TODO: Set up test data
        
        // Act
        // val result = subject.$method()
        
        // Assert
        // TODO: Add assertions
        assertTrue(true, "Implement test for $method")
    }
EOF
    done

    # Add edge case and error handling tests
    cat >> "$TEST_FILE" << EOF
    
    @Test
    fun test_handles_null_input() {
        // TODO: Test null handling
        assertFailsWith<NullPointerException> {
            // subject.methodWithNullableParam(null)
        }
    }
    
    @Test
    fun test_handles_error_conditions() = runTest {
        // TODO: Test error scenarios
        try {
            // subject.methodThatMightFail()
            fail("Should have thrown exception")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("expected") == true)
        }
    }
    
    @Test
    fun test_performance_requirements() = runTest {
        val startTime = System.currentTimeMillis()
        
        // TODO: Execute performance-critical operation
        // subject.performanceMethod()
        
        val duration = System.currentTimeMillis() - startTime
        assertTrue(duration < 1000, "Operation should complete in < 1s, took \${duration}ms")
    }
}
EOF
fi

echo -e "${GREEN}✅ Test file generated: $TEST_FILE${NC}"

# Add test file to git
git add "$TEST_FILE"
echo -e "${GREEN}✅ Test file added to git${NC}"

# Show summary
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Review and complete the generated test implementation"
echo "2. Replace TODO comments with actual test logic"
echo "3. Run: ./gradlew test to verify tests pass"
echo "4. Commit both source and test files together"