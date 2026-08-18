package com.xcloak.airflux.core.common

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.File

object VideoUtils {
    // Lowered from 15MB: base64 encoding adds ~33% overhead, and wrapping that in a
    // JSONObject + toString() briefly holds multiple full copies in memory at once.
    // 5MB raw keeps peak memory usage during send well within typical device heap limits.
    const val MAX_VIDEO_BYTES = 5L * 1024 * 1024

    fun getSizeBytes(context: Context, uri: Uri): Long {
        var size = -1L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex >= 0) size = cursor.getLong(sizeIndex)
            }
        } catch (e: Exception) { }
        return size
    }

    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                Base64.encodeToString(input.readBytes(), Base64.NO_WRAP)
            }
        } catch (e: Throwable) {
            // Throwable, not Exception: catches OutOfMemoryError too, so a too-large
            // file fails gracefully instead of crashing the whole app.
            null
        }
    }

    fun base64ToPlayableUri(context: Context, base64: String, messageId: Long): Uri? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val file = File(context.cacheDir, "chat_video_$messageId.mp4")
            file.writeBytes(bytes)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Throwable) {
            null
        }
    }
}