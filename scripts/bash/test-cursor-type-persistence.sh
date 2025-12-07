#!/bin/bash
# Test script for VoiceCursor cursor type persistence
# Tests the fix for cursor type not persisting across app restarts

set -e

ADB=~/Library/Android/sdk/platform-tools/adb
PACKAGE="com.augmentalis.voiceos"
PREFS_FILE="/data/data/$PACKAGE/shared_prefs/voice_cursor_prefs.xml"

echo "======================================"
echo "VoiceCursor Type Persistence Test"
echo "======================================"
echo ""

# Function to get current cursor type from SharedPreferences
get_cursor_type() {
    $ADB shell "run-as $PACKAGE cat $PREFS_FILE 2>/dev/null | grep 'cursor_type' | sed 's/.*>\(.*\)<.*/\1/'" 2>/dev/null || echo "Normal"
}

# Function to set cursor type via adb
set_cursor_type() {
    local type=$1
    echo "Setting cursor type to: $type"
    $ADB shell am start -n $PACKAGE/.cursor.ui.VoiceCursorSettingsActivity
    sleep 2
    # Note: Actual UI interaction would require UI Automator
    # For now, we'll set it directly via shared_prefs (simulating what the fix does)
}

# Function to force stop the app
force_stop_app() {
    echo "Force stopping app..."
    $ADB shell am force-stop $PACKAGE
    sleep 1
}

# Function to restart app
restart_app() {
    echo "Restarting app..."
    $ADB shell am start -n $PACKAGE/.MainActivity
    sleep 2
}

echo "Step 1: Check current cursor type"
current_type=$(get_cursor_type)
echo "Current cursor type: $current_type"
echo ""

echo "Step 2: Opening VoiceCursor Settings..."
$ADB shell am start -n $PACKAGE/com.augmentalis.voiceos.cursor.ui.VoiceCursorSettingsActivity
sleep 3
echo "✓ Settings opened"
echo ""

echo "Step 3: Manual Test Required"
echo "----------------------------"
echo "Please perform the following steps on the emulator:"
echo ""
echo "1. Change cursor type from 'Normal' to 'Hand'"
echo "2. Wait for this script to continue..."
echo ""
read -p "Press ENTER when you've changed cursor type to 'Hand'"
echo ""

echo "Step 4: Checking if cursor type was saved correctly..."
sleep 1
saved_type=$(get_cursor_type)
echo "Saved cursor type: $saved_type"

if [ "$saved_type" = "Hand" ]; then
    echo "✓ Cursor type saved correctly!"
else
    echo "✗ ERROR: Cursor type not saved correctly (expected: Hand, got: $saved_type)"
fi
echo ""

echo "Step 5: Force stopping app to test persistence..."
force_stop_app
echo "✓ App stopped"
echo ""

echo "Step 6: Checking if cursor type persisted..."
persisted_type=$(get_cursor_type)
echo "Persisted cursor type: $persisted_type"

if [ "$persisted_type" = "Hand" ]; then
    echo "✓ SUCCESS: Cursor type persisted correctly!"
    echo ""
    echo "======================================"
    echo "TEST PASSED ✓"
    echo "======================================"
    exit 0
else
    echo "✗ FAILURE: Cursor type did not persist (expected: Hand, got: $persisted_type)"
    echo ""
    echo "======================================"
    echo "TEST FAILED ✗"
    echo "======================================"
    exit 1
fi
