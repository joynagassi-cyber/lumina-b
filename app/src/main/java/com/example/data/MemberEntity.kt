package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Fiche d'un membre de l'église.
 *
 * Seuls le prénom et le nom sont obligatoires : tout le reste (situation
 * matrimoniale, conjoint, contact, adresse, lieu de résidence, profession,
 * notes) est optionnel et sert à joindre la personne et à tenir le registre
 * de l'assemblée.
 */
@Entity(
    tableName = "members",
    indices = [
        Index(value = ["lastName", "firstName"]),
        Index(value = ["phone"])
    ]
)
data class MemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val maritalStatus: String = "",
    val spouseName: String = "",
    val phone: String = "",
    val address: String = "",
    val residence: String = "",
    val profession: String = "",
    val notes: String = "",
    val joinedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    val fullName: String
        get() = listOf(firstName.trim(), lastName.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")

    /** Initiales pour l'avatar de la liste. */
    val initials: String
        get() = listOf(firstName, lastName)
            .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "?" }

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim()
        return listOf(firstName, lastName, phone, address, residence, profession, spouseName, notes)
            .any { it.contains(q, ignoreCase = true) }
    }
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY lastName COLLATE NOCASE ASC, firstName COLLATE NOCASE ASC")
    fun observeMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: MemberEntity): Long

    @Update
    suspend fun update(member: MemberEntity)

    @Delete
    suspend fun delete(member: MemberEntity)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM members")
    suspend fun count(): Int
}
