/**
 * VoiceOSServiceBinder.java - IPC service binder implementation for VoiceOSService
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-12
 *
 * Phase 3: Java implementation to resolve Hilt + ksp + AIDL circular dependency.
 * Java files compile before Kotlin, so AIDL-generated classes are available.
 */
package com.augmentalis.voiceoscore.accessibility;

import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * VoiceOS Service AIDL implementation (Java version)
 *
 * Exposes VoiceOSService functionality via IPC for use by external apps.
 * This binder wraps the VoiceOSService and provides thread-safe access
 * to its functionality across process boundaries.
 *
 * Note: Implemented in Java to avoid Kotlin/AIDL circular dependency.
 */
public class VoiceOSServiceBinder extends IVoiceOSService.Stub {

    private static final String TAG = "VoiceOSServiceBinder";

    private final VoiceOSService service;
    private final RemoteCallbackList<IVoiceOSCallback> callbacks;

    public VoiceOSServiceBinder(VoiceOSService service) {
        this.service = service;
        this.callbacks = new RemoteCallbackList<>();
        Log.d(TAG, "VoiceOSServiceBinder created");
    }

    @Override
    public boolean isServiceReady() {
        Log.d(TAG, "IPC: isServiceReady() called");
        try {
            return VoiceOSService.isServiceCurrentlyRunning() && service.isServiceReady;
        } catch (Exception e) {
            Log.e(TAG, "Error checking service ready state", e);
            return false;
        }
    }

    @Override
    public boolean executeCommand(String commandText) {
        Log.d(TAG, "IPC: executeCommand(" + commandText + ")");
        try {
            return VoiceOSService.executeStaticCommand(commandText);
        } catch (Exception e) {
            Log.e(TAG, "Error executing command via IPC", e);
            return false;
        }
    }

    @Override
    public boolean executeAccessibilityAction(String actionType, String parameters) {
        Log.d(TAG, "IPC: executeAccessibilityAction(" + actionType + ")");
        try {
            return service.executeAccessibilityActionByType(actionType);
        } catch (Exception e) {
            Log.e(TAG, "Error executing accessibility action via IPC", e);
            return false;
        }
    }

    @Override
    public String scrapeCurrentScreen() {
        Log.d(TAG, "IPC: scrapeCurrentScreen()");
        try {
            return service.scrapeScreen();
        } catch (Exception e) {
            Log.e(TAG, "Error scraping screen via IPC", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Override
    public void registerCallback(IVoiceOSCallback callback) {
        Log.d(TAG, "IPC: registerCallback()");
        if (callback != null) {
            callbacks.register(callback);
            Log.i(TAG, "Callback registered successfully");
        }
    }

    @Override
    public void unregisterCallback(IVoiceOSCallback callback) {
        Log.d(TAG, "IPC: unregisterCallback()");
        if (callback != null) {
            boolean removed = callbacks.unregister(callback);
            Log.i(TAG, "Callback unregistered: " + removed);
        }
    }

    @Override
    public String getServiceStatus() {
        Log.d(TAG, "IPC: getServiceStatus()");
        try {
            boolean ready = service.isServiceReady;
            return "{\"ready\": " + ready + ", \"running\": true}";
        } catch (Exception e) {
            Log.e(TAG, "Error getting service status via IPC", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Override
    public List<String> getAvailableCommands() {
        Log.d(TAG, "IPC: getAvailableCommands()");
        try {
            // TODO: Return actual available commands
            List<String> commands = new ArrayList<>();
            commands.add("back");
            commands.add("home");
            commands.add("recent");
            commands.add("notifications");
            return commands;
        } catch (Exception e) {
            Log.e(TAG, "Error getting available commands via IPC", e);
            return new ArrayList<>();
        }
    }

    // ============================================================
    // Phase 3: Extended IPC Methods
    // ============================================================

    @Override
    public boolean startVoiceRecognition(String language, String recognizerType) {
        Log.d(TAG, "IPC: startVoiceRecognition(language=" + language + ", type=" + recognizerType + ")");
        try {
            return service.startVoiceRecognition(language, recognizerType);
        } catch (Exception e) {
            Log.e(TAG, "Error starting voice recognition via IPC", e);
            return false;
        }
    }

    @Override
    public boolean stopVoiceRecognition() {
        Log.d(TAG, "IPC: stopVoiceRecognition()");
        try {
            return service.stopVoiceRecognition();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping voice recognition via IPC", e);
            return false;
        }
    }

    @Override
    public String learnCurrentApp() {
        Log.d(TAG, "IPC: learnCurrentApp()");
        try {
            return service.learnCurrentApp();
        } catch (Exception e) {
            Log.e(TAG, "Error learning current app via IPC", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Override
    public List<String> getLearnedApps() {
        Log.d(TAG, "IPC: getLearnedApps()");
        try {
            return service.getLearnedApps();
        } catch (Exception e) {
            Log.e(TAG, "Error getting learned apps via IPC", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getCommandsForApp(String packageName) {
        Log.d(TAG, "IPC: getCommandsForApp(packageName=" + packageName + ")");
        try {
            return service.getCommandsForApp(packageName);
        } catch (Exception e) {
            Log.e(TAG, "Error getting commands for app via IPC", e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean registerDynamicCommand(String commandText, String actionJson) {
        Log.d(TAG, "IPC: registerDynamicCommand(command=" + commandText + ")");
        try {
            return service.registerDynamicCommand(commandText, actionJson);
        } catch (Exception e) {
            Log.e(TAG, "Error registering dynamic command via IPC", e);
            return false;
        }
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        try {
            callbacks.kill();
            Log.d(TAG, "Callbacks cleaned up");
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up callbacks", e);
        }
    }
}
