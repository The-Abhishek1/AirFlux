package com.xcloak.airflux.core.common

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.xcloak.airflux.domain.model.SelectedFile
import kotlin.math.ln
import kotlin.math.pow

object FileUtils {

    fun resolveSelectedFile(context: Context, uri: Uri): SelectedFile? {
        var name = "Unknown"
        var size = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
            }
        } ?: return null

        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

        return SelectedFile(uri = uri, name = name, sizeBytes = size, mimeType = mimeType)
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format("%.1f %s", value, units[digitGroups])
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "-- KB/s"
        return "${formatSize(bytesPerSec)}/s"
    }

    fun formatEta(seconds: Long): String {
        if (seconds < 0) return "--"
        if (seconds < 60) return "${seconds}s"
        val minutes = seconds / 60
        val remSeconds = seconds % 60
        return "${minutes}m ${remSeconds}s"
    }
}