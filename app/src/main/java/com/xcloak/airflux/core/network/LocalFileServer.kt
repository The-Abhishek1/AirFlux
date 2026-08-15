package com.xcloak.airflux.core.network

import android.content.Context
import com.xcloak.airflux.core.security.RateLimiter
import com.xcloak.airflux.core.security.SecurityUtils
import com.xcloak.airflux.domain.model.SelectedFile
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class LocalFileServer(
    private val context: Context,
    port: Int,
    private val sessionToken: String,
    private val encryptionEnabled: Boolean,
    private val filesProvider: () -> List<SelectedFile>
) : NanoHTTPD(port) {

    private val rateLimiter = RateLimiter()

    override fun serve(session: IHTTPSession): Response {
        val clientId = try { session.remoteIpAddress } catch (e: Exception) { "unknown" }
        if (!rateLimiter.allowRequest(clientId)) {
            return newFixedLengthResponse(Response.Status.TOO_MANY_REQUESTS, MIME_PLAINTEXT, "Too many requests")
        }

        val token = session.parms["token"]
        if (token != sessionToken) {
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Invalid or missing session token")
        }

        val uri = session.uri
        return when {
            uri == "/" -> serveFileListPage()
            uri == "/files.json" -> serveFileListJson()
            uri.startsWith("/file/") -> serveFile(uri.removePrefix("/file/"))
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
    }

    private fun serveFileListJson(): Response {
        val files = filesProvider()
        val json = files.mapIndexed { index, file ->
            """{"index":$index,"name":"${escapeJson(file.name)}","size":${file.sizeBytes},"mimeType":"${file.mimeType}"}"""
        }.joinToString(",", prefix = "[", postfix = "]")
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun serveFileListPage(): Response {
        val files = filesProvider()
        val rows = files.mapIndexed { index, file ->
            """<li><a href="/file/$index?token=$sessionToken">${escapeHtml(file.name)}</a>
               (${formatSize(file.sizeBytes)})</li>"""
        }.joinToString("\n")

        val html = """
            <html>
            <head><title>AirFlux Share</title></head>
            <body style="font-family: sans-serif; background:#0A0E17; color:#F5F7FA; padding:24px;">
                <h2>Files shared via AirFlux</h2>
                <ul>$rows</ul>
            </body>
            </html>
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    private fun serveFile(indexParam: String): Response {
        val index = indexParam.toIntOrNull()
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid file id")

        val file = filesProvider().getOrNull(index)
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")

        return try {
            val rawStream = context.contentResolver.openInputStream(file.uri)
                ?: return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Could not open file")

            val finalStream = if (encryptionEnabled) {
                com.xcloak.airflux.core.security.CryptoUtils.wrapInputStream(rawStream, sessionToken)
            } else rawStream

            val response = newFixedLengthResponse(
                Response.Status.OK,
                file.mimeType,
                finalStream,
                file.sizeBytes
            )
            val safeName = SecurityUtils.sanitizeFileName(file.name)
            response.addHeader("Content-Disposition", "attachment; filename=\"$safeName\"")
            if (encryptionEnabled) response.addHeader("X-AirFlux-Encrypted", "1")
            response
        } catch (e: IOException) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error reading file")
        }
    }

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun escapeJson(text: String): String =
        text.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        return String.format("%.1f %s", value, units[unitIndex])
    }
}