/**
 * IntegrationTestHelper.kt - Test fixtures and utilities for integration testing
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Provides reusable test fixtures, builders, and utilities for
 * VoiceOSCoreNG integration tests.
 */
package com.augmentalis.voiceoscoreng.integration

import com.augmentalis.voiceoscoreng.common.CommandActionType
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkType
import com.augmentalis.voiceoscoreng.common.VUIDGenerator
import com.augmentalis.voiceoscoreng.common.VUIDTypeCode
import com.augmentalis.voiceoscoreng.handlers.ActionResult

/**
 * Test fixture factory for creating common test elements.
 *
 * Provides pre-configured ElementInfo objects representing various
 * UI scenarios commonly encountered during testing.
 */
object TestFixtures {

    // ==================== Package Names ====================

    const val TEST_PACKAGE = "com.test.app"
    const val FLUTTER_PACKAGE = "com.flutter.testapp"
    const val UNITY_PACKAGE = "com.unity.game"
    const val RN_PACKAGE = "com.reactnative.app"
    const val WEBVIEW_PACKAGE = "com.hybrid.webapp"

    // ==================== Standard Elements ====================

    /**
     * Create a standard submit button
     */
    fun submitButton(packageName: String = TEST_PACKAGE) = ElementInfo(
        className = "android.widget.Button",
        resourceId = "$packageName:id/submit_button",
        text = "Submit",
        contentDescription = "Submit form",
        bounds = Bounds(100, 200, 300, 250),
        isClickable = true,
        isEnabled = true,
        packageName = packageName
    )

    /**
     * Create a standard cancel button
     */
    fun cancelButton(packageName: String = TEST_PACKAGE) = ElementInfo(
        className = "android.widget.Button",
        resourceId = "$packageName:id/cancel_button",
        text = "Cancel",
        bounds = Bounds(100, 260, 300, 310),
        isClickable = true,
        isEnabled = true,
        packageName = packageName
    )

    /**
     * Create a standard email input field
     */
    fun emailInput(packageName: String = TEST_PACKAGE) = ElementInfo(
        className = "android.widget.EditText",
        resourceId = "$packageName:id/email_input",
        contentDescription = "Enter email address",
        bounds = Bounds(50, 100, 400, 150),
        isClickable = true,
        isEnabled = true,
        packageName = packageName
    )

    /**
     * Create a standard password input field
     */
    fun passwordInput(packageName: String = TEST_PACKAGE) = ElementInfo(
        className = "android.widget.EditText",
        resourceId = "$packageName:id/password_input",
        contentDescription = "Enter password",
        bounds = Bounds(50, 160, 400, 210),
        isClickable = true,
        isEnabled = true,
        packageName = packageName
    )

    /**
     * Create a static text label (non-actionable)
     */
    fun textLabel(text: String, packageName: String = TEST_PACKAGE) = ElementInfo(
        className = "android.widget.TextView",
        text = text,
        bounds = Bounds(50, 50, 400, 80),
        isClickable = false,
        isScrollable = false,
        isEnabled = true,
        packageName = packageName
    )

    /**
     * Create a scrollable list
     */
    fun scrollableList(packageName: String = TEST_PACKAGE) = ElementInfo(
        className = "androidx.recyclerview.widget.RecyclerView",
        resourceId = "$packageName:id/recycler_list",
        bounds = Bounds(0, 0, 1080, 1920),
        isClickable = false,
        isScrollable = true,
        isEnabled = true,
        packageName = packageName
    )

    // ==================== Dangerous Elements ====================

    /**
     * Create a dangerous delete button
     */
    fun deleteButton(packageName: String = TEST_PACKAGE) = ElementInfo(
        className = "android.widget.Button",
        resourceId = "$packageName:id/delete_button",
        text = "Delete Account",
        contentDescription = "Permanently delete your account",
        bounds = Bounds(100, 400, 300, 450),
        isClickable = true,
        isEnabled = true,
        packageName = packageName
    )

    /**
     * Create a purchase button
     */
    fun purchaseButton(price: String = "$9.99", packageName: String = TEST_PACKAGE) = ElementInfo(
        className = "android.widget.Button",
        resourceId = "$packageName:id/purchase_button",
        text = "Buy Now $price",
        bounds = Bounds(100, 500, 300, 550),
        isClickable = true,
        isEnabled = true,
        packageName = packageName
    )

    /**
     * Create a logout button
     */
    fun logoutButton(packageName: String = TEST_PACKAGE) = ElementInfo(
        className = "android.widget.Button",
        resourceId = "$packageName:id/logout_button",
        text = "Logout",
        bounds = Bounds(100, 600, 300, 650),
        isClickable = true,
        isEnabled = true,
        packageName = packageName
    )

    // ==================== Framework-Specific Elements ====================

    /**
     * Create Flutter view container
     */
    fun flutterView() = ElementInfo(
        className = "io.flutter.embedding.android.FlutterView",
        bounds = Bounds(0, 0, 1080, 1920),
        packageName = FLUTTER_PACKAGE
    )

