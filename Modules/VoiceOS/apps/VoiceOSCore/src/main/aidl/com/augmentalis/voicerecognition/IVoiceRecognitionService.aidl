package com.augmentalis.voicerecognition;

import com.augmentalis.voicerecognition.IRecognitionCallback;

/**
 * Voice Recognition Service Interface
 * 
 * Provides speech recognition functionality with multiple engine support
 * and real-time callback communication.
 */
interface IVoiceRecognitionService {
    
    /**
     * Start voice recognition with specified parameters
     * 
     * @param engine The recognition engine to use (e.g., "google", "vivoka", "whisper")
     * @param language The language code (e.g., "en-US", "fr-FR")
     * @param mode Recognition mode (0=continuous, 1=single_shot, 2=streaming)
     * @return true if recognition started successfully, false otherwise
     */
    boolean startRecognition(String engine, String language, int mode);
    
    /**
     * Stop current voice recognition session
     * 
     * @return true if recognition stopped successfully, false otherwise
     */
    boolean stopRecognition();
    
    /**
     * Check if recognition is currently active
     * 
     * @return true if currently recognizing, false otherwise
     */
    boolean isRecognizing();
    
    /**
     * Register a callback to receive recognition events
     * 
     * @param callback The callback interface to register
     */
    void registerCallback(IRecognitionCallback callback);
    
    /**
     * Unregister a previously registered callback
     * 
     * @param callback The callback interface to unregister
     */
    void unregisterCallback(IRecognitionCallback callback);
    
    /**
     * Get list of available recognition engines
     * 
     * @return List of available engine names
     */
    List<String> getAvailableEngines();
    
    /**
     * Get current service status
     * 
     * @return Status string describing current state
     */
    String getStatus();
}