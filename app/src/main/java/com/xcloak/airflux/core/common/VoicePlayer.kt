package com.xcloak.airflux.core.common

import android.media.MediaPlayer

object VoicePlayer {
    private var player: MediaPlayer? = null
    private var currentlyPlayingId: Long? = null

    fun playFile(messageId: Long, path: String, onCompletion: () -> Unit) {
        stop()
        try {
            player = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    currentlyPlayingId = null
                    onCompletion()
                }
                prepare()
                start()
            }
            currentlyPlayingId = messageId
        } catch (e: Throwable) {
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