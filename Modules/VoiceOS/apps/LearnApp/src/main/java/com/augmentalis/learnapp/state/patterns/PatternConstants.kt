/**
 * PatternConstants.kt - Centralized pattern definitions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Centralized repository of all keyword patterns and class names used for state detection.
 * Consolidates patterns from original AppStateDetector plus enhancements.
 *
 * SOLID: Single Responsibility - One place to manage all patterns
 */
package com.augmentalis.learnapp.state.patterns

/**
 * Centralized pattern constants for state detection
 *
 * All keyword sets, resource ID patterns, and framework class patterns in one place.
 * Makes it easy to update patterns without modifying detector logic.
 */
object PatternConstants {

    // ========== TEXT KEYWORD PATTERNS ==========

    /** Keywords indicating login/authentication screens */
    val LOGIN_KEYWORDS = setOf(
        "login", "sign in", "log in", "signin", "username", "password",
        "email", "authenticate", "sign up", "register", "create account"
    )

    /** Keywords indicating loading states */
    val LOADING_KEYWORDS = setOf(
        "loading", "please wait", "processing", "refreshing",
        "fetching", "syncing", "updating"
    )

    /** Keywords indicating error states */
    val ERROR_KEYWORDS = setOf(
        "error", "failed", "failure", "problem", "issue", "couldn't",
        "unable", "cannot", "retry", "try again", "oops"
    )

    /** Keywords indicating permission requests */
    val PERMISSION_KEYWORDS = setOf(
        "permission", "allow", "deny", "access", "authorize",
        "grant", "enable", "location", "camera", "microphone", "storage"
    )

    /** Keywords indicating tutorial/onboarding */
    val TUTORIAL_KEYWORDS = setOf(
        "welcome", "tutorial", "getting started", "onboarding",
        "skip", "next", "learn", "guide", "walkthrough"
    )

    /** Keywords indicating empty states */
    val EMPTY_STATE_KEYWORDS = setOf(
        "no items", "nothing here", "empty", "no results", "no data",
        "no content", "get started", "add your first"
    )

    /** Keywords indicating dialogs */
    val DIALOG_KEYWORDS = setOf(
        "ok", "cancel", "yes", "no", "confirm", "dismiss", "close"
    )

    // ========== RESOURCE ID PATTERNS (PHASE 1 ENHANCEMENT) ==========

    /** Resource ID patterns for login screens */
    val LOGIN_VIEW_ID_PATTERNS = setOf(
        "login", "signin", "sign_in", "btn_login", "button_login",
        "et_username", "et_email", "et_password", "input_username",
        "input_email", "input_password", "password_field", "email_field",
        "txt_username", "txt_password", "edt_login", "edit_password",
        "username", "password", "auth", "authenticate"
    )

    /** Resource ID patterns for loading indicators */
    val LOADING_VIEW_ID_PATTERNS = setOf(
        "progress", "loading", "spinner", "refresh", "sync",
        "progress_bar", "loading_indicator", "pb_loading"
    )

    /** Resource ID patterns for error states */
    val ERROR_VIEW_ID_PATTERNS = setOf(
        "error", "warning", "alert", "fail", "retry",
        "error_message", "error_text", "tv_error", "error_icon"
    )

    /** Resource ID patterns for permission dialogs */
    val PERMISSION_VIEW_ID_PATTERNS = setOf(
        "permission", "allow", "deny", "grant", "authorize",
        "btn_allow", "btn_deny", "permission_dialog"
    )

    /** Resource ID patterns for tutorial/onboarding */
    val TUTORIAL_VIEW_ID_PATTERNS = setOf(
        "tutorial", "onboarding", "welcome", "skip", "next",
        "btn_skip", "btn_next", "walkthrough", "intro"
    )

    /** Resource ID patterns for empty states */
    val EMPTY_STATE_VIEW_ID_PATTERNS = setOf(
        "empty", "no_data", "no_items", "no_results",
        "empty_view", "no_content", "placeholder"
    )

    /** Resource ID patterns for dialogs */
    val DIALOG_VIEW_ID_PATTERNS = setOf(
        "dialog", "alert", "modal", "popup",
        "btn_ok", "btn_cancel", "btn_close", "dialog_button"
    )

    // ========== FRAMEWORK CLASS PATTERNS (PHASE 1 ENHANCEMENT) ==========

    /** Framework dialog classes (strong indicators) */
    val DIALOG_FRAMEWORK_CLASSES = setOf(
        "android.app.AlertDialog",
        "android.app.Dialog",
        "androidx.appcompat.app.AlertDialog",
        "androidx.fragment.app.DialogFragment",
        "com.google.android.material.dialog.MaterialAlertDialog",
        "com.google.android.material.bottomsheet.BottomSheetDialog",
        "com.google.android.material.bottomsheet.BottomSheetDialogFragment"
    )

    /** Framework loading indicator classes */
    val LOADING_FRAMEWORK_CLASSES = setOf(
        "android.widget.ProgressBar",
        "androidx.core.widget.ContentLoadingProgressBar",
        "com.google.android.material.progressindicator.CircularProgressIndicator",
        "com.google.android.material.progressindicator.LinearProgressIndicator"
    )

    /** Material Design input field classes */
    val MATERIAL_INPUT_CLASSES = setOf(
        "com.google.android.material.textfield.TextInputLayout",
        "com.google.android.material.textfield.TextInputEditText"
    )

    /** Material Design button classes */
    val MATERIAL_BUTTON_CLASSES = setOf(
        "com.google.android.material.button.MaterialButton",
        "com.google.android.material.floatingactionbutton.FloatingActionButton",
        "com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton"
    )

    // ========== SCORE WEIGHTS (PHASE 11 ENHANCEMENT) ==========

    /** Weight for framework class matches (strongest signal) */
    const val WEIGHT_FRAMEWORK_CLASS = 0.5f

    /** Weight for resource ID matches (strong signal) */
    const val WEIGHT_RESOURCE_ID = 0.4f

    /** Weight for Material Design component matches */
    const val WEIGHT_MATERIAL_COMPONENT = 0.35f

    /** Weight for text keyword matches (weaker due to localization) */
    const val WEIGHT_TEXT_KEYWORD = 0.25f

    /** Weight for generic class name matches */
    const val WEIGHT_CLASS_NAME_GENERIC = 0.2f

    /** Penalty weight for negative indicators */
    const val WEIGHT_NEGATIVE_INDICATOR = -0.15f

    // ========== COMMON UI PATTERNS ==========

    /** Standard EditText class names */
    val EDITTEXT_CLASSES = setOf(
        "EditText",
        "android.widget.EditText",
        "androidx.appcompat.widget.AppCompatEditText"
    )

    /** Standard Button class names */
    val BUTTON_CLASSES = setOf(
        "Button",
        "android.widget.Button",
        "androidx.appcompat.widget.AppCompatButton"
    )

    /** List/ScrollView classes (negative indicator for login) */
    val LIST_CONTAINER_CLASSES = setOf(
        "RecyclerView",
        "ListView",
        "GridView",
        "ScrollView",
        "NestedScrollView",
        "ViewPager",
        "ViewPager2"
    )
}