    /**
     * Create Flutter semantic button
     */
    fun flutterButton(text: String) = ElementInfo(
        className = "io.flutter.SemanticsNode",
        text = text,
        bounds = Bounds(100, 200, 300, 250),
        isClickable = true,
        packageName = FLUTTER_PACKAGE
    )

    /**
     * Create Unity player surface
     */
    fun unityPlayer() = ElementInfo(
        className = "com.unity3d.player.UnityPlayer",
        bounds = Bounds(0, 0, 1080, 1920),
        packageName = UNITY_PACKAGE
    )

    /**
     * Create React Native root view
     */
    fun reactNativeRoot() = ElementInfo(
        className = "com.facebook.react.ReactRootView",
        bounds = Bounds(0, 0, 1080, 1920),
        packageName = RN_PACKAGE
    )

    /**
     * Create React Native button
     */
    fun reactNativeButton(text: String) = ElementInfo(
        className = "com.facebook.react.views.view.ReactViewGroup",
        text = text,
        bounds = Bounds(100, 200, 300, 250),
        isClickable = true,
        packageName = RN_PACKAGE
    )

    /**
     * Create WebView container
     */
    fun webView() = ElementInfo(
        className = "android.webkit.WebView",
        bounds = Bounds(0, 0, 1080, 1920),
        packageName = WEBVIEW_PACKAGE
    )

    // ==================== Screen Scenarios ====================

    /**
     * Create a complete login screen scenario
     */
    fun loginScreen(packageName: String = TEST_PACKAGE): List<ElementInfo> = listOf(
        textLabel("Welcome! Please sign in", packageName),
        emailInput(packageName),
        passwordInput(packageName),
        ElementInfo(
            className = "android.widget.Button",
            resourceId = "$packageName:id/login_button",
            text = "Login",
            bounds = Bounds(100, 280, 300, 330),
            isClickable = true,
            isEnabled = true,
            packageName = packageName
        ),
        ElementInfo(
            className = "android.widget.Button",
            resourceId = "$packageName:id/forgot_password",
            text = "Forgot Password?",
            bounds = Bounds(100, 340, 300, 380),
            isClickable = true,
            isEnabled = true,
            packageName = packageName
        )
    )

    /**
     * Create a settings screen with dangerous actions
     */
    fun settingsScreen(packageName: String = TEST_PACKAGE): List<ElementInfo> = listOf(
        textLabel("Settings", packageName),
        ElementInfo(
            className = "android.widget.Button",
            resourceId = "$packageName:id/profile_button",
            text = "Edit Profile",
            isClickable = true,
            packageName = packageName
        ),
        ElementInfo(
            className = "android.widget.Button",
            resourceId = "$packageName:id/notifications_button",
            text = "Notifications",
            isClickable = true,
            packageName = packageName
        ),
        logoutButton(packageName),
        deleteButton(packageName)
    )

    /**
     * Create a Flutter app screen
     */
    fun flutterAppScreen(): List<ElementInfo> = listOf(
        flutterView(),
        flutterButton("Home"),
        flutterButton("Profile"),
        flutterButton("Settings"),
        ElementInfo(
            className = "io.flutter.SemanticsNode",
            contentDescription = "Search",
            isClickable = true,
            packageName = FLUTTER_PACKAGE
        )
    )
}

/**
 * Builder for creating ElementInfo with fluent API.
 *
 * Usage:
 * ```kotlin
 * val element = ElementBuilder("Button")
 *     .withText("Click Me")
 *     .withResourceId("com.app:id/my_button")
 *     .clickable()
 *     .build()
 * ```
 */
class ElementBuilder(private val className: String) {

    private var resourceId: String = ""
    private var text: String = ""
    private var contentDescription: String = ""
    private var bounds: Bounds = Bounds.EMPTY
    private var isClickable: Boolean = false
    private var isScrollable: Boolean = false
    private var isEnabled: Boolean = true
    private var packageName: String = TestFixtures.TEST_PACKAGE

    fun withResourceId(resourceId: String) = apply { this.resourceId = resourceId }
    fun withText(text: String) = apply { this.text = text }
    fun withContentDescription(desc: String) = apply { this.contentDescription = desc }
    fun withBounds(left: Int, top: Int, right: Int, bottom: Int) = apply {
        this.bounds = Bounds(left, top, right, bottom)
    }
    fun withBounds(bounds: Bounds) = apply { this.bounds = bounds }
    fun clickable(value: Boolean = true) = apply { this.isClickable = value }
    fun scrollable(value: Boolean = true) = apply { this.isScrollable = value }
    fun enabled(value: Boolean = true) = apply { this.isEnabled = value }
    fun inPackage(packageName: String) = apply { this.packageName = packageName }

    fun build() = ElementInfo(
        className = className,
        resourceId = resourceId,
        text = text,
        contentDescription = contentDescription,
        bounds = bounds,
        isClickable = isClickable,
        isScrollable = isScrollable,
        isEnabled = isEnabled,
        packageName = packageName
    )
}

/**
 * Builder for creating QuantizedCommand with fluent API.
 */
class CommandBuilder(private val phrase: String) {

