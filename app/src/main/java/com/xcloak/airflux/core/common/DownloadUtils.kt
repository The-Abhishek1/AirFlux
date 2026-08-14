package com.xcloak.airflux.core.common

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

object DownloadUtils {

    fun buildCall(client: OkHttpClient, url: String): Call {
        val request = Request.Builder().url(url).build()
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
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }

            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val itemUri = resolver.insert(collection, values) ?: return false

            resolver.openOutputStream(itemUri)?.use { outputStream ->
                body.byteStream().use { inputStream ->
                    val buffer = ByteArray(8 * 1024)
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
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
        }
        return true
    }
}