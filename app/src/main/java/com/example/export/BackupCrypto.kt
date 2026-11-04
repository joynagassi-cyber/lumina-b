package com.example.export

import java.security.SecureRandom
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Protection de la sauvegarde par un mot de passe.
 *
 * Le texte est chiffré en AES-256-GCM avec une clé dérivée du mot de passe
 * (PBKDF2, 120 000 itérations, sel aléatoire). Sans le mot de passe, le fichier
 * est illisible : il peut donc être transmis sans risque.
 *
 * Format du fichier : une première ligne d'en-tête, puis sel | vecteur
 * d'initialisation | données chiffrées, le tout en Base64.
 */
object BackupCrypto {

    private const val HEADER = "LUMINA-SAUVEGARDE-CHIFFREE-1"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun isEncrypted(payload: String): Boolean = payload.trimStart().startsWith(HEADER)

    fun encrypt(plain: String, password: String): String {
        require(password.isNotEmpty()) { "Mot de passe vide." }
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            deriveKey(password, salt),
            GCMParameterSpec(TAG_BITS, iv)
        )
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val payload = ByteArray(salt.size + iv.size + encrypted.size)
        salt.copyInto(payload, 0)
        iv.copyInto(payload, salt.size)
        encrypted.copyInto(payload, salt.size + iv.size)
        return HEADER + "\n" + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    /** @throws IllegalArgumentException si le mot de passe est faux ou le fichier abîmé. */
    fun decrypt(payload: String, password: String): String {
        val trimmed = payload.trim()
        val body = trimmed.substringAfter('\n', "")
        if (body.isEmpty()) throw IllegalArgumentException("Sauvegarde chiffrée incomplète.")
        val raw = try {
            Base64.decode(body, Base64.DEFAULT)
        } catch (e: Exception) {
            throw IllegalArgumentException("Sauvegarde illisible : fichier endommagé.")
        }
        if (raw.size <= SALT_BYTES + IV_BYTES) {
            throw IllegalArgumentException("Sauvegarde illisible : fichier endommagé.")
        }
        val salt = raw.copyOfRange(0, SALT_BYTES)
        val iv = raw.copyOfRange(SALT_BYTES, SALT_BYTES + IV_BYTES)
        val encrypted = raw.copyOfRange(SALT_BYTES + IV_BYTES, raw.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            deriveKey(password, salt),
            GCMParameterSpec(TAG_BITS, iv)
        )
        val clear = try {
            cipher.doFinal(encrypted)
        } catch (e: Exception) {
            throw IllegalArgumentException("Mot de passe incorrect.")
        }
        return String(clear, Charsets.UTF_8)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(key, "AES")
    }
}
