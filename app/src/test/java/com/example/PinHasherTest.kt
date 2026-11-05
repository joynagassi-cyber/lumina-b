package com.example

import com.example.util.PinHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class PinHasherTest {
    @Test
    fun nouvelles_empreintes_sont_couteuses_et_les_anciennes_restent_migrables() {
        val salt = PinHasher.newSalt()
        val modern = PinHasher.hash("4826", salt)

        assertTrue(modern.startsWith("pbkdf2-sha1-v1$"))
        assertTrue(PinHasher.verify("4826", salt, modern))
        assertFalse(PinHasher.verify("4827", salt, modern))
        assertFalse(PinHasher.needsUpgrade(modern))

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        digest.update("4826".toByteArray(Charsets.UTF_8))
        val legacy = digest.digest().joinToString("") { "%02x".format(it) }
        assertTrue(PinHasher.verify("4826", salt, legacy))
        assertTrue(PinHasher.needsUpgrade(legacy))
    }
}
