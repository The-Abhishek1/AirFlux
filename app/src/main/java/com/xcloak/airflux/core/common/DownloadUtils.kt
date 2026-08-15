package com.xcloak.airflux.core.common

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import okhttp3.Call

object DownloadUtils {

    fun buildCall(client: okhttp3.OkHttpClient, url: String): Call {
        val request = okhttp3.Request.Builder().url(url).build()
        return client.newCall(request)
    }

    fun executeAndSave(
        context: Context,
        call: Call,
        fileName: String,
        mimeType: String,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit
    ): Boolean {
        call.execute().use { response ->
            if (!response.isSuccessful) return false
            val body = response.body ?: return false
            val totalBytes = body.contentLength()

            val resolver = context.contentResolver
            val (collection, relativePath) = collectionFor(mimeType)

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, com.xcloak.airflux.core.security.SecurityUtils.sanitizeFileName(fileName))
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val itemUri = resolver.insert(collection, values) ?: return false

            resolver.openOutputStream(itemUri)?.use { outputStream ->
                body.byteStream().use { inputStream ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Long = 0
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesRead += read
                        onProgress(bytesRead, totalBytes)
                    }
                }
            } ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
        }
        return true
    }

    /** Routes by MIME type: videos -> Movies/AirFlux, audio -> Music/AirFlux, everything else -> Download/AirFlux */
    private fun collectionFor(mimeType: String): Pair<Uri, String> {
        return when {
            mimeType.startsWith("video/") -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                uri to "Movies/AirFlux"
            }
            mimeType.startsWith("audio/") -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                uri to "Music/AirFlux"
            }
            mimeType.startsWith("image/") -> {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI to "Pictures/AirFlux"
            }
            else -> {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI to "Download/AirFlux"
            }
        }
    }
}