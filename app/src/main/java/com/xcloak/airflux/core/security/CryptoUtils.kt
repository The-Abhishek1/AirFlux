package com.xcloak.airflux.core.security

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private val FIXED_IV = ByteArray(16)

    private fun deriveKey(token: String): SecretKeySpec {
        val hash = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return SecretKeySpec(hash.copyOf(16), "AES")
    }

    /** AES/CTR is symmetric: applying this same transform to plaintext produces ciphertext,
     *  and applying it again to that ciphertext with the same key/IV recovers the plaintext. */
    fun wrapInputStream(rawInput: java.io.InputStream, token: String): CipherInputStream {
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(token), IvParameterSpec(FIXED_IV))
        return CipherInputStream(rawInput, cipher)
    }
}