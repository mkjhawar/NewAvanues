package com.augmentalis.voicerecognition;

/**
 * Recognition Callback Interface
 * 
 * Callback interface for receiving speech recognition events and results.
 * All callbacks are asynchronous and may be called from background threads.
 */
interface IRecognitionCallback {
    
    /**
     * Called when recognition produces a result
     * 
     * @param text The recognized text
     * @param confidence Confidence score (0.0 to 1.0)
     * @param isFinal true if this is a final result, false for partial/intermediate
     */
    void onRecognitionResult(String text, float confidence, boolean isFinal);
    
    /**
     * Called when an error occurs during recognition
     * 
     * @param errorCode Error code (standard Android speech recognition error codes)
     * @param message Human-readable error message
     */
    void onError(int errorCode, String message);
    
    /**
     * Called when recognition state changes
     * 
     * @param state New state (0=idle, 1=listening, 2=processing, 3=error)
     * @param message Optional state description message
     */
    void onStateChanged(int state, String message);
    
    /**
     * Called during recognition with partial results
     * 
     * @param partialText Partial recognition text (may change as recognition continues)
     */
    void onPartialResult(String partialText);
}