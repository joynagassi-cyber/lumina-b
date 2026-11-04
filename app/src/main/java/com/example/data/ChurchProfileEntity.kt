package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Identité de l'organisation (l'église locale).
 *
 * Une seule ligne (id = 1). Créée par l'assistant de premier lancement puis
 * modifiable dans les paramètres. Elle alimente l'en-tête des rapports PDF,
 * la couleur d'accent de l'interface et les préférences d'affichage.
 */
@Entity(tableName = "church_profile")
data class ChurchProfileEntity(
    @PrimaryKey
    val id: Long = 1L,
    val churchName: String = "",
    val denomination: String = "",
    val city: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val pastorName: String = "",
    val treasurerName: String = "",
    /** Logo encodé en Base64 (data:image/... retiré) : survit aux sauvegardes. */
    val logoBase64: String = "",
    /** Couleur d'accent choisie à l'onboarding, ex. "#3730A3". */
    val accentColorHex: String = DEFAULT_ACCENT,
    val onboardingCompleted: Boolean = false,
    val themeMode: String = "SYSTEM",
    val amountDisplayMode: String = "STANDARD",
    val notificationsEnabled: Boolean = true,
    /** Devise affichée dans les rapports (FCFA par défaut). */
    val currencyCode: String = "FCFA",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = churchName.trim().ifBlank { "Mon église" }

    val hasLogo: Boolean
        get() = logoBase64.isNotBlank()

    companion object {
        const val DEFAULT_ACCENT = "#3730A3"
        const val ROW_ID = 1L
    }
}

@Dao
interface ChurchProfileDao {
    @Query("SELECT * FROM church_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<ChurchProfileEntity?>

    @Query("SELECT * FROM church_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): ChurchProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ChurchProfileEntity)

    @Query("DELETE FROM church_profile")
    suspend fun deleteAllProfiles()
}
