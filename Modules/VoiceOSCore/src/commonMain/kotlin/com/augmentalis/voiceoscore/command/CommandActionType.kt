package com.augmentalis.voiceoscore

/**
 * Command Action Type - Action types for voice commands.
 *
 * Defines the action a command performs on a UI element or system.
 *
 * Categories:
 * - Element Actions: CLICK, LONG_CLICK, TYPE, SCROLL_*
 * - Navigation: BACK, HOME, RECENT_APPS, APP_DRAWER, NAVIGATE
 * - Media: MEDIA_*, VOLUME_*
 * - System: OPEN_SETTINGS, NOTIFICATIONS, SCREENSHOT, FLASHLIGHT_*
 * - VoiceOS: VOICE_*, DICTATION_*, SHOW_COMMANDS
 * - App Launch: OPEN_APP
 */
enum class CommandActionType {
    // ═══════════════════════════════════════════════════════════════════
    // Element Actions (UI interaction)
    // ═══════════════════════════════════════════════════════════════════

    /** Click/tap action */
    CLICK,

    /** Tap action (alias for CLICK) */
    TAP,

    /** Long press/hold action */
    LONG_CLICK,

    /** Execute/run action (generic) */
    EXECUTE,

    /** Text input action */
    TYPE,

    /** Focus an element */
    FOCUS,

    // ═══════════════════════════════════════════════════════════════════
    // Scroll Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Scroll down */
    SCROLL_DOWN,

    /** Scroll up */
    SCROLL_UP,

    /** Scroll left */
    SCROLL_LEFT,

    /** Scroll right */
    SCROLL_RIGHT,

    /** Generic scroll (direction determined by context) */
    SCROLL,

    // ═══════════════════════════════════════════════════════════════════
    // Navigation Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Navigate to previous screen */
    BACK,

    /** Navigate to home screen */
    HOME,

    /** Show recent apps */
    RECENT_APPS,

    /** Open app drawer */
    APP_DRAWER,

    /** Generic navigation action (screen transition) */
    NAVIGATE,

    // ═══════════════════════════════════════════════════════════════════
    // Media Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Play/resume media */
    MEDIA_PLAY,

    /** Pause media */
    MEDIA_PAUSE,

    /** Next track */
    MEDIA_NEXT,

    /** Previous track */
    MEDIA_PREVIOUS,

    /** Increase volume */
    VOLUME_UP,

    /** Decrease volume */
    VOLUME_DOWN,

    /** Mute audio */
    VOLUME_MUTE,

    // ═══════════════════════════════════════════════════════════════════
    // System Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Open system settings */
    OPEN_SETTINGS,

    /** Show notification panel */
    NOTIFICATIONS,

    /** Clear all notifications */
    CLEAR_NOTIFICATIONS,

    /** Take screenshot */
    SCREENSHOT,

    /** Turn flashlight on */
    FLASHLIGHT_ON,

    /** Turn flashlight off */
    FLASHLIGHT_OFF,

    // ═══════════════════════════════════════════════════════════════════
    // VoiceOS Control Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Mute voice recognition */
    VOICE_MUTE,

    /** Wake/unmute voice recognition */
    VOICE_WAKE,

    /** Start dictation mode */
    DICTATION_START,

    /** Stop dictation mode */
    DICTATION_STOP,

    /** Show available voice commands */
    SHOW_COMMANDS,

    /** Numbers overlay: always ON */
    NUMBERS_ON,

    /** Numbers overlay: always OFF */
    NUMBERS_OFF,

    /** Numbers overlay: AUTO (show for lists) */
    NUMBERS_AUTO,

    // ═══════════════════════════════════════════════════════════════════
    // App Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Open an app (requires metadata) */
    OPEN_APP,

    /** Close current app */
    CLOSE_APP,

    // ═══════════════════════════════════════════════════════════════════
    // Text/Clipboard Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Select all text */
    SELECT_ALL,

    /** Copy selection to clipboard */
    COPY,

    /** Paste from clipboard */
    PASTE,

