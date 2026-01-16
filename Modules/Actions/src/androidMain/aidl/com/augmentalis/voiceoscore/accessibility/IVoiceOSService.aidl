package com.augmentalis.voiceoscore.accessibility;

import com.augmentalis.voiceoscore.accessibility.IVoiceOSCallback;

/**
 * VoiceOS Service Interface
 *
 * Provides accessibility service functionality and voice command execution
 * for cross-app usage via IPC.
 */
interface IVoiceOSService {

    /**
     * Check if VoiceOS service is currently running and ready
     *
     * @return true if service is active and ready, false otherwise
     * @hide Internal use only - not exposed in public API
     */
    boolean isServiceReady();

    /**
     * Execute a system voice command
     *
     * Supported commands:
     * - "back", "go back" - Navigate back
     * - "home", "go home" - Go to home screen
     * - "recent", "recent apps" - Show recent apps
     * - "notifications" - Open notification panel
     * - "settings", "quick settings" - Open quick settings
     * - "power", "power menu" - Show power dialog
     * - "screenshot" - Take screenshot (Android P+)
     *
     * @param commandText The voice command to execute
     * @return true if command executed successfully, false otherwise
     */
    boolean executeCommand(String commandText);

    /**
     * Execute custom accessibility action
     *
     * @param actionType Action type identifier (e.g., "click", "scroll", "swipe")
     * @param parameters JSON string containing action parameters
     * @return true if action executed successfully, false otherwise
     */
    boolean executeAccessibilityAction(String actionType, String parameters);

    /**
     * Request UI scraping of current screen
     *
     * Triggers accessibility tree scraping and returns scraped elements.
     *
     * @return JSON string containing scraped UI elements
     * @hide Internal use only - not exposed in public API
     */
    String scrapeCurrentScreen();

    /**
     * Register callback for service events
     *
     * @param callback Callback interface to receive service events
     */
    void registerCallback(IVoiceOSCallback callback);

    /**
     * Unregister previously registered callback
     *
     * @param callback Callback interface to unregister
     */
    void unregisterCallback(IVoiceOSCallback callback);

    /**
     * Get service status information
     *
     * @return JSON string with service status details
     */
    String getServiceStatus();

    /**
     * Get available voice commands
     *
     * @return List of available voice command strings
     */
    List<String> getAvailableCommands();

    // ============================================================
    // Extended Methods - Voice Recognition Control
    // ============================================================

    /**
     * Start voice recognition with specified configuration
     *
     * @param language Language code (e.g., "en-US", "es-ES")
     * @param recognizerType Recognition mode ("system", "continuous", "command")
     * @return true if recognition started successfully, false otherwise
     */
    boolean startVoiceRecognition(String language, String recognizerType);

    /**
     * Stop currently active voice recognition
     *
     * @return true if recognition stopped successfully, false otherwise
     */
    boolean stopVoiceRecognition();

    // ============================================================
    // Extended Methods - App Learning & Commands
    // ============================================================

    /**
     * Trigger app learning for currently focused app
     *
     * Scrapes accessibility tree and extracts available actions
     * for voice command creation.
     *
     * @return JSON string with learning results
     */
    String learnCurrentApp();

    /**
     * Get list of apps that have learned voice commands
     *
     * @return List of package names with learned commands
     */
    List<String> getLearnedApps();

    /**
     * Get voice commands available for specific app
     *
     * @param packageName Package name of the app
     * @return List of command strings for the app
     */
    List<String> getCommandsForApp(String packageName);

    /**
     * Register dynamic voice command at runtime
     *
     * @param commandText Voice command phrase (e.g., "open settings")
     * @param actionJson JSON describing action to execute
     * @return true if command registered successfully, false otherwise
     */
    boolean registerDynamicCommand(String commandText, String actionJson);
}
