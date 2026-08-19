package com.xcloak.airflux.core.common

import android.content.Context
import android.util.Base64
import java.io.File
import java.util.UUID

enum class MediaKind { IMAGE, AUDIO, VIDEO }

object ChatMediaUtils {
    private fun mediaDir(context: Context): File {
        val dir = File(context.filesDir, "chat_media")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun extensionFor(kind: MediaKind): String = when (kind) {
        MediaKind.IMAGE -> "jpg"
        MediaKind.AUDIO -> "m4a"
        MediaKind.VIDEO -> "mp4"
    }

    fun saveBase64ToFile(context: Context, base64: String, kind: MediaKind): String? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val file = File(mediaDir(context), "${UUID.randomUUID()}.${extensionFor(kind)}")
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Throwable) {
            null
        }
    }

    fun deleteMediaFile(path: String?) {
        if (path == null) return
        try { File(path).delete() } catch (e: Exception) { }
    }
}