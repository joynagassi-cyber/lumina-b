package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hachage des codes PIN locaux. Les nouvelles empreintes utilisent PBKDF2-HMAC-SHA1
 * (210 000 itérations) avec un sel aléatoire. Les empreintes SHA-256 historiques
 * restent vérifiables et sont remplacées après une connexion réussie.
 */
object PinHasher {

    private const val SALT_BYTES = 16
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val MODERN_PREFIX = "pbkdf2-sha1-v1$"

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(pin: String, salt: String): String =
        MODERN_PREFIX + derive(pin, salt).toHex()

    fun verify(pin: String, salt: String, expectedHash: String): Boolean {
        val modern = expectedHash.startsWith(MODERN_PREFIX)
        val stored = if (modern) expectedHash.removePrefix(MODERN_PREFIX) else expectedHash
        val actual = if (modern) derive(pin, salt).toHex() else legacyHash(pin, salt)
        return MessageDigest.isEqual(
            actual.lowercase().toByteArray(Charsets.US_ASCII),
            stored.lowercase().toByteArray(Charsets.US_ASCII)
        )
    }

    fun needsUpgrade(expectedHash: String): Boolean = !expectedHash.startsWith(MODERN_PREFIX)

    private fun derive(pin: String, salt: String): ByteArray {
        val characters = pin.toCharArray()
        val spec = PBEKeySpec(characters, salt.toByteArray(Charsets.UTF_8), ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            characters.fill('\u0000')
        }
    }

    private fun legacyHash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { byte -> "%02x".format(byte) }
}
