package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Empreinte des codes PIN.
 *
 * Le PIN n'est jamais conservé en clair : on stocke un sel aléatoire et
 * SHA-256(sel + PIN). Volontairement simple et sans dépendance : le code PIN
 * protège l'accès à l'écran, pas le fichier de synchronisation (chiffré
 * séparément par phrase de passe).
 */
object PinHasher {

    private const val SALT_BYTES = 16

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest().toHex()
    }

    fun verify(pin: String, salt: String, expectedHash: String): Boolean =
        hash(pin, salt).equals(expectedHash, ignoreCase = true)

    private fun ByteArray.toHex(): String =
        joinToString("") { byte -> "%02x".format(byte) }
}
