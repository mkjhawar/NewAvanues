/**
 * VoiceOSIPCService.java - Companion IPC service for VoiceOSService
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-12
 *
 * Phase 3: Companion service pattern to resolve Hilt + ksp + AIDL circular dependency.
 * This service provides IPC access to VoiceOSService functionality without using Hilt.
 */
package com.augmentalis.voiceoscore.accessibility;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * VoiceOS IPC Companion Service
 *
 * Provides IPC access to VoiceOSService functionality via AIDL.
 * This service exists separately from VoiceOSService because:
 * - AccessibilityService.onBind() is final and cannot be overridden
 * - VoiceOSService uses Hilt which creates circular dependency with AIDL
 * - Companion service pattern allows clean IPC without architectural constraints
 *
 * Architecture:
 * External App → binds to → VoiceOSIPCService → delegates to → VoiceOSService
 *
 * Usage from external app:
 * <pre>{@code
 * Intent intent = new Intent();
 * intent.setAction("com.augmentalis.voiceoscore.BIND_IPC");
 * intent.setPackage("com.augmentalis.voiceoscore");
 * bindService(intent, connection, Context.BIND_AUTO_CREATE);
 * }</pre>
 *
 * Security:
 * - Signature-level permission protection
 * - Only apps signed with same certificate can bind
 * - Future: Custom permission for third-party SDK access
 */
public class VoiceOSIPCService extends Service {

    private static final String TAG = "VoiceOSIPCService";
    private static final String ACTION_BIND_IPC = "com.augmentalis.voiceoscore.BIND_IPC";

    private VoiceOSServiceBinder binder;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "VoiceOSIPCService created");
    }

    /**
     * Bind to service and return IPC binder
     *
     * Returns the AIDL binder for the IPC action, null otherwise.
     * The binder delegates all calls to VoiceOSService via getInstance().
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind() called with action: " + (intent != null ? intent.getAction() : "null"));

        if (intent != null && ACTION_BIND_IPC.equals(intent.getAction())) {
            // Get VoiceOSService instance
            VoiceOSService voiceOSService = VoiceOSService.getInstance();
            if (voiceOSService == null) {
                Log.e(TAG, "VoiceOSService not running, cannot bind");
                return null;
            }

            // Create binder if needed
            if (binder == null) {
                Log.i(TAG, "Creating new VoiceOSServiceBinder");
                binder = new VoiceOSServiceBinder(voiceOSService);
            }

            Log.i(TAG, "Returning IPC binder");
            return binder.asBinder();
        } else {
            Log.w(TAG, "Unknown action: " + (intent != null ? intent.getAction() : "null") + ", returning null");
            return null;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind() called");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "VoiceOSIPCService destroyed");

        // Cleanup binder resources
        if (binder != null) {
            binder.cleanup();
            binder = null;
        }
    }
}
