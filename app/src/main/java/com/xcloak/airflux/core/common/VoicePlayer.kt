package com.xcloak.airflux.core.common

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import java.io.File

object VoicePlayer {
    private var player: MediaPlayer? = null
    private var currentlyPlayingId: Long? = null

    fun play(context: Context, messageId: Long, base64: String, onCompletion: () -> Unit) {
        stop()
        try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val file = File(context.cacheDir, "playback_$messageId.m4a")
            file.writeBytes(bytes)
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    currentlyPlayingId = null
                    onCompletion()
                    file.delete()
                }
                prepare()
                start()
            }
            currentlyPlayingId = messageId
        } catch (e: Exception) {
            currentlyPlayingId = null
            onCompletion()
        }
    }

    fun stop() {
        try { player?.stop(); player?.release() } catch (e: Exception) { }
        player = null
        currentlyPlayingId = null
    }

    fun isPlaying(messageId: Long): Boolean = currentlyPlayingId == messageId
}