    /** Cut selection to clipboard */
    CUT,

    /** Undo last action */
    UNDO,

    /** Redo last undone action */
    REDO,

    /** Delete selected text or element */
    DELETE,

    // ═══════════════════════════════════════════════════════════════════
    // Screen & Display Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Increase screen brightness */
    BRIGHTNESS_UP,

    /** Decrease screen brightness */
    BRIGHTNESS_DOWN,

    /** Lock screen */
    LOCK_SCREEN,

    /** Rotate screen orientation */
    ROTATE_SCREEN,

    /** Zoom in */
    ZOOM_IN,

    /** Zoom out */
    ZOOM_OUT,

    // ═══════════════════════════════════════════════════════════════════
    // Connectivity Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Toggle WiFi on/off */
    TOGGLE_WIFI,

    /** Toggle Bluetooth on/off */
    TOGGLE_BLUETOOTH,

    // ═══════════════════════════════════════════════════════════════════
    // Cursor Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Show voice cursor */
    CURSOR_SHOW,

    /** Hide voice cursor */
    CURSOR_HIDE,

    /** Cursor click at current position */
    CURSOR_CLICK,

    /** Move cursor up */
    CURSOR_UP,

    /** Move cursor down */
    CURSOR_DOWN,

    /** Move cursor left */
    CURSOR_LEFT,

    /** Move cursor right */
    CURSOR_RIGHT,

    // ═══════════════════════════════════════════════════════════════════
    // Reading/TTS Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Read screen content aloud */
    READ_SCREEN,

    /** Stop reading */
    STOP_READING,

    // ═══════════════════════════════════════════════════════════════════
    // Input Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Show on-screen keyboard */
    SHOW_KEYBOARD,

    /** Hide on-screen keyboard */
    HIDE_KEYBOARD,

    // ═══════════════════════════════════════════════════════════════════
    // Custom
    // ═══════════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════════
    // Browser Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Force re-scrape of current web page (invalidate cache + rescrape DOM) */
    RETRAIN_PAGE,

    // ═══════════════════════════════════════════════════════════════════
    // Browser Navigation Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Browser back (history.back) */
    PAGE_BACK,

    /** Browser forward (history.forward) */
    PAGE_FORWARD,

    /** Reload the current page */
    PAGE_REFRESH,

    // ═══════════════════════════════════════════════════════════════════
    // Page Scrolling Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Scroll to the top of the page */
    SCROLL_TO_TOP,

    /** Scroll to the bottom of the page */
    SCROLL_TO_BOTTOM,

    // ═══════════════════════════════════════════════════════════════════
    // Form Navigation Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Focus next focusable element (tab) */
    TAB_NEXT,

    /** Focus previous focusable element (shift-tab) */
    TAB_PREV,

    /** Submit the current/nearest form */
    SUBMIT_FORM,

    // ═══════════════════════════════════════════════════════════════════
    // Gesture Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Swipe left on element or page */
    SWIPE_LEFT,

    /** Swipe right on element or page */
    SWIPE_RIGHT,

    /** Swipe up on element or page */
    SWIPE_UP,

    /** Swipe down on element or page */
    SWIPE_DOWN,

    /** Grab (start drag) an element */
    GRAB,

    /** Release a grabbed element */
    RELEASE,

    /** Rotate an element */
    ROTATE,

    /** Drag an element to a position */
    DRAG,

    /** Double-click/double-tap an element */
    DOUBLE_CLICK,

    /** Hover over an element */
    HOVER,

    /** Move viewport (pan camera) */
    PAN,

    /** Tilt element or viewport */
    TILT,

    /** Orbit around an element */
    ORBIT,

    /** Rotate element around X axis */
    ROTATE_X,

    /** Rotate element around Y axis */
    ROTATE_Y,

    /** Rotate element around Z axis */
    ROTATE_Z,

    /** Pinch gesture (two-finger zoom) */
    PINCH,

    /** Fling gesture (fast directional swipe) */
    FLING,

    /** Throw gesture (velocity-based release) */
    THROW,

