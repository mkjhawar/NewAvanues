package com.augmentalis.noteavanue.attachment

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private const val TAG = "NoteAudioRecorder"

/**
 * State of the audio recorder.
 */
enum class RecorderState {
    IDLE,
    RECORDING,
    PAUSED
}

/**
 * Simple audio recorder for note voice memos.
 *
 * Records audio as AAC in M4A container, optimized for speech:
 * - 44.1kHz sample rate
 * - 128kbps bitrate (good voice quality, reasonable file size)
 * - Pause/resume support (API 24+)
 *
 * Output files are temporary — the caller should use [NoteAttachmentResolver]
 * to import the recording into the managed attachment directory.
 */
class NoteAudioRecorder(
    private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L

    private val _state = MutableStateFlow(RecorderState.IDLE)
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    /**
     * Start recording audio.
     *
     * @return The temp file where audio is being written
     * @throws IllegalStateException if already recording
     */
    fun startRecording(): File {
        check(_state.value == RecorderState.IDLE) { "Already recording" }

        val tempFile = File(context.cacheDir, "note_recording_${System.currentTimeMillis()}.m4a")
        outputFile = tempFile

        recorder = createMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128_000)
            setOutputFile(tempFile.absolutePath)
            prepare()
            start()
        }

        startTimeMs = System.currentTimeMillis()
        _state.value = RecorderState.RECORDING
        Log.i(TAG, "Recording started → ${tempFile.absolutePath}")

        return tempFile
    }

    /**
     * Pause recording (API 24+).
     */
    fun pauseRecording() {
        check(_state.value == RecorderState.RECORDING) { "Not recording" }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder?.pause()
            _durationMs.value = System.currentTimeMillis() - startTimeMs
            _state.value = RecorderState.PAUSED
            Log.d(TAG, "Recording paused at ${_durationMs.value}ms")
        } else {
            Log.w(TAG, "Pause not supported below API 24")
        }
    }

    /**
     * Resume recording after pause.
     */
    fun resumeRecording() {
        check(_state.value == RecorderState.PAUSED) { "Not paused" }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder?.resume()
            _state.value = RecorderState.RECORDING
            Log.d(TAG, "Recording resumed")
        }
    }

    /**
     * Stop recording and return the output file.
     *
     * @return The recorded audio file, or null if recording failed
     */
    fun stopRecording(): File? {
        if (_state.value == RecorderState.IDLE) return null

        _durationMs.value = System.currentTimeMillis() - startTimeMs

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder: ${e.message}", e)
        }

        recorder = null
        _state.value = RecorderState.IDLE

        val file = outputFile
        outputFile = null

        if (file != null && file.exists() && file.length() > 0) {
            Log.i(TAG, "Recording stopped: ${file.name} (${file.length()} bytes, ${_durationMs.value}ms)")
            return file
        }

        Log.w(TAG, "Recording file empty or missing")
        return null
    }

    /**
     * Cancel recording and delete the temp file.
     */
    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
            // Ignore — may not have been started
        }

        recorder = null
        _state.value = RecorderState.IDLE
        _durationMs.value = 0

        outputFile?.delete()
        outputFile = null
        Log.d(TAG, "Recording cancelled")
    }

    /**
     * Release all resources.
     */
    fun release() {
        cancelRecording()
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }
}
