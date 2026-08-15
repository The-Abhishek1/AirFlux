package com.xcloak.airflux.core.security

import java.security.SecureRandom

object SecurityUtils {
    private val secureRandom = SecureRandom()

    fun generateSessionToken(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Never trust a filename from a remote device — strip path separators,
     *  traversal sequences, control characters, and cap length. */
    fun sanitizeFileName(name: String): String {
        var clean = name
            .replace(Regex("[/\\\\]"), "_")
            .replace("..", "_")
            .replace(Regex("[\u0000-\u001F]"), "")
            .trim()
        if (clean.isBlank()) clean = "file_${System.currentTimeMillis()}"
        if (clean.length > 200) clean = clean.takeLast(200)
        return clean
    }
}