    /** Scale an element */
    SCALE,

    /** Reset zoom to default */
    RESET_ZOOM,

    /** Select a word at position */
    SELECT_WORD,

    /** Clear text selection */
    CLEAR_SELECTION,

    /** Hover out (mouse leave) */
    HOVER_OUT,

    // ═══════════════════════════════════════════════════════════════════
    // Drawing/Annotation Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Begin a drawing stroke at cursor position */
    STROKE_START,

    /** End the current drawing stroke */
    STROKE_END,

    /** Toggle eraser mode at cursor position */
    ERASE,

    // ═══════════════════════════════════════════════════════════════════
    // Note Editing Actions (NoteAvanue)
    // ═══════════════════════════════════════════════════════════════════

    /** Open NoteAvanue module */
    OPEN_MODULE,

    /** Toggle bold formatting */
    FORMAT_BOLD,

    /** Toggle italic formatting */
    FORMAT_ITALIC,

    /** Toggle underline formatting */
    FORMAT_UNDERLINE,

    /** Toggle strikethrough formatting */
    FORMAT_STRIKETHROUGH,

    /** Apply heading level 1 */
    HEADING_1,

    /** Apply heading level 2 */
    HEADING_2,

    /** Apply heading level 3 */
    HEADING_3,

    /** Toggle bullet (unordered) list */
    BULLET_LIST,

    /** Toggle numbered (ordered) list */
    NUMBERED_LIST,

    /** Toggle checklist/task item */
    CHECKLIST,

    /** Toggle code block */
    CODE_BLOCK,

    /** Toggle blockquote */
    BLOCKQUOTE,

    /** Insert horizontal divider */
    INSERT_DIVIDER,

    /** Navigate to start of document */
    GO_TO_TOP,

    /** Navigate to end of document */
    GO_TO_BOTTOM,

    /** Navigate to next heading */
    NEXT_HEADING,

    /** Navigate to previous heading */
    PREVIOUS_HEADING,

    /** Delete current line/paragraph */
    DELETE_LINE,

    /** Insert new paragraph */
    NEW_PARAGRAPH,

    /** Switch to dictation mode */
    DICTATION_MODE,

    /** Switch to command mode */
    COMMAND_MODE,

    /** Switch to continuous dictation */
    CONTINUOUS_MODE,

    /** Create a new note */
    NEW_NOTE,

    /** Toggle note pin status */
    TOGGLE_PIN,

    /** Export note */
    EXPORT_NOTE,

    /** Search within notes */
    SEARCH_NOTES,

    /** Insert text at cursor (used by dictation pipeline) */
    INSERT_TEXT,

    /** Note-scoped undo (avoids collision with global UNDO in TextHandler) */
    NOTE_UNDO,

    /** Note-scoped redo (avoids collision with global REDO in TextHandler) */
    NOTE_REDO,

    /** Save current note */
    SAVE_NOTE,

    /** Attach a file to note */
    ATTACH_FILE,

    /** Attach an audio recording to note */
    ATTACH_AUDIO,

    /** Increase editor font size */
    INCREASE_FONT,

    /** Decrease editor font size */
    DECREASE_FONT,

    /** Clear all formatting from selection */
    CLEAR_FORMATTING,

    /** Show word/character count */
    WORD_COUNT,

    // ═══════════════════════════════════════════════════════════════════
    // Camera Module Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Capture a photo */
    CAPTURE_PHOTO,

    /** Start video recording */
    RECORD_START,

    /** Stop video recording */
    RECORD_STOP,

    /** Pause video recording */
    RECORD_PAUSE,

    /** Resume video recording */
    RECORD_RESUME,

    /** Switch camera lens (front/back) */
    SWITCH_LENS,

    /** Flash on */
    FLASH_ON,

    /** Flash off */
    FLASH_OFF,

    /** Flash auto */
    FLASH_AUTO,

    /** Flash torch (continuous) */
    FLASH_TORCH,

    /** Increase camera exposure */
    EXPOSURE_UP,

