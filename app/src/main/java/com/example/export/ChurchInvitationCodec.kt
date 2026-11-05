package com.example.export

import com.example.data.ChurchProfileEntity
import org.json.JSONObject

/** Petit paquet d'identité : aucun journal financier ni membre n'y est inclus. */
data class ChurchInvitation(val profile: ChurchProfileEntity, val invitationId: String, val createdAt: Long)

object ChurchInvitationCodec {
    const val FORMAT = "lumina-invitation"
    const val VERSION = 1
    const val MAX_BYTES = 2 * 1024 * 1024
    private const val MAX_LOGO_BASE64_CHARS = 512 * 1024

    fun encode(profile: ChurchProfileEntity, invitationId: String, createdAt: Long = System.currentTimeMillis()): ByteArray {
        require(profile.churchUid.isNotBlank()) { "L'identité de l'église ne possède pas d'identifiant stable." }
        require(profile.onboardingCompleted && profile.churchName.isNotBlank()) {
            "Configurez d'abord le profil de l'église pour créer une invitation."
        }
        require(profile.logoBase64.length <= MAX_LOGO_BASE64_CHARS) {
            "Le logo est trop volumineux pour une invitation légère; réduisez-le ou utilisez une archive complète."
        }
        val safeLogo = profile.logoBase64
        val root = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("invitationId", invitationId)
            put("createdAt", createdAt)
            put("churchUid", profile.churchUid)
            put("churchName", profile.churchName)
            put("denomination", profile.denomination)
            put("city", profile.city)
            put("logoBase64", safeLogo)
            put("accentColorHex", profile.accentColorHex)
            put("themeMode", profile.themeMode)
            put("amountDisplayMode", profile.amountDisplayMode)
            put("currencyCode", profile.currencyCode)
        }
        val bytes = root.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "Le fichier d'invitation dépasse la taille autorisée." }
        return bytes
    }

    fun decode(bytes: ByteArray): ChurchInvitation {
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES) { "Fichier d'invitation vide ou trop volumineux." }
        val root = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (_: Exception) {
            throw IllegalArgumentException("Fichier d'invitation illisible.")
        }
        require(root.optString("format") == FORMAT) { "Ce fichier n'est pas une invitation Lumina." }
        require(root.optInt("version", 0) in 1..VERSION) { "Version d'invitation Lumina incompatible." }
        val churchUid = root.optString("churchUid").trim()
        val churchName = root.optString("churchName").trim()
        require(churchUid.matches(Regex("[A-Za-z0-9_-]{8,128}")) && churchName.isNotBlank()) {
            "L'invitation ne contient pas une identité d'église valide."
        }
        val logo = root.optString("logoBase64")
        require(logo.length <= MAX_LOGO_BASE64_CHARS) { "Le logo de l'invitation dépasse la taille autorisée." }
        if (logo.isNotBlank()) {
            val decodedLogo = try {
                android.util.Base64.decode(logo, android.util.Base64.DEFAULT)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Logo d'invitation illisible.")
            }
            require(decodedLogo.isNotEmpty() && decodedLogo.size <= 384 * 1024) { "Logo d'invitation vide ou trop volumineux." }
        }
        val profile = ChurchProfileEntity(
            churchUid = churchUid,
            churchName = churchName,
            denomination = root.optString("denomination"),
            city = root.optString("city"),
            logoBase64 = logo,
            accentColorHex = root.optString("accentColorHex")
                .takeIf { it.matches(Regex("#[0-9A-Fa-f]{6}")) } ?: ChurchProfileEntity.DEFAULT_ACCENT,
            onboardingCompleted = true,
            themeMode = root.optString("themeMode", "SYSTEM")
                .takeIf { it in setOf("SYSTEM", "LIGHT", "DARK") } ?: "SYSTEM",
            amountDisplayMode = root.optString("amountDisplayMode", "STANDARD")
                .takeIf { it in setOf("STANDARD", "COMPACT", "COMPTABLE", "DISCRET") } ?: "STANDARD",
            currencyCode = root.optString("currencyCode", "FCFA").take(12).ifBlank { "FCFA" }
        )
        return ChurchInvitation(
            profile = profile,
            invitationId = root.optString("invitationId").take(80),
            createdAt = root.optLong("createdAt")
        )
    }
}
