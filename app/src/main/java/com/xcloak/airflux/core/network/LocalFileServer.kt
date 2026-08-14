package com.xcloak.airflux.core.network

import android.content.Context
import com.xcloak.airflux.domain.model.SelectedFile
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class LocalFileServer(
    private val context: Context,
    port: Int,
    private val filesProvider: () -> List<SelectedFile>
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        return when {
            uri == "/" -> serveFileListPage()
            uri.startsWith("/file/") -> serveFile(uri.removePrefix("/file/"))
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
    }

    private fun serveFileListPage(): Response {
        val files = filesProvider()
        val rows = files.mapIndexed { index, file ->
            """<li><a href="/file/$index">${escapeHtml(file.name)}</a> 
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
            val inputStream = context.contentResolver.openInputStream(file.uri)
                ?: return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Could not open file")

            val response = newFixedLengthResponse(
                Response.Status.OK,
                file.mimeType,
                inputStream,
                file.sizeBytes
            )
            response.addHeader("Content-Disposition", "attachment; filename=\"${sanitizeFilename(file.name)}\"")
            response
        } catch (e: IOException) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error reading file")
        }
    }

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun sanitizeFilename(name: String): String =
        name.replace(Regex("[/\\\\]"), "_")

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