    /** Decrease camera exposure */
    EXPOSURE_DOWN,

    /** Switch to photo capture mode */
    MODE_PHOTO,

    /** Switch to video recording mode */
    MODE_VIDEO,

    // ── Pro Camera ──

    /** Enable bokeh/portrait extension */
    BOKEH_MODE,

    /** Enable HDR extension */
    HDR_MODE,

    /** Enable night mode extension */
    NIGHT_MODE,

    /** Enable face retouch extension */
    RETOUCH_MODE,

    /** Disable all camera extensions (auto mode) */
    EXTENSION_OFF,

    /** Enable pro (manual) camera controls */
    PRO_MODE_ON,

    /** Disable pro mode (return to auto) */
    PRO_MODE_OFF,

    /** Increase ISO sensitivity */
    ISO_UP,

    /** Decrease ISO sensitivity */
    ISO_DOWN,

    /** Focus on nearby objects */
    FOCUS_NEAR,

    /** Focus on distant objects */
    FOCUS_FAR,

    /** Set white balance to auto */
    WB_AUTO,

    /** Set white balance to daylight */
    WB_DAYLIGHT,

    /** Set white balance to cloudy */
    WB_CLOUDY,

    /** Enable RAW/DNG capture */
    RAW_ON,

    /** Disable RAW capture */
    RAW_OFF,

    // ═══════════════════════════════════════════════════════════════════
    // Cockpit Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Add a new frame to cockpit */
    ADD_FRAME,

    /** Open layout picker */
    LAYOUT_PICKER,

    /** Switch to grid layout */
    LAYOUT_GRID,

    /** Switch to split layout */
    LAYOUT_SPLIT,

    /** Switch to freeform layout */
    LAYOUT_FREEFORM,

    /** Switch to fullscreen layout */
    LAYOUT_FULLSCREEN,

    /** Switch to workflow layout */
    LAYOUT_WORKFLOW,

    /** Minimize a frame */
    MINIMIZE_FRAME,

    /** Maximize a frame */
    MAXIMIZE_FRAME,

    /** Close a frame */
    CLOSE_FRAME,

    /** Add web content frame */
    ADD_WEB,

    /** Add camera frame */
    ADD_CAMERA,

    /** Add note frame */
    ADD_NOTE,

    /** Add PDF frame */
    ADD_PDF,

    /** Add image frame */
    ADD_IMAGE,

    /** Add video frame */
    ADD_VIDEO,

    /** Add whiteboard frame */
    ADD_WHITEBOARD,

    /** Add terminal frame */
    ADD_TERMINAL,

    // ═══════════════════════════════════════════════════════════════
    // PDF Viewing Actions
    // ═══════════════════════════════════════════════════════════════

    /** Go to next page in PDF viewer */
    PDF_NEXT_PAGE,

    /** Go to previous page in PDF viewer */
    PDF_PREVIOUS_PAGE,

    /** Go to first page in PDF viewer */
    PDF_FIRST_PAGE,

    /** Go to last page in PDF viewer */
    PDF_LAST_PAGE,

    /** Go to specific page (page number in metadata) */
    PDF_GO_TO_PAGE,

    /** Zoom in on PDF */
    PDF_ZOOM_IN,

    /** Zoom out on PDF */
    PDF_ZOOM_OUT,

    /** Reset PDF zoom to fit page */
    PDF_FIT_PAGE,

    // ═══════════════════════════════════════════════════════════════
    // Annotation/Drawing Actions
    // ═══════════════════════════════════════════════════════════════

    /** Select pen drawing tool */
    ANNOTATION_PEN,

    /** Select highlighter tool */
    ANNOTATION_HIGHLIGHTER,

    /** Draw rectangle shape */
    ANNOTATION_SHAPE_RECT,

    /** Draw circle/oval shape */
    ANNOTATION_SHAPE_CIRCLE,

    /** Draw arrow shape */
    ANNOTATION_SHAPE_ARROW,

    /** Draw straight line */
    ANNOTATION_SHAPE_LINE,

