package com.xcloak.airflux.core.common

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.File

object VideoUtils {
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
            null
        }
    }

    fun pathToPlayableUri(context: Context, path: String): Uri? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Throwable) {
            null
        }
    }
}