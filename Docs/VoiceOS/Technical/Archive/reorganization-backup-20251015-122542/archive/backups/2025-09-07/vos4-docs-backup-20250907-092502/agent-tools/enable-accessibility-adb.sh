#!/bin/bash
# Enable VoiceOS Accessibility Service via ADB (for testing)

PACKAGE="com.augmentalis.voiceos"
SERVICE="com.ai.voiceaccessibility.service.AccessibilityService"
COMPONENT="$PACKAGE/$SERVICE"

echo "Enabling VoiceOS Accessibility Service..."

# Enable the service
adb shell settings put secure enabled_accessibility_services $COMPONENT

# Mark accessibility as enabled
adb shell settings put secure accessibility_enabled 1

echo "Done! The accessibility service should now be enabled."
echo "Note: This only works on devices with ADB debugging enabled."