    /** Open color picker */
    ANNOTATION_COLOR_PICKER,

    /** Undo last annotation stroke */
    ANNOTATION_UNDO,

    /** Redo last undone stroke */
    ANNOTATION_REDO,

    /** Clear all annotations */
    ANNOTATION_CLEAR,

    /** Save annotation as image */
    ANNOTATION_SAVE,

    /** Share annotation */
    ANNOTATION_SHARE,

    /** Switch to eraser tool */
    ANNOTATION_ERASER,

    /** Increase pen thickness */
    ANNOTATION_PEN_SIZE_UP,

    /** Decrease pen thickness */
    ANNOTATION_PEN_SIZE_DOWN,

    // ═══════════════════════════════════════════════════════════════
    // Image Viewing/Editing Actions
    // ═══════════════════════════════════════════════════════════════

    /** Open image viewer */
    IMAGE_OPEN,

    /** Open photo gallery */
    IMAGE_GALLERY,

    /** Apply grayscale filter */
    IMAGE_FILTER_GRAYSCALE,

    /** Apply sepia filter */
    IMAGE_FILTER_SEPIA,

    /** Apply blur filter */
    IMAGE_FILTER_BLUR,

    /** Apply sharpen filter */
    IMAGE_FILTER_SHARPEN,

    /** Adjust image brightness */
    IMAGE_FILTER_BRIGHTNESS,

    /** Adjust image contrast */
    IMAGE_FILTER_CONTRAST,

    /** Rotate image 90° left */
    IMAGE_ROTATE_LEFT,

    /** Rotate image 90° right */
    IMAGE_ROTATE_RIGHT,

    /** Flip image horizontally */
    IMAGE_FLIP_H,

    /** Flip image vertically */
    IMAGE_FLIP_V,

    /** Enter crop mode */
    IMAGE_CROP,

    /** Share image */
    IMAGE_SHARE,

    /** Delete image */
    IMAGE_DELETE,

    /** Show image EXIF/metadata */
    IMAGE_INFO,

    /** View next image in gallery */
    IMAGE_NEXT,

    /** View previous image in gallery */
    IMAGE_PREVIOUS,

    // ═══════════════════════════════════════════════════════════════
    // Video Playback Actions
    // ═══════════════════════════════════════════════════════════════

    /** Play/resume video */
    VIDEO_PLAY,

    /** Pause video */
    VIDEO_PAUSE,

    /** Stop video playback */
    VIDEO_STOP,

    /** Skip forward (10 seconds) */
    VIDEO_SEEK_FWD,

    /** Skip backward (10 seconds) */
    VIDEO_SEEK_BACK,

    /** Increase playback speed */
    VIDEO_SPEED_UP,

    /** Decrease playback speed */
    VIDEO_SPEED_DOWN,

    /** Reset playback to normal speed */
    VIDEO_SPEED_NORMAL,

    /** Toggle fullscreen mode */
    VIDEO_FULLSCREEN,

    /** Mute video audio */
    VIDEO_MUTE,

    /** Unmute video audio */
    VIDEO_UNMUTE,

    /** Toggle loop playback */
    VIDEO_LOOP,

    // ═══════════════════════════════════════════════════════════════
    // Screen Casting Actions
    // ═══════════════════════════════════════════════════════════════

    /** Start screen casting */
    CAST_START,

    /** Stop screen casting */
    CAST_STOP,

    /** Connect to cast device */
    CAST_CONNECT,

    /** Disconnect from cast device */
    CAST_DISCONNECT,

    /** Change cast quality */
    CAST_QUALITY,

    // ═══════════════════════════════════════════════════════════════
    // AI Actions
    // ═══════════════════════════════════════════════════════════════

    /** Generate AI summary */
    AI_SUMMARIZE,

    /** Open AI chat */
    AI_CHAT,

    /** Search knowledge base via RAG */
    AI_RAG_SEARCH,

    /** Teach AI new knowledge */
    AI_TEACH,