    private var uuid: String = ""
    private var actionType: CommandActionType = CommandActionType.CLICK
    private var targetVuid: String? = null
    private var confidence: Float = 0.8f
    private var metadata: Map<String, String> = emptyMap()

    fun withUuid(uuid: String) = apply { this.uuid = uuid }
    fun withActionType(type: CommandActionType) = apply { this.actionType = type }
    fun withTargetVuid(vuid: String?) = apply { this.targetVuid = vuid }
    fun withConfidence(confidence: Float) = apply { this.confidence = confidence }
    fun withMetadata(metadata: Map<String, String>) = apply { this.metadata = metadata }
    fun addMetadata(key: String, value: String) = apply {
        this.metadata = this.metadata + (key to value)
    }

    /**
     * Generate a VUID based on provided parameters
     */
    fun withGeneratedVuid(
        packageName: String,
        typeCode: VUIDTypeCode,
        elementHash: String
    ) = apply {
        this.targetVuid = VUIDGenerator.generate(packageName, typeCode, elementHash)
    }

    fun build() = QuantizedCommand(
        uuid = uuid,
        phrase = phrase,
        actionType = actionType,
        targetVuid = targetVuid,
        confidence = confidence,
        metadata = metadata
    )
}

/**
 * Assertion utilities for integration tests.
 */
object TestAssertions {

    /**
     * Assert that an ActionResult is successful
     */
    fun assertSuccess(result: ActionResult, message: String? = null) {
        if (!result.isSuccess) {
            throw AssertionError(
                message ?: "Expected success but got failure: ${result.message}"
            )
        }
    }

    /**
     * Assert that an ActionResult is a failure
     */
    fun assertFailure(result: ActionResult, message: String? = null) {
        if (result.isSuccess) {
            throw AssertionError(
                message ?: "Expected failure but got success: ${result.message}"
            )
        }
    }

    /**
     * Assert that an ActionResult is of a specific type
     */
    inline fun <reified T : ActionResult> assertResultType(
        result: ActionResult,
        message: String? = null
    ) {
        if (result !is T) {
            throw AssertionError(
                message ?: "Expected ${T::class.simpleName} but got ${result::class.simpleName}"
            )
        }
    }

    /**
     * Assert that a VUID is valid
     */
    fun assertValidVuid(vuid: String?, message: String? = null) {
        if (vuid == null) {
            throw AssertionError(message ?: "VUID is null")
        }
        if (!VUIDGenerator.isValidVUID(vuid)) {
            throw AssertionError(message ?: "Invalid VUID format: $vuid")
        }
    }

    /**
     * Assert that a command was generated
     */
    fun assertCommandGenerated(command: QuantizedCommand?, message: String? = null) {
        if (command == null) {
            throw AssertionError(message ?: "Command was not generated (null)")
        }
    }

    /**
     * Assert that a command was not generated
     */
    fun assertCommandNotGenerated(command: QuantizedCommand?, message: String? = null) {
        if (command != null) {
            throw AssertionError(
                message ?: "Command should not be generated but got: ${command.phrase}"
            )
        }
    }
}

/**
 * Extension functions for testing convenience.
 */

/**
 * Extension to check if element is dangerous
 */
fun ElementInfo.isDangerous(): Boolean {
    val detector = com.augmentalis.voiceoscoreng.safety.DangerousElementDetector()
    return detector.analyze(text, contentDescription, resourceId).isDangerous
}

/**
 * Extension to generate command from element
 */
fun ElementInfo.toCommand(): QuantizedCommand? {
    return com.augmentalis.voiceoscoreng.command.CommandGenerator.fromElement(this, packageName)
}

/**
 * Extension to generate VUID from element
 */
fun ElementInfo.toVuid(): String {
    val typeCode = VUIDGenerator.getTypeCode(className)
    val elementHash = when {
        resourceId.isNotBlank() -> resourceId
        contentDescription.isNotBlank() -> contentDescription
        text.isNotBlank() -> text
        else -> "$className:$bounds"
    }
    return VUIDGenerator.generate(packageName, typeCode, elementHash)
}

/**
 * Extension to create a list of elements with sequential IDs
 */
fun List<String>.toButtonElements(packageName: String = TestFixtures.TEST_PACKAGE): List<ElementInfo> {
    return mapIndexed { index, text ->
        ElementInfo(
            className = "android.widget.Button",
            resourceId = "$packageName:id/btn_$index",
            text = text,
            bounds = Bounds(0, index * 60, 300, (index + 1) * 60 - 10),
            isClickable = true,
            packageName = packageName
        )
    }
}

/**
 * Performance measurement utility
 */
class PerformanceTimer {
    private var startTime: Long = 0
    private var endTime: Long = 0

    fun start() {
        startTime = kotlin.system.getTimeMillis()
    }

    fun stop(): Long {
        endTime = kotlin.system.getTimeMillis()
        return duration()
    }

    fun duration(): Long = endTime - startTime

    companion object {
        inline fun measure(block: () -> Unit): Long {
            val timer = PerformanceTimer()
            timer.start()
            block()
            return timer.stop()
        }
    }
}
