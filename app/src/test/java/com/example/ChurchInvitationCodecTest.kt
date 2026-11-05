package com.example

import com.example.data.ChurchProfileEntity
import com.example.export.ChurchInvitationCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChurchInvitationCodecTest {
    @Test
    fun invitation_ne_transporte_que_l_identite_de_l_eglise() {
        val profile = ChurchProfileEntity(
            churchUid = "church-uid-123456",
            churchName = "Église de Kandi",
            denomination = "Assemblée",
            city = "Kandi",
            phone = "+229 00 00 00",
            pastorName = "Nom privé",
            logoBase64 = "c21hbGwtaW1hZ2U=",
            accentColorHex = "#3730A3",
            themeMode = "GLASS_DARK",
            amountDisplayMode = "COMPACT",
            onboardingCompleted = true
        )

        val bytes = ChurchInvitationCodec.encode(profile, "invite-1", createdAt = 123L)
        val clearText = String(bytes, Charsets.UTF_8)
        val decoded = ChurchInvitationCodec.decode(bytes)

        assertTrue(bytes.size < 2 * 1024 * 1024)
        assertFalse(clearText.contains("phone"))
        assertFalse(clearText.contains("Nom privé"))
        assertFalse(clearText.contains("\"transactions\""))
        assertFalse(clearText.contains("\"members\""))
        assertFalse(clearText.contains("\"users\""))
        assertFalse(clearText.contains("\"accounts\""))
        assertEquals("invite-1", decoded.invitationId)
        assertEquals("church-uid-123456", decoded.profile.churchUid)
        assertEquals("Église de Kandi", decoded.profile.churchName)
        assertEquals("c21hbGwtaW1hZ2U=", decoded.profile.logoBase64)
        assertEquals("#3730A3", decoded.profile.accentColorHex)
        assertEquals("GLASS_DARK", decoded.profile.themeMode)
        assertTrue(decoded.profile.onboardingCompleted)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invitation_ne_peut_pas_etre_creee_depuis_un_profil_non_configure() {
        ChurchInvitationCodec.encode(ChurchProfileEntity(), "invite-2")
    }
}