    /** Clear AI conversation context */
    AI_CLEAR_CONTEXT,

    /** Custom/specialized action */
    CUSTOM,

    /** Macro: sequential chain of actions */
    MACRO;

    /**
     * Check if this is an element interaction action
     */
    fun isElementAction(): Boolean = this in listOf(
        CLICK, TAP, LONG_CLICK, TYPE, FOCUS, EXECUTE,
        SCROLL_DOWN, SCROLL_UP, SCROLL_LEFT, SCROLL_RIGHT, SCROLL,
        DOUBLE_CLICK, HOVER, GRAB, DRAG
    )

    /**
     * Check if this is a system-level action
     */
    fun isSystemAction(): Boolean = this in listOf(
        BACK, HOME, RECENT_APPS, APP_DRAWER,
        OPEN_SETTINGS, NOTIFICATIONS, CLEAR_NOTIFICATIONS,
        SCREENSHOT, FLASHLIGHT_ON, FLASHLIGHT_OFF,
        BRIGHTNESS_UP, BRIGHTNESS_DOWN, LOCK_SCREEN, ROTATE_SCREEN,
        TOGGLE_WIFI, TOGGLE_BLUETOOTH,
        OPEN_APP, CLOSE_APP
    )

    /**
     * Check if this is a text/clipboard action
     */
    fun isTextAction(): Boolean = this in listOf(
        SELECT_ALL, COPY, PASTE, CUT, UNDO, REDO, DELETE
    )

    /**
     * Check if this is a cursor action
     */
    fun isCursorAction(): Boolean = this in listOf(
        CURSOR_SHOW, CURSOR_HIDE, CURSOR_CLICK,
        CURSOR_UP, CURSOR_DOWN, CURSOR_LEFT, CURSOR_RIGHT
    )

    /**
     * Check if this is a reading/TTS action
     */
    fun isReadingAction(): Boolean = this in listOf(
        READ_SCREEN, STOP_READING
    )

    /**
     * Check if this is a media action
     */
    fun isMediaAction(): Boolean = this in listOf(
        MEDIA_PLAY, MEDIA_PAUSE, MEDIA_NEXT, MEDIA_PREVIOUS,
        VOLUME_UP, VOLUME_DOWN, VOLUME_MUTE
    )

    /**
     * Check if this is a VoiceOS control action
     */
    fun isVoiceOSAction(): Boolean = this in listOf(
        VOICE_MUTE, VOICE_WAKE, DICTATION_START, DICTATION_STOP, SHOW_COMMANDS,
        NUMBERS_ON, NUMBERS_OFF, NUMBERS_AUTO
    )

    /**
     * Check if this is a PDF viewing action
     */
    fun isPdfAction(): Boolean = this in listOf(
        PDF_NEXT_PAGE, PDF_PREVIOUS_PAGE, PDF_FIRST_PAGE, PDF_LAST_PAGE,
        PDF_GO_TO_PAGE, PDF_ZOOM_IN, PDF_ZOOM_OUT, PDF_FIT_PAGE
    )

    /**
     * Check if this is a camera/photo action
     */
    fun isCameraAction(): Boolean = this in listOf(
        CAPTURE_PHOTO, RECORD_START, RECORD_STOP, RECORD_PAUSE, RECORD_RESUME,
        SWITCH_LENS, FLASH_ON, FLASH_OFF, FLASH_AUTO, FLASH_TORCH,
        EXPOSURE_UP, EXPOSURE_DOWN, MODE_PHOTO, MODE_VIDEO,
        BOKEH_MODE, HDR_MODE, NIGHT_MODE, RETOUCH_MODE, EXTENSION_OFF,
        PRO_MODE_ON, PRO_MODE_OFF, ISO_UP, ISO_DOWN,
        FOCUS_NEAR, FOCUS_FAR, WB_AUTO, WB_DAYLIGHT, WB_CLOUDY,
        RAW_ON, RAW_OFF
    )

