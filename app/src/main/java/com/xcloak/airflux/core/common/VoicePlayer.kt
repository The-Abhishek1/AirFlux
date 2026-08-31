package com.xcloak.airflux.core.common

import android.media.MediaPlayer

object VoicePlayer {
    private var player: MediaPlayer? = null
    private var currentlyPlayingId: Long? = null

    /**
     * Plays a voice message file.
     *
     * Uses prepareAsync() instead of the blocking prepare() — the previous synchronous
     * version ran disk IO + decoder init on whatever thread called playFile(), which in
     * practice was the Compose onClick (main) thread, risking jank/ANRs on longer
     * recordings. A genuine failure (corrupt file, unsupported codec, etc.) is now
     * reported via onError instead of being silently treated as "finished playing".
     */
    fun playFile(messageId: Long, path: String, onCompletion: () -> Unit, onError: () -> Unit = onCompletion) {
        stop()
        try {
            player = MediaPlayer().apply {
                setDataSource(path)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    currentlyPlayingId = null
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    currentlyPlayingId = null
                    onError()
                    true
                }
                currentlyPlayingId = messageId
                prepareAsync()
            }
        } catch (e: Throwable) {
            currentlyPlayingId = null
            onError()
        }
    }

    fun stop() {
        try { player?.stop(); player?.release() } catch (e: Exception) { }
        player = null
        currentlyPlayingId = null
    }

    fun isPlaying(messageId: Long): Boolean = currentlyPlayingId == messageId
}
