package com.xcloak.airflux.core.common

import okhttp3.OkHttpClient
import okhttp3.Request

object MetadataResolver {
    fun resolve(client: OkHttpClient, url: String): Triple<String, String, Long>? {
        try {
            val headRequest = Request.Builder().url(url).head().build()
            client.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val fileName = UrlUtils.extractFileName(url, response.header("Content-Disposition"))
                    val mimeType = UrlUtils.guessMimeType(fileName, response.header("Content-Type"))
                    val size = response.header("Content-Length")?.toLongOrNull() ?: -1L
                    return Triple(fileName, mimeType, size)
                }
            }
        } catch (e: Exception) { }

        return try {
            val getRequest = Request.Builder().url(url).header("Range", "bytes=0-0").build()
            client.newCall(getRequest).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) return null
                val fileName = UrlUtils.extractFileName(url, response.header("Content-Disposition"))
                val mimeType = UrlUtils.guessMimeType(fileName, response.header("Content-Type"))
                val contentRange = response.header("Content-Range")
                val size = contentRange?.substringAfterLast('/')?.toLongOrNull()
                    ?: response.header("Content-Length")?.toLongOrNull()
                    ?: -1L
                Triple(fileName, mimeType, size)
            }
        } catch (e: Exception) {
            null
        }
    }
}