    /**
     * Check if this is an annotation/drawing action
     */
    fun isAnnotationAction(): Boolean = this in listOf(
        ANNOTATION_PEN, ANNOTATION_HIGHLIGHTER, ANNOTATION_SHAPE_RECT,
        ANNOTATION_SHAPE_CIRCLE, ANNOTATION_SHAPE_ARROW, ANNOTATION_SHAPE_LINE,
        ANNOTATION_COLOR_PICKER, ANNOTATION_UNDO, ANNOTATION_REDO, ANNOTATION_CLEAR,
        ANNOTATION_SAVE, ANNOTATION_SHARE, ANNOTATION_ERASER,
        ANNOTATION_PEN_SIZE_UP, ANNOTATION_PEN_SIZE_DOWN
    )

    /**
     * Check if this is an image viewing/editing action
     */
    fun isImageAction(): Boolean = this in listOf(
        IMAGE_OPEN, IMAGE_GALLERY, IMAGE_FILTER_GRAYSCALE, IMAGE_FILTER_SEPIA,
        IMAGE_FILTER_BLUR, IMAGE_FILTER_SHARPEN, IMAGE_FILTER_BRIGHTNESS,
        IMAGE_FILTER_CONTRAST, IMAGE_ROTATE_LEFT, IMAGE_ROTATE_RIGHT,
        IMAGE_FLIP_H, IMAGE_FLIP_V, IMAGE_CROP, IMAGE_SHARE, IMAGE_DELETE,
        IMAGE_INFO, IMAGE_NEXT, IMAGE_PREVIOUS
    )

    /**
     * Check if this is a video playback action
     */
    fun isVideoAction(): Boolean = this in listOf(
        VIDEO_PLAY, VIDEO_PAUSE, VIDEO_STOP, VIDEO_SEEK_FWD, VIDEO_SEEK_BACK,
        VIDEO_SPEED_UP, VIDEO_SPEED_DOWN, VIDEO_SPEED_NORMAL, VIDEO_FULLSCREEN,
        VIDEO_MUTE, VIDEO_UNMUTE, VIDEO_LOOP
    )

    /**
     * Check if this is a screen casting action
     */
    fun isCastAction(): Boolean = this in listOf(
        CAST_START, CAST_STOP, CAST_CONNECT, CAST_DISCONNECT, CAST_QUALITY
    )

    /**
     * Check if this is an AI action
     */
    fun isAIAction(): Boolean = this in listOf(
        AI_SUMMARIZE, AI_CHAT, AI_RAG_SEARCH, AI_TEACH, AI_CLEAR_CONTEXT
    )

    /**
     * Check if this is a browser/web action
     */
    fun isBrowserAction(): Boolean = this in listOf(
        RETRAIN_PAGE, PAGE_BACK, PAGE_FORWARD, PAGE_REFRESH,
        SCROLL_TO_TOP, SCROLL_TO_BOTTOM,
        TAB_NEXT, TAB_PREV, SUBMIT_FORM,
        SWIPE_LEFT, SWIPE_RIGHT, SWIPE_UP, SWIPE_DOWN,
        GRAB, RELEASE, ROTATE, DRAG,
        DOUBLE_CLICK, HOVER,
        PAN, TILT, ORBIT, ROTATE_X, ROTATE_Y, ROTATE_Z,
        PINCH, FLING, THROW, SCALE,
        RESET_ZOOM, SELECT_WORD, CLEAR_SELECTION, HOVER_OUT,
        // Dual-purpose: also handled by web pipeline when browser active
        ZOOM_IN, ZOOM_OUT, SCROLL_UP, SCROLL_DOWN, SCROLL_LEFT, SCROLL_RIGHT,
        TAP, LONG_CLICK, FOCUS,
        // Drawing/annotation
        STROKE_START, STROKE_END, ERASE
    )

    companion object {
        /**
         * Parse action type from string.
         *
         * @param value Action type string
         * @return CommandActionType, defaults to CLICK if invalid
         */
        fun fromString(value: String): CommandActionType {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                CLICK
            }
        }
    }
}
