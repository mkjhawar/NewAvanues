package com.augmentalis.voiceoscore.accessibility;

/**
 * VoiceOS Service Callback Interface
 *
 * Callback interface for receiving VoiceOS service events.
 * All callbacks are asynchronous and may be called from background threads.
 */
interface IVoiceOSCallback {

    /**
     * Called when a voice command is recognized
     *
     * @param command The recognized command text
     * @param confidence Confidence score (0.0 to 1.0)
     */
    void onCommandRecognized(String command, float confidence);

    /**
     * Called when a command execution completes
     *
     * @param command The executed command
     * @param success true if command succeeded, false otherwise
     * @param message Result message or error description
     */
    void onCommandExecuted(String command, boolean success, String message);

    /**
     * Called when service state changes
     *
     * @param state New state (0=stopped, 1=starting, 2=ready, 3=error)
     * @param message State description message
     */
    void onServiceStateChanged(int state, String message);

    /**
     * Called when UI scraping completes
     *
     * @param elementsJson JSON string containing scraped elements
     * @param elementCount Number of elements scraped
     */
    void onScrapingComplete(String elementsJson, int elementCount);
}
