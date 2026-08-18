package com.xcloak.airflux.core.common

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

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