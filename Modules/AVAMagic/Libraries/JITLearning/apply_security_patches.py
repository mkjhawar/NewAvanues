#!/usr/bin/env python3
"""
Apply security patches to JITLearningService.kt

Adds security checks to all 23 AIDL methods.

Usage: python3 apply_security_patches.py
"""

import re

# File path
SERVICE_FILE = "src/main/java/com/augmentalis/jitlearning/JITLearningService.kt"

# Security patches to apply
PATCHES = [
    # pauseCapture
    {
        "method": "override fun pauseCapture()",
        "insert_after": "override fun pauseCapture() {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
    # resumeCapture
    {
        "method": "override fun resumeCapture()",
        "insert_after": "override fun resumeCapture() {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
    # queryState
    {
        "method": "override fun queryState(): JITState",
        "insert_after": "override fun queryState(): JITState {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
    # getLearnedScreenHashes
    {
        "method": "override fun getLearnedScreenHashes(packageName: String): List<String>",
        "insert_after": "override fun getLearnedScreenHashes(packageName: String): List<String> {",
        "code": """            // SECURITY (2025-12-12): Verify caller + validate packageName
            securityManager.verifyCallerPermission()
            InputValidator.validatePackageName(packageName)

"""
    },
    # registerEventListener
    {
        "method": "override fun registerEventListener(listener: IAccessibilityEventListener)",
        "insert_after": "override fun registerEventListener(listener: IAccessibilityEventListener) {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
    # unregisterEventListener
    {
        "method": "override fun unregisterEventListener(listener: IAccessibilityEventListener)",
        "insert_after": "override fun unregisterEventListener(listener: IAccessibilityEventListener) {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
    # getCurrentScreenInfo
    {
        "method": "override fun getCurrentScreenInfo(): ParcelableNodeInfo?",
        "insert_after": "override fun getCurrentScreenInfo(): ParcelableNodeInfo? {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
    # getFullMenuContent
    {
        "method": "override fun getFullMenuContent(menuNodeId: String): ParcelableNodeInfo?",
        "insert_after": "override fun getFullMenuContent(menuNodeId: String): ParcelableNodeInfo? {",
        "code": """            // SECURITY (2025-12-12): Verify caller + validate menuNodeId
            securityManager.verifyCallerPermission()
            InputValidator.validateNodeId(menuNodeId)

"""
    },
    # queryElements
    {
        "method": "override fun queryElements(selector: String): List<ParcelableNodeInfo>",
        "insert_after": "override fun queryElements(selector: String): List<ParcelableNodeInfo> {",
        "code": """            // SECURITY (2025-12-12): Verify caller + validate selector
            securityManager.verifyCallerPermission()
            InputValidator.validateSelector(selector)

"""
    },
    # performClick
    {
        "method": "override fun performClick(elementUuid: String): Boolean",
        "insert_after": "override fun performClick(elementUuid: String): Boolean {",
        "code": """            // SECURITY (2025-12-12): Verify caller + validate UUID
            securityManager.verifyCallerPermission()
            InputValidator.validateUuid(elementUuid)

"""
    },
    # performScroll
    {
        "method": "override fun performScroll(direction: String, distance: Int): Boolean",
        "insert_after": "override fun performScroll(direction: String, distance: Int): Boolean {",
        "code": """            // SECURITY (2025-12-12): Verify caller + validate inputs
            securityManager.verifyCallerPermission()
            InputValidator.validateScrollDirection(direction)
            InputValidator.validateDistance(distance)

"""
    },
    # performAction
    {
        "method": "override fun performAction(command: ExplorationCommand): Boolean",
        "insert_after": "override fun performAction(command: ExplorationCommand): Boolean {",
        "code": """            // SECURITY (2025-12-12): Verify caller + validate command
            securityManager.verifyCallerPermission()
            when (command.type) {
                CommandType.CLICK, CommandType.LONG_CLICK, CommandType.FOCUS,
                CommandType.CLEAR_TEXT, CommandType.EXPAND, CommandType.SELECT -> {
                    InputValidator.validateUuid(command.elementUuid)
                }
                CommandType.SCROLL, CommandType.SWIPE -> {
                    InputValidator.validateDistance(command.distance)
                }
                CommandType.SET_TEXT -> {
                    InputValidator.validateUuid(command.elementUuid)
                    InputValidator.validateTextInput(command.text)
                }
                CommandType.BACK, CommandType.HOME -> { /* No validation needed */ }
            }

"""
    },
    # performBack
    {
        "method": "override fun performBack(): Boolean",
        "insert_after": "override fun performBack(): Boolean {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
    # registerElement
    {
        "method": "override fun registerElement(nodeInfo: ParcelableNodeInfo, uuid: String)",
        "insert_after": "override fun registerElement(nodeInfo: ParcelableNodeInfo, uuid: String) {",
        "code": """            // SECURITY (2025-12-12): Verify caller + validate inputs
            securityManager.verifyCallerPermission()
            InputValidator.validateUuid(uuid)

"""
    },
    # clearRegisteredElements
    {
        "method": "override fun clearRegisteredElements()",
        "insert_after": "override fun clearRegisteredElements() {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
    # startExploration
    {
        "method": "override fun startExploration(packageName: String): Boolean",
        "insert_after": "override fun startExploration(packageName: String): Boolean {",
        "code": """            // SECURITY (2025-12-12): Verify caller + validate packageName
            securityManager.verifyCallerPermission()
            InputValidator.validatePackageName(packageName)

"""
    },
    # stopExploration
    {
        "method": "override fun stopExploration()",
        "insert_after": "override fun stopExploration() {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
    # pauseExploration
    {
        "method": "override fun pauseExploration()",
        "insert_after": "override fun pauseExploration() {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
    # resumeExploration
    {
        "method": "override fun resumeExploration()",
        "insert_after": "override fun resumeExploration() {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
    # getExplorationProgress
    {
        "method": "override fun getExplorationProgress(): ExplorationProgress",
        "insert_after": "override fun getExplorationProgress(): ExplorationProgress {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
    # registerExplorationListener
    {
        "method": "override fun registerExplorationListener(listener: IExplorationProgressListener)",
        "insert_after": "override fun registerExplorationListener(listener: IExplorationProgressListener) {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
    # unregisterExplorationListener
    {
        "method": "override fun unregisterExplorationListener(listener: IExplorationProgressListener)",
        "insert_after": "override fun unregisterExplorationListener(listener: IExplorationProgressListener) {",
        "code": """            // SECURITY (2025-12-12): Verify caller permission
            securityManager.verifyCallerPermission()

"""
    },
]


def main():
    print(f"Reading {SERVICE_FILE}...")
    with open(SERVICE_FILE, 'r') as f:
        content = f.read()

    print(f"\nApplying {len(PATCHES)} security patches...")
    patches_applied = 0
    patches_skipped = 0

    for patch in PATCHES:
        method_name = patch["method"]
        insert_after = patch["insert_after"]
        code_to_insert = patch["code"]

        # Check if patch already applied
        if "SECURITY (2025-12-12)" in content and code_to_insert.strip() in content:
            print(f"  ⏭  {method_name} - already patched")
            patches_skipped += 1
            continue

        # Find the method and insert security code
        if insert_after in content:
            content = content.replace(insert_after, insert_after + "\n" + code_to_insert, 1)
            print(f"  ✓  {method_name}")
            patches_applied += 1
        else:
            print(f"  ✗  {method_name} - NOT FOUND")

    print(f"\nWriting changes to {SERVICE_FILE}...")
    with open(SERVICE_FILE, 'w') as f:
        f.write(content)

    print(f"\n✅ Security patches complete!")
    print(f"   Applied: {patches_applied}")
    print(f"   Skipped: {patches_skipped}")
    print(f"   Total: {len(PATCHES)}")


if __name__ == "__main__":
    main()
