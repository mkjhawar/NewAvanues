/**
 * StateDetectionPatterns.kt - Pattern constants for state detection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13 01:41:12 PDT
 *
 * Contains all pattern constants used for detecting various app states.
 * Includes resource ID patterns, framework class patterns, and keyword sets.
 */
package com.augmentalis.voiceoscore.learnapp.state

/**
 * Pattern constants for app state detection
 */
object StateDetectionPatterns {

    // === Keyword Patterns ===

    /**
     * Keywords commonly found in login/authentication screens
     */
    val LOGIN_KEYWORDS = setOf(
        "login", "sign in", "log in", "signin", "username", "password",
        "email", "authenticate", "sign up", "register", "create account"
    )

    /**
     * Keywords commonly found during loading states
     */
    val LOADING_KEYWORDS = setOf(
        "loading", "please wait", "processing", "refreshing",
        "fetching", "syncing", "updating"
    )

    /**
     * Keywords commonly found in error states
     */
    val ERROR_KEYWORDS = setOf(
        "error", "failed", "failure", "problem", "issue", "couldn't",
        "unable", "cannot", "retry", "try again", "oops"
    )

    /**
     * Keywords commonly found in permission dialogs
     */
    val PERMISSION_KEYWORDS = setOf(
        "permission", "allow", "deny", "access", "authorize",
        "grant", "enable", "location", "camera", "microphone", "storage"
    )

    /**
     * Keywords commonly found in tutorials/onboarding
     */
    val TUTORIAL_KEYWORDS = setOf(
        "welcome", "tutorial", "getting started", "onboarding",
        "skip", "next", "learn", "guide", "walkthrough"
    )

    /**
     * Keywords commonly found in empty states
     */
    val EMPTY_STATE_KEYWORDS = setOf(
        "no items", "nothing here", "empty", "no results", "no data",
        "no content", "get started", "add your first"
    )

    /**
     * Keywords commonly found in dialogs
     */
    val DIALOG_KEYWORDS = setOf(
        "ok", "cancel", "yes", "no", "confirm", "dismiss", "close"
    )

    // === Resource ID Patterns ===

    /**
     * Common resource ID patterns for login screens
     */
    val LOGIN_VIEW_ID_PATTERNS = setOf(
        "login", "signin", "sign_in", "btn_login", "button_login",
        "et_username", "et_email", "et_password", "input_username",
        "input_email", "input_password", "password_field", "email_field",
        "txt_username", "txt_password", "edt_login", "edit_password",
        "username", "password", "auth", "authenticate"
    )

    /**
     * Common resource ID patterns for loading indicators
     */
    val LOADING_VIEW_ID_PATTERNS = setOf(
        "progress", "loading", "spinner", "pb_loading",
        "progress_bar", "loading_indicator", "progressbar",
        "loading_spinner", "wait", "processing", "refresh"
    )

    /**
     * Common resource ID patterns for error states
     */
    val ERROR_VIEW_ID_PATTERNS = setOf(
        "error", "err", "error_message", "txt_error",
        "error_text", "error_icon", "retry", "btn_retry",
        "retry_button", "error_layout", "failure"
    )

    /**
     * Common resource ID patterns for dialogs
     */
    val DIALOG_VIEW_ID_PATTERNS = setOf(
        "dialog", "alert", "popup", "modal",
        "btn_ok", "btn_cancel", "btn_yes", "btn_no",
        "button1", "button2", "button3",  // Android standard dialog buttons
        "dialog_title", "dialog_message"
    )

    /**
     * Common resource ID patterns for permission requests
     */
    val PERMISSION_VIEW_ID_PATTERNS = setOf(
        "permission", "allow", "deny", "grant",
        "btn_allow", "btn_deny", "permission_message"
    )

    /**
     * Common resource ID patterns for tutorial/onboarding
     */
    val TUTORIAL_VIEW_ID_PATTERNS = setOf(
        "tutorial", "onboarding", "guide", "intro",
        "btn_skip", "btn_next", "skip", "next",
        "walkthrough", "intro_", "page_indicator"
    )

    /**
     * Common resource ID patterns for empty states
     */
    val EMPTY_STATE_VIEW_ID_PATTERNS = setOf(
        "empty", "empty_state", "empty_view", "no_data",
        "no_content", "no_items", "empty_message"
    )

    // === Framework Class Patterns ===

    /**
     * Dialog framework classes (strong indicators)
     */
    val DIALOG_FRAMEWORK_CLASSES = setOf(
        "android.app.AlertDialog",
        "android.app.Dialog",
        "androidx.appcompat.app.AlertDialog",
        "androidx.fragment.app.DialogFragment",
        "com.google.android.material.bottomsheet.BottomSheetDialog",
        "com.google.android.material.dialog.MaterialAlertDialog"
    )

    /**
     * Progress indicator framework classes
     */
    val PROGRESS_FRAMEWORK_CLASSES = setOf(
        "android.widget.ProgressBar",
        "com.google.android.material.progressindicator.CircularProgressIndicator",
        "com.google.android.material.progressindicator.LinearProgressIndicator"
    )

    /**
     * Material input field classes
     */
    val MATERIAL_INPUT_CLASSES = setOf(
        "com.google.android.material.textfield.TextInputLayout",
        "com.google.android.material.textfield.TextInputEditText"
    )

    /**
     * WebView classes (for web content detection)
     */
    val WEBVIEW_CLASSES = setOf(
        "android.webkit.WebView",
        "android.webkit.WebViewClient",
        "android.webkit.WebChromeClient"
    )

    /**
     * Jetpack Compose UI classes
     */
    val COMPOSE_UI_PATTERNS = setOf(
        "androidx.compose.ui",
        "androidx.compose.material",
        "androidx.compose.foundation"
    )
}
