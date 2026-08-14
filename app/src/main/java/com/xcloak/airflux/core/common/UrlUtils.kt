package com.xcloak.airflux.core.common

import android.webkit.MimeTypeMap
import java.net.URL
import java.net.URLDecoder

object UrlUtils {

    fun extractFileName(url: String, contentDisposition: String?): String {
        contentDisposition?.let {
            val match = Regex("filename=\"?([^\";]+)\"?").find(it)
            if (match != null) return match.groupValues[1]
        }
        val path = try { URL(url).path } catch (e: Exception) { url }
        val last = path.substringAfterLast('/').ifBlank { "download_${System.currentTimeMillis()}" }
        return try { URLDecoder.decode(last, "UTF-8") } catch (e: Exception) { last }
    }

    fun guessMimeType(fileName: String, headerMime: String?): String {
        if (!headerMime.isNullOrBlank() && headerMime != "application/octet-stream") return headerMime
        val ext = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase()) ?: "application/octet-stream"
    }

    fun isValidUrl(url: String): Boolean =
        url.startsWith("http://") || url.startsWith("https://")
}