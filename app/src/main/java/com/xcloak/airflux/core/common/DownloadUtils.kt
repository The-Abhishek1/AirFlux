package com.xcloak.airflux.core.common

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.xcloak.airflux.core.security.CryptoUtils
import com.xcloak.airflux.core.security.SecurityUtils
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

data class DownloadResult(
    val success: Boolean,
    val mediaUri: Uri? = null,
    val bytesWritten: Long = 0,
    val totalBytes: Long = 0
)

object DownloadUtils {

    fun buildCall(client: OkHttpClient, url: String, resumeFromByte: Long = 0): Call {
        val builder = Request.Builder().url(url)
        if (resumeFromByte > 0) {
            builder.header("Range", "bytes=$resumeFromByte-")
        }
        return client.newCall(builder.build())
    }

    fun executeAndSave(
        context: Context,
        call: Call,
        fileName: String,
        mimeType: String,
        existingUri: Uri? = null,
        resumeFromByte: Long = 0,
        throttleBytesPerSec: Long? = null,
        decryptWithToken: String? = null,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit
    ): DownloadResult {
        call.execute().use { response ->
            val serverHonoredRange = response.code == 206
            if (!response.isSuccessful && !serverHonoredRange) return DownloadResult(false)
            val body = response.body ?: return DownloadResult(false)

            val safeName = SecurityUtils.sanitizeFileName(fileName)
            val resolver = context.contentResolver
            val (collection, relativePath) = collectionFor(mimeType)

            val itemUri: Uri
            val openMode: String
            val effectiveResumeFrom: Long

            when {
                existingUri != null && serverHonoredRange -> {
                    itemUri = existingUri
                    openMode = "wa"
                    effectiveResumeFrom = resumeFromByte
                }
                existingUri != null -> {
                    itemUri = existingUri
                    openMode = "w"
                    effectiveResumeFrom = 0
                }
                else -> {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }
                    itemUri = resolver.insert(collection, values) ?: return DownloadResult(false)
                    openMode = "w"
                    effectiveResumeFrom = 0
                }
            }

            val contentLength = body.contentLength()
            val totalBytes = if (effectiveResumeFrom > 0) effectiveResumeFrom + contentLength else contentLength

            val outputStream = resolver.openOutputStream(itemUri, openMode) ?: return DownloadResult(false)
            outputStream.use { out ->
                val rawInput = body.byteStream()
                // This is the fix: without this wrap, an encrypted transfer was being
                // saved to disk as raw ciphertext instead of decrypted plaintext.
                val streamToUse = if (decryptWithToken != null) {
                    CryptoUtils.wrapInputStream(rawInput, decryptWithToken)
                } else rawInput

                streamToUse.use { inputStream ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Long = effectiveResumeFrom
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        val chunkStart = System.currentTimeMillis()
                        out.write(buffer, 0, read)
                        bytesRead += read
                        onProgress(bytesRead, totalBytes)

                        if (throttleBytesPerSec != null && throttleBytesPerSec > 0) {
                            val expectedMillis = (read * 1000L) / throttleBytesPerSec
                            val actualMillis = System.currentTimeMillis() - chunkStart
                            val sleepMillis = expectedMillis - actualMillis
                            if (sleepMillis > 0) Thread.sleep(sleepMillis)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(itemUri, values, null, null)
                    }
                    return DownloadResult(true, itemUri, bytesRead, totalBytes)
                }
            }
        }
    }

    private fun collectionFor(mimeType: String): Pair<Uri, String> {
        return when {
            mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI to "Movies/AirFlux"
            mimeType.startsWith("audio/") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to "Music/AirFlux"
            mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI to "Pictures/AirFlux"
            else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI to "Download/AirFlux"
        }
    }
}