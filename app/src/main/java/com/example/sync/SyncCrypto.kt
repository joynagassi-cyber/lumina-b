package com.example.sync

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Chiffrement des fichiers d'échange.
 *
 * Format binaire : "LUMINA1\n" + sel (16 octets) + IV (12 octets) + texte
 * chiffré AES-256-GCM. La clé est dérivée de la phrase de passe de l'église
 * (PBKDF2-HMAC-SHA1, 200 000 itérations — disponible dès Android 7).
 *
 * Sans la phrase de passe, le fichier est inutilisable : il peut donc circuler
 * par Bluetooth, WhatsApp ou clé USB sans exposer les comptes de l'église.
 */
object SyncCrypto {

    private val MAGIC = "LUMINA1\n".toByteArray(Charsets.US_ASCII)
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val TAG_BITS = 128
    private const val ITERATIONS = 200_000
    private const val KEY_BITS = 256

    class WrongPassphrase : Exception("Phrase de passe incorrecte ou fichier altéré")

    fun encrypt(plainText: ByteArray, passphrase: String): ByteArray {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val cipherText = cipher.doFinal(plainText)

        return MAGIC + salt + iv + cipherText
    }

    fun decrypt(data: ByteArray, passphrase: String): ByteArray {
        val headerSize = MAGIC.size + SALT_SIZE + IV_SIZE
        if (data.size <= headerSize || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw IllegalArgumentException("Ce fichier n'est pas un échange Lumina")
        }
        val salt = data.copyOfRange(MAGIC.size, MAGIC.size + SALT_SIZE)
        val iv = data.copyOfRange(MAGIC.size + SALT_SIZE, headerSize)
        val cipherText = data.copyOfRange(headerSize, data.size)

        val key = deriveKey(passphrase, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            cipher.doFinal(cipherText)
        } catch (e: Exception) {
            throw WrongPassphrase()
        }
    }

    fun looksEncrypted(data: ByteArray): Boolean =
        data.size > MAGIC.size && data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val raw = factory.generateSecret(spec).encoded
        return SecretKeySpec(raw, "AES")
    }
}
