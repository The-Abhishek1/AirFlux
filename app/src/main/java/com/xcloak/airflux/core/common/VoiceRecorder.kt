package com.xcloak.airflux.core.common

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import java.io.File

class VoiceRecorder(private val context: Context) {

    companion object {
        // Voice messages are sent as a single base64 JSON line over the chat socket
        // (see ChatSession/BluetoothChatSession). Capping duration keeps the payload
        // small so a send finishes quickly, which also shrinks the window in which a
        // concurrent send could contend for the write lock.
        const val MAX_DURATION_MS = 2 * 60 * 1000 // 2 minutes
    }

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    /** Called if the recording is auto-stopped after hitting [MAX_DURATION_MS]. */
    var onMaxDurationReached: (() -> Unit)? = null

    fun start(): Boolean {
        return try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = file
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                setMaxDuration(MAX_DURATION_MS)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        onMaxDurationReached?.invoke()
                    }
                }
                prepare()
                start()
            }
            recorder = r
            true
        } catch (e: Exception) {
            recorder = null
            false
        }
    }

    /** Stops recording and returns base64 audio, or null if too short / failed. */
    fun stopAndGetBase64(): String? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            val file = outputFile ?: return null
            if (!file.exists() || file.length() < 500) return null
            val bytes = file.readBytes()
            file.delete()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun cancel() {
        try {
            recorder?.apply { stop(); release() }
        } catch (e: Exception) { }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}
